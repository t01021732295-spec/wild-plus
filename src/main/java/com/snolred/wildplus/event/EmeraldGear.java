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
 * 3.6 로열 에메랄드 상향 (얻기 어려워진 만큼): 검 약탈 80% · 생명 수확 4 · 흡혈 15% / 곡괭이 65% · 성급함 II 10초 /
 * 도끼 3배 · 처형 1.8배 / 삽 10% / 괭이 100% · 13×13 / 활 회복 4 · 재생 II /
 * 2부위 + 포만 (10초마다 배고픔 1 회복) / 4부위 체력 증가 IV · 저항 I · 생명의 가호 30초마다 ·
 * 생명의 오라 (4초마다 8칸 안 다른 플레이어 체력 2 회복) · 불멸의 맹세 (죽을 피해를 받으면 체력 절반으로 버팀 · 10분마다).
 *
 * 에메랄드 장비 (풍요 · 행운 · 생명). 로열 에메랄드는 같은 능력이 훨씬 강하고 추가 능력이 붙으며 내구도 무한 · 불에 안 탐.
 *  - 검 "약탈": 처치하면 30%(로열 70%) 확률로 전리품 두 배, 경험치 1.3배(로열 2배)
 *        로열 추가 "생명 수확": 처치할 때마다 체력 2 회복 · 20% 확률로 에메랄드 1~2개
 *  - 곡괭이 "행운의 손길": 광석 20%(로열 50%) 확률로 전리품 하나 더 · 로열 추가 "광맥의 축복": 광석 캐면 성급함 II 6초
 *  - 도끼 "선제공격": 체력이 가득한 적에게 1.8배(로열 2.6배) · 로열 추가 "처형": 체력 30% 이하인 적에게 1.5배
 *  - 삽 "보물 찾기": 흙·모래·자갈에서 2%(로열 7%) 확률로 에메랄드 · 금 조각 · 가끔 다이아몬드(로열은 다이아 확률 2배)
 *  - 괭이 "풍작": 다 자란 작물 30%(로열 75%) 확률로 수확량 두 배 · 로열 추가 "대지의 축복": 손에 들면 주변 9×9 작물이 빨리 자람
 *  - 활 "생명의 화살": 맞히면 내 체력 1(로열 3 + 재생 + 흡수) 회복
 *  - 갑옷 2부위: 행운 I · 마을의 영웅 I (로열: 행운 II · 마을의 영웅 II · 재생 I)
 *          4부위: 체력 증가 I(+2하트) · 받는 피해 10% 감소 (로열: 체력 증가 III(+6하트) · 저항 I)
 *          로열 4부위 추가 "생명의 가호": 체력이 30% 아래로 떨어지면 흡수 III 10초 + 재생 III 5초 (40초마다)
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
        if (royal >= 2) {
            p.addEffect(fx(MobEffects.REGENERATION, 0));
            if (p.tickCount % 200 == 0) p.getFoodData().eat(1, 1.0F); // 포만
        }
        if (n >= 4) {
            // 체력 증가는 효과가 끊겼다 다시 붙으면 체력이 깎이므로 길게 유지
            MobEffectInstance hb = p.getEffect(MobEffects.HEALTH_BOOST);
            int amp = royal >= 4 ? 3 : 0;
            if (hb == null || hb.getAmplifier() != amp || hb.getDuration() < 200) {
                p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 400, amp, true, false, true));
            }
        }
        if (royal >= 4) {
            p.addEffect(fx(MobEffects.RESISTANCE, 0));
            // 생명의 오라: 4초마다 주변 8칸 다른 플레이어 체력 2 회복
            if (p.tickCount % 80 == 0) {
                ServerLevel sl0 = (ServerLevel) p.level();
                for (Player o : sl0.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8.0))) {
                    if (o == p || !o.isAlive() || o.getHealth() >= o.getMaxHealth()) continue;
                    o.heal(2.0F);
                    sl0.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX(), o.getY() + 1.2, o.getZ(), 6, 0.3, 0.4, 0.3, 0);
                }
            }
        }
        // 대지의 축복: 로열 괭이를 들면 주변 작물이 빨리 자람
        if (p.getMainHandItem().is(ROYAL_EMERALD_HOE.get()) || p.getOffhandItem().is(ROYAL_EMERALD_HOE.get())) {
            ServerLevel sl = (ServerLevel) p.level();
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-6, -2, -6), c.offset(6, 2, 6))) {
                BlockState st = sl.getBlockState(q);
                if (st.getBlock() instanceof CropBlock crop && !crop.isMaxAge(st) && p.getRandom().nextFloat() < 0.35F) {
                    st.randomTick(sl, q.immutable(), p.getRandom());
                    if (p.getRandom().nextFloat() < 0.3F)
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.getX() + 0.5, q.getY() + 0.6, q.getZ() + 0.5, 1, 0.2, 0.1, 0.2, 0);
                }
            }
        }
    }

    /** 생명의 가호 (로열 4부위): 체력이 30% 아래로 떨어지면 */
    private static final String GUARD_AT = "wildplus_royal_guard";

    @SubscribeEvent
    public static void onHurtPost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player p) || !(p.level() instanceof ServerLevel sl)) return;
        if (p.getHealth() <= 0 || p.getHealth() > p.getMaxHealth() * 0.3F || royalPieces(p) < 4) return;
        long now = sl.getGameTime();
        var tag = p.getPersistentData();
        if (now - tag.getLongOr(GUARD_AT, -10000L) < 600) return;
        tag.putLong(GUARD_AT, now);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0);
        sl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p
                && is(p.getMainHandItem(), EMERALD_AXE, ROYAL_EMERALD_AXE) && target.getHealth() >= target.getMaxHealth()) {
            event.setAmount(event.getAmount() * (p.getMainHandItem().is(ROYAL_EMERALD_AXE.get()) ? 3.0F : 1.8F));
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0);
            }
        } else if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p
                && p.getMainHandItem().is(ROYAL_EMERALD_AXE.get()) && target.getHealth() <= target.getMaxHealth() * 0.3F) {
            // 처형
            event.setAmount(event.getAmount() * 1.8F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            }
        }
        if (target instanceof Player p && pieces(p) >= 4 && royalPieces(p) < 4) event.setAmount(event.getAmount() * 0.9F);
    }

    /** 로열 검 흡혈: 근접 피해의 15% 회복 */
    @SubscribeEvent
    public static void onSwordHit(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p
                && p.getMainHandItem().is(ROYAL_EMERALD_SWORD.get()) && event.getEntity() != p) {
            p.heal(event.getHealthDamage() * 0.15F);
        }
    }

    /** 불멸의 맹세 (로열 4부위): 죽을 피해를 받으면 체력 절반으로 버팀 · 10분(12000틱)마다 */
    private static final String VOW_AT = "wildplus_royal_vow";

    @SubscribeEvent
    public static void onDeathVow(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player p) || !(p.level() instanceof ServerLevel sl) || royalPieces(p) < 4) return;
        long now = sl.getGameTime();
        var tag = p.getPersistentData();
        if (now - tag.getLongOr(VOW_AT, -100000L) < 12000) return;
        tag.putLong(VOW_AT, now);
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth() * 0.5F);
        p.removeEffect(MobEffects.WITHER);
        p.removeEffect(MobEffects.POISON);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
        sl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("💚 불멸의 맹세! 로열 에메랄드가 죽음을 막았어요 (10분 뒤 다시)")
                .withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
    }

    /** 생명의 화살 */
    @SubscribeEvent
    public static void onArrowHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow a) || !(a.getOwner() instanceof Player owner)) return;
        String bow = a.getPersistentData().getStringOr(Bows.BOW_TAG, "");
        if (bow.equals("emerald")) owner.heal(1.0F);
        else if (bow.equals("royal_emerald")) {
            owner.heal(4.0F);
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        } else return;
        if (owner.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2, owner.getZ(), 1, 0.2, 0.1, 0.2, 0);
    }

    // ───────── 검: 약탈 ─────────
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || event.getEntity() instanceof Player) return;
        ItemStack hand = p.getMainHandItem();
        if (!is(hand, EMERALD_SWORD, ROYAL_EMERALD_SWORD)) return;
        boolean royalSword = hand.is(ROYAL_EMERALD_SWORD.get());
        if (royalSword) {
            // 생명 수확
            p.heal(4.0F);
            if (p.getRandom().nextFloat() < 0.35F) {
                LivingEntity v = event.getEntity();
                event.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), new ItemStack(Items.EMERALD, 1 + p.getRandom().nextInt(2))));
            }
        }
        float chance = royalSword ? 0.8F : 0.3F;
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
        float mult = hand.is(ROYAL_EMERALD_SWORD.get()) ? 2.0F : 1.3F;
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
        if (tool.is(ROYAL_EMERALD_PICKAXE.get()) && state.is(Tags.Blocks.ORES) && event.getBreaker() instanceof Player miner) {
            miner.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1, true, false, true)); // 광맥의 축복
        }
        if (is(tool, EMERALD_PICKAXE, ROYAL_EMERALD_PICKAXE) && state.is(Tags.Blocks.ORES)
                && r.nextFloat() < (tool.is(ROYAL_EMERALD_PICKAXE.get()) ? 0.65F : 0.2F)) {
            for (ItemEntity e : event.getDrops()) {
                if (!e.getItem().is(state.getBlock().asItem())) {
                    e.getItem().grow(1);
                    break;
                }
            }
            event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        } else if (is(tool, EMERALD_SHOVEL, ROYAL_EMERALD_SHOVEL)
                && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL))) {
            boolean royalShovel = tool.is(ROYAL_EMERALD_SHOVEL.get());
            float chance = royalShovel ? 0.10F : 0.02F;
            if (r.nextFloat() < chance) {
                float k = r.nextFloat();
                ItemStack t = k < 0.5F ? new ItemStack(Items.GOLD_NUGGET, 2 + r.nextInt(4))
                        : k < (royalShovel ? 0.84F : 0.92F) ? new ItemStack(Items.EMERALD) : new ItemStack(Items.DIAMOND);
                event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, t));
                event.getLevel().sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
            }
        } else if (is(tool, EMERALD_HOE, ROYAL_EMERALD_HOE) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
                && r.nextFloat() < (tool.is(ROYAL_EMERALD_HOE.get()) ? 1.0F : 0.3F)) {
            for (ItemEntity e : event.getDrops()) if (!e.getItem().is(crop.asItem())) e.getItem().grow(e.getItem().getCount());
            event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        }
    }
}
