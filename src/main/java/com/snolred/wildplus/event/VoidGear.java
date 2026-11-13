package com.snolred.wildplus.event;

import static com.snolred.wildplus.registry.ModItems22.*;

import com.snolred.wildplus.WildPlus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.0 공허 장비 효과. 2부위 · 4부위 세트, 공허 도약 · 생환, 무기 고유 능력, 4부위 아우라. */
public final class VoidGear {
    private VoidGear() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String ENDER_DROP = "wildplus_void_ender";
    private static final int LEAP_CD = 160, RESCUE_CD = 3600, SURGE = 60;

    private static final class State {
        boolean sneak, arrowRest;
        int leapReady, rescueReady, surgeUntil, hits;
        Vec3 safe;
    }

    private record Replant(ServerLevel level, BlockPos pos, BlockState state) {}

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<LivingEntity, Integer> MARKS = new HashMap<>();
    private static final int MARK_TICKS = 100;
    private static final List<Replant> REPLANT = new ArrayList<>();
    private static boolean areaBreaking;

    private static State state(Player p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    public static int pieces(LivingEntity p) {
        Item[] a = {VOID_HELMET.get(), VOID_CHESTPLATE.get(), VOID_LEGGINGS.get(), VOID_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(ARMOR[i]).is(a[i])) n++;
        return n;
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
        var id = WildPlus.id("void_health");
        AttributeModifier cur = a.getModifier(id);
        double want = n >= 4 ? 8.0 : n >= 2 ? 4.0 : 0.0;
        if (cur != null && cur.amount() == want || cur == null && want == 0) return;
        a.removeModifier(id);
        if (want > 0) a.addPermanentModifier(new AttributeModifier(id, want, AttributeModifier.Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        int now = level.getServer().getTickCount();
        int n = pieces(p);
        if (p.tickCount % 20 == 0) {
            applyHealth(p, n);
            if (n >= 2) {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, n >= 4 ? 1 : 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false, true));
            }
            if (n >= 4) p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
        }
        State s = state(p);
        boolean sneak = p.isShiftKeyDown();
        if (n >= 4) {
            if (!p.isInvisible()) aura(level, p, now);
            boolean arena = com.snolred.wildplus.content.Arena.inMatch(p.getUUID());
            if (sneak && !s.sneak && !arena) leap(level, p, s, now);
            if (p.onGround() && p.tickCount % 10 == 0) s.safe = p.position();
            if (!arena && p.getY() < level.getMinY() - 1 && s.safe != null && now >= s.rescueReady) rescue(level, p, s, now);
        }
        s.sneak = sneak;
    }

    private static void aura(ServerLevel level, ServerPlayer p, int now) {
        if (now % 5 == 0) level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.35, 0.55, 0.35, 0.15);
        if (now % 10 == 0 && p.onGround()) {
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60 + now * 3);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + Math.cos(a) * 1.2, p.getY() + 0.1, p.getZ() + Math.sin(a) * 1.2, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void leap(ServerLevel level, ServerPlayer p, State s, int now) {
        if (now < s.leapReady || p.onGround() || p.isFallFlying() || p.getAbilities().flying || p.isPassenger() || p.isInWater()) return;
        Vec3 look = p.getLookAngle();
        Vec3 from = p.position();
        double best = 0;
        for (double d = 0.5; d <= 8.0; d += 0.5) {
            if (!level.noCollision(p, p.getBoundingBox().move(look.scale(d)))) break;
            best = d;
        }
        if (best < 1.5) return;
        Vec3 to = from.add(look.scale(best));
        for (double d = 0; d < best; d += 0.5) {
            Vec3 q = from.add(look.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 1.0, q.z, 1, 0.05, 0.2, 0.05, 0.0);
        }
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 40, 0.3, 0.6, 0.3, 0.4);
        p.teleportTo(to.x, to.y, to.z);
        p.resetFallDistance();
        s.leapReady = now + (Engraves.has(p, "WARP") ? LEAP_CD * 3 / 5 : LEAP_CD);
        s.surgeUntil = now + SURGE;
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    private static void rescue(ServerLevel level, ServerPlayer p, State s, int now) {
        p.teleportTo(s.safe.x, s.safe.y, s.safe.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 4, true, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
        s.rescueReady = now + RESCUE_CD;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 0.2, p.getZ(), 60, 1.0, 0.1, 1.0, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    @SubscribeEvent
    public static void onEndermanTarget(LivingChangeTargetEvent event) {
        LivingEntity e = event.getEntity();
        if (e.getType() == net.minecraft.world.entity.EntityTypes.ENDERMAN && event.getNewAboutToBeSetTarget() instanceof Player p
                && pieces(p) >= 2 && e.getLastHurtByMob() != p) event.setCanceled(true); // 먼저 때리면 반격은 함
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p) {
            ItemStack hand = p.getMainHandItem();
            if (hand.is(VOID_AXE.get()) && target.getHealth() <= target.getMaxHealth() * 0.3F) event.setAmount(event.getAmount() * 2.0F);
            State s = state(p);
            int now = p.level().getServer().getTickCount();
            if (now < s.surgeUntil) {
                s.surgeUntil = 0;
                event.setAmount(event.getAmount() * 2.0F);
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 16, 0.3, 0.4, 0.3, 0.08);
            }
        }
        if (target instanceof Player p && pieces(p) >= 4) event.setAmount(event.getAmount() * 0.9F);
        if (target.level() instanceof ServerLevel lv && lv.getServer().getTickCount() < MARKS.getOrDefault(target, 0)) {
            event.setAmount(event.getAmount() * (target instanceof Mob m && FrostBurst.boss(m) ? 1.08F : 1.15F));
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        if (event.getSource().getDirectEntity() instanceof AbstractArrow a) {
            if (!"void".equals(a.getPersistentData().getStringOr(Bows.BOW_TAG, "")) || !(a.getOwner() instanceof ServerPlayer owner)) return;
            mark(level, target, owner);
            if (!(target instanceof Mob m && FrostBurst.boss(m))) target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1));
            State st = state(owner);
            st.arrowRest = !st.arrowRest;
            if (st.arrowRest) { // 공허 화살: 한 번 터지고 다음 한 번은 쉼
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0))) {
                    if (e != owner && (e == target || e instanceof Enemy)) magic(level, e, owner, 8.0F);
                }
                burstFx(level, target, 3.0, ParticleTypes.REVERSE_PORTAL);
                if ("CHAIN".equals(RubyGear.engraving(owner.getMainHandItem())) || "CHAIN".equals(RubyGear.engraving(owner.getOffhandItem()))) {
                    LivingEntity next = null;
                    double bd = Double.MAX_VALUE;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(8.0))) {
                        double dd = e.distanceToSqr(target);
                        if (e != owner && e != target && e instanceof Enemy && dd > 9.0 && dd < bd) { bd = dd; next = e; }
                    }
                    if (next != null) {
                        magic(level, next, owner, 8.0F);
                        burstFx(level, next, 2.0, ParticleTypes.REVERSE_PORTAL);
                    }
                }
                level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 2, 0.6, 0.3, 0.6, 0);
                level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45F, 1.8F);
            }
            // 귀환: 플레이어가 주울 수 있는 화살만 (무한 · 다중 발사 복사본 제외), 종류와 효과 그대로
            if (a.pickup == AbstractArrow.Pickup.ALLOWED) {
                a.pickup = AbstractArrow.Pickup.DISALLOWED;
                ItemStack back = a.getPickupItemStackOrigin().copyWithCount(1);
                if (!owner.getInventory().add(back)) owner.spawnAtLocation(level, back);
            }
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p || !p.getMainHandItem().is(VOID_SWORD.get())) return;
        mark(level, target, p);
        State s = state(p);
        if (++s.hits < 3) return;
        s.hits = 0;
        boolean abyss = "ABYSS".equals(RubyGear.engraving(p.getMainHandItem()));
        double slashR = abyss ? 5.0 : 3.0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(slashR))) {
            if (e != p && (e == target || e instanceof Enemy)) magic(level, e, p, abyss ? 15.6F : 12.0F);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 1.4, 0.3, 1.4, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1, 0, 0, 0, 0);
        burstFx(level, target, slashR, ParticleTypes.END_ROD);
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6F, 1.5F);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** 바닥 고리 두 겹 + 위로 터지는 입자: 범위가 눈에 보이게 */
    private static void burstFx(ServerLevel level, LivingEntity at, double radius, net.minecraft.core.particles.ParticleOptions ring) {
        double y = at.getY() + 0.2;
        for (double r : new double[] {radius * 0.55, radius}) {
            int n = (int) (r * 14);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                level.sendParticles(ring, at.getX() + Math.cos(a) * r, y, at.getZ() + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), 50, 1.0, 0.9, 1.0, 0.35);
        level.sendParticles(ParticleTypes.PORTAL, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), 90, 0.5, 0.7, 0.5, 1.2);
    }

    /** 이 적에게 균열 표식이 걸려 있나 */
    public static boolean marked(LivingEntity e) {
        return e.level() instanceof ServerLevel lv && lv.getServer().getTickCount() < MARKS.getOrDefault(e, 0);
    }

    /** 균열 표식: 5초간 받는 피해 +15% (보스 +8%) */
    private static void mark(ServerLevel level, LivingEntity e, Player owner) {
        if (e == owner) return;
        int now = level.getServer().getTickCount();
        boolean fresh = MARKS.getOrDefault(e, 0) <= now;
        MARKS.put(e, now + MARK_TICKS);
        if (fresh) {
            level.sendParticles(ParticleTypes.WITCH, e.getX(), e.getY() + e.getBbHeight() * 0.7, e.getZ(), 30, 0.4, 0.5, 0.4, 0.15);
            level.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p
                || !p.getMainHandItem().is(VOID_AXE.get()) || !(dead.level() instanceof ServerLevel level)) return;
        float amount = Math.min(30.0F, dead.getMaxHealth() * 0.2F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, dead.getBoundingBox().inflate(4.0))) {
            if (e != p && e != dead && e instanceof Enemy) magic(level, e, p, amount);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, dead.getX(), dead.getY() + 1.0, dead.getZ(), 6, 1.6, 0.3, 1.6, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, dead.getX(), dead.getY() + 1.0, dead.getZ(), 40, 1.5, 0.4, 1.5, 0.1);
    }

    /** 화살이 벽 1겹(1.25칸까지)을 한 번 뚫음 */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow a) || !(a.level() instanceof ServerLevel level)) return;
        var data = a.getPersistentData();
        HitResult hit = event.getRayTraceResult();
        if (!"void".equals(data.getStringOr(Bows.BOW_TAG, "")) || !(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK
                || data.getBooleanOr("wildplus_void_pierced", false)) return;
        Vec3 dir = a.getDeltaMovement().normalize();
        for (double d = 0.25; d <= 1.25; d += 0.25) {
            Vec3 q = hit.getLocation().add(dir.scale(d));
            BlockPos pos = BlockPos.containing(q);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                data.putBoolean("wildplus_void_pierced", true);
                a.setPos(q.x, q.y, q.z);
                level.sendParticles(ParticleTypes.PORTAL, q.x, q.y, q.z, 12, 0.15, 0.15, 0.15, 0.1);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !event.getItemStack().is(VOID_PICKAXE.get())) return;
        boolean on = !p.getPersistentData().getBooleanOr(ENDER_DROP, false);
        p.getPersistentData().putBoolean(ENDER_DROP, on);
        p.sendOverlayMessage(Component.literal(on ? "엔더 상자로 바로 보내기: 켜짐" : "엔더 상자로 바로 보내기: 꺼짐").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(event.getBreaker() instanceof ServerPlayer p)) return;
        BlockState state = event.getState();
        if (tool.is(VOID_PICKAXE.get())) { // 원석 자동 제련
            for (ItemEntity e : event.getDrops()) {
                ItemStack d = e.getItem();
                net.minecraft.world.item.Item out = d.is(net.minecraft.world.item.Items.RAW_IRON) ? net.minecraft.world.item.Items.IRON_INGOT
                        : d.is(net.minecraft.world.item.Items.RAW_GOLD) ? net.minecraft.world.item.Items.GOLD_INGOT
                        : d.is(net.minecraft.world.item.Items.RAW_COPPER) ? net.minecraft.world.item.Items.COPPER_INGOT
                        : d.is(com.snolred.wildplus.registry.ModItems2.RAW_MYTHRIL.get()) ? com.snolred.wildplus.registry.ModItems2.MYTHRIL_INGOT.get() : null;
                if (out != null) e.setItem(new ItemStack(out, d.getCount()));
            }
        }
        if (tool.is(VOID_PICKAXE.get()) && p.getPersistentData().getBooleanOr(ENDER_DROP, false)) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemEntity e = it.next();
                ItemStack left = p.getEnderChestInventory().addItem(e.getItem().copy());
                if (left.isEmpty()) it.remove();
                else e.setItem(left);
            }
        } else if ((tool.is(VOID_HOE.get()) || tool.is(com.snolred.wildplus.registry.ModItems34.KALPA_HOE.get())) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            Item seed = crop.asItem();
            for (ItemEntity e : event.getDrops()) {
                if (e.getItem().is(seed)) {
                    e.getItem().shrink(1);
                    REPLANT.add(new Replant(event.getLevel(), event.getPos().immutable(), crop.getStateForAge(0)));
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        if (!MARKS.isEmpty()) {
            MARKS.entrySet().removeIf(en -> en.getValue() <= tick || !en.getKey().isAlive());
            if (tick % 3 == 0) MARKS.keySet().forEach(e -> markFx(e, tick));
        }
        if (REPLANT.isEmpty()) return;
        for (Replant r : REPLANT) if (r.level().getBlockState(r.pos()).isAir()) r.level().setBlockAndUpdate(r.pos(), r.state());
        REPLANT.clear();
    }

    /** 표식이 있는 동안 머리 위에서 도는 보라색 고리 */
    private static void markFx(LivingEntity e, int tick) {
        if (!(e.level() instanceof ServerLevel level)) return;
        double y = e.getY() + e.getBbHeight() + 0.45, r = Math.max(0.55, e.getBbWidth() * 0.75);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + tick * 0.18;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX() + Math.cos(a) * r, y, e.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), y, e.getZ(), 1, 0.1, 0.05, 0.1, 0);
    }

    /** 공허 도끼: 잎이 붙은 진짜 나무만 통째로 (웅크리면 한 칸) */
    private static void fellTree(ServerLevel level, ServerPlayer player, BlockPos start) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.LinkedHashSet<BlockPos> logs = new java.util.LinkedHashSet<>();
        queue.add(start);
        logs.add(start);
        boolean leaves = false;
        while (!queue.isEmpty() && logs.size() < 160) {
            BlockPos q = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(q.offset(-1, -1, -1), q.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(n);
                if (s.is(BlockTags.LEAVES)) leaves = true;
                else if (s.is(BlockTags.LOGS) && logs.add(n.immutable())) queue.add(n.immutable());
            }
        }
        if (!leaves) return;
        areaBreaking = true;
        try {
            for (BlockPos q : logs) if (!q.equals(start)) player.gameMode.destroyBlock(q);
        } finally {
            areaBreaking = false;
        }
    }

    /** 공허 삽: 흙 · 모래류 채굴 속도 3배 */
    @SubscribeEvent
    public static void onBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if ((event.getEntity().getMainHandItem().is(VOID_SHOVEL.get()) || event.getEntity().getMainHandItem().is(com.snolred.wildplus.registry.ModItems34.KALPA_SHOVEL.get())) && event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL)) event.setNewSpeed(event.getNewSpeed() * 3.0F);
    }

    /** 곡괭이 3×3 · 삽 5×5 · 괭이 7×7 (웅크리면 한 칸) */
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (areaBreaking || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        BlockPos c = event.getPos();
        boolean kalpa = KalpaGear.tool(tool);
        if ((tool.is(VOID_AXE.get()) || tool.is(com.snolred.wildplus.registry.ModItems34.KALPA_AXE.get())) && state.is(BlockTags.LOGS)) {
            fellTree(level, player, c);
            return;
        }
        boolean shovel = (tool.is(VOID_SHOVEL.get()) || tool.is(com.snolred.wildplus.registry.ModItems34.KALPA_SHOVEL.get())) && state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        boolean hoe = (tool.is(VOID_HOE.get()) || tool.is(com.snolred.wildplus.registry.ModItems34.KALPA_HOE.get())) && state.getBlock() instanceof CropBlock cb && cb.isMaxAge(state);
        boolean pick = (tool.is(VOID_PICKAXE.get()) || tool.is(com.snolred.wildplus.registry.ModItems34.KALPA_PICKAXE.get())) && state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        if (!shovel && !hoe && !pick) return;
        int reach = pick ? (kalpa ? 2 : 1) : hoe ? 3 : 2;
        Direction.Axis axis = hoe || Math.abs(player.getXRot()) > 45 ? Direction.Axis.Y : player.getDirection().getAxis();
        areaBreaking = true;
        try {
            for (int a = -reach; a <= reach; a++) {
                for (int b = -reach; b <= reach; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos p = switch (axis) {
                        case Y -> c.offset(a, 0, b);
                        case X -> c.offset(0, a, b);
                        default -> c.offset(a, b, 0);
                    };
                    BlockState s = level.getBlockState(p);
                    boolean ok = pick ? s.is(BlockTags.MINEABLE_WITH_PICKAXE) && s.getDestroySpeed(level, p) >= 0 && level.getBlockEntity(p) == null && tool.isCorrectToolForDrops(s)
                            : shovel ? s.is(BlockTags.MINEABLE_WITH_SHOVEL) && s.getDestroySpeed(level, p) >= 0
                            : s.getBlock() instanceof CropBlock cr && cr.isMaxAge(s);
                    if (ok) player.gameMode.destroyBlock(p);
                }
            }
        } finally {
            areaBreaking = false;
        }
    }
}
