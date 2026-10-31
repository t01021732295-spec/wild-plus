package com.snolred.wildplus.economy;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * 주식 시장 (게임 시간 기준). 서버가 켜져 있을 때만 시세가 움직인다.
 * - 장은 게임 속 낮(06시~18시)에만 열리고, 게임 속 1시간(1,000틱 ≈ 50초)마다 가격이 한 번 움직인다.
 * - 종목마다 변동성이 다르고, 보이지 않는 "적정가"를 따라 천천히 되돌아간다.
 * - 가끔 뉴스(호재·악재)가 터져 한 종목이 크게 오르내리고, 채팅으로 알린다.
 * - 날이 바뀌면 전일가가 그날 종가로 바뀐다 (등락률 기준).
 * 가격은 내부적으로 "동 × 100" 단위(소수 둘째 자리)로 다룬다.
 */
public final class StockMarket {
    private static final Logger LOG = LogUtils.getLogger();

    /** vol = 한 시간 변동폭(%) */
    public record Stock(String code, String symbol, String name, boolean usd, long fallbackCenti, double vol) {}

    public static final List<Stock> STOCKS = List.of(
            new Stock("samsung", "005930.KS", "삼성전자", false, 6000, 0.9),
            new Stock("skhynix", "000660.KS", "SK하이닉스", false, 20000, 1.4),
            new Stock("hyundai", "005380.KS", "현대차", false, 25000, 1.0),
            new Stock("naver", "035420.KS", "NAVER", false, 20000, 1.3),
            new Stock("kakao", "035720.KS", "카카오", false, 4000, 1.8),
            new Stock("apple", "AAPL", "애플", true, 32000, 0.8),
            new Stock("nvidia", "NVDA", "엔비디아", true, 25000, 2.2),
            new Stock("tesla", "TSLA", "테슬라", true, 35000, 2.6),
            new Stock("microsoft", "MSFT", "마이크로소프트", true, 58000, 0.8),
            new Stock("google", "GOOGL", "구글", true, 24000, 1.1));

    private static final String[] GOOD = {"신제품이 대박났어요", "실적이 예상보다 좋아요", "대형 계약을 따냈어요", "외국인 매수가 몰려요", "새 공장을 지어요"};
    private static final String[] BAD = {"리콜 소식이 나왔어요", "실적이 부진해요", "공장에 불이 났어요", "규제 조사를 받아요", "대표가 사임했어요"};

    private static final long[] CUR = new long[STOCKS.size()];
    private static final long[] PREV = new long[STOCKS.size()];
    private static final long[] FAIR = new long[STOCKS.size()];
    private static boolean loaded;
    private static long lastHour = Long.MIN_VALUE, lastDay = Long.MIN_VALUE;
    private static long clock;
    private static final Random RANDOM = new Random();

    private StockMarket() {}

    public static long price(int i) { return CUR[i]; }
    public static long prev(int i) { return PREV[i]; }

    /** 장이 열려 있는지 (게임 속 06시~18시) */
    public static boolean isLive() { return Math.floorMod(clock, 24000L) < 12000L; }

    public static String updated() {
        long day = clock / 24000L + 1;
        long hour = (Math.floorMod(clock, 24000L) / 1000L + 6) % 24;
        return String.format("%d일차 %02d시", day, hour);
    }

    public static int indexOf(String code) {
        for (int i = 0; i < STOCKS.size(); i++) if (STOCKS.get(i).code().equals(code)) return i;
        return -1;
    }

    /** 서버 틱마다 호출 */
    public static void tick(MinecraftServer server) {
        WildData data = WildData.get(server);
        if (!loaded) {
            loaded = true;
            for (int i = 0; i < STOCKS.size(); i++) {
                long[] v = data.lastPrices.get(STOCKS.get(i).code());
                CUR[i] = v != null ? v[0] : STOCKS.get(i).fallbackCenti();
                PREV[i] = v != null && v.length > 1 ? v[1] : CUR[i];
                FAIR[i] = v != null && v.length > 2 ? v[2] : CUR[i];
            }
        }
        clock = server.overworld().getOverworldClockTime();
        long hour = clock / 1000L, day = clock / 24000L;
        if (lastHour == Long.MIN_VALUE) {
            lastHour = hour;
            lastDay = day;
            return;
        }
        if (day != lastDay) {
            lastDay = day;
            System.arraycopy(CUR, 0, PREV, 0, CUR.length);
            save(data);
        }
        if (hour != lastHour) {
            lastHour = hour;
            if (isLive()) step(server, data);
        }
    }

    /** 명령어용: 지금 바로 한 번 움직이기 */
    public static void refresh(MinecraftServer server) {
        step(server, WildData.get(server));
    }

    private static void step(MinecraftServer server, WildData data) {
        for (int i = 0; i < STOCKS.size(); i++) {
            double vol = STOCKS.get(i).vol() / 100.0;
            // 적정가는 아주 천천히 떠돌고, 가격은 적정가 쪽으로 8%씩 끌려간다
            FAIR[i] = Math.max(100, Math.round(FAIR[i] * (1.0 + RANDOM.nextGaussian() * vol * 0.3)));
            double pull = (FAIR[i] - CUR[i]) / (double) CUR[i] * 0.08;
            double move = RANDOM.nextGaussian() * vol + pull;
            CUR[i] = Math.max(1, Math.round(CUR[i] * (1.0 + move)));
        }
        // 뉴스: 시간당 6%
        if (RANDOM.nextDouble() < 0.06) {
            int i = RANDOM.nextInt(STOCKS.size());
            boolean good = RANDOM.nextBoolean();
            double pct = 0.06 + RANDOM.nextDouble() * 0.10;
            CUR[i] = Math.max(1, Math.round(CUR[i] * (good ? 1 + pct : 1 - pct)));
            FAIR[i] = Math.max(100, Math.round(FAIR[i] * (good ? 1 + pct * 0.6 : 1 - pct * 0.6)));
            String why = (good ? GOOD : BAD)[RANDOM.nextInt(GOOD.length)];
            server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("%s [증권 뉴스] %s: %s (%s%.0f%%)",
                    good ? "📈" : "📉", STOCKS.get(i).name(), why, good ? "+" : "-", pct * 100))
                    .withStyle(good ? ChatFormatting.RED : ChatFormatting.BLUE), false);
            LOG.info("[WildPlus] stock news {} {}", STOCKS.get(i).code(), good);
        }
        save(data);
    }

    private static void save(WildData data) {
        for (int i = 0; i < STOCKS.size(); i++) {
            data.lastPrices.put(STOCKS.get(i).code(), new long[]{CUR[i], PREV[i], FAIR[i]});
        }
        data.setDirty();
    }

    /** 동×100 → "60.25" */
    public static String fmt(long centi) {
        return String.format("%,d.%02d", centi / 100, Math.abs(centi % 100));
    }
}
