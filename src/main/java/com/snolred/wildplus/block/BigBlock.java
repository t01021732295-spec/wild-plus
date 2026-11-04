package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlocks2;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 여러 칸짜리 큰 블록의 본체. 본체(가운데 아래 칸)가 큰 모델 전체를 그리고,
 * 나머지 칸은 보이지 않는 "부속 칸"(BigPartBlock)으로 채워 충돌·클릭·부수기를 본체로 넘긴다.
 * 크기: 좌우 lat0~lat1, 앞뒤 dep0~dep1 (+ = 뒤쪽), 높이 0~height-1. 위쪽 가운데 칸 모양을 따로 그리고 싶으면 topPart.
 */
public class BigBlock extends HorizontalDirectionalBlock {
    private final BiConsumer<ServerPlayer, BlockPos> opener;
    public final int lat0, lat1, dep0, dep1, height;
    private final BigPartBlock.Part topPart;

    public BigBlock(Properties properties, BiConsumer<ServerPlayer, BlockPos> opener, int lat0, int lat1, int dep0, int dep1, int height,
                    BigPartBlock.Part topPart) {
        super(properties);
        this.opener = opener;
        this.lat0 = lat0;
        this.lat1 = lat1;
        this.dep0 = dep0;
        this.dep1 = dep1;
        this.height = height;
        this.topPart = topPart;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 본체 위치·방향에서 부속 칸 위치들 (본체 제외) */
    public List<BlockPos> parts(BlockPos master, Direction facing) {
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int l = lat0; l <= lat1; l++) {
                for (int d = dep0; d <= dep1; d++) {
                    if (y == 0 && l == 0 && d == 0) continue;
                    out.add(master.relative(right, l).relative(back, d).above(y));
                }
            }
        }
        return out;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        for (BlockPos p : parts(ctx.getClickedPos(), s.getValue(FACING))) {
            if (!ctx.getLevel().getBlockState(p).canBeReplaced() || ctx.getLevel().isOutsideBuildHeight(p)) return null;
        }
        return s;
    }

    /** 놓일 때(손으로 놓기 · /setblock · 구조물 모두) 부속 칸을 채운다 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide() && !old.is(this)) placeParts(level, pos, state);
    }

    public void placeParts(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos top = pos.above(height - 1);
        for (BlockPos p : parts(pos, f)) {
            BigPartBlock.Part part = partAt(pos, p, top);
            level.setBlock(p, ModBlocks2.BIG_PART.get().defaultBlockState().setValue(BigPartBlock.FACING, f).setValue(BigPartBlock.PART, part), 3);
        }
    }

    /** 부속 칸 종류 (가구는 낮은 칸 · 납작한 칸) */
    protected BigPartBlock.Part partAt(BlockPos master, BlockPos p, BlockPos top) {
        return topPart != null && p.equals(top) ? topPart : BigPartBlock.Part.HIDDEN;
    }

    public void removeParts(Level level, BlockPos pos, BlockState state) {
        for (BlockPos p : parts(pos, state.getValue(FACING))) {
            if (level.getBlockState(p).is(ModBlocks2.BIG_PART.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) removeParts(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        open(player, pos);
        return InteractionResult.SUCCESS;
    }

    public void open(Player player, BlockPos masterPos) {
        if (player instanceof ServerPlayer sp && opener != null) opener.accept(sp, masterPos.immutable());
    }

    /** 부속 칸에서 본체 찾기 */
    public static BlockPos findMaster(Level level, BlockPos part) {
        for (BlockPos p : BlockPos.betweenClosed(part.offset(-2, -2, -2), part.offset(2, 0, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof BigBlock big && big.parts(p, s.getValue(FACING)).contains(part)) return p.immutable();
        }
        return null;
    }
}
