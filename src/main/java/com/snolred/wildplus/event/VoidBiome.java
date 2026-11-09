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

    /** 시험용: 엔드에서 평원 지면이 깔린 곳을 찾아 그 위로 날아간다 */
    public static void debugGoto(net.minecraft.server.level.ServerPlayer p) {
        var end = p.level().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return;
        var turf = com.snolred.wildplus.registry.ModBlocks15.VOID_TURF.get();
        int checked = 0;
        for (int ring = 0; ring < 80 && checked < 700; ring++) {
            for (int cx = -ring; cx <= ring; cx++) {
                for (int cz = -ring; cz <= ring; cz++) {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring || (cx + cz) % 2 != 0) continue;
                    int bx = 832 + cx * 16 + 8, bz = 608 + cz * 16 + 8;
                    checked++;
                    var chunk = end.getChunk(bx >> 4, bz >> 4);
                    for (int y = 90; y > 20; y--) {
                        var q = new net.minecraft.core.BlockPos(bx, y, bz);
                        if (!chunk.getBlockState(q).is(turf)) continue;
                        p.getAbilities().flying = true;
                        p.onUpdateAbilities();
                        p.teleportTo(end, bx + 0.5, y + 16.0, bz + 0.5, java.util.Set.of(), 0F, 45F, true);
                        com.snolred.wildplus.WildPlus.LOGGER.info("ENDPLAINS found {} {} {} after {}", bx, y, bz, checked);
                        return;
                    }
                }
            }
        }
        com.snolred.wildplus.WildPlus.LOGGER.info("ENDPLAINS none after {}", checked);
    }

    /** 시험용: 가장 가까운 공허 유적으로 순간이동 */
    public static void debugRuin(net.minecraft.server.level.ServerPlayer p) {
        var end = p.level().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return;
        var reg = end.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var key = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("wildplus", "void_ruin"));
        var found = end.getChunkSource().getGenerator().findNearestMapStructure(end, net.minecraft.core.HolderSet.direct(reg.getOrThrow(key)), new net.minecraft.core.BlockPos(0, 60, 0), 100, false);
        if (found == null) {
            com.snolred.wildplus.WildPlus.LOGGER.info("ENDRUIN none");
            return;
        }
        var at = found.getFirst();
        com.snolred.wildplus.WildPlus.LOGGER.info("ENDRUIN at {}", at);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(end, at.getX() + 0.5, 60.0, at.getZ() + 0.5, java.util.Set.of(), 0F, 20F, true);
    }

    /** x, z 는 쿼트 좌표(블록÷4). 중앙 섬 부근은 제외 */
    public static boolean inZone(int x, int z) {
        if ((long) x * x + (long) z * z < 62_500L) return false; // 약 1000블록 안쪽
        return Math.sin(x * 0.011) + Math.cos(z * 0.013) + Math.sin((x + z) * 0.007) > 0.9;
    }
}
