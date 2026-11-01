package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.WildPlus;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 2.12: 에메랄드 장비 (풍요 · 행운 · 생명) → 생명의 결정으로 로열 에메랄드 승급.
 */
public final class ModItems12 {
    public static final TagKey<Item> EMERALD_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("emerald_repair"));

    /** 생명의 결정: 불사의 토템 + 바다의 심장 + 에메랄드 블록 2 → 2개 */
    public static final DeferredItem<Item> LIFE_CRYSTAL = ITEMS.registerSimpleItem("life_crystal",
            p -> p.rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    /** 마법이 가장 잘 붙는 재질 (인챈트 수치 25) */
    public static final ToolMaterial EMERALD_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2000, 9.0F, 3.0F, 25, EMERALD_REPAIR);

    public static final DeferredItem<Item> EMERALD_SWORD = ITEMS.registerItem("emerald_sword",
            p -> new Item(p.sword(EMERALD_TOOL, 3.0F, -2.3F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_PICKAXE = ITEMS.registerItem("emerald_pickaxe",
            p -> new Item(p.pickaxe(EMERALD_TOOL, 1.0F, -2.8F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_AXE = ITEMS.registerItem("emerald_axe",
            p -> new Item(p.axe(EMERALD_TOOL, 5.0F, -3.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_SHOVEL = ITEMS.registerItem("emerald_shovel",
            p -> new Item(p.shovel(EMERALD_TOOL, 1.5F, -3.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_HOE = ITEMS.registerItem("emerald_hoe",
            p -> new Item(p.hoe(EMERALD_TOOL, -3.0F, 0.0F).rarity(Rarity.UNCOMMON)));

    public static final ResourceKey<EquipmentAsset> EMERALD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("emerald"));
    public static final ArmorMaterial EMERALD_ARMOR = new ArmorMaterial(36, defense(3, 6, 8, 3, 11), 25,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.08F, EMERALD_REPAIR, EMERALD_ASSET);

    public static final DeferredItem<Item> EMERALD_HELMET = ITEMS.registerItem("emerald_helmet",
            p -> new Item(p.humanoidArmor(EMERALD_ARMOR, ArmorType.HELMET).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_CHESTPLATE = ITEMS.registerItem("emerald_chestplate",
            p -> new Item(p.humanoidArmor(EMERALD_ARMOR, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_LEGGINGS = ITEMS.registerItem("emerald_leggings",
            p -> new Item(p.humanoidArmor(EMERALD_ARMOR, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EMERALD_BOOTS = ITEMS.registerItem("emerald_boots",
            p -> new Item(p.humanoidArmor(EMERALD_ARMOR, ArmorType.BOOTS).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> EMERALD_BOW = ITEMS.registerItem("emerald_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(p.durability(1200).enchantable(1).rarity(Rarity.RARE), "emerald", 1.3F, 1.25F));

    // ───────── 로열 에메랄드 ─────────
    public static final ToolMaterial ROYAL_EMERALD_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.0F, 4.5F, 30, EMERALD_REPAIR);

    private static Item.Properties royal(Item.Properties p) {
        return p.fireResistant().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    /** 검: 공격력 8.5 · 공격 속도 1.8 */
    public static final DeferredItem<Item> ROYAL_EMERALD_SWORD = ITEMS.registerItem("royal_emerald_sword",
            p -> new Item(royal(p.sword(ROYAL_EMERALD_TOOL, 3.0F, -2.2F))));
    public static final DeferredItem<Item> ROYAL_EMERALD_PICKAXE = ITEMS.registerItem("royal_emerald_pickaxe",
            p -> new Item(royal(p.pickaxe(ROYAL_EMERALD_TOOL, 1.0F, -2.8F))));
    public static final DeferredItem<Item> ROYAL_EMERALD_AXE = ITEMS.registerItem("royal_emerald_axe",
            p -> new Item(royal(p.axe(ROYAL_EMERALD_TOOL, 5.0F, -3.0F))));
    public static final DeferredItem<Item> ROYAL_EMERALD_SHOVEL = ITEMS.registerItem("royal_emerald_shovel",
            p -> new Item(royal(p.shovel(ROYAL_EMERALD_TOOL, 1.5F, -3.0F))));
    public static final DeferredItem<Item> ROYAL_EMERALD_HOE = ITEMS.registerItem("royal_emerald_hoe",
            p -> new Item(royal(p.hoe(ROYAL_EMERALD_TOOL, -4.5F, 0.0F))));

    public static final ResourceKey<EquipmentAsset> ROYAL_EMERALD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("royal_emerald"));
    public static final ArmorMaterial ROYAL_EMERALD_ARMOR = new ArmorMaterial(50, defense(4, 7, 9, 4, 13), 30,
            SoundEvents.ARMOR_EQUIP_GOLD, 4.5F, 0.15F, EMERALD_REPAIR, ROYAL_EMERALD_ASSET);

    public static final DeferredItem<Item> ROYAL_EMERALD_HELMET = ITEMS.registerItem("royal_emerald_helmet",
            p -> new Item(royal(p.humanoidArmor(ROYAL_EMERALD_ARMOR, ArmorType.HELMET))));
    public static final DeferredItem<Item> ROYAL_EMERALD_CHESTPLATE = ITEMS.registerItem("royal_emerald_chestplate",
            p -> new Item(royal(p.humanoidArmor(ROYAL_EMERALD_ARMOR, ArmorType.CHESTPLATE))));
    public static final DeferredItem<Item> ROYAL_EMERALD_LEGGINGS = ITEMS.registerItem("royal_emerald_leggings",
            p -> new Item(royal(p.humanoidArmor(ROYAL_EMERALD_ARMOR, ArmorType.LEGGINGS))));
    public static final DeferredItem<Item> ROYAL_EMERALD_BOOTS = ITEMS.registerItem("royal_emerald_boots",
            p -> new Item(royal(p.humanoidArmor(ROYAL_EMERALD_ARMOR, ArmorType.BOOTS))));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> ROYAL_EMERALD_BOW = ITEMS.registerItem("royal_emerald_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(royal(p.durability(4000).enchantable(1)), "royal_emerald", 1.6F, 1.5F));

    /** 로열 에메랄드 사과: 재생 II 1분 · 흡수 IV 3분 · 행운 II · 마을의 영웅 III · 체력 증가 II 5분 */
    public static final DeferredItem<Item> ROYAL_EMERALD_APPLE = ModItems10.apple("royal_emerald_apple", Rarity.EPIC, true, 8, 1.6F,
            ModItems10.e(MobEffects.REGENERATION, 60, 1), ModItems10.e(MobEffects.ABSORPTION, 180, 3),
            ModItems10.e(MobEffects.LUCK, 300, 1), ModItems10.e(MobEffects.HERO_OF_THE_VILLAGE, 300, 2),
            ModItems10.e(MobEffects.HEALTH_BOOST, 300, 1));

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    public static Map<Item, Item> upgrades() {
        Map<Item, Item> m = new LinkedHashMap<>();
        m.put(EMERALD_SWORD.get(), ROYAL_EMERALD_SWORD.get());
        m.put(EMERALD_PICKAXE.get(), ROYAL_EMERALD_PICKAXE.get());
        m.put(EMERALD_AXE.get(), ROYAL_EMERALD_AXE.get());
        m.put(EMERALD_SHOVEL.get(), ROYAL_EMERALD_SHOVEL.get());
        m.put(EMERALD_HOE.get(), ROYAL_EMERALD_HOE.get());
        m.put(EMERALD_HELMET.get(), ROYAL_EMERALD_HELMET.get());
        m.put(EMERALD_CHESTPLATE.get(), ROYAL_EMERALD_CHESTPLATE.get());
        m.put(EMERALD_LEGGINGS.get(), ROYAL_EMERALD_LEGGINGS.get());
        m.put(EMERALD_BOOTS.get(), ROYAL_EMERALD_BOOTS.get());
        m.put(EMERALD_BOW.get(), ROYAL_EMERALD_BOW.get());
        return m;
    }

    public static void init() {}

    private ModItems12() {}
}
