package com.jbarnes.turtledragonquests.currency;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record AchievementReward(ResourceLocation advancement, long tdg) {
    public static final Codec<AchievementReward> CODEC = RecordCodecBuilder.<AchievementReward>create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("advancement").forGetter(AchievementReward::advancement),
            Codec.LONG.fieldOf("tdg").forGetter(AchievementReward::tdg)
    ).apply(i, AchievementReward::new)).validate(AchievementReward::validate);

    private static DataResult<AchievementReward> validate(AchievementReward reward) {
        if (reward.tdg() < 1L) {
            return DataResult.error(() -> "tdg must be >= 1");
        }
        return DataResult.success(reward);
    }
}
