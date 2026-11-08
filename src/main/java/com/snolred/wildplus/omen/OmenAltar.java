package com.snolred.wildplus.omen;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.entity.MercenaryEntity;
import com.snolred.wildplus.entity.SmugglerEntity;
import com.snolred.wildplus.item.ThemedOmenItem;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModBlocks9;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.8 흉조 제단: 마을에 놓고 흉조 병을 바치면 수호 게이지가 차고 단계마다 마을이 영구 강화된다 (제단 주변 48칸).
 * 1단계 경비 골렘 +1 · 2단계 주민 체력 2배 · 3단계 망루 자동 사격 · 4단계 마을 상점 10% 할인 · 5단계 습격 때 주민 보호막.
 * 흉조 병 I~V = 1~5점, 테마 흉조 = 6점.
 */
public final class OmenAltar {
    private OmenAltar() {}

    public static final int[] NEED = {10, 25, 45, 70, 100};
    public static final String[] PERK = {"경비 골렘 +1", "주민 체력 2배", "망루 자동 사격", "마을 상점 10% 할인", "습격 때 주민 보호막"};
    static final String GOLEM_TAG = "wildplus_altar_golem";
    static final int RADIUS = 48;

    private static CompoundTag root(MinecraftServer s) {
        return ContentData.get(s).section("omen_altar");
    }

    private static String key(BlockPos p) {
        return p.getX() + "|" + p.getY() + "|" + p.getZ();
    }

    public static int stage(int points) {
        int s = 0;
        for (int n : NEED) if (points >= n) s++;
        return s;
    }

    /** 이 자리 근처(64칸) 제단의 가장 높은 단계 */
    public static int stageNear(MinecraftServer s, BlockPos pos) {
        int best = 0;
        CompoundTag r = root(s);
        for (String k : r.keySet()) {
            CompoundTag t = r.getCompoundOrEmpty(k);
            if (BlockPos.of(t.getLongOr("pos", 0L)).distSqr(pos) < 64 * 64) best = Math.max(best, stage(t.getIntOr("points", 0)));
        }
        return best;
    }

    // ───────── 바치기 · 보기 ─────────
    /** 테스트용: 제단 점수 바로 넣기 (/wildplus uitest) */
    public static void debugPoints(MinecraftServer s, BlockPos pos, int points) {
        CompoundTag t = root(s).getCompoundOrEmpty(key(pos));
        t.putLong("pos", pos.asLong());
        t.putInt("points", points);
        root(s).put(key(pos), t);
        ContentData.get(s).dirty();
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            p.sendOverlayMessage(Component.literal("흉조 제단은 지상 세계에서만 힘을 써요").withStyle(ChatFormatting.GRAY));
            return;
        }
        MinecraftServer s = level.getServer();
        CompoundTag r = root(s);
        CompoundTag t = r.getCompoundOrEmpty(key(pos));
        t.putLong("pos", pos.asLong());
        int points = t.getIntOr("points", 0);
        ItemStack hand = p.getMainHandItem();
        boolean bottle = Omen.isOminousBottle(hand), themed = hand.getItem() instanceof ThemedOmenItem;
        if (bottle || themed) {
            int before = stage(points);
            int n = p.isShiftKeyDown() ? hand.getCount() : 1;
            int add = (themed ? 6 : Omen.amp(hand) + 1) * n;
            if (!p.getAbilities().instabuild) hand.shrink(n);
            points += add;
            t.putInt("points", points);
            r.put(key(pos), t);
            ContentData.get(s).dirty();
            level.playSound(null, pos, SoundEvents.OMINOUS_BOTTLE_DISPOSE.value(), SoundSource.BLOCKS, 1.0F, 0.9F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 1, pos.getY() + 1.6, pos.getZ() + 0.5, 25, 0.4, 0.4, 0.4, 0.03);
            int after = stage(points);
            p.sendOverlayMessage(Component.literal(String.format("☠ 흉조 병 %d개를 바쳤어요 +%d점 → %d점 (%d단계)", n, add, points, after))
                    .withStyle(ChatFormatting.AQUA));
            if (after > before) {
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5F, 1.0F);
                s.getPlayerList().broadcastSystemMessage(Component.literal("🛡 흉조 제단 " + after + "단계! 마을 수호: " + PERK[after - 1])
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            }
            return;
        }
        int st = stage(points);
        List<PickMenu.Entry> es = new ArrayList<>();
        int next = st < NEED.length ? NEED[st] : NEED[NEED.length - 1];
        es.add(PickMenu.Entry.of(PickMenu.K_INFO, "수호 게이지 " + points + " / " + next + (st >= 5 ? " (최대)" : ""),
                "흉조 병 I~V = 1~5점 · 테마 흉조 = 6점 · 병을 들고 누르면 1개, 웅크리면 한 묶음"));
        for (int i = 0; i < PERK.length; i++) {
            es.add(new PickMenu.Entry((i < st ? "✔ " : "🔒 ") + (i + 1) + "단계 · " + PERK[i], NEED[i] + "점" + (i < st ? " · 적용 중" : ""),
                    new ItemStack(i == 0 ? Items.IRON_BLOCK : i == 1 ? Items.GOLDEN_APPLE : i == 2 ? Items.ARROW : i == 3 ? Items.EMERALD : Items.SHIELD),
                    i < st ? PickMenu.K_ROW : PickMenu.K_INFO));
        }
        PickMenu.open(p, Component.literal("흉조 제단 · 마을 수호"), "주변 48칸 마을에 효과", "", es, PickMenu.themed(PickMenu.T_ALTAR), i -> {}, null,
                q -> q.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64);
    }

    // ───────── 효과 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        long tick = s.getTickCount();
        if (tick % 40 != 0) return;
        ServerLevel level = s.overworld();
        CompoundTag r = root(s);
        for (String k : new ArrayList<>(r.keySet())) {
            CompoundTag t = r.getCompoundOrEmpty(k);
            BlockPos pos = BlockPos.of(t.getLongOr("pos", 0L));
            if (!level.isLoaded(pos)) continue;
            if (!level.getBlockState(pos).is(ModBlocks9.OMEN_ALTAR.get())) {
                r.remove(k);
                ContentData.get(s).dirty();
                continue;
            }
            int st = stage(t.getIntOr("points", 0));
            if (st > 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 1, pos.getY() + 1.4, pos.getZ() + 0.5, st, 0.2, 0.4, 0.2, 0.01);
            AABB area = new AABB(pos).inflate(RADIUS);
            if (st >= 1 && tick % 1200 == 0) golem(level, pos, t);
            if (st >= 2 && tick % 200 == 0) {
                for (AbstractVillager v : level.getEntitiesOfClass(AbstractVillager.class, area)) {
                    var hp = v.getAttribute(Attributes.MAX_HEALTH);
                    if (hp == null || hp.hasModifier(WildPlus.id("altar_health"))) continue;
                    hp.addPermanentModifier(new AttributeModifier(WildPlus.id("altar_health"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    v.setHealth(v.getMaxHealth());
                }
            }
            if (st >= 3) turret(level, pos);
            if (st >= 5 && level.getRaidAt(pos) != null) {
                for (AbstractVillager v : level.getEntitiesOfClass(AbstractVillager.class, area)) {
                    v.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 3, true, false));
                    v.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
                    if (tick % 200 == 0) level.sendParticles(ParticleTypes.ENCHANT, v.getX(), v.getY() + 1, v.getZ(), 10, 0.4, 0.6, 0.4, 0.3);
                }
            }
        }
    }

    private static void golem(ServerLevel level, BlockPos pos, CompoundTag t) {
        String id = t.getStringOr("golem", "");
        UUID u = Omen.uuidOr(id);
        if (u != null) {
            Entity e = level.getEntity(u);
            if (e != null && e.isAlive()) return;
        }
        IronGolem g = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (g == null) return;
        BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(3, 0, 3));
        g.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
        g.getPersistentData().putBoolean(GOLEM_TAG, true);
        g.setPersistenceRequired();
        level.addFreshEntity(g);
        t.putString("golem", g.getUUID().toString());
        ContentData.get(level.getServer()).dirty();
        level.sendParticles(ParticleTypes.SOUL, g.getX(), g.getY() + 1.5, g.getZ(), 20, 0.5, 0.8, 0.5, 0.02);
    }

    private static void turret(ServerLevel level, BlockPos pos) {
        Vec3 from = new Vec3(pos.getX() + 1, pos.getY() + 2.2, pos.getZ() + 0.5);
        List<Mob> foes = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(24),
                m -> m instanceof Enemy && m.isAlive() && !(m instanceof MercenaryEntity) && !(m instanceof SmugglerEntity)
                        && m.getType() != EntityTypes.CREEPER);
        foes.sort(Comparator.comparingDouble(m -> m.distanceToSqr(from)));
        int shots = 0;
        for (LivingEntity m : foes) {
            if (shots >= 2) break;
            AbstractArrow a = EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);
            if (a == null) return;
            a.snapTo(from.x, from.y, from.z, 0F, 0F);
            Vec3 to = m.getEyePosition().subtract(0, 0.4, 0);
            double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
            a.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.12, dz, 2.2F, 1.0F);
            a.setBaseDamage(4.0);
            a.pickup = AbstractArrow.Pickup.DISALLOWED;
            level.addFreshEntity(a);
            shots++;
        }
        if (shots > 0) level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    /** 웅크린 채 병을 들고 눌러도(바닐라는 이때 블록을 건너뜀) 제단에 바치도록 먼저 가로챔 */
    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        ItemStack hand = p.getMainHandItem();
        if (!Omen.isOminousBottle(hand) && !(hand.getItem() instanceof ThemedOmenItem)) return;
        var level = p.level();
        BlockPos pos = event.getPos();
        BlockPos master = level.getBlockState(pos).is(ModBlocks9.OMEN_ALTAR.get()) ? pos
                : level.getBlockState(pos).is(com.snolred.wildplus.registry.ModBlocks2.BIG_PART.get()) ? com.snolred.wildplus.block.BigBlock.findMaster(level, pos) : null;
        if (master == null || !level.getBlockState(master).is(ModBlocks9.OMEN_ALTAR.get())) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        open(p, master);
    }

    // ───────── 4단계: 바닐라 주민 10% 할인 ─────────
    @SubscribeEvent
    public static void onVillager(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof net.minecraft.world.entity.npc.villager.Villager v) || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (v.isTrading() || stageNear(p.level().getServer(), v.blockPosition()) < 4) return;
        for (MerchantOffer o : v.getOffers()) {
            int c = o.getBaseCostA().getCount();
            int cut = (int) Math.floor(c * 0.10);
            if (cut > 0) o.addToSpecialPriceDiff(-cut);
        }
    }

    /** 다른 곳에서 쓰는 할인율 (마을의 영웅 + 제단 4단계) */
    public static int discountPct(Player p, BlockPos where) {
        return Discounts.pct(p, where);
    }
}
