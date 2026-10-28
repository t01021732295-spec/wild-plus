package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.block.ChiliCropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WildPlus.MODID);

    public static final DeferredBlock<ChiliCropBlock> CHILI_CROP = BLOCKS.registerBlock(
            "chili_crop", ChiliCropBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    private ModBlocks() {}
}
