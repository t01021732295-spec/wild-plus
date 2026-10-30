package com.snolred.wildplus.economy;

import com.snolred.wildplus.block.WarpPointBlockEntity;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** 워프 포인트끼리의 순간이동 (횟수·비용 제한 없음) */
public final class Warps {
    private Warps() {}

    /** 블록에 워프가 아직 등록되지 않았으면(명령어·구조물로 놓인 경우) 공용 워프로 등록 */
    public static WildData.Warp ensure(ServerLevel level, BlockPos pos, WarpPointBlockEntity be) {
        WildData data = WildData.get(level.getServer());
        WildData.Warp w = be.warpId() == null ? null : data.warps.get(be.warpId());
        if (w == null) {
            w = new WildData.Warp(UUID.randomUUID(), "워프 " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                    new UUID(0L, 0L), "", level.dimension().identifier().toString(), pos.immutable(), true);
            data.warps.put(w.id, w);
            data.setDirty();
            be.setWarpId(w.id);
        }
        return w;
    }

    public static WildData.Warp register(ServerPlayer owner, ServerLevel level, BlockPos pos, @Nullable String customName) {
        WildData data = WildData.get(level.getServer());
        String name = customName;
        if (name == null || name.isBlank()) {
            long mine = data.warps.values().stream().filter(x -> x.owner.equals(owner.getUUID())).count();
            name = owner.getName().getString() + "의 워프 " + (mine + 1);
        }
        WildData.Warp w = new WildData.Warp(UUID.randomUUID(), name, owner.getUUID(), owner.getName().getString(),
                level.dimension().identifier().toString(), pos.immutable(), true);
        data.warps.put(w.id, w);
        data.setDirty();
        return w;
    }

    public static void remove(ServerLevel level, @Nullable UUID id) {
        if (id != null) {
            WildData data = WildData.get(level.getServer());
            if (data.warps.remove(id) != null) data.setDirty();
        }
    }

    /** 플레이어가 마지막으로 연 (자기) 워프 — /warpname 이 이 워프 이름을 바꾼다 */
    public static final java.util.Map<UUID, UUID> LAST_OPENED = new java.util.HashMap<>();

    public static boolean rename(ServerPlayer player, String name) {
        UUID id = LAST_OPENED.get(player.getUUID());
        WildData data = WildData.get(player.level().getServer());
        WildData.Warp w = id == null ? null : data.warps.get(id);
        if (w == null || !w.owner.equals(player.getUUID())) return false;
        w.name = name.length() > 32 ? name.substring(0, 32) : name;
        data.setDirty();
        return true;
    }

    /** 워프 포인트를 우클릭했을 때: 갈 수 있는 워프 목록 화면 */
    public static void openMenu(ServerPlayer player, WildData.Warp here) {
        WildData data = WildData.get(player.level().getServer());
        List<WildData.Warp> targets = new ArrayList<>();
        for (WildData.Warp w : data.sortedWarps()) {
            if (!w.id.equals(here.id) && (w.isPublic || w.owner.equals(player.getUUID()))) {
                targets.add(w);
            }
        }
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (WildData.Warp w : targets) {
            String who = w.ownerName.isEmpty() ? "공용" : w.ownerName;
            entries.add(new PickMenu.Entry((w.isPublic ? "" : "[비공개] ") + w.name,
                    who + " · " + dimName(w.dimension) + " " + w.pos.getX() + ", " + w.pos.getY() + ", " + w.pos.getZ()));
        }
        boolean owner = here.owner.equals(player.getUUID());
        if (owner) LAST_OPENED.put(player.getUUID(), here.id);
        int flags = (owner ? PickMenu.FLAG_CAN_TOGGLE : 0) | (here.isPublic ? PickMenu.FLAG_PUBLIC : 0);
        BlockPos herePos = here.pos;
        PickMenu.open(player, Component.translatable("container.wildplus.warp", here.name),
                "현재 위치: " + here.name + (here.isPublic ? " (공개)" : " (비공개)"),
                "갈 수 있는 다른 워프 포인트가 없어요. 다른 곳에 워프 포인트를 하나 더 설치해 보세요.",
                entries, flags,
                i -> {
                    player.closeContainer();
                    teleport(player, targets.get(i));
                },
                owner ? () -> {
                    here.isPublic = !here.isPublic;
                    data.setDirty();
                    player.closeContainer();
                    openMenu(player, here);
                } : null,
                p -> p.distanceToSqr(herePos.getX() + 0.5, herePos.getY() + 0.5, herePos.getZ() + 0.5) < 64.0);
    }

    static String dimName(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "오버월드";
            case "minecraft:the_nether" -> "네더";
            case "minecraft:the_end" -> "엔드";
            default -> dim;
        };
    }

    public static void teleport(ServerPlayer player, WildData.Warp target) {
        ServerLevel level = player.level().getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, Identifier.parse(target.dimension)));
        if (level == null || !(level.getBlockEntity(target.pos) instanceof WarpPointBlockEntity be)
                || !target.id.equals(be.warpId())) {
            // 워프 포인트가 없어졌다
            if (level == null || level.isLoaded(target.pos)) {
                WildData data = WildData.get(player.level().getServer());
                data.warps.remove(target.id);
                data.setDirty();
                player.sendSystemMessage(Component.translatable("message.wildplus.warp.gone", target.name).withStyle(ChatFormatting.RED));
                return;
            }
        }
        jump(player, level, target.pos.getX() + 0.5, target.pos.getY() + 1.0, target.pos.getZ() + 0.5);
        player.sendOverlayMessage(Component.translatable("message.wildplus.warp.arrived", target.name).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** 효과음·입자와 함께 순간이동 (다른 차원도 가능) */
    public static void jump(ServerPlayer player, ServerLevel to, double x, double y, double z) {
        ServerLevel from = player.level();
        from.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        from.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        player.teleportTo(to, x, y, z, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        to.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        to.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 40, 0.4, 0.8, 0.4, 0.05);
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }
}
