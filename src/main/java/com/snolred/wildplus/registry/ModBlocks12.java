package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.VoidLiftBlock;
import com.snolred.wildplus.block.VoidSealBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.0 공허 유적 전용 블록 (부서지지 않음 · 아이템 없음) */
public final class ModBlocks12 {
    private ModBlocks12() {}

    private static BlockBehaviour.Properties hard(int light) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).lightLevel(s -> light);
    }

    public static final DeferredBlock<VoidSealBlock> VOID_SEAL = BLOCKS.registerBlock("void_seal", VoidSealBlock::new, () -> hard(9));
    public static final DeferredBlock<Block> VOID_GATE = BLOCKS.registerBlock("void_gate", Block::new, () -> hard(6));
    public static final DeferredBlock<Block> VOID_ALTAR = BLOCKS.registerBlock("void_altar", Block::new, () -> hard(12));
    public static final DeferredBlock<VoidLiftBlock> VOID_LIFT = BLOCKS.registerBlock("void_lift", VoidLiftBlock::new, () -> hard(11));

    public static void init() {}
}
