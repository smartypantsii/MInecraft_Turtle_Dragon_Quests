package com.jbarnes.turtledragonquests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.ResourceLocation;

public final class TDQ {
    public static final String MOD_ID = "turtle_dragon_quests";
    public static final Logger LOGGER = LoggerFactory.getLogger("TDQ");

    private TDQ() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
