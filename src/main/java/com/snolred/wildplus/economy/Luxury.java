package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 명품관: 큰돈을 쓰는 곳 (쓴 돈은 국고로 사라져서 물가를 잡는다).
 * 귀한 물건 · 행운 상자 · 경험치 · 몸 주변 오라(게임 속 7일) · 이름 앞 칭호(영구).
 */
public final class Luxury {
    private Luxury() {}

    private record Good(String name, String sub, long price, Consumer<ServerPlayer> give) {}

    public static final String[] AURA = {"불꽃", "눈꽃", "하트", "별빛"};
    public static final String[] TITLE = {"부자", "재벌", "황제"};
    private static final long[] TITLE_PRICE = {1_000_000, 5_000_000, 20_000_000};
    private static final ChatFormatting[] TITLE_COLOR = {ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.GOLD};
    private static final long AURA_PRICE = 500_000, AURA_TICKS = 24000L * 7;

    private static CompoundTag data(MinecraftServer s, ServerPlayer p) {
        return ContentData.get(s).player("luxury", p.getUUID());
    }

    private static void item(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
    }

    private static List<Good> goods(ServerPlayer p) {
        List<Good> g = new ArrayList<>();
        g.add(new Good("✦ 네더의 별", "스타 루비 승급 재료", 1_000_000, pl -> item(pl, new ItemStack(Items.NETHER_STAR))));
        g.add(new Good("✦ 겉날개", "하늘을 나는 날개", 2_000_000, pl -> item(pl, new ItemStack(Items.ELYTRA))));
        g.add(new Good("✦ 용의 깃털", "스카이 사파이어 승급 재료", 1_500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems11.DRAGON_PLUME.get()))));
        g.add(new Good("✦ 불사의 토템", "죽음을 한 번 막아 줌", 500_000, pl -> item(pl, new ItemStack(Items.TOTEM_OF_UNDYING))));
        g.add(new Good("✦ 신호기", "", 1_500_000, pl -> item(pl, new ItemStack(Items.BEACON))));
        g.add(new Good("✦ 셜커 상자", "", 500_000, pl -> item(pl, new ItemStack(Items.SHULKER_BOX))));
        g.add(new Good("✦ 경험치 30레벨", "", 500_000, pl -> pl.giveExperienceLevels(30)));
        g.add(new Good("🎁 행운 상자", "루비·사파이어 블록 · 네더라이트 · 토템 · 가끔 네더의 별·겉날개", 500_000, Luxury::luckyBox));
        CompoundTag d = data(p.level().getServer(), p);
        for (int i = 0; i < AURA.length; i++) {
            int k = i;
            g.add(new Good("✨ 오라: " + AURA[i], "몸 주변에 맴도는 이펙트 · 게임 속 7일", AURA_PRICE, pl -> {
                CompoundTag dd = data(pl.level().getServer(), pl);
                dd.putInt("aura", k);
                dd.putLong("aura_until", Math.max(dd.getLongOr("aura_until", 0L), pl.level().getGameTime()) + AURA_TICKS);
                ContentData.get(pl.level().getServer()).dirty();
            }));
        }
        int owned = d.getIntOr("titles", 0);
        for (int i = 0; i < TITLE.length; i++) {
            int k = i;
            boolean have = (owned & (1 << i)) != 0;
            g.add(new Good("👑 칭호 [" + TITLE[i] + "]" + (have ? " (보유 · 누르면 달기)" : ""), "이름 앞에 영구 표시", have ? 0 : TITLE_PRICE[i], pl -> {
                CompoundTag dd = data(pl.level().getServer(), pl);
                dd.putInt("titles", dd.getIntOr("titles", 0) | (1 << k));
                dd.putInt("title", k + 1);
                ContentData.get(pl.level().getServer()).dirty();
                applyTitle(pl);
            }));
        }
        g.add(new Good("칭호 떼기", "", 0, pl -> {
            data(pl.level().getServer(), pl).putInt("title", 0);
            ContentData.get(pl.level().getServer()).dirty();
            applyTitle(pl);
        }));
        return g;
    }

    private static void luckyBox(ServerPlayer p) {
        float r = p.getRandom().nextFloat();
        List<ItemStack> got = new ArrayList<>();
        if (r < 0.25F) got.add(new ItemStack(Items.NETHERITE_INGOT, 4));
        else if (r < 0.45F) got.add(new ItemStack(com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get(), 2));
        else if (r < 0.65F) got.add(new ItemStack(com.snolred.wildplus.registry.ModItems11.SAPPHIRE_BLOCK.get(), 2));
        else if (r < 0.80F) got.add(new ItemStack(Items.TOTEM_OF_UNDYING, 3));
        else if (r < 0.90F) got.add(new ItemStack(Items.NETHER_STAR));
        else if (r < 0.97F) got.add(new ItemStack(Items.ELYTRA));
        else {
            got.add(new ItemStack(Items.ELYTRA));
            got.add(new ItemStack(Items.NETHER_STAR, 2));
        }
        ItemStack s = got.get(0);
        p.sendSystemMessage(Component.literal("🎁 행운 상자에서: ").withStyle(ChatFormatting.GOLD)
                .append(s.getHoverName().copy().withStyle(s.getRarity().color()))
                .append(Component.literal(s.getCount() > 1 ? " ×" + s.getCount() : ""))
                .append(Component.literal(got.size() > 1 ? " + 네더의 별 ×2 (대박!)" : "")));
        for (ItemStack it : got) item(p, it);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        List<Good> list = goods(p);
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Good g : list) {
            String price = g.price() > 0 ? String.format("%,d동", g.price()) : "";
            entries.add(new PickMenu.Entry(g.name(), g.sub().isEmpty() ? price : g.sub() + (price.isEmpty() ? "" : " · " + price)));
        }
        PickMenu.open(p, Component.literal("💎 명품관"), "고르면 바로 결제 (통장에서 먼저 · 쓴 돈은 국고로)", "", entries, 0, i -> {
            Good g = list.get(i);
            if (g.price() > 0 && !Honor.pay(p, g.price())) {
                p.sendOverlayMessage(Component.literal(String.format("돈이 모자라요 (%,d동)", g.price())).withStyle(ChatFormatting.RED));
                return;
            }
            g.give().accept(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
            if (g.price() > 0) p.sendSystemMessage(Component.literal(String.format("💎 %s 구매 (%,d동)", g.name(), g.price())).withStyle(ChatFormatting.LIGHT_PURPLE));
            open(p, pos);
        }, null, pl -> pos == null || pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }

    /** 칭호: 스코어보드 팀 앞말로 이름 앞에 붙인다 (채팅 · 이름표 · 탭 목록) */
    public static void applyTitle(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        Scoreboard sb = s.getScoreboard();
        String name = p.getScoreboardName();
        PlayerTeam cur = sb.getPlayersTeam(name);
        if (cur != null && cur.getName().startsWith("wp_title_")) sb.removePlayerFromTeam(name, cur);
        int t = data(s, p).getIntOr("title", 0);
        if (t <= 0 || t > TITLE.length) return;
        String team = "wp_title_" + t;
        PlayerTeam pt = sb.getPlayerTeam(team);
        if (pt == null) {
            pt = sb.addPlayerTeam(team);
            pt.setPlayerPrefix(Component.literal("[" + TITLE[t - 1] + "] ").withStyle(TITLE_COLOR[t - 1], ChatFormatting.BOLD));
        }
        sb.addPlayerToTeam(name, pt);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyTitle(p);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 8 != 0 || p.isInvisible()) return;
        CompoundTag d = data(p.level().getServer(), p);
        if (d.getLongOr("aura_until", 0L) < p.level().getGameTime()) return;
        ParticleOptions o = switch (d.getIntOr("aura", 0)) {
            case 1 -> ParticleTypes.SNOWFLAKE;
            case 2 -> ParticleTypes.HEART;
            case 3 -> ParticleTypes.END_ROD;
            default -> ParticleTypes.FLAME;
        };
        double a = p.tickCount * 0.3;
        ((ServerLevel) p.level()).sendParticles(o, p.getX() + Math.cos(a) * 0.7, p.getY() + 1.0 + Math.sin(a * 0.5) * 0.4,
                p.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
    }
}
