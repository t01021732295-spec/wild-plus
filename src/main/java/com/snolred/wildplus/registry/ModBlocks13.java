package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 4.1 큰 ATM(1×1×2) · 약국 매대(3×1×3). 예전 1칸짜리는 설치된 그대로 작동 */
public final class ModBlocks13 {
    private ModBlocks13() {}

    public static final DeferredBlock<BigBlock> ATM_TALL = BLOCKS.registerBlock("atm_tall",
            p -> new BigBlock(p, com.snolred.wildplus.menu.BankMenu::open, 0, 0, 0, 0, 2, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 1200.0F).lightLevel(s -> 5).noOcclusion());
    public static final DeferredBlock<BigBlock> PHARMACY_RACK = BLOCKS.registerBlock("pharmacy_rack",
            p -> new BigBlock(p, com.snolred.wildplus.economy.Services::openPharmacy, -1, 1, 0, 0, 3, null),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static void init() {}
}
