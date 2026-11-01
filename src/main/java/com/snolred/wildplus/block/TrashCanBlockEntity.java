package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 쓰레기통: 칸 9개짜리 보관함처럼 보이지만, 들어온 아이템은 바로 사라진다.
 * 호퍼·필터 호퍼·드로퍼가 넣어도, 손으로 넣어도 똑같이 지워짐. 칸이 늘 비어 있어서 막히지 않는다.
 */
public class TrashCanBlockEntity extends BoxBlockEntity {
    public static final int SIZE = 9;
    private long lastSound;
    public long deleted;

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN.get(), pos, state, SIZE);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return;
        deleted += stack.getCount();
        if (level != null && !level.isClientSide() && level.getGameTime() - lastSound > 20) {
            lastSound = level.getGameTime();
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.6F);
        }
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        setItem(0, stack.copy());
        return ItemStack.EMPTY;
    }
}
