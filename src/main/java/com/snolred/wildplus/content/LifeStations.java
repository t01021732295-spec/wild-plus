package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Grade;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 5.0 생활 시설. 블록 엔티티 없이 "자리 → 내용물"을 ContentData("life") 에 적어 두고, 누를 때 지난 시간으로 계산한다.
 * - 양봉 상자: 주변 꽃이 많을수록 꿀이 빨리 참 (최대 16) · 꿀 2개마다 벌집 1 · 가끔 로열젤리 · 겨울엔 절반
 * - 건조대 · 장독대: 재료를 들고 누르면 넣고(한 번에 16개까지), 다 되면 빈손으로 꺼냄
 * - 양어장: 같은 물고기 2마리 이상 넣으면 게임 하루마다 1마리씩 늘어남 (최대 +16) · 웅크리고 누르면 전부 꺼냄
 */
public final class LifeStations {
    private LifeStations() {}

    public enum Kind { BEEHIVE, DRYING, JANGDOK, FISHFARM }

    /** 재료 → 결과 · 재료 1개당 결과 수 · 걸리는 틱 */
    public record Recipe(String out, int count, int ticks) {}

    private static final Map<String, Recipe> DRY = new LinkedHashMap<>(), JAR = new LinkedHashMap<>();
    private static final int HONEY_TICKS = 6000, HONEY_MAX = 16, FISH_TICKS = 24000, FISH_MAX = 16, BATCH = 16;

    static {
        DRY.put("wildplus:chili", new Recipe("wildplus:chili_powder", 1, 4800));
        DRY.put("wildplus:grape", new Recipe("wildplus:raisin", 1, 4800));
        DRY.put("minecraft:beef", new Recipe("wildplus:jerky", 1, 6000));
        DRY.put("minecraft:porkchop", new Recipe("wildplus:jerky", 1, 6000));
        DRY.put("minecraft:mutton", new Recipe("wildplus:jerky", 1, 6000));
        DRY.put("@fish", new Recipe("wildplus:dried_fish", 1, 6000));
        JAR.put("wildplus:radish", new Recipe("wildplus:kkakdugi", 2, 24000));
        JAR.put("wildplus:cabbage", new Recipe("wildplus:danmuji", 2, 24000)); // 절임 (단무지)
        JAR.put("wildplus:soybean", new Recipe("wildplus:doenjang", 1, 48000));
        JAR.put("wildplus:doenjang", new Recipe("wildplus:soy_sauce", 1, 24000));
    }

    private static String key(Level level, BlockPos pos) {
        return level.dimension().identifier() + "/" + pos.getX() + "/" + pos.getY() + "/" + pos.getZ();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static String id(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    private static String name(String id) {
        return new ItemStack(item(id)).getHoverName().getString();
    }

    private static void give(ServerPlayer p, String id, int n) {
        Item it = item(id);
        while (n > 0 && it != Items.AIR) {
            ItemStack s = new ItemStack(it, Math.min(n, 64));
            n -= s.getCount();
            if (!p.getInventory().add(s)) Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
        }
    }

    private static void bar(ServerPlayer p, String msg, ChatFormatting c) {
        p.sendOverlayMessage(Component.literal(msg).withStyle(c));
    }

    private static String time(long ticks) {
        long sec = Math.max(1, ticks / 20);
        return sec >= 60 ? (sec / 60) + "분 " + (sec % 60) + "초" : sec + "초";
    }

    private static boolean legend(String id) {
        return id.equals("wildplus:dragon_king_fish") || id.equals("wildplus:golden_carp");
    }

    public static void use(ServerPlayer p, BlockPos pos, Kind kind) {
        ServerLevel level = (ServerLevel) p.level();
        ContentData cd = ContentData.get(level.getServer());
        CompoundTag all = cd.section("life");
        String k = key(level, pos);
        CompoundTag d = all.get(k) instanceof CompoundTag c ? c : new CompoundTag();
        long now = level.getGameTime();
        ItemStack hand = p.getMainHandItem();
        switch (kind) {
            case BEEHIVE -> {
                if (!d.contains("t")) {
                    d.putLong("t", now);
                    all.put(k, d);
                    cd.dirty();
                    bar(p, "🐝 벌들이 일을 시작했어요 · 주변에 꽃을 심으면 꿀이 빨리 차요", ChatFormatting.YELLOW);
                    return;
                }
                int flowers = 0;
                for (BlockPos q : BlockPos.betweenClosed(pos.offset(-5, -2, -5), pos.offset(5, 2, 5))) if (level.getBlockState(q).is(BlockTags.FLOWERS)) flowers++;
                flowers = Math.min(12, flowers);
                long per = (long) (HONEY_TICKS / (1 + flowers * 0.25)) * (Seasons.now() == Seasons.WINTER ? 2 : 1);
                long elapsed = now - d.getLongOr("t", now);
                int honey = (int) Math.min(HONEY_MAX, elapsed / per);
                if (honey <= 0) {
                    bar(p, String.format("🐝 꿀 모으는 중 · 다음 꿀까지 %s (꽃 %d송이%s)", time(per - elapsed), flowers, Seasons.now() == Seasons.WINTER ? " · 겨울이라 느려요" : ""), ChatFormatting.YELLOW);
                    return;
                }
                d.putLong("t", now);
                all.put(k, d);
                cd.dirty();
                give(p, "wildplus:honey_jar", honey);
                if (honey >= 2) give(p, "minecraft:honeycomb", honey / 2);
                int jelly = 0;
                for (int i = 0; i < honey; i++) if (level.getRandom().nextInt(12) == 0) jelly++;
                if (jelly > 0) give(p, "wildplus:royal_jelly", jelly);
                bar(p, String.format("🍯 꿀단지 %d개%s%s", honey, honey >= 2 ? " · 벌집 " + honey / 2 : "", jelly > 0 ? " · 로열젤리 " + jelly + "!" : ""), ChatFormatting.GOLD);
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.9F, 1.0F);
                Codex.add(p, "honey", honey);
            }
            case DRYING, JANGDOK -> {
                Map<String, Recipe> table = kind == Kind.DRYING ? DRY : JAR;
                String what = kind == Kind.DRYING ? "건조대" : "장독대";
                String in = d.getStringOr("in", "");
                if (!in.isEmpty()) {
                    Recipe r = table.getOrDefault(in, table.values().iterator().next());
                    long left = d.getLongOr("t", now) + r.ticks() - now;
                    if (left > 0) {
                        bar(p, String.format("⏳ %s %d개 %s · %s 남음", name(r.out()), d.getIntOr("n", 0) * r.count(), kind == Kind.DRYING ? "말리는 중" : "익는 중", time(left)), ChatFormatting.YELLOW);
                        return;
                    }
                    int n = d.getIntOr("n", 0) * r.count();
                    all.remove(k);
                    cd.dirty();
                    give(p, r.out(), n);
                    bar(p, String.format("✔ %s %d개 완성!", name(r.out()), n), ChatFormatting.GREEN);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                    Codex.add(p, "craft", n);
                    return;
                }
                String hid = id(hand);
                String rk = table.containsKey(hid) ? hid : (kind == Kind.DRYING && !hand.isEmpty() && Grade.isFish(hand) && !legend(hid)) ? "@fish" : null;
                if (rk == null) {
                    StringBuilder sb = new StringBuilder("📋 " + what + ": ");
                    boolean first = true;
                    for (Map.Entry<String, Recipe> e : table.entrySet()) {
                        if (!first) sb.append(" · ");
                        first = false;
                        sb.append(e.getKey().equals("@fish") ? "물고기" : name(e.getKey())).append(" → ").append(name(e.getValue().out()));
                    }
                    p.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
                    bar(p, "재료를 들고 눌러 주세요 (한 번에 16개까지)", ChatFormatting.YELLOW);
                    return;
                }
                int n = Math.min(BATCH, hand.getCount());
                hand.shrink(n);
                d.putString("in", rk);
                d.putString("raw", hid);
                d.putInt("n", n);
                d.putLong("t", now);
                all.put(k, d);
                cd.dirty();
                Recipe r = table.get(rk);
                bar(p, String.format("%s %d개를 넣었어요 · %s 뒤 %s 완성", rk.equals("@fish") ? "물고기" : name(rk), n, time(r.ticks()), name(r.out())), ChatFormatting.AQUA);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            case FISHFARM -> {
                String fish = d.getStringOr("in", "");
                int n = d.getIntOr("n", 0);
                long t = d.getLongOr("t", now);
                int born = n >= 2 ? (int) Math.min(FISH_MAX, (now - t) / FISH_TICKS) : 0;
                String hid = id(hand);
                boolean canAdd = !hand.isEmpty() && Grade.isFish(hand) && !legend(hid) && (fish.isEmpty() || fish.equals(hid));
                if (p.isShiftKeyDown() && !fish.isEmpty()) {
                    all.remove(k);
                    cd.dirty();
                    give(p, fish, n + born);
                    bar(p, String.format("🐟 %s %d마리를 모두 꺼냈어요", name(fish), n + born), ChatFormatting.AQUA);
                    return;
                }
                if (canAdd && n < 8) {
                    int add = Math.min(8 - n, hand.getCount());
                    hand.shrink(add);
                    if (born > 0) give(p, hid, born);
                    d.putString("in", hid);
                    d.putInt("n", n + add);
                    d.putLong("t", now);
                    all.put(k, d);
                    cd.dirty();
                    bar(p, String.format("🐟 %s %d마리 (어미) · 2마리 이상이면 하루에 1마리씩 늘어요", name(hid), n + add), ChatFormatting.AQUA);
                    level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.8F, 1.0F);
                    return;
                }
                if (fish.isEmpty()) {
                    bar(p, !hand.isEmpty() && legend(hid) ? "전설의 물고기는 양식할 수 없어요" : "같은 물고기 2마리 이상을 들고 눌러 주세요", ChatFormatting.YELLOW);
                    return;
                }
                if (born <= 0) {
                    bar(p, n < 2 ? String.format("🐟 %s %d마리 · 1마리 더 넣어야 늘어나요", name(fish), n)
                            : String.format("🐟 %s 어미 %d마리 · 다음 새끼까지 %s · 웅크리고 누르면 전부 꺼냄", name(fish), n, time(FISH_TICKS - (now - t) % FISH_TICKS)), ChatFormatting.YELLOW);
                    return;
                }
                d.putLong("t", now);
                all.put(k, d);
                cd.dirty();
                give(p, fish, born);
                bar(p, String.format("🐟 %s %d마리를 건졌어요 (어미 %d마리는 그대로)", name(fish), born, n), ChatFormatting.GREEN);
                level.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.8F, 1.0F);
                Codex.add(p, "farmfish", born);
            }
        }
    }

    /** 부술 때: 넣어 둔 재료 · 어미 물고기를 돌려준다 */
    public static void spill(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        ContentData cd = ContentData.get(sl.getServer());
        CompoundTag all = cd.section("life");
        String k = key(level, pos);
        if (!(all.get(k) instanceof CompoundTag d)) return;
        all.remove(k);
        cd.dirty();
        String raw = d.getStringOr("raw", d.getStringOr("in", ""));
        int n = d.getIntOr("n", 0);
        Item it = raw.isEmpty() || raw.startsWith("@") ? Items.AIR : item(raw);
        if (it != Items.AIR && n > 0) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(it, Math.min(64, n)));
    }
}
