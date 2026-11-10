package com.snolred.wildplus.net;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.Land;
import com.snolred.wildplus.economy.WildData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 4.8 미니맵 표시: 친구 · 사망 위치 · 워프 · 내 땅(청크)을 1초마다 보낸다 */
public final class MapNet {
    public static final int FRIEND = 0, DEATH = 1, WARP = 2, LAND = 3;

    public record Marker(int type, int x, int z, String label) {}

    public record Sync(String world, List<Marker> markers) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(WildPlus.id("map_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = StreamCodec.of((b, p) -> {
            b.writeUtf(p.world);
            b.writeVarInt(p.markers.size());
            for (Marker m : p.markers) {
                b.writeByte(m.type);
                b.writeInt(m.x);
                b.writeInt(m.z);
                b.writeUtf(m.label);
            }
        }, b -> {
            String w = b.readUtf();
            int n = b.readVarInt();
            List<Marker> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) l.add(new Marker(b.readByte(), b.readInt(), b.readInt(), b.readUtf()));
            return new Sync(w, l);
        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private MapNet() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Sync.TYPE, Sync.CODEC, (p, c) -> Client.accept(p));
    }

    static final class Client {
        static void accept(Sync p) {
            com.snolred.wildplus.client.MiniMap.accept(p);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 20 != 9) return;
        ContentData d = ContentData.get(s);
        CompoundTag meta = d.section("map");
        String world = meta.getStringOr("id", "");
        if (world.isEmpty()) {
            world = UUID.randomUUID().toString();
            meta.putString("id", world);
            d.dirty();
        }
        WildData wd = WildData.get(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String dim = p.level().dimension().identifier().toString();
            List<Marker> out = new ArrayList<>();
            for (ServerPlayer o : s.getPlayerList().getPlayers()) {
                if (o != p && o.level() == p.level() && !o.isSpectator()) out.add(new Marker(FRIEND, o.getBlockX(), o.getBlockZ(), o.getName().getString()));
            }
            p.getLastDeathLocation().ifPresent(g -> {
                if (g.dimension() == p.level().dimension()) out.add(new Marker(DEATH, g.pos().getX(), g.pos().getZ(), "사망 위치"));
            });
            for (WildData.Warp w : wd.warps.values()) {
                if (w.dimension.equals(dim) && (w.isPublic || w.owner.equals(p.getUUID()))) out.add(new Marker(WARP, w.pos.getX(), w.pos.getZ(), w.name));
            }
            for (int[] c : Land.chunksOf(p.getUUID(), dim)) out.add(new Marker(LAND, c[0], c[1], ""));
            PacketDistributor.sendToPlayer(p, new Sync(world, out));
        }
    }
}
