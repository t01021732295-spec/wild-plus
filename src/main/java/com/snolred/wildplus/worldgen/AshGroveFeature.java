package com.snolred.wildplus.worldgen;

import com.mojang.serialization.MapCodec;
import com.snolred.wildplus.event.NetherBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** 5.1.1 재의 숲: 바닥을 영혼 흙 · 회색 재(응회암) · 흑암으로 덮고, 불탄 나무(현무암 줄기 + 가지)와 영혼 불을 둔다 */
public final class AshGroveFeature implements Feature {
    public static final AshGroveFeature INSTANCE = new AshGroveFeature();
    public static final MapCodec<AshGroveFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    private static boolean ground(BlockState s) {
        return s.is(Blocks.NETHERRACK) || s.is(Blocks.SOUL_SAND) || s.is(Blocks.SOUL_SOIL);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource r, BlockPos origin) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        if (!level.getBiome(new BlockPos(cx + 8, 64, cz + 8)).is(NetherBiomes.ASH)) return false;
        BlockState soil = Blocks.SOUL_SOIL.defaultBlockState(), tuff = Blocks.TUFF.defaultBlockState(), black = Blocks.BLACKSTONE.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                for (int y = 32; y < 118; y++) {
                    m.set(x, y, z);
                    if (!ground(level.getBlockState(m)) || !level.getBlockState(m.above()).isAir() || !level.getBiome(m).is(NetherBiomes.ASH)) continue;
                    float q = r.nextFloat();
                    level.setBlock(m, q < 0.45F ? tuff : q < 0.8F ? soil : black, 2);
                    if (q >= 0.45F && q < 0.47F) level.setBlock(m.above(), Blocks.SOUL_FIRE.defaultBlockState(), 2);
                }
            }
        }
        int trees = 1 + r.nextInt(3);
        for (int i = 0; i < trees; i++) tree(level, r, cx + 2 + r.nextInt(12), cz + 2 + r.nextInt(12));
        return true;
    }

    /** 불탄 나무: 현무암 줄기 4~8칸 + 꼭대기 근처 흑암 담장 가지 */
    private static void tree(WorldGenLevel level, RandomSource r, int x, int z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = 34; y < 110; y++) {
            m.set(x, y, z);
            var s = level.getBlockState(m);
            if (!(s.is(Blocks.TUFF) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.BLACKSTONE)) || !level.getBlockState(m.above()).isAir()) continue;
            int h = 4 + r.nextInt(5);
            for (int k = 1; k <= h; k++) {
                if (!level.getBlockState(m.above(k)).isAir()) return;
            }
            BlockState trunk = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int k = 1; k <= h; k++) level.setBlock(m.above(k), trunk, 2);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (r.nextFloat() < 0.55F) {
                    BlockPos b = m.above(h - 1 - r.nextInt(2)).relative(d);
                    if (level.getBlockState(b).isAir()) level.setBlock(b, Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()), 2);
                    BlockPos b2 = b.relative(d).above();
                    if (r.nextBoolean() && level.getBlockState(b2).isAir()) level.setBlock(b2, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 2);
                }
            }
            if (r.nextFloat() < 0.4F) level.setBlock(m.above(h + 1), Blocks.SOUL_FIRE.defaultBlockState(), 2);
            return;
        }
    }
}
