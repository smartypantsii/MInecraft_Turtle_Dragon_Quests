package com.jbarnes.turtledragonquests;

import com.jbarnes.turtledragonquests.command.TDQCommands;
import com.jbarnes.turtledragonquests.datagen.TDQDatapackProvider;
import com.jbarnes.turtledragonquests.event.AchievementEvents;
import com.jbarnes.turtledragonquests.event.DatapackSyncEvents;
import com.jbarnes.turtledragonquests.event.PlayerPersistenceEvents;
import com.jbarnes.turtledragonquests.event.QuestGameplayEvents;
import com.jbarnes.turtledragonquests.gametest.AchievementGameTests;
import com.jbarnes.turtledragonquests.gametest.CurrencyGameTests;
import com.jbarnes.turtledragonquests.gametest.QuestCodecGameTests;
import com.jbarnes.turtledragonquests.gametest.QuestEngineGameTests;
import com.jbarnes.turtledragonquests.gametest.ShopGameTests;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQCreativeTabs;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.registry.TDQItems;
import com.jbarnes.turtledragonquests.registry.TDQMenus;

import net.neoforged.bus.api.EventPriority;
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
        TDQMenus.register(modEventBus);
        modEventBus.addListener(TDQDataRegistries::newDatapackRegistries);
        modEventBus.addListener(TDQNetwork::register);
        modEventBus.addListener(TDQDatapackProvider::gatherData);
        modEventBus.addListener(TurtleDragonQuests::registerGameTests);
        modContainer.registerConfig(ModConfig.Type.COMMON, TDQConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(TDQCommands::register);
        NeoForge.EVENT_BUS.addListener(AchievementEvents::onEarn);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onClone);
        NeoForge.EVENT_BUS.addListener(PlayerPersistenceEvents::onDimension);
        NeoForge.EVENT_BUS.addListener(DatapackSyncEvents::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(DatapackSyncEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, QuestGameplayEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(QuestGameplayEvents::onPickup);
        NeoForge.EVENT_BUS.addListener(QuestGameplayEvents::onDestroyItem);
        NeoForge.EVENT_BUS.addListener(QuestGameplayEvents::onContainerClose);
        NeoForge.EVENT_BUS.addListener(QuestGameplayEvents::onCraft);
        NeoForge.EVENT_BUS.addListener(QuestGameplayEvents::onTick);

        TDQ.LOGGER.info("Turtle Dragon Quests {} loading", modContainer.getModInfo().getVersion());
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(CurrencyGameTests.class);
        event.register(AchievementGameTests.class);
        event.register(QuestCodecGameTests.class);
        event.register(QuestEngineGameTests.class);
        event.register(ShopGameTests.class);
    }
}
