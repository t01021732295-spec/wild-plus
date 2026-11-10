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
        // 식당: 이름 바꾸기 · 평점 (카운터 화면의 입력칸이 이 명령을 보냄)
        event.getDispatcher().register(Commands.literal("restaurant")
                .then(Commands.literal("name").then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    if (!(sp.containerMenu instanceof com.snolred.wildplus.menu.RestaurantMenu m) || m.blockEntity() == null
                            || m.blockEntity().role(sp) != com.snolred.wildplus.block.RestaurantCounterBlockEntity.ROLE_OWNER) {
                        ctx.getSource().sendFailure(Component.literal("자기 식당 카운터 화면에서만 이름을 바꿀 수 있어요"));
                        return 0;
                    }
                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name").trim();
                    if (name.length() > 20) name = name.substring(0, 20);
                    var be = m.blockEntity();
                    be.name = name;
                    be.setChanged();
                    final String n = name;
                    ctx.getSource().sendSuccess(() -> Component.literal("🍽 식당 이름: " + n), false);
                    com.snolred.wildplus.menu.RestaurantMenu.open(sp, be.getBlockPos());
                    return 1;
                })))
                .then(Commands.literal("reply").then(Commands.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 20))
                        .then(Commands.argument("text", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            if (!(sp.containerMenu instanceof com.snolred.wildplus.menu.RestaurantMenu m) || m.blockEntity() == null
                                    || !m.blockEntity().reply(sp, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "index"),
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "text"))) {
                                ctx.getSource().sendFailure(Component.literal("자기 식당 카운터 화면에서만 답글을 달 수 있어요"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("💬 답글을 달았어요"), false);
                            com.snolred.wildplus.menu.RestaurantMenu.open(sp, m.blockEntity().getBlockPos());
                            return 1;
                        }))))
                .then(Commands.literal("review").then(Commands.argument("stars", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 5))
                        .executes(ctx -> review(ctx, ""))
                        .then(Commands.argument("text", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(ctx -> review(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "text")))))));
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
                        .then(Commands.argument("a", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5)).executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            var d = com.snolred.wildplus.content.ContentData.get(p.level().getServer()).player("luxury", p.getUUID());
                            d.putInt("wings", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "w"));
                            d.putInt("halo", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "h"));
                            d.putInt("hat", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "a"));
                            com.snolred.wildplus.content.ContentData.get(p.level().getServer()).dirty();
                            com.snolred.wildplus.net.CosmeticSync.broadcast(p.level().getServer());
                            return 1;
                        })))))
                .then(Commands.literal("townhall").executes(ctx -> { // 3.9.9 이미 있는 마을에 게시관 짓기 (보는 쪽 2칸 앞이 건물 앞쪽)
                    var p = ctx.getSource().getPlayerOrException();
                    var lv = p.level();
                    var t = lv.getStructureTemplateManager().get(com.snolred.wildplus.WildPlus.id("village/notice_hall")).orElse(null);
                    if (t == null) return 0;
                    net.minecraft.core.BlockPos pivot = new net.minecraft.core.BlockPos(4, 0, 8);
                    net.minecraft.core.BlockPos at = p.blockPosition().below().relative(p.getDirection(), 2);
                    net.minecraft.core.BlockPos origin = at.subtract(pivot);
                    var set = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                            .setRotation(com.snolred.wildplus.home.HouseKitItem.rotationFor(p.getDirection())).setRotationPivot(pivot);
                    t.placeInWorld(lv, origin, origin, set, lv.getRandom(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    com.snolred.wildplus.home.HouseKitItem.refillParts(lv, t.getBoundingBox(set, origin));
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("🏛 마을 게시관을 지었어요"), false);
                    return 1;
                }))
                .then(Commands.literal("uitest").then(Commands.argument("what", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    var at = p.blockPosition();
                    var front = at.relative(p.getDirection(), 3);
                    switch (com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "what")) {
                        case "pharmacy" -> com.snolred.wildplus.economy.Services.openPharmacy(p, at);
                        case "endplains" -> VoidBiome.debugGoto(p);
                        case "market" -> {
                            p.level().setBlock(front, com.snolred.wildplus.registry.ModBlocks19.MARKET_STALL.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, p.getDirection().getOpposite()), 3);
                            com.snolred.wildplus.economy.Market.open(p, front);
                        }
                        case "bulkship" -> {
                            p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT, 64));
                            p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 23));
                            com.snolred.wildplus.economy.BulkShip.open(p, front);
                        }
                        case "musicfx" -> com.snolred.wildplus.music.Music.debugVisual(p);
                        case "lottodraw" -> com.snolred.wildplus.economy.Lotto.draw(p.level().getServer());
                        case "endruin" -> VoidBiome.debugRuin(p);
                        case "brew" -> {
                            p.level().setBlockAndUpdate(front, net.minecraft.world.level.block.Blocks.BREWING_STAND.defaultBlockState());
                            if (p.level().getBlockEntity(front) instanceof net.minecraft.world.MenuProvider m) p.openMenu(m);
                        }
                        case "weather" -> com.snolred.wildplus.economy.Services.openWeather(p, at);
                        case "landbuy" -> com.snolred.wildplus.content.Land.debugLand(p);
                        case "land" -> com.snolred.wildplus.content.Land.open(p);
                        case "contest" -> com.snolred.wildplus.content.Contest.openBoard(p, at);
                        case "contract" -> com.snolred.wildplus.omen.OmenRaids.debugContract(p);
                        case "prisonfill" -> com.snolred.wildplus.omen.Prisoners.debugFill(p, front);
                        case "prison" -> com.snolred.wildplus.omen.Prisoners.open(p, front);
                        case "altar" -> {
                            com.snolred.wildplus.omen.OmenAltar.debugPoints(p.level().getServer(), at, 34);
                            com.snolred.wildplus.omen.OmenAltar.open(p, at);
                        }
                        default -> {
                            return 0;
                        }
                    }
                    return 1;
                })))
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

    private static int review(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx, String text)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer sp = ctx.getSource().getPlayerOrException();
        if (!(sp.containerMenu instanceof com.snolred.wildplus.menu.RestaurantMenu m) || m.blockEntity() == null) {
            ctx.getSource().sendFailure(Component.literal("식당 카운터 화면에서 평점을 남겨 주세요"));
            return 0;
        }
        int stars = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "stars");
        if (!m.blockEntity().rate(sp, stars, text)) return 0;
        ctx.getSource().sendSuccess(() -> Component.literal("⭐ 평점 " + "★".repeat(stars) + " 남김 · 고마워요!"), false);
        com.snolred.wildplus.menu.RestaurantMenu.open(sp, m.blockEntity().getBlockPos());
        return 1;
    }
}
