package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.registry.ModItems17;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 3.4 목장: 동물마다 애정도 (0~500 = 하트 0~5).
 * 브러시로 하루 한 번 쓰다듬으면 +40 과 그날의 생산물 (소 → 우유병 · 닭 → 달걀 · 양 → 양털 뭉치 · 염소 → 우유병),
 * 건초 여물통에서 하루 한 번 먹으면 +25. 때리면 -60. 하트가 높을수록 생산물 등급이 오른다 (은 · 금 · 무지개).
 */
public final class Ranch {
    private Ranch() {}

    public static final String HEART = "wp_heart", BRUSH_DAY = "wp_brush_day", FED_DAY = "wp_fed_day", LAID_DAY = "wp_laid_day";
    public static final int MAX = 500;

    public static long today(ServerLevel level) {
        return level.getServer().overworld().getOverworldClockTime() / 24000L;
    }

    public static int hearts(LivingEntity e) {
        return Math.max(0, Math.min(MAX, e.getPersistentData().getIntOr(HEART, 0)));
    }

    public static void addHearts(LivingEntity e, int d) {
        e.getPersistentData().putInt(HEART, Math.max(0, Math.min(MAX, hearts(e) + d)));
        if (d > 0 && e.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 2 + d / 20, 0.3, 0.2, 0.3, 0.0);
        }
    }

    /** 하트 → 등급: 2개 은 · 4개 금 · 5개 무지개 */
    public static int grade(LivingEntity e) {
        int h = hearts(e) / 100;
        return h >= 5 ? 3 : h >= 4 ? 2 : h >= 2 ? 1 : 0;
    }

    public static String heartText(LivingEntity e) {
        int full = hearts(e) / 100;
        return "♥".repeat(full) + "♡".repeat(5 - full) + String.format(" (%d/%d)", hearts(e), MAX);
    }

    public static boolean isRanchAnimal(LivingEntity e) {
        return e instanceof Animal && (e.getType() == net.minecraft.world.entity.EntityType.COW || e.getType() == net.minecraft.world.entity.EntityType.MOOSHROOM
                || e.getType() == net.minecraft.world.entity.EntityType.CHICKEN || e.getType() == net.minecraft.world.entity.EntityType.SHEEP
                || e.getType() == net.minecraft.world.entity.EntityType.GOAT || e.getType() == net.minecraft.world.entity.EntityType.PIG);
    }

    /** 오늘의 생산물 (등급 붙음) */
    public static ItemStack product(LivingEntity e, ServerLevel level) {
        int g = grade(e);
        var type = e.getType();
        ItemStack s;
        if (type == net.minecraft.world.entity.EntityType.CHICKEN) {
            if (hearts(e) >= 450 && level.getRandom().nextInt(8) == 0) return new ItemStack(ModItems17.GOLDEN_EGG.get());
            s = new ItemStack(Items.EGG, 1 + (g >= 2 ? 1 : 0));
        } else if (type == net.minecraft.world.entity.EntityType.SHEEP) {
            s = new ItemStack(ModItems17.WOOL_TUFT.get(), 1 + g / 2);
        } else if (type == net.minecraft.world.entity.EntityType.PIG) {
            s = new ItemStack(Items.BROWN_MUSHROOM, 1); // 돼지는 버섯을 찾아 옴 (송로 대신)
        } else {
            s = new ItemStack(ModItems17.MILK_BOTTLE.get(), 1 + (g >= 2 ? 1 : 0));
        }
        return Grade.apply(s, g);
    }

    /** 목장 대회 점수 (생산물 하나 1점 + 등급) */
    public static void contest(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) return;
        Contest.add(p, Contest.Kind.RANCH, s.getCount() * (1 + Grade.of(s)) + (s.is(ModItems17.GOLDEN_EGG.get()) ? 10 : 0));
    }

    /** 때리면 애정도 감소 */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide() || !isRanchAnimal(e) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (hearts(e) > 0) {
            addHearts(e, -60);
            p.sendOverlayMessage(Component.literal("💔 " + e.getName().getString() + " 애정도 " + heartText(e)).withStyle(ChatFormatting.RED));
        }
    }
}
