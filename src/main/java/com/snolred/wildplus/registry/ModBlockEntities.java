package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.block.VendingMachineBlockEntity;
import com.snolred.wildplus.block.GraveBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WildPlus.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE =
            BLOCK_ENTITIES.register("vending_machine",
                    () -> new BlockEntityType<>(VendingMachineBlockEntity::new, ModBlocks.VENDING_MACHINE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE =
            BLOCK_ENTITIES.register("grave", () -> new BlockEntityType<>(GraveBlockEntity::new, ModBlocks.GRAVE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.WarpPointBlockEntity>> WARP_POINT =
            BLOCK_ENTITIES.register("warp_point", () -> new BlockEntityType<>(com.snolred.wildplus.block.WarpPointBlockEntity::new,
                    ModBlocks.WARP_POINT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.ShippingBinBlockEntity>> SHIPPING_BIN =
            BLOCK_ENTITIES.register("shipping_bin", () -> new BlockEntityType<>(com.snolred.wildplus.block.ShippingBinBlockEntity::new,
                    ModBlocks.SHIPPING_BIN.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.ChunkLoaderBlockEntity>> CHUNK_LOADER =
            BLOCK_ENTITIES.register("chunk_loader", () -> new BlockEntityType<>(com.snolred.wildplus.block.ChunkLoaderBlockEntity::new,
                    ModBlocks.CHUNK_LOADER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.AutoHarvesterBlockEntity>> AUTO_HARVESTER =
            BLOCK_ENTITIES.register("auto_harvester", () -> new BlockEntityType<>(com.snolred.wildplus.block.AutoHarvesterBlockEntity::new,
                    ModBlocks.AUTO_HARVESTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.OreMinerBlockEntity>> ORE_MINER =
            BLOCK_ENTITIES.register("ore_miner", () -> new BlockEntityType<>(com.snolred.wildplus.block.OreMinerBlockEntity::new,
                    ModBlocks.ORE_MINER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.AutoFisherBlockEntity>> AUTO_FISHER =
            BLOCK_ENTITIES.register("auto_fisher", () -> new BlockEntityType<>(com.snolred.wildplus.block.AutoFisherBlockEntity::new,
                    ModBlocks.AUTO_FISHER.get()));

    private ModBlockEntities() {}
}
