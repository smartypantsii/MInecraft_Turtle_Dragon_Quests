package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class DatapackSyncEvents {
    private DatapackSyncEvents() {}

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer joining = event.getPlayer();
        if (joining != null) {
            QuestEngine.recomputeGraph(joining);
            QuestEngine.recomputeGather(joining);
            QuestEngine.recomputeVisit(joining);
            QuestEngine.sync(joining);
            CurrencyService.sync(joining);
            return;
        }
        MinecraftServer server = event.getPlayerList().getServer();
        logQuestCount(server);
        QuestEngine.afterReload(server);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        logQuestCount(event.getServer());
        QuestEngine.afterReload(event.getServer());
    }

    private static void logQuestCount(MinecraftServer server) {
        int n = server.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY).size();
        TDQ.LOGGER.info("Loaded {} quest definitions", n);
    }
}
