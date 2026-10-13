package com.jbarnes.turtledragonquests.currency;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.TDQConfig;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AchievementRewards {
    private AchievementRewards() {}

    public static void onEarn(ServerPlayer player, ResourceLocation advancementId) {
        tryPay(player, advancementId);
    }

    public static void backfill(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Registry<AchievementReward> registry = player.registryAccess()
                .registryOrThrow(TDQDataRegistries.ACHIEVEMENT_REWARD_REGISTRY);
        for (AchievementReward reward : registry) {
            ResourceLocation id = reward.advancement();
            if (CurrencyService.wallet(player).paidAchievements().contains(id)) {
                continue;
            }
            AdvancementHolder holder = server.getAdvancements().get(id);
            if (holder == null) {
                continue;
            }
            if (!player.getAdvancements().getOrStartProgress(holder).isDone()) {
                continue;
            }
            tryPay(player, id);
        }
    }

    public static void backfillAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            backfill(player);
        }
    }

    static Optional<AchievementReward> mapping(RegistryAccess access, ResourceLocation advancementId) {
        Registry<AchievementReward> registry = access.registryOrThrow(TDQDataRegistries.ACHIEVEMENT_REWARD_REGISTRY);
        for (AchievementReward reward : registry) {
            if (reward.advancement().equals(advancementId)) {
                return Optional.of(reward);
            }
        }
        return Optional.empty();
    }

    private static void tryPay(ServerPlayer player, ResourceLocation advancementId) {
        WalletAttachment current = CurrencyService.wallet(player);
        if (current.paidAchievements().contains(advancementId)) {
            return;
        }
        Optional<AchievementReward> mapping = mapping(player.registryAccess(), advancementId);
        if (mapping.isEmpty()) {
            return;
        }
        long next = current.tdg();
        long cap = TDQConfig.maxCoins();
        long room = cap - next;
        if (room > 0L) {
            next += Math.min(mapping.get().tdg(), room);
        }
        Set<ResourceLocation> paid = new HashSet<>(current.paidAchievements());
        paid.add(advancementId);
        player.setData(TDQAttachmentTypes.WALLET, current.withTdg(next).withPaidAchievements(paid));
        CurrencyService.sync(player);
        TDQ.LOGGER.debug(
                "Paid {} TDG to {} for advancement {}",
                mapping.get().tdg(),
                player.getGameProfile().getName(),
                advancementId);
    }
}
