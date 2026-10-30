package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.menu.VendingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WildPlus.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VendingMenu>> VENDING =
            MENUS.register("vending_machine", () -> new MenuType<>(VendingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.PickMenu>> PICK =
            MENUS.register("pick", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.PickMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.BackpackMenu>> BACKPACK =
            MENUS.register("backpack", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.BackpackMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.StockMenu>> STOCKS =
            MENUS.register("stocks", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.StockMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.BankMenu>> BANK =
            MENUS.register("bank", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.BankMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.BountyMenu>> BOUNTY =
            MENUS.register("bounty", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.BountyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.snolred.wildplus.menu.ExchangeMenu>> EXCHANGE =
            MENUS.register("exchange", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.snolred.wildplus.menu.ExchangeMenu::new));

    private ModMenus() {}
}
