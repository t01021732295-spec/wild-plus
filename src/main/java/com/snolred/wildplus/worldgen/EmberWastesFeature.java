package com.snolred.wildplus.worldgen;

import com.mojang.serialization.MapCodec;
import com.snolred.wildplus.event.NetherBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** 5.1.1 잿불 황야: 드러난 네더랙 바닥을 흑암 · 마그마 · 현무암으로 덮고, 군데군데 불 뿜는 잿불 분화구를 세운다 */
public final class EmberWastesFeature implements Feature {
    public static final EmberWastesFeature INSTANCE = new EmberWastesFeature();
    public static final MapCodec<EmberWastesFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    static boolean floor(WorldGenLevel level, BlockPos p) {
        return level.getBlockState(p).is(Blocks.NETHERRACK) && level.getBlockState(p.above()).isAir();
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource r, BlockPos origin) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        if (!level.getBiome(new BlockPos(cx + 8, 64, cz + 8)).is(NetherBiomes.EMBER)) return false;
        BlockState black = Blocks.BLACKSTONE.defaultBlockState(), magma = Blocks.MAGMA_BLOCK.defaultBlockState(), basalt = Blocks.BASALT.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                for (int y = 32; y < 118; y++) {
                    m.set(x, y, z);
                    if (!floor(level, m) || !level.getBiome(m).is(NetherBiomes.EMBER)) continue;
                    float q = r.nextFloat();
                    level.setBlock(m, q < 0.12F ? magma : q < 0.55F ? black : q < 0.65F ? basalt : level.getBlockState(m), 2);
                    if (q < 0.015F) level.setBlock(m.above(), Blocks.FIRE.defaultBlockState(), 2);
                }
            }
        }
        // 잿불 분화구: 마그마 둔덕 + 가운데 용암
        for (int i = 0; i < 2; i++) {
            if (r.nextFloat() > 0.45F) continue;
            int x = cx + 3 + r.nextInt(10), z = cz + 3 + r.nextInt(10);
            for (int y = 40; y < 110; y++) {
                m.set(x, y, z);
                if (!floor(level, m) && !(level.getBlockState(m).is(Blocks.BLACKSTONE) || level.getBlockState(m).is(Blocks.MAGMA_BLOCK)) || !level.getBlockState(m.above()).isAir()) continue;
                int h = 2 + r.nextInt(3);
                for (int dy = 1; dy <= h; dy++) {
                    int rad = h - dy + 1;
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++)
                            if (dx * dx + dz * dz <= rad * rad && level.getBlockState(m.offset(dx, dy, dz)).isAir())
                                level.setBlock(m.offset(dx, dy, dz), r.nextFloat() < 0.4F ? magma : black, 2);
                }
                level.setBlock(m.offset(0, h, 0), Blocks.LAVA.defaultBlockState(), 2);
                break;
            }
        }
        return true;
    }
}
