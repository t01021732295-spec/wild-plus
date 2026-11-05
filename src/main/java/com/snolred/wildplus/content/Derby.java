package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.5 경마장.
 * <ul>
 *   <li>마구간: 말을 타고 우클릭하면 내 경주마로 등록 (그 말의 속도 · 점프 · 체력이 능력치가 됨) · 훈련 · 먹이 · 휴식 · 출전 신청</li>
 *   <li>경주: 게임 속 매일 오후 3시. 출전 신청한 친구들의 말 + 마을 말로 6마리. 출발 게이트가 있으면 말들이 실제로 주로를 한 바퀴 달린다</li>
 *   <li>마권: 오전 6시 ~ 오후 3시에 단승(1등) · 연승(2등 안) · 복연승(3등 안). 배당은 능력치로 정해지고 산 순간 고정</li>
 * </ul>
 */
public final class Derby {
    private Derby() {}

    public static final long RACE_TOD = 9000; // 오후 3시
    public static final long[] CHIPS = {1_000, 5_000, 10_000, 50_000};
    public static final String[] TYPES = {"단승 (1등)", "연승 (2등 안)", "복연승 (3등 안)"};
    public static final long[] PRIZE = {20_000, 10_000, 5_000};
    private static final String[] NPC = {"초록바람", "황금갈기", "보라돌", "흑진주", "은하수", "질풍", "새벽별", "붉은노을", "구름발", "모래폭풍"};

    // ───────── 내 경주마 ─────────
    private static long today(MinecraftServer s) {
        return s.overworld().getOverworldClockTime() / 24000L;
    }

    private static long tod(MinecraftServer s) {
        return Math.floorMod(s.overworld().getOverworldClockTime(), 24000L);
    }

    /** 내 말 (하루가 지나면 배고픔 · 컨디션이 줄고 훈련 횟수 초기화) */
    public static CompoundTag horse(MinecraftServer s, UUID id) {
        CompoundTag h = ContentData.get(s).player("derby_horse", id);
        long day = today(s);
        long last = h.getLongOr("last_day", day);
        if (day > last && h.getBooleanOr("reg", false)) {
            int d = (int) Math.min(10, day - last);
            h.putInt("cond", Math.max(0, h.getIntOr("cond", 3) - d));
            h.putInt("hunger", Math.max(0, h.getIntOr("hunger", 80) - 30 * d));
            h.putInt("train_n", 0);
        }
        h.putLong("last_day", day);
        return h;
    }

    private static double gain(double v, double base) {
        return Math.max(0.2, base * (1 - v / 110.0));
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) {
        p.sendOverlayMessage(Component.literal(s).withStyle(c));
    }

    /** 마구간 화면 버튼: 0 먹이 · 1 달리기 · 2 장애물 · 3 휴식 · 4 등록 · 5 출전 신청 */
    private static void stableAction(ServerPlayer p, int id) {
        MinecraftServer s = p.level().getServer();
        CompoundTag h = horse(s, p.getUUID());
        ContentData data = ContentData.get(s);
        if (id == 4) {
            if (!(p.getVehicle() instanceof LivingEntity mount) || !(mount.getType() == EntityTypes.HORSE || mount.getType() == EntityTypes.DONKEY
                    || mount.getType() == EntityTypes.MULE)) {
                msg(p, "🐴 말을 타고 마구간을 눌러야 등록돼요", ChatFormatting.YELLOW);
                return;
            }
            double spd = mount.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), jmp = mount.getAttributeBaseValue(Attributes.JUMP_STRENGTH),
                    hp = mount.getAttributeBaseValue(Attributes.MAX_HEALTH);
            h.putBoolean("reg", true);
            h.putString("name", mount.hasCustomName() ? mount.getCustomName().getString() : p.getName().getString() + "의 말");
            h.putDouble("sp", clamp(30 + (spd - 0.1125) / 0.225 * 60));
            h.putDouble("ag", clamp(30 + (jmp - 0.4) / 0.6 * 60));
            h.putDouble("st", clamp(30 + (hp - 15) / 15 * 60));
            h.putDouble("gu", 30 + p.getRandom().nextInt(30));
            h.putInt("cond", 4);
            h.putInt("hunger", 80);
            h.putInt("coat", Math.floorMod(mount.getUUID().hashCode(), 6));
            data.dirty();
            p.level().playSound(null, p.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
            p.sendSystemMessage(Component.literal("🐴 " + h.getStringOr("name", "") + " 을(를) 내 경주마로 등록했어요! (이름표로 이름을 지으면 그 이름)").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!h.getBooleanOr("reg", false)) {
            msg(p, "먼저 말을 타고 마구간을 눌러 내 경주마로 등록해 주세요", ChatFormatting.YELLOW);
            return;
        }
        switch (id) {
            case 0 -> {
                var inv = p.getInventory();
                ItemStack[] pick = {ItemStack.EMPTY};
                int[] food = {0, 0};
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack st = inv.getItem(i);
                    int[] f = st.is(Items.GOLDEN_CARROT) ? new int[]{40, 1} : st.is(Items.HAY_BLOCK) ? new int[]{30, 0} : st.is(Items.CARROT) ? new int[]{15, 0}
                            : st.is(Items.APPLE) ? new int[]{12, 0} : null;
                    if (f != null && f[0] + f[1] * 100 > food[0] + food[1] * 100) {
                        food = f;
                        pick[0] = st;
                    }
                }
                if (pick[0].isEmpty()) {
                    msg(p, "🥕 먹이가 없어요 (당근 · 황금 당근 · 건초 더미 · 사과)", ChatFormatting.YELLOW);
                    return;
                }
                if (h.getIntOr("hunger", 0) >= 100) {
                    msg(p, "배불러서 더 못 먹어요", ChatFormatting.GRAY);
                    return;
                }
                pick[0].shrink(1);
                h.putInt("hunger", Math.min(100, h.getIntOr("hunger", 0) + food[0]));
                if (food[1] > 0) h.putInt("cond", Math.min(5, h.getIntOr("cond", 0) + 1));
                p.level().playSound(null, p.blockPosition(), SoundEvents.HORSE_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
                msg(p, String.format("🥕 냠냠! 배고픔 %d%s", h.getIntOr("hunger", 0), food[1] > 0 ? " · 컨디션 +1" : ""), ChatFormatting.GREEN);
            }
            case 1, 2 -> {
                long cost = id == 1 ? 1_000 : 2_000;
                if (h.getIntOr("train_n", 0) >= 3) {
                    msg(p, "오늘 훈련은 다 했어요 (하루 3번)", ChatFormatting.GRAY);
                    return;
                }
                if (h.getIntOr("hunger", 0) < 20) {
                    msg(p, "배고파서 훈련을 못 해요 · 먹이를 주세요", ChatFormatting.YELLOW);
                    return;
                }
                if (!Honor.pay(p, cost)) {
                    msg(p, String.format("돈이 모자라요 (%,d동)", cost), ChatFormatting.RED);
                    return;
                }
                if (id == 1) {
                    h.putDouble("sp", clamp(h.getDoubleOr("sp", 40) + gain(h.getDoubleOr("sp", 40), 2)));
                    h.putDouble("st", clamp(h.getDoubleOr("st", 40) + gain(h.getDoubleOr("st", 40), 1)));
                } else {
                    h.putDouble("ag", clamp(h.getDoubleOr("ag", 40) + gain(h.getDoubleOr("ag", 40), 2)));
                    h.putDouble("gu", clamp(h.getDoubleOr("gu", 40) + gain(h.getDoubleOr("gu", 40), 1)));
                }
                h.putInt("train_n", h.getIntOr("train_n", 0) + 1);
                h.putInt("hunger", Math.max(0, h.getIntOr("hunger", 0) - 15));
                h.putInt("xp", h.getIntOr("xp", 0) + 8);
                p.level().playSound(null, p.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.6, 0.1, 0.6, 0.02);
                msg(p, id == 1 ? "🏃 주로 달리기! 속도 · 지구력 ↑" : "🚧 장애물 연습! 순발력 · 근성 ↑", ChatFormatting.AQUA);
            }
            case 3 -> {
                if (h.getLongOr("rest_day", -1) == today(s)) {
                    msg(p, "오늘은 이미 푹 쉬었어요", ChatFormatting.GRAY);
                    return;
                }
                h.putLong("rest_day", today(s));
                h.putInt("cond", Math.min(5, h.getIntOr("cond", 0) + 2));
                msg(p, "😴 푹 쉬었어요 · 컨디션 +2", ChatFormatting.LIGHT_PURPLE);
            }
            case 5 -> {
                if (raceDone(s)) {
                    msg(p, "오늘 경주는 끝났어요 · 내일 오후 3시에 다시", ChatFormatting.GRAY);
                    return;
                }
                if (h.getIntOr("hunger", 0) < 10) {
                    msg(p, "배고파서 출전할 수 없어요", ChatFormatting.YELLOW);
                    return;
                }
                ListTag e = entries(s);
                for (Tag t : e) if (t.asString().orElse("").equals(p.getUUID().toString())) {
                    msg(p, "이미 오늘 경주에 출전 신청했어요", ChatFormatting.GRAY);
                    return;
                }
                if (e.size() >= 6) {
                    msg(p, "오늘 경주는 6마리가 다 찼어요", ChatFormatting.YELLOW);
                    return;
                }
                e.add(net.minecraft.nbt.StringTag.valueOf(p.getUUID().toString()));
                p.sendSystemMessage(Component.literal("🏁 " + h.getStringOr("name", "") + " 오늘 오후 3시 경주에 출전 신청!").withStyle(ChatFormatting.GOLD));
            }
            default -> {}
        }
        data.dirty();
    }

    private static double clamp(double v) {
        return Math.max(1, Math.min(100, v));
    }

    // ───────── 오늘 경주 ─────────
    private static CompoundTag sec(MinecraftServer s) {
        return ContentData.get(s).section("derby");
    }

    private static boolean raceDone(MinecraftServer s) {
        return sec(s).getLongOr("race_day", -1) == today(s);
    }

    private static boolean betsOpen(MinecraftServer s) {
        return !raceDone(s) && tod(s) < RACE_TOD;
    }

    private static ListTag entries(MinecraftServer s) {
        CompoundTag d = sec(s);
        if (d.getLongOr("entry_day", -1) != today(s)) {
            d.putLong("entry_day", today(s));
            d.put("entries", new ListTag());
        }
        ListTag l = d.getListOrEmpty("entries");
        d.put("entries", l);
        return l;
    }

    public record Runner(String name, String owner, UUID ownerId, double sp, double st, double ag, double gu, int cond, int coat, String recent) {
        double rating() {
            return sp * 0.45 + st * 0.30 + ag * 0.15 + gu * 0.10 + cond * 3;
        }
    }

    /** 오늘 출전 6마리 (신청한 친구 말 + 마을 말) */
    public static List<Runner> field(MinecraftServer s) {
        List<Runner> out = new ArrayList<>();
        for (Tag t : entries(s)) {
            UUID id = UUID.fromString(t.asString().orElse(new UUID(0, 0).toString()));
            CompoundTag h = horse(s, id);
            if (!h.getBooleanOr("reg", false)) continue;
            String owner = WildData.get(s).names.getOrDefault(id, "친구");
            out.add(new Runner(h.getStringOr("name", "말"), owner, id, h.getDoubleOr("sp", 40), h.getDoubleOr("st", 40), h.getDoubleOr("ag", 40),
                    h.getDoubleOr("gu", 40), h.getIntOr("cond", 3), h.getIntOr("coat", 0), h.getStringOr("recent", "-")));
        }
        long day = today(s);
        Random r = new Random(day * 7919L + 3);
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < NPC.length; i++) pool.add(i);
        java.util.Collections.shuffle(pool, r);
        for (int k = 0; out.size() < 6; k++) {
            int i = pool.get(k);
            Random hr = new Random(i * 104729L + Math.floorDiv(day, 7));
            double base = 45 + i * 3.2;
            out.add(new Runner(NPC[i], "마을", null, base + hr.nextInt(15), base + hr.nextInt(15), base + hr.nextInt(12), base + hr.nextInt(12),
                    2 + r.nextInt(3), i % 6, (1 + hr.nextInt(6)) + "-" + (1 + hr.nextInt(6)) + "-" + (1 + hr.nextInt(6))));
        }
        return out;
    }

    /** 단승 배당 (능력치 확률의 0.85) */
    public static double[] odds(List<Runner> f) {
        double[] w = new double[f.size()];
        double sum = 0;
        for (int i = 0; i < f.size(); i++) sum += w[i] = Math.exp(f.get(i).rating() / 6.0);
        double[] o = new double[f.size()];
        for (int i = 0; i < f.size(); i++) o[i] = Math.round(Math.max(1.2, Math.min(50, 0.85 / (w[i] / sum))) * 10) / 10.0;
        return o;
    }

    public static double payoutOdds(double win, int type) {
        return type == 0 ? win : type == 1 ? Math.max(1.1, 1 + (win - 1) * 0.4) : Math.max(1.05, 1 + (win - 1) * 0.25);
    }

    // ───────── 경주 진행 (말들이 주로를 달림) ─────────
    private static final List<Entity> RUNNERS = new ArrayList<>();
    private static double[] finish = new double[0];
    private static List<Runner> raceField = List.of();
    private static int raceTick = -1;
    private static BlockPos gate;
    private static Direction gateFacing = Direction.NORTH;
    private static ServerLevel raceLevel;

    /** 출발 게이트가 놓이면 경주 길을 기억 */
    public static void registerGate(ServerLevel level, BlockPos pos, Direction facing) {
        CompoundTag d = sec(level.getServer());
        d.putLong("gate", pos.asLong());
        d.putInt("gate_facing", facing.get2DDataValue());
        d.putString("gate_dim", level.dimension().identifier().toString());
        ContentData.get(level.getServer()).dirty();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (raceTick >= 0) {
            raceTick++;
            move();
            int end = (int) java.util.Arrays.stream(finish).max().orElse(0) + 60;
            if (raceTick >= end) endRace(s);
            return;
        }
        if (s.getTickCount() % 20 != 0) return;
        long tod = tod(s);
        CompoundTag d = sec(s);
        if (tod >= RACE_TOD - 1000 && tod < RACE_TOD && d.getLongOr("warn_day", -1) != today(s)) {
            d.putLong("warn_day", today(s));
            s.getPlayerList().broadcastSystemMessage(Component.literal("🏇 경주 1시간 전 (실제 50초)! 마권 발매기에서 마권을 사세요 · 출전 신청은 마구간에서").withStyle(ChatFormatting.GOLD), false);
        }
        if (tod >= RACE_TOD && tod < RACE_TOD + 3000 && !raceDone(s)) startRace(s);
    }

    private static void startRace(MinecraftServer s) {
        CompoundTag d = sec(s);
        d.putLong("race_day", today(s));
        ContentData.get(s).dirty();
        raceField = field(s);
        Random r = new Random();
        finish = new double[raceField.size()];
        for (int i = 0; i < raceField.size(); i++) {
            double perf = raceField.get(i).rating() + r.nextGaussian() * 5;
            finish[i] = Math.max(420, 900 - perf * 4);
        }
        raceTick = 0;
        RUNNERS.clear();
        gate = null;
        raceLevel = null;
        if (d.contains("gate")) {
            ServerLevel lv = null;
            for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(d.getStringOr("gate_dim", ""))) lv = l;
            BlockPos g = BlockPos.of(d.getLongOr("gate", 0));
            if (lv != null && lv.isLoaded(g) && lv.getBlockState(g).getBlock() instanceof com.snolred.wildplus.block.BigBlock) {
                gate = g;
                gateFacing = Direction.from2DDataValue(d.getIntOr("gate_facing", 2));
                raceLevel = lv;
                for (int i = 0; i < raceField.size(); i++) {
                    var h = EntityTypes.HORSE.create(lv, EntitySpawnReason.EVENT);
                    if (h == null) continue;
                    h.setNoAi(true);
                    h.setCustomName(Component.literal((i + 1) + "번 " + raceField.get(i).name()));
                    h.setCustomNameVisible(true);
                    h.getPersistentData().putBoolean("wp_race", true);
                    Pos p = pos(i, 0);
                    h.snapTo(p.x, gate.getY(), p.z, p.yaw, 0);
                    lv.addFreshEntity(h);
                    RUNNERS.add(h);
                }
                lv.playSound(null, gate, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
        }
        StringBuilder sb = new StringBuilder("🏇 출발! ");
        for (int i = 0; i < raceField.size(); i++) sb.append(i + 1).append("번 ").append(raceField.get(i).name()).append(i < raceField.size() - 1 ? " · " : "");
        s.getPlayerList().broadcastSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private record Pos(double x, double z, float yaw) {}

    /** 타원 경주로 위 위치: 게이트에서 왼쪽 10칸이 가운데, 앞(게이트 방향)이 긴 지름 */
    private static Pos pos(int lane, double frac) {
        double th = 2 * Math.PI * Math.min(1.0, frac);
        double rx = 8.9 + lane * 0.38, rz = rx + 8;
        Direction left = gateFacing.getCounterClockWise(), right = gateFacing.getClockWise();
        double cx = gate.getX() + 0.5 + left.getStepX() * 10, cz = gate.getZ() + 0.5 + left.getStepZ() * 10;
        double a = rx * Math.cos(th), b = rz * Math.sin(th);
        double x = cx + right.getStepX() * a + gateFacing.getStepX() * b, z = cz + right.getStepZ() * a + gateFacing.getStepZ() * b;
        double da = -rx * Math.sin(th), db = rz * Math.cos(th);
        double dx = right.getStepX() * da + gateFacing.getStepX() * db, dz = right.getStepZ() * da + gateFacing.getStepZ() * db;
        return new Pos(x, z, (float) Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private static void move() {
        if (gate == null || raceLevel == null) return;
        for (int i = 0; i < RUNNERS.size() && i < finish.length; i++) {
            Entity h = RUNNERS.get(i);
            if (h.isRemoved()) continue;
            double frac = raceTick / finish[i];
            double wob = Math.sin(raceTick * 0.05 + i * 1.7) * 0.012 * Math.min(1, Math.max(0, 1 - frac));
            Pos p = pos(i, Math.min(1, frac + wob));
            h.setPos(p.x, gate.getY(), p.z);
            h.setYRot(p.yaw);
            h.setYHeadRot(p.yaw);
            if (raceTick % 6 == i % 6 && frac < 1) raceLevel.sendParticles(ParticleTypes.CLOUD, p.x, gate.getY() + 0.1, p.z, 1, 0.1, 0, 0.1, 0.01);
        }
        if (raceTick % 40 == 0) raceLevel.playSound(null, gate, SoundEvents.HORSE_GALLOP, SoundSource.NEUTRAL, 1.5F, 1.0F);
    }

    private static void endRace(MinecraftServer s) {
        raceTick = -1;
        for (Entity e : RUNNERS) e.discard();
        RUNNERS.clear();
        Integer[] order = new Integer[raceField.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, Comparator.comparingDouble(i -> finish[i]));
        int[] place = new int[order.length];
        for (int k = 0; k < order.length; k++) place[order[k]] = k + 1;
        double[] od = odds(raceField);
        StringBuilder res = new StringBuilder();
        s.getPlayerList().broadcastSystemMessage(Component.literal("🏁 경주 끝! 결과").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        String[] medal = {"🥇", "🥈", "🥉"};
        WildData wd = WildData.get(s);
        for (int k = 0; k < order.length; k++) {
            Runner r = raceField.get(order[k]);
            res.append(k > 0 ? "\n" : "").append(order[k] + 1).append('\t').append(r.name()).append('\t').append(r.owner());
            String line = String.format("%s %d등 %d번 %s (%s) · 배당 %.1f배", k < 3 ? medal[k] : " ", k + 1, order[k] + 1, r.name(), r.owner(), od[order[k]]);
            s.getPlayerList().broadcastSystemMessage(Component.literal(line).withStyle(k == 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE), false);
            if (r.ownerId() != null) {
                CompoundTag h = horse(s, r.ownerId());
                h.putInt("starts", h.getIntOr("starts", 0) + 1);
                if (k < 3) {
                    h.putInt("w" + (k + 1), h.getIntOr("w" + (k + 1), 0) + 1);
                    h.putLong("prize", h.getLongOr("prize", 0) + PRIZE[k]);
                    wd.addBalance(r.ownerId(), PRIZE[k]);
                    s.getPlayerList().broadcastSystemMessage(Component.literal(String.format("   💰 %s 상금 %,d동", r.owner(), PRIZE[k])).withStyle(ChatFormatting.GREEN), false);
                }
                h.putInt("xp", h.getIntOr("xp", 0) + new int[]{60, 40, 25, 10, 10, 10}[k]);
                String rec = h.getStringOr("recent", "");
                h.putString("recent", ((k + 1) + (rec.isEmpty() || rec.equals("-") ? "" : "-" + rec)).replaceAll("^((?:\\d-){2}\\d).*", "$1"));
                h.putInt("hunger", Math.max(0, h.getIntOr("hunger", 0) - 20));
            }
        }
        sec(s).putString("last", res.toString());
        // 마권 정산
        ListTag bets = sec(s).getListOrEmpty("bets");
        ListTag keep = new ListTag();
        long day = today(s);
        for (Tag t : bets) {
            CompoundTag b = (CompoundTag) t;
            if (b.getLongOr("day", -1) != day) continue;
            int pl = place[Math.max(0, Math.min(place.length - 1, b.getIntOr("horse", 0)))];
            int type = b.getIntOr("type", 0);
            boolean hit = type == 0 ? pl == 1 : type == 1 ? pl <= 2 : pl <= 3;
            if (!hit) continue;
            long win = Math.round(b.getLongOr("amount", 0) * b.getDoubleOr("odds", 1));
            UUID by = UUID.fromString(b.getStringOr("by", new UUID(0, 0).toString()));
            wd.addBalance(by, win);
            ServerPlayer p = s.getPlayerList().getPlayer(by);
            if (p != null) {
                p.sendSystemMessage(Component.literal(String.format("🎫 마권 적중! %d번 %s · %s · %,d동 (통장)", b.getIntOr("horse", 0) + 1,
                        raceField.get(b.getIntOr("horse", 0)).name(), TYPES[type], win)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
        }
        sec(s).put("bets", keep);
        ContentData.get(s).dirty();
        if (gate != null && raceLevel != null) Contest.fireworks(raceLevel, gate, 3);
    }

    private static void bet(ServerPlayer p, int horse, int type, int chip) {
        MinecraftServer s = p.level().getServer();
        if (!betsOpen(s)) {
            msg(p, "🎫 지금은 마권을 살 수 없어요 (오전 6시 ~ 오후 3시)", ChatFormatting.YELLOW);
            return;
        }
        List<Runner> f = field(s);
        if (horse < 0 || horse >= f.size() || type < 0 || type > 2 || chip < 0 || chip >= CHIPS.length) return;
        ListTag bets = sec(s).getListOrEmpty("bets");
        int mine = 0;
        for (Tag t : bets) if (((CompoundTag) t).getStringOr("by", "").equals(p.getUUID().toString()) && ((CompoundTag) t).getLongOr("day", -1) == today(s)) mine++;
        if (mine >= 10) {
            msg(p, "한 경주에 마권은 10장까지", ChatFormatting.YELLOW);
            return;
        }
        long amount = CHIPS[chip];
        if (!Honor.pay(p, amount)) {
            msg(p, String.format("돈이 모자라요 (%,d동)", amount), ChatFormatting.RED);
            return;
        }
        double o = payoutOdds(odds(f)[horse], type);
        CompoundTag b = new CompoundTag();
        b.putString("by", p.getUUID().toString());
        b.putLong("day", today(s));
        b.putInt("horse", horse);
        b.putInt("type", type);
        b.putLong("amount", amount);
        b.putDouble("odds", o);
        bets.add(b);
        sec(s).put("bets", bets);
        ContentData.get(s).dirty();
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.sendSystemMessage(Component.literal(String.format("🎫 마권: %d번 %s · %s · %,d동 × %.1f배 = %,d동", horse + 1, f.get(horse).name(), TYPES[type], amount, o,
                Math.round(amount * o))).withStyle(ChatFormatting.GOLD));
    }

    // ───────── 화면 ─────────
    private static String status(MinecraftServer s) {
        if (raceTick >= 0) return "1";
        if (raceDone(s)) return "2";
        return "0";
    }

    public static void openBetting(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        List<Runner> f = field(s);
        double[] od = odds(f);
        long left = Math.max(0, (RACE_TOD - tod(s)) / 20);
        int myN = 0;
        long myAmt = 0;
        for (Tag t : sec(s).getListOrEmpty("bets")) {
            CompoundTag b = (CompoundTag) t;
            if (b.getStringOr("by", "").equals(p.getUUID().toString()) && b.getLongOr("day", -1) == today(s)) {
                myN++;
                myAmt += b.getLongOr("amount", 0);
            }
        }
        String weather = p.level().isRaining() ? "비 · 주로 무거움" : "맑음 · 주로 양호";
        List<PickMenu.Entry> rows = new ArrayList<>();
        rows.add(new PickMenu.Entry("제 " + (today(s) + 1) + " 경주", String.join("\n", status(s), String.valueOf(left), weather,
                myN == 0 ? "아직 산 마권이 없어요" : String.format("내 마권 %d장 · %,d동", myN, myAmt), sec(s).getStringOr("last", "").replace("\n", " | ").replace("\t", " "),
                sec(s).contains("gate") ? "1" : "0")));
        for (int i = 0; i < f.size(); i++) {
            Runner r = f.get(i);
            rows.add(new PickMenu.Entry(r.name(), String.join("\n", r.owner(), String.valueOf((int) r.sp()), String.valueOf((int) r.st()), r.recent(),
                    String.format("%.1f", od[i]), String.valueOf(i), p.getUUID().equals(r.ownerId()) ? "1" : "0"),
                    new ItemStack(com.snolred.wildplus.registry.ModItems18.RACE_HORSES.get(r.coat() % 6).get())));
        }
        PickMenu.open(p, Component.literal("🏇 야생 경마장"), "", "", rows, PickMenu.FLAG_DERBY_BET, id -> {
            if (id >= 1000) {
                int k = id - 1000;
                bet(p, k / 100, (k / 10) % 10, k % 10);
                openBetting(p, pos);
            }
        }, null, pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }

    public static void openStable(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        CompoundTag h = horse(s, p.getUUID());
        boolean entered = false;
        for (Tag t : entries(s)) if (t.asString().orElse("").equals(p.getUUID().toString())) entered = true;
        int xp = h.getIntOr("xp", 0);
        String race = raceDone(s) ? "오늘 경주 끝 · 내일 오후 3시" : String.format("다음 경주 오후 3시 (%d초 뒤)", Math.max(0, (RACE_TOD - tod(s)) / 20));
        List<PickMenu.Entry> rows = new ArrayList<>();
        rows.add(new PickMenu.Entry(h.getStringOr("name", "아직 없음"), String.join("\n", h.getBooleanOr("reg", false) ? "1" : "0",
                String.valueOf((int) h.getDoubleOr("sp", 0)), String.valueOf((int) h.getDoubleOr("st", 0)), String.valueOf((int) h.getDoubleOr("ag", 0)),
                String.valueOf((int) h.getDoubleOr("gu", 0)), String.valueOf(h.getIntOr("cond", 0)), String.valueOf(h.getIntOr("hunger", 0)),
                String.valueOf(1 + xp / 100), String.valueOf(xp % 100), String.valueOf(h.getIntOr("starts", 0)), String.valueOf(h.getIntOr("w1", 0)),
                String.valueOf(h.getIntOr("w2", 0)), String.valueOf(h.getLongOr("prize", 0)), String.valueOf(h.getIntOr("train_n", 0)), entered ? "1" : "0", race,
                String.valueOf(h.getIntOr("w3", 0))),
                new ItemStack(com.snolred.wildplus.registry.ModItems18.RACE_HORSES.get(h.getIntOr("coat", 0) % 6).get())));
        for (int i = 0; i < 6; i++) rows.add(new PickMenu.Entry("", ""));
        PickMenu.open(p, Component.literal("🐴 마구간"), "", "", rows, PickMenu.FLAG_STABLE, id -> {
            stableAction(p, id);
            openStable(p, pos);
        }, null, pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }

    /** 전광판: 오늘 경주표 · 배당 · 지난 결과 */
    public static void openBoard(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        List<Runner> f = field(s);
        double[] od = odds(f);
        p.sendSystemMessage(Component.literal("━━ 🏇 제 " + (today(s) + 1) + " 경주 · 오후 3시 ━━").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 0; i < f.size(); i++) {
            p.sendSystemMessage(Component.literal(String.format("%d번 %s (%s) · 배당 %.1f배 · 최근 %s", i + 1, f.get(i).name(), f.get(i).owner(), od[i], f.get(i).recent()))
                    .withStyle(ChatFormatting.WHITE));
        }
        String last = sec(s).getStringOr("last", "");
        if (!last.isEmpty()) {
            p.sendSystemMessage(Component.literal("지난 경주").withStyle(ChatFormatting.GRAY));
            String[] ls = last.split("\n");
            for (int k = 0; k < Math.min(3, ls.length); k++) {
                String[] q = ls[k].split("\t");
                p.sendSystemMessage(Component.literal((k + 1) + "등 " + q[0] + "번 " + (q.length > 1 ? q[1] : "")).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** 경주 중인 말은 다치지 않음 */
    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr("wp_race", false)) event.setCanceled(true);
    }

    // ───────── 시험 명령: /derby race ─────────
    @SubscribeEvent
    public static void onCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("derby")
                .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(net.minecraft.commands.Commands.literal("race").executes(ctx -> {
                    MinecraftServer s = ctx.getSource().getServer();
                    if (raceTick < 0) {
                        sec(s).putLong("race_day", -1);
                        startRace(s);
                    }
                    return 1;
                })));
    }
}
