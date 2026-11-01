package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.AutoCrafterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 자동 제작대: 나무+철 기계 판.
 * 왼쪽 금테 3×3 설계도 → 화살표 → 만들 물건(유리 진열칸) → 결과 칸,
 * 아래 재료 9칸(호퍼 입력), 그 아래 내 가방.
 */
public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {
    private static final int W = 176, H = 216;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFF8A5A33, 0xFF4E3220);
        UiKit.screws(g, x, y, W, 128);
        // 설계도 판 (짙은 청사진)
        int px = x + AutoCrafterMenu.PAT_X - 4, py = y + AutoCrafterMenu.PAT_Y - 4;
        UiKit.round(g, px - 1, py - 1, 62, 62, 3, 0xFFF2C14E);
        g.fillGradient(px, py, px + 60, py + 60, 0xFF1D3B5C, 0xFF132840);
        for (int i = 0; i <= 60; i += 6) {
            g.fill(px + i, py, px + i + 1, py + 60, 0x22FFFFFF);
            g.fill(px, py + i, px + 60, py + i + 1, 0x22FFFFFF);
        }
        // 화살표 (작동 중이면 초록, 조합법이 없으면 빨강)
        boolean ok = menu.hasRecipe();
        int ac = ok ? 0xFF4ADE80 : (menu.patternEmpty() ? 0xFF9CA3AF : 0xFFE5484D);
        int ay = y + AutoCrafterMenu.PREVIEW_Y + 7;
        int ax = x + 72;
        g.fill(ax, ay, ax + 16, ay + 3, ac);
        for (int k = 0; k < 5; k++) g.fill(ax + 16 + k, ay - 4 + k, ax + 17 + k, ay + 7 - k, ac);
        // 미리보기 진열칸 (크게)
        int vx = x + AutoCrafterMenu.PREVIEW_X - 5, vy = y + AutoCrafterMenu.PREVIEW_Y - 5;
        UiKit.round(g, vx - 1, vy - 1, 28, 28, 3, 0xFFF2C14E);
        g.fillGradient(vx, vy, vx + 26, vy + 26, 0xFFFFF7E0, 0xFFE8D7A8);
        // 두 번째 화살표
        int bx = x + 126;
        g.fill(bx, ay, bx + 10, ay + 3, ac);
        for (int k = 0; k < 4; k++) g.fill(bx + 10 + k, ay - 3 + k, bx + 11 + k, ay + 6 - k, ac);
        // 결과 칸 (큰 테두리)
        int ox = x + AutoCrafterMenu.OUT_X - 5, oy = y + AutoCrafterMenu.OUT_Y - 5;
        UiKit.round(g, ox - 1, oy - 1, 28, 28, 3, 0xFF22A35A);
        g.fill(ox, oy, ox + 26, oy + 26, 0xFF2B1A10);
        // 재료 띠
        UiKit.round(g, x + 5, y + AutoCrafterMenu.BUF_Y - 13, W - 10, 33, 2, 0xFF2B1A10);
        g.fillGradient(x + 6, y + AutoCrafterMenu.BUF_Y - 12, x + W - 6, y + AutoCrafterMenu.BUF_Y + 19, 0xFF5A3A22, 0xFF3E2716);
        UiKit.inventoryTray(g, x + 4, y + AutoCrafterMenu.INV_Y - 4, W - 8, 82);
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            if (s.index < 9) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
            } else if (s.index == 9) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
            } else if (s.index < 19) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
                UiKit.outline(g, x + s.x - 1, y + s.y - 1, 18, 18, 0x66F2C14E);
            } else if (s.index == 19) {
                // 미리보기는 진열칸 배경 그대로
            } else {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.CLASSIC);
            }
        }
        // 레드스톤 정지 표시등
        UiKit.led(g, x + W - 14, y + 7, ok ? UiKit.GREEN : 0xFF6B7280);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, this.title.getString(), 8, 6, 0xFFFFF1D6);
        UiKit.text(g, "설계도", AutoCrafterMenu.PAT_X, AutoCrafterMenu.PAT_Y + 57, 0xFFF2C14E);
        UiKit.center(g, "만들 물건", AutoCrafterMenu.PREVIEW_X + 8, AutoCrafterMenu.PREVIEW_Y - 16, 0xFFFFF1D6);
        UiKit.center(g, "결과", AutoCrafterMenu.OUT_X + 8, AutoCrafterMenu.OUT_Y - 16, 0xFF86EFAC);
        String state = menu.hasRecipe() ? "● 자동 제작 중" : (menu.patternEmpty() ? "설계도를 채워 주세요" : "맞는 조합법 없음");
        UiKit.center(g, state, 112, AutoCrafterMenu.PREVIEW_Y + 26, menu.hasRecipe() ? 0xFFBBF7D0 : 0xFFFCA5A5);
        UiKit.text(g, "재료 칸  (위·옆 호퍼 입력 / 아래로 출력)", 8, AutoCrafterMenu.BUF_Y - 10, 0xFFE8D7A8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}
