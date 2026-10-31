package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 모드 상태 효과: 흡혈 (입힌 피해의 20%만큼 회복) */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, WildPlus.MODID);

    public static final DeferredHolder<MobEffect, LifestealEffect> LIFESTEAL = EFFECTS.register("lifesteal", LifestealEffect::new);

    public static class LifestealEffect extends MobEffect {
        public LifestealEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xB0122A);
        }
    }

    private ModEffects() {}
}
