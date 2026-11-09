package com.snolred.wildplus.event;

import com.snolred.wildplus.entity.EndMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 4.3 엔드 몬스터 능력: 추적자 실명 · 진드기 폭발 · 망령 추락 */
public final class EndMobEvents {
    private static final String GROUNDED = "wp_grounded_until";

    private EndMobEvents() {}

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof EndMobs.Stalker) EndMobs.onStalkerHit(event.getEntity());
        if (event.getSource().getEntity() instanceof EndMobs.Wraith && event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().putLong(GROUNDED, p.level().getGameTime() + 100L);
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("망령이 날개를 붙잡았어요! (5초간 비행 불가)").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.level().getGameTime() >= p.getPersistentData().getLongOr(GROUNDED, 0L) || p.getAbilities().instabuild) return;
        if (p.isFallFlying()) p.stopFallFlying();
        if (p.getAbilities().flying) {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof EndMobs.Mite) || !(e.level() instanceof ServerLevel level)) return;
        for (Player p : level.getEntitiesOfClass(Player.class, e.getBoundingBox().inflate(3.0))) {
            p.hurtServer(level, level.damageSources().magic(), 4.0F);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.3, e.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.3, e.getZ(), 30, 1.0, 0.4, 1.0, 0.1);
        level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.4F, 1.6F);
    }
}
