package com.snolred.wildplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * 귀환의 두루마리: 3초 동안 읽으면 침대(리스폰 지점) 또는 월드 스폰으로 순간이동.
 * 읽는 도중 피해를 입거나 손을 떼면 취소된다(바닐라 아이템 사용 중단 규칙).
 */
public class RecallScrollItem extends Item {
    private static final int USE_TICKS = 60;

    public RecallScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
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
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel) {
            if (Recall.teleportHome(player)) {
                player.displayClientMessage(Component.translatable("message.wildplus.recall.success")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.getCooldowns().addCooldown(stack, 20 * 10);
            } else {
                player.displayClientMessage(Component.translatable("message.wildplus.recall.fail")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
        return stack;
    }

    /** 순간이동 로직은 버전 차이를 흡수하기 위해 따로 둔다 */
    static final class Recall {
        static boolean teleportHome(ServerPlayer player) {
            ServerLevel from = player.level();
            BlockPos dest = RecallTarget.find(player);
            ServerLevel target = RecallTarget.level(player);
            if (dest == null || target == null) {
                return false;
            }
            from.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.teleportTo(target, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            target.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
    }
}
