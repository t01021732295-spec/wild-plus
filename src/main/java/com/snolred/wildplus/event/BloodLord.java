package com.snolred.wildplus.event;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModItems6;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 미니보스 "핏빛 군주".
 * 블러드문 중 서버 전체에서 정예 몬스터를 10마리 잡으면 마지막으로 잡은 사람 근처에 한 번 나타난다.
 * (명예의 전당 "보스 소환권"으로도 부를 수 있음)
 * 체력 400 · 공격력 16 · 방어 14, 보스바. 8초마다 부하 소환, 체력 절반 이하에서 광폭화, 때릴 때 흡혈 30%.
 * 보상: 군주의 심장, 핏빛 파편, 금화, 다이아몬드, 행운의 부적(50%).
 */
public final class BloodLord {
    public static final String TAG = "wildplus_blood_lord";
    public static final int KILLS_NEEDED = 10;
    private static final String RAGE_TAG = "wildplus_rage";
    private static int eliteKills;
    private static boolean spawnedThisMoon;
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private BloodLord() {}

    static void reset() {
        eliteKills = 0;
        spawnedThisMoon = false;
    }

    public static int eliteKills() {
        return eliteKills;
    }

    public static boolean isLord(Entity e) {
        return e.getPersistentData().getBooleanOr(TAG, false);
    }

    /** 블러드문 중 정예 처치 → 10마리째에 소환 */
    static void onEliteKilled(ServerLevel level, ServerPlayer killer) {
        if (spawnedThisMoon) return;
        eliteKills++;
        if (eliteKills >= KILLS_NEEDED) {
            spawnedThisMoon = true;
            spawn(level, killer);
        } else if (eliteKills >= KILLS_NEEDED - 3) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.blood_lord.near",
                    eliteKills, KILLS_NEEDED).withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    private static void set(Mob mob, Holder<Attribute> attr, double v) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a != null) a.setBaseValue(v);
    }

    /** 플레이어 근처(8~14칸)에 핏빛 군주를 소환한다 */
    public static Mob spawn(ServerLevel level, Player near) {
        RandomSource rnd = level.getRandom();
        BlockPos at = null;
        for (int i = 0; i < 12 && at == null; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, dist = 8 + rnd.nextDouble() * 6;
            int x = Mth.floor(near.getX() + Math.cos(ang) * dist), z = Mth.floor(near.getZ() + Math.sin(ang) * dist);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(top.getY() - near.getY()) <= 10 && level.getBlockState(top).isAir() && level.getBlockState(top.above()).isAir()
                    && level.getBlockState(top.above(2)).isAir()) {
                at = top;
            }
        }
        if (at == null) at = near.blockPosition().offset(3, 0, 3);
        Mob lord = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
        if (lord == null) return null;
        lord.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360F, 0F);
        lord.getPersistentData().putBoolean(TAG, true);
        set(lord, Attributes.MAX_HEALTH, 400);
        set(lord, Attributes.ATTACK_DAMAGE, 16);
        set(lord, Attributes.ARMOR, 14);
        set(lord, Attributes.ARMOR_TOUGHNESS, 6);
        set(lord, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        set(lord, Attributes.MOVEMENT_SPEED, 0.3);
        set(lord, Attributes.FOLLOW_RANGE, 48);
        set(lord, Attributes.SCALE, 1.6);
        lord.setHealth(lord.getMaxHealth());
        lord.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        lord.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.BLOODSTEEL_HELMET.get()));
        lord.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BLOODSTEEL_CHESTPLATE.get()));
        for (EquipmentSlot s : EquipmentSlot.values()) lord.setDropChance(s, 0.0F);
        lord.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        lord.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 30, 0, false, false));
        lord.setCustomName(Component.translatable("entity.wildplus.blood_lord").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        lord.setCustomNameVisible(true);
        lord.setPersistenceRequired();
        lord.setTarget(near);
        level.addFreshEntity(lord);
        level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.8F);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.blood_lord.spawn",
                near.getName()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        WildPlus.LOGGER.info("Blood Lord spawned at {}", at);
        return lord;
    }

    // ───────── 보스바 · 행동 ─────────
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isLord(event.getEntity()) && !BARS.containsKey(event.getEntity().getUUID())) {
            ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.wildplus.blood_lord")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
            BARS.put(event.getEntity().getUUID(), bar);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (BARS.isEmpty() || event.getServer().getTickCount() % 5 != 0) return;
        var it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entity ent = null;
            for (ServerLevel lv : event.getServer().getAllLevels()) {
                ent = lv.getEntity(e.getKey());
                if (ent != null) break;
            }
            ServerBossEvent bar = e.getValue();
            if (!(ent instanceof Mob lord) || !lord.isAlive()) {
                if (ent == null || !ent.isAlive()) {
                    bar.removeAllPlayers();
                    if (ent != null) it.remove();
                    else if (event.getServer().getTickCount() % 200 == 0) it.remove(); // 청크가 내려가면 정리
                }
                continue;
            }
            bar.setProgress(Mth.clamp(lord.getHealth() / lord.getMaxHealth(), 0F, 1F));
            ServerLevel level = (ServerLevel) lord.level();
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) {
                if (p.level() != level || p.distanceToSqr(lord) > 64 * 64) bar.removePlayer(p);
            }
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(lord) <= 64 * 64 && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            }
            // 광폭화
            if (lord.getHealth() < lord.getMaxHealth() * 0.5F && !lord.getPersistentData().getBooleanOr(RAGE_TAG, false)) {
                lord.getPersistentData().putBoolean(RAGE_TAG, true);
                lord.addEffect(new MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, 1));
                lord.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 1));
                bar.setColor(BossEvent.BossBarColor.PURPLE);
                level.playSound(null, lord.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.7F);
                for (ServerPlayer p : bar.getPlayers()) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.blood_lord.rage").withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            // 부하 소환 (8초마다, 근처 부하 6마리 이하)
            if (lord.tickCount % 160 < 5 && lord.getTarget() != null
                    && level.getEntitiesOfClass(Mob.class, lord.getBoundingBox().inflate(16), m -> m.getPersistentData().getBooleanOr(EliteMobs.BLOOD_TAG, false)).size() < 6) {
                for (int i = 0; i < 2; i++) {
                    Mob m = (i == 0 ? EntityTypes.ZOMBIE : EntityTypes.SKELETON).create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (m == null) continue;
                    m.snapTo(lord.getX() + (i == 0 ? 2 : -2), lord.getY(), lord.getZ() + 1, 0F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(lord.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
                    m.getPersistentData().putBoolean(EliteMobs.BLOOD_TAG, true);
                    m.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 120, 0));
                    m.setTarget(lord.getTarget());
                    level.addFreshEntity(m);
                }
                level.playSound(null, lord.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2F, 0.6F);
            }
        }
    }

    /** 군주가 때리면 30% 흡혈, 맞은 쪽에 위더 */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity src && isLord(src)) {
            src.heal(event.getHealthDamage() * 0.3F);
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 4, 0));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!isLord(e) || !(e.level() instanceof ServerLevel level)) return;
        ServerBossEvent bar = BARS.remove(e.getUUID());
        if (bar != null) bar.removeAllPlayers();
        Component who = event.getSource().getEntity() instanceof Player p ? p.getName() : Component.literal("누군가");
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.blood_lord.dead", who)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        level.playSound(null, e.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!isLord(e)) return;
        RandomSource r = e.getRandom();
        drop(event, e, new ItemStack(ModItems6.LORD_HEART.get()));
        drop(event, e, new ItemStack(ModItems.BLOOD_SHARD.get(), 10 + r.nextInt(11)));
        drop(event, e, new ItemStack(ModItems3.GOLD_COIN.get(), 5 + r.nextInt(6)));
        drop(event, e, new ItemStack(Items.DIAMOND, 3 + r.nextInt(4)));
        if (r.nextFloat() < 0.5F) drop(event, e, new ItemStack(ModItems.LUCKY_CHARM.get()));
        if (r.nextFloat() < 0.3F) drop(event, e, new ItemStack(Items.NETHERITE_SCRAP, 1 + r.nextInt(2)));
    }

    private static void drop(LivingDropsEvent event, LivingEntity e, ItemStack s) {
        ItemEntity it = new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), s);
        it.setDefaultPickUpDelay();
        event.getDrops().add(it);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        if (isLord(event.getEntity())) event.setDroppedExperience(500);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        reset();
    }
}
