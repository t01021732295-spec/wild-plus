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

/**
 * 비료 (1 일반 · 2 고급 · 3 황금): 클릭한 곳 주변 3x3 의 작물·묘목에 뼛가루 효과(단계+1번)를 주고,
 * 그 아래 경작지에 영양을 남긴다 → 더 빨리 자라고 수확할 때 품질이 붙는다 (Soil, 수확 3번까지).
 */
public class FertilizerItem extends Item {
    public final int tier;

    public FertilizerItem(Properties properties) {
        this(properties, 1);
    }

    public FertilizerItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
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
                    // 경작지: 영양 남기기
                    if (level instanceof net.minecraft.server.level.ServerLevel sl && state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                            && com.snolred.wildplus.content.Soil.fertilize(sl, p, tier)) {
                        any = true;
                    }
                    if (!state.is(BlockTags.CROPS) && !state.is(BlockTags.SAPLINGS)) {
                        continue;
                    }
                    boolean grew = false;
                    for (int i = 0; i < tier + 1; i++) {
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
