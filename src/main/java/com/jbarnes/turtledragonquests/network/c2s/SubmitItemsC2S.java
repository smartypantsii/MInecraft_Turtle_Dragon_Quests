package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SubmitItemsC2S(ResourceLocation questId, String objectiveId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SubmitItemsC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("submit_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitItemsC2S> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SubmitItemsC2S::questId,
            ByteBufCodecs.STRING_UTF8,
            SubmitItemsC2S::objectiveId,
            SubmitItemsC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
