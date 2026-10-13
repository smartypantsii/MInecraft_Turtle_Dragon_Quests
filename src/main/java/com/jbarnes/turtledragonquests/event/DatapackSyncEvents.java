package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.shop.ShopService;

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
        logCounts(server);
        QuestEngine.afterReload(server);
        ShopService.afterReload(server);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        logCounts(event.getServer());
        QuestEngine.afterReload(event.getServer());
        ShopService.afterReload(event.getServer());
    }

    private static void logCounts(MinecraftServer server) {
        int quests = server.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY).size();
        int offers = server.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY).size();
        TDQ.LOGGER.info("Loaded {} quest definitions", quests);
        TDQ.LOGGER.info("Loaded {} shop offers", offers);
    }
}
