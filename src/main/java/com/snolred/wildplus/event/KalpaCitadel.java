package com.snolred.wildplus.event;

import com.snolred.wildplus.block.KalpaBrazierBlock;
import com.snolred.wildplus.entity.KalpaLordEntity;
import com.snolred.wildplus.registry.ModBlocks22;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 5.1 겁화의 성채: 모서리 탑 꼭대기 화로 4개에 불을 붙이면 본성 문(kalpa_gate)이 녹아내린다.
 * 본성 가운데 군주의 제단에 겁화석 3개 → 겁화의 군주 (KalpaLordEntity).
 */
public final class KalpaCitadel {
    private KalpaCitadel() {}

    /** 화로는 성채 모서리(서로 38칸 떨어짐)에 있어서 반경 44 · 위아래 28 을 훑는다 */
    private static final int R = 44, H = 28;

    public static void onLit(ServerLevel level, BlockPos pos, Player who) {
        List<BlockPos> braziers = new ArrayList<>();
        int lit = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -H; dy <= H; dy++) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    var st = level.getBlockState(m);
                    if (st.getBlock() instanceof KalpaBrazierBlock) {
                        braziers.add(m.immutable());
                        if (st.getValue(KalpaBrazierBlock.LIT)) lit++;
                    }
                }
            }
        }
        int need = Math.min(4, Math.max(1, braziers.size()));
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (p.blockPosition().closerThan(pos, 96)) near.add(p);
        if (lit < need) {
            for (ServerPlayer p : near)
                p.sendSystemMessage(Component.literal("🔥 겁화의 화로 " + lit + " / " + need + " · " + who.getName().getString() + " 님이 불을 붙였어요").withStyle(ChatFormatting.GOLD));
            return;
        }
        // 화로들의 가운데 = 본성. 그 둘레의 문을 연다
        long sx = 0, sy = 0, sz = 0;
        for (BlockPos b : braziers) {
            sx += b.getX();
            sy += b.getY();
            sz += b.getZ();
        }
        int n = braziers.size();
        BlockPos center = new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
        int opened = 0;
        for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -24; dz <= 24; dz++) {
                for (int dy = -24; dy <= 6; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(m).is(ModBlocks22.KALPA_GATE.get())) continue;
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.sendParticles(ParticleTypes.LAVA, m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.1);
                    opened++;
                }
            }
        }
        if (opened > 0) level.playSound(null, center, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.6F);
        for (ServerPlayer p : near) {
            p.sendSystemMessage(Component.literal("🔥 화로 " + need + "개가 모두 타오릅니다! 본성의 문이 녹아내렸어요").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            p.sendSystemMessage(Component.literal("   본성 가운데 제단에 겁화석 3개를 바치면 겁화의 군주가 깨어나요").withStyle(ChatFormatting.GOLD));
        }
    }

    /** 군주는 체력 상한(1024)을 넘는 몫만큼 받는 피해를 줄임 · 3단계(핵 개방)에는 받는 피해 +30% */
    @SubscribeEvent
    public static void onLordHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof KalpaLordEntity l) event.setAmount(event.getAmount() * l.damageScale());
    }
}
