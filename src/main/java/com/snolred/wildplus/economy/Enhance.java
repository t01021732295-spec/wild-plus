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
    /** 인챈트별 강화 최대 레벨: 레벨이 있는 인챈트는 모두 XV 까지 (X 부터는 시련 + 재료가 필요, Trials) */
    public static final Map<String, Integer> CAP = Map.ofEntries(
            Map.entry("sharpness", 15), Map.entry("smite", 15), Map.entry("bane_of_arthropods", 15), Map.entry("efficiency", 15),
            Map.entry("power", 15), Map.entry("impaling", 15), Map.entry("density", 15),
            Map.entry("unbreaking", 15), Map.entry("fortune", 15), Map.entry("looting", 15), Map.entry("luck_of_the_sea", 15),
            Map.entry("respiration", 15), Map.entry("sweeping_edge", 15), Map.entry("thorns", 15), Map.entry("swift_sneak", 15),
            Map.entry("soul_speed", 15), Map.entry("loyalty", 15),
            Map.entry("protection", 15), Map.entry("fire_protection", 15), Map.entry("blast_protection", 15),
            Map.entry("projectile_protection", 15), Map.entry("feather_falling", 15), Map.entry("piercing", 15), Map.entry("breach", 15),
            Map.entry("fire_aspect", 15), Map.entry("punch", 15), Map.entry("knockback", 15),
            Map.entry("armor_piercing", 15), Map.entry("bow_looting", 15));

    public static final long BASE_COST = 200;

    public record Option(Holder<Enchantment> enchant, int level, int vanillaMax, int cap, long cost) {}

    private Enhance() {}

    public static String path(Holder<Enchantment> h) {
        return h.unwrapKey().map(k -> {
            String ns = k.identifier().getNamespace();
            return ns.equals("minecraft") || ns.equals("wildplus") ? k.identifier().getPath() : "";
        }).orElse("");
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
        if (level + 1 >= Trials.FIRST) return Trials.cost(level + 1); // X 이상: 고정 가격
        long c = BASE_COST;
        for (int i = vanillaMax; i < level; i++) c *= 3;
        return Prices.x(c);
    }

    /** 수리 비용: 기본 5동 + 닳은 내구도 20당 1동 */
    public static long repairCost(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged() ? Prices.x(5L + stack.getDamageValue() / 20) : 0;
    }

    public static void apply(ItemStack stack, Option o) {
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(o.enchant(), o.level() + 1));
    }
}
