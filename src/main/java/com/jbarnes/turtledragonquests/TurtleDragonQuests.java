package com.jbarnes.turtledragonquests;

import com.jbarnes.turtledragonquests.command.TDQCommands;
import com.jbarnes.turtledragonquests.datagen.TDQDatapackProvider;
import com.jbarnes.turtledragonquests.event.DatapackSyncEvents;
import com.jbarnes.turtledragonquests.event.PlayerPersistenceEvents;
import com.jbarnes.turtledragonquests.gametest.CurrencyGameTests;
import com.jbarnes.turtledragonquests.gametest.QuestCodecGameTests;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQCreativeTabs;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.registry.TDQItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(TDQ.MOD_ID)
public final class TurtleDragonQuests {
    public TurtleDragonQuests(IEventBus modEventBus, ModContainer modContainer) {
        TDQItems.register(modEventBus);
        TDQCreativeTabs.register(modEventBus);
        TDQAttachmentTypes.register(modEventBus);
        modEventBus.addListener(TDQDataRegistries::newDatapackRegistries);
        modEventBus.addListener(TDQNetwork::register);
        modEventBus.addListener(TDQDatapackProvider::gatherData);
        modEventBus.addListener(TurtleDragonQuests::registerGameTests);
        modContainer.registerConfig(ModConfig.Type.COMMON, TDQConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(TDQCommands::register);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onClone);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onDimension);
        NeoForge.EVENT_BUS.addListener(DatapackSyncEvents::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(DatapackSyncEvents::onServerStarted);

        TDQ.LOGGER.info("Turtle Dragon Quests {} loading", modContainer.getModInfo().getVersion());
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(CurrencyGameTests.class);
        event.register(QuestCodecGameTests.class);
    }
}
