package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.2 엔드 시티 금고 자물쇠 (부서지지 않음 · 아이템 없음) */
public final class ModBlocks14 {
    private ModBlocks14() {}

    public static final DeferredBlock<Block> VAULT_LOCK = BLOCKS.registerBlock("vault_lock", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).lightLevel(s -> 12));

    public static void init() {}
}
