package com.snolred.wildplus.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 4.3 엔드 몬스터: 바닐라 몸을 쓰고 능력만 더한다 */
public final class EndMobs {
    private EndMobs() {}

    public static class Stalker extends Enderman {
        private int blink = 100;

        public Stalker(EntityType<? extends Enderman> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Enderman.createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.FOLLOW_RANGE, 40.0);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
            super.customServerAiStep(level);
            LivingEntity t = getTarget();
            if (t == null || --blink > 0) return;
            blink = 120 + getRandom().nextInt(60);
            Vec3 back = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(2.0));
            if (!level.noCollision(this, getBoundingBox().move(back.subtract(position())))) return;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.4, getZ(), 40, 0.3, 0.8, 0.3, 0.15);
            teleportTo(back.x, back.y, back.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, back.x, back.y + 1.4, back.z, 40, 0.3, 0.8, 0.3, 0.15);
        }
    }

    public static class Mite extends Endermite {
        public Mite(EntityType<? extends Endermite> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Endermite.createAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.ATTACK_DAMAGE, 4.0);
        }
    }

    public static class Wraith extends Phantom {
        public Wraith(EntityType<? extends Phantom> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 64.0);
        }
    }

    /** 추적자 일격: 실명 */
    public static void onStalkerHit(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
    }

    public static boolean isAny(Entity e) {
        return e instanceof Stalker || e instanceof Mite || e instanceof Wraith;
    }
}
