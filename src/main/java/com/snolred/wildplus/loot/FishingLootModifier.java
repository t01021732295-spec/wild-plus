package com.snolred.wildplus.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.snolred.wildplus.registry.ModItems2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 낚시 시스템: 바닐라 낚시 결과에 새 어종을 섞는다.
 * 잡히는 물고기는 장소(강·바다·추운 곳), 시간(낮·밤), 날씨(비·뇌우)에 따라 달라진다.
 * 비취 낚싯대를 쓰면 확률이 2배, 가끔 두 마리.
 */
public class FishingLootModifier extends LootModifier {
    public static final MapCodec<FishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst).apply(inst, FishingLootModifier::new));

    public FishingLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        Vec3 origin = ctx.getOptional(LootContextParams.ORIGIN);
        if (origin == null) {
            return loot;
        }
        ServerLevel level = ctx.getLevel();
        RandomSource random = ctx.getRandom();
        boolean jade = ctx.getOptional(LootContextParams.TOOL) instanceof ItemStack rod
                && rod.is(ModItems2.JADE_FISHING_ROD.get());
        Entity hook = ctx.getOptional(LootContextParams.THIS_ENTITY);
        boolean openWater = !(hook instanceof FishingHook fh) || fh.isOpenWaterFishing();

        // 침몰한 보물상자 (탁 트인 물에서만)
        float chestChance = jade ? 0.05F : 0.02F;
        if (openWater && random.nextFloat() < chestChance) {
            loot.clear();
            loot.add(new ItemStack(ModItems2.SUNKEN_CHEST.get()));
            return loot;
        }

        int fishIndex = -1;
        for (int i = 0; i < loot.size(); i++) {
            if (loot.get(i).is(ItemTags.FISHES)) {
                fishIndex = i;
                break;
            }
        }
        if (fishIndex < 0) {
            return loot; // 쓰레기·보물은 그대로
        }
        if (random.nextFloat() < (jade ? 0.5F : 0.25F)) {
            Item fish = pickFish(level, BlockPos.containing(origin), random);
            if (fish != null) {
                loot.set(fishIndex, new ItemStack(fish));
            }
        }
        if (jade && random.nextFloat() < 0.2F) {
            loot.add(loot.get(fishIndex).copy());
        }
        return loot;
    }

    private static Item pickFish(ServerLevel level, BlockPos pos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(pos);
        boolean night = level.isDarkOutside();
        List<Item> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        // 황금 잉어는 별도 확률 (새 어종 중 6% → 전체 물고기의 약 1.5%, 비취 낚싯대 3%)
        if (random.nextFloat() < 0.06F) {
            return ModItems2.GOLDEN_CARP.get();
        }
        if (biome.is(Tags.Biomes.IS_RIVER)) {
            add(pool, weights, ModItems2.RAINBOW_TROUT.get(), night ? 5 : 12);
        } else if (!night) {
            add(pool, weights, ModItems2.RAINBOW_TROUT.get(), 5);
        }
        if (night) {
            add(pool, weights, ModItems2.MOONLIGHT_FISH.get(), 8);
        }
        if (level.isRaining()) {
            add(pool, weights, ModItems2.STORM_EEL.get(), level.isThundering() ? 16 : 9);
        }
        if (biome.is(Tags.Biomes.IS_OCEAN)) {
            add(pool, weights, ModItems2.ANGLERFISH.get(), night ? 10 : 5);
        }
        if (biome.is(Tags.Biomes.IS_COLD)) {
            add(pool, weights, ModItems2.ICE_SMELT.get(), 10);
        }

        int total = 0;
        for (int w : weights) total += w;
        if (total <= 0) {
            return ModItems2.RAINBOW_TROUT.get();
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return pool.get(i);
            }
        }
        return null;
    }

    private static void add(List<Item> pool, List<Integer> weights, Item item, int weight) {
        pool.add(item);
        weights.add(weight);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
