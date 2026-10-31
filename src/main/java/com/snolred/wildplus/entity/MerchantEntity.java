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
        if (!refreshed || this.level().getGameTime() % 24000L == 0L) {
            refreshed = true;
            this.offers = new MerchantOffers();
            this.offers.addAll(MerchantTrades.build(kind));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
