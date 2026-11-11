package com.snolred.wildplus.event;

import static com.snolred.wildplus.registry.ModItems34.*;

import com.snolred.wildplus.WildPlus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.0 겁화(劫火) 장비 효과 — 공허(회피 · 순간이동)와 달리 누적 · 폭발 · 버티기.
 * 표식: 검 · 활 · 투구가 적에게 "겁화 표식"을 쌓고(8초), 5개가 되면 터진다.
 * 2부위: 화염 저항 · 힘 I · 최대 체력 +2칸 / 4부위: 저항 I · (네더에서) 신속 I · 최대 체력 +4칸 · 용암 위에 뜸
 * 투구: 3초마다 주변 6칸 적에게 표식 1 / 흉갑: 죽을 피해를 한 번 버티고 폭발 (5분) / 각반: 잃은 체력만큼 피해 최대 +30% / 장화: 달린 자리에 불길
 * 도구(곡괭이 5×5 · 도끼 · 삽 · 괭이)의 범위는 VoidGear 가 같이 처리하고, 여기서는 "바로 가방으로" 와 광석 2배만.
 */
public final class KalpaGear {
    private KalpaGear() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int MARK_TICKS = 160, MAX_MARK = 5, REVIVE_CD = 6000;

    private record Mark(int count, int until, UUID owner) {}

    private record Trail(ServerLevel level, Vec3 pos, int until, UUID owner) {}

    private static final Map<LivingEntity, Mark> MARKS = new HashMap<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static final Map<UUID, Integer> REVIVE = new HashMap<>();

    public static int pieces(LivingEntity p) {
        Item[] a = {KALPA_HELMET.get(), KALPA_CHESTPLATE.get(), KALPA_LEGGINGS.get(), KALPA_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(ARMOR[i]).is(a[i])) n++;
        return n;
    }

    public static boolean tool(ItemStack s) {
        return s.is(KALPA_PICKAXE.get()) || s.is(KALPA_AXE.get()) || s.is(KALPA_SHOVEL.get()) || s.is(KALPA_HOE.get());
    }

    private static void magic(ServerLevel level, LivingEntity e, Player owner, float amount) {
        int invul = e.getInvulnerableTime();
        e.setInvulnerableTime(0);
        e.hurtServer(level, level.damageSources().indirectMagic(e, owner), amount);
        e.setInvulnerableTime(invul);
    }

    private static void applyHealth(Player p, int n) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        var id = WildPlus.id("kalpa_health");
        AttributeModifier cur = a.getModifier(id);
        double want = n >= 4 ? 8.0 : n >= 2 ? 4.0 : 0.0;
        if (cur != null && cur.amount() == want || cur == null && want == 0) return;
        a.removeModifier(id);
        if (want > 0) a.addPermanentModifier(new AttributeModifier(id, want, AttributeModifier.Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** 표식을 하나 쌓고, 5개가 되면 터뜨린다 */
    public static void mark(ServerLevel level, LivingEntity e, ServerPlayer owner, int add) {
        if (e == owner || !e.isAlive()) return;
        int now = level.getServer().getTickCount();
        Mark m = MARKS.get(e);
        int n = (m != null && now < m.until() ? m.count() : 0) + add;
        if (n < MAX_MARK) {
            MARKS.put(e, new Mark(n, now + MARK_TICKS, owner.getUUID()));
            level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), n * 2, 0.2, 0.05, 0.2, 0.01);
            return;
        }
        MARKS.remove(e);
        burst(level, e, owner, 16.0F, 10.0F, 3.5);
    }

    /** 겁화 폭발: 가운데 적에게 center, 주변 적에게 around (방어 무시) + 불 */
    private static void burst(ServerLevel level, LivingEntity at, ServerPlayer owner, float center, float around, double radius) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, at.getBoundingBox().inflate(radius))) {
            if (e == owner || !(e == at || e instanceof Enemy)) continue;
            magic(level, e, owner, e == at ? center : around);
            e.igniteForSeconds(5);
        }
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            level.sendParticles(ParticleTypes.FLAME, at.getX() + Math.cos(a) * radius, at.getY() + 0.2, at.getZ() + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
        level.sendParticles(ParticleTypes.LAVA, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), 18, 0.8, 0.5, 0.8, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), 3, 0.7, 0.3, 0.7, 0);
        level.playSound(null, at.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.2F);
        level.playSound(null, at.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        int n = pieces(p);
        int now = level.getServer().getTickCount();
        if (p.tickCount % 20 == 0) {
            applyHealth(p, n);
            if (n >= 2) {
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false, true));
            }
            if (n >= 4) {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false, true));
                if (level.dimension() == Level.NETHER) p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
            }
        }
        if (n >= 4) {
            if (p.isInLava() && !p.isShiftKeyDown() && p.tickCount % 4 == 0) { // 용암 위에 뜸
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, Math.max(v.y, 0.28), v.z);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            }
            if (now % 6 == 0 && !p.isInvisible()) level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.9, p.getZ(), 2, 0.3, 0.5, 0.3, 0.005);
        }
        // 투구: 3초마다 주변 적에게 표식
        if (p.tickCount % 60 == 0 && p.getItemBySlot(EquipmentSlot.HEAD).is(KALPA_HELMET.get())) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.0))) {
                if (e instanceof Enemy && e.isAlive()) mark(level, e, p, 1);
            }
        }
        // 장화: 달린 자리에 불길
        if (p.tickCount % 4 == 0 && p.isSprinting() && p.onGround() && p.getItemBySlot(EquipmentSlot.FEET).is(KALPA_BOOTS.get())) {
            TRAILS.add(new Trail(level, p.position(), now + 60, p.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        if (now % 5 != 0) return;
        for (Iterator<Trail> it = TRAILS.iterator(); it.hasNext(); ) {
            Trail t = it.next();
            if (now >= t.until()) {
                it.remove();
                continue;
            }
            t.level().sendParticles(ParticleTypes.FLAME, t.pos().x, t.pos().y + 0.1, t.pos().z, 2, 0.25, 0.05, 0.25, 0.005);
            if (now % 10 != 0) continue;
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(t.owner());
            if (owner == null) continue;
            for (LivingEntity e : t.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(t.pos().add(-1.1, -0.5, -1.1), t.pos().add(1.1, 1.5, 1.1)))) {
                if (e instanceof Enemy && e.isAlive()) {
                    magic(t.level(), e, owner, 2.0F);
                    e.igniteForSeconds(3);
                }
            }
        }
        if (now % 200 == 0) MARKS.entrySet().removeIf(en -> now >= en.getValue().until() || !en.getKey().isAlive());
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // 각반: 잃은 체력만큼 피해 최대 +30%
        if (event.getSource().getEntity() instanceof ServerPlayer p && p.getItemBySlot(EquipmentSlot.LEGS).is(KALPA_LEGGINGS.get())) {
            float lost = 1.0F - p.getHealth() / Math.max(1.0F, p.getMaxHealth());
            event.setAmount(event.getAmount() * (1.0F + 0.3F * Math.max(0F, Math.min(1F, lost))));
        }
        // 흉갑: 죽을 피해를 한 번 버팀
        if (target instanceof ServerPlayer p && p.level() instanceof ServerLevel level && p.getItemBySlot(EquipmentSlot.CHEST).is(KALPA_CHESTPLATE.get())
                && event.getAmount() >= p.getHealth() && !com.snolred.wildplus.content.Arena.inMatch(p.getUUID())) {
            int now = level.getServer().getTickCount();
            if (now < REVIVE.getOrDefault(p.getUUID(), 0)) return;
            REVIVE.put(p.getUUID(), now + REVIVE_CD);
            event.setCanceled(true);
            p.setHealth(Math.max(p.getHealth(), p.getMaxHealth() * 0.4F));
            p.clearFire();
            p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 3, true, false, true));
            burst(level, p, p, 0F, 20.0F, 5.0);
            level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 0.8F);
            p.sendOverlayMessage(Component.literal("🔥 겁화 재생! (다음 재생까지 5분)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        if (event.getSource().getDirectEntity() instanceof AbstractArrow a) {
            if (!"kalpa".equals(a.getPersistentData().getStringOr(Bows.BOW_TAG, "")) || !(a.getOwner() instanceof ServerPlayer owner)) return;
            target.igniteForSeconds(5);
            if (a.isCritArrow()) burst(level, target, owner, 8.0F, 8.0F, 3.0); // 끝까지 당긴 화살: 폭발
            mark(level, target, owner, 1);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p && p.getMainHandItem().is(KALPA_SWORD.get())) {
            target.igniteForSeconds(4);
            mark(level, target, p, 1);
        }
    }

    /** 잿불 파편: 블레이즈 25% · 위더 스켈레톤 30% · 마그마 큐브 8% (플레이어가 잡았을 때) */
    @SubscribeEvent
    public static void onMobDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer) || !(e.level() instanceof ServerLevel level)) return;
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        float chance = id.equals("minecraft:blaze") ? 0.25F : id.equals("minecraft:magma_cube") ? 0.08F : id.equals("minecraft:wither_skeleton") ? 0.30F : 0F;
        if (chance > 0 && level.getRandom().nextFloat() < chance) {
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(EMBER_SHARD.get())));
        }
    }

    /** 곡괭이: 원석 자동 제련 + 광석 15% 2배 / 모든 겁화 도구: 캔 것이 바로 가방으로 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !tool(tool) || !(event.getBreaker() instanceof ServerPlayer p)) return;
        boolean pick = tool.is(KALPA_PICKAXE.get());
        boolean twice = pick && event.getState().is(Tags.Blocks.ORES) && p.getRandom().nextFloat() < 0.15F;
        for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
            ItemEntity e = it.next();
            ItemStack d = e.getItem();
            if (pick) {
                Item out = d.is(net.minecraft.world.item.Items.RAW_IRON) ? net.minecraft.world.item.Items.IRON_INGOT
                        : d.is(net.minecraft.world.item.Items.RAW_GOLD) ? net.minecraft.world.item.Items.GOLD_INGOT
                        : d.is(net.minecraft.world.item.Items.RAW_COPPER) ? net.minecraft.world.item.Items.COPPER_INGOT
                        : d.is(com.snolred.wildplus.registry.ModItems2.RAW_MYTHRIL.get()) ? com.snolred.wildplus.registry.ModItems2.MYTHRIL_INGOT.get() : null;
                if (out != null) d = new ItemStack(out, d.getCount());
                if (twice && !(d.getItem() instanceof net.minecraft.world.item.BlockItem)) d.setCount(Math.min(d.getMaxStackSize(), d.getCount() * 2));
            }
            if (p.getInventory().add(d)) it.remove();
            else e.setItem(d);
        }
    }
}
