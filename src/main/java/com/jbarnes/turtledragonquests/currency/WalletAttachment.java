package com.jbarnes.turtledragonquests.currency;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record WalletAttachment(long tdg, boolean starterGranted, Set<ResourceLocation> paidAchievements) {
    public static final int SCHEMA = 1;

    public WalletAttachment {
        if (tdg < 0) {
            throw new IllegalArgumentException("tdg must be >= 0");
        }
        paidAchievements = Set.copyOf(paidAchievements);
    }

    public static WalletAttachment empty() {
        return new WalletAttachment(0L, false, Set.of());
    }

    public WalletAttachment withTdg(long tdg) {
        return new WalletAttachment(tdg, starterGranted, paidAchievements);
    }

    public WalletAttachment withStarterGranted() {
        return new WalletAttachment(tdg, true, paidAchievements);
    }

    public WalletAttachment withPaidAchievements(Set<ResourceLocation> paid) {
        return new WalletAttachment(tdg, starterGranted, paid);
    }

    public static final Codec<WalletAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema", SCHEMA).forGetter(w -> SCHEMA),
            Codec.LONG.fieldOf("tdg").forGetter(WalletAttachment::tdg),
            Codec.BOOL.optionalFieldOf("starter_granted", false).forGetter(WalletAttachment::starterGranted),
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("paid_achievements", List.of())
                    .forGetter(w -> List.copyOf(w.paidAchievements()))
    ).apply(instance, (schema, tdg, starter, paid) -> {
        if (schema != SCHEMA) {
            throw new IllegalArgumentException("Unsupported wallet schema " + schema);
        }
        return new WalletAttachment(tdg, starter, new HashSet<>(paid));
    }));
}
