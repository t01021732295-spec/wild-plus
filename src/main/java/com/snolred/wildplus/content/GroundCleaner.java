package com.snolred.wildplus.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.0.8 떨어진 아이템 자동 청소 (렉 줄이기): 10분마다 땅에 떨어진 지 1분 넘은 아이템을 치운다. 30초 · 5초 전에 알림.
 * 지키는 것: 이름 붙인 것 · 인챈트된 것 · 희귀(파랑) 이상 · 떨어진 지 1분 안 된 것 (방금 죽어서 흩어진 물건 등).
 */
public final class GroundCleaner {
    private GroundCleaner() {}

    private static final int INTERVAL = 20 * 60 * 10, MIN_AGE = 20 * 60;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getPlayerList().getPlayers().isEmpty()) return;
        int t = s.getTickCount() % INTERVAL;
        if (t == INTERVAL - 600) say(s, "🧹 30초 뒤에 땅에 떨어진 아이템을 치워요 (이름 · 인챈트 · 희귀 아이템은 남겨요)", ChatFormatting.YELLOW);
        else if (t == INTERVAL - 100) say(s, "🧹 5초 뒤에 땅에 떨어진 아이템을 치워요!", ChatFormatting.GOLD);
        else if (t == 0 && s.getTickCount() > 0) {
            int n = 0;
            for (ServerLevel lv : s.getAllLevels()) {
                List<ItemEntity> gone = new ArrayList<>();
                for (Entity e : lv.getAllEntities()) {
                    if (e instanceof ItemEntity it && it.isAlive() && it.getAge() >= MIN_AGE && !keep(it.getItem())) gone.add(it);
                }
                for (ItemEntity it : gone) {
                    n += it.getItem().getCount();
                    it.discard();
                }
            }
            say(s, n > 0 ? String.format("🧹 떨어진 아이템 %,d개를 치웠어요", n) : "🧹 치울 아이템이 없었어요", ChatFormatting.GRAY);
        }
    }

    private static boolean keep(ItemStack st) {
        if (st.isEmpty()) return false;
        if (st.has(DataComponents.CUSTOM_NAME) || st.isEnchanted()) return true;
        Rarity r = st.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        return r == Rarity.RARE || r == Rarity.EPIC;
    }

    private static void say(MinecraftServer s, String msg, ChatFormatting c) {
        s.getPlayerList().broadcastSystemMessage(Component.literal(msg).withStyle(c), false);
    }
}
