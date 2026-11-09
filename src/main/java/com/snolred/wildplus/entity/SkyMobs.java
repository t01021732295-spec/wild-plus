package com.snolred.wildplus.entity;

import java.util.Comparator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 4.7 하늘의 탑 잡몹 */
public final class SkyMobs {
    private SkyMobs() {}

    /** 구름 파수병: 원 표시 후 바람을 끌며 방패 돌진 */
    public static class Sentinel extends FxBoss {
        private int charge;
        private Vec3 dir = Vec3.ZERO;

        public Sentinel(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 8.0)
                    .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3);
        }

        @Override
        protected void skills(ServerLevel lv) {
            LivingEntity t = getTarget();
            if (charge > 0) {
                charge--;
                if (charge <= 14) {
                    setDeltaMovement(dir.x * 1.2, getDeltaMovement().y, dir.z * 1.2);
                    lv.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 6, 0.3, 0.5, 0.3, 0.02);
                    for (ServerPlayer p : near(lv, 2.4)) hit(lv, p, 8F);
                }
                return;
            }
            if (t != null && tickCount % 120 == 30 && distanceToSqr(t) > 16 && distanceToSqr(t) < 256) {
                dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                ring(lv, t.position(), 2.0, ParticleTypes.CLOUD, 0.2);
                charge = 34;
            }
        }
    }

    /** 천둥매: 원 표시 후 벼락 */
    public static class Hawk extends Phantom {
        private Vec3 mark;
        private int left;

        public Hawk(EntityType<? extends Phantom> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.FOLLOW_RANGE, 64.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!(level() instanceof ServerLevel lv)) return;
            if (mark != null) {
                if (left % 4 == 0) FxBoss.ring(lv, mark, 3.0, ParticleTypes.ELECTRIC_SPARK, 0.2);
                if (--left <= 0) {
                    FxBoss.bolt(lv, mark);
                    for (ServerPlayer p : lv.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(mark, mark).inflate(3.0, 5.0, 3.0))) p.hurtServer(lv, lv.damageSources().lightningBolt(), 8F);
                    mark = null;
                }
            } else if (getTarget() != null && tickCount % 160 == 40) {
                mark = getTarget().position();
                left = 24;
            }
        }
    }

    /** 구름 사제: 다친 아군을 초록 광선으로 치유, 멀리서 마법 탄 */
    public static class Priest extends Monster {
        public Priest(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.MOVEMENT_SPEED, 0.2);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.8, true));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!(level() instanceof ServerLevel lv) || isDeadOrDying()) return;
            if (tickCount % 60 == 0) {
                lv.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16.0), m -> m != this && m instanceof Monster && m.getHealth() < m.getMaxHealth())
                        .stream().min(Comparator.comparingDouble(m -> m.getHealth() / m.getMaxHealth())).ifPresent(m -> {
                            m.heal(m.getMaxHealth() * 0.2F);
                            Vec3 a = position().add(0, 2.5, 0), b = m.position().add(0, m.getBbHeight() * 0.6, 0);
                            for (int i = 0; i <= 16; i++) lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.x + (b.x - a.x) * i / 16, a.y + (b.y - a.y) * i / 16, a.z + (b.z - a.z) * i / 16, 1, 0, 0, 0, 0);
                            FxBoss.ring(lv, m.position(), 1.2, ParticleTypes.HAPPY_VILLAGER, 0.2);
                        });
            }
            if (tickCount % 80 == 20 && getTarget() instanceof ServerPlayer p && distanceToSqr(p) < 400) {
                Vec3 a = position().add(0, 2.5, 0), b = p.position().add(0, 1.2, 0);
                for (int i = 0; i <= 18; i++) lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, a.x + (b.x - a.x) * i / 18, a.y + (b.y - a.y) * i / 18, a.z + (b.z - a.z) * i / 18, 1, 0, 0, 0, 0);
                p.hurtServer(lv, lv.damageSources().indirectMagic(this, this), 5F);
            }
        }
    }
}
