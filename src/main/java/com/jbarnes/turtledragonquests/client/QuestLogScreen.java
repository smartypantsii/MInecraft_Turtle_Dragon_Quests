package com.jbarnes.turtledragonquests.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jbarnes.turtledragonquests.network.c2s.AcceptQuestC2S;
import com.jbarnes.turtledragonquests.network.c2s.ClaimRewardsC2S;
import com.jbarnes.turtledragonquests.network.c2s.SubmitItemsC2S;
import com.jbarnes.turtledragonquests.quest.CraftObjective;
import com.jbarnes.turtledragonquests.quest.CurrencyReward;
import com.jbarnes.turtledragonquests.quest.GatherObjective;
import com.jbarnes.turtledragonquests.quest.ItemIngredient;
import com.jbarnes.turtledragonquests.quest.ItemReward;
import com.jbarnes.turtledragonquests.quest.KillObjective;
import com.jbarnes.turtledragonquests.quest.Objective;
import com.jbarnes.turtledragonquests.quest.QuestDefinition;
import com.jbarnes.turtledragonquests.quest.QuestStatus;
import com.jbarnes.turtledragonquests.quest.Reward;
import com.jbarnes.turtledragonquests.quest.SubmitObjective;
import com.jbarnes.turtledragonquests.quest.UnlockReward;
import com.jbarnes.turtledragonquests.quest.VisitObjective;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestLogScreen extends Screen {
    private static final List<String> GROUP_ORDER = List.of(
            "first_light", "settling", "tide", "depths", "nether", "dragon");

    private QuestPickList list;
    private Button accept;
    private Button claim;
    private Button submit;
    private ResourceLocation selectedId;
    private int detailLeft;

    public QuestLogScreen() {
        super(Component.translatable("screen.turtle_dragon_quests.quest_log"));
    }

    @Override
    protected void init() {
        int listWidth = Math.max(160, this.width / 3);
        int listTop = 28;
        int listHeight = this.height - 40;
        this.detailLeft = 16 + listWidth;
        int buttonY = this.height - 26;
        int buttonW = 72;
        this.accept = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.turtle_dragon_quests.accept"),
                b -> sendAccept())
                .bounds(this.detailLeft, buttonY, buttonW, 20)
                .build());
        this.claim = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.turtle_dragon_quests.claim"),
                b -> sendClaim())
                .bounds(this.detailLeft + buttonW + 6, buttonY, buttonW, 20)
                .build());
        this.submit = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.turtle_dragon_quests.submit"),
                b -> sendSubmit())
                .bounds(this.detailLeft + (buttonW + 6) * 2, buttonY, buttonW, 20)
                .build());
        this.list = new QuestPickList(this.minecraft, listWidth, listHeight, listTop, 18);
        this.list.setX(8);
        this.addRenderableWidget(this.list);
        this.list.populate();
        updateButtons();
    }

    @Override
    public void tick() {
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title, 12, 10, 0xFFFFFF, true);
        Component wallet = Component.translatable("screen.turtle_dragon_quests.tdg", ClientWallet.tdg());
        graphics.drawString(this.font, wallet, this.width - 12 - this.font.width(wallet), 10, 0xFFE082, true);
        renderDetails(graphics);
    }

    private void renderDetails(GuiGraphics graphics) {
        Optional<QuestDefinition> def = selectedDefinition();
        if (def.isEmpty()) {
            graphics.drawString(
                    this.font,
                    Component.translatable("screen.turtle_dragon_quests.select"),
                    this.detailLeft,
                    32,
                    0xAAAAAA,
                    false);
            return;
        }
        QuestDefinition quest = def.get();
        QuestStatus status = ClientQuestData.status(this.selectedId);
        int x = this.detailLeft;
        int y = 32;
        int maxWidth = this.width - x - 12;
        y = drawWrapped(graphics, quest.title(), x, y, maxWidth, 0xFFFFFF);
        y = drawWrapped(
                graphics,
                Component.translatable("screen.turtle_dragon_quests.status." + status.getSerializedName()),
                x,
                y,
                maxWidth,
                statusColor(status));
        y += 4;
        y = drawWrapped(graphics, quest.description(), x, y, maxWidth, 0xCCCCCC);
        y += 6;
        graphics.drawString(this.font, Component.translatable("screen.turtle_dragon_quests.objectives"), x, y, 0xFFD54F, true);
        y += this.font.lineHeight + 2;
        for (Objective objective : quest.objectives()) {
            int have = ClientQuestData.count(this.selectedId, objective.id());
            int need = requiredCount(objective);
            Component line = Component.translatable(
                    "screen.turtle_dragon_quests.objective_line",
                    have,
                    need,
                    describe(objective));
            int color = have >= need ? 0x55FF55 : 0xEEEEEE;
            y = drawWrapped(graphics, line, x, y, maxWidth, color);
            if (y > this.height - 80) {
                break;
            }
        }
        y += 6;
        if (y < this.height - 70) {
            graphics.drawString(this.font, Component.translatable("screen.turtle_dragon_quests.rewards"), x, y, 0xFFD54F, true);
            y += this.font.lineHeight + 2;
            for (Reward reward : quest.rewards()) {
                y = drawWrapped(graphics, describe(reward), x, y, maxWidth, 0xDDDDFF);
                if (y > this.height - 50) {
                    break;
                }
            }
        }
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        for (FormattedCharSequence line : this.font.split(text, maxWidth)) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += this.font.lineHeight;
        }
        return y;
    }

    private void updateButtons() {
        if (this.accept == null || this.claim == null || this.submit == null) {
            return;
        }
        Optional<QuestDefinition> def = selectedDefinition();
        QuestStatus status = this.selectedId == null ? QuestStatus.LOCKED : ClientQuestData.status(this.selectedId);
        this.accept.visible = def.isPresent() && status == QuestStatus.AVAILABLE;
        this.claim.visible = def.isPresent() && status == QuestStatus.COMPLETED;
        this.submit.visible = def.isPresent() && status == QuestStatus.ACTIVE && firstIncompleteSubmit(def.get()).isPresent();
        this.accept.active = this.accept.visible;
        this.claim.active = this.claim.visible;
        this.submit.active = this.submit.visible;
    }

    private Optional<QuestDefinition> selectedDefinition() {
        if (this.selectedId == null || this.minecraft == null || this.minecraft.player == null) {
            return Optional.empty();
        }
        return this.minecraft.player.registryAccess()
                .registryOrThrow(TDQDataRegistries.QUEST_REGISTRY)
                .getOptional(this.selectedId);
    }

    private void sendAccept() {
        if (this.selectedId != null) {
            PacketDistributor.sendToServer(new AcceptQuestC2S(this.selectedId));
        }
    }

    private void sendClaim() {
        if (this.selectedId != null) {
            PacketDistributor.sendToServer(new ClaimRewardsC2S(this.selectedId));
        }
    }

    private void sendSubmit() {
        Optional<QuestDefinition> def = selectedDefinition();
        if (this.selectedId == null || def.isEmpty()) {
            return;
        }
        firstIncompleteSubmit(def.get()).ifPresent(id ->
                PacketDistributor.sendToServer(new SubmitItemsC2S(this.selectedId, id)));
    }

    private Optional<String> firstIncompleteSubmit(QuestDefinition def) {
        for (Objective objective : def.objectives()) {
            if (objective instanceof SubmitObjective submitObj
                    && ClientQuestData.count(this.selectedId, submitObj.id()) < submitObj.count()) {
                return Optional.of(submitObj.id());
            }
        }
        return Optional.empty();
    }

    private static int requiredCount(Objective objective) {
        return switch (objective) {
            case KillObjective kill -> kill.count();
            case GatherObjective gather -> gather.count();
            case SubmitObjective submitObj -> submitObj.count();
            case CraftObjective craft -> craft.count();
            case VisitObjective ignored -> 1;
        };
    }

    private static Component describe(Objective objective) {
        return switch (objective) {
            case KillObjective kill -> Component.translatable(
                    "screen.turtle_dragon_quests.objective.kill",
                    kill.entity().map(QuestLogScreen::entityName)
                            .orElseGet(() -> kill.entityTag().map(tag -> Component.literal(tag.location().toString()))
                                    .orElse(Component.literal("?"))));
            case GatherObjective gather -> Component.translatable(
                    "screen.turtle_dragon_quests.objective.gather", ingredientName(gather.items()));
            case SubmitObjective submitObj -> Component.translatable(
                    "screen.turtle_dragon_quests.objective.submit", ingredientName(submitObj.items()));
            case CraftObjective craft -> Component.translatable(
                    "screen.turtle_dragon_quests.objective.craft", ingredientName(craft.output()));
            case VisitObjective visit -> Component.translatable(
                    "screen.turtle_dragon_quests.objective.visit", visitLabel(visit));
        };
    }

    private static Component describe(Reward reward) {
        return switch (reward) {
            case CurrencyReward currency -> Component.translatable(
                    "screen.turtle_dragon_quests.reward.tdg", currency.tdg());
            case ItemReward item -> Component.translatable(
                    "screen.turtle_dragon_quests.reward.item",
                    itemName(item.stack().item()),
                    item.stack().count());
            case UnlockReward unlock -> Component.translatable(
                    "screen.turtle_dragon_quests.reward.unlock", unlock.questId().toString());
        };
    }

    private static Component ingredientName(ItemIngredient ingredient) {
        if (ingredient.item().isPresent()) {
            return itemName(ingredient.item().get());
        }
        return Component.literal(ingredient.tag().map(tag -> tag.location().toString()).orElse("?"));
    }

    private static Component itemName(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(Item::getDescription)
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private static Component entityName(ResourceLocation id) {
        return EntityType.byString(id.toString())
                .map(EntityType::getDescription)
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private static Component visitLabel(VisitObjective visit) {
        if (visit.biome().isPresent()) {
            ResourceLocation biome = visit.biome().get().location();
            return Component.translatable("biome." + biome.getNamespace() + "." + biome.getPath());
        }
        if (visit.dimension().isPresent()) {
            return Component.literal(visit.dimension().get().location().toString());
        }
        if (visit.box().isPresent()) {
            return Component.translatable("screen.turtle_dragon_quests.visit.box");
        }
        if (visit.center().isPresent()) {
            return Component.translatable("screen.turtle_dragon_quests.visit.radius");
        }
        return Component.literal("?");
    }

    private static int statusColor(QuestStatus status) {
        return switch (status) {
            case LOCKED -> 0x888888;
            case AVAILABLE -> 0xFFFF55;
            case ACTIVE -> 0x55FFFF;
            case COMPLETED -> 0xFFAA00;
            case CLAIMED -> 0x55FF55;
        };
    }

    private final class QuestPickList extends ObjectSelectionList<QuestPickList.Entry> {
        private QuestPickList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        private void populate() {
            this.clearEntries();
            if (minecraft.player == null) {
                return;
            }
            var registry = minecraft.player.registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
            List<Map.Entry<ResourceLocation, QuestDefinition>> rows = new ArrayList<>();
            registry.entrySet().forEach(entry -> {
                if (entry.getValue().group().isEmpty()) {
                    return;
                }
                rows.add(Map.entry(entry.getKey().location(), entry.getValue()));
            });
            rows.sort(Comparator
                    .comparingInt((Map.Entry<ResourceLocation, QuestDefinition> e) -> groupIndex(e.getValue().group().orElse("")))
                    .thenComparing(e -> e.getValue().title().getString()));
            String lastGroup = null;
            Entry toSelect = null;
            for (var row : rows) {
                String group = row.getValue().group().orElse("");
                if (!group.equals(lastGroup)) {
                    this.addEntry(new Entry(null, groupTitle(group), true));
                    lastGroup = group;
                }
                Entry questEntry = new Entry(row.getKey(), row.getValue().title(), false);
                this.addEntry(questEntry);
                if (toSelect == null && ClientQuestData.status(row.getKey()) == QuestStatus.ACTIVE) {
                    toSelect = questEntry;
                } else if (toSelect == null && ClientQuestData.status(row.getKey()) == QuestStatus.AVAILABLE) {
                    toSelect = questEntry;
                } else if (toSelect == null && ClientQuestData.status(row.getKey()) == QuestStatus.COMPLETED) {
                    toSelect = questEntry;
                }
            }
            if (toSelect != null) {
                this.setSelected(toSelect);
                QuestLogScreen.this.selectedId = toSelect.questId;
            }
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 8;
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            QuestLogScreen.this.selectedId = entry == null || entry.header ? null : entry.questId;
            QuestLogScreen.this.updateButtons();
        }

        private final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ResourceLocation questId;
            private final Component label;
            private final boolean header;

            private Entry(ResourceLocation questId, Component label, boolean header) {
                this.questId = questId;
                this.label = label;
                this.header = header;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }

            @Override
            public void render(
                    GuiGraphics graphics,
                    int index,
                    int top,
                    int left,
                    int width,
                    int height,
                    int mouseX,
                    int mouseY,
                    boolean hovering,
                    float partialTick) {
                if (this.header) {
                    graphics.drawString(QuestLogScreen.this.font, this.label, left + 2, top + 4, 0xFFD54F, true);
                    return;
                }
                QuestStatus status = ClientQuestData.status(this.questId);
                Component line = Component.literal(statusMark(status) + " ").append(this.label);
                graphics.drawString(QuestLogScreen.this.font, line, left + 8, top + 4, statusColor(status), false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.header) {
                    return false;
                }
                QuestPickList.this.setSelected(this);
                return true;
            }
        }
    }

    private static int groupIndex(String group) {
        int i = GROUP_ORDER.indexOf(group);
        return i < 0 ? GROUP_ORDER.size() : i;
    }

    private static Component groupTitle(String group) {
        return Component.translatable("quest.turtle_dragon_quests.group." + group);
    }

    private static String statusMark(QuestStatus status) {
        return switch (status) {
            case LOCKED -> "--";
            case AVAILABLE -> "<>";
            case ACTIVE -> ">>";
            case COMPLETED -> "!!";
            case CLAIMED -> "OK";
        };
    }
}
