package com.jbarnes.turtledragonquests.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.network.s2c.ShopStateSyncS2C;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.quest.QuestStatus;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.currency.CurrencyService;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class ShopService {
    private ShopService() {}

    public static boolean canAct(ServerPlayer player) {
        return player.isAlive() && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
    }

    public static boolean unlocked(ServerPlayer player, ShopOffer offer) {
        for (ResourceLocation questId : offer.requiredQuests()) {
            if (QuestEngine.status(player, questId) != QuestStatus.CLAIMED) {
                return false;
            }
        }
        return true;
    }

    public static int remaining(MinecraftServer server, ResourceLocation offerId) {
        Optional<ShopOffer> offer = server.registryAccess()
                .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                .getOptional(offerId);
        if (offer.isEmpty()) {
            return 0;
        }
        if (offer.get().globalStock().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return ShopWorldState.get(server).remainingOrSeed(offerId, offer.get().globalStock().get());
    }

    public static Optional<Integer> remainingOptional(MinecraftServer server, ResourceLocation offerId) {
        int remaining = remaining(server, offerId);
        if (remaining == Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(remaining);
    }

    public static boolean tryConsume(MinecraftServer server, ResourceLocation offerId, int units) {
        if (units < 1) {
            return false;
        }
        int remaining = remaining(server, offerId);
        if (remaining == Integer.MAX_VALUE) {
            return true;
        }
        if (remaining < units) {
            return false;
        }
        ShopWorldState.get(server).setRemaining(offerId, remaining - units);
        return true;
    }

    public static void setStock(MinecraftServer server, ResourceLocation offerId, int remaining) {
        ShopWorldState.get(server).setRemaining(offerId, remaining);
        syncAll(server);
    }

    public static void afterReload(MinecraftServer server) {
        ShopWorldState.afterReload(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ShopMenu) {
                player.closeContainer();
            }
            sync(player);
        }
    }

    public static void open(ServerPlayer player) {
        if (!canAct(player)) {
            TDQNetwork.sendToPlayer(player, new ActionResultS2C(
                    false, Component.translatable("screen.turtle_dragon_quests.shop.reject")));
            return;
        }
        ShopMenuSnapshot snapshot = snapshot(player);
        Component title = Component.translatable("screen.turtle_dragon_quests.shop");
        player.openMenu(
                new SimpleMenuProvider((id, inv, p) -> new ShopMenu(id, inv, snapshot), title),
                snapshot::write);
        CurrencyService.sync(player);
        sync(player);
    }

    public static ShopMenu openForTest(ServerPlayer player) {
        return openForTest(player, snapshot(player));
    }

    public static ShopMenu openForTest(ServerPlayer player, ShopMenuSnapshot snapshot) {
        ShopMenu menu = new ShopMenu(1, player.getInventory(), snapshot);
        player.containerMenu = menu;
        return menu;
    }

    public static ShopMenuSnapshot snapshot(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Registry<ShopOffer> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY);
        List<ResourceLocation> ids = new ArrayList<>();
        Map<ResourceLocation, Optional<Integer>> remaining = new HashMap<>();
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (isTestFixture(id)) {
                continue;
            }
            ShopOffer offer = entry.getValue();
            if (!unlocked(player, offer)) {
                continue;
            }
            ids.add(id);
            remaining.put(id, remainingOptional(server, id));
        }
        return new ShopMenuSnapshot(ids, remaining);
    }

    public static Map<ResourceLocation, Optional<Integer>> remainingMap(MinecraftServer server) {
        Registry<ShopOffer> registry = server.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY);
        Map<ResourceLocation, Optional<Integer>> remaining = new HashMap<>();
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (isTestFixture(id)) {
                continue;
            }
            remaining.put(id, remainingOptional(server, id));
        }
        return remaining;
    }

    static boolean isTestFixture(ResourceLocation id) {
        return id.getPath().startsWith("fixtures/");
    }

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TDQNetwork.sendToPlayer(player, new ShopStateSyncS2C(remainingMap(server)));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    static boolean canFit(Inventory inventory, List<ItemStack> gifts) {
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack stack : inventory.items) {
            slots.add(stack.copy());
        }
        for (ItemStack stack : inventory.offhand) {
            slots.add(stack.copy());
        }
        for (ItemStack gift : gifts) {
            int left = gift.getCount();
            Item item = gift.getItem();
            int max = item.getDefaultMaxStackSize();
            for (int i = 0; i < slots.size() && left > 0; i++) {
                ItemStack slot = slots.get(i);
                if (slot.isEmpty()) {
                    int n = Math.min(max, left);
                    slots.set(i, new ItemStack(item, n));
                    left -= n;
                } else if (ItemStack.isSameItemSameComponents(slot, gift) && slot.getCount() < max) {
                    int n = Math.min(max - slot.getCount(), left);
                    slot.grow(n);
                    left -= n;
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }
}
