package com.snolred.wildplus.home;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.snolred.wildplus.block.BigBlock;
import com.snolred.wildplus.block.FurnitureBedBlock;
import com.snolred.wildplus.block.FurnitureBlock;
import com.snolred.wildplus.block.GuestbookBlockEntity;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.Mail;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 3.4 집들이 · 집 평가.
 * 집 주인이 방명록을 놓으면 그 집이 등록된다. 손님은 방명록에서 별점(1~5)을 고르고 한 줄 후기를 남길 수 있고 (하루 한 번),
 * 주인은 후기에 답글을 단다. 집 점수(0~100)는 방명록 근처 가구의 종류 · 개수 · 침대 · 조명 · 트로피 · 꽃으로 계산.
 * 게임 속 7일마다 '이번 주 예쁜 집' (평균 별점 × 20 + 집 점수가 가장 높은 집) 에 상금과 문패 금 리본.
 */
public final class HouseVisit {
    private HouseVisit() {}

    public static final long BEST_PRIZE = 50_000;
    public static final int MAX_REVIEWS = 20;

    private static CompoundTag house(MinecraftServer server, UUID owner) {
        return ContentData.get(server).player("house", owner);
    }

    // ───────── 등록 ─────────
    public static void register(ServerPlayer owner, BlockPos pos) {
        CompoundTag h = house(owner.level().getServer(), owner.getUUID());
        if (!h.contains("name")) h.putString("name", owner.getName().getString() + "네 집");
        h.putString("owner_name", owner.getName().getString());
        h.putLong("pos", pos.asLong());
        h.putString("dim", owner.level().dimension().identifier().toString());
        ContentData.get(owner.level().getServer()).dirty();
        owner.sendSystemMessage(Component.literal("🏠 방명록을 놓았어요! 친구들이 와서 별점과 후기를 남길 수 있어요 · ")
                .append(suggest("[집 이름 정하기]", "/house name "))
                .withStyle(ChatFormatting.GOLD));
    }

    private static MutableComponent suggest(String label, String cmd) {
        return Component.literal(label).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand(cmd)));
    }

    // ───────── 집 점수 ─────────
    public record Score(int total, int kinds, int pieces, int beds, int lights, int trophies, int plants) {}

    public static Score score(ServerLevel level, BlockPos center) {
        Set<String> kinds = new HashSet<>();
        int pieces = 0, beds = 0, lights = 0, trophies = 0, plants = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-12, -4, -12), center.offset(12, 8, 12))) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            var b = s.getBlock();
            if (b instanceof FurnitureBlock f) {
                pieces++;
                kinds.add(f.kind.name());
            } else if (b instanceof FurnitureBedBlock || s.is(BlockTags.BEDS)) {
                if (!s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)
                        || s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)
                        == net.minecraft.world.level.block.state.properties.BedPart.FOOT) beds++;
            } else if (b instanceof com.snolred.wildplus.block.TrophyBlock) {
                trophies++;
            } else if (b instanceof BigBlock && !(b instanceof com.snolred.wildplus.block.GuestbookBlock)) {
                pieces++;
                kinds.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath());
            }
            if (s.getLightEmission() > 0) lights++;
            if (s.is(BlockTags.FLOWERS) || s.is(BlockTags.FLOWER_POTS)) plants++;
        }
        int total = Math.min(100, kinds.size() * 4 + Math.min(pieces, 30) + Math.min(beds, 4) * 5 + Math.min(lights, 10) * 2
                + Math.min(trophies, 5) * 6 + Math.min(plants, 8));
        return new Score(total, kinds.size(), pieces, beds, lights, trophies, plants);
    }

    // ───────── 후기 ─────────
    public static ListTag reviews(CompoundTag h) {
        ListTag l = h.getListOrEmpty("reviews");
        h.put("reviews", l);
        return l;
    }

    public static double average(CompoundTag h) {
        ListTag l = reviews(h);
        if (l.isEmpty()) return 0;
        double s = 0;
        for (Tag t : l) s += ((CompoundTag) t).getIntOr("stars", 0);
        return s / l.size();
    }

    private static String stars(int n) {
        return "★".repeat(n) + "☆".repeat(5 - n);
    }

    public static boolean review(ServerPlayer p, UUID owner, int stars, String text) {
        MinecraftServer server = p.level().getServer();
        if (owner.equals(p.getUUID())) {
            p.sendSystemMessage(Component.literal("자기 집에는 후기를 남길 수 없어요").withStyle(ChatFormatting.RED));
            return false;
        }
        CompoundTag h = house(server, owner);
        ListTag l = reviews(h);
        long day = ContentData.gameDay;
        // 같은 손님의 전 후기는 지우고 새로 (하루 한 번)
        for (int i = l.size() - 1; i >= 0; i--) {
            CompoundTag r = l.getCompoundOrEmpty(i);
            if (r.getStringOr("by", "").equals(p.getUUID().toString())) {
                if (r.getLongOr("day", -1) == day && !r.getStringOr("text", "").isEmpty() && !text.isEmpty()) {
                    p.sendSystemMessage(Component.literal("이 집 후기는 하루에 한 번만 쓸 수 있어요").withStyle(ChatFormatting.YELLOW));
                    return false;
                }
                l.remove(i);
            }
        }
        CompoundTag r = new CompoundTag();
        r.putString("by", p.getUUID().toString());
        r.putString("name", p.getName().getString());
        r.putInt("stars", Math.max(1, Math.min(5, stars)));
        r.putString("text", text.length() > 60 ? text.substring(0, 60) : text);
        r.putLong("day", day);
        r.putLong("week", ContentData.week());
        l.add(0, r);
        while (l.size() > MAX_REVIEWS) l.remove(l.size() - 1);
        ContentData.get(server).dirty();
        p.sendSystemMessage(Component.literal("💌 " + h.getStringOr("name", "집") + "에 " + stars(stars) + (text.isEmpty() ? "" : " “" + text + "”") + " 남겼어요")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (text.isEmpty()) p.sendSystemMessage(suggest("[한 줄 후기 쓰기]", "/house review " + stars + " "));
        ServerPlayer o = server.getPlayerList().getPlayer(owner);
        if (o != null) {
            o.sendSystemMessage(Component.literal("🏠 " + p.getName().getString() + "님이 우리 집에 " + stars(stars) + (text.isEmpty() ? "" : " “" + text + "”"))
                    .withStyle(ChatFormatting.GOLD).append(" ").append(suggest("[답글]", "/house reply 0 ")));
            o.level().playSound(null, o.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return true;
    }

    public static boolean reply(ServerPlayer owner, int index, String text) {
        CompoundTag h = house(owner.level().getServer(), owner.getUUID());
        ListTag l = reviews(h);
        if (index < 0 || index >= l.size()) return false;
        CompoundTag r = l.getCompoundOrEmpty(index);
        r.putString("reply", text.length() > 60 ? text.substring(0, 60) : text);
        ContentData.get(owner.level().getServer()).dirty();
        ServerPlayer by = owner.level().getServer().getPlayerList().getPlayer(UUID.fromString(r.getStringOr("by", owner.getUUID().toString())));
        if (by != null) by.sendSystemMessage(Component.literal("💬 " + owner.getName().getString() + "님의 답글: “" + text + "”").withStyle(ChatFormatting.AQUA));
        return true;
    }

    // ───────── 방명록 · 문패 화면 ─────────
    public static void openGuestbook(ServerPlayer p, BlockPos pos) {
        if (!(p.level() instanceof ServerLevel level) || !(level.getBlockEntity(pos) instanceof GuestbookBlockEntity be)) return;
        if (be.owner == null) { // 주인 없는 방명록 (구조물 · 명령어로 놓임): 처음 누른 사람이 주인
            be.owner = p.getUUID();
            be.ownerName = p.getName().getString();
            be.setChanged();
            register(p, pos);
        }
        MinecraftServer server = level.getServer();
        UUID owner = be.owner;
        CompoundTag h = house(server, owner);
        h.putLong("pos", pos.asLong());
        h.putString("dim", level.dimension().identifier().toString());
        Score sc = score(level, pos);
        h.putInt("score", sc.total());
        ContentData.get(server).dirty();
        String name = h.getStringOr("name", be.ownerName + "네 집");
        ListTag l = reviews(h);
        String header = String.format("%s · %s %.1f (%d개)%s", name, stars((int) Math.round(average(h))), average(h), l.size(),
                h.getLongOr("best_week", -99) >= ContentData.week() - 1 ? " · 🎀 예쁜 집" : "");
        List<PickMenu.Entry> rows = new ArrayList<>();
        boolean mine = owner.equals(p.getUUID());
        if (!mine) {
            for (int s = 5; s >= 1; s--) rows.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, stars(s), s == 5 ? "최고예요!" : s == 4 ? "멋져요" : s == 3 ? "괜찮아요" : s == 2 ? "아쉬워요" : "별로예요"));
        } else {
            rows.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, "✏ 집 이름 바꾸기", "지금: " + name));
        }
        rows.add(PickMenu.Entry.of(PickMenu.K_INFO, String.format("🏅 집 점수 %d/100", sc.total()),
                String.format("가구 %d종 %d개 · 침대 %d · 조명 %d · 트로피 %d · 꽃 %d", sc.kinds(), sc.pieces(), sc.beds(), sc.lights(), sc.trophies(), sc.plants())));
        int base = rows.size();
        for (Tag t : l) {
            CompoundTag r = (CompoundTag) t;
            String reply = r.getStringOr("reply", "");
            rows.add(new PickMenu.Entry(stars(r.getIntOr("stars", 0)) + " " + r.getStringOr("name", "?") + " · " + (r.getLongOr("day", 0) + 1) + "일차",
                    (r.getStringOr("text", "").isEmpty() ? "(별점만)" : "“" + r.getStringOr("text", "") + "”") + (reply.isEmpty() ? (mine ? " · 눌러서 답글" : "") : " ↳ 주인: " + reply)));
        }
        p.getPersistentData().putString("wp_gb_owner", owner.toString());
        PickMenu.open(p, Component.literal("📖 방명록"), header, "아직 후기가 없어요", rows, 0, i -> {
            if (!mine && i < 5) {
                review(p, owner, 5 - i, "");
                p.closeContainer();
            } else if (mine && i == 0) {
                p.closeContainer();
                p.sendSystemMessage(suggest("[여기를 눌러 집 이름 입력]", "/house name "));
            } else if (mine && i >= base) {
                p.closeContainer();
                p.sendSystemMessage(suggest("[" + (i - base + 1) + "번 후기에 답글 쓰기]", "/house reply " + (i - base) + " "));
            }
        }, null, pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }

    /** 문패: 근처 방명록의 집 정보 */
    public static void plaque(ServerPlayer p, BlockPos pos) {
        if (!(p.level() instanceof ServerLevel level)) return;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-16, -6, -16), pos.offset(16, 6, 16))) {
            if (level.getBlockEntity(q) instanceof GuestbookBlockEntity be && be.owner != null) {
                CompoundTag h = house(level.getServer(), be.owner);
                p.sendSystemMessage(Component.literal(String.format("🏠 %s · 주인 %s", h.getStringOr("name", "집"), be.ownerName)).withStyle(ChatFormatting.GOLD));
                p.sendSystemMessage(Component.literal(String.format("%s %.1f (후기 %d개) · 집 점수 %d/100", stars((int) Math.round(average(h))), average(h),
                        reviews(h).size(), score(level, q.immutable()).total())).withStyle(ChatFormatting.YELLOW));
                if (h.getLongOr("best_week", -99) >= ContentData.week() - 1) {
                    p.sendSystemMessage(Component.literal("🎀 이번 주 예쁜 집!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
                int n = 0;
                for (Tag t : reviews(h)) {
                    if (n++ >= 3) break;
                    CompoundTag r = (CompoundTag) t;
                    p.sendSystemMessage(Component.literal(stars(r.getIntOr("stars", 0)) + " " + r.getStringOr("name", "") + " “" + r.getStringOr("text", "") + "”")
                            .withStyle(ChatFormatting.GRAY));
                }
                return;
            }
        }
        p.sendSystemMessage(Component.literal("🏠 근처(16칸)에 방명록이 없어요 · 방명록을 놓으면 집 정보가 문패에 나와요").withStyle(ChatFormatting.GRAY));
    }

    // ───────── 이번 주 예쁜 집 ─────────
    public static void tick(MinecraftServer server) {
        CompoundTag s = ContentData.get(server).section("house_week");
        long week = ContentData.week();
        long last = s.getLongOr("week", Long.MIN_VALUE);
        if (last == week) return;
        s.putLong("week", week);
        ContentData.get(server).dirty();
        if (last == Long.MIN_VALUE) return;
        // 지난주 (last) 후기로 뽑기
        CompoundTag all = ContentData.get(server).section("house");
        String best = null;
        double bestVal = -1;
        for (String id : all.keySet()) {
            CompoundTag h = all.getCompoundOrEmpty(id);
            double sum = 0;
            int n = 0;
            for (Tag t : reviews(h)) {
                CompoundTag r = (CompoundTag) t;
                if (r.getLongOr("week", -1) == last) {
                    sum += r.getIntOr("stars", 0);
                    n++;
                }
            }
            if (n == 0) continue;
            double val = sum / n * 20 + h.getIntOr("score", 0) + n * 2;
            if (val > bestVal) {
                bestVal = val;
                best = id;
            }
        }
        if (best == null) return;
        CompoundTag h = all.getCompoundOrEmpty(best);
        h.putLong("best_week", week);
        UUID id = UUID.fromString(best);
        WildData.get(server).addBalance(id, BEST_PRIZE);
        Mail.notify(server, id, "집들이 심사단", 0);
        server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("🎀 이번 주 예쁜 집: %s (%s) — 상금 %,d동!",
                h.getStringOr("name", "?"), h.getStringOr("owner_name", "?"), BEST_PRIZE)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
    }

    // ───────── 명령: /house name · review · reply ─────────
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("house")
                .then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    String n = StringArgumentType.getString(ctx, "name").trim();
                    if (n.length() > 20) n = n.substring(0, 20);
                    house(sp.level().getServer(), sp.getUUID()).putString("name", n);
                    ContentData.get(sp.level().getServer()).dirty();
                    final String fn = n;
                    ctx.getSource().sendSuccess(() -> Component.literal("🏠 집 이름: " + fn), false);
                    return 1;
                })))
                .then(Commands.literal("review").then(Commands.argument("stars", IntegerArgumentType.integer(1, 5))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            String o = sp.getPersistentData().getStringOr("wp_gb_owner", "");
                            if (o.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("먼저 친구 집의 방명록을 눌러 주세요"));
                                return 0;
                            }
                            return review(sp, UUID.fromString(o), IntegerArgumentType.getInteger(ctx, "stars"), StringArgumentType.getString(ctx, "text").trim()) ? 1 : 0;
                        }))))
                .then(Commands.literal("reply").then(Commands.argument("index", IntegerArgumentType.integer(0, MAX_REVIEWS))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            if (!reply(sp, IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "text").trim())) {
                                ctx.getSource().sendFailure(Component.literal("그 번호의 후기가 없어요"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("💬 답글을 달았어요"), false);
                            return 1;
                        })))));
    }
}
