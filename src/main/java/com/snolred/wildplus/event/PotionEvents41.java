package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModEffects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.1 신규 포션: 공허 회피 · 회복 오라 · 광맥의 눈 · 시간 왜곡 (철벽의 숨결은 속성만) */
public final class PotionEvents41 {
    private PotionEvents41() {}

    private static final String ORE_MARK = "wildplus_ore_mark";
    private static final Map<UUID, Integer> EVADE_READY = new HashMap<>();
    private static final Map<LivingEntity, Integer> WARPED = new HashMap<>();
    private static final Map<Mob, Integer> MARKS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || p.tickCount % 20 != 0) return;
        int now = level.getServer().getTickCount();
        if (p.hasEffect(ModEffects.HEAL_AURA)) {
            for (Player o : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(5.0))) {
                o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
            }
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 3, 1.5, 0.4, 1.5, 0);
        }
        if (p.hasEffect(ModEffects.TIME_WARP)) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0))) {
                if (!(e instanceof Enemy)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                WARPED.put(e, now + 40);
            }
            level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 4.0, 1.0, 4.0, 0.4);
        }
        if (p.hasEffect(ModEffects.ORE_SIGHT) && p.tickCount % 40 == 0) oreSight(level, p, now);
    }

    private static void oreSight(ServerLevel level, ServerPlayer p, int now) {
        List<BlockPos> ores = new ArrayList<>();
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-12, -12, -12), c.offset(12, 12, 12))) {
            if (level.getBlockState(q).is(Tags.Blocks.ORES)) ores.add(q.immutable());
        }
        ores.sort(Comparator.comparingDouble(q -> q.distSqr(c)));
        for (BlockPos q : ores.subList(0, Math.min(16, ores.size()))) {
            Mob m = EntityTypes.SHULKER.create(level, EntitySpawnReason.TRIGGERED);
            if (m == null) continue;
            m.snapTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, 0F, 0F);
            m.setNoAi(true);
            m.setSilent(true);
            m.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 255, false, false));
            m.setInvisible(true);
            m.setGlowingTag(true);
            m.getPersistentData().putBoolean(ORE_MARK, true);
            MARKS.put(m, now + 50);
            level.addFreshEntity(m);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        for (Iterator<Map.Entry<Mob, Integer>> it = MARKS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Mob, Integer> e = it.next();
            if (now >= e.getValue()) {
                e.getKey().discard();
                it.remove();
            }
        }
        if (now % 20 == 0) WARPED.entrySet().removeIf(e -> e.getValue() <= now || !e.getKey().isAlive());
    }

    /** 재시작 등으로 남은 광맥 표시 정리 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob m && m.getPersistentData().getBooleanOr(ORE_MARK, false) && !MARKS.containsKey(m)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        int now = level.getServer().getTickCount();
        if (event.getSource().getEntity() instanceof LivingEntity a && now < WARPED.getOrDefault(a, 0)) {
            event.setAmount(event.getAmount() * 0.7F);
        }
        if (!(target instanceof ServerPlayer p) || !p.hasEffect(ModEffects.VOID_EVADE) || now < EVADE_READY.getOrDefault(p.getUUID(), 0)
                || p.getRandom().nextFloat() >= 0.25F) return;
        Vec3 back = p.getLookAngle().multiply(-1, 0, -1).normalize();
        double best = 0;
        for (double d = 0.5; d <= 5.0; d += 0.5) {
            if (!level.noCollision(p, p.getBoundingBox().move(back.scale(d)))) break;
            best = d;
        }
        EVADE_READY.put(p.getUUID(), now + 160);
        event.setCanceled(true);
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 50, 0.4, 0.7, 0.4, 0.5);
        if (best >= 1.0) {
            Vec3 to = p.position().add(back.scale(best));
            p.teleportTo(to.x, to.y, to.z);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
    }
}
