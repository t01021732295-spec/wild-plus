package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.CasinoMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 카지노 테이블: 나무 테두리(레일) + 초록 펠트 + 금색 선.
 * 판돈은 칩 버튼으로, 동전·주사위는 그림으로, 슬롯머신은 릴 3개 창으로 보여준다.
 */
public class CasinoScreen extends AbstractContainerScreen<CasinoMenu> {
    private static final int W = 252, H = 232;
    private static final int FELT_TOP = 0xFF1E7A45, FELT_BOTTOM = 0xFF105A30, GOLD_LINE = 0xFFC9A54A, PRINT = 0xFFE9DCA8;
    private static final int[] SYMBOL_COLORS = {0xFFE0252B, 0xFFE8C21A, 0xFFD9A22E, 0xFFD0101A, 0xFF1AA8D9};

    public CasinoScreen(CasinoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        // 칩 (판돈)
        btn("+10", CasinoMenu.BET_10, x + 14, y + 42, 38, UiButton.chip(0xFFF2F2F2, 0xFF1E2430));
        btn("+100", CasinoMenu.BET_100, x + 56, y + 42, 38, UiButton.chip(0xFFD02A2A, 0xFFFFFFFF));
        btn("+1000", CasinoMenu.BET_1000, x + 98, y + 42, 42, UiButton.chip(0xFF1E1E22, 0xFFFFFFFF));
        btn("×2", CasinoMenu.BET_DOUBLE, x + 144, y + 42, 34, UiButton.chip(0xFF2A5FD0, 0xFFFFFFFF));
        btn("10동", CasinoMenu.BET_RESET, x + 182, y + 42, 56, UiButton.chip(0xFF7A7F88, 0xFFFFFFFF));
        // 동전 던지기
        btn("앞", CasinoMenu.COIN_HEADS, x + 128, y + 74, 54, UiButton.GOLD);
        btn("뒤", CasinoMenu.COIN_TAILS, x + 186, y + 74, 52, UiButton.GOLD);
        // 주사위
        btn("소 1~3", CasinoMenu.DICE_SMALL, x + 128, y + 98, 36, UiButton.WHITE);
        btn("대 4~6", CasinoMenu.DICE_BIG, x + 166, y + 98, 36, UiButton.WHITE);
        btn("6 ×5", CasinoMenu.DICE_SIX, x + 204, y + 98, 34, UiButton.RED);
        // 슬롯
        btn("SPIN", CasinoMenu.SLOTS, x + 190, y + 140, 48, UiButton.RED);
    }

    private void btn(String label, int id, int x, int y, int w, UiButton.Style style) {
        int h = id == CasinoMenu.SLOTS ? 26 : 18;
        addRenderableWidget(UiButton.of(x, y, w, h, label, style, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 나무 레일
        UiKit.device(g, x, y, W, H, 0xFF7A4520, 0xFF3E200C);
        UiKit.speckle(g, x + 2, y + 2, W - 4, H - 4, 7, 240, 0x22000000, 0x18FFE0B0);
        // 펠트
        UiKit.round(g, x + 6, y + 6, W - 12, H - 12, 3, 0xFF0A3A1E);
        UiKit.roundGradient(g, x + 7, y + 7, W - 14, H - 14, 3, FELT_TOP, FELT_BOTTOM);
        UiKit.speckle(g, x + 7, y + 7, W - 14, H - 14, 3, 300, 0x14000000, 0x10FFFFFF);
        UiKit.outline(g, x + 10, y + 10, W - 20, H - 20, UiKit.alpha(GOLD_LINE, 0x99));
        // 판돈·보유 표시판
        UiKit.round(g, x + 14, y + 20, W - 28, 18, 2, 0x55000000);
        // 칩 더미 그림
        for (int i = 0; i < 4; i++) {
            int c = i % 2 == 0 ? 0xFFD02A2A : 0xFFF2F2F2;
            UiKit.round(g, x + 18, y + 31 - i * 2, 12, 4, 1, UiKit.darker(c, 0.4F));
            UiKit.round(g, x + 18, y + 30 - i * 2, 12, 4, 1, c);
        }
        // 구분선
        UiKit.hline(g, x + 14, y + 66, W - 28, UiKit.alpha(GOLD_LINE, 0xAA));
        UiKit.hline(g, x + 14, y + 122, W - 28, UiKit.alpha(GOLD_LINE, 0xAA));
        // 동전 그림
        int game = menu.game();
        coin(g, x + 104, y + 74, game == CasinoMenu.GAME_COIN ? menu.r1() : -1);
        // 주사위 그림
        die(g, x + 105, y + 98, game == CasinoMenu.GAME_DICE ? menu.r1() : 6);
        // 슬롯머신 몸체 + 릴 3개
        UiKit.round(g, x + 14, y + 136, 166, 34, 3, 0xFF3A0A0E);
        UiKit.roundGradient(g, x + 15, y + 137, 164, 32, 3, 0xFFB01822, 0xFF6E0A12);
        for (int i = 0; i < 3; i++) {
            int rx = x + 22 + i * 52;
            UiKit.round(g, rx - 1, y + 140, 48, 26, 2, 0xFF1A0004);
            g.fillGradient(rx, y + 141, rx + 46, y + 153, 0xFFB8BCC4, 0xFFFFFFFF);
            g.fillGradient(rx, y + 153, rx + 46, y + 165, 0xFFFFFFFF, 0xFFB8BCC4);
            UiKit.hline(g, rx, y + 153, 46, 0x22D02A2A);
        }
        // 슬롯 손잡이 (레버)
        g.fill(x + 183, y + 140, x + 185, y + 162, 0xFF9AA2AD);
        UiKit.circle(g, x + 181, y + 135, 6, 0xFFE0252B);
        // 결과 화면 (전광판)
        UiKit.screen(g, x + 16, y + 186, W - 32, 32, 0xFF0D0D10, 0xFF1A1A20);
        int o = menu.outcome();
        if (game != 0 && o == 1) {
            for (int i = 0; i < W - 32; i += 6) UiKit.led(g, x + 18 + i, y + 212, (i / 6) % 2 == 0 ? 0xFFFFD84A : 0xFFFF5A5A);
        }
    }

    /** 금화: side -1 아직 안 던짐, 0 앞, 1 뒤 */
    private static void coin(GuiGraphicsExtractor g, int x, int y, int side) {
        UiKit.circle(g, x, y + 1, 18, 0x66000000);
        UiKit.circle(g, x, y, 18, 0xFF8A6410);
        UiKit.circle(g, x + 1, y + 1, 16, side == 1 ? 0xFFD9B04A : 0xFFF2CC5A);
        UiKit.circle(g, x + 4, y + 4, 10, side == 1 ? 0xFFC99A2E : 0xFFE8B83A);
    }

    /** 주사위 (흰 정육면체 + 점) */
    private static void die(GuiGraphicsExtractor g, int x, int y, int n) {
        UiKit.round(g, x, y + 1, 17, 17, 2, 0x66000000);
        UiKit.round(g, x, y, 17, 17, 2, 0xFF8A8F98);
        UiKit.roundGradient(g, x + 1, y + 1, 15, 15, 2, 0xFFFFFFFF, 0xFFE3E6EA);
        int[][] pos = switch (n) {
            case 1 -> new int[][]{{1, 1}};
            case 2 -> new int[][]{{0, 0}, {2, 2}};
            case 3 -> new int[][]{{0, 0}, {1, 1}, {2, 2}};
            case 4 -> new int[][]{{0, 0}, {2, 0}, {0, 2}, {2, 2}};
            case 5 -> new int[][]{{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}};
            default -> new int[][]{{0, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {2, 2}};
        };
        int pip = n == 1 ? 0xFFD0101A : 0xFF1E2430;
        for (int[] p : pos) {
            int px = x + 3 + p[0] * 4, py = y + 3 + p[1] * 4;
            UiKit.round(g, px, py, 3, 3, 1, pip);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.center(g, "♠ ♥  WILD CASINO  ♦ ♣", W / 2, 12, 0xFFE9DCA8);
        UiKit.text(g, "판돈", 36, 25, 0xFFB8D8C0);
        UiKit.bold(g, String.format("%,d동", menu.bet()), 60, 25, 0xFFFFD84A);
        UiKit.right(g, String.format("보유 %,d동", menu.cash()), W - 20, 25, 0xFFFFFFFF);
        // 게임 이름 (펠트에 인쇄된 글씨)
        UiKit.bold(g, "COIN FLIP", 16, 74, PRINT);
        UiKit.text(g, "맞히면 ×1.95", 16, 84, 0xFFB8D8C0);
        UiKit.bold(g, "DICE", 16, 98, PRINT);
        UiKit.text(g, "소·대 ×1.9", 16, 108, 0xFFB8D8C0);
        UiKit.bold(g, "SLOTS", 16, 126, PRINT);
        UiKit.text(g, "3개: 체리5 레몬8 종20 7=50 ◆100 · 2개: 절반", 16, 174, 0xFFB8D8C0);
        // 동전 앞뒤 글자
        if (menu.game() == CasinoMenu.GAME_COIN) UiKit.center(g, menu.r1() == 0 ? "앞" : "뒤", 113, 79, 0xFF6B4A0A);
        else UiKit.center(g, "₩", 113, 79, 0xFF6B4A0A);
        // 릴 그림
        boolean slots = menu.game() == CasinoMenu.GAME_SLOTS;
        int[] rs = slots ? new int[]{menu.r1(), menu.r2(), menu.r3()} : new int[]{3, 3, 3};
        for (int i = 0; i < 3; i++) {
            String sym = CasinoMenu.SYMBOLS[rs[i]];
            UiKit.big(g, sym, 22 + i * 52 + 23 - UiKit.bigWidth(sym, 2) / 2, 146, 2, slots ? SYMBOL_COLORS[rs[i]] : 0xFFB8BCC4);
        }
        // 결과
        String result = switch (menu.game()) {
            case CasinoMenu.GAME_COIN -> "동전: " + (menu.r1() == 0 ? "앞" : "뒤");
            case CasinoMenu.GAME_DICE -> "주사위: " + menu.r1();
            case CasinoMenu.GAME_SLOTS -> "슬롯: " + CasinoMenu.SYMBOLS[menu.r1()] + " · " + CasinoMenu.SYMBOLS[menu.r2()] + " · "
                    + CasinoMenu.SYMBOLS[menu.r3()];
            default -> "칩으로 판돈을 정하고 게임을 고르세요";
        };
        UiKit.text(g, result, 22, 189, 0xFFDDDDDD);
        int o = menu.outcome();
        if (menu.game() != 0) {
            String line = switch (o) {
                case 1 -> String.format("WIN!  +%,d동 (세후)", menu.win());
                case 3 -> String.format("PUSH · %,d동 돌려받음", menu.win());
                default -> "LOSE…  다음엔 될 거예요";
            };
            UiKit.bold(g, line, 22, 200, o == 1 ? 0xFF7CFC6A : o == 3 ? 0xFFFFD84A : 0xFFFF6A6A);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}
