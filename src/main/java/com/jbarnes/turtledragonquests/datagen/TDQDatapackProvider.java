package com.jbarnes.turtledragonquests.datagen;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.quest.CraftObjective;
import com.jbarnes.turtledragonquests.quest.CurrencyReward;
import com.jbarnes.turtledragonquests.quest.GatherObjective;
import com.jbarnes.turtledragonquests.quest.ItemIngredient;
import com.jbarnes.turtledragonquests.quest.ItemReward;
import com.jbarnes.turtledragonquests.quest.KillObjective;
import com.jbarnes.turtledragonquests.quest.Objective;
import com.jbarnes.turtledragonquests.quest.QuestDefinition;
import com.jbarnes.turtledragonquests.quest.Reward;
import com.jbarnes.turtledragonquests.quest.SimpleStack;
import com.jbarnes.turtledragonquests.quest.SubmitObjective;
import com.jbarnes.turtledragonquests.quest.VisitObjective;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class TDQDatapackProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(TDQDataRegistries.QUEST_REGISTRY, TDQDatapackProvider::bootstrapQuests);

    private TDQDatapackProvider() {}

    public static void gatherData(GatherDataEvent event) {
        event.createDatapackRegistryObjects(BUILDER, Set.of(TDQ.MOD_ID));
    }

    static void bootstrapQuests(BootstrapContext<QuestDefinition> ctx) {
        quest(ctx, "first_timber", "first_light", true, List.of(), "minecraft:oak_log",
                List.of(new GatherObjective("any_logs", ItemIngredient.ofTag(ItemTags.LOGS), 8)),
                List.of(tdg(10), item("stick", 16)));

        quest(ctx, "crafting_bench", "first_light", false, req("first_timber"), "minecraft:crafting_table",
                List.of(craft("make_bench", "crafting_table", 1)),
                List.of(tdg(10)));

        quest(ctx, "stone_pick", "first_light", false, req("crafting_bench"), "minecraft:stone_pickaxe",
                List.of(craft("make_pick", "stone_pickaxe", 1)),
                List.of(tdg(15), item("torch", 8)));

        quest(ctx, "night_watch", "first_light", false, req("stone_pick"), "minecraft:rotten_flesh",
                List.of(kill("kill_zombies", "zombie", 3)),
                List.of(tdg(20)));

        quest(ctx, "cobble_tithe", "first_light", false, req("night_watch"), "minecraft:cobblestone",
                List.of(submit("turn_in_cobble", "cobblestone", 16)),
                List.of(tdg(15), item("bread", 5)));

        quest(ctx, "iron_will", "settling", false, req("cobble_tithe"), "minecraft:iron_ingot",
                List.of(gatherItem("iron", "iron_ingot", 8)),
                List.of(tdg(30)));

        quest(ctx, "iron_edge", "settling", false, req("iron_will"), "minecraft:iron_sword",
                List.of(craft("make_sword", "iron_sword", 1)),
                List.of(tdg(25), item("iron_helmet", 1)));

        quest(ctx, "skeleton_crew", "settling", false, req("iron_edge"), "minecraft:bone",
                List.of(kill("kill_skeletons", "skeleton", 8)),
                List.of(tdg(40), item("arrow", 16)));

        quest(ctx, "wheat_tithe", "settling", false, req("skeleton_crew"), "minecraft:wheat",
                List.of(submit("turn_in_wheat", "wheat", 16)),
                List.of(tdg(25), item("hay_block", 1)));

        quest(ctx, "beach_scouts", "tide", false, req("wheat_tithe"), "minecraft:turtle_egg",
                List.of(new VisitObjective("find_beach", Optional.empty(), Optional.of(Biomes.BEACH),
                        Optional.empty(), Optional.empty(), Optional.empty())),
                List.of(tdg(20)));

        quest(ctx, "drowned_watch", "tide", false, req("beach_scouts"), "minecraft:heart_of_the_sea",
                List.of(kill("kill_drowned", "drowned", 8)),
                List.of(tdg(50), item("heart_of_the_sea", 1)));

        quest(ctx, "scute_duty", "tide", false, req("drowned_watch"), "minecraft:turtle_scute",
                List.of(gatherItem("scute", "turtle_scute", 1)),
                List.of(tdg(40)));

        quest(ctx, "turtle_helm", "tide", false, req("scute_duty"), "minecraft:turtle_helmet",
                List.of(craft("make_helm", "turtle_helmet", 1)),
                List.of(tdg(60), item("turtle_scute", 8)));

        quest(ctx, "diamond_glint", "depths", false, req("drowned_watch"), "minecraft:diamond",
                List.of(gatherItem("diamonds", "diamond", 3)),
                List.of(tdg(80), item("diamond_pickaxe", 1)));

        quest(ctx, "creeper_company", "depths", false, req("diamond_glint"), "minecraft:gunpowder",
                List.of(kill("kill_creepers", "creeper", 5)),
                List.of(tdg(50), item("gunpowder", 8)));

        quest(ctx, "into_the_nether", "depths", false, req("diamond_glint"), "minecraft:flint_and_steel",
                List.of(new VisitObjective("enter_nether", Optional.of(Level.NETHER), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty())),
                List.of(tdg(40), item("flint_and_steel", 1)));

        quest(ctx, "blaze_rods", "nether", false, req("into_the_nether"), "minecraft:blaze_rod",
                List.of(kill("kill_blazes", "blaze", 10), gatherItem("rods", "blaze_rod", 4)),
                List.of(tdg(100), item("blaze_powder", 4)));

        quest(ctx, "brew_station", "nether", false, req("blaze_rods"), "minecraft:brewing_stand",
                List.of(craft("make_stand", "brewing_stand", 1)),
                List.of(tdg(80), item("magma_cream", 3)));

        quest(ctx, "ender_eyes", "dragon", false, req("brew_station"), "minecraft:ender_eye",
                List.of(submit("turn_in_eyes", "ender_eye", 12)),
                List.of(tdg(120), item("ender_eye", 1)));

        quest(ctx, "the_dragon", "dragon", false, req("ender_eyes"), "minecraft:dragon_head",
                List.of(kill("kill_dragon", "ender_dragon", 1)),
                List.of(tdg(500), item("ender_pearl", 16), item("enchanted_golden_apple", 1)));

        quest(ctx, "after_the_fire", "dragon", false, req("the_dragon"), "minecraft:elytra",
                List.of(gatherItem("elytra", "elytra", 1)),
                List.of(tdg(250), item("shulker_shell", 1)));

        ctx.register(key("fixtures/visit_box"), new QuestDefinition(
                Component.literal("visit box fixture"),
                Component.literal("test only"),
                Optional.empty(),
                List.of(),
                false,
                Optional.empty(),
                List.of(new VisitObjective(
                        "in_box",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new BoundingBox(0, 64, 0, 16, 80, 16)),
                        Optional.empty(),
                        Optional.empty())),
                List.of()));
    }

    private static void quest(
            BootstrapContext<QuestDefinition> ctx,
            String id,
            String group,
            boolean autoAccept,
            List<ResourceLocation> prerequisites,
            String icon,
            List<Objective> objectives,
            List<Reward> rewards
    ) {
        ctx.register(key(id), new QuestDefinition(
                Component.translatable("quest.turtle_dragon_quests." + id + ".title"),
                Component.translatable("quest.turtle_dragon_quests." + id + ".desc"),
                Optional.of(ResourceLocation.parse(icon)),
                prerequisites,
                autoAccept,
                Optional.of(group),
                objectives,
                rewards));
    }

    private static ResourceKey<QuestDefinition> key(String path) {
        return ResourceKey.create(TDQDataRegistries.QUEST_REGISTRY, TDQ.id(path));
    }

    private static List<ResourceLocation> req(String... ids) {
        return java.util.Arrays.stream(ids).map(TDQ::id).toList();
    }

    private static Reward tdg(long amount) {
        return new CurrencyReward(amount);
    }

    private static Reward item(String path, int count) {
        return new ItemReward(new SimpleStack(ResourceLocation.withDefaultNamespace(path), count));
    }

    private static Objective gatherItem(String id, String item, int count) {
        return new GatherObjective(id, ItemIngredient.ofItem(ResourceLocation.withDefaultNamespace(item)), count);
    }

    private static Objective craft(String id, String item, int count) {
        return new CraftObjective(id, ItemIngredient.ofItem(ResourceLocation.withDefaultNamespace(item)), count);
    }

    private static Objective submit(String id, String item, int count) {
        return new SubmitObjective(id, ItemIngredient.ofItem(ResourceLocation.withDefaultNamespace(item)), count);
    }

    private static Objective kill(String id, String entity, int count) {
        return new KillObjective(id, Optional.of(ResourceLocation.withDefaultNamespace(entity)), Optional.empty(), count);
    }
}
