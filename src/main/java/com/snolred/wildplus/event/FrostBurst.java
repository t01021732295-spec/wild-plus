package com.snolred.wildplus.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.9.8 스카이 사파이어 빙결 폭발 (검 · 활 확정): 주변 3칸 적에게 피해 6 · 정지 · 구속 III · 동상(0.5초마다 1) · 얼림.
 * 지속: 검 정지 1.5초 · 구속/동상 4초 / 활 정지 0.75초 · 구속/동상 2.5초.
 * 정지는 같은 대상에 4초에 한 번, 보스와 플레이어는 정지만 면역.
 */
public final class FrostBurst {
    private FrostBurst() {}

    private static final String FROZEN_TAG = "wildplus_frost_stop";
    private static final int RE_STOP = 80;
    /** 약하게 연타해도 매번 터지지 않도록 검은 폭발 사이 최소 간격 */
    private static final int SWORD_GAP = 8;

    private static final class Dot {
        final Player owner;
        int left;
        int next;

        Dot(Player owner, int left, int next) {
            this.owner = owner;
            this.left = left;
            this.next = next;
        }
    }

    private static final Map<Mob, Integer> STOPPED = new HashMap<>();
    private static final Map<LivingEntity, Dot> DOTS = new HashMap<>();
    private static final Map<UUID, Integer> STOP_READY = new HashMap<>();
    private static final Map<UUID, Integer> SWORD_READY = new HashMap<>();

    public static void burst(ServerLevel level, Player owner, LivingEntity target, boolean sword) {
        int now = level.getServer().getTickCount();
        if (sword) {
            if (now < SWORD_READY.getOrDefault(owner.getUUID(), 0)) return;
            SWORD_READY.put(owner.getUUID(), now + SWORD_GAP);
        }
        int stop = sword ? 30 : 15;
        int slow = sword ? 80 : 50;
        int dotHits = sword ? 8 : 5;
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.6, target.getZ(), 50, 1.5, 0.5, 1.5, 0.05);
        level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0))) {
            if (e == owner || !(e instanceof Enemy || (e == target && e instanceof Player))) continue;
            hurt(level, e, owner, 6.0F);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 2));
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + slow));
            DOTS.put(e, new Dot(owner, dotHits, now + 10));
            if (e instanceof Mob m) stop(m, stop, now);
        }
        if (STOP_READY.size() > 256) STOP_READY.values().removeIf(t -> t <= now);
    }

    /** 추가 피해가 무적 시간을 건드려 플레이어의 다음 공격을 막지 않도록 앞뒤로 되돌린다 */
    private static void hurt(ServerLevel level, LivingEntity e, Player owner, float amount) {
        int invul = e.getInvulnerableTime();
        e.setInvulnerableTime(0);
        e.hurtServer(level, level.damageSources().indirectMagic(e, owner), amount);
        e.setInvulnerableTime(invul);
    }

    private static boolean boss(Mob m) {
        return DungeonGuardian.isBoss(m) || m.getMaxHealth() >= 150.0F; // 드래곤 · 위더 · 워든 · 핏빛 군주도 여기 걸림
    }

    private static void stop(Mob m, int ticks, int now) {
        if (boss(m) || m.isNoAi() || now < STOP_READY.getOrDefault(m.getUUID(), 0)) return;
        STOP_READY.put(m.getUUID(), now + RE_STOP);
        m.setNoAi(true);
        m.getPersistentData().putBoolean(FROZEN_TAG, true);
        STOPPED.put(m, now + ticks);
    }

    private static void release(Mob m) {
        m.setNoAi(false);
        m.getPersistentData().remove(FROZEN_TAG);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        for (Iterator<Map.Entry<Mob, Integer>> it = STOPPED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Mob, Integer> en = it.next();
            if (now >= en.getValue() || en.getKey().isRemoved()) {
                release(en.getKey());
                it.remove();
            }
        }
        if (DOTS.isEmpty()) return;
        // 동상으로 죽으며 새 폭발이 생겨도 안전하도록 복사본을 돈다
        for (Map.Entry<LivingEntity, Dot> en : new HashMap<>(DOTS).entrySet()) {
            LivingEntity e = en.getKey();
            Dot d = en.getValue();
            if (e.isRemoved() || !e.isAlive() || !(e.level() instanceof ServerLevel level)) {
                DOTS.remove(e);
                continue;
            }
            if (now < d.next) continue;
            d.next = now + 10;
            hurt(level, e, d.owner, 1.0F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 4, 0.25, 0.25, 0.25, 0.01);
            if (--d.left <= 0) DOTS.remove(e);
        }
    }

    /** 정지 중 청크가 내려가거나 서버가 죽어 저장된 몹이 굳은 채로 남지 않게 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob m
                && m.getPersistentData().getBooleanOr(FROZEN_TAG, false) && !STOPPED.containsKey(m)) {
            release(m);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        STOPPED.keySet().forEach(FrostBurst::release);
        STOPPED.clear();
        DOTS.clear();
        STOP_READY.clear();
        SWORD_READY.clear();
    }
}
