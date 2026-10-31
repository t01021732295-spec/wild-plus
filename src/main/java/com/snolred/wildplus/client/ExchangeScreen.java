package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.ExchangeMenu;
import com.snolred.wildplus.registry.ModItems3;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 환전소: 은행 창구 느낌. 위에는 검은 LED 환율 고시판, 가운데 동전 3종 카드, 아래 올리기(초록)·내리기(파랑) 창구 버튼.
 */
public class ExchangeScreen extends AbstractContainerScreen<ExchangeMenu> {
    private static final int W = 228, H = 218;

    public ExchangeScreen(ExchangeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        String[] labels = {"동화 10 → 은화 1", "은화 10 → 금화 1", "금화 1 → 은화 10", "은화 1 → 동화 10"};
        int[] ids = {ExchangeMenu.C2S, ExchangeMenu.S2G, ExchangeMenu.G2S, ExchangeMenu.S2C};
        for (int r = 0; r < 4; r++) {
            int y = topPos + 96 + r * 19 + (r >= 2 ? 14 : 0);
            int id = ids[r];
            UiButton.Style st = r < 2 ? UiButton.GREEN : UiButton.BLUE;
            btn(labels[r], id, leftPos + 10, y, 136, st);
            btn("전부", id + 1, leftPos + 150, y, 68, UiButton.WHITE);
        }
        btn("가장 적은 개수로 정리", ExchangeMenu.TIDY, leftPos + 10, topPos + H - 26, 124, UiButton.GOLD);
        btn("전부 동화로", ExchangeMenu.ALL_COPPER, leftPos + 138, topPos + H - 26, 80, UiButton.WHITE);
    }

    private void btn(String label, int id, int x, int y, int w, UiButton.Style style) {
        addRenderableWidget(UiButton.of(x, y, w, 17, label, style, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 창구 판 (밝은 대리석색)
        UiKit.device(g, x, y, W, H, 0xFFF4F1EA, 0xFFDCD6CA);
        // 머리판 (짙은 초록 은행 간판)
        UiKit.round(g, x + 4, y + 4, W - 8, 14, 2, 0xFF0F4D3A);
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 17, 0xFF1C7458, 0xFF0F4D3A);
        // LED 환율 고시판
        UiKit.screen(g, x + 10, y + 24, W - 20, 26, 0xFF0B0B0B, 0xFF151515);
        for (int i = 0; i < W - 20; i += 2) g.fill(x + 10 + i, y + 24, x + 11 + i, y + 50, 0x10FFFFFF);
        // 동전 카드 3개
        int[] tints = {0xFFF6E3D6, 0xFFEDEFF2, 0xFFFFF3CF};
        int[] borders = {0xFFB87333, 0xFF9AA2AD, 0xFFD9A22E};
        for (int k = 0; k < 3; k++) {
            int cx = x + 10 + k * 70;
            UiKit.card(g, cx, y + 54, 66, 26, tints[k], borders[k]);
        }
        g.item(new ItemStack(ModItems3.COPPER_COIN.get()), x + 14, y + 59);
        g.item(new ItemStack(ModItems3.SILVER_COIN.get()), x + 84, y + 59);
        g.item(new ItemStack(ModItems3.GOLD_COIN.get()), x + 154, y + 59);
        // 구역 띠
        UiKit.hline(g, x + 8, y + 84, W - 16, 0xFFCFC7B8);
        UiKit.hline(g, x + 8, y + 135, W - 16, 0xFFCFC7B8);
        UiKit.hline(g, x + 8, y + H - 32, W - 16, 0xFFCFC7B8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "₩ 환전 창구", 10, 7, 0xFFFFFFFF);
        UiKit.right(g, "수수료 없음", W - 10, 7, 0xFFB8F0D8);
        // 고시판 (호박색 LED 글자)
        UiKit.text(g, "환율 고시", 16, 28, 0xFFFF9F1A);
        UiKit.right(g, "금화 1 = 은화 10 = 동화 100", W - 16, 28, 0xFFFFC34D);
        UiKit.text(g, "● 영업 중", 16, 39, 0xFF56E39F);
        UiKit.right(g, "올리기·내리기 같은 비율", W - 16, 39, 0xFFFF9F1A);
        // 동전 수
        long[] counts = {menu.copper(), menu.silver(), menu.gold()};
        String[] names = {"동화", "은화", "금화"};
        int[] colors = {0xFF8A4A1A, 0xFF4A525C, 0xFF7A5400};
        for (int k = 0; k < 3; k++) {
            int cx = 10 + k * 70;
            UiKit.text(g, names[k], cx + 22, 57, colors[k]);
            UiKit.bold(g, String.format("%,d", counts[k]), cx + 22, 68, UiKit.INK);
        }
        UiKit.bold(g, "▲ 올리기", 10, 86, 0xFF1E8A4C);
        UiKit.text(g, "작은 동전을 큰 동전으로", 66, 86, UiKit.INK_SOFT);
        UiKit.bold(g, "▼ 내리기", 10, 138, 0xFF2563D9);
        UiKit.text(g, "잔돈 만들기", 66, 138, UiKit.INK_SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}
