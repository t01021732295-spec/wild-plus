package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 3.8 흉조 블록: 흉조 제단 (마을 수호 게이지) · 우민 감옥 (포로) */
public final class ModBlocks9 {
    public static final DeferredBlock<BigBlock> OMEN_ALTAR = BLOCKS.registerBlock("omen_altar",
            p -> new BigBlock(p, com.snolred.wildplus.omen.OmenAltar::open, 0, 1, 0, 0, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).noOcclusion().strength(3.0F, 9.0F).lightLevel(s -> 9));
    public static final DeferredBlock<BigBlock> ILLAGER_PRISON = BLOCKS.registerBlock("illager_prison",
            p -> new BigBlock(p, com.snolred.wildplus.omen.Prisoners::open, -1, 1, 0, 1, 2, null), // 3.9: 3×2×2
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).sound(SoundType.METAL).noOcclusion().strength(3.0F, 9.0F).lightLevel(s -> 6));

    public static void init() {}

    private ModBlocks9() {}
}
