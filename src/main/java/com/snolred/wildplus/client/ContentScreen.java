package com.snolred.wildplus.client;

import com.snolred.wildplus.content.WeeklyGoals;
import com.snolred.wildplus.menu.ContentMenu;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModItems7;
import com.snolred.wildplus.registry.ModItems8;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 주말 콘텐츠 화면 3종.
 *  - 레이드 제단: 흑요석 판 + 금테, 이번 주 보스 카드 · 페이즈 패턴 · 보상 · 기록
 *  - 하늘의 탑: 하늘색 판 + 구름, 탑 그림(최고 층까지 불 켜짐) · 기록 · 최근 도전
 *  - 목표 게시판: 코르크 판 + 메모지 4장, 아래 시즌 패스 10단계
 */
public class ContentScreen extends AbstractContainerScreen<ContentMenu> {
    private static final int W = 320, H = 226;
    private static final String[] BOSS_NAMES = {"대지의 거신", "망령 군주", "서리 여왕"};
    private static final String[][] BOSS_PHASES = {
            {"돌진 · 땅 내리치기", "+ 머리 위 낙석", "광폭화 · 약탈자 호위"},
            {"송곳니 · 벡스 소환 · 순간이동", "+ 번개 표식", "위더 스켈레톤 · 암흑"},
            {"얼음 화살 · 서리 폭발", "+ 발밑 얼음 가시", "눈보라 · 호위병"}};
    private static final int[] BOSS_COLOR = {0xFFD9A22E, 0xFF9B59D6, 0xFF5FC8F0};

    public ContentScreen(ContentMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    private CompoundTag info() {
        return menu.info;
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        CompoundTag t = info();
        switch (menu.mode) {
            case ContentMenu.RAID -> {
                boolean busy = !t.getStringOr("status", "").isEmpty();
                UiButton b = addRenderableWidget(UiButton.of(x + 172, y + H - 30, W - 182, 20,
                        busy ? "레이드 진행 중" : "⚔ 레이드 시작 (근처 " + t.getIntOr("party", 1) + "명)", UiButton.RED, () -> press(ContentMenu.BTN_START)));
                b.active = !busy;
            }
            case ContentMenu.TOWER -> {
                boolean busy = !t.getStringOr("status", "").isEmpty();
                UiButton b = addRenderableWidget(UiButton.of(x + 128, y + H - 30, W - 138, 20,
                        busy ? "누군가 오르는 중" : "🗼 탑 오르기 (근처 " + t.getIntOr("party", 1) + "명)", UiButton.BLUE, () -> press(ContentMenu.BTN_START)));
                b.active = !busy;
            }
            default -> {
                ListTag goals = t.getListOrEmpty("goals");
                for (int i = 0; i < Math.min(4, goals.size()); i++) {
                    CompoundTag e = goals.getCompoundOrEmpty(i);
                    boolean done = e.getIntOr("progress", 0) >= e.getIntOr("target", 1);
                    if (done && !e.getBooleanOr("claimed", false)) {
                        int gx = x + 10 + (i % 2) * 152, gy = y + 26 + (i / 2) * 46;
                        int id = ContentMenu.BTN_GOAL + i;
                        addRenderableWidget(UiButton.of(gx + 108, gy + 27, 36, 13, "받기", UiButton.GREEN, () -> press(id)));
                    }
                }
                int points = t.getIntOr("points", 0), claimed = t.getIntOr("tierClaimed", 0);
                for (int i = 0; i < WeeklyGoals.TIERS; i++) {
                    if (points >= (i + 1) * WeeklyGoals.TIER_POINTS && (claimed & (1 << i)) == 0) {
                        int id = ContentMenu.BTN_TIER + i;
                        addRenderableWidget(UiButton.of(x + 11 + i * 30, y + H - 20, 28, 12, "받기", UiButton.GOLD, () -> press(id)));
                    }
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // 글자는 모두 배경에서 그린다
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        switch (menu.mode) {
            case ContentMenu.RAID -> raid(g);
            case ContentMenu.TOWER -> tower(g);
            default -> goals(g);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    // ───────────────────── 공통 ─────────────────────
    private static void bigItem(GuiGraphicsExtractor g, ItemStack s, int x, int y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.item(s, 0, 0);
        g.pose().popMatrix();
    }

    private static void itemCount(GuiGraphicsExtractor g, ItemStack s, int x, int y, int textColor) {
        g.item(s, x, y);
        if (s.getCount() > 1) {
            String n = String.valueOf(s.getCount());
            g.text(UiKit.font(), Component.literal(n), x + 17 - UiKit.font().width(n), y + 9, 0xFFFFFFFF, true);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float ratio, int track, int fillTop, int fillBottom) {
        UiKit.round(g, x, y, w, h, 2, track);
        int fw = Math.round((w - 2) * Math.max(0F, Math.min(1F, ratio)));
        if (fw > 0) UiKit.roundGradient(g, x + 1, y + 1, fw, h - 2, 1, fillTop, fillBottom);
        g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x40FFFFFF);
    }

    private static String fmt(long n) {
        return String.format("%,d", n);
    }

    // ───────────────────── 레이드 제단 ─────────────────────
    private void raid(GuiGraphicsExtractor g) {
        CompoundTag t = info();
        int x = leftPos, y = topPos;
        int boss = Math.max(0, Math.min(2, t.getIntOr("boss", 0)));
        int bc = BOSS_COLOR[boss];
        int tier = t.getIntOr("tier", 1);
        // 금테 + 흑요석 판
        UiKit.device(g, x, y, W, H, 0xFF3A2A4A, 0xFF120C1A);
        UiKit.outline(g, x + 3, y + 3, W - 6, H - 6, 0xFFB8862A);
        g.fillGradient(x + 4, y + 4, x + W - 4, y + H - 4, 0xFF1D1428, 0xFF0B0710);
        UiKit.speckle(g, x + 4, y + 4, W - 8, H - 8, 77, 220, 0x18B48CFF, 0x10FFFFFF);
        // 머리 띠
        g.fillGradient(x + 4, y + 4, x + W - 4, y + 20, 0xFF5A1020, 0xFF2A0610);
        UiKit.hline(g, x + 4, y + 20, W - 8, 0xFFB8862A);
        UiKit.bold(g, "⚔ 주말 레이드", x + 10, y + 8, 0xFFFFD27A);
        UiKit.right(g, "새 보스까지 게임 속 " + t.getIntOr("reset", 7) + "일", x + W - 10, y + 8, 0xFFE8B8C0);

        // 보스 카드 (왼쪽)
        int cx = x + 10, cy = y + 26, cw = 154, ch = 120;
        UiKit.round(g, cx, cy, cw, ch, 3, UiKit.darker(bc, 0.75F));
        UiKit.roundGradient(g, cx + 1, cy + 1, cw - 2, ch - 2, 3, UiKit.darker(bc, 0.55F), 0xFF0E0A14);
        // 빛나는 원 + 보스 상징
        int ox = cx + cw / 2, oy = cy + 44;
        for (int r = 34; r > 0; r -= 4) UiKit.circle(g, ox - r, oy - r, r * 2, UiKit.alpha(bc, 18 + (34 - r) * 5));
        UiKit.circle(g, ox - 20, oy - 20, 40, UiKit.darker(bc, 0.6F));
        UiKit.circle(g, ox - 18, oy - 18, 36, 0xFF140E1C);
        ItemStack emblem = new ItemStack(boss == 0 ? ModItems8.TITAN_CORE.get() : boss == 1 ? ModItems8.LICH_ORB.get() : ModItems8.FROST_CROWN.get());
        bigItem(g, emblem, ox - 16, oy - 16, 2.0F);
        UiKit.center(g, "이번 주 보스", ox, cy + 6, UiKit.lighter(bc, 0.4F));
        int nw = UiKit.bigWidth(BOSS_NAMES[boss], 1);
        UiKit.bold(g, BOSS_NAMES[boss], ox - nw / 2, cy + 84, 0xFFFFFFFF);
        // 단계 별 10칸
        for (int i = 0; i < 10; i++) {
            int sx = cx + 12 + i * 13;
            UiKit.round(g, sx, cy + 99, 11, 5, 1, i < tier ? bc : 0xFF2E2638);
        }
        UiKit.center(g, tier + "단계 / 10", ox, cy + 107, 0xFFBFB2CF);

        // 정보 (오른쪽)
        int rx = x + 172, ry = y + 26;
        UiKit.round(g, rx, ry, W - 182, 120, 3, 0xFF2A2036);
        UiKit.roundGradient(g, rx + 1, ry + 1, W - 184, 118, 3, 0xFF221A2C, 0xFF16101E);
        UiKit.text(g, "체력 (" + t.getIntOr("party", 1) + "명 기준)", rx + 7, ry + 6, 0xFF9C8FB0);
        UiKit.bold(g, fmt(t.getLongOr("hp", 0)), rx + 7, ry + 17, 0xFFFF6B7A);
        UiKit.text(g, "공격력", rx + 80, ry + 6, 0xFF9C8FB0);
        UiKit.bold(g, String.valueOf(t.getIntOr("damage", 0)), rx + 80, ry + 17, 0xFFFFB020);
        UiKit.hline(g, rx + 6, ry + 29, W - 194, 0xFF3A2E48);
        int[] pc = {0xFF3FB27F, 0xFFE0A030, 0xFFE5484D};
        for (int i = 0; i < 3; i++) {
            int py = ry + 34 + i * 17;
            UiKit.pill(g, rx + 6, py, 28, 11, pc[i]);
            UiKit.center(g, (i + 1) + "P", rx + 20, py + 2, 0xFFFFFFFF);
            UiKit.text(g, BOSS_PHASES[boss][i], rx + 38, py + 2, 0xFFE6DEF0);
        }
        UiKit.text(g, "바닥에 원이 보이면 피하세요!", rx + 7, ry + 88, 0xFFB8A8C8);
        UiKit.text(g, "제한 시간 15분 · 깰 때마다 단계 +1", rx + 7, ry + 101, 0xFF7E7290);

        // 보상 (아래 왼쪽)
        int by = y + 152;
        UiKit.text(g, "이번 주 첫 클리어 보상 (각자)", x + 10, by, 0xFFFFD27A);
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(ModItems3.GOLD_COIN.get(), 8 + 2 * tier));
        loot.add(new ItemStack(ModItems7.RUBY.get(), 2 + tier / 3));
        loot.add(new ItemStack(Items.DIAMOND, 2 + tier / 4));
        loot.add(emblem.copy());
        if (tier >= 5) loot.add(new ItemStack(Items.NETHERITE_SCRAP, 1 + tier / 5));
        for (int i = 0; i < loot.size(); i++) {
            int ix = x + 10 + i * 24;
            UiKit.round(g, ix, by + 11, 20, 20, 2, 0xFF2A2036);
            itemCount(g, loot.get(i), ix + 2, by + 13, 0xFFFFFFFF);
        }
        boolean cleared = t.getBooleanOr("cleared", false);
        UiKit.pill(g, x + 10, by + 36, 150, 12, cleared ? 0xFF1E7A4C : 0xFF4A2A36);
        UiKit.center(g, cleared ? "✔ 이번 주 보상 받음 (반복은 일부만)" : "아직 이번 주에 안 깼어요", x + 85, by + 38, 0xFFFFFFFF);
        UiKit.text(g, "MVP(피해 1등) +500동 · 루비", x + 10, by + 54, 0xFFB8A8C8);

        // 기록 (아래 오른쪽)
        long best = t.getLongOr("best", -1);
        UiKit.text(g, "서버 클리어 " + t.getIntOr("clears", 0) + "회 · 나 " + t.getIntOr("myClears", 0) + "회", rx, by, 0xFFBFB2CF);
        UiKit.text(g, "최근: " + t.getStringOr("last", "-"), rx, by + 11, 0xFF9C8FB0);
        UiKit.text(g, "MVP: " + t.getStringOr("mvp", "-") + (best > 0 ? String.format(" · 최고 %d:%02d", best / 1200, best / 20 % 60) : ""), rx, by + 22, 0xFF9C8FB0);
        String status = t.getStringOr("status", "");
        if (!status.isEmpty()) UiKit.text(g, status, rx, by + 33, 0xFFFF8A8A);
    }

    // ───────────────────── 하늘의 탑 ─────────────────────
    private void tower(GuiGraphicsExtractor g) {
        CompoundTag t = info();
        int x = leftPos, y = topPos;
        int best = t.getIntOr("best", 0), mine = t.getIntOr("myBest", 0);
        UiKit.device(g, x, y, W, H, 0xFFE9F4FF, 0xFFB8D4EE);
        g.fillGradient(x + 4, y + 4, x + W - 4, y + H - 4, 0xFF7FB8F0, 0xFFE8F3FF);
        // 구름
        int[][] clouds = {{30, 40, 46}, {200, 30, 60}, {260, 120, 40}, {90, 170, 54}, {150, 90, 36}};
        for (int[] c : clouds) {
            UiKit.round(g, x + c[0], y + c[1], c[2], 10, 5, 0x60FFFFFF);
            UiKit.round(g, x + c[0] + c[2] / 4, y + c[1] - 5, c[2] / 2, 10, 5, 0x60FFFFFF);
        }
        g.fillGradient(x + 4, y + 4, x + W - 4, y + 20, 0xFF1E5AA8, 0xFF16437E);
        UiKit.hline(g, x + 4, y + 20, W - 8, 0xFFCFE6FF);
        UiKit.bold(g, "🗼 하늘의 탑", x + 10, y + 8, 0xFFFFFFFF);
        UiKit.right(g, "친구와 함께 · 쓰러져도 아이템 유지", x + W - 10, y + 8, 0xFFBFDCFF);

        // 탑 그림 (최고 층 비율만큼 창문에 불)
        int tx = x + 22, tw = 84, base = y + H - 12, floors = 14;
        int lit = best <= 0 ? 0 : Math.max(1, Math.min(floors, (int) Math.ceil(best / 5.0)));
        UiKit.round(g, tx - 12, base - 4, tw + 24, 8, 3, 0xFF6E7F92);
        for (int i = 0; i < floors; i++) {
            int fy = base - 4 - (i + 1) * 12;
            int inset = i / 3;
            int fx = tx + inset, fw = tw - inset * 2;
            int stone = (i % 2 == 0) ? 0xFFD8DEE6 : 0xFFC9D1DB;
            g.fill(fx, fy, fx + fw, fy + 12, stone);
            g.fill(fx, fy, fx + fw, fy + 1, 0xFFF4F7FA);
            g.fill(fx, fy + 11, fx + fw, fy + 12, 0xFF9AA6B4);
            for (int wdw = 0; wdw < 3; wdw++) {
                int wx = fx + fw / 2 - 16 + wdw * 13;
                boolean on = i < lit;
                g.fill(wx, fy + 3, wx + 6, fy + 9, on ? 0xFFFFD45A : 0xFF4A5A70);
                if (on) g.fill(wx, fy + 3, wx + 6, fy + 4, 0xFFFFF2B0);
            }
        }
        int topY = base - 4 - floors * 12;
        // 지붕 (삼각)
        for (int i = 0; i < 16; i++) g.fill(tx + 4 + i * 2, topY - i, tx + tw - 4 - i * 2, topY - i + 1, 0xFF2B5FA8);
        g.fill(tx + tw / 2 - 1, topY - 26, tx + tw / 2 + 1, topY - 15, 0xFF8A6A3A);
        g.fill(tx + tw / 2 + 1, topY - 26, tx + tw / 2 + 9, topY - 21, 0xFFE5484D);

        // 오른쪽 정보
        int rx = x + 128, rw = W - 138;
        UiKit.card(g, rx, y + 26, rw, 46, 0xF2FFFFFF, 0xFF9CC4EA);
        UiKit.text(g, "서버 최고 기록", rx + 7, y + 31, 0xFF5B7590);
        UiKit.big(g, best + "층", rx + 7, y + 42, 2, 0xFF1E5AA8);
        String names = t.getStringOr("bestNames", "-");
        while (UiKit.font().width(names) > rw - 80 && names.length() > 3) names = names.substring(0, names.length() - 2) + "…";
        UiKit.right(g, names, rx + rw - 7, y + 46, 0xFF3A4A5E);
        UiKit.right(g, "내 최고 " + mine + "층", rx + rw - 7, y + 58, 0xFF22A35A);

        UiKit.card(g, rx, y + 76, rw, 64, 0xEBFFFFFF, 0xFFB8D4EE);
        UiKit.text(g, "최근 도전", rx + 7, y + 81, 0xFF5B7590);
        ListTag runs = t.getListOrEmpty("runs");
        if (runs.isEmpty()) UiKit.text(g, "아직 아무도 오르지 않았어요", rx + 7, y + 94, 0xFF8A9AB0);
        for (int i = 0; i < Math.min(4, runs.size()); i++) {
            Tag tag = runs.get(i);
            if (!(tag instanceof CompoundTag r)) continue;
            int ly = y + 93 + i * 11;
            String nm = r.getStringOr("names", "?");
            while (UiKit.font().width(nm) > rw - 60 && nm.length() > 3) nm = nm.substring(0, nm.length() - 2) + "…";
            UiKit.pill(g, rx + 6, ly - 1, 30, 10, 0xFF3B82F6);
            UiKit.center(g, r.getIntOr("floor", 0) + "층", rx + 21, ly, 0xFFFFFFFF);
            UiKit.text(g, nm, rx + 40, ly, 0xFF2A3A4E);
        }

        UiKit.text(g, "층마다 각자 층×10동 · 5층마다 보너스 상자", rx, y + 146, 0xFF1E3A5F);
        UiKit.text(g, "5층 정예 · 10층 수문장 · 10층마다 경치 변화", rx, y + 157, 0xFF1E3A5F);
        UiKit.text(g, "쉬는 시간에 금빛 발판 = 스스로 내려가기", rx, y + 168, 0xFF1E3A5F);
        UiKit.text(g, "쓰러지면 탈락 (아이템은 그대로)", rx, y + 179, 0xFF1E3A5F);
        String status = t.getStringOr("status", "");
        if (!status.isEmpty()) UiKit.text(g, status, rx, y + H - 42, 0xFFC92A31);
    }

    // ───────────────────── 목표 게시판 + 시즌 패스 ─────────────────────
    private void goals(GuiGraphicsExtractor g) {
        CompoundTag t = info();
        int x = leftPos, y = topPos;
        // 나무 테 + 코르크
        UiKit.device(g, x, y, W, H, 0xFF9A6A3A, 0xFF5E3A1A);
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 116, 0xFFC99A64, 0xFFB98A54);
        UiKit.speckle(g, x + 5, y + 5, W - 10, 111, 13, 420, 0x30704020, 0x22FFF0D0);
        UiKit.bold(g, "📋 이번 주 공동 목표", x + 10, y + 9, 0xFF3A2410);
        UiKit.right(g, "새 목표까지 게임 속 " + t.getIntOr("reset", 7) + "일", x + W - 10, y + 9, 0xFF5A3A1A);

        ListTag goals = t.getListOrEmpty("goals");
        int[] tints = {0xFFFFF6C8, 0xFFE4F4FF, 0xFFFFE4EC, 0xFFE6FFE6};
        int[] pins = {0xFFE5484D, 0xFF3B82F6, 0xFF22A35A, 0xFFF2C14E};
        for (int i = 0; i < Math.min(4, goals.size()); i++) {
            CompoundTag e = goals.getCompoundOrEmpty(i);
            int gx = x + 10 + (i % 2) * 152, gy = y + 26 + (i / 2) * 46;
            int prog = e.getIntOr("progress", 0), target = Math.max(1, e.getIntOr("target", 1));
            boolean done = prog >= target, claimed = e.getBooleanOr("claimed", false);
            g.fill(gx + 2, gy + 2, gx + 148, gy + 44, 0x40000000);
            g.fill(gx, gy, gx + 146, gy + 42, tints[i]);
            g.fill(gx, gy, gx + 146, gy + 1, 0x60FFFFFF);
            UiKit.circle(g, gx + 69, gy - 3, 7, pins[i]);
            UiKit.bold(g, e.getStringOr("label", "?"), gx + 6, gy + 6, 0xFF2A2018);
            bar(g, gx + 6, gy + 18, 134, 7, prog / (float) target, 0xFFD8CCB4, done ? 0xFF4BD58A : 0xFF6BA8FF, done ? 0xFF1E8A4C : 0xFF2563D9);
            UiKit.text(g, fmt(prog) + " / " + fmt(target), gx + 6, gy + 29, 0xFF5A4A38);
            if (claimed) {
                UiKit.right(g, "받음 ✔", gx + 140, gy + 29, 0xFF1E8A4C);
            } else if (!done) {
                UiKit.right(g, fmt(e.getIntOr("reward", 0)) + "동 · " + WeeklyGoals.GOAL_POINTS + "점", gx + 140, gy + 29, 0xFF8A6A3A);
            }
        }

        // 시즌 패스
        int sy = y + 120;
        g.fillGradient(x + 5, sy, x + W - 5, y + H - 5, 0xFF2A1F3D, 0xFF161026);
        UiKit.hline(g, x + 5, sy, W - 10, 0xFFE0B04A);
        int points = t.getIntOr("points", 0), claimed = t.getIntOr("tierClaimed", 0);
        int tier = Math.min(WeeklyGoals.TIERS, points / WeeklyGoals.TIER_POINTS);
        UiKit.bold(g, "✦ 시즌 " + t.getLongOr("season", 1) + " 패스", x + 10, sy + 5, 0xFFFFD27A);
        UiKit.right(g, "점수 " + fmt(points) + " · " + tier + "단계 · " + t.getIntOr("weeksLeft", 8) + "주 남음", x + W - 10, sy + 5, 0xFFCFC2E8);
        float within = tier >= WeeklyGoals.TIERS ? 1F : (points % WeeklyGoals.TIER_POINTS) / (float) WeeklyGoals.TIER_POINTS;
        bar(g, x + 10, sy + 17, W - 20, 6, (tier + within) / WeeklyGoals.TIERS, 0xFF3A2F50, 0xFFFFE08A, 0xFFE0A030);
        for (int i = 0; i < WeeklyGoals.TIERS; i++) {
            int cx = x + 11 + i * 30, cy = sy + 28;
            boolean reached = points >= (i + 1) * WeeklyGoals.TIER_POINTS, got = (claimed & (1 << i)) != 0;
            int border = got ? 0xFF3FB27F : reached ? 0xFFFFD27A : 0xFF4A3F60;
            UiKit.round(g, cx, cy, 28, 38, 3, border);
            UiKit.roundGradient(g, cx + 1, cy + 1, 26, 36, 3, reached ? 0xFF4A3A20 : 0xFF241C34, reached ? 0xFF2A2010 : 0xFF181226);
            UiKit.center(g, String.valueOf(i + 1), cx + 14, cy + 3, reached ? 0xFFFFE08A : 0xFF7E7290);
            List<ItemStack> r = WeeklyGoals.tierReward(i);
            itemCount(g, r.get(0), cx + 6, cy + 14, 0xFFFFFFFF);
            if (!reached) g.fill(cx + 1, cy + 1, cx + 27, cy + 37, 0x70000000);
            if (got) UiKit.center(g, "✔", cx + 14, cy + 29, 0xFF4BD58A);
        }
        UiKit.center(g, "점수: 목표 40 · 레이드 60 · 탑 1층 4 · 100점 = 1단계", x + W / 2, y + H - 33, 0xFF9C8FB0);
    }
}
