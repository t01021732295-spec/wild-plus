package com.snolred.wildplus.item;

import com.snolred.wildplus.content.Ranch;
import net.minecraft.ChatFormatting;
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

/** 3.4 동물 브러시: 소 · 닭 · 양 · 염소 · 돼지를 하루 한 번 빗질 → 애정도 +40 · 오늘의 생산물 (하트가 높을수록 좋은 등급) */
public class AnimalBrushItem extends Item {
    public AnimalBrushItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!Ranch.isRanchAnimal(target)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        long today = Ranch.today(level);
        if (target.getPersistentData().getLongOr(Ranch.BRUSH_DAY, -1L) == today) {
            sp.sendOverlayMessage(Component.literal("🪮 오늘은 이미 빗질했어요 · " + Ranch.heartText(target)).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        target.getPersistentData().putLong(Ranch.BRUSH_DAY, today);
        Ranch.addHearts(target, 40);
        ItemStack got = Ranch.product(target, level);
        Ranch.contest(sp, got);
        String name = got.getHoverName().getString();
        int count = got.getCount();
        if (!sp.getInventory().add(got)) net.minecraft.world.Containers.dropItemStack(level, sp.getX(), sp.getY(), sp.getZ(), got);
        level.playSound(null, target.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.2F);
        sp.sendOverlayMessage(Component.literal(String.format("🪮 %s %s · %s ×%d", target.getName().getString(), Ranch.heartText(target), name, count))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        stack.hurtAndBreak(1, sp, hand);
        return InteractionResult.SUCCESS;
    }
}
