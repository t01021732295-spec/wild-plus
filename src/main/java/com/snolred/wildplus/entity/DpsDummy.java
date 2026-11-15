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
 * 우클릭 = 기록 초기화 (갑옷을 들고 있으면 입히기 — 인챈트까지 적용), 웅크리고 우클릭 = 갑옷 벗기기 → 회수. 10초 동안 안 맞으면 기록이 저절로 지워진다.
 */
public class DpsDummy extends Mob {
    private static final int WINDOW = 200; // 10초

    private static final class Stat {
        final ArrayDeque<double[]> hits = new ArrayDeque<>(); // {tick, 피해}
        double total;
        float max, lastDmg;
        int crits, count;
        long first, last;
    }

    private final Map<UUID, Stat> stats = new HashMap<>();
    /** 5.1.5 마지막으로 받은 한 방 (누가 때렸든) */
    private float lastHit;
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
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1024.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0);
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

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};

    /** 이 아이템이 입는 부위 (갑옷이 아니면 null) */
    private static net.minecraft.world.entity.EquipmentSlot armorSlot(ItemStack s) {
        var eq = s.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (eq == null) return null;
        for (var a : ARMOR) if (eq.slot() == a) return a;
        return null;
    }

    private void giveBack(ServerPlayer sp, ItemStack s) {
        if (s.isEmpty()) return;
        if (!sp.getInventory().add(s)) spawnAtLocation((ServerLevel) level(), s);
    }

    /**
     * 우클릭: 갑옷을 들고 있으면 허수아비에게 입힘(입던 건 돌려받음, 인챈트 그대로 적용) · 빈손이면 기록 초기화
     * 웅크리고 우클릭: 갑옷을 입고 있으면 모두 벗김, 안 입었으면 허수아비 회수
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        ItemStack held = sp.getMainHandItem();
        var slot = armorSlot(held);
        if (slot != null && !sp.isShiftKeyDown()) {
            ItemStack old = getItemBySlot(slot).copy();
            setItemSlot(slot, held.copyWithCount(1));
            if (!sp.getAbilities().instabuild) held.shrink(1);
            giveBack(sp, old);
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
            updateName();
            sp.sendOverlayMessage(Component.literal("§a허수아비가 갑옷을 입었어요 §7· 방어 " + armorText()));
            return InteractionResult.SUCCESS;
        }
        if (sp.isShiftKeyDown()) {
            boolean any = false;
            for (var a : ARMOR) {
                ItemStack s = getItemBySlot(a);
                if (s.isEmpty()) continue;
                giveBack(sp, s.copy());
                setItemSlot(a, ItemStack.EMPTY);
                any = true;
            }
            if (any) {
                updateName();
                sp.sendOverlayMessage(Component.literal("§e갑옷을 모두 벗겼어요 §7· 한 번 더 웅크리고 우클릭하면 회수"));
                return InteractionResult.SUCCESS;
            }
            discard();
            giveBack(sp, new ItemStack(com.snolred.wildplus.registry.ModItems35.DPS_DUMMY.get()));
            sp.sendOverlayMessage(Component.literal("§a허수아비를 회수했어요"));
        } else {
            stats.clear();
            updateName();
            level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
            sp.sendOverlayMessage(Component.literal("§e기록 초기화 §7· 갑옷을 들고 우클릭 = 입히기 · 웅크리고 우클릭 = 벗기기/회수"));
        }
        return InteractionResult.SUCCESS;
    }

    private String armorText() {
        return String.format("%d · 강도 %d", (int) getArmorValue(), (int) getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    private boolean armored() {
        for (var a : ARMOR) if (!getItemBySlot(a).isEmpty()) return true;
        return false;
    }

    private void record(ServerPlayer p, float dmg, boolean crit) {
        long now = level().getGameTime();
        Stat s = stats.computeIfAbsent(p.getUUID(), k -> new Stat());
        if (s.count == 0) s.first = now;
        s.hits.addLast(new double[] {now, dmg});
        s.total += dmg;
        s.max = Math.max(s.max, dmg);
        s.lastDmg = dmg;
        s.count++;
        if (crit) s.crits++;
        s.last = now;
        lastHit = dmg;
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
        p.sendOverlayMessage(Component.literal("§6내 DPS §f" + f(dps(s, now)) + " §7· 방금 §c" + f(s.lastDmg) + " §7· 한 방 최고 §f" + f(s.max) + " §7· 치명타 §f" + s.crits
                + "/" + s.count + "회 §7· 누적 §f" + f(s.total) + " §7(" + secs + "초)"));
    }

    private void updateName() {
        long now = level().getGameTime();
        if (stats.isEmpty()) {
            setCustomName(Component.literal("§eDPS 측정 허수아비 §7· 때려 보세요" + (armored() ? " §b🛡 " + armorText() : "")));
            return;
        }
        double sum = 0;
        for (Stat s : stats.values()) sum += dps(s, now);
        setCustomName(Component.literal("§c⚔ §fDPS §e" + f(sum) + " §7· 방금 §c" + f(lastHit) + (stats.size() > 1 ? " §7(" + stats.size() + "명)" : "") + (armored() ? " §b🛡 " + armorText() : "")));
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
        float dmg = event.getHealthDamage();
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
