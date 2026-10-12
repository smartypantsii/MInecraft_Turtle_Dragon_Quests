package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SubmitObjective(String id, ItemIngredient items, int count) implements Objective {
    public static final MapCodec<SubmitObjective> MAP_CODEC = RecordCodecBuilder.<SubmitObjective>mapCodec(i -> i.group(
            Objective.ID_CODEC.fieldOf("id").forGetter(SubmitObjective::id),
            ItemIngredient.MAP_CODEC.forGetter(SubmitObjective::items),
            Objective.COUNT_CODEC.fieldOf("count").forGetter(SubmitObjective::count)
    ).apply(i, SubmitObjective::new));

    @Override
    public ObjectiveType type() {
        return ObjectiveType.SUBMIT;
    }
}
