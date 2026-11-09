package com.snolred.wildplus.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 4.7 레이드 보스 화산 거신: 용암 웅덩이 · 바위 투척 · 대분화 · 냉각(8초간 약점, 받는 피해 2배) */
public class VolcanoEntity extends FxBoss {
    private static final EntityDataAccessor<Boolean> COOLED = SynchedEntityData.defineId(VolcanoEntity.class, EntityDataSerializers.BOOLEAN);
    private int cool;

    public VolcanoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 640.0).add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.MOVEMENT_SPEED, 0.26);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(COOLED, false);
    }

    public boolean cooled() {
        return entityData.get(COOLED);
    }

    @Override
    public boolean hurtServer(ServerLevel lv, DamageSource s, float amount) {
        return super.hurtServer(lv, s, cooled() ? amount * 2F : amount);
    }

    @Override
    protected void skills(ServerLevel lv) {
        List<ServerPlayer> all = near(lv, 48);
        if (cool > 0 && --cool == 0) entityData.set(COOLED, false);
        if (cooled()) {
            setDeltaMovement(Vec3.ZERO);
            if (tickCount % 10 == 0) lv.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 12, 1.2, 1.5, 1.2, 0.02);
            return;
        }
        if (all.isEmpty()) return;
        var r = getRandom();
        int t = tickCount;
        float base = 10F;
        if (t % 120 == 0) for (ServerPlayer p : all) warn(lv, p.getX(), p.getY(), p.getZ(), 3.5, base * 0.8F, 40, LAVA);
        if (t % 200 == 60) {
            ServerPlayer p = all.get(r.nextInt(all.size()));
            Vec3 from = position().add(0, 4, 0), to = p.position();
            for (int i = 0; i <= 14; i++) {
                double k = i / 14.0;
                lv.sendParticles(ParticleTypes.LARGE_SMOKE, from.x + (to.x - from.x) * k, from.y + (to.y - from.y) * k + Math.sin(k * Math.PI) * 10, from.z + (to.z - from.z) * k, 3, 0.2, 0.2, 0.2, 0);
            }
            warn(lv, to.x, to.y, to.z, 3.0, base * 1.4F, 30, ROCK);
            lv.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        if (t % 360 == 180 && getHealth() / getMaxHealth() < 0.66F) {
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5, d = 7 + (i % 2) * 5;
                warn(lv, getX() + Math.cos(a) * d, getY(), getZ() + Math.sin(a) * d, 2.6, base * 1.6F, 45, ERUPT);
            }
            lv.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
        }
        if (t % 560 == 500) {
            entityData.set(COOLED, true);
            cool = 160;
            lv.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.6F);
        }
    }

    @Override
    protected void afterFire(ServerLevel lv, Warn w) {
        // 용암 웅덩이: 3번 더 지속 피해
        if (w.kind == LAVA && w.dmg > 2.0F) for (int i = 1; i <= 3; i++) warns.add(new Warn(w.at, w.r, 2.0F, i * 20, LAVA));
    }
}
