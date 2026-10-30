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
        Component line1 = Component.translatable("hud.wildplus.time", day, ampm, h12, String.format("%02d", minute));
        int x = 4, y = 4;
        int w = mc.font.width(line1);
        Component line2 = null;
        if (ClientConfig.HUD_COORDS.getAsBoolean()) {
            BlockPos p = player.blockPosition();
            line2 = Component.translatable("hud.wildplus.coords", p.getX(), p.getY(), p.getZ(),
                    Component.translatable("hud.wildplus.dir." + player.getDirection().getName()));
            w = Math.max(w, mc.font.width(line2));
        }
        // 소지금 (가방 동전 + 동전 주머니)
        Component line3 = Component.translatable("hud.wildplus.money", String.format("%,d", com.snolred.wildplus.economy.Coins.totalValue(player)));
        w = Math.max(w, mc.font.width(line3));
        int lines = 2 + (line2 == null ? 0 : 1);
        int h = lines * 10 + 2;
        g.fill(x - 2, y - 2, x + w + 2, y + h - 2, 0x66000000);
        g.text(mc.font, line1, x, y, night ? 0xFF9FB4FF : 0xFFFFE08A, true);
        int ly = y + 10;
        if (line2 != null) {
            g.text(mc.font, line2, x, ly, 0xFFE0E0E0, true);
            ly += 10;
        }
        g.text(mc.font, line3, x, ly, 0xFFFFD84A, true);
    }
}
