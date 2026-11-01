package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import com.snolred.wildplus.registry.ModItems2;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 광석 채굴기: 화면에서 광물 하나를 고르면 현실 시간으로 분당 1~2개(30~60초마다 하나)씩 캐서 결과 칸에 쌓는다.
 * 청크가 꺼져 있던 동안의 시간도 다시 켜질 때 몰아서 계산한다 (최대 8시간, 결과 칸이 차면 멈춤).
 * 칸: 0~17 결과. 호퍼로 뺄 수 있다.
 */
public class OreMinerBlockEntity extends MachineBlockEntity {
    public static final int OUTPUT = 18;
    private static final long MAX_CATCHUP_MS = 8L * 60 * 60 * 1000;

    public record Ore(String name, Supplier<Item> item, int count) {}

    public static final List<Ore> ORES = List.of(
            new Ore("석탄", () -> Items.COAL, 1),
            new Ore("구리 원석", () -> Items.RAW_COPPER, 1),
            new Ore("철 원석", () -> Items.RAW_IRON, 1),
            new Ore("금 원석", () -> Items.RAW_GOLD, 1),
            new Ore("레드스톤", () -> Items.REDSTONE, 2),
            new Ore("청금석", () -> Items.LAPIS_LAZULI, 2),
            new Ore("석영", () -> Items.QUARTZ, 1),
            new Ore("다이아몬드", () -> Items.DIAMOND, 1),
            new Ore("에메랄드", () -> Items.EMERALD, 1),
            new Ore("미스릴 원석", () -> ModItems2.RAW_MYTHRIL.get(), 1),
            new Ore("비취", () -> ModItems2.JADE.get(), 1),
            new Ore("루비", () -> com.snolred.wildplus.registry.ModItems7.RUBY.get(), 1));

    private int selected = -1;
    private long lastMillis;
    private long progressMs;
    private long nextMs = 45_000;

    public OreMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_MINER.get(), pos, state, 0, OUTPUT);
    }

    @Override
    public boolean isValidInput(ItemStack stack) {
        return false;
    }

    public int selected() {
        return selected;
    }

    public void select(int i) {
        if (i >= -1 && i < ORES.size() && i != selected) {
            selected = i;
            progressMs = 0;
            lastMillis = System.currentTimeMillis();
            setChanged();
        }
    }

    /** 다음 광물까지 남은 초 */
    public int secondsLeft() {
        return selected < 0 ? 0 : (int) Math.max(0, (nextMs - progressMs) / 1000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OreMinerBlockEntity be) {
        if (level.getGameTime() % 20 != 5) return;
        long now = System.currentTimeMillis();
        if (be.lastMillis == 0) be.lastMillis = now;
        long elapsed = Math.min(MAX_CATCHUP_MS, Math.max(0, now - be.lastMillis));
        be.lastMillis = now;
        if (be.selected < 0) return;
        be.progressMs += elapsed;
        Ore ore = ORES.get(be.selected);
        while (be.progressMs >= be.nextMs) {
            if (be.outputFull()) {
                be.progressMs = be.nextMs; // 꽉 차면 대기
                break;
            }
            be.progressMs -= be.nextMs;
            be.nextMs = 30_000 + level.getRandom().nextInt(30_001);
            be.addOutput(new ItemStack(ore.item().get(), ore.count()));
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("selected", selected);
        out.putLong("last", lastMillis);
        out.putLong("progress", progressMs);
        out.putLong("next", nextMs);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        selected = in.getIntOr("selected", -1);
        lastMillis = in.getLongOr("last", 0L);
        progressMs = in.getLongOr("progress", 0L);
        nextMs = in.getLongOr("next", 45_000L);
    }
}
