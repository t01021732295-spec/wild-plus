package com.snolred.wildplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** 3.8 금지된 물약 (밀수꾼): 1분 동안 힘 III · 신속 II · 저항 I, 끝나면 1분 나약함 II · 구속 (omen.OmenEvents) */
public class ForbiddenPotionItem extends Item {
    public static final String UNTIL = "wildplus_forbidden_until";

    public ForbiddenPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1200, 2));
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0));
            p.getPersistentData().putLong(UNTIL, level.getGameTime() + 1200);
            level.playSound(null, p.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0F, 0.6F);
            p.sendOverlayMessage(Component.literal("🧪 광기! 1분 뒤에는 대가를 치러요").withStyle(ChatFormatting.DARK_RED));
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }
}
