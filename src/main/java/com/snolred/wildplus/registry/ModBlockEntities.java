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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.SortingChestBlockEntity>> SORTING_CHEST =
            BLOCK_ENTITIES.register("sorting_chest", () -> new BlockEntityType<>(com.snolred.wildplus.block.SortingChestBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks2.SORTING_CHEST.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.SpreaderBlockEntity>> SPREADER =
            BLOCK_ENTITIES.register("fertilizer_spreader", () -> new BlockEntityType<>(com.snolred.wildplus.block.SpreaderBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks3.SPREADER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.FeederBlockEntity>> FEEDER =
            BLOCK_ENTITIES.register("feeder", () -> new BlockEntityType<>(com.snolred.wildplus.block.FeederBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks2.FEEDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.FilterHopperBlockEntity>> FILTER_HOPPER =
            BLOCK_ENTITIES.register("filter_hopper", () -> new BlockEntityType<>(com.snolred.wildplus.block.FilterHopperBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks2.FILTER_HOPPER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.AutoFisherBlockEntity>> AUTO_FISHER =
            BLOCK_ENTITIES.register("auto_fisher", () -> new BlockEntityType<>(com.snolred.wildplus.block.AutoFisherBlockEntity::new,
                    ModBlocks.AUTO_FISHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITIES.register("trash_can", () -> new BlockEntityType<>(com.snolred.wildplus.block.TrashCanBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks2.TRASH_CAN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.CookingStationBlockEntity>> COOKING =
            BLOCK_ENTITIES.register("cooking_station", () -> new BlockEntityType<>(com.snolred.wildplus.block.CookingStationBlockEntity::new,
                    ModBlocks4.COOKING_POT.get(), ModBlocks4.BRICK_OVEN.get(), ModBlocks4.BBQ_GRILL.get(), ModBlocks4.PREP_TABLE.get(),
                    ModBlocks4.KITCHEN_STOVE.get(), ModBlocks4.AGING_RACK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.RestaurantCounterBlockEntity>> RESTAURANT =
            BLOCK_ENTITIES.register("restaurant_counter", () -> new BlockEntityType<>(com.snolred.wildplus.block.RestaurantCounterBlockEntity::new,
                    ModBlocks5.RESTAURANT_COUNTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.FurnitureBlockEntity>> FURNITURE =
            BLOCK_ENTITIES.register("furniture", () -> new BlockEntityType<>(com.snolred.wildplus.block.FurnitureBlockEntity::new,
                    ModBlocks6.FURNITURE.get("wardrobe").get(), ModBlocks6.FURNITURE.get("fridge").get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.snolred.wildplus.block.AutoCrafterBlockEntity>> AUTO_CRAFTER =
            BLOCK_ENTITIES.register("auto_crafter", () -> new BlockEntityType<>(com.snolred.wildplus.block.AutoCrafterBlockEntity::new,
                    com.snolred.wildplus.registry.ModBlocks2.AUTO_CRAFTER.get()));

    private ModBlockEntities() {}
}
