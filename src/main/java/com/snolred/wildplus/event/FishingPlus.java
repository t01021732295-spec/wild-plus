package com.snolred.wildplus.event;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.item.TieredRodItem;
import com.snolred.wildplus.registry.ModItems13;
import com.snolred.wildplus.registry.ModItems2;
import java.lang.reflect.Field;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 2.18 낚시 개편 공통: 낚싯대 단계 · 미끼 · 등급 · 크기 기록.
 * <ul>
 *   <li>낚싯대 단계(0 기본 · 비취 · 1 미스릴 · 2 루비 · 3 사파이어 · 4 용왕)마다 입질 속도 · 희귀 · 보물 · 두 마리 · 등급 확률</li>
 *   <li>미끼(왼손): 지렁이 입질↑ · 새우 미끼 희귀↑ · 황금 미끼 등급↑↑ — 한 마리마다 하나씩</li>
 *   <li>잡은 물고기에 크기(cm)와 등급이 붙고, 어종별 최대어 기록을 남긴다</li>
 * </ul>
 */
public final class FishingPlus {
    private FishingPlus() {}

    /** 낚싯대 능력 */
    public record Rod(int lure, float rare, float chest, float twin, float king, float grade) {}

    private static final Rod VANILLA = new Rod(0, 0.25F, 0.02F, 0F, 0.01F, 1F);
    private static final Rod JADE = new Rod(40, 0.5F, 0.05F, 0.2F, 0.02F, 1.2F);
    private static final Rod[] TIERS = {VANILLA,
            new Rod(100, 0.5F, 0.04F, 0.15F, 0.02F, 1.3F),  // 미스릴
            new Rod(140, 0.6F, 0.05F, 0.2F, 0.03F, 1.6F),   // 루비
            new Rod(180, 0.65F, 0.08F, 0.35F, 0.03F, 2.0F), // 사파이어
            new Rod(220, 0.75F, 0.1F, 0.5F, 0.06F, 3.0F)};  // 용왕

    public static Rod rod(ItemStack s) {
        if (s.getItem() instanceof TieredRodItem t) return TIERS[Math.min(4, t.tier)];
        if (s.is(ModItems2.JADE_FISHING_ROD.get())) return JADE;
        return VANILLA;
    }

    /** 미끼 종류: 0 없음 · 1 지렁이 · 2 새우 · 3 황금 */
    public static int bait(ItemStack off) {
        if (off.is(ModItems13.WORM.get())) return 1;
        if (off.is(ModItems13.SHRIMP_BAIT.get())) return 2;
        if (off.is(ModItems13.GOLDEN_BAIT.get())) return 3;
        return 0;
    }

    public static ItemStack rodOf(Player p) {
        ItemStack m = p.getMainHandItem();
        return m.getItem() instanceof FishingRodItem ? m : p.getOffhandItem();
    }

    // ───────── 입질 속도: 찌가 물에 들어갈 때 낚싯대 · 미끼만큼 기다림 시간을 줄인다 ─────────
    private static Field lureField, luckField;

    @SubscribeEvent
    public static void onHookJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof FishingHook hook) || hook.getPlayerOwner() == null) return;
        Player p = hook.getPlayerOwner();
        Rod r = rod(rodOf(p));
        int b = bait(p.getOffhandItem());
        int extra = r.lure() + (b == 1 ? 40 : b == 3 ? 60 : b == 2 ? 20 : 0);
        if (extra <= 0) return;
        try {
            if (lureField == null) {
                lureField = FishingHook.class.getDeclaredField("lureSpeed");
                lureField.setAccessible(true);
                luckField = FishingHook.class.getDeclaredField("luck");
                luckField.setAccessible(true);
            }
            lureField.setInt(hook, Math.min(480, lureField.getInt(hook) + extra));
            if (r.grade() >= 2F) luckField.setInt(hook, luckField.getInt(hook) + 1);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 이름이 바뀐 버전이면 입질 보너스만 빠진다
        }
    }

    // ───────── 등급 · 크기 ─────────
    /** 등급 굴리기: 배율 m (낚싯대 × 미끼 × 자동이면 절반) */
    public static int rollGrade(RandomSource r, float m) {
        float p3 = 0.004F * m, p2 = 0.03F * m, p1 = Math.min(0.5F, 0.12F * m);
        float x = r.nextFloat();
        return x < p3 ? 3 : x < p3 + p2 ? 2 : x < p3 + p2 + p1 ? 1 : 0;
    }

    private static final float[] SIZE_UP = {1F, 1.15F, 1.35F, 1.7F};

    /** 어종별 기본 길이 (cm) */
    private static float baseCm(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return switch (id) {
            case "cod" -> 45; case "salmon" -> 60; case "tropical_fish" -> 12; case "pufferfish" -> 25;
            case "rainbow_trout" -> 40; case "ice_smelt" -> 15; case "moonlight_fish" -> 35; case "storm_eel" -> 80;
            case "anglerfish" -> 70; case "golden_carp" -> 50; case "catfish" -> 65; case "tuna" -> 150;
            case "sweetfish" -> 22; case "dragon_king_fish" -> 220;
            default -> 30;
        };
    }

    /** 잡은 물고기 알림 + 최대어 기록. 등급 있는 물고기만 채팅, 전설은 서버 전체에 */
    public static void announce(ServerPlayer p, ItemStack fish, int g) {
        RandomSource r = p.getRandom();
        float cm = baseCm(fish) * (0.6F + r.nextFloat() * 0.8F) * SIZE_UP[g];
        cm = Math.round(cm * 10) / 10F;
        String id = BuiltInRegistries.ITEM.getKey(fish.getItem()).toString();
        CompoundTag rec = ContentData.get(p.level().getServer()).player("fishrec", p.getUUID());
        int best = rec.getIntOr(id, 0);
        boolean record = cm * 10 > best;
        if (record) {
            rec.putInt(id, Math.round(cm * 10));
            rec.putString("name_" + id, fish.getHoverName().getString());
            ContentData.get(p.level().getServer()).dirty();
        }
        Component line = Component.literal("🐟 ").append(g > 0 ? Grade.tag(fish, g).append(" ") : Component.empty())
                .append(fish.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format(" %.1fcm", cm)).withStyle(ChatFormatting.YELLOW))
                .append(record && best > 0 ? Component.literal(" 🏆 최대어 갱신!").withStyle(ChatFormatting.GOLD) : Component.empty());
        if (g >= 3) {
            p.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("🎣 " + p.getName().getString() + " 님이 ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE).append(line), false);
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.2F);
        } else if (g >= 1 || (record && best > 0)) {
            p.sendSystemMessage(line);
            if (g >= 2) p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        } else {
            p.sendOverlayMessage(line);
        }
    }

    // ───────── 지렁이: 흙 · 잔디 · 경작지를 파면 가끔 ─────────
    @SubscribeEvent
    public static void onDig(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player)) return;
        var st = event.getState();
        if (!(st.is(BlockTags.DIRT) || st.is(Blocks.FARMLAND) || st.is(Blocks.MUD))) return;
        if (event.getLevel().getRandom().nextFloat() >= 0.05F) return;
        var pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(ModItems13.WORM.get(), 1 + event.getLevel().getRandom().nextInt(2))));
    }
}
