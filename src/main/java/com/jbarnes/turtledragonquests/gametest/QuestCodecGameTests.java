package com.jbarnes.turtledragonquests.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.quest.GatherObjective;
import com.jbarnes.turtledragonquests.quest.QuestDefinition;
import com.jbarnes.turtledragonquests.quest.VisitObjective;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TDQ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestCodecGameTests {
    private static final String FIRST_TIMBER_JSON = """
            {
              "title": { "translate": "quest.turtle_dragon_quests.first_timber.title" },
              "description": { "translate": "quest.turtle_dragon_quests.first_timber.desc" },
              "icon": "minecraft:oak_log",
              "prerequisites": [],
              "auto_accept": true,
              "group": "first_light",
              "objectives": [
                {
                  "type": "gather",
                  "id": "any_logs",
                  "tag": "minecraft:logs",
                  "count": 8
                }
              ],
              "rewards": [
                { "type": "currency", "tdg": 10 },
                { "type": "item", "item": "minecraft:stick", "count": 16 }
              ]
            }
            """;

    private static final String VISIT_BOX_JSON = """
            {
              "title": { "text": "visit box fixture" },
              "description": { "text": "test only" },
              "objectives": [
                {
                  "type": "visit",
                  "id": "in_box",
                  "box": { "min": [0, 64, 0], "max": [16, 80, 16] }
                }
              ]
            }
            """;

    private QuestCodecGameTests() {}

    @GameTest(template = "empty")
    public static void codec_roundtrip_example_quests(GameTestHelper helper) {
        QuestDefinition timber = parse(FIRST_TIMBER_JSON);
        helper.assertTrue(timber.autoAccept(), "first_timber auto_accept");
        helper.assertTrue(timber.objectives().getFirst() instanceof GatherObjective gather
                        && gather.items().tag().isPresent()
                        && gather.items().tag().get().equals(ItemTags.LOGS)
                        && gather.count() == 8,
                "first_timber gathers 8 minecraft:logs");
        roundTrip(helper, "first_timber json", timber);

        QuestDefinition boxQuest = parse(VISIT_BOX_JSON);
        helper.assertTrue(boxQuest.objectives().getFirst() instanceof VisitObjective visit
                        && visit.box().isPresent()
                        && visit.box().get().minX() == 0
                        && visit.box().get().minY() == 64
                        && visit.box().get().maxX() == 16
                        && visit.box().get().maxY() == 80,
                "visit box {min,max}");
        roundTrip(helper, "visit box json", boxQuest);

        Registry<QuestDefinition> registry = helper.getLevel().registryAccess()
                .registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        helper.assertTrue(registry.size() >= 22, "campaign plus visit-box fixture");
        helper.assertTrue(
                registry.getOptional(TDQ.id("first_timber")).filter(QuestDefinition::autoAccept).isPresent(),
                "loaded first_timber is auto_accept");
        helper.assertTrue(
                registry.getOptional(TDQ.id("fixtures/visit_box")).filter(q -> q.group().isEmpty()).isPresent(),
                "visit-box fixture is not a journal group");

        int campaign = 0;
        for (var entry : registry.entrySet()) {
            ResourceKey<QuestDefinition> key = entry.getKey();
            QuestDefinition def = entry.getValue();
            roundTrip(helper, key.location().toString(), def);
            if (!key.location().getPath().startsWith("fixtures/")) {
                campaign++;
                if (!key.location().equals(TDQ.id("first_timber"))) {
                    helper.assertFalse(def.autoAccept(), key.location() + " must not auto_accept");
                }
            }
        }
        helper.assertTrue(campaign == 21, "six-chapter campaign size");
        helper.succeed();
    }

    private static QuestDefinition parse(String json) {
        JsonElement element = JsonParser.parseString(json);
        return QuestDefinition.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
    }

    private static void roundTrip(GameTestHelper helper, String label, QuestDefinition def) {
        JsonElement encoded = QuestDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow();
        QuestDefinition decoded = QuestDefinition.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        JsonElement encodedAgain = QuestDefinition.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        helper.assertTrue(encoded.equals(encodedAgain), "codec round-trip " + label);
    }
}
