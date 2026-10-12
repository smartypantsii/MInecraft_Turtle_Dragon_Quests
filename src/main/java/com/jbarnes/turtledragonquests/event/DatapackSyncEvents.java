package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class DatapackSyncEvents {
    private DatapackSyncEvents() {}

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        logQuestCount(event.getPlayerList().getServer());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        logQuestCount(event.getServer());
    }

    private static void logQuestCount(MinecraftServer server) {
        int n = server.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY).size();
        TDQ.LOGGER.info("Loaded {} quest definitions", n);
    }
}
