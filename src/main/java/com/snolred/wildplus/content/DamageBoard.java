package com.snolred.wildplus.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 피해량 순위표: 순위 · 이름 · 피해량 · 비율 막대 */
public final class DamageBoard {
    private DamageBoard() {}

    public static MutableComponent of(String title, Map<UUID, Float> damage, Map<UUID, String> names) {
        MutableComponent out = Component.literal("⚔ " + title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        List<Map.Entry<UUID, Float>> sorted = new ArrayList<>(damage.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        float total = 0;
        for (var e : sorted) total += e.getValue();
        if (sorted.isEmpty() || total <= 0) {
            return out.append(Component.literal("\n  (기록된 피해 없음)").withStyle(ChatFormatting.GRAY));
        }
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            float pct = e.getValue() / total;
            int bars = Math.round(pct * 10);
            String rank = (i == 0 ? "👑 " : "") + (i + 1) + "위";
            ChatFormatting c = i == 0 ? ChatFormatting.YELLOW : i == 1 ? ChatFormatting.WHITE : i == 2 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            out.append(Component.literal("\n  " + rank + " ").withStyle(c))
                    .append(Component.literal(names.getOrDefault(e.getKey(), "?")).withStyle(c, ChatFormatting.BOLD))
                    .append(Component.literal(String.format("  %,.0f", e.getValue())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format("  (%d%%) ", Math.round(pct * 100))).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("█".repeat(bars)).withStyle(ChatFormatting.RED))
                    .append(Component.literal("█".repeat(10 - bars)).withStyle(ChatFormatting.DARK_GRAY));
        }
        out.append(Component.literal(String.format("\n  합계 %,.0f", total)).withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }
}
