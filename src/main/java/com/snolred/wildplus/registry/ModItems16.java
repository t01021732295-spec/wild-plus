package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.home.HouseKitItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.3 집 꾸미기: 가구 아이템 · 집 상자 6종 · 가구점 */
public final class ModItems16 {
    public static final Map<String, DeferredItem<BlockItem>> FURNITURE = new LinkedHashMap<>();

    static {
        ModBlocks6.FURNITURE.forEach((n, b) -> FURNITURE.put(n, ITEMS.registerSimpleBlockItem(n, b)));
    }

    public static final DeferredItem<BlockItem> CANOPY_BED = ITEMS.registerSimpleBlockItem("canopy_bed", ModBlocks6.CANOPY_BED);
    public static final DeferredItem<BlockItem> BUNK_BED = ITEMS.registerSimpleBlockItem("bunk_bed", ModBlocks6.BUNK_BED);
    public static final DeferredItem<BlockItem> HOME_STORE = ITEMS.registerSimpleBlockItem("home_store", ModBlocks6.HOME_STORE);
    /** 집 상자: 이름 · 구조물 크기 (가로 x · 높이 y · 깊이 z) */
    public static final Map<String, DeferredItem<HouseKitItem>> HOUSES = new LinkedHashMap<>();

    private static void house(String id, int sx, int sy, int sz) {
        HOUSES.put(id, ITEMS.registerItem("house_" + id, p -> new HouseKitItem(p.stacksTo(1), id, sx, sy, sz)));
    }

    static {
        house("cabin", 9, 10, 11);
        house("log_house", 11, 11, 11);
        house("brick_house", 11, 15, 12);
        house("hanok", 13, 10, 12);
        house("modern", 13, 10, 13);
        house("mansion", 19, 19, 18);
    }

    public static void init() {}

    private ModItems16() {}
}
