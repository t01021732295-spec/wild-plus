package com.snolred.wildplus.event;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModEntities;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 5.3 소음 게이지 (딥다크 안에서만): 0~100.
 * 달리기 · 점프 · 블록 부수기/놓기 · 전투 · 문 = 올라감 / 웅크리기 · 양털 위 = 거의 안 올라감 / 3초 조용하면 내려감.
 * 25 촉수가 꿈틀 · 55 스컬크 추적자 · 80 추적자 떼 + 메아리 박쥐 + 침묵의 사제 · 100 워든 소환 (고대의 워든, 3분에 한 번).
 */
public final class Noise {
    private Noise() {}

    private static final class St {
        float v;
        long lastGain, nextSpawn, nextWarden, calmUntil;
        boolean wasGround = true;
        int sent = -2;
    }

    private static final Map<UUID, St> ST = new HashMap<>();

    /** 클라이언트가 받은 값 (-1 = 딥다크 밖, 숨김) */
    public static volatile int client = -1;

    public record Payload(int value) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(WildPlus.id("noise"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Payload::value, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC, (p, ctx) -> client = p.value());
    }

    private static St st(Player p) {
        return ST.computeIfAbsent(p.getUUID(), k -> new St());
    }

    public static boolean inDeepDark(Player p) {
        return p.level().getBiome(p.blockPosition()).is(Biomes.DEEP_DARK);
    }

    public static float get(Player p) {
        St s = ST.get(p.getUUID());
        return s == null ? 0 : s.v;
    }

    /** 소음 올리기 (딥다크 밖이면 무시) · 고요 향 효과 중엔 20% 만 */
    public static void add(Player p, float n) {
        if (!(p instanceof ServerPlayer) || p.isCreative() || p.isSpectator() || !inDeepDark(p)) return;
        St s = st(p);
        long now = p.level().getGameTime();
        if (now < s.calmUntil) n *= 0.2F;
        s.v = Math.min(100F, s.v + n);
        s.lastGain = now;
    }

    public static void reset(Player p, int calmTicks) {
        St s = st(p);
        s.v = 0;
        s.calmUntil = p.level().getGameTime() + calmTicks;
    }

    private static boolean onWool(Player p) {
        BlockPos b = p.blockPosition().below();
        return p.level().getBlockState(b).is(BlockTags.WOOL) || p.level().getBlockState(p.blockPosition()).is(BlockTags.WOOL_CARPETS);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        boolean dd = p.tickCount % 10 == 0 ? inDeepDark(p) : st(p).sent >= 0;
        St s = st(p);
        if (!dd) {
            if (s.sent != -1) {
                s.v = 0;
                s.sent = -1;
                PacketDistributor.sendToPlayer(p, new Payload(-1));
            }
            return;
        }
        long now = lv.getGameTime();
        boolean quiet = p.isShiftKeyDown() || onWool(p);
        double moved = p.getDeltaMovement().horizontalDistanceSqr();
        if (!quiet && moved > 0.002) add(p, p.isSprinting() ? 0.14F : 0.03F);
        boolean ground = p.onGround();
        if (s.wasGround && !ground && p.getDeltaMovement().y > 0.2 && !quiet) add(p, 2.5F);
        s.wasGround = ground;
        if (now - s.lastGain > 60) s.v = Math.max(0, s.v - 0.15F);
        int iv = Math.round(s.v);
        if (p.tickCount % 5 == 0 && iv != s.sent) {
            s.sent = iv;
            PacketDistributor.sendToPlayer(p, new Payload(iv));
        }
        if (p.tickCount % 20 == 0) stages(lv, p, s, now);
    }

    private static void stages(ServerLevel lv, ServerPlayer p, St s, long now) {
        if (p.isCreative() || p.isSpectator()) return;
        if (s.v >= 25) { // 촉수가 꿈틀
            for (int i = 0; i < 4; i++) {
                double a = lv.random.nextDouble() * Math.PI * 2, r = 2 + lv.random.nextDouble() * 4;
                lv.sendParticles(ParticleTypes.SCULK_CHARGE_POP, p.getX() + Math.cos(a) * r, p.getY() + 0.1, p.getZ() + Math.sin(a) * r, 2, 0.1, 0, 0.1, 0);
            }
            if (lv.random.nextInt(3) == 0) lv.playSound(null, p.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.HOSTILE, 0.8F, 0.8F);
        }
        if (s.v >= 100 && now >= s.nextWarden) {
            s.nextWarden = now + 3600;
            s.v = 60;
            if (lv.getEntitiesOfClass(Warden.class, new AABB(p.blockPosition()).inflate(48)).isEmpty()) {
                BlockPos at = spot(lv, p, 8, 12);
                Warden w = net.minecraft.world.entity.EntityTypes.WARDEN.create(lv, EntitySpawnReason.TRIGGERED);
                if (w != null && at != null) {
                    w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, lv.random.nextFloat() * 360F, 0F);
                    lv.addFreshEntity(w);
                    w.increaseAngerAt(p, 80, false);
                    for (ServerPlayer q : lv.players())
                        if (q.distanceToSqr(p) < 64 * 64)
                            q.sendSystemMessage(Component.literal("🔊 너무 시끄러웠어요… 고대의 워든이 깨어났습니다!").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
                }
            }
            return;
        }
        if (s.v >= 55 && now >= s.nextSpawn) {
            boolean heavy = s.v >= 80;
            s.nextSpawn = now + (heavy ? 400 : 600);
            int stalkers = heavy ? 2 : 1;
            for (int i = 0; i < stalkers; i++) spawn(lv, p, ModEntities.SCULK_STALKER.get());
            if (heavy) {
                spawn(lv, p, ModEntities.ECHO_BATS.get());
                if (lv.random.nextInt(3) == 0) spawn(lv, p, ModEntities.SILENT_PRIEST.get());
            }
            p.sendOverlayMessage(Component.literal(heavy ? "🔊 소음이 너무 커요! 무언가 몰려옵니다" : "🔊 무언가 소리를 듣고 다가옵니다…").withStyle(heavy ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
    }

    private static void spawn(ServerLevel lv, ServerPlayer p, EntityType<? extends Mob> type) {
        if (lv.getEntitiesOfClass(Mob.class, new AABB(p.blockPosition()).inflate(32), m -> m.getType() == type).size() >= 6) return;
        BlockPos at = spot(lv, p, 10, 16);
        if (at == null) return;
        Mob m = type.create(lv, EntitySpawnReason.EVENT);
        if (m == null) return;
        m.snapTo(at.getX() + 0.5, at.getY() + (type == ModEntities.ECHO_BATS.get() ? 2 : 0), at.getZ() + 0.5, lv.random.nextFloat() * 360F, 0F);
        m.setTarget(p);
        lv.addFreshEntity(m);
        lv.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
    }

    /** 사람 주변 min~max 칸 떨어진, 발 디딜 곳 */
    public static BlockPos spot(ServerLevel lv, Player p, int min, int max) {
        for (int tries = 0; tries < 16; tries++) {
            double a = lv.random.nextDouble() * Math.PI * 2, r = min + lv.random.nextDouble() * (max - min);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * r), z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
            for (int dy = 6; dy >= -6; dy--) {
                BlockPos b = new BlockPos(x, (int) p.getY() + dy, z);
                if (lv.getBlockState(b.below()).isSolidRender() && lv.getBlockState(b).isAir() && lv.getBlockState(b.above()).isAir()) return b;
            }
        }
        return null;
    }

    // ───────── 소리 나는 행동 ─────────
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!onWoolOrSneak(event.getPlayer())) add(event.getPlayer(), 4F);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && !onWoolOrSneak(p)) add(p, 2F);
    }

    private static boolean onWoolOrSneak(Player p) {
        return p.isShiftKeyDown() && onWool(p);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        var b = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock) add(event.getEntity(), 3F);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player p) add(p, 3F);
        if (event.getEntity() instanceof Player p) add(p, 2F);
    }
}
