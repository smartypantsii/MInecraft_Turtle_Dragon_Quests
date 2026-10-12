package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.quest.QuestEngine;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class QuestGameplayEvents {
    private QuestGameplayEvents() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        QuestEngine.onKill(player, event.getEntity().getType());
    }
}
