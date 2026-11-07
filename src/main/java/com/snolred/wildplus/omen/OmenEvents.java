package com.snolred.wildplus.omen;

import com.snolred.wildplus.entity.MercenaryEntity;
import com.snolred.wildplus.entity.SmugglerEntity;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.item.ForbiddenPotionItem;
import com.snolred.wildplus.item.ThemedOmenItem;
import com.snolred.wildplus.registry.ModItems20;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 3.8 흉조 잡다한 규칙: 흉조 투척병 · 흉조 양조 세기 이어받기 · 금지된 물약 대가 · 용병/밀수꾼 표적 막기 · /omen 명령 */
public final class OmenEvents {
    private OmenEvents() {}

    // ───────── 흉조 투척병 ─────────
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrowableItemProjectile proj) || !proj.getItem().is(ModItems20.OMEN_FLASK.get())
                || !(proj.level() instanceof ServerLevel level) || proj.isRemoved()) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, proj.getX(), proj.getY(), proj.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.WITCH, proj.getX(), proj.getY(), proj.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
        level.playSound(null, proj.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        String thrower = proj.getOwner() != null ? proj.getOwner().getName().getString() : "누군가";
        if (event.getRayTraceResult() instanceof EntityHitResult eh) {
            if (eh.getEntity() instanceof ServerPlayer target) {
                if (target.getPersistentData().getBooleanOr(Omen.PRANK_OFF, false)) {
                    target.sendOverlayMessage(Component.literal("🛡 흉조 장난을 막았어요 (/omen prank on 으로 허용)").withStyle(ChatFormatting.GRAY));
                    if (proj.getOwner() instanceof ServerPlayer o) {
                        o.sendOverlayMessage(Component.literal(target.getName().getString() + " 님은 흉조 장난을 거부 중이에요").withStyle(ChatFormatting.GRAY));
                    }
                } else {
                    int amp = Math.max(0, Omen.omenLevel(target));
                    target.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 120000, Math.min(4, amp + (Omen.omenLevel(target) >= 0 ? 1 : 0))));
                    level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("☠ " + thrower + " 님이 " + target.getName().getString()
                            + " 님에게 흉조를 씌웠어요! (마을에 가면 습격 · 흉조 계약 가능)").withStyle(ChatFormatting.DARK_PURPLE), false);
                }
            } else if (eh.getEntity() instanceof Mob m && m instanceof Enemy && !(m instanceof MercenaryEntity) && !(m instanceof SmugglerEntity)
                    && !EliteMobs.isElite(m) && m.getMaxHealth() <= 150) {
                EliteMobs.makeElite(m, 1 + level.getRandom().nextInt(2));
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 8, 0.4, 0.3, 0.4, 0);
                if (proj.getOwner() instanceof ServerPlayer o) {
                    o.sendOverlayMessage(Component.literal("☠ 정예가 됐어요! (정예는 생명의 정수 · 보상이 더 좋아요)").withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
        proj.discard();
    }

    // ───────── 흉조 양조: 원래 흉조 병의 세기를 이어받음 ─────────
    private static int[] lastAmps;

    @SubscribeEvent
    public static void onBrewPre(PotionBrewEvent.Pre event) {
        int[] amps = new int[3];
        boolean any = false;
        for (int i = 0; i < Math.min(3, event.getLength()); i++) {
            ItemStack s = event.getItem(i);
            amps[i] = Omen.isOminousBottle(s) ? Omen.amp(s) : -1;
            any |= amps[i] >= 0;
        }
        lastAmps = any ? amps : null;
    }

    @SubscribeEvent
    public static void onBrewPost(PotionBrewEvent.Post event) {
        if (lastAmps == null) return;
        for (int i = 0; i < Math.min(3, event.getLength()); i++) {
            ItemStack s = event.getItem(i);
            if (s.getItem() instanceof ThemedOmenItem && lastAmps[i] >= 0) Omen.withAmp(s, lastAmps[i]);
        }
        lastAmps = null;
    }

    /** 바닐라 흉조 병을 마시면 예전에 마신 테마는 지워짐 */
    @SubscribeEvent
    public static void onDrink(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer p && Omen.isOminousBottle(event.getItem())) p.getPersistentData().remove(Omen.THEME_KEY);
    }

    // ───────── 금지된 물약의 대가 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            long until = p.getPersistentData().getLongOr(ForbiddenPotionItem.UNTIL, 0L);
            if (until <= 0 || p.level().getGameTime() < until) continue;
            p.getPersistentData().remove(ForbiddenPotionItem.UNTIL);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 0));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
            p.sendOverlayMessage(Component.literal("🧪 광기가 끝나고 대가가 찾아왔어요… (1분 나약함 · 구속)").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ───────── 골렘 · 몬스터가 용병/밀수꾼을 노리지 않게 ─────────
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        var t = event.getNewAboutToBeSetTarget();
        if (t instanceof SmugglerEntity) {
            event.setCanceled(true);
            return;
        }
        if (t instanceof MercenaryEntity && event.getEntity() instanceof net.minecraft.world.entity.animal.golem.IronGolem) event.setCanceled(true);
    }

    // ───────── /omen ─────────
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("omen")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    for (String line : new String[]{
                            "☠ 흉조 (3.8)",
                            " · 흉조 병을 마신 채 마을 종을 누르면 흉조 계약서 (저주를 붙일수록 보상 배율 ↑)",
                            " · 양조대: 흉조 병 + 블레이즈 가루/푸른 얼음/프리즈머린 수정/팬텀 막/금 블록 → 테마 흉조",
                            " · 이기면 5분 정산 대기: 같은 마을에서 다시 습격하면 연쇄 ×1.5→×2→×3 · 종을 치면 정산 · 지면 증발",
                            " · 흉조 투척병: 몬스터 → 정예 / 친구 → 흉조 (장난, /omen prank off 로 거부)",
                            " · 우민 올가미 → 우민 감옥: 몸값 · 전향(용병) · 심문",
                            " · 전초 기지 깃발에 흉조 병을 쓰면 역습격 → 점령하면 전진 기지",
                            " · 흉조가 걸려 있으면 가끔 우민 밀수꾼이 나타나요 · 흉조 제단에 병을 바치면 마을 수호",
                            " · 장난 허용: " + (p.getPersistentData().getBooleanOr(Omen.PRANK_OFF, false) ? "끔" : "켬")}) {
                        p.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    return 1;
                })
                .then(Commands.literal("prank")
                        .then(Commands.literal("on").executes(ctx -> prank(ctx.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("off").executes(ctx -> prank(ctx.getSource().getPlayerOrException(), false)))));
    }

    private static int prank(ServerPlayer p, boolean on) {
        p.getPersistentData().putBoolean(Omen.PRANK_OFF, !on);
        p.sendSystemMessage(Component.literal(on ? "흉조 투척병 장난을 받아요" : "흉조 투척병 장난을 막아요").withStyle(ChatFormatting.LIGHT_PURPLE));
        return 1;
    }
}
