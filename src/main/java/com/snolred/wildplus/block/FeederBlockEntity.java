package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 자동 사료통 (9칸): 반경 8칸 동물에게 30초마다 먹이를 줘서 번식시키고(근처 동물 24마리까지),
 * 5초마다 바닥의 알·깃털·양털을 주워 담고, 빈 양동이가 있으면 소에게서 우유를 받는다.
 */
public class FeederBlockEntity extends BoxBlockEntity {
    public static final int SIZE = 9, RADIUS = 8, CAP = 24;

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDER.get(), pos, state, SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        long t = level.getGameTime();
        AABB box = new AABB(pos).inflate(RADIUS, 3, RADIUS);
        if (t % 100 == 17) be.collect(sl, box);
        if (t % 600 == 41) be.feed(sl, pos, box);
    }

    private static boolean collectible(ItemStack s) {
        String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return (path.endsWith("egg") && !path.contains("spawn") && !path.contains("dragon") && !path.contains("sniffer") && !path.contains("turtle"))
                || s.is(Items.FEATHER) || s.is(ItemTags.WOOL) || s.is(Items.LEATHER) || s.is(Items.RABBIT_HIDE);
    }

    private void collect(ServerLevel level, AABB box) {
        for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, box.deflate(2, 0, 2))) {
            if (!it.isAlive() || !collectible(it.getItem())) continue;
            ItemStack left = insert(it.getItem().copy());
            if (left.isEmpty()) it.discard();
            else it.setItem(left);
        }
    }

    private void feed(ServerLevel level, BlockPos pos, AABB box) {
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box);
        boolean fed = false;
        if (animals.size() < CAP) {
            for (Animal a : animals) {
                if (a.getAge() != 0 || !a.canFallInLove()) continue;
                for (int i = 0; i < SIZE; i++) {
                    ItemStack s = items.get(i);
                    if (!s.isEmpty() && a.isFood(s)) {
                        s.shrink(1);
                        a.setInLove(null);
                        level.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + a.getBbHeight(), a.getZ(), 2, 0.2, 0.2, 0.2, 0);
                        fed = true;
                        break;
                    }
                }
            }
        }
        // 우유
        for (Animal a : animals) {
            if (a.getType() != EntityTypes.COW || a.isBaby()) continue;
            for (int i = 0; i < SIZE; i++) {
                if (items.get(i).is(Items.BUCKET)) {
                    ItemStack bucket = items.get(i);
                    bucket.shrink(1);
                    ItemStack left = insert(new ItemStack(Items.MILK_BUCKET));
                    if (!left.isEmpty()) { // 자리가 없으면 양동이를 되돌림
                        if (bucket.isEmpty()) items.set(i, new ItemStack(Items.BUCKET));
                        else bucket.grow(1);
                        break;
                    }
                    level.playSound(null, a.blockPosition(), SoundEvents.COW_MILK, SoundSource.BLOCKS, 0.6F, 1.0F);
                    fed = true;
                    break;
                }
            }
            break;
        }
        if (fed) setChanged();
    }
}
