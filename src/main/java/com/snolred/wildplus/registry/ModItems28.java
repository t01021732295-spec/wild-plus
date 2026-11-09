package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems28 {
    public static final DeferredItem<BlockItem> LOTTERY_MACHINE = ITEMS.registerSimpleBlockItem("lottery_machine", ModBlocks17.LOTTERY_MACHINE);
    public static final DeferredItem<BlockItem> FATE_ALTAR = ITEMS.registerSimpleBlockItem("fate_altar", ModBlocks17.FATE_ALTAR);

    public static void init() {}

    private ModItems28() {}
}
