package com.jbarnes.turtledragonquests.network.s2c;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenQuestLogS2C() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenQuestLogS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("open_quest_log_s2c"));

    public static final OpenQuestLogS2C INSTANCE = new OpenQuestLogS2C();

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestLogS2C> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
