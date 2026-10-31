package com.snolred.wildplus.economy;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** 돈으로 사는 서비스: 약국(효과 구매) · 기상 관측소(날씨·시간) */
public final class Services {
    private Services() {}

    private record Buff(String name, Holder<MobEffect> effect, int amp, int seconds, long price) {}

    private static List<Buff> buffs() {
        return List.of(
                new Buff("신속 II (10분)", MobEffects.SPEED, 1, 600, 60),
                new Buff("성급함 II (10분)", MobEffects.HASTE, 1, 600, 80),
                new Buff("야간 투시 (10분)", MobEffects.NIGHT_VISION, 0, 600, 30),
                new Buff("화염 저항 (10분)", MobEffects.FIRE_RESISTANCE, 0, 600, 60),
                new Buff("수중 호흡 (10분)", MobEffects.WATER_BREATHING, 0, 600, 40),
                new Buff("힘 I (10분)", MobEffects.STRENGTH, 0, 600, 120),
                new Buff("저항 I (10분)", MobEffects.RESISTANCE, 0, 600, 150),
                new Buff("행운 I (10분)", MobEffects.LUCK, 0, 600, 150),
                new Buff("재생 I (2분)", MobEffects.REGENERATION, 0, 120, 100),
                new Buff("광부의 집중 (10분)", com.snolred.wildplus.registry.ModEffects.MINERS_FOCUS, 0, 600, 100),
                new Buff("강철 피부 (10분)", com.snolred.wildplus.registry.ModEffects.IRONSKIN, 0, 600, 140),
                new Buff("탐욕 (10분)", com.snolred.wildplus.registry.ModEffects.GREED, 0, 600, 200),
                new Buff("광전사 (3분)", com.snolred.wildplus.registry.ModEffects.BERSERK, 0, 180, 120));
    }

    private static boolean near(Player p, BlockPos pos) {
        return p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }

    public static void openPharmacy(ServerPlayer player, BlockPos pos) {
        List<Buff> list = buffs();
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Buff b : list) entries.add(new PickMenu.Entry(b.name(), String.format("%,d동", b.price())));
        entries.add(new PickMenu.Entry("체력·배고픔 가득 채우기", "80동"));
        PickMenu.open(player, Component.translatable("container.wildplus.pharmacy"), "고르면 바로 결제돼요 (가방 동전·주머니에서)",
                "", entries, 0, i -> {
                    long price = i < list.size() ? list.get(i).price() : 80;
                    if (!Coins.pay(player, price)) {
                        player.sendOverlayMessage(Component.translatable("message.wildplus.service.no_money", price).withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (i < list.size()) {
                        Buff b = list.get(i);
                        player.addEffect(new MobEffectInstance(b.effect(), b.seconds() * 20, b.amp()));
                        player.sendOverlayMessage(Component.translatable("message.wildplus.service.bought", b.name(), price).withStyle(ChatFormatting.GREEN));
                    } else {
                        player.setHealth(player.getMaxHealth());
                        player.getFoodData().eat(20, 1.0F);
                        player.sendOverlayMessage(Component.translatable("message.wildplus.service.bought", "체력·배고픔 회복", price).withStyle(ChatFormatting.GREEN));
                    }
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6F, 1.2F);
                }, null, p -> near(p, pos));
    }

    private record Weather(String name, String command, long price) {}

    public static void openWeather(ServerPlayer player, BlockPos pos) {
        List<Weather> list = List.of(
                new Weather("맑게 하기", "weather clear", 200),
                new Weather("비 내리게 하기", "weather rain", 200),
                new Weather("뇌우 부르기", "weather thunder", 400),
                new Weather("아침으로 (오전 7시)", "time set day", 300),
                new Weather("밤으로", "time set night", 300));
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Weather w : list) entries.add(new PickMenu.Entry(w.name(), String.format("%,d동", w.price())));
        PickMenu.open(player, Component.translatable("container.wildplus.weather_station"), "날씨·시간은 모두에게 적용돼요",
                "", entries, 0, i -> {
                    Weather w = list.get(i);
                    if (!Coins.pay(player, w.price())) {
                        player.sendOverlayMessage(Component.translatable("message.wildplus.service.no_money", w.price()).withStyle(ChatFormatting.RED));
                        return;
                    }
                    MinecraftServer server = player.level().getServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), w.command());
                    server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.service.weather",
                            player.getName(), w.name(), w.price()).withStyle(ChatFormatting.AQUA), false);
                    player.closeContainer();
                }, null, p -> near(p, pos));
    }
}
