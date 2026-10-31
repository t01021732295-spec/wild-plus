package com.snolred.wildplus.event;

import com.snolred.wildplus.WildConfig;
import com.snolred.wildplus.WildPlus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 블러드문: 밤이 시작될 때 일정 확률로 발생.
 * - 보스바로 남은 시간 표시, 잠들 수 없음
 * - 주변에 몬스터가 추가로 출몰하고, 새로 나타나는 몬스터는 힘·신속 버프
 * - 처치 시 핏빛 파편 드롭 확률, 경험치 2배 (EliteMobs 참고)
 */
public final class BloodMoon {
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;
    private static final int SPAWN_INTERVAL = 200;
    private static final int MOB_CAP_NEAR_PLAYER = 30;

    private static boolean active;
    private static long lastRolledDay = Long.MIN_VALUE;
    private static ServerBossEvent bar;
    private static boolean sawNight;
    private static final java.util.UUID BAR_ID = java.util.UUID.nameUUIDFromBytes("wildplus:blood_moon".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private BloodMoon() {}

    public static boolean isActive(Level level) {
        return active && level.dimension() == Level.OVERWORLD;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long time = TimeHelper.dayTime(level);
        long day = time / 24000L;
        long timeOfDay = time % 24000L;
        boolean night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;

        if (night && !active && lastRolledDay != day) {
            lastRolledDay = day;
            if (day >= WildConfig.BLOOD_MOON_GRACE_DAYS.getAsInt()
                    && level.getRandom().nextDouble() < WildConfig.BLOOD_MOON_CHANCE.get()) {
                start(level);
            }
        } else if (night && active) {
            sawNight = true;
        } else if (!night && active && sawNight) {
            stop(level, true);
        }

        if (active) {
            if (level.getGameTime() % 20 == 0) {
                updateBar(level, timeOfDay);
            }
            if (level.getGameTime() % SPAWN_INTERVAL == 0) {
                spawnExtras(level);
            }
        }
    }

    public static void start(ServerLevel level) {
        if (active) {
            return;
        }
        active = true;
        BloodMoonReward.reset();
        BloodLord.reset();
        long tod = TimeHelper.dayTime(level) % 24000L;
        sawNight = tod >= NIGHT_START && tod < NIGHT_END;
        lastRolledDay = TimeHelper.dayTime(level) / 24000L;
        if (bar == null) {
            bar = new ServerBossEvent(BAR_ID, Component.translatable("bossbar.wildplus.blood_moon").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(true);
            bar.setCreateWorldFog(true);
        }
        bar.setVisible(true);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.translatable("message.wildplus.blood_moon.start")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 0.6F);
        }
        updateBar(level, TimeHelper.dayTime(level) % 24000L);
        WildPlus.LOGGER.info("Blood Moon started");
    }

    public static void stop(ServerLevel level, boolean survived) {
        if (!active) {
            return;
        }
        active = false;
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
        if (survived) {
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.translatable("message.wildplus.blood_moon.end")
                        .withStyle(ChatFormatting.GOLD));
            }
            BloodMoonReward.reward(level);
        }
        WildPlus.LOGGER.info("Blood Moon ended");
    }

    private static void updateBar(ServerLevel level, long timeOfDay) {
        if (bar == null) {
            return;
        }
        float remaining = 1.0F - Mth.clamp((timeOfDay - NIGHT_START) / (float) (NIGHT_END - NIGHT_START), 0.0F, 1.0F);
        bar.setProgress(remaining);
        List<ServerPlayer> inLevel = level.players();
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) {
            if (!inLevel.contains(p)) {
                bar.removePlayer(p);
            }
        }
        for (ServerPlayer p : inLevel) {
            if (!bar.getPlayers().contains(p)) {
                bar.addPlayer(p);
            }
        }
    }

    // ───────── 추가 출몰 ─────────

    private static final List<EntityType<? extends Mob>> HORDE = List.of(
            EntityTypes.ZOMBIE, EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.CREEPER);

    private static void spawnExtras(ServerLevel level) {
        int count = WildConfig.BLOOD_MOON_EXTRA_SPAWNS.getAsInt();
        if (count <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            int nearby = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(40.0)).size();
            if (nearby >= MOB_CAP_NEAR_PLAYER) {
                continue;
            }
            for (int i = 0; i < count; i++) {
                trySpawnNear(level, player, random);
            }
        }
    }

    private static void trySpawnNear(ServerLevel level, Player player, RandomSource random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 18.0 + random.nextDouble() * 14.0;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            // 플레이어가 동굴 깊숙이 있으면 지표면에 소환하지 않음
            if (Math.abs(top.getY() - player.getY()) > 12) {
                continue;
            }
            BlockState below = level.getBlockState(top.below());
            if (!below.isSolid() || !level.getFluidState(top).isEmpty()
                    || !level.getBlockState(top).isAir() || !level.getBlockState(top.above()).isAir()) {
                continue;
            }
            EntityType<? extends Mob> type = HORDE.get(random.nextInt(HORDE.size()));
            Mob mob = type.create(level, EntitySpawnReason.EVENT);
            if (mob == null) {
                return;
            }
            mob.snapTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(top), EntitySpawnReason.EVENT, null);
            level.addFreshEntityWithPassengers(mob);
            return;
        }
    }

    // ───────── 수면 금지 ─────────

    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        if (isActive(event.getLevel()) && event.getVanillaProblem() == null) {
            event.setProblem(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().sendOverlayMessage(Component.translatable("message.wildplus.blood_moon.no_sleep")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        active = false;
        lastRolledDay = Long.MIN_VALUE;
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }
}
