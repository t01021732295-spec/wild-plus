package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.net.DoubleJump;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 스카이 사파이어 부츠 더블 점프 (클라이언트): 점프 키를 뗐다가 공중에서 다시 누르면 한 번 더 */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class JumpClient {
    private JumpClient() {}

    private static boolean used, wasDown;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return;
        boolean down = mc.options.keyJump.isDown();
        if (p.onGround() || p.isInWater() || p.onClimbable()) used = false;
        if (down && !wasDown && !p.onGround() && !used && !p.isInWater() && !p.isFallFlying() && !p.getAbilities().flying
                && !p.isPassenger() && DoubleJump.wearing(p)) {
            used = true;
            Vec3 v = p.getDeltaMovement();
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            p.setDeltaMovement(v.x + look.x * 0.15, DoubleJump.POWER, v.z + look.z * 0.15);
            p.resetFallDistance();
            if (mc.getConnection() != null) mc.getConnection().send(new ServerboundCustomPayloadPacket(new DoubleJump.Payload()));
        }
        wasDown = down;
    }
}
