package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.item.LuckyCharmItem;
import com.snolred.wildplus.item.RecallScrollItem;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WildPlus.MODID);

    // ───────── 태그 ─────────
    public static final TagKey<Item> BLOODSTEEL_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("bloodsteel_repair"));

    // ───────── 재료 ─────────
    /** 블러드문·정예 몹에게서 얻는 재료 */
    public static final DeferredItem<Item> BLOOD_SHARD = ITEMS.registerSimpleItem("blood_shard",
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> BLOODSTEEL_INGOT = ITEMS.registerSimpleItem("bloodsteel_ingot",
            p -> p.rarity(Rarity.UNCOMMON));

    // ───────── 핏빛 강철 장비 ─────────
    /** 철과 다이아몬드 사이. 내구도는 다이아몬드보다 낮지만 인챈트가 잘 붙음 */
    public static final ToolMaterial BLOODSTEEL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1100, 7.5F, 2.5F, 18, BLOODSTEEL_REPAIR);

    public static final ResourceKey<EquipmentAsset> BLOODSTEEL_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("bloodsteel"));

    public static final ArmorMaterial BLOODSTEEL_ARMOR = new ArmorMaterial(
            28,
            make(),
            18,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.0F,
            0.0F,
            BLOODSTEEL_REPAIR,
            BLOODSTEEL_ASSET);

    private static EnumMap<ArmorType, Integer> make() {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, 3);
        map.put(ArmorType.LEGGINGS, 6);
        map.put(ArmorType.CHESTPLATE, 7);
        map.put(ArmorType.HELMET, 3);
        map.put(ArmorType.BODY, 9);
        return map;
    }

    public static final DeferredItem<Item> BLOODSTEEL_SWORD = ITEMS.registerItem("bloodsteel_sword",
            p -> new Item(p.sword(BLOODSTEEL, 3.0F, -2.4F)));
    public static final DeferredItem<Item> BLOODSTEEL_PICKAXE = ITEMS.registerItem("bloodsteel_pickaxe",
            p -> new Item(p.pickaxe(BLOODSTEEL, 1.0F, -2.8F)));
    public static final DeferredItem<Item> BLOODSTEEL_AXE = ITEMS.registerItem("bloodsteel_axe",
            p -> new Item(p.axe(BLOODSTEEL, 5.5F, -3.0F)));
    public static final DeferredItem<Item> BLOODSTEEL_SHOVEL = ITEMS.registerItem("bloodsteel_shovel",
            p -> new Item(p.shovel(BLOODSTEEL, 1.5F, -3.0F)));

    public static final DeferredItem<Item> BLOODSTEEL_HELMET = ITEMS.registerItem("bloodsteel_helmet",
            p -> new Item(p.humanoidArmor(BLOODSTEEL_ARMOR, ArmorType.HELMET)));
    public static final DeferredItem<Item> BLOODSTEEL_CHESTPLATE = ITEMS.registerItem("bloodsteel_chestplate",
            p -> new Item(p.humanoidArmor(BLOODSTEEL_ARMOR, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> BLOODSTEEL_LEGGINGS = ITEMS.registerItem("bloodsteel_leggings",
            p -> new Item(p.humanoidArmor(BLOODSTEEL_ARMOR, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> BLOODSTEEL_BOOTS = ITEMS.registerItem("bloodsteel_boots",
            p -> new Item(p.humanoidArmor(BLOODSTEEL_ARMOR, ArmorType.BOOTS)));

    // ───────── 탐험 보물 ─────────
    public static final DeferredItem<RecallScrollItem> RECALL_SCROLL = ITEMS.registerItem("recall_scroll",
            RecallScrollItem::new, p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<LuckyCharmItem> LUCKY_CHARM = ITEMS.registerItem("lucky_charm",
            LuckyCharmItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> TRAVELER_RATION = ITEMS.registerSimpleItem("traveler_ration",
            p -> p.food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.0F).build(),
                    Consumables.defaultFood()
                            .consumeSeconds(0.8F)
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0)))
                            .build()));

    // ───────── 고추 농사·요리 ─────────
    public static final DeferredItem<BlockItem> CHILI_SEEDS = ITEMS.registerItem("chili_seeds",
            p -> new BlockItem(ModBlocks.CHILI_CROP.get(), p.useItemDescriptionPrefix()));

    public static final DeferredItem<Item> CHILI = ITEMS.registerSimpleItem("chili",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build(),
                    effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 0))));

    public static final DeferredItem<Item> SPICY_STEW = ITEMS.registerSimpleItem("spicy_stew",
            p -> p.stacksTo(1).usingConvertsTo(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0F).build(),
                            effect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 90, 0),
                                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 90, 0))));

    public static final DeferredItem<Item> HONEY_TOAST = ITEMS.registerSimpleItem("honey_toast",
            p -> p.food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build(),
                    effect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0))));

    public static final DeferredItem<Item> RAW_SKEWER = ITEMS.registerSimpleItem("raw_skewer",
            p -> p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));

    public static final DeferredItem<Item> SPICY_SKEWER = ITEMS.registerSimpleItem("spicy_skewer",
            p -> p.food(new FoodProperties.Builder().nutrition(9).saturationModifier(1.2F).build(),
                    effect(new MobEffectInstance(MobEffects.HASTE, 20 * 60, 0))));

    private static Consumable effect(MobEffectInstance... effects) {
        Consumable.Builder builder = Consumables.defaultFood();
        for (MobEffectInstance e : effects) {
            builder.onConsume(new ApplyStatusEffectsConsumeEffect(e));
        }
        return builder.build();
    }

    private ModItems() {}
}
