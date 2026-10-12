package com.jbarnes.turtledragonquests.quest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public record QuestDefinition(
        Component title,
        Component description,
        Optional<ResourceLocation> icon,
        List<ResourceLocation> prerequisites,
        boolean autoAccept,
        Optional<String> group,
        List<Objective> objectives,
        List<Reward> rewards
) {
    public QuestDefinition {
        prerequisites = List.copyOf(prerequisites);
        objectives = List.copyOf(objectives);
        rewards = List.copyOf(rewards);
        group = group.filter(s -> !s.isBlank());
    }

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.<QuestDefinition>create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(QuestDefinition::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(QuestDefinition::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(QuestDefinition::icon),
            ResourceLocation.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(QuestDefinition::prerequisites),
            Codec.BOOL.optionalFieldOf("auto_accept", false).forGetter(QuestDefinition::autoAccept),
            Codec.STRING.optionalFieldOf("group").forGetter(QuestDefinition::group),
            Objective.CODEC.listOf().fieldOf("objectives").forGetter(QuestDefinition::objectives),
            Reward.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(QuestDefinition::rewards)
    ).apply(i, QuestDefinition::new)).validate(QuestDefinition::validate);

    private static DataResult<QuestDefinition> validate(QuestDefinition def) {
        if (def.objectives().isEmpty()) {
            return DataResult.error(() -> "quest objectives must be non-empty");
        }
        Set<String> ids = new HashSet<>();
        for (Objective objective : def.objectives()) {
            if (!ids.add(objective.id())) {
                return DataResult.error(() -> "duplicate objective id " + objective.id());
            }
        }
        return DataResult.success(def);
    }
}
