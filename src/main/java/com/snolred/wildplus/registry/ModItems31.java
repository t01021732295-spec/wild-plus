package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems31 {
    public static final DeferredItem<BlockItem> MARKET_STALL = ITEMS.registerSimpleBlockItem("market_stall", ModBlocks19.MARKET_STALL);

    public static void init() {}

    private ModItems31() {}
}
