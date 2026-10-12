package com.jbarnes.turtledragonquests.client;

import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.network.s2c.WalletSyncS2C;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloads {
    private ClientPayloads() {}

    public static void wallet(WalletSyncS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientWallet.set(payload.tdg()));
    }

    public static void actionResult(ActionResultS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player != null) {
                player.displayClientMessage(payload.reason(), false);
            }
        });
    }
}
