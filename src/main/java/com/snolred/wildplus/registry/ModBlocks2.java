package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 13차: 루비 광석 (다이아몬드보다 깊고 드묾) */
public final class ModBlocks2 {
    public static final DeferredBlock<DropExperienceBlock> RUBY_ORE = BLOCKS.registerBlock("ruby_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_RUBY_ORE = BLOCKS.registerBlock("deepslate_ruby_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    public static final DeferredBlock<Block> RUBY_BLOCK = BLOCKS.registerBlock("ruby_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));

    public static void init() {}

    private ModBlocks2() {}
}
