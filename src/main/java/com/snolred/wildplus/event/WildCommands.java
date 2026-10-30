package com.snolred.wildplus.event;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** 테스트용 명령어: /wildplus bloodmoon start|stop (관리자 전용) */
public final class WildCommands {
    private WildCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wildplus")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bloodmoon")
                        .then(Commands.literal("start").executes(ctx -> {
                            BloodMoon.start(ctx.getSource().getServer().overworld());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.wildplus.blood_moon.started"), true);
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            BloodMoon.stop(ctx.getSource().getServer().overworld(), false);
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.wildplus.blood_moon.stopped"), true);
                            return 1;
                        })))
                .then(Commands.literal("stocks").then(Commands.literal("refresh").executes(ctx -> {
                    com.snolred.wildplus.economy.StockMarket.refresh(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("주식 시세를 새로 받아오는 중…"), true);
                    return 1;
                })))
                .then(Commands.literal("interest").executes(ctx -> {
                    EconomyTick.payInterest(ctx.getSource().getServer(),
                            com.snolred.wildplus.economy.WildData.get(ctx.getSource().getServer()));
                    ctx.getSource().sendSuccess(() -> Component.literal("이자 지급 완료"), true);
                    return 1;
                }))
                .then(Commands.literal("guide").executes(ctx -> {
                    ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Guidebook.create());
                    return 1;
                }))
                .then(Commands.literal("cookbook").executes(ctx -> {
                    ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Cookbook.create());
                    return 1;
                })));
    }
}
