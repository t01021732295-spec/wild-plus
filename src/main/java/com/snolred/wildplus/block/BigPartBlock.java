package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 큰 블록의 부속 칸: 보통은 안 보이고(빈 모델), 클릭·부수기를 본체로 넘긴다. GATE_TOP 은 탑 입구 윗부분을 그린다. */
public class BigPartBlock extends HorizontalDirectionalBlock {
    public enum Part implements StringRepresentable {
        HIDDEN("hidden"), GATE_TOP("gate_top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BigPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.HIDDEN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** 본체가 폭발 등으로 사라져 혼자 남은 부속 칸은 스스로 없어진다 */
    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (BigBlock.findMaster(level, pos) == null) level.removeBlock(pos, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos m = BigBlock.findMaster(level, pos);
        if (m != null && level.getBlockState(m).getBlock() instanceof BigBlock big) {
            big.open(player, m);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos m = BigBlock.findMaster(level, pos);
            if (m != null && level.getBlockState(m).getBlock() instanceof BigBlock big) {
                BlockState ms = level.getBlockState(m);
                big.removeParts(level, m, ms);
                level.destroyBlock(m, !player.isCreative());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
