package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModItems3;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 환전소: 가방 속 동전을 바꿔 준다 (수수료 없음).
 * 올리기: 동화 10 → 은화 1, 은화 10 → 금화 1 / 내리기: 금화 1 → 은화 10, 은화 1 → 동화 10.
 * 버튼 = 종류 × 2 + (0: 한 번, 1: 전부). [정리] 은 가장 적은 개수로, [잔돈으로] 는 전부 동화로.
 */
public class ExchangeMenu extends AbstractContainerMenu {
    public static final int C2S = 0, S2G = 2, G2S = 4, S2C = 6, TIDY = 10, ALL_COPPER = 11;
    public static final int DATA = 3;

    private final ContainerData data;
    private final @Nullable BlockPos pos;

    /** 클라이언트 */
    public ExchangeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.EXCHANGE.get(), id);
        this.data = new SimpleContainerData(DATA);
        this.pos = null;
        addDataSlots(data);
    }

    private ExchangeMenu(int id, ServerPlayer player, BlockPos pos) {
        super(ModMenus.EXCHANGE.get(), id);
        this.pos = pos;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                Item item = switch (index) {
                    case 0 -> ModItems3.COPPER_COIN.get();
                    case 1 -> ModItems3.SILVER_COIN.get();
                    default -> ModItems3.GOLD_COIN.get();
                };
                return Math.min(32767, count(player, item));
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
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ExchangeMenu(id, player, pos),
                Component.translatable("container.wildplus.exchange")), buf -> {});
    }

    public int copper() { return data.get(0); }
    public int silver() { return data.get(1); }
    public int gold() { return data.get(2); }

    private static int count(Player p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        }
        return n;
    }

    private static void take(Player p, Item item, int n) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
        inv.setChanged();
    }

    private static void giveItems(Player p, Item item, int n) {
        while (n > 0) {
            ItemStack s = new ItemStack(item, Math.min(n, 64));
            n -= s.getCount();
            if (!p.getInventory().add(s) && !s.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        Item copper = ModItems3.COPPER_COIN.get(), silver = ModItems3.SILVER_COIN.get(), gold = ModItems3.GOLD_COIN.get();
        boolean all = id % 2 == 1 && id < TIDY;
        int kind = id < TIDY ? id - id % 2 : id;
        boolean ok = switch (kind) {
            case C2S -> convert(sp, copper, 10, silver, 1, all);
            case S2G -> convert(sp, silver, 10, gold, 1, all);
            case G2S -> convert(sp, gold, 1, silver, 10, all);
            case S2C -> convert(sp, silver, 1, copper, 10, all);
            case TIDY, ALL_COPPER -> {
                long total = (long) count(sp, copper) + count(sp, silver) * 10L + count(sp, gold) * 100L;
                if (total == 0) yield false;
                take(sp, copper, Integer.MAX_VALUE);
                take(sp, silver, Integer.MAX_VALUE);
                take(sp, gold, Integer.MAX_VALUE);
                if (kind == TIDY) {
                    Coins.give(sp, total);
                } else {
                    giveItems(sp, copper, (int) Math.min(Integer.MAX_VALUE, total));
                }
                yield true;
            }
            default -> false;
        };
        if (ok) {
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        } else {
            sp.sendOverlayMessage(Component.translatable("message.wildplus.exchange.not_enough").withStyle(ChatFormatting.RED));
        }
        return true;
    }

    /** from n개 → to m개 (all 이면 가능한 만큼 여러 번) */
    private static boolean convert(Player p, Item from, int n, Item to, int m, boolean all) {
        int have = count(p, from);
        int times = all ? have / n : (have >= n ? 1 : 0);
        if (times <= 0) return false;
        take(p, from, times * n);
        giveItems(p, to, times * m);
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
