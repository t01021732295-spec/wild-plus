package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.RestaurantCounterBlockEntity;
import com.snolred.wildplus.cooking.Cooking;
import com.snolred.wildplus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 식당 카운터 화면: 주방(티켓) · 손님(메뉴 · 평점) · 관리(이름 · 알바 · 직원) + 진열대 10칸 */
public class RestaurantMenu extends AbstractContainerMenu {
    public static final int W = 256, H = 246, SHELF_X = 38, SHELF_Y = 136, INV_X = 48, INV_Y = 166;
    public static final int BTN_OPEN = 10, BTN_HIRING = 11, BTN_FEE_DOWN = 12, BTN_FEE_UP = 13, BTN_PCT_DOWN = 14, BTN_PCT_UP = 15,
            BTN_ALBA = 20, BTN_APPLY = 21, BTN_QUIT = 22, BTN_ACCEPT = 30, BTN_FIRE = 40, BTN_BUY = 50;

    /** 화면에 보여 줄 글 (열 때 한 번 보냄) */
    public record Extra(String owner, List<String> staff, List<String> applicants, List<String> reviews, BlockPos pos) {
        void write(RegistryFriendlyByteBuf b) {
            b.writeUtf(owner);
            writeList(b, staff);
            writeList(b, applicants);
            writeList(b, reviews);
            b.writeBlockPos(pos);
        }

        static Extra read(RegistryFriendlyByteBuf b) {
            return new Extra(b.readUtf(), readList(b), readList(b), readList(b), b.readBlockPos());
        }

        private static void writeList(RegistryFriendlyByteBuf b, List<String> l) {
            b.writeVarInt(l.size());
            for (String s : l) b.writeUtf(s);
        }

        private static List<String> readList(RegistryFriendlyByteBuf b) {
            int n = b.readVarInt();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < n; i++) out.add(b.readUtf());
            return out;
        }
    }

    private final Container box;
    private final ContainerData data;
    public final Extra extra;
    private final RestaurantCounterBlockEntity be;

    public RestaurantMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(RestaurantCounterBlockEntity.SHELF), new SimpleContainerData(RestaurantCounterBlockEntity.DATA), Extra.read(buf), null);
    }

    private RestaurantMenu(int id, Inventory inv, Container box, ContainerData data, Extra extra, RestaurantCounterBlockEntity be) {
        super(ModMenus.RESTAURANT.get(), id);
        this.box = box;
        this.data = data;
        this.extra = extra;
        this.be = be;
        for (int i = 0; i < RestaurantCounterBlockEntity.SHELF; i++) {
            addSlot(new Slot(box, i, SHELF_X + i * 18, SHELF_Y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return canStock() && Cooking.dishOf(s) != null;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return canStock();
                }
            });
        }
        addStandardInventorySlots(inv, INV_X, INV_Y);
        addDataSlots(data);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        if (!(p.level().getBlockEntity(pos) instanceof RestaurantCounterBlockEntity be)) return;
        Extra extra = new Extra(be.village ? "마을" : be.ownerName, List.copyOf(be.staffNames), List.copyOf(be.applicantNames), List.copyOf(be.reviews), pos);
        ContainerData d = new ContainerData() {
            @Override
            public int get(int i) {
                return be.data(i, p);
            }

            @Override
            public void set(int i, int v) {}

            @Override
            public int getCount() {
                return RestaurantCounterBlockEntity.DATA;
            }
        };
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new RestaurantMenu(id, inv, be, d, extra, be), Component.literal(be.displayName())),
                buf -> extra.write(buf));
    }

    // ───────── 숫자 ─────────
    public int get(int i) {
        return data.get(i);
    }

    public int role() {
        return data.get(44);
    }

    public boolean village() {
        return data.get(45) == 1;
    }

    public boolean canStock() {
        return !village() && role() >= RestaurantCounterBlockEntity.ROLE_STAFF;
    }

    public boolean canServe() {
        return role() >= RestaurantCounterBlockEntity.ROLE_ALBA;
    }

    public RestaurantCounterBlockEntity blockEntity() {
        return be;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        int role = be.role(sp);
        boolean owner = role == RestaurantCounterBlockEntity.ROLE_OWNER;
        boolean refresh = false;
        if (id >= 0 && id < RestaurantCounterBlockEntity.TICKETS) be.serve(sp, id);
        else if (id == BTN_OPEN && owner) be.open = !be.open;
        else if (id == BTN_HIRING && owner) be.hiring = !be.hiring;
        else if (id == BTN_FEE_DOWN && owner) be.albaFee = Math.max(10, be.albaFee - 10);
        else if (id == BTN_FEE_UP && owner) be.albaFee = Math.min(500, be.albaFee + 10);
        else if (id == BTN_PCT_DOWN && owner) be.staffPct = Math.max(10, be.staffPct - 5);
        else if (id == BTN_PCT_UP && owner) be.staffPct = Math.min(50, be.staffPct + 5);
        else if (id == BTN_ALBA) be.startAlba(sp);
        else if (id == BTN_APPLY) {
            be.apply(sp);
            refresh = true;
        } else if (id == BTN_QUIT) {
            be.quit(sp);
            refresh = true;
        } else if (id >= BTN_ACCEPT && id < BTN_ACCEPT + 5 && owner) {
            be.accept(id - BTN_ACCEPT);
            refresh = true;
        } else if (id >= BTN_FIRE && id < BTN_FIRE + 3 && owner) {
            be.fire(id - BTN_FIRE);
            refresh = true;
        } else if (id >= BTN_BUY && id < BTN_BUY + RestaurantCounterBlockEntity.SHELF) be.buy(sp, id - BTN_BUY);
        else return false;
        be.setChanged();
        if (refresh) {
            BlockPos pos = be.getBlockPos();
            sp.level().getServer().execute(() -> open(sp, pos));
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || !canStock()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int n = RestaurantCounterBlockEntity.SHELF;
        if (index < n) {
            if (!moveItemStackTo(s, n, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (Cooking.dishOf(s) == null || !moveItemStackTo(s, 0, n, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return box.stillValid(p);
    }
}
