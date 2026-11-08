package com.snolred.wildplus.economy;

import com.snolred.wildplus.WildConfig;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * 소득세: 하루(게임 속 하루) 동안의 소득을 쌓아 구간마다 세율이 올라가는 누진세.
 * 0~100동 5% · ~500동 10% · ~1,000동 15% · ~3,000동 20% · ~10,000동 25% · 그 이상 30%.
 * 걷힌 세금은 국고(WildData.treasury)에 쌓인다.
 */
public final class Tax {
    private static final long[] LIMITS = {100, 500, 1000, 3000, 10000, Long.MAX_VALUE};
    private static final int[] RATES = {5, 10, 15, 20, 25, 30};

    private Tax() {}

    /** 소득 x 전체에 대한 세금 (누진 구간 합) */
    static long totalTax(long income) {
        long tax = 0, lower = 0;
        for (int i = 0; i < LIMITS.length && income > lower; i++) {
            long part = Math.min(income, LIMITS[i]) - lower;
            tax += part * RATES[i];
            lower = LIMITS[i];
        }
        return tax / 100;
    }

    /** 지금 적용되는 최고 세율 (%) */
    public static int currentRate(long incomeToday) {
        for (int i = 0; i < LIMITS.length; i++) if (incomeToday < LIMITS[i]) return RATES[i];
        return RATES[RATES.length - 1];
    }

    /** 소득을 신고하고 세금을 뗀 실수령액을 돌려준다 */
    public static long collect(MinecraftServer server, UUID player, long gross) {
        Loans.addIncome(server, player, gross);
        if (gross <= 0 || !WildConfig.TAX_ENABLED.getAsBoolean()) return Math.max(0, gross);
        if (com.snolred.wildplus.event.RandomEvents.isActive(com.snolred.wildplus.event.RandomEvents.Type.TAX_HOLIDAY)) return gross;
        WildData data = WildData.get(server);
        long day = server.overworld().getOverworldClockTime() / 24000L;
        long[] rec = data.taxes.computeIfAbsent(player, k -> new long[3]);
        if (rec[0] != day) {
            rec[0] = day;
            rec[1] = 0;
            rec[2] = 0;
        }
        long tax = totalTax(rec[1] + gross) - totalTax(rec[1]);
        tax = tax * (100 - Honor.taxCut(data.honor(player))) / 100; // 명예 등급 세금 감면
        rec[1] += gross;
        rec[2] += tax;
        data.treasury += tax;
        data.setDirty();
        lastTax = tax;
        return gross - tax;
    }

    /** 바로 직전 collect 에서 뗀 세금 (안내 문구용) */
    public static long lastTax;

    /** [오늘 소득, 오늘 낸 세금] */
    public static long[] today(MinecraftServer server, UUID player) {
        long day = server.overworld().getOverworldClockTime() / 24000L;
        long[] rec = WildData.get(server).taxes.get(player);
        return rec == null || rec[0] != day ? new long[]{0, 0} : new long[]{rec[1], rec[2]};
    }
}
