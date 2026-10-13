package com.jbarnes.turtledragonquests.client;

import java.util.Optional;

import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.shop.ShopMenu;
import com.jbarnes.turtledragonquests.shop.ShopOffer;
import com.jbarnes.turtledragonquests.shop.ShopSide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private OfferList list;
    private Button action;
    private int selectedIndex;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 122;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        this.list = this.addRenderableWidget(new OfferList(this.minecraft, 160, 62, this.topPos + 16, 18));
        this.list.setX(this.leftPos + 8);
        this.list.populate();
        this.action = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.turtle_dragon_quests.shop.buy"),
                b -> clickSelected())
                .bounds(this.leftPos + 8, this.topPos + 80, 72, 16)
                .build());
        updateAction();
    }

    @Override
    protected void containerTick() {
        updateAction();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        Component wallet = Component.translatable("screen.turtle_dragon_quests.tdg", ClientWallet.tdg());
        graphics.drawString(this.font, wallet, this.leftPos + this.imageWidth - 8 - this.font.width(wallet), this.topPos + 6, 0xFFE082, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xC0101010);
        graphics.fill(this.leftPos + 7, this.topPos + 97, this.leftPos + 169, this.topPos + 115, 0x66000000);
        graphics.fill(this.leftPos + 7, this.topPos + 133, this.leftPos + 169, this.topPos + 211, 0x66000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.action != null && this.action.isMouseOver(mouseX, mouseY) && this.action.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickSelected() {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        if (this.selectedIndex < 0 || this.selectedIndex >= this.menu.snapshot().offerIds().size()) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.selectedIndex);
    }

    private void updateAction() {
        if (this.action == null) {
            return;
        }
        Optional<ShopOffer> offer = selectedOffer();
        if (offer.isEmpty()) {
            this.action.active = false;
            this.action.setMessage(Component.translatable("screen.turtle_dragon_quests.shop.buy"));
            return;
        }
        this.action.active = true;
        this.action.setMessage(Component.translatable(offer.get().side() == ShopSide.SELL
                ? "screen.turtle_dragon_quests.shop.sell"
                : "screen.turtle_dragon_quests.shop.buy"));
    }

    private Optional<ShopOffer> selectedOffer() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return Optional.empty();
        }
        if (this.selectedIndex < 0 || this.selectedIndex >= this.menu.snapshot().offerIds().size()) {
            return Optional.empty();
        }
        ResourceLocation id = this.menu.snapshot().offerIds().get(this.selectedIndex);
        return this.minecraft.level.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY).getOptional(id);
    }

    private final class OfferList extends ObjectSelectionList<OfferList.OfferEntry> {
        private OfferList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        private void populate() {
            this.clearEntries();
            int i = 0;
            for (ResourceLocation id : ShopScreen.this.menu.snapshot().offerIds()) {
                this.addEntry(new OfferEntry(id, i++));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        private final class OfferEntry extends ObjectSelectionList.Entry<OfferEntry> {
            private final ResourceLocation id;
            private final int index;

            private OfferEntry(ResourceLocation id, int index) {
                this.id = id;
                this.index = index;
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
                    float partialTick
            ) {
                ShopOffer offer = OfferList.this.minecraft.level == null
                        ? null
                        : OfferList.this.minecraft.level.registryAccess()
                                .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                                .get(this.id);
                if (offer == null) {
                    graphics.drawString(ShopScreen.this.font, this.id.toString(), left + 2, top + 4, 0xAAAAAA, false);
                    return;
                }
                ItemStack stack = new ItemStack(
                        BuiltInRegistries.ITEM.get(offer.stack().item()),
                        offer.stack().count());
                graphics.renderItem(stack, left, top);
                graphics.renderItemDecorations(ShopScreen.this.font, stack, left, top);
                Optional<Integer> stock = ShopScreen.this.menu.snapshot().remaining()
                        .getOrDefault(this.id, Optional.empty());
                if (ClientShopState.contains(this.id)) {
                    stock = ClientShopState.remaining(this.id);
                }
                Component stockText = stock.isEmpty()
                        ? Component.translatable("screen.turtle_dragon_quests.shop.unlimited")
                        : Component.translatable("screen.turtle_dragon_quests.shop.stock", stock.get());
                graphics.drawString(
                        ShopScreen.this.font,
                        Component.translatable("screen.turtle_dragon_quests.shop.price", offer.unitPrice()),
                        left + 20,
                        top + 1,
                        0xFFFFFF,
                        false);
                graphics.drawString(ShopScreen.this.font, stockText, left + 20, top + 10, 0xCCCCCC, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                ShopScreen.this.selectedIndex = this.index;
                OfferList.this.setSelected(this);
                ShopScreen.this.updateAction();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.id.toString());
            }
        }
    }
}
