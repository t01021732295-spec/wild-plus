package com.snolred.wildplus.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 도적: 밤에 들판·숲에 나타나고, 도적 야영지를 지킨다.
 * 플레이어를 때리면 동전을 훔쳐 가며, 처치하면 훔친 동전을 되찾는다. (BanditEvents)
 */
public class BanditEntity extends Vindicator {
    public static final String STOLEN_KEY = "wildplus_stolen";

    public BanditEntity(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.IRON_SWORD : Items.STONE_SWORD));
    }

    /** 도적 아지트 두목 표시 (구조물 NBT 의 NeoForgeData) */
    public static final String BOSS_TAG = "wildplus_bandit_boss";
    private boolean prepared;

    /**
     * 구조물로 생긴 도적은 무기 없이 나타나므로 처음 움직일 때 무장시킨다.
     * 두목은 체력 160 · 공격 9 · 방어 10, 네더라이트 도끼, 정예 능력(가시·흡혈).
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (prepared || this.level().isClientSide()) return;
        prepared = true;
        if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(this.random.nextBoolean() ? Items.IRON_SWORD : Items.STONE_SWORD));
        }
        var data = this.getPersistentData();
        if (data.getBooleanOr(BOSS_TAG, false) && !data.getBooleanOr("wildplus_boss_ready", false)) {
            data.putBoolean("wildplus_boss_ready", true);
            set(Attributes.MAX_HEALTH, 160);
            set(Attributes.ATTACK_DAMAGE, 9);
            set(Attributes.ARMOR, 10);
            set(Attributes.KNOCKBACK_RESISTANCE, 0.8);
            set(Attributes.SCALE, 1.25);
            this.setHealth(this.getMaxHealth());
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            this.setDropChance(EquipmentSlot.HEAD, 0.0F);
            data.putBoolean(com.snolred.wildplus.event.EliteMobs.ELITE_TAG, true);
            data.putString(com.snolred.wildplus.event.EliteMobs.AFFIX_TAG, "THORNS,VAMPIRIC,");
            this.setCustomName(net.minecraft.network.chat.Component.translatable("entity.wildplus.bandit_boss")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
            this.setCustomNameVisible(true);
            this.setPersistenceRequired();
        }
    }

    private void set(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        var inst = this.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.getPersistentData().getBooleanOr(BOSS_TAG, false)) {
            this.spawnAtLocation(level, new ItemStack(com.snolred.wildplus.registry.ModItems3.GOLD_COIN.get(), 5 + this.random.nextInt(6)));
            this.spawnAtLocation(level, new ItemStack(Items.DIAMOND, 2 + this.random.nextInt(3)));
            if (this.random.nextFloat() < 0.5F) this.spawnAtLocation(level, new ItemStack(com.snolred.wildplus.registry.ModItems.LUCKY_CHARM.get()));
        }
    }
}
