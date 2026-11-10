package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.8 거래소 (3×1×4) */
public final class ModBlocks19 {
    private ModBlocks19() {}

    public static final DeferredBlock<BigBlock> MARKET_STALL = BLOCKS.registerBlock("market_stall",
            p -> new BigBlock(p, com.snolred.wildplus.economy.Market::open, -1, 1, 0, 0, 4, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static void init() {}
}
