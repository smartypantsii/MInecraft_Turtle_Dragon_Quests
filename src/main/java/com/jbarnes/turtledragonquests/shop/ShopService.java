package com.jbarnes.turtledragonquests.shop;

import java.util.Optional;

import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public final class ShopService {
    private ShopService() {}

    public static int remaining(MinecraftServer server, ResourceLocation offerId) {
        Optional<ShopOffer> offer = server.registryAccess()
                .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                .getOptional(offerId);
        if (offer.isEmpty()) {
            return 0;
        }
        if (offer.get().globalStock().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return ShopWorldState.get(server).remainingOrSeed(offerId, offer.get().globalStock().get());
    }

    public static void setStock(MinecraftServer server, ResourceLocation offerId, int remaining) {
        ShopWorldState.get(server).setRemaining(offerId, remaining);
    }

    public static void afterReload(MinecraftServer server) {
        ShopWorldState.afterReload(server);
    }
}
