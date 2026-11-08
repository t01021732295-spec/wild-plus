package com.snolred.wildplus.omen;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.Mail;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.entity.MercenaryEntity;
import com.snolred.wildplus.item.LassoItem;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems19;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.8 우민 감옥: 올가미로 묶은 우민을 가둔다 (감옥 하나에 4명).
 * 포로마다 💰 몸값(다음 날 아침 통장 + 우편) · 🛡 전향(흉조 병 1 → 우민 용병) · 📜 심문(기력 3번: 전초 기지 · 밀수꾼 · 저택).
 */
public final class Prisoners {
    private Prisoners() {}

    public static final int MAX = 4, MAX_MERCS = 4;
    public static final TagKey<Structure> OUTPOSTS = TagKey.create(Registries.STRUCTURE, WildPlus.id("outposts"));
    public static final TagKey<Structure> MANSIONS = TagKey.create(Registries.STRUCTURE, WildPlus.id("mansions"));

    private static CompoundTag root(MinecraftServer s) {
        return ContentData.get(s).section("omen_prison");
    }

    private static String key(ServerLevel l, BlockPos p) {
        return l.dimension().identifier() + "|" + p.getX() + "|" + p.getY() + "|" + p.getZ();
    }

    private static List<CompoundTag> list(CompoundTag cell) {
        List<CompoundTag> out = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            CompoundTag t = cell.getCompoundOrEmpty(String.valueOf(i));
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static void store(MinecraftServer s, String k, List<CompoundTag> ps) {
        CompoundTag cell = new CompoundTag();
        for (int i = 0; i < ps.size(); i++) cell.put(String.valueOf(i), ps.get(i));
        if (ps.isEmpty()) root(s).remove(k);
        else root(s).put(k, cell);
        ContentData.get(s).dirty();
        sync(s, k);
    }

    // ───────── 3.9 감옥 안에 포로 모습 ─────────
    private static ServerLevel levelOf(MinecraftServer s, String k) {
        String dim = k.substring(0, k.indexOf('|'));
        for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(dim)) return l;
        return null;
    }

    private static BlockPos posOf(String k) {
        String[] a = k.split("\\|");
        return new BlockPos(Integer.parseInt(a[a.length - 3]), Integer.parseInt(a[a.length - 2]), Integer.parseInt(a[a.length - 1]));
    }

    private static boolean isPrison(ServerLevel l, BlockPos pos) {
        return l.getBlockState(pos).is(com.snolred.wildplus.registry.ModBlocks9.ILLAGER_PRISON.get());
    }

    /** 포로 그림이 아직 자료와 맞는가 (아니면 스스로 사라짐) */
    public static boolean stillHeld(com.snolred.wildplus.entity.PrisonerEntity e) {
        if (e.cell.isEmpty() || !(e.level() instanceof ServerLevel l)) return false;
        List<CompoundTag> ps = list(root(l.getServer()).getCompoundOrEmpty(e.cell));
        return e.slot < ps.size() && e.prison != null && isPrison(l, e.prison)
                && com.snolred.wildplus.entity.PrisonerEntity.kindOf(ps.get(e.slot).getStringOr("type", "")) == e.kind();
    }

    /** 자료대로 포로를 세우고, 남는 그림은 지운다 */
    static void sync(MinecraftServer s, String k) {
        ServerLevel l;
        BlockPos pos;
        try {
            l = levelOf(s, k);
            pos = posOf(k);
        } catch (RuntimeException ex) {
            return;
        }
        if (l == null || !l.isLoaded(pos)) return;
        List<com.snolred.wildplus.entity.PrisonerEntity> have = l.getEntities(ModEntities.PRISONER.get(),
                new net.minecraft.world.phys.AABB(pos).inflate(4), e -> k.equals(e.cell));
        if (!isPrison(l, pos)) {
            // 감옥이 부서짐: 포로들은 달아난다
            for (var e : have) e.discard();
            if (root(s).contains(k)) {
                root(s).remove(k);
                ContentData.get(s).dirty();
                l.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 1, 0.6, 1, 0.02);
            }
            return;
        }
        List<CompoundTag> ps = list(root(s).getCompoundOrEmpty(k));
        net.minecraft.core.Direction f = l.getBlockState(pos).getValue(com.snolred.wildplus.block.BigBlock.FACING);
        net.minecraft.core.Direction right = f.getClockWise(), back = f.getOpposite();
        com.snolred.wildplus.entity.PrisonerEntity[] slot = new com.snolred.wildplus.entity.PrisonerEntity[MAX];
        for (var e : have) {
            if (e.slot < ps.size() && slot[e.slot] == null
                    && com.snolred.wildplus.entity.PrisonerEntity.kindOf(ps.get(e.slot).getStringOr("type", "")) == e.kind()) {
                slot[e.slot] = e;
            } else {
                e.discard();
            }
        }
        double[] lat = {-1.05, 1.05, -0.35, 0.35};
        double[] dep = {0.85, 0.85, 0.95, 0.95};
        for (int i = 0; i < ps.size(); i++) {
            if (slot[i] != null) continue;
            var e = ModEntities.PRISONER.get().create(l, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (e == null) continue;
            double x = pos.getX() + 0.5 + right.getStepX() * lat[i] + back.getStepX() * dep[i];
            double z = pos.getZ() + 0.5 + right.getStepZ() * lat[i] + back.getStepZ() * dep[i];
            float yaw = f.toYRot() + (i % 2 == 0 ? 12F : -12F);
            e.snapTo(x, pos.getY() + 0.06, z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.yBodyRot = yaw;
            e.cell = k;
            e.slot = i;
            e.prison = pos.immutable();
            CompoundTag t = ps.get(i);
            e.setKind(com.snolred.wildplus.entity.PrisonerEntity.kindOf(t.getStringOr("type", "")), t.getBooleanOr("elite", false));
            l.addFreshEntity(e);
        }
    }

    static long ransom(String type, boolean elite) {
        long v = switch (type) {
            case "minecraft:vindicator" -> 35_000;
            case "minecraft:evoker" -> 90_000;
            case "minecraft:illusioner" -> 150_000;
            default -> 25_000;
        };
        return elite ? v * 2 : v;
    }

    private static String title(CompoundTag t) {
        return (t.getBooleanOr("elite", false) ? "정예 " : "") + LassoItem.nameOf(t.getStringOr("type", "")) + " 포로";
    }

    // ───────── 열기 ─────────
    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        MinecraftServer s = level.getServer();
        String k = key(level, pos);
        List<CompoundTag> ps = list(root(s).getCompoundOrEmpty(k));
        ItemStack hand = p.getMainHandItem();
        if (hand.getItem() instanceof LassoItem && !LassoItem.captive(hand).isEmpty()) {
            if (ps.size() >= MAX) {
                p.sendOverlayMessage(Component.literal("감옥이 꽉 찼어요 (4명)").withStyle(ChatFormatting.RED));
                return;
            }
            CompoundTag t = new CompoundTag();
            t.putString("type", LassoItem.captive(hand));
            t.putBoolean("elite", LassoItem.captiveElite(hand));
            t.putInt("stamina", 3);
            t.putString("by", p.getUUID().toString());
            ps.add(t);
            store(s, k, ps);
            LassoItem.clear(hand);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 0.5, 15, 0.5, 0.5, 0.3, 0.02);
            p.sendSystemMessage(Component.literal("⛓ " + title(t) + "을(를) 감옥에 가뒀어요 (" + ps.size() + "/4) — 감옥을 눌러 몸값 · 전향 · 심문")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        List<PickMenu.Entry> es = new ArrayList<>();
        for (CompoundTag t : ps) {
            String sub = "기력 " + t.getIntOr("stamina", 0) + "/3";
            if (!t.getStringOr("ransomBy", "").isEmpty()) sub += " · 💰 몸값 대기 (내일 아침)";
            es.add(new PickMenu.Entry("⛓ " + title(t), sub, new ItemStack(icon(t.getStringOr("type", "")))));
        }
        PickMenu.open(p, Component.literal("⛓ 우민 감옥"), "포로 " + ps.size() + "/4 · 올가미로 묶은 우민을 들고 감옥을 누르면 가둬요",
                "비어 있어요 — 체력 40% 아래로 만든 우민을 올가미로 묶어 오세요", es, PickMenu.themed(PickMenu.T_PRISON), i -> {
                    if (i >= 0 && i < ps.size()) prisoner(p, pos, i);
                }, null, q -> near(q, pos));
    }

    private static boolean near(net.minecraft.world.entity.player.Player q, BlockPos pos) {
        return q.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }

    private static net.minecraft.world.item.Item icon(String type) {
        return switch (type) {
            case "minecraft:vindicator" -> Items.IRON_AXE;
            case "minecraft:evoker" -> Items.TOTEM_OF_UNDYING;
            case "minecraft:illusioner" -> Items.ENDER_EYE;
            default -> Items.CROSSBOW;
        };
    }

    private static void prisoner(ServerPlayer p, BlockPos pos, int idx) {
        ServerLevel level = p.level();
        MinecraftServer s = level.getServer();
        String k = key(level, pos);
        List<CompoundTag> ps = list(root(s).getCompoundOrEmpty(k));
        if (idx >= ps.size()) return;
        CompoundTag t = ps.get(idx);
        String type = t.getStringOr("type", "");
        boolean elite = t.getBooleanOr("elite", false);
        int stamina = t.getIntOr("stamina", 0);
        boolean waiting = !t.getStringOr("ransomBy", "").isEmpty();
        int mercs = mercCount(s, p.getUUID());
        List<PickMenu.Entry> es = new ArrayList<>();
        es.add(waiting ? PickMenu.Entry.of(PickMenu.K_INFO, "💰 몸값 대기 중", "내일 아침에 몸값 상인이 와요")
                : new PickMenu.Entry("💰 몸값 요구", "내일 아침 통장 " + Omen.won(ransom(type, elite)) + " + 에메랄드 · 가끔 생명의 정수 (우편)",
                new ItemStack(Items.GOLD_INGOT), PickMenu.K_PRIMARY));
        es.add(new PickMenu.Entry("🛡 전향 → 우민 용병", "흉조 병 1개 · 내 용병 " + mercs + "/" + MAX_MERCS + " · 나를 따라다니며 싸워요",
                new ItemStack(Items.SHIELD), PickMenu.K_PRIMARY));
        es.add(new PickMenu.Entry("📜 심문", stamina <= 0 ? "더 털어놓을 게 없어요" : "기력 " + stamina + "/3 · "
                + (stamina == 3 ? "가까운 전초 기지 위치" : stamina == 2 ? "다음 흉조 때 밀수꾼이 꼭 나타남" : "삼림 대저택 위치"),
                new ItemStack(Items.WRITABLE_BOOK), stamina <= 0 ? PickMenu.K_INFO : PickMenu.K_PRIMARY));
        es.add(PickMenu.Entry.of(PickMenu.K_SMALL, "◀ 감옥으로", ""));
        PickMenu.open(p, Component.literal("⛓ " + title(t)), "기력 " + stamina + "/3", "", es, PickMenu.themed(PickMenu.T_PRISON), i -> {
            List<CompoundTag> cur = list(root(s).getCompoundOrEmpty(k));
            if (idx >= cur.size()) return;
            CompoundTag c = cur.get(idx);
            switch (i) {
                case 0 -> {
                    if (!c.getStringOr("ransomBy", "").isEmpty()) return;
                    c.putString("ransomBy", p.getUUID().toString());
                    c.putLong("ransomDay", ContentData.gameDay);
                    store(s, k, cur);
                    p.sendSystemMessage(Component.literal("💰 몸값 편지를 보냈어요. 내일 아침 " + Omen.won(ransom(type, elite)) + "이 통장으로 들어와요")
                            .withStyle(ChatFormatting.GOLD));
                    p.closeContainer();
                }
                case 1 -> {
                    if (mercCount(s, p.getUUID()) >= MAX_MERCS) {
                        p.sendOverlayMessage(Component.literal("용병은 4명까지예요").withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!Omen.takeBottles(p, 1)) {
                        p.sendOverlayMessage(Component.literal("흉조 병 1개가 필요해요 (마음을 돌릴 대가)").withStyle(ChatFormatting.RED));
                        return;
                    }
                    cur.remove(idx);
                    store(s, k, cur);
                    MercenaryEntity m = MercenaryEntity.spawn(level, pos.relative(level.getBlockState(pos).getValue(
                            com.snolred.wildplus.block.BigBlock.FACING)), p);
                    if (m != null && c.getBooleanOr("elite", false)) {
                        var hp = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                        if (hp != null) hp.setBaseValue(60);
                        m.setHealth(m.getMaxHealth());
                    }
                    level.playSound(null, pos, SoundEvents.VINDICATOR_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    p.sendSystemMessage(Component.literal("🛡 " + title(c) + "이(가) 마음을 돌려 우민 용병이 됐어요! (웅크리고 누르면 지키기 · 철괴로 회복)")
                            .withStyle(ChatFormatting.AQUA));
                    p.closeContainer();
                }
                case 2 -> {
                    int st = c.getIntOr("stamina", 0);
                    if (st <= 0) return;
                    c.putInt("stamina", st - 1);
                    store(s, k, cur);
                    p.closeContainer();
                    interrogate(p, st);
                }
                case 3 -> open(p, pos);
                default -> {}
            }
        }, null, q -> near(q, pos));
    }

    private static void interrogate(ServerPlayer p, int stamina) {
        ServerLevel level = p.level().getServer().overworld();
        p.level().playSound(null, p.blockPosition(), SoundEvents.VINDICATOR_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.7F);
        if (stamina == 2) {
            p.getPersistentData().putBoolean(Omen.SURE_SMUGGLER, true);
            p.sendSystemMessage(Component.literal("📜 \"…다음에 흉조가 걸리면 밀수꾼이 너를 찾아갈 거다.\" (다음 흉조 때 밀수꾼 확정)").withStyle(ChatFormatting.YELLOW));
            return;
        }
        BlockPos found = level.findNearestMapStructure(stamina == 3 ? OUTPOSTS : MANSIONS, p.blockPosition(), 100, false);
        if (found == null) {
            p.sendSystemMessage(Component.literal("📜 \"…모른다. 정말이다.\" (근처에서 찾지 못했어요)").withStyle(ChatFormatting.GRAY));
            return;
        }
        p.sendSystemMessage(Component.literal(String.format("📜 \"…%s은(는) %d, %d 근처에 있다.\" (여기서 %d칸)", stamina == 3 ? "우리 전초 기지" : "삼림 대저택",
                found.getX(), found.getZ(), (int) Math.sqrt(found.distSqr(p.blockPosition())))).withStyle(ChatFormatting.YELLOW));
    }

    static int mercCount(MinecraftServer s, UUID owner) {
        int n = 0;
        for (ServerLevel l : s.getAllLevels()) {
            n += l.getEntities(ModEntities.MERCENARY.get(), m -> owner.equals(m.ownerId()) && m.isAlive()).size();
        }
        return n;
    }

    // ───────── 아침마다 몸값 지급 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 40 == 20) {
            for (String k : new ArrayList<>(root(s).keySet())) sync(s, k);
        }
        if (s.getTickCount() % 200 != 0) return;
        CompoundTag r = root(s);
        long today = ContentData.gameDay;
        for (String k : new ArrayList<>(r.keySet())) {
            List<CompoundTag> ps = list(r.getCompoundOrEmpty(k));
            boolean changed = false;
            for (CompoundTag t : new ArrayList<>(ps)) {
                String by = t.getStringOr("ransomBy", "");
                if (by.isEmpty() || today <= t.getLongOr("ransomDay", today)) continue;
                UUID id = Omen.uuidOr(by);
                ps.remove(t);
                changed = true;
                if (id == null) continue;
                String type = t.getStringOr("type", "");
                boolean elite = t.getBooleanOr("elite", false);
                long money = ransom(type, elite);
                WildData wd = WildData.get(s);
                wd.addBalance(id, money);
                wd.setDirty();
                var rnd = s.overworld().getRandom();
                List<ItemStack> gifts = new ArrayList<>();
                gifts.add(new ItemStack(Items.EMERALD, 8 + rnd.nextInt(9)));
                if (rnd.nextFloat() < (elite ? 0.35F : 0.15F)) gifts.add(new ItemStack(ModItems19.LIFE_ESSENCE.get()));
                if (type.equals("minecraft:evoker") && rnd.nextFloat() < 0.5F) gifts.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                if (rnd.nextFloat() < 0.3F) gifts.add(Omen.withAmp(new ItemStack(Items.OMINOUS_BOTTLE), rnd.nextInt(3)));
                Mail.put(s, id, gifts);
                Mail.notify(s, id, "몸값 상인", gifts.size());
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("💰 몸값 상인이 " + title(t) + "을(를) 데려가고 " + Omen.won(money)
                            + "을 통장에 넣었어요 (물건은 우편함)").withStyle(ChatFormatting.GOLD));
                }
            }
            if (changed) store(s, k, ps);
        }
    }
}
