package com.jbarnes.turtledragonquests.registry;

import com.jbarnes.turtledragonquests.TDQ;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TDQCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TDQ.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.turtle_dragon_quests.main"))
                    .icon(() -> new ItemStack(TDQItems.TDG_COIN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(TDQItems.QUEST_JOURNAL.get());
                        output.accept(TDQItems.SHOP_CATALOG.get());
                        output.accept(TDQItems.TDG_COIN.get());
                    })
                    .build());

    private TDQCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
