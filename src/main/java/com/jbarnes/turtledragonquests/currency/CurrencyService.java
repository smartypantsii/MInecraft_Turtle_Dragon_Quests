package com.jbarnes.turtledragonquests.currency;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.TDQConfig;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.network.s2c.WalletSyncS2C;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class CurrencyService {
    private CurrencyService() {}

    public static WalletAttachment wallet(ServerPlayer player) {
        return player.getData(TDQAttachmentTypes.WALLET);
    }

    public static long get(ServerPlayer player) {
        return wallet(player).tdg();
    }

    public static boolean tryDebit(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return false;
        }
        WalletAttachment current = wallet(player);
        if (current.tdg() < amount) {
            return false;
        }
        player.setData(TDQAttachmentTypes.WALLET, current.withTdg(current.tdg() - amount));
        sync(player);
        return true;
    }

    public static boolean hasRoom(ServerPlayer player, long amount) {
        if (amount < 0L) {
            return false;
        }
        if (amount == 0L) {
            return true;
        }
        long cap = TDQConfig.maxCoins();
        long current = get(player);
        return current <= cap && amount <= cap - current;
    }

    public static void credit(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return;
        }
        WalletAttachment current = wallet(player);
        long next = current.tdg();
        long cap = TDQConfig.maxCoins();
        long room = cap - next;
        if (room <= 0) {
            return;
        }
        next += Math.min(amount, room);
        player.setData(TDQAttachmentTypes.WALLET, current.withTdg(next));
        sync(player);
    }

    public static void set(ServerPlayer player, long tdg) {
        long cap = TDQConfig.maxCoins();
        long clamped = Math.max(0L, Math.min(tdg, cap));
        player.setData(TDQAttachmentTypes.WALLET, wallet(player).withTdg(clamped));
        sync(player);
    }

    public static void grantStarterIfNeeded(ServerPlayer player) {
        WalletAttachment current = wallet(player);
        if (current.starterGranted()) {
            return;
        }
        long start = Math.min(Math.max(0L, TDQConfig.startingCoins()), TDQConfig.maxCoins());
        player.setData(TDQAttachmentTypes.WALLET, new WalletAttachment(start, true, current.paidAchievements()));
        sync(player);
    }

    public static boolean tryWithdraw(ServerPlayer player, int itemCount) {
        if (!canUseCoinItems(player)) {
            return false;
        }
        if (itemCount < 1 || itemCount > 64) {
            return false;
        }
        if (freeCoinSpace(player.getInventory()) < itemCount) {
            return false;
        }
        if (!tryDebit(player, itemCount)) {
            return false;
        }
        Item coin = TDQItems.TDG_COIN.get();
        int leftover = 0;
        int remaining = itemCount;
        while (remaining > 0) {
            int n = Math.min(coin.getDefaultMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(coin, n);
            player.getInventory().add(stack);
            leftover += stack.getCount();
            remaining -= n;
        }
        if (leftover > 0) {
            credit(player, leftover);
            TDQ.LOGGER.warn("TDG withdraw leftover {} for {} after simulated space; credited back", leftover, player.getGameProfile().getName());
        }
        return leftover < itemCount;
    }

    public static long depositAll(ServerPlayer player) {
        if (!canUseCoinItems(player)) {
            return 0L;
        }
        Item coin = TDQItems.TDG_COIN.get();
        long deposited = 0L;
        Inventory inv = player.getInventory();
        deposited += extractCoins(inv.items, coin);
        deposited += extractCoins(inv.offhand, coin);
        if (deposited > 0) {
            credit(player, deposited);
        }
        return deposited;
    }

    public static void sync(ServerPlayer player) {
        TDQNetwork.sendToPlayer(player, new WalletSyncS2C(get(player)));
    }

    /**
     * GameTest mock ServerPlayers override {@code isSpectator()} to always return false.
     * {@link ServerPlayer#isSpectator()} is this same game-mode comparison on real players.
     */
    static boolean canUseCoinItems(ServerPlayer player) {
        return TDQConfig.allowCoinItem()
                && player.isAlive()
                && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
    }

    static int freeCoinSpace(Inventory inventory) {
        Item coin = TDQItems.TDG_COIN.get();
        int space = 0;
        space += spaceIn(inventory.items, coin);
        space += spaceIn(inventory.offhand, coin);
        return space;
    }

    private static int spaceIn(java.util.List<ItemStack> slots, Item coin) {
        int space = 0;
        int max = coin.getDefaultMaxStackSize();
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                space += max;
            } else if (stack.is(coin)) {
                space += Math.max(0, max - stack.getCount());
            }
        }
        return space;
    }

    private static long extractCoins(java.util.List<ItemStack> slots, Item coin) {
        long taken = 0L;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.is(coin)) {
                taken += stack.getCount();
                slots.set(i, ItemStack.EMPTY);
            }
        }
        return taken;
    }
}
