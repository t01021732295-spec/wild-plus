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
 * - 값 (3.7 물가 3배): 첫 땅 120만동, 땅이 하나 늘 때마다 +60만동. 되팔면 60만동만 돌려받음 (나머지는 국고로 사라짐)
 * - 재산세: 청크당 5,000동. 접속 중이면 게임 하루마다(통장→동전), 접속 안 하면 현실 24시간마다(통장에서). 7일치 밀리면 땅이 모두 풀림
 * - 설정(주인의 모든 땅에 적용): 출입(모두 · 친구만 · 주인만), 블록 부수기·설치, 상자·문 등 사용
 * - 폭발(크리퍼 · TNT)로 땅의 블록이 부서지지 않음
 */
public final class Land {
    private Land() {}

    public static final long BASE_PRICE = com.snolred.wildplus.economy.Prices.x(400_000L), STEP_PRICE = com.snolred.wildplus.economy.Prices.x(200_000L), REFUND = com.snolred.wildplus.economy.Prices.x(200_000L), UPKEEP = com.snolred.wildplus.economy.Prices.x(5_000L);
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

    /** 이 사람이 이 차원에 가진 땅의 청크 좌표들 (미니맵용) */
    public static java.util.List<int[]> chunksOf(UUID id, String dim) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (var e : CHUNKS.entrySet()) {
            if (!e.getValue().equals(id)) continue;
            String[] k = e.getKey().split("\\|");
            if (k.length == 3 && k[0].equals(dim)) out.add(new int[] {Integer.parseInt(k[1]), Integer.parseInt(k[2])});
        }
        return out;
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
    /**
     * 3.9.4 누가 봐도 주인 있는 땅: 둘레 5×5 청크 중 주인 있는 땅의 바깥 경계선을 주인 색 빛 울타리로,
     * 땅 안에는 주인 색 반짝이가 떠다님 (1.5초마다 · 그 사람 화면에만 보냄).
     */
    private static void showClaims(ServerPlayer p) {
        ServerLevel lv = p.level();
        if (CHUNKS.isEmpty()) return;
        String dim = lv.dimension().identifier() + "|";
        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        var r = p.getRandom();
        for (int cx = pcx - 2; cx <= pcx + 2; cx++) {
            for (int cz = pcz - 2; cz <= pcz + 2; cz++) {
                UUID o = CHUNKS.get(dim + cx + "|" + cz);
                if (o == null || !lv.hasChunk(cx, cz)) continue;
                int col = com.snolred.wildplus.block.LandPostBlock.RGB[com.snolred.wildplus.block.LandPostBlock.colorOf(o)];
                var dust = new net.minecraft.core.particles.DustParticleOptions(col, 2.0F);
                int mx = cx << 4, mz = cz << 4;
                boolean n = !o.equals(CHUNKS.get(dim + cx + "|" + (cz - 1))), sth = !o.equals(CHUNKS.get(dim + cx + "|" + (cz + 1)));
                boolean w = !o.equals(CHUNKS.get(dim + (cx - 1) + "|" + cz)), e = !o.equals(CHUNKS.get(dim + (cx + 1) + "|" + cz));
                for (int i = 0; i <= 16; i++) {
                    if (n) fence(p, lv, dust, mx + i, mz);
                    if (sth) fence(p, lv, dust, mx + i, mz + 16);
                    if (w) fence(p, lv, dust, mx, mz + i);
                    if (e) fence(p, lv, dust, mx + 16, mz + i);
                }
                for (int k = 0; k < 8; k++) {
                    double x = mx + r.nextDouble() * 16, z = mz + r.nextDouble() * 16;
                    int y = lv.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
                    if (Math.abs(y - p.getY()) > 24) continue;
                    lv.sendParticles(p, dust, false, false, x, y + 0.4 + r.nextDouble() * 1.5, z, 1, 0.1, 0.2, 0.1, 0);
                }
            }
        }
    }

    private static void fence(ServerPlayer p, ServerLevel lv, net.minecraft.core.particles.ParticleOptions dust, double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        if (!lv.hasChunk(bx >> 4, bz >> 4)) return;
        int y = lv.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        if (Math.abs(y - p.getY()) > 24) return;
        lv.sendParticles(p, dust, false, false, x, y + 0.2, z, 1, 0, 0, 0, 0);
        lv.sendParticles(p, dust, false, false, x, y + 0.8, z, 1, 0, 0, 0, 0);
        lv.sendParticles(p, dust, false, false, x, y + 1.4, z, 1, 0, 0, 0, 0);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        if (p.tickCount % 20 == 0) showClaims(p);
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
                boolean wasMine = before.equals(p.getUUID().toString());
                if (mine && !wasMine) {
                    title(p, "🏠 내 땅", "어서 와요, " + p.getGameProfile().name() + " 님");
                    edgeGlow(p);
                } else {
                    p.sendOverlayMessage(Component.literal(mine ? "🏠 내 땅" : "🏠 " + ownerName(s, o) + "의 땅")
                            .withStyle(mine ? ChatFormatting.GREEN : ChatFormatting.GOLD));
                }
            } else if (!before.isEmpty()) {
                p.sendOverlayMessage(Component.literal("🌲 주인 없는 땅").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * 재산세: 접속 중인 주인은 게임 날짜가 바뀔 때마다,
     * 접속하지 않은 주인은 현실 시간 24시간마다 (서버가 꺼져 있던 시간도 다음에 켜질 때 몰아서, 최대 7일치) 걷는다.
     */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 100 != 13 || CHUNKS.isEmpty()) return;
        CompoundTag r = root(s);
        long day = ContentData.gameDay;
        long last = r.getLongOr("tax_day", -1L);
        boolean newDay = last >= 0 && last < day;
        if (last < day) {
            r.putLong("tax_day", day);
            ContentData.get(s).dirty();
        }
        long nowMs = System.currentTimeMillis();
        java.util.Set<UUID> owners = new java.util.HashSet<>(CHUNKS.values());
        for (UUID o : owners) {
            CompoundTag me = owner(s, o);
            boolean online = s.getPlayerList().getPlayer(o) != null;
            long realMs = me.getLongOr("real_ms", 0L);
            if (realMs == 0L) {
                me.putLong("real_ms", nowMs);
                ContentData.get(s).dirty();
                continue;
            }
            if (online) {
                if (newDay) {
                    chargeOwner(s, o);
                    me.putLong("real_ms", nowMs);
                }
            } else {
                long days = (nowMs - realMs) / REAL_DAY_MS;
                if (days <= 0) continue;
                for (int i = 0; i < Math.min(MAX_DEBT, days); i++) chargeOwner(s, o);
                me.putLong("real_ms", realMs + days * REAL_DAY_MS);
                ContentData.get(s).dirty();
            }
        }
    }

    private static final long REAL_DAY_MS = 24L * 60 * 60 * 1000;

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
        buyAt(p, p.blockPosition());
    }

    /** 3.4 부동산 중개소: 서 있는 곳이 아니어도 그 청크를 산다 */
    public static boolean buyAt(ServerPlayer p, BlockPos at) {
        MinecraftServer s = p.level().getServer();
        String k = key(p.level(), at);
        UUID o = CHUNKS.get(k);
        if (o != null) {
            p.sendSystemMessage(Component.literal(o.equals(p.getUUID()) ? "이미 내 땅이에요" : "이미 " + ownerName(s, o) + "의 땅이에요").withStyle(ChatFormatting.RED));
            return false;
        }
        CompoundTag me = owner(s, p.getUUID());
        if (me.getIntOr("debt", 0) > 0) {
            p.sendSystemMessage(Component.literal("밀린 재산세가 있으면 땅을 더 살 수 없어요").withStyle(ChatFormatting.RED));
            return false;
        }
        long price = nextPrice(p.getUUID());
        if (!Honor.pay(p, price)) {
            p.sendSystemMessage(Component.literal(String.format("돈이 모자라요 (%,d동 필요 · 통장에서 먼저 빠져요)", price)).withStyle(ChatFormatting.RED));
            return false;
        }
        CHUNKS.put(k, p.getUUID());
        chunks(s).putString(k, p.getUUID().toString());
        me.putString("name", p.getGameProfile().name());
        ContentData.get(s).dirty();
        if ((at.getX() >> 4) == (p.getBlockX() >> 4) && (at.getZ() >> 4) == (p.getBlockZ() >> 4)) showBorder(p);
        placePosts(p.level(), at, p.getUUID(), false);
        title(p, "🏠 내 땅이 됐어요!", "모서리에 경계 기둥을 세웠어요 · /land 로 관리");
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.sendSystemMessage(Component.literal(String.format("🏠 땅을 샀어요! (%,d동 · 이제 땅 %d칸 · 재산세 하루 %,d동)",
                price, owned(p.getUUID()), UPKEEP * owned(p.getUUID()))).withStyle(ChatFormatting.GREEN));
        return true;
    }

    private static void sell(ServerPlayer p) {
        sellAt(p, p.blockPosition());
    }

    public static boolean sellAt(ServerPlayer p, BlockPos at) {
        MinecraftServer s = p.level().getServer();
        String k = key(p.level(), at);
        if (!p.getUUID().equals(CHUNKS.get(k))) {
            p.sendSystemMessage(Component.literal("그곳은 내 땅이 아니에요").withStyle(ChatFormatting.RED));
            return false;
        }
        CHUNKS.remove(k);
        chunks(s).remove(k);
        ContentData.get(s).dirty();
        com.snolred.wildplus.economy.WildData.get(s).addBalance(p.getUUID(), REFUND);
        placePosts(p.level(), at, p.getUUID(), false);
        p.sendSystemMessage(Component.literal(String.format("🏠 땅을 팔았어요 (%,d동은 통장으로)", REFUND)).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public static String ownerNameOf(MinecraftServer s, UUID id) {
        return ownerName(s, id);
    }

    // ───────── 3.9 경계 기둥 ─────────
    private static boolean ownedBy(Level level, int cx, int cz, UUID id) {
        return id.equals(CHUNKS.get(level.dimension().identifier() + "|" + cx + "|" + cz));
    }

    /**
     * 청크 꼭짓점(vx, vz)마다 이 주인의 땅 모양을 보고 기둥을 세우거나 치운다.
     * 둘레 4청크 중 내 땅이 1 · 3개이거나 대각선 2개면 모서리 → 기둥 하나 (내 땅 청크 쪽 칸에).
     */
    private static int vertex(ServerLevel level, int vx, int vz, UUID id, boolean force) {
        int[][] around = {{vx - 1, vz - 1}, {vx, vz - 1}, {vx - 1, vz}, {vx, vz}};
        boolean[] mine = new boolean[4];
        int n = 0;
        for (int i = 0; i < 4; i++) if (mine[i] = ownedBy(level, around[i][0], around[i][1], id)) n++;
        boolean corner = n == 1 || n == 3 || (n == 2 && mine[0] == mine[3]);
        int keep = -1;
        if (corner) for (int i = 0; i < 4; i++) if (mine[i]) { keep = i; break; }
        int placed = 0;
        for (int i = 0; i < 4; i++) {
            int cx = around[i][0], cz = around[i][1];
            UUID o = CHUNKS.get(level.dimension().identifier() + "|" + cx + "|" + cz);
            if (o != null && !o.equals(id)) continue; // 남의 땅 기둥은 건드리지 않음
            int bx = cx == vx ? vx * 16 : vx * 16 - 1, bz = cz == vz ? vz * 16 : vz * 16 - 1;
            if (!level.hasChunk(cx, cz)) continue;
            BlockPos found = null;
            for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                BlockPos q = new BlockPos(bx, y, bz);
                if (level.getBlockState(q).is(com.snolred.wildplus.registry.ModBlocks10.LAND_POST.get())) {
                    found = q;
                    break;
                }
            }
            if (i != keep) {
                if (found != null) level.removeBlock(found, false);
                continue;
            }
            if (found != null && !force) continue;
            if (found != null) level.removeBlock(found, false);
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(bx, 0, bz));
            if (!level.getBlockState(top).canBeReplaced() || !level.getBlockState(top.above()).canBeReplaced()) continue;
            if (!level.getFluidState(top).isEmpty()) continue;
            level.setBlock(top, com.snolred.wildplus.registry.ModBlocks10.LAND_POST.get().defaultBlockState()
                    .setValue(com.snolred.wildplus.block.LandPostBlock.COLOR, com.snolred.wildplus.block.LandPostBlock.colorOf(id)), 3);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, bx + 0.5, top.getY() + 1.2, bz + 0.5, 12, 0.3, 0.8, 0.3, 0);
            placed++;
        }
        return placed;
    }

    /** 이 청크의 네 꼭짓점 기둥 정리 (사기 · 팔기 · 다시 세우기) */
    public static int placePosts(ServerLevel level, BlockPos at, UUID id, boolean force) {
        int cx = at.getX() >> 4, cz = at.getZ() >> 4, n = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) n += vertex(level, cx + dx, cz + dz, id, force);
        return n;
    }

    /** 화면 가운데 큰 글씨 */
    private static void title(ServerPlayer p, String big, String small) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 70, 20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(small).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(big).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
    }

    /** 내 땅에 들어설 때: 지금 청크 중 내 땅 바깥쪽 경계선만 잠깐 빛나게 */
    private static void edgeGlow(ServerPlayer p) {
        ServerLevel lv = p.level();
        int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
        UUID me = p.getUUID();
        double y = p.getY() + 0.3;
        int mx = cx << 4, mz = cz << 4;
        for (int i = 0; i <= 16; i += 2) {
            if (!ownedBy(lv, cx, cz - 1, me)) lv.sendParticles(p, ParticleTypes.WAX_ON, true, true, mx + i, y, mz, 2, 0, 0.4, 0, 0);
            if (!ownedBy(lv, cx, cz + 1, me)) lv.sendParticles(p, ParticleTypes.WAX_ON, true, true, mx + i, y, mz + 16, 2, 0, 0.4, 0, 0);
            if (!ownedBy(lv, cx - 1, cz, me)) lv.sendParticles(p, ParticleTypes.WAX_ON, true, true, mx, y, mz + i, 2, 0, 0.4, 0, 0);
            if (!ownedBy(lv, cx + 1, cz, me)) lv.sendParticles(p, ParticleTypes.WAX_ON, true, true, mx + 16, y, mz + i, 2, 0, 0.4, 0, 0);
        }
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

    /** 테스트용 (/wildplus uitest land): 지금 청크 + 동쪽 청크를 사고, 남쪽 청크는 가짜 주인 땅으로 */
    public static void debugLand(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        com.snolred.wildplus.economy.WildData.get(s).addBalance(p.getUUID(), nextPrice(p.getUUID()) * 3);
        buyAt(p, p.blockPosition());
        buyAt(p, p.blockPosition().east(16));
        UUID other = UUID.nameUUIDFromBytes("wildplus-uitest".getBytes());
        String k = key(p.level(), p.blockPosition().south(16));
        if (!CHUNKS.containsKey(k)) {
            CHUNKS.put(k, other);
            chunks(s).putString(k, other.toString());
            owner(s, other).putString("name", "이웃");
            ContentData.get(s).dirty();
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
        // 3.9 지도: 둘레 9×9 청크 (0 빈 땅 · 1 내 땅 · 2 친구 땅 · 3 남의 땅) + 정보 (ThemedScreen 이 그림)
        StringBuilder map = new StringBuilder("MAP:");
        int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
        String dim = p.level().dimension().identifier() + "|";
        for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                UUID o = CHUNKS.get(dim + (cx + dx) + "|" + (cz + dz));
                map.append(o == null ? '0' : o.equals(me) ? '1' : isMember(s, o, me) || isMember(s, me, o) ? '2' : '3');
            }
        }
        int friends = mine.getListOrEmpty("members").size();
        int debt = mine.getIntOr("debt", 0);
        map.append("|여기=").append(hereText)
                .append("|내 땅=").append(n).append("칸")
                .append("|재산세=").append(String.format("%,d동/일", UPKEEP * n))
                .append("|친구=").append(friends).append("명")
                .append("|").append(debt > 0 ? "밀린 세금=" + debt + "일 (" + MAX_DEBT + "일이면 풀림)" : "다음 땅=" + String.format("%,d동", nextPrice(me)));
        e.add(PickMenu.Entry.of(PickMenu.K_INFO, "지도", map.toString()));
        act.add(() -> {});
        e.add(new PickMenu.Entry("📍 테두리 보기", ""));
        act.add(() -> showBorder(p));
        if (here != null && here.equals(me)) {
            e.add(new PickMenu.Entry("🚩 경계 기둥 다시 세우기", ""));
            act.add(() -> placePosts(p.level(), p.blockPosition(), me, true));
        }
        if (here == null) {
            e.add(new PickMenu.Entry("🏠 이 땅 사기", String.format("%,d동", nextPrice(me))));
            act.add(() -> buy(p));
        } else if (here.equals(me)) {
            e.add(new PickMenu.Entry("💸 이 땅 팔기", String.format("+%,d동", REFUND)));
            act.add(() -> sell(p));
        }
        int access = mine.getIntOr("access", ACCESS_ALL);
        e.add(new PickMenu.Entry("🚪 출입: " + ACCESS_NAME[access], "바꾸기"));
        act.add(() -> {
            mine.putInt("access", (access + 1) % 3);
            ContentData.get(s).dirty();
        });
        boolean build = mine.getBooleanOr("build", false);
        e.add(new PickMenu.Entry("⛏ 짓기: " + (build ? "모두 허용" : "친구만"), "바꾸기"));
        act.add(() -> {
            mine.putBoolean("build", !build);
            ContentData.get(s).dirty();
        });
        boolean use = mine.getBooleanOr("use", false);
        e.add(new PickMenu.Entry("📦 상자·문 사용: " + (use ? "모두 허용" : "친구만"), "바꾸기"));
        act.add(() -> {
            mine.putBoolean("use", !use);
            ContentData.get(s).dirty();
        });
        ListTag members = mine.getListOrEmpty("members");
        List<String> names = new ArrayList<>();
        for (Tag t : members) if (t instanceof StringTag st) names.add(st.value());
        for (String id : names) {
            String nm = ContentData.get(s).player("land_owner", UUID.fromString(id)).getStringOr("name", null);
            ServerPlayer on = s.getPlayerList().getPlayer(UUID.fromString(id));
            String shown = on != null ? on.getGameProfile().name() : nm != null ? nm : mine.getCompoundOrEmpty("member_names").getStringOr(id, "?");
            e.add(new PickMenu.Entry("👥 친구: " + shown, "빼기"));
            act.add(() -> {
                ListTag l = mine.getListOrEmpty("members");
                l.removeIf(t -> t instanceof StringTag st && st.value().equals(id));
                mine.put("members", l);
                ContentData.get(s).dirty();
            });
        }
        PickMenu.open(p, Component.literal("내 땅 관리"), "친구 추가: /land trust 이름",
                "", e, PickMenu.themed(PickMenu.T_LAND), i -> {
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
