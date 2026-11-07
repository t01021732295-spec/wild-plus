package com.snolred.wildplus.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 3.8 우민 용병: 감옥에서 전향한 우민. 바닐라 변명자 몸에 마을 파란 옷 · 철모.
 * 주인을 따라다니며 몬스터와 우민을 공격한다. 주인이 웅크리고 누르면 "이 자리 지키기" ↔ "따라오기".
 * 철괴를 주면 체력 회복. 플레이어 · 주민 · 골렘은 공격하지 않고, 플레이어에게 피해를 받지 않는다.
 */
public class MercenaryEntity extends Vindicator {
    public static final String OWNER = "wildplus_merc_owner", GUARD = "wildplus_merc_guard", HOME = "wildplus_merc_home";

    public MercenaryEntity(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
        this.setCanJoinRaid(false);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static MercenaryEntity spawn(ServerLevel level, BlockPos at, ServerPlayer owner) {
        MercenaryEntity m = com.snolred.wildplus.registry.ModEntities.MERCENARY.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (m == null) return null;
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, owner.getYRot() + 180F, 0F);
        m.getPersistentData().putString(OWNER, owner.getUUID().toString());
        m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        m.setDropChance(EquipmentSlot.MAINHAND, 0F);
        m.setCustomName(Component.literal("⚔ " + owner.getName().getString() + "의 우민 용병").withStyle(ChatFormatting.AQUA));
        m.setCustomNameVisible(true);
        level.addFreshEntity(m);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.6, 0.4, 0);
        return m;
    }

    public UUID ownerId() {
        String s = this.getPersistentData().getStringOr(OWNER, "");
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Player owner() {
        UUID id = ownerId();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(g -> true);
        this.goalSelector.addGoal(3, new FollowOwner(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class, MercenaryEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                (e, lvl) -> e instanceof Enemy && e.getType() != EntityTypes.CREEPER && e.getType() != EntityTypes.ENDERMAN
                        && !(e instanceof MercenaryEntity) && !(e instanceof SmugglerEntity)));
    }

    @Override
    protected boolean considersEntityAsAlly(Entity e) {
        return e instanceof Player || e instanceof AbstractVillager || e instanceof IronGolem || e instanceof MercenaryEntity || e instanceof SmugglerEntity;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player || target instanceof AbstractVillager || target instanceof IronGolem || target instanceof MercenaryEntity) return false;
        return super.canAttack(target);
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
        if (source.getEntity() instanceof Player || source.getEntity() instanceof MercenaryEntity) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.IRON_INGOT) && this.getHealth() < this.getMaxHealth()) {
            this.heal(15F);
            if (!player.getAbilities().instabuild) held.shrink(1);
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 2, getZ(), 4, 0.3, 0.2, 0.3, 0);
            return InteractionResult.SUCCESS;
        }
        if (!player.getUUID().equals(ownerId())) {
            sp.sendOverlayMessage(Component.literal("다른 사람의 용병이에요").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            boolean guard = !this.getPersistentData().getBooleanOr(GUARD, false);
            this.getPersistentData().putBoolean(GUARD, guard);
            this.getPersistentData().putLong(HOME, this.blockPosition().asLong());
            this.getNavigation().stop();
            sp.sendOverlayMessage(Component.literal(guard ? "🛡 이 자리를 지킬게요 (주변 12칸)" : "⚔ 따라갈게요").withStyle(ChatFormatting.AQUA));
        } else {
            sp.sendOverlayMessage(Component.literal(String.format("⚔ 용병 체력 %d/%d · %s · 웅크리고 누르면 지키기/따라오기 · 철괴로 회복",
                    Math.round(getHealth()), Math.round(getMaxHealth()), this.getPersistentData().getBooleanOr(GUARD, false) ? "지키는 중" : "따라오는 중"))
                    .withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        if (this.tickCount % 100 == 0 && this.getTarget() == null && this.getHealth() < this.getMaxHealth()) this.heal(1F);
        if (this.getTarget() instanceof Player || this.getTarget() instanceof AbstractVillager) this.setTarget(null);
    }

    @Override
    public void die(DamageSource source) {
        Player o = owner();
        if (o instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("💀 우민 용병이 쓰러졌어요").withStyle(ChatFormatting.RED));
        super.die(source);
    }

    /** 주인 따라가기 (멀면 순간이동) · 지키기 모드면 집 근처로 돌아가기 */
    static final class FollowOwner extends Goal {
        private final MercenaryEntity m;
        private int recalc;

        FollowOwner(MercenaryEntity m) {
            this.m = m;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private double distSq() {
            if (m.getPersistentData().getBooleanOr(GUARD, false)) {
                BlockPos h = BlockPos.of(m.getPersistentData().getLongOr(HOME, m.blockPosition().asLong()));
                return m.distanceToSqr(h.getX() + 0.5, h.getY(), h.getZ() + 0.5);
            }
            Player o = m.owner();
            return o == null || o.level() != m.level() || o.isSpectator() ? -1 : m.distanceToSqr(o);
        }

        @Override
        public boolean canUse() {
            double d = distSq();
            boolean guard = m.getPersistentData().getBooleanOr(GUARD, false);
            return d > (guard ? 144 : 64) && (m.getTarget() == null || d > 576);
        }

        @Override
        public boolean canContinueToUse() {
            double d = distSq();
            return d > 9 && (m.getTarget() == null || d > 576);
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void tick() {
            if (--recalc > 0) return;
            recalc = 10;
            boolean guard = m.getPersistentData().getBooleanOr(GUARD, false);
            if (guard) {
                BlockPos h = BlockPos.of(m.getPersistentData().getLongOr(HOME, m.blockPosition().asLong()));
                m.getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 1.0);
                return;
            }
            Player o = m.owner();
            if (o == null) return;
            if (m.distanceToSqr(o) > 32 * 32 && o.onGround()) {
                m.setTarget(null);
                m.teleportTo(o.getX() - Math.sin(Math.toRadians(o.getYRot())) * -2, o.getY(), o.getZ() + Math.cos(Math.toRadians(o.getYRot())) * -2);
                m.getNavigation().stop();
                return;
            }
            m.getNavigation().moveTo(o, 1.15);
        }
    }
}
