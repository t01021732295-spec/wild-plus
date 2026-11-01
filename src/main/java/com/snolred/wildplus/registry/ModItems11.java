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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 2.9: 사파이어 (루비의 반대편 — 불 대신 얼음·물, 공격 대신 방어·기동).
 * 용의 깃털(드래곤의 숨결 + 겉날개)로 강화대에서 스카이 사파이어로 승급.
 */
public final class ModItems11 {
    public static final TagKey<Item> SAPPHIRE_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("sapphire_repair"));

    public static final DeferredItem<BlockItem> SAPPHIRE_ORE = ITEMS.registerSimpleBlockItem("sapphire_ore", ModBlocks2.SAPPHIRE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_SAPPHIRE_ORE = ITEMS.registerSimpleBlockItem("deepslate_sapphire_ore", ModBlocks2.DEEPSLATE_SAPPHIRE_ORE);
    public static final DeferredItem<BlockItem> SAPPHIRE_BLOCK = ITEMS.registerSimpleBlockItem("sapphire_block", ModBlocks2.SAPPHIRE_BLOCK);
    public static final DeferredItem<Item> SAPPHIRE = ITEMS.registerSimpleItem("sapphire", p -> p.rarity(Rarity.UNCOMMON));
    /** 용의 깃털: 드래곤의 숨결 + 겉날개 */
    public static final DeferredItem<Item> DRAGON_PLUME = ITEMS.registerSimpleItem("dragon_plume",
            p -> p.rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // ───────── 사파이어 장비 ─────────
    /** 루비(1800 · 9.5 · 3.5 · 18)보다 단단하고 마법이 잘 붙지만 덜 아프다 */
    public static final ToolMaterial SAPPHIRE_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2200, 9.0F, 3.0F, 22, SAPPHIRE_REPAIR);

    public static final DeferredItem<Item> SAPPHIRE_SWORD = ITEMS.registerItem("sapphire_sword",
            p -> new Item(p.sword(SAPPHIRE_TOOL, 3.0F, -2.2F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_PICKAXE = ITEMS.registerItem("sapphire_pickaxe",
            p -> new Item(p.pickaxe(SAPPHIRE_TOOL, 1.0F, -2.8F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_AXE = ITEMS.registerItem("sapphire_axe",
            p -> new Item(p.axe(SAPPHIRE_TOOL, 5.0F, -3.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_SHOVEL = ITEMS.registerItem("sapphire_shovel",
            p -> new Item(p.shovel(SAPPHIRE_TOOL, 1.5F, -3.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_HOE = ITEMS.registerItem("sapphire_hoe",
            p -> new Item(p.hoe(SAPPHIRE_TOOL, -3.0F, 0.0F).rarity(Rarity.UNCOMMON)));

    public static final ResourceKey<EquipmentAsset> SAPPHIRE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("sapphire"));
    public static final ArmorMaterial SAPPHIRE_ARMOR = new ArmorMaterial(38, defense(3, 7, 8, 3, 11), 22,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 3.5F, 0.1F, SAPPHIRE_REPAIR, SAPPHIRE_ASSET);

    public static final DeferredItem<Item> SAPPHIRE_HELMET = ITEMS.registerItem("sapphire_helmet",
            p -> new Item(p.humanoidArmor(SAPPHIRE_ARMOR, ArmorType.HELMET).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_CHESTPLATE = ITEMS.registerItem("sapphire_chestplate",
            p -> new Item(p.humanoidArmor(SAPPHIRE_ARMOR, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_LEGGINGS = ITEMS.registerItem("sapphire_leggings",
            p -> new Item(p.humanoidArmor(SAPPHIRE_ARMOR, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SAPPHIRE_BOOTS = ITEMS.registerItem("sapphire_boots",
            p -> new Item(p.humanoidArmor(SAPPHIRE_ARMOR, ArmorType.BOOTS).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> SAPPHIRE_BOW = ITEMS.registerItem("sapphire_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(p.durability(1300).enchantable(1).rarity(Rarity.RARE), "sapphire", 1.4F, 1.25F));

    // ───────── 스카이 사파이어 (내구도 무한 · 불에 안 탐) ─────────
    public static final ToolMaterial SKY_SAPPHIRE_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.0F, 4.0F, 26,
            SAPPHIRE_REPAIR);

    private static Item.Properties sky(Item.Properties p) {
        return p.fireResistant().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    /** 검: 공격력 8 · 공격 속도 2.0 (스타 루비 9 · 1.6 보다 빠름) */
    public static final DeferredItem<Item> SKY_SAPPHIRE_SWORD = ITEMS.registerItem("sky_sapphire_sword",
            p -> new Item(sky(p.sword(SKY_SAPPHIRE_TOOL, 3.0F, -2.0F))));
    public static final DeferredItem<Item> SKY_SAPPHIRE_PICKAXE = ITEMS.registerItem("sky_sapphire_pickaxe",
            p -> new Item(sky(p.pickaxe(SKY_SAPPHIRE_TOOL, 1.0F, -2.8F))));
    public static final DeferredItem<Item> SKY_SAPPHIRE_AXE = ITEMS.registerItem("sky_sapphire_axe",
            p -> new Item(sky(p.axe(SKY_SAPPHIRE_TOOL, 5.0F, -3.0F))));
    public static final DeferredItem<Item> SKY_SAPPHIRE_SHOVEL = ITEMS.registerItem("sky_sapphire_shovel",
            p -> new Item(sky(p.shovel(SKY_SAPPHIRE_TOOL, 1.5F, -3.0F))));
    public static final DeferredItem<Item> SKY_SAPPHIRE_HOE = ITEMS.registerItem("sky_sapphire_hoe",
            p -> new Item(sky(p.hoe(SKY_SAPPHIRE_TOOL, -4.0F, 0.0F))));

    public static final ResourceKey<EquipmentAsset> SKY_SAPPHIRE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("sky_sapphire"));
    public static final ArmorMaterial SKY_SAPPHIRE_ARMOR = new ArmorMaterial(50, defense(4, 7, 9, 4, 13), 26,
            SoundEvents.ARMOR_EQUIP_ELYTRA, 5.0F, 0.2F, SAPPHIRE_REPAIR, SKY_SAPPHIRE_ASSET);

    public static final DeferredItem<Item> SKY_SAPPHIRE_HELMET = ITEMS.registerItem("sky_sapphire_helmet",
            p -> new Item(sky(p.humanoidArmor(SKY_SAPPHIRE_ARMOR, ArmorType.HELMET))));
    /** 흉갑은 겉날개 겸용 (폭죽 가속도 됨) */
    public static final DeferredItem<Item> SKY_SAPPHIRE_CHESTPLATE = ITEMS.registerItem("sky_sapphire_chestplate",
            p -> new Item(sky(p.humanoidArmor(SKY_SAPPHIRE_ARMOR, ArmorType.CHESTPLATE)).component(DataComponents.GLIDER, Unit.INSTANCE)));
    public static final DeferredItem<Item> SKY_SAPPHIRE_LEGGINGS = ITEMS.registerItem("sky_sapphire_leggings",
            p -> new Item(sky(p.humanoidArmor(SKY_SAPPHIRE_ARMOR, ArmorType.LEGGINGS))));
    public static final DeferredItem<Item> SKY_SAPPHIRE_BOOTS = ITEMS.registerItem("sky_sapphire_boots",
            p -> new Item(sky(p.humanoidArmor(SKY_SAPPHIRE_ARMOR, ArmorType.BOOTS))));

    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> SKY_SAPPHIRE_BOW = ITEMS.registerItem("sky_sapphire_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(sky(p.durability(4000).enchantable(1)), "sky_sapphire", 1.75F, 1.5F));

    // ───────── 사과 ─────────
    /** 사파이어: 수중 호흡 · 돌고래의 가호 3분, 저항 I · 흡수 I 1분 */
    public static final DeferredItem<Item> SAPPHIRE_APPLE = ModItems10.apple("sapphire_apple", Rarity.EPIC, true, 4, 1.2F,
            ModItems10.e(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 180, 0),
            ModItems10.e(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 180, 0),
            ModItems10.e(net.minecraft.world.effect.MobEffects.RESISTANCE, 60, 0),
            ModItems10.e(net.minecraft.world.effect.MobEffects.ABSORPTION, 60, 0));
    /** 스카이 사파이어: 저항 II · 신속 II 1분, 흡수 IV 3분, 느린 낙하 · 전달체의 힘 · 수중 호흡 5분 */
    public static final DeferredItem<Item> SKY_SAPPHIRE_APPLE = ModItems10.apple("sky_sapphire_apple", Rarity.EPIC, true, 8, 1.6F,
            ModItems10.e(net.minecraft.world.effect.MobEffects.RESISTANCE, 60, 1),
            ModItems10.e(net.minecraft.world.effect.MobEffects.SPEED, 60, 1),
            ModItems10.e(net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 3),
            ModItems10.e(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 300, 0),
            ModItems10.e(net.minecraft.world.effect.MobEffects.CONDUIT_POWER, 300, 0),
            ModItems10.e(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 300, 0));

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    /** 사파이어 → 스카이 사파이어 짝 */
    public static Map<Item, Item> upgrades() {
        Map<Item, Item> m = new LinkedHashMap<>();
        m.put(SAPPHIRE_SWORD.get(), SKY_SAPPHIRE_SWORD.get());
        m.put(SAPPHIRE_PICKAXE.get(), SKY_SAPPHIRE_PICKAXE.get());
        m.put(SAPPHIRE_AXE.get(), SKY_SAPPHIRE_AXE.get());
        m.put(SAPPHIRE_SHOVEL.get(), SKY_SAPPHIRE_SHOVEL.get());
        m.put(SAPPHIRE_HOE.get(), SKY_SAPPHIRE_HOE.get());
        m.put(SAPPHIRE_HELMET.get(), SKY_SAPPHIRE_HELMET.get());
        m.put(SAPPHIRE_CHESTPLATE.get(), SKY_SAPPHIRE_CHESTPLATE.get());
        m.put(SAPPHIRE_LEGGINGS.get(), SKY_SAPPHIRE_LEGGINGS.get());
        m.put(SAPPHIRE_BOOTS.get(), SKY_SAPPHIRE_BOOTS.get());
        m.put(SAPPHIRE_BOW.get(), SKY_SAPPHIRE_BOW.get());
        return m;
    }

    public static void init() {}

    private ModItems11() {}
}
