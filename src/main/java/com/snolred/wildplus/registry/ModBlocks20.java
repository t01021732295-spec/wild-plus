package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.LifeBlock;
import com.snolred.wildplus.block.SeasonCropBlock;
import com.snolred.wildplus.content.LifeStations;
import com.snolred.wildplus.content.Seasons;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 5.0 생활: 계절 작물 4종 · 양봉 상자 · 건조대 · 장독대 · 양어장(2칸) */
public final class ModBlocks20 {
    private ModBlocks20() {}

    public static final DeferredBlock<SeasonCropBlock> SOYBEAN_CROP = BLOCKS.registerBlock("soybean_crop",
            p -> new SeasonCropBlock(p, () -> ModItems33.SOYBEAN_SEEDS.get(), Seasons.SPRING), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SeasonCropBlock> KOREAN_MELON_CROP = BLOCKS.registerBlock("korean_melon_crop",
            p -> new SeasonCropBlock(p, () -> ModItems33.KOREAN_MELON_SEEDS.get(), Seasons.SUMMER), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SeasonCropBlock> SWEET_POTATO_CROP = BLOCKS.registerBlock("sweet_potato_crop",
            p -> new SeasonCropBlock(p, () -> ModItems33.SWEET_POTATO_SEEDS.get(), Seasons.AUTUMN), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SeasonCropBlock> RADISH_CROP = BLOCKS.registerBlock("radish_crop",
            p -> new SeasonCropBlock(p, () -> ModItems33.RADISH_SEEDS.get(), Seasons.WINTER), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    public static final DeferredBlock<LifeBlock> BEEHIVE_BOX = BLOCKS.registerBlock("beehive_box",
            p -> new LifeBlock(p, LifeStations.Kind.BEEHIVE, 0, 0), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());
    public static final DeferredBlock<LifeBlock> DRYING_RACK = BLOCKS.registerBlock("drying_rack",
            p -> new LifeBlock(p, LifeStations.Kind.DRYING, 0, 0), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());
    public static final DeferredBlock<LifeBlock> JANGDOK = BLOCKS.registerBlock("jangdok",
            p -> new LifeBlock(p, LifeStations.Kind.JANGDOK, 0, 0), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).noOcclusion());
    public static final DeferredBlock<LifeBlock> FISH_FARM = BLOCKS.registerBlock("fish_farm",
            p -> new LifeBlock(p, LifeStations.Kind.FISHFARM, 0, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static void init() {}
}
