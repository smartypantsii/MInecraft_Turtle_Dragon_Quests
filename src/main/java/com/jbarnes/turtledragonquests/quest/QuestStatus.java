package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum QuestStatus implements StringRepresentable {
    LOCKED,
    AVAILABLE,
    ACTIVE,
    COMPLETED,
    CLAIMED;

    public static final Codec<QuestStatus> CODEC = StringRepresentable.fromEnum(QuestStatus::values);

    public byte wire() {
        return (byte) ordinal();
    }

    public static QuestStatus fromWire(byte value) {
        int i = Byte.toUnsignedInt(value);
        QuestStatus[] values = values();
        return i < values.length ? values[i] : LOCKED;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
