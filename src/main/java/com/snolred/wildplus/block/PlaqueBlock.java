package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 3.4 우리 집 문패: 벽(뒤쪽)에 붙는 얇은 판. 누르면 근처 방명록의 집 이름 · 별점 · 집 점수 */
public class PlaqueBlock extends BigBlock {
    private static final VoxelShape N = Block.box(1, 2, 13, 15, 15, 16), S = Block.box(1, 2, 0, 15, 15, 3),
            E = Block.box(0, 2, 1, 3, 15, 15), W = Block.box(13, 2, 1, 16, 15, 15);

    public PlaqueBlock(Properties p) {
        super(p, com.snolred.wildplus.home.HouseVisit::plaque, 0, 0, 0, 0, 1, null);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        return f == Direction.NORTH ? N : f == Direction.SOUTH ? S : f == Direction.EAST ? E : W;
    }
}
