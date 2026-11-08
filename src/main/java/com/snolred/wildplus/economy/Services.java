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

    private record Buff(String name, Holder<MobEffect> effect, int amp, int seconds, long price) {
        Buff {
            price = com.snolred.wildplus.economy.Prices.x(price);
        }
    }

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

    private static final net.minecraft.world.item.Item[] ICON = {
            net.minecraft.world.item.Items.SUGAR, net.minecraft.world.item.Items.GOLDEN_PICKAXE, net.minecraft.world.item.Items.GOLDEN_CARROT,
            net.minecraft.world.item.Items.MAGMA_CREAM, net.minecraft.world.item.Items.PUFFERFISH, net.minecraft.world.item.Items.BLAZE_POWDER,
            net.minecraft.world.item.Items.IRON_CHESTPLATE, net.minecraft.world.item.Items.RABBIT_FOOT, net.minecraft.world.item.Items.GHAST_TEAR,
            net.minecraft.world.item.Items.DIAMOND_PICKAXE, net.minecraft.world.item.Items.IRON_BLOCK, net.minecraft.world.item.Items.GOLD_INGOT,
            net.minecraft.world.item.Items.NETHERITE_AXE};
    private static final String[] DESC = {
            "걸음이 빨라져요. 먼 길 갈 때 좋아요.", "캐는 속도가 빨라져요. 광산 갈 때 필수!", "어두운 곳도 환하게 보여요.",
            "불과 용암 피해를 막아요. 네더 탐험용.", "물속에서 숨을 오래 참아요.", "근접 공격 피해가 세져요.",
            "받는 피해가 줄어요.", "낚시 · 보물 운이 좋아져요.", "체력이 조금씩 차요.",
            "캐는 속도가 40% 빨라져요.", "방어력 +4 · 방어 강도 +2.", "몬스터를 잡으면 은화 1~3개가 나와요.",
            "더 세게 싸우지만 받는 피해도 25% 늘어요."};

    private static boolean near(Player p, BlockPos pos) {
        return p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }

    public static void openPharmacy(ServerPlayer player, BlockPos pos) {
        List<Buff> list = buffs();
        List<PickMenu.Entry> entries = new ArrayList<>();
        int off = com.snolred.wildplus.omen.Discounts.pct(player, pos);
        for (int k = 0; k < list.size(); k++) {
            Buff b = list.get(k);
            entries.add(new PickMenu.Entry(b.name(), String.format("%,d동", com.snolred.wildplus.omen.Discounts.apply(b.price(), off)) + "\n" + (k < DESC.length ? DESC[k] : ""),
                    new net.minecraft.world.item.ItemStack(k < ICON.length ? ICON[k] : net.minecraft.world.item.Items.GLASS_BOTTLE), PickMenu.K_ROW));
        }
        entries.add(new PickMenu.Entry("체력·배고픔 가득 채우기", String.format("%,d동", com.snolred.wildplus.omen.Discounts.apply(com.snolred.wildplus.economy.Prices.x(80L), off))
                + "\n체력과 배고픔을 한 번에 가득 채워요. 긴 탐험 뒤에 딱!", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE), PickMenu.K_ROW));
        PickMenu.open(player, Component.translatable("container.wildplus.pharmacy"), "가방 동전·주머니에서 결제"
                        + com.snolred.wildplus.omen.Discounts.tag(off),
                "", entries, PickMenu.themed(PickMenu.T_PHARMACY), i -> {
                    long price = com.snolred.wildplus.omen.Discounts.apply(i < list.size() ? list.get(i).price() : com.snolred.wildplus.economy.Prices.x(80L), off);
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

    private record Weather(String name, String command, long price) {
        Weather {
            price = com.snolred.wildplus.economy.Prices.x(price);
        }
    }

    public static void openWeather(ServerPlayer player, BlockPos pos) {
        List<Weather> list = List.of(
                new Weather("맑게 하기", "weather clear", 200),
                new Weather("비 내리게 하기", "weather rain", 200),
                new Weather("뇌우 부르기", "weather thunder", 400),
                new Weather("아침으로 (오전 7시)", "time set day", 300),
                new Weather("밤으로", "time set night", 300));
        List<PickMenu.Entry> entries = new ArrayList<>();
        int off = com.snolred.wildplus.omen.Discounts.pct(player, pos);
        for (Weather w : list) entries.add(new PickMenu.Entry(w.name(), String.format("%,d동", com.snolred.wildplus.omen.Discounts.apply(w.price(), off))));
        PickMenu.open(player, Component.translatable("container.wildplus.weather_station"), "고르면 바로 바뀌어요" + com.snolred.wildplus.omen.Discounts.tag(off),
                "", entries, PickMenu.themed(PickMenu.T_WEATHER), i -> {
                    Weather w = list.get(i);
                    long wp = com.snolred.wildplus.omen.Discounts.apply(w.price(), off);
                    if (!Coins.pay(player, wp)) {
                        player.sendOverlayMessage(Component.translatable("message.wildplus.service.no_money", wp).withStyle(ChatFormatting.RED));
                        return;
                    }
                    MinecraftServer server = player.level().getServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), w.command());
                    server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.service.weather",
                            player.getName(), w.name(), wp).withStyle(ChatFormatting.AQUA), false);
                    player.closeContainer();
                }, null, p -> near(p, pos));
    }
}
