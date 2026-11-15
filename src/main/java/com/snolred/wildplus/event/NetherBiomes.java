package com.snolred.wildplus.event;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * 5.1.1 네더 바이옴 2종 (mixin/MultiNoiseBiomeSourceMixin 이 바꿔 끼움)
 * · 잿불 황야(ember_wastes): 네더 황무지 중 현무암 삼각주 쪽(온도가 낮은 쪽) 부분
 * · 재의 숲(ash_grove): 영혼 모래 골짜기 깊은 쪽(습도가 아주 낮은 쪽) 부분
 */
public final class NetherBiomes {
    public static final ResourceKey<Biome> EMBER = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("wildplus", "ember_wastes"));
    public static final ResourceKey<Biome> ASH = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("wildplus", "ash_grove"));
    public static volatile Holder<Biome> ember, ash;

    private NetherBiomes() {}

    @SubscribeEvent
    public static void onStart(ServerAboutToStartEvent e) {
        var reg = e.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
        ember = reg.get(EMBER).map(h -> (Holder<Biome>) h).orElse(null);
        ash = reg.get(ASH).map(h -> (Holder<Biome>) h).orElse(null);
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {
        ember = null;
        ash = null;
    }

    /** 바닐라가 고른 바이옴을 기후 값에 따라 바꿔 끼운다 (네더는 온도 · 습도만 씀, 값은 ×10000) */
    public static Holder<Biome> swap(Holder<Biome> r, Climate.TargetPoint t) {
        if (ember != null && r.is(Biomes.NETHER_WASTES) && t.temperature() < -1200L) return ember;
        if (ash != null && r.is(Biomes.SOUL_SAND_VALLEY) && t.humidity() < -6500L) return ash;
        return r;
    }
}
