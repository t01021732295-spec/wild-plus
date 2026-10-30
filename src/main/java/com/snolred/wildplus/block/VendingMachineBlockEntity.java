package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.menu.VendingMenu;
import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 자판기: 진열 칸 9개 (칸마다 상품 한 묶음 = 재고) + 칸별 가격.
 * - 플레이어가 설치하면 그 사람이 주인 → 상품·가격을 직접 정한다.
 * - 구조물에서 발견된 자판기(infinite)는 주인이 없고, 정해진 상품·가격이 무한 재고로 팔린다.
 */
public class VendingMachineBlockEntity extends BlockEntity implements MenuProvider, ItemOwner {
    public static final int SLOTS = 9;
    public static final int MAX_PRICE = 9999;

    private final SimpleContainer items = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            VendingMachineBlockEntity.this.markUpdated();
        }
    };
    private final int[] prices = new int[SLOTS];
    private long earnings;
    private boolean infinite;
    private @Nullable UUID owner;
    private String ownerName = "";
    private @Nullable UUID legacyDisplay;   // 1.2.0 의 공중 진열 엔티티 (정리용)

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE.get(), pos, state);
        java.util.Arrays.fill(prices, 10);
    }

    // ───────── 접근자 ─────────
    public SimpleContainer items() { return items; }
    public int price(int slot) { return prices[slot]; }
    public long earnings() { return earnings; }
    public boolean infinite() { return infinite; }
    public String ownerName() { return ownerName; }
    public boolean hasOwner() { return owner != null; }

    /** 상품·가격을 바꿀 수 있는 사람: 자기가 설치한 자판기의 주인만 */
    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
        markUpdated();
    }

    public void setPrice(int slot, int price) {
        prices[slot] = Math.max(1, Math.min(MAX_PRICE, price));
        markUpdated();
    }

    public long takeEarnings() {
        long e = earnings;
        earnings = 0;
        markUpdated();
        return e;
    }

    /** 칸 하나를 판매: 꺼낸 상품(1개) 반환, 못 팔면 EMPTY */
    public ItemStack sellOne(int slot) {
        ItemStack stock = items.getItem(slot);
        if (stock.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = stock.copyWithCount(1);
        if (!infinite) {
            stock.shrink(1);
            earnings += prices[slot];
            items.setChanged();
        } else {
            markUpdated();
        }
        return out;
    }

    public void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ───────── 진열 렌더링용 (ItemOwner) ─────────
    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public Vec3 position() {
        return Vec3.atCenterOf(getBlockPos());
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return getBlockState().getValue(VendingMachineBlock.FACING).toYRot();
    }

    // ───────── 화면 ─────────
    @Override
    public Component getDisplayName() {
        if (infinite) {
            return Component.translatable("container.wildplus.vending_machine.wild");
        }
        return ownerName.isEmpty()
                ? Component.translatable("block.wildplus.vending_machine")
                : Component.translatable("container.wildplus.vending_machine.owned", ownerName);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new VendingMenu(id, inv, this, isOwner(player) && !infinite);
    }

    // ───────── 저장·동기화 ─────────
    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getItem(i);
            if (!s.isEmpty()) {
                out.store("item" + i, ItemStack.CODEC, s);
            }
        }
        out.putIntArray("prices", prices.clone());
        out.putLong("earnings", earnings);
        out.putBoolean("infinite", infinite);
        out.putString("owner_name", ownerName);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < SLOTS; i++) {
            items.setItem(i, in.read("item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY), false);
        }
        in.getIntArray("prices").ifPresent(arr -> {
            for (int i = 0; i < SLOTS && i < arr.length; i++) prices[i] = Math.max(1, arr[i]);
        });
        earnings = in.getLongOr("earnings", 0L);
        infinite = in.getBooleanOr("infinite", false);
        ownerName = in.getStringOr("owner_name", "");
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        // 1.2.0 형식(상품 1종 + stock/price) 변환
        in.read("product", ItemStack.CODEC).ifPresent(product -> {
            int stock = infinite ? 1 : in.getIntOr("stock", 0);
            int price = Math.max(1, in.getIntOr("price", 10));
            int slot = 0;
            while (stock > 0 && slot < SLOTS) {
                int n = Math.min(stock, product.getMaxStackSize());
                items.setItem(slot, product.copyWithCount(n), false);
                prices[slot] = price;
                stock -= n;
                slot++;
            }
        });
        legacyDisplay = in.read("display", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    /** 예전 버전의 공중 진열 엔티티 정리 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, VendingMachineBlockEntity be) {
        // 1.3.0 이전(1칸짜리) 자판기나 명령어로 아래 칸만 놓인 경우 위 칸을 채워 준다
        if (level.getGameTime() % 20 == (pos.asLong() & 15)) {
            BlockPos up = pos.above();
            BlockState above = level.getBlockState(up);
            if (!above.is(state.getBlock()) && above.canBeReplaced() && up.getY() < level.getMaxY()) {
                level.setBlock(up, state.setValue(VendingMachineBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            }
        }
        if (be.legacyDisplay != null && level instanceof ServerLevel server) {
            Entity e = server.getEntity(be.legacyDisplay);
            if (e != null) e.discard();
            be.legacyDisplay = null;
            be.markUpdated();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!infinite) {
                Containers.dropContents(server, pos, items);
            }
            for (ItemStack coin : Coins.toCoins(earnings)) {
                Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, coin);
            }
        }
    }
}
