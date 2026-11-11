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

/** 5.0 겁화(劫火) 장비 — 공허와 같은 단계. 제작대에서 미완성 장비 → 강화대에서 군주의 뿔 + 돈으로 완성. 효과는 event.KalpaGear */
public final class ModItems34 {
    public static final TagKey<Item> KALPA_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("kalpa_repair"));

    public static final DeferredItem<BlockItem> KALPA_ORE = ITEMS.registerSimpleBlockItem("kalpa_ore", ModBlocks21.KALPA_ORE);
    public static final DeferredItem<BlockItem> KALPA_BLOCK = ITEMS.registerSimpleBlockItem("kalpa_block", ModBlocks21.KALPA_BLOCK);
    public static final DeferredItem<Item> EMBER_SHARD = ITEMS.registerSimpleItem("ember_shard", p -> p.rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> RAW_KALPA = ITEMS.registerSimpleItem("raw_kalpa", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> KALPA_STONE = ITEMS.registerSimpleItem("kalpa_stone", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> KALPA_HORN = ITEMS.registerSimpleItem("kalpa_horn",
            p -> p.rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final ToolMaterial KALPA_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.5F, 5.0F, 26, KALPA_REPAIR);

    private static Item.Properties full(Item.Properties p) {
        return p.fireResistant().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    private static DeferredItem<Item> blank(String name) {
        return ITEMS.registerSimpleItem("kalpa_" + name + "_blank", p -> p.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    /** 공격력 10.5 · 속도 1.9 (공허 검과 같음) */
    public static final DeferredItem<Item> KALPA_SWORD = ITEMS.registerItem("kalpa_sword", p -> new Item(full(p.sword(KALPA_TOOL, 4.5F, -2.1F))));
    public static final DeferredItem<Item> KALPA_PICKAXE = ITEMS.registerItem("kalpa_pickaxe", p -> new Item(full(p.pickaxe(KALPA_TOOL, 1.0F, -2.8F))));
    public static final DeferredItem<Item> KALPA_AXE = ITEMS.registerItem("kalpa_axe", p -> new Item(full(p.axe(KALPA_TOOL, 5.0F, -3.0F))));
    public static final DeferredItem<Item> KALPA_SHOVEL = ITEMS.registerItem("kalpa_shovel", p -> new Item(full(p.shovel(KALPA_TOOL, 1.5F, -3.0F))));
    public static final DeferredItem<Item> KALPA_HOE = ITEMS.registerItem("kalpa_hoe", p -> new Item(full(p.hoe(KALPA_TOOL, -4.0F, 0.0F))));

    public static final ResourceKey<EquipmentAsset> KALPA_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("kalpa"));
    public static final ArmorMaterial KALPA_ARMOR = new ArmorMaterial(50, defense(4, 8, 10, 4, 13), 26,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.2F, KALPA_REPAIR, KALPA_ASSET);

    public static final DeferredItem<Item> KALPA_HELMET = ITEMS.registerItem("kalpa_helmet", p -> new Item(full(p.humanoidArmor(KALPA_ARMOR, ArmorType.HELMET))));
    public static final DeferredItem<Item> KALPA_CHESTPLATE = ITEMS.registerItem("kalpa_chestplate", p -> new Item(full(p.humanoidArmor(KALPA_ARMOR, ArmorType.CHESTPLATE))));
    public static final DeferredItem<Item> KALPA_LEGGINGS = ITEMS.registerItem("kalpa_leggings", p -> new Item(full(p.humanoidArmor(KALPA_ARMOR, ArmorType.LEGGINGS))));
    public static final DeferredItem<Item> KALPA_BOOTS = ITEMS.registerItem("kalpa_boots", p -> new Item(full(p.humanoidArmor(KALPA_ARMOR, ArmorType.BOOTS))));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> KALPA_BOW = ITEMS.registerItem("kalpa_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(full(p.durability(4000).enchantable(1)), "kalpa", 2.0F, 1.8F));

    public static final DeferredItem<Item> SWORD_BLANK = blank("sword"), PICKAXE_BLANK = blank("pickaxe"), AXE_BLANK = blank("axe"), SHOVEL_BLANK = blank("shovel"),
            HOE_BLANK = blank("hoe"), BOW_BLANK = blank("bow"), HELMET_BLANK = blank("helmet"), CHESTPLATE_BLANK = blank("chestplate"),
            LEGGINGS_BLANK = blank("leggings"), BOOTS_BLANK = blank("boots");

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    /** 미완성 → 완성 */
    public static Map<Item, Item> upgrades() {
        Map<Item, Item> m = new LinkedHashMap<>();
        m.put(SWORD_BLANK.get(), KALPA_SWORD.get());
        m.put(PICKAXE_BLANK.get(), KALPA_PICKAXE.get());
        m.put(AXE_BLANK.get(), KALPA_AXE.get());
        m.put(SHOVEL_BLANK.get(), KALPA_SHOVEL.get());
        m.put(HOE_BLANK.get(), KALPA_HOE.get());
        m.put(BOW_BLANK.get(), KALPA_BOW.get());
        m.put(HELMET_BLANK.get(), KALPA_HELMET.get());
        m.put(CHESTPLATE_BLANK.get(), KALPA_CHESTPLATE.get());
        m.put(LEGGINGS_BLANK.get(), KALPA_LEGGINGS.get());
        m.put(BOOTS_BLANK.get(), KALPA_BOOTS.get());
        return m;
    }

    public static void init() {}

    private ModItems34() {}
}
