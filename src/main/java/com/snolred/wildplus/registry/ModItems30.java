package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.7 화산 거신 전리품 */
public final class ModItems30 {
    public static final DeferredItem<Item> VOLCANO_HEART = ITEMS.registerSimpleItem("volcano_heart", p -> p.rarity(Rarity.EPIC).fireResistant());

    public static void init() {}

    private ModItems30() {}
}
