package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CurrencyReward(long tdg) implements Reward {
    public static final MapCodec<CurrencyReward> MAP_CODEC = RecordCodecBuilder.<CurrencyReward>mapCodec(i -> i.group(
            Codec.LONG.fieldOf("tdg").forGetter(CurrencyReward::tdg)
    ).apply(i, CurrencyReward::new)).validate(r -> r.tdg() >= 1
            ? DataResult.success(r)
            : DataResult.error(() -> "tdg must be >= 1"));

    @Override
    public RewardType type() {
        return RewardType.CURRENCY;
    }
}
