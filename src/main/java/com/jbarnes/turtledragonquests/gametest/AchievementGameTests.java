package com.jbarnes.turtledragonquests.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.AchievementReward;
import com.jbarnes.turtledragonquests.currency.AchievementRewards;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.registry.TDQAttachmentTypes;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TDQ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AchievementGameTests {
    private static final String MINE_STONE_JSON = """
            {
              "advancement": "minecraft:story/mine_stone",
              "tdg": 5
            }
            """;

    private static final ResourceLocation MINE_STONE = ResourceLocation.parse("minecraft:story/mine_stone");

    private AchievementGameTests() {}

    @GameTest(template = "empty")
    public static void codec_roundtrip_example_achievement_rewards(GameTestHelper helper) {
        AchievementReward parsed = parse(MINE_STONE_JSON);
        helper.assertTrue(parsed.advancement().equals(MINE_STONE), "story_mine_stone advancement");
        helper.assertTrue(parsed.tdg() == 5L, "story_mine_stone tdg");
        roundTrip(helper, "story_mine_stone json", parsed);

        helper.assertTrue(
                AchievementReward.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                        { "advancement": "minecraft:story/mine_stone", "tdg": 0 }
                        """)).isError(),
                "tdg 0 is a codec error");
        helper.assertTrue(
                AchievementReward.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                        { "advancement": "minecraft:story/mine_stone", "tdg": 1 }
                        """)).result().isPresent(),
                "tdg 1 is allowed");

        Registry<AchievementReward> registry = helper.getLevel().registryAccess()
                .registryOrThrow(TDQDataRegistries.ACHIEVEMENT_REWARD_REGISTRY);
        helper.assertTrue(registry.getOptional(TDQ.id("story_mine_stone")).isPresent(), "loaded story_mine_stone");
        helper.assertTrue(
                registry.getOptional(TDQ.id("story_mine_stone")).filter(r -> r.tdg() == 5L).isPresent(),
                "loaded mapping pays 5 TDG");
        for (var entry : registry.entrySet()) {
            roundTrip(helper, entry.getKey().location().toString(), entry.getValue());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void achievement_tdg_pays_once(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CurrencyService.set(player, 0);
        AchievementRewards.onEarn(player, MINE_STONE);
        helper.assertValueEqual(5L, CurrencyService.get(player), "first earn pays 5 TDG");
        helper.assertTrue(
                CurrencyService.wallet(player).paidAchievements().contains(MINE_STONE),
                "mine_stone marked paid");

        AchievementRewards.onEarn(player, MINE_STONE);
        helper.assertValueEqual(5L, CurrencyService.get(player), "second earn does not pay again");

        AchievementRewards.onEarn(player, ResourceLocation.parse("minecraft:story/mine_diamond"));
        helper.assertValueEqual(5L, CurrencyService.get(player), "unmapped advancement pays nothing");

        if (completeAdvancement(helper, player, MINE_STONE)) {
            player.setData(
                    TDQAttachmentTypes.WALLET,
                    CurrencyService.wallet(player).withTdg(0L).withPaidAchievements(Set.of()));
            AchievementRewards.backfill(player);
            helper.assertValueEqual(5L, CurrencyService.get(player), "backfill pays already-earned mapping");
            AchievementRewards.backfill(player);
            helper.assertValueEqual(5L, CurrencyService.get(player), "backfill is pay-once");
        }

        helper.succeed();
    }

    private static boolean completeAdvancement(GameTestHelper helper, ServerPlayer player, ResourceLocation id) {
        AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);
        if (holder == null) {
            return false;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        List<String> remaining = new ArrayList<>();
        advancements.getOrStartProgress(holder).getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) {
            advancements.award(holder, criterion);
        }
        return advancements.getOrStartProgress(holder).isDone();
    }

    private static AchievementReward parse(String json) {
        JsonElement element = JsonParser.parseString(json);
        return AchievementReward.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
    }

    private static void roundTrip(GameTestHelper helper, String label, AchievementReward reward) {
        JsonElement encoded = AchievementReward.CODEC.encodeStart(JsonOps.INSTANCE, reward).getOrThrow();
        AchievementReward decoded = AchievementReward.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        JsonElement encodedAgain = AchievementReward.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        helper.assertTrue(encoded.equals(encodedAgain), "codec round-trip " + label);
    }
}
