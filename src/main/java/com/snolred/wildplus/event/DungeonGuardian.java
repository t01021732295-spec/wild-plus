package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 던전 보스 "고대 수호자": 고대 지하 묘지 맨 아래 투기장에 있다 (구조물 NBT 의 wildplus_dungeon_boss 표시).
 * 체력 300 · 공격 14 · 방어 14, 보스바. 10초마다 땅을 내리쳐 주변을 밀쳐 내고 구속, 체력 절반 이하에서 해골 호위병 소환.
 * 보상: 금화 · 다이아몬드 · 네더라이트 주괴 · 마법 부여 책 · 행운의 부적.
 */
public final class DungeonGuardian {
    public static final String TAG = "wildplus_dungeon_boss";
    private static final String READY = "wildplus_dungeon_boss_ready", CALLED = "wildplus_dungeon_boss_called", HOME = "wildplus_dungeon_boss_home",
            TIER = "wildplus_dungeon_tier";
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private DungeonGuardian() {}

    public static boolean isBoss(Entity e) {
        return e.getPersistentData().getBooleanOr(TAG, false);
    }

    private static void set(Mob mob, Holder<Attribute> attr, double v) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a != null) a.setBaseValue(v);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob boss) || !isBoss(boss)) return;
        var data = boss.getPersistentData();
        if (!data.getBooleanOr(READY, false)) {
            data.putBoolean(READY, true);
            data.putLong(HOME, boss.blockPosition().asLong());
            int tier = Math.max(1, data.getIntOr(TIER, 1));
            set(boss, Attributes.MAX_HEALTH, 300 * (1.0 + 0.35 * (tier - 1)));
            set(boss, Attributes.ATTACK_DAMAGE, 14 * (1.0 + 0.15 * (tier - 1)));
            set(boss, Attributes.ARMOR, 14);
            set(boss, Attributes.ARMOR_TOUGHNESS, 6);
            set(boss, Attributes.KNOCKBACK_RESISTANCE, 1.0);
            set(boss, Attributes.FOLLOW_RANGE, 32);
            set(boss, Attributes.SCALE, 1.5);
            boss.setHealth(boss.getMaxHealth());
            boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            for (EquipmentSlot s : EquipmentSlot.values()) boss.setDropChance(s, 0.0F);
            boss.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            boss.setCustomName(Component.translatable("entity.wildplus.dungeon_guardian").append(tier > 1 ? " · " + tier + "단계" : "")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            boss.setCustomNameVisible(true);
            boss.setPersistenceRequired();
        }
        BARS.computeIfAbsent(boss.getUUID(), id -> new ServerBossEvent(UUID.randomUUID(),
                Component.translatable("entity.wildplus.dungeon_guardian").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_12));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (BARS.isEmpty() || event.getServer().getTickCount() % 10 != 0) return;
        var it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entity ent = null;
            for (ServerLevel lv : event.getServer().getAllLevels()) {
                ent = lv.getEntity(e.getKey());
                if (ent != null) break;
            }
            ServerBossEvent bar = e.getValue();
            if (!(ent instanceof Mob boss) || !boss.isAlive()) {
                bar.removeAllPlayers();
                if (ent != null || event.getServer().getTickCount() % 400 == 0) it.remove();
                continue;
            }
            ServerLevel level = (ServerLevel) boss.level();
            bar.setProgress(Mth.clamp(boss.getHealth() / boss.getMaxHealth(), 0F, 1F));
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) {
                if (p.level() != level || p.distanceToSqr(boss) > 40 * 40) bar.removePlayer(p);
            }
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(boss) <= 40 * 40 && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            }
            // 땅 내리치기: 10초마다 주변 6칸 플레이어를 밀쳐 내고 구속
            if (boss.getTarget() != null && boss.tickCount % 200 < 10) {
                level.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0F, 0.6F);
                for (Player p : level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(6))) {
                    double dx = p.getX() - boss.getX(), dz = p.getZ() - boss.getZ();
                    double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                    p.push(dx / len * 1.4, 0.5, dz / len * 1.4);
                    if (p instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                }
            }
            // 체력 절반 이하: 해골 호위병 4마리 (한 번)
            if (boss.getHealth() < boss.getMaxHealth() * 0.5F && !boss.getPersistentData().getBooleanOr(CALLED, false)) {
                boss.getPersistentData().putBoolean(CALLED, true);
                for (int i = 0; i < 4; i++) {
                    Mob m = net.minecraft.world.entity.EntityTypes.SKELETON.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                    if (m == null) continue;
                    m.snapTo(boss.getX() + (i % 2 == 0 ? 2 : -2), boss.getY(), boss.getZ() + (i < 2 ? 2 : -2), 0F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
                    if (boss.getTarget() != null) m.setTarget(boss.getTarget());
                    level.addFreshEntity(m);
                }
                for (ServerPlayer p : bar.getPlayers()) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.dungeon_guardian.call").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!isBoss(e) || !(e.level() instanceof ServerLevel level)) return;
        ServerBossEvent bar = BARS.remove(e.getUUID());
        if (bar != null) bar.removeAllPlayers();
        BlockPos home = BlockPos.of(e.getPersistentData().getLongOr(HOME, e.blockPosition().asLong()));
        List<BlockPos> gates = new java.util.ArrayList<>(openVault(level, home));
        gates.addAll(openVault(level, e.blockPosition()));
        remember(level, home, Math.max(1, e.getPersistentData().getIntOr(TIER, 1)), gates);
        Component who = event.getSource().getEntity() instanceof Player p ? p.getName() : Component.literal("누군가");
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.dungeon_guardian.dead", who)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    /** 보스 주변의 봉인문(gate=true)을 없앤다 */
    private static List<BlockPos> openVault(ServerLevel level, BlockPos center) {
        List<BlockPos> out = new java.util.ArrayList<>();
        int opened = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-16, -6, -16), center.offset(16, 8, 16))) {
            var st = level.getBlockState(p);
            if (st.is(com.snolred.wildplus.registry.ModBlocks2.DUNGEON_SEAL.get()) && st.getValue(com.snolred.wildplus.block.SealBlock.GATE)) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                out.add(p.immutable());
                opened++;
            }
        }
        if (opened > 0) level.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.8F, 1.2F);
        return out;
    }

    // ───────── 던전 단계: 쓰러뜨린 다음 주(게임 속 7일)에 더 강해져서 다시 나타나고, 창고가 다시 봉인·채워진다 ─────────
    private static void remember(ServerLevel level, BlockPos home, int tier, List<BlockPos> gates) {
        var d = com.snolred.wildplus.content.ContentData.get(level.getServer());
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("tier", Math.min(10, tier + 1));
        t.putLong("week", com.snolred.wildplus.content.ContentData.week());
        t.putLongArray("gates", gates.stream().mapToLong(BlockPos::asLong).toArray());
        d.section("dungeons").put(String.valueOf(home.asLong()), t);
        d.dirty();
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("다음 주(게임 속 7일 뒤)에 이 던전의 수호자가 "
                + Math.min(10, tier + 1) + "단계로 돌아오고 보물 창고가 다시 채워져요").withStyle(ChatFormatting.GRAY), false);
    }

    @SubscribeEvent
    public static void onRespawnTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 200 != 37) return;
        var d = com.snolred.wildplus.content.ContentData.get(event.getServer());
        net.minecraft.nbt.CompoundTag all = d.section("dungeons");
        long week = com.snolred.wildplus.content.ContentData.week();
        ServerLevel level = event.getServer().overworld();
        for (String key : new java.util.ArrayList<>(all.keySet())) {
            net.minecraft.nbt.CompoundTag t = all.getCompoundOrEmpty(key);
            if (t.getLongOr("week", week) >= week) continue;
            BlockPos home = BlockPos.of(Long.parseLong(key));
            if (!level.isLoaded(home) || level.getNearestPlayer(home.getX(), home.getY(), home.getZ(), 64, false) == null) continue;
            int tier = t.getIntOr("tier", 2);
            // 봉인문 다시 닫기
            for (long g : t.getLongArray("gates").orElse(new long[0])) {
                level.setBlock(BlockPos.of(g), com.snolred.wildplus.registry.ModBlocks2.DUNGEON_SEAL.get().defaultBlockState()
                        .setValue(com.snolred.wildplus.block.SealBlock.GATE, true), 3);
            }
            // 창고 상자 다시 채우기 (보스 자리에서 16칸 안의 상자)
            for (BlockPos p : BlockPos.betweenClosed(home.offset(-16, -4, -4), home.offset(16, 6, 16))) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    chest.clearContent();
                    var cs = level.getBlockState(p);
                    boolean vault = cs.hasProperty(net.minecraft.world.level.block.ChestBlock.FACING)
                            && cs.getValue(net.minecraft.world.level.block.ChestBlock.FACING) == net.minecraft.core.Direction.NORTH;
                    chest.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                            com.snolred.wildplus.WildPlus.id(vault ? "chests/dungeon_vault" : "chests/dungeon")), level.getRandom().nextLong());
                }
            }
            Mob boss = net.minecraft.world.entity.EntityTypes.WITHER_SKELETON.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (boss != null) {
                boss.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 180F, 0F);
                boss.getPersistentData().putBoolean(TAG, true);
                boss.getPersistentData().putInt(TIER, tier);
                level.addFreshEntity(boss);
            }
            all.remove(key);
            d.dirty();
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("☠ 고대 수호자가 " + tier + "단계로 되살아났어요! 보물 창고가 다시 봉인됐어요")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!isBoss(e)) return;
        RandomSource r = e.getRandom();
        int tier = Math.max(1, e.getPersistentData().getIntOr(TIER, 1));
        drop(event, e, new ItemStack(ModItems3.GOLD_COIN.get(), (10 + r.nextInt(11)) * (2 + tier) / 3));
        if (tier >= 2) drop(event, e, new ItemStack(com.snolred.wildplus.registry.ModItems7.RUBY.get(), tier));
        drop(event, e, new ItemStack(Items.DIAMOND, 4 + r.nextInt(5)));
        drop(event, e, new ItemStack(Items.NETHERITE_INGOT));
        drop(event, e, new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + r.nextInt(9)));
        if (r.nextFloat() < 0.6F) drop(event, e, new ItemStack(ModItems.LUCKY_CHARM.get()));
        if (r.nextFloat() < 0.25F) drop(event, e, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
    }

    private static void drop(LivingDropsEvent event, LivingEntity e, ItemStack s) {
        ItemEntity it = new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), s);
        it.setDefaultPickUpDelay();
        event.getDrops().add(it);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        if (isBoss(event.getEntity())) event.setDroppedExperience(400);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
    }
}
