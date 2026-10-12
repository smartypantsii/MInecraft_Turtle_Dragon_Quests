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
}
