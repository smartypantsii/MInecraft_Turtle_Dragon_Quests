package com.jbarnes.turtledragonquests.network.c2s;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenQuestLogC2S() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenQuestLogC2S> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("open_quest_log"));

    public static final OpenQuestLogC2S INSTANCE = new OpenQuestLogC2S();

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestLogC2S> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
