package com.snolred.wildplus.content;

import com.snolred.wildplus.content.Mail;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 3.4 주말 대회 (게임 시간 기준). 게임 속 1일차 = 월요일, 7일마다 한 주.
 * 금 · 토요일 오전 6시 ~ 오후 6시 (실제 10분) 에 종목 하나가 열린다: 낚시 · 채굴 · 요리 서빙 · 목장 수확이 번갈아.
 * 끝나면 1 · 2 · 3등은 금 · 은 · 동 트로피 가구와 상금, 점수를 낸 나머지는 참가상을 우편으로 받는다.
 */
public final class Contest {
    private Contest() {}

    public enum Kind {
        FISHING("🎣 낚시 대회", "물고기 1마리 1점 · 등급 물고기는 +등급"),
        MINING("⛏ 채굴 대회", "광석 1개 1점 · 다이아 · 에메랄드 · 모드 보석은 3점"),
        COOKING("🍳 요리 대회", "식당에서 요리 서빙 1접시 2점 · 등급 요리는 +등급"),
        RANCH("🐄 목장 대회", "브러시 · 둥지 · 가공으로 얻은 목장 생산물 1개 1점 · 등급은 +등급");

        public final String title, rule;

        Kind(String title, String rule) {
            this.title = title;
            this.rule = rule;
        }
    }

    public static final String[] WEEKDAY = {"월", "화", "수", "목", "금", "토", "일"};
    public static final int FRI = 4, SAT = 5;
    public static final long START = 0, END = 12000; // 오전 6시 ~ 오후 6시
    public static final long[] PRIZE = {30_000, 15_000, 8_000}, JOIN = {2_000};

    public static String weekdayName(long dayIndex) {
        return WEEKDAY[(int) Math.floorMod(dayIndex, 7)];
    }

    private static long clock(MinecraftServer server) {
        return server.overworld().getOverworldClockTime();
    }

    /** 이 날 열리는 종목 (금 · 토만) */
    public static Kind kindFor(long dayIndex) {
        int wd = (int) Math.floorMod(dayIndex, 7);
        if (wd != FRI && wd != SAT) return null;
        long week = Math.floorDiv(dayIndex, 7);
        return Kind.values()[(int) Math.floorMod(week * 2 + (wd - FRI), Kind.values().length)];
    }

    /** 지금 열려 있는 대회 (없으면 null) */
    public static Kind active(MinecraftServer server) {
        long t = clock(server);
        long tod = Math.floorMod(t, 24000L);
        if (tod < START || tod >= END) return null;
        return kindFor(Math.floorDiv(t, 24000L));
    }

    private static CompoundTag data(MinecraftServer server) {
        return ContentData.get(server).section("contest");
    }

    // ───────── 진행 ─────────
    public static void tick(MinecraftServer server) {
        long t = clock(server);
        long day = Math.floorDiv(t, 24000L);
        long tod = Math.floorMod(t, 24000L);
        CompoundTag d = data(server);
        Kind k = kindFor(day);
        if (k != null && tod >= START && tod < END && d.getLongOr("day", -1) != day) {
            d.putLong("day", day);
            d.putString("kind", k.name());
            d.put("scores", new CompoundTag());
            d.put("names", new CompoundTag());
            d.putBoolean("ended", false);
            d.putBoolean("warned", false);
            ContentData.get(server).dirty();
            broadcast(server, Component.literal("🏆 주말 대회 시작! " + k.title + " — " + k.rule + " · 오후 6시까지 (실제 10분)")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
            }
        }
        if (d.getLongOr("day", -1) == day && !d.getBooleanOr("ended", true)) {
            if (tod >= END - 1200 && tod < END && !d.getBooleanOr("warned", false)) {
                d.putBoolean("warned", true);
                broadcast(server, Component.literal("⏰ 대회 종료 1분 전! 지금 1위: " + leaderText(server)).withStyle(ChatFormatting.YELLOW));
            }
            if (tod >= END || tod < START) finish(server);
        }
    }

    private static void broadcast(MinecraftServer server, Component c) {
        server.getPlayerList().broadcastSystemMessage(c, false);
    }

    /** 점수 더하기: 지금 열린 종목일 때만 */
    public static void add(ServerPlayer p, Kind kind, int pts) {
        MinecraftServer server = p.level().getServer();
        if (server == null || pts <= 0 || active(server) != kind) return;
        CompoundTag d = data(server);
        if (d.getLongOr("day", -1) != Math.floorDiv(clock(server), 24000L) || d.getBooleanOr("ended", true)) return;
        CompoundTag sc = d.getCompoundOrEmpty("scores");
        CompoundTag nm = d.getCompoundOrEmpty("names");
        String id = p.getUUID().toString();
        int now = sc.getIntOr(id, 0) + pts;
        sc.putInt(id, now);
        nm.putString(id, p.getName().getString());
        d.put("scores", sc);
        d.put("names", nm);
        ContentData.get(server).dirty();
        p.sendOverlayMessage(Component.literal(String.format("🏆 %s +%d점 (지금 %d점 · %d위)", kind.title, pts, now, rankOf(server, id)))
                .withStyle(ChatFormatting.GOLD));
    }

    public record Row(String id, String name, int score) {}

    public static List<Row> standings(MinecraftServer server) {
        CompoundTag d = data(server);
        CompoundTag sc = d.getCompoundOrEmpty("scores");
        CompoundTag nm = d.getCompoundOrEmpty("names");
        List<Row> rows = new ArrayList<>();
        for (String id : sc.keySet()) rows.add(new Row(id, nm.getStringOr(id, "?"), sc.getIntOr(id, 0)));
        rows.sort(Comparator.comparingInt(Row::score).reversed());
        return rows;
    }

    private static int rankOf(MinecraftServer server, String id) {
        List<Row> rows = standings(server);
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).id().equals(id)) return i + 1;
        return rows.size() + 1;
    }

    private static String leaderText(MinecraftServer server) {
        List<Row> rows = standings(server);
        return rows.isEmpty() ? "아직 없음" : rows.get(0).name() + " " + rows.get(0).score() + "점";
    }

    private static void finish(MinecraftServer server) {
        CompoundTag d = data(server);
        d.putBoolean("ended", true);
        Kind k;
        try {
            k = Kind.valueOf(d.getStringOr("kind", "FISHING"));
        } catch (IllegalArgumentException e) {
            k = Kind.FISHING;
        }
        List<Row> rows = standings(server);
        // 지난 결과 (시상대 · TV)
        CompoundTag last = new CompoundTag();
        last.putString("kind", k.name());
        last.putLong("day", d.getLongOr("day", 0));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(4, rows.size()); i++) sb.append(i > 0 ? "\n" : "").append(rows.get(i).name()).append('\t').append(rows.get(i).score());
        last.putString("top", sb.toString());
        ContentData.get(server).section("contest_last").merge(last);
        ContentData.get(server).dirty();
        broadcast(server, Component.literal("🏁 " + k.title + " 끝!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (rows.isEmpty()) {
            broadcast(server, Component.literal("참가자가 없었어요. 다음 대회에서 만나요!").withStyle(ChatFormatting.GRAY));
            return;
        }
        String[] medal = {"🥇", "🥈", "🥉"};
        WildData wd = WildData.get(server);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            UUID id = UUID.fromString(r.id());
            long prize = i < 3 ? PRIZE[i] : JOIN[0];
            wd.addBalance(id, prize);
            List<ItemStack> gifts = new ArrayList<>();
            if (i < 3) {
                ItemStack trophy = new ItemStack(i == 0 ? com.snolred.wildplus.registry.ModItems17.TROPHY_GOLD.get()
                        : i == 1 ? com.snolred.wildplus.registry.ModItems17.TROPHY_SILVER.get() : com.snolred.wildplus.registry.ModItems17.TROPHY_BRONZE.get());
                trophy.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(String.format("%s %s %d등 · %s · %d점",
                        weekdayName(d.getLongOr("day", 0)) + "요일", k.title, i + 1, r.name(), r.score())).withStyle(ChatFormatting.GOLD))));
                gifts.add(trophy);
            }
            Mail.put(server, id, gifts);
            if (!gifts.isEmpty()) Mail.notify(server, id, "주말 대회", gifts.size());
            String line = i < 3 ? String.format("%s %d등 %s — %d점 · 상금 %,d동 · %s 트로피 (우편)", medal[i], i + 1, r.name(), r.score(), prize,
                    i == 0 ? "금" : i == 1 ? "은" : "동") : String.format("%d등 %s — %d점 · 참가상 %,d동", i + 1, r.name(), r.score(), prize);
            broadcast(server, Component.literal(line).withStyle(i == 0 ? ChatFormatting.YELLOW : i < 3 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && i < 3) fireworks(p.level(), p.blockPosition(), 3 - i);
        }
    }

    public static void fireworks(net.minecraft.world.level.Level level, BlockPos pos, int n) {
        if (!(level instanceof ServerLevel sl)) return;
        for (int i = 0; i < n; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            FireworkRocketEntity e = new FireworkRocketEntity(sl, pos.getX() + 0.5 + sl.getRandom().nextGaussian() * 0.8, pos.getY() + 1.0,
                    pos.getZ() + 0.5 + sl.getRandom().nextGaussian() * 0.8, rocket);
            sl.addFreshEntity(e);
        }
    }

    // ───────── 보여 주기 (게시판 · TV · 시상대) ─────────
    public static String headline(MinecraftServer server) {
        Kind a = active(server);
        if (a != null) return "주말 대회 진행 중: " + a.title + " · 1위 " + leaderText(server);
        long day = Math.floorDiv(clock(server), 24000L);
        for (int k = 0; k < 8; k++) {
            Kind n = kindFor(day + k);
            if (n != null && (k > 0 || Math.floorMod(clock(server), 24000L) < START)) {
                return k == 0 ? "오늘 " + n.title + " (오전 6시)" : k + "일 뒤 " + weekdayName(day + k) + "요일 " + n.title;
            }
        }
        return "다음 주말 대회를 기다려요";
    }

    public static List<Component> tvLines(MinecraftServer server) {
        List<Component> out = new ArrayList<>();
        Kind a = active(server);
        if (a != null) {
            long left = (END - Math.floorMod(clock(server), 24000L)) / 20;
            out.add(Component.literal(String.format("📡 생중계! %s · 남은 시간 %d분 %02d초", a.title, left / 60, left % 60)).withStyle(ChatFormatting.LIGHT_PURPLE));
            out.add(Component.literal(a.rule).withStyle(ChatFormatting.GRAY));
            List<Row> rows = standings(server);
            if (rows.isEmpty()) out.add(Component.literal("아직 점수를 낸 사람이 없어요!").withStyle(ChatFormatting.WHITE));
            for (int i = 0; i < Math.min(4, rows.size()); i++) {
                out.add(Component.literal(String.format("%d위 %s — %d점", i + 1, rows.get(i).name(), rows.get(i).score()))
                        .withStyle(i == 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            }
        } else {
            out.add(Component.literal("📅 " + headline(server)).withStyle(ChatFormatting.LIGHT_PURPLE));
            out.addAll(lastLines(server));
        }
        return out;
    }

    public static List<Component> lastLines(MinecraftServer server) {
        List<Component> out = new ArrayList<>();
        CompoundTag last = ContentData.get(server).section("contest_last");
        String top = last.getStringOr("top", "");
        if (top.isEmpty()) return out;
        String kind;
        try {
            kind = Kind.valueOf(last.getStringOr("kind", "FISHING")).title;
        } catch (IllegalArgumentException e) {
            kind = "";
        }
        out.add(Component.literal("지난 대회 " + kind + " 결과").withStyle(ChatFormatting.GOLD));
        String[] medal = {"🥇", "🥈", "🥉", "4위"};
        String[] lines = top.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] f = lines[i].split("\t");
            out.add(Component.literal(medal[Math.min(3, i)] + " " + f[0] + " " + (f.length > 1 ? f[1] : "0") + "점").withStyle(ChatFormatting.WHITE));
        }
        return out;
    }

    /** 대회 게시판: 종목 · 남은 시간 · 순위 (목록 화면) */
    public static void openBoard(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        List<PickMenu.Entry> rows = new ArrayList<>();
        Kind a = active(server);
        String header;
        if (a != null) {
            long left = (END - Math.floorMod(clock(server), 24000L)) / 20;
            header = String.format("%s · 남은 %d분 %02d초 · %s", a.title, left / 60, left % 60, a.rule);
            List<Row> st = standings(server);
            String[] medal = {"🥇", "🥈", "🥉"};
            for (int i = 0; i < st.size(); i++) {
                rows.add(PickMenu.Entry.of(PickMenu.K_INFO, (i < 3 ? medal[i] + " " : (i + 1) + "위 ") + st.get(i).name(),
                        st.get(i).score() + "점" + (i < 3 ? String.format(" · 상금 %,d동", PRIZE[i]) : "")));
            }
        } else {
            header = "📅 " + headline(server) + " · 금 · 토요일 오전 6시 ~ 오후 6시 (게임 시간)";
            for (Component c : lastLines(server)) rows.add(PickMenu.Entry.of(PickMenu.K_INFO, c.getString(), ""));
        }
        long day = Math.floorDiv(clock(server), 24000L);
        rows.add(PickMenu.Entry.of(PickMenu.K_INFO, "오늘은 " + (day + 1) + "일차 " + weekdayName(day) + "요일",
                "다음 금요일까지 " + Math.floorMod(FRI - Math.floorMod(day, 7), 7) + "일"));
        PickMenu.open(p, Component.literal("🏆 주말 대회 게시판"), header, "아직 참가자가 없어요 — 지금 바로 점수를 내 보세요!", rows, PickMenu.themed(PickMenu.T_CONTEST), i -> {}, null,
                pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }

    /** 시상대: 올라서서 누르면 폭죽 + 지난 결과 */
    public static void podium(ServerPlayer p, BlockPos pos) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLongOr("wp_podium", -9999L) >= 200) {
            p.getPersistentData().putLong("wp_podium", now);
            fireworks(p.level(), p.blockPosition(), 2);
        }
        List<Component> lines = lastLines(p.level().getServer());
        if (lines.isEmpty()) p.sendSystemMessage(Component.literal("🏆 아직 대회 결과가 없어요 · " + headline(p.level().getServer())).withStyle(ChatFormatting.GOLD));
        for (Component c : lines) p.sendSystemMessage(c);
    }

    // ───────── 시험 · 관리 명령 ─────────
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("contest")
                .executes(ctx -> {
                    for (Component c : tvLines(ctx.getSource().getServer())) ctx.getSource().sendSuccess(() -> c, false);
                    return 1;
                })
                .then(Commands.literal("start").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    // 다음 금요일 오전 6시로 시간을 옮김
                    MinecraftServer server = ctx.getSource().getServer();
                    long t = clock(server);
                    long day = Math.floorDiv(t, 24000L);
                    long add = Math.floorMod(FRI - Math.floorMod(day, 7), 7);
                    long target = (day + add) * 24000L + 10;
                    if (target <= t) target += 7 * 24000L;
                    server.getCommands().performPrefixedCommand(ctx.getSource().withSuppressedOutput(), "time add " + (target - t));
                    ctx.getSource().sendSuccess(() -> Component.literal("금요일 오전 6시로 이동 · 대회 시작"), true);
                    return 1;
                }))
                .then(Commands.literal("finish").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    finish(ctx.getSource().getServer());
                    return 1;
                })));
    }
}
