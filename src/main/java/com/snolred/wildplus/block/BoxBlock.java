package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** 정렬 상자(27칸, 상자 창) · 자동 사료통 · 비료 살포기(9칸, 발사기 창) */
public class BoxBlock extends BaseEntityBlock {
    public enum Kind { SORTING, FEEDER, SPREADER }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape FEEDER_SHAPE = Block.box(0, 0, 2, 16, 8, 14);
    private final Kind kind;

    public BoxBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (kind != Kind.FEEDER) return super.getShape(state, level, pos, ctx);
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? FEEDER_SHAPE : Block.box(2, 0, 0, 14, 8, 16);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case SORTING -> new SortingChestBlockEntity(pos, state);
            case FEEDER -> new FeederBlockEntity(pos, state);
            case SPREADER -> new SpreaderBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return switch (kind) {
            case SORTING -> createTickerHelper(type, ModBlockEntities.SORTING_CHEST.get(), SortingChestBlockEntity::serverTick);
            case FEEDER -> createTickerHelper(type, ModBlockEntities.FEEDER.get(), FeederBlockEntity::serverTick);
            case SPREADER -> createTickerHelper(type, ModBlockEntities.SPREADER.get(), SpreaderBlockEntity::serverTick);
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BoxBlockEntity be) {
            if (kind == Kind.SORTING) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, be),
                        Component.translatable("block.wildplus.sorting_chest")));
            } else {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DispenserMenu(id, inv, be),
                        Component.translatable(kind == Kind.SPREADER ? "block.wildplus.fertilizer_spreader" : "block.wildplus.feeder")));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
