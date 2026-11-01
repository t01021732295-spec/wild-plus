package com.snolred.wildplus.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 특수 화살: 폭발 · 유도 · 번개 · 서리. 날아가는 화살에 종류 표시를 붙이고 효과는 Bows 에서 처리 */
public class WildArrowItem extends ArrowItem {
    public final String kind;

    public WildArrowItem(Properties properties, String kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        AbstractArrow a = super.createArrow(level, ammo, shooter, weapon);
        a.getPersistentData().putString(com.snolred.wildplus.event.Bows.ARROW_TAG, kind);
        return a;
    }
}
