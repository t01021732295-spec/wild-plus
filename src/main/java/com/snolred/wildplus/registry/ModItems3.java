package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.CoinPouchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3차 업데이트: 화폐·거래·몹·자판기 */
public final class ModItems3 {
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin", p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<CoinPouchItem> COIN_POUCH = ITEMS.registerItem("coin_pouch", CoinPouchItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> VENDING_MACHINE = ITEMS.registerSimpleBlockItem("vending_machine", ModBlocks.VENDING_MACHINE);

    public static final DeferredItem<SpawnEggItem> MERCHANT_SPAWN_EGG = ITEMS.registerItem("merchant_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(ModEntities.MERCHANT.get())));
    public static final DeferredItem<SpawnEggItem> BLACKSMITH_MERCHANT_SPAWN_EGG = ITEMS.registerItem("blacksmith_merchant_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(ModEntities.BLACKSMITH_MERCHANT.get())));
    public static final DeferredItem<SpawnEggItem> BANDIT_SPAWN_EGG = ITEMS.registerItem("bandit_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(ModEntities.BANDIT.get())));
    public static final DeferredItem<SpawnEggItem> GOLDEN_SLIME_SPAWN_EGG = ITEMS.registerItem("golden_slime_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(ModEntities.GOLDEN_SLIME.get())));

    public static void init() {}

    private ModItems3() {}
}
