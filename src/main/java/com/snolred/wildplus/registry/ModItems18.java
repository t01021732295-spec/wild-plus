package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.home.HouseKitItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.5 아이템: 경마장 블록 · 경주마 피규어 6 · 경마장 상자 · 마을 블록 · 공사 부지 상자 5 */
public final class ModItems18 {
    public static final DeferredItem<BlockItem> STABLE = ITEMS.registerSimpleBlockItem("stable", ModBlocks8.STABLE);
    public static final DeferredItem<BlockItem> BETTING_KIOSK = ITEMS.registerSimpleBlockItem("betting_kiosk", ModBlocks8.BETTING_KIOSK);
    public static final DeferredItem<BlockItem> STARTING_GATE = ITEMS.registerSimpleBlockItem("starting_gate", ModBlocks8.STARTING_GATE);
    public static final DeferredItem<BlockItem> SCOREBOARD = ITEMS.registerSimpleBlockItem("scoreboard", ModBlocks8.SCOREBOARD);
    public static final DeferredItem<BlockItem> FINISH_POST = ITEMS.registerSimpleBlockItem("finish_post", ModBlocks8.FINISH_POST);
    public static final DeferredItem<BlockItem> PROJECT_BOARD = ITEMS.registerSimpleBlockItem("project_board", ModBlocks8.PROJECT_BOARD);
    public static final DeferredItem<BlockItem> DONATION_BOX = ITEMS.registerSimpleBlockItem("donation_box", ModBlocks8.DONATION_BOX);
    public static final DeferredItem<BlockItem> CLOCK_FACE = ITEMS.registerSimpleBlockItem("clock_face", ModBlocks8.CLOCK_FACE);
    public static final List<DeferredItem<Item>> RACE_HORSES = new ArrayList<>();

    static {
        for (int k = 0; k < 6; k++) RACE_HORSES.add(ITEMS.registerSimpleItem("race_horse_" + k, p -> p.stacksTo(1)));
    }

    public static final DeferredItem<HouseKitItem> RACETRACK_KIT = ITEMS.registerItem("house_racetrack", p -> new HouseKitItem(p.stacksTo(1), "racetrack", 37, 9, 41));
    public static final Map<String, DeferredItem<com.snolred.wildplus.home.VillageSiteItem>> SITES = new LinkedHashMap<>();

    static {
        for (String id : com.snolred.wildplus.content.Village.IDS) {
            SITES.put(id, ITEMS.registerItem("village_" + id, p -> new com.snolred.wildplus.home.VillageSiteItem(p.stacksTo(1), id)));
        }
    }

    public static void init() {}

    private ModItems18() {}
}
