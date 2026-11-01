package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.WildPlus;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 16차: 스타 루비 장비 (네더라이트 위 최종 장비). 강화대에서 루비 장비 + 네더의 별 + 1,000동으로 승급.
 * 모두 불·용암에 타지 않고, 내구도가 닳지 않는다.
 * 검: 공격력 9 · 공격 속도 1.6 · 고정 피해 4 (강화대에서 0.5씩 최대 6)
 */
public final class ModItems9 {
    /** 공격 보너스 5 (네더라이트 4) · 속도 11 · 인챈트 22 */
    public static final ToolMaterial STAR_RUBY_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.0F, 5.0F, 22,
            ModItems7.RUBY_REPAIR);

    private static Item.Properties star(Item.Properties p) {
        return p.fireResistant().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    public static final DeferredItem<Item> STAR_RUBY_SWORD = ITEMS.registerItem("star_ruby_sword",
            p -> new Item(star(p.sword(STAR_RUBY_TOOL, 3.0F, -2.4F))));
    public static final DeferredItem<Item> STAR_RUBY_PICKAXE = ITEMS.registerItem("star_ruby_pickaxe",
            p -> new Item(star(p.pickaxe(STAR_RUBY_TOOL, 1.0F, -2.8F))));
    public static final DeferredItem<Item> STAR_RUBY_AXE = ITEMS.registerItem("star_ruby_axe",
            p -> new Item(star(p.axe(STAR_RUBY_TOOL, 5.0F, -3.0F))));
    public static final DeferredItem<Item> STAR_RUBY_SHOVEL = ITEMS.registerItem("star_ruby_shovel",
            p -> new Item(star(p.shovel(STAR_RUBY_TOOL, 1.5F, -3.0F))));
    public static final DeferredItem<Item> STAR_RUBY_HOE = ITEMS.registerItem("star_ruby_hoe",
            p -> new Item(star(p.hoe(STAR_RUBY_TOOL, -4.0F, 0.0F))));

    public static final ResourceKey<EquipmentAsset> STAR_RUBY_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("star_ruby"));
    /** 네더라이트보다 한 단계 위: 방어 4/7/9/4 · 강인함 4 · 밀치기 저항 0.15 */
    public static final ArmorMaterial STAR_RUBY_ARMOR = new ArmorMaterial(50, defense(4, 7, 9, 4, 13), 22,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.15F, ModItems7.RUBY_REPAIR, STAR_RUBY_ASSET);

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    public static final DeferredItem<Item> STAR_RUBY_HELMET = ITEMS.registerItem("star_ruby_helmet",
            p -> new Item(star(p.humanoidArmor(STAR_RUBY_ARMOR, ArmorType.HELMET))));
    public static final DeferredItem<Item> STAR_RUBY_CHESTPLATE = ITEMS.registerItem("star_ruby_chestplate",
            p -> new Item(star(p.humanoidArmor(STAR_RUBY_ARMOR, ArmorType.CHESTPLATE))));
    public static final DeferredItem<Item> STAR_RUBY_LEGGINGS = ITEMS.registerItem("star_ruby_leggings",
            p -> new Item(star(p.humanoidArmor(STAR_RUBY_ARMOR, ArmorType.LEGGINGS))));
    public static final DeferredItem<Item> STAR_RUBY_BOOTS = ITEMS.registerItem("star_ruby_boots",
            p -> new Item(star(p.humanoidArmor(STAR_RUBY_ARMOR, ArmorType.BOOTS))));

    /** 다이아 배낭 (98칸) */
    public static final DeferredItem<com.snolred.wildplus.item.BackpackItem> DIAMOND_BACKPACK = ITEMS.registerItem("diamond_backpack",
            p -> new com.snolred.wildplus.item.BackpackItem(p, 3), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<net.minecraft.world.item.BlockItem> FILTER_HOPPER = ITEMS.registerSimpleBlockItem("filter_hopper",
            ModBlocks2.FILTER_HOPPER);

    // 17차
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SORTING_CHEST = ITEMS.registerSimpleBlockItem("sorting_chest", ModBlocks2.SORTING_CHEST);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> FEEDER = ITEMS.registerSimpleBlockItem("feeder", ModBlocks2.FEEDER);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MAILBOX = ITEMS.registerSimpleBlockItem("mailbox", ModBlocks2.MAILBOX);

    // 2.8
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TRASH_CAN = ITEMS.registerSimpleBlockItem("trash_can", ModBlocks2.TRASH_CAN);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> AUTO_CRAFTER = ITEMS.registerSimpleBlockItem("auto_crafter", ModBlocks2.AUTO_CRAFTER);

    /** 루비 → 스타 루비 짝 */
    public static Map<Item, Item> upgrades() {
        return Map.of(
                ModItems7.RUBY_SWORD.get(), STAR_RUBY_SWORD.get(), ModItems7.RUBY_PICKAXE.get(), STAR_RUBY_PICKAXE.get(),
                ModItems7.RUBY_AXE.get(), STAR_RUBY_AXE.get(), ModItems7.RUBY_SHOVEL.get(), STAR_RUBY_SHOVEL.get(),
                ModItems7.RUBY_HOE.get(), STAR_RUBY_HOE.get(), ModItems7.RUBY_HELMET.get(), STAR_RUBY_HELMET.get(),
                ModItems7.RUBY_CHESTPLATE.get(), STAR_RUBY_CHESTPLATE.get(), ModItems7.RUBY_LEGGINGS.get(), STAR_RUBY_LEGGINGS.get(),
                ModItems7.RUBY_BOOTS.get(), STAR_RUBY_BOOTS.get(), ModItems10.RUBY_BOW.get(), ModItems10.STAR_RUBY_BOW.get());
    }

    public static void init() {}

    private ModItems9() {}
}
