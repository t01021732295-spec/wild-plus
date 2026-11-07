package com.snolred.wildplus.economy;

/**
 * 3.7 물가 인상: 돈을 쓰는 곳(상점 · 명품관 · 가구점 · 집 · 땅 · 서비스 · 강화 · 인챈트 · 자판기 · 상인)은 3배,
 * 원래 100만동 이상인 최상위 상품은 2배. 돈을 버는 쪽(출하가 · 동전 · 알바 · 채굴기)은 그대로.
 */
public final class Prices {
    private Prices() {}

    public static long x(long base) {
        return base >= 1_000_000 ? base * 2 : base * 3;
    }

    public static int x(int base) {
        return (int) Math.min(Integer.MAX_VALUE, x((long) base));
    }
}
