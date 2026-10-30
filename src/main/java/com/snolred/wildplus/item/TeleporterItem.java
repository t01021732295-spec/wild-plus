package com.snolred.wildplus.item;

import com.snolred.wildplus.economy.Warps;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 소형 텔레포트기 (1회용). 사용하면 접속 중인 유저 목록이 뜨고, 고른 유저에게 순간이동한다.
 * 제작법은 없고 상인·자판기에서만 살 수 있다.
 */
public class TeleporterItem extends Item {
    public TeleporterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            List<ServerPlayer> others = new ArrayList<>();
            for (ServerPlayer p : sp.level().getServer().getPlayerList().getPlayers()) {
                if (p != sp && !p.isSpectator()) others.add(p);
            }
            List<PickMenu.Entry> entries = new ArrayList<>();
            List<UUID> ids = new ArrayList<>();
            for (ServerPlayer p : others) {
                entries.add(new PickMenu.Entry(p.getName().getString(),
                        p.level().dimension().identifier().getPath() + " · " + p.blockPosition().getX() + ", "
                                + p.blockPosition().getY() + ", " + p.blockPosition().getZ()));
                ids.add(p.getUUID());
            }
            PickMenu.open(sp, Component.translatable("item.wildplus.teleporter"), "누구에게 갈까요? (1회용)",
                    "지금 접속한 다른 유저가 없어요.", entries, 0,
                    i -> {
                        sp.closeContainer();
                        ServerPlayer target = sp.level().getServer().getPlayerList().getPlayer(ids.get(i));
                        ItemStack held = sp.getItemInHand(hand);
                        if (target == null) {
                            sp.sendSystemMessage(Component.translatable("message.wildplus.teleporter.offline").withStyle(ChatFormatting.RED));
                            return;
                        }
                        if (!(held.getItem() instanceof TeleporterItem)) {
                            return;
                        }
                        if (!sp.getAbilities().instabuild) held.shrink(1);
                        Warps.jump(sp, target.level(), target.getX(), target.getY(), target.getZ());
                        sp.sendOverlayMessage(Component.translatable("message.wildplus.teleporter.done", target.getName())
                                .withStyle(ChatFormatting.AQUA));
                        target.sendSystemMessage(Component.translatable("message.wildplus.teleporter.arrived", sp.getName())
                                .withStyle(ChatFormatting.AQUA));
                    },
                    null,
                    p -> p.getItemInHand(hand).getItem() instanceof TeleporterItem);
        }
        return InteractionResult.SUCCESS;
    }
}
