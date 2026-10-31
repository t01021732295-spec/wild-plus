package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.item.EventBottleItem;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
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
 * 13차: 루비 (다이아몬드와 네더라이트 사이) 광석 · 도구 · 갑옷, 이벤트 투척병.
 * 루비 장비는 모두 불에 타지 않고, 부위마다 특수 능력이 있다 (RubyGear).
 */
public final class ModItems7 {
    public static final TagKey<Item> RUBY_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("ruby_repair"));

    public static final DeferredItem<BlockItem> RUBY_ORE = ITEMS.registerSimpleBlockItem("ruby_ore", ModBlocks2.RUBY_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_RUBY_ORE = ITEMS.registerSimpleBlockItem("deepslate_ruby_ore", ModBlocks2.DEEPSLATE_RUBY_ORE);
    public static final DeferredItem<BlockItem> RUBY_BLOCK = ITEMS.registerSimpleBlockItem("ruby_block", ModBlocks2.RUBY_BLOCK);
    public static final DeferredItem<Item> RUBY = ITEMS.registerSimpleItem("ruby", p -> p.rarity(Rarity.UNCOMMON).fireResistant());

    /** 내구도 1800 · 속도 9.5 · 공격 +3.5 · 인챈트 18 (다이아 1561/8/3/10, 네더라이트 2031/9/4/15) */
    public static final ToolMaterial RUBY_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 9.5F, 3.5F, 18, RUBY_REPAIR);

    public static final DeferredItem<Item> RUBY_SWORD = ITEMS.registerItem("ruby_sword",
            p -> new Item(p.sword(RUBY_TOOL, 3.0F, -2.4F).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_PICKAXE = ITEMS.registerItem("ruby_pickaxe",
            p -> new Item(p.pickaxe(RUBY_TOOL, 1.0F, -2.8F).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_AXE = ITEMS.registerItem("ruby_axe",
            p -> new Item(p.axe(RUBY_TOOL, 5.0F, -3.0F).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_SHOVEL = ITEMS.registerItem("ruby_shovel",
            p -> new Item(p.shovel(RUBY_TOOL, 1.5F, -3.0F).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_HOE = ITEMS.registerItem("ruby_hoe",
            p -> new Item(p.hoe(RUBY_TOOL, -3.5F, 0.0F).fireResistant().rarity(Rarity.UNCOMMON)));

    public static final ResourceKey<EquipmentAsset> RUBY_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("ruby"));
    /** 다이아몬드와 같은 방어 + 강인함 3 · 밀치기 저항 0.08 */
    public static final ArmorMaterial RUBY_ARMOR = new ArmorMaterial(35, defense(3, 6, 8, 3, 11), 18,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.08F, RUBY_REPAIR, RUBY_ASSET);

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    public static final DeferredItem<Item> RUBY_HELMET = ITEMS.registerItem("ruby_helmet",
            p -> new Item(p.humanoidArmor(RUBY_ARMOR, ArmorType.HELMET).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_CHESTPLATE = ITEMS.registerItem("ruby_chestplate",
            p -> new Item(p.humanoidArmor(RUBY_ARMOR, ArmorType.CHESTPLATE).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_LEGGINGS = ITEMS.registerItem("ruby_leggings",
            p -> new Item(p.humanoidArmor(RUBY_ARMOR, ArmorType.LEGGINGS).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RUBY_BOOTS = ITEMS.registerItem("ruby_boots",
            p -> new Item(p.humanoidArmor(RUBY_ARMOR, ArmorType.BOOTS).fireResistant().rarity(Rarity.UNCOMMON)));

    /** 이벤트 투척병: 깨지면 랜덤 이벤트(또는 병에 적힌 이벤트)가 바로 시작 */
    public static final DeferredItem<EventBottleItem> EVENT_BOTTLE = ITEMS.registerItem("event_bottle",
            EventBottleItem::new, p -> p.stacksTo(16).rarity(Rarity.RARE));

    public static void init() {
        ModBlocks2.init();
    }

    private ModItems7() {}
}
