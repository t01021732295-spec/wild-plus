package com.snolred.wildplus.event;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.registry.ModBlocks14;
import com.snolred.wildplus.registry.ModItems22;
import com.snolred.wildplus.registry.ModItems23;
import com.snolred.wildplus.registry.ModItems25;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.2 엔드 시티: 셜커가 드래곤 처치 수만큼 강해지고, 시티 40% 에 금고실이 붙는다 (공허 열쇠 → 웨이브 → 상자 · 7일마다 재봉인) */
public final class EndCity {
    private static final String SEEN = "end_city_seen", VAULTS = "end_vaults", TAG = "wp_vault", SCALED = "wp_city_scaled";
    private static final int CHANCE = 40, WEEK = 7;

    private EndCity() {}

    private static int kills(ServerLevel level) {
        return Math.min(10, ContentData.get(level.getServer()).section("end_city").getIntOr("kills", 0));
    }

    @SubscribeEvent
    public static void onDragon(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.getType() != EntityTypes.ENDER_DRAGON || !(e.level() instanceof ServerLevel level)) return;
        ContentData d = ContentData.get(level.getServer());
        CompoundTag s = d.section("end_city");
        s.putInt("kills", s.getIntOr("kills", 0) + 1);
        d.dirty();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.getType() != EntityTypes.SHULKER || !(e.level() instanceof ServerLevel level) || level.dimension() != Level.END
                || e.getTags().contains(SCALED) || !(e instanceof LivingEntity l)) return;
        e.addTag(SCALED);
        AttributeInstance a = l.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        a.setBaseValue(30.0 * (1.0 + 0.2 * kills(level)));
        l.setHealth(l.getMaxHealth());
    }

    @SubscribeEvent
    public static void onBullet(LivingIncomingDamageEvent event) {
        Entity d = event.getSource().getDirectEntity();
        if (d != null && d.getType() == EntityTypes.SHULKER_BULLET && d.level() instanceof ServerLevel level) {
            event.setAmount(event.getAmount() * (1.0F + 0.1F * kills(level)));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        ServerLevel end = server.getLevel(Level.END);
        if (end == null || server.getTickCount() % 40 != 7) return;
        ContentData d = ContentData.get(server);
        tickVaults(end, d);
        if (server.getTickCount() % 120 != 7) return;
        Structure city = end.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(BuiltinStructures.END_CITY);
        for (ServerPlayer p : end.players()) {
            StructureStart start = end.structureManager().getStructureAt(p.blockPosition(), city);
            if (start.isValid()) consider(end, d, start.getBoundingBox());
        }
    }

    private static void consider(ServerLevel level, ContentData d, BoundingBox bb) {
        CompoundTag seen = d.section(SEEN);
        String key = bb.minX() + "," + bb.minZ();
        if (seen.contains(key)) return;
        boolean vault = new Random(((long) bb.minX() << 32) ^ bb.minZ() ^ level.getSeed()).nextInt(100) < CHANCE;
        BlockPos lock = new BlockPos(bb.maxX() + 16, Math.min(level.getMaxY() - 12, bb.minY() + 18), (bb.minZ() + bb.maxZ()) / 2);
        if (vault && !level.hasChunksAt(lock.offset(-24, 0, -8), lock.offset(8, 0, 8))) return; // 청크가 준비되면 다음에
        seen.putBoolean(key, vault);
        if (vault) {
            buildRoom(level, lock, bb.maxX());
            CompoundTag c = new CompoundTag();
            c.putInt("state", 0);
            d.section(VAULTS).put(Long.toString(lock.asLong()), c);
        }
        d.dirty();
    }

    private static void buildRoom(ServerLevel level, BlockPos c, int cityEdge) {
        BlockState wall = Blocks.PURPUR_BLOCK.defaultBlockState(), floor = Blocks.END_STONE_BRICKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockState brick = ModItems22.VOID_BRICK.get().getBlock().defaultBlockState();
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) for (int y = -2; y <= 6; y++) {
            boolean edge = Math.abs(x) == 7 || Math.abs(z) == 7 || y == -2 || y == 6;
            boolean pillar = Math.abs(x) == 7 && Math.abs(z) == 7;
            level.setBlock(c.offset(x, y, z), !edge ? air : pillar ? brick : y == -2 ? floor : wall, 2);
        }
        for (int[] q : new int[][] {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) level.setBlock(c.offset(q[0], 5, q[1]), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        // 시티 쪽 입구와 다리
        for (int z = -1; z <= 1; z++) for (int y = -1; y <= 1; y++) level.setBlock(c.offset(-7, y, z), air, 2);
        for (int x = c.getX() - 8; x > cityEdge - 2; x--) for (int z = -1; z <= 1; z++) level.setBlock(new BlockPos(x, c.getY() - 2, c.getZ() + z), wall, 2);
        level.setBlock(c.below(), brick, 2);
        level.setBlock(c, ModBlocks14.VAULT_LOCK.get().defaultBlockState(), 2);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getBlockState(event.getPos()).is(ModBlocks14.VAULT_LOCK.get())
                || !(event.getEntity() instanceof ServerPlayer p)) return;
        event.setCanceled(true);
        ContentData d = ContentData.get(level.getServer());
        if (!(d.section(VAULTS).get(Long.toString(event.getPos().asLong())) instanceof CompoundTag c)) return;
        if (c.getIntOr("state", 0) != 0) return;
        ItemStack hand = p.getItemInHand(event.getHand());
        if (!hand.is(ModItems23.VOID_KEY.get())) {
            p.sendOverlayMessage(Component.literal("공허 열쇠가 필요해요").withStyle(ChatFormatting.RED));
            return;
        }
        if (!p.getAbilities().instabuild) hand.shrink(1);
        BlockPos lock = event.getPos();
        int[][] at = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}, {0, -5}, {0, 5}};
        for (int[] q : at) {
            Mob m = EntityTypes.SHULKER.create(level, EntitySpawnReason.EVENT);
            if (m == null) continue;
            m.setPos(lock.getX() + q[0] + 0.5, lock.getY() - 1, lock.getZ() + q[1] + 0.5);
            m.addTag(TAG);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
        }
        c.putInt("state", 1);
        d.dirty();
        level.playSound(null, lock, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, lock.getX() + 0.5, lock.getY() + 1.0, lock.getZ() + 0.5, 120, 2.0, 1.5, 2.0, 0.2);
        for (ServerPlayer q : level.players()) if (q.blockPosition().closerThan(lock, 40)) q.sendSystemMessage(Component.literal("금고의 수호자들이 깨어났어요! 모두 처치하세요").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void tickVaults(ServerLevel level, ContentData d) {
        CompoundTag s = d.section(VAULTS);
        for (String k : new ArrayList<>(s.keySet())) {
            if (!(s.get(k) instanceof CompoundTag c)) continue;
            BlockPos lock = BlockPos.of(Long.parseLong(k));
            if (!level.isPositionEntityTicking(lock)) continue;
            int state = c.getIntOr("state", 0);
            if (state == 1) {
                List<Mob> left = level.getEntitiesOfClass(Mob.class, new AABB(lock).inflate(40.0), m -> m.getTags().contains(TAG));
                if (left.isEmpty()) {
                    reward(level, lock);
                    c.putInt("state", 2);
                    c.putLong("day", ContentData.gameDay);
                    d.dirty();
                }
            } else if (state == 2 && ContentData.gameDay >= c.getLongOr("day", 0L) + WEEK
                    && level.getBlockEntity(lock) instanceof ChestBlockEntity ch && ch.isEmpty()) {
                level.setBlock(lock, ModBlocks14.VAULT_LOCK.get().defaultBlockState(), 3);
                c.putInt("state", 0);
                d.dirty();
            }
        }
    }

    private static void reward(ServerLevel level, BlockPos lock) {
        level.setBlock(lock, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(lock) instanceof ChestBlockEntity ch)) return;
        var r = level.getRandom();
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ModItems22.VOID_SHARD.get(), 8 + r.nextInt(13)));
        out.add(new ItemStack(ModItems22.RAW_VOID_STONE.get(), 2 + r.nextInt(5)));
        out.add(new ItemStack(Items.SHULKER_SHELL, 2 + r.nextInt(5)));
        out.add(new ItemStack(Items.DIAMOND, 4 + r.nextInt(9)));
        out.add(new ItemStack(ModItems25.LORD_SHARD.get(), 1 + r.nextInt(2)));
        out.add(stone(r, 0.4F));
        if (r.nextFloat() < 0.3F) out.add(stone(r, 0.25F));
        if (r.nextFloat() < 0.2F) out.add(new ItemStack(ModItems25.STONES.get("GLIDE").get()));
        for (int i = 0; i < out.size(); i++) ch.setItem(i * 3 + 1, out.get(i));
        ch.setChanged();
        level.playSound(null, lock, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /** 각인석 하나: voidChance 확률로 공허 전용 */
    private static ItemStack stone(net.minecraft.util.RandomSource r, float voidChance) {
        String[] v = {"ABYSS", "DEVOUR", "CHAIN", "WARP", "HEART"}, c = {"HARVEST", "FURY", "GUARD", "VIGOR", "RUSH"};
        String[] pool = r.nextFloat() < voidChance ? v : c;
        return new ItemStack(ModItems25.STONES.get(pool[r.nextInt(pool.length)]).get());
    }
}
