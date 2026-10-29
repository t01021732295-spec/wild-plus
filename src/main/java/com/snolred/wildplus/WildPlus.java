package com.snolred.wildplus;

import com.mojang.logging.LogUtils;
import com.snolred.wildplus.event.BloodMoon;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.event.GearEffects;
import com.snolred.wildplus.event.WildCommands;
import com.snolred.wildplus.registry.ModBlocks;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModBlockEntities;
import com.snolred.wildplus.event.EconomyEvents;
import com.snolred.wildplus.registry.ModLoot;
import com.snolred.wildplus.registry.ModTabs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(WildPlus.MODID)
public class WildPlus {
    public static final String MODID = "wildplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildPlus(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems2.init();
        ModItems3.init();
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
        modEventBus.addListener(ModEntities::onSpawnPlacements);
        ModItems.ITEMS.register(modEventBus);
        ModLoot.MODIFIERS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(BloodMoon.class);
        NeoForge.EVENT_BUS.register(EliteMobs.class);
        NeoForge.EVENT_BUS.register(GearEffects.class);
        NeoForge.EVENT_BUS.register(WildCommands.class);
        NeoForge.EVENT_BUS.register(EconomyEvents.class);

        modContainer.registerConfig(ModConfig.Type.SYNCED, WildConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
