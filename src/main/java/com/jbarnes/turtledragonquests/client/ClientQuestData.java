package com.jbarnes.turtledragonquests.client;

import java.util.HashMap;
import java.util.Map;

import com.jbarnes.turtledragonquests.network.s2c.QuestDeltaS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressEntry;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressSyncS2C;

import net.minecraft.resources.ResourceLocation;

public final class ClientQuestData {
    private static Map<ResourceLocation, QuestProgressEntry> quests = Map.of();

    private ClientQuestData() {}

    public static void replace(QuestProgressSyncS2C payload) {
        quests = Map.copyOf(payload.quests());
    }

    public static void patch(QuestDeltaS2C payload) {
        Map<ResourceLocation, QuestProgressEntry> next = new HashMap<>(quests);
        QuestProgressEntry current = next.getOrDefault(payload.questId(), new QuestProgressEntry((byte) 0, Map.of()));
        Map<String, Integer> counts = new HashMap<>(current.counts());
        if (!payload.objectiveId().isEmpty()) {
            counts.put(payload.objectiveId(), payload.newCount());
        }
        next.put(payload.questId(), new QuestProgressEntry(payload.status(), counts));
        quests = Map.copyOf(next);
    }

    public static Map<ResourceLocation, QuestProgressEntry> snapshot() {
        return quests;
    }
}
