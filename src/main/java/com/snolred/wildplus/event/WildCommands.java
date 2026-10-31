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
        // 누구나 쓰는 명령어: 안내서·요리책 받기, 워프 이름 바꾸기
        event.getDispatcher().register(Commands.literal("guide").executes(ctx -> {
            ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Guidebook.create());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.wildplus.guide"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("cookbook").executes(ctx -> {
            ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Cookbook.create());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.wildplus.cookbook"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("warpname")
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx -> {
                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name");
                    net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    if (com.snolred.wildplus.economy.Warps.rename(sp, name)) {
                        sp.closeContainer();
                        ctx.getSource().sendSuccess(() -> Component.translatable("commands.wildplus.warp_renamed", name), false);
                        return 1;
                    }
                    ctx.getSource().sendFailure(Component.translatable("commands.wildplus.warp_rename_fail"));
                    return 0;
                })));
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
                .then(Commands.literal("treasury").executes(ctx -> {
                    long t = com.snolred.wildplus.economy.WildData.get(ctx.getSource().getServer()).treasury;
                    ctx.getSource().sendSuccess(() -> Component.literal("TREASURY " + t), false);
                    return 1;
                }))
                .then(Commands.literal("guide").executes(ctx -> {
                    ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Guidebook.create());
                    return 1;
                }))
                .then(Commands.literal("cookbook").executes(ctx -> {
                    ctx.getSource().getPlayerOrException().getInventory().add(com.snolred.wildplus.economy.Cookbook.create());
                    return 1;
                }))
                // 시험용: 핏빛 군주 · 정예 몹 소환
                .then(Commands.literal("bloodlord").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    BloodLord.spawn((net.minecraft.server.level.ServerLevel) p.level(), p);
                    return 1;
                }))
                .then(Commands.literal("elite").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    var level = (net.minecraft.server.level.ServerLevel) p.level();
                    var z = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if (z == null) return 0;
                    z.snapTo(p.getX() + 3, p.getY(), p.getZ(), 0F, 0F);
                    EliteMobs.makeElite(z, 2);
                    level.addFreshEntity(z);
                    ctx.getSource().sendSuccess(() -> Component.literal("ELITE " + z.getPersistentData().getStringOr(EliteMobs.AFFIX_TAG, "")), false);
                    return 1;
                })));
    }
}
