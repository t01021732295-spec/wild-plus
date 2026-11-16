package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

/**
 * 5.2 경기 지수: "최근 7일 동안 1인이 하루 평균 번 돈". 고정 보상(알바 · 의뢰 · 주간 목표 · 시즌 패스 돈)이 이 값의 몇 % 를 주게 해서
 * 경제가 커져도 할 이유가 남도록 한다. 소득은 Tax.collect 를 거치는 돈(출하 · 채굴기 · 요리 · 식당 · 알바 · 이자 등)으로 센다
 * (카지노 · 주식 차익은 빼고). 지수는 게임 하루에 한 번만 바뀌고 한 번에 ±15% 까지만 움직인다.
 */
public final class EconIndex {
    private EconIndex() {}

    /** 데이터가 없을 때의 기본 지수 (1인 하루 3만동) */
    public static final long DEFAULT = 30_000L;
    private static final int DAYS = 7;

    private static CompoundTag tag(MinecraftServer s) {
        return ContentData.get(s).section("econ_index");
    }

    /** 소득 기록 (센트가 아닌 동 단위) */
    public static void add(MinecraftServer s, UUID who, long gross) {
        if (s == null || gross <= 0) return;
        CompoundTag t = tag(s);
        long day = ContentData.gameDay;
        roll(s, t, day);
        String k = "t" + (day % DAYS);
        t.putLong(k, t.getLongOr(k, 0L) + gross);
        CompoundTag who7 = t.getCompoundOrEmpty("p" + (day % DAYS));
        who7.putBoolean(who.toString(), true);
        t.put("p" + (day % DAYS), who7);
        ContentData.get(s).dirty();
    }

    /** 하루가 바뀌면 그 칸을 비우고 지수를 다시 계산 */
    private static void roll(MinecraftServer s, CompoundTag t, long day) {
        long last = t.getLongOr("day", -1L);
        if (last == day) return;
        // 지난날까지의 기록으로 지수 갱신 (오늘 칸은 비움)
        if (last >= 0) {
            long sum = 0;
            int days = 0;
            for (int i = 0; i < DAYS; i++) {
                if (i == day % DAYS) continue;
                long tot = t.getLongOr("t" + i, 0L);
                int ppl = t.getCompoundOrEmpty("p" + i).size();
                if (tot <= 0 || ppl == 0) continue;
                sum += tot / ppl;
                days++;
            }
            if (days > 0) {
                long raw = sum / days;
                long prev = t.getLongOr("index", DEFAULT);
                long next = Math.max(prev * 85 / 100, Math.min(prev * 115 / 100, raw));
                t.putLong("index", Math.max(DEFAULT, next));
            }
        }
        // 오래된 날 칸 비우기 (건너뛴 날 포함)
        long from = last < 0 ? day - DAYS + 1 : last + 1;
        for (long d = Math.max(from, day - DAYS + 1); d <= day; d++) {
            t.remove("t" + (d % DAYS));
            t.remove("p" + (d % DAYS));
        }
        t.putLong("day", day);
        ContentData.get(s).dirty();
    }

    /** 1인 하루 평균 소득 (동) */
    public static long daily(MinecraftServer s) {
        if (s == null) return DEFAULT;
        CompoundTag t = tag(s);
        roll(s, t, ContentData.gameDay);
        return Math.max(DEFAULT, t.getLongOr("index", DEFAULT));
    }

    /** 지수의 pct% 와 최소 보장(floor) 중 큰 값, 100동 단위 */
    public static long reward(MinecraftServer s, double pct, long floor) {
        long v = Math.max(floor, Math.round(daily(s) * pct / 100.0));
        return v >= 1000 ? Math.round(v / 100.0) * 100 : v;
    }

    public static String label(MinecraftServer s) {
        return String.format("📈 경기 지수: 1인 하루 %,d동", daily(s));
    }
}
