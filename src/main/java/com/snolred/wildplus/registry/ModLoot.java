package com.snolred.wildplus.registry;

import com.mojang.serialization.MapCodec;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.loot.FishingLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLoot {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WildPlus.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<FishingLootModifier>> FISHING =
            MODIFIERS.register("fishing", () -> FishingLootModifier.CODEC);

    private ModLoot() {}
}
