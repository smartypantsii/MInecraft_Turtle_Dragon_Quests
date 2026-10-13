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
import java.util.function.Predicate;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.TDQConfig;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.network.s2c.OpenQuestLogS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestDeltaS2C;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressEntry;
import com.jbarnes.turtledragonquests.network.s2c.QuestProgressSyncS2C;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.core.BlockPos;
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
        recomputeGather(player);
        recomputeGraph(player);
        sync(player);
        TDQ.LOGGER.debug("Quest accepted {} by {}", id, player.getGameProfile().getName());
        return true;
    }

    public static boolean tryClaim(ServerPlayer player, ResourceLocation id) {
        if (!canAct(player)) {
            return false;
        }
        recomputeGather(player);
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
            TDQ.LOGGER.info("Quest claim refused for {} on {} (wallet or inventory full)", player.getGameProfile().getName(), id);
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
        TDQ.LOGGER.debug(
                "Quest claimed {} by {} balance {}",
                id,
                player.getGameProfile().getName(),
                CurrencyService.get(player));
        return true;
    }

    public static boolean grant(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> def = definition(player, id);
        if (def.isEmpty()) {
            return false;
        }
        activate(player, id, def.get());
        recomputeGather(player);
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
        recomputeGather(player);
        sync(player);
    }

    public static void resetAll(ServerPlayer player) {
        player.setData(TDQAttachmentTypes.QUEST_PROGRESS, QuestProgressAttachment.empty());
        recomputeGraph(player);
        recomputeGather(player);
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

    public static void onCraft(ServerPlayer player, ItemStack crafted) {
        if (crafted.isEmpty()) {
            return;
        }
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        boolean changed = false;
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            QuestProgress progress = attachment(player).get(id);
            if (progress.status() != QuestStatus.ACTIVE) {
                continue;
            }
            for (Objective objective : entry.getValue().objectives()) {
                if (!(objective instanceof CraftObjective craft) || !craft.output().matches(crafted)) {
                    continue;
                }
                int current = progress.objectiveCounts().getOrDefault(craft.id(), 0);
                int next = (int) Math.min(craft.count(), (long) current + crafted.getCount());
                if (next == current) {
                    continue;
                }
                progress = progress.withCount(craft.id(), next);
                put(player, id, progress);
                TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, progress.status().wire(), craft.id(), next));
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

    public static boolean trySubmit(ServerPlayer player, ResourceLocation id, String objectiveId) {
        if (!canAct(player)) {
            return false;
        }
        Optional<QuestDefinition> def = definition(player, id);
        QuestProgress progress = attachment(player).get(id);
        if (def.isEmpty() || progress.status() != QuestStatus.ACTIVE) {
            return false;
        }
        SubmitObjective submit = null;
        for (Objective objective : def.get().objectives()) {
            if (objective instanceof SubmitObjective candidate && candidate.id().equals(objectiveId)) {
                submit = candidate;
                break;
            }
        }
        if (submit == null) {
            return false;
        }
        int current = progress.objectiveCounts().getOrDefault(submit.id(), 0);
        int remaining = submit.count() - current;
        if (remaining <= 0) {
            return false;
        }
        int taken = extractMatching(player.getInventory(), submit.items(), remaining);
        if (taken <= 0) {
            return false;
        }
        int next = current + taken;
        progress = progress.withCount(submit.id(), next);
        put(player, id, progress);
        TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, progress.status().wire(), submit.id(), next));
        tryComplete(player, id);
        recomputeGather(player);
        sync(player);
        return true;
    }

    public static void recomputeGather(ServerPlayer player) {
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        boolean changed = false;
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            QuestProgress progress = attachment(player).get(id);
            if (progress.status() != QuestStatus.ACTIVE) {
                continue;
            }
            boolean anyGather = false;
            for (Objective objective : entry.getValue().objectives()) {
                if (!(objective instanceof GatherObjective gather)) {
                    continue;
                }
                anyGather = true;
                int owned = countMatching(player.getInventory(), gather.items());
                int next = Math.min(gather.count(), owned);
                int current = progress.objectiveCounts().getOrDefault(gather.id(), 0);
                if (next == current) {
                    continue;
                }
                progress = progress.withCount(gather.id(), next);
                put(player, id, progress);
                TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, progress.status().wire(), gather.id(), next));
                changed = true;
            }
            if (anyGather && tryComplete(player, id)) {
                changed = true;
            }
        }
        if (changed) {
            sync(player);
        }
    }

    public static void recomputeVisit(ServerPlayer player) {
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        boolean changed = false;
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            QuestProgress progress = attachment(player).get(id);
            if (progress.status() != QuestStatus.ACTIVE) {
                continue;
            }
            for (Objective objective : entry.getValue().objectives()) {
                if (!(objective instanceof VisitObjective visit)) {
                    continue;
                }
                int current = progress.objectiveCounts().getOrDefault(visit.id(), 0);
                if (current >= 1) {
                    continue;
                }
                if (!matchesVisit(player, visit)) {
                    continue;
                }
                progress = progress.withCount(visit.id(), 1);
                put(player, id, progress);
                TDQNetwork.sendToPlayer(player, new QuestDeltaS2C(id, progress.status().wire(), visit.id(), 1));
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

    public static void tick(ServerPlayer player) {
        int interval = TDQConfig.visitCheckIntervalTicks();
        if (interval <= 0 || player.tickCount % interval != 0) {
            return;
        }
        if (hasActive(player, objective -> objective instanceof GatherObjective)) {
            recomputeGather(player);
        }
        if (hasActive(player, objective -> objective instanceof VisitObjective)) {
            recomputeVisit(player);
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
        TDQ.LOGGER.debug("Quest completed {} by {}", id, player.getGameProfile().getName());
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
            recomputeGather(player);
            recomputeVisit(player);
            sync(player);
            CurrencyService.sync(player);
        }
    }

    public static void openLog(ServerPlayer player) {
        recomputeGraph(player);
        recomputeGather(player);
        recomputeVisit(player);
        CurrencyService.sync(player);
        sync(player);
        TDQNetwork.sendToPlayer(player, OpenQuestLogS2C.INSTANCE);
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

    private static boolean hasActive(ServerPlayer player, Predicate<Objective> predicate) {
        Registry<QuestDefinition> registry = player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        for (var entry : registry.entrySet()) {
            if (attachment(player).get(entry.getKey().location()).status() != QuestStatus.ACTIVE) {
                continue;
            }
            for (Objective objective : entry.getValue().objectives()) {
                if (predicate.test(objective)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesVisit(ServerPlayer player, VisitObjective visit) {
        if (visit.dimension().isPresent() && !player.level().dimension().equals(visit.dimension().get())) {
            return false;
        }
        if (visit.biome().isPresent() && !player.level().getBiome(player.blockPosition()).is(visit.biome().get())) {
            return false;
        }
        if (visit.box().isPresent() && !visit.box().get().isInside(player.blockPosition())) {
            return false;
        }
        if (visit.center().isPresent() && visit.radius().isPresent()) {
            BlockPos center = visit.center().get();
            long radius = visit.radius().get();
            if (player.blockPosition().distSqr(center) > radius * radius) {
                return false;
            }
        }
        return true;
    }

    private static int countMatching(Inventory inventory, ItemIngredient ingredient) {
        return countList(inventory.items, ingredient) + countList(inventory.offhand, ingredient);
    }

    private static int countList(List<ItemStack> slots, ItemIngredient ingredient) {
        int n = 0;
        for (ItemStack stack : slots) {
            if (ingredient.matches(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static int extractMatching(Inventory inventory, ItemIngredient ingredient, int max) {
        int taken = extractList(inventory.items, ingredient, max);
        if (taken < max) {
            taken += extractList(inventory.offhand, ingredient, max - taken);
        }
        return taken;
    }

    private static int extractList(List<ItemStack> slots, ItemIngredient ingredient, int max) {
        int taken = 0;
        for (ItemStack stack : slots) {
            if (taken >= max) {
                break;
            }
            if (!ingredient.matches(stack)) {
                continue;
            }
            int n = Math.min(stack.getCount(), max - taken);
            stack.shrink(n);
            taken += n;
        }
        return taken;
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
