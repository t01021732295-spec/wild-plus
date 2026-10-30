package com.snolred.wildplus.economy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * 주식 시장. 실제 주가를 인터넷(야후 파이낸스 공개 시세)에서 10분마다 받아 게임 화폐로 환산한다.
 * 환산: 1,000원 = 1동 (달러 주식은 실시간 환율로 원화 환산 후 같은 비율).
 * 인터넷이 안 되면 마지막 시세에서 조금씩 움직이는 가상 시세로 대신한다.
 * 가격은 내부적으로 "동 × 100" 단위(소수 둘째 자리)로 다룬다.
 */
public final class StockMarket {
    private static final Logger LOG = LogUtils.getLogger();

    public record Stock(String code, String symbol, String name, boolean usd, long fallbackCenti) {}

    public static final List<Stock> STOCKS = List.of(
            new Stock("samsung", "005930.KS", "삼성전자", false, 6000),
            new Stock("skhynix", "000660.KS", "SK하이닉스", false, 20000),
            new Stock("hyundai", "005380.KS", "현대차", false, 25000),
            new Stock("naver", "035420.KS", "NAVER", false, 20000),
            new Stock("kakao", "035720.KS", "카카오", false, 4000),
            new Stock("apple", "AAPL", "애플", true, 32000),
            new Stock("nvidia", "NVDA", "엔비디아", true, 25000),
            new Stock("tesla", "TSLA", "테슬라", true, 35000),
            new Stock("microsoft", "MSFT", "마이크로소프트", true, 58000),
            new Stock("google", "GOOGL", "구글", true, 24000));

    private static final long REFRESH_MS = 10 * 60 * 1000L;
    private static final long[] CUR = new long[STOCKS.size()];
    private static final long[] PREV = new long[STOCKS.size()];
    private static volatile boolean live;
    private static volatile String updated = "-";
    private static long nextFetch;
    private static boolean loaded;
    private static volatile boolean fetching;
    private static final Random RANDOM = new Random();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private StockMarket() {}

    public static long price(int i) { return CUR[i]; }
    public static long prev(int i) { return PREV[i]; }
    public static boolean isLive() { return live; }
    public static String updated() { return updated; }

    public static int indexOf(String code) {
        for (int i = 0; i < STOCKS.size(); i++) if (STOCKS.get(i).code().equals(code)) return i;
        return -1;
    }

    /** 서버 틱마다 호출 (1초 간격이면 충분) */
    public static void tick(MinecraftServer server) {
        if (!loaded) {
            loaded = true;
            WildData data = WildData.get(server);
            for (int i = 0; i < STOCKS.size(); i++) {
                long[] v = data.lastPrices.get(STOCKS.get(i).code());
                CUR[i] = v != null ? v[0] : STOCKS.get(i).fallbackCenti();
                PREV[i] = v != null ? v[1] : CUR[i];
            }
        }
        long now = System.currentTimeMillis();
        if (now >= nextFetch && !fetching) {
            nextFetch = now + REFRESH_MS;
            refresh(server);
        }
    }

    public static void refresh(MinecraftServer server) {
        fetching = true;
        CompletableFuture.supplyAsync(StockMarket::fetchAll).whenComplete((result, err) -> server.execute(() -> {
            fetching = false;
            WildData data = WildData.get(server);
            int ok = 0;
            if (result != null) {
                for (int i = 0; i < STOCKS.size(); i++) {
                    if (result[i] != null) {
                        CUR[i] = Math.max(1, result[i][0]);
                        PREV[i] = Math.max(1, result[i][1]);
                        ok++;
                    }
                }
            }
            live = ok > 0;
            if (ok < STOCKS.size()) {
                // 못 받은 종목은 가상으로 조금씩 움직인다 (±1.5%)
                for (int i = 0; i < STOCKS.size(); i++) {
                    if (result == null || result[i] == null) {
                        CUR[i] = Math.max(1, Math.round(CUR[i] * (1.0 + (RANDOM.nextDouble() - 0.5) * 0.03)));
                    }
                }
            }
            for (int i = 0; i < STOCKS.size(); i++) {
                data.lastPrices.put(STOCKS.get(i).code(), new long[]{CUR[i], PREV[i]});
            }
            data.setDirty();
            updated = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
            LOG.info("[WildPlus] 주식 시세 갱신: {}/{} 실시간{}", ok, STOCKS.size(), err != null ? " (" + err + ")" : "");
        }));
    }

    /** 다른 스레드에서 실행: [i] = {현재가, 전일가} (동×100), 실패하면 null */
    private static long[][] fetchAll() {
        long[][] out = new long[STOCKS.size()][];
        double usdKrw = quote("KRW=X").map(q -> q[0]).orElse(Double.NaN);
        for (int i = 0; i < STOCKS.size(); i++) {
            Stock s = STOCKS.get(i);
            final double rate = s.usd() ? usdKrw : 1.0;
            if (Double.isNaN(rate)) continue;
            int idx = i;
            quote(s.symbol()).ifPresent(q -> out[idx] = new long[]{toCenti(q[0] * rate), toCenti(q[1] * rate)});
        }
        return out;
    }

    /** 원화 → 동×100 (1,000원 = 1동) */
    private static long toCenti(double krw) {
        return Math.round(krw / 10.0);
    }

    /** {현재가, 전일 종가} (원래 통화) */
    private static java.util.Optional<double[]> quote(String symbol) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                            "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?range=1d&interval=1d"))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (WildPlus Minecraft mod)")
                    .GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return java.util.Optional.empty();
            JsonObject meta = JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonObject("chart")
                    .getAsJsonArray("result").get(0).getAsJsonObject().getAsJsonObject("meta");
            OptionalDouble cur = num(meta, "regularMarketPrice");
            OptionalDouble prev = num(meta, "chartPreviousClose");
            if (prev.isEmpty()) prev = num(meta, "previousClose");
            if (cur.isEmpty()) return java.util.Optional.empty();
            return java.util.Optional.of(new double[]{cur.getAsDouble(), prev.orElse(cur.getAsDouble())});
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private static OptionalDouble num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? OptionalDouble.of(o.get(key).getAsDouble()) : OptionalDouble.empty();
    }

    /** 동×100 → "60.25" */
    public static String fmt(long centi) {
        return String.format("%,d.%02d", centi / 100, Math.abs(centi % 100));
    }
}
