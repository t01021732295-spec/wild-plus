package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 4.1 은행 대출. 게임 속 하루 단위. 신용점수 300~1000 (시작 600), 한도 = 최근 7일 소득 × 신용 배율(1~3배, 최소 10만 · 최대 5,000만),
 * 금리 하루 1.5~3%, 원리금 균등 7/14일 자동이체. 연체 금리 +2%p · 신용 -30, 3일 연속 출하 수익 30% 압류, 7일 연속 송금 · 명품관 · 신규 대출 금지.
 */
public final class Loans {
    private Loans() {}

    public static final long MAX_LIMIT = 50_000_000L, MIN_LIMIT = 100_000L;

    private static CompoundTag tag(MinecraftServer s, UUID id) {
        return ContentData.get(s).player("loans", id);
    }

    private static void save(MinecraftServer s) {
        ContentData.get(s).dirty();
    }

    public static int credit(MinecraftServer s, UUID id) {
        return tag(s, id).getIntOr("credit", 600);
    }

    private static void addCredit(MinecraftServer s, UUID id, int d) {
        CompoundTag t = tag(s, id);
        t.putInt("credit", Math.max(300, Math.min(1000, credit(s, id) + d)));
    }

    /** 만분율 (150 = 1.5%) */
    public static int rate(int credit) {
        return 300 - (credit - 300) * 150 / 700;
    }

    public static long debt(MinecraftServer s, UUID id) {
        return tag(s, id).getLongOr("debt", 0L);
    }

    public static int daysLeft(MinecraftServer s, UUID id) {
        return tag(s, id).getIntOr("days", 0);
    }

    public static int streak(MinecraftServer s, UUID id) {
        return tag(s, id).getIntOr("streak", 0);
    }

    public static boolean locked(MinecraftServer s, UUID id) {
        return streak(s, id) >= 7;
    }

    public static long payment(MinecraftServer s, UUID id) {
        return tag(s, id).getLongOr("pay", 0L);
    }

    public static int activeRate(MinecraftServer s, UUID id) {
        return tag(s, id).getIntOr("rate", rate(credit(s, id)));
    }

    public static void addIncome(MinecraftServer s, UUID id, long gross) {
        if (gross <= 0) return;
        CompoundTag inc = tag(s, id).getCompoundOrEmpty("inc");
        String key = Long.toString(ContentData.gameDay);
        inc.putLong(key, inc.getLongOr(key, 0L) + gross);
        for (String k : java.util.List.copyOf(inc.keySet())) {
            if (Long.parseLong(k) < ContentData.gameDay - 6) inc.remove(k);
        }
        tag(s, id).put("inc", inc);
        save(s);
    }

    public static long limit(MinecraftServer s, UUID id) {
        CompoundTag inc = tag(s, id).getCompoundOrEmpty("inc");
        long sum = 0;
        for (String k : inc.keySet()) sum += inc.getLongOr(k, 0L);
        long x100 = 100 + (credit(s, id) - 300) * 200L / 700;
        return Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, sum * x100 / 100));
    }

    public static long annuity(long principal, int rate, int term) {
        double r = rate / 10000.0;
        return Math.max(1L, Math.round(principal * r / (1 - Math.pow(1 + r, -term))));
    }

    /** 실행 성공 여부 */
    public static boolean take(ServerPlayer p, long amount, int term) {
        MinecraftServer s = p.level().getServer();
        UUID id = p.getUUID();
        if (debt(s, id) > 0 || locked(s, id) || amount <= 0 || amount > limit(s, id)) return false;
        int r = rate(credit(s, id));
        CompoundTag t = tag(s, id);
        t.putLong("debt", amount);
        t.putInt("days", term);
        t.putInt("rate", r);
        t.putLong("pay", annuity(amount, r, term));
        t.putInt("streak", 0);
        WildData wd = WildData.get(s);
        wd.reason = "대출 실행";
        wd.addBalance(id, amount);
        save(s);
        return true;
    }

    /** 전액 상환 (원금의 1% 수수료) */
    public static boolean repayAll(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        UUID id = p.getUUID();
        long debt = debt(s, id), total = debt + debt / 100;
        WildData wd = WildData.get(s);
        if (debt <= 0 || wd.balance(id) < total) return false;
        wd.reason = "대출 전액 상환";
        wd.addBalance(id, -total);
        clear(s, id);
        addCredit(s, id, 20);
        save(s);
        return true;
    }

    private static void clear(MinecraftServer s, UUID id) {
        CompoundTag t = tag(s, id);
        t.putLong("debt", 0L);
        t.putInt("days", 0);
        t.putInt("streak", 0);
        t.putLong("pay", 0L);
    }

    /** 하루가 바뀔 때 (EconomyTick) */
    public static void dailyTick(MinecraftServer s) {
        WildData wd = WildData.get(s);
        CompoundTag all = ContentData.get(s).section("loans");
        for (String key : java.util.List.copyOf(all.keySet())) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            long debt = debt(s, id);
            if (debt <= 0) continue;
            CompoundTag t = tag(s, id);
            int r = activeRate(s, id);
            long pay = Math.min(t.getLongOr("pay", 0L), debt + debt * r / 10000);
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (wd.balance(id) >= pay) {
                wd.reason = "대출 자동 상환";
                wd.addBalance(id, -pay);
                long left = debt + debt * r / 10000 - pay;
                int days = t.getIntOr("days", 1) - 1;
                t.putInt("streak", 0);
                addCredit(s, id, 5);
                if (days <= 0 || left <= 0) {
                    clear(s, id);
                    addCredit(s, id, 30);
                    if (p != null) p.sendSystemMessage(Component.literal("🏦 대출을 모두 갚았어요! 신용점수가 올랐어요").withStyle(ChatFormatting.GREEN));
                } else {
                    t.putLong("debt", left);
                    t.putInt("days", days);
                    if (p != null) p.sendSystemMessage(Component.literal(String.format("🏦 대출 %,d동 자동 상환 · 남은 빚 %,d동 (%d일)", pay, left, days)).withStyle(ChatFormatting.GRAY));
                }
            } else {
                int streak = t.getIntOr("streak", 0) + 1;
                t.putInt("streak", streak);
                t.putLong("debt", debt + debt * (r + 200) / 10000);
                addCredit(s, id, -30);
                if (p != null) {
                    String warn = streak >= 7 ? " · 송금 · 명품관 · 신규 대출 금지" : streak >= 3 ? " · 출하 수익 30% 압류 중" : "";
                    p.sendSystemMessage(Component.literal(String.format("🏦 대출 연체 %d일째 (필요 %,d동)%s", streak, pay, warn)).withStyle(ChatFormatting.RED));
                }
            }
        }
        save(s);
    }

    /** 3일 이상 연체: 출하 수익의 30%를 압류해 빚을 갚고 나머지를 돌려준다 */
    public static long garnish(MinecraftServer s, UUID id, long net) {
        if (net <= 0 || streak(s, id) < 3 || debt(s, id) <= 0) return net;
        long take = Math.min(debt(s, id), net * 30 / 100);
        CompoundTag t = tag(s, id);
        long left = debt(s, id) - take;
        if (left <= 0) clear(s, id);
        else t.putLong("debt", left);
        save(s);
        return net - take;
    }
}
