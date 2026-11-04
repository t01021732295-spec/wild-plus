package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 일일 알바: 게임 속 하루마다 사람마다 알바 3개가 게시판에 붙는다. 하나를 골라 "출근"하면 그 일을 하는 만큼 진행되고,
 * 다 채우면 보수가 통장으로 들어온다 (소득세 적용). 하루에 알바를 하나라도 끝내면 연속 근무 일수가 늘어 보수 +5%씩 (최대 +50%).
 * 일거리: 농장 · 어시장 · 광산 · 벌목 · 경비 · 목장 · 주방 · 땅 고르기.
 */
public final class Jobs {
    private Jobs() {}

    public static final int COUNT = 3;

    public enum Kind {
        FARM("🌾 농장 일손", "다 자란 작물 %d개 수확", 32, 64, 12),
        FISH("🎣 어시장 알바", "물고기 %d마리 낚기", 8, 16, 60),
        MINE("⛏ 광산 보조", "광석 %d개 캐기", 12, 24, 40),
        LOG("🪓 벌목 작업", "원목 %d개 베기", 32, 64, 10),
        GUARD("🗡 마을 경비", "몬스터 %d마리 처치", 10, 20, 45),
        RANCH("🐄 목장 도우미", "동물 %d쌍 번식시키기", 4, 8, 110),
        KITCHEN("🍳 주방 보조", "음식 %d개 만들기 (굽기·조합)", 16, 32, 25),
        DIG("🚜 땅 고르기", "흙·모래·자갈 %d개 파기", 64, 128, 5),
        SERVE("🍽 식당 서빙", "식당 손님 %d명 서빙 (마을 식당 알바 가능)", 4, 8, 90);

        public final String title, task;
        public final int min, max, payEach;

        Kind(String title, String task, int min, int max, int payEach) {
            this.title = title;
            this.task = task;
            this.min = min;
            this.max = max;
            this.payEach = payEach;
        }
    }

    public record Job(Kind kind, int count, long pay) {}

    public static long day(MinecraftServer s) {
        return s.overworld().getGameTime() / 24000L;
    }

    /** 그날 그 사람의 알바 3개 (날짜 · 사람으로 정해지므로 저장 안 함) */
    public static List<Job> forDay(UUID id, long day) {
        Random r = new Random(day * 7_919L ^ id.getMostSignificantBits() * 31 ^ id.getLeastSignificantBits());
        List<Kind> kinds = new ArrayList<>(List.of(Kind.values()));
        List<Job> out = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            Kind k = kinds.remove(r.nextInt(kinds.size()));
            int n = k.min + r.nextInt(k.max - k.min + 1);
            out.add(new Job(k, n, Math.round(n * k.payEach / 10.0) * 10));
        }
        return out;
    }

    private static CompoundTag data(MinecraftServer s, UUID id) {
        CompoundTag d = ContentData.get(s).player("jobs", id);
        long today = day(s);
        if (d.getLongOr("day", -1L) != today) {
            // 새 날: 어제 하나라도 끝냈으면 연속 유지, 아니면 끊김
            long last = d.getLongOr("lastDone", -10L);
            if (last < today - 1) d.putInt("streak", 0);
            d.putLong("day", today);
            d.putInt("done", 0);
            d.putInt("active", -1);
            d.putInt("progress", 0);
            ContentData.get(s).dirty();
        }
        return d;
    }

    private static int bonusPct(CompoundTag d) {
        return Math.min(50, d.getIntOr("streak", 0) * 5);
    }

    // ───────── 게시판 ─────────
    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        List<Job> jobs = forDay(p.getUUID(), day(s));
        int active = d.getIntOr("active", -1), done = d.getIntOr("done", 0), prog = d.getIntOr("progress", 0);
        int bonus = bonusPct(d);
        List<PickMenu.Entry> e = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            long pay = j.pay() * (100 + bonus) / 100;
            String task = String.format(j.kind().task, j.count());
            String sub;
            if ((done & (1 << i)) != 0) sub = "✔ 오늘 완료";
            else if (active == i) sub = String.format("▶ 근무 중 %d/%d · 누르면 그만두기", prog, j.count());
            else sub = String.format("보수 %,d동 · 누르면 출근", pay);
            e.add(new PickMenu.Entry(j.kind().title + " — " + task, sub, ItemStack.EMPTY, PickMenu.K_ROW));
        }
        String header = String.format("오늘의 알바 · 연속 근무 %d일 (보수 +%d%%)", d.getIntOr("streak", 0), bonus);
        PickMenu.open(p, Component.literal("📋 알바 게시판"), header, "", e, PickMenu.FLAG_JOB, i -> {
            if (i < 0 || i >= jobs.size()) return;
            CompoundTag dd = data(s, p.getUUID());
            int act = dd.getIntOr("active", -1);
            if ((dd.getIntOr("done", 0) & (1 << i)) != 0) return;
            if (act == i) {
                dd.putInt("active", -1);
                dd.putInt("progress", 0);
                p.sendSystemMessage(Component.literal("📋 알바를 그만뒀어요 (진행 초기화)").withStyle(ChatFormatting.GRAY));
            } else if (act >= 0) {
                p.sendOverlayMessage(Component.literal("다른 알바 근무 중이에요. 먼저 그만두거나 끝내세요").withStyle(ChatFormatting.RED));
                return;
            } else {
                dd.putInt("active", i);
                dd.putInt("progress", 0);
                Job j = jobs.get(i);
                p.sendSystemMessage(Component.literal("📋 출근! " + j.kind().title + " — " + String.format(j.kind().task, j.count()))
                        .withStyle(ChatFormatting.AQUA));
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            ContentData.get(s).dirty();
            open(p, pos);
        }, null, pl -> true);
    }

    // ───────── 진행 ─────────
    /** 식당에서 손님 1명 서빙 */
    public static void served(ServerPlayer p) {
        add(p, Kind.SERVE, 1);
    }

    private static void add(ServerPlayer p, Kind kind, int n) {
        if (n <= 0) return;
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        int act = d.getIntOr("active", -1);
        if (act < 0) return;
        List<Job> jobs = forDay(p.getUUID(), day(s));
        if (act >= jobs.size()) return;
        Job j = jobs.get(act);
        if (j.kind() != kind) return;
        int prog = Math.min(j.count(), d.getIntOr("progress", 0) + n);
        d.putInt("progress", prog);
        ContentData.get(s).dirty();
        if (prog < j.count()) {
            p.sendOverlayMessage(Component.literal(String.format("📋 %s %d/%d", j.kind().title, prog, j.count())).withStyle(ChatFormatting.YELLOW));
            return;
        }
        // 완료: 보수 지급
        long today = day(s);
        boolean firstToday = d.getLongOr("lastDone", -10L) != today;
        if (firstToday) d.putInt("streak", d.getIntOr("streak", 0) + 1);
        d.putLong("lastDone", today);
        long gross = j.pay() * (100 + bonusPct(d)) / 100;
        long net = Tax.collect(s, p.getUUID(), gross);
        WildData wd = WildData.get(s);
        wd.addBalance(p.getUUID(), net);
        wd.setDirty();
        d.putInt("done", d.getIntOr("done", 0) | (1 << act));
        d.putInt("active", -1);
        d.putInt("progress", 0);
        ContentData.get(s).dirty();
        p.sendSystemMessage(Component.literal(String.format("📋 알바 완료! %s · 보수 %,d동 (세후, 통장으로) · 연속 근무 %d일",
                j.kind().title, net, d.getIntOr("streak", 0))).withStyle(ChatFormatting.GREEN));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.3F);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p)) return;
        var st = event.getState();
        if (st.getBlock() instanceof CropBlock c && c.isMaxAge(st)) add(p, Kind.FARM, 1);
        else if (st.is(Tags.Blocks.ORES)) add(p, Kind.MINE, 1);
        else if (st.is(BlockTags.LOGS)) add(p, Kind.LOG, 1);
        else if (st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(Blocks.GRAVEL)) add(p, Kind.DIG, 1);
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) add(p, Kind.FISH, 1);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster && event.getSource().getEntity() instanceof ServerPlayer p) add(p, Kind.GUARD, 1);
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer p) add(p, Kind.RANCH, 1);
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        ItemStack s = event.getSmelting();
        if (event.getEntity() instanceof ServerPlayer p && s.has(DataComponents.FOOD)) add(p, Kind.KITCHEN, s.getCount());
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        ItemStack s = event.getCrafting();
        if (event.getEntity() instanceof ServerPlayer p && s.has(DataComponents.FOOD)) add(p, Kind.KITCHEN, s.getCount());
    }
}
