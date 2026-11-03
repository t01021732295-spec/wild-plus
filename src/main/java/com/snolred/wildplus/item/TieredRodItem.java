package com.snolred.wildplus.item;

import net.minecraft.world.item.FishingRodItem;

/**
 * 단계별 낚싯대 (1 미스릴 · 2 루비 · 3 사파이어 · 4 용왕). 효과는 FishingPlus · FishingLootModifier 에서.
 * 단계가 오를수록 입질이 빨라지고, 희귀 어종 · 보물상자 · 두 마리 · 높은 등급 확률이 오른다.
 */
public class TieredRodItem extends FishingRodItem {
    public final int tier;

    public TieredRodItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }
}
