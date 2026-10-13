package com.jbarnes.turtledragonquests.event;

import com.jbarnes.turtledragonquests.currency.AchievementRewards;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

public final class AchievementEvents {
    private AchievementEvents() {}

    public static void onEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AchievementRewards.onEarn(player, event.getAdvancement().id());
        }
    }
}
