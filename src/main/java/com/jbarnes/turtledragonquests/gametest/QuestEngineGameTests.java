package com.jbarnes.turtledragonquests.gametest;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.quest.QuestStatus;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TDQ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestEngineGameTests {
    private QuestEngineGameTests() {}

    @GameTest(template = "empty")
    public static void quest_kill_increments_and_completes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("night_watch");
        helper.assertTrue(QuestEngine.grant(player, id), "grant night_watch");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "night_watch active");
        for (int i = 0; i < 3; i++) {
            QuestEngine.onKill(player, EntityType.ZOMBIE);
        }
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "night_watch completed");
        helper.assertValueEqual(0L, CurrencyService.get(player), "no TDG before claim");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quest_claim_refuses_full_inventory_no_coin_credit(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("cobble_tithe");
        helper.assertTrue(QuestEngine.grant(player, id), "grant cobble_tithe");
        helper.assertTrue(QuestEngine.forceComplete(player, id), "force complete cobble_tithe");
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            inventory.items.set(i, new ItemStack(Items.DIRT, 64));
        }
        inventory.offhand.set(0, new ItemStack(Items.DIRT, 64));
        helper.assertFalse(QuestEngine.tryClaim(player, id), "full inventory should refuse claim");
        helper.assertValueEqual(0L, CurrencyService.get(player), "wallet after refused claim");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "still COMPLETED");
        int bread = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(Items.BREAD)) {
                bread += stack.getCount();
            }
        }
        helper.assertTrue(bread == 0, "no bread granted on refused claim");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quest_gather_possess_does_not_consume(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("first_timber");
        helper.assertTrue(QuestEngine.grant(player, id), "grant first_timber");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "first_timber active");
        var inventory = player.getInventory();
        inventory.add(new ItemStack(Items.OAK_LOG, 3));
        QuestEngine.recomputeGather(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "still ACTIVE with 3 logs");
        helper.assertValueEqual(3, countItem(inventory, Items.OAK_LOG), "possessed oak after partial gather");
        helper.assertValueEqual(
                3,
                QuestEngine.attachment(player).get(id).objectiveCounts().getOrDefault("any_logs", 0),
                "gather count tracks possess");
        inventory.clearContent();
        inventory.add(new ItemStack(Items.OAK_LOG, 4));
        inventory.add(new ItemStack(Items.BIRCH_LOG, 4));
        QuestEngine.recomputeGather(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "first_timber completed");
        helper.assertValueEqual(4, countItem(inventory, Items.OAK_LOG), "oak not consumed");
        helper.assertValueEqual(4, countItem(inventory, Items.BIRCH_LOG), "birch not consumed");
        inventory.clearContent();
        QuestEngine.recomputeGather(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "gather latched after dump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quest_submit_consumes_and_rejects_empty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("cobble_tithe");
        helper.assertTrue(QuestEngine.grant(player, id), "grant cobble_tithe");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "cobble_tithe active");
        helper.assertFalse(QuestEngine.trySubmit(player, id, "turn_in_cobble"), "empty submit rejected");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "still ACTIVE after empty submit");
        helper.assertValueEqual(
                0,
                QuestEngine.attachment(player).get(id).objectiveCounts().getOrDefault("turn_in_cobble", 0),
                "empty submit does not increment");
        var inventory = player.getInventory();
        inventory.add(new ItemStack(Items.COBBLESTONE, 4));
        helper.assertTrue(QuestEngine.trySubmit(player, id, "turn_in_cobble"), "partial submit");
        helper.assertValueEqual(0, countItem(inventory, Items.COBBLESTONE), "partial submit consumes cobble");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "still ACTIVE after partial");
        helper.assertValueEqual(
                4,
                QuestEngine.attachment(player).get(id).objectiveCounts().getOrDefault("turn_in_cobble", 0),
                "partial submit count");
        helper.assertFalse(QuestEngine.trySubmit(player, id, "turn_in_cobble"), "second empty submit rejected");
        helper.assertValueEqual(
                4,
                QuestEngine.attachment(player).get(id).objectiveCounts().getOrDefault("turn_in_cobble", 0),
                "rejected empty leaves count");
        inventory.add(new ItemStack(Items.COBBLESTONE, 16));
        helper.assertTrue(QuestEngine.trySubmit(player, id, "turn_in_cobble"), "finish submit");
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "cobble_tithe completed");
        helper.assertValueEqual(4, countItem(inventory, Items.COBBLESTONE), "only remaining need consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quest_craft_increments_and_completes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("crafting_bench");
        helper.assertTrue(QuestEngine.grant(player, id), "grant crafting_bench");
        QuestEngine.onCraft(player, new ItemStack(Items.CRAFTING_TABLE));
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "crafting_bench completed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quest_visit_box_completes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = TDQ.id("fixtures/visit_box");
        helper.assertTrue(QuestEngine.grant(player, id), "grant visit_box");
        player.setPos(100.0, 10.0, 100.0);
        QuestEngine.recomputeVisit(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.ACTIVE, "outside box stays ACTIVE");
        player.setPos(8.0, 70.0, 8.0);
        QuestEngine.recomputeVisit(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "inside box completes");
        player.setPos(100.0, 10.0, 100.0);
        QuestEngine.recomputeVisit(player);
        helper.assertTrue(QuestEngine.status(player, id) == QuestStatus.COMPLETED, "visit latches after leaving");
        helper.succeed();
    }

    private static int countItem(Inventory inventory, Item item) {
        int n = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }
}
