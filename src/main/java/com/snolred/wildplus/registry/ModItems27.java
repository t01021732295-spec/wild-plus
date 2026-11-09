package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.4 군주의 인장 · 제단 */
public final class ModItems27 {
    public static final DeferredItem<Item> LORD_SIGIL = ITEMS.registerSimpleItem("lord_sigil", p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(16));
    public static final DeferredItem<BlockItem> LORD_ALTAR = ITEMS.registerSimpleBlockItem("lord_altar", ModBlocks16.LORD_ALTAR);

    public static void init() {}

    private ModItems27() {}
}
