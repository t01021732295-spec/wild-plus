package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.3.7 루비화(100,000동) · 루비 배낭(160칸, 20×8) */
public final class ModItems40 {
    private ModItems40() {}

    public static final DeferredItem<Item> RUBY_COIN = ITEMS.registerSimpleItem("ruby_coin",
            p -> p.rarity(Rarity.EPIC).fireResistant().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<com.snolred.wildplus.item.BackpackItem> RUBY_BACKPACK = ITEMS.registerItem("ruby_backpack",
            p -> new com.snolred.wildplus.item.BackpackItem(p, 5), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static void init() {}
}
