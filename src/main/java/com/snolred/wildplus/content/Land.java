package com.snolred.wildplus.content;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 부동산: 청크(16×16, 위아래 전부)를 돈으로 사서 내 땅으로 만든다.
 * - 값: 첫 땅 40만동, 땅이 하나 늘 때마다 +20만동. 되팔면 20만동만 돌려받음 (나머지는 국고로 사라짐)
 * - 재산세: 청크당 게임 하루 5,000동, 접속 여부와 상관없이 매일 걷음 (접속 중이면 통장→동전, 아니면 통장에서). 7일치 밀리면 땅이 모두 풀림
 * - 설정(주인의 모든 땅에 적용): 출입(모두 · 친구만 · 주인만), 블록 부수기·설치, 상자·문 등 사용
 * - 폭발(크리퍼 · TNT)로 땅의 블록이 부서지지 않음
 */
public final class Land {
    private Land() {}

    public static final long BASE_PRICE = 400_000, STEP_PRICE = 200_000, REFUND = 200_000, UPKEEP = 5_000;
    public static final int ACCESS_ALL = 0, ACCESS_FRIENDS = 1, ACCESS_OWNER = 2, MAX_DEBT = 7;
    private static final String[] ACCESS_NAME = {"모두 출입 가능", "친구만 출입", "나만 출입"};

    /** "차원|x|z" → 주인 */
    private static final Map<String, UUID> CHUNKS = new HashMap<>();
    private static final Map<UUID, String> LAST_SEEN = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_OK = new HashMap<>();

    private static String key(Level level, BlockPos pos) {
        return level.dimension().identifier() + "|" + (pos.getX() >> 4) + "|" + (pos.getZ() >> 4);
    }

    private static CompoundTag root(MinecraftServer s) {
        return ContentData.get(s).section("land");
    }

    private static CompoundTag chunks(MinecraftServer s) {
        CompoundTag r = root(s);
        if (!(r.get("chunks") instanceof CompoundTag c)) {
            CompoundTag c2 = new CompoundTag();
            r.put("chunks", c2);
            return c2;
        }
        return c;
    }

    private static CompoundTag owner(MinecraftServer s, UUID id) {
        return ContentData.get(s).player("land_owner", id);
    }

    @SubscribeEvent
    public static void onStart(ServerStartedEvent event) {
        CHUNKS.clear();
        CompoundTag c = chunks(event.getServer());
        for (String k : c.keySet()) c.getString(k).ifPresent(v -> CHUNKS.put(k, UUID.fromString(v)));
    }

    public static UUID ownerAt(Level level, BlockPos pos) {
        return CHUNKS.get(key(level, pos));
    }

    public static int owned(UUID id) {
        int n = 0;
        for (UUID u : CHUNKS.values()) if (u.equals(id)) n++;
        return n;
    }

    public static long nextPrice(UUID id) {
        return BASE_PRICE + STEP_PRICE * owned(id);
    }

    private static boolean isMember(MinecraftServer s, UUID owner, UUID who) {
        if (owner.equals(who)) return true;
        ListTag list = owner(s, owner).getListOrEmpty("members");
        for (Tag t : list) if (t instanceof StringTag st && st.value().equals(who.toString())) return true;
        return false;
    }

    /** 이 위치에서 블록을 부수거나 놓을 수 있는가 */
    public static boolean canBuild(Player p, BlockPos pos) {
        UUID o = ownerAt(p.level(), pos);
        if (o == null || p.isCreative() || p.level().getServer() == null) return true;
        return isMember(p.level().getServer(), o, p.getUUID()) || owner(p.level().getServer(), o).getBooleanOr("build", false);
    }

    public static boolean canUse(Player p, BlockPos pos) {
        UUID o = ownerAt(p.level(), pos);
        if (o == null || p.isCreative() || p.level().getServer() == null) return true;
        return isMember(p.level().getServer(), o, p.getUUID()) || owner(p.level().getServer(), o).getBooleanOr("use", false);
    }

    public static boolean canEnter(ServerPlayer p, BlockPos pos) {
        UUID o = ownerAt(p.level(), pos);
        if (o == null || p.isCreative() || p.isSpectator()) return true;
        int access = owner(p.level().getServer(), o).getIntOr("access", ACCESS_ALL);
        if (access == ACCESS_ALL) return true;
        if (o.equals(p.getUUID())) return true;
        return access == ACCESS_FRIENDS && isMember(p.level().getServer(), o, p.getUUID());
    }

    private static String ownerName(MinecraftServer s, UUID o) {
        return owner(s, o).getStringOr("name", "?");
    }

    // ───────── 보호 ─────────
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && !canBuild(p, event.getPos())) {
            event.setCanceled(true);
            deny(p, "블록을 부술 수 없어요");
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        BlockPos pos = event.getPos();
        ItemStack held = event.getItemStack();
        boolean placing = held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem;
        if (!canUse(p, pos) || (placing && !canBuild(p, pos.relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace())))) {
            event.setCanceled(true);
            deny(p, placing ? "블록을 놓을 수 없어요" : "사용할 수 없어요");
        }
    }

    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate event) {
        if (CHUNKS.isEmpty()) return;
        event.getAffectedBlocks().removeIf(pos -> ownerAt(event.getLevel(), pos) != null);
    }

    private static void deny(ServerPlayer p, String what) {
        UUID o = ownerAt(p.level(), p.blockPosition());
        String name = o == null ? "다른 사람" : ownerName(p.level().getServer(), o);
        p.sendOverlayMessage(Component.literal("🏠 " + name + "의 땅 · " + what).withStyle(ChatFormatting.RED));
    }

    // ───────── 출입 · 안내 · 유지비 ─────────
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        MinecraftServer s = p.level().getServer();
        UUID o = ownerAt(p.level(), p.blockPosition());
        // 출입 금지 땅이면 직전 위치로 밀어냄
        if (o != null && !canEnter(p, p.blockPosition())) {
            Vec3 back = LAST_OK.get(p.getUUID());
            if (back != null && ownerAt(p.level(), BlockPos.containing(back)) != o) {
                p.teleportTo(back.x, back.y, back.z);
            } else {
                Vec3 out = p.position().add(p.getLookAngle().multiply(-3, 0, -3));
                p.teleportTo(out.x, p.getY(), out.z);
            }
            p.sendOverlayMessage(Component.literal("⛔ " + ownerName(s, o) + "의 땅 · 출입 금지").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            return;
        }
        LAST_OK.put(p.getUUID(), p.position());
        String now = o == null ? "" : o.toString();
        String before = LAST_SEEN.getOrDefault(p.getUUID(), "");
        if (!now.equals(before)) {
            LAST_SEEN.put(p.getUUID(), now);
            if (o != null) {
                boolean mine = o.equals(p.getUUID());
                p.sendOverlayMessage(Component.literal(mine ? "🏠 내 땅" : "🏠 " + ownerName(s, o) + "의 땅")
                        .withStyle(mine ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            } else if (!before.isEmpty()) {
                p.sendOverlayMessage(Component.literal("🌲 주인 없는 땅").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** 재산세: 게임 날짜가 바뀌면 모든 땅 주인에게서 (접속 안 해도) 걷는다 */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 100 != 13 || CHUNKS.isEmpty()) return;
        CompoundTag r = root(s);
        long day = ContentData.gameDay;
        long last = r.getLongOr("tax_day", -1L);
        if (last < 0) {
            r.putLong("tax_day", day);
            ContentData.get(s).dirty();
            return;
        }
        if (last >= day) return;
        r.putLong("tax_day", day);
        long days = Math.min(MAX_DEBT, day - last);
        java.util.Set<UUID> owners = new java.util.HashSet<>(CHUNKS.values());
        for (UUID o : owners) for (int i = 0; i < days; i++) chargeOwner(s, o);
        ContentData.get(s).dirty();
    }

    private static void chargeOwner(MinecraftServer s, UUID id) {
        int n = owned(id);
        if (n == 0) return;
        CompoundTag me = owner(s, id);
        long cost = UPKEEP * n;
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        var wd = com.snolred.wildplus.economy.WildData.get(s);
        boolean paid;
        if (p != null) {
            paid = Honor.pay(p, cost);
        } else if (wd.balance(id) >= cost) {
            wd.addBalance(id, -cost);
            wd.treasury += cost;
            wd.setDirty();
            paid = true;
        } else {
            paid = false;
        }
        if (paid) {
            me.putInt("debt", 0);
            me.putLong("tax_paid", me.getLongOr("tax_paid", 0L) + cost);
            if (p != null) p.sendSystemMessage(Component.literal(String.format("🏠 재산세 %,d동을 냈어요 (땅 %d칸 × %,d동)", cost, n, UPKEEP))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            int debt = me.getIntOr("debt", 0) + 1;
            me.putInt("debt", debt);
            if (debt >= MAX_DEBT) {
                releaseAll(s, id);
                if (p != null) p.sendSystemMessage(Component.literal("🏠 재산세가 " + MAX_DEBT + "일 밀려서 땅이 모두 풀렸어요").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else if (p != null) {
                p.sendSystemMessage(Component.literal(String.format("🏠 재산세 %,d동을 못 냈어요 (%d/%d일 밀림 · %d일 밀리면 땅이 풀려요)",
                        cost, debt, MAX_DEBT, MAX_DEBT)).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void releaseAll(MinecraftServer s, UUID id) {
        CompoundTag c = chunks(s);
        CHUNKS.entrySet().removeIf(e -> {
            if (e.getValue().equals(id)) {
                c.remove(e.getKey());
                return true;
            }
            return false;
        });
        owner(s, id).putInt("debt", 0);
        ContentData.get(s).dirty();
    }

    // ───────── 사고팔기 ─────────
    private static void buy(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        String k = key(p.level(), p.blockPosition());
        UUID o = CHUNKS.get(k);
        if (o != null) {
            p.sendSystemMessage(Component.literal(o.equals(p.getUUID()) ? "이미 내 땅이에요" : "이미 " + ownerName(s, o) + "의 땅이에요").withStyle(ChatFormatting.RED));
            return;
        }
        CompoundTag me = owner(s, p.getUUID());
        if (me.getIntOr("debt", 0) > 0) {
            p.sendSystemMessage(Component.literal("밀린 재산세가 있으면 땅을 더 살 수 없어요").withStyle(ChatFormatting.RED));
            return;
        }
        long price = nextPrice(p.getUUID());
        if (!Honor.pay(p, price)) {
            p.sendSystemMessage(Component.literal(String.format("돈이 모자라요 (%,d동 필요 · 통장에서 먼저 빠져요)", price)).withStyle(ChatFormatting.RED));
            return;
        }
        CHUNKS.put(k, p.getUUID());
        chunks(s).putString(k, p.getUUID().toString());
        me.putString("name", p.getGameProfile().name());
        ContentData.get(s).dirty();
        showBorder(p);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.sendSystemMessage(Component.literal(String.format("🏠 땅을 샀어요! (%,d동 · 이제 땅 %d칸 · 재산세 하루 %,d동)",
                price, owned(p.getUUID()), UPKEEP * owned(p.getUUID()))).withStyle(ChatFormatting.GREEN));
    }

    private static void sell(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        String k = key(p.level(), p.blockPosition());
        if (!p.getUUID().equals(CHUNKS.get(k))) {
            p.sendSystemMessage(Component.literal("지금 서 있는 곳이 내 땅이 아니에요").withStyle(ChatFormatting.RED));
            return;
        }
        CHUNKS.remove(k);
        chunks(s).remove(k);
        ContentData.get(s).dirty();
        com.snolred.wildplus.economy.WildData.get(s).addBalance(p.getUUID(), REFUND);
        p.sendSystemMessage(Component.literal(String.format("🏠 땅을 팔았어요 (%,d동은 통장으로)", REFUND)).withStyle(ChatFormatting.YELLOW));
    }

    /** 지금 청크 테두리를 잠깐 표시 */
    private static void showBorder(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        int mx = (p.getBlockX() >> 4) << 4, mz = (p.getBlockZ() >> 4) << 4;
        double y = p.getY() + 0.2;
        for (int i = 0; i <= 16; i++) {
            for (double[] q : new double[][]{{mx + i, mz}, {mx + i, mz + 16}, {mx, mz + i}, {mx + 16, mz + i}}) {
                lv.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, true, q[0], y, q[1], 3, 0, 0.5, 0, 0);
            }
        }
    }

    // ───────── 관리 창 ─────────
    public static void open(ServerPlayer p, BlockPos ignored) {
        open(p);
    }

    public static void open(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        UUID me = p.getUUID();
        CompoundTag mine = owner(s, me);
        UUID here = ownerAt(p.level(), p.blockPosition());
        int n = owned(me);
        List<PickMenu.Entry> e = new ArrayList<>();
        List<Runnable> act = new ArrayList<>();
        String hereText = here == null ? "주인 없는 땅" : here.equals(me) ? "내 땅" : ownerName(s, here) + "의 땅";
        e.add(new PickMenu.Entry("📍 여기: " + hereText, String.format("청크 %d, %d · 누르면 테두리 보기", p.getBlockX() >> 4, p.getBlockZ() >> 4)));
        act.add(() -> showBorder(p));
        if (here == null) {
            e.add(new PickMenu.Entry("🏠 이 땅 사기", String.format("%,d동 (통장 먼저)", nextPrice(me))));
            act.add(() -> buy(p));
        } else if (here.equals(me)) {
            e.add(new PickMenu.Entry("💸 이 땅 팔기", String.format("%,d동 돌려받음", REFUND)));
            act.add(() -> sell(p));
        }
        int access = mine.getIntOr("access", ACCESS_ALL);
        e.add(new PickMenu.Entry("🚪 출입: " + ACCESS_NAME[access], "누르면 바뀜"));
        act.add(() -> {
            mine.putInt("access", (access + 1) % 3);
            ContentData.get(s).dirty();
        });
        boolean build = mine.getBooleanOr("build", false);
        e.add(new PickMenu.Entry("⛏ 블록 부수기·놓기: " + (build ? "모두 허용" : "친구만"), "누르면 바뀜"));
        act.add(() -> {
            mine.putBoolean("build", !build);
            ContentData.get(s).dirty();
        });
        boolean use = mine.getBooleanOr("use", false);
        e.add(new PickMenu.Entry("📦 상자·문·기계 사용: " + (use ? "모두 허용" : "친구만"), "누르면 바뀜"));
        act.add(() -> {
            mine.putBoolean("use", !use);
            ContentData.get(s).dirty();
        });
        e.add(new PickMenu.Entry(String.format("📜 내 땅 %d칸 · 재산세 하루 %,d동", n, UPKEEP * n),
                mine.getIntOr("debt", 0) > 0 ? "⚠ 밀린 재산세 " + mine.getIntOr("debt", 0) + "일" : "다음 땅 " + String.format("%,d동", nextPrice(me))));
        act.add(() -> {});
        ListTag members = mine.getListOrEmpty("members");
        List<String> names = new ArrayList<>();
        for (Tag t : members) if (t instanceof StringTag st) names.add(st.value());
        for (String id : names) {
            String nm = ContentData.get(s).player("land_owner", UUID.fromString(id)).getStringOr("name", null);
            ServerPlayer on = s.getPlayerList().getPlayer(UUID.fromString(id));
            String shown = on != null ? on.getGameProfile().name() : nm != null ? nm : mine.getCompoundOrEmpty("member_names").getStringOr(id, "?");
            e.add(new PickMenu.Entry("👥 친구: " + shown, "누르면 친구에서 빼기"));
            act.add(() -> {
                ListTag l = mine.getListOrEmpty("members");
                l.removeIf(t -> t instanceof StringTag st && st.value().equals(id));
                mine.put("members", l);
                ContentData.get(s).dirty();
            });
        }
        PickMenu.open(p, Component.literal("🏠 부동산"), "친구 추가: /land trust 이름 · 빼기: /land untrust 이름",
                "", e, 0, i -> {
                    if (i >= 0 && i < act.size()) act.get(i).run();
                    open(p);
                }, null, pl -> true);
    }

    private static int trust(ServerPlayer me, String name, boolean add) {
        MinecraftServer s = me.level().getServer();
        ServerPlayer who = s.getPlayerList().getPlayerByName(name);
        UUID id = who != null ? who.getUUID() : null;
        CompoundTag mine = owner(s, me.getUUID());
        ListTag l = mine.getListOrEmpty("members");
        if (id == null) {
            // 접속하지 않은 사람: 이름으로 기억해 둔 목록에서 찾기
            CompoundTag nm = mine.getCompoundOrEmpty("member_names");
            for (String k : nm.keySet()) if (nm.getStringOr(k, "").equalsIgnoreCase(name)) id = UUID.fromString(k);
        }
        if (id == null) {
            me.sendSystemMessage(Component.literal("그 사람이 접속해 있을 때 해 주세요").withStyle(ChatFormatting.RED));
            return 0;
        }
        String ids = id.toString();
        l.removeIf(t -> t instanceof StringTag st && st.value().equals(ids));
        if (add) {
            l.add(StringTag.valueOf(ids));
            CompoundTag nm = mine.getCompoundOrEmpty("member_names");
            nm.putString(ids, who != null ? who.getGameProfile().name() : name);
            mine.put("member_names", nm);
        }
        mine.put("members", l);
        ContentData.get(s).dirty();
        me.sendSystemMessage(Component.literal(add ? "👥 " + name + " 님을 땅 친구로 추가했어요" : "👥 " + name + " 님을 땅 친구에서 뺐어요")
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("land")
                .executes(ctx -> {
                    open(ctx.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("buy").executes(ctx -> {
                    buy(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("trust").then(Commands.argument("name", StringArgumentType.word()).executes(ctx ->
                        trust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"), true))))
                .then(Commands.literal("untrust").then(Commands.argument("name", StringArgumentType.word()).executes(ctx ->
                        trust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"), false)))));
    }
}
