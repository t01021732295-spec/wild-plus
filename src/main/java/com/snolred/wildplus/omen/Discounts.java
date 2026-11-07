package com.snolred.wildplus.omen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 3.8 할인: 바닐라 "마을의 영웅" 효과가 모드 상인 · 마을 서비스 · 마을 자판기에도 적용된다.
 * 영웅 I 10% · II 15% · III 20% · IV 이상 25%, 흉조 제단 4단계 마을이면 +10% (최대 35%).
 */
public final class Discounts {
    private Discounts() {}

    public static int hero(Player p) {
        MobEffectInstance e = p.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (e == null) return 0;
        int a = e.getAmplifier();
        return a <= 0 ? 10 : a == 1 ? 15 : a == 2 ? 20 : 25;
    }

    public static int pct(Player p, BlockPos where) {
        int v = hero(p);
        if (where != null && p.level() instanceof ServerLevel sl && OmenAltar.stageNear(sl.getServer(), where) >= 4) v += 10;
        return Math.min(35, v);
    }

    public static long apply(long price, int pct) {
        return pct <= 0 ? price : Math.max(1, Math.round(price * (100 - pct) / 100.0));
    }

    public static String tag(int pct) {
        return pct <= 0 ? "" : " (영웅 할인 " + pct + "%)";
    }
}
