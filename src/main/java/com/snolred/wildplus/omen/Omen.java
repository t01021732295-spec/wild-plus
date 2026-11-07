package com.snolred.wildplus.omen;

import com.snolred.wildplus.economy.WildData;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.OminousBottleAmplifier;

/**
 * 3.8 흉조 업데이트 공용: 흉조 테마(양조) · 계약 저주 · 흉조 병 세기/주기 · 후반 판정.
 */
public final class Omen {
    private Omen() {}

    /** 플레이어 데이터 키 */
    public static final String THEME_KEY = "wildplus_omen_theme", CURSE_KEY = "wildplus_omen_curses", CURSE_AT = "wildplus_omen_curses_at",
            PRANK_OFF = "wildplus_omen_prank_off", SURE_SMUGGLER = "wildplus_smuggler_sure";
    /** 몹 데이터 키 */
    public static final String MOB_DONE = "wildplus_omen_done", MOB_THEME = "wildplus_omen_mob_theme", ALLY = "wildplus_omen_ally";

    /** 흉조 양조 테마: 이름 · 보상 배율 가산 · 돈 배수 · 양조 재료 */
    public enum Theme {
        NONE("", 0.0, 1.0, null),
        BURNING("불타는 흉조", 0.30, 1.0, Items.BLAZE_POWDER),
        FROZEN("얼어붙은 흉조", 0.30, 1.0, Items.BLUE_ICE),
        DEEP("심해의 흉조", 0.30, 1.0, Items.PRISMARINE_CRYSTALS),
        WRAITH("망령의 흉조", 0.40, 1.0, Items.PHANTOM_MEMBRANE),
        GOLDEN("황금 흉조", 0.0, 2.5, Items.GOLD_BLOCK);

        public final String title;
        public final double bonus, money;
        public final Item reagent;

        Theme(String title, double bonus, double money, Item reagent) {
            this.title = title;
            this.bonus = bonus;
            this.money = money;
            this.reagent = reagent;
        }

        public String desc() {
            return switch (this) {
                case BURNING -> "우민이 불에 강하고 불을 붙임 · 블레이즈 합류 · 보상 +30%";
                case FROZEN -> "맞으면 느려짐 · 스트레이 합류 · 보상 +30%";
                case DEEP -> "맞으면 나약함 · 삼지창 드라운드 합류 · 보상 +30%";
                case WRAITH -> "맞으면 시듦 · 벡스 합류 · 우민 흡수 보호막 · 보상 +40%";
                case GOLDEN -> "우민이 금 갑옷 · 체력 1.5배 · 처치마다 금화 · 돈 보상 2.5배";
                default -> "";
            };
        }

        public static Theme of(String s) {
            try {
                return s == null || s.isEmpty() ? NONE : valueOf(s);
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
    }

    /** 흉조 계약서 저주 카드 */
    public enum Curse {
        FOG("안개", "습격 동안 주변이 어두워짐", 0.20, Items.INK_SAC),
        NIGHT("영원한 밤", "습격 내내 밤 (좀비 · 스켈레톤도 나옴)", 0.15, Items.PHANTOM_MEMBRANE),
        ARMORED("무장한 우민", "우민 전원 철 갑옷", 0.30, Items.IRON_CHESTPLATE),
        PANIC("겁먹은 주민", "주민이 받는 피해 2배", 0.25, Items.EMERALD),
        BLOOD("피의 맹세", "근처 플레이어의 모든 회복 절반", 0.40, Items.REDSTONE),
        TIMED("시간 제한", "15분 안에 못 끝내면 실패", 0.35, Items.CLOCK);

        public final String title, desc;
        public final double bonus;
        public final Item icon;

        Curse(String title, String desc, double bonus, Item icon) {
            this.title = title;
            this.desc = desc;
            this.bonus = bonus;
            this.icon = icon;
        }

        public boolean in(int mask) {
            return (mask & (1 << ordinal())) != 0;
        }
    }

    public static double curseBonus(int mask) {
        double s = 0;
        for (Curse c : Curse.values()) if (c.in(mask)) s += c.bonus;
        return s;
    }

    public static int curseCount(int mask) {
        return Integer.bitCount(mask & 63);
    }

    // ───────── 흉조 병 ─────────
    public static boolean isOminousBottle(ItemStack s) {
        return s.is(Items.OMINOUS_BOTTLE);
    }

    /** 흉조 병 세기 (0 = I … 4 = V) */
    public static int amp(ItemStack s) {
        OminousBottleAmplifier a = s.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        return a == null ? 0 : Math.max(0, Math.min(4, a.value()));
    }

    public static ItemStack withAmp(ItemStack s, int amp) {
        s.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(Math.max(0, Math.min(4, amp))));
        return s;
    }

    /** 흉조 걸린 상태 (나쁜 징조 · 습격 징조) — 세기, 없으면 -1 */
    public static int omenLevel(Player p) {
        MobEffectInstance e = p.getEffect(MobEffects.BAD_OMEN);
        if (e == null) e = p.getEffect(MobEffects.RAID_OMEN);
        return e == null ? -1 : e.getAmplifier();
    }

    /** 가방 속 바닐라 흉조 병 개수 */
    public static int countBottles(Player p) {
        Inventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (isOminousBottle(inv.getItem(i))) n += inv.getItem(i).getCount();
        return n;
    }

    public static boolean takeBottles(Player p, int n) {
        if (p.getAbilities().instabuild) return true;
        if (countBottles(p) < n) return false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!isOminousBottle(s)) continue;
            int k = Math.min(n, s.getCount());
            s.shrink(k);
            n -= k;
        }
        inv.setChanged();
        return true;
    }

    public static void give(Player p, ItemStack s) {
        if (s.isEmpty()) return;
        if (!p.getInventory().add(s) && !s.isEmpty()) {
            ItemEntity it = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), s);
            it.setNoPickUpDelay();
            p.level().addFreshEntity(it);
        }
    }

    /** 후반 판정: 누군가 통장에 300만동 이상 (물가 3배 기준) */
    public static boolean late(MinecraftServer server) {
        WildData wd = WildData.get(server);
        for (long v : wd.bank.values()) if (v >= 3_000_000L) return true;
        return false;
    }

    public static String won(long v) {
        return String.format("%,d동", v);
    }

    public static UUID uuidOr(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }
}
