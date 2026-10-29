package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 자판기 데이터: 상품(1종류)·재고·가격·주인·수익.
 * infinite=true 이면 주인 없는 무한 재고 자판기 (구조물에 배치되는 것).
 * 머리 위에 상품을 보여주는 아이템 디스플레이를 띄운다.
 */
public class VendingMachineBlockEntity extends BlockEntity {
    private ItemStack product = ItemStack.EMPTY;   // 상품 견본 (개수 1)
    private int stock;
    private int price = 10;                        // 1개당 가격 (동)
    private long earnings;
    private boolean infinite;
    private @Nullable UUID owner;
    private String ownerName = "";
    private @Nullable UUID displayId;
    private float spin;

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE.get(), pos, state);
    }

    // ───────── 접근자 ─────────
    public ItemStack product() { return product; }
    public int stock() { return infinite ? Integer.MAX_VALUE : stock; }
    public int price() { return price; }
    public long earnings() { return earnings; }
    public boolean infinite() { return infinite; }
    public String ownerName() { return ownerName; }
    public boolean hasOwner() { return owner != null; }

    public boolean canManage(Player player) {
        if (owner == null) return player.isCreative();
        return owner.equals(player.getUUID()) || player.isCreative();
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
    }

    public void setPrice(int price) {
        this.price = Math.max(1, price);
        setChanged();
    }

    /** 상품 보충. 다른 상품이 들어 있으면 0 반환 */
    public int addStock(ItemStack held) {
        if (!product.isEmpty() && !ItemStack.isSameItemSameComponents(product, held)) {
            return 0;
        }
        if (product.isEmpty()) {
            product = held.copyWithCount(1);
        }
        int n = held.getCount();
        if (!infinite) stock += n;
        held.shrink(n);
        setChanged();
        return n;
    }

    /** 재고 회수 (주인) */
    public ItemStack takeAllStock() {
        if (product.isEmpty() || infinite) return ItemStack.EMPTY;
        ItemStack out = product.copyWithCount(stock);
        stock = 0;
        product = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public long takeEarnings() {
        long e = earnings;
        earnings = 0;
        setChanged();
        return e;
    }

    /** 판매 처리: 재고 차감·수익 적립. 실제로 건넨 개수 반환 */
    public int sell(int count) {
        if (product.isEmpty()) return 0;
        int n = infinite ? count : Math.min(count, stock);
        if (!infinite) {
            stock -= n;
            earnings += (long) n * price;
            if (stock <= 0) {
                stock = 0;
            }
        }
        setChanged();
        return n;
    }

    // ───────── 저장 ─────────
    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!product.isEmpty()) {
            out.store("product", ItemStack.CODEC, product);
        }
        out.putInt("stock", stock);
        out.putInt("price", price);
        out.putLong("earnings", earnings);
        out.putBoolean("infinite", infinite);
        out.putString("owner_name", ownerName);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        if (displayId != null) out.store("display", UUIDUtil.CODEC, displayId);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        product = in.read("product", ItemStack.CODEC).map(s -> s.copyWithCount(1)).orElse(ItemStack.EMPTY);
        stock = in.getIntOr("stock", 0);
        price = Math.max(1, in.getIntOr("price", 10));
        earnings = in.getLongOr("earnings", 0L);
        infinite = in.getBooleanOr("infinite", false);
        ownerName = in.getStringOr("owner_name", "");
        Optional<UUID> o = in.read("owner", UUIDUtil.CODEC);
        owner = o.orElse(null);
        displayId = in.read("display", UUIDUtil.CODEC).orElse(null);
    }

    // ───────── 상품 진열 (머리 위 아이템 디스플레이) ─────────
    public static void serverTick(Level level, BlockPos pos, BlockState state, VendingMachineBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 4 != 0) {
            return;
        }
        Entity existing = be.displayId == null ? null : server.getEntity(be.displayId);
        boolean soldOut = be.product.isEmpty() || (!be.infinite && be.stock <= 0);
        if (soldOut) {
            if (existing != null) existing.discard();
            if (be.displayId != null) {
                be.displayId = null;
                be.setChanged();
            }
            return;
        }
        Display.ItemDisplay display;
        if (existing instanceof Display.ItemDisplay d) {
            display = d;
        } else {
            display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, server);
            display.setPos(pos.getX() + 0.5, pos.getY() + 1.45, pos.getZ() + 0.5);
            display.addTag("wildplus_vending");
            server.addFreshEntity(display);
            be.displayId = display.getUUID();
            be.setChanged();
        }
        if (!ItemStack.isSameItemSameComponents(display.getSlot(0).get(), be.product)) {
            display.getSlot(0).set(be.product.copy());
        }
        be.spin = (be.spin + 12.0F) % 360.0F;
        display.setYRot(be.spin);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (displayId != null) {
                Entity e = server.getEntity(displayId);
                if (e != null) e.discard();
            }
            if (!infinite && !product.isEmpty()) {
                int left = stock;
                while (left > 0) {
                    int n = Math.min(left, product.getMaxStackSize());
                    Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, product.copyWithCount(n));
                    left -= n;
                }
            }
            for (ItemStack coin : Coins.toCoins(earnings)) {
                Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, coin);
            }
        }
    }
}
