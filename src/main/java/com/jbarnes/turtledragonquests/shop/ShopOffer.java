package com.jbarnes.turtledragonquests.shop;

import java.util.List;
import java.util.Optional;

import com.jbarnes.turtledragonquests.quest.SimpleStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record ShopOffer(
        ShopSide side,
        SimpleStack stack,
        long unitPrice,
        Optional<Integer> globalStock,
        List<ResourceLocation> requiredQuests,
        Optional<String> category
) {
    public ShopOffer {
        requiredQuests = List.copyOf(requiredQuests);
        category = category.filter(s -> !s.isBlank());
    }

    public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.<ShopOffer>create(i -> i.group(
            ShopSide.CODEC.fieldOf("side").forGetter(ShopOffer::side),
            SimpleStack.MAP_CODEC.forGetter(ShopOffer::stack),
            Codec.LONG.fieldOf("unit_price").forGetter(ShopOffer::unitPrice),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("global_stock").forGetter(ShopOffer::globalStock),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_quests", List.of()).forGetter(ShopOffer::requiredQuests),
            Codec.STRING.optionalFieldOf("category").forGetter(ShopOffer::category)
    ).apply(i, ShopOffer::new)).validate(ShopOffer::validate);

    private static DataResult<ShopOffer> validate(ShopOffer offer) {
        if (offer.unitPrice() < 0L) {
            return DataResult.error(() -> "unit_price must be >= 0");
        }
        try {
            Math.multiplyExact(offer.unitPrice(), 64L);
        } catch (ArithmeticException e) {
            return DataResult.error(() -> "unit_price overflows unit_price * 64");
        }
        return DataResult.success(offer);
    }
}
