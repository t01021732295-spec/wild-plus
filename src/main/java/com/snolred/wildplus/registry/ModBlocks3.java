package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.SimpleCropBlock;
import com.snolred.wildplus.block.SlowCropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 2.18: 새 작물 · 비료 살포기 · 알바 게시판 */
public final class ModBlocks3 {
    public static final DeferredBlock<SimpleCropBlock> GRAPE_CROP = BLOCKS.registerBlock("grape_crop",
            p -> new SimpleCropBlock(p, () -> ModItems13.GRAPE_SEEDS.get()), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SimpleCropBlock> RICE_CROP = BLOCKS.registerBlock("rice_crop",
            p -> new SimpleCropBlock(p, () -> ModItems13.RICE_SEEDS.get()), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SimpleCropBlock> ONION_CROP = BLOCKS.registerBlock("onion_crop",
            p -> new SimpleCropBlock(p, () -> ModItems13.ONION_SEEDS.get()), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    /** 인삼: 아주 느리게 자람 (게임 속 약 7일) · 비싸다 */
    public static final DeferredBlock<SlowCropBlock> GINSENG_CROP = BLOCKS.registerBlock("ginseng_crop",
            p -> new SlowCropBlock(p, () -> ModItems13.GINSENG_SEEDS.get(), 0.12F), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    public static final DeferredBlock<com.snolred.wildplus.block.BoxBlock> SPREADER = BLOCKS.registerBlock("fertilizer_spreader",
            p -> new com.snolred.wildplus.block.BoxBlock(p, com.snolred.wildplus.block.BoxBlock.Kind.SPREADER),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion());
    public static final DeferredBlock<com.snolred.wildplus.block.ServiceBlock> JOB_BOARD = BLOCKS.registerBlock("job_board",
            p -> new com.snolred.wildplus.block.ServiceBlock(p, com.snolred.wildplus.economy.Jobs::open),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F, 1200.0F));

    public static void init() {}

    private ModBlocks3() {}
}
