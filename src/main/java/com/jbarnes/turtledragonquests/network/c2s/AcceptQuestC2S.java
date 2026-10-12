package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AcceptQuestC2S(ResourceLocation questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AcceptQuestC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("accept_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AcceptQuestC2S> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            AcceptQuestC2S::questId,
            AcceptQuestC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
