package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.block.ChiliCropBlock;
import com.snolred.wildplus.block.SimpleCropBlock;
import com.snolred.wildplus.block.SprinklerBlock;
import com.snolred.wildplus.block.VendingMachineBlock;
import com.snolred.wildplus.block.GachaMachineBlock;
import com.snolred.wildplus.block.GraveBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WildPlus.MODID);

    public static final DeferredBlock<ChiliCropBlock> CHILI_CROP = BLOCKS.registerBlock(
            "chili_crop", ChiliCropBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    // ───────── 광석 ─────────
    public static final DeferredBlock<DropExperienceBlock> MYTHRIL_ORE = BLOCKS.registerBlock("mythril_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_MYTHRIL_ORE = BLOCKS.registerBlock("deepslate_mythril_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));
    public static final DeferredBlock<DropExperienceBlock> JADE_ORE = BLOCKS.registerBlock("jade_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_JADE_ORE = BLOCKS.registerBlock("deepslate_jade_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));
    public static final DeferredBlock<Block> MYTHRIL_BLOCK = BLOCKS.registerBlock("mythril_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> JADE_BLOCK = BLOCKS.registerBlock("jade_block",
            Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

    // ───────── 농사 ─────────
    public static final DeferredBlock<SimpleCropBlock> TOMATO_CROP = BLOCKS.registerBlock("tomato_crop",
            p -> new SimpleCropBlock(p, () -> ModItems2.TOMATO_SEEDS.get()), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SimpleCropBlock> STRAWBERRY_CROP = BLOCKS.registerBlock("strawberry_crop",
            p -> new SimpleCropBlock(p, () -> ModItems2.STRAWBERRY.get()), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    public static final DeferredBlock<SprinklerBlock> SPRINKLER = BLOCKS.registerBlock("sprinkler",
            SprinklerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion());

    // ───────── 거래 ─────────
    public static final DeferredBlock<VendingMachineBlock> VENDING_MACHINE = BLOCKS.registerBlock("vending_machine",
            VendingMachineBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 1200.0F).noOcclusion());

    public static final DeferredBlock<GachaMachineBlock> GACHA_MACHINE = BLOCKS.registerBlock("gacha_machine",
            GachaMachineBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 1200.0F));

    // ───────── 편의 ─────────
    public static final DeferredBlock<GraveBlock> GRAVE = BLOCKS.registerBlock("grave",
            GraveBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(2.0F, 3600000.0F).noOcclusion().noLootTable());

    private ModBlocks() {}
}
