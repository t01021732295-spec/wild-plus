package com.snolred.wildplus.item;

import com.snolred.wildplus.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 즉석 복권 (3,000동). 우클릭으로 긁는다.
 * 꽝 55.6% · 1,000동 25% · 3,000동 12% · 1만동 6% · 5만동 1.2% · 50만동 0.2% (평균 약 2,810동). 당첨금은 세금 없음.
 */
public class LotteryTicketItem extends Item {
    private static final long[] PRIZES = {0, 1_000, 3_000, 10_000, 50_000, 500_000};
    private static final int[] WEIGHTS = {5560, 2500, 1200, 600, 120, 20};

    public LotteryTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ItemStack stack = sp.getItemInHand(hand);
            int roll = sp.getRandom().nextInt(10000);
            int i = 0;
            while (roll >= WEIGHTS[i]) {
                roll -= WEIGHTS[i];
                i++;
            }
            long prize = PRIZES[i];
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            if (prize == 0) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.lottery.lose").withStyle(ChatFormatting.GRAY));
                level.playSound(null, sp.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 1.0F);
            } else {
                // 복권 당첨금은 세금 없이, 동전으로 바로 가방에 (가방이 꽉 차면 발밑에)
                Coins.give(sp, prize);
                com.snolred.wildplus.content.Codex.add(sp, "lotto", 1);
                sp.inventoryMenu.broadcastChanges();
                sp.sendOverlayMessage(Component.translatable("message.wildplus.lottery.win", String.format("%,d", prize)).withStyle(ChatFormatting.GOLD));
                sp.sendSystemMessage(Component.translatable("message.wildplus.lottery.win", String.format("%,d", prize)).withStyle(ChatFormatting.GOLD));
                level.playSound(null, sp.blockPosition(), prize >= 50_000 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.8F, 1.2F);
                if (prize >= 50_000) {
                    sp.level().getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.wildplus.lottery.jackpot", sp.getName(), prize).withStyle(ChatFormatting.GOLD), false);
                }
            }
            sp.getCooldowns().addCooldown(stack, 10);
        }
        return InteractionResult.SUCCESS;
    }
}
