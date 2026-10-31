package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 9차: 핏빛 군주 보상, 명예의 전당 */
public final class ModItems6 {
    /** 핏빛 군주의 심장: 출하하면 비싸게 팔리는 전리품 */
    public static final DeferredItem<Item> LORD_HEART = ITEMS.registerSimpleItem("lord_heart",
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FARMER_MERCHANT_SPAWN_EGG = ITEMS.registerItem("farmer_merchant_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.spawnEgg(ModEntities.FARMER_MERCHANT.get())));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FISHER_MERCHANT_SPAWN_EGG = ITEMS.registerItem("fisher_merchant_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.spawnEgg(ModEntities.FISHER_MERCHANT.get())));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> BANKER_MERCHANT_SPAWN_EGG = ITEMS.registerItem("banker_merchant_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.spawnEgg(ModEntities.BANKER_MERCHANT.get())));
    public static final DeferredItem<BlockItem> HALL_OF_FAME = ITEMS.registerSimpleBlockItem("hall_of_fame", ModBlocks.HALL_OF_FAME);

    public static void init() {}

    private ModItems6() {}
}
