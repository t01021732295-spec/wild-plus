package com.snolred.wildplus.content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 우편: 받은 물건은 서버에 저장되고 어느 우편함에서나 꺼낼 수 있다 (한 사람당 최대 54묶음) */
public final class Mail {
    public static final int MAX = 54;

    private Mail() {}

    private static ListTag list(MinecraftServer server, UUID id) {
        CompoundTag me = ContentData.get(server).player("mail_p", id);
        ListTag l = me.getListOrEmpty("items");
        me.put("items", l);
        return l;
    }

    public static int count(MinecraftServer server, UUID id) {
        return list(server, id).size();
    }

    /** 받은 우편 전부 꺼내기 (저장소는 비움) */
    public static List<ItemStack> take(MinecraftServer server, UUID id) {
        ListTag l = list(server, id);
        var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> out = new ArrayList<>();
        for (Tag t : l) ItemStack.OPTIONAL_CODEC.parse(ops, t).result().ifPresent(s -> { if (!s.isEmpty()) out.add(s); });
        l.clear();
        ContentData.get(server).dirty();
        return out;
    }

    /** 우편함에 넣기. 다 못 넣으면 남은 것을 돌려준다 */
    public static List<ItemStack> put(MinecraftServer server, UUID id, List<ItemStack> stacks) {
        ListTag l = list(server, id);
        var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            if (l.size() >= MAX) {
                left.add(s);
                continue;
            }
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, s).result().ifPresent(l::add);
        }
        ContentData.get(server).dirty();
        return left;
    }

    public static void notify(MinecraftServer server, UUID to, String from, int n) {
        ServerPlayer p = server.getPlayerList().getPlayer(to);
        if (p != null) {
            p.sendSystemMessage(Component.literal("📬 " + from + " 님이 우편을 보냈어요 (" + n + "묶음) · 아무 우편함에서나 받으세요")
                    .withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            int n = count(p.level().getServer(), p.getUUID());
            if (n > 0) p.sendSystemMessage(Component.literal("📬 받은 우편이 " + n + "묶음 있어요").withStyle(ChatFormatting.GOLD));
        }
    }
}
