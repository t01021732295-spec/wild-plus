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
 * 낚싯대 단계 · 미끼에 따라 희귀 어종 · 보물상자 · 두 마리 · 등급 확률이 달라진다 (FishingPlus).
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
        ItemStack rodStack = ctx.getOptional(LootContextParams.TOOL) instanceof ItemStack rs ? rs : ItemStack.EMPTY;
        com.snolred.wildplus.event.FishingPlus.Rod rod = com.snolred.wildplus.event.FishingPlus.rod(rodStack);
        Entity hook = ctx.getOptional(LootContextParams.THIS_ENTITY);
        boolean openWater = !(hook instanceof FishingHook fh) || fh.isOpenWaterFishing();
        // 사람이 낚았나 (자동 낚시 장치는 찌가 없음) · 미끼
        net.minecraft.server.level.ServerPlayer angler = hook instanceof FishingHook fh2 && fh2.getPlayerOwner() instanceof net.minecraft.server.level.ServerPlayer sp
                ? sp : null;
        int bait = angler != null ? com.snolred.wildplus.event.FishingPlus.bait(angler.getOffhandItem()) : 0;

        // 침몰한 보물상자: 3.9부터 "보물" 갈래에 포함 — 바닐라가 보물(이름표 · 안장 · 앵무조개 껍데기 · 마법 부여된 책 · 활 · 낚싯대)을
        // 골랐으면 절반은 보물상자로, 아니면 낚싯대 확률 × 행운(바다의 행운 · 행운 효과 · 행운의 부적)으로 보물상자 (탁 트인 물에서만)
        boolean vanillaTreasure = false;
        for (ItemStack s : loot) {
            if (s.is(net.minecraft.world.item.Items.NAME_TAG) || s.is(net.minecraft.world.item.Items.SADDLE) || s.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)
                    || s.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) || s.is(net.minecraft.world.item.Items.BOW) || s.is(net.minecraft.world.item.Items.FISHING_ROD)) {
                vanillaTreasure = true;
            }
        }
        float luck = Math.max(0F, ctx.getLuck());
        float chestChance = Math.min(0.6F, rod.chest() * (1F + 0.2F * luck));
        if (openWater && (vanillaTreasure ? random.nextFloat() < 0.5F : random.nextFloat() < chestChance)) {
            loot.clear();
            loot.add(com.snolred.wildplus.item.SunkenChestItem.make(com.snolred.wildplus.item.SunkenChestItem.roll(random,
                    rod.grade() * (1F + 0.05F * luck) * (angler == null ? 0.5F : 1F))));
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
        if (random.nextFloat() < rod.rare() + (bait == 2 ? 0.15F : 0F)) {
            Item fish = random.nextFloat() < rod.king() ? com.snolred.wildplus.registry.ModItems5.DRAGON_KING_FISH.get()
                    : pickFish(level, BlockPos.containing(origin), random);
            if (fish != null) {
                loot.set(fishIndex, new ItemStack(fish));
            }
        }
        // 등급: 낚싯대 × 미끼, 자동 낚시는 절반
        float m = rod.grade() * (bait == 1 ? 1.3F : bait == 2 ? 1.5F : bait == 3 ? 3F : 1F) * (angler == null ? 0.5F : 1F);
        if (com.snolred.wildplus.content.Village.done(level.getServer(), "harbor")) m *= 1.15F; // 3.5 마을 항구 완공
        int g = com.snolred.wildplus.event.FishingPlus.rollGrade(random, m);
        ItemStack caught = loot.get(fishIndex);
        com.snolred.wildplus.economy.Grade.apply(caught, g);
        if (random.nextFloat() < rod.twin() || (angler != null && com.snolred.wildplus.event.LuckyCharms.doubleFish(angler))) {
            loot.add(caught.copy());
        }
        if (angler != null) {
            if (bait > 0 && !angler.getAbilities().instabuild) angler.getOffhandItem().shrink(1);
            com.snolred.wildplus.event.FishingPlus.announce(angler, caught, g);
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
        // 8차 어종
        if (biome.is(Tags.Biomes.IS_SWAMP)) {
            add(pool, weights, com.snolred.wildplus.registry.ModItems5.CATFISH.get(), 14);
        } else if (biome.is(Tags.Biomes.IS_RIVER) && night) {
            add(pool, weights, com.snolred.wildplus.registry.ModItems5.CATFISH.get(), 8);
        }
        if (biome.is(Tags.Biomes.IS_OCEAN) && !night) {
            add(pool, weights, com.snolred.wildplus.registry.ModItems5.TUNA.get(), biome.is(Tags.Biomes.IS_DEEP_OCEAN) ? 14 : 8);
        }
        if (biome.is(Tags.Biomes.IS_RIVER) && !night) {
            add(pool, weights, com.snolred.wildplus.registry.ModItems5.SWEETFISH.get(), 9);
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
