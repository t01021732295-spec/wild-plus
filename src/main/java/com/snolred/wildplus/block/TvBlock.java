package com.snolred.wildplus.block;

import com.snolred.wildplus.content.Contest;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.WeeklyGoals;
import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.economy.StockMarket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 3.4 TV: 켜면 채널 5개 (누를 때마다 다음 채널 · 웅크리고 누르면 끄기). 화면은 채널마다 움직이는 영상이고,
 * 채널을 틀면 그 채널의 진짜 서버 정보가 채팅에 나온다 (뉴스 · 주식 시세 · 주말 대회 중계).
 */
public class TvBlock extends FurnitureBlock {
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 4);
    public static final String[] NAMES = {"자연 다큐", "WP 뉴스", "주식 채널", "스포츠 중계", "만화 동산"};

    public TvBlock(Properties p) {
        super(p, Kind.TV_STAND);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHANNEL);
    }

    @Override
    public void open(Player player, BlockPos m) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return;
        BlockState st = level.getBlockState(m);
        if (!(st.getBlock() instanceof TvBlock)) return;
        if (sp.isShiftKeyDown() || !st.getValue(LIT)) {
            boolean on = !st.getValue(LIT);
            level.setBlock(m, st.setValue(LIT, on), 3);
            level.playSound(null, m, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.7F, on ? 1.2F : 0.8F);
            if (on) show(sp, level, st.getValue(CHANNEL));
            else sp.sendOverlayMessage(Component.literal("📺 TV를 껐어요").withStyle(ChatFormatting.GRAY));
            return;
        }
        int ch = (st.getValue(CHANNEL) + 1) % 5;
        level.setBlock(m, st.setValue(CHANNEL, ch), 3);
        level.playSound(null, m, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, 0.8F + ch * 0.15F);
        show(sp, level, ch);
    }

    private static void show(ServerPlayer sp, ServerLevel level, int ch) {
        sp.sendOverlayMessage(Component.literal("📺 " + ch + "번 · " + NAMES[ch] + "  (누르면 다음 채널 · 웅크리고 누르면 끄기)").withStyle(ChatFormatting.AQUA));
        for (Component c : lines(level, ch)) sp.sendSystemMessage(c);
    }

    public static List<Component> lines(ServerLevel level, int ch) {
        MinecraftServer server = level.getServer();
        List<Component> out = new ArrayList<>();
        long t = server.overworld().getOverworldClockTime();
        long day = t / 24000L + 1;
        String wd = Contest.weekdayName(t / 24000L);
        out.add(Component.literal("━━ 📺 " + NAMES[ch] + " ━━").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        switch (ch) {
            case 0 -> {
                String weather = level.isThundering() ? "⛈ 뇌우" : level.isRaining() ? "🌧 비" : "☀ 맑음";
                out.add(Component.literal(String.format("🌿 %d일차 %s요일 · 날씨 %s", day, wd, weather)).withStyle(ChatFormatting.GREEN));
                String[] facts = {"벌은 꽃가루를 옮기며 작물을 빨리 자라게 해요.", "여우는 입에 아이템을 물고 다녀요.", "아홀로틀은 물고기와 싸우는 걸 도와줘요.",
                        "문어(오징어)는 위험하면 먹물을 뿜어요.", "판다는 성격이 일곱 가지나 돼요.", "돌고래 옆에서 헤엄치면 빨라져요."};
                out.add(Component.literal("“" + facts[(int) Math.floorMod(day, facts.length)] + "”").withStyle(ChatFormatting.GRAY));
            }
            case 1 -> {
                out.add(Component.literal(String.format("📰 %d일차 %s요일 소식", day, wd)).withStyle(ChatFormatting.WHITE));
                out.add(Component.literal("· 블러드문: " + (com.snolred.wildplus.event.BloodMoon.isActive(level) ? "🔴 지금 진행 중! 조심하세요" : "오늘은 조용해요"))
                        .withStyle(ChatFormatting.RED));
                CompoundTag g = WeeklyGoals.goals(server);
                for (int i = 0; i < 4; i++) {
                    WeeklyGoals.Kind k = WeeklyGoals.kind(g, i);
                    int p = g.getIntOr("p" + i, 0);
                    out.add(Component.literal(String.format("· 주간 목표 %s %d/%d%s", k.label, Math.min(p, k.target), k.target, p >= k.target ? " ✔" : ""))
                            .withStyle(p >= k.target ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                }
                var rank = Honor.ranking(server, 1);
                if (!rank.isEmpty()) out.add(Component.literal(String.format("· 부자 1위: %s (%,d동)", rank.get(0).name(), rank.get(0).wealth()))
                        .withStyle(ChatFormatting.AQUA));
                out.add(Component.literal("· " + Contest.headline(server)).withStyle(ChatFormatting.LIGHT_PURPLE));
                out.add(Component.literal("· 다음 주 초기화까지 " + ContentData.daysToReset() + "일").withStyle(ChatFormatting.GRAY));
            }
            case 2 -> {
                out.add(Component.literal("📈 시세 (" + (StockMarket.isLive() ? "장 중" : "장 마감") + ")").withStyle(ChatFormatting.WHITE));
                for (int i = 0; i < StockMarket.STOCKS.size(); i++) {
                    long cur = StockMarket.price(i), prev = StockMarket.prev(i);
                    double pct = prev > 0 ? (cur - prev) * 100.0 / prev : 0;
                    out.add(Component.literal(String.format("%s %s  %s%.1f%%", StockMarket.STOCKS.get(i).name(), StockMarket.fmt(cur), pct >= 0 ? "▲" : "▼", Math.abs(pct)))
                            .withStyle(pct >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
            }
            case 3 -> out.addAll(Contest.tvLines(server));
            default -> {
                String[] toons = {"슬라임: “통! 통! 오늘도 점프 연습!”", "크리퍼: “쉬익… 친구 하고 싶었을 뿐인데…”", "주민: “흠! 흠? 흐음!”",
                        "닭: “알 낳았다고 자랑하러 왔어요!”", "좀비: “햇빛 너무 눈부셔… 모자 어디 갔지?”"};
                out.add(Component.literal("🎬 " + toons[level.getRandom().nextInt(toons.length)]).withStyle(ChatFormatting.YELLOW));
            }
        }
        return out;
    }
}
