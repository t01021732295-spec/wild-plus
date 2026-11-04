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
        HIDDEN("hidden"), GATE_TOP("gate_top"), LOW("low"), FLAT("flat");

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

    private static final net.minecraft.world.phys.shapes.VoxelShape LOW_SHAPE = Block.box(0, 0, 0, 16, 10, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape FLAT_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                  net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return switch (state.getValue(PART)) {
            case LOW -> LOW_SHAPE;
            case FLAT -> FLAT_SHAPE;
            default -> super.getShape(state, level, pos, ctx);
        };
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                           net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return state.getValue(PART) == Part.FLAT ? net.minecraft.world.phys.shapes.Shapes.empty() : getShape(state, level, pos, ctx);
    }

    /** 책장 가구의 부속 칸도 인챈트 책장으로 침 */
    @Override
    public float getEnchantPowerBonus(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 0, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FurnitureBlock f && f.parts(p, s.getValue(FACING)).contains(pos)) return f.enchantPower();
        }
        return 0.0F;
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
