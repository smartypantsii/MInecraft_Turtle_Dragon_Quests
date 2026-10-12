package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CraftObjective(String id, ItemIngredient output, int count) implements Objective {
    public static final MapCodec<CraftObjective> MAP_CODEC = RecordCodecBuilder.<CraftObjective>mapCodec(i -> i.group(
            Objective.ID_CODEC.fieldOf("id").forGetter(CraftObjective::id),
            ItemIngredient.MAP_CODEC.forGetter(CraftObjective::output),
            Objective.COUNT_CODEC.fieldOf("count").forGetter(CraftObjective::count)
    ).apply(i, CraftObjective::new));

    @Override
    public ObjectiveType type() {
        return ObjectiveType.CRAFT;
    }
}
