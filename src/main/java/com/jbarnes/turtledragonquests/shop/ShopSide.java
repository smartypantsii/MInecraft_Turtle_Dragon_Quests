package com.jbarnes.turtledragonquests.shop;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum ShopSide implements StringRepresentable {
    BUY("buy"),
    SELL("sell");

    public static final Codec<ShopSide> CODEC = StringRepresentable.fromEnum(ShopSide::values);

    private final String name;

    ShopSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
