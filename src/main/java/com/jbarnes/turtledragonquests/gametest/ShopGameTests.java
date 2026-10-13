package com.jbarnes.turtledragonquests.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.shop.ShopMenu;
import com.jbarnes.turtledragonquests.shop.ShopMenuSnapshot;
import com.jbarnes.turtledragonquests.shop.ShopOffer;
import com.jbarnes.turtledragonquests.shop.ShopService;
import com.jbarnes.turtledragonquests.shop.ShopSide;
import com.jbarnes.turtledragonquests.shop.ShopWorldState;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TDQ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShopGameTests {
    private static final String BUY_STEAK_JSON = """
            {
              "side": "buy",
              "item": "minecraft:cooked_beef",
              "count": 8,
              "unit_price": 10,
              "required_quests": ["turtle_dragon_quests:cobble_tithe"],
              "category": "food"
            }
            """;

    private static final String SELL_DIAMONDS_JSON = """
            {
              "side": "sell",
              "item": "minecraft:diamond",
              "count": 1,
              "unit_price": 50,
              "global_stock": 64,
              "required_quests": ["turtle_dragon_quests:diamond_glint"],
              "category": "ores"
            }
            """;

    private ShopGameTests() {}

    @GameTest(template = "empty")
    public static void codec_roundtrip_example_offers(GameTestHelper helper) {
        ShopOffer steak = parse(BUY_STEAK_JSON);
        helper.assertTrue(steak.side() == ShopSide.BUY, "buy_steak side");
        helper.assertTrue(steak.stack().item().equals(ResourceLocation.withDefaultNamespace("cooked_beef")), "buy_steak item");
        helper.assertTrue(steak.stack().count() == 8, "buy_steak count");
        helper.assertTrue(steak.unitPrice() == 10L, "buy_steak unit_price");
        helper.assertTrue(steak.globalStock().isEmpty(), "buy_steak unlimited");
        helper.assertTrue(steak.requiredQuests().equals(java.util.List.of(TDQ.id("cobble_tithe"))), "buy_steak gate");
        helper.assertTrue(steak.category().orElse("").equals("food"), "buy_steak category");
        roundTrip(helper, "buy_steak json", steak);

        ShopOffer diamonds = parse(SELL_DIAMONDS_JSON);
        helper.assertTrue(diamonds.side() == ShopSide.SELL, "sell_diamonds side");
        helper.assertTrue(diamonds.globalStock().orElse(-1) == 64, "sell_diamonds quota 64");
        helper.assertTrue(diamonds.unitPrice() == 50L, "sell_diamonds unit_price");
        roundTrip(helper, "sell_diamonds json", diamonds);

        helper.assertTrue(
                ShopOffer.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                        { "side": "buy", "item": "minecraft:dirt", "unit_price": -1 }
                        """)).isError(),
                "unit_price < 0 is a codec error");
        helper.assertTrue(
                ShopOffer.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                        { "side": "buy", "item": "minecraft:dirt", "unit_price": 0 }
                        """)).result().isPresent(),
                "unit_price 0 is allowed");

        Registry<ShopOffer> registry = helper.getLevel().registryAccess()
                .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY);
        helper.assertTrue(registry.getOptional(TDQ.id("buy_steak")).isPresent(), "loaded buy_steak");
        helper.assertTrue(registry.getOptional(TDQ.id("sell_diamonds")).isPresent(), "loaded sell_diamonds");
        helper.assertTrue(registry.getOptional(TDQ.id("fixtures/stock_seed")).isPresent(), "loaded stock_seed fixture");
        helper.assertTrue(registry.getOptional(TDQ.id("fixtures/stock_deplete")).isPresent(), "loaded stock_deplete fixture");
        int campaign = 0;
        for (var entry : registry.entrySet()) {
            roundTrip(helper, entry.getKey().location().toString(), entry.getValue());
            if (!entry.getKey().location().getPath().startsWith("fixtures/")) {
                campaign++;
            }
        }
        helper.assertTrue(campaign == 13, "chapter-gated campaign offers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_stock_lazy_seed_and_reload_clamp(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation steak = TDQ.id("buy_steak");
        ResourceLocation seed = TDQ.id("fixtures/stock_seed");
        helper.assertTrue(
                helper.getLevel().registryAccess()
                        .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                        .getOptional(seed)
                        .isPresent(),
                "stock_seed fixture in registry");
        ShopWorldState.get(server).forget(seed);

        helper.assertValueEqual(Integer.MAX_VALUE, ShopService.remaining(server, steak), "unlimited remaining");
        helper.assertValueEqual(8, ShopService.remaining(server, seed), "lazy seed from JSON");

        ShopWorldState.get(server).setRemaining(seed, 3);
        helper.assertValueEqual(3, ShopService.remaining(server, seed), "admin remaining");
        ShopWorldState.afterReload(server);
        helper.assertValueEqual(3, ShopService.remaining(server, seed), "reload never raises remaining");

        ShopWorldState.get(server).setRemaining(seed, 100);
        ShopWorldState.afterReload(server);
        helper.assertValueEqual(8, ShopService.remaining(server, seed), "reload clamps down to JSON");

        ShopWorldState.get(server).setRemaining(seed, 0);
        ShopWorldState.afterReload(server);
        helper.assertValueEqual(0, ShopService.remaining(server, seed), "reload never resurrects remaining==0");

        CompoundTag leftover = new CompoundTag();
        leftover.putInt("schema", 2);
        leftover.putInt("keep_me", 7);
        ShopWorldState unreadable = ShopWorldState.load(leftover, helper.getLevel().registryAccess());
        helper.assertTrue(unreadable.unreadable(), "schema > 1 is unreadable");
        CompoundTag saved = unreadable.save(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(saved.getInt("schema") == 2, "leftover schema preserved");
        helper.assertTrue(saved.getInt("keep_me") == 7, "leftover NBT not clobbered");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_buy_insufficient_funds(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        claim(helper, player, "cobble_tithe");
        CurrencyService.set(player, 0);
        ShopMenu menu = ShopService.openForTest(player);
        int index = menu.snapshot().indexOf(TDQ.id("buy_steak"));
        helper.assertTrue(index >= 0, "buy_steak visible after cobble_tithe");
        helper.assertFalse(menu.clickMenuButton(player, index), "insufficient funds rejected");
        helper.assertValueEqual(0L, CurrencyService.get(player), "wallet unchanged");
        helper.assertValueEqual(0, countItem(player, Items.COOKED_BEEF), "no steak given");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_buy_success_gives_item_and_debits(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        claim(helper, player, "cobble_tithe");
        CurrencyService.set(player, 10);
        ShopMenu menu = ShopService.openForTest(player);
        int index = menu.snapshot().indexOf(TDQ.id("buy_steak"));
        helper.assertTrue(index >= 0, "buy_steak visible");
        helper.assertTrue(menu.clickMenuButton(player, index), "buy steak");
        helper.assertValueEqual(0L, CurrencyService.get(player), "debited 10 TDG");
        helper.assertValueEqual(8, countItem(player, Items.COOKED_BEEF), "8 cooked beef");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_buy_rejected_if_menu_closed(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        claim(helper, player, "cobble_tithe");
        CurrencyService.set(player, 10);
        ShopMenu menu = new ShopMenu(1, player.getInventory(), ShopService.snapshot(player));
        int index = menu.snapshot().indexOf(TDQ.id("buy_steak"));
        helper.assertTrue(index >= 0, "buy_steak visible");
        helper.assertTrue(player.containerMenu != menu, "menu is not open");
        helper.assertFalse(menu.clickMenuButton(player, index), "closed menu rejected");
        helper.assertValueEqual(10L, CurrencyService.get(player), "wallet unchanged when closed");
        helper.assertValueEqual(0, countItem(player, Items.COOKED_BEEF), "no steak when closed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_sell_extracts_then_credits(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MinecraftServer server = helper.getLevel().getServer();
        ShopWorldState.get(server).setRemaining(TDQ.id("sell_diamonds"), 64);
        claim(helper, player, "diamond_glint");
        long before = CurrencyService.get(player);
        ShopMenu menu = ShopService.openForTest(player);
        int index = menu.snapshot().indexOf(TDQ.id("sell_diamonds"));
        helper.assertTrue(index >= 0, "sell_diamonds visible after diamond_glint");
        menu.sellSlots().setItem(0, new ItemStack(Items.DIAMOND, 1));
        helper.assertTrue(menu.clickMenuButton(player, index), "sell diamond");
        helper.assertTrue(menu.sellSlots().getItem(0).isEmpty(), "sell slot extracted");
        helper.assertValueEqual(before + 50L, CurrencyService.get(player), "credited 50 TDG");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_stock_depletes_after_n_units(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation deplete = TDQ.id("fixtures/stock_deplete");
        helper.assertTrue(
                helper.getLevel().registryAccess()
                        .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                        .getOptional(deplete)
                        .isPresent(),
                "stock_deplete fixture in registry");
        ShopWorldState.get(server).setRemaining(deplete, 1);
        ShopMenuSnapshot snapshot = new ShopMenuSnapshot(
                java.util.List.of(deplete),
                java.util.Map.of(deplete, java.util.Optional.of(1)));
        ShopMenu menu = ShopService.openForTest(player, snapshot);
        int index = menu.snapshot().indexOf(deplete);
        helper.assertTrue(index >= 0, "stock_deplete visible");
        menu.sellSlots().setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        helper.assertTrue(menu.clickMenuButton(player, index), "first unit sells");
        helper.assertValueEqual(0, ShopService.remaining(server, deplete), "stock depleted");
        helper.runAtTickTime(helper.getTick() + 10, () -> {
            menu.sellSlots().setItem(0, new ItemStack(Items.COBBLESTONE, 1));
            helper.assertFalse(menu.clickMenuButton(player, index), "second unit refused");
            helper.assertTrue(menu.sellSlots().getItem(0).is(Items.COBBLESTONE), "item remains when sold out");
            helper.succeed();
        });
    }

    private static void claim(GameTestHelper helper, ServerPlayer player, String path) {
        ResourceLocation id = TDQ.id(path);
        helper.assertTrue(QuestEngine.grant(player, id), "grant " + path);
        helper.assertTrue(QuestEngine.forceComplete(player, id), "complete " + path);
        helper.assertTrue(QuestEngine.tryClaim(player, id), "claim " + path);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static ShopOffer parse(String json) {
        JsonElement element = JsonParser.parseString(json);
        return ShopOffer.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
    }

    private static void roundTrip(GameTestHelper helper, String label, ShopOffer offer) {
        JsonElement encoded = ShopOffer.CODEC.encodeStart(JsonOps.INSTANCE, offer).getOrThrow();
        ShopOffer decoded = ShopOffer.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        JsonElement encodedAgain = ShopOffer.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        helper.assertTrue(encoded.equals(encodedAgain), "codec round-trip " + label);
    }
}
