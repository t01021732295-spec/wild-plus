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
 * 즉석 복권 (30동). 우클릭으로 긁는다.
 * 꽝 50% · 5동 30% · 20동 12% · 100동 6% · 500동 1.8% · 5,000동 0.2% (평균 약 29동)
 */
public class LotteryTicketItem extends Item {
    private static final long[] PRIZES = {0, 5, 20, 100, 500, 5000};
    private static final int[] WEIGHTS = {5000, 3000, 1200, 600, 180, 20};

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
                Coins.refund(sp, prize);
                sp.sendOverlayMessage(Component.translatable("message.wildplus.lottery.win", prize).withStyle(ChatFormatting.GOLD));
                level.playSound(null, sp.blockPosition(), prize >= 500 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.8F, 1.2F);
                if (prize >= 500) {
                    sp.level().getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.wildplus.lottery.jackpot", sp.getName(), prize).withStyle(ChatFormatting.GOLD), false);
                }
            }
            sp.getCooldowns().addCooldown(stack, 10);
        }
        return InteractionResult.SUCCESS;
    }
}
