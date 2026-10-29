package com.snolred.wildplus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 비료: 클릭한 곳 주변 3x3 의 작물·묘목에 뼛가루 2번 효과 */
public class FertilizerItem extends Item {
    public FertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos center = ctx.getClickedPos();
        boolean any = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(p);
                    if (!state.is(BlockTags.CROPS) && !state.is(BlockTags.SAPLINGS)) {
                        continue;
                    }
                    boolean grew = false;
                    for (int i = 0; i < 2; i++) {
                        if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, p)) {
                            grew = true;
                        }
                    }
                    if (grew) {
                        any = true;
                        if (!level.isClientSide()) {
                            level.levelEvent(null, 1505, p, 15);
                        }
                    }
                }
            }
        }
        if (!any) {
            return InteractionResult.PASS;
        }
        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
