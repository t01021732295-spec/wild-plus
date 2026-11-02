package com.snolred.wildplus.net;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.ContentData;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 치장(날개 · 링 · 동물 탈)을 모든 클라이언트에 알려 준다. 2초마다 접속한 사람 전원의 치장 목록을 보낸다.
 * 클라이언트는 CosmeticLayer 에서 이 표를 보고 플레이어 몸에 그린다.
 */
public final class CosmeticSync {
    private CosmeticSync() {}

    /** 클라이언트: 플레이어 → [날개, 링, 탈] (0 = 없음) */
    public static final Map<UUID, int[]> CLIENT = new HashMap<>();

    public record Entry(UUID id, int wings, int halo, int hat) {
        static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                net.minecraft.core.UUIDUtil.STREAM_CODEC, Entry::id, ByteBufCodecs.VAR_INT, Entry::wings,
                ByteBufCodecs.VAR_INT, Entry::halo, ByteBufCodecs.VAR_INT, Entry::hat, Entry::new);
    }

    public record Payload(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(WildPlus.id("cosmetics"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
                Entry.CODEC.apply(ByteBufCodecs.list()), Payload::entries, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC, CosmeticSync::handle);
    }

    private static void handle(Payload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            CLIENT.clear();
            for (Entry e : p.entries()) CLIENT.put(e.id(), new int[]{e.wings(), e.halo(), e.hat()});
        });
    }

    public static int[] of(MinecraftServer s, UUID id) {
        CompoundTag d = ContentData.get(s).player("luxury", id);
        return new int[]{d.getIntOr("wings", 0), d.getIntOr("halo", 0), d.getIntOr("hat", 0)};
    }

    public static void broadcast(MinecraftServer s) {
        List<Entry> list = new ArrayList<>();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            int[] c = of(s, p.getUUID());
            if (c[0] != 0 || c[1] != 0 || c[2] != 0) list.add(new Entry(p.getUUID(), c[0], c[1], c[2]));
        }
        PacketDistributor.sendToAllPlayers(new Payload(list));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 == 11) broadcast(event.getServer());
    }
}
