package com.jbarnes.turtledragonquests.quest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.network.s2c.QuestDeltaS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressEntry;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressSyncS2C;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class QuestEngine {
    private QuestEngine() {}

    public static QuestProgressAttachment attachment(ServerPlayer player) {
        return player.getData(TDQAttachmentTypes.QUEST_PROGRESS);
    }

    public static QuestStatus status(ServerPlayer player, ResourceLocation id) {
        return attachment(player).get(id).status();
    }

    public static Optional<QuestDefinition> definition(ServerPlayer player, ResourceLocation id) {
        return player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY).getOptional(id);
    }

    public static boolean tryAccept(ServerPlayer player, ResourceLocation id) {
        if (!canAct(player)) {
            return false;
        }
        Optional<QuestDefinition> def = definition(player, id);
        if (def.isEmpty()) {
            return false;
        }
        if (status(player, id) != QuestStatus.AVAILABLE) {
            return false;
        }
        activate(player, id, def.get());
        recomputeGraph(player);
        sync(player);
        return true;
    }

    public static boolean tryClaim(ServerPlayer player, ResourceLocation id) {
        if (!canAct(player)) {
            return false;
        }
        Optional<QuestDefinition> defOpt = definition(player, id);
        if (defOpt.isEmpty() || status(player, id) != QuestStatus.COMPLETED) {
            return false;
        }
        QuestDefinition def = defOpt.get();
        long tdg = 0L;
        List<ItemStack> items = new ArrayList<>();
        for (Reward reward : def.rewards()) {
            switch (reward) {
                case CurrencyReward currency -> tdg += currency.tdg();
                case ItemReward item -> items.add(item.stack().toStack());
                case UnlockReward ignored -> {
                }
            }
        }
        if (!CurrencyService.hasRoom(player, tdg) || !canFit(player.getInventory(), items)) {
            return false;
        }
        if (tdg > 0) {
            CurrencyService.credit(player, tdg);
        }
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && !player.getInventory().add(stack) && !stack.isEmpty()) {
                TDQ.LOGGER.warn("Quest claim leftover {} for {}", stack, player.getGameProfile().getName());
            }
        }
        long time = player.level().getGameTime();
        QuestProgress current = attachment(player).get(id);
        put(player, id, new QuestProgress(QuestStatus.CLAIMED, current.objectiveCounts(), current.acceptedGameTime(), time));
        for (Reward reward : def.rewards()) {
            if (reward instanceof UnlockReward unlock) {
                unlockQuest(player, unlock.questId());
            }
        }
        recomputeGraph(player);
        sync(player);
        return true;
    }

    public static boolean grant(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> def = definition(player, id);
        if (def.isEmpty()) {
            return false;
        }
        activate(player, id, def.get());
        recomputeGraph(player);
        sync(player);
        return true;
    }

    public static boolean forceComplete(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> def = definition(player, id);
        if (def.isEmpty()) {
            return false;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Objective objective : def.get().objectives()) {
            counts.put(objective.id(), requiredCount(objective));
        }
        QuestProgress current = attachment(player).get(id);
        long accepted = current.acceptedGameTime() == 0L ? player.level().getGameTime() : current.acceptedGameTime();
        put(player, id, new QuestProgress(QuestStatus.COMPLETED, counts, accepted, player.level().getGameTime()));
        sync(player);
        return true;
    }

    public static void reset(ServerPlayer player, ResourceLocation id) {
        player.setData(TDQAttachmentTypes.QUEST_PROGRESS, attachment(player).without(id));
        recomputeGraph(player);
        sync(player);
    }

    public static void resetAll(ServerPlayer player) {
        player.setData(TDQAttachmentTypes.QUEST_PROGRESS, QuestProgressAttachment.empty());
        recomputeGraph(player);
        sync(player);
    }

    public static void onKill(ServerPlayer player, EntityType<?> type) {
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        boolean changed = false;
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            QuestProgress progress = attachment(player).get(id);
            if (progress.status() != QuestStatus.ACTIVE) {
                continue;
            }
            for (Objective objective : entry.getValue().objectives()) {
                if (!(objective instanceof KillObjective kill) || !matches(kill, type)) {
                    continue;
                }
                int current = progress.objectiveCounts().getOrDefault(kill.id(), 0);
                int next = Math.min(kill.count(), current + 1);
                if (next == current) {
                    continue;
                }
                progress = progress.withCount(kill.id(), next);
                put(player, id, progress);
                TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, progress.status().wire(), kill.id(), next));
                changed = true;
            }
            if (tryComplete(player, id)) {
                changed = true;
            }
        }
        if (changed) {
            sync(player);
        }
    }

    public static boolean tryComplete(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> def = definition(player, id);
        QuestProgress progress = attachment(player).get(id);
        if (def.isEmpty() || progress.status() != QuestStatus.ACTIVE) {
            return false;
        }
        for (Objective objective : def.get().objectives()) {
            if (!objectiveMet(objective, progress)) {
                return false;
            }
        }
        put(player, id, new QuestProgress(
                QuestStatus.COMPLETED,
                progress.objectiveCounts(),
                progress.acceptedGameTime(),
                player.level().getGameTime()));
        TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, QuestStatus.COMPLETED.wire(), "", 0));
        return true;
    }

    public static void recomputeGraph(ServerPlayer player) {
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        Set<ResourceLocation> cyclic = cyclicQuests(registry);
        QuestProgressAttachment state = attachment(player);
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            QuestDefinition def = entry.getValue();
            QuestProgress current = state.get(id);
            QuestStatus status = current.status();
            if (status == QuestStatus.CLAIMED || status == QuestStatus.COMPLETED || status == QuestStatus.ACTIVE) {
                continue;
            }
            if (cyclic.contains(id)) {
                if (status != QuestStatus.LOCKED) {
                    put(player, id, current.withStatus(QuestStatus.LOCKED));
                    state = attachment(player);
                }
                continue;
            }
            if (prereqsClaimed(player, def, registry)) {
                if (def.autoAccept()) {
                    activate(player, id, def);
                } else if (status != QuestStatus.AVAILABLE) {
                    put(player, id, current.withStatus(QuestStatus.AVAILABLE));
                }
            } else if (status != QuestStatus.LOCKED) {
                put(player, id, current.withStatus(QuestStatus.LOCKED));
            }
            state = attachment(player);
        }
    }

    public static void afterReload(MinecraftServer server) {
        Registry<QuestDefinition> registry = server.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        Set<ResourceLocation> cyclic = cyclicQuests(registry);
        if (!cyclic.isEmpty()) {
            TDQ.LOGGER.warn("Quest graph has cycles; staying LOCKED: {}", cyclic);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            recomputeGraph(player);
            sync(player);
            CurrencyService.sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        Map<ResourceLocation, QuestProgressEntry> payload = new HashMap<>();
        for (var entry : attachment(player).quests().entrySet()) {
            QuestProgress progress = entry.getValue();
            payload.put(entry.getKey(), new QuestProgressEntry(progress.status().wire(), new HashMap<>(progress.objectiveCounts())));
        }
        TDQNetwork.sendToPlayer(player, new QuestProgressSyncS2C(payload));
    }

    static boolean canAct(ServerPlayer player) {
        return player.isAlive() && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
    }

    private static void activate(ServerPlayer player, ResourceLocation id, QuestDefinition def) {
        Map<String, Integer> counts = new HashMap<>();
        for (Objective objective : def.objectives()) {
            counts.put(objective.id(), 0);
        }
        put(player, id, new QuestProgress(QuestStatus.ACTIVE, counts, player.level().getGameTime(), 0L));
    }

    private static void unlockQuest(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> def = definition(player, id);
        if (def.isEmpty()) {
            return;
        }
        QuestStatus status = status(player, id);
        if (status == QuestStatus.CLAIMED || status == QuestStatus.COMPLETED || status == QuestStatus.ACTIVE) {
            return;
        }
        if (def.get().autoAccept()) {
            activate(player, id, def.get());
        } else {
            put(player, id, attachment(player).get(id).withStatus(QuestStatus.AVAILABLE));
        }
    }

    private static void put(ServerPlayer player, ResourceLocation id, QuestProgress progress) {
        player.setData(TDQAttachmentTypes.QUEST_PROGRESS, attachment(player).with(id, progress));
    }

    private static boolean prereqsClaimed(ServerPlayer player, QuestDefinition def, Registry<QuestDefinition> registry) {
        for (ResourceLocation prereq : def.prerequisites()) {
            if (registry.getOptional(prereq).isEmpty()) {
                continue;
            }
            if (status(player, prereq) != QuestStatus.CLAIMED) {
                return false;
            }
        }
        return true;
    }

    private static Set<ResourceLocation> cyclicQuests(Registry<QuestDefinition> registry) {
        Map<ResourceLocation, Integer> indegree = new HashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> outgoing = new HashMap<>();
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            indegree.putIfAbsent(id, 0);
            for (ResourceLocation prereq : entry.getValue().prerequisites()) {
                if (registry.getOptional(prereq).isEmpty()) {
                    continue;
                }
                outgoing.computeIfAbsent(prereq, k -> new ArrayList<>()).add(id);
                indegree.merge(id, 1, Integer::sum);
                indegree.putIfAbsent(prereq, 0);
            }
        }
        Queue<ResourceLocation> ready = new ArrayDeque<>();
        for (var entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }
        int seen = 0;
        while (!ready.isEmpty()) {
            ResourceLocation id = ready.remove();
            seen++;
            for (ResourceLocation next : outgoing.getOrDefault(id, List.of())) {
                int n = indegree.merge(next, -1, Integer::sum);
                if (n == 0) {
                    ready.add(next);
                }
            }
        }
        if (seen == indegree.size()) {
            return Set.of();
        }
        Set<ResourceLocation> cyclic = new HashSet<>();
        for (var entry : indegree.entrySet()) {
            if (entry.getValue() > 0) {
                cyclic.add(entry.getKey());
            }
        }
        return cyclic;
    }

    private static boolean matches(KillObjective kill, EntityType<?> type) {
        if (kill.entity().isPresent()) {
            return kill.entity().get().equals(EntityType.getKey(type));
        }
        return kill.entityTag().isPresent() && type.is(kill.entityTag().get());
    }

    private static boolean objectiveMet(Objective objective, QuestProgress progress) {
        int n = progress.objectiveCounts().getOrDefault(objective.id(), 0);
        return switch (objective) {
            case KillObjective kill -> n >= kill.count();
            case GatherObjective gather -> n >= gather.count();
            case SubmitObjective submit -> n >= submit.count();
            case CraftObjective craft -> n >= craft.count();
            case VisitObjective ignored -> n >= 1;
        };
    }

    private static int requiredCount(Objective objective) {
        return switch (objective) {
            case KillObjective kill -> kill.count();
            case GatherObjective gather -> gather.count();
            case SubmitObjective submit -> submit.count();
            case CraftObjective craft -> craft.count();
            case VisitObjective ignored -> 1;
        };
    }

    static boolean canFit(Inventory inventory, List<ItemStack> gifts) {
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack stack : inventory.items) {
            slots.add(stack.copy());
        }
        for (ItemStack stack : inventory.offhand) {
            slots.add(stack.copy());
        }
        for (ItemStack gift : gifts) {
            int remaining = gift.getCount();
            Item item = gift.getItem();
            int max = item.getDefaultMaxStackSize();
            for (int i = 0; i < slots.size() && remaining > 0; i++) {
                ItemStack slot = slots.get(i);
                if (slot.isEmpty()) {
                    int n = Math.min(max, remaining);
                    slots.set(i, new ItemStack(item, n));
                    remaining -= n;
                } else if (ItemStack.isSameItemSameComponents(slot, gift) && slot.getCount() < max) {
                    int n = Math.min(max - slot.getCount(), remaining);
                    slot.grow(n);
                    remaining -= n;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }
}
