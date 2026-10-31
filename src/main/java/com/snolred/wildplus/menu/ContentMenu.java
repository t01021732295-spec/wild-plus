package com.snolred.wildplus.menu;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.Raid;
import com.snolred.wildplus.content.Tower;
import com.snolred.wildplus.content.WeeklyGoals;
import com.snolred.wildplus.registry.ModMenus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 주말 콘텐츠 창 (레이드 제단 · 탑 입구 · 목표 게시판). 열 때 서버가 정보 묶음(NBT)을 한 번 보내고,
 * 버튼을 누르면 서버가 처리한 뒤 창을 새 정보로 다시 연다.
 */
public class ContentMenu extends AbstractContainerMenu {
    public static final int RAID = 0, TOWER = 1, GOALS = 2;
    public static final int BTN_START = 0, BTN_GOAL = 1, BTN_TIER = 10;

    public final int mode;
    public final CompoundTag info;
    public final BlockPos pos;

    public ContentMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.CONTENT.get(), id);
        this.mode = buf.readVarInt();
        this.pos = buf.readBlockPos();
        CompoundTag t = buf.readNbt();
        this.info = t == null ? new CompoundTag() : t;
    }

    private ContentMenu(int id, int mode, BlockPos pos, CompoundTag info) {
        super(ModMenus.CONTENT.get(), id);
        this.mode = mode;
        this.pos = pos;
        this.info = info;
    }

    public static void openRaid(ServerPlayer p, BlockPos pos) {
        open(p, pos, RAID);
    }

    public static void openTower(ServerPlayer p, BlockPos pos) {
        open(p, pos, TOWER);
    }

    public static void openGoals(ServerPlayer p, BlockPos pos) {
        open(p, pos, GOALS);
    }

    public static void open(ServerPlayer p, BlockPos pos, int mode) {
        CompoundTag info = snapshot(p, pos, mode);
        String title = switch (mode) {
            case RAID -> "container.wildplus.raid_altar";
            case TOWER -> "container.wildplus.tower_gate";
            default -> "container.wildplus.goal_board";
        };
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContentMenu(id, mode, pos, info), Component.translatable(title)), buf -> {
            buf.writeVarInt(mode);
            buf.writeBlockPos(pos);
            buf.writeNbt(info);
        });
    }

    /** 다음 금요일까지 남은 날 */
    public static int daysToReset() {
        LocalDate d = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int n = (DayOfWeek.FRIDAY.getValue() - d.getDayOfWeek().getValue() + 7) % 7;
        return n == 0 ? 7 : n;
    }

    private static CompoundTag snapshot(ServerPlayer p, BlockPos pos, int mode) {
        MinecraftServer server = p.level().getServer();
        ContentData d = ContentData.get(server);
        CompoundTag t = new CompoundTag();
        t.putInt("reset", daysToReset());
        switch (mode) {
            case RAID -> {
                Raid.Boss b = Raid.bossOfWeek();
                CompoundTag st = Raid.state(server);
                int tier = Raid.tier(server);
                int n = Math.max(1, Raid.nearby((ServerLevel) p.level(), pos, 24).size());
                t.putInt("boss", b.ordinal());
                t.putInt("tier", tier);
                t.putInt("party", n);
                t.putLong("hp", Raid.hpFor(b, tier, n));
                t.putInt("damage", Math.round(b.damage * (1.0F + 0.15F * (tier - 1))));
                t.putBoolean("cleared", d.player("raid_p", p.getUUID()).getLongOr("week", Long.MIN_VALUE) == ContentData.week());
                t.putInt("clears", st.getIntOr("clears", 0));
                t.putInt("myClears", d.player("raid_p", p.getUUID()).getIntOr("clears", 0));
                t.putString("mvp", st.getStringOr("mvp", "-"));
                t.putString("last", st.getStringOr("last", "-"));
                t.putLong("best", st.getLongOr("best_" + b.name(), -1L));
                t.putString("status", Raid.status());
            }
            case TOWER -> {
                CompoundTag st = Tower.state(server);
                t.putInt("best", st.getIntOr("best", 0));
                t.putString("bestNames", st.getStringOr("best_names", "-"));
                t.putInt("myBest", d.player("tower_p", p.getUUID()).getIntOr("best", 0));
                t.put("runs", st.getListOrEmpty("runs").copy());
                t.putInt("party", Math.max(1, Raid.nearby((ServerLevel) p.level(), pos, 8).size()));
                t.putString("status", Tower.status());
            }
            default -> {
                CompoundTag g = WeeklyGoals.goals(server);
                ListTag goals = new ListTag();
                int claimed = WeeklyGoals.claimedGoals(server, p);
                for (int i = 0; i < 4; i++) {
                    WeeklyGoals.Kind k = WeeklyGoals.kind(g, i);
                    CompoundTag e = new CompoundTag();
                    e.putString("kind", k.name());
                    e.putString("label", k.label);
                    e.putInt("progress", g.getIntOr("p" + i, 0));
                    e.putInt("target", k.target);
                    e.putInt("reward", k.reward);
                    e.putBoolean("claimed", (claimed & (1 << i)) != 0);
                    goals.add(e);
                }
                t.put("goals", goals);
                CompoundTag s = WeeklyGoals.season(server, p.getUUID());
                t.putInt("points", s.getIntOr("points", 0));
                t.putInt("tierClaimed", s.getIntOr("claimed", 0));
                // 시즌 번호는 이 월드에서 처음 연 시즌을 1로
                CompoundTag meta = d.section("meta");
                if (meta.getLongOr("season0", Long.MIN_VALUE) == Long.MIN_VALUE) {
                    meta.putLong("season0", ContentData.season());
                    d.dirty();
                }
                t.putLong("season", ContentData.season() - meta.getLongOr("season0", ContentData.season()) + 1);
                t.putInt("weeksLeft", ContentData.weeksLeftInSeason());
            }
        }
        return t;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        MinecraftServer server = sp.level().getServer();
        boolean reopen = true;
        switch (mode) {
            case RAID -> {
                if (id == BTN_START) {
                    Raid.start(sp, pos);
                    reopen = false;
                }
            }
            case TOWER -> {
                if (id == BTN_START) {
                    Tower.start(sp, pos);
                    reopen = false;
                }
            }
            default -> {
                if (id >= BTN_GOAL && id < BTN_GOAL + 4) WeeklyGoals.claimGoal(sp, id - BTN_GOAL);
                else if (id >= BTN_TIER && id < BTN_TIER + WeeklyGoals.TIERS) WeeklyGoals.claimTier(sp, id - BTN_TIER);
            }
        }
        if (reopen) server.execute(() -> open(sp, pos, mode));
        else server.execute(sp::closeContainer);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 100.0;
    }
}
