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
import com.snolred.wildplus.event.QolEvents;
import com.snolred.wildplus.registry.ModMenus;
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
        com.snolred.wildplus.registry.ModItems4.init();
        com.snolred.wildplus.registry.ModItems5.init();
        com.snolred.wildplus.registry.ModItems6.init();
        com.snolred.wildplus.registry.ModItems7.init();
        com.snolred.wildplus.registry.ModItems8.init();
        com.snolred.wildplus.registry.ModItems9.init();
        com.snolred.wildplus.registry.ModItems10.init();
        com.snolred.wildplus.registry.ModItems11.init();
        com.snolred.wildplus.registry.ModItems12.init();
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        com.snolred.wildplus.registry.ModEffects.EFFECTS.register(modEventBus);
        com.snolred.wildplus.registry.ModEffects.POTIONS.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
        modEventBus.addListener(com.snolred.wildplus.net.BankSync::register);
        // 한 묶음 늘리기: 원래 64개씩 쌓이는 아이템을 99개까지 (마인크래프트가 허용하는 최대치)
        modEventBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.event.ModifyDefaultComponentsEvent e) -> e.modifyMatching(
                        (item, comps) -> comps.getOrDefault(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1) == 64,
                        (comps, ctx, item) -> comps.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 99)));
        modEventBus.addListener(ModEntities::onSpawnPlacements);
        ModItems.ITEMS.register(modEventBus);
        ModLoot.MODIFIERS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(BloodMoon.class);
        NeoForge.EVENT_BUS.register(EliteMobs.class);
        NeoForge.EVENT_BUS.register(GearEffects.class);
        NeoForge.EVENT_BUS.register(WildCommands.class);
        NeoForge.EVENT_BUS.register(EconomyEvents.class);
        NeoForge.EVENT_BUS.register(QolEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EconomyTick.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.BloodMoonReward.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.BloodLord.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.RandomEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.VillageLife.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.DungeonGuardian.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EffectEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Honor.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.RubyGear.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.SapphireGear.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EmeraldGear.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.BossSkills.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Land.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.WealthTax.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.net.BankSync.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Luxury.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.ContentEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Raid.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Tower.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Social.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Mail.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.Bows.class);

        modContainer.registerConfig(ModConfig.Type.SYNCED, WildConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
