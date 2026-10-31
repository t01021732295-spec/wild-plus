package com.snolred.wildplus.item;

import com.snolred.wildplus.event.RandomEvents;
import com.snolred.wildplus.registry.ModItems7;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 이벤트 투척병: 던져서 깨지면 그 자리에서 이벤트가 바로 시작된다 (이미 진행 중인 이벤트는 끝나고 바뀜).
 * 병에 이벤트 이름이 적혀 있으면 그 이벤트, 없으면 무작위 (위험한 이벤트도 나올 수 있음).
 */
public class EventBottleItem extends Item {
    public static final String KEY = "wildplus_event";

    public EventBottleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(RandomEvents.Type type) {
        ItemStack s = new ItemStack(ModItems7.EVENT_BOTTLE.get());
        if (type != null) CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> tag.putString(KEY, type.name()));
        return s;
    }

    public static RandomEvents.Type typeOf(ItemStack stack) {
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(KEY, "");
        if (name.isEmpty()) return null;
        try {
            return RandomEvents.Type.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        RandomEvents.Type t = typeOf(stack);
        if (t == null) return Component.translatable("item.wildplus.event_bottle.random");
        return Component.translatable("item.wildplus.event_bottle.of", Component.translatable(t.key()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation(Snowball::new, server, stack, player, -20.0F, 0.5F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack, 40);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
