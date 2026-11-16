package com.snolred.wildplus.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.2.1 렉 줄이기: 5초마다 땅에 많이 쌓인 것들을 합친다.
 * · 떨어진 아이템: 같은 아이템이면 반경 3칸 안에서 한 묶음으로 (바닐라는 0.5칸)
 * · 경험치 구슬: 반경 3칸 안의 구슬을 하나로 (값은 합침)
 * 몹 농장 · 레이드 팜 · 자동 낚시 근처에서 엔티티 수가 크게 줄어든다.
 */
public final class LagGuard {
    private LagGuard() {}

    private static final double R = 3.0;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 100 != 23 || server.getPlayerList().getPlayers().isEmpty()) return;
        for (ServerLevel lv : server.getAllLevels()) merge(lv);
    }

    private static long cell(Entity e) {
        long x = (long) Math.floor(e.getX() / R), y = (long) Math.floor(e.getY() / R), z = (long) Math.floor(e.getZ() / R);
        return (x & 0x1FFFFF) | ((y & 0xFFFFF) << 21) | ((z & 0x1FFFFF) << 41);
    }

    private static void merge(ServerLevel lv) {
        Map<Long, List<ItemEntity>> items = new HashMap<>();
        Map<Long, ExperienceOrb> orbs = new HashMap<>();
        List<Entity> all = new ArrayList<>();
        for (Entity e : lv.getAllEntities()) all.add(e);
        for (Entity e : all) {
            if (e.isRemoved()) continue;
            if (e instanceof ExperienceOrb orb) {
                long c = cell(orb);
                ExperienceOrb keep = orbs.get(c);
                if (keep == null || keep.isRemoved()) {
                    orbs.put(c, orb);
                } else {
                    int sum = keep.getValue() + orb.getValue();
                    if (sum > 0 && sum < 30_000) {
                        orb.discard();
                        keep.discard();
                        ExperienceOrb merged = new ExperienceOrb(lv, keep.getX(), keep.getY(), keep.getZ(), sum);
                        lv.addFreshEntity(merged);
                        orbs.put(c, merged);
                    }
                }
            } else if (e instanceof ItemEntity it && !it.hasPickUpDelay() && !it.getItem().isEmpty()) {
                items.computeIfAbsent(cell(it), k -> new ArrayList<>()).add(it);
            }
        }
        for (List<ItemEntity> group : items.values()) {
            if (group.size() < 2) continue;
            for (int i = 0; i < group.size(); i++) {
                ItemEntity a = group.get(i);
                if (a.isRemoved()) continue;
                ItemStack sa = a.getItem();
                for (int j = i + 1; j < group.size() && sa.getCount() < sa.getMaxStackSize(); j++) {
                    ItemEntity b = group.get(j);
                    if (b.isRemoved()) continue;
                    ItemStack sb = b.getItem();
                    if (!ItemStack.isSameItemSameComponents(sa, sb)) continue;
                    int move = Math.min(sa.getMaxStackSize() - sa.getCount(), sb.getCount());
                    if (move <= 0) continue;
                    ItemStack na = sa.copyWithCount(sa.getCount() + move);
                    a.setItem(na);
                    sa = na;
                    if (sb.getCount() == move) b.discard();
                    else b.setItem(sb.copyWithCount(sb.getCount() - move));
                }
            }
        }
    }
}
