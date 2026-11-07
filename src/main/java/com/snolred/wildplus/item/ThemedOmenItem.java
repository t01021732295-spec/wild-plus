package com.snolred.wildplus.item;

import com.snolred.wildplus.omen.Omen;
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

/**
 * 3.8 테마 흉조 (양조대에서 흉조 병 + 재료): 마시면 나쁜 징조 + 다음 습격 테마.
 * 세기(I~V)는 원래 흉조 병의 세기를 그대로 이어받는다 (흉조 병 증폭 성분).
 */
public class ThemedOmenItem extends Item {
    public final Omen.Theme theme;

    public ThemedOmenItem(Properties properties, Omen.Theme theme) {
        super(properties);
        this.theme = theme;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            int amp = Omen.amp(stack);
            p.removeEffect(MobEffects.BAD_OMEN);
            p.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 120000, amp, false, false, true));
            p.getPersistentData().putString(Omen.THEME_KEY, theme.name());
            level.playSound(null, p.blockPosition(), SoundEvents.OMINOUS_BOTTLE_DISPOSE, SoundSource.PLAYERS, 1.0F, 0.8F);
            p.sendSystemMessage(Component.literal("☠ " + theme.title + " " + (amp + 1) + "단계가 걸렸어요 — " + theme.desc()).withStyle(ChatFormatting.DARK_PURPLE));
            p.sendSystemMessage(Component.literal("  마을에 들어가거나 마을 종을 누르면(흉조 계약서) 이 테마로 습격이 시작돼요").withStyle(ChatFormatting.GRAY));
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }
}
