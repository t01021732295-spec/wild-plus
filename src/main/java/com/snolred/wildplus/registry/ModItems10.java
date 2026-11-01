package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 18차: 광물 사과 (사과 + 광물 8개, 황금 사과처럼 언제든 먹을 수 있음).
 * 비싼 광물일수록 효과가 강하다. 루비·네더라이트·스타 루비 사과는 빛이 난다.
 */
public final class ModItems10 {
    static MobEffectInstance e(Holder<MobEffect> eff, int seconds, int amp) {
        return new MobEffectInstance(eff, seconds * 20, amp);
    }

    static DeferredItem<Item> apple(String name, Rarity rarity, boolean glint, int food, float sat, MobEffectInstance... effects) {
        return ITEMS.registerSimpleItem(name, p -> {
            p = p.rarity(rarity).food(new FoodProperties.Builder().nutrition(food).saturationModifier(sat).alwaysEdible().build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects))).build());
            if (glint) p = p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return p;
        });
    }

    /** 구리: 신속 I 1분 · 성급함 I 1분 */
    public static final DeferredItem<Item> COPPER_APPLE = apple("copper_apple", Rarity.COMMON, false, 4, 0.6F,
            e(MobEffects.SPEED, 60, 0), e(MobEffects.HASTE, 60, 0));
    /** 철: 저항 I 1분 · 흡수 I 1분 */
    public static final DeferredItem<Item> IRON_APPLE = apple("iron_apple", Rarity.COMMON, false, 4, 0.8F,
            e(MobEffects.RESISTANCE, 60, 0), e(MobEffects.ABSORPTION, 60, 0));
    /** 레드스톤: 신속 II 45초 · 점프 강화 II 45초 */
    public static final DeferredItem<Item> REDSTONE_APPLE = apple("redstone_apple", Rarity.COMMON, false, 4, 0.6F,
            e(MobEffects.SPEED, 45, 1), e(MobEffects.JUMP_BOOST, 45, 1));
    /** 청금석: 행운 I 3분 · 수중 호흡 3분 · 돌고래의 우아함 1분 */
    public static final DeferredItem<Item> LAPIS_APPLE = apple("lapis_apple", Rarity.UNCOMMON, false, 4, 0.8F,
            e(MobEffects.LUCK, 180, 0), e(MobEffects.WATER_BREATHING, 180, 0), e(MobEffects.DOLPHINS_GRACE, 60, 0));
    /** 에메랄드: 마을의 영웅 II 5분 (거래 할인) · 행운 II 5분 */
    public static final DeferredItem<Item> EMERALD_APPLE = apple("emerald_apple", Rarity.UNCOMMON, false, 4, 1.0F,
            e(MobEffects.HERO_OF_THE_VILLAGE, 300, 1), e(MobEffects.LUCK, 300, 1));
    /** 비취: 재생 I 1분 · 포화 · 행운 I 3분 */
    public static final DeferredItem<Item> JADE_APPLE = apple("jade_apple", Rarity.UNCOMMON, false, 6, 1.2F,
            e(MobEffects.REGENERATION, 60, 0), e(MobEffects.SATURATION, 2, 0), e(MobEffects.LUCK, 180, 0));
    /** 미스릴: 성급함 II 5분 · 야간 투시 5분 */
    public static final DeferredItem<Item> MYTHRIL_APPLE = apple("mythril_apple", Rarity.RARE, false, 4, 1.0F,
            e(MobEffects.HASTE, 300, 1), e(MobEffects.NIGHT_VISION, 300, 0));
    /** 다이아몬드: 저항 II 1분 · 흡수 II 2분 · 재생 II 10초 */
    public static final DeferredItem<Item> DIAMOND_APPLE = apple("diamond_apple", Rarity.RARE, false, 4, 1.2F,
            e(MobEffects.RESISTANCE, 60, 1), e(MobEffects.ABSORPTION, 120, 1), e(MobEffects.REGENERATION, 10, 1));
    /** 루비: 힘 II 1분 · 화염 저항 5분 · 흡수 I 2분 */
    public static final DeferredItem<Item> RUBY_APPLE = apple("ruby_apple", Rarity.EPIC, true, 4, 1.2F,
            e(MobEffects.STRENGTH, 60, 1), e(MobEffects.FIRE_RESISTANCE, 300, 0), e(MobEffects.ABSORPTION, 120, 0));
    /** 네더라이트: 저항 III 30초 · 흡수 IV 2분 · 재생 II 20초 · 화염 저항 5분 */
    public static final DeferredItem<Item> NETHERITE_APPLE = apple("netherite_apple", Rarity.EPIC, true, 6, 1.4F,
            e(MobEffects.RESISTANCE, 30, 2), e(MobEffects.ABSORPTION, 120, 3), e(MobEffects.REGENERATION, 20, 1), e(MobEffects.FIRE_RESISTANCE, 300, 0));
    /** 스타 루비: 힘 II · 저항 II · 재생 II · 신속 II 1분 + 흡수 IV 3분 + 화염 저항 · 야간 투시 5분 */
    public static final DeferredItem<Item> STAR_RUBY_APPLE = apple("star_ruby_apple", Rarity.EPIC, true, 8, 1.6F,
            e(MobEffects.STRENGTH, 60, 1), e(MobEffects.RESISTANCE, 60, 1), e(MobEffects.REGENERATION, 60, 1), e(MobEffects.SPEED, 60, 1),
            e(MobEffects.ABSORPTION, 180, 3), e(MobEffects.FIRE_RESISTANCE, 300, 0), e(MobEffects.NIGHT_VISION, 300, 0));

    // ───────── 활 · 화살 ─────────
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> MYTHRIL_BOW = ITEMS.registerItem("mythril_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(p.durability(768).enchantable(1).rarity(Rarity.UNCOMMON), "mythril", 1.5F, 1.2F));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> RUBY_BOW = ITEMS.registerItem("ruby_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(p.durability(1100).enchantable(1).fireResistant().rarity(Rarity.RARE), "ruby", 1.25F, 1.35F));
    public static final DeferredItem<com.snolred.wildplus.item.WildBowItem> STAR_RUBY_BOW = ITEMS.registerItem("star_ruby_bow",
            p -> new com.snolred.wildplus.item.WildBowItem(p.durability(4000).enchantable(1).fireResistant().rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE), "star_ruby", 2.0F, 1.6F));
    public static final DeferredItem<com.snolred.wildplus.item.WildArrowItem> EXPLOSIVE_ARROW = ITEMS.registerItem("explosive_arrow",
            p -> new com.snolred.wildplus.item.WildArrowItem(p, "explosive"));
    public static final DeferredItem<com.snolred.wildplus.item.WildArrowItem> HOMING_ARROW = ITEMS.registerItem("homing_arrow",
            p -> new com.snolred.wildplus.item.WildArrowItem(p, "homing"));
    public static final DeferredItem<com.snolred.wildplus.item.WildArrowItem> LIGHTNING_ARROW = ITEMS.registerItem("lightning_arrow",
            p -> new com.snolred.wildplus.item.WildArrowItem(p, "lightning"));
    public static final DeferredItem<com.snolred.wildplus.item.WildArrowItem> FROST_ARROW = ITEMS.registerItem("frost_arrow",
            p -> new com.snolred.wildplus.item.WildArrowItem(p, "frost"));

    public static void init() {}

    private ModItems10() {}
}
