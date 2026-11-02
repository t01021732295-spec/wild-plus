package com.snolred.wildplus.menu;

import com.snolred.wildplus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 목록에서 하나를 고르는 화면 (워프 포인트 목적지, 소형 텔레포트기 대상 유저).
 * 목록은 화면을 열 때 서버가 보내고, 고른 번호는 메뉴 버튼으로 돌아온다.
 */
public class PickMenu extends AbstractContainerMenu {
    public static final int BTN_TOGGLE = 900;
    public static final int FLAG_CAN_TOGGLE = 1, FLAG_PUBLIC = 2, FLAG_CAN_RENAME = 4;
    /** 화면 테마: 투기장(콜로세움) · 진열장(유리 케이스 격자) */
    public static final int FLAG_ARENA = 8, FLAG_SHOP = 16;

    /** 한 줄: 이름 · 부가 설명 · (진열장 화면용) 아이템 그림 · 모양(K_…) */
    public record Entry(String title, String sub, ItemStack icon, int kind) {
        public Entry(String title, String sub) {
            this(title, sub, ItemStack.EMPTY, K_ROW);
        }

        public Entry(String title, String sub, ItemStack icon) {
            this(title, sub, icon, K_ROW);
        }

        public static Entry of(int kind, String title, String sub) {
            return new Entry(title, sub, ItemStack.EMPTY, kind);
        }
    }

    /**
     * 투기장 판 화면의 칸 모양.
     * 설정(◀ 값 ▶: ▶ = 번호, ◀ = 번호 + DEC) · 큰 버튼 · 아래 작은 버튼 · 안내 글(못 누름) · A팀/B팀 판(참가 버튼)
     */
    public static final int K_ROW = 0, K_OPTION = 1, K_PRIMARY = 2, K_SMALL = 3, K_INFO = 4, K_TEAM_A = 5, K_TEAM_B = 6, K_DANGER = 7;
    public static final int DEC = 1000;

    public final String header;
    public final String empty;
    public final List<Entry> entries;
    public final int flags;

    private final @Nullable IntConsumer onPick;
    private final @Nullable Runnable onToggle;
    private final @Nullable Predicate<Player> valid;

    /** 클라이언트 */
    public PickMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.PICK.get(), id);
        this.header = buf.readUtf();
        this.empty = buf.readUtf();
        this.flags = buf.readVarInt();
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readUtf(), buf.readUtf(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        this.entries = list;
        this.onPick = null;
        this.onToggle = null;
        this.valid = null;
    }

    private PickMenu(int id, String header, String empty, List<Entry> entries, int flags,
                     IntConsumer onPick, @Nullable Runnable onToggle, Predicate<Player> valid) {
        super(ModMenus.PICK.get(), id);
        this.header = header;
        this.empty = empty;
        this.entries = entries;
        this.flags = flags;
        this.onPick = onPick;
        this.onToggle = onToggle;
        this.valid = valid;
    }

    /** 서버에서 목록 화면을 연다 */
    public static void open(ServerPlayer player, Component title, String header, String empty, List<Entry> entries, int flags,
                            IntConsumer onPick, @Nullable Runnable onToggle, Predicate<Player> valid) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                new PickMenu(id, header, empty, entries, flags, onPick, onToggle, valid), title), buf -> {
            buf.writeUtf(header);
            buf.writeUtf(empty);
            buf.writeVarInt(flags);
            buf.writeVarInt(entries.size());
            for (Entry e : entries) {
                buf.writeUtf(e.title());
                buf.writeUtf(e.sub());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon());
                buf.writeVarInt(e.kind());
            }
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < entries.size() && onPick != null) {
            onPick.accept(id);
            return true;
        }
        if (id >= DEC && id - DEC < entries.size() && onPick != null) { // 설정 ◀
            onPick.accept(id);
            return true;
        }
        if (id == BTN_TOGGLE && onToggle != null) {
            onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return valid == null || valid.test(player);
    }
}
