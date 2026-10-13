package com.jbarnes.turtledragonquests.shop;

import java.util.List;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.TDQConfig;
import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.network.TDQNetwork;
import com.jbarnes.turtledragonquests.network.s2c.ActionResultS2C;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.registry.TDQMenus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ShopMenu extends AbstractContainerMenu {
    public static final int SELL_SLOTS = 9;
    public static final int PLAYER_INV_START = 9;
    public static final int PLAYER_HOTBAR_START = 36;
    public static final int SLOT_COUNT = 45;

    private final SimpleContainer sellSlots = new SimpleContainer(SELL_SLOTS);
    private final ShopMenuSnapshot snapshot;
    private long lastClickGameTime = Long.MIN_VALUE / 4;

    public ShopMenu(int containerId, Inventory playerInv, ShopMenuSnapshot snapshot) {
        super(TDQMenus.SHOP.get(), containerId);
        this.snapshot = snapshot;
        for (int i = 0; i < SELL_SLOTS; i++) {
            this.addSlot(new Slot(this.sellSlots, i, 8 + i * 18, 98));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 134 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 192));
        }
    }

    public static ShopMenu fromNetwork(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        return new ShopMenu(containerId, playerInv, ShopMenuSnapshot.read(buf));
    }

    public ShopMenuSnapshot snapshot() {
        return snapshot;
    }

    public SimpleContainer sellSlots() {
        return sellSlots;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (serverPlayer.containerMenu != this) {
            fail(serverPlayer, "screen.turtle_dragon_quests.shop.fail.closed");
            return false;
        }
        if (!ShopService.canAct(serverPlayer)) {
            fail(serverPlayer, "screen.turtle_dragon_quests.shop.reject");
            return false;
        }
        long now = serverPlayer.level().getGameTime();
        int cooldown = TDQConfig.shopActionCooldownTicks();
        if (cooldown > 0 && now - lastClickGameTime < cooldown && lastClickGameTime != Long.MIN_VALUE / 4) {
            fail(serverPlayer, "screen.turtle_dragon_quests.shop.fail.cooldown");
            return false;
        }
        if (buttonId < 0 || buttonId >= snapshot.offerIds().size()) {
            return false;
        }
        ResourceLocation offerId = snapshot.offerIds().get(buttonId);
        ShopOffer offer = serverPlayer.registryAccess()
                .registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY)
                .get(offerId);
        if (offer == null || !ShopService.unlocked(serverPlayer, offer)) {
            fail(serverPlayer, "screen.turtle_dragon_quests.shop.fail.closed");
            return false;
        }
        lastClickGameTime = now;
        boolean ok = offer.side() == ShopSide.BUY
                ? buy(serverPlayer, offerId, offer)
                : sell(serverPlayer, offerId, offer);
        return ok;
    }

    private boolean buy(ServerPlayer player, ResourceLocation offerId, ShopOffer offer) {
        long cost;
        try {
            cost = Math.multiplyExact(offer.unitPrice(), 1L);
        } catch (ArithmeticException e) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.closed");
            return false;
        }
        int remaining = ShopService.remaining(player.getServer(), offerId);
        if (remaining < 1) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.stock");
            return false;
        }
        ItemStack goods = offer.stack().toStack();
        if (!ShopService.canFit(player.getInventory(), List.of(goods.copy()))) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.space");
            return false;
        }
        if (cost > 0L && CurrencyService.get(player) < cost) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.funds");
            return false;
        }
        if (cost > 0L && !CurrencyService.tryDebit(player, cost)) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.funds");
            return false;
        }
        if (!ShopService.tryConsume(player.getServer(), offerId, 1)) {
            if (cost > 0L) {
                CurrencyService.credit(player, cost);
            }
            fail(player, "screen.turtle_dragon_quests.shop.fail.stock");
            return false;
        }
        ItemStack give = offer.stack().toStack();
        if (!player.getInventory().add(give) && !give.isEmpty()) {
            TDQ.LOGGER.warn("Shop buy leftover {} for {}", give, player.getGameProfile().getName());
            player.drop(give, false);
        }
        ShopService.syncAll(player.getServer());
        ok(player, "screen.turtle_dragon_quests.shop.ok.buy");
        return true;
    }

    private boolean sell(ServerPlayer player, ResourceLocation offerId, ShopOffer offer) {
        int remaining = ShopService.remaining(player.getServer(), offerId);
        if (remaining < 1) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.stock");
            return false;
        }
        long payout = offer.unitPrice();
        if (payout > 0L && !CurrencyService.hasRoom(player, payout)) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.wallet");
            return false;
        }
        ResourceLocation itemId = offer.stack().item();
        int need = offer.stack().count();
        if (countMatching(itemId) < need) {
            fail(player, "screen.turtle_dragon_quests.shop.fail.items");
            return false;
        }
        extractMatching(itemId, need);
        if (!ShopService.tryConsume(player.getServer(), offerId, 1)) {
            sellSlots.addItem(new ItemStack(BuiltInRegistries.ITEM.get(itemId), need));
            fail(player, "screen.turtle_dragon_quests.shop.fail.stock");
            return false;
        }
        if (payout > 0L) {
            CurrencyService.credit(player, payout);
        }
        ShopService.syncAll(player.getServer());
        ok(player, "screen.turtle_dragon_quests.shop.ok.sell");
        return true;
    }

    private int countMatching(ResourceLocation itemId) {
        int n = 0;
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = sellSlots.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private void extractMatching(ResourceLocation itemId, int need) {
        int left = need;
        for (int i = 0; i < SELL_SLOTS && left > 0; i++) {
            ItemStack stack = sellSlots.getItem(i);
            if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                continue;
            }
            int take = Math.min(stack.getCount(), left);
            stack.shrink(take);
            left -= take;
            sellSlots.setChanged();
        }
    }

    private static void fail(ServerPlayer player, String key) {
        TDQNetwork.sendToPlayer(player, new ActionResultS2C(false, Component.translatable(key)));
    }

    private static void ok(ServerPlayer player, String key) {
        TDQNetwork.sendToPlayer(player, new ActionResultS2C(true, Component.translatable(key)));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SELL_SLOTS) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, SELL_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.sellSlots);
    }
}
