package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Grade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 흙 영양 (비료): 경작지 칸마다 비료 단계(1 일반 · 2 고급 · 3 황금)와 남은 수확 횟수를 기억한다.
 * <ul>
 *   <li>비료가 든 칸의 작물은 더 빨리 자란다 (2초마다 단계 × 6% 확률로 한 단계 자람 · 밝을 때). 밀 기준 일반 약 4분 · 황금 약 1분 반.</li>
 *   <li>다 자란 작물을 거두면 품질(은 · 금 · 무지개)이 붙는다. 비료 단계가 높을수록, 물이 촉촉할수록 잘 붙는다.</li>
 *   <li>비료 한 번에 수확 3번까지. 경작지가 사라지면 지워진다.</li>
 * </ul>
 */
public final class Soil {
    private Soil() {}

    public static final int USES = 3;
    /** 비료 단계별 품질 확률 (%): [은, 금, 무지개] */
    private static final int[][] CHANCE = {{4, 0, 0}, {25, 6, 1}, {30, 15, 3}, {30, 28, 8}};

    private static String key(ServerLevel level, BlockPos p) {
        return level.dimension().identifier() + "|" + p.getX() + "|" + p.getY() + "|" + p.getZ();
    }

    private static CompoundTag data(MinecraftServer s) {
        return ContentData.get(s).section("soil");
    }

    /** 이 칸(경작지)의 비료 단계 (없으면 0) */
    public static int tier(ServerLevel level, BlockPos farmland) {
        return data(level.getServer()).getIntOr(key(level, farmland), 0) / 10;
    }

    /** 경작지에 비료를 준다. 더 낮은 단계로 덮어쓰지 않음. 바뀌었으면 true */
    public static boolean fertilize(ServerLevel level, BlockPos farmland, int tier) {
        if (!level.getBlockState(farmland).is(Blocks.FARMLAND)) return false;
        CompoundTag d = data(level.getServer());
        String k = key(level, farmland);
        int cur = d.getIntOr(k, 0);
        if (cur / 10 > tier || (cur / 10 == tier && cur % 10 >= USES)) return false;
        d.putInt(k, tier * 10 + USES);
        ContentData.get(level.getServer()).dirty();
        level.sendParticles(tier >= 3 ? ParticleTypes.WAX_ON : ParticleTypes.HAPPY_VILLAGER,
                farmland.getX() + 0.5, farmland.getY() + 1.1, farmland.getZ() + 0.5, 4 + tier * 2, 0.3, 0.1, 0.3, 0);
        return true;
    }

    /** 작물 아래 경작지를 찾는다 (작물 자리 그대로 들어와도 됨) */
    public static BlockPos farmlandOf(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).is(Blocks.FARMLAND) ? p : p.below();
    }

    /** 수확물에 품질을 붙이고 비료를 한 번 쓴다. 자동 수확기 · 손 수확 공용 */
    public static void gradeHarvest(ServerLevel level, BlockPos cropPos, BlockState state, List<ItemStack> drops) {
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || drops.isEmpty()) return;
        BlockPos fl = cropPos.below();
        CompoundTag d = data(level.getServer());
        String k = key(level, fl);
        int cur = d.getIntOr(k, 0);
        int tier = cur / 10;
        RandomSource r = level.getRandom();
        int[] c = CHANCE[Math.min(3, tier)];
        BlockState soil = level.getBlockState(fl);
        float wet = soil.hasProperty(BlockStateProperties.MOISTURE) && soil.getValue(BlockStateProperties.MOISTURE) >= 7 ? 1.1F : 1.0F;
        int roll = r.nextInt(1000);
        int g = roll < c[2] * 10 * wet ? 3 : roll < (c[2] + c[1]) * 10 * wet ? 2 : roll < (c[2] + c[1] + c[0]) * 10 * wet ? 1 : 0;
        if (g > 0) {
            // 씨앗(이 작물을 심는 아이템)이 아닌 것에만. 열매가 곧 씨앗인 작물(당근 · 감자)은 전부
            boolean hasProduce = false;
            for (ItemStack s : drops) if (!plants(s, crop)) hasProduce = true;
            for (ItemStack s : drops) if (!hasProduce || !plants(s, crop)) Grade.apply(s, g);
            level.sendParticles(g >= 3 ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, cropPos.getX() + 0.5, cropPos.getY() + 0.6,
                    cropPos.getZ() + 0.5, 3 + g * 3, 0.3, 0.3, 0.3, 0.02);
        }
        if (tier > 0) {
            int left = cur % 10 - 1;
            if (left <= 0) d.remove(k);
            else d.putInt(k, tier * 10 + left);
            ContentData.get(level.getServer()).dirty();
        }
    }

    private static boolean plants(ItemStack s, CropBlock crop) {
        return s.getItem() instanceof BlockItem bi && bi.getBlock() == crop;
    }

    /** 손 · 도구로 거둘 때 */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getState().getBlock() instanceof CropBlock)) return;
        List<ItemStack> list = new ArrayList<>();
        for (ItemEntity e : event.getDrops()) list.add(e.getItem());
        gradeHarvest(event.getLevel(), event.getPos(), event.getState(), list);
    }

    /** 비료 칸 작물 빨리 키우기 (2초마다) */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 40 != 17) return;
        CompoundTag d = data(s);
        if (d.isEmpty()) return;
        List<String> gone = new ArrayList<>();
        for (String k : d.keySet()) {
            String[] parts = k.split("\\|");
            if (parts.length != 4) {
                gone.add(k);
                continue;
            }
            ServerLevel level = null;
            for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(parts[0])) level = l;
            if (level == null) continue;
            BlockPos fl = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            if (!level.isLoaded(fl)) continue;
            if (!level.getBlockState(fl).is(Blocks.FARMLAND)) {
                gone.add(k);
                continue;
            }
            BlockPos up = fl.above();
            BlockState st = level.getBlockState(up);
            int tier = d.getIntOr(k, 0) / 10;
            // 예전엔 randomTick 을 한 번 더 불렀는데, 그 안에서 빛 · 수분 · 주변 작물로 또 확률을 굴려서 거의 안 자랐음 → 직접 한 단계 올림
            if (st.getBlock() instanceof CropBlock crop && !crop.isMaxAge(st) && level.getRawBrightness(up, 0) >= 9
                    && level.getRandom().nextFloat() < tier * 0.06F) {
                level.setBlock(up, crop.getStateForAge(crop.getAge(st) + 1), 2);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, up.getX() + 0.5, up.getY() + 0.4, up.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0);
            }
        }
        if (!gone.isEmpty()) {
            for (String k : gone) d.remove(k);
            ContentData.get(s).dirty();
        }
    }
}
