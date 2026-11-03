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
 * 광석 채굴기: 화면에서 광물 하나를 고르면 현실 시간 기준으로 캐서 결과 칸에 쌓는다.
 * - 광물마다 속도가 다르다: 비쌀수록 느림. 어떤 광물이든 1대가 시간당 출하가 약 1,000동이 되게 맞췄다
 *   (값싼 광물은 최소 15초에 하나 — 돈보다는 재료용).
 * - 강화 단계: 0단계 석탄~석영 / 1단계(다이아 블록 4개) 다이아 · 에메랄드 / 2단계(루비 블록 8개) 미스릴 · 비취 · 루비.
 *   채굴기를 그 블록을 들고 우클릭하면 강화된다.
 * 서버와 청크가 켜져 있을 때만 캔다 (꺼져 있던 시간은 계산하지 않음). 결과 칸이 차면 멈춘다.
 * 칸: 0~17 결과. 호퍼로 뺄 수 있다.
 */
public class OreMinerBlockEntity extends MachineBlockEntity {
    public static final int OUTPUT = 18;
    /** 오프라인 보충 없음: 1초마다 도는 틱 사이 시간만 센다 (서버 렉 대비로 최대 5초) */
    private static final long MAX_CATCHUP_MS = 5_000L;

    /** 광물: 이름 · 아이템 · 한 번에 개수 · 필요한 강화 단계 · 하나 캐는 데 걸리는 초 */
    public record Ore(String name, Supplier<Item> item, int count, int tier, int seconds) {}

    // 초 = 출하가(동) × 3.6 → 시간당 약 1,000동 (최소 15초)
    public static final List<Ore> ORES = List.of(
            new Ore("석탄", () -> Items.COAL, 1, 0, 15),
            new Ore("구리 원석", () -> Items.RAW_COPPER, 1, 0, 15),
            new Ore("철 원석", () -> Items.RAW_IRON, 1, 0, 15),
            new Ore("금 원석", () -> Items.RAW_GOLD, 1, 0, 15),
            new Ore("레드스톤", () -> Items.REDSTONE, 2, 0, 15),
            new Ore("청금석", () -> Items.LAPIS_LAZULI, 2, 0, 15),
            new Ore("석영", () -> Items.QUARTZ, 1, 0, 15),
            new Ore("다이아몬드", () -> Items.DIAMOND, 1, 1, 108),
            new Ore("에메랄드", () -> Items.EMERALD, 1, 1, 36),
            new Ore("미스릴 원석", () -> ModItems2.RAW_MYTHRIL.get(), 1, 2, 18),
            new Ore("비취", () -> ModItems2.JADE.get(), 1, 2, 18),
            new Ore("루비", () -> com.snolred.wildplus.registry.ModItems7.RUBY.get(), 1, 2, 144));

    public static final int MAX_TIER = 2;
    /** 강화 재료: 1단계 다이아 블록 4개, 2단계 루비 블록 8개 */
    public static final int[] UPGRADE_COUNT = {4, 8};
    public static final String[] TIER_NAME = {"기본", "1단계", "2단계"};

    private int tier;

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

    public int tier() {
        return tier;
    }

    /** 들고 있는 블록으로 강화. 되면 true */
    public boolean upgrade(ItemStack hand, net.minecraft.world.entity.player.Player p) {
        if (tier >= MAX_TIER) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("이미 최대 강화예요"));
            return false;
        }
        Item need = tier == 0 ? Items.DIAMOND_BLOCK : com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get();
        int n = UPGRADE_COUNT[tier];
        if (!hand.is(need)) return false;
        if (hand.getCount() < n && !p.getAbilities().instabuild) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("%s %d개가 필요해요", hand.getHoverName().getString(), n))
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        if (!p.getAbilities().instabuild) hand.shrink(n);
        tier++;
        setChanged();
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("⛏ 채굴기 " + TIER_NAME[tier] + " 강화 완료! "
                + (tier == 1 ? "다이아몬드 · 에메랄드" : "미스릴 · 비취 · 루비") + "를 캘 수 있어요").withStyle(net.minecraft.ChatFormatting.AQUA));
        return true;
    }

    public void select(int i) {
        if (i >= 0 && i < ORES.size() && ORES.get(i).tier() > tier) return; // 강화가 모자람
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
            int sec = ore.seconds();
            be.nextMs = sec * 800L + level.getRandom().nextInt(sec * 400 + 1); // 0.8~1.2배
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
        out.putInt("tier", tier);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        selected = in.getIntOr("selected", -1);
        lastMillis = in.getLongOr("last", 0L);
        progressMs = in.getLongOr("progress", 0L);
        nextMs = in.getLongOr("next", 45_000L);
        // 강화 단계가 없던 옛 채굴기: 지금 캐던 광물은 계속 캘 수 있게
        tier = in.getIntOr("tier", selected >= 0 && selected < ORES.size() ? ORES.get(selected).tier() : 0);
        if (selected >= 0 && selected < ORES.size()) nextMs = Math.min(nextMs, ORES.get(selected).seconds() * 1200L);
    }
}
