package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.StockMarket;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 증권 단말기 화면. 종목별 현재가·등락·보유 수량이 실시간으로 동기화되고,
 * 버튼(종목 번호 × 10 + 동작)으로 사고판다. 동작: 0 = 1주 매수, 1 = 10주 매수, 2 = 1주 매도, 3 = 전부 매도.
 */
public class StockMenu extends AbstractContainerMenu {
    public static final int PER = 8;  // 종목당 데이터 칸: 가격 lo/hi, 전일 lo/hi, 보유 lo/hi, 매입액 lo/hi
    public static final int N = StockMarket.STOCKS.size();
    public static final int D_CASH = N * PER, D_LIVE = D_CASH + 2, DATA = D_LIVE + 1;

    private final ContainerData data;
    private final @Nullable ServerPlayer player;
    private final @Nullable BlockPos pos;
    public final String updated;

    /** 클라이언트 */
    public StockMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.STOCKS.get(), id);
        this.data = new SimpleContainerData(DATA);
        this.player = null;
        this.pos = null;
        this.updated = buf.readUtf();
        addDataSlots(data);
    }

    private StockMenu(int id, ServerPlayer player, BlockPos pos) {
        super(ModMenus.STOCKS.get(), id);
        this.player = player;
        this.pos = pos;
        this.updated = StockMarket.updated();
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return split(serverValue(index / 2 * 2), index % 2 == 1);
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA;
            }
        };
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StockMenu(id, player, pos),
                Component.translatable("container.wildplus.stocks")), buf -> buf.writeUtf(StockMarket.updated()));
    }

    private static int split(long v, boolean hi) {
        v = Math.max(0, v);
        return (int) (hi ? Math.min(32767, v / 10000) : v % 10000);
    }

    private long serverValue(int index) {
        if (player == null) return 0;
        if (index == D_CASH) return Coins.totalValue(player);
        if (index == D_LIVE) return StockMarket.isLive() ? 1 : 0;
        int stock = index / PER, field = (index % PER) / 2;
        long[] h = WildData.get(player.level().getServer()).holding(player.getUUID(), StockMarket.STOCKS.get(stock).code());
        return switch (field) {
            case 0 -> StockMarket.price(stock);
            case 1 -> StockMarket.prev(stock);
            case 2 -> h[0];
            default -> h[1];
        };
    }

    private long read(int index) {
        return data.get(index) + data.get(index + 1) * 10000L;
    }

    // ── 화면에서 읽는 값 ──
    public long price(int i) { return read(i * PER); }
    public long prev(int i) { return read(i * PER + 2); }
    public long shares(int i) { return read(i * PER + 4); }
    public long cost(int i) { return read(i * PER + 6); }
    public long cash() { return read(D_CASH); }
    public boolean live() { return data.get(D_LIVE) == 1; }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        int i = id / 10, action = id % 10;
        if (i < 0 || i >= N) return false;
        StockMarket.Stock stock = StockMarket.STOCKS.get(i);
        WildData wd = WildData.get(sp.level().getServer());
        long[] h = wd.holding(sp.getUUID(), stock.code());
        long price = StockMarket.price(i);
        if (action == 0 || action == 1) {
            long n = action == 0 ? 1 : 10;
            long cost = (price * n + 99) / 100;
            if (!Coins.pay(sp, cost)) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.stock.no_money", cost).withStyle(ChatFormatting.RED));
                return true;
            }
            h[0] += n;
            h[1] += cost;
            wd.setDirty();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
            sp.sendOverlayMessage(Component.translatable("message.wildplus.stock.bought", stock.name(), n, cost).withStyle(ChatFormatting.GREEN));
        } else if (action == 2 || action == 3) {
            long n = action == 2 ? Math.min(1, h[0]) : h[0];
            if (n <= 0) return true;
            long gain = price * n / 100;
            long basis = h[1] * n / h[0];
            h[1] -= basis;
            h[0] -= n;
            wd.setDirty();
            Coins.refund(sp, gain);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.8F);
            long profit = gain - basis;
            sp.sendOverlayMessage(Component.translatable("message.wildplus.stock.sold", stock.name(), n, gain,
                    (profit >= 0 ? "+" : "") + profit).withStyle(profit >= 0 ? ChatFormatting.RED : ChatFormatting.BLUE));
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return pos == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }
}
