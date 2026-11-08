package com.snolred.wildplus.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 3.9 우민 감옥 안에 보이는 포로 (그림만): 움직이지 않고 다치지 않으며 저장하지 않는다.
 * 감옥 자료(Prisoners)에 맞춰 서버가 세우고 지운다. 누르면 감옥 창이 열린다.
 */
public class PrisonerEntity extends PathfinderMob {
    /** 0 약탈자 · 1 변명자 · 2 소환사 · 3 환술사 */
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PrisonerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ELITE = SynchedEntityData.defineId(PrisonerEntity.class, EntityDataSerializers.BOOLEAN);
    public String cell = "";
    public int slot;
    public net.minecraft.core.BlockPos prison;

    public PrisonerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, 0);
        b.define(ELITE, false);
    }

    public static int kindOf(String type) {
        return switch (type) {
            case "minecraft:vindicator" -> 1;
            case "minecraft:evoker" -> 2;
            case "minecraft:illusioner" -> 3;
            default -> 0;
        };
    }

    public void setKind(int k, boolean elite) {
        this.entityData.set(KIND, k);
        this.entityData.set(ELITE, elite);
    }

    public int kind() {
        return this.entityData.get(KIND);
    }

    public boolean elite() {
        return this.entityData.get(ELITE);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && prison != null) {
            com.snolred.wildplus.omen.Prisoners.open(sp, prison);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 5 && !com.snolred.wildplus.omen.Prisoners.stillHeld(this)) {
            this.discard();
            return;
        }
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot() + (float) Math.sin(this.tickCount * 0.03 + this.getId()) * 25F;
    }
}
