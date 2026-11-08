package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** 4.0.2 양조대 옆 제조법 패널: [제조법] 버튼으로 열고, 휠로 넘긴다. 레시피는 data/wildplus/recipe/brewing 과 맞출 것. */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class BrewGuide {
    private BrewGuide() {}

    private record Row(ItemStack icon, Component label, boolean header) {}

    private static final int PW = 150, PH = 190, ROW = 16, HEAD = 18;
    private static final List<Row> ROWS = new ArrayList<>();
    private static boolean open;
    private static int scroll;

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    private static void head(String s) {
        ROWS.add(new Row(ItemStack.EMPTY, Component.literal(s), true));
    }

    private static void row(String id, String text) {
        ROWS.add(new Row(item(id), Component.literal(text), false));
    }

    private static void potion(String id, String effect) {
        ROWS.add(new Row(item(id), Component.translatable("item.minecraft.potion.effect." + effect), false));
    }

    private static void build() {
        head("기본");
        row("minecraft:nether_wart", "물병 → 어색한 물약");
        row("minecraft:redstone", "지속 시간 늘리기");
        row("minecraft:glowstone_dust", "효과 강화");
        row("minecraft:fermented_spider_eye", "효과 뒤집기");
        row("minecraft:gunpowder", "투척형으로");
        row("minecraft:dragon_breath", "잔류형으로");
        head("어색한 물약 +");
        potion("minecraft:sugar", "swiftness");
        potion("minecraft:rabbit_foot", "leaping");
        potion("minecraft:blaze_powder", "strength");
        potion("minecraft:glistering_melon_slice", "healing");
        potion("minecraft:spider_eye", "poison");
        potion("minecraft:ghast_tear", "regeneration");
        potion("minecraft:magma_cream", "fire_resistance");
        potion("minecraft:pufferfish", "water_breathing");
        potion("minecraft:golden_carrot", "night_vision");
        potion("minecraft:phantom_membrane", "slow_falling");
        potion("minecraft:turtle_helmet", "turtle_master");
        potion("minecraft:breeze_rod", "wind_charged");
        potion("minecraft:cobweb", "weaving");
        potion("minecraft:slime_block", "oozing");
        potion("minecraft:stone", "infested");
        head("어색한 물약 + (모드)");
        potion("wildplus:blood_shard", "wildplus_lifesteal");
        potion("wildplus:chili", "wildplus_berserk");
        potion("wildplus:gold_coin", "wildplus_greed");
        potion("minecraft:iron_ingot", "wildplus_ironskin");
        potion("wildplus:raw_mythril", "wildplus_miners_focus");
        head("물약 변환 (모드)");
        row("wildplus:jade", "광전사 → 유리 대포");
        row("minecraft:fermented_spider_eye", "철피부 → 취약");
        row("minecraft:fermented_spider_eye", "흡혈 → 출혈");
        row("minecraft:glowstone_dust", "광부의 집중 강화");
        head("불길한 병 +");
        row("minecraft:blaze_powder", "불타는 흉조");
        row("minecraft:prismarine_crystals", "심해의 흉조");
        row("minecraft:blue_ice", "얼어붙은 흉조");
        row("minecraft:gold_block", "황금 흉조");
        row("minecraft:phantom_membrane", "망령의 흉조");
    }

    private static int visible() {
        return (PH - 28) / ROW;
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof BrewingStandScreen s)) return;
        if (ROWS.isEmpty()) build();
        if (System.getenv("WILDPLUS_UITEST") != null) open = true;
        int left = (s.width - 176) / 2, top = (s.height - 166) / 2;
        event.addListener(UiButton.of(left + 126, top - 15, 50, 14, "제조법", UiButton.STEEL, () -> open = !open));
    }

    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        if (!open || !(event.getScreen() instanceof BrewingStandScreen s)) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int x = Math.max(2, (s.width - 176) / 2 - PW - 4), y = (s.height - PH) / 2;
        UiKit.card(g, x, y, PW, PH, UiKit.PAPER, UiKit.LINE);
        UiKit.bold(g, "포션 제조법", x + 8, y + 7, UiKit.INK);
        int max = Math.max(0, ROWS.size() - visible());
        scroll = Math.min(scroll, max);
        int ry = y + 22;
        for (int i = scroll; i < Math.min(ROWS.size(), scroll + visible()); i++, ry += ROW) {
            Row r = ROWS.get(i);
            if (r.header) {
                UiKit.bold(g, r.label.getString(), x + 8, ry + 4, UiKit.BLUE);
            } else {
                g.item(r.icon, x + 6, ry);
                UiKit.text(g, UiKit.font().plainSubstrByWidth(r.label.getString(), PW - 32), x + 26, ry + 5, UiKit.INK);
            }
        }
        if (max > 0) {
            int track = PH - 28, bar = Math.max(10, track * visible() / ROWS.size());
            g.fill(x + PW - 5, y + 22, x + PW - 3, y + 22 + track, UiKit.LINE);
            int by = y + 22 + (track - bar) * scroll / max;
            g.fill(x + PW - 5, by, x + PW - 3, by + bar, UiKit.INK_SOFT);
        }
    }

    @SubscribeEvent
    static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!open || !(event.getScreen() instanceof BrewingStandScreen s)) return;
        int x = Math.max(2, (s.width - 176) / 2 - PW - 4), y = (s.height - PH) / 2;
        if (event.getMouseX() < x || event.getMouseX() > x + PW || event.getMouseY() < y || event.getMouseY() > y + PH) return;
        scroll = Math.max(0, Math.min(Math.max(0, ROWS.size() - visible()), scroll - (int) Math.signum(event.getScrollDeltaY())));
        event.setCanceled(true);
    }
}
