package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.6 음악 플레이어 (3×1×2) */
public final class ModBlocks18 {
    private ModBlocks18() {}

    public static final DeferredBlock<BigBlock> MUSIC_PLAYER = BLOCKS.registerBlock("music_player",
            p -> new BigBlock(p, com.snolred.wildplus.music.Music::open, -1, 1, 0, 0, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static void init() {}
}
