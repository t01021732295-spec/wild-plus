package com.snolred.wildplus.event;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** 공허 수정 평원: 엔드 외곽(고지·중지)의 일부를 이 바이옴으로 바꾼다. mixin/TheEndBiomeSourceMixin 이 읽음 */
public final class VoidBiome {
    public static final ResourceKey<Biome> KEY = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("wildplus", "void_crystal_plains"));
    public static volatile Holder<Biome> holder;

    private VoidBiome() {}

    @SubscribeEvent
    public static void onStart(ServerAboutToStartEvent e) {
        holder = e.getServer().registryAccess().lookupOrThrow(Registries.BIOME).get(KEY).map(h -> (Holder<Biome>) h).orElse(null);
    }

    /** x, z 는 쿼트 좌표(블록÷4). 중앙 섬 부근은 제외 */
    public static boolean inZone(int x, int z) {
        if ((long) x * x + (long) z * z < 62_500L) return false; // 약 1000블록 안쪽
        return Math.sin(x * 0.011) + Math.cos(z * 0.013) + Math.sin((x + z) * 0.007) > 0.9;
    }
}
