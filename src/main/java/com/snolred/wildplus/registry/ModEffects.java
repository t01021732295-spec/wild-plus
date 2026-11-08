package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 모드 상태 효과와 물약.
 * - 흡혈: 입힌 피해의 20%만큼 회복
 * - 광부의 집중(버프): 블록 캐는 속도 +40%/단계
 * - 강철 피부(버프): 방어 +4 · 방어 강도 +2 /단계
 * - 탐욕(버프): 몬스터를 잡으면 동전이 더 떨어짐
 * - 출혈(디버프): 2초마다 피해, 움직이면 더 아픔
 * - 취약(디버프): 받는 피해 +25%/단계
 * - 광전사(버프+디버프): 공격력 +40%, 받는 피해 +25%
 * - 유리 대포(버프+디버프): 공격력 +80%, 최대 체력 -40%
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, WildPlus.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, WildPlus.MODID);

    public static final DeferredHolder<MobEffect, LifestealEffect> LIFESTEAL = EFFECTS.register("lifesteal", LifestealEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> MINERS_FOCUS = EFFECTS.register("miners_focus",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0xF2C14E)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, WildPlus.id("effect.miners_focus"), 0.4,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> IRONSKIN = EFFECTS.register("ironskin",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0x9AA1AB)
                    .addAttributeModifier(Attributes.ARMOR, WildPlus.id("effect.ironskin"), 4.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.ARMOR_TOUGHNESS, WildPlus.id("effect.ironskin_tough"), 2.0,
                            AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> GREED = EFFECTS.register("greed",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0xFFD84A));
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FRAILTY = EFFECTS.register("frailty",
            () -> new Simple(MobEffectCategory.HARMFUL, 0x8A7AA8));
    public static final DeferredHolder<MobEffect, MobEffect> BERSERK = EFFECTS.register("berserk",
            () -> new Simple(MobEffectCategory.NEUTRAL, 0xD0202A)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, WildPlus.id("effect.berserk"), 0.4,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> GLASS_CANNON = EFFECTS.register("glass_cannon",
            () -> new Simple(MobEffectCategory.NEUTRAL, 0x9AE8FF)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, WildPlus.id("effect.glass_cannon"), 0.8,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.MAX_HEALTH, WildPlus.id("effect.glass_cannon_hp"), -0.4,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    // 4.1 신규 효과
    public static final DeferredHolder<MobEffect, MobEffect> VOID_EVADE = EFFECTS.register("void_evade", () -> new Simple(MobEffectCategory.BENEFICIAL, 0x7A46D2));
    public static final DeferredHolder<MobEffect, MobEffect> HEAL_AURA = EFFECTS.register("heal_aura", () -> new Simple(MobEffectCategory.BENEFICIAL, 0xF27896));
    public static final DeferredHolder<MobEffect, MobEffect> IRON_WALL = EFFECTS.register("iron_wall",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0x8C96AA)
                    .addAttributeModifier(Attributes.ARMOR, WildPlus.id("effect.iron_wall"), 6.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, WildPlus.id("effect.iron_wall_kb"), 1.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> ORE_SIGHT = EFFECTS.register("ore_sight", () -> new Simple(MobEffectCategory.BENEFICIAL, 0x5AD2DC));
    public static final DeferredHolder<MobEffect, MobEffect> TIME_WARP = EFFECTS.register("time_warp",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0xFAC85A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, WildPlus.id("effect.time_warp"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, WildPlus.id("effect.time_warp_as"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    // ───────── 물약 ─────────
    private static DeferredHolder<Potion, Potion> potion(String name, DeferredHolder<MobEffect, ? extends MobEffect> effect, int seconds, int amp) {
        return POTIONS.register(name, () -> new Potion("wildplus_" + name, new MobEffectInstance(effect, seconds * 20, amp)));
    }

    public static final DeferredHolder<Potion, Potion> MINERS_FOCUS_POTION = potion("miners_focus", MINERS_FOCUS, 180, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_MINERS_FOCUS_POTION = potion("strong_miners_focus", MINERS_FOCUS, 90, 1);
    public static final DeferredHolder<Potion, Potion> IRONSKIN_POTION = potion("ironskin", IRONSKIN, 180, 0);
    public static final DeferredHolder<Potion, Potion> GREED_POTION = potion("greed", GREED, 300, 0);
    public static final DeferredHolder<Potion, Potion> BLEEDING_POTION = potion("bleeding", BLEEDING, 20, 0);
    public static final DeferredHolder<Potion, Potion> FRAILTY_POTION = potion("frailty", FRAILTY, 60, 0);
    public static final DeferredHolder<Potion, Potion> BERSERK_POTION = potion("berserk", BERSERK, 120, 0);
    public static final DeferredHolder<Potion, Potion> GLASS_CANNON_POTION = potion("glass_cannon", GLASS_CANNON, 90, 0);
    public static final DeferredHolder<Potion, Potion> LIFESTEAL_POTION = potion("lifesteal", LIFESTEAL, 120, 0);
    public static final DeferredHolder<Potion, Potion> VOID_EVADE_POTION = potion("void_evade", VOID_EVADE, 180, 0);
    public static final DeferredHolder<Potion, Potion> LONG_VOID_EVADE_POTION = potion("long_void_evade", VOID_EVADE, 360, 0);
    public static final DeferredHolder<Potion, Potion> HEAL_AURA_POTION = potion("heal_aura", HEAL_AURA, 120, 0);
    public static final DeferredHolder<Potion, Potion> LONG_HEAL_AURA_POTION = potion("long_heal_aura", HEAL_AURA, 240, 0);
    public static final DeferredHolder<Potion, Potion> IRON_WALL_POTION = potion("iron_wall", IRON_WALL, 180, 0);
    public static final DeferredHolder<Potion, Potion> LONG_IRON_WALL_POTION = potion("long_iron_wall", IRON_WALL, 360, 0);
    public static final DeferredHolder<Potion, Potion> ORE_SIGHT_POTION = potion("ore_sight", ORE_SIGHT, 120, 0);
    public static final DeferredHolder<Potion, Potion> LONG_ORE_SIGHT_POTION = potion("long_ore_sight", ORE_SIGHT, 240, 0);
    public static final DeferredHolder<Potion, Potion> TIME_WARP_POTION = potion("time_warp", TIME_WARP, 60, 0);
    public static final DeferredHolder<Potion, Potion> LONG_TIME_WARP_POTION = potion("long_time_warp", TIME_WARP, 120, 0);

    static class Simple extends MobEffect {
        Simple(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static class LifestealEffect extends MobEffect {
        public LifestealEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xB0122A);
        }
    }

    /** 출혈: 2초(단계가 높을수록 더 자주)마다 1 피해, 달리는 중이면 2 */
    public static class BleedingEffect extends MobEffect {
        public BleedingEffect() {
            super(MobEffectCategory.HARMFUL, 0x8A0010);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = Math.max(10, 40 >> amplifier);
            return duration % interval == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            float dmg = entity.isSprinting() || entity.getDeltaMovement().horizontalDistanceSqr() > 0.01 ? 2.0F : 1.0F;
            entity.hurtServer(level, entity.damageSources().magic(), dmg);
            return true;
        }
    }

    private ModEffects() {}
}
