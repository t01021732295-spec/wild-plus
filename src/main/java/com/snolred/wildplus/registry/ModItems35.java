package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.DpsDummyItem;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.0.10 DPS 측정 허수아비 */
public final class ModItems35 {
    public static final DeferredItem<DpsDummyItem> DPS_DUMMY = ITEMS.registerItem("dps_dummy", p -> new DpsDummyItem(p.stacksTo(16)));

    public static void init() {}
}
