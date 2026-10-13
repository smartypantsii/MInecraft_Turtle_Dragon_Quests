package com.jbarnes.turtledragonquests.shop;

import java.util.HashMap;
import java.util.Map;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ShopWorldState extends SavedData {
    public static final String FILE_ID = "turtle_dragon_quests_shop";
    public static final int SCHEMA = 1;

    public static final Factory<ShopWorldState> FACTORY = new Factory<>(
            ShopWorldState::new,
            ShopWorldState::load);

    private final Map<String, Integer> remaining = new HashMap<>();
    private CompoundTag leftoverNbt;
    private boolean unreadable;

    public ShopWorldState() {}

    public static ShopWorldState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static ShopWorldState load(CompoundTag tag, HolderLookup.Provider registries) {
        ShopWorldState state = new ShopWorldState();
        int schema = tag.contains("schema", Tag.TAG_INT) ? tag.getInt("schema") : SCHEMA;
        if (schema > SCHEMA) {
            TDQ.LOGGER.error("Unsupported shop stock schema {} — leaving leftover NBT", schema);
            state.leftoverNbt = tag.copy();
            state.unreadable = true;
            return state;
        }
        CompoundTag stock = tag.getCompound("remaining");
        for (String key : stock.getAllKeys()) {
            state.remaining.put(key, stock.getInt(key));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (unreadable && leftoverNbt != null) {
            return leftoverNbt.copy();
        }
        tag.putInt("schema", SCHEMA);
        CompoundTag stock = new CompoundTag();
        remaining.forEach(stock::putInt);
        tag.put("remaining", stock);
        return tag;
    }

    public boolean unreadable() {
        return unreadable;
    }

    public int remainingOrSeed(ResourceLocation offerId, int jsonStock) {
        if (unreadable) {
            return jsonStock;
        }
        String key = offerId.toString();
        Integer current = remaining.get(key);
        if (current != null) {
            return current;
        }
        remaining.put(key, jsonStock);
        setDirty();
        return jsonStock;
    }

    public void setRemaining(ResourceLocation offerId, int n) {
        if (unreadable) {
            TDQ.LOGGER.error("Refusing to write shop stock; unsupported schema on {}", FILE_ID);
            return;
        }
        remaining.put(offerId.toString(), n);
        setDirty();
    }

    public void forget(ResourceLocation offerId) {
        if (unreadable) {
            return;
        }
        if (remaining.remove(offerId.toString()) != null) {
            setDirty();
        }
    }

    public void applyReload(Registry<ShopOffer> registry) {
        if (unreadable) {
            return;
        }
        boolean dirty = false;
        for (var entry : registry.entrySet()) {
            String key = entry.getKey().location().toString();
            ShopOffer offer = entry.getValue();
            if (offer.globalStock().isEmpty()) {
                if (remaining.remove(key) != null) {
                    dirty = true;
                }
                continue;
            }
            Integer current = remaining.get(key);
            if (current == null || current == 0) {
                continue;
            }
            int jsonStock = offer.globalStock().get();
            if (current > jsonStock) {
                remaining.put(key, jsonStock);
                dirty = true;
            }
        }
        if (dirty) {
            setDirty();
        }
    }

    public static void afterReload(MinecraftServer server) {
        get(server).applyReload(server.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY));
    }
}
