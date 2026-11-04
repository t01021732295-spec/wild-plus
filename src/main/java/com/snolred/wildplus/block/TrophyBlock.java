package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 3.4 주말 대회 트로피 (금 · 은 · 동): 장식 가구 · 집 점수 +6 · 반짝임 */
public class TrophyBlock extends BigBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    public final int place;

    public TrophyBlock(Properties p, int place) {
        super(p, null, 0, 0, 0, 0, 1, null);
        this.place = place;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(place == 1 ? 3 : 6) == 0) {
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.5 + r.nextGaussian() * 0.25, pos.getY() + 0.9 + r.nextDouble() * 0.3,
                    pos.getZ() + 0.5 + r.nextGaussian() * 0.25, 0, 0.01, 0);
        }
    }
}
