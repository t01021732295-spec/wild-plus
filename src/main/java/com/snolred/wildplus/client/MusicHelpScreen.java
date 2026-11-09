package com.snolred.wildplus.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 음악 · 악보 사용 설명서 */
public class MusicHelpScreen extends Screen {
    private record Sec(String title, int color, List<String> lines) {}

    private static final int GOLD = 0xFFFFD36B, BLUE = 0xFF8FD3FF, GREEN = 0xFF9BE8A0;
    private static final List<Sec> DISC = List.of(
            new Sec("1. 음악 파일 준비", GOLD, List.of("노래를 .ogg 파일로 준비해요. (mp3는 바로 안 돼요)", "mp3는 Audacity 같은 무료 프로그램으로 ogg로 내보내면 돼요.", "파일 하나당 최대 40MB, 파일 이름이 곡 이름이 돼요.")),
            new Sec("2. 서버에 넣기", BLUE, List.of("서버 폴더 안 wildplus_music 폴더에 파일을 넣어요.", "(혼자 하는 세계라면 게임 폴더의 wildplus_music)", "폴더는 게임을 한 번 켜면 자동으로 생겨요.", "친구들은 따로 받을 필요 없이 서버가 자동으로 보내 줘요.")),
            new Sec("3. 바로 틀기", GREEN, List.of("음악 플레이어 우클릭 → [곡 고르기] → 곡을 눌러요.", "[음량] [들리는 범위] [반복]은 누를 때마다 바뀌어요.", "[정지]로 멈춰요. 처음 트는 곡은 전송되는 동안 잠깐 걸려요.")),
            new Sec("4. 음반으로 만들기", GOLD, List.of("빈 음반을 손에 들고 [음반에 곡 담기] → 곡 선택.", "만든 음반을 들고 음악 플레이어를 우클릭하면 재생돼요.")));
    private static final List<Sec> SHEET = List.of(
            new Sec("1. 악보 파일 준비", GOLD, List.of("MIDI 파일(.mid)을 준비해요. 인터넷에서 곡 이름 + midi로 찾거나", "작곡 프로그램에서 내보낼 수 있어요. 최대 1MB.")),
            new Sec("2. 서버에 넣기", BLUE, List.of("음악과 같은 wildplus_music 폴더에 .mid 파일을 넣어요.")),
            new Sec("3. 악보 만들기", GREEN, List.of("빈 악보를 손에 들고 음악 플레이어를 우클릭해요.", "[악보에 곡 담기] → 곡을 고르면 악보가 완성돼요.")),
            new Sec("4. 피아노로 연주", GOLD, List.of("완성된 악보를 손에 들고 피아노를 우클릭하면 한 번 자동 연주해요.", "연주 중에 악보를 들고 다시 우클릭하면 멈춰요.", "드럼 트랙은 연주되지 않고, 아주 빠른 곡은 음이 몰릴 수 있어요.")));

    private int tab, scroll;

    public MusicHelpScreen() {
        super(Component.literal("사용법"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 150, y = Math.max(8, height / 2 - 112);
        addRenderableWidget(UiButton.of(x + 8, y + 22, 70, 16, "🎵 음악 · 음반", UiButton.BLUE, () -> {
            tab = 0;
            scroll = 0;
        }));
        addRenderableWidget(UiButton.of(x + 84, y + 22, 70, 16, "🎹 악보 · 피아노", UiButton.GOLD, () -> {
            tab = 1;
            scroll = 0;
        }));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (sy * 14)));
        return true;
    }

    private List<Sec> cur() {
        return tab == 0 ? DISC : SHEET;
    }

    private int contentH() {
        int h = 0;
        for (Sec s : cur()) h += 16 + s.lines().size() * 12 + 6;
        return h;
    }

    private int maxScroll() {
        return Math.max(0, contentH() - 176);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        int x = width / 2 - 150, y = Math.max(8, height / 2 - 112), w = 300, h = 224;
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(x, y, x + w, y + h, 0xF0181C2C);
        g.fill(x, y, x + w, y + 1, GOLD);
        g.text(font, "사용 설명서", x + 10, y + 8, GOLD, true);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.enableScissor(x + 4, y + 42, x + w - 4, y + h - 4);
        int ty = y + 44 - scroll;
        for (Sec s : cur()) {
            g.text(font, s.title(), x + 12, ty, s.color(), true);
            ty += 14;
            for (String l : s.lines()) {
                g.text(font, l, x + 18, ty, 0xFFE8E8F0, false);
                ty += 12;
            }
            ty += 8;
        }
        g.disableScissor();
        if (maxScroll() > 0) g.text(font, "스크롤 ↕", x + w - 52, y + 8, 0xFF9AA0B0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
