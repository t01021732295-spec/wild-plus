package com.snolred.wildplus.item;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** 모드 활: 활 종류 표시(bow 태그)만 붙이고 능력은 Bows 에서 처리 */
public class WildBowItem extends BowItem {
    public final String kind;
    public final float drawSpeed, damage;

    public WildBowItem(Properties properties, String kind, float drawSpeed, float damage) {
        super(properties);
        this.kind = kind;
        this.drawSpeed = drawSpeed;
        this.damage = damage;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        AbstractArrow a = super.customArrow(arrow, projectile, weapon);
        a.getPersistentData().putString(com.snolred.wildplus.event.Bows.BOW_TAG, kind);
        a.setBaseDamage(2.0 * damage);
        return a;
    }
}
