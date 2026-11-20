package com.snolred.wildplus.client;

import com.snolred.wildplus.net.MusicNet;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.neoforged.fml.loading.FMLPaths;

/** 4.6 음악 재생(클라이언트): 서버에서 받은 ogg 를 저장해 두고 블록 주변에서 재생. 거리 감쇠는 직접 계산 (스테레오 파일도 멀어지면 작아지게) */
public final class MusicClient {
    private static final Map<String, Track> PLAYING = new HashMap<>();
    private static final Map<String, MusicNet.Play> WAITING = new HashMap<>();
    private static final Map<String, Part> PARTS = new HashMap<>();

    private static final class Part {
        final byte[][] data;
        int have;

        Part(int total) {
            data = new byte[total][];
        }
    }

    private MusicClient() {}

    private static Path cache(String sha) throws java.io.IOException {
        Path d = FMLPaths.GAMEDIR.get().resolve("wildplus_music_cache");
        Files.createDirectories(d);
        return d.resolve(sha + ".ogg");
    }

    public static void play(MusicNet.Play p) {
        try {
            Path f = cache(p.sha());
            if (Files.exists(f) && Files.size(f) == p.size()) {
                start(p, f);
            } else {
                boolean first = WAITING.values().stream().noneMatch(w -> w.sha().equals(p.sha()));
                WAITING.put(p.key(), p);
                if (first) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new MusicNet.Request(p.sha()));
            }
        } catch (Exception ignored) {
        }
    }

    public static void stop(String key) {
        WAITING.remove(key);
        Track t = PLAYING.remove(key);
        if (t != null) t.end();
    }

    public static void chunk(MusicNet.Chunk c) {
        Part part = PARTS.computeIfAbsent(c.sha(), k -> new Part(c.total()));
        if (c.index() >= part.data.length || part.data[c.index()] != null) return;
        part.data[c.index()] = c.data();
        if (++part.have < part.data.length) return;
        PARTS.remove(c.sha());
        try {
            java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
            for (byte[] b : part.data) all.write(b);
            byte[] bytes = all.toByteArray();
            if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes)).equals(c.sha())) return;
            Path f = cache(c.sha());
            Files.write(f, bytes);
            for (var it = WAITING.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (e.getValue().sha().equals(c.sha())) {
                    start(e.getValue(), f);
                    it.remove();
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ───────── 5.3.5 내 곡 올리기 ─────────
    private static final java.util.ArrayDeque<Path> UPQ = new java.util.ArrayDeque<>();
    private static byte[] upData;
    private static String upName;
    private static int upNext, upDone;

    /** 내 폴더에서 서버에 없는 (이름이 없거나 크기가 다른) 곡을 줄 세움 */
    public static void uploadAsk(MusicNet.UploadAsk a) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) { // 혼자 하는 세계는 같은 폴더
            if (a.manual() && mc.player != null) say("혼자 하는 세계는 폴더에 넣기만 하면 돼요", net.minecraft.ChatFormatting.YELLOW);
            return;
        }
        java.util.Map<String, Long> have = new HashMap<>();
        for (String line : a.have().split("\n")) {
            int t = line.lastIndexOf('\t');
            if (t > 0) try {
                have.put(line.substring(0, t), Long.parseLong(line.substring(t + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        int added = 0;
        try (var st = Files.list(dir())) {
            for (Path f : (Iterable<Path>) st::iterator) {
                String n = f.getFileName().toString(), l = n.toLowerCase(java.util.Locale.ROOT);
                boolean ogg = l.endsWith(".ogg"), mid = l.endsWith(".mid") || l.endsWith(".midi");
                if (!ogg && !mid || n.startsWith(".")) continue;
                long size = Files.size(f);
                if (size > (ogg ? 40L << 20 : 1L << 20) || Long.valueOf(size).equals(have.get(n))) continue;
                if (UPQ.contains(f) || n.equals(upName)) continue;
                UPQ.add(f);
                added++;
            }
        } catch (Exception ignored) {
        }
        if (added > 0) say("🎵 내 폴더의 새 곡 " + added + "개를 서버에 올려요…", net.minecraft.ChatFormatting.AQUA);
        else if (a.manual() && upName == null) say("서버에 없는 새 곡이 없어요 (내 wildplus_music 폴더에 .ogg 를 넣어 주세요)", net.minecraft.ChatFormatting.YELLOW);
    }

    private static void say(String s, net.minecraft.ChatFormatting c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(s).withStyle(c));
    }

    private static Path dir() throws java.io.IOException {
        Path d = FMLPaths.GAMEDIR.get().resolve("wildplus_music");
        Files.createDirectories(d);
        return d;
    }

    /** 틱마다 조각 2개씩 (약 1MB/초) */
    public static void tickUpload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            UPQ.clear();
            upData = null;
            upName = null;
            return;
        }
        for (int k = 0; k < 2; k++) {
            if (upData == null) {
                Path f = UPQ.poll();
                if (f == null) return;
                try {
                    upData = Files.readAllBytes(f);
                    upName = f.getFileName().toString();
                    upNext = 0;
                } catch (Exception e) {
                    continue;
                }
            }
            int total = Math.max(1, (upData.length + MusicNet.Up.SIZE - 1) / MusicNet.Up.SIZE);
            int from = upNext * MusicNet.Up.SIZE, to = Math.min(upData.length, from + MusicNet.Up.SIZE);
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new MusicNet.Up(upName, upNext, total, java.util.Arrays.copyOfRange(upData, from, to)));
            upNext++;
            if (mc.player != null && upNext % 10 == 0)
                mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("🎵 올리는 중: %s (%d%%)", upName, upNext * 100 / total)).withStyle(net.minecraft.ChatFormatting.AQUA));
            if (upNext >= total) {
                upData = null;
                upName = null;
            }
        }
    }

    /** 내 컴퓨터의 wildplus_music 폴더 열기 (서버 접속 중이면 넣은 뒤 올리기 안내) */
    public static void openDir() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer())
            say("내 폴더예요. 곡을 넣은 뒤 음악 플레이어를 다시 열면 서버로 올라가요", net.minecraft.ChatFormatting.YELLOW);
        try {
            Path d = FMLPaths.GAMEDIR.get().resolve("wildplus_music");
            Files.createDirectories(d);
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            new ProcessBuilder(os.contains("win") ? "explorer" : os.contains("mac") ? "open" : "xdg-open", d.toAbsolutePath().toString()).start();
        } catch (Exception ignored) {
        }
    }

    public static void help() {
        Minecraft.getInstance().gui.setScreen(new MusicHelpScreen());
    }

    private static void start(MusicNet.Play p, Path file) {
        stop(p.key());
        Track t = new Track(p, file);
        PLAYING.put(p.key(), t);
        Minecraft.getInstance().getSoundManager().play(t);
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private final Path file;
        private final BlockPos at;
        private final float full;
        private final int range;

        Track(MusicNet.Play p, Path file) {
            super(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("wildplus", "custom/" + p.sha())), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.file = file;
            this.at = BlockPos.of(p.pos());
            this.full = p.volume();
            this.range = p.range();
            this.looping = p.loop();
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = level();
        }

        private float level() {
            var pl = Minecraft.getInstance().player;
            if (pl == null) return 0F;
            double d = Math.sqrt(pl.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5));
            return (float) (full * Math.max(0.0, 1.0 - d / range));
        }

        void end() {
            stop();
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().player == null) {
                stop();
                return;
            }
            volume = level();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public WeighedSoundEvents getOrResolve(SoundManager manager) {
            WeighedSoundEvents ev = new WeighedSoundEvents(getIdentifier(), null);
            this.sound = new Sound(getIdentifier(), ConstantFloat.of(1F), ConstantFloat.of(1F), 1, Sound.Type.FILE, true, false, 16);
            this.soundEvent = ev;
            return ev;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound s, boolean loop) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    InputStream in = new BufferedInputStream(Files.newInputStream(file));
                    return loop ? new LoopingAudioStream(JOrbisAudioStream::new, in) : new JOrbisAudioStream(in);
                } catch (Exception e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            });
        }
    }
}
