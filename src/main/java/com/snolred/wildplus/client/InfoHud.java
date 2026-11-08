package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 왼쪽 위 정보창: "📅 12일차 · 오후 3:20" / "X 120 Y 64 Z -40 · 북" */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class InfoHud {
    private InfoHud() {}

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, WildPlus.id("info_hud"), InfoHud::render);
    }

    private static int moneyTick = -1000;
    private static String moneyText = "0";

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !ClientConfig.SHOW_HUD.getAsBoolean()) {
            return;
        }
        long time = mc.level.getOverworldClockTime();
        long day = time / 24000L + 1;
        int tod = (int) (time % 24000L);
        int hour = (tod / 1000 + 6) % 24;
        int minute = (tod % 1000) * 60 / 1000;
        boolean night = tod >= 13000 && tod < 23000;
        String ampm = hour < 12 ? "오전" : "오후";
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        Component line1 = Component.translatable("hud.wildplus.time_wd", day, Component.translatable("hud.wildplus.wd." + Math.floorMod(day - 1, 7)),
                ampm, h12, String.format("%02d", minute));
        int x = 4, y = 4;
        int w = mc.font.width(line1);
        Component line2 = null;
        if (ClientConfig.HUD_COORDS.getAsBoolean()) {
            BlockPos p = player.blockPosition();
            line2 = Component.translatable("hud.wildplus.coords", p.getX(), p.getY(), p.getZ(),
                    Component.translatable("hud.wildplus.dir." + player.getDirection().getName()));
            w = Math.max(w, mc.font.width(line2));
        }
        // 소지금 (가방 동전 + 동전 주머니 + 통장)
        // 3.9.3: 가방을 매 화면마다 세지 않고 0.5초마다 한 번만 셈
        if (player.tickCount != moneyTick && (player.tickCount - moneyTick >= 10 || player.tickCount < moneyTick)) {
            moneyTick = player.tickCount;
            moneyText = String.format("%,d", com.snolred.wildplus.economy.Coins.totalValue(player) + com.snolred.wildplus.net.BankSync.clientBank);
        }
        Component line3 = Component.translatable("hud.wildplus.money", moneyText);
        w = Math.max(w, mc.font.width(line3));
        int lines = 2 + (line2 == null ? 0 : 1);
        int h = lines * 11 + 5;
        int tx = x + 12;
        // 반투명 카드 + 왼쪽 색 띠
        UiKit.round(g, x - 2, y - 2, w + 18, h, 3, 0x9A0E1218);
        UiKit.round(g, x - 2, y - 2, 2, h, 1, night ? 0xFF7A8CFF : 0xFFFFC34D);
        g.fill(x + 1, y - 2, x + w + 13, y - 1, 0x22FFFFFF);
        // 해 · 달
        if (night) {
            UiKit.circle(g, x + 2, y, 7, 0xFFDDE4FF);
            UiKit.circle(g, x + 4, y - 1, 6, 0xFF20263A);
        } else {
            UiKit.circle(g, x + 1, y - 1, 9, 0x55FFC34D);
            UiKit.circle(g, x + 2, y, 7, 0xFFFFC34D);
        }
        g.text(mc.font, line1, tx, y, night ? 0xFFB8C6FF : 0xFFFFE08A, true);
        int ly = y + 11;
        if (line2 != null) {
            // 나침반
            UiKit.circle(g, x + 2, ly, 7, 0xFF8A94A6);
            g.fill(x + 5, ly + 1, x + 6, ly + 4, 0xFFFF5050);
            g.fill(x + 5, ly + 4, x + 6, ly + 6, 0xFFFFFFFF);
            g.text(mc.font, line2, tx, ly, 0xFFE6EBF2, true);
            ly += 11;
        }
        // 동전
        UiKit.circle(g, x + 2, ly, 7, 0xFF8A6410);
        UiKit.circle(g, x + 3, ly + 1, 5, 0xFFF2CC5A);
        g.text(mc.font, line3, tx, ly, 0xFFFFD84A, true);
    }
}
