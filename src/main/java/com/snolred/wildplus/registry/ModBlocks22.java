package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.KalpaAltarBlock;
import com.snolred.wildplus.block.KalpaBrazierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 5.1 겁화의 성채 전용 블록 (부서지지 않음 · 아이템 없음): 본성 문 · 군주의 제단 · 화로 */
public final class ModBlocks22 {
    private ModBlocks22() {}

    private static BlockBehaviour.Properties hard(int light) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).lightLevel(s -> light);
    }

    public static final DeferredBlock<Block> KALPA_GATE = BLOCKS.registerBlock("kalpa_gate", Block::new, () -> hard(7));
    public static final DeferredBlock<KalpaAltarBlock> KALPA_ALTAR = BLOCKS.registerBlock("kalpa_altar", KalpaAltarBlock::new, () -> hard(13));
    public static final DeferredBlock<KalpaBrazierBlock> KALPA_BRAZIER = BLOCKS.registerBlock("kalpa_brazier", KalpaBrazierBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noOcclusion().lightLevel(s -> s.getValue(KalpaBrazierBlock.LIT) ? 15 : 4));

    public static void init() {}
}
