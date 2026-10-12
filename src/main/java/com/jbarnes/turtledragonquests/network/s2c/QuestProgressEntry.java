package com.jbarnes.turtledragonquests.network.s2c;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record QuestProgressEntry(byte status, Map<String, Integer> counts) {
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            QuestProgressEntry::status,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            QuestProgressEntry::counts,
            QuestProgressEntry::new);
}
