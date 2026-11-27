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
    private static final Map<String, UUID> VISUALS = new HashMap<>();

    private static final class Performance {
        final ServerLevel level;
        final BlockPos pos;
        final List<Midi.Note> notes;
        final String title;
        final long start = System.currentTimeMillis();
        int next, shift;

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
        e.add(new PickMenu.Entry("플레이리스트 (" + playlist(c).size() + "곡)", "곡을 모아 차례로 재생 · 순서 바꾸기", new ItemStack(Items.WRITABLE_BOOK)));
        e.add(new PickMenu.Entry("정지", act.isEmpty() ? "재생 중이 아니에요" : "재생 중: " + title(act.getStringOr("name", "")), new ItemStack(Items.BARRIER)));
        e.add(new PickMenu.Entry("음량: " + c.getIntOr("vol", 75) + "%", "누를 때마다 변경", new ItemStack(Items.NOTE_BLOCK)));
        e.add(new PickMenu.Entry("들리는 범위: " + c.getIntOr("range", 32) + "칸", "누를 때마다 변경", new ItemStack(Items.ENDER_EYE)));
        e.add(new PickMenu.Entry("반복: " + (c.getBooleanOr("loop", false) ? "켜짐" : "꺼짐"), "한 곡 · 플레이리스트 전체를 반복", new ItemStack(Items.REPEATER)));
        e.add(new PickMenu.Entry("음반에 곡 담기", "빈 음반을 들고 눌러요", new ItemStack(ModItems29.CUSTOM_DISC.get())));
        e.add(new PickMenu.Entry("악보에 곡 담기", "빈 악보를 들고 눌러요", new ItemStack(ModItems29.SHEET_MUSIC.get())));
        e.add(new PickMenu.Entry("사용법", "음악 · 악보 넣는 법", new ItemStack(Items.BOOK)));
        e.add(new PickMenu.Entry("폴더 열기", "내 컴퓨터의 wildplus_music 폴더를 열어요", new ItemStack(Items.CHEST)));
        e.add(new PickMenu.Entry("내 곡 서버에 올리기", "내 폴더에만 있는 곡을 서버로 보내요 (모두가 들을 수 있게)", new ItemStack(Items.HOPPER)));
        askUpload(p, false);
        PickMenu.open(p, Component.literal("🎵 음악 플레이어"), act.isEmpty() ? "멈춰 있어요" : "♪ " + title(act.getStringOr("name", "")), "", e, PickMenu.FLAG_SHOP, i -> {
            switch (i) {
                case 0 -> pickSong(p, lv, pos, false, 0);
                case 1 -> openPlaylist(p, lv, pos);
                case 2 -> {
                    stop(lv, pos);
                    open(p, pos);
                }
                case 3 -> cycle(lv, pos, "vol", VOLS, 75, p);
                case 4 -> cycle(lv, pos, "range", RANGES, 32, p);
                case 5 -> {
                    c.putBoolean("loop", !c.getBooleanOr("loop", false));
                    ContentData.get(lv.getServer()).dirty();
                    open(p, pos);
                }
                case 6 -> pickSong(p, lv, pos, false, 1);
                case 7 -> pickSong(p, lv, pos, true, 2);
                case 8 -> {
                    p.closeContainer();
                    PacketDistributor.sendToPlayer(p, new MusicNet.Help());
                }
                case 9 -> {
                    p.closeContainer();
                    PacketDistributor.sendToPlayer(p, new MusicNet.OpenDir());
                }
                default -> {
                    p.closeContainer();
                    askUpload(p, true);
                }
            }
        }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    // ───────── 5.3.1 플레이리스트 ─────────
    /** 이 플레이어의 플레이리스트 (파일 이름, 순서대로) */
    private static List<String> playlist(CompoundTag c) {
        String raw = c.getStringOr("playlist", "");
        List<String> out = new ArrayList<>();
        if (!raw.isEmpty()) for (String n : raw.split("\n")) if (!n.isEmpty()) out.add(n);
        return out;
    }

    private static void savePlaylist(ServerLevel lv, CompoundTag c, List<String> l) {
        c.putString("playlist", String.join("\n", l));
        ContentData.get(lv.getServer()).dirty();
    }

    private static void openPlaylist(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        CompoundTag c = cfg(lv, pos);
        List<String> pl = playlist(c);
        CompoundTag act = ContentData.get(lv.getServer()).section(ACTIVE).getCompoundOrEmpty(keyOf(lv, pos));
        int playing = act.getIntOr("pl", -1);
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("▶ 처음부터 재생", pl.isEmpty() ? "곡을 먼저 추가해 주세요" : pl.size() + "곡 · 끝나면 다음 곡으로", new ItemStack(Items.JUKEBOX)));
        e.add(new PickMenu.Entry("＋ 곡 추가", "폴더의 곡을 골라 맨 뒤에 넣어요", new ItemStack(Items.MUSIC_DISC_CAT)));
        e.add(new PickMenu.Entry("모두 비우기", "플레이리스트를 비워요", new ItemStack(Items.BARRIER)));
        for (int k = 0; k < pl.size(); k++)
            e.add(new PickMenu.Entry((k == playing ? "♪ " : "") + (k + 1) + ". " + title(pl.get(k)), k == playing ? "지금 재생 중 · 눌러서 순서 바꾸기" : "눌러서 순서 바꾸기 · 빼기 · 여기서부터 재생",
                    new ItemStack(k == playing ? Items.MUSIC_DISC_PIGSTEP : Items.MUSIC_DISC_13)));
        PickMenu.open(p, Component.literal("🎵 플레이리스트"), pl.size() + "곡 · 반복 " + (c.getBooleanOr("loop", false) ? "켜짐" : "꺼짐"), "", e, PickMenu.FLAG_SHOP, i -> {
            List<String> cur = playlist(cfg(lv, pos));
            if (i == 0) {
                if (cur.isEmpty()) {
                    say(p, "플레이리스트가 비어 있어요", ChatFormatting.RED);
                    openPlaylist(p, lv, pos);
                } else {
                    playAt(p, lv, pos, 0);
                    openPlaylist(p, lv, pos);
                }
            } else if (i == 1) {
                addSong(p, lv, pos);
            } else if (i == 2) {
                savePlaylist(lv, cfg(lv, pos), new ArrayList<>());
                openPlaylist(p, lv, pos);
            } else if (i - 3 < cur.size()) {
                songMenu(p, lv, pos, i - 3);
            }
        }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    private static void addSong(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        List<String> names = list(false);
        List<PickMenu.Entry> e = new ArrayList<>();
        for (String n : names) e.add(new PickMenu.Entry(title(n), n, new ItemStack(Items.MUSIC_DISC_CAT)));
        PickMenu.open(p, Component.literal("플레이리스트에 곡 추가"), "누르면 맨 뒤에 들어가요 (여러 번 눌러 계속 추가)", "wildplus_music 폴더에 파일이 없어요", e, 0, i -> {
            if (i < 0 || i >= names.size()) return;
            CompoundTag c = cfg(lv, pos);
            List<String> pl = playlist(c);
            if (pl.size() >= 100) {
                say(p, "플레이리스트는 100곡까지예요", ChatFormatting.RED);
                return;
            }
            pl.add(names.get(i));
            savePlaylist(lv, c, pl);
            say(p, "추가: " + title(names.get(i)) + " (" + pl.size() + "번째)", ChatFormatting.GREEN);
        }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    /** 곡 하나: 위로 · 아래로 · 맨 위로 · 빼기 · 여기서부터 재생 */
    private static void songMenu(ServerPlayer p, ServerLevel lv, BlockPos pos, int idx) {
        List<String> pl = playlist(cfg(lv, pos));
        if (idx < 0 || idx >= pl.size()) {
            openPlaylist(p, lv, pos);
            return;
        }
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("▲ 위로", idx == 0 ? "이미 맨 위예요" : (idx) + "번째로", new ItemStack(Items.ARROW)));
        e.add(new PickMenu.Entry("▼ 아래로", idx == pl.size() - 1 ? "이미 맨 아래예요" : (idx + 2) + "번째로", new ItemStack(Items.SPECTRAL_ARROW)));
        e.add(new PickMenu.Entry("⤒ 맨 위로", "1번째로", new ItemStack(Items.FEATHER)));
        e.add(new PickMenu.Entry("⤓ 맨 아래로", pl.size() + "번째로", new ItemStack(Items.ANVIL)));
        e.add(new PickMenu.Entry("▶ 여기서부터 재생", "이 곡부터 차례로", new ItemStack(Items.JUKEBOX)));
        e.add(new PickMenu.Entry("✕ 빼기", "플레이리스트에서 지워요", new ItemStack(Items.BARRIER)));
        e.add(new PickMenu.Entry("← 돌아가기", "플레이리스트로", new ItemStack(Items.OAK_DOOR)));
        PickMenu.open(p, Component.literal((idx + 1) + ". " + title(pl.get(idx))), "순서 바꾸기", "", e, PickMenu.FLAG_SHOP, i -> {
            CompoundTag c = cfg(lv, pos);
            List<String> cur = playlist(c);
            if (idx >= cur.size()) {
                openPlaylist(p, lv, pos);
                return;
            }
            int to = idx;
            switch (i) {
                case 0 -> to = Math.max(0, idx - 1);
                case 1 -> to = Math.min(cur.size() - 1, idx + 1);
                case 2 -> to = 0;
                case 3 -> to = cur.size() - 1;
                case 4 -> {
                    playAt(p, lv, pos, idx);
                    openPlaylist(p, lv, pos);
                    return;
                }
                case 5 -> {
                    cur.remove(idx);
                    savePlaylist(lv, c, cur);
                    shiftPlaying(lv, pos, idx, -1);
                    openPlaylist(p, lv, pos);
                    return;
                }
                default -> {
                    openPlaylist(p, lv, pos);
                    return;
                }
            }
            if (to != idx) {
                String moved = cur.remove(idx);
                cur.add(to, moved);
                savePlaylist(lv, c, cur);
                shiftPlaying(lv, pos, idx, to);
            }
            songMenu(p, lv, pos, to);
        }, null, x -> x.blockPosition().closerThan(pos, 8));
    }

    /** 재생 중인 곡의 플레이리스트 번호를 순서 변경에 맞춰 고침 (to = -1 이면 from 을 뺀 것) */
    private static void shiftPlaying(ServerLevel lv, BlockPos pos, int from, int to) {
        CompoundTag act = ContentData.get(lv.getServer()).section(ACTIVE).getCompoundOrEmpty(keyOf(lv, pos));
        int cur = act.getIntOr("pl", -1);
        if (cur < 0) return;
        int n = cur;
        if (to < 0) {
            if (cur == from) n = cur - 1; // 지금 곡을 뺐으면 끝나고 그 자리(다음 곡)로
            else if (cur > from) n = cur - 1;
        } else if (cur == from) {
            n = to;
        } else if (from < cur && to >= cur) {
            n = cur - 1;
        } else if (from > cur && to <= cur) {
            n = cur + 1;
        }
        if (n != cur) {
            act.putInt("pl", n);
            ContentData.get(lv.getServer()).dirty();
        }
    }

    /** 플레이리스트 idx 번째 곡 재생 */
    private static void playAt(ServerPlayer p, ServerLevel lv, BlockPos pos, int idx) {
        List<String> pl = playlist(cfg(lv, pos));
        if (idx < 0 || idx >= pl.size()) return;
        start(p, lv, pos, pl.get(idx), idx);
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
        start(p, lv, pos, file, -1);
    }

    /** 재생 시작. pl = 플레이리스트 번호 (-1 = 한 곡만) · p 가 null 이면 (다음 곡 자동 재생) 메시지 없이 */
    private static boolean start(ServerPlayer p, ServerLevel lv, BlockPos pos, String file, int pl) {
        Path f = file(file, false);
        if (f == null) {
            if (p != null) say(p, "서버 폴더에 이 곡이 없어요: " + file, ChatFormatting.RED);
            return false;
        }
        try {
            if (Files.size(f) > MAX_OGG) {
                if (p != null) say(p, "파일이 너무 커요 (최대 40MB)", ChatFormatting.RED);
                return false;
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
            // 플레이리스트는 곡마다 반복하지 않고 목록 단위로 반복 (tickActive 가 다음 곡으로 넘김)
            t.putBoolean("loop", pl < 0 && c.getBooleanOr("loop", false));
            t.putInt("pl", pl);
            t.putLong("end", sec > 0 ? System.currentTimeMillis() + (long) (sec * 1000) + 3000 : 0L);
            ContentData d = ContentData.get(lv.getServer());
            d.section(ACTIVE).put(key, t);
            d.dirty();
            TOLD.remove(key);
            String msg = "🎵 재생: " + title(file) + (pl >= 0 ? " (플레이리스트 " + (pl + 1) + "/" + playlist(c).size() + ")" : "");
            if (p != null) p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
            else for (ServerPlayer q : lv.players()) if (q.blockPosition().closerThan(pos, c.getIntOr("range", 32))) q.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
            return true;
        } catch (Exception e) {
            if (p != null) say(p, "파일을 읽지 못했어요", ChatFormatting.RED);
            return false;
        }
    }

    public static void stop(ServerLevel lv, BlockPos pos) {
        String key = keyOf(lv, pos);
        ContentData d = ContentData.get(lv.getServer());
        d.section(ACTIVE).remove(key);
        d.dirty();
        TOLD.remove(key);
        dropVisual(lv.getServer(), key);
        PacketDistributor.sendToAllPlayers(new MusicNet.Stop(key));
    }

    // ───────── 5.3.5 내 곡 올리기 (친구 컴퓨터 → 서버 폴더) ─────────
    private static final class Incoming {
        final String name;
        final byte[][] parts;
        int have;
        long size;

        Incoming(String name, int total) {
            this.name = name;
            this.parts = new byte[total][];
        }
    }

    private static final Map<UUID, Incoming> INCOMING = new HashMap<>();

    /** 서버 폴더에 있는 곡 목록을 보내 클라이언트가 없는 것만 올리게 */
    public static void askUpload(ServerPlayer p, boolean manual) {
        StringBuilder sb = new StringBuilder();
        try (var st = Files.list(dir())) {
            for (Path f : (Iterable<Path>) st::iterator) sb.append(f.getFileName()).append('\t').append(Files.size(f)).append('\n');
        } catch (IOException ignored) {
        }
        PacketDistributor.sendToPlayer(p, new MusicNet.UploadAsk(sb.toString(), manual));
    }

    private static boolean okName(String n) {
        String l = n.toLowerCase(Locale.ROOT);
        return !n.isBlank() && n.length() <= 120 && !n.contains("/") && !n.contains("\\") && !n.contains("..") && !n.startsWith(".")
                && n.chars().noneMatch(ch -> ch < 32 || ":*?\"<>|".indexOf(ch) >= 0)
                && (l.endsWith(".ogg") || l.endsWith(".mid") || l.endsWith(".midi"));
    }

    public static void receive(ServerPlayer p, MusicNet.Up u) {
        boolean midi = !u.name().toLowerCase(Locale.ROOT).endsWith(".ogg");
        long max = midi ? MAX_MID : MAX_OGG;
        if (!okName(u.name()) || u.total() <= 0 || (long) u.total() * MusicNet.Up.SIZE > max + MusicNet.Up.SIZE || u.index() < 0 || u.index() >= u.total()) return;
        Incoming in = INCOMING.get(p.getUUID());
        if (in == null || !in.name.equals(u.name()) || in.parts.length != u.total()) {
            in = new Incoming(u.name(), u.total());
            INCOMING.put(p.getUUID(), in);
        }
        if (in.parts[u.index()] != null) return;
        in.parts[u.index()] = u.data();
        in.size += u.data().length;
        if (in.size > max) {
            INCOMING.remove(p.getUUID());
            say(p, "파일이 너무 커요: " + u.name(), ChatFormatting.RED);
            return;
        }
        if (++in.have < in.parts.length) return;
        INCOMING.remove(p.getUUID());
        try {
            java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
            for (byte[] b : in.parts) all.write(b);
            Path tmp = dir().resolve(".upload-" + p.getUUID() + ".tmp");
            Files.write(tmp, all.toByteArray());
            Files.move(tmp, dir().resolve(in.name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            say(p, "🎵 서버에 올렸어요: " + title(in.name), ChatFormatting.GREEN);
        } catch (Exception e) {
            say(p, "올리지 못했어요: " + in.name, ChatFormatting.RED);
        }
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
        if (p.isShiftKeyDown()) { // 5.3.7 웅크리고 누르면 음 높이 고르기
            pitchMenu(p, lv, m, key);
            return true;
        }
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
            Performance perf = new Performance(lv, m.immutable(), notes, title(file));
            perf.shift = shift(hand);
            PIANOS.put(key, perf);
            p.sendSystemMessage(Component.literal("🎹 자동 연주: " + title(file) + (perf.shift != 0 ? " · 음 높이 " + shiftLabel(perf.shift) : "")
                    + " (악보를 들고 다시 누르면 멈춤 · 웅크리고 누르면 음 높이)").withStyle(ChatFormatting.AQUA));
        } catch (Exception e) {
            say(p, "악보를 읽지 못했어요 (MIDI 파일인지 확인)", ChatFormatting.RED);
        }
        return true;
    }

    // ───────── 5.3.7 피아노 음 높이 (악보마다 저장, -24 ~ +24 반음) ─────────
    private static final String SHIFT = "wp_pitch";

    public static int shift(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(SHIFT, 0);
    }

    private static String shiftLabel(int v) {
        if (v == 0) return "원래대로";
        String oct = v % 12 == 0 ? (Math.abs(v) / 12) + "옥타브" : Math.abs(v) + "반음";
        return (v > 0 ? "▲ " : "▼ ") + oct + (v > 0 ? " 높게" : " 낮게");
    }

    private static void pitchMenu(ServerPlayer p, ServerLevel lv, BlockPos m, String key) {
        int cur = shift(p.getMainHandItem());
        int[] step = {12, 1, 0, -1, -12};
        String[] name = {"▲ 한 옥타브 높게", "▲ 반음 높게", "● 원래 높이로", "▼ 반음 낮게", "▼ 한 옥타브 낮게"};
        List<PickMenu.Entry> e = new ArrayList<>();
        for (int i = 0; i < step.length; i++)
            e.add(new PickMenu.Entry(name[i], step[i] == 0 ? "음 높이를 처음으로" : "지금보다 " + (step[i] > 0 ? "+" : "") + step[i] + "반음",
                    new ItemStack(i < 2 ? Items.SPECTRAL_ARROW : i == 2 ? Items.NOTE_BLOCK : Items.ARROW)));
        PickMenu.open(p, Component.literal("🎹 피아노 음 높이"), "지금: " + shiftLabel(cur), "", e, PickMenu.FLAG_SHOP, i -> {
            if (i < 0 || i >= step.length) return;
            ItemStack hand = p.getMainHandItem();
            if (!hand.is(ModItems29.SHEET_MUSIC.get())) return;
            int v = step[i] == 0 ? 0 : Math.max(-24, Math.min(24, shift(hand) + step[i]));
            CustomData.update(DataComponents.CUSTOM_DATA, hand, t -> t.putInt(SHIFT, v));
            Performance perf = PIANOS.get(key);
            if (perf != null) perf.shift = v;
            lv.playSound(null, m, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 1F, (float) Math.pow(2.0, Math.max(-12, Math.min(12, v)) / 12.0));
            pitchMenu(p, lv, m, key);
        }, null, x -> x.blockPosition().closerThan(m, 8));
    }

    /** 또렷한 타건: 하프 몸통 + 플링 어택. 음역에 따라 베이스 · 하프 · 벨로 나눠 실제 음높이를 맞춘다 */
    private static void strike(ServerLevel lv, BlockPos m, Midi.Note n) {
        strike(lv, m, n, 0);
    }

    private static void strike(ServerLevel lv, BlockPos m, Midi.Note n, int shift) {
        int key = n.key() + shift;
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
        if (s.getTickCount() % 8 == 2) visuals(s);
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
                strike(perf.level, perf.pos, perf.notes.get(perf.next++), perf.shift);
                played++;
            }
            return perf.next >= perf.notes.size();
        });
    }

    /** 재생 중인 플레이어마다 움직임용 받침을 유지하고 음표를 띄운다 */
    private static void visuals(MinecraftServer s) {
        CompoundTag act = ContentData.get(s).section(ACTIVE);
        for (String key : new ArrayList<>(act.keySet())) {
            if (!(act.get(key) instanceof CompoundTag t)) continue;
            ServerLevel lv = null;
            for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(t.getStringOr("dim", ""))) lv = l;
            BlockPos pos = BlockPos.of(t.getLongOr("pos", 0L));
            if (lv == null || !lv.isLoaded(pos)) continue;
            spawnVisual(key, lv, pos);
            notes(lv, pos);
        }
    }

    private static void notes(ServerLevel lv, BlockPos pos) {
        var state = lv.getBlockState(pos);
        if (!state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) return;
        var f = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        var side = f.getClockWise();
        var r = lv.getRandom();
        for (int sx : new int[] {-1, 1}) {
            lv.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5 + side.getStepX() * sx * 1.1, pos.getY() + 2.2, pos.getZ() + 0.5 + side.getStepZ() * sx * 1.1, 0, r.nextDouble(), 0, 0, 1);
        }
    }

    /** 받침이 없으면 만든다 */
    static void spawnVisual(String key, ServerLevel lv, BlockPos pos) {
        UUID id = VISUALS.get(key);
        if (id != null && lv.getEntity(id) != null) return;
        var state = lv.getBlockState(pos);
        if (!state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) return;
        var m = com.snolred.wildplus.registry.ModEntities.MUSIC_VISUAL.get().create(lv, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (m == null) return;
        float yaw = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).toYRot();
        m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0F);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        lv.addFreshEntity(m);
        VISUALS.put(key, m.getUUID());
    }

    private static void dropVisual(MinecraftServer s, String key) {
        UUID id = VISUALS.remove(key);
        if (id == null) return;
        for (ServerLevel l : s.getAllLevels()) {
            var e = l.getEntity(id);
            if (e != null) e.discard();
        }
    }

    /** 시험용: 눈앞에 플레이어를 놓고 움직임만 보여 준다 */
    public static void debugVisual(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        BlockPos at = p.blockPosition().relative(p.getDirection(), 4);
        lv.setBlock(at, com.snolred.wildplus.registry.ModBlocks18.MUSIC_PLAYER.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, p.getDirection().getOpposite()), 3);
        spawnVisual("debug", lv, at);
        notes(lv, at);
    }

    /** 플레이리스트 다음 곡 (끝이면 반복일 때만 처음으로). 넘겼으면 true */
    private static boolean next(ServerLevel lv, BlockPos pos, int cur) {
        CompoundTag c = cfg(lv, pos);
        List<String> pl = playlist(c);
        if (pl.isEmpty()) return false;
        for (int tries = 0; tries < pl.size(); tries++) {
            int n = cur + 1 + tries;
            if (n >= pl.size()) {
                if (!c.getBooleanOr("loop", false)) return false;
                n %= pl.size();
            }
            if (start(null, lv, pos, pl.get(n), n)) return true;
        }
        return false;
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
            if (over && !gone && t.getIntOr("pl", -1) >= 0 && next(lv, pos, t.getIntOr("pl", -1))) continue; // 5.3.1 다음 곡
            if (over || gone) {
                act.remove(key);
                d.dirty();
                TOLD.remove(key);
                dropVisual(s, key);
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
