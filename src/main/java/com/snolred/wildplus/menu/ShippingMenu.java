package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.ShippingBinBlockEntity;
import com.snolred.wildplus.economy.ShipPrices;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;

/** 출하 상자 창: 상자 27칸 + 주인이면 공개/비공개 전환 버튼 */
public class ShippingMenu extends ChestMenu {
    public static final int BTN_TOGGLE = 0;
    public final boolean owner, isPublic;
    public final long pending;
    private final BlockPos pos;

    public ShippingMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.SHIPPING.get(), id, inv, new SimpleContainer(27), 3);
        this.pos = buf.readBlockPos();
        this.owner = buf.readBoolean();
        this.isPublic = buf.readBoolean();
        this.pending = buf.readVarLong();
    }

    private ShippingMenu(int id, Inventory inv, ShippingBinBlockEntity be, boolean owner) {
        super(ModMenus.SHIPPING.get(), id, inv, be.items(), 3);
        this.pos = be.getBlockPos();
        this.owner = owner;
        this.isPublic = be.isPublic();
        this.pending = be.pendingCenti();
    }

    public static void open(ServerPlayer p, ShippingBinBlockEntity be) {
        boolean owner = be.isOwner(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ShippingMenu(id, inv, be, owner),
                Component.translatable("block.wildplus.shipping_bin")), buf -> {
            buf.writeBlockPos(be.getBlockPos());
            buf.writeBoolean(owner);
            buf.writeBoolean(be.isPublic());
            buf.writeVarLong(be.pendingCenti());
        });
    }

    public String pendingText() {
        return ShipPrices.fmt((int) Math.min(Integer.MAX_VALUE, pending));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_TOGGLE && player instanceof ServerPlayer sp && owner
                && sp.level().getBlockEntity(pos) instanceof ShippingBinBlockEntity be) {
            be.togglePublic();
            sp.level().getServer().execute(() -> open(sp, be));
            return true;
        }
        return false;
    }
}
