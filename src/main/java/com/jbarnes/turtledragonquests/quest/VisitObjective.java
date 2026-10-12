package com.jbarnes.turtledragonquests.quest;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record VisitObjective(
        String id,
        Optional<ResourceKey<Level>> dimension,
        Optional<ResourceKey<Biome>> biome,
        Optional<BoundingBox> box,
        Optional<Integer> radius,
        Optional<BlockPos> center
) implements Objective {
    public static final MapCodec<BoundingBox> VISIT_BOX_CODEC = RecordCodecBuilder.<BoundingBox>mapCodec(i -> i.group(
            BlockPos.CODEC.fieldOf("min").forGetter(b -> new BlockPos(b.minX(), b.minY(), b.minZ())),
            BlockPos.CODEC.fieldOf("max").forGetter(b -> new BlockPos(b.maxX(), b.maxY(), b.maxZ()))
    ).apply(i, (min, max) -> new BoundingBox(
            min.getX(), min.getY(), min.getZ(),
            max.getX(), max.getY(), max.getZ())));

    public static final MapCodec<VisitObjective> MAP_CODEC = RecordCodecBuilder.<VisitObjective>mapCodec(i -> i.group(
            Objective.ID_CODEC.fieldOf("id").forGetter(VisitObjective::id),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(VisitObjective::dimension),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(VisitObjective::biome),
            VISIT_BOX_CODEC.codec().optionalFieldOf("box").forGetter(VisitObjective::box),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("radius").forGetter(VisitObjective::radius),
            BlockPos.CODEC.optionalFieldOf("center").forGetter(VisitObjective::center)
    ).apply(i, VisitObjective::new)).validate(VisitObjective::validateConstraints);

    @Override
    public ObjectiveType type() {
        return ObjectiveType.VISIT;
    }

    private static DataResult<VisitObjective> validateConstraints(VisitObjective o) {
        boolean pair = o.center().isPresent() && o.radius().isPresent();
        boolean halfPair = o.center().isPresent() ^ o.radius().isPresent();
        if (halfPair) {
            return DataResult.error(() -> "visit center and radius must be used together");
        }
        boolean has = o.dimension().isPresent() || o.biome().isPresent() || o.box().isPresent() || pair;
        return has
                ? DataResult.success(o)
                : DataResult.error(() -> "visit requires dimension, biome, box, or center+radius");
    }
}
