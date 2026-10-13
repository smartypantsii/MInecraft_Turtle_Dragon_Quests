package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenShopC2S() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenShopC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("open_shop"));

    public static final OpenShopC2S INSTANCE = new OpenShopC2S();

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopC2S> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
