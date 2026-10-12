package com.jbarnes.turtledragonquests.gametest;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.registry.TDQItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TDQ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CurrencyGameTests {
    private CurrencyGameTests() {}

    @GameTest(template = "empty")
    public static void currency_debit_rejects_overdraft(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CurrencyService.set(player, 5);
        helper.assertFalse(CurrencyService.tryDebit(player, 6), "overdraft should fail");
        helper.assertValueEqual(5L, CurrencyService.get(player), "wallet after overdraft");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void currency_withdraw_rollback_when_inventory_full(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CurrencyService.set(player, 10);
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            inventory.items.set(i, new ItemStack(Items.DIRT, 64));
        }
        inventory.offhand.set(0, new ItemStack(Items.DIRT, 64));
        helper.assertFalse(CurrencyService.tryWithdraw(player, 1), "full inventory should reject withdraw");
        helper.assertValueEqual(10L, CurrencyService.get(player), "wallet after full-inventory withdraw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void currency_withdraw_rejects_spectator(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CurrencyService.set(player, 10);
        helper.assertTrue(player.setGameMode(GameType.SPECTATOR), "setGameMode spectator");
        helper.assertTrue(
                player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
                "mock player gameMode should be SPECTATOR");
        helper.assertFalse(CurrencyService.tryWithdraw(player, 1), "spectator withdraw should fail");
        helper.assertValueEqual(10L, CurrencyService.get(player), "wallet after spectator withdraw");
        int coins = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(TDQItems.TDG_COIN.get())) {
                coins += stack.getCount();
            }
        }
        helper.assertTrue(coins == 0, "spectator must not receive coin items");
        helper.succeed();
    }
}
