package com.snolred.wildplus.content;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.snolred.wildplus.registry.ModItems3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 친구 기능.
 * - 파티: /party invite <이름> · accept · leave · list. 파티원끼리는 서로 때려도 피해 없음,
 *   파티원이 몬스터를 잡으면 48칸 안의 다른 파티원도 경험치 + 가끔 동화, 화면 위에 파티원 체력·거리·방향 막대.
 * - 집: /sethome 으로 집 위치 저장, /home 으로 이동 (1분에 한 번).
 */
public final class Social {
    private static final Map<UUID, UUID> INVITES = new HashMap<>();
    private static final Map<UUID, Map<UUID, ServerBossEvent>> BARS = new HashMap<>();
    private static final Map<UUID, Long> HOME_COOLDOWN = new HashMap<>();

    private Social() {}

    // ───────── 파티 저장 (리더 UUID 를 파티 id 로) ─────────
    private static CompoundTag parties(MinecraftServer server) {
        return ContentData.get(server).section("party");
    }

    public static UUID partyOf(MinecraftServer server, UUID p) {
        String s = parties(server).getStringOr(p.toString(), "");
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public static List<UUID> members(MinecraftServer server, UUID party) {
        List<UUID> out = new ArrayList<>();
        if (party == null) return out;
        CompoundTag t = parties(server);
        for (String k : t.keySet()) if (t.getStringOr(k, "").equals(party.toString())) out.add(UUID.fromString(k));
        return out;
    }

    public static boolean sameParty(MinecraftServer server, UUID a, UUID b) {
        UUID pa = partyOf(server, a);
        return pa != null && pa.equals(partyOf(server, b));
    }

    private static void join(MinecraftServer server, UUID p, UUID party) {
        parties(server).putString(p.toString(), party.toString());
        ContentData.get(server).dirty();
    }

    private static void leave(MinecraftServer server, UUID p) {
        UUID party = partyOf(server, p);
        parties(server).remove(p.toString());
        // 혼자 남으면 파티 해체
        if (party != null) {
            List<UUID> rest = members(server, party);
            if (rest.size() == 1) parties(server).remove(rest.get(0).toString());
        }
        ContentData.get(server).dirty();
        clearBars(p);
    }

    private static String name(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) return p.getGameProfile().name();
        return com.snolred.wildplus.economy.WildData.get(server).names.getOrDefault(id, "?");
    }

    // ───────── 명령어 ─────────
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("party")
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer me = ctx.getSource().getPlayerOrException();
                    ServerPlayer who = EntityArgument.getPlayer(ctx, "player");
                    if (who == me) return 0;
                    INVITES.put(who.getUUID(), me.getUUID());
                    me.sendSystemMessage(Component.literal("🤝 " + who.getGameProfile().name() + " 님에게 파티 초대를 보냈어요").withStyle(ChatFormatting.AQUA));
                    who.sendSystemMessage(Component.literal("🤝 " + me.getGameProfile().name() + " 님이 파티에 초대했어요 · /party accept 로 수락")
                            .withStyle(ChatFormatting.AQUA));
                    return 1;
                })))
                .then(Commands.literal("accept").executes(ctx -> {
                    ServerPlayer me = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = me.level().getServer();
                    UUID from = INVITES.remove(me.getUUID());
                    if (from == null) {
                        me.sendSystemMessage(Component.literal("받은 파티 초대가 없어요").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    if (partyOf(server, me.getUUID()) != null) leave(server, me.getUUID());
                    UUID party = partyOf(server, from);
                    if (party == null) {
                        party = from;
                        join(server, from, party);
                    }
                    join(server, me.getUUID(), party);
                    for (UUID m : members(server, party)) {
                        ServerPlayer p = server.getPlayerList().getPlayer(m);
                        if (p != null) p.sendSystemMessage(Component.literal("🤝 " + me.getGameProfile().name() + " 님이 파티에 들어왔어요")
                                .withStyle(ChatFormatting.GREEN));
                    }
                    return 1;
                }))
                .then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer me = ctx.getSource().getPlayerOrException();
                    leave(me.level().getServer(), me.getUUID());
                    me.sendSystemMessage(Component.literal("파티에서 나왔어요").withStyle(ChatFormatting.GRAY));
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer me = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = me.level().getServer();
                    UUID party = partyOf(server, me.getUUID());
                    if (party == null) {
                        me.sendSystemMessage(Component.literal("파티가 없어요 · /party invite <이름>").withStyle(ChatFormatting.GRAY));
                        return 1;
                    }
                    List<String> names = new ArrayList<>();
                    for (UUID m : members(server, party)) names.add(name(server, m));
                    me.sendSystemMessage(Component.literal("🤝 파티: " + String.join(", ", names)).withStyle(ChatFormatting.AQUA));
                    return 1;
                })));
        event.getDispatcher().register(Commands.literal("sethome").executes(ctx -> {
            ServerPlayer me = ctx.getSource().getPlayerOrException();
            CompoundTag h = ContentData.get(me.level().getServer()).player("home_p", me.getUUID());
            h.putLong("pos", me.blockPosition().asLong());
            h.putString("dim", me.level().dimension().identifier().toString());
            ContentData.get(me.level().getServer()).dirty();
            me.sendSystemMessage(Component.literal("🏠 여기를 집으로 정했어요 · /home 으로 돌아올 수 있어요").withStyle(ChatFormatting.GREEN));
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("home").executes(ctx -> {
            ServerPlayer me = ctx.getSource().getPlayerOrException();
            MinecraftServer server = me.level().getServer();
            CompoundTag h = ContentData.get(server).player("home_p", me.getUUID());
            if (h.getLongOr("pos", Long.MIN_VALUE) == Long.MIN_VALUE) {
                me.sendSystemMessage(Component.literal("아직 집이 없어요 · /sethome").withStyle(ChatFormatting.RED));
                return 0;
            }
            long now = server.overworld().getGameTime();
            long ready = HOME_COOLDOWN.getOrDefault(me.getUUID(), 0L);
            if (now < ready) {
                me.sendSystemMessage(Component.literal("집 이동은 " + (ready - now) / 20 + "초 뒤에 다시 쓸 수 있어요").withStyle(ChatFormatting.RED));
                return 0;
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(h.getStringOr("dim", "minecraft:overworld")));
            ServerLevel lv = server.getLevel(dim);
            if (lv == null) lv = server.overworld();
            BlockPos pos = BlockPos.of(h.getLongOr("pos", 0L));
            HOME_COOLDOWN.put(me.getUUID(), now + 1200);
            me.teleportTo(lv, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), me.getYRot(), me.getXRot(), true);
            lv.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
            me.sendOverlayMessage(Component.literal("🏠 집에 왔어요").withStyle(ChatFormatting.GREEN));
            return 1;
        }));
    }

    // ───────── 파티 효과 ─────────
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target && event.getSource().getEntity() instanceof ServerPlayer attacker
                && target != attacker && sameParty(target.level().getServer(), target.getUUID(), attacker.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster m) || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        MinecraftServer server = killer.level().getServer();
        UUID party = partyOf(server, killer.getUUID());
        if (party == null) return;
        for (UUID id : members(server, party)) {
            if (id.equals(killer.getUUID())) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || p.level() != killer.level() || p.distanceToSqr(m) > 48 * 48) continue;
            p.giveExperiencePoints(3 + (int) (m.getMaxHealth() / 10));
            if (p.getRandom().nextFloat() < 0.25F) {
                ItemStack coin = new ItemStack(ModItems3.COPPER_COIN.get());
                if (!p.getInventory().add(coin)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), coin);
            }
        }
    }

    /** 화면 위 파티원 막대: "철수 ♥18 · 23m ↗" */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            UUID party = partyOf(server, viewer.getUUID());
            Map<UUID, ServerBossEvent> mine = BARS.computeIfAbsent(viewer.getUUID(), k -> new HashMap<>());
            Set<UUID> keep = new LinkedHashSet<>();
            if (party != null) {
                for (UUID id : members(server, party)) {
                    if (id.equals(viewer.getUUID())) continue;
                    keep.add(id);
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    ServerBossEvent bar = mine.computeIfAbsent(id, k -> {
                        ServerBossEvent b = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.GREEN,
                                BossEvent.BossBarOverlay.NOTCHED_10);
                        b.addPlayer(viewer);
                        return b;
                    });
                    if (p == null) {
                        bar.setName(Component.literal("🤝 " + name(server, id) + " · 접속 안 함").withStyle(ChatFormatting.GRAY));
                        bar.setProgress(0F);
                        bar.setColor(BossEvent.BossBarColor.WHITE);
                        continue;
                    }
                    float hp = p.getHealth() / Math.max(1F, p.getMaxHealth());
                    String where;
                    if (p.level() != viewer.level()) {
                        where = "다른 차원";
                    } else {
                        double dx = p.getX() - viewer.getX(), dz = p.getZ() - viewer.getZ();
                        int dist = (int) Math.sqrt(dx * dx + dz * dz);
                        float ang = (float) Math.toDegrees(Math.atan2(-dx, dz)) - viewer.getYRot();
                        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
                        int idx = Math.floorMod(Math.round(ang / 45F), 8);
                        where = dist + "m " + arrows[idx];
                    }
                    bar.setName(Component.literal(String.format("🤝 %s  ♥%d  · %s", p.getGameProfile().name(), Math.round(p.getHealth()), where))
                            .withStyle(hp < 0.3F ? ChatFormatting.RED : ChatFormatting.GREEN));
                    bar.setProgress(Mth.clamp(hp, 0F, 1F));
                    bar.setColor(hp < 0.3F ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN);
                }
            }
            mine.entrySet().removeIf(e -> {
                if (keep.contains(e.getKey())) return false;
                e.getValue().removeAllPlayers();
                return true;
            });
        }
    }

    private static void clearBars(UUID viewer) {
        Map<UUID, ServerBossEvent> mine = BARS.remove(viewer);
        if (mine != null) mine.values().forEach(ServerBossEvent::removeAllPlayers);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearBars(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        BARS.values().forEach(m -> m.values().forEach(ServerBossEvent::removeAllPlayers));
        BARS.clear();
        INVITES.clear();
        HOME_COOLDOWN.clear();
    }
}
