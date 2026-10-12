package com.jbarnes.turtledragonquests.registry;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.quest.QuestDefinition;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class TDQDataRegistries {
    public static final ResourceKey<Registry<QuestDefinition>> QUEST_REGISTRY =
            ResourceKey.createRegistryKey(TDQ.id("quest"));

    private TDQDataRegistries() {}

    public static void newDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(QUEST_REGISTRY, QuestDefinition.CODEC, QuestDefinition.CODEC);
    }
}
