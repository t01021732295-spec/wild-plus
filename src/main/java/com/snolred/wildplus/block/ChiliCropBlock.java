package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** 고추 작물: 밀과 같은 8단계 성장 (텍스처는 4단계) */
public class ChiliCropBlock extends CropBlock {
    public ChiliCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.CHILI_SEEDS.get();
    }
}
