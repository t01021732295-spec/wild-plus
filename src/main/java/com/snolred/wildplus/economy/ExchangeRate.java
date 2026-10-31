package com.snolred.wildplus.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

/**
 * 환전소 환율 (게임 속 하루마다 바뀜, 날짜로 정해지므로 저장할 필요 없음).
 * 동전은 작업대에서 언제나 10:1 로 바꿀 수 있으므로, 환전소가 그보다 유리하면 무한 증식이 된다.
 * 그래서 환율은 "그날의 수수료"로만 움직인다:
 *  - 살 때(작은 동전 → 큰 동전 1개) 10~12개 (10보다 싸지지 않음)
 *  - 팔 때(큰 동전 1개 → 작은 동전) 8~10개 (10보다 많지 않음)
 * 은화(동↔은)와 금화(은↔금)는 따로 움직인다.
 */
public final class ExchangeRate {
    private static final int[] BUY = {10, 10, 10, 11, 11, 11, 12, 12};
    private static final int[] SELL = {10, 10, 9, 9, 9, 8, 8, 9};

    private ExchangeRate() {}

    public static long day(MinecraftServer server) {
        return server.overworld().getOverworldClockTime() / 24000L;
    }

    private static int roll(long day, int salt, int[] table) {
        RandomSource r = RandomSource.create(day * 0x9E3779B97F4A7C15L + salt * 31L + 12345L);
        return table[r.nextInt(table.length)];
    }

    /** 은화 1개를 사는 데 드는 동화 (10 이상) */
    public static int silverBuy(long day) {
        return Math.max(10, roll(day, 1, BUY));
    }

    /** 은화 1개를 팔면 받는 동화 (10 이하) */
    public static int silverSell(long day) {
        return Math.min(10, roll(day, 3, SELL));
    }

    /** 금화 1개를 사는 데 드는 은화 (10 이상) */
    public static int goldBuy(long day) {
        return Math.max(10, roll(day, 2, BUY));
    }

    /** 금화 1개를 팔면 받는 은화 (10 이하) */
    public static int goldSell(long day) {
        return Math.min(10, roll(day, 4, SELL));
    }
}
