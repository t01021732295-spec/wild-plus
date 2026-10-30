package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.ShipPrices;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 출하 상자: 넣어 둔 물건이 매일 아침(오전 6시) 자동으로 팔리고, 대금은 설치한 사람의 은행 계좌로 들어간다.
 * 가격표에 없는 물건은 그대로 남는다.
 */
public class ShippingBinBlockEntity extends BlockEntity {
    public static final int SIZE = 27;
    private final SimpleContainer items = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            ShippingBinBlockEntity.this.setChanged();
        }
    };
    private @Nullable UUID owner;
    private long lastDay = -1;
    private long remainderCenti;

    public ShippingBinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIPPING_BIN.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        setChanged();
    }

    /** 지금 상자에 든 물건을 팔면 받을 금액 (동 × 100) */
    public long pendingCenti() {
        long total = 0;
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = items.getItem(i);
            total += (long) ShipPrices.centi(s) * s.getCount();
        }
        return total;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShippingBinBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 7) return;
        long day = server.getServer().overworld().getOverworldClockTime() / 24000L;
        if (be.lastDay < 0) {
            be.lastDay = day;
            be.setChanged();
            return;
        }
        if (day > be.lastDay) {
            be.lastDay = day;
            be.sell(server);
        }
    }

    /** 지금 바로 판매 (아침 정산 · 시험 명령어) */
    public void sell(ServerLevel server) {
        long centi = remainderCenti;
        int sold = 0;
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = items.getItem(i);
            int price = ShipPrices.centi(s);
            if (price > 0) {
                centi += (long) price * s.getCount();
                sold += s.getCount();
                items.setItem(i, ItemStack.EMPTY);
            }
        }
        long coins = centi / 100;
        remainderCenti = centi % 100;
        setChanged();
        if (sold == 0 || owner == null) return;
        WildData.get(server.getServer()).addBalance(owner, coins);
        ServerPlayer p = server.getServer().getPlayerList().getPlayer(owner);
        if (p != null) {
            p.sendSystemMessage(Component.translatable("message.wildplus.shipping.sold", sold, coins).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.getItems().stream().map(ItemStack::copy).toList());
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.putLong("last_day", lastDay);
        out.putLong("remainder", remainderCenti);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SIZE; i++) items.setItem(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        lastDay = in.getLongOr("last_day", -1L);
        remainderCenti = in.getLongOr("remainder", 0L);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            Containers.dropContents(server, pos, items);
        }
    }
}
