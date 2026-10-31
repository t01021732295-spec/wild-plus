package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 농작물 자동 수확기: 스프링클러 바로 밑에 설치하면 스프링클러 범위(9×9) 안의 다 자란 작물을 2초마다 수확하고,
 * 씨앗 칸에 씨앗이 있으면 그 자리(와 비어 있는 경작지)에 다시 심는다.
 * 칸: 0~8 씨앗 / 9~26 수확물. 호퍼로 씨앗을 넣고 수확물을 뺄 수 있다.
 */
public class AutoHarvesterBlockEntity extends MachineBlockEntity {
    public static final int SEEDS = 9, OUTPUT = 18;

    public AutoHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_HARVESTER.get(), pos, state, SEEDS, OUTPUT);
    }

    @Override
    public boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock;
    }

    public boolean hasSprinkler() {
        return level != null && level.getBlockState(worldPosition.above()).getBlock() instanceof SprinklerBlock;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoHarvesterBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 40 != 11 || !be.hasSprinkler()) return;
        BlockPos center = pos.above();
        int r = SprinklerBlock.RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState s = server.getBlockState(p);
                    if (s.getBlock() instanceof CropBlock crop) {
                        if (crop.isMaxAge(s)) be.harvest(server, p, s, crop);
                    } else if (s.getBlock() instanceof FarmlandBlock && server.getBlockState(p.above()).isAir()) {
                        be.plant(server, p.above(), null);
                    }
                }
            }
        }
    }

    private void harvest(ServerLevel level, BlockPos p, BlockState s, CropBlock crop) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(s, level, p, null));
        if (!hasRoomFor(drops)) return;
        Item seed = s.getBlock().asItem();
        // 다시 심을 씨앗: 씨앗 칸 → 없으면 수확물에서 하나
        boolean replant = takeSeed(seed);
        if (!replant) {
            for (ItemStack d : drops) {
                if (d.is(seed) && !d.isEmpty()) {
                    d.shrink(1);
                    replant = true;
                    break;
                }
            }
        }
        level.setBlock(p, replant ? crop.getStateForAge(0) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack d : drops) {
            if (!d.isEmpty()) {
                ItemStack rest = addOutput(d);
                if (!rest.isEmpty()) Block.popResource(level, worldPosition.above(2), rest);
            }
        }
    }

    /** 빈 경작지에 씨앗 칸의 씨앗을 심는다 */
    private void plant(ServerLevel level, BlockPos p, Item only) {
        for (int i = 0; i < SEEDS; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock crop) {
                BlockState st = crop.defaultBlockState();
                if (st.canSurvive(level, p)) {
                    level.setBlock(p, st, Block.UPDATE_ALL);
                    s.shrink(1);
                    setChanged();
                }
                return;
            }
        }
    }

    private boolean takeSeed(Item seed) {
        for (int i = 0; i < SEEDS; i++) {
            ItemStack s = items.get(i);
            if (s.is(seed)) {
                s.shrink(1);
                setChanged();
                return true;
            }
        }
        return false;
    }
}
