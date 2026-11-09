package com.snolred.wildplus.music;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.net.MusicNet;
import com.snolred.wildplus.registry.ModItems29;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 4.6 음악: 서버 wildplus_music 폴더의 .ogg → 음악 플레이어 · 음반, .mid → 악보 · 피아노 자동 연주 */
public final class Music {
    private static final String ACTIVE = "music_active", CFG = "music_cfg", SONG = "wp_song";
    private static final long MAX_OGG = 40L * 1024 * 1024, MAX_MID = 1024L * 1024;
    private static final int CHUNK = 32_000, PER_TICK = 6;
    private static final int[] VOLS = {25, 50, 75, 100}, RANGES = {16, 32, 64, 128};

    private record Sent(String sha, byte[] data) {}

    private static final class Upload {
        final String sha;
        final byte[] data;
        int next;

        Upload(String sha, byte[] data) {
            this.sha = sha;
            this.data = data;
        }

        int total() {
            return (data.length + CHUNK - 1) / CHUNK;
        }
    }

    private static final Map<UUID, ArrayDeque<Upload>> UPLOADS = new HashMap<>();
    private static final Map<String, Set<UUID>> TOLD = new HashMap<>();
    private static final Map<String, Path> BY_SHA = new HashMap<>();
    private static final Map<String, String> SHA_CACHE = new HashMap<>();
    private static final Map<String, Performance> PIANOS = new HashMap<>();

    private static final class Performance {
        final ServerLevel level;
        final BlockPos pos;
        final List<Midi.Note> notes;
        final String title;
        final long start = System.currentTimeMillis();
        int next;

        Performance(ServerLevel level, BlockPos pos, List<Midi.Note> notes, String title) {
            this.level = level;
            this.pos = pos;
            this.notes = notes;
            this.title = title;
        }
    }

    private Music() {}

    private static Path dir() {
        Path d = FMLPaths.GAMEDIR.get().resolve("wildplus_music");
        try {
            Files.createDirectories(d);
        } catch (IOException ignored) {
        }
        return d;
    }

    /** 폴더의 파일 이름 (.ogg 또는 .mid/.midi) */
    private static List<String> list(boolean midi) {
        List<String> out = new ArrayList<>();
        try (var s = Files.list(dir())) {
            s.forEach(p -> {
                String n = p.getFileName().toString(), l = n.toLowerCase(Locale.ROOT);
                if (midi ? l.endsWith(".mid") || l.endsWith(".midi") : l.endsWith(".ogg")) out.add(n);
            });
        } catch (IOException ignored) {
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** 이름이 폴더 안의 실제 파일일 때만 */
    private static Path file(String name, boolean midi) {
        return list(midi).contains(name) ? dir().resolve(name) : null;
    }

    private static String title(String file) {
        int i = file.lastIndexOf('.');
        return i > 0 ? file.substring(0, i) : file;
    }

    private static String sha(Path p) throws Exception {
        String k = p + "|" + Files.getLastModifiedTime(p).toMillis();
        String c = SHA_CACHE.get(k);
        if (c != null) return c;
        String h = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(p)));
        SHA_CACHE.put(k, h);
        return h;
    }

    /** ogg 길이(초) — 못 읽으면 0 */
    private static double seconds(Path p) {
        try {
            byte[] b = Files.readAllBytes(p);
            int seg = b[26] & 255, id = 27 + seg;
            long rate = (b[id + 12] & 255L) | (b[id + 13] & 255L) << 8 | (b[id + 14] & 255L) << 16 | (b[id + 15] & 255L) << 24;
            for (int i = b.length - 27; i >= 0; i--) {
                if (b[i] == 'O' && b[i + 1] == 'g' && b[i + 2] == 'g' && b[i + 3] == 'S') {
                    long g = 0;
                    for (int k = 7; k >= 0; k--) g = g << 8 | (b[i + 6 + k] & 255L);
                    return rate > 0 && g > 0 ? (double) g / rate : 0;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String keyOf(ServerLevel l, BlockPos p) {
        return l.dimension().identifier() + "|" + p.asLong();
    }

    private static void say(ServerPlayer p, String s, ChatFormatting c) {
        p.sendOverlayMessage(Component.literal(s).withStyle(c));
    }

    // ───────── 음악 플레이어 화면 ─────────
    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel lv = (ServerLevel) p.level();
        ItemStack hand = p.getMainHandItem();
        if (hand.is(ModItems29.CUSTOM_DISC.get()) && !song(hand).isEmpty()) {
            play(p, lv, pos, song(hand));
            return;
        }
        CompoundTag c = cfg(lv, pos);
        CompoundTag act = ContentData.get(lv.getServer()).section(ACTIVE).getCompoundOrEmpty(keyOf(lv, pos));
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("곡 고르기", list(false).size() + "곡 · 폴더의 ogg 파일 재생", new ItemStack(Items.MUSIC_DISC_CAT)));
        e.add(new PickMenu.Entry("정지", act.isEmpty() ? "재생 중이 아니에요" : "재생 중: " + title(act.getStringOr("name", "")), new ItemStack(Items.BARRIER)));
        e.add(new PickMenu.Entry("음량: " + c.getIntOr("vol", 75) + "%", "누를 때마다 변경", new ItemStack(Items.NOTE_BLOCK)));
        e.add(new PickMenu.Entry("들리는 범위: " + c.getIntOr("range", 32) + "칸", "누를 때마다 변경", new ItemStack(Items.ENDER_EYE)));
        e.add(new PickMenu.Entry("반복: " + (c.getBooleanOr("loop", false) ? "켜짐" : "꺼짐"), "누를 때마다 변경", new ItemStack(Items.REPEATER)));
        e.add(new PickMenu.Entry("음반에 곡 담기", "빈 음반을 들고 눌러요", new ItemStack(ModItems29.CUSTOM_DISC.get())));
        e.add(new PickMenu.Entry("악보에 곡 담기", "빈 악보를 들고 눌러요", new ItemStack(ModItems29.SHEET_MUSIC.get())));
        e.add(new PickMenu.Entry("사용법", "음악 · 악보 넣는 법", new ItemStack(Items.BOOK)));
        e.add(new PickMenu.Entry("폴더 열기", "wildplus_music 폴더를 바로 열어요", new ItemStack(Items.CHEST)));
        PickMenu.open(p, Component.literal("🎵 음악 플레이어"), act.isEmpty() ? "멈춰 있어요" : "♪ " + title(act.getStringOr("name", "")), "", e, PickMenu.FLAG_SHOP, i -> {
            switch (i) {
                case 0 -> pickSong(p, lv, pos, false, 0);
                case 1 -> {
                    stop(lv, pos);
                    open(p, pos);
                }
                case 2 -> cycle(lv, pos, "vol", VOLS, 75, p);
                case 3 -> cycle(lv, pos, "range", RANGES, 32, p);
                case 4 -> {
                    c.putBoolean("loop", !c.getBooleanOr("loop", false));
                    ContentData.get(lv.getServer()).dirty();
                    open(p, pos);
                }
                case 5 -> pickSong(p, lv, pos, false, 1);
                case 6 -> pickSong(p, lv, pos, true, 2);
                case 7 -> {
                    p.closeContainer();
                    PacketDistributor.sendToPlayer(p, new MusicNet.Help());
                }
                default -> {
                    p.closeContainer();
                    PacketDistributor.sendToPlayer(p, new MusicNet.OpenDir());
                }
            }
        }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    private static CompoundTag cfg(ServerLevel lv, BlockPos pos) {
        CompoundTag all = ContentData.get(lv.getServer()).section(CFG);
        String k = keyOf(lv, pos);
        if (!(all.get(k) instanceof CompoundTag t)) {
            CompoundTag n = new CompoundTag();
            all.put(k, n);
            return n;
        }
        return t;
    }

    private static void cycle(ServerLevel lv, BlockPos pos, String key, int[] vals, int def, ServerPlayer p) {
        CompoundTag c = cfg(lv, pos);
        int cur = c.getIntOr(key, def), next = vals[0];
        for (int i = 0; i < vals.length; i++) if (vals[i] == cur) next = vals[(i + 1) % vals.length];
        c.putInt(key, next);
        ContentData.get(lv.getServer()).dirty();
        open(p, pos);
    }

    /** mode 0 재생 · 1 음반에 담기 · 2 악보에 담기 */
    private static void pickSong(ServerPlayer p, ServerLevel lv, BlockPos pos, boolean midi, int mode) {
        List<String> names = list(midi);
        List<PickMenu.Entry> e = new ArrayList<>();
        for (String n : names) e.add(new PickMenu.Entry(title(n), n, new ItemStack(midi ? ModItems29.SHEET_MUSIC.get() : Items.MUSIC_DISC_CAT)));
        PickMenu.open(p, Component.literal(midi ? "악보 고르기" : "곡 고르기"), mode == 0 ? "누르면 재생" : "누르면 손에 든 " + (midi ? "악보" : "음반") + "에 담겨요",
                "wildplus_music 폴더에 파일이 없어요. '사용법'을 봐 주세요", e, 0, i -> {
                    if (i < 0 || i >= names.size()) return;
                    String n = names.get(i);
                    if (mode == 0) play(p, lv, pos, n);
                    else write(p, n, midi);
                    p.closeContainer();
                }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    private static void write(ServerPlayer p, String file, boolean midi) {
        ItemStack hand = p.getMainHandItem();
        if (!hand.is(midi ? ModItems29.SHEET_MUSIC.get() : ModItems29.CUSTOM_DISC.get())) {
            say(p, midi ? "빈 악보를 손에 들고 해 주세요" : "빈 음반을 손에 들고 해 주세요", ChatFormatting.RED);
            return;
        }
        ItemStack one = hand.split(1);
        CustomData.update(DataComponents.CUSTOM_DATA, one, t -> t.putString(SONG, file));
        one.set(DataComponents.CUSTOM_NAME, Component.literal((midi ? "악보: " : "음반: ") + title(file)).withStyle(ChatFormatting.AQUA));
        if (!p.getInventory().add(one)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), one);
        say(p, "곡을 담았어요: " + title(file), ChatFormatting.GREEN);
    }

    public static String song(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(SONG, "");
    }

    private static void play(ServerPlayer p, ServerLevel lv, BlockPos pos, String file) {
        Path f = file(file, false);
        if (f == null) {
            say(p, "서버 폴더에 이 곡이 없어요: " + file, ChatFormatting.RED);
            return;
        }
        try {
            if (Files.size(f) > MAX_OGG) {
                say(p, "파일이 너무 커요 (최대 40MB)", ChatFormatting.RED);
                return;
            }
            String key = keyOf(lv, pos), sha = sha(f);
            stop(lv, pos);
            BY_SHA.put(sha, f);
            CompoundTag c = cfg(lv, pos), t = new CompoundTag();
            double sec = seconds(f);
            t.putString("name", file);
            t.putString("dim", lv.dimension().identifier().toString());
            t.putLong("pos", pos.asLong());
            t.putString("sha", sha);
            t.putLong("size", Files.size(f));
            t.putFloat("vol", c.getIntOr("vol", 75) / 100F);
            t.putInt("range", c.getIntOr("range", 32));
            t.putBoolean("loop", c.getBooleanOr("loop", false));
            t.putLong("end", sec > 0 ? System.currentTimeMillis() + (long) (sec * 1000) + 3000 : 0L);
            ContentData d = ContentData.get(lv.getServer());
            d.section(ACTIVE).put(key, t);
            d.dirty();
            TOLD.remove(key);
            p.sendSystemMessage(Component.literal("🎵 재생: " + title(file)).withStyle(ChatFormatting.AQUA));
        } catch (Exception e) {
            say(p, "파일을 읽지 못했어요", ChatFormatting.RED);
        }
    }

    public static void stop(ServerLevel lv, BlockPos pos) {
        String key = keyOf(lv, pos);
        ContentData d = ContentData.get(lv.getServer());
        d.section(ACTIVE).remove(key);
        d.dirty();
        TOLD.remove(key);
        PacketDistributor.sendToAllPlayers(new MusicNet.Stop(key));
    }

    public static void request(ServerPlayer p, String sha) {
        Path f = BY_SHA.get(sha);
        if (f == null) return;
        try {
            UPLOADS.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>()).add(new Upload(sha, Files.readAllBytes(f)));
        } catch (IOException ignored) {
        }
    }

    // ───────── 피아노 자동 연주 ─────────
    /** 손에 악보가 있으면 연주를 시작하고 true */
    public static boolean tryPiano(ServerPlayer p, ServerLevel lv, BlockPos m) {
        ItemStack hand = p.getMainHandItem();
        if (!hand.is(ModItems29.SHEET_MUSIC.get())) return false;
        String file = song(hand);
        if (file.isEmpty()) {
            say(p, "빈 악보예요. 음악 플레이어에서 곡을 담아 주세요", ChatFormatting.RED);
            return true;
        }
        String key = keyOf(lv, m);
        if (PIANOS.containsKey(key)) {
            PIANOS.remove(key);
            say(p, "연주를 멈췄어요", ChatFormatting.YELLOW);
            return true;
        }
        Path f = file(file, true);
        try {
            if (f == null || Files.size(f) > MAX_MID) {
                say(p, "서버 폴더에 이 악보가 없어요: " + file, ChatFormatting.RED);
                return true;
            }
            List<Midi.Note> notes = Midi.read(f.toFile());
            if (notes.isEmpty()) {
                say(p, "악보에 음이 없어요", ChatFormatting.RED);
                return true;
            }
            PIANOS.put(key, new Performance(lv, m.immutable(), notes, title(file)));
            p.sendSystemMessage(Component.literal("🎹 자동 연주: " + title(file) + " (악보를 들고 다시 누르면 멈춤)").withStyle(ChatFormatting.AQUA));
        } catch (Exception e) {
            say(p, "악보를 읽지 못했어요 (MIDI 파일인지 확인)", ChatFormatting.RED);
        }
        return true;
    }

    /** 또렷한 타건: 하프 몸통 + 플링 어택. 음역에 따라 베이스 · 하프 · 벨로 나눠 실제 음높이를 맞춘다 */
    private static void strike(ServerLevel lv, BlockPos m, Midi.Note n) {
        int key = n.key();
        while (key < 30) key += 12;
        while (key > 102) key -= 12;
        var body = key < 54 ? SoundEvents.NOTE_BLOCK_BASS : key > 78 ? SoundEvents.NOTE_BLOCK_BELL : SoundEvents.NOTE_BLOCK_HARP;
        int root = key < 54 ? 42 : key > 78 ? 90 : 66;
        float pitch = (float) Math.pow(2.0, (key - root) / 12.0);
        lv.playSound(null, m, body.value(), SoundSource.RECORDS, 1.6F * n.volume(), pitch);
        if (key >= 54 && key <= 78) lv.playSound(null, m, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS, 0.55F * n.volume(), pitch);
        lv.sendParticles(ParticleTypes.NOTE, m.getX() + 0.5 + lv.getRandom().nextDouble() - 0.5, m.getY() + 2.1, m.getZ() + 0.5, 0, (key % 24) / 24.0, 0, 0, 1);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        tickUploads(s);
        tickPianos();
        if (s.getTickCount() % 20 == 3) tickActive(s);
    }

    private static void tickUploads(MinecraftServer s) {
        UPLOADS.entrySet().removeIf(en -> {
            ServerPlayer p = s.getPlayerList().getPlayer(en.getKey());
            if (p == null) return true;
            ArrayDeque<Upload> q = en.getValue();
            int left = PER_TICK;
            while (left > 0 && !q.isEmpty()) {
                Upload u = q.peek();
                int from = u.next * CHUNK, to = Math.min(u.data.length, from + CHUNK);
                PacketDistributor.sendToPlayer(p, new MusicNet.Chunk(u.sha, u.next, u.total(), java.util.Arrays.copyOfRange(u.data, from, to)));
                u.next++;
                left--;
                if (u.next >= u.total()) q.poll();
            }
            return q.isEmpty();
        });
    }

    private static void tickPianos() {
        long now = System.currentTimeMillis();
        PIANOS.values().removeIf(perf -> {
            Block b = perf.level.getBlockState(perf.pos).getBlock();
            if (!(b instanceof com.snolred.wildplus.block.FurnitureBlock)) return true;
            long at = now - perf.start;
            int played = 0;
            while (perf.next < perf.notes.size() && perf.notes.get(perf.next).ms() <= at && played < 16) {
                strike(perf.level, perf.pos, perf.notes.get(perf.next++));
                played++;
            }
            return perf.next >= perf.notes.size();
        });
    }

    private static void tickActive(MinecraftServer s) {
        ContentData d = ContentData.get(s);
        CompoundTag act = d.section(ACTIVE);
        for (String key : new ArrayList<>(act.keySet())) {
            if (!(act.get(key) instanceof CompoundTag t)) continue;
            BlockPos pos = BlockPos.of(t.getLongOr("pos", 0L));
            ServerLevel lv = null;
            for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(t.getStringOr("dim", ""))) lv = l;
            long end = t.getLongOr("end", 0L);
            boolean over = !t.getBooleanOr("loop", false) && end > 0 && System.currentTimeMillis() > end;
            boolean gone = lv == null || lv.isLoaded(pos) && !lv.getBlockState(pos).is(com.snolred.wildplus.registry.ModBlocks18.MUSIC_PLAYER.get());
            if (over || gone) {
                act.remove(key);
                d.dirty();
                TOLD.remove(key);
                PacketDistributor.sendToAllPlayers(new MusicNet.Stop(key));
                continue;
            }
            Path f = file(t.getStringOr("name", ""), false);
            if (f != null) BY_SHA.put(t.getStringOr("sha", ""), f);
            Set<UUID> told = TOLD.computeIfAbsent(key, k -> new HashSet<>());
            int range = t.getIntOr("range", 32);
            for (ServerPlayer p : lv.players()) {
                if (told.contains(p.getUUID()) || !p.blockPosition().closerThan(pos, range + 32)) continue;
                told.add(p.getUUID());
                PacketDistributor.sendToPlayer(p, new MusicNet.Play(key, t.getStringOr("sha", ""), t.getLongOr("size", 0L), pos.asLong(), t.getFloatOr("vol", 0.75F), range, t.getBooleanOr("loop", false)));
            }
        }
    }
}
