package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** 3.4 방명록: 놓은 사람이 집 주인. 손님은 별점 · 후기, 주인은 답글 (HouseVisit) */
public class GuestbookBlock extends BigBlock implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);

    public GuestbookBlock(Properties p) {
        super(p, com.snolred.wildplus.home.HouseVisit::openGuestbook, 0, 0, 0, 0, 1, null);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof GuestbookBlockEntity be) {
            be.owner = sp.getUUID();
            be.ownerName = sp.getName().getString();
            be.setChanged();
            com.snolred.wildplus.home.HouseVisit.register(sp, pos);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuestbookBlockEntity(pos, state);
    }
}
