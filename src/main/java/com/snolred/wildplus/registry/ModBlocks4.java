package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.CookingStationBlock;
import com.snolred.wildplus.cooking.CookingData.Station;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 요리 2차: 조리 기구 6종 (여러 칸 큰 블록) */
public final class ModBlocks4 {
    private static DeferredBlock<CookingStationBlock> station(String name, Station st, int lat1, int dep1, BlockBehaviour.Properties base) {
        return BLOCKS.registerBlock(name, p -> new CookingStationBlock(p, st, 0, lat1, 0, dep1, 2), () -> base.noOcclusion().strength(2.5F, 6.0F)
                .lightLevel(s -> s.hasProperty(com.snolred.wildplus.block.CookingStationBlock.LIT) && s.getValue(com.snolred.wildplus.block.CookingStationBlock.LIT) ? 9 : 0));
    }

    public static final DeferredBlock<CookingStationBlock> COOKING_POT = station("cooking_pot", Station.POT, 1, 0,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).lightLevel(s -> 9));
    public static final DeferredBlock<CookingStationBlock> BRICK_OVEN = station("brick_oven", Station.OVEN, 1, 1,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).lightLevel(s -> 11));
    public static final DeferredBlock<CookingStationBlock> BBQ_GRILL = station("bbq_grill", Station.GRILL, 1, 0,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.METAL).lightLevel(s -> 8));
    public static final DeferredBlock<CookingStationBlock> PREP_TABLE = station("prep_table", Station.PREP, 1, 0,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final DeferredBlock<CookingStationBlock> KITCHEN_STOVE = station("kitchen_stove", Station.STOVE, 0, 0,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.METAL));
    public static final DeferredBlock<CookingStationBlock> AGING_RACK = station("aging_rack", Station.AGING, 1, 0,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS));

    public static void init() {}

    private ModBlocks4() {}
}
