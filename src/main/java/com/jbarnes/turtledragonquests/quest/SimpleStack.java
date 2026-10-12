package com.jbarnes.turtledragonquests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

public record SimpleStack(ResourceLocation item, int count) {
    public static final MapCodec<SimpleStack> MAP_CODEC = RecordCodecBuilder.<SimpleStack>mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(SimpleStack::item),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(SimpleStack::count)
    ).apply(i, SimpleStack::new));

    public static final Codec<SimpleStack> CODEC = MAP_CODEC.codec();

    public ItemStack toStack() {
        var resolved = BuiltInRegistries.ITEM.getOptional(item).orElse(Items.AIR);
        if (resolved == Items.AIR) {
            throw new IllegalStateException("Unknown item " + item);
        }
        return new ItemStack(resolved, count);
    }
}
