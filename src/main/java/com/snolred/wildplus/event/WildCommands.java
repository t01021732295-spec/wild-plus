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
                // 랜덤 이벤트: /wildplus event <이름> · /wildplus event stop
                .then(eventCommand())
                .then(weekendCommand())
                .then(Commands.literal("cosmetic").then(Commands.argument("w", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                        .then(Commands.argument("h", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 2))
                        .then(Commands.argument("o", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4)).executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            var d = com.snolred.wildplus.content.ContentData.get(p.level().getServer()).player("luxury", p.getUUID());
                            d.putInt("wings", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "w"));
                            d.putInt("halo", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "h"));
                            d.putInt("outfit", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "o"));
                            com.snolred.wildplus.content.ContentData.get(p.level().getServer()).dirty();
                            com.snolred.wildplus.net.CosmeticSync.broadcast(p.level().getServer());
                            return 1;
                        })))))
                .then(Commands.literal("arenamap").then(Commands.argument("t", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 9)).executes(ctx -> {
                    com.snolred.wildplus.content.Arena.debugBuild(ctx.getSource().getPlayerOrException(),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "t"));
                    return 1;
                })))
                .then(Commands.literal("enchtest").executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    var reg = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                    for (var e : com.snolred.wildplus.registry.ModItems.ITEMS.getEntries()) {
                        var stack = new net.minecraft.world.item.ItemStack(e.get());
                        if (!stack.isDamageableItem() && !(stack.getItem() instanceof net.minecraft.world.item.BowItem)) continue;
                        int n = 0;
                        try {
                            n = net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(30, stack,
                                    reg.listElements().map(h -> (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>) h)).size();
                        } catch (Throwable t) {
                            n = -1;
                        }
                        String line = "ENCH " + e.getId().getPath() + " enchantable=" + stack.isEnchantable()
                                + " comp=" + stack.get(net.minecraft.core.component.DataComponents.ENCHANTABLE) + " options=" + n;
                        com.snolred.wildplus.WildPlus.LOGGER.info(line);
                    }
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

    /** 주말 콘텐츠 시험용: /wildplus weekend raid|tower|leave|goal <종류> <수>|points <수> */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> weekendCommand() {
        var root = Commands.literal("weekend");
        root.then(Commands.literal("raid").executes(ctx -> {
            var sp = ctx.getSource().getPlayerOrException();
            com.snolred.wildplus.content.Raid.start(sp, sp.blockPosition());
            return 1;
        }));
        root.then(Commands.literal("tower").executes(ctx -> {
            var sp = ctx.getSource().getPlayerOrException();
            com.snolred.wildplus.content.Tower.start(sp, sp.blockPosition());
            return 1;
        }));
        root.then(Commands.literal("leave").executes(ctx -> {
            var sp = ctx.getSource().getPlayerOrException();
            com.snolred.wildplus.content.Tower.eliminate(ctx.getSource().getServer(), sp.getUUID(), sp, "명령어");
            return 1;
        }));
        root.then(Commands.literal("points").then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    com.snolred.wildplus.content.WeeklyGoals.addPoints(ctx.getSource().getPlayerOrException(),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "n"));
                    return 1;
                })));
        var goal = Commands.literal("goal");
        for (var k : com.snolred.wildplus.content.WeeklyGoals.Kind.values()) {
            goal.then(Commands.literal(k.name().toLowerCase(java.util.Locale.ROOT))
                    .then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(ctx -> {
                        com.snolred.wildplus.content.WeeklyGoals.add(ctx.getSource().getServer(), k,
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "n"));
                        ctx.getSource().sendSuccess(() -> Component.literal("GOAL " + k.name()), false);
                        return 1;
                    })));
        }
        root.then(goal);
        return root;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> eventCommand() {
        var root = Commands.literal("event");
        root.then(Commands.literal("stop").executes(ctx -> {
            RandomEvents.stop(ctx.getSource().getServer());
            return 1;
        }));
        for (RandomEvents.Type t : RandomEvents.Type.values()) {
            root.then(Commands.literal(t.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx -> {
                RandomEvents.start(ctx.getSource().getServer(), t);
                ctx.getSource().sendSuccess(() -> Component.literal("EVENT " + t.name()), false);
                return 1;
            }));
        }
        return root;
    }
}
