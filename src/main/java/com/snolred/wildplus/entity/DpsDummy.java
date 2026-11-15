package com.snolred.wildplus.entity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * 5.0.10 DPS 측정 허수아비: 맞아도 죽지 않고, 최근 10초 동안 받은 피해로 DPS 를 잰다.
 * 머리 위 이름표 = 모두 합친 DPS, 때린 사람 화면 아래 = 내 DPS · 한 방 최고 · 치명타 · 누적.
 * 우클릭 = 기록 초기화, 웅크리고 우클릭 = 회수. 10초 동안 안 맞으면 기록이 저절로 지워진다.
 */
public class DpsDummy extends Mob {
    private static final int WINDOW = 200; // 10초

    private static final class Stat {
        final ArrayDeque<double[]> hits = new ArrayDeque<>(); // {tick, 피해}
        double total;
        float max;
        int crits, count;
        long first, last;
    }

    private final Map<UUID, Stat> stats = new HashMap<>();
    /** 치명타 이벤트 → 바로 뒤 피해와 짝지음 */
    private static final Map<UUID, Long> CRIT = new HashMap<>();

    public DpsDummy(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setCustomName(Component.literal("§eDPS 측정 허수아비 §7· 때려 보세요"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1024.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 0.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {}

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {}

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown()) {
            discard();
            ItemStack item = new ItemStack(com.snolred.wildplus.registry.ModItems35.DPS_DUMMY.get());
            if (!sp.getInventory().add(item)) spawnAtLocation((ServerLevel) level(), item);
            sp.sendOverlayMessage(Component.literal("§a허수아비를 회수했어요"));
        } else {
            stats.clear();
            updateName();
            level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
            sp.sendOverlayMessage(Component.literal("§e기록 초기화 · §7웅크리고 우클릭하면 회수"));
        }
        return InteractionResult.SUCCESS;
    }

    private void record(ServerPlayer p, float dmg, boolean crit) {
        long now = level().getGameTime();
        Stat s = stats.computeIfAbsent(p.getUUID(), k -> new Stat());
        if (s.count == 0) s.first = now;
        s.hits.addLast(new double[] {now, dmg});
        s.total += dmg;
        s.max = Math.max(s.max, dmg);
        s.count++;
        if (crit) s.crits++;
        s.last = now;
        setHealth(getMaxHealth());
        show(p, s, now);
        updateName();
    }

    private static double dps(Stat s, long now) {
        double sum = 0;
        for (double[] h : s.hits) sum += h[1];
        double secs = Math.max(1.0, Math.min(WINDOW, now - s.first) / 20.0);
        return sum / secs;
    }

    private static String f(double v) {
        return v >= 1000 ? String.format("%,.0f", v) : String.format("%.1f", v);
    }

    private void show(ServerPlayer p, Stat s, long now) {
        long secs = (now - s.first) / 20;
        p.sendOverlayMessage(Component.literal("§6내 DPS §f" + f(dps(s, now)) + " §7· 한 방 최고 §f" + f(s.max) + " §7· 치명타 §f" + s.crits
                + "/" + s.count + "회 §7· 누적 §f" + f(s.total) + " §7(" + secs + "초)"));
    }

    private void updateName() {
        long now = level().getGameTime();
        if (stats.isEmpty()) {
            setCustomName(Component.literal("§eDPS 측정 허수아비 §7· 때려 보세요"));
            return;
        }
        double sum = 0;
        for (Stat s : stats.values()) sum += dps(s, now);
        setCustomName(Component.literal("§c⚔ §fDPS §e" + f(sum) + (stats.size() > 1 ? " §7(" + stats.size() + "명)" : "")));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 10 != 0) return;
        long now = level().getGameTime();
        boolean changed = false;
        for (Iterator<Map.Entry<UUID, Stat>> it = stats.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Stat s = e.getValue();
            while (!s.hits.isEmpty() && now - s.hits.peekFirst()[0] > WINDOW) s.hits.removeFirst();
            if (now - s.last > WINDOW) {
                it.remove();
                changed = true;
                if (level().getPlayerByUUID(e.getKey()) instanceof ServerPlayer p)
                    p.sendOverlayMessage(Component.literal("§7허수아비 기록 끝 · 누적 " + f(s.total) + " · 한 방 최고 " + f(s.max)));
                continue;
            }
            if (level().getPlayerByUUID(e.getKey()) instanceof ServerPlayer p && p.distanceToSqr(this) < 48 * 48) show(p, s, now);
            changed = true;
        }
        if (changed) updateName();
        if (getHealth() < getMaxHealth()) setHealth(getMaxHealth());
    }

    // ───────── 이벤트 ─────────
    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        if (event.getTarget() instanceof DpsDummy && event.isCriticalHit()) CRIT.put(event.getEntity().getUUID(), event.getEntity().level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof DpsDummy d)) return;
        float dmg = event.getNewDamage();
        if (event.getSource().getEntity() instanceof ServerPlayer p && dmg > 0) {
            Long t = CRIT.remove(p.getUUID());
            d.record(p, dmg, t != null && d.level().getGameTime() - t <= 1);
        } else {
            d.setHealth(d.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof DpsDummy d && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            d.setHealth(d.getMaxHealth());
        }
    }
}
