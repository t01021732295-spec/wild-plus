package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModItems4;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.5 복권 발매기: 즉석 복권 · 숫자 복권(1~25 중 5개). 추첨은 게임 속 토요일 오후 8시 45분, 그 시각을 건너뛰면 다음 확인 때. */
public final class Lotto {
    private static final long TICKET = 10_000L, SCRATCH = 9_000L, BASE_JACKPOT = 50_000_000L;
    private static final int MAX = 25, PICK = 5;
    /** 첫 추첨: 6일차(토) 오후 8시 45분 — 오전 6시가 틱 0, 1시간 = 1000틱 */
    private static final long FIRST_DRAW = 5L * 24_000L + 14_750L, WEEK = 7L * 24_000L;
    private static final Map<UUID, TreeSet<Integer>> PICKS = new HashMap<>();

    private Lotto() {}

    private static CompoundTag data(MinecraftServer s) {
        return ContentData.get(s).section("lotto");
    }

    private static CompoundTag tickets(MinecraftServer s) {
        return ContentData.get(s).section("lotto_tickets");
    }

    private static long jackpot(MinecraftServer s) {
        return data(s).getLongOr("jackpot", BASE_JACKPOT);
    }

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.blockPosition().closerThan(pos, 8);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("즉석 복권 1장", "9,000동 · 우클릭으로 긁기. 최고 500,000동!", new ItemStack(ModItems4.LOTTERY_TICKET.get())));
        e.add(new PickMenu.Entry("숫자 복권 · 자동", "10,000동 · 번호 5개를 무작위로 골라 드려요", new ItemStack(Items.PAPER)));
        e.add(new PickMenu.Entry("숫자 복권 · 직접 고르기", "10,000동 · 1~25 중 5개를 직접 골라요", new ItemStack(Items.WRITABLE_BOOK)));
        e.add(new PickMenu.Entry("내 복권", "이번 회차에 산 번호 보기", new ItemStack(Items.BOOK)));
        e.add(new PickMenu.Entry("지난 추첨 결과", "최근 회차의 당첨 번호", new ItemStack(Items.CLOCK)));
        String head = String.format("%d회차 · 잭팟 %,d동 · 게임 속 토요일 오후 8시 45분 추첨", round(s), jackpot(s));
        PickMenu.open(p, Component.literal("복권 발매기"), head, "", e, PickMenu.FLAG_SHOP, i -> {
            switch (i) {
                case 0 -> buyScratch(p);
                case 1 -> buyAuto(p);
                case 2 -> {
                    PICKS.put(p.getUUID(), new TreeSet<>());
                    picker(p, pos);
                }
                case 3 -> mine(p);
                default -> history(p);
            }
        }, null, x -> near(p, pos));
    }

    private static int round(MinecraftServer s) {
        return Math.max(1, data(s).getIntOr("round", 1));
    }

    private static void buyScratch(ServerPlayer p) {
        if (!Coins.pay(p, SCRATCH)) {
            poor(p, SCRATCH);
            return;
        }
        ItemStack t = new ItemStack(ModItems4.LOTTERY_TICKET.get());
        if (!p.getInventory().add(t)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), t);
        ding(p);
    }

    private static void poor(ServerPlayer p, long price) {
        p.sendOverlayMessage(Component.literal(String.format("%,d동이 필요해요", price)).withStyle(ChatFormatting.RED));
    }

    private static void ding(ServerPlayer p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.7F, 1.4F);
    }

    private static void buyAuto(ServerPlayer p) {
        TreeSet<Integer> n = new TreeSet<>();
        while (n.size() < PICK) n.add(1 + p.getRandom().nextInt(MAX));
        buy(p, n);
    }

    private static void buy(ServerPlayer p, TreeSet<Integer> nums) {
        if (!Coins.pay(p, TICKET)) {
            poor(p, TICKET);
            return;
        }
        MinecraftServer s = p.level().getServer();
        ContentData d = ContentData.get(s);
        CompoundTag data = data(s), all = tickets(s);
        int seq = data.getIntOr("seq", 0) + 1;
        data.putInt("seq", seq);
        data.putLong("sold", data.getLongOr("sold", 0L) + TICKET);
        CompoundTag t = new CompoundTag();
        t.putString("u", p.getUUID().toString());
        t.putString("name", p.getName().getString());
        t.putString("n", join(nums));
        all.put(Integer.toString(seq), t);
        d.dirty();
        ding(p);
        p.sendSystemMessage(Component.literal("🎟 숫자 복권 구매: " + join(nums) + String.format(" · %d회차", round(s))).withStyle(ChatFormatting.GOLD));
    }

    private static String join(java.util.Collection<Integer> nums) {
        StringBuilder b = new StringBuilder();
        for (int n : nums) b.append(b.isEmpty() ? "" : ", ").append(n);
        return b.toString();
    }

    private static void picker(ServerPlayer p, BlockPos pos) {
        TreeSet<Integer> set = PICKS.computeIfAbsent(p.getUUID(), k -> new TreeSet<>());
        List<PickMenu.Entry> e = new ArrayList<>();
        for (int n = 1; n <= MAX; n++) {
            boolean on = set.contains(n);
            e.add(new PickMenu.Entry((on ? "✔ " : "") + n, on ? "선택됨" : "누르면 선택", new ItemStack(on ? Items.EMERALD : Items.PAPER)));
        }
        boolean ready = set.size() == PICK;
        e.add(new PickMenu.Entry(ready ? "이 번호로 구매" : "5개를 골라 주세요", "10,000동", new ItemStack(ready ? Items.EMERALD : Items.BARRIER)));
        PickMenu.open(p, Component.literal("번호 고르기"), "선택: " + (set.isEmpty() ? "없음" : join(set)) + "  (" + set.size() + "/5)", "", e, PickMenu.FLAG_SHOP, i -> {
            if (i == MAX) {
                if (set.size() != PICK) return;
                buy(p, new TreeSet<>(set));
                PICKS.remove(p.getUUID());
                p.closeContainer();
                return;
            }
            int n = i + 1;
            if (!set.remove(n) && set.size() < PICK) set.add(n);
            picker(p, pos);
        }, null, x -> near(p, pos));
    }

    private static void mine(ServerPlayer p) {
        CompoundTag all = tickets(p.level().getServer());
        int c = 0;
        p.sendSystemMessage(Component.literal("── 내 숫자 복권 ──").withStyle(ChatFormatting.GOLD));
        for (String k : all.keySet()) {
            if (all.get(k) instanceof CompoundTag t && t.getStringOr("u", "").equals(p.getUUID().toString())) {
                p.sendSystemMessage(Component.literal("  🎟 " + t.getStringOr("n", "")).withStyle(ChatFormatting.YELLOW));
                c++;
            }
        }
        if (c == 0) p.sendSystemMessage(Component.literal("  이번 회차에 산 복권이 없어요").withStyle(ChatFormatting.GRAY));
        p.closeContainer();
    }

    private static void history(ServerPlayer p) {
        String h = data(p.level().getServer()).getStringOr("history", "");
        p.sendSystemMessage(Component.literal("── 지난 추첨 ──").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal(h.isEmpty() ? "  아직 추첨이 없었어요" : h).withStyle(ChatFormatting.YELLOW));
        p.closeContainer();
    }

    /** 지금까지 지나간 가장 최근 추첨 시각 (게임 시간: 틱 0 = 1일차(월) 오전 6시). 아직 첫 회 전이면 -1 */
    private static long lastSlot(MinecraftServer s) {
        long now = s.overworld().getOverworldClockTime();
        long weeks = Math.floorDiv(now - FIRST_DRAW, WEEK);
        return weeks < 0 ? -1L : FIRST_DRAW + weeks * WEEK;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 100 != 13) return;
        CompoundTag d = data(s);
        long slot = lastSlot(s);
        if (!d.contains("game_last")) {
            d.putLong("game_last", slot);
            ContentData.get(s).dirty();
        } else if (d.getLongOr("game_last", -1L) < slot) {
            d.putLong("game_last", slot);
            draw(s);
        }
    }

    /** 추첨 (시험 명령에서도 부름) */
    public static void draw(MinecraftServer s) {
        ContentData cd = ContentData.get(s);
        CompoundTag d = data(s), all = tickets(s);
        TreeSet<Integer> win = new TreeSet<>();
        var r = s.overworld().getRandom();
        while (win.size() < PICK) win.add(1 + r.nextInt(MAX));
        int round = round(s);
        long jp = jackpot(s), sold = d.getLongOr("sold", 0L);
        Map<String, Integer> best = new HashMap<>();
        List<CompoundTag> first = new ArrayList<>();
        WildData wd = WildData.get(s);
        int[] counts = new int[PICK + 1];
        for (String k : new ArrayList<>(all.keySet())) {
            if (!(all.get(k) instanceof CompoundTag t)) continue;
            int m = 0;
            for (String part : t.getStringOr("n", "").split(", ")) if (!part.isEmpty() && win.contains(Integer.parseInt(part))) m++;
            counts[m]++;
            if (m == PICK) first.add(t);
            else if (m >= 2) pay(s, wd, t, m == 4 ? 1_000_000L : m == 3 ? 50_000L : 10_000L, m + "개 일치");
        }
        if (!first.isEmpty()) {
            long each = jp / first.size();
            for (CompoundTag t : first) pay(s, wd, t, each, "잭팟!");
            jp = BASE_JACKPOT;
        } else {
            jp += sold * 3 / 10;
        }
        d.putLong("jackpot", jp);
        d.putLong("sold", 0L);
        d.putInt("round", round + 1);
        String res = String.format("%d회차 당첨 번호: %s  (5개 %d명 · 4개 %d명 · 3개 %d명 · 2개 %d명)", round, join(win), counts[5], counts[4], counts[3], counts[2]);
        d.putString("history", res);
        for (String k : new ArrayList<>(all.keySet())) all.remove(k);
        cd.dirty();
        s.getPlayerList().broadcastSystemMessage(Component.literal("🎰 " + res).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        s.getPlayerList().broadcastSystemMessage(Component.literal(String.format("   다음 회차 잭팟: %,d동", jp)).withStyle(ChatFormatting.YELLOW), false);
    }

    private static void pay(MinecraftServer s, WildData wd, CompoundTag t, long amount, String why) {
        UUID id = UUID.fromString(t.getStringOr("u", ""));
        wd.reason = "숫자 복권 당첨";
        wd.addBalance(id, amount);
        com.snolred.wildplus.content.Codex.add(s, id, "lotto", 1);
        ServerPlayer on = s.getPlayerList().getPlayer(id);
        if (on != null) on.sendSystemMessage(Component.literal(String.format("🎉 숫자 복권 %s  %,d동이 통장에 들어왔어요", why, amount)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
    }
}
