package com.jbarnes.turtledragonquests.quest;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record QuestProgressAttachment(Map<ResourceLocation, QuestProgress> quests) {
    public static final int SCHEMA = 1;

    public QuestProgressAttachment {
        quests = Map.copyOf(quests);
    }

    public static QuestProgressAttachment empty() {
        return new QuestProgressAttachment(Map.of());
    }

    public QuestProgress get(ResourceLocation id) {
        return quests.getOrDefault(id, QuestProgress.locked());
    }

    public QuestProgressAttachment with(ResourceLocation id, QuestProgress progress) {
        Map<ResourceLocation, QuestProgress> next = new HashMap<>(quests);
        next.put(id, progress);
        return new QuestProgressAttachment(next);
    }

    public QuestProgressAttachment without(ResourceLocation id) {
        if (!quests.containsKey(id)) {
            return this;
        }
        Map<ResourceLocation, QuestProgress> next = new HashMap<>(quests);
        next.remove(id);
        return new QuestProgressAttachment(next);
    }

    public static final Codec<QuestProgressAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", SCHEMA).forGetter(a -> SCHEMA),
            Codec.unboundedMap(ResourceLocation.CODEC, QuestProgress.CODEC)
                    .optionalFieldOf("quests", Map.of())
                    .forGetter(QuestProgressAttachment::quests)
    ).apply(i, (schema, quests) -> {
        if (schema != SCHEMA) {
            throw new IllegalArgumentException("Unsupported quest progress schema " + schema);
        }
        return new QuestProgressAttachment(quests);
    }));
}
