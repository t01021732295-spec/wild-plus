package com.snolred.wildplus.client;

import com.snolred.wildplus.net.MapNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 큰 지도: 휠로 확대 · 축소 */
public class MapScreen extends Screen {
    private static final int PX = 3;
    private static final int[] SCALES = {1, 2, 4, 8};
    private static final String[] NAMES = {"친구", "사망 위치", "워프", "내 땅"};
    private int scale = 1;

    public MapScreen() {
        super(Component.literal("지도"));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scale = Math.max(0, Math.min(SCALES.length - 1, scale - (int) Math.signum(sy)));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int cols = (width - 24) / PX, rows = (height - 44) / PX, s = SCALES[scale];
        int ox = (width - cols * PX) / 2, oy = 26;
        g.fill(0, 0, width, height, 0xB0000000);
        g.fill(ox - 2, oy - 2, ox + cols * PX + 2, oy + rows * PX + 2, 0xFFE8BA46);
        g.fill(ox, oy, ox + cols * PX, oy + rows * PX, 0xFF141822);
        int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
        MiniMap.draw(g, MiniMap.cur(mc.level), ox, oy, cols, rows, PX, px, pz, s, false);
        int cx = ox + cols / 2 * PX, cy = oy + rows / 2 * PX;
        for (MapNet.Marker m : MiniMap.markers) {
            if (m.type() == MapNet.LAND) continue;
            int mx = cx + (m.x() - px) * PX / s, my = cy + (m.z() - pz) * PX / s;
            mx = Math.max(ox + 3, Math.min(ox + cols * PX - 3, mx));
            my = Math.max(oy + 3, Math.min(oy + rows * PX - 3, my));
            g.fill(mx - 4, my - 4, mx + 4, my + 4, 0xFF000000);
            g.fill(mx - 3, my - 3, mx + 3, my + 3, MiniMap.MARK[m.type()]);
            g.text(font, m.label(), mx + 6, my - 4, 0xFFFFFFFF, true);
        }
        MiniMap.arrow(g, cx, cy, mc.player.getYRot());
        g.text(font, "지도 · 1칸 = " + s + "블록 · 휠로 확대/축소 · ESC 닫기", ox, 10, 0xFFFFD36B, true);
        int lx = ox;
        for (int i = 0; i < NAMES.length; i++) {
            g.fill(lx, height - 13, lx + 6, height - 7, MiniMap.MARK[i]);
            g.text(font, NAMES[i], lx + 9, height - 14, 0xFFE8E8F0, false);
            lx += 16 + font.width(NAMES[i]);
        }
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
