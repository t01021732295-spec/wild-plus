package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.3.6 해제 각인석: 강화대에서 장비의 각인을 하나 지움 (고대 도시 상자 · 고대의 워든) */
public final class ModItems39 {
    private ModItems39() {}

    public static final DeferredItem<Item> ENGRAVE_ERASER = ITEMS.registerSimpleItem("engrave_eraser", p -> p.rarity(Rarity.EPIC).fireResistant());

    public static void init() {}
}
