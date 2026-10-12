package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.StringRepresentable;

public enum ObjectiveType implements StringRepresentable {
    KILL("kill", KillObjective.MAP_CODEC),
    GATHER("gather", GatherObjective.MAP_CODEC),
    SUBMIT("submit", SubmitObjective.MAP_CODEC),
    CRAFT("craft", CraftObjective.MAP_CODEC),
    VISIT("visit", VisitObjective.MAP_CODEC);

    public static final Codec<ObjectiveType> CODEC = StringRepresentable.fromEnum(ObjectiveType::values);

    private final String name;
    private final MapCodec<? extends Objective> codec;

    ObjectiveType(String name, MapCodec<? extends Objective> codec) {
        this.name = name;
        this.codec = codec;
    }

    public MapCodec<? extends Objective> codec() {
        return codec;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
