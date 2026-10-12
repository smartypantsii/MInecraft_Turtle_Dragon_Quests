package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public sealed interface Objective permits KillObjective, GatherObjective, SubmitObjective, CraftObjective, VisitObjective {
    Codec<String> ID_CODEC = Codec.STRING.validate(s ->
            s.matches("[a-z0-9_]+")
                    ? DataResult.success(s)
                    : DataResult.error(() -> "objective id must match [a-z0-9_]+: " + s));

    Codec<Integer> COUNT_CODEC = Codec.intRange(1, Integer.MAX_VALUE);

    Codec<Objective> CODEC = ObjectiveType.CODEC.dispatch("type", Objective::type, ObjectiveType::codec);

    ObjectiveType type();

    String id();
}
