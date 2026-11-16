package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.2.1 각인 확장석(장비 각인 2칸) + 고대 각인석 4종 (공허 · 겁화 각인보다 20% 강함, 모든 장비) */
public final class ModItems37 {
    private ModItems37() {}

    public static final DeferredItem<Item> ENGRAVE_EXPANDER = ITEMS.registerSimpleItem("engrave_expander", p -> p.rarity(Rarity.EPIC).fireResistant());

    static {
        for (String k : new String[] {"EXECUTE", "SONIC", "BULWARK", "ECHO"})
            ModItems25.STONES.put(k, ITEMS.registerSimpleItem("engrave_" + k.toLowerCase(java.util.Locale.ROOT), p -> p.rarity(Rarity.EPIC).fireResistant()));
    }

    public static void init() {}
}
