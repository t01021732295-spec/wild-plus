package com.snolred.wildplus.event;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.registry.ModBlocks12;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems22;
import com.snolred.wildplus.registry.ModItems23;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 4.0 공허 유적: 열쇠로 입구를 열고 → 복도 레버 3개를 당기면 보스실이 열리며 문지기 등장 → 처치하면 창고가 열림.
 * 게임 속 7일 뒤 열쇠로 다시 열면 유적 전체가 복구되고 문지기가 한 단계 강해진다 (최대 10).
 * 좌표는 tools/structures_v40.py 와 같아야 한다.
 */
public final class VoidRuin {
    private VoidRuin() {}

    private static final BlockPos T_SEAL = new BlockPos(11, 17, 18), T_ALTAR = new BlockPos(81, 16, 18);
    private static final String BOSS = "wildplus_ruin_boss", ALTAR = "wildplus_ruin_altar", CLONE = "wildplus_ruin_clone", HALF = "wildplus_ruin_half", TIER = "wildplus_ruin_tier";
    private static final long WEEK = 168000L;

    private record Pending(ServerLevel level, BlockPos pos) {}

    private static final List<Pending> LEVERS = new ArrayList<>();
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, ServerLevel> LEVELS = new HashMap<>();

    private static Rotation rotationOf(Direction world) {
        for (Rotation r : Rotation.values()) if (r.rotate(Direction.WEST) == world) return r;
        return Rotation.NONE;
    }

    private static BlockPos turn(BlockPos p, Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> new BlockPos(-p.getZ(), p.getY(), p.getX());
            case CLOCKWISE_180 -> new BlockPos(-p.getX(), p.getY(), -p.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(p.getZ(), p.getY(), -p.getX());
            default -> p;
        };
    }

    private static CompoundTag entry(ServerLevel level, BlockPos altar) {
        ContentData d = ContentData.get(level.getServer());
        CompoundTag s = d.section("void_ruins");
        String key = Long.toString(altar.asLong());
        if (s.get(key) instanceof CompoundTag have) return have;
        CompoundTag c = new CompoundTag();
        c.putInt("tier", 1);
        s.put(key, c);
        d.dirty();
        return c;
    }

    private static int open(ServerLevel level, BlockPos center, double radius) {
        int n = 0, r = Mth.ceil(radius);
        for (BlockPos q : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 8, r))) {
            if (q.distSqr(center) > radius * radius || !level.getBlockState(q).is(ModBlocks12.VOID_GATE.get())) continue;
            level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
            level.sendParticles(ParticleTypes.PORTAL, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.3);
            n++;
        }
        if (n > 0) level.playSound(null, center, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
        return n;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || level.dimension() != Level.END) return;
        BlockPos pos = event.getPos();
        BlockState st = level.getBlockState(pos);
        if (st.is(Blocks.LEVER)) {
            LEVERS.add(new Pending(level, pos.immutable()));
            return;
        }
        if (!st.is(ModBlocks12.VOID_SEAL.get())) return;
        event.setCanceled(true);
        Rotation rot = rotationOf(st.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING));
        BlockPos origin = pos.subtract(turn(T_SEAL, rot));
        BlockPos altar = origin.offset(turn(T_ALTAR, rot));
        CompoundTag e = entry(level, altar);
        long now = level.getGameTime();
        boolean cleared = e.getBooleanOr("cleared", false);
        if (cleared && now < e.getLongOr("ready", 0L)) {
            long left = (e.getLongOr("ready", 0L) - now) / 24000L + 1;
            p.sendOverlayMessage(Component.literal("다시 봉인되기까지 게임 속 약 " + left + "일 남았어요").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!p.getItemInHand(event.getHand()).is(ModItems23.VOID_KEY.get())) {
            p.sendOverlayMessage(Component.literal("공허 열쇠로 열 수 있어요").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (cleared) {
            StructureTemplate t = level.getStructureTemplateManager().get(WildPlus.id("void_ruin")).orElse(null);
            if (t == null) return;
            t.placeInWorld(level, origin, origin, new StructurePlaceSettings().setRotation(rot), level.getRandom(), Block.UPDATE_ALL);
            e.putBoolean("cleared", false);
            e.putInt("tier", Math.min(10, e.getIntOr("tier", 1) + 1));
            ContentData.get(level.getServer()).dirty();
            p.sendSystemMessage(Component.literal("🌌 유적이 다시 채워졌어요. 문지기 " + e.getIntOr("tier", 1) + "단계").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (open(level, pos, 6.5) == 0) {
            p.sendOverlayMessage(Component.literal("이미 열려 있어요").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        p.getItemInHand(event.getHand()).consume(1, p);
    }

    private static void checkLevers(Pending l) {
        ServerLevel level = l.level;
        List<BlockPos> found = new ArrayList<>();
        BlockPos altar = null;
        for (BlockPos q : BlockPos.betweenClosed(l.pos.offset(-14, -2, -14), l.pos.offset(14, 3, 14))) {
            if (level.getBlockState(q).is(Blocks.LEVER)) found.add(q.immutable());
        }
        if (found.size() < 3) return;
        for (BlockPos q : found) if (!level.getBlockState(q).getValue(LeverBlock.POWERED)) return;
        for (BlockPos q : BlockPos.betweenClosed(l.pos.offset(-50, -4, -50), l.pos.offset(50, 2, 50))) {
            if (level.getBlockState(q).is(ModBlocks12.VOID_ALTAR.get())) {
                altar = q.immutable();
                break;
            }
        }
        if (altar == null || open(level, l.pos, 17.0) == 0) return;
        spawnBoss(level, altar);
    }

    private static void set(Mob m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double v) {
        AttributeInstance a = m.getAttribute(attr);
        if (a != null) a.setBaseValue(v);
    }

    private static void spawnBoss(ServerLevel level, BlockPos altar) {
        Mob b = ModEntities.VOID_BOSS.get().create(level, EntitySpawnReason.EVENT);
        if (b == null) return;
        int tier = entry(level, altar).getIntOr("tier", 1);
        b.snapTo(altar.getX() + 0.5, altar.getY() + 1, altar.getZ() + 0.5, 0F, 0F);
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(altar), EntitySpawnReason.EVENT, null);
        set(b, Attributes.MAX_HEALTH, 600 * (1.0 + 0.35 * (tier - 1)));
        set(b, Attributes.ATTACK_DAMAGE, 15 * (1.0 + 0.15 * (tier - 1)));
        set(b, Attributes.ARMOR, 16);
        set(b, Attributes.ARMOR_TOUGHNESS, 8);
        set(b, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        set(b, Attributes.FOLLOW_RANGE, 48);
        set(b, Attributes.SCALE, 1.5);
        b.setHealth(b.getMaxHealth());
        b.setPersistenceRequired();
        var d = b.getPersistentData();
        d.putBoolean(BOSS, true);
        d.putLong(ALTAR, altar.asLong());
        d.putInt(TIER, tier);
        Component name = Component.literal("공허의 문지기" + (tier > 1 ? " · " + tier + "단계" : "")).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        b.setCustomName(name);
        b.setCustomNameVisible(true);
        level.addFreshEntity(b);
        BARS.put(b.getUUID(), new ServerBossEvent(UUID.randomUUID(), name, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_12));
        LEVELS.put(b.getUUID(), level);
        level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!LEVERS.isEmpty()) {
            List<Pending> copy = new ArrayList<>(LEVERS);
            LEVERS.clear();
            copy.forEach(VoidRuin::checkLevers);
        }
        if (BARS.isEmpty() || event.getServer().getTickCount() % 5 != 0) return;
        var it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerLevel level = LEVELS.get(en.getKey());
            ServerBossEvent bar = en.getValue();
            if (level == null || !(level.getEntity(en.getKey()) instanceof Mob b) || !b.isAlive()) {
                bar.removeAllPlayers();
                it.remove();
                continue;
            }
            bar.setProgress(Mth.clamp(b.getHealth() / b.getMaxHealth(), 0F, 1F));
            List<ServerPlayer> near = new ArrayList<>();
            for (ServerPlayer p : level.players()) if (!p.isSpectator() && p.distanceToSqr(b) < 36 * 36) near.add(p);
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
            near.forEach(p -> {
                if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            });
            patterns(level, b, near);
        }
    }

    private static void patterns(ServerLevel level, Mob b, List<ServerPlayer> near) {
        if (near.isEmpty()) return;
        var r = level.getRandom();
        if (b.tickCount % 140 < 5) { // 순간이동 강타
            ServerPlayer t = near.get(r.nextInt(near.size()));
            Vec3 back = t.position().subtract(t.getLookAngle().multiply(2, 0, 2));
            if (level.noCollision(b, b.getBoundingBox().move(back.subtract(b.position())))) {
                level.sendParticles(ParticleTypes.PORTAL, b.getX(), b.getY() + 1, b.getZ(), 40, 0.5, 1, 0.5, 0.4);
                b.teleportTo(back.x, back.y, back.z);
                level.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.7F);
                b.setTarget(t);
                AwakenedDragon.warn(level, new Vec3(b.getX(), b.getY(), b.getZ()), 4.0, 16, 8, false, b);
            }
        }
        var d = b.getPersistentData();
        if (b.getHealth() < b.getMaxHealth() * 0.5F && !d.getBooleanOr(HALF, false)) { // 절반: 분신 · 장판 시작
            d.putBoolean(HALF, true);
            for (int i = 0; i < 2; i++) {
                Mob c = ModEntities.VOID_BOSS.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (c == null) continue;
                c.snapTo(b.getX() + (i == 0 ? 3 : -3), b.getY(), b.getZ(), 0F, 0F);
                set(c, Attributes.MAX_HEALTH, 80);
                c.setHealth(80);
                c.getPersistentData().putBoolean(CLONE, true);
                c.setCustomName(Component.literal("문지기의 분신").withStyle(ChatFormatting.DARK_PURPLE));
                c.setTarget(near.get(0));
                level.addFreshEntity(c);
            }
            near.forEach(p -> p.sendOverlayMessage(Component.literal("문지기가 분신을 불렀어요!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
        }
        if (d.getBooleanOr(HALF, false) && b.tickCount % 200 < 5) {
            for (int i = 0; i < 3; i++) {
                ServerPlayer p = near.get(r.nextInt(near.size()));
                AwakenedDragon.warn(level, new Vec3(p.getX() + r.nextInt(7) - 3, p.getY(), p.getZ() + r.nextInt(7) - 3), 3.0, 34, 9, false, b);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !e.getPersistentData().getBooleanOr(BOSS, false)) return;
        BlockPos altar = BlockPos.of(e.getPersistentData().getLongOr(ALTAR, 0L));
        open(level, altar, 18.0);
        CompoundTag c = entry(level, altar);
        c.putBoolean("cleared", true);
        c.putLong("ready", level.getGameTime() + WEEK);
        ContentData.get(level.getServer()).dirty();
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🌌 공허의 문지기가 쓰러졌어요. 창고가 열렸어요!").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /** 엔드의 엔더맨 · 셜커가 파편을, 문지기가 심장을 떨군다 */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || level.dimension() != Level.END || !(event.getSource().getEntity() instanceof Player)) return;
        float chance = e.getType() == EntityTypes.SHULKER ? 0.4F : e.getType() == EntityTypes.ENDERMAN ? 0.15F : 0F;
        var data = e.getPersistentData();
        if (data.getBooleanOr(BOSS, false)) {
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(ModItems22.VOID_SHARD.get(), 12 + data.getIntOr(TIER, 1) * 2)));
            if (level.getRandom().nextFloat() < 0.25F) event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(ModItems22.VOID_HEART.get())));
        } else if (chance > 0 && level.getRandom().nextFloat() < chance) {
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(ModItems22.VOID_SHARD.get())));
        }
    }
}
