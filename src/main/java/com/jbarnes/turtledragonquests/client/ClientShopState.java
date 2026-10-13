package com.jbarnes.turtledragonquests.client;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public final class ClientShopState {
    private static Map<ResourceLocation, Optional<Integer>> remaining = Map.of();

    private ClientShopState() {}

    public static void replace(Map<ResourceLocation, Optional<Integer>> next) {
        remaining = Map.copyOf(next);
    }

    public static boolean contains(ResourceLocation offerId) {
        return remaining.containsKey(offerId);
    }

    public static Optional<Integer> remaining(ResourceLocation offerId) {
        return remaining.getOrDefault(offerId, Optional.empty());
    }

    public static Map<ResourceLocation, Optional<Integer>> all() {
        return remaining;
    }
}
