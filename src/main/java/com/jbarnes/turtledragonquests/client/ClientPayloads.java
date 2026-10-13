package com.jbarnes.turtledragonquests.client;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.network.s2c.OpenQuestLogS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestDeltaS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressSyncS2C;
import com.jbarnes.turtledragonquests.network.s2c.ShopStateSyncS2C;
import com.jbarnes.turtledragonquests.network.s2c.WalletSyncS2C;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloads {
    private ClientPayloads() {}

    public static void wallet(WalletSyncS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientWallet.set(payload.tdg()));
    }

    public static void shopState(ShopStateSyncS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientShopState.replace(payload.remaining()));
    }

    public static void questSync(QuestProgressSyncS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientQuestData.replace(payload));
    }

    public static void questDelta(QuestDeltaS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientQuestData.patch(payload));
    }

    public static void actionResult(ActionResultS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player != null) {
                player.displayClientMessage(payload.reason(), false);
            }
        });
    }

    public static void openQuestLog(OpenQuestLogS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }
            try {
                Class.forName("com.jbarnes.turtledragonquests.client.ClientQuestLog")
                        .getMethod("open")
                        .invoke(null);
            } catch (ReflectiveOperationException e) {
                TDQ.LOGGER.error("Failed to open quest log", e);
            }
        });
    }
}
