package com.jbarnes.turtledragonquests;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TDQ.MOD_ID)
public final class TurtleDragonQuests {
    public TurtleDragonQuests(IEventBus modEventBus, ModContainer modContainer) {
        TDQ.LOGGER.info("Turtle Dragon Quests {} loading", modContainer.getModInfo().getVersion());
    }
}
