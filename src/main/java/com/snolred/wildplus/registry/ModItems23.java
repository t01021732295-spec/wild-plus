package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.0 공허 열쇠 · 각성의 핵 I~V */
public final class ModItems23 {
    public static final DeferredItem<Item> VOID_KEY = ITEMS.registerSimpleItem("void_key", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final List<DeferredItem<Item>> CORES = new ArrayList<>();

    static {
        for (int t = 1; t <= 5; t++) {
            CORES.add(ITEMS.registerSimpleItem("awakening_core_" + t,
                    p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        }
    }

    /** 각성의 핵 단계 (아니면 0) */
    public static int coreStage(Item item) {
        for (int i = 0; i < CORES.size(); i++) if (CORES.get(i).get() == item) return i + 1;
        return 0;
    }

    public static void init() {}

    private ModItems23() {}
}
