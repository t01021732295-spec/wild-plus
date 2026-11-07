package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.item.ForbiddenPotionItem;
import com.snolred.wildplus.item.LassoItem;
import com.snolred.wildplus.item.OmenFlaskItem;
import com.snolred.wildplus.item.ThemedOmenItem;
import com.snolred.wildplus.omen.Omen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.neoforged.neoforge.registries.DeferredItem;

/** 3.8 흉조 아이템: 흉조 투척병 · 테마 흉조 5종 · 우민 올가미 · 금지된 물약 · 흉조 제단 · 우민 감옥 */
public final class ModItems20 {
    public static final DeferredItem<OmenFlaskItem> OMEN_FLASK = ITEMS.registerItem("omen_flask", OmenFlaskItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ThemedOmenItem> BURNING_OMEN = themed("burning_omen", Omen.Theme.BURNING);
    public static final DeferredItem<ThemedOmenItem> FROZEN_OMEN = themed("frozen_omen", Omen.Theme.FROZEN);
    public static final DeferredItem<ThemedOmenItem> DEEP_OMEN = themed("deep_omen", Omen.Theme.DEEP);
    public static final DeferredItem<ThemedOmenItem> WRAITH_OMEN = themed("wraith_omen", Omen.Theme.WRAITH);
    public static final DeferredItem<ThemedOmenItem> GOLDEN_OMEN = themed("golden_omen", Omen.Theme.GOLDEN);
    public static final DeferredItem<LassoItem> ILLAGER_LASSO = ITEMS.registerItem("illager_lasso", LassoItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<ForbiddenPotionItem> FORBIDDEN_POTION = ITEMS.registerItem("forbidden_potion", ForbiddenPotionItem::new,
            p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> OMEN_ALTAR = ITEMS.registerSimpleBlockItem("omen_altar", ModBlocks9.OMEN_ALTAR);
    public static final DeferredItem<BlockItem> ILLAGER_PRISON = ITEMS.registerSimpleBlockItem("illager_prison", ModBlocks9.ILLAGER_PRISON);

    private static DeferredItem<ThemedOmenItem> themed(String name, Omen.Theme t) {
        return ITEMS.registerItem(name, p -> new ThemedOmenItem(p, t), p -> p.stacksTo(16).rarity(Rarity.RARE)
                .component(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(0)));
    }

    public static void init() {}

    private ModItems20() {}
}
