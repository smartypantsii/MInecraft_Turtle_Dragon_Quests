package com.jbarnes.turtledragonquests.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ShopMenuSnapshot(
        List<ResourceLocation> offerIds,
        Map<ResourceLocation, Optional<Integer>> remaining
) {
    public ShopMenuSnapshot {
        offerIds = List.copyOf(offerIds);
        remaining = Map.copyOf(remaining);
    }

    public static ShopMenuSnapshot empty() {
        return new ShopMenuSnapshot(List.of(), Map.of());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(offerIds.size());
        for (ResourceLocation id : offerIds) {
            buf.writeResourceLocation(id);
            Optional<Integer> stock = remaining.getOrDefault(id, Optional.empty());
            buf.writeBoolean(stock.isPresent());
            if (stock.isPresent()) {
                buf.writeVarInt(stock.get());
            }
        }
    }

    public static ShopMenuSnapshot read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(n);
        Map<ResourceLocation, Optional<Integer>> remaining = new HashMap<>();
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ids.add(id);
            if (buf.readBoolean()) {
                remaining.put(id, Optional.of(buf.readVarInt()));
            } else {
                remaining.put(id, Optional.empty());
            }
        }
        return new ShopMenuSnapshot(ids, remaining);
    }

    public int indexOf(ResourceLocation offerId) {
        return offerIds.indexOf(offerId);
    }
}
