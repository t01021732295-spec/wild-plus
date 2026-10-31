package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredItem;

/** 8차: 새 작물·물고기, 드릴, 자동화 기계·카지노 */
public final class ModItems5 {
    // ───────── 작물 ─────────
    public static final DeferredItem<BlockItem> CORN_SEEDS = ITEMS.registerItem("corn_seeds",
            p -> new BlockItem(ModBlocks.CORN_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> CORN = food("corn", 3, 0.4F);
    public static final DeferredItem<Item> GRILLED_CORN = food("grilled_corn", 7, 0.7F);
    public static final DeferredItem<BlockItem> CABBAGE_SEEDS = ITEMS.registerItem("cabbage_seeds",
            p -> new BlockItem(ModBlocks.CABBAGE_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> CABBAGE = food("cabbage", 3, 0.5F);
    public static final DeferredItem<Item> SALAD = ITEMS.registerSimpleItem("salad", p -> p.stacksTo(16)
            .usingConvertsTo(net.minecraft.world.item.Items.BOWL).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.8F).build()));
    /** 블루베리는 열매를 그대로 심는다 */
    public static final DeferredItem<BlockItem> BLUEBERRY = ITEMS.registerItem("blueberry",
            p -> new BlockItem(ModBlocks.BLUEBERRY_CROP.get(), p.useItemDescriptionPrefix()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final DeferredItem<Item> BLUEBERRY_MUFFIN = ITEMS.registerSimpleItem("blueberry_muffin",
            p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build(),
                    ModItems.effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0))));

    // ───────── 물고기 ─────────
    public static final DeferredItem<Item> CATFISH = food("catfish", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_CATFISH = food("cooked_catfish", 6, 0.7F);
    public static final DeferredItem<Item> TUNA = food("tuna", 3, 0.3F);
    public static final DeferredItem<Item> TUNA_STEAK = food("tuna_steak", 8, 0.9F);
    public static final DeferredItem<Item> SWEETFISH = food("sweetfish", 2, 0.2F);
    public static final DeferredItem<Item> GRILLED_SWEETFISH = ITEMS.registerSimpleItem("grilled_sweetfish",
            p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build(),
                    ModItems.effect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0))));
    public static final DeferredItem<Item> DRAGON_KING_FISH = ITEMS.registerSimpleItem("dragon_king_fish",
            p -> p.rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.2F).alwaysEdible().build(),
                    ModItems.effect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 120, 1),
                            new MobEffectInstance(MobEffects.RESISTANCE, 20 * 120, 1),
                            new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1))));

    // ───────── 드릴: 곡괭이 + 삽, 3×3 채굴 ─────────
    public static final ToolMaterial DRILL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2400, 14.0F, 3.0F, 12,
            ModItems2.MYTHRIL_REPAIR);
    public static final DeferredItem<Item> DRILL = ITEMS.registerItem("drill",
            p -> new Item(p.pickaxe(DRILL_MATERIAL, 1.0F, -2.8F).rarity(Rarity.RARE)));

    // ───────── 기계 · 카지노 ─────────
    public static final DeferredItem<BlockItem> AUTO_HARVESTER = ITEMS.registerSimpleBlockItem("auto_harvester", ModBlocks.AUTO_HARVESTER);
    public static final DeferredItem<BlockItem> ORE_MINER = ITEMS.registerSimpleBlockItem("ore_miner", ModBlocks.ORE_MINER);
    public static final DeferredItem<BlockItem> AUTO_FISHER = ITEMS.registerSimpleBlockItem("auto_fisher", ModBlocks.AUTO_FISHER);
    public static final DeferredItem<BlockItem> CASINO_TABLE = ITEMS.registerSimpleBlockItem("casino_table", ModBlocks.CASINO_TABLE);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name,
                p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    public static void init() {}

    private ModItems5() {}
}
