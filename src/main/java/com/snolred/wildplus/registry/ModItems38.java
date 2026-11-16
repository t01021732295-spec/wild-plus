package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.event.Noise;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

/** 5.3 침묵 향: 나와 8칸 안 친구들의 소음을 0으로, 30초 동안 소음이 20% 만 오름 */
public final class ModItems38 {
    private ModItems38() {}

    public static class Incense extends Item {
        public Incense(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel lv)) return InteractionResult.SUCCESS;
            for (ServerPlayer q : lv.players()) {
                if (q.distanceToSqr(player) > 64) continue;
                Noise.reset(q, 600);
                q.sendOverlayMessage(Component.literal("♪ 침묵 향 — 30초 동안 고요해요").withStyle(ChatFormatting.AQUA));
            }
            lv.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, 2, 1, 2, 0.02);
            lv.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            return InteractionResult.CONSUME;
        }
    }

    public static final DeferredItem<Incense> SILENCE_INCENSE = ITEMS.registerItem("silence_incense", p -> new Incense(p.stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static void init() {}
}
