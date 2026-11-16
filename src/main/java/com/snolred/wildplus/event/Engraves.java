package com.snolred.wildplus.event;

import static com.snolred.wildplus.registry.ModItems22.*;

import com.snolred.wildplus.WildPlus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 4.2 각인: 공허 장비 전용 5종 · 다른 장비 공용 5종 · 겉날개 1종 · (5.1.1) 겁화 장비 전용 4종 · 제련. 저장 위치는 RubyGear.ENGRAVE 와 같다 */
public final class Engraves {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final long HEART_CD = 12_000L;

    private Engraves() {}

    private static boolean voidArmor(ItemStack s) {
        return s.is(VOID_HELMET.get()) || s.is(VOID_CHESTPLATE.get()) || s.is(VOID_LEGGINGS.get()) || s.is(VOID_BOOTS.get());
    }

    private static boolean kalpaArmor(ItemStack s) {
        return s.is(com.snolred.wildplus.registry.ModItems34.KALPA_HELMET.get()) || s.is(com.snolred.wildplus.registry.ModItems34.KALPA_CHESTPLATE.get())
                || s.is(com.snolred.wildplus.registry.ModItems34.KALPA_LEGGINGS.get()) || s.is(com.snolred.wildplus.registry.ModItems34.KALPA_BOOTS.get());
    }

    private static boolean kalpaWeapon(ItemStack s) {
        return s.is(com.snolred.wildplus.registry.ModItems34.KALPA_SWORD.get()) || s.is(com.snolred.wildplus.registry.ModItems34.KALPA_BOW.get());
    }

    private static boolean voidGear(ItemStack s) {
        return voidArmor(s) || s.is(VOID_SWORD.get()) || s.is(VOID_BOW.get()) || s.is(VOID_PICKAXE.get()) || s.is(VOID_AXE.get()) || s.is(VOID_SHOVEL.get()) || s.is(VOID_HOE.get());
    }

    /** 내구도가 있거나, 파괴 불가 장비(스텔라 루비 · 스카이 사파이어 · 로열 에메랄드 · 공허)면 장비 */
    public static boolean gear(ItemStack s) {
        return s.isDamageableItem() || s.has(DataComponents.UNBREAKABLE);
    }

    private static boolean armor(ItemStack s) {
        if (!gear(s) || s.is(Items.ELYTRA)) return false;
        var eq = s.get(DataComponents.EQUIPPABLE);
        if (eq == null) return false;
        for (EquipmentSlot a : ARMOR) if (eq.slot() == a) return true;
        return false;
    }

    private static boolean boots(ItemStack s) {
        var eq = s.get(DataComponents.EQUIPPABLE);
        return armor(s) && eq != null && eq.slot() == EquipmentSlot.FEET;
    }

    private static boolean tool(ItemStack s) {
        return s.has(DataComponents.TOOL) && gear(s) && !s.is(net.minecraft.tags.ItemTags.SWORDS);
    }

    private static boolean weapon(ItemStack s) {
        return RubyGear.lifestealable(s) || s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem;
    }

    /** 이 장비에 이 각인을 새길 수 있는가 (새 각인만) */
    public static boolean can(ItemStack s, String kind) {
        return switch (kind) {
            case "ABYSS", "DEVOUR" -> s.is(VOID_SWORD.get());
            case "CHAIN" -> s.is(VOID_BOW.get());
            case "WARP", "HEART" -> voidArmor(s);
            case "HARVEST" -> tool(s) && !voidGear(s);
            case "FURY" -> weapon(s) && !voidGear(s);
            case "GUARD", "VIGOR" -> armor(s) && !voidGear(s);
            case "RUSH" -> boots(s) && !voidGear(s);
            case "GLIDE" -> s.is(Items.ELYTRA);
            case "INFERNO" -> kalpaWeapon(s);
            case "PYRE" -> s.is(com.snolred.wildplus.registry.ModItems34.KALPA_BOW.get());
            case "CINDER", "PHOENIX" -> kalpaArmor(s);
            case "SMELT" -> tool(s);
            case "EXECUTE", "SONIC" -> weapon(s) || kalpaWeapon(s) || s.is(VOID_SWORD.get()) || s.is(VOID_BOW.get());
            case "BULWARK", "ECHO" -> armor(s) || voidArmor(s) || kalpaArmor(s);
            default -> false;
        };
    }

    /** 각인 버튼을 보여 줄 장비인가 */
    public static boolean engravable(ItemStack s) {
        return voidGear(s) && !(s.is(VOID_PICKAXE.get()) || s.is(VOID_AXE.get()) || s.is(VOID_SHOVEL.get()) || s.is(VOID_HOE.get()))
                || s.is(Items.ELYTRA) || tool(s) || weapon(s) || armor(s);
    }

    public static String name(String k) {
        return switch (k) {
            case "ABYSS" -> "심연 각인";
            case "DEVOUR" -> "포식 각인";
            case "CHAIN" -> "연쇄 균열 각인";
            case "WARP" -> "차원 도약 각인";
            case "HEART" -> "공허 심장 각인";
            case "HARVEST" -> "수확 각인";
            case "FURY" -> "맹공 각인";
            case "GUARD" -> "견고 각인";
            case "VIGOR" -> "활력 각인";
            case "RUSH" -> "질주 각인";
            case "GLIDE" -> "공허 활공 각인";
            case "INFERNO" -> "업화 각인";
            case "PYRE" -> "화장 각인";
            case "CINDER" -> "잿불 갑주 각인";
            case "PHOENIX" -> "불사조 각인";
            case "SMELT" -> "제련 각인";
            case "EXECUTE" -> "고대 처형 각인";
            case "SONIC" -> "고대 음파 각인";
            case "BULWARK" -> "고대 보루 각인";
            case "ECHO" -> "고대 메아리 각인";
            default -> "";
        };
    }

    /** 툴팁 문구 ("" 이면 새 각인 아님) */
    public static String text(String k) {
        return switch (k) {
            case "ABYSS" -> "✦ 심연 각인: 공허 베기 범위 +2칸 · 피해 +30%";
            case "DEVOUR" -> "✦ 포식 각인: 균열 표식 걸린 적을 처치하면 체력 4 회복";
            case "CHAIN" -> "✦ 연쇄 균열 각인: 공허 화살 폭발이 주변 적 1명에게 번짐";
            case "WARP" -> "✦ 차원 도약 각인: 공허 도약 쿨타임 -40%";
            case "HEART" -> "✦ 공허 심장 각인: 치명타를 입으면 3초 무적 (10분에 1번)";
            case "HARVEST" -> "✦ 수확 각인: 드롭 +10%";
            case "FURY" -> "✦ 맹공 각인: 체력이 가득 찬 적에게 피해 +25%";
            case "GUARD" -> "✦ 견고 각인: 받는 피해 -5% (부위마다, 최대 -20%)";
            case "VIGOR" -> "✦ 활력 각인: 최대 체력 +2칸 (여러 부위여도 1번만)";
            case "RUSH" -> "✦ 질주 각인: 이동속도 +8%";
            case "GLIDE" -> "✦ 공허 활공 각인: 겉날개 내구도가 닳지 않음";
            case "INFERNO" -> "✦ 업화 각인: 불타는 적에게 피해 +25%";
            case "PYRE" -> "✦ 화장 각인: 화살이 맞은 자리 3칸에 불기둥 (피해 6 · 불붙임)";
            case "CINDER" -> "✦ 잿불 갑주 각인: 나를 때린 적에게 피해 2 + 불 (부위마다, 최대 8)";
            case "PHOENIX" -> "✦ 불사조 각인: 불·용암 피해 무시 · 불타는 동안 초당 체력 1 회복";
            case "SMELT" -> "✦ 제련 각인: 캔 원석(철·금·구리)이 바로 주괴로";
            case "EXECUTE" -> "✦ 고대 처형 각인: 체력 절반 이하인 적에게 피해 +30%";
            case "SONIC" -> "✦ 고대 음파 각인: 4타마다 주변 3칸 방어 무시 피해 21.6";
            case "BULWARK" -> "✦ 고대 보루 각인: 받는 피해 -6% (부위마다, 최대 -24%) · 어둠 면역";
            case "ECHO" -> "✦ 고대 메아리 각인: 나를 때린 적에게 피해 2.4 + 구속 (부위마다, 최대 9.6)";
            default -> "";
        };
    }

    public static boolean has(LivingEntity e, String kind) {
        return RubyGear.hasEngraving(e, kind);
    }

    private static final java.util.Map<java.util.UUID, Integer> SONIC_HITS = new java.util.HashMap<>();

    private static int armorCount(LivingEntity e, String kind) {
        int n = 0;
        for (EquipmentSlot a : ARMOR) if (RubyGear.has(e.getItemBySlot(a), kind)) n++;
        return n;
    }

    private static void setMod(Player p, Holder<Attribute> attr, String path, double want, AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        var id = WildPlus.id(path);
        AttributeModifier cur = a.getModifier(id);
        if (cur != null && cur.amount() == want || cur == null && want == 0) return;
        a.removeModifier(id);
        if (want != 0) a.addPermanentModifier(new AttributeModifier(id, want, op));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        setMod(p, Attributes.MAX_HEALTH, "engrave_vigor", armorCount(p, "VIGOR") > 0 ? 4.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        setMod(p, Attributes.MOVEMENT_SPEED, "engrave_rush", armorCount(p, "RUSH") > 0 ? 0.08 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.isOnFire() && armorCount(p, "PHOENIX") > 0) p.heal(1.0F);
        if (armorCount(p, "BULWARK") > 0 && p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) p.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (p.isFallFlying() && RubyGear.has(chest, "GLIDE") && chest.getDamageValue() > 0) chest.setDamageValue(chest.getDamageValue() - 1);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Player p && RubyGear.has(p.getMainHandItem(), "FURY") && target.getHealth() >= target.getMaxHealth()) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        if (event.getSource().getEntity() instanceof Player p && target.isOnFire() && RubyGear.has(p.getMainHandItem(), "INFERNO"))
            event.setAmount(event.getAmount() * 1.25F);
        if (armorCount(target, "PHOENIX") > 0 && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }
        int cinder = armorCount(target, "CINDER");
        if (cinder > 0 && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS) && event.getSource().getDirectEntity() instanceof LivingEntity hitter && hitter != target && target.level() instanceof ServerLevel lv) {
            hitter.hurtServer(lv, lv.damageSources().thorns(target), 2.0F * cinder);
            hitter.igniteForSeconds(3);
        }
        int guard = armorCount(target, "GUARD");
        if (guard > 0) event.setAmount(event.getAmount() * (1.0F - 0.05F * guard));
        // 5.2.1 고대 각인
        int bulwark = armorCount(target, "BULWARK");
        if (bulwark > 0) event.setAmount(event.getAmount() * (1.0F - 0.06F * bulwark));
        int echo = armorCount(target, "ECHO");
        if (echo > 0 && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS) && event.getSource().getDirectEntity() instanceof LivingEntity hitter2 && hitter2 != target && target.level() instanceof ServerLevel lv2) {
            hitter2.hurtServer(lv2, lv2.damageSources().thorns(target), 2.4F * echo);
            hitter2.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 1));
        }
        if (event.getSource().getEntity() instanceof Player p3 && event.getSource().getDirectEntity() == p3 && target.level() instanceof ServerLevel lv3) {
            ItemStack w = p3.getMainHandItem();
            if (RubyGear.has(w, "EXECUTE") && target.getHealth() <= target.getMaxHealth() * 0.5F) event.setAmount(event.getAmount() * 1.30F);
            if (RubyGear.has(w, "SONIC") && SONIC_HITS.merge(p3.getUUID(), 1, Integer::sum) % 4 == 0) {
                for (LivingEntity e : lv3.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0),
                        e -> e != p3 && (e == target || e instanceof net.minecraft.world.entity.monster.Enemy))) {
                    if (e == target) event.setAmount(event.getAmount() + 21.6F);
                    else e.hurtServer(lv3, lv3.damageSources().indirectMagic(p3, p3), 21.6F);
                }
                lv3.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1, 0, 0, 0, 0);
                lv3.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.4F);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof ServerPlayer p && p.level() instanceof ServerLevel level && armorCount(p, "HEART") > 0
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            long now = level.getGameTime();
            var data = p.getPersistentData();
            if (now >= data.getLongOr("wp_heart_ready", 0L)) {
                data.putLong("wp_heart_ready", now + HEART_CD);
                event.setCanceled(true);
                p.setHealth(Math.max(4.0F, p.getMaxHealth() * 0.2F));
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 4, true, false, true));
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 120, 0.6, 0.9, 0.6, 0.25);
                level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 0.7F);
                p.sendSystemMessage(Component.literal("✦ 공허 심장이 당신을 붙잡았어요! (10분 뒤 다시 발동)").withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && RubyGear.has(p.getMainHandItem(), "DEVOUR") && VoidGear.marked(dead)) {
            p.heal(4.0F);
            if (p.level() instanceof ServerLevel lv) lv.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
    }

    /** 화장 각인: 겁화 활 화살이 맞으면 그 자리에 불기둥 */
    @SubscribeEvent
    public static void onArrow(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow a) || !(a.level() instanceof ServerLevel lv)
                || !(a.getOwner() instanceof ServerPlayer p) || a.getPersistentData().getBooleanOr("wp_pyre_done", false)) return;
        if (!RubyGear.has(p.getMainHandItem(), "PYRE") && !RubyGear.has(p.getOffhandItem(), "PYRE")) return;
        a.getPersistentData().putBoolean("wp_pyre_done", true);
        var at = event.getRayTraceResult().getLocation();
        lv.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 50, 1.0, 1.2, 1.0, 0.05);
        lv.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 10, 0.8, 0.3, 0.8, 0.1);
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(3.0), e -> e != p && !(e instanceof Player))) {
            e.hurtServer(lv, lv.damageSources().indirectMagic(a, p), 6.0F);
            e.igniteForSeconds(4);
        }
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (event.getBlockEntity() == null && RubyGear.has(event.getTool(), "SMELT")) {
            for (ItemEntity e : event.getDrops()) {
                ItemStack s = e.getItem();
                var to = s.is(Items.RAW_IRON) ? Items.IRON_INGOT : s.is(Items.RAW_GOLD) ? Items.GOLD_INGOT : s.is(Items.RAW_COPPER) ? Items.COPPER_INGOT : null;
                if (to != null) e.setItem(new ItemStack(to, s.getCount()));
            }
        }
        if (event.getBlockEntity() != null || !RubyGear.has(event.getTool(), "HARVEST")) return;
        for (ItemEntity e : event.getDrops()) {
            if (event.getLevel().getRandom().nextFloat() < 0.10F) e.getItem().grow(1);
        }
    }
}
