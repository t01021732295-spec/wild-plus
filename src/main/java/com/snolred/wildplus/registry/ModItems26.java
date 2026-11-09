package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.3 평원 블록 · 몬스터 전리품 */
public final class ModItems26 {
    public static final DeferredItem<BlockItem> VOID_TURF = ITEMS.registerSimpleBlockItem("void_turf", ModBlocks15.VOID_TURF);
    public static final DeferredItem<BlockItem> VOID_CRYSTAL_CYAN = ITEMS.registerSimpleBlockItem("void_crystal_cyan", ModBlocks15.VOID_CRYSTAL_CYAN);
    public static final DeferredItem<BlockItem> VOID_CRYSTAL_PURPLE = ITEMS.registerSimpleBlockItem("void_crystal_purple", ModBlocks15.VOID_CRYSTAL_PURPLE);
    public static final DeferredItem<Item> RIFT_DUST = ITEMS.registerSimpleItem("rift_dust", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> WRAITH_PLUME = ITEMS.registerSimpleItem("wraith_plume", p -> p.rarity(Rarity.RARE));

    public static void init() {}

    private ModItems26() {}
}
