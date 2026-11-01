package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 자동 제작대.
 * <ul>
 *   <li>설계도 3×3 (칸당 1개 견본): 만들 물건의 조합법을 제작대처럼 놓는다.</li>
 *   <li>재료 칸 9개(0~8): 설계도에 쓰인 아이템만 들어간다. 호퍼로 옆·위에서 넣을 수 있다.</li>
 *   <li>결과 칸(9): 0.4초마다 하나씩 만들어 쌓는다. 아래 호퍼로 꺼낼 수 있다.</li>
 *   <li>레드스톤 신호를 받으면 멈춘다.</li>
 * </ul>
 */
public class AutoCrafterBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int BUFFER = 9, OUT = 9, SIZE = 10, INTERVAL = 8;
    private static final int[] IN_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUT_SLOTS = {OUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** 설계도 견본 9칸 */
    public final SimpleContainer pattern = new SimpleContainer(9) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AutoCrafterBlockEntity.this.recipeDirty = true;
            AutoCrafterBlockEntity.this.setChanged();
        }
    };
    /** 만들어질 물건 미리보기 (저장 안 함, 꺼낼 수 없음) */
    public final SimpleContainer preview = new SimpleContainer(1);
    private boolean recipeDirty = true;
    private @Nullable RecipeHolder<CraftingRecipe> recipe;
    private int cooldown;
    public int crafted;

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state);
    }

    // ───────── 제작 ─────────

    private CraftingInput patternInput() {
        List<ItemStack> list = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) list.add(pattern.getItem(i).copyWithCount(1));
        return CraftingInput.of(3, 3, list);
    }

    private void refreshRecipe(ServerLevel level) {
        recipeDirty = false;
        recipe = null;
        ItemStack show = ItemStack.EMPTY;
        if (!pattern.isEmpty()) {
            CraftingInput in = patternInput();
            Optional<RecipeHolder<CraftingRecipe>> r = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level);
            if (r.isPresent()) {
                recipe = r.get();
                show = recipe.value().assemble(in);
            }
        }
        preview.setItem(0, show);
    }

    /** 설계도 칸 i의 견본과 같은 아이템인가 (내구도 등 세부는 무시) */
    private boolean usedInPattern(ItemStack s) {
        for (int i = 0; i < 9; i++) {
            ItemStack p = pattern.getItem(i);
            if (!p.isEmpty() && ItemStack.isSameItem(p, s)) return true;
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoCrafterBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.recipeDirty) be.refreshRecipe(sl);
        if (--be.cooldown > 0) return;
        be.cooldown = INTERVAL;
        if (be.recipe == null || level.hasNeighborSignal(pos)) return;
        if (be.craftOnce(sl, pos)) be.setChanged();
    }

    private boolean craftOnce(ServerLevel level, BlockPos pos) {
        CraftingInput in = patternInput();
        ItemStack result = recipe.value().assemble(in);
        if (result.isEmpty()) return false;
        ItemStack out = items.get(OUT);
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, result) || out.getCount() + result.getCount() > out.getMaxStackSize())) {
            return false;
        }
        // 필요한 재료를 칸별로 고른다 (같은 재료가 여러 칸에 나뉘어 있어도 됨)
        int[] take = new int[BUFFER];
        List<ItemStack> realGrid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack p = pattern.getItem(i);
            if (p.isEmpty()) {
                realGrid.add(ItemStack.EMPTY);
                continue;
            }
            int found = -1;
            for (int b = 0; b < BUFFER; b++) {
                ItemStack s = items.get(b);
                if (!s.isEmpty() && ItemStack.isSameItem(s, p) && s.getCount() - take[b] > 0) {
                    found = b;
                    break;
                }
            }
            if (found < 0) return false;
            take[found]++;
            realGrid.add(items.get(found).copyWithCount(1));
        }
        // 실제 재료로 다시 확인 (부서진 도구 등 세부 조건 대비)
        CraftingInput real = CraftingInput.of(3, 3, realGrid);
        if (!recipe.value().matches(real, level)) return false;
        result = recipe.value().assemble(real);
        NonNullList<ItemStack> remain = recipe.value().getRemainingItems(real);
        for (int b = 0; b < BUFFER; b++) if (take[b] > 0) items.get(b).shrink(take[b]);
        if (out.isEmpty()) items.set(OUT, result.copy());
        else out.grow(result.getCount());
        for (ItemStack r : remain) {
            if (r.isEmpty()) continue;
            ItemStack left = insertBuffer(r.copy());
            if (!left.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, left);
        }
        crafted++;
        if (crafted % 8 == 1) level.playSound(null, pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 0.35F, 1.0F);
        return true;
    }

    /** 재료 칸에 넣고 남은 것을 돌려준다 (설계도에 쓰이는 것만) */
    private ItemStack insertBuffer(ItemStack stack) {
        if (!usedInPattern(stack)) return stack;
        for (int b = 0; b < BUFFER && !stack.isEmpty(); b++) {
            ItemStack s = items.get(b);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int m = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(m);
                stack.shrink(m);
            }
        }
        for (int b = 0; b < BUFFER && !stack.isEmpty(); b++) {
            if (items.get(b).isEmpty()) {
                items.set(b, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    // ───────── 보관함 (호퍼 연결) ─────────

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUT_SLOTS : IN_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < BUFFER && dir != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUT && dir == Direction.DOWN;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < BUFFER && usedInPattern(stack);
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
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.stream().map(ItemStack::copy).toList());
        List<ItemStack> pat = new ArrayList<>();
        for (int i = 0; i < 9; i++) pat.add(pattern.getItem(i).copy());
        out.store("pattern", ItemStack.OPTIONAL_CODEC.listOf(), pat);
        out.putInt("crafted", crafted);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SIZE; i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        List<ItemStack> pat = in.read("pattern", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < 9; i++) pattern.getItems().set(i, i < pat.size() ? pat.get(i) : ItemStack.EMPTY);
        crafted = in.getIntOr("crafted", 0);
        recipeDirty = true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            Containers.dropContents(server, pos, items);
            Containers.dropContents(server, pos, pattern);
        }
    }
}
