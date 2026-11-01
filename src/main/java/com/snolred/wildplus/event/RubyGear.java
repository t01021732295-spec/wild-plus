package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModItems7;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 루비 장비 특수 능력 (모두 불에 타지 않음).
 *  - 검 "화염검": 맞은 적을 4초 동안 불태움, 25% 확률로 폭염 (주변 3칸 적에게 피해 4 + 불)
 *  - 곡괭이 "광맥 탐지": 웅크리고 들고 있으면 주변 8칸 광석 위치에 불꽃 표시
 *  - 도끼 "처형": 불타는 적에게 피해 1.5배
 *  - 삽 "용광 삽": 모래 → 유리, 자갈 → 부싯돌, 점토 → 벽돌 (바로 구워져 나옴)
 *  - 괭이 "자동 파종": 다 자란 작물을 캐면 씨앗 하나로 그 자리에 다시 심어 줌
 *  - 갑옷 2부위: 화염 저항 / 4부위: 나를 때린 적이 3초 동안 불타고, 받는 피해 10% 감소
 */
public final class RubyGear {
    private RubyGear() {}

    private static boolean bursting;
    private static final List<Replant> REPLANT = new ArrayList<>();

    private record Replant(ServerLevel level, BlockPos pos, BlockState state) {}

    /** 루비 또는 스텔라 루비 */
    private static boolean is(ItemStack s, net.neoforged.neoforge.registries.DeferredItem<Item> ruby, net.neoforged.neoforge.registries.DeferredItem<Item> star) {
        return s.is(ruby.get()) || s.is(star.get());
    }

    public static final String TRUE_DMG = "wildplus_true_dmg", ENGRAVE = "wildplus_engrave", LIFESTEAL = "wildplus_lifesteal";

    /** 흡혈 강화 단계 (1단계 = 준 피해의 1%) */
    public static int lifesteal(ItemStack s) {
        return s.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getIntOr(LIFESTEAL, 0);
    }

    /** 흡혈 강화를 할 수 있는 근접 무기 (검 · 도끼 · 철퇴 · 삼지창) */
    public static boolean lifestealable(ItemStack s) {
        return s.is(net.minecraft.tags.ItemTags.SWORDS) || s.is(net.minecraft.tags.ItemTags.AXES)
                || s.is(net.minecraft.world.item.Items.MACE) || s.is(net.minecraft.world.item.Items.TRIDENT);
    }

    /** 스텔라 루비 각인: TITAN(대지) · LICH(망령) · FROST(서리), 없으면 "" */
    public static String engraving(ItemStack s) {
        return s.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getStringOr(ENGRAVE, "");
    }

    public static boolean isStar(ItemStack s) {
        return com.snolred.wildplus.registry.ModItems9.upgrades().containsValue(s.getItem());
    }

    /** 손에 든 무기·입은 갑옷 중 하나라도 그 각인이 있으면 */
    public static boolean hasEngraving(LivingEntity e, String kind) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (kind.equals(engraving(e.getItemBySlot(slot)))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player p && hasEngraving(p, "TITAN")) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKillEngrave(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player p && !p.level().isClientSide() && hasEngraving(p, "LICH")) {
            p.heal(3.0F);
        }
    }

    @SubscribeEvent
    public static void onXpEngrave(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() != null && hasEngraving(event.getAttackingPlayer(), "LICH")) {
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.5F));
        }
    }

    /** 스텔라 루비 검 고정 피해 강화 단계 (0~4, 한 단계 0.5) */
    public static int trueLevel(ItemStack s) {
        return s.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getIntOr(TRUE_DMG, 0);
    }

    public static float trueDamage(ItemStack s) {
        return 4.0F + 0.5F * Math.min(4, trueLevel(s));
    }

    public static int starPieces(LivingEntity p) {
        Item[] items = {com.snolred.wildplus.registry.ModItems9.STAR_RUBY_HELMET.get(), com.snolred.wildplus.registry.ModItems9.STAR_RUBY_CHESTPLATE.get(),
                com.snolred.wildplus.registry.ModItems9.STAR_RUBY_LEGGINGS.get(), com.snolred.wildplus.registry.ModItems9.STAR_RUBY_BOOTS.get()};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(slots[i]).is(items[i])) n++;
        return n;
    }

    public static int pieces(LivingEntity p) {
        Item[] items = {ModItems7.RUBY_HELMET.get(), ModItems7.RUBY_CHESTPLATE.get(), ModItems7.RUBY_LEGGINGS.get(), ModItems7.RUBY_BOOTS.get()};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(slots[i]).is(items[i])) n++;
        return n;
    }

    // ───────── 갑옷 · 곡괭이 ─────────
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || p.tickCount % 40 != 0) return;
        if (pieces(p) >= 2) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
        // 핏빛 각인: 체력이 절반 이하면 힘 I
        if (hasEngraving(p, "BLOOD") && p.getHealth() <= p.getMaxHealth() * 0.5F) {
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false, true));
        }
        // 스텔라 루비 세트: 2부위 재생 I / 4부위 재생 II + 화염 저항 + 야간 투시
        int star = starPieces(p);
        if (star >= 2) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, star >= 4 ? 1 : 0, true, false, true));
        if (star >= 4) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
        }
        if (p.isShiftKeyDown() && is(p.getMainHandItem(), ModItems7.RUBY_PICKAXE, com.snolred.wildplus.registry.ModItems9.STAR_RUBY_PICKAXE) && p instanceof ServerPlayer sp) {
            BlockPos c = p.blockPosition();
            int shown = 0;
            // 스텔라 루비 곡괭이: 16칸 · 48개까지
            boolean starPick = p.getMainHandItem().is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_PICKAXE.get());
            int r = starPick ? 16 : 8, max = starPick ? 48 : 24;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
                if (!level.getBlockState(pos).is(Tags.Blocks.ORES)) continue;
                level.sendParticles(sp, starPick ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, true, true,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
                if (++shown >= max) break;
            }
            if (shown > 0) level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // 도끼: 불타는 적에게 1.5배
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p
                && is(p.getMainHandItem(), ModItems7.RUBY_AXE, com.snolred.wildplus.registry.ModItems9.STAR_RUBY_AXE) && target.isOnFire()) {
            event.setAmount(event.getAmount() * (p.getMainHandItem().is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_AXE.get()) ? 2.0F : 1.5F));
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
            }
        }
        // 스텔라 루비 도끼: 때린 적에게 불 (처형과 바로 이어짐)
        if (event.getSource().getEntity() instanceof Player p2 && event.getSource().getDirectEntity() == p2
                && p2.getMainHandItem().is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_AXE.get())) {
            target.igniteForSeconds(4.0F);
        }
        // 각인: 대지 = 낙하 피해 없음 · 서리 = 나를 때린 적 구속
        if (target instanceof Player p && hasEngraving(p, "TITAN") && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof Player p && hasEngraving(p, "FROST") && event.getSource().getDirectEntity() instanceof LivingEntity att && att != p) {
            att.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        }
        // 4부위: 피해 10% 감소 + 때린 적 불태우기
        if (target instanceof Player p && pieces(p) >= 4) {
            event.setAmount(event.getAmount() * 0.9F);
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != p) attacker.igniteForSeconds(3.0F);
        }
    }

    // ───────── 검 ─────────
    @SubscribeEvent
    public static void onFrostHit(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p && !p.level().isClientSide()
                && hasEngraving(p, "FROST")) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        }
    }

    /** 핏빛 각인: 준 피해의 15% 흡혈 (최대 4) */
    @SubscribeEvent
    public static void onBloodHit(LivingDamageEvent.Post event) {
        // 핏빛 강철 흡혈 강화: 손에 든 무기로 직접 때렸을 때 준 피해의 n%
        if (event.getSource().getEntity() instanceof Player ap && !ap.level().isClientSide()
                && event.getSource().getDirectEntity() == ap && ap.getHealth() < ap.getMaxHealth()) {
            int ls = lifesteal(ap.getMainHandItem());
            if (ls > 0) ap.heal(event.getHealthDamage() * ls / 100.0F);
        }
        if (event.getSource().getEntity() instanceof Player p && !p.level().isClientSide() && hasEngraving(p, "BLOOD")
                && p.getHealth() < p.getMaxHealth()) {
            p.heal(Math.min(4.0F, event.getHealthDamage() * 0.15F));
        }
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (bursting || !(event.getSource().getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || event.getSource().getDirectEntity() != player
                || !is(player.getMainHandItem(), ModItems7.RUBY_SWORD, com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SWORD)) {
            return;
        }
        LivingEntity target = event.getEntity();
        // 스텔라 루비 검: 방어구를 무시하는 고정 피해 (기본 4, 강화로 최대 6)
        if (player.getMainHandItem().is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SWORD.get()) && target.isAlive()) {
            bursting = true;
            try {
                // 무적 시간 때문에 두 번째 피해가 무시되지 않도록 체력을 직접 깎는다
                float left = target.getHealth() - trueDamage(player.getMainHandItem());
                if (left > 0) {
                    target.setHealth(left);
                } else {
                    target.setHealth(0.0F);
                    target.die(level.damageSources().playerAttack(player));
                }
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 6, 0.2, 0.2, 0.2, 0.2);
            } finally {
                bursting = false;
            }
        }
        target.igniteForSeconds(4.0F);
        if (player.getRandom().nextFloat() >= 0.25F) return;
        bursting = true;
        try {
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5, target.getZ(), 40, 1.5, 0.4, 1.5, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.9F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0))) {
                if (e == player || e == target || !(e instanceof Enemy)) continue;
                e.hurtServer(level, level.damageSources().playerAttack(player), 4.0F);
                e.igniteForSeconds(3.0F);
            }
        } finally {
            bursting = false;
        }
    }

    private static boolean areaBreaking;

    /** 스텔라 루비 삽: 3×3 파기 / 스텔라 루비 괭이: 주변 3×3의 다 자란 작물도 함께 수확 (웅크리면 한 칸) */
    @SubscribeEvent
    public static void onBreakArea(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (areaBreaking || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        BlockPos c = event.getPos();
        boolean shovel = tool.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SHOVEL.get()) && state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
        boolean hoe = tool.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_HOE.get()) && state.getBlock() instanceof CropBlock cb && cb.isMaxAge(state);
        if (!shovel && !hoe) return;
        net.minecraft.core.Direction.Axis axis = hoe || Math.abs(player.getXRot()) > 45 ? net.minecraft.core.Direction.Axis.Y : player.getDirection().getAxis();
        areaBreaking = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos p = switch (axis) {
                        case Y -> c.offset(a, 0, b);
                        case X -> c.offset(0, a, b);
                        default -> c.offset(a, b, 0);
                    };
                    BlockState s = level.getBlockState(p);
                    boolean ok = shovel ? s.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL) && s.getDestroySpeed(level, p) >= 0
                            : s.getBlock() instanceof CropBlock cr && cr.isMaxAge(s);
                    if (ok) player.gameMode.destroyBlock(p);
                }
            }
        } finally {
            areaBreaking = false;
        }
    }

    // ───────── 삽 · 괭이 ─────────
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(event.getBreaker() instanceof Player)) return;
        BlockState state = event.getState();
        // 스텔라 루비 곡괭이: 광석을 캐면 25% 확률로 전리품 두 배 (광석 블록 그대로 나올 땐 제외)
        if (tool.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_PICKAXE.get()) && state.is(Tags.Blocks.ORES)
                && event.getLevel().getRandom().nextFloat() < 0.25F) {
            boolean selfDrop = false;
            for (ItemEntity e : event.getDrops()) if (e.getItem().is(state.getBlock().asItem())) selfDrop = true;
            if (!selfDrop) {
                for (ItemEntity e : event.getDrops()) e.getItem().grow(e.getItem().getCount());
                BlockPos pos = event.getPos();
                event.getLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
            }
        }
        // 스텔라 루비 괭이: 다 자란 작물 50% 확률로 수확량 두 배
        if (tool.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_HOE.get()) && state.getBlock() instanceof CropBlock c0 && c0.isMaxAge(state)
                && event.getLevel().getRandom().nextBoolean()) {
            for (ItemEntity e : event.getDrops()) if (!e.getItem().is(c0.asItem())) e.getItem().grow(e.getItem().getCount());
        }
        if (is(tool, ModItems7.RUBY_SHOVEL, com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SHOVEL)) {
            boolean smelted = false;
            for (ItemEntity e : event.getDrops()) {
                ItemStack s = e.getItem();
                Item out = s.is(Items.SAND) || s.is(Items.RED_SAND) ? Items.GLASS
                        : s.is(Items.GRAVEL) ? Items.FLINT : s.is(Items.CLAY_BALL) ? Items.BRICK
                        : s.is(Items.CLAY) ? Items.TERRACOTTA : null;
                if (out != null) {
                    e.setItem(new ItemStack(out, s.getCount()));
                    smelted = true;
                }
            }
            if (smelted) {
                BlockPos pos = event.getPos();
                event.getLevel().sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.25, 0.25, 0.25, 0.01);
            }
        } else if (is(tool, ModItems7.RUBY_HOE, com.snolred.wildplus.registry.ModItems9.STAR_RUBY_HOE) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
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
        for (Replant r : REPLANT) {
            if (r.level().getBlockState(r.pos()).isAir() && r.level().getBlockState(r.pos().below()).is(Blocks.FARMLAND)) {
                r.level().setBlock(r.pos(), r.state(), 3);
            }
        }
        REPLANT.clear();
    }
}
