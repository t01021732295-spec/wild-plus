package com.snolred.wildplus.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 5.0 네더 몬스터 4종 (도안 tools/nether_designs_v50.py · 모델 tools/gen_models_v50.py).
 * 잿불 사냥개(무리 돌진 · 죽으면 불씨 폭발) · 용암 거북(등껍질 방어, 용암탄을 쏜 직후가 약점) ·
 * 재의 주술사(화염 장판 · 주변 몬스터 강화) · 흑요석 골렘(충격파 · 체력 절반 아래로 수정이 깨져 방어 감소)
 */
public final class NetherMobs {
    private NetherMobs() {}

    private static AttributeSupplier.Builder base(double hp, double dmg, double armor, double speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.ATTACK_DAMAGE, dmg).add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, speed);
    }

    /** 잿불 사냥개 */
    public static class Hound extends FxBoss {
        private int dash;
        private Vec3 dir = Vec3.ZERO;

        public Hound(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(36.0, 7.0, 4.0, 0.36);
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (dash > 0) {
                dash--;
                if (dash <= 10) {
                    setDeltaMovement(dir.x * 1.1, getDeltaMovement().y, dir.z * 1.1);
                    lv.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 4, 0.3, 0.2, 0.3, 0.01);
                    for (ServerPlayer p : near(lv, 1.8)) {
                        hit(lv, p, 6F);
                        p.igniteForSeconds(3);
                    }
                }
                return;
            }
            if (t != null && tickCount % 90 == 20 && distanceToSqr(t) > 9 && distanceToSqr(t) < 196) {
                dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                dash = 22; // 12틱 예고(웅크림) 뒤 돌진
                lv.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 0.8F, 1.6F);
            }
        }

        @Override
        public void die(DamageSource source) {
            if (level() instanceof ServerLevel lv) {
                lv.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 14, 0.6, 0.3, 0.6, 0);
                lv.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 30, 0.9, 0.4, 0.9, 0.04);
                for (ServerPlayer p : near(lv, 2.5)) {
                    hit(lv, p, 5F);
                    p.igniteForSeconds(4);
                }
            }
            super.die(source);
        }
    }

    /** 용암 거북 */
    public static class Turtle extends FxBoss {
        private int open; // 용암탄을 쏜 뒤 등껍질이 열려 있는 시간

        public Turtle(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(90.0, 8.0, 10.0, 0.16).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
        }

        @Override
        protected void skills(ServerLevel lv) {
            if (open > 0) {
                open--;
                if (open % 5 == 0) lv.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.6, getZ(), 6, 0.4, 0.2, 0.4, 0.02);
            }
            LivingEntity t = getTarget();
            if (t != null && tickCount % 110 == 40 && distanceToSqr(t) < 400) {
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = i == 0 ? 0 : 2.5 + random.nextDouble() * 2;
                    warn(lv, t.getX() + Math.cos(a) * r, t.getY(), t.getZ() + Math.sin(a) * r, 2.2, 9F, 30, LAVA);
                }
                open = 60;
                lv.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.5F);
            }
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            if (open <= 0 && amount < 1000F) {
                amount *= 0.3F; // 등껍질
                if (tickCount % 4 == 0) level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 6, 0.5, 0.3, 0.5, 0.1);
            }
            return super.hurtServer(level, source, amount);
        }
    }

    /** 재의 주술사 */
    public static class Shaman extends FxBoss {
        public Shaman(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(60.0, 4.0, 2.0, 0.22);
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (t != null && tickCount % 70 == 30 && distanceToSqr(t) < 484) {
                warn(lv, t.getX(), t.getY(), t.getZ(), 2.6, 8F, 28, LAVA);
                Vec3 a = position().add(0, 2.4, 0), b = t.position().add(0, 0.3, 0);
                for (int i = 0; i <= 14; i++) lv.sendParticles(ParticleTypes.FLAME, a.x + (b.x - a.x) * i / 14, a.y + (b.y - a.y) * i / 14, a.z + (b.z - a.z) * i / 14, 1, 0, 0, 0, 0);
            }
            if (tickCount % 100 == 0) { // 주변 몬스터 강화
                for (Mob m : lv.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(10.0), m -> m != this && m instanceof Monster && !(m instanceof Shaman))) {
                    m.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 0));
                    m.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0));
                    lv.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 5, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }
    }

    /** 흑요석 골렘 */
    public static class Golem extends FxBoss {
        private boolean cracked;

        public Golem(EntityType<? extends Monster> type, Level level) {
            super(type, level, false);
        }

        public static AttributeSupplier.Builder attributes() {
            return base(220.0, 14.0, 18.0, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR_TOUGHNESS, 6.0);
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (t != null && tickCount % 120 == 50 && distanceToSqr(t) < 100) {
                warn(lv, getX(), getY(), getZ(), 5.0, 12F, 34, RING); // 땅 내리치기
                lv.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5F, 0.6F);
            }
            if (!cracked && getHealth() < getMaxHealth() * 0.5F) { // 어깨 수정이 깨짐
                cracked = true;
                var armor = getAttribute(Attributes.ARMOR);
                if (armor != null) armor.setBaseValue(4.0);
                lv.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.6, getZ(), 30, 1.0, 0.4, 1.0, 0);
                lv.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
            }
        }
    }
}
