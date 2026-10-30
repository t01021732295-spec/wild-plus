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

    private ModMenus() {}
}
