package com.jbarnes.turtledragonquests.network.s2c;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActionResultS2C(boolean ok, Component reason) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionResultS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("action_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActionResultS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ActionResultS2C::ok,
                    ComponentSerialization.STREAM_CODEC,
                    ActionResultS2C::reason,
                    ActionResultS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
