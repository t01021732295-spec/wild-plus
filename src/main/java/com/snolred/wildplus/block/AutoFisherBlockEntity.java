package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * 자동 낚시 장치: 반경 2칸 안에 물이 있는 곳에 설치하고 낚싯대 칸(1칸)에 낚싯대를 넣으면 15~30초마다 낚시를 한다.
 * 낚싯대의 미끼(대기 시간 감소)·바다의 행운(행운)·비취 낚싯대 효과가 그대로 적용되고, 한 번 낚을 때마다 내구도가 1 줄어든다.
 * 칸: 0 낚싯대 / 1~18 잡은 것. 호퍼로 낚싯대를 넣고 잡은 것을 뺄 수 있다.
 */
public class AutoFisherBlockEntity extends MachineBlockEntity {
    public static final int OUTPUT = 18;
    private int timer = 400;
    private @org.jspecify.annotations.Nullable BlockPos water;

    public AutoFisherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_FISHER.get(), pos, state, 1, OUTPUT);
    }

    @Override
    public boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem && items.get(0).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    public static final int WATER_RADIUS = 2;

    /** 반경 2칸 안의 물 (가장 가까운 것), 없으면 null */
    public @org.jspecify.annotations.Nullable BlockPos findWater() {
        if (level == null) return null;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-WATER_RADIUS, -WATER_RADIUS, -WATER_RADIUS),
                worldPosition.offset(WATER_RADIUS, WATER_RADIUS, WATER_RADIUS))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) {
                double d = p.distSqr(worldPosition);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public boolean onWater() {
        return findWater() != null;
    }

    public int secondsLeft() {
        return timer / 20;
    }

    private static int level(ServerLevel level, ItemStack rod, ResourceKey<Enchantment> key) {
        Holder<Enchantment> h = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(h, rod);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoFisherBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        ItemStack rod = be.items.get(0);
        if (rod.isEmpty() || be.outputFull()) return;
        if (level.getGameTime() % 20 == 0) be.water = be.findWater();
        if (be.water == null) return;
        if (--be.timer > 0) {
            if (be.timer % 20 == 0) be.setChanged();
            return;
        }
        int lure = level(server, rod, Enchantments.LURE);
        int luck = level(server, rod, Enchantments.LUCK_OF_THE_SEA);
        be.timer = Math.max(100, 300 + level.getRandom().nextInt(301) - lure * 100);
        LootTable table = server.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(be.water))
                .withParameter(LootContextParams.TOOL, rod)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        for (ItemStack caught : table.getRandomItems(params)) {
            ItemStack rest = be.addOutput(caught);
            if (!rest.isEmpty()) net.minecraft.world.level.block.Block.popResource(server, pos.above(), rest);
        }
        server.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.4F, 1.0F);
        // 낚싯대 내구도
        rod.setDamageValue(rod.getDamageValue() + 1);
        if (rod.getDamageValue() >= rod.getMaxDamage()) {
            be.items.set(0, ItemStack.EMPTY);
            server.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("timer", timer);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        timer = in.getIntOr("timer", 400);
    }
}
