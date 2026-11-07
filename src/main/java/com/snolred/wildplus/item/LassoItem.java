package com.snolred.wildplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 3.8 우민 올가미: 체력이 40% 이하로 떨어진 우민(약탈자 · 변명자 · 소환사 · 환술사)을 묶어 포로로 잡는다.
 * 묶은 올가미를 우민 감옥에 쓰면 포로가 감옥으로 들어간다. 웅크리고 쓰면 풀어 줌.
 */
public class LassoItem extends Item {
    public static final String KEY = "wildplus_captive", ELITE = "wildplus_captive_elite";

    public LassoItem(Properties properties) {
        super(properties);
    }

    public static boolean capturable(EntityType<?> t) {
        return t == EntityTypes.PILLAGER || t == EntityTypes.VINDICATOR || t == EntityTypes.EVOKER || t == EntityTypes.ILLUSIONER;
    }

    public static String captive(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(KEY, "");
    }

    public static boolean captiveElite(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(ELITE, false);
    }

    public static void clear(ItemStack s) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> {
            t.remove(KEY);
            t.remove(ELITE);
        });
    }

    public static String nameOf(String typeId) {
        return switch (typeId) {
            case "minecraft:pillager" -> "약탈자";
            case "minecraft:vindicator" -> "변명자";
            case "minecraft:evoker" -> "소환사";
            case "minecraft:illusioner" -> "환술사";
            default -> "우민";
        };
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack ignored, Player player, LivingEntity target, InteractionHand hand) {
        if (!capturable(target.getType())) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        if (!captive(stack).isEmpty()) {
            sp.sendOverlayMessage(Component.literal("이미 한 명이 묶여 있어요 — 우민 감옥에 먼저 넣으세요").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (target.getHealth() > target.getMaxHealth() * 0.4F) {
            sp.sendOverlayMessage(Component.literal(String.format("아직 기운이 넘쳐요 — 체력 40%% 이하로 만든 뒤 묶으세요 (지금 %d%%)",
                    Math.round(target.getHealth() * 100 / target.getMaxHealth()))).withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        boolean elite = com.snolred.wildplus.event.EliteMobs.isElite(target);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            t.putString(KEY, id);
            t.putBoolean(ELITE, elite);
        });
        if (target instanceof Raider r && r.getCurrentRaid() != null) r.getCurrentRaid().removeFromRaid(level, r, true);
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, target.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        target.discard();
        sp.sendSystemMessage(Component.literal("🪢 " + (elite ? "정예 " : "") + nameOf(id) + "을(를) 묶었어요! 우민 감옥에 쓰면 포로가 돼요")
                .withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String id = captive(stack);
        if (id.isEmpty() || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level instanceof ServerLevel sl) {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
            if (t != null && t.create(sl, EntitySpawnReason.TRIGGERED) instanceof Mob m) {
                m.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0F);
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(player.blockPosition()), EntitySpawnReason.TRIGGERED, null);
                m.setHealth(m.getMaxHealth() * 0.4F);
                sl.addFreshEntity(m);
            }
            clear(stack);
            player.sendOverlayMessage(Component.literal("포로를 풀어 줬어요").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = captive(stack);
        if (id.isEmpty()) return super.getName(stack);
        return Component.literal("우민 올가미 (" + (captiveElite(stack) ? "정예 " : "") + nameOf(id) + " 묶임)");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !captive(stack).isEmpty();
    }
}
