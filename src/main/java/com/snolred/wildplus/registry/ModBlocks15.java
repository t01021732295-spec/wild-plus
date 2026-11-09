package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.3 공허 수정 평원 지면 · 수정 */
public final class ModBlocks15 {
    private ModBlocks15() {}

    public static final DeferredBlock<Block> VOID_TURF = BLOCKS.registerBlock("void_turf", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE));
    public static final DeferredBlock<Block> VOID_CRYSTAL_CYAN = BLOCKS.registerBlock("void_crystal_cyan", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel(s -> 9));
    public static final DeferredBlock<Block> VOID_CRYSTAL_PURPLE = BLOCKS.registerBlock("void_crystal_purple", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel(s -> 7));

    public static void init() {}
}
