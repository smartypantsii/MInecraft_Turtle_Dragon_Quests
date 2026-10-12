package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.MapCodec;

public record ItemReward(SimpleStack stack) implements Reward {
    public static final MapCodec<ItemReward> MAP_CODEC = SimpleStack.MAP_CODEC.xmap(ItemReward::new, ItemReward::stack);

    @Override
    public RewardType type() {
        return RewardType.ITEM;
    }
}
