package com.jbarnes.turtledragonquests.network;

import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.network.c2s.AcceptQuestC2S;
import com.jbarnes.turtledragonquests.network.c2s.ClaimRewardsC2S;
import com.jbarnes.turtledragonquests.network.c2s.DepositCoinsC2S;
import com.jbarnes.turtledragonquests.network.c2s.WithdrawCoinsC2S;
import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.quest.QuestEngine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloads {
    private ServerPayloads() {}

    public static void withdraw(WithdrawCoinsC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean ok = CurrencyService.tryWithdraw(player, payload.count());
            TDQNetwork.sendToPlayer(player, new ActionResultS2C(ok, Component.translatable(
                    ok ? "commands.turtle_dragon_quests.withdraw.success" : "commands.turtle_dragon_quests.withdraw.fail",
                    payload.count())));
        });
    }

    public static void deposit(DepositCoinsC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            long deposited = CurrencyService.depositAll(player);
            boolean ok = deposited > 0;
            TDQNetwork.sendToPlayer(player, new ActionResultS2C(ok, Component.translatable(
                    ok ? "commands.turtle_dragon_quests.deposit.success" : "commands.turtle_dragon_quests.deposit.fail",
                    deposited)));
        });
    }

    public static void acceptQuest(AcceptQuestC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean ok = QuestEngine.tryAccept(player, payload.questId());
            TDQNetwork.sendToPlayer(player, new ActionResultS2C(ok, Component.translatable(
                    ok ? "commands.turtle_dragon_quests.quest.accept.success" : "commands.turtle_dragon_quests.quest.accept.fail",
                    payload.questId().toString())));
        });
    }

    public static void claim(ClaimRewardsC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean ok = QuestEngine.tryClaim(player, payload.questId());
            TDQNetwork.sendToPlayer(player, new ActionResultS2C(ok, Component.translatable(
                    ok ? "commands.turtle_dragon_quests.quest.claim.success" : "commands.turtle_dragon_quests.quest.claim.fail",
                    payload.questId().toString())));
        });
    }
}
