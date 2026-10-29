package com.snolred.wildplus.block;

import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** 씨앗(또는 열매)만 다른 일반 작물 블록 */
public class SimpleCropBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seed;

    public SimpleCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = seed;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
