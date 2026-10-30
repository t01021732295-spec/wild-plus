package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Bounties;
import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModMenus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 의뢰 게시판 화면: 오늘의 납품 의뢰 3개. 버튼 0~2 = 납품. */
public class BountyMenu extends AbstractContainerMenu {
    public static final int D_REWARD = 0, D_HAVE = 6, D_DONE = 9, DATA = 10;

    private final SimpleContainer display = new SimpleContainer(Bounties.COUNT);
    private final ContainerData data;
    private final @Nullable List<Bounties.Bounty> bounties;
    private final @Nullable BlockPos pos;
    private final long day;

    /** 클라이언트 */
    public BountyMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.BOUNTY.get(), id);
        this.data = new SimpleContainerData(DATA);
        this.bounties = null;
        this.pos = null;
        this.day = buf.readVarLong();
        addSlots();
        addDataSlots(data);
    }

    private BountyMenu(int id, ServerPlayer player, BlockPos pos, long day) {
        super(ModMenus.BOUNTY.get(), id);
        this.pos = pos;
        this.day = day;
        this.bounties = Bounties.forDay(player.getUUID(), day);
        for (int i = 0; i < Bounties.COUNT; i++) display.setItem(i, bounties.get(i).stack().copy());
        List<Bounties.Bounty> list = bounties;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < D_HAVE) {
                    long r = list.get(index / 2).reward();
                    return (int) (index % 2 == 1 ? r / 10000 : r % 10000);
                }
                if (index < D_DONE) return Math.min(9999, count(player, list.get(index - D_HAVE).stack()));
                return WildData.get(player.level().getServer()).bountyDone(player.getUUID(), day);
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA;
            }
        };
        addSlots();
        addDataSlots(data);
    }

    private void addSlots() {
        for (int i = 0; i < Bounties.COUNT; i++) {
            addSlot(new Slot(display, i, 12, 24 + i * 30) {
                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack s) {
                    return false;
                }
            });
        }
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        long day = player.level().getServer().overworld().getOverworldClockTime() / 24000L;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BountyMenu(id, player, pos, day),
                Component.translatable("container.wildplus.bounty_board")), buf -> buf.writeVarLong(day));
    }

    private static int count(Player p, ItemStack want) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (ItemStack.isSameItem(s, want) && !s.isDamaged()) n += s.getCount();
        }
        return n;
    }

    public long reward(int i) { return data.get(D_REWARD + i * 2) + data.get(D_REWARD + i * 2 + 1) * 10000L; }
    public int have(int i) { return data.get(D_HAVE + i); }
    public boolean done(int i) { return (data.get(D_DONE) & (1 << i)) != 0; }
    public ItemStack wanted(int i) { return slots.get(i).getItem(); }
    public long day() { return day; }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp) || bounties == null || id < 0 || id >= Bounties.COUNT) return false;
        WildData wd = WildData.get(sp.level().getServer());
        if ((wd.bountyDone(sp.getUUID(), day) & (1 << id)) != 0) return true;
        Bounties.Bounty b = bounties.get(id);
        int need = b.stack().getCount();
        if (count(sp, b.stack()) < need) {
            sp.sendOverlayMessage(Component.translatable("message.wildplus.bounty.not_enough").withStyle(ChatFormatting.RED));
            return true;
        }
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (ItemStack.isSameItem(s, b.stack()) && !s.isDamaged()) {
                int take = Math.min(need, s.getCount());
                s.shrink(take);
                need -= take;
            }
        }
        wd.markBountyDone(sp.getUUID(), day, id);
        Coins.refund(sp, b.reward());
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.7F, 1.0F);
        sp.sendSystemMessage(Component.translatable("message.wildplus.bounty.done", b.stack().getHoverName(), b.stack().getCount(), b.reward())
                .withStyle(ChatFormatting.GOLD));
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
