package com.snolred.wildplus.net;

import com.snolred.wildplus.WildPlus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 통장 잔액을 클라이언트로 보내 HUD "소지금"에 동전 + 통장을 함께 보여 준다.
 * 그리고 서버가 켜지면 모든 플레이어 이름표 아래 · Tab 목록에 체력을 표시한다.
 */
public final class BankSync {
    private BankSync() {}

    /** 클라이언트가 마지막으로 받은 통장 잔액 */
    public static volatile long clientBank;

    public record Payload(long balance) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(WildPlus.id("bank"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Payload::balance, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC, BankSync::handle);
    }

    private static void handle(Payload p, IPayloadContext ctx) {
        clientBank = p.balance();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 5) return;
        long bal = com.snolred.wildplus.economy.WildData.get(p.level().getServer()).balance(p.getUUID());
        PacketDistributor.sendToPlayer(p, new Payload(bal));
    }

    /** 모든 플레이어 체력 표시 (이름표 아래 + Tab 목록) */
    @SubscribeEvent
    public static void onStart(ServerStartedEvent event) {
        var s = event.getServer();
        String[] cmds = {
                "scoreboard objectives add wp_hp health {\"text\":\"❤\",\"color\":\"red\"}",
                "scoreboard objectives modify wp_hp rendertype hearts",
                "scoreboard objectives setdisplay below_name wp_hp",
                "scoreboard objectives setdisplay list wp_hp"};
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }
}
