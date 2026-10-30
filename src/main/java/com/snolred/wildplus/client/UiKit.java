package com.snolred.wildplus.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 화면 그리기 공용 도구. 마인크래프트 기본 창과 같은 모양:
 * 검은 외곽선(모서리는 둥글게) + 위·왼쪽 흰 입체선 + 아래·오른쪽 회색 그림자 + #C6C6C6 바탕.
 */
public final class UiKit {
    public static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, DARK = 0xFF373737, TEXT = 0xFF404040;

    private UiKit() {}

    /** 기본 창 판 */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 3, y + 1, 0xFF000000);
        g.fill(x + 2, y + h - 1, x + w - 3, y + h, 0xFF000000);
        g.fill(x, y + 2, x + 1, y + h - 3, 0xFF000000);
        g.fill(x + w - 1, y + 3, x + w, y + h - 3, 0xFF000000);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        g.fill(x + w - 3, y + 1, x + w - 2, y + 2, 0xFF000000);
        g.fill(x + w - 2, y + 2, x + w - 1, y + 3, 0xFF000000);
        g.fill(x + 1, y + h - 3, x + 2, y + h - 2, 0xFF000000);
        g.fill(x + 2, y + h - 2, x + 3, y + h - 1, 0xFF000000);
        g.fill(x + w - 2, y + h - 3, x + w - 1, y + h - 2, 0xFF000000);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, BG);
        g.fill(x + 2, y + 1, x + w - 3, y + h - 1, BG);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BG);
    }

    /** 예전 호출 호환 (색 인자는 무시하고 기본 창 모양) */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int border, int fill) {
        panel(g, x, y, w, h);
    }

    /** 움푹 들어간 칸 (아이템 칸·표시창): 위·왼쪽 진회색, 아래·오른쪽 흰색 */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    public static void slot(GuiGraphicsExtractor g, int sx, int sy) {
        inset(g, sx - 1, sy - 1, 18, 18, 0xFF8B8B8B);
    }

    public static void slots(GuiGraphicsExtractor g, int left, int top, AbstractContainerMenu menu) {
        for (Slot s : menu.slots) {
            if (s.isActive()) slot(g, left + s.x, top + s.y);
        }
    }
}
