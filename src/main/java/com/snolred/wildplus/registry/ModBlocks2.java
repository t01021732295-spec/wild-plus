package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 13차: 루비 광석 (다이아몬드보다 깊고 드묾) */
public final class ModBlocks2 {
    public static final DeferredBlock<DropExperienceBlock> RUBY_ORE = BLOCKS.registerBlock("ruby_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_RUBY_ORE = BLOCKS.registerBlock("deepslate_ruby_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    public static final DeferredBlock<Block> RUBY_BLOCK = BLOCKS.registerBlock("ruby_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));

    // ───────── 14차: 주말 콘텐츠 큰 블록 ─────────
    public static final DeferredBlock<com.snolred.wildplus.block.BigPartBlock> BIG_PART = BLOCKS.registerBlock("big_part",
            com.snolred.wildplus.block.BigPartBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).noOcclusion().noLootTable().strength(3.0F, 1200.0F).randomTicks());
    /** 레이드 제단: 3×3 바닥, 2칸 높이 */
    public static final DeferredBlock<com.snolred.wildplus.block.BigBlock> RAID_ALTAR = BLOCKS.registerBlock("raid_altar",
            p -> new com.snolred.wildplus.block.BigBlock(p, com.snolred.wildplus.menu.ContentMenu::openRaid, -1, 1, -1, 1, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).noOcclusion().strength(3.0F, 1200.0F).lightLevel(s -> 10));
    /** 하늘의 탑 입구: 폭 3, 높이 3 아치 */
    public static final DeferredBlock<com.snolred.wildplus.block.BigBlock> TOWER_GATE = BLOCKS.registerBlock("tower_gate",
            p -> new com.snolred.wildplus.block.BigBlock(p, com.snolred.wildplus.menu.ContentMenu::openTower, -1, 1, 0, 0, 3,
                    com.snolred.wildplus.block.BigPartBlock.Part.GATE_TOP),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion().strength(3.0F, 1200.0F).lightLevel(s -> 12));
    /** 주간 목표 게시판: 폭 3, 높이 2 */
    public static final DeferredBlock<com.snolred.wildplus.block.BigBlock> GOAL_BOARD = BLOCKS.registerBlock("goal_board",
            p -> new com.snolred.wildplus.block.BigBlock(p, com.snolred.wildplus.menu.ContentMenu::openGoals, -1, 1, 0, 0, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion().strength(2.0F));

    /** 던전 보물 창고 봉인 벽돌 (부술 수 없음, 보스를 잡으면 문만 열림) */
    public static final DeferredBlock<com.snolred.wildplus.block.SealBlock> DUNGEON_SEAL = BLOCKS.registerBlock("dungeon_seal",
            com.snolred.wildplus.block.SealBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noLootTable()
                    .lightLevel(s -> s.getValue(com.snolred.wildplus.block.SealBlock.GATE) ? 7 : 0));

    /** 필터 호퍼 (미스릴·비취) */
    public static final DeferredBlock<com.snolred.wildplus.block.FilterHopperBlock> FILTER_HOPPER = BLOCKS.registerBlock("filter_hopper",
            com.snolred.wildplus.block.FilterHopperBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER));

    public static void init() {}

    private ModBlocks2() {}
}
