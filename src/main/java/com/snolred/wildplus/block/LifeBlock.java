package com.snolred.wildplus.block;

import com.snolred.wildplus.content.LifeStations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 5.0 생활 시설 (양봉 상자 · 건조대 · 장독대 · 양어장). 내용물은 ContentData 에 자리별로 저장 → 부수면 쏟아 준다 */
public class LifeBlock extends BigBlock {
    public final LifeStations.Kind kind;

    public LifeBlock(Properties properties, LifeStations.Kind kind, int lat0, int lat1) {
        super(properties, (p, pos) -> LifeStations.use(p, pos, kind), lat0, lat1, 0, 0, 1, null);
        this.kind = kind;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) LifeStations.spill(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
