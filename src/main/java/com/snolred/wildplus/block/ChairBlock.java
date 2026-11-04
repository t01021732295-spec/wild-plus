package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 식당 의자: 손님이 여기 앉아서 먹는다 (FACING = 앉은 사람이 보는 쪽) */
public class ChairBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SEAT = Block.box(2.5, 0, 2.5, 13.5, 9.5, 13.5);

    public ChairBlock(Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        VoxelShape back = switch (f) {
            case NORTH -> Block.box(2.5, 9.5, 11.5, 13.5, 19, 13.5);
            case SOUTH -> Block.box(2.5, 9.5, 2.5, 13.5, 19, 4.5);
            case WEST -> Block.box(11.5, 9.5, 2.5, 13.5, 19, 13.5);
            default -> Block.box(2.5, 9.5, 2.5, 4.5, 19, 13.5);
        };
        return Shapes.or(SEAT, back);
    }
}
