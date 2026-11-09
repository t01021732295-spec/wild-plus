package com.snolred.wildplus.net;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.music.Music;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 4.6 음악: 재생 · 정지 알림, 파일 요청과 전송 */
public final class MusicNet {
    private MusicNet() {}

    public record Play(String key, String sha, long size, long pos, float volume, int range, boolean loop) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(WildPlus.id("music_play"));
        public static final StreamCodec<FriendlyByteBuf, Play> CODEC = StreamCodec.of((b, p) -> {
            b.writeUtf(p.key);
            b.writeUtf(p.sha);
            b.writeVarLong(p.size);
            b.writeLong(p.pos);
            b.writeFloat(p.volume);
            b.writeVarInt(p.range);
            b.writeBoolean(p.loop);
        }, b -> new Play(b.readUtf(), b.readUtf(), b.readVarLong(), b.readLong(), b.readFloat(), b.readVarInt(), b.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Stop(String key) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(WildPlus.id("music_stop"));
        public static final StreamCodec<FriendlyByteBuf, Stop> CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.key), b -> new Stop(b.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Request(String sha) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(WildPlus.id("music_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.sha), b -> new Request(b.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Chunk(String sha, int index, int total, byte[] data) implements CustomPacketPayload {
        public static final Type<Chunk> TYPE = new Type<>(WildPlus.id("music_chunk"));
        public static final StreamCodec<FriendlyByteBuf, Chunk> CODEC = StreamCodec.of((b, p) -> {
            b.writeUtf(p.sha);
            b.writeVarInt(p.index);
            b.writeVarInt(p.total);
            b.writeByteArray(p.data);
        }, b -> new Chunk(b.readUtf(), b.readVarInt(), b.readVarInt(), b.readByteArray(64_000)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Help() implements CustomPacketPayload {
        public static final Type<Help> TYPE = new Type<>(WildPlus.id("music_help"));
        public static final StreamCodec<FriendlyByteBuf, Help> CODEC = StreamCodec.unit(new Help());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenDir() implements CustomPacketPayload {
        public static final Type<OpenDir> TYPE = new Type<>(WildPlus.id("music_open_dir"));
        public static final StreamCodec<FriendlyByteBuf, OpenDir> CODEC = StreamCodec.unit(new OpenDir());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Play.TYPE, Play.CODEC, MusicNet::onPlay);
        r.playToClient(Stop.TYPE, Stop.CODEC, MusicNet::onStop);
        r.playToClient(Chunk.TYPE, Chunk.CODEC, MusicNet::onChunk);
        r.playToClient(Help.TYPE, Help.CODEC, MusicNet::onHelp);
        r.playToClient(OpenDir.TYPE, OpenDir.CODEC, (p, c) -> ClientBridge.openDir());
        r.playToServer(Request.TYPE, Request.CODEC, MusicNet::onRequest);
    }

    private static void onPlay(Play p, IPayloadContext c) {
        ClientBridge.play(p);
    }

    private static void onStop(Stop p, IPayloadContext c) {
        ClientBridge.stop(p);
    }

    private static void onChunk(Chunk p, IPayloadContext c) {
        ClientBridge.chunk(p);
    }

    private static void onHelp(Help p, IPayloadContext c) {
        ClientBridge.help();
    }

    private static void onRequest(Request p, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer sp) Music.request(sp, p.sha());
    }

    /** 전용 서버에서 클라이언트 클래스가 읽히지 않게 따로 둔다 */
    static final class ClientBridge {
        static void play(Play p) {
            com.snolred.wildplus.client.MusicClient.play(p);
        }

        static void stop(Stop p) {
            com.snolred.wildplus.client.MusicClient.stop(p.key());
        }

        static void chunk(Chunk p) {
            com.snolred.wildplus.client.MusicClient.chunk(p);
        }

        static void openDir() {
            com.snolred.wildplus.client.MusicClient.openDir();
        }

        static void help() {
            com.snolred.wildplus.client.MusicClient.help();
        }
    }
}
