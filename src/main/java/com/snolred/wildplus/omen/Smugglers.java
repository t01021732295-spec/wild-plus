package com.snolred.wildplus.omen;

import com.snolred.wildplus.entity.SmugglerEntity;
import com.snolred.wildplus.event.RandomEvents;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems19;
import com.snolred.wildplus.registry.ModItems20;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.8 우민 밀수꾼: 흉조가 걸린 플레이어 근처에 30초마다 12% (흉조 폭풍 36%, 심문으로 정보를 얻었으면 확정) 확률로 나타난다.
 * 흉조 병을 화폐로 받고, 흉조가 풀리거나 10분이 지나면 떠난다.
 */
public final class Smugglers {
    private Smugglers() {}

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    record Good(String name, String sub, int price, int stock, Function<ServerLevel, ItemStack> make) {}

    static List<Good> goods() {
        return List.of(
                new Good("날카로움 VI 책", "날카로움 VI · 소실 저주", 3, 1, l -> book(l, Enchantments.SHARPNESS, 6)),
                new Good("효율 VI 책", "효율 VI · 소실 저주", 3, 1, l -> book(l, Enchantments.EFFICIENCY, 6)),
                new Good("광기의 물약", "힘 III · 신속 II · 저항", 2, 3, l -> new ItemStack(ModItems20.FORBIDDEN_POTION.get())),
                new Good("생명의 정수", "정예 몹 3% · 제작 재료", 4, 2, l -> new ItemStack(ModItems19.LIFE_ESSENCE.get())),
                new Good("겉날개", "비행 장비", 8, 1, l -> new ItemStack(Items.ELYTRA)),
                new Good("불사의 토템", "죽음 1회 방지", 3, 2, l -> new ItemStack(Items.TOTEM_OF_UNDYING)),
                new Good("흉조 병 ×3", "우민 정예화 · 흉조 효과", 1, 3, l -> new ItemStack(ModItems20.OMEN_FLASK.get(), 3)),
                new Good("양조 재료 5종", "테마 흉조 양조 재료", 1, 2, l -> ItemStack.EMPTY),
                new Good("우민 올가미", "약화된 우민 포획", 1, 2, l -> new ItemStack(ModItems20.ILLAGER_LASSO.get())),
                new Good("밀수 지도", "전초 기지 · 대저택 위치", 1, 1, l -> ItemStack.EMPTY));
    }

    private static ItemStack book(ServerLevel l, net.minecraft.resources.ResourceKey<Enchantment> key, int lv) {
        ItemStack b = new ItemStack(Items.ENCHANTED_BOOK);
        var reg = l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> e = reg.getOrThrow(key);
        Holder<Enchantment> curse = reg.getOrThrow(Enchantments.VANISHING_CURSE);
        EnchantmentHelper.updateEnchantments(b, m -> {
            m.set(e, lv);
            m.set(curse, 1);
        });
        return b;
    }

    public static void openShop(ServerPlayer p, SmugglerEntity s) {
        CompoundTag sold = s.getPersistentData().getCompoundOrEmpty(SmugglerEntity.SOLD);
        List<Good> gs = goods();
        List<PickMenu.Entry> es = new ArrayList<>();
        for (int i = 0; i < gs.size(); i++) {
            Good g = gs.get(i);
            int left = g.stock() - sold.getIntOr(String.valueOf(i), 0);
            ItemStack icon = g.make().apply(p.level());
            if (icon.isEmpty()) icon = new ItemStack(i == 7 ? Items.BREWING_STAND : Items.MAP);
            es.add(new PickMenu.Entry(g.name(), left <= 0 ? "품절" : g.sub() + " · 흉조 병 " + g.price() + "개 · 남은 " + left, icon,
                    left <= 0 ? PickMenu.K_INFO : PickMenu.K_ROW));
        }
        PickMenu.open(p, Component.literal("🕯 밀수꾼 상점"), "흉조 병 " + Omen.countBottles(p) + "개 보유 · 흉조 종료 시 퇴장", "", es,
                PickMenu.FLAG_SHOP, i -> {
                    if (i < 0 || i >= gs.size() || !s.isAlive()) return;
                    Good g = gs.get(i);
                    CompoundTag sd = s.getPersistentData().getCompoundOrEmpty(SmugglerEntity.SOLD);
                    int done = sd.getIntOr(String.valueOf(i), 0);
                    if (done >= g.stock()) {
                        p.sendOverlayMessage(Component.literal("품절이에요").withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!Omen.takeBottles(p, g.price())) {
                        p.sendOverlayMessage(Component.literal("흉조 병이 " + g.price() + "개 필요해요").withStyle(ChatFormatting.RED));
                        return;
                    }
                    sd.putInt(String.valueOf(i), done + 1);
                    s.getPersistentData().put(SmugglerEntity.SOLD, sd);
                    deliver(p, i, g);
                    p.level().playSound(null, s.blockPosition(), SoundEvents.VINDICATOR_CELEBRATE, SoundSource.NEUTRAL, 0.8F, 1.3F);
                    openShop(p, s);
                }, null, q -> q.isAlive() && s.isAlive() && q.distanceToSqr(s) < 64);
    }

    private static void deliver(ServerPlayer p, int i, Good g) {
        ServerLevel l = p.level();
        if (i == 7) {
            for (ItemStack st : new ItemStack[]{new ItemStack(Items.BLAZE_POWDER, 2), new ItemStack(Items.BLUE_ICE, 2), new ItemStack(Items.PRISMARINE_CRYSTALS, 2),
                    new ItemStack(Items.PHANTOM_MEMBRANE, 2), new ItemStack(Items.GOLD_BLOCK)}) {
                Omen.give(p, st);
            }
        } else if (i == 9) {
            ServerLevel o = l.getServer().overworld();
            BlockPos a = o.findNearestMapStructure(Prisoners.OUTPOSTS, p.blockPosition(), 100, false);
            BlockPos b = o.findNearestMapStructure(Prisoners.MANSIONS, p.blockPosition(), 100, false);
            p.sendSystemMessage(Component.literal("🗺 밀수 지도: 전초 기지 " + (a == null ? "못 찾음" : a.getX() + ", " + a.getZ())
                    + " · 삼림 대저택 " + (b == null ? "못 찾음" : b.getX() + ", " + b.getZ())).withStyle(ChatFormatting.YELLOW));
        } else {
            Omen.give(p, g.make().apply(l));
        }
        p.sendOverlayMessage(Component.literal("🕯 " + g.name() + " 구매 (흉조 병 " + g.price() + "개)").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** 떠날 때: 10분이 지났거나, 32칸 안에 흉조 걸린 플레이어가 1분 넘게 없음 */
    public static boolean shouldLeave(ServerLevel level, SmugglerEntity s) {
        CompoundTag d = s.getPersistentData();
        long now = level.getGameTime();
        if (d.getLongOr(SmugglerEntity.BORN, -1L) < 0) d.putLong(SmugglerEntity.BORN, now);
        if (now - d.getLongOr(SmugglerEntity.BORN, now) > 12000) return true;
        boolean omen = false;
        for (ServerPlayer p : level.players()) {
            if (Omen.omenLevel(p) >= 0 && p.distanceToSqr(s) < 48 * 48) {
                omen = true;
                break;
            }
        }
        if (omen) {
            d.putLong("wildplus_smuggler_seen", now);
            return false;
        }
        return now - d.getLongOr("wildplus_smuggler_seen", now) > 1200;
    }

    // ───────── 나타나기 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 600 != 0) return;
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        float chance = RandomEvents.isActive(RandomEvents.Type.OMEN_STORM) ? 0.36F : 0.12F;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || Omen.omenLevel(p) < 0 || level.getRaidAt(p.blockPosition()) != null) continue;
            if (now - COOLDOWN.getOrDefault(p.getUUID(), -100000L) < 12000) continue;
            boolean sure = p.getPersistentData().getBooleanOr(Omen.SURE_SMUGGLER, false);
            if (!sure && level.getRandom().nextFloat() > chance) continue;
            if (!level.getEntitiesOfClass(SmugglerEntity.class, p.getBoundingBox().inflate(64)).isEmpty()) continue;
            BlockPos at = spot(level, p);
            if (at == null) continue;
            SmugglerEntity s = ModEntities.SMUGGLER.get().create(level, EntitySpawnReason.EVENT);
            if (s == null) continue;
            s.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
            s.getPersistentData().putLong(SmugglerEntity.BORN, now);
            s.getPersistentData().putLong("wildplus_smuggler_seen", now);
            s.setCustomName(Component.literal("🕯 우민 밀수꾼").withStyle(ChatFormatting.DARK_PURPLE));
            s.setCustomNameVisible(true);
            level.addFreshEntity(s);
            COOLDOWN.put(p.getUUID(), now);
            p.getPersistentData().remove(Omen.SURE_SMUGGLER);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, s.getX(), s.getY() + 1, s.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            p.sendSystemMessage(Component.literal(String.format("🕯 흉조 냄새를 맡고 우민 밀수꾼이 나타났어요! (%d, %d · 흉조 병으로 거래 · 흉조가 풀리면 떠나요)",
                    at.getX(), at.getZ())).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static BlockPos spot(ServerLevel level, ServerPlayer p) {
        var rnd = level.getRandom();
        for (int i = 0; i < 10; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = 6 + rnd.nextDouble() * 6;
            int x = Mth.floor(p.getX() + Math.cos(ang) * d), z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(top.getY() - p.getY()) > 8) continue;
            if (level.getBlockState(top).isAir() && level.getBlockState(top.below()).isSolid() && level.getFluidState(top).isEmpty()) return top;
        }
        return null;
    }
}
