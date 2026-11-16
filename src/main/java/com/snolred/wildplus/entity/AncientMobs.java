package com.snolred.wildplus.entity;

import com.snolred.wildplus.event.Noise;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 5.3 고대 도시 몬스터 4종 (도안 tools/ancient_designs_v53.py · 모델 tools/gen_models_v53.py)
 * · 스컬크 추적자: 눈이 없어 소리로만 쫓는다 (소음 25 이상 · 달리는 사람 · 3칸 안). 덮치기(예고 후 도약 → 착지 음파)
 * · 메아리 박쥐 떼: 날아다니며 부딪히면 피해 + 소음 크게 올림
 * · 침묵의 사제: 침묵의 종(장판 + 어둠) · 의식(3초 동안 막지 않으면 주변 소음 +35, 맞으면 끊김). 잡으면 근처 소음 초기화
 * · 스컬크 골렘(정예): 땅 내리치기(음파 고리) · 촉수 채찍(앞으로 3연속 장판) · 맞으면 어둠
 */
public final class AncientMobs {
    private AncientMobs() {}

    private static AttributeSupplier.Builder base(double hp, double dmg, double armor, double speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.ATTACK_DAMAGE, dmg).add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, speed);
    }

    /** 스컬크 추적자 */
    public static class Stalker extends FxBoss {
        private int pounce;
        private Vec3 dir = Vec3.ZERO;

        public Stalker(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(44.0, 8.0, 4.0, 0.33);
        }

        @Override
        protected void registerGoals() { // 눈으로 찾지 않음 — skills 에서 소리로 고름
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        }

        private boolean hears(ServerPlayer p) {
            double d = p.distanceToSqr(this);
            if (d < 9) return true;
            if (p.isShiftKeyDown() && Noise.get(p) < 55) return false;
            return d < 24 * 24 && (Noise.get(p) >= 25 || p.isSprinting());
        }

        @Override
        protected void skills(ServerLevel lv) {
            if (tickCount % 10 == 0) {
                ServerPlayer best = null;
                for (ServerPlayer p : near(lv, 24)) if (hears(p) && (best == null || p.distanceToSqr(this) < best.distanceToSqr(this))) best = p;
                if (best != getTarget()) {
                    setTarget(best);
                    if (best != null) { // 들었다! 감지 촉수가 빛남
                        lv.sendParticles(ParticleTypes.SCULK_CHARGE_POP, getX(), getY() + 2.6, getZ(), 10, 0.4, 0.2, 0.4, 0.02);
                        lv.playSound(null, blockPosition(), SoundEvents.SCULK_SENSOR_CLICKING, SoundSource.HOSTILE, 1.2F, 0.7F);
                    }
                }
            }
            LivingEntity t = getTarget();
            if (pounce > 0) {
                pounce--;
                if (pounce == 10) { // 도약
                    setDeltaMovement(dir.x * 1.2, 0.55, dir.z * 1.2);
                    lv.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 0.8F, 1.4F);
                } else if (pounce > 10 && pounce % 3 == 0) {
                    lv.sendParticles(ParticleTypes.SCULK_CHARGE_POP, getX(), getY() + 0.2, getZ(), 6, 0.4, 0, 0.4, 0);
                } else if (pounce < 10 && onGround()) { // 착지 음파
                    pounce = 0;
                    warn(lv, getX(), getY(), getZ(), 2.4, 7F, 1, SCULK);
                }
                return;
            }
            if (t != null && tickCount % 80 == 30 && distanceToSqr(t) > 16 && distanceToSqr(t) < 120 && onGround()) {
                dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                pounce = 22; // 12틱 웅크림 예고
                warn(lv, t.getX(), t.getY(), t.getZ(), 2.4, 0F, 22, SCULK);
            }
        }
    }

    /** 메아리 박쥐 떼 */
    public static class Bats extends FxBoss {
        private int hitCd;

        public Bats(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(18.0, 3.0, 0.0, 0.3).add(Attributes.FLYING_SPEED, 0.6);
        }

        @Override
        protected void registerGoals() {
            targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, false));
        }

        @Override
        public boolean causeFallDamage(double d, float m, DamageSource s) {
            return false;
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            double sway = Math.sin(tickCount * 0.25) * 0.08;
            if (t != null && t.isAlive()) {
                Vec3 want = t.position().add(Math.sin(tickCount * 0.11) * 1.5, 1.4 + Math.sin(tickCount * 0.2) * 0.6, Math.cos(tickCount * 0.11) * 1.5);
                Vec3 v = want.subtract(position()).normalize().scale(0.32);
                setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.5)).add(0, sway, 0));
                getLookControl().setLookAt(t);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.8).add(Math.cos(tickCount * 0.07) * 0.03, sway * 0.5, Math.sin(tickCount * 0.07) * 0.03));
            }
            if (tickCount % 6 == 0) lv.sendParticles(ParticleTypes.SCULK_CHARGE_POP, getX(), getY() + 0.5, getZ(), 2, 0.5, 0.4, 0.5, 0);
            if (hitCd > 0) hitCd--;
            if (hitCd == 0) {
                for (ServerPlayer p : near(lv, 1.6)) {
                    hit(lv, p, 3F);
                    Noise.add(p, 12F);
                    hitCd = 20;
                    lv.playSound(null, blockPosition(), SoundEvents.BAT_HURT, SoundSource.HOSTILE, 1F, 0.6F);
                }
            }
        }
    }

    /** 침묵의 사제 */
    public static class Priest extends FxBoss {
        private int ritual;

        public Priest(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(64.0, 4.0, 2.0, 0.22);
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            if (ritual > 0 && source.getEntity() instanceof ServerPlayer) { // 의식이 끊김
                ritual = 0;
                level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.5, getZ(), 20, 0.5, 0.6, 0.5, 0.08);
                level.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_BREAK, SoundSource.HOSTILE, 1F, 1.2F);
            }
            return super.hurtServer(level, source, amount);
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (ritual > 0) {
                ritual--;
                getNavigation().stop();
                if (ritual % 3 == 0) column(lv, position(), 3.5, ParticleTypes.SCULK_SOUL, ParticleTypes.SCULK_CHARGE_POP);
                if (ritual % 10 == 0) lv.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.2F, 0.8F);
                if (ritual == 0) {
                    for (ServerPlayer p : near(lv, 20)) {
                        Noise.add(p, 35F);
                        p.sendOverlayMessage(Component.literal("♪ 침묵의 사제의 의식이 끝났어요 — 소음 +35").withStyle(ChatFormatting.RED));
                    }
                }
                return;
            }
            if (t != null && tickCount % 80 == 25 && distanceToSqr(t) < 400) { // 침묵의 종
                warn(lv, t.getX(), t.getY(), t.getZ(), 3.0, 7F, 26, SCULK);
                lv.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.4F, 0.5F);
                if (t instanceof ServerPlayer sp) sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            if (t != null && tickCount % 220 == 120) {
                ritual = 60;
                for (ServerPlayer p : near(lv, 20))
                    p.sendOverlayMessage(Component.literal("♪ 침묵의 사제가 의식을 시작했어요! 때려서 끊으세요").withStyle(ChatFormatting.GOLD));
            }
        }

        @Override
        public void die(DamageSource source) {
            if (level() instanceof ServerLevel lv) {
                lv.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1, getZ(), 40, 1.5, 1, 1.5, 0.05);
                for (ServerPlayer p : near(lv, 16)) {
                    Noise.reset(p, 200);
                    p.sendOverlayMessage(Component.literal("♪ 사제가 쓰러지자 주변이 고요해졌어요").withStyle(ChatFormatting.AQUA));
                }
            }
            super.die(source);
        }
    }

    /** 스컬크 골렘 (정예) */
    public static class SculkGolem extends FxBoss {
        public SculkGolem(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(180.0, 13.0, 12.0, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR_TOUGHNESS, 4.0);
        }

        @Override
        protected void afterFire(ServerLevel lv, Warn w) { // 음파에 맞으면 어둠
            for (ServerPlayer p : near(lv, 12)) if (p.distanceToSqr(w.at) < (w.r + 2) * (w.r + 2)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (tickCount % 120 == 40 && distanceToSqr(t) < 100) { // 땅 내리치기
                for (int i = 0; i < 2; i++) warn(lv, getX(), getY(), getZ(), 3.5 + i * 3, 12F, 30 + i * 8, SCULK_RING);
                lv.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.2F, 0.8F);
            }
            if (tickCount % 160 == 100 && distanceToSqr(t) < 256) { // 촉수 채찍
                Vec3 d = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 3; i++) {
                    Vec3 at = position().add(d.scale(i * 2.6));
                    warn(lv, at.x, getY(), at.z, 2.0, 10F, 20 + i * 4, SCULK);
                }
                lv.playSound(null, blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 1.5F, 0.6F);
            }
        }
    }
}
