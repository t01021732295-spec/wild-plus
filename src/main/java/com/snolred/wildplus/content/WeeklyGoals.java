package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.event.RandomEvents;
import com.snolred.wildplus.item.EventBottleItem;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModItems7;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * 주간 공동 목표 + 시즌 패스.
 * - 매주 금요일(한국 시간)마다 목표 4개가 새로 뽑힌다. 진행도는 서버 모두가 함께 채운다.
 * - 목표가 달성되면 각자 게시판에서 보상(동전 + 시즌 점수 40)을 받는다 (접속 안 했던 친구도 나중에 받을 수 있음).
 * - 시즌(8주)마다 점수 100점마다 한 단계, 10단계 보상을 각자 받는다. 레이드 첫 클리어 60점, 탑은 오른 층 × 4점.
 */
public final class WeeklyGoals {
    public enum Kind {
        KILL_MONSTER("몬스터 처치", 300, 400),
        KILL_ELITE("정예 몬스터 처치", 12, 500),
        MINE_ORE("광석 캐기", 250, 400),
        HARVEST("다 자란 작물 수확", 300, 350),
        CHOP_LOG("원목 베기", 400, 300),
        FISH("물고기 낚기", 60, 400),
        TRADE("상인과 거래", 30, 350),
        TOWER_FLOOR("하늘의 탑 층 돌파", 15, 600),
        RAID_CLEAR("주말 레이드 클리어", 1, 800);

        public final String label;
        public final int target, reward;

        Kind(String label, int target, int reward) {
            this.label = label;
            this.target = target;
            this.reward = reward;
        }
    }

    public static final int GOAL_POINTS = 40, TIER_POINTS = 100, TIERS = 10;

    /** 시즌 패스 단계 보상 */
    public static List<ItemStack> tierReward(int tier) {
        List<ItemStack> out = new ArrayList<>();
        switch (tier) {
            case 0 -> out.add(new ItemStack(ModItems3.GOLD_COIN.get(), 5));
            case 1 -> out.add(EventBottleItem.of(null).copyWithCount(2));
            case 2 -> out.add(new ItemStack(ModItems7.RUBY.get(), 3));
            case 3 -> out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 32));
            case 4 -> out.add(new ItemStack(ModItems.LUCKY_CHARM.get()));
            case 5 -> out.add(new ItemStack(ModItems3.GOLD_COIN.get(), 20));
            case 6 -> out.add(EventBottleItem.of(RandomEvents.Type.GOLD_RUSH).copyWithCount(2));
            case 7 -> out.add(new ItemStack(ModItems7.RUBY_BLOCK.get()));
            case 8 -> out.add(new ItemStack(Items.TOTEM_OF_UNDYING));
            default -> {
                out.add(new ItemStack(Items.NETHERITE_INGOT, 2));
                out.add(new ItemStack(ModItems3.GOLD_COIN.get(), 50));
            }
        }
        return out;
    }

    private WeeklyGoals() {}

    // ───────── 이번 주 목표 ─────────
    public static Kind[] pick(long week) {
        List<Kind> all = new ArrayList<>(List.of(Kind.values()));
        Collections.shuffle(all, new Random(week * 7919L + 17));
        // 탑·레이드 중 하나는 꼭 들어가게
        Kind special = new Random(week).nextBoolean() ? Kind.RAID_CLEAR : Kind.TOWER_FLOOR;
        all.remove(Kind.RAID_CLEAR);
        all.remove(Kind.TOWER_FLOOR);
        return new Kind[]{all.get(0), all.get(1), all.get(2), special};
    }

    public static CompoundTag goals(MinecraftServer server) {
        ContentData d = ContentData.get(server);
        CompoundTag g = d.section("goals");
        long week = ContentData.week();
        if (g.getLongOr("week", Long.MIN_VALUE) != week) {
            g.putLong("week", week);
            Kind[] k = pick(week);
            for (int i = 0; i < 4; i++) {
                g.putString("k" + i, k[i].name());
                g.putInt("p" + i, 0);
            }
            d.dirty();
        }
        return g;
    }

    public static Kind kind(CompoundTag g, int i) {
        try {
            return Kind.valueOf(g.getStringOr("k" + i, "KILL_MONSTER"));
        } catch (IllegalArgumentException e) {
            return Kind.KILL_MONSTER;
        }
    }

    public static void add(MinecraftServer server, Kind kind, int n) {
        if (server == null || n <= 0) return;
        CompoundTag g = goals(server);
        for (int i = 0; i < 4; i++) {
            if (kind(g, i) != kind) continue;
            int before = g.getIntOr("p" + i, 0);
            if (before >= kind.target) continue;
            int after = Math.min(kind.target, before + n);
            g.putInt("p" + i, after);
            ContentData.get(server).dirty();
            if (after >= kind.target) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("★ 주간 목표 달성: " + kind.label + " " + kind.target
                        + " — 목표 게시판에서 보상을 받으세요!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1.0F);
                }
            }
        }
    }

    /** 이번 주 목표 i 보상 받기 */
    public static boolean claimGoal(ServerPlayer p, int i) {
        MinecraftServer server = p.level().getServer();
        CompoundTag g = goals(server);
        Kind k = kind(g, i);
        if (g.getIntOr("p" + i, 0) < k.target) return false;
        ContentData d = ContentData.get(server);
        CompoundTag me = d.player("goals_p", p.getUUID());
        long week = ContentData.week();
        if (me.getLongOr("week", Long.MIN_VALUE) != week) {
            me.putLong("week", week);
            me.putInt("claimed", 0);
        }
        int mask = me.getIntOr("claimed", 0);
        if ((mask & (1 << i)) != 0) return false;
        me.putInt("claimed", mask | (1 << i));
        d.dirty();
        Coins.give(p, k.reward);
        addPoints(p, GOAL_POINTS);
        p.sendSystemMessage(Component.literal(String.format("주간 목표 보상: %,d동 + 시즌 점수 %d", k.reward, GOAL_POINTS)).withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static int claimedGoals(MinecraftServer server, ServerPlayer p) {
        CompoundTag me = ContentData.get(server).player("goals_p", p.getUUID());
        return me.getLongOr("week", Long.MIN_VALUE) == ContentData.week() ? me.getIntOr("claimed", 0) : 0;
    }

    // ───────── 시즌 패스 ─────────
    public static CompoundTag season(MinecraftServer server, java.util.UUID id) {
        ContentData d = ContentData.get(server);
        CompoundTag me = d.player("season_p", id);
        long s = ContentData.season();
        if (me.getLongOr("season", Long.MIN_VALUE) != s) {
            me.putLong("season", s);
            me.putInt("points", 0);
            me.putInt("claimed", 0);
            d.dirty();
        }
        return me;
    }

    public static void addPoints(ServerPlayer p, int n) {
        if (n <= 0) return;
        MinecraftServer server = p.level().getServer();
        CompoundTag me = season(server, p.getUUID());
        int before = me.getIntOr("points", 0);
        me.putInt("points", before + n);
        ContentData.get(server).dirty();
        int tierBefore = before / TIER_POINTS, tierAfter = (before + n) / TIER_POINTS;
        p.sendOverlayMessage(Component.literal("시즌 점수 +" + n).withStyle(ChatFormatting.AQUA));
        if (tierAfter > tierBefore && tierBefore < TIERS) {
            p.sendSystemMessage(Component.literal("✦ 시즌 패스 " + Math.min(TIERS, tierAfter) + "단계 달성! 목표 게시판에서 보상을 받으세요")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static boolean claimTier(ServerPlayer p, int tier) {
        if (tier < 0 || tier >= TIERS) return false;
        MinecraftServer server = p.level().getServer();
        CompoundTag me = season(server, p.getUUID());
        int mask = me.getIntOr("claimed", 0);
        if ((mask & (1 << tier)) != 0 || me.getIntOr("points", 0) < (tier + 1) * TIER_POINTS) return false;
        me.putInt("claimed", mask | (1 << tier));
        ContentData.get(server).dirty();
        for (ItemStack s : tierReward(tier)) {
            if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    // ───────── 진행도 수집 (WildPlus 에서 이벤트 등록) ─────────
    public static void onKill(ServerPlayer killer, net.minecraft.world.entity.LivingEntity dead) {
        if (!(dead instanceof Monster)) return;
        add(killer.level().getServer(), Kind.KILL_MONSTER, 1);
        if (EliteMobs.isElite(dead)) add(killer.level().getServer(), Kind.KILL_ELITE, 1);
    }

    public static void onBreak(ServerPlayer p, BlockState state) {
        MinecraftServer server = p.level().getServer();
        if (state.is(Tags.Blocks.ORES)) add(server, Kind.MINE_ORE, 1);
        else if (state.is(BlockTags.LOGS)) add(server, Kind.CHOP_LOG, 1);
        else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) add(server, Kind.HARVEST, 1);
    }
}
