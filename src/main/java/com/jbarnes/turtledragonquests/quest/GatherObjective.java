package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GatherObjective(String id, ItemIngredient items, int count) implements Objective {
    public static final MapCodec<GatherObjective> MAP_CODEC = RecordCodecBuilder.<GatherObjective>mapCodec(i -> i.group(
            Objective.ID_CODEC.fieldOf("id").forGetter(GatherObjective::id),
            ItemIngredient.MAP_CODEC.forGetter(GatherObjective::items),
            Objective.COUNT_CODEC.fieldOf("count").forGetter(GatherObjective::count)
    ).apply(i, GatherObjective::new));

    @Override
    public ObjectiveType type() {
        return ObjectiveType.GATHER;
    }
}
