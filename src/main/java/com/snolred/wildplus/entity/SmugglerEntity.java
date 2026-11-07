package com.snolred.wildplus.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 3.8 우민 밀수꾼: 흉조(나쁜 징조 · 습격 징조)가 걸린 플레이어 근처에만 나타나는 떠돌이 상인.
 * 바닐라 변명자 몸 · 보라 두건 망토 · 배낭. 흉조 병을 받고 귀한 물건을 판다. 공격받지 않고, 흉조가 사라지거나 10분이 지나면 떠난다.
 */
public class SmugglerEntity extends Vindicator {
    public static final String BORN = "wildplus_smuggler_born", SOLD = "wildplus_smuggler_sold";

    public SmugglerEntity(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
        this.setCanJoinRaid(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.45));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected boolean considersEntityAsAlly(Entity e) {
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    public boolean canJoinPatrol() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 몬스터지만 곁에 있어도 잠잘 수 있게 */
    @Override
    public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) com.snolred.wildplus.omen.Smugglers.openShop(sp, this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setTarget(null);
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (this.tickCount % 40 == 0) sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2.1, getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        if (this.tickCount % 100 == 0 && com.snolred.wildplus.omen.Smugglers.shouldLeave(sl, this)) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            this.discard();
        }
    }
}
