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
 * 농작물 자동 수확기: 스프링클러 바로 밑에 설치하면 스프링클러 범위(9×9) 안의 다 자란 작물을 2초마다 수확한다.
 * 수확에서 나온 씨앗은 씨앗 칸(종류마다 32개까지)에 먼저 채우고, 나머지 작물·남는 씨앗은 작물 칸으로.
 * 자동 심기가 켜져 있으면 씨앗 칸의 씨앗으로 그 자리와 빈 경작지에 다시 심는다.
 * 칸: 0~8 씨앗 / 9~26 작물. 호퍼로 씨앗을 넣고 작물을 뺄 수 있다.
 */
public class AutoHarvesterBlockEntity extends MachineBlockEntity {
    public static final int SEEDS = 9, OUTPUT = 18, SEED_RESERVE = 16;
    private boolean autoPlant = true;

    public boolean autoPlant() {
        return autoPlant;
    }

    public void toggleAutoPlant() {
        autoPlant = !autoPlant;
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("auto_plant", autoPlant);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        autoPlant = in.getBooleanOr("auto_plant", true);
    }

    /** 이 아이템이 씨앗(심으면 작물이 되는 것)인지 */
    public static boolean isSeed(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock;
    }

    private int seedCount(Item item) {
        int n = 0;
        for (int i = 0; i < SEEDS; i++) if (items.get(i).is(item)) n += items.get(i).getCount();
        return n;
    }

    /** 이름이 "~씨앗"인 진짜 씨앗 (당근·감자처럼 먹는 작물은 아님) */
    public static boolean isPureSeed(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("seeds");
    }

    /**
     * 수확한 씨앗을 씨앗 칸에 채우고 남은 것을 돌려준다.
     * 진짜 씨앗은 씨앗 칸이 꽉 찰 때까지 전부, 당근·감자처럼 작물이 곧 씨앗인 것은 다시 심을 만큼(SEED_RESERVE)만.
     */
    private ItemStack storeSeed(ItemStack stack) {
        int room = isPureSeed(stack) ? Integer.MAX_VALUE : SEED_RESERVE - seedCount(stack.getItem());
        if (room <= 0) return stack;
        ItemStack part = stack.split(Math.min(room, stack.getCount()));
        for (int i = 0; i < SEEDS && !part.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (ItemStack.isSameItemSameComponents(s, part) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(part.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                part.shrink(move);
            }
        }
        for (int i = 0; i < SEEDS && !part.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, part.copy());
                part.setCount(0);
            }
        }
        stack.grow(part.getCount()); // 칸이 없어 못 넣은 만큼 되돌림
        setChanged();
        return stack;
    }

    public AutoHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_HARVESTER.get(), pos, state, SEEDS, OUTPUT);
    }

    @Override
    public boolean isValidInput(ItemStack stack) {
        return isSeed(stack);
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
                    } else if (be.autoPlant && s.getBlock() instanceof FarmlandBlock && server.getBlockState(p.above()).isAir()) {
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
        // 씨앗은 씨앗 칸으로 먼저
        for (ItemStack d : drops) {
            if (!d.isEmpty() && isSeed(d)) storeSeed(d);
        }
        // 다시 심기 (자동 심기 켜짐 + 씨앗 칸에 그 씨앗이 있을 때)
        boolean replant = autoPlant && takeSeed(seed);
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
