package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.home.HouseKitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 집 상자를 들고 땅을 보면 집이 들어설 자리를 빛나는 테두리 (금색 · 현관 쪽은 초록) 로 보여 준다 */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class HousePreview {
    private HousePreview() {}

    private static final DustParticleOptions EDGE = new DustParticleOptions(0xFFD24A, 1.2F), FRONT = new DustParticleOptions(0x46E07A, 1.4F);

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.tickCount % 3 != 0) return;
        if (!(p.getMainHandItem().getItem() instanceof HouseKitItem kit)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos target = hit.getBlockPos();
        if (mc.level.getBlockState(target).canBeReplaced()) target = target.below();
        Direction look = p.getDirection();
        int[] fp = kit.footprint(target, look);
        double y = target.getY() + 1.05;
        for (int x = fp[0]; x <= fp[2] + 1; x++) {
            for (int z : new int[]{fp[1], fp[3] + 1}) mark(mc, x, y, z, isFront(look, z, fp, true));
        }
        for (int z = fp[1]; z <= fp[3] + 1; z++) {
            for (int x : new int[]{fp[0], fp[2] + 1}) mark(mc, x, y, z, isFront(look, x, fp, false));
        }
        // 모서리 기둥 (높이)
        for (int x : new int[]{fp[0], fp[2] + 1}) {
            for (int z : new int[]{fp[1], fp[3] + 1}) {
                for (int k = 0; k < kit.sy; k += 2) mc.level.addParticle(EDGE, x, y + k, z, 0, 0, 0);
            }
        }
    }

    /** 현관(나를 향한 쪽) 테두리인지 */
    private static boolean isFront(Direction look, int v, int[] fp, boolean zEdge) {
        return switch (look) {
            case NORTH -> zEdge && v == fp[3] + 1;
            case SOUTH -> zEdge && v == fp[1];
            case EAST -> !zEdge && v == fp[0];
            default -> !zEdge && v == fp[2] + 1;
        };
    }

    private static void mark(Minecraft mc, double x, double y, double z, boolean front) {
        mc.level.addParticle(front ? FRONT : EDGE, x, y, z, 0, 0, 0);
    }
}
