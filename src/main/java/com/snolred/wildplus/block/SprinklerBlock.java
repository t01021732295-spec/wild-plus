package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 스프링클러: 2초마다 주변 9x9 범위에 물을 뿌린다.
 * - 경작지를 항상 촉촉하게 유지
 * - 작물이 자라는 속도가 약 3~4배 빨라짐
 */
public class SprinklerBlock extends Block {
    public static final int INTERVAL = 40;
    public static final int RADIUS = 4;
    private static final float GROW_CHANCE = 0.12F;
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0);

    public SprinklerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.hasProperty(BlockStateProperties.MOISTURE)) {
                        if (s.getValue(BlockStateProperties.MOISTURE) < 7) {
                            level.setBlock(p, s.setValue(BlockStateProperties.MOISTURE, 7), Block.UPDATE_CLIENTS);
                        }
                    } else if (s.is(BlockTags.CROPS) && random.nextFloat() < GROW_CHANCE) {
                        s.randomTick(level, p, random);
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 1.8, 0.2, 1.8, 0.1);
        level.scheduleTick(pos, this, INTERVAL);
    }
}
