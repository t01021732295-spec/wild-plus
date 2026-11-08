package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.LandPostBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 3.9 땅 경계 기둥 (땅을 사면 모서리에 저절로 섬 · 아이템 없음) */
public final class ModBlocks10 {
    private ModBlocks10() {}

    public static final DeferredBlock<LandPostBlock> LAND_POST = BLOCKS.registerBlock("land_post", LandPostBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE).sound(SoundType.WOOD).noOcclusion().noLootTable()
                    .strength(2.0F, 6.0F).lightLevel(s -> 12));

    public static void init() {}
}
