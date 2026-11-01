package com.snolred.wildplus.event;

import static com.snolred.wildplus.registry.ModItems11.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 사파이어 장비 특수 능력 — 루비(불·공격)의 반대편인 얼음·물·방어.
 *  - 검 "빙결검": 맞은 적 구속 II 3초, 25% 확률로 서리 폭발 (주변 3칸 적 피해 4 + 얼림)
 *  - 곡괭이 "냉각": 웅크리고 들면 주변 4칸 용암을 굳힘 (원천 → 흑요석, 흐름 → 조약돌) · 물속에서 빨리 캠
 *  - 도끼 "얼음 처형": 느려졌거나 얼어 있는 적에게 피해 1.5배
 *  - 삽 "빙하 삽": 얼음·꽁꽁 언 얼음·푸른 얼음을 그대로 주움, 눈 블록은 눈 블록으로
 *  - 괭이 "물의 축복": 웅크리고 들면 주변 4칸 작물이 빨리 자라고 경작지가 촉촉해짐
 *  - 갑옷 2부위: 수중 호흡 + 돌고래의 가호 / 4부위: 받는 피해 10% 감소, 때린 적 얼림, 낙하 피해 절반
 * 스카이 사파이어(용의 깃털로 승급): 내구도 무한 · 불에 안 탐 · 위 능력 강화
 *  - 검: 공격력 8 · 속도 2.0, 공중에서 때리면 1.5배 (급강하 일격), 서리 폭발 40% · 피해 6
 *  - 흉갑: 겉날개 겸용
 *  - 2부위: 저항 I + 수중 호흡 / 4부위: + 신속 I · 돌고래의 가호 · 낙하 피해 없음
 */
public final class SapphireGear {
    private SapphireGear() {}

    private static boolean bursting;

    private static boolean is(ItemStack s, net.neoforged.neoforge.registries.DeferredItem<? extends Item> a, net.neoforged.neoforge.registries.DeferredItem<? extends Item> b) {
        return s.is(a.get()) || s.is(b.get());
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** 사파이어(스카이 포함) 갑옷 부위 수 */
    public static int pieces(LivingEntity p) {
        Item[] a = {SAPPHIRE_HELMET.get(), SAPPHIRE_CHESTPLATE.get(), SAPPHIRE_LEGGINGS.get(), SAPPHIRE_BOOTS.get()};
        Item[] b = {SKY_SAPPHIRE_HELMET.get(), SKY_SAPPHIRE_CHESTPLATE.get(), SKY_SAPPHIRE_LEGGINGS.get(), SKY_SAPPHIRE_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(ARMOR[i]);
            if (s.is(a[i]) || s.is(b[i])) n++;
        }
        return n;
    }

    public static int skyPieces(LivingEntity p) {
        Item[] b = {SKY_SAPPHIRE_HELMET.get(), SKY_SAPPHIRE_CHESTPLATE.get(), SKY_SAPPHIRE_LEGGINGS.get(), SKY_SAPPHIRE_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(ARMOR[i]).is(b[i])) n++;
        return n;
    }

    private static MobEffectInstance fx(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        return new MobEffectInstance(e, 80, amp, true, false, true);
    }

    // ───────── 갑옷 · 곡괭이 · 괭이 ─────────
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || p.tickCount % 40 != 0) return;
        int n = pieces(p), sky = skyPieces(p);
        if (n >= 2) {
            p.addEffect(fx(MobEffects.WATER_BREATHING, 0));
            p.addEffect(fx(MobEffects.DOLPHINS_GRACE, 0));
        }
        if (sky >= 2) p.addEffect(fx(MobEffects.RESISTANCE, 0));
        if (sky >= 4) p.addEffect(fx(MobEffects.SPEED, 0));
        ItemStack hand = p.getMainHandItem();
        // 곡괭이: 물속에서는 해양의 힘(빨리 캠), 웅크리면 주변 용암을 굳힘
        if (is(hand, SAPPHIRE_PICKAXE, SKY_SAPPHIRE_PICKAXE)) {
            if (p.isInWater()) p.addEffect(fx(MobEffects.CONDUIT_POWER, 0));
            if (p.isShiftKeyDown()) {
                int cooled = 0;
                BlockPos c = p.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, -4, -4), c.offset(4, 4, 4))) {
                    BlockState st = level.getBlockState(pos);
                    if (!st.is(Blocks.LAVA)) continue;
                    level.setBlock(pos, st.getFluidState().isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.01);
                    if (++cooled >= 40) break;
                }
                if (cooled > 0) level.playSound(null, p.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }
        // 괭이: 웅크리면 주변 작물 성장 + 경작지 촉촉
        if (p.isShiftKeyDown() && is(hand, SAPPHIRE_HOE, SKY_SAPPHIRE_HOE)) {
            BlockPos c = p.blockPosition();
            int grown = 0, tries = hand.is(SKY_SAPPHIRE_HOE.get()) ? 6 : 3;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
                BlockState st = level.getBlockState(pos);
                if (st.getBlock() instanceof FarmBlock && st.getValue(FarmBlock.MOISTURE) < 7) {
                    level.setBlock(pos, st.setValue(FarmBlock.MOISTURE, 7), 2);
                }
                if (grown < tries && st.getBlock() instanceof CropBlock crop && !crop.isMaxAge(st) && p.getRandom().nextInt(3) == 0) {
                    ((BonemealableBlock) crop).performBonemeal(level, level.getRandom(), pos.immutable(), st);
                    level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.25, 0.1, 0.25, 0.0);
                    grown++;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (skyPieces(p) >= 4) event.setCanceled(true);
        else if (pieces(p) >= 4) event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p) {
            ItemStack hand = p.getMainHandItem();
            // 도끼: 느려졌거나 얼어 있는 적에게 1.5배
            if (is(hand, SAPPHIRE_AXE, SKY_SAPPHIRE_AXE) && (target.hasEffect(MobEffects.SLOWNESS) || target.getTicksFrozen() > 0)) {
                event.setAmount(event.getAmount() * 1.5F);
                if (target.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                }
            }
            // 스카이 사파이어 검: 공중에서 때리면 1.5배 (급강하 일격)
            if (hand.is(SKY_SAPPHIRE_SWORD.get()) && !p.onGround() && (p.isFallFlying() || p.fallDistance > 1.5)) {
                event.setAmount(event.getAmount() * 1.5F);
                if (target.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
        // 4부위: 받는 피해 10% 감소 + 때린 적 얼림
        if (target instanceof Player p && pieces(p) >= 4) {
            event.setAmount(event.getAmount() * 0.9F);
            if (event.getSource().getDirectEntity() instanceof LivingEntity att && att != p) {
                att.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                att.setTicksFrozen(Math.max(att.getTicksFrozen(), att.getTicksRequiredToFreeze() + 40));
            }
        }
    }

    // ───────── 검 ─────────
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (bursting || !(event.getSource().getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || event.getSource().getDirectEntity() != player || !is(player.getMainHandItem(), SAPPHIRE_SWORD, SKY_SAPPHIRE_SWORD)) {
            return;
        }
        boolean sky = player.getMainHandItem().is(SKY_SAPPHIRE_SWORD.get());
        LivingEntity target = event.getEntity();
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        if (player.getRandom().nextFloat() >= (sky ? 0.4F : 0.25F)) return;
        bursting = true;
        try {
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.6, target.getZ(), 50, 1.5, 0.5, 1.5, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0))) {
                if (e == player || !(e instanceof Enemy)) continue;
                if (e != target) e.hurtServer(level, level.damageSources().playerAttack(player), sky ? 6.0F : 4.0F);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 60));
            }
        } finally {
            bursting = false;
        }
    }

    // ───────── 삽 ─────────
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(event.getBreaker() instanceof Player) || !is(tool, SAPPHIRE_SHOVEL, SKY_SAPPHIRE_SHOVEL)) return;
        BlockState state = event.getState();
        Item keep = state.is(Blocks.ICE) ? Items.ICE : state.is(Blocks.PACKED_ICE) ? Items.PACKED_ICE
                : state.is(Blocks.BLUE_ICE) ? Items.BLUE_ICE : state.is(Blocks.SNOW_BLOCK) ? Items.SNOW_BLOCK : null;
        if (keep == null) return;
        BlockPos pos = event.getPos();
        event.getDrops().clear();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(keep)));
        event.getLevel().sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
    }
}
