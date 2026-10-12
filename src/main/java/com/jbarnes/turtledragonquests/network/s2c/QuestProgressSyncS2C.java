package com.jbarnes.turtledragonquests.network.s2c;

import java.util.HashMap;
import java.util.Map;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestProgressSyncS2C(Map<ResourceLocation, QuestProgressEntry> quests) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestProgressSyncS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("quest_progress_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressSyncS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, QuestProgressEntry.STREAM_CODEC),
            QuestProgressSyncS2C::quests,
            QuestProgressSyncS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
