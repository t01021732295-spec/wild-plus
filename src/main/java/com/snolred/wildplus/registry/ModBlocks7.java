package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import com.snolred.wildplus.block.GuestbookBlock;
import com.snolred.wildplus.block.PlaqueBlock;
import com.snolred.wildplus.block.RanchBlock;
import com.snolred.wildplus.block.ServiceBlock;
import com.snolred.wildplus.block.TrophyBlock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 3.4 블록: 목장 기계 5 · 가축 상점 · 방명록 · 문패 · 대회 게시판 · 시상대 · 트로피 3 */
public final class ModBlocks7 {
    public static final Map<RanchBlock.Kind, DeferredBlock<RanchBlock>> RANCH = new LinkedHashMap<>();

    static {
        for (RanchBlock.Kind k : RanchBlock.Kind.values()) {
            RANCH.put(k, BLOCKS.registerBlock(k.name().toLowerCase(Locale.ROOT), p -> new RanchBlock(p, k),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.5F, 3.0F)));
        }
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.5F, 3.0F);
    }

    public static final DeferredBlock<ServiceBlock> RANCH_MARKET = BLOCKS.registerBlock("ranch_market",
            p -> new ServiceBlock(p, com.snolred.wildplus.home.RanchShop::open), () -> wood().strength(2.0F, 6.0F).lightLevel(s -> 6));
    public static final DeferredBlock<GuestbookBlock> GUESTBOOK = BLOCKS.registerBlock("guestbook", GuestbookBlock::new, ModBlocks7::wood);
    public static final DeferredBlock<PlaqueBlock> HOUSE_PLAQUE = BLOCKS.registerBlock("house_plaque", PlaqueBlock::new, ModBlocks7::wood);
    public static final DeferredBlock<BigBlock> CONTEST_BOARD = BLOCKS.registerBlock("contest_board",
            p -> new BigBlock(p, com.snolred.wildplus.content.Contest::openBoard, -1, 0, 0, 0, 2, null), () -> wood().lightLevel(s -> 5));
    public static final DeferredBlock<BigBlock> PODIUM = BLOCKS.registerBlock("podium",
            p -> new BigBlock(p, com.snolred.wildplus.content.Contest::podium, -1, 1, 0, 0, 1, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion().strength(2.0F, 6.0F));
    public static final DeferredBlock<TrophyBlock> TROPHY_GOLD = trophy("trophy_gold", 1);
    public static final DeferredBlock<TrophyBlock> TROPHY_SILVER = trophy("trophy_silver", 2);
    public static final DeferredBlock<TrophyBlock> TROPHY_BRONZE = trophy("trophy_bronze", 3);

    private static DeferredBlock<TrophyBlock> trophy(String name, int place) {
        return BLOCKS.registerBlock(name, p -> new TrophyBlock(p, place),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).sound(SoundType.METAL).noOcclusion().strength(1.0F, 6.0F).lightLevel(s -> 4 - place));
    }

    public static void init() {}

    private ModBlocks7() {}
}
