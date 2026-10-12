package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.StringRepresentable;

public enum RewardType implements StringRepresentable {
    ITEM("item", ItemReward.MAP_CODEC),
    CURRENCY("currency", CurrencyReward.MAP_CODEC),
    UNLOCK("unlock", UnlockReward.MAP_CODEC);

    public static final Codec<RewardType> CODEC = StringRepresentable.fromEnum(RewardType::values);

    private final String name;
    private final MapCodec<? extends Reward> codec;

    RewardType(String name, MapCodec<? extends Reward> codec) {
        this.name = name;
        this.codec = codec;
    }

    public MapCodec<? extends Reward> codec() {
        return codec;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
