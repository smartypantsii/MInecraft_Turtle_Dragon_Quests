package com.jbarnes.turtledragonquests.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
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
        helper.assertTrue(registry.size() == 13, "chapter-gated campaign offers");
        helper.assertTrue(registry.getOptional(TDQ.id("buy_steak")).isPresent(), "loaded buy_steak");
        helper.assertTrue(registry.getOptional(TDQ.id("sell_diamonds")).isPresent(), "loaded sell_diamonds");
        for (var entry : registry.entrySet()) {
            roundTrip(helper, entry.getKey().location().toString(), entry.getValue());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shop_stock_lazy_seed_and_reload_clamp(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation steak = TDQ.id("buy_steak");
        ResourceLocation diamonds = TDQ.id("sell_diamonds");

        helper.assertValueEqual(Integer.MAX_VALUE, ShopService.remaining(server, steak), "unlimited remaining");
        helper.assertValueEqual(64, ShopService.remaining(server, diamonds), "lazy seed from JSON");

        ShopService.setStock(server, diamonds, 10);
        helper.assertValueEqual(10, ShopService.remaining(server, diamonds), "admin remaining");
        ShopService.afterReload(server);
        helper.assertValueEqual(10, ShopService.remaining(server, diamonds), "reload never raises remaining");

        ShopService.setStock(server, diamonds, 100);
        ShopService.afterReload(server);
        helper.assertValueEqual(64, ShopService.remaining(server, diamonds), "reload clamps down to JSON");

        ShopService.setStock(server, diamonds, 0);
        ShopService.afterReload(server);
        helper.assertValueEqual(0, ShopService.remaining(server, diamonds), "reload never resurrects remaining==0");

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
