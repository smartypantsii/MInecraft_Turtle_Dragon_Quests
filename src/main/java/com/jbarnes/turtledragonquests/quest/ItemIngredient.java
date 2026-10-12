package com.jbarnes.turtledragonquests.quest;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record ItemIngredient(Optional<ResourceLocation> item, Optional<TagKey<Item>> tag) {
    public static final MapCodec<ItemIngredient> MAP_CODEC = RecordCodecBuilder.<ItemIngredient>mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemIngredient::item),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ItemIngredient::tag)
    ).apply(i, ItemIngredient::new)).validate(ItemIngredient::validateXor);

    public static final Codec<ItemIngredient> CODEC = MAP_CODEC.codec();

    public static ItemIngredient ofItem(ResourceLocation item) {
        return new ItemIngredient(Optional.of(item), Optional.empty());
    }

    public static ItemIngredient ofTag(TagKey<Item> tag) {
        return new ItemIngredient(Optional.empty(), Optional.of(tag));
    }

    private static DataResult<ItemIngredient> validateXor(ItemIngredient ing) {
        boolean hasItem = ing.item().isPresent();
        boolean hasTag = ing.tag().isPresent();
        return hasItem == hasTag
                ? DataResult.error(() -> "item_ingredient requires exactly one of item or tag")
                : DataResult.success(ing);
    }
}
