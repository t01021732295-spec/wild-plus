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
    private static final int W = 262, H = 292;

    public ExchangeScreen(ExchangeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int[] ids = {ExchangeMenu.C2S, ExchangeMenu.S2G, ExchangeMenu.G2D, ExchangeMenu.D2E,
                ExchangeMenu.E2D, ExchangeMenu.D2G, ExchangeMenu.G2S, ExchangeMenu.S2C};
        rateButtons.clear();
        for (int r = 0; r < 8; r++) {
            int y = topPos + 96 + r * 18 + (r >= 4 ? 16 : 0);
            int id = ids[r];
            UiButton.Style st = r < 4 ? UiButton.GREEN : UiButton.BLUE;
            rateButtons.add(btn("", id, leftPos + 10, y, 166, st));
            btn("전부", id + 1, leftPos + 180, y, 72, UiButton.WHITE);
        }
        updateLabels();
        btn("가장 적은 개수로 정리", ExchangeMenu.TIDY, leftPos + 10, topPos + H - 26, 150, UiButton.GOLD);
        btn("전부 동화로", ExchangeMenu.ALL_COPPER, leftPos + 164, topPos + H - 26, 88, UiButton.WHITE);
    }

    private final java.util.List<UiButton> rateButtons = new java.util.ArrayList<>();

    private void updateLabels() {
        if (rateButtons.size() < 8) return;
        rateButtons.get(0).setMessage(Component.literal("동화 " + menu.silverBuy() + " → 은화 1"));
        rateButtons.get(1).setMessage(Component.literal("은화 " + menu.goldBuy() + " → 금화 1"));
        rateButtons.get(2).setMessage(Component.literal("금화 10 → 다이아화 1"));
        rateButtons.get(3).setMessage(Component.literal("다이아화 10 → 에메랄드화 1"));
        rateButtons.get(4).setMessage(Component.literal("에메랄드화 1 → 다이아화 10"));
        rateButtons.get(5).setMessage(Component.literal("다이아화 1 → 금화 10"));
        rateButtons.get(6).setMessage(Component.literal("금화 1 → 은화 " + menu.goldSell()));
        rateButtons.get(7).setMessage(Component.literal("은화 1 → 동화 " + menu.silverSell()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    private UiButton btn(String label, int id, int x, int y, int w, UiButton.Style style) {
        return addRenderableWidget(UiButton.of(x, y, w, 17, label, style, () -> {
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
        // 동전 카드 5개
        int[] tints = {0xFFF6E3D6, 0xFFEDEFF2, 0xFFFFF3CF, 0xFFDDF8F8, 0xFFDDF6E4};
        int[] borders = {0xFFB87333, 0xFF9AA2AD, 0xFFD9A22E, 0xFF2CB8C4, 0xFF1E9E54};
        net.minecraft.world.item.Item[] coins = {ModItems3.COPPER_COIN.get(), ModItems3.SILVER_COIN.get(), ModItems3.GOLD_COIN.get(),
                ModItems3.DIAMOND_COIN.get(), com.snolred.wildplus.registry.ModItems13.EMERALD_COIN.get()};
        for (int k = 0; k < 5; k++) {
            int cx = x + 10 + k * 49;
            UiKit.card(g, cx, y + 54, 46, 26, tints[k], borders[k]);
            g.item(new ItemStack(coins[k]), cx + 2, y + 59);
        }
        // 구역 띠
        UiKit.hline(g, x + 8, y + 84, W - 16, 0xFFCFC7B8);
        UiKit.hline(g, x + 8, y + 167, W - 16, 0xFFCFC7B8);
        UiKit.hline(g, x + 8, y + H - 32, W - 16, 0xFFCFC7B8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "₩ 환전 창구", 10, 7, 0xFFFFFFFF);
        UiKit.right(g, "고정 환율", W - 10, 7, 0xFFB8F0D8);
        // 고시판: 고정 환율 안내 (호박색 LED 글자)
        UiKit.text(g, "동화 10 = 은화 1 · 은화 10 = 금화 1", 16, 28, 0xFFFFC34D);
        UiKit.text(g, "금화 10 = 다이아화 1 · 다이아화 10 = 에메랄드화 1", 16, 39, 0xFF7FE8F0);
        UiKit.right(g, "수수료 없음", W - 16, 28, 0xFF8A6A3A);
        // 동전 수
        long[] counts = {menu.copper(), menu.silver(), menu.gold(), menu.diamond(), menu.emerald()};
        String[] names = {"동화", "은화", "금화", "다이아", "에메랄드"};
        int[] colors = {0xFF8A4A1A, 0xFF4A525C, 0xFF7A5400, 0xFF0E7C86, 0xFF0E7A3A};
        for (int k = 0; k < 5; k++) {
            int cx = 10 + k * 49;
            UiKit.text(g, names[k], cx + 20, 57, colors[k]);
            UiKit.bold(g, String.format("%,d", counts[k]), cx + 20, 68, UiKit.INK);
        }
        UiKit.bold(g, "▲ 올리기", 10, 86, 0xFF1E8A4C);
        UiKit.text(g, "작은 동전을 큰 동전으로", 66, 86, UiKit.INK_SOFT);
        UiKit.bold(g, "▼ 내리기", 10, 170, 0xFF2563D9);
        UiKit.text(g, "잔돈 만들기", 66, 170, UiKit.INK_SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}
