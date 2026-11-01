package com.snolred.wildplus.event;

import static com.snolred.wildplus.registry.ModItems12.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 에메랄드 장비 (풍요 · 행운 · 생명). 로열 에메랄드는 같은 능력이 더 강하고 내구도 무한 · 불에 안 탐.
 *  - 검 "약탈": 처치하면 30%(로열 50%) 확률로 전리품 두 배, 경험치 1.3배(로열 1.6배)
 *  - 곡괭이 "행운의 손길": 광석 20%(35%) 확률로 전리품 하나 더
 *  - 도끼 "선제공격": 체력이 가득한 적에게 1.8배(2.2배)
 *  - 삽 "보물 찾기": 흙·모래·자갈에서 2%(4%) 확률로 에메랄드 · 금 조각 · 가끔 다이아몬드
 *  - 괭이 "풍작": 다 자란 작물 30%(50%) 확률로 수확량 두 배
 *  - 활 "생명의 화살": 맞히면 내 체력 1(로열 2 + 재생) 회복
 *  - 갑옷 2부위: 행운 I · 마을의 영웅 I (로열: 행운 II · 마을의 영웅 II · 재생 I)
 *          4부위: 체력 증가 I(+2하트) · 받는 피해 10% 감소 (로열: 체력 증가 II(+4하트) · 저항 I)
 */
public final class EmeraldGear {
    private EmeraldGear() {}

    private static boolean is(ItemStack s, net.neoforged.neoforge.registries.DeferredItem<? extends Item> a, net.neoforged.neoforge.registries.DeferredItem<? extends Item> b) {
        return s.is(a.get()) || s.is(b.get());
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static int pieces(LivingEntity p) {
        Item[] a = {EMERALD_HELMET.get(), EMERALD_CHESTPLATE.get(), EMERALD_LEGGINGS.get(), EMERALD_BOOTS.get()};
        Item[] b = {ROYAL_EMERALD_HELMET.get(), ROYAL_EMERALD_CHESTPLATE.get(), ROYAL_EMERALD_LEGGINGS.get(), ROYAL_EMERALD_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(ARMOR[i]);
            if (s.is(a[i]) || s.is(b[i])) n++;
        }
        return n;
    }

    public static int royalPieces(LivingEntity p) {
        Item[] b = {ROYAL_EMERALD_HELMET.get(), ROYAL_EMERALD_CHESTPLATE.get(), ROYAL_EMERALD_LEGGINGS.get(), ROYAL_EMERALD_BOOTS.get()};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(ARMOR[i]).is(b[i])) n++;
        return n;
    }

    private static MobEffectInstance fx(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        return new MobEffectInstance(e, 80, amp, true, false, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel) || p.tickCount % 40 != 0) return;
        int n = pieces(p), royal = royalPieces(p);
        if (n >= 2) {
            p.addEffect(fx(MobEffects.LUCK, royal >= 2 ? 1 : 0));
            p.addEffect(fx(MobEffects.HERO_OF_THE_VILLAGE, royal >= 2 ? 1 : 0));
        }
        if (royal >= 2) p.addEffect(fx(MobEffects.REGENERATION, 0));
        if (n >= 4) {
            // 체력 증가는 효과가 끊겼다 다시 붙으면 체력이 깎이므로 길게 유지
            MobEffectInstance hb = p.getEffect(MobEffects.HEALTH_BOOST);
            int amp = royal >= 4 ? 1 : 0;
            if (hb == null || hb.getAmplifier() != amp || hb.getDuration() < 200) {
                p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 400, amp, true, false, true));
            }
        }
        if (royal >= 4) p.addEffect(fx(MobEffects.RESISTANCE, 0));
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p
                && is(p.getMainHandItem(), EMERALD_AXE, ROYAL_EMERALD_AXE) && target.getHealth() >= target.getMaxHealth()) {
            event.setAmount(event.getAmount() * (p.getMainHandItem().is(ROYAL_EMERALD_AXE.get()) ? 2.2F : 1.8F));
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0);
            }
        }
        if (target instanceof Player p && pieces(p) >= 4 && royalPieces(p) < 4) event.setAmount(event.getAmount() * 0.9F);
    }

    /** 생명의 화살 */
    @SubscribeEvent
    public static void onArrowHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow a) || !(a.getOwner() instanceof Player owner)) return;
        String bow = a.getPersistentData().getStringOr(Bows.BOW_TAG, "");
        if (bow.equals("emerald")) owner.heal(1.0F);
        else if (bow.equals("royal_emerald")) {
            owner.heal(2.0F);
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        } else return;
        if (owner.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2, owner.getZ(), 1, 0.2, 0.1, 0.2, 0);
    }

    // ───────── 검: 약탈 ─────────
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || event.getEntity() instanceof Player) return;
        ItemStack hand = p.getMainHandItem();
        if (!is(hand, EMERALD_SWORD, ROYAL_EMERALD_SWORD)) return;
        float chance = hand.is(ROYAL_EMERALD_SWORD.get()) ? 0.5F : 0.3F;
        if (p.getRandom().nextFloat() >= chance) return;
        for (ItemEntity e : event.getDrops()) {
            ItemStack s = e.getItem();
            if (s.getMaxStackSize() > 1) s.grow(s.getCount());
        }
        if (p.level() instanceof ServerLevel sl) {
            LivingEntity v = event.getEntity();
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 0.5, v.getZ(), 15, 0.4, 0.4, 0.4, 0);
        }
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        Player p = event.getAttackingPlayer();
        if (p == null) return;
        ItemStack hand = p.getMainHandItem();
        if (!is(hand, EMERALD_SWORD, ROYAL_EMERALD_SWORD)) return;
        float mult = hand.is(ROYAL_EMERALD_SWORD.get()) ? 1.6F : 1.3F;
        event.setDroppedExperience(Math.round(event.getDroppedExperience() * mult));
    }

    // ───────── 곡괭이 · 삽 · 괭이 ─────────
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(event.getBreaker() instanceof Player)) return;
        BlockState state = event.getState();
        RandomSource r = event.getLevel().getRandom();
        BlockPos pos = event.getPos();
        if (is(tool, EMERALD_PICKAXE, ROYAL_EMERALD_PICKAXE) && state.is(Tags.Blocks.ORES)
                && r.nextFloat() < (tool.is(ROYAL_EMERALD_PICKAXE.get()) ? 0.35F : 0.2F)) {
            for (ItemEntity e : event.getDrops()) {
                if (!e.getItem().is(state.getBlock().asItem())) {
                    e.getItem().grow(1);
                    break;
                }
            }
            event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        } else if (is(tool, EMERALD_SHOVEL, ROYAL_EMERALD_SHOVEL)
                && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL))) {
            float chance = tool.is(ROYAL_EMERALD_SHOVEL.get()) ? 0.04F : 0.02F;
            if (r.nextFloat() < chance) {
                float k = r.nextFloat();
                ItemStack t = k < 0.5F ? new ItemStack(Items.GOLD_NUGGET, 2 + r.nextInt(4))
                        : k < 0.92F ? new ItemStack(Items.EMERALD) : new ItemStack(Items.DIAMOND);
                event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, t));
                event.getLevel().sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
            }
        } else if (is(tool, EMERALD_HOE, ROYAL_EMERALD_HOE) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
                && r.nextFloat() < (tool.is(ROYAL_EMERALD_HOE.get()) ? 0.5F : 0.3F)) {
            for (ItemEntity e : event.getDrops()) if (!e.getItem().is(crop.asItem())) e.getItem().grow(e.getItem().getCount());
            event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        }
    }
}
