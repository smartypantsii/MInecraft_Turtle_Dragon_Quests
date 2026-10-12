package com.jbarnes.turtledragonquests.network.s2c;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestDeltaS2C(ResourceLocation questId, byte status, String objectiveId, int newCount)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestDeltaS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("quest_delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestDeltaS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            QuestDeltaS2C::questId,
            ByteBufCodecs.BYTE,
            QuestDeltaS2C::status,
            ByteBufCodecs.STRING_UTF8,
            QuestDeltaS2C::objectiveId,
            ByteBufCodecs.VAR_INT,
            QuestDeltaS2C::newCount,
            QuestDeltaS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
