package com.jbarnes.turtledragonquests.network.s2c;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShopStateSyncS2C(Map<ResourceLocation, Optional<Integer>> remaining)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShopStateSyncS2C> TYPE =
            new CustomPacketPayload.Type<>(TDQ.id("shop_state_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopStateSyncS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(
                            HashMap::new,
                            ResourceLocation.STREAM_CODEC,
                            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT)),
                    ShopStateSyncS2C::remaining,
                    ShopStateSyncS2C::new);

    public ShopStateSyncS2C {
        remaining = Map.copyOf(remaining);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
