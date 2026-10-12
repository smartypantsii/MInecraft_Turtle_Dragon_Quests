package com.jbarnes.turtledragonquests;

import com.jbarnes.turtledragonquests.registry.TDQCreativeTabs;
import com.jbarnes.turtledragonquests.registry.TDQItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TDQ.MOD_ID)
public final class TurtleDragonQuests {
    public TurtleDragonQuests(IEventBus modEventBus, ModContainer modContainer) {
        TDQItems.register(modEventBus);
        TDQCreativeTabs.register(modEventBus);
        TDQ.LOGGER.info("Turtle Dragon Quests {} loading", modContainer.getModInfo().getVersion());
    }
}
