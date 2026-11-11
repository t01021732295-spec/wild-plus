package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 5.0 네더: 겁화석 광석(네더 깊은 곳 · 네더라이트 곡괭이) · 겁화석 블록 */
public final class ModBlocks21 {
    private ModBlocks21() {}

    public static final DeferredBlock<DropExperienceBlock> KALPA_ORE = BLOCKS.registerBlock("kalpa_ore",
            p -> new DropExperienceBlock(UniformInt.of(6, 12), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERRACK).strength(8.0F, 12.0F).lightLevel(s -> 6));
    public static final DeferredBlock<Block> KALPA_BLOCK = BLOCKS.registerBlock("kalpa_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).lightLevel(s -> 8));

    public static void init() {}
}
