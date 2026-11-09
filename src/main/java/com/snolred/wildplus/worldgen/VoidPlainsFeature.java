package com.snolred.wildplus.worldgen;

import com.mojang.serialization.MapCodec;
import com.snolred.wildplus.event.VoidBiome;
import com.snolred.wildplus.registry.ModBlocks15;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** 공허 수정 평원: 청크마다 지면을 덮고 수정 기둥을 세운다 */
public final class VoidPlainsFeature implements Feature {
    public static final VoidPlainsFeature INSTANCE = new VoidPlainsFeature();
    public static final MapCodec<VoidPlainsFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    private static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    private static boolean here(WorldGenLevel level, BlockPos p) {
        return level.getBiome(p).is(VoidBiome.KEY);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource r, BlockPos origin) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        BlockState turf = ModBlocks15.VOID_TURF.get().defaultBlockState();
        boolean any = false;
        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                int y = top(level, x, z);
                if (y <= level.getMinY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                if (level.getBlockState(p).is(Blocks.END_STONE) && here(level, p)) {
                    level.setBlock(p, turf, 2);
                    any = true;
                }
            }
        }
        if (!any) return false;
        int tries = 1 + r.nextInt(3);
        for (int i = 0; i < tries; i++) crystal(level, r, cx + 2 + r.nextInt(12), cz + 2 + r.nextInt(12), turf);
        for (int i = 0; i < 6; i++) {
            int x = cx + r.nextInt(16), z = cz + r.nextInt(16), y = top(level, x, z);
            if (level.getBlockState(new BlockPos(x, y, z)).is(turf.getBlock())) level.setBlock(new BlockPos(x, y + 1, z), purple(), 2);
        }
        return true;
    }

    private static BlockState purple() {
        return ModBlocks15.VOID_CRYSTAL_PURPLE.get().defaultBlockState();
    }

    private static void crystal(WorldGenLevel level, RandomSource r, int x, int z, BlockState turf) {
        int y = top(level, x, z);
        if (!level.getBlockState(new BlockPos(x, y, z)).is(turf.getBlock())) return;
        BlockState c = r.nextBoolean() ? ModBlocks15.VOID_CRYSTAL_CYAN.get().defaultBlockState() : purple();
        int h = 4 + r.nextInt(13);
        for (int k = 1; k <= h; k++) level.setBlock(new BlockPos(x, y + k, z), c, 2);
        int base = h / 3;
        for (int k = 1; k <= base; k++) {
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) level.setBlock(new BlockPos(x + d[0], y + k, z + d[1]), c, 2);
        }
    }
}
