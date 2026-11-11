package com.snolred.wildplus.block;

import com.snolred.wildplus.content.Seasons;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

/** 5.0 계절 작물: 제철엔 2배 빨리, 제철이 아니면 8번에 1번만 자란다 (뼛가루는 언제나 됨) */
public class SeasonCropBlock extends SimpleCropBlock {
    public final int season;

    public SeasonCropBlock(Properties properties, Supplier<? extends ItemLike> seed, int season) {
        super(properties, seed);
        this.season = season;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Seasons.now() == season) {
            super.randomTick(state, level, pos, random);
            BlockState again = level.getBlockState(pos);
            if (again.is(this)) super.randomTick(again, level, pos, random);
        } else if (random.nextInt(8) == 0) {
            super.randomTick(state, level, pos, random);
        }
    }
}
