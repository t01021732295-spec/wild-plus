package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.net.MapNet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 4.8 미니맵: 지나간 곳의 지표 색을 청크 단위로 기억해 그린다 (북쪽이 위).
 * 그림은 텍스처 없이 같은 색 칸을 가로로 이어 붙인 사각형으로 그린다. 기록은 세계마다 wildplus_map 폴더에 저장.
 */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class MiniMap {
    private static final int CELLS = 44, PX = 2, RANGE = 5;
    private static final int[] ZOOMS = {1, 2, 4};
    static final int[] MARK = {0xFF5AC8FF, 0xFFFF6464, 0xFF78FF8C, 0xFFFFD75A};
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(WildPlus.id("main"));
    private static final KeyMapping KEY_MAP = new KeyMapping("key.wildplus.map", 77, CATEGORY);
    private static final KeyMapping KEY_IN = new KeyMapping("key.wildplus.map_in", 61, CATEGORY);
    private static final KeyMapping KEY_OUT = new KeyMapping("key.wildplus.map_out", 45, CATEGORY);

    private static final Map<String, Map<Long, int[]>> DIMS = new HashMap<>();
    static volatile List<MapNet.Marker> markers = List.of();
    static final Set<Long> land = new HashSet<>();
    private static String world = "";
    private static int zoom = 1, cursor, dirty;
    /** 5.0.6 최적화: 지도 내용이 바뀔 때마다 올라가는 번호 (그림 캐시 무효화용) · 저장할 차원 · 저장 중 여부 */
    static int version;
    private static final Set<String> DIRTY_DIMS = new HashSet<>();
    private static final java.util.concurrent.atomic.AtomicBoolean SAVING = new java.util.concurrent.atomic.AtomicBoolean();
    private static final Cache HUD = new Cache();

    private MiniMap() {}

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(KEY_MAP);
        event.register(KEY_IN);
        event.register(KEY_OUT);
    }

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, WildPlus.id("minimap"), MiniMap::render);
    }

    public static void accept(MapNet.Sync p) {
        if (!p.world().equals(world)) {
            world = p.world();
            load();
        }
        markers = p.markers();
        Set<Long> next = new HashSet<>();
        for (MapNet.Marker m : p.markers()) if (m.type() == MapNet.LAND) next.add(ChunkPos.pack(m.x(), m.z()));
        if (!next.equals(land)) {
            land.clear();
            land.addAll(next);
            version++;
        }
    }

    private static String dimKey(Level lv) {
        return lv.dimension().identifier().toString();
    }

    static Map<Long, int[]> cur(Level lv) {
        return DIMS.computeIfAbsent(dimKey(lv), k -> new HashMap<>());
    }

    /** 기록된 색 (없으면 0) */
    static int colorAt(Map<Long, int[]> map, int bx, int bz) {
        int[] c = map.get(ChunkPos.pack(bx >> 4, bz >> 4));
        return c == null ? 0 : c[(bz & 15) * 16 + (bx & 15)];
    }

    static int tint(int c, int bx, int bz) {
        return land.contains(ChunkPos.pack(bx >> 4, bz >> 4)) ? UiKit.mix(c, 0xFFFFD75A, 0.3F) : c;
    }

    // ───────── 기록 ─────────
    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel lv = mc.level;
        if (p == null || lv == null) return;
        while (KEY_IN.consumeClick()) zoom = Math.max(0, zoom - 1);
        while (KEY_OUT.consumeClick()) zoom = Math.min(ZOOMS.length - 1, zoom + 1);
        while (KEY_MAP.consumeClick()) mc.gui.setScreen(new MapScreen());
        if (!ClientConfig.MINIMAP.getAsBoolean()) return;
        int side = RANGE * 2 + 1, pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        Map<Long, int[]> map = cur(lv);
        for (int n = 0; n < 2; n++) {
            cursor = (cursor + 1) % (side * side);
            int cx = pcx + cursor % side - RANGE, cz = pcz + cursor / side - RANGE;
            if (!lv.hasChunk(cx, cz)) continue;
            int[] col = sample(lv, lv.getChunk(cx, cz), p.getBlockY());
            if (col == null) continue;
            int[] old = map.put(ChunkPos.pack(cx, cz), col);
            if (old == null || !java.util.Arrays.equals(old, col)) {
                version++;
                DIRTY_DIMS.add(dimKey(lv));
            }
        }
        if (++dirty >= 6000) save(true);
    }

    private static int[] sample(ClientLevel lv, LevelChunk c, int py) {
        int[] out = new int[256];
        boolean roofed = lv.dimension() == Level.NETHER;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ(), min = lv.getMinY();
        boolean any = false;
        for (int x = 0; x < 16; x++) {
            int last = Integer.MIN_VALUE;
            for (int z = 0; z < 16; z++) {
                int y;
                if (roofed) {
                    y = py + 8;
                    for (int k = 0; k < 40 && y > min; k++, y--) {
                        if (!c.getBlockState(m.set(bx + x, y, bz + z)).isAir() && c.getBlockState(m.set(bx + x, y + 1, bz + z)).isAir()) break;
                    }
                } else {
                    y = c.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                }
                int rgb = 0;
                for (int k = 0; k < 6 && y > min; k++, y--) {
                    BlockState st = c.getBlockState(m.set(bx + x, y, bz + z));
                    rgb = st.getMapColor(lv, m).col;
                    if (rgb != 0) break;
                }
                if (rgb == 0) {
                    last = y;
                    continue;
                }
                float f = last == Integer.MIN_VALUE || y == last ? 0.86F : y > last ? 1F : 0.72F;
                last = y;
                out[z * 16 + x] = 0xFF000000 | (int) ((rgb >> 16 & 255) * f) << 16 | (int) ((rgb >> 8 & 255) * f) << 8 | (int) ((rgb & 255) * f);
                any = true;
            }
        }
        return any ? out : null;
    }

    // ───────── 저장 ─────────
    private static Path dir() {
        return FMLPaths.GAMEDIR.get().resolve("wildplus_map").resolve(world.replaceAll("[^a-zA-Z0-9-]", "_"));
    }

    private static void load() {
        DIMS.clear();
        DIRTY_DIMS.clear();
        version++;
        if (world.isEmpty()) return;
        try {
            Path d = dir();
            if (!Files.isDirectory(d)) return;
            try (var files = Files.list(d)) {
                for (Path f : files.toList()) {
                    String name = f.getFileName().toString();
                    if (!name.endsWith(".bin")) continue;
                    try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(Files.newInputStream(f)), 1 << 16))) {
                        String dim = in.readUTF();
                        int n = in.readInt();
                        Map<Long, int[]> map = DIMS.computeIfAbsent(dim, k -> new HashMap<>());
                        for (int i = 0; i < n; i++) {
                            long key = in.readLong();
                            int[] c = new int[256];
                            for (int k = 0; k < 256; k++) c[k] = in.readInt();
                            map.put(key, c);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** async: 게임 진행 중 저장은 별도 스레드에서 (5분마다 멈칫하던 원인) · 접속 끊을 때는 바로 */
    private static void save(boolean async) {
        dirty = 0;
        if (world.isEmpty() || DIRTY_DIMS.isEmpty()) return;
        if (async && SAVING.get()) return; // 앞 저장이 아직 진행 중이면 다음 기회에
        Path d = dir();
        Map<String, Map<Long, int[]>> snap = new HashMap<>();
        for (String dim : DIRTY_DIMS) {
            Map<Long, int[]> m = DIMS.get(dim);
            if (m != null && !m.isEmpty()) snap.put(dim, new HashMap<>(m));
        }
        DIRTY_DIMS.clear();
        Runnable job = () -> {
            try {
                Files.createDirectories(d);
                for (var en : snap.entrySet()) {
                    Path f = d.resolve(en.getKey().replaceAll("[^a-zA-Z0-9-]", "_") + ".bin");
                    Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
                    try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp)), 1 << 16))) {
                        out.writeUTF(en.getKey());
                        out.writeInt(en.getValue().size());
                        for (var c : en.getValue().entrySet()) {
                            out.writeLong(c.getKey());
                            for (int v : c.getValue()) out.writeInt(v);
                        }
                    }
                    Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            } finally {
                SAVING.set(false);
            }
        };
        if (!async) {
            while (SAVING.get()) Thread.onSpinWait();
            SAVING.set(true);
            job.run();
            return;
        }
        SAVING.set(true);
        Thread th = new Thread(job, "wildplus-map-save");
        th.setDaemon(false);
        th.start();
    }

    @SubscribeEvent
    static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        save(false);
        DIMS.clear();
        markers = List.of();
        land.clear();
        world = "";
    }

    // ───────── 그리기 ─────────
    /** 5.0.6: 미리 계산한 사각형 목록 (x0, y0, x1, y1, 색) — 매 프레임 칸마다 지도를 찾던 것을 없앰 */
    static final class Cache {
        int[] rects = new int[1024];
        int n, key0 = Integer.MIN_VALUE, key1, key2, key3, key4, ver = -1;
        Object map;

        void add(int x0, int y0, int x1, int y1, int c) {
            if (n + 5 > rects.length) rects = java.util.Arrays.copyOf(rects, rects.length * 2);
            rects[n++] = x0;
            rects[n++] = y0;
            rects[n++] = x1;
            rects[n++] = y1;
            rects[n++] = c;
        }
    }

    /** (ox, oy) 부터 cols × rows 칸을 그린다. 같은 색은 가로로 이어서 한 번에. round 면 원 안쪽만 */
    static void draw(GuiGraphicsExtractor g, Cache cache, Map<Long, int[]> map, int ox, int oy, int cols, int rows, int px, int cx, int cz, int scale, boolean round) {
        int k0 = ox * 31 + oy, k1 = cols * 31 + rows + (round ? 7 : 0), k2 = px * 31 + scale;
        if (cache.map != map || cache.ver != version || cache.key0 != k0 || cache.key1 != k1 || cache.key2 != k2 || cache.key3 != cx || cache.key4 != cz) {
            cache.map = map;
            cache.ver = version;
            cache.key0 = k0;
            cache.key1 = k1;
            cache.key2 = k2;
            cache.key3 = cx;
            cache.key4 = cz;
            build(cache, map, ox, oy, cols, rows, px, cx, cz, scale, round);
        }
        int[] r = cache.rects;
        for (int i = 0; i < cache.n; i += 5) g.fill(r[i], r[i + 1], r[i + 2], r[i + 3], r[i + 4]);
    }

    private static void build(Cache cache, Map<Long, int[]> map, int ox, int oy, int cols, int rows, int px, int cx, int cz, int scale, boolean round) {
        cache.n = 0;
        double r = cols / 2.0;
        long lastKey = Long.MIN_VALUE;
        int[] lastArr = null;
        boolean lastLand = false;
        for (int j = 0; j < rows; j++) {
            int from = 0, to = cols;
            if (round) {
                double dy = j + 0.5 - r, half = Math.sqrt(Math.max(0, r * r - dy * dy));
                from = (int) Math.ceil(r - half);
                to = (int) Math.floor(r + half);
            }
            int bz = cz + (j - rows / 2) * scale;
            int runStart = from, runColor = 0;
            for (int i = from; i <= to; i++) {
                int col = 0;
                if (i < to) {
                    int bx = cx + (i - cols / 2) * scale;
                    long key = ChunkPos.pack(bx >> 4, bz >> 4);
                    if (key != lastKey) { // 같은 청크 안에서는 다시 찾지 않음
                        lastKey = key;
                        lastArr = map.get(key);
                        lastLand = !land.isEmpty() && land.contains(key);
                    }
                    if (lastArr != null) {
                        col = lastArr[(bz & 15) * 16 + (bx & 15)];
                        if (col != 0 && lastLand) col = UiKit.mix(col, 0xFFFFD75A, 0.3F);
                    }
                }
                if (col != runColor || i == to) {
                    if (runColor != 0) cache.add(ox + runStart * px, oy + j * px, ox + i * px, oy + j * px + px, runColor);
                    runStart = i;
                    runColor = col;
                }
            }
        }
    }

    static void arrow(GuiGraphicsExtractor g, int x, int y, float yRot) {
        double a = Math.toRadians(yRot + 180.0);
        double dx = Math.sin(a), dy = -Math.cos(a);
        g.fill(x - 2, y - 2, x + 2, y + 2, 0xFF000000);
        g.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
        for (int k = 2; k <= 5; k++) {
            int qx = x + (int) Math.round(dx * k), qy = y + (int) Math.round(dy * k);
            g.fill(qx - 1, qy - 1, qx + 1, qy + 1, 0xFFFFFFFF);
        }
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || !ClientConfig.MINIMAP.getAsBoolean()) return;
        int size = CELLS * PX, scale = ZOOMS[zoom];
        int ox = g.guiWidth() - size - 6, oy = p.getActiveEffects().isEmpty() ? 6 : 58;
        UiKit.circle(g, ox - 2, oy - 2, size + 4, 0xFFE8BA46);
        UiKit.circle(g, ox, oy, size, 0xFF141822);
        draw(g, HUD, cur(mc.level), ox, oy, CELLS, CELLS, PX, p.getBlockX(), p.getBlockZ(), scale, true);
        int cx = ox + size / 2, cy = oy + size / 2;
        double lim = size / 2.0 - 3;
        for (MapNet.Marker m : markers) {
            if (m.type() == MapNet.LAND) continue;
            double dx = (m.x() - p.getBlockX()) * (double) PX / scale, dy = (m.z() - p.getBlockZ()) * (double) PX / scale;
            double len = Math.hypot(dx, dy);
            if (len > lim) {
                dx = dx / len * lim;
                dy = dy / len * lim;
            }
            int mx = cx + (int) dx, my = cy + (int) dy;
            g.fill(mx - 3, my - 3, mx + 3, my + 3, 0xFF000000);
            g.fill(mx - 2, my - 2, mx + 2, my + 2, MARK[m.type()]);
        }
        arrow(g, cx, cy, p.getYRot());
        g.text(mc.font, "N", cx - 2, oy - 1, 0xFFFF5050, true);
    }
}
