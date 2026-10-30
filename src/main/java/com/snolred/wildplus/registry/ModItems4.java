package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.item.BackpackItem;
import com.snolred.wildplus.item.LotteryTicketItem;
import com.snolred.wildplus.item.TeleporterItem;
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

/** 5차: 비취 도구·갑옷, 미스릴 갑옷, 워프·텔레포트기·배낭·경제 아이템 */
public final class ModItems4 {
    public static final TagKey<Item> JADE_REPAIR = TagKey.create(Registries.ITEM, WildPlus.id("jade_repair"));

    // ───────── 비취 도구: 철 등급 채굴, 빠르고 인챈트가 아주 잘 붙음 ─────────
    public static final ToolMaterial JADE_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 800, 9.0F, 2.5F, 25, JADE_REPAIR);

    public static final DeferredItem<Item> JADE_SWORD = ITEMS.registerItem("jade_sword", p -> new Item(p.sword(JADE_TOOL, 3.0F, -2.3F)));
    public static final DeferredItem<Item> JADE_PICKAXE = ITEMS.registerItem("jade_pickaxe", p -> new Item(p.pickaxe(JADE_TOOL, 1.0F, -2.8F)));
    public static final DeferredItem<Item> JADE_AXE = ITEMS.registerItem("jade_axe", p -> new Item(p.axe(JADE_TOOL, 6.0F, -3.1F)));
    public static final DeferredItem<Item> JADE_SHOVEL = ITEMS.registerItem("jade_shovel", p -> new Item(p.shovel(JADE_TOOL, 1.5F, -3.0F)));
    public static final DeferredItem<Item> JADE_HOE = ITEMS.registerItem("jade_hoe", p -> new Item(p.hoe(JADE_TOOL, -2.0F, -1.0F)));

    // ───────── 갑옷 ─────────
    public static final ResourceKey<EquipmentAsset> MYTHRIL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("mythril"));
    public static final ResourceKey<EquipmentAsset> JADE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, WildPlus.id("jade"));

    /** 미스릴: 다이아몬드급 방어 + 더 튼튼함 */
    public static final ArmorMaterial MYTHRIL_ARMOR = new ArmorMaterial(35, defense(3, 6, 8, 3, 11), 20,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, 0.05F, ModItems2.MYTHRIL_REPAIR, MYTHRIL_ASSET);
    /** 비취: 철급 방어 + 인챈트가 가장 잘 붙음 */
    public static final ArmorMaterial JADE_ARMOR = new ArmorMaterial(20, defense(2, 5, 6, 2, 5), 30,
            SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.0F, JADE_REPAIR, JADE_ASSET);

    private static EnumMap<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    public static final DeferredItem<Item> MYTHRIL_HELMET = ITEMS.registerItem("mythril_helmet", p -> new Item(p.humanoidArmor(MYTHRIL_ARMOR, ArmorType.HELMET)));
    public static final DeferredItem<Item> MYTHRIL_CHESTPLATE = ITEMS.registerItem("mythril_chestplate", p -> new Item(p.humanoidArmor(MYTHRIL_ARMOR, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> MYTHRIL_LEGGINGS = ITEMS.registerItem("mythril_leggings", p -> new Item(p.humanoidArmor(MYTHRIL_ARMOR, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> MYTHRIL_BOOTS = ITEMS.registerItem("mythril_boots", p -> new Item(p.humanoidArmor(MYTHRIL_ARMOR, ArmorType.BOOTS)));
    public static final DeferredItem<Item> JADE_HELMET = ITEMS.registerItem("jade_helmet", p -> new Item(p.humanoidArmor(JADE_ARMOR, ArmorType.HELMET)));
    public static final DeferredItem<Item> JADE_CHESTPLATE = ITEMS.registerItem("jade_chestplate", p -> new Item(p.humanoidArmor(JADE_ARMOR, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> JADE_LEGGINGS = ITEMS.registerItem("jade_leggings", p -> new Item(p.humanoidArmor(JADE_ARMOR, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> JADE_BOOTS = ITEMS.registerItem("jade_boots", p -> new Item(p.humanoidArmor(JADE_ARMOR, ArmorType.BOOTS)));

    // ───────── 이동 · 배낭 · 경제 ─────────
    public static final DeferredItem<BlockItem> WARP_POINT = ITEMS.registerSimpleBlockItem("warp_point", ModBlocks.WARP_POINT);
    public static final DeferredItem<TeleporterItem> TELEPORTER = ITEMS.registerItem("teleporter", TeleporterItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> STOCK_TERMINAL = ITEMS.registerSimpleBlockItem("stock_terminal", ModBlocks.STOCK_TERMINAL);
    public static final DeferredItem<BlockItem> ATM = ITEMS.registerSimpleBlockItem("atm", ModBlocks.ATM);
    public static final DeferredItem<BlockItem> BOUNTY_BOARD = ITEMS.registerSimpleBlockItem("bounty_board", ModBlocks.BOUNTY_BOARD);
    public static final DeferredItem<BlockItem> EXCHANGE = ITEMS.registerSimpleBlockItem("exchange", ModBlocks.EXCHANGE);
    public static final DeferredItem<BlockItem> SHIPPING_BIN = ITEMS.registerSimpleBlockItem("shipping_bin", ModBlocks.SHIPPING_BIN);
    public static final DeferredItem<BackpackItem> COPPER_BACKPACK = ITEMS.registerItem("copper_backpack",
            p -> new BackpackItem(p, 0), p -> p.stacksTo(1));
    public static final DeferredItem<BackpackItem> SILVER_BACKPACK = ITEMS.registerItem("silver_backpack",
            p -> new BackpackItem(p, 1), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BackpackItem> GOLD_BACKPACK = ITEMS.registerItem("gold_backpack",
            p -> new BackpackItem(p, 2), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<LotteryTicketItem> LOTTERY_TICKET = ITEMS.registerItem("lottery_ticket", LotteryTicketItem::new);

    // ───────── 6차 ─────────
    public static final DeferredItem<BlockItem> CHUNK_LOADER = ITEMS.registerSimpleBlockItem("chunk_loader", ModBlocks.CHUNK_LOADER);
    public static final DeferredItem<BlockItem> ENHANCE_TABLE = ITEMS.registerSimpleBlockItem("enhance_table", ModBlocks.ENHANCE_TABLE);
    public static final DeferredItem<BlockItem> PHARMACY = ITEMS.registerSimpleBlockItem("pharmacy", ModBlocks.PHARMACY);
    public static final DeferredItem<BlockItem> WEATHER_STATION = ITEMS.registerSimpleBlockItem("weather_station", ModBlocks.WEATHER_STATION);

    public static void init() {}

    private ModItems4() {}
}
