package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import com.snolred.wildplus.block.ChairBlock;
import com.snolred.wildplus.block.RestaurantCounterBlock;
import com.snolred.wildplus.block.RestaurantEntranceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 식당 (요리 3차): 카운터 · 입구 · 식탁 · 의자 · 간판 */
public final class ModBlocks5 {
    public static final DeferredBlock<RestaurantCounterBlock> RESTAURANT_COUNTER = BLOCKS.registerBlock("restaurant_counter",
            RestaurantCounterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(2.5F, 6.0F).lightLevel(s -> 6));
    public static final DeferredBlock<RestaurantEntranceBlock> RESTAURANT_ENTRANCE = BLOCKS.registerBlock("restaurant_entrance",
            RestaurantEntranceBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(0.8F).lightLevel(s -> 10));
    public static final DeferredBlock<BigBlock> DINING_TABLE = BLOCKS.registerBlock("dining_table",
            p -> new BigBlock(p, null, 0, 1, 0, 0, 1, null), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).noOcclusion().strength(2.0F));
    public static final DeferredBlock<ChairBlock> DINING_CHAIR = BLOCKS.registerBlock("dining_chair",
            ChairBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.5F));
    public static final DeferredBlock<BigBlock> RESTAURANT_SIGN = BLOCKS.registerBlock("restaurant_sign",
            p -> new BigBlock(p, null, 0, 1, 0, 0, 2, null), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion().strength(1.5F).lightLevel(s -> 8));

    public static void init() {}

    private ModBlocks5() {}
}
