package com.jbarnes.turtledragonquests.network.s2c;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WalletSyncS2C(long tdg) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WalletSyncS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("wallet_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WalletSyncS2C> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_LONG, WalletSyncS2C::tdg, WalletSyncS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
