package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.Tax;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 카지노 테이블 (도박장).
 * 동전 던지기: 앞/뒤 맞히면 1.95배 · 주사위: 소(1~3)/대(4~6) 1.9배, 6 맞히기 5배
 * 슬롯머신: 같은 그림 3개 = 체리 5배 · 레몬 8배 · 종 20배 · 7 50배 · 다이아 100배, 2개 같으면 절반 돌려받음
 * 판돈은 1~10,000동, 이긴 돈(순이익)에는 소득세가 붙는다.
 */
public class CasinoMenu extends AbstractContainerMenu {
    public static final int BET_10 = 0, BET_100 = 1, BET_1000 = 2, BET_RESET = 3, BET_DOUBLE = 4;
    public static final int COIN_HEADS = 10, COIN_TAILS = 11, DICE_SMALL = 20, DICE_BIG = 21, DICE_SIX = 22, SLOTS = 30;
    public static final int GAME_COIN = 1, GAME_DICE = 2, GAME_SLOTS = 3;
    public static final String[] SYMBOLS = {"체리", "레몬", "종", "7", "◆"};
    private static final int[] WEIGHTS = {40, 30, 15, 10, 5};
    private static final int[] TRIPLE = {5, 8, 20, 50, 100};
    public static final long MAX_BET = 10_000;

    private final ContainerData data;
    private final @Nullable BlockPos pos;
    private long bet = 10;
    private int game, r1, r2, r3, outcome;
    private long win;

    public CasinoMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.CASINO.get(), id);
        this.pos = null;
        this.data = new SimpleContainerData(11);
        addDataSlots(data);
    }

    private CasinoMenu(int id, ServerPlayer player, BlockPos pos) {
        super(ModMenus.CASINO.get(), id);
        this.pos = pos;
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> (int) (bet % 10000);
                    case 1 -> (int) (bet / 10000);
                    case 2 -> (int) (Coins.totalValue(player) % 10000);
                    case 3 -> (int) Math.min(32767, Coins.totalValue(player) / 10000);
                    case 4 -> game;
                    case 5 -> r1;
                    case 6 -> r2;
                    case 7 -> r3;
                    case 8 -> (int) (win % 10000);
                    case 9 -> (int) Math.min(32767, win / 10000);
                    default -> outcome;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 11;
            }
        };
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CasinoMenu(id, player, pos),
                Component.translatable("container.wildplus.casino")), buf -> {});
    }

    public long bet() { return data.get(0) + data.get(1) * 10000L; }
    public long cash() { return data.get(2) + data.get(3) * 10000L; }
    public int game() { return data.get(4); }
    public int r1() { return data.get(5); }
    public int r2() { return data.get(6); }
    public int r3() { return data.get(7); }
    public long win() { return data.get(8) + data.get(9) * 10000L; }
    /** 0 없음 · 1 이김 · 2 짐 · 3 일부 돌려받음 */
    public int outcome() { return data.get(10); }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        switch (id) {
            case BET_10 -> bet = Math.min(MAX_BET, bet + 10);
            case BET_100 -> bet = Math.min(MAX_BET, bet + 100);
            case BET_1000 -> bet = Math.min(MAX_BET, bet + 1000);
            case BET_RESET -> bet = 10;
            case BET_DOUBLE -> bet = Math.min(MAX_BET, bet * 2);
            case COIN_HEADS, COIN_TAILS, DICE_SMALL, DICE_BIG, DICE_SIX, SLOTS -> play(sp, id);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void play(ServerPlayer sp, int id) {
        if (bet <= 0 || !Coins.pay(sp, bet)) {
            sp.sendOverlayMessage(Component.translatable("message.wildplus.casino.no_money", bet).withStyle(ChatFormatting.RED));
            return;
        }
        RandomSource rnd = sp.getRandom();
        long gross = 0; // 돌려받는 총액 (판돈 포함)
        if (id == COIN_HEADS || id == COIN_TAILS) {
            game = GAME_COIN;
            r1 = rnd.nextInt(2);
            if ((r1 == 0) == (id == COIN_HEADS)) gross = bet * 195 / 100;
        } else if (id == DICE_SMALL || id == DICE_BIG || id == DICE_SIX) {
            game = GAME_DICE;
            r1 = 1 + rnd.nextInt(6);
            boolean ok = id == DICE_SMALL ? r1 <= 3 : id == DICE_BIG ? r1 >= 4 : r1 == 6;
            if (ok) gross = id == DICE_SIX ? bet * 5 : bet * 190 / 100;
        } else {
            game = GAME_SLOTS;
            r1 = spin(rnd);
            r2 = spin(rnd);
            r3 = spin(rnd);
            if (r1 == r2 && r2 == r3) gross = bet * TRIPLE[r1];
            else if (r1 == r2 || r2 == r3 || r1 == r3) gross = bet / 2;
        }
        long profit = gross - bet;
        if (profit > 0) {
            long net = Tax.collect(sp.level().getServer(), sp.getUUID(), profit);
            gross = bet + net;
        }
        win = gross;
        outcome = gross > bet ? 1 : gross > 0 ? 3 : 2;
        if (gross > 0) Coins.refund(sp, gross);
        sp.level().playSound(null, sp.blockPosition(), outcome == 1 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS, 0.6F, outcome == 1 ? 1.4F : 1.0F);
        if (game == GAME_SLOTS && r1 == r2 && r2 == r3 && r1 >= 3) {
            sp.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.casino.jackpot",
                    sp.getName(), SYMBOLS[r1], gross).withStyle(ChatFormatting.GOLD), false);
        }
    }

    private static int spin(RandomSource rnd) {
        int roll = rnd.nextInt(100);
        for (int i = 0; i < WEIGHTS.length; i++) {
            roll -= WEIGHTS[i];
            if (roll < 0) return i;
        }
        return 0;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return pos == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }
}
