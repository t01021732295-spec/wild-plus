package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 보유세: 게임 하루에 한 번, 통장 + 가진 동전을 합친 재산에 누진 세율로 걷어 국고로 보낸다 (돈이 사라짐).
 * 10만동까지 0% · 10만~100만 1% · 100만~1,000만 2% · 1,000만 넘는 부분 3%.
 * 접속해 있을 때만 걷는다 (주말에만 놀아도 손해 없음). 통장에서 먼저 빠짐.
 */
public final class WealthTax {
    private WealthTax() {}

    private static final long[] LIMITS = {100_000, 1_000_000, 10_000_000, Long.MAX_VALUE};
    private static final int[] RATES = {0, 1, 2, 3};

    public static long taxFor(long wealth) {
        long tax = 0, lower = 0;
        for (int i = 0; i < LIMITS.length && wealth > lower; i++) {
            long part = Math.min(wealth, LIMITS[i]) - lower;
            tax += part / 100 * RATES[i];
            lower = LIMITS[i];
        }
        return tax;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 200 != 37) return;
        var s = p.level().getServer();
        CompoundTag me = ContentData.get(s).player("wealth_tax", p.getUUID());
        long day = ContentData.gameDay;
        long last = me.getLongOr("day", -1L);
        if (last >= day) return;
        me.putLong("day", day);
        ContentData.get(s).dirty();
        if (last < 0) return; // 처음엔 기록만
        long wealth = Honor.wealth(s, p);
        long tax = taxFor(wealth);
        if (tax <= 0) return;
        if (Honor.pay(p, tax)) {
            me.putLong("paid", me.getLongOr("paid", 0L) + tax);
            p.sendSystemMessage(Component.literal(String.format("🏦 보유세 %,d동 (재산 %,d동 기준 · 10만동 넘는 부분에 1~3%%)", tax, wealth))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
