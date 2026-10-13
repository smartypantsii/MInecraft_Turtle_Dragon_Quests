package com.jbarnes.turtledragonquests.client;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.network.c2s.OpenQuestLogC2S;
import com.jbarnes.turtledragonquests.network.c2s.OpenShopC2S;
import com.jbarnes.turtledragonquests.registry.TDQMenus;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = TDQ.MOD_ID, dist = Dist.CLIENT)
public final class TurtleDragonQuestsClient {
    public static final KeyMapping OPEN_QUEST_LOG = new KeyMapping(
            "key.turtle_dragon_quests.quest_log",
            InputConstants.KEY_J,
            "key.categories.turtle_dragon_quests");

    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.turtle_dragon_quests.shop",
            InputConstants.KEY_K,
            "key.categories.turtle_dragon_quests");

    public TurtleDragonQuestsClient(IEventBus modEventBus) {
        modEventBus.addListener(TurtleDragonQuestsClient::registerKeys);
        modEventBus.addListener(TurtleDragonQuestsClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(TurtleDragonQuestsClient::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_QUEST_LOG);
        event.register(OPEN_SHOP);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TDQMenus.SHOP.get(), ShopScreen::new);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (OPEN_QUEST_LOG.consumeClick()) {
            PacketDistributor.sendToServer(OpenQuestLogC2S.INSTANCE);
        }
        while (OPEN_SHOP.consumeClick()) {
            PacketDistributor.sendToServer(OpenShopC2S.INSTANCE);
        }
    }
}
