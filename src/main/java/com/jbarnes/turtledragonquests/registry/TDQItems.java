package com.jbarnes.turtledragonquests.registry;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.item.QuestJournalItem;
import com.jbarnes.turtledragonquests.item.ShopCatalogItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TDQItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TDQ.MOD_ID);

    public static final DeferredItem<QuestJournalItem> QUEST_JOURNAL = ITEMS.registerItem(
            "quest_journal",
            QuestJournalItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<ShopCatalogItem> SHOP_CATALOG = ITEMS.registerItem(
            "shop_catalog",
            ShopCatalogItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> TDG_COIN = ITEMS.registerItem(
            "tdg_coin",
            Item::new,
            new Item.Properties().stacksTo(64));

    private TDQItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
