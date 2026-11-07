package com.snolred.wildplus.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.UseItemGoal;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * 동전으로 거래하는 상인. 바닐라 떠돌이 상인의 거래 화면을 그대로 쓴다.
 * - 사라지지 않음, 밤에 투명화 물약을 마시지 않음
 * - 하루(24000틱)마다 거래 횟수가 초기화(재입고)
 */
public class MerchantEntity extends WanderingTrader {
    public enum Kind { GENERAL, BLACKSMITH, FARMER, FISHERMAN, BANKER }

    private final Kind kind;

    public MerchantEntity(EntityType<? extends WanderingTrader> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.setDespawnDelay(0);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof UseItemGoal);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }
        this.offers.addAll(MerchantTrades.build(kind));
    }

    /** 업데이트로 품목이 바뀌어도 예전 상인이 새 목록을 쓰도록, 불러온 뒤 한 번 · 매일 아침 목록을 새로 만든다 */
    private boolean refreshed;

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        // 명예의 전당 "상인 호출"로 온 상인은 10분 뒤 떠난다
        long leave = this.getPersistentData().getLongOr(com.snolred.wildplus.economy.Honor.LEAVE_AT, 0L);
        if (leave > 0 && this.level().getGameTime() > leave) {
            this.discard();
            return;
        }
        if ((!refreshed || this.level().getGameTime() % 24000L == 0L) && baseOffers == null) {
            refreshed = true;
            this.offers = new MerchantOffers();
            this.offers.addAll(MerchantTrades.build(kind));
        }
    }

    // ───────── 3.8 마을의 영웅 할인 ─────────
    /** 거래하는 동안만 할인된 사본으로 바꿔 끼우고, 끝나면 쓴 횟수를 원본에 돌려준다 */
    private MerchantOffers baseOffers;

    @Override
    public void setTradingPlayer(net.minecraft.world.entity.player.Player player) {
        if (player == null && baseOffers != null && this.offers != null) {
            for (int i = 0; i < Math.min(baseOffers.size(), this.offers.size()); i++) {
                var b = baseOffers.get(i);
                int extra = this.offers.get(i).getUses() - b.getUses();
                for (int k = 0; k < extra; k++) b.increaseUses();
            }
            this.offers = baseOffers;
            baseOffers = null;
        }
        super.setTradingPlayer(player);
        if (player != null && !this.level().isClientSide() && baseOffers == null) {
            int pct = com.snolred.wildplus.omen.Discounts.pct(player, this.blockPosition());
            if (pct > 0) {
                MerchantOffers base = this.getOffers();
                MerchantOffers disc = new MerchantOffers();
                for (var o : base) disc.add(discounted(o, pct));
                baseOffers = base;
                this.offers = disc;
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal("🏅 마을의 영웅 할인 " + pct + "%")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }
        }
    }

    /** 동전으로 사는 물건만 할인 (상인이 동전을 주는 매입은 그대로) */
    private static net.minecraft.world.item.trading.MerchantOffer discounted(net.minecraft.world.item.trading.MerchantOffer o, int pct) {
        long a = com.snolred.wildplus.economy.Coins.unitValue(o.getBaseCostA()) * (long) o.getBaseCostA().getCount();
        long b = o.getItemCostB().map(c -> (long) com.snolred.wildplus.economy.Coins.unitValue(c.itemStack()) * c.count()).orElse(0L);
        boolean coinA = com.snolred.wildplus.economy.Coins.unitValue(o.getBaseCostA()) > 0;
        boolean coinB = o.getItemCostB().isEmpty() || com.snolred.wildplus.economy.Coins.unitValue(o.getItemCostB().get().itemStack()) > 0;
        if (!coinA || !coinB) return o.copy();
        long price = com.snolred.wildplus.omen.Discounts.apply(a + b, pct);
        java.util.List<net.minecraft.world.item.ItemStack> coins = com.snolred.wildplus.economy.Coins.toCoins(price);
        if (coins.isEmpty()) return o.copy(); // 동전 종류가 셋 이상이면 큰 두 종류만 (조금 더 깎아 줌)
        var ca = new net.minecraft.world.item.trading.ItemCost(coins.get(0).getItem(), coins.get(0).getCount());
        java.util.Optional<net.minecraft.world.item.trading.ItemCost> cb = coins.size() > 1
                ? java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(coins.get(1).getItem(), coins.get(1).getCount())) : java.util.Optional.empty();
        return new net.minecraft.world.item.trading.MerchantOffer(ca, cb, o.getResult().copy(), o.getUses(), o.getMaxUses(), o.getXp(), o.getPriceMultiplier());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
