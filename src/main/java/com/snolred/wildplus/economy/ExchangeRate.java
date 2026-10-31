package com.snolred.wildplus.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

/**
 * 환전소 환율: 게임 속 하루마다 바뀐다 (그날 날짜로 정해지므로 저장할 필요 없음).
 * 올리기(작은 동전 → 큰 동전 1개)에 드는 개수 = 9~12, 내리기(큰 동전 1개 → 작은 동전)로 받는 개수 = 올리기 - 1.
 * 항상 올리기가 1개 더 비싸서, 바꿨다 되돌려도 돈이 늘지 않는다.
 * 은화(동↔은)와 금화(은↔금)는 따로 움직인다.
 */
public final class ExchangeRate {
    private static final int[] TABLE = {9, 10, 10, 10, 11, 11, 11, 12, 12, 10};

    private ExchangeRate() {}

    public static long day(MinecraftServer server) {
        return server.overworld().getOverworldClockTime() / 24000L;
    }

    private static int roll(long day, int salt) {
        RandomSource r = RandomSource.create(day * 0x9E3779B97F4A7C15L + salt * 31L + 12345L);
        return TABLE[r.nextInt(TABLE.length)];
    }

    /** 은화 1개를 사는 데 드는 동화 */
    public static int silverBuy(long day) {
        return roll(day, 1);
    }

    /** 은화 1개를 팔면 받는 동화 */
    public static int silverSell(long day) {
        return silverBuy(day) - 1;
    }

    /** 금화 1개를 사는 데 드는 은화 */
    public static int goldBuy(long day) {
        return roll(day, 2);
    }

    /** 금화 1개를 팔면 받는 은화 */
    public static int goldSell(long day) {
        return goldBuy(day) - 1;
    }
}
