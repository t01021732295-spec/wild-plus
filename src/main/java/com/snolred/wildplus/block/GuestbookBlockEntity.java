package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 방명록: 집 주인 */
public class GuestbookBlockEntity extends BlockEntity {
    public UUID owner;
    public String ownerName = "";

    public GuestbookBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUESTBOOK.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = in.getStringOr("owner_name", "");
    }
}
