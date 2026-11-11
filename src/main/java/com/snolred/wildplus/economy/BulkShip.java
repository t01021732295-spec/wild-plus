package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 4.8 일괄 출하: 가방의 팔 수 있는 물건을 바로 판다. 장비 · 인챈트된 물건 · 제외 목록은 빼고 */
public final class BulkShip {
    private BulkShip() {}

    private static CompoundTag excluded(ServerPlayer p) {
        return ContentData.get(p.level().getServer()).player("ship_exclude", p.getUUID());
    }

    private static String key(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).toString();
    }

    private static boolean sellable(ItemStack s) {
        return !s.isEmpty() && ShipPrices.centi(s) > 0 && !s.isEnchanted() && !com.snolred.wildplus.event.Engraves.gear(s) && Coins.unitValue(s) == 0 && !Coins.isPouch(s);
    }

    /** 가방(36칸)에서 종류별 [개수, 금액×100] */
    private static Map<Item, long[]> scan(ServerPlayer p) {
        Map<Item, long[]> m = new LinkedHashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!sellable(s)) continue;
            long[] v = m.computeIfAbsent(s.getItem(), k -> new long[2]);
            v[0] += s.getCount();
            v[1] += (long) ShipPrices.centi(s) * s.getCount();
        }
        return m;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        CompoundTag ex = excluded(p);
        Map<Item, long[]> m = scan(p);
        List<Item> order = new ArrayList<>(m.keySet());
        long total = 0;
        for (Item i : order) if (!ex.contains(key(i))) total += m.get(i)[1];
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("전부 팔기", String.format("합계 %,d동 · 세금은 팔 때 계산돼요", total / 100), new ItemStack(Items.EMERALD)));
        for (Item i : order) {
            long[] v = m.get(i);
            boolean out = ex.contains(key(i));
            e.add(new PickMenu.Entry((out ? "✕ " : "") + new ItemStack(i).getHoverName().getString() + " ×" + v[0],
                    out ? "제외됨 · 누르면 다시 팔아요" : String.format("%,d동 · 누르면 제외", v[1] / 100), new ItemStack(i)));
        }
        final long sum = total;
        PickMenu.open(p, Component.literal("📦 일괄 출하"), String.format("합계 %,d동 · 장비와 인챈트된 물건은 빠져요", total / 100), "", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_MARKET, idx -> {
            if (idx == 0) {
                if (sum > 0) sell(p);
                p.closeContainer();
                return;
            }
            if (idx - 1 >= order.size()) return;
            String k = key(order.get(idx - 1));
            if (ex.contains(k)) ex.remove(k);
            else ex.putBoolean(k, true);
            ContentData.get(p.level().getServer()).dirty();
            open(p, pos);
        }, null, x -> x.blockPosition().closerThan(pos, 10));
    }

    private static void sell(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag ex = excluded(p);
        long centi = 0;
        int sold = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!sellable(st) || ex.contains(key(st.getItem()))) continue;
            centi += (long) ShipPrices.centi(st) * st.getCount();
            sold += st.getCount();
            p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        if (sold == 0) return;
        WildData wd = WildData.get(s);
        int hr = wd.honor(p.getUUID()); // 출하 상자와 같은 등급 보너스
        if (hr >= 7) centi += centi / 5;
        else if (hr >= 6) centi += centi / 10;
        long coins = centi / 100;
        long net = Loans.garnish(s, p.getUUID(), Tax.collect(s, p.getUUID(), coins));
        wd.reason = "일괄 출하";
        wd.addBalance(p.getUUID(), net);
        com.snolred.wildplus.content.Codex.add(p, "ship", net);
        p.sendSystemMessage(Component.literal(String.format("📦 %d개 출하 · %,d동 − 세금 등 %,d동 = %,d동 (통장 입금)", sold, coins, coins - net, net)).withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /** 출하 상자를 웅크리고 때리면 일괄 출하 */
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.snolred.wildplus.block.ShippingBinBlock)) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        event.setCanceled(true);
        open(p, event.getPos());
    }
}
