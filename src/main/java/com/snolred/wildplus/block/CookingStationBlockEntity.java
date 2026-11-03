package com.snolred.wildplus.block;

import com.snolred.wildplus.cooking.Cooking;
import com.snolred.wildplus.cooking.CookingData;
import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.registry.ModBlockEntities;
import com.snolred.wildplus.registry.ModItems14;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 조리 기구 공통: 재료 6칸(0~5) · 연료 1칸(6) · 완성 1칸(7).
 * 재료가 레시피에 맞으면(남는 재료 종류 없이) 연료를 태우며 조리하고, 끝나면 재료를 쓰고 완성 칸에 요리를 쌓는다.
 * 재료 등급 평균으로 요리 등급이 붙는다. 호퍼: 위·옆으로 재료(연료는 연료 칸), 아래로 완성품.
 */
public class CookingStationBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int INPUTS = 6, FUEL = 6, OUT = 7, SIZE = 8;
    private static final int[] TOP = {0, 1, 2, 3, 4, 5, 6}, BOTTOM = {7};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    public int progress, total, burn, burnMax;
    private int dishIndex = -1;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> burn;
                case 3 -> burnMax;
                case 4 -> dishIndex;
                default -> station().ordinal();
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public CookingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOKING.get(), pos, state);
    }

    public CookingData.Station station() {
        return getBlockState().getBlock() instanceof CookingStationBlock b ? b.station : CookingData.Station.PREP;
    }

    /** 연료 값 (틱) */
    public static int fuelTicks(ItemStack s) {
        if (s.is(Items.COAL) || s.is(Items.CHARCOAL)) return 1600;
        if (s.is(Items.COAL_BLOCK)) return 16000;
        if (s.is(Items.BLAZE_ROD)) return 2400;
        if (s.is(Items.DRIED_KELP_BLOCK)) return 4000;
        if (s.is(ItemTags.LOGS)) return 300;
        if (s.is(ItemTags.PLANKS)) return 300;
        if (s.is(Items.STICK)) return 100;
        return 0;
    }

    private List<ItemStack> inputs() {
        List<ItemStack> l = new ArrayList<>(INPUTS);
        for (int i = 0; i < INPUTS; i++) l.add(items.get(i));
        return l;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingStationBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        CookingData.Station st = be.station();
        boolean changed = false;
        if (be.burn > 0) be.burn--;
        CookingData.Dish d = Cooking.find(st, be.inputs());
        int idx = -1;
        if (d != null) for (int i = 0; i < CookingData.DISHES.length; i++) if (CookingData.DISHES[i] == d) idx = i;
        if (idx != be.dishIndex) {
            be.dishIndex = idx;
            be.progress = 0;
            changed = true;
        }
        if (d == null || !be.roomFor(d)) {
            if (be.progress > 0) be.progress = Math.max(0, be.progress - 2);
            if (changed) be.setChanged();
            return;
        }
        be.total = st.seconds * 20;
        if (st.fuel && be.burn <= 0) {
            ItemStack f = be.items.get(FUEL);
            int t = fuelTicks(f);
            if (t <= 0) return; // 연료 없음
            be.burn = be.burnMax = t;
            f.shrink(1);
            changed = true;
        }
        be.progress++;
        if (be.progress % 20 == 0) changed = true;
        if (be.progress >= be.total) {
            be.cook(sl, d);
            be.progress = 0;
            changed = true;
        }
        if (changed) be.setChanged();
    }

    private boolean roomFor(CookingData.Dish d) {
        ItemStack out = items.get(OUT);
        if (out.isEmpty()) return true;
        return out.is(ModItems14.dish(d.id())) && out.getCount() + d.count() <= out.getMaxStackSize();
    }

    private void cook(ServerLevel level, CookingData.Dish d) {
        int[] take = Cooking.plan(d, inputs());
        if (take == null) return;
        List<ItemStack> used = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) {
            if (take[i] <= 0) continue;
            ItemStack s = items.get(i);
            used.add(s.copyWithCount(take[i]));
            ItemStack remainder = s.getItem().getCraftingRemainder(s);
            s.shrink(take[i]);
            if (!remainder.isEmpty()) {
                if (s.isEmpty()) items.set(i, remainder.copyWithCount(take[i]));
                else Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, remainder);
            }
        }
        ItemStack result = new ItemStack(ModItems14.dish(d.id()), d.count());
        int g = Cooking.grade(used, level.getRandom());
        Grade.apply(result, g);
        ItemStack out = items.get(OUT);
        if (out.isEmpty()) items.set(OUT, result);
        else if (ItemStack.isSameItemSameComponents(out, result)) out.grow(result.getCount());
        else Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, result);
        level.playSound(null, worldPosition, switch (station()) {
            case GRILL, STOVE -> SoundEvents.FIRE_EXTINGUISH;
            case PREP -> SoundEvents.SHEEP_SHEAR;
            case AGING -> SoundEvents.BARREL_CLOSE;
            default -> SoundEvents.BREWING_STAND_BREW;
        }, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    // ───────── 보관함 ─────────
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir == Direction.DOWN || slot == OUT) return false;
        boolean fuel = fuelTicks(stack) > 0 && !stack.is(Items.STICK);
        if (slot == FUEL) return station().fuel && fuel;
        if (fuel && station().fuel) return false; // 연료는 연료 칸으로
        // 같은 재료가 이미 있는 칸이나 빈 칸
        ItemStack here = items.get(slot);
        return here.isEmpty() || ItemStack.isSameItemSameComponents(here, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUT && dir == Direction.DOWN;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == OUT) return false;
        if (slot == FUEL) return station().fuel && fuelTicks(stack) > 0;
        return true;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player, 8.0F);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.stream().map(ItemStack::copy).toList());
        out.putInt("progress", progress);
        out.putInt("burn", burn);
        out.putInt("burnMax", burnMax);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SIZE; i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
        burn = in.getIntOr("burn", 0);
        burnMax = in.getIntOr("burnMax", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) Containers.dropContents(server, pos, items);
    }
}
