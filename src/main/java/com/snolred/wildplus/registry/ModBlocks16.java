package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.LordAltarBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.4 공허 군주의 제단 */
public final class ModBlocks16 {
    private ModBlocks16() {}

    public static final DeferredBlock<LordAltarBlock> LORD_ALTAR = BLOCKS.registerBlock("lord_altar", LordAltarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).lightLevel(s -> 12));

    public static void init() {}
}
