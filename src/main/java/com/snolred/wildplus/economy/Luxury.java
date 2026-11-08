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
 * 귀한 물건 · 행운 상자 · 승급 재료 · 몸 주변 오라(게임 속 7일) · 이름 앞 칭호(영구).
 */
public final class Luxury {
    private Luxury() {}

    private record Good(String name, String sub, long price, Consumer<ServerPlayer> give) {
        Good {
            price = com.snolred.wildplus.economy.Prices.x(price); // 3.7 물가 인상
        }
    }

    public static final String[] AURA = {"불꽃", "눈꽃", "하트", "별빛"};
    /** 앞 3개는 명품관에서 돈으로, 뒤 2개는 투기장 메달 상점에서 */
    public static final String[] TITLE = {"부자", "재벌", "황제", "검투사", "챔피언"};
    public static final int SHOP_TITLES = 3;
    private static final long[] TITLE_PRICE = {1_000_000, 5_000_000, 20_000_000, 0, 0};
    private static final ChatFormatting[] TITLE_COLOR = {ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.GOLD,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE};

    /** 칭호 i(0부터)를 얻고 바로 단다 */
    public static void grantTitle(ServerPlayer p, int i) {
        CompoundTag dd = data(p.level().getServer(), p);
        dd.putInt("titles", dd.getIntOr("titles", 0) | (1 << i));
        dd.putInt("title", i + 1);
        ContentData.get(p.level().getServer()).dirty();
        applyTitle(p);
    }

    public static boolean hasTitle(ServerPlayer p, int i) {
        return (data(p.level().getServer(), p).getIntOr("titles", 0) & (1 << i)) != 0;
    }
    private static final long AURA_PRICE = 500_000, AURA_TICKS = 24000L * 7;

    private static CompoundTag data(MinecraftServer s, ServerPlayer p) {
        return ContentData.get(s).player("luxury", p.getUUID());
    }

    private static void item(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
    }

    private static List<Good> goods(ServerPlayer p) {
        List<Good> g = new ArrayList<>();
        g.add(new Good("✦ 네더의 별", "스텔라 코어 재료", 1_000_000, pl -> item(pl, new ItemStack(Items.NETHER_STAR))));
        g.add(new Good("✦ 스텔라 코어", "스텔라 루비 승급 재료", 1_500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems13.STELLAR_CORE.get()))));
        g.add(new Good("✦ 겉날개", "하늘을 나는 날개", 2_000_000, pl -> item(pl, new ItemStack(Items.ELYTRA))));
        g.add(new Good("✦ 용의 깃털", "스카이 사파이어 승급 재료", 1_500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems11.DRAGON_PLUME.get()))));
        g.add(new Good("✦ 생명의 결정", "로열 에메랄드 승급 재료", 1_500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems12.LIFE_CRYSTAL.get()))));
        g.add(new Good("✦ 불사의 토템", "죽음을 한 번 막아 줌", 500_000, pl -> item(pl, new ItemStack(Items.TOTEM_OF_UNDYING))));
        g.add(new Good("✦ 신호기", "", 1_500_000, pl -> item(pl, new ItemStack(Items.BEACON))));
        g.add(new Good("✦ 셜커 상자", "", 500_000, pl -> item(pl, new ItemStack(Items.SHULKER_BOX))));
        g.add(new Good("✦ 생명의 결정", "로열 에메랄드 승급 재료", 1_500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems12.LIFE_CRYSTAL.get()))));
        g.add(new Good("🎁 행운 상자", "들고 우클릭해서 열기 · 루비·사파이어 블록 · 네더라이트 · 토템 · 가끔 네더의 별·겉날개", 500_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems13.LUCKY_BOX.get()))));
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
            if (i >= SHOP_TITLES && !have) continue; // 투기장 칭호는 가진 경우에만 달기 메뉴로
            g.add(new Good("👑 칭호 [" + TITLE[i] + "]" + (have ? " (보유 · 누르면 달기)" : ""), "이름 앞에 영구 표시", have ? 0 : TITLE_PRICE[i],
                    pl -> grantTitle(pl, k)));
        }
        // ───────── 치장 (몸에 그려짐) ─────────
        // 옛 옷(비트 5~8)은 없어졌으니 산 값을 통장으로 돌려준다
        int cos = d.getIntOr("cos", 0);
        int oldOutfits = Integer.bitCount(cos & (0b1111 << 5));
        if (oldOutfits > 0) {
            WildData wd = WildData.get(p.level().getServer());
            wd.addBalance(p.getUUID(), oldOutfits * 1_000_000L);
            wd.setDirty();
            cos &= ~(0b1111 << 5);
            d.putInt("cos", cos);
            d.remove("outfit");
            ContentData.get(p.level().getServer()).dirty();
            p.sendSystemMessage(Component.literal(String.format("💎 옷 치장이 없어져서 %,d동을 통장으로 돌려드렸어요", oldOutfits * 1_000_000L))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        // {종류, 값, 이름, 가격, 보유 비트}
        String[][] items = {
                {"wings", "1", "🪽 천사의 날개", "1500000", "0"}, {"wings", "2", "🦇 악마의 날개", "1500000", "1"}, {"wings", "3", "🐉 용의 날개", "2500000", "2"},
                {"halo", "1", "😇 황금 링", "1000000", "3"}, {"halo", "2", "✨ 별빛 링", "1500000", "4"},
                {"hat", "1", "🐤 오리 탈", "700000", "9"}, {"hat", "2", "🐰 토끼 탈", "700000", "10"}, {"hat", "3", "🐱 고양이 탈", "700000", "11"},
                {"hat", "4", "🐻 곰 탈", "700000", "12"}, {"hat", "5", "🐸 개구리 탈", "700000", "13"}};
        for (String[] it : items) {
            int bit = 1 << Integer.parseInt(it[4]);
            boolean have = (cos & bit) != 0;
            int val = Integer.parseInt(it[1]);
            boolean worn = d.getIntOr(it[0], 0) == val;
            String label = it[2] + (worn ? " (착용 중 · 누르면 벗기)" : have ? " (보유 · 누르면 쓰기)" : "");
            g.add(new Good(label, "치장", have ? 0 : Long.parseLong(it[3]), pl -> {
                CompoundTag dd = data(pl.level().getServer(), pl);
                dd.putInt("cos", dd.getIntOr("cos", 0) | bit);
                dd.putInt(it[0], dd.getIntOr(it[0], 0) == val && have ? 0 : val);
                ContentData.get(pl.level().getServer()).dirty();
                com.snolred.wildplus.net.CosmeticSync.broadcast(pl.level().getServer());
            }));
        }
        g.add(new Good("⚔ 관상용 무기: 황금 왕의 대검", "", 1_000_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems12.DISPLAY_GREATSWORD.get()))));
        g.add(new Good("⚔ 관상용 무기: 별빛 지팡이", "", 1_000_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems12.DISPLAY_STAFF.get()))));
        g.add(new Good("⚔ 관상용 무기: 공허의 낫", "", 1_000_000,
                pl -> item(pl, new ItemStack(com.snolred.wildplus.registry.ModItems12.DISPLAY_SCYTHE.get()))));
        g.add(new Good("칭호 떼기", "", 0, pl -> {
            data(pl.level().getServer(), pl).putInt("title", 0);
            ContentData.get(pl.level().getServer()).dirty();
            applyTitle(pl);
        }));
        return g;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        if (com.snolred.wildplus.economy.Loans.locked(p.level().getServer(), p.getUUID())) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("대출 연체 7일 이상: 명품관을 이용할 수 없어요").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        List<Good> list = goods(p);
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Good g : list) {
            String price = g.price() > 0 ? String.format("%,d동", g.price()) : "";
            entries.add(new PickMenu.Entry(g.name(), price.isEmpty() && g.price() == 0 ? "" : price, icon(g.name())));
        }
        PickMenu.open(p, Component.literal("💎 명품관"), "고르면 바로 결제 · 통장에서 먼저", "", entries, PickMenu.FLAG_SHOP, i -> {
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

    /** 진열장에 놓을 물건 그림 */
    private static ItemStack icon(String name) {
        String[][] map = {
                {"네더의 별", "minecraft:nether_star"}, {"겉날개", "minecraft:elytra"}, {"용의 깃털", "wildplus:dragon_plume"},
                {"불사의 토템", "minecraft:totem_of_undying"}, {"신호기", "minecraft:beacon"}, {"셜커 상자", "minecraft:purple_shulker_box"},
                {"생명의 결정", "wildplus:life_crystal"}, {"스텔라 코어", "wildplus:stellar_core"}, {"행운 상자", "wildplus:lucky_box"},
                {"오라: 불꽃", "minecraft:blaze_powder"}, {"오라: 눈꽃", "minecraft:snowball"}, {"오라: 하트", "minecraft:poppy"},
                {"오라: 별빛", "minecraft:glowstone_dust"},
                {"칭호 떼기", "minecraft:shears"}, {"칭호", "minecraft:name_tag"},
                {"천사의 날개", "minecraft:feather"}, {"악마의 날개", "minecraft:phantom_membrane"}, {"용의 날개", "minecraft:dragon_breath"},
                {"황금 링", "minecraft:gold_ingot"}, {"별빛 링", "minecraft:amethyst_shard"},
                {"오리 탈", "minecraft:chicken_spawn_egg"}, {"토끼 탈", "minecraft:rabbit_spawn_egg"}, {"고양이 탈", "minecraft:cat_spawn_egg"},
                {"곰 탈", "minecraft:polar_bear_spawn_egg"}, {"개구리 탈", "minecraft:frog_spawn_egg"},
                {"황금 왕의 대검", "wildplus:display_greatsword"}, {"별빛 지팡이", "wildplus:display_staff"}, {"공허의 낫", "wildplus:display_scythe"}};
        for (String[] m : map) {
            if (name.contains(m[0])) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(m[1]));
                if (item != null && item != Items.AIR) return new ItemStack(item);
            }
        }
        return new ItemStack(Items.EMERALD);
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
