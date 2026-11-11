package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.0 생활: 계절 작물 · 꿀 · 말린 것 · 장 · 시설 */
public final class ModItems33 {
    private ModItems33() {}

    public static final DeferredItem<BlockItem> SOYBEAN_SEEDS = ITEMS.registerItem("soybean_seeds", p -> new BlockItem(ModBlocks20.SOYBEAN_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<BlockItem> KOREAN_MELON_SEEDS = ITEMS.registerItem("korean_melon_seeds", p -> new BlockItem(ModBlocks20.KOREAN_MELON_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<BlockItem> SWEET_POTATO_SEEDS = ITEMS.registerItem("sweet_potato_seeds", p -> new BlockItem(ModBlocks20.SWEET_POTATO_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<BlockItem> RADISH_SEEDS = ITEMS.registerItem("radish_seeds", p -> new BlockItem(ModBlocks20.RADISH_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> SOYBEAN = food("soybean", 1, 0.2F);
    public static final DeferredItem<Item> KOREAN_MELON = food("korean_melon", 3, 0.4F);
    public static final DeferredItem<Item> SWEET_POTATO = food("sweet_potato", 3, 0.4F);
    public static final DeferredItem<Item> RADISH = food("radish", 2, 0.3F);

    public static final DeferredItem<Item> HONEY_JAR = food("honey_jar", 4, 0.5F);
    public static final DeferredItem<Item> ROYAL_JELLY = ITEMS.registerSimpleItem("royal_jelly", p -> p.rarity(Rarity.RARE)
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2F).alwaysEdible().build()));
    public static final DeferredItem<Item> CHILI_POWDER = ITEMS.registerSimpleItem("chili_powder");
    public static final DeferredItem<Item> RAISIN = food("raisin", 2, 0.4F);
    public static final DeferredItem<Item> DRIED_FISH = food("dried_fish", 5, 0.7F);
    public static final DeferredItem<Item> JERKY = food("jerky", 7, 0.9F);
    public static final DeferredItem<Item> KKAKDUGI = food("kkakdugi", 3, 0.5F);
    public static final DeferredItem<Item> DANMUJI = food("danmuji", 3, 0.5F);
    public static final DeferredItem<Item> DOENJANG = ITEMS.registerSimpleItem("doenjang");
    public static final DeferredItem<Item> SOY_SAUCE = ITEMS.registerSimpleItem("soy_sauce");

    public static final DeferredItem<BlockItem> BEEHIVE_BOX = ITEMS.registerSimpleBlockItem("beehive_box", ModBlocks20.BEEHIVE_BOX);
    public static final DeferredItem<BlockItem> DRYING_RACK = ITEMS.registerSimpleBlockItem("drying_rack", ModBlocks20.DRYING_RACK);
    public static final DeferredItem<BlockItem> JANGDOK = ITEMS.registerSimpleBlockItem("jangdok", ModBlocks20.JANGDOK);
    public static final DeferredItem<BlockItem> FISH_FARM = ITEMS.registerSimpleBlockItem("fish_farm", ModBlocks20.FISH_FARM);

    private static DeferredItem<Item> food(String name, int nutrition, float sat) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(sat).build()));
    }

    public static void init() {}
}
