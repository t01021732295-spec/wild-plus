package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;

/** 식당 블록 아이템 */
public final class ModItems15 {
    public static final DeferredItem<BlockItem> RESTAURANT_COUNTER = ITEMS.registerSimpleBlockItem("restaurant_counter", ModBlocks5.RESTAURANT_COUNTER);
    public static final DeferredItem<BlockItem> RESTAURANT_ENTRANCE = ITEMS.registerSimpleBlockItem("restaurant_entrance", ModBlocks5.RESTAURANT_ENTRANCE);
    public static final DeferredItem<BlockItem> DINING_TABLE = ITEMS.registerSimpleBlockItem("dining_table", ModBlocks5.DINING_TABLE);
    public static final DeferredItem<BlockItem> DINING_CHAIR = ITEMS.registerSimpleBlockItem("dining_chair", ModBlocks5.DINING_CHAIR);
    public static final DeferredItem<BlockItem> RESTAURANT_SIGN = ITEMS.registerSimpleBlockItem("restaurant_sign", ModBlocks5.RESTAURANT_SIGN);

    public static void init() {}

    private ModItems15() {}
}
