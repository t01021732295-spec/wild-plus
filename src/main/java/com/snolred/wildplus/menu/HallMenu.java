package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 명예의 전당 창: 내 등급 · 혜택 · 구매 버튼 · 부자 순위 (순위는 열 때 한 번 받아 온다) */
public class HallMenu extends AbstractContainerMenu {
    public final List<Honor.Entry> ranking = new ArrayList<>();
    private final ContainerData data;
    private final @Nullable BlockPos pos;

    public HallMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.HALL.get(), id);
        this.pos = null;
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) ranking.add(new Honor.Entry(buf.readUtf(), buf.readVarLong(), buf.readVarInt()));
        this.data = new SimpleContainerData(8);
        addDataSlots(data);
    }

    private HallMenu(int id, ServerPlayer player, BlockPos pos) {
        super(ModMenus.HALL.get(), id);
        this.pos = pos;
        MinecraftServer server = player.level().getServer();
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                WildData d = WildData.get(server);
                long now = Honor.now(server);
                long wealth = Honor.wealth(server, player);
                return switch (i) {
                    case 0 -> d.honor(player.getUUID());
                    case 1 -> (int) (wealth % 10000);
                    case 2 -> (int) Math.min(32767, wealth / 10000);
                    case 3 -> (int) Math.min(32767, Math.max(0, d.flightUntil.getOrDefault(player.getUUID(), 0L) - now) / 20 / 60);
                    case 4 -> (int) Math.min(32767, Math.max(0, d.festivalUntil - now) / 20 / 60);
                    case 5 -> (int) Math.min(32767, d.treasury / 10000);
                    case 6 -> (int) (d.treasury % 10000);
                    default -> d.flightUntil.containsKey(player.getUUID()) && d.flightUntil.get(player.getUUID()) > now ? 1 : 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 8;
            }
        };
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        List<Honor.Entry> top = Honor.ranking(player.level().getServer(), 5);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new HallMenu(id, player, pos),
                Component.translatable("container.wildplus.hall_of_fame")), buf -> {
            buf.writeVarInt(top.size());
            for (Honor.Entry e : top) {
                buf.writeUtf(e.name());
                buf.writeVarLong(e.wealth());
                buf.writeVarInt(e.rank());
            }
        });
    }

    public int rank() { return data.get(0); }
    public long wealth() { return data.get(1) + data.get(2) * 10000L; }
    public int flightMinutes() { return data.get(3); }
    public int festivalMinutes() { return data.get(4); }
    public long treasury() { return data.get(5) * 10000L + data.get(6); }
    public boolean flying() { return data.get(7) == 1; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer sp) {
            Honor.press(sp, id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return pos == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }
}
