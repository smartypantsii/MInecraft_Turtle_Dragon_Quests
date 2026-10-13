package com.jbarnes.turtledragonquests.registry;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.shop.ShopMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TDQMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TDQ.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP = MENUS.register(
            "shop",
            () -> IMenuTypeExtension.create(ShopMenu::fromNetwork));

    private TDQMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
