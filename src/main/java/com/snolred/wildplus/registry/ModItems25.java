package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.2 각인석 · 공허 깃 · 군주 소환 재료 조각 */
public final class ModItems25 {
    /** 각인 종류 → 각인석 */
    public static final Map<String, DeferredItem<Item>> STONES = new LinkedHashMap<>();
    public static final DeferredItem<Item> LORD_SHARD = ITEMS.registerSimpleItem("lord_shard", p -> p.rarity(Rarity.RARE).fireResistant());

    static {
        for (String k : new String[] {"ABYSS", "DEVOUR", "CHAIN", "WARP", "HEART"}) stone(k, Rarity.EPIC);
        for (String k : new String[] {"HARVEST", "FURY", "GUARD", "VIGOR", "RUSH"}) stone(k, Rarity.RARE);
        STONES.put("GLIDE", ITEMS.registerSimpleItem("void_feather", p -> p.rarity(Rarity.EPIC).fireResistant()));
    }

    private static void stone(String kind, Rarity r) {
        STONES.put(kind, ITEMS.registerSimpleItem("engrave_" + kind.toLowerCase(java.util.Locale.ROOT), p -> p.rarity(r).fireResistant()));
    }

    /** 각인석이면 종류, 아니면 "" */
    public static String kind(ItemStack s) {
        for (var e : STONES.entrySet()) if (s.is(e.getValue().get())) return e.getKey();
        return "";
    }

    public static void init() {}

    private ModItems25() {}
}
