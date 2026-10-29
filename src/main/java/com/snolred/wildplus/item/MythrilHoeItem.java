package com.snolred.wildplus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

/** 미스릴 괭이: 3x3 범위를 한 번에 경작. 웅크리고 쓰면 한 칸만. */
public class MythrilHoeItem extends Item {
    public MythrilHoeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || ctx.getClickedFace() == Direction.DOWN) {
            return super.useOn(ctx);
        }
        Level level = ctx.getLevel();
        BlockPos center = ctx.getClickedPos();
        int range = player.isSecondaryUseActive() ? 0 : 1;
        int tilled = 0;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                BlockPos p = center.offset(dx, 0, dz);
                if (!level.getBlockState(p.above()).isAir()) {
                    continue;
                }
                BlockState state = level.getBlockState(p);
                UseOnContext sub = new UseOnContext(level, player, ctx.getHand(), ctx.getItemInHand(),
                        new BlockHitResult(Vec3.atCenterOf(p).add(0, 0.5, 0), Direction.UP, p, false));
                BlockState result = state.getToolModifiedState(sub, ItemAbilities.HOE_TILL, false);
                if (result == null) {
                    result = fallbackTill(state);
                }
                if (result != null && result != state) {
                    if (!level.isClientSide()) {
                        level.setBlock(p, result, Block.UPDATE_ALL_IMMEDIATE);
                    }
                    tilled++;
                }
            }
        }
        if (tilled == 0) {
            return super.useOn(ctx);
        }
        level.playSound(player, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            ctx.getItemInHand().hurtAndBreak(1, player, ctx.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    /** 바닐라 경작 규칙과 같은 기본값 (다른 모드 블록은 위 NeoForge 훅이 처리) */
    private static BlockState fallbackTill(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) {
            return Blocks.FARMLAND.defaultBlockState();
        }
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return null;
    }
}
