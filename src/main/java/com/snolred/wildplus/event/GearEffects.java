package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModItems4;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 모드 장비 효과.
 *
 * 갑옷 세트 (2부위 / 4부위)
 *  - 핏빛 강철: 밤에 힘 I / + 저항 I, 밤 몬스터 피해 25% 감소
 *  - 미스릴: 성급함 I / 성급함 II + 신속 I
 *  - 비취: 행운 I / 행운 II + 얻는 경험치 1.5배
 *
 * 도구
 *  - 핏빛 강철 검: 흡혈 20% · 곡괭이·도끼·삽: 밤(블러드문)에 채굴 속도 1.5배
 *  - 미스릴 검: 20% 확률로 둔화 II · 곡괭이: 원석 자동 제련 · 도끼: 나무 한 번에 베기 256개 · 삽: 3×3 파기 · 괭이: 3×3 경작
 *  - 비취 검: 몬스터 처치 시 동전 보너스 · 곡괭이: 광석 25% 두 배 · 도끼: 원목 20% 추가 + 사과
 *    삽: 흙·모래·자갈에서 가끔 보물 · 괭이: 작물 수확량 +1
 */
public final class GearEffects {
    private GearEffects() {}

    enum Set { BLOODSTEEL, MYTHRIL, JADE }

    static int pieces(Player p, Set set) {
        Item[] items = switch (set) {
            case BLOODSTEEL -> new Item[]{ModItems.BLOODSTEEL_HELMET.get(), ModItems.BLOODSTEEL_CHESTPLATE.get(),
                    ModItems.BLOODSTEEL_LEGGINGS.get(), ModItems.BLOODSTEEL_BOOTS.get()};
            case MYTHRIL -> new Item[]{ModItems4.MYTHRIL_HELMET.get(), ModItems4.MYTHRIL_CHESTPLATE.get(),
                    ModItems4.MYTHRIL_LEGGINGS.get(), ModItems4.MYTHRIL_BOOTS.get()};
            case JADE -> new Item[]{ModItems4.JADE_HELMET.get(), ModItems4.JADE_CHESTPLATE.get(),
                    ModItems4.JADE_LEGGINGS.get(), ModItems4.JADE_BOOTS.get()};
        };
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int n = 0;
        for (int i = 0; i < 4; i++) if (p.getItemBySlot(slots[i]).is(items[i])) n++;
        return n;
    }

    private static void effect(Player p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        p.addEffect(new MobEffectInstance(e, 80, amp, true, false, true));
    }

    // ───────── 갑옷 세트 ─────────
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide() || p.tickCount % 40 != 0) return;
        boolean night = p.level().isDarkOutside();
        int blood = pieces(p, Set.BLOODSTEEL);
        if (blood >= 2 && night) effect(p, MobEffects.STRENGTH, 0);
        if (blood >= 4 && night) effect(p, MobEffects.RESISTANCE, 0);
        int myth = pieces(p, Set.MYTHRIL);
        if (myth >= 2) effect(p, MobEffects.HASTE, myth >= 4 ? 1 : 0);
        if (myth >= 4) effect(p, MobEffects.SPEED, 0);
        int jade = pieces(p, Set.JADE);
        if (jade >= 2) effect(p, MobEffects.LUCK, jade >= 4 ? 1 : 0);
    }

    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        if (event.getAmount() > 0 && pieces(event.getEntity(), Set.JADE) >= 4) {
            event.setAmount(Math.round(event.getAmount() * 1.5F));
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof Monster
                && player.level().isDarkOutside() && pieces(player, Set.BLOODSTEEL) >= 4) {
            event.setAmount(event.getAmount() * 0.75F);
        }
    }

    // ───────── 검 ─────────
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()
                || event.getSource().getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.is(ModItems.BLOODSTEEL_SWORD.get()) && player.getHealth() < player.getMaxHealth()) {
            float heal = Math.min(event.getHealthDamage() * 0.2F, 3.0F);
            if (heal > 0.0F) player.heal(heal);
        }
        if (weapon.is(ModItems2.MYTHRIL_SWORD.get()) && player.getRandom().nextFloat() < 0.2F) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        }
    }

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player && event.getEntity() instanceof Monster
                && player.getMainHandItem().is(ModItems4.JADE_SWORD.get()) && player.getRandom().nextFloat() < 0.5F) {
            LivingEntity e = event.getEntity();
            event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(),
                    new ItemStack(ModItems3.COPPER_COIN.get(), 1 + player.getRandom().nextInt(3))));
        }
    }

    // ───────── 채굴 속도 ─────────
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        // 드릴은 삽으로 캐는 블록도 빠르게
        if (tool.is(com.snolred.wildplus.registry.ModItems5.DRILL.get()) && event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), com.snolred.wildplus.registry.ModItems5.DRILL_MATERIAL.speed()));
        }
        if ((tool.is(ModItems.BLOODSTEEL_PICKAXE.get()) || tool.is(ModItems.BLOODSTEEL_AXE.get()) || tool.is(ModItems.BLOODSTEEL_SHOVEL.get()))
                && event.getEntity().level().isDarkOutside()) {
            event.setNewSpeed(event.getNewSpeed() * 1.5F);
        }
    }

    // ───────── 블록 드롭 ─────────
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(event.getBreaker() instanceof Player player)) return;
        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        List<ItemEntity> drops = event.getDrops();
        var rnd = player.getRandom();
        if (tool.is(ModItems2.MYTHRIL_PICKAXE.get())) {
            for (ItemEntity e : drops) {
                ItemStack s = e.getItem();
                Item smelted = s.is(Items.RAW_IRON) ? Items.IRON_INGOT : s.is(Items.RAW_GOLD) ? Items.GOLD_INGOT
                        : s.is(Items.RAW_COPPER) ? Items.COPPER_INGOT : s.is(ModItems2.RAW_MYTHRIL.get()) ? ModItems2.MYTHRIL_INGOT.get() : null;
                if (smelted != null) e.setItem(new ItemStack(smelted, s.getCount()));
            }
        } else if (tool.is(ModItems4.JADE_PICKAXE.get())) {
            if (state.is(Tags.Blocks.ORES) && !drops.isEmpty() && rnd.nextFloat() < 0.25F) {
                add(level, pos, drops, drops.get(0).getItem().copy());
            }
        } else if (tool.is(ModItems4.JADE_AXE.get())) {
            if (state.is(BlockTags.LOGS)) {
                if (rnd.nextFloat() < 0.2F) add(level, pos, drops, new ItemStack(state.getBlock().asItem()));
                if (rnd.nextFloat() < 0.05F) add(level, pos, drops, new ItemStack(Items.APPLE));
            }
        } else if (tool.is(ModItems4.JADE_SHOVEL.get())) {
            if ((state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(net.minecraft.world.level.block.Blocks.GRAVEL))
                    && rnd.nextFloat() < 0.06F) {
                ItemStack[] treasure = {new ItemStack(Items.FLINT), new ItemStack(Items.CLAY_BALL, 2), new ItemStack(Items.WHEAT_SEEDS),
                        new ItemStack(ModItems3.COPPER_COIN.get(), 2), new ItemStack(Items.GOLD_NUGGET, 2), new ItemStack(ModItems2.JADE.get())};
                add(level, pos, drops, treasure[rnd.nextInt(treasure.length)].copy());
            }
        } else if (tool.is(ModItems4.JADE_HOE.get())) {
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Item seed = state.getBlock().asItem();
                for (ItemEntity e : drops) {
                    if (!e.getItem().is(seed)) {
                        add(level, pos, drops, e.getItem().copyWithCount(1));
                        break;
                    }
                }
            }
        }
    }

    private static void add(ServerLevel level, BlockPos pos, List<ItemEntity> drops, ItemStack stack) {
        drops.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }
}
