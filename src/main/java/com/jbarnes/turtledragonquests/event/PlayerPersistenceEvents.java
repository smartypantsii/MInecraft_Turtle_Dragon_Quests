package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerPersistenceEvents {
    private PlayerPersistenceEvents() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CurrencyService.grantStarterIfNeeded(player);
            QuestEngine.recomputeGraph(player);
            CurrencyService.sync(player);
            QuestEngine.sync(player);
        }
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CurrencyService.sync(player);
            QuestEngine.sync(player);
        }
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CurrencyService.sync(player);
            QuestEngine.sync(player);
        }
    }
}
