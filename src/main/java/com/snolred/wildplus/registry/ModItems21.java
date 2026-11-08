package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.LuckyCharmItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.9 행운의 부적 2~10단계 (1단계는 ModItems.LUCKY_CHARM) */
public final class ModItems21 {
    public static final List<DeferredItem<LuckyCharmItem>> CHARMS = new ArrayList<>();

    static {
        for (int t = 2; t <= 10; t++) {
            final int tier = t;
            CHARMS.add(ITEMS.registerItem("lucky_charm_" + t, p -> new LuckyCharmItem(p, tier),
                    p -> p.stacksTo(1).fireResistant().rarity(tier >= 8 ? Rarity.EPIC : tier >= 4 ? Rarity.RARE : Rarity.UNCOMMON)));
        }
    }

    /** 단계별 부적 아이템 (1 ~ 10) */
    public static net.minecraft.world.item.Item charm(int tier) {
        return tier <= 1 ? ModItems.LUCKY_CHARM.get() : CHARMS.get(Math.min(10, tier) - 2).get();
    }

    public static void init() {}

    private ModItems21() {}
}
