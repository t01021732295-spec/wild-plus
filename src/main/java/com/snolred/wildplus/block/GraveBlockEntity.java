package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** 무덤: 죽은 플레이어의 아이템을 보관 */
public class GraveBlockEntity extends BlockEntity {
    private final List<ItemStack> items = new ArrayList<>();
    private @Nullable UUID owner;
    private String ownerName = "";
    private boolean emptied;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
    }

    public void fill(Player player, List<ItemStack> stacks) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
        this.items.clear();
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) items.add(s.copy());
        }
        setChanged();
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public String ownerName() { return ownerName; }
    public int count() { return items.size(); }

    /** 주인에게 모두 돌려준다 (가방이 차면 바닥에) */
    public void giveBack(Player player) {
        for (ItemStack s : items) {
            if (!player.getInventory().add(s) && !s.isEmpty()) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), s);
            }
        }
        items.clear();
        emptied = true;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.CODEC.listOf(), List.copyOf(items));
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items.clear();
        in.read("items", ItemStack.CODEC.listOf()).ifPresent(items::addAll);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = in.getStringOr("owner_name", "");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && !emptied) {
            for (ItemStack s : items) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
            items.clear();
        }
    }
}
