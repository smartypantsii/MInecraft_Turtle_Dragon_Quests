package com.jbarnes.turtledragonquests;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

public final class TDQ {
    public static final String MOD_ID = "turtle_dragon_quests";
    public static final Logger LOGGER = LogUtils.getLogger();

    private TDQ() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
