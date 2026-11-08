package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.0 공허: 광석(엔드 외곽 · 네더라이트 곡괭이) · 블록 · 유적 벽돌 */
public final class ModBlocks11 {
    private ModBlocks11() {}

    public static final DeferredBlock<DropExperienceBlock> VOID_ORE = BLOCKS.registerBlock("void_ore",
            p -> new DropExperienceBlock(UniformInt.of(6, 12), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).strength(8.0F, 12.0F).lightLevel(s -> 4));
    public static final DeferredBlock<Block> VOID_BLOCK = BLOCKS.registerBlock("void_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).lightLevel(s -> 5));
    public static final DeferredBlock<Block> VOID_BRICK = BLOCKS.registerBlock("void_brick",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS));

    public static void init() {}
}
