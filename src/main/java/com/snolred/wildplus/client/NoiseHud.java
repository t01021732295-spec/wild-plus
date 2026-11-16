package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.event.Noise;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 5.3 소음 게이지 HUD: 딥다크 안에서만 핫바 위 가운데. 4칸 (고요 · 꿈틀 · 추적자 · 워든) */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class NoiseHud {
    private NoiseHud() {}

    private static float shown;

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, WildPlus.id("noise_hud"), NoiseHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        int v = Noise.client;
        if (mc.player == null || v < 0 || mc.options.hideGui) {
            shown = 0;
            return;
        }
        shown += (v - shown) * 0.2F;
        int w = 122, h = 7;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() - 62;
        int col = shown >= 80 ? 0xFFE84848 : shown >= 55 ? 0xFFE8B440 : shown >= 25 ? 0xFF5ADCB4 : 0xFF3CC8D2;
        UiKit.round(g, x - 2, y - 11, w + 4, h + 14, 3, 0x9A0A1216);
        String label = shown >= 80 ? "♪ 소음 · 위험!" : shown >= 55 ? "♪ 소음 · 무언가 다가옴" : shown >= 25 ? "♪ 소음 · 촉수가 꿈틀" : "♪ 소음 · 고요함";
        g.text(mc.font, label, x, y - 10, col, true);
        g.fill(x, y, x + w, y + h, 0xFF1E262C);
        int fill = Mth.clamp(Math.round(w * shown / 100F), 0, w);
        g.fill(x, y, x + fill, y + h, col);
        if (shown >= 80 && (mc.player.tickCount / 4) % 2 == 0) g.fill(x, y, x + fill, y + 1, 0xFFFFFFFF);
        for (int i = 1; i < 4; i++) {
            int lx = x + (i == 1 ? w * 25 / 100 : i == 2 ? w * 55 / 100 : w * 80 / 100);
            g.fill(lx, y, lx + 1, y + h, 0xFF55656E);
        }
    }
}
