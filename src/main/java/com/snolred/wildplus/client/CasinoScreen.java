package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.CasinoMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 카지노 테이블 화면: 판돈 조절 + 동전 던지기 · 주사위 · 슬롯머신 */
public class CasinoScreen extends AbstractContainerScreen<CasinoMenu> {
    private static final int W = 232, H = 200;

    public CasinoScreen(CasinoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        btn("+10", CasinoMenu.BET_10, x + 8, y + 44, 30);
        btn("+100", CasinoMenu.BET_100, x + 40, y + 44, 34);
        btn("+1000", CasinoMenu.BET_1000, x + 76, y + 44, 40);
        btn("×2", CasinoMenu.BET_DOUBLE, x + 118, y + 44, 26);
        btn("10동", CasinoMenu.BET_RESET, x + 146, y + 44, 30);
        btn("앞", CasinoMenu.COIN_HEADS, x + 84, y + 74, 40);
        btn("뒤", CasinoMenu.COIN_TAILS, x + 126, y + 74, 40);
        btn("소 1~3", CasinoMenu.DICE_SMALL, x + 84, y + 98, 46);
        btn("대 4~6", CasinoMenu.DICE_BIG, x + 132, y + 98, 46);
        btn("6 (5배)", CasinoMenu.DICE_SIX, x + 180, y + 98, 44);
        btn("돌리기!", CasinoMenu.SLOTS, x + 84, y + 122, 60);
    }

    private void btn(String label, int id, int x, int y, int w) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(x, y, w, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.inset(g, leftPos + 7, topPos + 17, W - 14, 24, 0xFF0E4A26);
        UiKit.inset(g, leftPos + 7, topPos + 152, W - 14, 40, 0xFF102018);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, Component.literal(String.format("판돈 %,d동", menu.bet())), 12, 25, 0xFFFFD84A, true);
        g.text(this.font, Component.literal(String.format("가진 돈 %,d동", menu.cash())), 120, 25, 0xFFE0E0E0, true);
        g.text(this.font, Component.literal("동전 던지기 ×1.95"), 8, 79, UiKit.TEXT, false);
        g.text(this.font, Component.literal("주사위 ×1.9"), 8, 103, UiKit.TEXT, false);
        g.text(this.font, Component.literal("슬롯머신"), 8, 127, UiKit.TEXT, false);
        g.text(this.font, Component.literal("3개: 체리5·레몬8·종20·7=50·◆100배 / 2개: 절반"), 8, 140, 0xFF606060, false);
        // 결과
        String result = switch (menu.game()) {
            case CasinoMenu.GAME_COIN -> "동전: " + (menu.r1() == 0 ? "앞" : "뒤");
            case CasinoMenu.GAME_DICE -> "주사위: " + menu.r1();
            case CasinoMenu.GAME_SLOTS -> "슬롯: [" + CasinoMenu.SYMBOLS[menu.r1()] + "] [" + CasinoMenu.SYMBOLS[menu.r2()] + "] ["
                    + CasinoMenu.SYMBOLS[menu.r3()] + "]";
            default -> "판돈을 정하고 게임을 고르세요";
        };
        g.text(this.font, Component.literal(result), 14, 158, 0xFFFFFFFF, true);
        int o = menu.outcome();
        if (menu.game() != 0) {
            String line = switch (o) {
                case 1 -> String.format("당첨! %,d동 받음 (세금 뗀 금액)", menu.win());
                case 3 -> String.format("두 개 같음 · %,d동 돌려받음", menu.win());
                default -> "꽝…";
            };
            g.text(this.font, Component.literal(line), 14, 172, o == 1 ? 0xFF7CFC6A : o == 3 ? 0xFFFFD84A : 0xFFFF6A6A, true);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}
