package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.quest.QuestEngine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class QuestGameplayEvents {
    private QuestGameplayEvents() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        QuestEngine.onKill(player, event.getEntity().getType());
    }

    public static void onPickup(ItemEntityPickupEvent.Post event) {
        ServerPlayer player = serverPlayer(event.getPlayer());
        if (player != null) {
            QuestEngine.recomputeGather(player);
        }
    }

    public static void onDestroyItem(PlayerDestroyItemEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player != null) {
            QuestEngine.recomputeGather(player);
        }
    }

    public static void onContainerClose(PlayerContainerEvent.Close event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player != null) {
            QuestEngine.recomputeGather(player);
        }
    }

    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player != null) {
            QuestEngine.onCraft(player, event.getCrafting());
            QuestEngine.recomputeGather(player);
        }
    }

    public static void onTick(PlayerTickEvent.Post event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player != null) {
            QuestEngine.tick(player);
        }
    }

    private static ServerPlayer serverPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide) {
            return serverPlayer;
        }
        return null;
    }
}
