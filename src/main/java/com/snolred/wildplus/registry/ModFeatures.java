package com.snolred.wildplus.registry;

import com.mojang.serialization.MapCodec;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.worldgen.VoidPlainsFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, WildPlus.MODID);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<VoidPlainsFeature>> VOID_PLAINS = TYPES.register("void_plains", () -> VoidPlainsFeature.CODEC);

    private ModFeatures() {}
}
