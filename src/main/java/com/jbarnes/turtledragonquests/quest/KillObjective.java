package com.jbarnes.turtledragonquests.quest;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record KillObjective(
        String id,
        Optional<ResourceLocation> entity,
        Optional<TagKey<EntityType<?>>> entityTag,
        int count
) implements Objective {
    private static final Codec<TagKey<EntityType<?>>> ENTITY_TAG_CODEC = ResourceLocation.CODEC.xmap(
            id -> TagKey.create(Registries.ENTITY_TYPE, id),
            TagKey::location);

    public static final MapCodec<KillObjective> MAP_CODEC = RecordCodecBuilder.<KillObjective>mapCodec(i -> i.group(
            Objective.ID_CODEC.fieldOf("id").forGetter(KillObjective::id),
            ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(KillObjective::entity),
            ENTITY_TAG_CODEC.optionalFieldOf("entity_tag").forGetter(KillObjective::entityTag),
            Objective.COUNT_CODEC.fieldOf("count").forGetter(KillObjective::count)
    ).apply(i, KillObjective::new)).validate(KillObjective::validateXor);

    @Override
    public ObjectiveType type() {
        return ObjectiveType.KILL;
    }

    private static DataResult<KillObjective> validateXor(KillObjective o) {
        boolean hasEntity = o.entity().isPresent();
        boolean hasTag = o.entityTag().isPresent();
        return hasEntity == hasTag
                ? DataResult.error(() -> "kill objective requires exactly one of entity or entity_tag")
                : DataResult.success(o);
    }
}
