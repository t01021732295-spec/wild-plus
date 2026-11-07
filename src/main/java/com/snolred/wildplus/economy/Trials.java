package com.snolred.wildplus.economy;

import com.snolred.wildplus.event.EliteMobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 인챈트 X~XV 강화 시련: 강화대에서 IX → X 이상으로 올리려면 돈 · 재료와 함께 그 장비로 시련(퀘스트)을 끝내야 한다.
 * 시련은 장비에 저장되고(장비마다 하나), 도구 종류 · 인챈트에 맞는 일을 그 장비로 해야 진행된다.
 * 단계가 오를수록 수가 늘고, XIII 부터는 조건이 붙는다 (엘리트 · 강한 몬스터 / 귀한 광석 / 고급 이상 물고기).
 */
public final class Trials {
    private Trials() {}

    public static final String KEY = "wp_trial";
    public static final int FIRST = 10, MAX = 15;

    public enum Kind {
        KILL("이 무기로 몬스터 %d마리 처치", "이 무기로 엘리트·강한 몬스터 %d마리 처치", 120, true),
        SHOOT("이 활로 몬스터 %d마리 사냥", "이 활로 엘리트·강한 몬스터 %d마리 사냥", 100, true),
        MINE("이 곡괭이로 광석 %d개 캐기", "이 곡괭이로 다이아·에메랄드·루비·사파이어 광석 %d개 캐기", 250, true),
        CHOP("이 도끼로 원목 %d개 베기", null, 500, false),
        DIG("이 삽으로 흙·모래·자갈 %d개 파기", null, 1000, false),
        HARVEST("이 괭이로 다 자란 작물 %d개 수확", null, 600, false),
        FISH("이 낚싯대로 물고기 %d마리 낚기", "이 낚싯대로 고급 이상 물고기 %d마리 낚기", 100, true),
        GUARD("이 장비를 입고 몬스터에게 피해 %d하트 받아내기", null, 150, false);

        public final String text, hard;
        public final int base;
        public final boolean special;

        Kind(String text, String hard, int base, boolean special) {
            this.text = text;
            this.hard = hard;
            this.base = base;
            this.special = special;
        }
    }

    /** 목표 단계별 돈 · 재료 */
    public static final long[] COST = {30_000, 60_000, 120_000, 250_000, 500_000, 1_000_000};
    private static final float[] MUL = {1F, 1.6F, 2.5F, 3.5F, 5F, 7F};
    private static final float[] MUL_HARD = {1F, 1.6F, 2.5F, 0.5F, 0.8F, 1.2F};

    public static long cost(int target) {
        return Prices.x(COST[Math.max(0, Math.min(5, target - FIRST))]);
    }

    public static Item material(int target) {
        return switch (target) {
            case 10 -> Items.DIAMOND_BLOCK;
            case 11 -> com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get();
            case 12 -> com.snolred.wildplus.registry.ModItems11.SAPPHIRE_BLOCK.get();
            case 13 -> Items.NETHERITE_INGOT;
            case 14 -> com.snolred.wildplus.registry.ModItems13.STELLAR_CORE.get();
            default -> Items.NETHER_STAR;
        };
    }

    public static int materialCount(int target) {
        return target <= 13 ? 4 : target == 14 ? 1 : 3;
    }

    private static final java.util.Set<String> ATTACK = java.util.Set.of("sharpness", "smite", "bane_of_arthropods", "looting", "fire_aspect",
            "knockback", "sweeping_edge", "breach", "density", "impaling", "armor_piercing");
    private static final java.util.Set<String> BOW = java.util.Set.of("power", "punch", "piercing");

    /** 이 장비 · 인챈트에 맞는 시련 종류 */
    public static Kind kindFor(ItemStack s, String ench) {
        if (s.getItem() instanceof FishingRodItem) return Kind.FISH;
        if (s.getItem() instanceof ProjectileWeaponItem || BOW.contains(ench)) return Kind.SHOOT;
        var eq = s.get(DataComponents.EQUIPPABLE);
        if (eq != null && eq.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return Kind.GUARD;
        if (ATTACK.contains(ench) || s.is(ItemTags.SWORDS) || s.is(Items.TRIDENT) || s.is(Items.MACE)) return Kind.KILL;
        if (s.is(ItemTags.PICKAXES)) return Kind.MINE;
        if (s.is(ItemTags.AXES)) return Kind.CHOP;
        if (s.is(ItemTags.SHOVELS)) return Kind.DIG;
        if (s.is(ItemTags.HOES)) return Kind.HARVEST;
        return Kind.KILL;
    }

    public static int need(Kind k, int target) {
        int i = Math.max(0, Math.min(5, target - FIRST));
        return Math.max(1, Math.round(k.base * (k.special ? MUL_HARD[i] : MUL[i])));
    }

    public static boolean hard(Kind k, int target) {
        return k.special && target >= 13;
    }

    public static String describe(Kind k, int target) {
        return String.format(hard(k, target) ? k.hard : k.text, need(k, target));
    }

    // ───────── 장비에 저장 ─────────
    public static CompoundTag get(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag().getCompoundOrEmpty(KEY);
    }

    public static boolean has(ItemStack s) {
        return !get(s).isEmpty();
    }

    private static void put(ItemStack s, CompoundTag t) {
        CompoundTag root = s.has(DataComponents.CUSTOM_DATA) ? s.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        if (t == null || t.isEmpty()) root.remove(KEY);
        else root.put(KEY, t);
        if (root.isEmpty()) s.remove(DataComponents.CUSTOM_DATA);
        else s.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** 시련을 준다 */
    public static void start(ItemStack s, String ench, int target) {
        Kind k = kindFor(s, ench);
        CompoundTag t = new CompoundTag();
        t.putString("e", ench);
        t.putInt("lv", target);
        t.putString("k", k.name());
        t.putInt("need", need(k, target));
        t.putInt("have", 0);
        put(s, t);
    }

    public static void clear(ItemStack s) {
        put(s, null);
    }

    public static boolean done(ItemStack s) {
        CompoundTag t = get(s);
        return !t.isEmpty() && t.getIntOr("have", 0) >= t.getIntOr("need", 1);
    }

    /** 진행 (끝나면 알림) */
    private static void progress(ServerPlayer p, ItemStack s, Kind kind, int n, java.util.function.IntPredicate hardOk) {
        CompoundTag t = get(s);
        if (t.isEmpty() || !t.getStringOr("k", "").equals(kind.name())) return;
        int lv = t.getIntOr("lv", FIRST);
        if (hard(kind, lv) && !hardOk.test(lv)) return;
        int need = t.getIntOr("need", 1), have = t.getIntOr("have", 0);
        if (have >= need) return;
        have = Math.min(need, have + n);
        t.putInt("have", have);
        put(s, t);
        if (have >= need) {
            p.sendSystemMessage(Component.literal("⚔ 강화 시련 완료! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(s.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" → 강화대에서 " + roman(lv) + " 강화 가능").withStyle(ChatFormatting.YELLOW)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.1F);
        } else if (have % Math.max(1, need / 20) == 0) {
            p.sendOverlayMessage(Component.literal(String.format("⚔ 강화 시련 %d/%d", have, need)).withStyle(ChatFormatting.YELLOW));
        }
    }

    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    // ───────── 진행 이벤트 ─────────
    private static boolean strong(LivingEntity e) {
        return EliteMobs.isElite(e) || e.getMaxHealth() >= 60F;
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster m) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        boolean ranged = event.getSource().getDirectEntity() instanceof Projectile;
        ItemStack hand = p.getMainHandItem();
        progress(p, hand, ranged ? Kind.SHOOT : Kind.KILL, 1, lv -> strong(m));
        if (ranged && !(hand.getItem() instanceof ProjectileWeaponItem)) progress(p, p.getOffhandItem(), Kind.SHOOT, 1, lv -> strong(m));
    }

    @SubscribeEvent
    public static void onBreak(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p)) return;
        var st = event.getState();
        ItemStack hand = p.getMainHandItem();
        if (st.is(Tags.Blocks.ORES)) {
            String id = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
            boolean precious = id.contains("diamond") || id.contains("emerald") || id.contains("ruby") || id.contains("sapphire")
                    || st.is(Blocks.ANCIENT_DEBRIS);
            progress(p, hand, Kind.MINE, 1, lv -> precious);
        } else if (st.is(BlockTags.LOGS)) {
            progress(p, hand, Kind.CHOP, 1, lv -> true);
        } else if (st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(Blocks.GRAVEL) || st.is(Blocks.CLAY)) {
            progress(p, hand, Kind.DIG, 1, lv -> true);
        } else if (st.getBlock() instanceof CropBlock c && c.isMaxAge(st)) {
            progress(p, hand, Kind.HARVEST, 1, lv -> true);
        }
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack rod = p.getMainHandItem().getItem() instanceof FishingRodItem ? p.getMainHandItem() : p.getOffhandItem();
        boolean graded = false;
        for (ItemStack s : event.getDrops()) if (Grade.of(s) >= 1) graded = true;
        boolean g = graded;
        progress(p, rod, Kind.FISH, 1, lv -> g);
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getSource().getEntity() instanceof Monster)) return;
        int hearts = Math.max(1, Math.round(event.getHealthDamage() / 2F));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            progress(p, p.getItemBySlot(slot), Kind.GUARD, hearts, lv -> true);
        }
    }

    /** 강화대 화면 · 툴팁용 한 줄 */
    public static String status(ItemStack s) {
        CompoundTag t = get(s);
        if (t.isEmpty()) return "";
        Kind k = Kind.valueOf(t.getStringOr("k", "KILL"));
        int lv = t.getIntOr("lv", FIRST);
        return String.format("%s %s 시련: %s (%d/%d)", t.getStringOr("e", ""), roman(lv), describe(k, lv), t.getIntOr("have", 0), t.getIntOr("need", 1));
    }
}
