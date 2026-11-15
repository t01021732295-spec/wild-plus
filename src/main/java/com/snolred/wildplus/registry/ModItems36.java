package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.Rarity;

/** 5.1.1 겁화 각인석 4종(겁화 장비 전용) + 제련 각인석(일반 도구). 각인석 목록(ModItems25.STONES)에 더한다 */
public final class ModItems36 {
    private ModItems36() {}

    static {
        for (String k : new String[] {"INFERNO", "PYRE", "CINDER", "PHOENIX"})
            ModItems25.STONES.put(k, ITEMS.registerSimpleItem("engrave_" + k.toLowerCase(java.util.Locale.ROOT), p -> p.rarity(Rarity.EPIC).fireResistant()));
        ModItems25.STONES.put("SMELT", ITEMS.registerSimpleItem("engrave_smelt", p -> p.rarity(Rarity.RARE).fireResistant()));
    }

    public static void init() {}
}
