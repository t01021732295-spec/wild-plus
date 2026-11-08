package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems24 {
    public static final DeferredItem<BlockItem> ATM_TALL = ITEMS.registerSimpleBlockItem("atm_tall", ModBlocks13.ATM_TALL);
    public static final DeferredItem<BlockItem> PHARMACY_RACK = ITEMS.registerSimpleBlockItem("pharmacy_rack", ModBlocks13.PHARMACY_RACK);

    public static void init() {}

    private ModItems24() {}
}
