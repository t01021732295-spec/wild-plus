package com.snolred.wildplus.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * 실제 기기 버튼처럼 생긴 버튼. 스타일마다 색·모양이 다르다.
 * 마우스를 올리면 밝아지고, 못 누르는 상태면 회색으로 바랜다.
 */
public class UiButton extends AbstractButton {
    /** top·bottom 그라데이션, 테두리, 글자색, 글자 그림자, 모양 */
    public record Style(int top, int bottom, int border, int text, boolean shadow, Shape shape) {}

    public enum Shape { SOFT, KEY, CHIP, FLAT, PAPER }

    // 화면 공용 스타일
    public static final Style BLUE = new Style(0xFF4C8DF6, 0xFF2563D9, 0xFF123C8C, 0xFFFFFFFF, true, Shape.SOFT);
    public static final Style RED = new Style(0xFFF0595E, 0xFFC92A31, 0xFF701217, 0xFFFFFFFF, true, Shape.SOFT);
    public static final Style GREEN = new Style(0xFF34C075, 0xFF1E8A4C, 0xFF0D4A28, 0xFFFFFFFF, true, Shape.SOFT);
    public static final Style GOLD = new Style(0xFFFFDE85, 0xFFD9A22E, 0xFF6B4A0A, 0xFF3A2400, false, Shape.SOFT);
    public static final Style DARK = new Style(0xFF37414F, 0xFF242C37, 0xFF0B0F14, 0xFFE6EDF3, false, Shape.SOFT);
    public static final Style STEEL = new Style(0xFFF4F6F8, 0xFFC5CBD3, 0xFF5E6672, 0xFF1E2A3A, false, Shape.KEY);
    public static final Style WHITE = new Style(0xFFFFFFFF, 0xFFEEF1F5, 0xFFB5BDC8, 0xFF1E2430, false, Shape.SOFT);
    public static final Style ORANGE = new Style(0xFFFFA24A, 0xFFE0661A, 0xFF6E2C05, 0xFFFFFFFF, true, Shape.SOFT);
    public static final Style TEAL = new Style(0xFF2CC9B5, 0xFF138C7E, 0xFF06463F, 0xFFFFFFFF, true, Shape.SOFT);
    public static final Style FLAT_DARK = new Style(0xFF1C232D, 0xFF1C232D, 0xFF2E3846, 0xFFE6EDF3, false, Shape.FLAT);

    /** 카지노 칩 색 */
    public static Style chip(int color, int text) {
        return new Style(UiKit.lighter(color, 0.15F), UiKit.darker(color, 0.15F), UiKit.darker(color, 0.55F), text, text == 0xFFFFFFFF, Shape.CHIP);
    }

    private final Runnable action;
    private Style style;
    private boolean selected;
    private boolean leftAlign;

    public UiButton(int x, int y, int w, int h, Component label, Style style, Runnable action) {
        super(x, y, w, h, label);
        this.style = style;
        this.action = action;
    }

    public static UiButton of(int x, int y, int w, int h, String label, Style style, Runnable action) {
        return new UiButton(x, y, w, h, Component.literal(label), style, action);
    }

    public UiButton style(Style s) {
        this.style = s;
        return this;
    }

    /** 글자를 왼쪽 정렬 (목록 줄) */
    public UiButton left() {
        this.leftAlign = true;
        return this;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = this.active;
        boolean hover = on && isHoveredOrFocused();
        int top = style.top(), bottom = style.bottom(), border = style.border(), text = style.text();
        if (!on) {
            top = UiKit.mix(top, 0xFF9AA0A8, 0.65F);
            bottom = UiKit.mix(bottom, 0xFF8A9098, 0.65F);
            border = UiKit.mix(border, 0xFF5A6068, 0.6F);
            text = 0xFF4A4F57;
        } else if (hover) {
            top = UiKit.lighter(top, 0.18F);
            bottom = UiKit.lighter(bottom, 0.12F);
        }
        switch (style.shape()) {
            case KEY -> {
                // 금속 키패드: 두꺼운 아래 턱
                UiKit.round(g, x, y + 1, w, h, 2, UiKit.darker(border, 0.3F));
                UiKit.round(g, x, y, w, h - 1, 2, border);
                UiKit.roundGradient(g, x + 1, y + 1, w - 2, h - 3, 2, top, bottom);
                g.fill(x + 2, y + 1, x + w - 2, y + 2, 0xAAFFFFFF);
            }
            case CHIP -> {
                // 카지노 칩: 둥근 판 + 흰 점선 테
                UiKit.round(g, x, y + 1, w, h, 3, 0x55000000);
                UiKit.round(g, x, y, w, h, 3, border);
                UiKit.roundGradient(g, x + 1, y + 1, w - 2, h - 2, 3, top, bottom);
                UiKit.dashed(g, x + 2, y + 2, w - 4, h - 4, 2, 0xCCFFFFFF);
            }
            case FLAT -> {
                UiKit.round(g, x, y, w, h, 2, hover ? UiKit.lighter(border, 0.2F) : border);
                UiKit.round(g, x + 1, y + 1, w - 2, h - 2, 2, top);
            }
            case PAPER -> {
                UiKit.round(g, x, y + 1, w, h, 1, 0x33000000);
                UiKit.round(g, x, y, w, h, 1, border);
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, top);
            }
            default -> {
                UiKit.round(g, x, y + 1, w, h, 2, 0x40000000);
                UiKit.round(g, x, y, w, h, 2, border);
                UiKit.roundGradient(g, x + 1, y + 1, w - 2, h - 2, 2, top, bottom);
                g.fill(x + 3, y + 1, x + w - 3, y + 2, 0x55FFFFFF);
            }
        }
        if (selected) UiKit.outline(g, x - 1, y - 1, w + 2, h + 2, 0xFFFFE066);
        Component msg = getMessage();
        var font = UiKit.font();
        int tw = font.width(msg);
        int tx = x + (w - tw) / 2, ty = y + (h - 7) / 2;
        if (tw > w - 4 || leftAlign) tx = x + (leftAlign ? 8 : 2);
        g.text(font, msg, tx, ty, text, style.shadow() && on);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
