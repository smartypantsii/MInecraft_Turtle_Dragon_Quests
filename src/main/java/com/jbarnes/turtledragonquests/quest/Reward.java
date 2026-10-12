package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;

public sealed interface Reward permits ItemReward, CurrencyReward, UnlockReward {
    Codec<Reward> CODEC = RewardType.CODEC.dispatch("type", Reward::type, RewardType::codec);

    RewardType type();
}
