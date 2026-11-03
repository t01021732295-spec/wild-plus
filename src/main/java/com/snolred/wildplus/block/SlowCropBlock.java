package com.snolred.wildplus.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

/** 느리게 자라는 작물: 무작위 틱 중 일부(chance)만 성장에 쓴다. 뼛가루도 한 번에 한 단계만. */
public class SlowCropBlock extends SimpleCropBlock {
    private final float chance;

    public SlowCropBlock(Properties properties, Supplier<? extends ItemLike> seed, float chance) {
        super(properties, seed);
        this.chance = chance;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < chance) super.randomTick(state, level, pos, random);
    }

    @Override
    protected int getBonemealAgeIncrease(net.minecraft.world.level.Level level) {
        return 1;
    }
}
