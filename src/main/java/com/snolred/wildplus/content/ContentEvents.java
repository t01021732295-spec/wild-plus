package com.snolred.wildplus.content;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** 주간 목표 진행도 수집 */
public final class ContentEvents {
    private ContentEvents() {}

    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            ContentData.updateDay(event.getServer());
            Contest.tick(event.getServer());
            com.snolred.wildplus.home.HouseVisit.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && !event.getEntity().level().isClientSide()) {
            WeeklyGoals.onKill(p, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p) {
            WeeklyGoals.onBreak(p, event.getState());
            if (event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
                boolean rare = event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES_DIAMOND)
                        || event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES_EMERALD)
                        || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).getNamespace().equals("wildplus");
                Contest.add(p, Contest.Kind.MINING, rare ? 3 : 1);
            }
        }
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (event.getHookEntity().getPlayerOwner() instanceof ServerPlayer p) {
            WeeklyGoals.add(p.level().getServer(), WeeklyGoals.Kind.FISH, 1);
            int pts = 0;
            for (net.minecraft.world.item.ItemStack s : event.getDrops()) {
                if (com.snolred.wildplus.economy.Grade.isFish(s)) pts += 1 + com.snolred.wildplus.economy.Grade.of(s);
            }
            Contest.add(p, Contest.Kind.FISHING, pts);
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) WeeklyGoals.add(p.level().getServer(), WeeklyGoals.Kind.TRADE, 1);
    }
}
