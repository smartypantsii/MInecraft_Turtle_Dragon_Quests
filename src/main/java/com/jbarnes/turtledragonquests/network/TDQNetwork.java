package com.jbarnes.turtledragonquests.network;

import com.jbarnes.turtledragonquests.client.ClientPayloads;
import com.jbarnes.turtledragonquests.network.c2s.AcceptQuestC2S;
import com.jbarnes.turtledragonquests.network.c2s.ClaimRewardsC2S;
import com.jbarnes.turtledragonquests.network.c2s.DepositCoinsC2S;
import com.jbarnes.turtledragonquests.network.c2s.SubmitItemsC2S;
import com.jbarnes.turtledragonquests.network.c2s.WithdrawCoinsC2S;
import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestDeltaS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressSyncS2C;
import com.jbarnes.turtledragonquests.network.s2c.WalletSyncS2C;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TDQNetwork {
    private TDQNetwork() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(WithdrawCoinsC2S.TYPE, WithdrawCoinsC2S.STREAM_CODEC, ServerPayloads::withdraw);
        registrar.playToServer(DepositCoinsC2S.TYPE, DepositCoinsC2S.STREAM_CODEC, ServerPayloads::deposit);
        registrar.playToServer(AcceptQuestC2S.TYPE, AcceptQuestC2S.STREAM_CODEC, ServerPayloads::acceptQuest);
        registrar.playToServer(ClaimRewardsC2S.TYPE, ClaimRewardsC2S.STREAM_CODEC, ServerPayloads::claim);
        registrar.playToServer(SubmitItemsC2S.TYPE, SubmitItemsC2S.STREAM_CODEC, ServerPayloads::submit);
        registrar.playToClient(WalletSyncS2C.TYPE, WalletSyncS2C.STREAM_CODEC, ClientPayloads::wallet);
        registrar.playToClient(ActionResultS2C.TYPE, ActionResultS2C.STREAM_CODEC, ClientPayloads::actionResult);
        registrar.playToClient(QuestProgressSyncS2C.TYPE, QuestProgressSyncS2C.STREAM_CODEC, ClientPayloads::questSync);
        registrar.playToClient(QuestDeltaS2C.TYPE, QuestDeltaS2C.STREAM_CODEC, ClientPayloads::questDelta);
    }
}
