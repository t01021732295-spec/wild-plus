package com.snolred.wildplus.block;

import com.snolred.wildplus.menu.MachineMenu;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** 자동화 기계 블록 (자동 수확기 · 광석 채굴기 · 자동 낚시 장치). 우클릭하면 기계 화면이 열린다. */
public class MachineBlock extends BaseEntityBlock {
    public enum Kind { HARVESTER, MINER, FISHER }

    private final Kind kind;

    public MachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case HARVESTER -> new AutoHarvesterBlockEntity(pos, state);
            case MINER -> new OreMinerBlockEntity(pos, state);
            case FISHER -> new AutoFisherBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return switch (kind) {
            case HARVESTER -> createTickerHelper(type, ModBlockEntities.AUTO_HARVESTER.get(), AutoHarvesterBlockEntity::serverTick);
            case MINER -> createTickerHelper(type, ModBlockEntities.ORE_MINER.get(), OreMinerBlockEntity::serverTick);
            case FISHER -> createTickerHelper(type, ModBlockEntities.AUTO_FISHER.get(), AutoFisherBlockEntity::serverTick);
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            MachineMenu.open(sp, kind, be);
        }
        return InteractionResult.SUCCESS;
    }
}
