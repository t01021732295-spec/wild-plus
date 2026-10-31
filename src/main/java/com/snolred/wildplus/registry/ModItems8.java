package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 14차: 주말 레이드 · 하늘의 탑 · 주간 목표 */
public final class ModItems8 {
    public static final DeferredItem<BlockItem> RAID_ALTAR = ITEMS.registerSimpleBlockItem("raid_altar", ModBlocks2.RAID_ALTAR,
            p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> TOWER_GATE = ITEMS.registerSimpleBlockItem("tower_gate", ModBlocks2.TOWER_GATE,
            p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> GOAL_BOARD = ITEMS.registerSimpleBlockItem("goal_board", ModBlocks2.GOAL_BOARD,
            p -> p.rarity(Rarity.RARE));
    /** 레이드 보스 전리품 (은행 상인이 비싸게 사 줌) */
    public static final DeferredItem<Item> TITAN_CORE = ITEMS.registerSimpleItem("titan_core", p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<Item> LICH_ORB = ITEMS.registerSimpleItem("lich_orb", p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<Item> FROST_CROWN = ITEMS.registerSimpleItem("frost_crown", p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());

    public static void init() {}

    private ModItems8() {}
}
