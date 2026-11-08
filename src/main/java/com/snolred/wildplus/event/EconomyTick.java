package com.snolred.wildplus.event;

import com.snolred.wildplus.economy.Cookbook;
import com.snolred.wildplus.economy.StockMarket;
import com.snolred.wildplus.economy.WildData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 경제 시스템의 시간 처리: 매일 아침 은행 이자, 주식 시세 갱신, 첫 접속 요리책 · 레시피 해금 */
public final class EconomyTick {
    public static final long INTEREST_CAP = 500;

    private EconomyTick() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 3) return;
        StockMarket.tick(server);
        long day = server.overworld().getOverworldClockTime() / 24000L;
        WildData data = WildData.get(server);
        if (data.lastInterestDay < 0) {
            data.lastInterestDay = day;
            data.setDirty();
        } else if (day > data.lastInterestDay) {
            data.lastInterestDay = day;
            payInterest(server, data);
            com.snolred.wildplus.economy.Loans.dailyTick(server);
        }
    }

    public static void payInterest(MinecraftServer server, WildData data) {
        for (Map.Entry<UUID, Long> e : new ArrayList<>(data.bank.entrySet())) {
            int rank = data.honor(e.getKey());
            long gross = Math.min(INTEREST_CAP * (1 + rank), e.getValue() * (100 + 20L * rank) / 10000);
            if (gross <= 0) continue;
            long interest = com.snolred.wildplus.economy.Tax.collect(server, e.getKey(), gross);
            data.reason = "예금 이자";
            data.addBalance(e.getKey(), interest);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                p.sendSystemMessage(Component.translatable("message.wildplus.bank.interest", interest, data.balance(e.getKey()))
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        data.setDirty();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 모드 레시피를 레시피 책에 모두 등록
        List<RecipeHolder<?>> ours = new ArrayList<>();
        for (RecipeHolder<?> h : player.level().getServer().getRecipeManager().getRecipes()) {
            if ("wildplus".equals(h.id().identifier().getNamespace())) ours.add(h);
        }
        player.awardRecipes(ours);
        // 첫 접속: 요리책 한 권
        CompoundTag tag = player.getPersistentData();
        if (!tag.getBooleanOr("wildplus_cookbook", false)) {
            tag.putBoolean("wildplus_cookbook", true);
            if (!player.getInventory().add(Cookbook.create())) {
                net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), Cookbook.create());
            }
        }
        // 첫 접속(또는 1.4.0 이후 처음): 제작 안내서 한 권
        if (!tag.getBooleanOr("wildplus_guide", false)) {
            tag.putBoolean("wildplus_guide", true);
            ItemStack guide = com.snolred.wildplus.economy.Guidebook.create();
            if (!player.getInventory().add(guide)) {
                net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), guide);
            }
        }
    }
}
