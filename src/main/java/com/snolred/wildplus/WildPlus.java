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
        com.snolred.wildplus.registry.ModItems13.init();
        com.snolred.wildplus.registry.ModBlocks3.init();
        com.snolred.wildplus.registry.ModBlocks4.init();
        com.snolred.wildplus.registry.ModItems14.init();
        com.snolred.wildplus.registry.ModBlocks5.init();
        com.snolred.wildplus.registry.ModItems15.init();
        com.snolred.wildplus.registry.ModBlocks6.init();
        com.snolred.wildplus.registry.ModItems16.init();
        com.snolred.wildplus.registry.ModItems19.init();
        com.snolred.wildplus.registry.ModBlocks7.init();
        com.snolred.wildplus.registry.ModItems17.init();
        com.snolred.wildplus.registry.ModBlocks8.init();
        com.snolred.wildplus.registry.ModItems18.init();
        com.snolred.wildplus.registry.ModBlocks9.init();
        com.snolred.wildplus.registry.ModBlocks10.init();
        com.snolred.wildplus.registry.ModItems20.init();
        com.snolred.wildplus.registry.ModItems21.init();
        com.snolred.wildplus.registry.ModBlocks11.init();
        com.snolred.wildplus.registry.ModItems22.init();
        com.snolred.wildplus.registry.ModBlocks12.init();
        com.snolred.wildplus.registry.ModItems23.init();
        com.snolred.wildplus.registry.ModBlocks13.init();
        com.snolred.wildplus.registry.ModItems24.init();
        com.snolred.wildplus.registry.ModBlocks14.init();
        com.snolred.wildplus.registry.ModItems25.init();
        com.snolred.wildplus.registry.ModBlocks15.init();
        com.snolred.wildplus.registry.ModItems26.init();
        com.snolred.wildplus.registry.ModBlocks16.init();
        com.snolred.wildplus.registry.ModItems27.init();
        com.snolred.wildplus.registry.ModBlocks17.init();
        com.snolred.wildplus.registry.ModItems28.init();
        com.snolred.wildplus.registry.ModBlocks18.init();
        com.snolred.wildplus.registry.ModItems29.init();
        com.snolred.wildplus.registry.ModItems30.init();
        com.snolred.wildplus.registry.ModBlocks19.init();
        com.snolred.wildplus.registry.ModItems31.init();
        com.snolred.wildplus.registry.ModItems32.init();
        com.snolred.wildplus.registry.ModBlocks20.init();
        com.snolred.wildplus.registry.ModItems33.init();
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        com.snolred.wildplus.registry.ModEffects.EFFECTS.register(modEventBus);
        com.snolred.wildplus.registry.ModEffects.POTIONS.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
        modEventBus.addListener(com.snolred.wildplus.net.BankSync::register);
        modEventBus.addListener(com.snolred.wildplus.net.CosmeticSync::register);
        modEventBus.addListener(com.snolred.wildplus.net.DoubleJump::register);
        modEventBus.addListener(com.snolred.wildplus.net.MusicNet::register);
        modEventBus.addListener(com.snolred.wildplus.net.MapNet::register);
        // 한 묶음 늘리기: 원래 64개씩 쌓이는 아이템을 99개까지 (마인크래프트가 허용하는 최대치)
        modEventBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.event.ModifyDefaultComponentsEvent e) -> e.modifyMatching(
                        (item, comps) -> comps.getOrDefault(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1) == 64,
                        (comps, ctx, item) -> comps.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 99)));
        modEventBus.addListener(ModEntities::onSpawnPlacements);
        ModItems.ITEMS.register(modEventBus);
        ModLoot.MODIFIERS.register(modEventBus);
        com.snolred.wildplus.registry.ModFeatures.TYPES.register(modEventBus);
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
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.FrostBurst.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.VoidGear.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.AwakenedDragon.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.VoidRuin.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.VoidBiome.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.Engraves.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EndCity.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EndMobEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Lotto.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.music.Music.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Market.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Codex.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Seasons.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.BulkShip.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.net.MapNet.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.PotionEvents41.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.EmeraldGear.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Soil.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.block.OreMinerBlockEntity.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.FishingPlus.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Jobs.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Trials.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.net.DoubleJump.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.cooking.Cooking.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.BossSkills.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Land.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Arena.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.WealthTax.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.net.BankSync.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.net.CosmeticSync.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.economy.Luxury.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.ContentEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Contest.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Ranch.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.home.HouseVisit.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Derby.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Village.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Raid.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Tower.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Social.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.content.Mail.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.Bows.class);
        // 3.8 흉조
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.OmenRaids.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.OmenEvents.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.Prisoners.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.CounterRaid.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.Smugglers.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.omen.OmenAltar.class);
        NeoForge.EVENT_BUS.register(com.snolred.wildplus.event.LuckyCharms.class);

        modContainer.registerConfig(ModConfig.Type.SYNCED, WildConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
