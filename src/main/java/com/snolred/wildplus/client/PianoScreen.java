package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 3.4 피아노 연주 화면: 두 옥타브 건반 (G3 ~ F#5, 소리 블록 음역 그대로).
 * 마우스로 건반을 누르거나 키보드로 친다 — 아래 옥타브 Z X C V B N M , (흰) · S D G H J (검은),
 * 위 옥타브 Q W E R T Y U I (흰) · 2 3 5 6 7 (검은). 악기 7가지 · 예시 곡 자동 연주.
 * 누른 음은 서버로 가서 피아노 자리에서 울린다 (근처 친구들도 들음): 버튼 번호 = 악기 × 25 + 음(0~24).
 */
public class PianoScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 300, H = 170;
    public static final String[] INSTRUMENTS = {"피아노", "벨", "플루트", "기타", "베이스", "실로폰", "8비트"};
    /** 음 번호 1~24 (0 = F#3 은 건반에서 뺌) → 흰 건반인가 */
    private static boolean white(int n) {
        int s = Math.floorMod(n - 6, 12); // 6 = C4 기준 반음
        return s == 0 || s == 2 || s == 4 || s == 5 || s == 7 || s == 9 || s == 11;
    }

    private static final String[] NAME = {"도", "도#", "레", "레#", "미", "파", "파#", "솔", "솔#", "라", "라#", "시"};
    private final List<Integer> whites = new ArrayList<>(), blacks = new ArrayList<>();
    private final long[] lit = new long[25];
    private int instrument;
    private long ticks;
    // 예시 곡: 반짝반짝 작은 별 (음 번호, 박자)
    private static final int[][] SONG = {{6, 2}, {6, 2}, {13, 2}, {13, 2}, {15, 2}, {15, 2}, {13, 4}, {11, 2}, {11, 2}, {10, 2}, {10, 2}, {8, 2}, {8, 2}, {6, 4},
            {13, 2}, {13, 2}, {11, 2}, {11, 2}, {10, 2}, {10, 2}, {8, 4}, {13, 2}, {13, 2}, {11, 2}, {11, 2}, {10, 2}, {10, 2}, {8, 4},
            {6, 2}, {6, 2}, {13, 2}, {13, 2}, {15, 2}, {15, 2}, {13, 4}, {11, 2}, {11, 2}, {10, 2}, {10, 2}, {8, 2}, {8, 2}, {6, 4}};
    private int songAt = -1;
    private long songNext;
    private final List<UiButton> instButtons = new ArrayList<>();
    private UiButton songButton;

    public PianoScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        for (int n = 1; n <= 24; n++) (white(n) ? whites : blacks).add(n);
    }

    private int kx0() {
        return leftPos + (W - whites.size() * 18) / 2;
    }

    private static final int KY = 62, WH = 70, BH = 42;

    @Override
    protected void init() {
        super.init();
        instButtons.clear();
        int bx = leftPos + 10;
        for (int i = 0; i < INSTRUMENTS.length; i++) {
            final int k = i;
            int w = UiKit.font().width(INSTRUMENTS[i]) + 10;
            instButtons.add(addRenderableWidget(UiButton.of(bx, topPos + 32, w, 15, INSTRUMENTS[i], UiButton.WHITE, () -> instrument = k)));
            bx += w + 3;
        }
        songButton = addRenderableWidget(UiButton.of(leftPos + W - 92, topPos + H - 22, 82, 15, "▶ 작은 별 연주", UiButton.GOLD, () -> {
            songAt = songAt < 0 ? 0 : -1;
            songNext = ticks;
        }));
        // 검은 건반을 먼저 넣어 클릭 우선 (겹치는 곳)
        for (int n : blacks) {
            int x = blackX(n);
            final int note = n;
            addRenderableWidget(new KeyButton(x, topPos + KY, 12, BH, () -> play(note)));
        }
        for (int i = 0; i < whites.size(); i++) {
            final int note = whites.get(i);
            addRenderableWidget(new KeyButton(kx0() + i * 18, topPos + KY, 18, WH, () -> play(note)));
        }
    }

    /** 검은 건반 x: 바로 앞 흰 건반 오른쪽 끝에 걸침 */
    private int blackX(int n) {
        int idx = whites.indexOf(n - 1);
        return kx0() + idx * 18 + 18 - 6;
    }

    private void play(int note) {
        if (note < 0 || note > 24) return;
        lit[note] = ticks + 5;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, instrument * 25 + note);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
        if (songAt >= 0 && ticks >= songNext) {
            if (songAt >= SONG.length) {
                songAt = -1;
            } else {
                play(SONG[songAt][0]);
                songNext = ticks + SONG[songAt][1] * 4L;
                songAt++;
            }
        }
        songButton.setMessage(Component.literal(songAt >= 0 ? "■ 멈추기" : "▶ 작은 별 연주"));
        for (int i = 0; i < instButtons.size(); i++) instButtons.get(i).style(i == instrument ? UiButton.BLUE : UiButton.WHITE);
    }

    // 키보드: 아래 옥타브 (흰 Z X C V B N M , / 검은 S D G H J) · 위 옥타브 (흰 Q W E R T Y U I / 검은 2 3 5 6 7)
    private static final int[] LOW_WHITE = {'Z', 'X', 'C', 'V', 'B', 'N', 'M', 44}, LOW_BLACK = {'S', 'D', 'G', 'H', 'J'};
    private static final int[] HIGH_WHITE = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I'}, HIGH_BLACK = {'2', '3', '5', '6', '7'};
    /** C4 부터: 흰 음 번호 · 검은 음 번호 */
    private static final int[] LOW_W_NOTE = {6, 8, 10, 11, 13, 15, 17, 18}, LOW_B_NOTE = {7, 9, 12, 14, 16};
    private static final int[] HIGH_W_NOTE = {18, 20, 22, 23, 25, 27, 29, 30}, HIGH_B_NOTE = {19, 21, 24, 26, 28};

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int k = event.key();
        if (!event.isEscape()) {
            int note = find(k, LOW_WHITE, LOW_W_NOTE);
            if (note < 0) note = find(k, LOW_BLACK, LOW_B_NOTE);
            if (note < 0) note = find(k, HIGH_WHITE, HIGH_W_NOTE);
            if (note < 0) note = find(k, HIGH_BLACK, HIGH_B_NOTE);
            if (note >= 0) {
                while (note > 24) note -= 12; // 위 옥타브 끝은 한 옥타브 내림 (음역 밖)
                play(note);
                return true;
            }
            if (k >= '1' && k <= '7' && k != '2' && k != '3' && k != '5' && k != '6') {
                // 1 · 4 = 악기 바꾸기 (앞 · 뒤)
                instrument = Math.floorMod(instrument + (k == '1' ? -1 : 1), INSTRUMENTS.length);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static int find(int key, int[] keys, int[] notes) {
        for (int i = 0; i < keys.length; i++) if (keys[i] == key) return notes[i];
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 반짝이는 검정 피아노 몸통 + 금 테 + 빨간 펠트
        UiKit.device(g, x, y, W, H, 0xFF2A2A30, 0xFF0E0E12);
        g.fill(x + 3, y + 28, x + W - 3, y + 29, 0xFFD9A22E);
        g.fill(x + 6, y + KY - 4, x + W - 6, y + KY - 1, 0xFF8E1520);
        for (int k = 0; k < 8; k++) g.fill(x + 10 + k * 36, y + 6, x + 20 + k * 36, y + 8, 0x22FFFFFF);
        // 흰 건반
        for (int i = 0; i < whites.size(); i++) {
            int n = whites.get(i);
            int kx = kx0() + i * 18, ky = y + KY;
            boolean on = lit[n] > ticks;
            g.fill(kx, ky, kx + 17, ky + WH, on ? 0xFFFFE27A : 0xFFFAFAF6);
            g.fillGradient(kx, ky + WH - 8, kx + 17, ky + WH, on ? 0xFFF2C14E : 0xFFE6E6E0, on ? 0xFFD9A22E : 0xFFC8C8C0);
            g.fill(kx + 17, ky, kx + 18, ky + WH, 0xFF7A7A80);
            if (Math.floorMod(n - 6, 12) == 0) g.fill(kx + 6, ky + WH - 14, kx + 11, ky + WH - 12, 0xFFD0303A); // 도 표시
        }
        // 검은 건반
        for (int n : blacks) {
            int kx = blackX(n), ky = y + KY;
            boolean on = lit[n] > ticks;
            g.fillGradient(kx, ky, kx + 12, ky + BH, on ? 0xFF5A78C8 : 0xFF2C2C32, on ? 0xFF2A3E80 : 0xFF0A0A0E);
            g.fill(kx + 2, ky + 1, kx + 4, ky + BH - 6, on ? 0x55FFFFFF : 0x22FFFFFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "🎹 " + this.title.getString().replace("🎹", "").trim(), 10, 8, 0xFFFFE9B0);
        UiKit.text(g, "악기: " + INSTRUMENTS[instrument] + " · 근처 친구들도 들어요", 10, 18, 0xFFB8B8C4);
        // 흰 건반 이름 (도 레 미…)
        for (int i = 0; i < whites.size(); i++) {
            int n = whites.get(i);
            String s = NAME[Math.floorMod(n - 6, 12)];
            int kx = kx0() - leftPos + i * 18;
            g.text(UiKit.font(), s, kx + 9 - UiKit.font().width(s) / 2, KY + WH - 22, 0xFF6B6B74, false);
        }
        UiKit.text(g, "⌨ 아래 Z~, (검은 S D G H J) · 위 Q~I (검은 2 3 5 6 7)", 10, H - 30, 0xFF9A9AA8);
        UiKit.text(g, "1 · 4 = 악기 바꾸기", 10, H - 19, 0xFF9A9AA8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    /** 건반: 그림은 배경에서 그리고, 버튼은 누르기만 (투명) */
    private static final class KeyButton extends UiButton {
        KeyButton(int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty(), UiButton.WHITE, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused()) g.fill(getX(), getY() + getHeight() - 3, getX() + getWidth() - 1, getY() + getHeight(), 0x663B82F6);
        }
    }
}
