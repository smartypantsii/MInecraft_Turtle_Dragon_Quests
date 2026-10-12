package com.jbarnes.turtledragonquests.quest;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record QuestProgress(
        QuestStatus status,
        Map<String, Integer> objectiveCounts,
        long acceptedGameTime,
        long completedGameTime
) {
    public QuestProgress {
        objectiveCounts = Map.copyOf(objectiveCounts);
    }

    public static QuestProgress locked() {
        return new QuestProgress(QuestStatus.LOCKED, Map.of(), 0L, 0L);
    }

    public QuestProgress withStatus(QuestStatus status) {
        return new QuestProgress(status, objectiveCounts, acceptedGameTime, completedGameTime);
    }

    public QuestProgress withCount(String objectiveId, int count) {
        Map<String, Integer> next = new HashMap<>(objectiveCounts);
        next.put(objectiveId, count);
        return new QuestProgress(status, next, acceptedGameTime, completedGameTime);
    }

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            QuestStatus.CODEC.fieldOf("status").forGetter(QuestProgress::status),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("objective_counts", Map.of())
                    .forGetter(QuestProgress::objectiveCounts),
            Codec.LONG.optionalFieldOf("accepted_game_time", 0L).forGetter(QuestProgress::acceptedGameTime),
            Codec.LONG.optionalFieldOf("completed_game_time", 0L).forGetter(QuestProgress::completedGameTime)
    ).apply(i, QuestProgress::new));
}
