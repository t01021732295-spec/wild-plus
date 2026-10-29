package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;
import static com.snolred.wildplus.registry.ModItems.effect;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.item.FertilizerItem;
import com.snolred.wildplus.item.JadeFishingRodItem;
import com.snolred.wildplus.item.MythrilHoeItem;
import com.snolred.wildplus.item.SunkenChestItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredItem;

/** 2차 업데이트: 광석·도구, 낚시, 농사 */
public final class ModItems2 {
    public static final TagKey<Item> MYTHRIL_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("mythril_repair"));

    // ───────── 광석 블록 아이템 ─────────
    public static final DeferredItem<BlockItem> MYTHRIL_ORE = ITEMS.registerSimpleBlockItem("mythril_ore", ModBlocks.MYTHRIL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MYTHRIL_ORE = ITEMS.registerSimpleBlockItem("deepslate_mythril_ore", ModBlocks.DEEPSLATE_MYTHRIL_ORE);
    public static final DeferredItem<BlockItem> MYTHRIL_BLOCK = ITEMS.registerSimpleBlockItem("mythril_block", ModBlocks.MYTHRIL_BLOCK);
    public static final DeferredItem<BlockItem> JADE_ORE = ITEMS.registerSimpleBlockItem("jade_ore", ModBlocks.JADE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_JADE_ORE = ITEMS.registerSimpleBlockItem("deepslate_jade_ore", ModBlocks.DEEPSLATE_JADE_ORE);
    public static final DeferredItem<BlockItem> JADE_BLOCK = ITEMS.registerSimpleBlockItem("jade_block", ModBlocks.JADE_BLOCK);

    // ───────── 재료 ─────────
    public static final DeferredItem<Item> RAW_MYTHRIL = ITEMS.registerSimpleItem("raw_mythril");
    public static final DeferredItem<Item> MYTHRIL_INGOT = ITEMS.registerSimpleItem("mythril_ingot");
    public static final DeferredItem<Item> JADE = ITEMS.registerSimpleItem("jade");

    // ───────── 미스릴 도구: 다이아몬드보다 빠르고 가볍다 ─────────
    public static final ToolMaterial MYTHRIL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1300, 10.0F, 3.0F, 20, MYTHRIL_REPAIR);

    public static final DeferredItem<Item> MYTHRIL_SWORD = ITEMS.registerItem("mythril_sword",
            p -> new Item(p.sword(MYTHRIL, 3.0F, -2.2F)));
    public static final DeferredItem<Item> MYTHRIL_PICKAXE = ITEMS.registerItem("mythril_pickaxe",
            p -> new Item(p.pickaxe(MYTHRIL, 1.0F, -2.8F)));
    public static final DeferredItem<Item> MYTHRIL_AXE = ITEMS.registerItem("mythril_axe",
            p -> new Item(p.axe(MYTHRIL, 5.0F, -3.0F)));
    public static final DeferredItem<Item> MYTHRIL_SHOVEL = ITEMS.registerItem("mythril_shovel",
            p -> new Item(p.shovel(MYTHRIL, 1.5F, -3.0F)));
    public static final DeferredItem<MythrilHoeItem> MYTHRIL_HOE = ITEMS.registerItem("mythril_hoe",
            p -> new MythrilHoeItem(p.hoe(MYTHRIL, -3.0F, 0.0F)));

    // ───────── 낚시 ─────────
    public static final DeferredItem<JadeFishingRodItem> JADE_FISHING_ROD = ITEMS.registerItem("jade_fishing_rod",
            p -> new JadeFishingRodItem(p.durability(512).enchantable(15).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> RAINBOW_TROUT = food("rainbow_trout", 2, 0.1F);
    public static final DeferredItem<Item> COOKED_RAINBOW_TROUT = food("cooked_rainbow_trout", 7, 0.8F);
    public static final DeferredItem<Item> GOLDEN_CARP = ITEMS.registerSimpleItem("golden_carp",
            p -> p.rarity(Rarity.RARE).food(new FoodProperties.Builder().nutrition(4).saturationModifier(1.2F).alwaysEdible().build(),
                    effect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1),
                            new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1))));
    public static final DeferredItem<Item> MOONLIGHT_FISH = ITEMS.registerSimpleItem("moonlight_fish",
            p -> p.rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build(),
                    effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 180, 0))));
    public static final DeferredItem<Item> STORM_EEL = ITEMS.registerSimpleItem("storm_eel",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build(),
                    effect(new MobEffectInstance(MobEffects.SPEED, 20 * 30, 0))));
    public static final DeferredItem<Item> GRILLED_EEL = ITEMS.registerSimpleItem("grilled_eel",
            p -> p.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9F).build(),
                    effect(new MobEffectInstance(MobEffects.SPEED, 20 * 120, 0))));
    public static final DeferredItem<Item> ANGLERFISH = ITEMS.registerSimpleItem("anglerfish",
            p -> p.rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build(),
                    effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 180, 0))));
    public static final DeferredItem<Item> ICE_SMELT = ITEMS.registerSimpleItem("ice_smelt",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build(),
                    effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60, 0))));
    public static final DeferredItem<SunkenChestItem> SUNKEN_CHEST = ITEMS.registerItem("sunken_chest",
            SunkenChestItem::new, p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SASHIMI_PLATTER = ITEMS.registerSimpleItem("sashimi_platter",
            p -> p.stacksTo(1).usingConvertsTo(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build(),
                            effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 120, 0),
                                    new MobEffectInstance(MobEffects.LUCK, 20 * 120, 0))));
    public static final DeferredItem<Item> FISH_STEW = ITEMS.registerSimpleItem("fish_stew",
            p -> p.stacksTo(1).usingConvertsTo(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(12).saturationModifier(1.2F).build(),
                            effect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 120, 0),
                                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0))));

    // ───────── 농사 ─────────
    public static final DeferredItem<BlockItem> TOMATO_SEEDS = ITEMS.registerItem("tomato_seeds",
            p -> new BlockItem(ModBlocks.TOMATO_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> TOMATO = food("tomato", 3, 0.3F);
    public static final DeferredItem<BlockItem> STRAWBERRY = ITEMS.registerItem("strawberry",
            p -> new BlockItem(ModBlocks.STRAWBERRY_CROP.get(), p.useItemDescriptionPrefix()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build())));
    public static final DeferredItem<BlockItem> SPRINKLER = ITEMS.registerSimpleBlockItem("sprinkler", ModBlocks.SPRINKLER);
    public static final DeferredItem<FertilizerItem> FERTILIZER = ITEMS.registerItem("fertilizer", FertilizerItem::new);
    public static final DeferredItem<Item> TOMATO_SOUP = ITEMS.registerSimpleItem("tomato_soup",
            p -> p.stacksTo(1).usingConvertsTo(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build(),
                            effect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 0))));
    public static final DeferredItem<Item> PIZZA = food("pizza", 7, 0.9F);
    public static final DeferredItem<Item> STRAWBERRY_TART = ITEMS.registerSimpleItem("strawberry_tart",
            p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build(),
                    effect(new MobEffectInstance(MobEffects.SPEED, 20 * 45, 0))));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name,
                p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** 클래스 로딩(=아이템 등록)을 보장하기 위해 모드 생성자에서 호출 */
    public static void init() {}

    private ModItems2() {}
}
