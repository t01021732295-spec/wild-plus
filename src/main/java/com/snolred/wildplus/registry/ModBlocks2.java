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

    // ───────── 17차 ─────────
    public static final DeferredBlock<com.snolred.wildplus.block.BoxBlock> SORTING_CHEST = BLOCKS.registerBlock("sorting_chest",
            p -> new com.snolred.wildplus.block.BoxBlock(p, com.snolred.wildplus.block.BoxBlock.Kind.SORTING),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL));
    public static final DeferredBlock<com.snolred.wildplus.block.BoxBlock> FEEDER = BLOCKS.registerBlock("feeder",
            p -> new com.snolred.wildplus.block.BoxBlock(p, com.snolred.wildplus.block.BoxBlock.Kind.FEEDER),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion());
    public static final DeferredBlock<com.snolred.wildplus.block.MailboxBlock> MAILBOX = BLOCKS.registerBlock("mailbox",
            com.snolred.wildplus.block.MailboxBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0F));

    // ───────── 2.9: 사파이어 (높은 산에서 나는 푸른 보석) ─────────
    public static final DeferredBlock<DropExperienceBlock> SAPPHIRE_ORE = BLOCKS.registerBlock("sapphire_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_SAPPHIRE_ORE = BLOCKS.registerBlock("deepslate_sapphire_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE));
    public static final DeferredBlock<Block> SAPPHIRE_BLOCK = BLOCKS.registerBlock("sapphire_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));

    // ───────── 2.11: 명품관 · 부동산 중개소 ─────────
    public static final DeferredBlock<com.snolred.wildplus.block.ServiceBlock> LUXURY_SHOP = BLOCKS.registerBlock("luxury_shop",
            p -> new com.snolred.wildplus.block.ServiceBlock(p, com.snolred.wildplus.economy.Luxury::open),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).strength(3.0F, 1200.0F).lightLevel(s -> 8));
    public static final DeferredBlock<com.snolred.wildplus.block.ServiceBlock> REALTY = BLOCKS.registerBlock("realty",
            p -> new com.snolred.wildplus.block.ServiceBlock(p, com.snolred.wildplus.content.Land::open),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5F, 1200.0F));

    // ───────── 2.12: 투기장 접수처 ─────────
    public static final DeferredBlock<com.snolred.wildplus.block.ServiceBlock> ARENA_DESK = BLOCKS.registerBlock("arena_desk",
            p -> new com.snolred.wildplus.block.ServiceBlock(p, com.snolred.wildplus.content.Arena::open),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BRICKS).strength(3.0F, 1200.0F).lightLevel(s -> 6));

    // ───────── 2.8 ─────────
    public static final DeferredBlock<com.snolred.wildplus.block.TrashCanBlock> TRASH_CAN = BLOCKS.registerBlock("trash_can",
            com.snolred.wildplus.block.TrashCanBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0F));
    public static final DeferredBlock<com.snolred.wildplus.block.AutoCrafterBlock> AUTO_CRAFTER = BLOCKS.registerBlock("auto_crafter",
            com.snolred.wildplus.block.AutoCrafterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTER));

    public static void init() {}

    private ModBlocks2() {}
}
