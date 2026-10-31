package com.snolred.wildplus.economy;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.event.BloodLord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 명예의 전당 (돈으로 하는 엔드 컨텐츠).
 * - 명예 등급 5단계: 브론즈 → 실버 → 골드 → 다이아 → 레전드. 영구 혜택: 최대 체력 +2하트/등급, 세금 10%/등급 감면,
 *   은행 이자 +0.2%p/등급(상한도 늘어남), 이름 앞 칭호. 레전드는 비행 허가증 반값.
 * - 비행 허가증: 30분 동안 서바이벌에서 날 수 있음 (끝나면 느린 낙하).
 * - 보스 소환권: 핏빛 군주를 바로 내 앞에 소환.
 * - 서버 축제 후원: 1시간 동안 접속한 모두에게 신속·성급함·행운.
 * 결제는 은행 통장에서 먼저, 모자라면 가진 동전으로. 쓴 돈은 국고로 들어간다.
 */
public final class Honor {
    public static final long[] RANK_COST = {2_000, 8_000, 25_000, 80_000, 250_000};
    public static final String[] RANK_NAME = {"없음", "브론즈", "실버", "골드", "다이아", "레전드"};
    public static final int[] RANK_COLOR = {0xFFAAAAAA, 0xFFCD7F32, 0xFFC0C8D0, 0xFFFFC83D, 0xFF5FE3FF, 0xFFFF5AE0};
    public static final long FLIGHT_COST = 500, BOSS_COST = 3_000, FESTIVAL_COST = 5_000;
    public static final long FLIGHT_TICKS = 20L * 60 * 30, FESTIVAL_TICKS = 20L * 60 * 60;
    public static final int BTN_RANK = 0, BTN_FLIGHT = 1, BTN_BOSS = 2, BTN_FESTIVAL = 3;
    // 11차 특별 구매
    public static final long HEAL_COST = 300, XP_COST = 800, MERCHANT_COST = 1_000, MAP_COST = 1_500, GUARD_COST = 2_000;
    public static final int BTN_HEAL = 4, BTN_XP = 5, BTN_MERCHANT = 6, BTN_MAP = 7, BTN_GUARD = 8;
    /** 등급마다 새로 생기는 혜택 (누적) */
    public static final String[] PERK = {"", "1분마다 배고픔 회복", "낙하 피해 절반", "몬스터 처치 시 동전",
            "상시 성급함 I", "상시 신속 I · 받는 피해 -10% · 비행 반값"};
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> TREASURE_SITES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, WildPlus.id("treasure_sites"));
    public static final String LEAVE_AT = "wildplus_leave_at";

    private Honor() {}

    public static int taxCut(int rank) {
        return 10 * rank;
    }

    public static long flightCost(int rank) {
        return rank >= 5 ? FLIGHT_COST / 2 : FLIGHT_COST;
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** 통장 + 현금 */
    public static long wealth(MinecraftServer server, Player p) {
        return WildData.get(server).balance(p.getUUID()) + Coins.totalValue(p);
    }

    /** 통장에서 먼저, 모자라면 동전으로 결제. 성공하면 국고로 */
    public static boolean pay(ServerPlayer p, long amount) {
        MinecraftServer server = p.level().getServer();
        WildData data = WildData.get(server);
        long bank = data.balance(p.getUUID());
        if (bank + Coins.totalValue(p) < amount) return false;
        long fromBank = Math.min(bank, amount);
        long rest = amount - fromBank;
        if (rest > 0 && !Coins.pay(p, rest)) return false;
        data.addBalance(p.getUUID(), -fromBank);
        data.treasury += amount;
        data.setDirty();
        return true;
    }

    private static void fail(ServerPlayer p, long cost) {
        p.sendSystemMessage(Component.translatable("message.wildplus.honor.no_money", String.format("%,d", cost)).withStyle(ChatFormatting.RED));
    }

    /** 명예의 전당 버튼 처리 */
    public static void press(ServerPlayer p, int id) {
        MinecraftServer server = p.level().getServer();
        WildData data = WildData.get(server);
        int rank = data.honor(p.getUUID());
        switch (id) {
            case BTN_RANK -> {
                if (rank >= 5) return;
                long cost = RANK_COST[rank];
                if (!pay(p, cost)) {
                    fail(p, cost);
                    return;
                }
                data.honor.put(p.getUUID(), rank + 1);
                data.setDirty();
                applyHealth(p, rank + 1);
                p.refreshDisplayName();
                p.refreshTabListName();
                server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.honor.rank_up", p.getName(),
                        title(rank + 1)).withStyle(ChatFormatting.GOLD), false);
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case BTN_FLIGHT -> {
                long cost = flightCost(rank);
                if (!pay(p, cost)) {
                    fail(p, cost);
                    return;
                }
                long base = Math.max(now(server), data.flightUntil.getOrDefault(p.getUUID(), 0L));
                data.flightUntil.put(p.getUUID(), base + FLIGHT_TICKS);
                data.setDirty();
                p.sendSystemMessage(Component.translatable("message.wildplus.honor.flight", (base + FLIGHT_TICKS - now(server)) / 20 / 60)
                        .withStyle(ChatFormatting.AQUA));
                p.level().playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4F, 1.4F);
            }
            case BTN_BOSS -> {
                if (p.level().dimension() != Level.OVERWORLD) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.honor.boss_overworld").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!pay(p, BOSS_COST)) {
                    fail(p, BOSS_COST);
                    return;
                }
                p.closeContainer();
                BloodLord.spawn((ServerLevel) p.level(), p);
            }
            case BTN_FESTIVAL -> {
                if (!pay(p, FESTIVAL_COST)) {
                    fail(p, FESTIVAL_COST);
                    return;
                }
                data.festivalUntil = Math.max(now(server), data.festivalUntil) + FESTIVAL_TICKS;
                data.festivalBy = p.getName().getString();
                data.setDirty();
                server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.honor.festival", p.getName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
                for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                    o.level().playSound(null, o.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            case BTN_HEAL -> {
                if (!pay(p, HEAL_COST)) {
                    fail(p, HEAL_COST);
                    return;
                }
                p.setHealth(p.getMaxHealth());
                p.getFoodData().eat(20, 1.0F);
                for (var inst : List.copyOf(p.getActiveEffects())) {
                    if (inst.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) p.removeEffect(inst.getEffect());
                }
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
                p.sendSystemMessage(Component.translatable("message.wildplus.honor.heal").withStyle(ChatFormatting.GREEN));
            }
            case BTN_XP -> {
                if (!pay(p, XP_COST)) {
                    fail(p, XP_COST);
                    return;
                }
                p.giveExperienceLevels(5);
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            case BTN_MERCHANT -> {
                if (p.level().dimension() != Level.OVERWORLD) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.honor.boss_overworld").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!pay(p, MERCHANT_COST)) {
                    fail(p, MERCHANT_COST);
                    return;
                }
                var kinds = List.of(com.snolred.wildplus.registry.ModEntities.MERCHANT.get(), com.snolred.wildplus.registry.ModEntities.BLACKSMITH_MERCHANT.get(),
                        com.snolred.wildplus.registry.ModEntities.FARMER_MERCHANT.get(), com.snolred.wildplus.registry.ModEntities.FISHER_MERCHANT.get(),
                        com.snolred.wildplus.registry.ModEntities.BANKER_MERCHANT.get());
                var m = kinds.get(p.getRandom().nextInt(kinds.size())).create(p.level(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (m != null) {
                    var dir = p.getLookAngle();
                    m.snapTo(p.getX() + dir.x * 2, p.getY(), p.getZ() + dir.z * 2, p.getYRot() + 180F, 0F);
                    m.getPersistentData().putLong(LEAVE_AT, now(server) + 20L * 60 * 10);
                    p.level().addFreshEntity(m);
                    p.sendSystemMessage(Component.translatable("message.wildplus.honor.merchant", m.getName()).withStyle(ChatFormatting.YELLOW));
                }
            }
            case BTN_MAP -> {
                ServerLevel level = (ServerLevel) p.level();
                if (level.dimension() != Level.OVERWORLD) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.honor.boss_overworld").withStyle(ChatFormatting.RED));
                    return;
                }
                if (Coins.totalValue(p) + data.balance(p.getUUID()) < MAP_COST) {
                    fail(p, MAP_COST);
                    return;
                }
                net.minecraft.core.BlockPos found = level.findNearestMapStructure(TREASURE_SITES, p.blockPosition(), 64, false);
                if (found == null) {
                    p.sendSystemMessage(Component.translatable("message.wildplus.honor.map_none").withStyle(ChatFormatting.GRAY));
                    return;
                }
                if (!pay(p, MAP_COST)) {
                    fail(p, MAP_COST);
                    return;
                }
                int dist = (int) Math.sqrt(found.distSqr(new net.minecraft.core.BlockPos(p.getBlockX(), found.getY(), p.getBlockZ())));
                p.sendSystemMessage(Component.translatable("message.wildplus.honor.map", found.getX(), found.getZ(), dist).withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case BTN_GUARD -> {
                if (!pay(p, GUARD_COST)) {
                    fail(p, GUARD_COST);
                    return;
                }
                int t = 20 * 60 * 30;
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, t, 0));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, t, 0));
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, t, 0));
                p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
                p.sendSystemMessage(Component.translatable("message.wildplus.honor.guard").withStyle(ChatFormatting.AQUA));
            }
            default -> {}
        }
    }

    // ───────── 등급 혜택 (피해 · 드롭) ─────────
    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        int rank = WildData.get(p.level().getServer()).honor(p.getUUID());
        if (rank <= 0) return;
        float amount = event.getAmount();
        if (rank >= 2 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) amount *= 0.5F;
        if (rank >= 5) amount *= 0.9F;
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onKillDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster m) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (WildData.get(p.level().getServer()).honor(p.getUUID()) < 3) return;
        var it = new net.minecraft.world.entity.item.ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(),
                new net.minecraft.world.item.ItemStack(com.snolred.wildplus.registry.ModItems3.COPPER_COIN.get(), 1 + p.getRandom().nextInt(3)));
        event.getDrops().add(it);
    }

    public static MutableComponent title(int rank) {
        return Component.literal("[" + RANK_NAME[rank] + "]").withColor(RANK_COLOR[rank] & 0xFFFFFF);
    }

    private static void applyHealth(Player p, int rank) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        var id = WildPlus.id("honor_health");
        AttributeModifier cur = a.getModifier(id);
        double want = 4.0 * rank;
        if (cur != null && cur.amount() == want) return;
        a.removeModifier(id);
        if (want > 0) a.addPermanentModifier(new AttributeModifier(id, want, AttributeModifier.Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ───────── 매 틱 혜택 ─────────
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        MinecraftServer server = p.level().getServer();
        WildData data = WildData.get(server);
        UUID id = p.getUUID();
        int rank = data.honor(id);
        if (rank >= 1 && p.tickCount % 1200 == 0 && p.getFoodData().getFoodLevel() < 20) p.getFoodData().eat(1, 0.3F);
        if (p.tickCount % 100 == 0) {
            if (rank >= 4) p.addEffect(new MobEffectInstance(MobEffects.HASTE, 140, 0, true, false, true));
            if (rank >= 5) p.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
            applyHealth(p, data.honor(id));
            String name = p.getName().getString();
            if (!name.equals(data.names.get(id))) {
                data.names.put(id, name);
                data.setDirty();
            }
        }
        long now = now(server);
        // 비행 허가증
        Long until = data.flightUntil.get(id);
        if (until != null && !p.isCreative() && !p.isSpectator()) {
            long left = until - now;
            if (left > 0) {
                if (!p.getAbilities().mayfly) {
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                }
                if (left / 10 == 60 * 2 || left / 10 == 10 * 2) {
                    p.sendOverlayMessage(Component.translatable("message.wildplus.honor.flight_left", left / 20).withStyle(ChatFormatting.YELLOW));
                }
            } else {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 15, 0));
                p.sendSystemMessage(Component.translatable("message.wildplus.honor.flight_end").withStyle(ChatFormatting.YELLOW));
                data.flightUntil.remove(id);
                data.setDirty();
            }
        }
        // 서버 축제
        if (data.festivalUntil > now && p.tickCount % 100 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.HASTE, 140, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.LUCK, 140, 0, true, false, true));
        }
    }

    // ───────── 이름 앞 칭호 ─────────
    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            int rank = WildData.get(p.level().getServer()).honor(p.getUUID());
            if (rank > 0) event.setDisplayname(Component.empty().append(title(rank)).append(" ").append(event.getDisplayname()));
        }
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            int rank = WildData.get(p.level().getServer()).honor(p.getUUID());
            if (rank > 0) event.setDisplayName(Component.empty().append(title(rank)).append(" ").append(p.getName()));
        }
    }

    // ───────── 부자 순위 ─────────
    public record Entry(String name, long wealth, int rank) {}

    public static List<Entry> ranking(MinecraftServer server, int limit) {
        WildData data = WildData.get(server);
        Map<UUID, Long> total = new HashMap<>(data.bank);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) total.merge(p.getUUID(), Coins.totalValue(p), Long::sum);
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(total.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<Entry> out = new ArrayList<>();
        for (var e : list) {
            if (out.size() >= limit) break;
            String name = data.names.getOrDefault(e.getKey(), "(알 수 없음)");
            ServerPlayer online = server.getPlayerList().getPlayer(e.getKey());
            if (online != null) name = online.getName().getString();
            out.add(new Entry(name, e.getValue(), data.honor(e.getKey())));
        }
        return out;
    }
}
