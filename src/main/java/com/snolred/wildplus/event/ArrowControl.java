package com.snolred.wildplus.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.2.1 화살 렉 줄이기
 * · 다중 발사가 높으면(운명의 제단 XV = 31발) 한 번에 최대 5발만 쏘고, 빠진 화살의 피해를 5발에 나눠 더한다 (총 피해는 같음)
 * · 회수할 수 없는 화살(다중 발사 여분 · 몬스터 화살 · 무한)이 땅에 꽂혀 멈추면 2~4초 뒤 사라짐 (바닐라는 1분)
 */
public final class ArrowControl {
    private ArrowControl() {}

    public static final int MAX_PER_SHOT = 5;
    /** 쏜 사람 → {게임 시각, 이번 발사에서 들어온 수} */
    private static final Map<UUID, long[]> SHOT = new HashMap<>();
    private static final String POS = "wp_arrow_pos";

    private static int multishot(Player p, ServerLevel lv) {
        var ms = lv.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT);
        int a = EnchantmentHelper.getItemEnchantmentLevel(ms, p.getMainHandItem());
        int b = EnchantmentHelper.getItemEnchantmentLevel(ms, p.getOffhandItem());
        return Math.max(a, b);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow a) || !(event.getLevel() instanceof ServerLevel lv)
                || !(a.getOwner() instanceof Player p)) return;
        int lvl = multishot(p, lv);
        int total = 1 + 2 * lvl;
        if (total <= MAX_PER_SHOT) return;
        long now = lv.getGameTime();
        long[] s = SHOT.computeIfAbsent(p.getUUID(), k -> new long[2]);
        if (s[0] != now) {
            s[0] = now;
            s[1] = 0;
        }
        if (++s[1] > MAX_PER_SHOT) {
            event.setCanceled(true);
            return;
        }
        a.setBaseDamage(a.getBaseDamage() * total / (double) MAX_PER_SHOT);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 0) return;
        if (SHOT.size() > 64) SHOT.clear();
        for (ServerLevel lv : server.getAllLevels()) {
            for (var e : lv.getAllEntities()) {
                if (!(e instanceof AbstractArrow a) || a.pickup == AbstractArrow.Pickup.ALLOWED || a.tickCount < 20) continue;
                var tag = a.getPersistentData();
                Vec3 pos = a.position();
                long key = Double.doubleToLongBits(pos.x) * 31 + Double.doubleToLongBits(pos.y) * 17 + Double.doubleToLongBits(pos.z);
                if (tag.getLongOr(POS, 0L) == key && a.getDeltaMovement().lengthSqr() < 0.01) a.discard();
                else tag.putLong(POS, key);
            }
        }
    }
}
