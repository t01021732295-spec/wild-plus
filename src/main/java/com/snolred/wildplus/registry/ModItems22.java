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

/** 4.0 공허석 재료와 공허 장비. 제작대에서 미완성 장비 → 강화대에서 공허 심장 + 돈으로 완성. 효과는 event.VoidGear */
public final class ModItems22 {
    public static final TagKey<Item> VOID_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("void_repair"));

    public static final DeferredItem<BlockItem> VOID_ORE = ITEMS.registerSimpleBlockItem("void_ore", ModBlocks11.VOID_ORE);
    public static final DeferredItem<BlockItem> VOID_BLOCK = ITEMS.registerSimpleBlockItem("void_block", ModBlocks11.VOID_BLOCK);
    public static final DeferredItem<BlockItem> VOID_BRICK = ITEMS.registerSimpleBlockItem("void_brick", ModBlocks11.VOID_BRICK);
    public static final DeferredItem<Item> VOID_SHARD = ITEMS.registerSimpleItem("void_shard", p -> p.rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> RAW_VOID_STONE = ITEMS.registerSimpleItem("raw_void_stone", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> VOID_STONE = ITEMS.registerSimpleItem("void_stone", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> VOID_HEART = ITEMS.registerSimpleItem("void_heart",
            p -> p.rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    /** 스텔라 루비 · 스카이 사파이어 · 로열 에메랄드와 같은 단계 */
    public static final ToolMaterial VOID_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.5F, 5.0F, 26, VOID_REPAIR);

    private static Item.Properties full(Item.Properties p) {
        return p.fireResistant().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    private static DeferredItem<Item> blank(String name) {
        return ITEMS.registerSimpleItem("void_" + name + "_blank", p -> p.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    /** 공격력 10.5 · 속도 1.9 */
    public static final DeferredItem<Item> VOID_SWORD = ITEMS.registerItem("void_sword", p -> new Item(full(p.sword(VOID_TOOL, 4.5F, -2.1F))));
    public static final DeferredItem<Item> VOID_PICKAXE = ITEMS.registerItem("void_pickaxe", p -> new Item(full(p.pickaxe(VOID_TOOL, 1.0F, -2.8F))));
    public static final DeferredItem<Item> VOID_AXE = ITEMS.registerItem("void_axe", p -> new Item(full(p.axe(VOID_TOOL, 5.0F, -3.0F))));
    public static final DeferredItem<Item> VOID_SHOVEL = ITEMS.registerItem("void_shovel", p -> new Item(full(p.shovel(VOID_TOOL, 1.5F, -3.0F))));
    public static final DeferredItem<Item> VOID_HOE = ITEMS.registerItem("void_hoe", p -> new Item(full(p.hoe(VOID_TOOL, -4.0F, 0.0F))));

    public static final ResourceKey<EquipmentAsset> VOID_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("void"));
    public static final ArmorMaterial VOID_ARMOR = new ArmorMaterial(50, defense(4, 8, 10, 4, 13), 26,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.2F, VOID_REPAIR, VOID_ASSET);

    public static final DeferredItem<Item> VOID_HELMET = ITEMS.registerItem("void_helmet", p -> new Item(full(p.humanoidArmor(VOID_ARMOR, ArmorType.HELMET))));
    public static final DeferredItem<Item> VOID_CHESTPLATE = ITEMS.registerItem("void_chestplate",
            p -> new Item(full(p.humanoidArmor(VOID_ARMOR, ArmorType.CHESTPLATE)).component(DataComponents.GLIDER, Unit.INSTANCE)));
    public static final DeferredItem<Item> VOID_LEGGINGS = ITEMS.registerItem("void_leggings", p -> new Item(full(p.humanoidArmor(VOID_ARMOR, ArmorType.LEGGINGS))));
    public static final DeferredItem<Item> VOID_BOOTS = ITEMS.registerItem("void_boots", p -> new Item(full(p.humanoidArmor(VOID_ARMOR, ArmorType.BOOTS))));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> VOID_BOW = ITEMS.registerItem("void_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(full(p.durability(4000).enchantable(1)), "void", 2.0F, 1.8F));

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
        m.put(SWORD_BLANK.get(), VOID_SWORD.get());
        m.put(PICKAXE_BLANK.get(), VOID_PICKAXE.get());
        m.put(AXE_BLANK.get(), VOID_AXE.get());
        m.put(SHOVEL_BLANK.get(), VOID_SHOVEL.get());
        m.put(HOE_BLANK.get(), VOID_HOE.get());
        m.put(BOW_BLANK.get(), VOID_BOW.get());
        m.put(HELMET_BLANK.get(), VOID_HELMET.get());
        m.put(CHESTPLATE_BLANK.get(), VOID_CHESTPLATE.get());
        m.put(LEGGINGS_BLANK.get(), VOID_LEGGINGS.get());
        m.put(BOOTS_BLANK.get(), VOID_BOOTS.get());
        return m;
    }

    public static void init() {}

    private ModItems22() {}
}
