package com.snolred.wildplus.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** 음악 재생 중 턴테이블 · 스피커 움직임을 그리는 보이지 않는 받침 (저장 안 함, 재생하는 동안 Music 이 관리) */
public class MusicVisual extends Mob {
    public MusicVisual(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setSilent(true);
        setNoAi(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected void pushEntities() {}
}
