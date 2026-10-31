package com.snolred.wildplus.item;

import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * 망자의 두루마리: 3초 동안 읽으면 마지막으로 죽은 곳으로 순간이동 (다른 차원이어도).
 * 도착하면 10초 동안 화염 저항 · 느린 낙하 · 저항 (용암·낙사·몹 사이 대비). 한 번 쓰면 사라진다.
 */
public class DeathScrollItem extends Item {
    private static final int USE_TICKS = 60;

    public DeathScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getLastDeathLocation().isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.death_scroll.none").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        Optional<GlobalPos> death = player.getLastDeathLocation();
        ServerLevel target = death.map(g -> player.level().getServer().getLevel(g.dimension())).orElse(null);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.wildplus.death_scroll.none").withStyle(ChatFormatting.RED));
            return stack;
        }
        BlockPos pos = death.get().pos();
        int y = Math.max(pos.getY(), target.getMinY() + 1);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.teleportTo(target, pos.getX() + 0.5, y + 0.1, pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
        target.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.wildplus.death_scroll.done", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.DARK_AQUA));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(stack, 20 * 10);
        return stack;
    }
}
