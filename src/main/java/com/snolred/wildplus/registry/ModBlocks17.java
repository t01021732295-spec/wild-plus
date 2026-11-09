package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.5 복권 발매기(1×1×3) · 운명의 제단(1×1×2) */
public final class ModBlocks17 {
    private ModBlocks17() {}

    public static final DeferredBlock<BigBlock> LOTTERY_MACHINE = BLOCKS.registerBlock("lottery_machine",
            p -> new BigBlock(p, com.snolred.wildplus.economy.Lotto::open, 0, 0, 0, 0, 3, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<BigBlock> FATE_ALTAR = BLOCKS.registerBlock("fate_altar",
            p -> new BigBlock(p, com.snolred.wildplus.block.FateAltar::use, 0, 0, 0, 0, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion().lightLevel(s -> 9));

    public static void init() {}
}
