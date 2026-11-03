package com.snolred.wildplus.net;

import com.snolred.wildplus.WildPlus;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 스카이 사파이어 부츠 더블 점프: 공중에서 점프 키를 한 번 더 누르면 한 번 더 뛴다 (땅에 닿으면 다시 충전).
 * 움직임은 클라이언트가 바로 처리하고(JumpClient), 서버는 낙하 피해를 지우고 효과를 보여 준다.
 */
public final class DoubleJump {
    private DoubleJump() {}

    public static final double POWER = 0.62;
    private static final Set<UUID> USED = new HashSet<>();

    public record Payload() implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(WildPlus.id("double_jump"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.unit(new Payload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static boolean wearing(Player p) {
        return p.getItemBySlot(EquipmentSlot.FEET).is(com.snolred.wildplus.registry.ModItems11.SKY_SAPPHIRE_BOOTS.get());
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Payload.TYPE, Payload.CODEC, DoubleJump::handle);
    }

    private static void handle(Payload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !wearing(sp) || sp.onGround() || USED.contains(sp.getUUID())) return;
            USED.add(sp.getUUID());
            sp.resetFallDistance();
            sp.level().sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY(), sp.getZ(), 12, 0.3, 0.05, 0.3, 0.02);
            sp.level().sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 0.1, sp.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
        });
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.onGround()) USED.remove(p.getUUID());
    }
}
