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
    private static final Map<UUID, Integer> MARKS = new HashMap<>();
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
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30 + now * 3);
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
        s.leapReady = now + LEAP_CD;
        s.surgeUntil = now + SURGE;
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
        p.sendOverlayMessage(Component.literal("공허 도약! 3초 안의 첫 공격 피해 2배").withStyle(ChatFormatting.LIGHT_PURPLE));
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
        p.sendOverlayMessage(Component.literal("공허 생환! (다음 사용까지 3분)").withStyle(ChatFormatting.LIGHT_PURPLE));
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
        if (target.level() instanceof ServerLevel lv && lv.getServer().getTickCount() < MARKS.getOrDefault(target.getUUID(), 0)) {
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
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 0.5, target.getZ(), 40, 1.2, 0.5, 1.2, 0.1);
                level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_HURT, SoundSource.PLAYERS, 0.5F, 1.4F);
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
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0))) {
            if (e != p && (e == target || e instanceof Enemy)) magic(level, e, p, 12.0F);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 3, 1.0, 0.2, 1.0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30, 1.0, 0.4, 1.0, 0.1);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_HURT, SoundSource.PLAYERS, 0.5F, 1.7F);
    }

    /** 균열 표식: 5초간 받는 피해 +15% (보스 +8%) */
    private static void mark(ServerLevel level, LivingEntity e, Player owner) {
        if (e == owner) return;
        int now = level.getServer().getTickCount();
        MARKS.put(e.getUUID(), now + MARK_TICKS);
        if (MARKS.size() > 256) MARKS.values().removeIf(t -> t <= now);
        level.sendParticles(ParticleTypes.WITCH, e.getX(), e.getY() + e.getBbHeight() * 0.8, e.getZ(), 4, 0.25, 0.25, 0.25, 0.01);
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
        if (tool.is(VOID_PICKAXE.get()) && p.getPersistentData().getBooleanOr(ENDER_DROP, false)) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemEntity e = it.next();
                ItemStack left = p.getEnderChestInventory().addItem(e.getItem().copy());
                if (left.isEmpty()) it.remove();
                else e.setItem(left);
            }
        } else if (tool.is(VOID_HOE.get()) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
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
        if (REPLANT.isEmpty()) return;
        for (Replant r : REPLANT) if (r.level().getBlockState(r.pos()).isAir()) r.level().setBlockAndUpdate(r.pos(), r.state());
        REPLANT.clear();
    }

    /** 삽 · 괭이 5×5 (웅크리면 한 칸) */
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (areaBreaking || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        BlockPos c = event.getPos();
        boolean shovel = tool.is(VOID_SHOVEL.get()) && state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        boolean hoe = tool.is(VOID_HOE.get()) && state.getBlock() instanceof CropBlock cb && cb.isMaxAge(state);
        if (!shovel && !hoe) return;
        Direction.Axis axis = hoe || Math.abs(player.getXRot()) > 45 ? Direction.Axis.Y : player.getDirection().getAxis();
        areaBreaking = true;
        try {
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos p = switch (axis) {
                        case Y -> c.offset(a, 0, b);
                        case X -> c.offset(0, a, b);
                        default -> c.offset(a, b, 0);
                    };
                    BlockState s = level.getBlockState(p);
                    boolean ok = shovel ? s.is(BlockTags.MINEABLE_WITH_SHOVEL) && s.getDestroySpeed(level, p) >= 0
                            : s.getBlock() instanceof CropBlock cr && cr.isMaxAge(s);
                    if (ok) player.gameMode.destroyBlock(p);
                }
            }
        } finally {
            areaBreaking = false;
        }
    }
}
