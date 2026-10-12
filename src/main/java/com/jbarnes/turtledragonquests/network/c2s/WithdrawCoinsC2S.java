package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WithdrawCoinsC2S(int count) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WithdrawCoinsC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("withdraw_coins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WithdrawCoinsC2S> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, WithdrawCoinsC2S::count, WithdrawCoinsC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
