package com.snolred.wildplus.omen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 3.8 할인: 바닐라 "마을의 영웅" 효과가 모드 상인 · 마을 서비스 · 마을 자판기 · 마을 전문 상점에 적용된다.
 * 영웅 I 10% · II 15% · III 20% · IV 이상 25%, 흉조 제단 4단계 마을이면 +10% (최대 35%).
 */
public final class Discounts {
    private Discounts() {}

    /** 흉조 효과 증폭값(-1 = 효과 없음)을 할인율로 변환한다. */
    public static int heroPctForAmplifier(int amplifier) {
        if (amplifier < 0) return 0;
        return amplifier == 0 ? 10 : amplifier == 1 ? 15 : amplifier == 2 ? 20 : 25;
    }

    /** 영웅 할인과 제단 4단계 보너스를 합친다. 순수 계산 함수라 테스트할 수 있다. */
    public static int combinePct(int heroPct, int altarStage) {
        int hero = Math.max(0, Math.min(25, heroPct));
        return Math.min(35, hero + (altarStage >= 4 ? 10 : 0));
    }

    public static int hero(Player p) {
        MobEffectInstance e = p.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        return heroPctForAmplifier(e == null ? -1 : e.getAmplifier());
    }

    public static int pct(Player p, BlockPos where) {
        int altarStage = 0;
        if (where != null && p.level() instanceof ServerLevel sl) {
            altarStage = OmenAltar.stageNear(sl.getServer(), where);
        }
        return combinePct(hero(p), altarStage);
    }

    public static long apply(long price, int pct) {
        if (price <= 0 || pct <= 0) return price;
        int safePct = Math.min(100, pct);
        return Math.max(1, Math.round(price * (100 - safePct) / 100.0));
    }

    public static String tag(int pct) {
        return pct <= 0 ? "" : " (🛡 마을의 영웅 −" + pct + "%)";
    }
}
