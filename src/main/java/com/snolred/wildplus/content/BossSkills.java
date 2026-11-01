package com.snolred.wildplus.content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 하늘의 탑 보스 6종의 고유 패턴 + 바닥에 표시되고 늦게 터지는 범위 공격.
 * 범위 공격 종류: 0 땅울림(밀침) 1 화염(불) 2 모래(실명·구속) 3 물(구속·밀침) 4 공허(공중 부양) 5 황금(큰 피해) 6 덩굴(속박)
 */
public final class BossSkills {
    private BossSkills() {}

    /** 테마 순서: 이끼 낀 회랑 · 사막 신전 · 지옥 요새 · 해저 궁전 · 끝의 도시 · 황금 왕좌 */
    public static final String[] TOWER_NAMES = {"덩굴 집행자", "모래 파라오", "화염 군주", "심해 수호자", "공허의 기사", "황금 왕"};
    public static final String[] TOWER_SKILLS = {"덩굴 속박 · 도약 내려찍기", "모래 폭풍 · 유사 · 미라 소환", "화염 낙인 · 지옥불 고리",
            "해일 · 삼지창 비", "공허 도약 · 공허 구체", "황금 방패 · 금화 폭격 · 근위병"};

    private record Strike(ServerLevel lv, Vec3 pos, long at, float radius, float damage, int kind, UUID src) {}

    private static final List<Strike> STRIKES = new ArrayList<>();

    public static void clear() {
        STRIKES.clear();
    }

    public static void strike(ServerLevel lv, Vec3 at, int delay, float radius, float damage, int kind, Entity src) {
        STRIKES.add(new Strike(lv, new Vec3(at.x, Math.floor(at.y) + 0.1, at.z), lv.getGameTime() + delay, radius, damage, kind,
                src == null ? null : src.getUUID()));
    }

    private static ParticleOptions ring(int kind) {
        return switch (kind) {
            case 1 -> ParticleTypes.FLAME;
            case 2 -> ParticleTypes.WHITE_ASH;
            case 3 -> ParticleTypes.SPLASH;
            case 4 -> ParticleTypes.PORTAL;
            case 5 -> ParticleTypes.WAX_ON;
            case 6 -> ParticleTypes.COMPOSTER;
            default -> ParticleTypes.CRIT;
        };
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (STRIKES.isEmpty()) return;
        var it = STRIKES.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            ServerLevel lv = s.lv();
            long now = lv.getGameTime();
            if (now < s.at()) {
                if (now % 3 == 0) {
                    int pts = (int) (s.radius() * 9);
                    for (int i = 0; i < pts; i++) {
                        double a = i * Math.PI * 2 / pts;
                        lv.sendParticles(ring(s.kind()), s.pos().x + Math.cos(a) * s.radius(), s.pos().y, s.pos().z + Math.sin(a) * s.radius(), 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            it.remove();
            SoundEvent snd = switch (s.kind()) {
                case 1 -> SoundEvents.BLAZE_SHOOT;
                case 2 -> SoundEvents.SAND_BREAK;
                case 3 -> SoundEvents.GENERIC_SPLASH;
                case 4 -> SoundEvents.ENDERMAN_TELEPORT;
                case 5 -> SoundEvents.ANVIL_LAND;
                case 6 -> SoundEvents.GRASS_BREAK;
                default -> SoundEvents.GENERIC_EXPLODE.value();
            };
            lv.playSound(null, BlockPos.containing(s.pos()), snd, SoundSource.HOSTILE, 1.0F, 0.8F);
            ParticleOptions burst = switch (s.kind()) {
                case 1 -> ParticleTypes.LAVA;
                case 2 -> ParticleTypes.WHITE_ASH;
                case 3 -> ParticleTypes.BUBBLE_POP;
                case 4 -> ParticleTypes.REVERSE_PORTAL;
                case 5 -> ParticleTypes.END_ROD;
                case 6 -> ParticleTypes.SPORE_BLOSSOM_AIR;
                default -> ParticleTypes.EXPLOSION;
            };
            lv.sendParticles(burst, s.pos().x, s.pos().y + 0.5, s.pos().z, 25, s.radius() * 0.4, 0.4, s.radius() * 0.4, 0.05);
            Entity src = s.src() == null ? null : lv.getEntity(s.src());
            for (ServerPlayer p : lv.players()) {
                double dx = p.getX() - s.pos().x, dz = p.getZ() - s.pos().z;
                if (dx * dx + dz * dz > s.radius() * s.radius() || Math.abs(p.getY() - s.pos().y) > 3) continue;
                p.hurtServer(lv, src instanceof LivingEntity le ? lv.damageSources().mobAttack(le) : lv.damageSources().magic(), s.damage());
                double len = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
                switch (s.kind()) {
                    case 0 -> push(p, dx / len * 1.1, 0.6, dz / len * 1.1);
                    case 1 -> p.igniteForSeconds(4.0F);
                    case 2 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                    }
                    case 3 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                        push(p, dx / len * 0.8, 0.4, dz / len * 0.8);
                    }
                    case 4 -> p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
                    case 6 -> p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
                    default -> {}
                }
            }
        }
    }

    static void push(ServerPlayer p, double x, double y, double z) {
        p.push(x, y, z);
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    static void announce(List<ServerPlayer> players, String text, ChatFormatting color) {
        for (ServerPlayer p : players) p.sendOverlayMessage(Component.literal("⚠ " + text).withStyle(color, ChatFormatting.BOLD));
    }

    private static List<ServerPlayer> within(List<ServerPlayer> players, Entity c, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : players) if (p.distanceToSqr(c) <= r * r) out.add(p);
        return out;
    }

    private static void minions(ServerLevel lv, Mob boss, EntityType<? extends Mob> type, int n, ItemStack head) {
        for (int i = 0; i < n; i++) {
            Mob m = type.create(lv, EntitySpawnReason.MOB_SUMMONED);
            if (m == null) continue;
            m.snapTo(boss.getX() + lv.getRandom().nextInt(5) - 2, boss.getY(), boss.getZ() + lv.getRandom().nextInt(5) - 2, 0F, 0F);
            m.finalizeSpawn(lv, lv.getCurrentDifficultyAt(boss.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            if (!head.isEmpty()) {
                m.setItemSlot(EquipmentSlot.HEAD, head.copy());
                m.setDropChance(EquipmentSlot.HEAD, 0F);
            }
            if (boss.getTarget() != null) m.setTarget(boss.getTarget());
            lv.addFreshEntity(m);
        }
        lv.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY() + 1, boss.getZ(), 20, 1.5, 0.5, 1.5, 0.02);
    }

    /** 하늘의 탑 보스 패턴 (10틱마다 불림). t = 층 시작 후 지난 틱 */
    public static void tower(int kind, ServerLevel lv, Mob boss, List<ServerPlayer> players, long t, float dmg) {
        if (players.isEmpty() || t < 60) return;
        RandomSource r = lv.getRandom();
        ServerPlayer target = boss.getTarget() instanceof ServerPlayer sp && players.contains(sp) ? sp : players.get(r.nextInt(players.size()));
        boolean low = boss.getHealth() < boss.getMaxHealth() * 0.5F;
        switch (kind) {
            case 0 -> { // 덩굴 집행자
                if (t % 100 == 0) {
                    announce(players, "덩굴 속박!", ChatFormatting.GREEN);
                    for (ServerPlayer p : within(players, boss, 10)) {
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
                        Vec3 d = boss.position().subtract(p.position()).normalize();
                        push(p, d.x * 0.9, 0.2, d.z * 0.9);
                        for (int i = 0; i < 8; i++) {
                            Vec3 at = p.position().add(boss.position().subtract(p.position()).scale(i / 8.0));
                            lv.sendParticles(ParticleTypes.COMPOSTER, at.x, at.y + 1, at.z, 2, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
                if (t % 160 == 80) {
                    announce(players, "도약 내려찍기!", ChatFormatting.DARK_GREEN);
                    Vec3 d = target.position().subtract(boss.position());
                    boss.setDeltaMovement(d.x * 0.16, 0.9, d.z * 0.16);
                    boss.hurtMarked = true;
                    strike(lv, target.position(), 25, 3.5F, dmg * 1.2F, 0, boss);
                }
                if (low && t % 120 == 40) for (ServerPlayer p : players) strike(lv, p.position(), 30, 2.5F, dmg * 0.6F, 6, boss);
            }
            case 1 -> { // 모래 파라오
                if (t % 140 == 0) {
                    announce(players, "모래 폭풍!", ChatFormatting.YELLOW);
                    for (ServerPlayer p : within(players, boss, 14)) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        p.hurtServer(lv, lv.damageSources().mobAttack(boss), dmg * 0.3F);
                    }
                    lv.sendParticles(ParticleTypes.WHITE_ASH, boss.getX(), boss.getY() + 2, boss.getZ(), 300, 8, 2, 8, 0.1);
                    lv.playSound(null, boss.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
                }
                if (t % 120 == 60) for (ServerPlayer p : players) strike(lv, p.position(), 30, 2.5F, dmg * 0.7F, 2, boss);
                if (t % 220 == 110) {
                    announce(players, "저주받은 미라들이 깨어나요!", ChatFormatting.GOLD);
                    minions(lv, boss, EntityTypes.HUSK, low ? 3 : 2, new ItemStack(Items.GOLDEN_HELMET));
                }
            }
            case 2 -> { // 화염 군주
                if (t % 80 == 0) for (ServerPlayer p : players) strike(lv, p.position(), 25, 2.2F, dmg * 0.7F, 1, boss);
                if (t % 200 == 100) {
                    announce(players, "지옥불 고리! 멀리 피하세요", ChatFormatting.RED);
                    for (int i = 1; i <= 3; i++) strike(lv, boss.position(), 15 + i * 12, 3.0F + i * 2.5F, dmg * 0.8F, 1, boss);
                }
                if (low && t % 60 == 30) {
                    for (ServerPlayer p : within(players, boss, 6)) p.igniteForSeconds(3.0F);
                }
            }
            case 3 -> { // 심해 수호자
                if (t % 140 == 0) {
                    announce(players, "해일!", ChatFormatting.AQUA);
                    for (ServerPlayer p : within(players, boss, 12)) {
                        Vec3 d = p.position().subtract(boss.position()).normalize();
                        push(p, d.x * 1.6, 0.5, d.z * 1.6);
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
                        p.hurtServer(lv, lv.damageSources().mobAttack(boss), dmg * 0.4F);
                    }
                    lv.sendParticles(ParticleTypes.SPLASH, boss.getX(), boss.getY() + 0.5, boss.getZ(), 400, 6, 0.5, 6, 0.3);
                    lv.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.5F, 0.6F);
                }
                if (t % 100 == 50) {
                    announce(players, "삼지창 비!", ChatFormatting.DARK_AQUA);
                    for (ServerPlayer p : players) {
                        for (int i = 0; i < (low ? 3 : 2); i++) {
                            strike(lv, p.position().add(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), 30 + i * 6, 2.0F, dmg * 0.6F, 3, boss);
                        }
                    }
                }
            }
            case 4 -> { // 공허의 기사
                if (t % 100 == 0) {
                    Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
                    Vec3 to = target.position().subtract(look.scale(2.0));
                    lv.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 40, 0.4, 1, 0.4, 0.1);
                    boss.teleportTo(to.x, target.getY(), to.z);
                    lv.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
                    target.sendOverlayMessage(Component.literal("⚠ 뒤를 조심하세요!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
                if (t % 160 == 80) {
                    announce(players, "공허 구체!", ChatFormatting.DARK_PURPLE);
                    for (ServerPlayer p : players) strike(lv, p.position(), 30, 2.5F, dmg * 0.6F, 4, boss);
                }
                if (low && t % 120 == 40) for (ServerPlayer p : within(players, boss, 10)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            case 5 -> { // 황금 왕
                if (t % 240 == 0) {
                    announce(players, "황금 방패! 잠시 공격이 거의 안 들어가요", ChatFormatting.GOLD);
                    boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 3));
                    lv.sendParticles(ParticleTypes.WAX_ON, boss.getX(), boss.getY() + 1, boss.getZ(), 60, 0.8, 1.2, 0.8, 0.1);
                    lv.playSound(null, boss.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.HOSTILE, 1.5F, 0.7F);
                }
                if (t % 120 == 60) {
                    announce(players, "금화 폭격!", ChatFormatting.YELLOW);
                    for (ServerPlayer p : players) {
                        for (int i = 0; i < 3; i++) strike(lv, p.position().add(r.nextInt(7) - 3, 0, r.nextInt(7) - 3), 25 + i * 5, 2.0F, dmg * 0.9F, 5, boss);
                    }
                }
                if (t % 300 == 150) {
                    announce(players, "근위병 소환!", ChatFormatting.GOLD);
                    minions(lv, boss, EntityTypes.VINDICATOR, low ? 3 : 2, new ItemStack(Items.GOLDEN_HELMET));
                }
            }
            default -> {}
        }
    }
}
