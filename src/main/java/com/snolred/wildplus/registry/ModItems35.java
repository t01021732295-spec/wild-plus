package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.DpsDummyItem;
import com.snolred.wildplus.item.PurifyScrollItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.0.10 DPS 측정 허수아비 · 5.0.11 정화의 주문서 */
public final class ModItems35 {
    public static final DeferredItem<DpsDummyItem> DPS_DUMMY = ITEMS.registerItem("dps_dummy", p -> new DpsDummyItem(p.stacksTo(16)));

    /** 5.0.11 정화의 주문서 (저주 제거) */
    public static final DeferredItem<PurifyScrollItem> PURIFY_SCROLL = ITEMS.registerItem("purify_scroll", p -> new PurifyScrollItem(p.stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static void init() {}
}
