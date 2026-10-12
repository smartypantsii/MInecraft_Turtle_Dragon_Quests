package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClaimRewardsC2S(ResourceLocation questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimRewardsC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("claim_rewards"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimRewardsC2S> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ClaimRewardsC2S::questId,
            ClaimRewardsC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
