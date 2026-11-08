package com.snolred.wildplus.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 행운의 부적 (3.9: 1~10단계). 같은 단계를 제작대에서 합치면 다음 단계 1개 (II·III 9개 · IV·V 3개 · VI~X 2개).
 * 효과는 가방 속 가장 높은 단계 하나만 적용 (event.LuckyCharms). 출하 상자에 팔 수 있다.
 */
public class LuckyCharmItem extends Item {
    public final int tier;

    public LuckyCharmItem(Properties properties) {
        this(properties, 1);
    }

    public LuckyCharmItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public static int tierOf(ItemStack s) {
        return s.getItem() instanceof LuckyCharmItem c ? c.tier : 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
