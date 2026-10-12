package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DepositCoinsC2S() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DepositCoinsC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("deposit_coins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositCoinsC2S> STREAM_CODEC =
            StreamCodec.unit(new DepositCoinsC2S());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
