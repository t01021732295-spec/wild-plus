package com.snolred.wildplus.menu;

import com.snolred.wildplus.content.Mail;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 우편함 창: 위 18칸 받은 우편(꺼내기만), 가운데 9칸 보낼 물건 + 받는 사람 고르기, 아래 내 가방 */
public class MailMenu extends AbstractContainerMenu {
    public static final int INBOX = 18, SEND = 9, BTN_PREV = 0, BTN_NEXT = 1, BTN_SEND = 2;
    public static final int INBOX_Y = 26, SEND_Y = 78, INV_Y = 124;
    public final List<String> names = new ArrayList<>();
    private final List<UUID> ids = new ArrayList<>();
    private final SimpleContainer inbox = new SimpleContainer(INBOX), send = new SimpleContainer(SEND);
    private final DataSlot target = DataSlot.standalone();
    private final BlockPos pos;
    public int waiting;

    public MailMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.MAIL.get(), id);
        this.pos = buf.readBlockPos();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) names.add(buf.readUtf());
        this.waiting = buf.readVarInt();
        build(inv);
    }

    private MailMenu(int id, Inventory inv, BlockPos pos, List<UUID> ids, List<String> names, List<ItemStack> mail) {
        super(ModMenus.MAIL.get(), id);
        this.pos = pos;
        this.ids.addAll(ids);
        this.names.addAll(names);
        for (int i = 0; i < Math.min(INBOX, mail.size()); i++) inbox.setItem(i, mail.get(i));
        build(inv);
    }

    private void build(Inventory inv) {
        for (int i = 0; i < INBOX; i++) {
            addSlot(new Slot(inbox, i, 8 + (i % 9) * 18, INBOX_Y + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return false;
                }
            });
        }
        for (int i = 0; i < SEND; i++) addSlot(new Slot(send, i, 8 + i * 18, SEND_Y));
        addStandardInventorySlots(inv, 8, INV_Y);
        addDataSlot(target);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        List<UUID> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map.Entry<UUID, String> e : WildData.get(server).names.entrySet()) {
            if (e.getKey().equals(p.getUUID())) continue;
            ids.add(e.getKey());
            names.add(e.getValue());
        }
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (!o.getUUID().equals(p.getUUID()) && !ids.contains(o.getUUID())) {
                ids.add(o.getUUID());
                names.add(o.getGameProfile().name());
            }
        }
        List<ItemStack> mail = Mail.take(server, p.getUUID());
        if (mail.size() > INBOX) Mail.put(server, p.getUUID(), new ArrayList<>(mail.subList(INBOX, mail.size())));
        int waiting = Math.max(0, mail.size() - INBOX);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new MailMenu(id, inv, pos, ids, names, mail),
                Component.translatable("block.wildplus.mailbox")), buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(names.size());
            for (String n : names) buf.writeUtf(n);
            buf.writeVarInt(waiting);
        });
    }

    public int target() {
        return target.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        int n = ids.size();
        if (n == 0) return true;
        if (id == BTN_PREV) target.set((target.get() - 1 + n) % n);
        else if (id == BTN_NEXT) target.set((target.get() + 1) % n);
        else if (id == BTN_SEND) {
            UUID to = ids.get(Math.floorMod(target.get(), n));
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < SEND; i++) {
                if (!send.getItem(i).isEmpty()) out.add(send.removeItemNoUpdate(i));
            }
            if (out.isEmpty()) return true;
            List<ItemStack> left = Mail.put(sp.level().getServer(), to, out);
            for (int i = 0; i < left.size() && i < SEND; i++) send.setItem(i, left.get(i));
            int sent = out.size() - left.size();
            if (sent > 0) {
                sp.sendSystemMessage(Component.literal("📮 " + names.get(Math.floorMod(target.get(), n)) + " 님에게 " + sent + "묶음을 보냈어요")
                        .withStyle(ChatFormatting.GREEN));
                sp.level().playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                Mail.notify(sp.level().getServer(), to, sp.getGameProfile().name(), sent);
            }
            if (!left.isEmpty()) sp.sendSystemMessage(Component.literal("받는 사람의 우편함이 가득 찼어요 (남은 물건은 그대로)").withStyle(ChatFormatting.RED));
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int own = INBOX + SEND;
        if (index < own) {
            if (!moveItemStackTo(s, own, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, INBOX, own, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            List<ItemStack> back = new ArrayList<>();
            for (int i = 0; i < INBOX; i++) if (!inbox.getItem(i).isEmpty()) back.add(inbox.removeItemNoUpdate(i));
            List<ItemStack> overflow = Mail.put(sp.level().getServer(), sp.getUUID(), back);
            for (ItemStack s : overflow) if (!sp.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(sp.level(), sp.getX(), sp.getY(), sp.getZ(), s);
            clearContainer(player, send);
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }
}
