package com.snolred.wildplus;

import com.mojang.logging.LogUtils;
import com.snolred.wildplus.event.BloodMoon;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.event.GearEffects;
import com.snolred.wildplus.registry.ModBlocks;
import com.snolred.wildplus.registry.ModItems;
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
        ModItems.ITEMS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(BloodMoon.class);
        NeoForge.EVENT_BUS.register(EliteMobs.class);
        NeoForge.EVENT_BUS.register(GearEffects.class);

        modContainer.registerConfig(ModConfig.Type.SERVER, WildConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
