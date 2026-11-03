package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.FertilizerItem;
import com.snolred.wildplus.item.TieredRodItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 2.18: 낚시 · 농사 대개편 — 낚싯대 라인 · 미끼 · 비료 3단계 · 새 작물 · 비료 살포기 · 알바 게시판 */
public final class ModItems13 {
    // 낚싯대: 비취(기존) → 미스릴 → 루비 → 사파이어 → 용왕
    public static final DeferredItem<TieredRodItem> MYTHRIL_FISHING_ROD = ITEMS.registerItem("mythril_fishing_rod",
            p -> new TieredRodItem(p.durability(768).enchantable(15).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<TieredRodItem> RUBY_FISHING_ROD = ITEMS.registerItem("ruby_fishing_rod",
            p -> new TieredRodItem(p.durability(1200).enchantable(18).rarity(Rarity.RARE).fireResistant(), 2));
    public static final DeferredItem<TieredRodItem> SAPPHIRE_FISHING_ROD = ITEMS.registerItem("sapphire_fishing_rod",
            p -> new TieredRodItem(p.durability(1600).enchantable(20).rarity(Rarity.RARE), 3));
    public static final DeferredItem<TieredRodItem> DRAGON_FISHING_ROD = ITEMS.registerItem("dragon_fishing_rod",
            p -> new TieredRodItem(p.durability(4000).enchantable(22).rarity(Rarity.EPIC).fireResistant(), 4));

    // 미끼 (왼손에 들고 낚시하면 한 마리마다 하나씩)
    public static final DeferredItem<Item> WORM = ITEMS.registerSimpleItem("worm");
    public static final DeferredItem<Item> SHRIMP_BAIT = ITEMS.registerSimpleItem("shrimp_bait");
    public static final DeferredItem<Item> GOLDEN_BAIT = ITEMS.registerSimpleItem("golden_bait", p -> p.rarity(Rarity.UNCOMMON));

    // 비료: 기존 비료(1단계) + 고급 · 황금
    public static final DeferredItem<FertilizerItem> RICH_FERTILIZER = ITEMS.registerItem("rich_fertilizer", p -> new FertilizerItem(p, 2));
    public static final DeferredItem<FertilizerItem> GOLDEN_FERTILIZER = ITEMS.registerItem("golden_fertilizer",
            p -> new FertilizerItem(p.rarity(Rarity.UNCOMMON), 3));

    // 새 작물
    public static final DeferredItem<BlockItem> GRAPE_SEEDS = ITEMS.registerItem("grape_seeds",
            p -> new BlockItem(ModBlocks3.GRAPE_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> GRAPE = food("grape", 2, 0.3F);
    public static final DeferredItem<BlockItem> RICE_SEEDS = ITEMS.registerItem("rice_seeds",
            p -> new BlockItem(ModBlocks3.RICE_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> RICE = ITEMS.registerSimpleItem("rice");
    public static final DeferredItem<BlockItem> ONION_SEEDS = ITEMS.registerItem("onion_seeds",
            p -> new BlockItem(ModBlocks3.ONION_CROP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> ONION = food("onion", 2, 0.2F);
    public static final DeferredItem<BlockItem> GINSENG_SEEDS = ITEMS.registerItem("ginseng_seeds",
            p -> new BlockItem(ModBlocks3.GINSENG_CROP.get(), p.useItemDescriptionPrefix().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> GINSENG = ITEMS.registerSimpleItem("ginseng", p -> p.rarity(Rarity.RARE)
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(1.2F).alwaysEdible().build()));

    public static final DeferredItem<BlockItem> SPREADER = ITEMS.registerSimpleBlockItem("fertilizer_spreader", ModBlocks3.SPREADER);
    public static final DeferredItem<BlockItem> JOB_BOARD = ITEMS.registerSimpleBlockItem("job_board", ModBlocks3.JOB_BOARD);

    private static DeferredItem<Item> food(String name, int nutrition, float sat) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(sat).build()));
    }

    /** 낚싯대 승급 (강화대, 인챈트 유지): 비취 → 미스릴 → 루비 → 사파이어 → 용왕 */
    public static java.util.Map<Item, Item> rodUpgrades() {
        java.util.Map<Item, Item> m = new java.util.LinkedHashMap<>();
        m.put(ModItems2.JADE_FISHING_ROD.get(), MYTHRIL_FISHING_ROD.get());
        m.put(MYTHRIL_FISHING_ROD.get(), RUBY_FISHING_ROD.get());
        m.put(RUBY_FISHING_ROD.get(), SAPPHIRE_FISHING_ROD.get());
        m.put(SAPPHIRE_FISHING_ROD.get(), DRAGON_FISHING_ROD.get());
        return m;
    }

    /** 낚싯대 승급 재료 (낚싯대가 아니면 null) */
    public static Item rodMaterial(Item from) {
        if (from == ModItems2.JADE_FISHING_ROD.get()) return ModItems2.MYTHRIL_BLOCK.get();
        if (from == MYTHRIL_FISHING_ROD.get()) return ModItems7.RUBY_BLOCK.get();
        if (from == RUBY_FISHING_ROD.get()) return ModItems11.SAPPHIRE_BLOCK.get();
        if (from == SAPPHIRE_FISHING_ROD.get()) return ModItems5.DRAGON_KING_FISH.get();
        return null;
    }

    public static void init() {}

    private ModItems13() {}
}
