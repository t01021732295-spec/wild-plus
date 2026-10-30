package com.snolred.wildplus.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 강화: 바닐라 최대 레벨까지는 마법 부여대·모루로 원래대로 올리고,
 * 그 너머 레벨(모드가 새로 연 레벨)은 강화대에서 동전을 내야만 올라간다.
 * 비용 = 200동 × 3^(바닐라 최대를 넘은 단계) → 200, 600, 1,800 …
 */
public final class Enhance {
    /** 인챈트별 모드 최대 레벨 (마인크래프트 기본 인챈트) */
    public static final Map<String, Integer> CAP = Map.ofEntries(
            Map.entry("sharpness", 7), Map.entry("smite", 7), Map.entry("bane_of_arthropods", 7), Map.entry("efficiency", 7),
            Map.entry("power", 7), Map.entry("impaling", 7), Map.entry("density", 7),
            Map.entry("unbreaking", 5), Map.entry("fortune", 5), Map.entry("looting", 5), Map.entry("luck_of_the_sea", 5),
            Map.entry("respiration", 5), Map.entry("sweeping_edge", 5), Map.entry("thorns", 5), Map.entry("swift_sneak", 5),
            Map.entry("soul_speed", 5), Map.entry("loyalty", 5),
            Map.entry("protection", 6), Map.entry("fire_protection", 6), Map.entry("blast_protection", 6),
            Map.entry("projectile_protection", 6), Map.entry("feather_falling", 6), Map.entry("piercing", 6), Map.entry("breach", 6),
            Map.entry("fire_aspect", 3), Map.entry("punch", 3), Map.entry("knockback", 3));

    public static final long BASE_COST = 200;

    public record Option(Holder<Enchantment> enchant, int level, int vanillaMax, int cap, long cost) {}

    private Enhance() {}

    public static String path(Holder<Enchantment> h) {
        return h.unwrapKey().map(k -> k.identifier().getNamespace().equals("minecraft") ? k.identifier().getPath() : "").orElse("");
    }

    /** 이 아이템에서 돈으로 올릴 수 있는 인챈트 목록 (이름순, 서버·화면이 같은 순서) */
    public static List<Option> options(ItemStack stack) {
        List<Option> out = new ArrayList<>();
        if (stack.isEmpty()) return out;
        ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var e : ench.entrySet()) {
            Holder<Enchantment> h = e.getKey();
            int level = e.getIntValue();
            Integer cap = CAP.get(path(h));
            int vMax = h.value().getMaxLevel();
            if (cap == null || level < vMax || level >= cap) continue;
            out.add(new Option(h, level, vMax, cap, cost(level, vMax)));
        }
        out.sort(Comparator.comparing(o -> path(o.enchant())));
        return out;
    }

    public static long cost(int level, int vanillaMax) {
        long c = BASE_COST;
        for (int i = vanillaMax; i < level; i++) c *= 3;
        return c;
    }

    /** 수리 비용: 기본 5동 + 닳은 내구도 20당 1동 */
    public static long repairCost(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged() ? 5 + stack.getDamageValue() / 20 : 0;
    }

    public static void apply(ItemStack stack, Option o) {
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(o.enchant(), o.level() + 1));
    }
}
