package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
 * 은행 ATM: 예금 · 출금 · 송금. 예금에는 매일 아침 이자(1%, 하루 최대 500동)가 붙는다.
 * 송금은 은행 잔액에서 상대 계좌로 바로 들어간다 (상대가 접속 중일 때만 고를 수 있음).
 */
public class BankMenu extends AbstractContainerMenu {
    public static final int DEP_ALL = 0, DEP_100 = 1, DEP_10 = 2;
    public static final int WD_10 = 10, WD_100 = 11, WD_1000 = 12, WD_ALL = 13, WD_10K = 14, WD_100K = 15;
    public static final int AMT_10 = 20, AMT_100 = 21, AMT_1000 = 22, AMT_RESET = 23, PREV = 24, NEXT = 25, SEND = 26;
    public static final int D_BAL = 0, D_CASH = 2, D_AMT = 4, D_TARGET = 6, D_INCOME = 7, D_TAX = 9, D_TREASURY = 11, DATA = 13;

    public final List<String> names;
    private final List<UUID> ids;
    private final ContainerData data;
    private final @Nullable ServerPlayer player;
    private final @Nullable BlockPos pos;
    private long amount;
    private int target;

    /** 클라이언트 */
    public BankMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.BANK.get(), id);
        int n = buf.readVarInt();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(buf.readUtf());
        this.names = list;
        this.ids = List.of();
        this.player = null;
        this.pos = null;
        this.data = new SimpleContainerData(DATA);
        addDataSlots(data);
    }

    private BankMenu(int id, ServerPlayer player, BlockPos pos, List<String> names, List<UUID> ids) {
        super(ModMenus.BANK.get(), id);
        this.player = player;
        this.pos = pos;
        this.names = names;
        this.ids = ids;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == D_TARGET) return target;
                int base = index < D_TARGET ? index / 2 * 2 : D_INCOME + (index - D_INCOME) / 2 * 2;
                boolean hi = (index - base) == 1;
                net.minecraft.server.MinecraftServer server = player.level().getServer();
                long v;
                if (base == D_BAL) v = WildData.get(server).balance(player.getUUID());
                else if (base == D_CASH) v = Coins.totalValue(player);
                else if (base == D_AMT) v = amount;
                else if (base == D_INCOME) v = com.snolred.wildplus.economy.Tax.today(server, player.getUUID())[0];
                else if (base == D_TAX) v = com.snolred.wildplus.economy.Tax.today(server, player.getUUID())[1];
                else v = WildData.get(server).treasury;
                return (int) (hi ? Math.min(32767, v / 10000) : v % 10000);
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
        List<String> names = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
            if (p != player) {
                names.add(p.getName().getString());
                ids.add(p.getUUID());
            }
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BankMenu(id, player, pos, names, ids),
                Component.translatable("container.wildplus.bank")), buf -> {
            buf.writeVarInt(names.size());
            names.forEach(buf::writeUtf);
        });
    }

    private long read(int i) {
        return data.get(i) + data.get(i + 1) * 10000L;
    }

    public long balance() { return read(D_BAL); }
    public long cash() { return read(D_CASH); }
    public long amount() { return read(D_AMT); }
    public int target() { return data.get(D_TARGET); }
    public long incomeToday() { return read(D_INCOME); }
    public long taxToday() { return read(D_TAX); }
    public long treasury() { return read(D_TREASURY); }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        WildData wd = WildData.get(sp.level().getServer());
        UUID me = sp.getUUID();
        switch (id) {
            case DEP_ALL, DEP_100, DEP_10 -> {
                long want = id == DEP_ALL ? Coins.totalValue(sp) : id == DEP_100 ? 100 : 10;
                if (want > 0 && Coins.pay(sp, want)) {
                    wd.addBalance(me, want);
                    ding(sp, 1.2F);
                } else {
                    sp.sendOverlayMessage(Component.translatable("message.wildplus.bank.no_cash").withStyle(ChatFormatting.RED));
                }
            }
            case WD_10, WD_100, WD_1000, WD_ALL, WD_10K, WD_100K -> {
                long bal = wd.balance(me);
                long want = switch (id) {
                    case WD_ALL -> bal;
                    case WD_10 -> 10;
                    case WD_100 -> 100;
                    case WD_10K -> 10_000;
                    case WD_100K -> 100_000;
                    default -> 1000;
                };
                if (want > 0 && bal >= want) {
                    wd.addBalance(me, -want);
                    Coins.refund(sp, want);
                    ding(sp, 0.9F);
                } else {
                    sp.sendOverlayMessage(Component.translatable("message.wildplus.bank.no_balance").withStyle(ChatFormatting.RED));
                }
            }
            case AMT_10 -> amount += 10;
            case AMT_100 -> amount += 100;
            case AMT_1000 -> amount += 1000;
            case AMT_RESET -> amount = 0;
            case PREV -> target = ids.isEmpty() ? 0 : (target + ids.size() - 1) % ids.size();
            case NEXT -> target = ids.isEmpty() ? 0 : (target + 1) % ids.size();
            case SEND -> {
                if (ids.isEmpty() || amount <= 0) return true;
                if (wd.balance(me) < amount) {
                    sp.sendOverlayMessage(Component.translatable("message.wildplus.bank.no_balance").withStyle(ChatFormatting.RED));
                    return true;
                }
                UUID to = ids.get(target);
                wd.addBalance(me, -amount);
                wd.addBalance(to, amount);
                sp.sendSystemMessage(Component.translatable("message.wildplus.bank.sent", names.get(target), amount).withStyle(ChatFormatting.GOLD));
                ServerPlayer other = sp.level().getServer().getPlayerList().getPlayer(to);
                if (other != null) {
                    other.sendSystemMessage(Component.translatable("message.wildplus.bank.received", sp.getName(), amount).withStyle(ChatFormatting.GOLD));
                }
                ding(sp, 1.5F);
                amount = 0;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void ding(ServerPlayer sp, float pitch) {
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, pitch);
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
