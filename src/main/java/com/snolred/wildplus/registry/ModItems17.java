package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.block.RanchBlock;
import com.snolred.wildplus.home.HouseKitItem;
import com.snolred.wildplus.item.AnimalBrushItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.4 아이템: 목장 재료 · 브러시 · 축사 상자 · 목장 기계 · 방명록 · 문패 · 대회 블록 · 트로피 */
public final class ModItems17 {
    public static final DeferredItem<Item> MILK_BOTTLE = ITEMS.registerSimpleItem("milk_bottle", p -> p.stacksTo(16));
    public static final DeferredItem<Item> BUTTER = ITEMS.registerSimpleItem("butter");
    public static final DeferredItem<Item> CREAM = ITEMS.registerSimpleItem("cream");
    public static final DeferredItem<Item> WOOL_TUFT = ITEMS.registerSimpleItem("wool_tuft");
    public static final DeferredItem<Item> YARN = ITEMS.registerSimpleItem("yarn");
    public static final DeferredItem<Item> CLOTH = ITEMS.registerSimpleItem("cloth");
    public static final DeferredItem<Item> GOLDEN_EGG = ITEMS.registerSimpleItem("golden_egg", p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<AnimalBrushItem> ANIMAL_BRUSH = ITEMS.registerItem("animal_brush", p -> new AnimalBrushItem(p.durability(128)));
    public static final DeferredItem<HouseKitItem> BARN_KIT = ITEMS.registerItem("house_barn", p -> new HouseKitItem(p.stacksTo(1), "barn", 13, 11, 15));
    public static final DeferredItem<HouseKitItem> COOP_KIT = ITEMS.registerItem("house_coop", p -> new HouseKitItem(p.stacksTo(1), "coop", 7, 6, 9));

    public static final DeferredItem<BlockItem> HAY_TROUGH = ITEMS.registerSimpleBlockItem("hay_trough", ModBlocks7.RANCH.get(RanchBlock.Kind.HAY_TROUGH));
    public static final DeferredItem<BlockItem> NEST_BOX = ITEMS.registerSimpleBlockItem("nest_box", ModBlocks7.RANCH.get(RanchBlock.Kind.NEST_BOX));
    public static final DeferredItem<BlockItem> CHEESE_BARREL = ITEMS.registerSimpleBlockItem("cheese_barrel", ModBlocks7.RANCH.get(RanchBlock.Kind.CHEESE_BARREL));
    public static final DeferredItem<BlockItem> BUTTER_CHURN = ITEMS.registerSimpleBlockItem("butter_churn", ModBlocks7.RANCH.get(RanchBlock.Kind.BUTTER_CHURN));
    public static final DeferredItem<BlockItem> SPINNING_WHEEL = ITEMS.registerSimpleBlockItem("spinning_wheel", ModBlocks7.RANCH.get(RanchBlock.Kind.SPINNING_WHEEL));
    public static final DeferredItem<BlockItem> RANCH_MARKET = ITEMS.registerSimpleBlockItem("ranch_market", ModBlocks7.RANCH_MARKET);
    public static final DeferredItem<BlockItem> GUESTBOOK = ITEMS.registerSimpleBlockItem("guestbook", ModBlocks7.GUESTBOOK);
    public static final DeferredItem<BlockItem> HOUSE_PLAQUE = ITEMS.registerSimpleBlockItem("house_plaque", ModBlocks7.HOUSE_PLAQUE);
    public static final DeferredItem<BlockItem> CONTEST_BOARD = ITEMS.registerSimpleBlockItem("contest_board", ModBlocks7.CONTEST_BOARD);
    public static final DeferredItem<BlockItem> PODIUM = ITEMS.registerSimpleBlockItem("podium", ModBlocks7.PODIUM);
    public static final DeferredItem<BlockItem> TROPHY_GOLD = ITEMS.registerSimpleBlockItem("trophy_gold", ModBlocks7.TROPHY_GOLD, p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> TROPHY_SILVER = ITEMS.registerSimpleBlockItem("trophy_silver", ModBlocks7.TROPHY_SILVER, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> TROPHY_BRONZE = ITEMS.registerSimpleBlockItem("trophy_bronze", ModBlocks7.TROPHY_BRONZE, p -> p.rarity(Rarity.UNCOMMON));

    public static void init() {}

    private ModItems17() {}
}
