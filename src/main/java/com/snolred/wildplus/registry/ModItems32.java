package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.9 모험 수첩 (도감 · 업적 · 칭호 화면을 여는 책) */
public final class ModItems32 {
    public static final DeferredItem<Item> ADVENTURE_JOURNAL = ITEMS.registerSimpleItem("adventure_journal", p -> p.stacksTo(1));

    public static void init() {}

    private ModItems32() {}
}
