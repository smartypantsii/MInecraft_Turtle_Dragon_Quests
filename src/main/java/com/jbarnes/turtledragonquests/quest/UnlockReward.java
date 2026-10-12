package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record UnlockReward(ResourceLocation questId) implements Reward {
    public static final MapCodec<UnlockReward> MAP_CODEC = RecordCodecBuilder.<UnlockReward>mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("quest").forGetter(UnlockReward::questId)
    ).apply(i, UnlockReward::new));

    @Override
    public RewardType type() {
        return RewardType.UNLOCK;
    }
}
