package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Warps;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** 워프 포인트 블록 엔티티: 월드 데이터의 워프 항목 번호만 기억한다 */
public class WarpPointBlockEntity extends BlockEntity {
    private @Nullable UUID warpId;

    public WarpPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARP_POINT.get(), pos, state);
    }

    public @Nullable UUID warpId() {
        return warpId;
    }

    public void setWarpId(UUID id) {
        this.warpId = id;
        setChanged();
    }

    /** 주인(또는 주인 없는 공용 워프면 누구나)인지 */
    public boolean canBreak(Player player) {
        if (!(level instanceof ServerLevel server) || warpId == null) return true;
        WildData.Warp w = WildData.get(server.getServer()).warps.get(warpId);
        return w == null || w.owner.equals(new UUID(0L, 0L)) || w.owner.equals(player.getUUID()) || player.isCreative();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (warpId != null) out.store("warp_id", UUIDUtil.CODEC, warpId);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        warpId = in.read("warp_id", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            Warps.remove(server, warpId);
        }
    }
}
