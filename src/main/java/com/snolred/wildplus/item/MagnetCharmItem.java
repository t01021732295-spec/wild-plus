package com.snolred.wildplus.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** 자석 부적: 켜 두면 주변 8칸 안의 아이템·경험치 구슬을 끌어온다. 우클릭으로 켜고 끄기. 웅크리면 잠시 멈춤. */
public class MagnetCharmItem extends Item {
    private static final String KEY = "wildplus_magnet_on";
    private static final double RANGE = 8.0;

    public MagnetCharmItem(Properties properties) {
        super(properties);
    }

    public static boolean isOn(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(KEY, true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            boolean on = !isOn(stack);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KEY, on));
            sp.sendOverlayMessage(Component.translatable(on ? "message.wildplus.magnet.on" : "message.wildplus.magnet.off")
                    .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, on ? 1.6F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isOn(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player) || player.isSpectator() || player.isShiftKeyDown() || !isOn(stack)) {
            return;
        }
        if (level.getGameTime() % 2 != 0) {
            return;
        }
        Vec3 target = player.position().add(0, 0.6, 0);
        List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(RANGE),
                e -> (e instanceof ItemEntity item && !item.hasPickUpDelay()) || e instanceof ExperienceOrb);
        for (Entity e : nearby) {
            Vec3 dir = target.subtract(e.position());
            double dist = dir.length();
            if (dist < 0.6) continue;
            e.setDeltaMovement(dir.normalize().scale(Math.min(0.45, 0.12 + dist * 0.05)));
        }
    }
}
