package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.6: 생명의 정수 (정예 몹 · 핏빛 군주에게서만 나오는 로열 에메랄드 재료) */
public final class ModItems19 {
    public static final DeferredItem<Item> LIFE_ESSENCE = ITEMS.registerSimpleItem("life_essence",
            p -> p.rarity(Rarity.EPIC).fireResistant());

    public static void init() {}

    private ModItems19() {}
}
