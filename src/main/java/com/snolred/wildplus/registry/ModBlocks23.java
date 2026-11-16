package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.AncientBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 5.3 고대 도시 구조물 블록 (부서지지 않음 · 아이템 없음): 공명 종 · 스컬크 봉인 · 악보 석판 · 성소 제단 */
public final class ModBlocks23 {
    private ModBlocks23() {}

    private static BlockBehaviour.Properties hard(int light) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).lightLevel(s -> light);
    }

    public static final DeferredBlock<AncientBlocks.Bell> ANCIENT_BELL = BLOCKS.registerBlock("ancient_bell", AncientBlocks.Bell::new, () -> hard(6).noOcclusion());
    public static final DeferredBlock<Block> ANCIENT_SEAL = BLOCKS.registerBlock("ancient_seal", Block::new, () -> hard(4));
    public static final DeferredBlock<AncientBlocks.Tablet> SCORE_TABLET = BLOCKS.registerBlock("score_tablet", AncientBlocks.Tablet::new, () -> hard(5));
    public static final DeferredBlock<AncientBlocks.Altar> SANCTUM_ALTAR = BLOCKS.registerBlock("sanctum_altar", AncientBlocks.Altar::new, () -> hard(9));

    public static void init() {}
}
