package com.snolred.wildplus.event;

import com.snolred.wildplus.item.LuckyCharmItem;
import com.snolred.wildplus.registry.ModItems3;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.9 행운의 부적 단계별 효과 (가방 속 가장 높은 단계 하나만, 아래 단계 효과는 모두 포함):
 * 1 행운 I · 2 행운 II · 3 경험치 +20% · 4 행운 III + 몬스터 동전 +50% · 5 행운의 일격 (7% 피해 2배)
 * 6 행운 IV + 광석 두 배 (8%) · 7 물고기 두 배 (15%) · 8 행운 V + 기적 (죽을 때 한 번 버팀, 10분)
 * 9 정예 보상 두 배 · 10 행운 VI + 황금 오라 (32칸 안 친구 행운 II)
 */
public final class LuckyCharms {
    private LuckyCharms() {}

    public static final String MIRACLE_AT = "wildplus_charm_miracle";
    static final int[] LUCK = {-1, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5};

    /** 가방(손 · 갑옷 칸 포함)에서 가장 높은 부적 단계, 없으면 0 */
    public static int tier(Player p) {
        Inventory inv = p.getInventory();
        int best = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) best = Math.max(best, LuckyCharmItem.tierOf(inv.getItem(i)));
        return best;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            int t = tier(p);
            if (t <= 0) continue;
            p.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 4, LUCK[t], true, false, true));
            if (t >= 10) {
                for (Player o : p.level().players()) {
                    if (o != p && o.distanceToSqr(p) < 32 * 32 && tier(o) < 10) {
                        o.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 4, Math.max(1, LUCK[tier(o)]), true, false, true));
                    }
                }
                if (event.getServer().getTickCount() % 200 == 0) {
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 6, 0.6, 0.8, 0.6, 0.02);
                }
            }
        }
    }

    // 3단계: 경험치 +20%
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        if (event.getAmount() > 0 && tier(event.getEntity()) >= 3) event.setAmount(Math.round(event.getAmount() * 1.2F));
    }

    // 4단계: 몬스터 동전 +50% / 9단계: 정예 보상 두 배
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || !(event.getEntity() instanceof Monster m)) return;
        int t = tier(p);
        if (t < 4) return;
        boolean elite = t >= 9 && EliteMobs.isElite(m);
        for (ItemEntity it : event.getDrops()) {
            ItemStack s = it.getItem();
            if (s.isDamageableItem() || s.getMaxStackSize() <= 1) continue;
            boolean coin = s.is(ModItems3.COPPER_COIN.get()) || s.is(ModItems3.SILVER_COIN.get()) || s.is(ModItems3.GOLD_COIN.get())
                    || s.is(ModItems3.DIAMOND_COIN.get());
            int n = s.getCount();
            if (coin) n += Math.max(1, n / 2);
            if (elite) n *= 2;
            if (n != s.getCount()) s.setCount(Math.min(s.getMaxStackSize(), n));
        }
    }

    // 5단계: 행운의 일격
    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() instanceof Player) return;
        if (tier(p) < 5 || p.getRandom().nextFloat() >= 0.07F) return;
        event.setAmount(event.getAmount() * 2F);
        var e = event.getEntity();
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.7, e.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        p.level().playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.4F);
        p.sendOverlayMessage(Component.literal("🍀 행운의 일격! 피해 2배").withStyle(ChatFormatting.GREEN));
    }

    // 6단계: 광석 두 배
    @SubscribeEvent
    public static void onOre(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player p) || !event.getState().is(Tags.Blocks.ORES) || event.getDrops().isEmpty()) return;
        if (tier(p) < 6 || p.getRandom().nextFloat() >= 0.08F) return;
        ItemStack first = event.getDrops().get(0).getItem();
        if (first.is(event.getState().getBlock().asItem())) return; // 섬세한 손길로 광석 블록째 캔 건 제외
        var pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, first.copy()));
        p.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    // 8단계: 기적 — 죽을 때 한 번 버팀 (10분)
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p) || tier(p) < 8) return;
        if (com.snolred.wildplus.content.Arena.inMatch(p.getUUID())) return;
        long now = p.level().getGameTime();
        var d = p.getPersistentData();
        if (now - d.getLongOr(MIRACLE_AT, -100000L) < 12000) return;
        event.setCanceled(true);
        d.putLong(MIRACLE_AT, now);
        p.setHealth(p.getMaxHealth() * 0.4F);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 2));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 50, 0.5, 1, 0.5, 0.3);
        p.level().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        p.sendSystemMessage(Component.literal("🍀 행운의 부적이 기적을 일으켰어요! (10분 뒤 다시)").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
    }

    /** 7단계: 물고기 두 배 확률 (FishingLootModifier 에서 사용) */
    public static boolean doubleFish(Player p) {
        return tier(p) >= 7 && p.getRandom().nextFloat() < 0.15F;
    }
}
