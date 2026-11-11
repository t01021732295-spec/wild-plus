package com.snolred.wildplus.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.0 계절: 게임 속 7일마다 봄 → 여름 → 가을 → 겨울. 계절 작물(block/SeasonCropBlock)은 제철에 빨리, 아니면 아주 느리게 자란다.
 * 양봉 상자는 겨울에 절반만 모은다.
 */
public final class Seasons {
    private Seasons() {}

    public static final int SPRING = 0, SUMMER = 1, AUTUMN = 2, WINTER = 3, DAYS = 7;
    public static final String[] NAME = {"봄", "여름", "가을", "겨울"};
    private static final String[] ICON = {"🌸", "☀", "🍂", "❄"};
    private static final String[] CROP = {"콩", "참외", "고구마", "무"};
    private static final ChatFormatting[] COLOR = {ChatFormatting.LIGHT_PURPLE, ChatFormatting.GREEN, ChatFormatting.GOLD, ChatFormatting.AQUA};
    private static int last = -1;

    public static int now() {
        return (int) ((ContentData.gameDay / DAYS) % 4);
    }

    /** 이 계절이 며칠 남았는지 (오늘 포함) */
    public static int daysLeft() {
        return DAYS - (int) (ContentData.gameDay % DAYS);
    }

    public static Component line() {
        int s = now();
        return Component.literal(String.format("%s 지금은 %s (%d일 남음) · 제철 작물: %s", ICON[s], NAME[s], daysLeft(), CROP[s])).withStyle(COLOR[s]);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 != 3) return;
        int s = now();
        if (last >= 0 && s != last) {
            event.getServer().getPlayerList().broadcastSystemMessage(Component.literal(String.format("%s 계절이 %s(으)로 바뀌었어요! 제철 작물: %s", ICON[s], NAME[s], CROP[s]))
                    .withStyle(COLOR[s], ChatFormatting.BOLD), false);
        }
        last = s;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) p.sendSystemMessage(line());
    }
}
