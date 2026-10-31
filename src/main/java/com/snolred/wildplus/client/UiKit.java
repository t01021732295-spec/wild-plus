package com.snolred.wildplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 화면 그리기 공용 도구 (9차 리뉴얼).
 * 실제 기기처럼 보이도록: 둥근 모서리 본체 · 그라데이션 · 하이라이트/그림자 · 나사 · LCD 화면 · LED · 카드.
 * 색은 모두 ARGB.
 */
public final class UiKit {
    // 예전 기본 창 색 (호환)
    public static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, DARK = 0xFF373737, TEXT = 0xFF404040;

    // 공용 팔레트
    public static final int INK = 0xFF1E2430, INK_SOFT = 0xFF5B6475, PAPER = 0xFFF7F8FA, LINE = 0xFFDCE0E6;
    public static final int RED = 0xFFE5484D, BLUE = 0xFF3B82F6, GREEN = 0xFF22A35A, GOLD = 0xFFF2C14E, AMBER = 0xFFFFB020;
    public static final int UP = 0xFFFF4D4F, DOWN = 0xFF3D8BFF;

    private UiKit() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    // ───────── 색 계산 ─────────
    public static int mix(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16
                | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    public static int lighter(int c, float t) {
        return mix(c, 0xFF000000 | 0xFFFFFF, t) & 0x00FFFFFF | c & 0xFF000000;
    }

    public static int darker(int c, float t) {
        return mix(c, 0xFF000000, t) & 0x00FFFFFF | c & 0xFF000000;
    }

    public static int alpha(int c, int a) {
        return (a & 255) << 24 | c & 0xFFFFFF;
    }

    // ───────── 기본 도형 ─────────
    /** 모서리가 r(0~3)만큼 깎인 사각형 */
    public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        switch (r) {
            case 0 -> g.fill(x, y, x + w, y + h, c);
            case 1 -> {
                g.fill(x + 1, y, x + w - 1, y + h, c);
                g.fill(x, y + 1, x + 1, y + h - 1, c);
                g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
            }
            case 2 -> {
                g.fill(x + 2, y, x + w - 2, y + h, c);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, c);
                g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, c);
                g.fill(x, y + 2, x + 1, y + h - 2, c);
                g.fill(x + w - 1, y + 2, x + w, y + h - 2, c);
            }
            default -> {
                g.fill(x + 3, y, x + w - 3, y + h, c);
                g.fill(x + 1, y + 1, x + 3, y + h - 1, c);
                g.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, c);
                g.fill(x, y + 3, x + 1, y + h - 3, c);
                g.fill(x + w - 1, y + 3, x + w, y + h - 3, c);
            }
        }
    }

    /** 위→아래 그라데이션 둥근 사각형 */
    public static void roundGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r == 0) {
            g.fillGradient(x, y, x + w, y + h, top, bottom);
            return;
        }
        g.fillGradient(x + r, y, x + w - r, y + h, top, bottom);
        for (int i = 0; i < r; i++) {
            int inset = r - i; // 맨 바깥 열일수록 위아래가 더 깎임
            int t = mix(top, bottom, (float) inset / h), b = mix(top, bottom, 1f - (float) inset / h);
            g.fillGradient(x + i, y + inset, x + i + 1, y + h - inset, t, b);
            g.fillGradient(x + w - 1 - i, y + inset, x + w - i, y + h - inset, t, b);
        }
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    /** 점선 사각형 (바느질, 칩 무늬) */
    public static void dashed(GuiGraphicsExtractor g, int x, int y, int w, int h, int dash, int c) {
        for (int i = 0; i < w; i += dash * 2) {
            int e = Math.min(i + dash, w);
            g.fill(x + i, y, x + e, y + 1, c);
            g.fill(x + i, y + h - 1, x + e, y + h, c);
        }
        for (int i = 0; i < h; i += dash * 2) {
            int e = Math.min(i + dash, h);
            g.fill(x, y + i, x + 1, y + e, c);
            g.fill(x + w - 1, y + i, x + w, y + e, c);
        }
    }

    public static void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
        g.fill(x, y, x + w, y + 1, c);
    }

    /** 원 (지름 d, 픽셀 근사) */
    public static void circle(GuiGraphicsExtractor g, int x, int y, int d, int c) {
        float r = d / 2f;
        for (int i = 0; i < d; i++) {
            float dy = i + 0.5f - r;
            int half = Math.round((float) Math.sqrt(Math.max(0, r * r - dy * dy)));
            int cx = x + Math.round(r);
            g.fill(cx - half, y + i, cx + half, y + i + 1, c);
        }
    }

    // ───────── 기기 부품 ─────────
    /** 창 아래 깔리는 부드러운 그림자 */
    public static void dropShadow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        round(g, x - 1, y + 2, w + 2, h + 2, 3, 0x30000000);
        round(g, x, y + 3, w, h + 1, 3, 0x30000000);
    }

    /** 기기 본체: 검은 테두리 + 그라데이션 + 위 하이라이트 + 아래 음영 */
    public static void device(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        dropShadow(g, x, y, w, h);
        round(g, x, y, w, h, 3, 0xFF101216);
        roundGradient(g, x + 1, y + 1, w - 2, h - 2, 3, top, bottom);
        g.fill(x + 4, y + 1, x + w - 4, y + 2, 0x66FFFFFF);
        g.fill(x + 1, y + 4, x + 2, y + h / 2, 0x33FFFFFF);
        g.fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 0x55000000);
    }

    /** 십자 나사 */
    public static void screw(GuiGraphicsExtractor g, int x, int y) {
        round(g, x, y, 5, 5, 1, 0xFF4A4E55);
        round(g, x + 1, y + 1, 3, 3, 1, 0xFFC9CED6);
        g.fill(x + 1, y + 2, x + 4, y + 3, 0xFF6A707A);
        g.fill(x + 2, y + 1, x + 3, y + 4, 0xFF6A707A);
    }

    public static void screws(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        screw(g, x + 4, y + 4);
        screw(g, x + w - 9, y + 4);
        screw(g, x + 4, y + h - 9);
        screw(g, x + w - 9, y + h - 9);
    }

    /** LCD·터치 화면: 검은 베젤 + 그라데이션 + 위쪽 반사광 */
    public static void screen(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        round(g, x - 3, y - 3, w + 6, h + 6, 3, 0xFF0B0D10);
        round(g, x - 2, y - 2, w + 4, h + 4, 2, 0xFF22262C);
        g.fillGradient(x, y, x + w, y + h, top, bottom);
        g.fill(x, y, x + w, y + 1, 0x33000000);
        g.fillGradient(x, y + 1, x + w, y + Math.max(2, h / 4), 0x12FFFFFF, 0x00FFFFFF);
    }

    /** 움푹 들어간 홈 (배출구, 카드 투입구) */
    public static void groove(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        round(g, x, y, w, h, 1, 0xFF0A0B0D);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0x55FFFFFF);
    }

    /** 카드 (흰 판 + 얇은 테두리 + 아래 그림자) */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        round(g, x, y + 1, w, h, 2, 0x22000000);
        round(g, x, y, w, h, 2, border);
        round(g, x + 1, y + 1, w - 2, h - 2, 2, fill);
    }

    /** 알약 모양 배지 */
    public static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        round(g, x, y, w, h, Math.min(3, h / 2), c);
    }

    /** 빛나는 LED */
    public static void led(GuiGraphicsExtractor g, int x, int y, int c) {
        round(g, x - 1, y - 1, 6, 6, 2, alpha(c, 0x40));
        round(g, x, y, 4, 4, 1, c);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xCCFFFFFF);
    }

    /** 노랑·검정 경고 띠 */
    public static void hazard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        for (int col = 0; col < w; col++) {
            for (int row = 0; row < h; row++) {
                if (((col + row) / 4) % 2 == 0) g.fill(x + col, y + row, x + col + 1, y + row + 1, 0xFFF2C14E);
            }
        }
    }

    /** 결정적 얼룩 무늬 (코르크·가죽·펠트 질감) */
    public static void speckle(GuiGraphicsExtractor g, int x, int y, int w, int h, int seed, int count, int c1, int c2) {
        long s = seed * 0x5DEECE66DL + 11;
        for (int i = 0; i < count; i++) {
            s = s * 0x5DEECE66DL + 11 & 0xFFFFFFFFFFFFL;
            int px = (int) ((s >>> 17) % Math.max(1, w));
            s = s * 0x5DEECE66DL + 11 & 0xFFFFFFFFFFFFL;
            int py = (int) ((s >>> 17) % Math.max(1, h));
            int sz = (s >>> 40 & 3) == 0 ? 2 : 1;
            g.fill(x + px, y + py, x + px + sz, y + py + 1, (i & 1) == 0 ? c1 : c2);
        }
    }

    // ───────── 아이템 칸 ─────────
    public enum SlotStyle { CLASSIC, DARK, LIGHT, BROWN }

    public static void slot(GuiGraphicsExtractor g, int sx, int sy, SlotStyle style) {
        int x = sx - 1, y = sy - 1;
        switch (style) {
            case DARK -> {
                round(g, x, y, 18, 18, 1, 0xFF0B0E12);
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF262C35);
                g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF151A20);
                g.fill(x + 1, y + 16, x + 17, y + 17, 0xFF39414D);
            }
            case LIGHT -> {
                round(g, x, y, 18, 18, 1, 0xFFB7BEC8);
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFE9EDF2);
                g.fill(x + 1, y + 1, x + 17, y + 2, 0xFFCDD3DB);
            }
            case BROWN -> {
                round(g, x, y, 18, 18, 1, 0xFF2A170A);
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF5A3A22);
                g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF3E2614);
                g.fill(x + 1, y + 16, x + 17, y + 17, 0xFF7A5534);
            }
            default -> inset(g, x, y, 18, 18, 0xFF8B8B8B);
        }
    }

    public static void slot(GuiGraphicsExtractor g, int sx, int sy) {
        slot(g, sx, sy, SlotStyle.CLASSIC);
    }

    public static void slots(GuiGraphicsExtractor g, int left, int top, AbstractContainerMenu menu, SlotStyle style) {
        for (Slot s : menu.slots) {
            if (s.isActive()) slot(g, left + s.x, top + s.y, style);
        }
    }

    public static void slots(GuiGraphicsExtractor g, int left, int top, AbstractContainerMenu menu) {
        slots(g, left, top, menu, SlotStyle.CLASSIC);
    }

    /** 플레이어 가방 받침 (밝은 금속 쟁반) */
    public static void inventoryTray(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        round(g, x, y, w, h, 2, 0xFF8E96A3);
        roundGradient(g, x + 1, y + 1, w - 2, h - 2, 2, 0xFFE4E8EE, 0xFFCDD3DC);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x88FFFFFF);
    }

    // ───────── 글자 ─────────
    public static void text(GuiGraphicsExtractor g, String s, int x, int y, int c) {
        g.text(font(), Component.literal(s), x, y, c, false);
    }

    public static void text(GuiGraphicsExtractor g, Component s, int x, int y, int c) {
        g.text(font(), s, x, y, c, false);
    }

    public static void right(GuiGraphicsExtractor g, String s, int xEnd, int y, int c) {
        g.text(font(), Component.literal(s), xEnd - font().width(s), y, c, false);
    }

    public static void center(GuiGraphicsExtractor g, String s, int cx, int y, int c) {
        g.text(font(), Component.literal(s), cx - font().width(s) / 2, y, c, false);
    }

    public static void center(GuiGraphicsExtractor g, Component s, int cx, int y, int c) {
        g.text(font(), s, cx - font().width(s) / 2, y, c, false);
    }

    /** 두껍게 (1픽셀 옆으로 한 번 더) */
    public static void bold(GuiGraphicsExtractor g, String s, int x, int y, int c) {
        text(g, s, x, y, c);
        text(g, s, x + 1, y, c);
    }

    /** 크게 (정수 배율) */
    public static void big(GuiGraphicsExtractor g, String s, int x, int y, int scale, int c) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font(), Component.literal(s), 0, 0, c, false);
        g.pose().popMatrix();
    }

    public static int bigWidth(String s, int scale) {
        return font().width(s) * scale;
    }

    // ───────── 예전 호출 호환 ─────────
    /** 기본 창 판 (마인크래프트 원래 모양) */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        round(g, x, y, w, h, 2, 0xFF000000);
        round(g, x + 1, y + 1, w - 2, h - 2, 2, BG);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BG);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int border, int fill) {
        panel(g, x, y, w, h);
    }

    /** 움푹 들어간 칸 (마인크래프트 원래 모양) */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }
}
