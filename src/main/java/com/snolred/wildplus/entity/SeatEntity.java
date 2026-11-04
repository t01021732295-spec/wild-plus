package com.snolred.wildplus.entity;

import com.snolred.wildplus.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** 보이지 않는 의자 자리: 가구(소파 · 안락의자 · 의자 · 욕조)에 앉으면 이걸 탄다. 내리면 사라짐. */
public class SeatEntity extends Entity {
    private BlockPos block = BlockPos.ZERO;
    private boolean bath;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /** 이 자리에 이미 누가 앉아 있나 */
    public static boolean taken(Level level, Vec3 at) {
        return !level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.3)).isEmpty();
    }

    /** 앉기: at = 엉덩이 높이 (블록 좌표 + 픽셀/16) */
    public static boolean sit(Player p, BlockPos block, Vec3 at, float yaw, boolean bath) {
        if (!(p.level() instanceof ServerLevel sl) || p.isPassenger() || taken(sl, at)) return false;
        SeatEntity s = ModEntities.SEAT.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (s == null) return false;
        // 타는 사람은 자리(0.01) 위에서 0.6 내려가 앉고, 앉은 자세의 엉덩이는 발 + 0.75 → 엉덩이가 at.y 에 오도록
        s.snapTo(at.x, at.y - 0.75 + 0.6 - 0.01, at.z, yaw, 0);
        s.block = block.immutable();
        s.bath = bath;
        sl.addFreshEntity(s);
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        return p.startRiding(s);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (this.getPassengers().isEmpty() || sl.getBlockState(block).isAir()) {
            this.ejectPassengers();
            this.discard();
            return;
        }
        if (bath && this.tickCount % 40 == 0) {
            for (Entity e : this.getPassengers()) {
                if (e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
            }
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, getX(), getY() + 0.6, getZ(), 4, 0.4, 0.1, 0.4, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {}
}
