package com.snolred.wildplus.block;

import com.mojang.serialization.Codec;
import com.snolred.wildplus.cooking.Cooking;
import com.snolred.wildplus.cooking.CookingData;
import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.economy.Jobs;
import com.snolred.wildplus.economy.ShipPrices;
import com.snolred.wildplus.economy.Tax;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.entity.CustomerEntity;
import com.snolred.wildplus.registry.ModBlockEntities;
import com.snolred.wildplus.registry.ModBlocks5;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems14;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 식당 카운터: 진열대 10칸 · 손님 주문 티켓 4장 · 평판(별) · 평점 · 알바 · 직원.
 * 손님은 근처(16칸 안) 식당 입구 블록에서 나타나 카운터 앞에 줄을 서고, 주인 · 직원 · 알바가 [내주기] 하면
 * 진열대 요리를 받아 빈 의자에 앉아 먹고 떠난다. 근처(32칸)에 플레이어가 있을 때만 영업한다 (오프라인 수익 없음).
 */
public class RestaurantCounterBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SHELF = 10, TICKETS = 4, DATA = 48;
    public static final int ROLE_GUEST = 0, ROLE_ALBA = 1, ROLE_STAFF = 2, ROLE_OWNER = 3;
    private static final int[] ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    /** 별 (1~5) 기준 평판 점수 */
    public static final int[] STAR_XP = {0, 0, 40, 150, 400, 900};
    /** 별마다 손님 간격 (초) */
    private static final int[] INTERVAL = {0, 60, 48, 38, 30, 22};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SHELF, ItemStack.EMPTY);
    public String name = "";
    public @Nullable UUID owner;
    public String ownerName = "";
    public boolean village, open = true, hiring;
    public int albaFee = 30, staffPct = 20, xp;
    public final List<UUID> staff = new ArrayList<>();
    public final List<String> staffNames = new ArrayList<>();
    public final List<UUID> applicants = new ArrayList<>();
    public final List<String> applicantNames = new ArrayList<>();
    public final Map<UUID, Long> albaDay = new HashMap<>();
    public long ratingSum;
    public int ratingCount;
    /** 최근 후기 (최신 먼저): "별\t이름\t글" */
    public final List<String> reviews = new ArrayList<>();
    public final Map<UUID, Long> ratedDay = new HashMap<>();
    public long statDay, salesToday, tipsToday;
    public int servedToday;
    public final List<UUID> tickets = new ArrayList<>();
    private final Map<UUID, Integer> missing = new HashMap<>();
    private int cooldown = 8;
    private @Nullable BlockPos entrance;

    public RestaurantCounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESTAURANT.get(), pos, state);
    }

    public String displayName() {
        if (!name.isBlank()) return name;
        if (village) return "마을 식당";
        return ownerName.isBlank() ? "이름 없는 식당" : ownerName + "의 식당";
    }

    public static int stars(int xp) {
        int s = 1;
        for (int i = 1; i < STAR_XP.length; i++) if (xp >= STAR_XP[i]) s = i;
        return s;
    }

    public int stars() {
        return stars(xp);
    }

    public long day() {
        return level == null ? 0 : level.getGameTime() / 24000L;
    }

    public int role(Player p) {
        UUID id = p.getUUID();
        if (!village && id.equals(owner)) return ROLE_OWNER;
        if (staff.contains(id)) return ROLE_STAFF;
        Long d = albaDay.get(id);
        if (d != null && d == day()) return ROLE_ALBA;
        return ROLE_GUEST;
    }

    // ───────── 메뉴 가격 ─────────
    /** 식당 메뉴 값 (동): 출하가 × 2 + 조리 수고비 (기구마다) — 별이 높을수록 +10%/별 */
    public int menuPrice(ItemStack dish) {
        return price(dish, stars());
    }

    public static int price(ItemStack dish, int stars) {
        CookingData.Dish d = Cooking.dishOf(dish);
        int labor = d == null ? 20 : switch (d.station()) {
            case PREP -> 30;
            case STOVE -> 40;
            case GRILL, POT -> 50;
            case OVEN -> 60;
            case AGING -> 80;
        };
        long base = ShipPrices.centi(dish) * 2L / 100 + (long) labor * Grade.priceX100(dish) / 100;
        return (int) Math.max(5, base * (100 + 10 * (stars - 1)) / 100);
    }

    // ───────── 진행 ─────────
    public static void serverTick(Level level, BlockPos pos, BlockState state, RestaurantCounterBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || level.getGameTime() % 20 != 3) return;
        if (be.statDay != be.day()) {
            be.statDay = be.day();
            be.salesToday = be.tipsToday = 0;
            be.servedToday = 0;
            be.setChanged();
        }
        // 사라진 손님 티켓 정리 (5초 넘게 안 보이면)
        be.tickets.removeIf(id -> {
            Entity e = sl.getEntity(id);
            if (e instanceof CustomerEntity c && c.isAlive() && c.waiting()) {
                be.missing.remove(id);
                return false;
            }
            int n = be.missing.merge(id, 1, Integer::sum);
            if (n > 5 || e != null) {
                be.missing.remove(id);
                return true;
            }
            return false;
        });
        if (!be.open || be.tickets.size() >= TICKETS) return;
        if (sl.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 32, false) == null) return;
        if (--be.cooldown > 0) return;
        be.cooldown = INTERVAL[be.stars()] + level.getRandom().nextInt(INTERVAL[be.stars()] / 2 + 1);
        be.spawnCustomer(sl);
    }

    /** 근처 16칸 안의 식당 입구 */
    public @Nullable BlockPos entrance(Level level) {
        if (entrance != null && level.getBlockState(entrance).is(ModBlocks5.RESTAURANT_ENTRANCE.get())) return entrance;
        entrance = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-16, -4, -16), worldPosition.offset(16, 4, 16))) {
            if (level.getBlockState(p).is(ModBlocks5.RESTAURANT_ENTRANCE.get())) {
                double d = p.distSqr(worldPosition);
                if (d < best) {
                    best = d;
                    entrance = p.immutable();
                }
            }
        }
        return entrance;
    }

    /** 진열대에 있는 요리 종류 */
    private List<ItemStack> menu() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : items) {
            if (s.isEmpty() || Cooking.dishOf(s) == null) continue;
            boolean dup = false;
            for (ItemStack o : out) if (o.is(s.getItem())) dup = true;
            if (!dup) out.add(s.copyWithCount(1));
        }
        return out;
    }

    private void spawnCustomer(ServerLevel level) {
        BlockPos door = entrance(level);
        List<ItemStack> menu = menu();
        if (door == null || menu.isEmpty()) return;
        RandomSource r = level.getRandom();
        int roll = r.nextInt(100);
        CustomerEntity.Kind kind = roll < 50 ? CustomerEntity.Kind.VILLAGER : roll < 75 ? CustomerEntity.Kind.TRAVELER
                : roll < 92 || stars() < 3 ? CustomerEntity.Kind.ADVENTURER : CustomerEntity.Kind.NOBLE;
        int n = kind.minOrder + r.nextInt(kind.maxOrder - kind.minOrder + 1);
        List<ItemStack> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(menu.get(r.nextInt(menu.size())).copyWithCount(1));
        CustomerEntity c = ModEntities.CUSTOMER.get().create(level, EntitySpawnReason.EVENT);
        if (c == null) return;
        c.snapTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, r.nextFloat() * 360, 0);
        c.setup(kind, worldPosition, door, order, displayName());
        level.addFreshEntity(c);
        tickets.add(c.getUUID());
        level.playSound(null, door, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
        setChanged();
    }

    /** 줄 선 자리 (카운터 앞 2칸, 티켓 순서대로 옆으로) */
    public BlockPos queueSpot(UUID id) {
        int i = Math.max(0, tickets.indexOf(id));
        Direction f = getBlockState().getValue(RestaurantCounterBlock.FACING);
        int[] lat = {0, -1, 1, 0};
        int dep = i == 3 ? 3 : 2;
        return worldPosition.relative(f, dep).relative(f.getClockWise(), lat[i]);
    }

    public @Nullable CustomerEntity ticket(int i) {
        if (i < 0 || i >= tickets.size() || !(level instanceof ServerLevel sl)) return null;
        return sl.getEntity(tickets.get(i)) instanceof CustomerEntity c ? c : null;
    }

    /** 진열대에 이 주문이 다 있나 (마을 식당은 무한) */
    public boolean hasStock(List<ItemStack> order) {
        if (village) return true;
        int[] need = new int[SHELF];
        for (ItemStack o : order) {
            boolean ok = false;
            for (int i = 0; i < SHELF && !ok; i++) {
                ItemStack s = items.get(i);
                if (s.is(o.getItem()) && s.getCount() - need[i] > 0) {
                    need[i]++;
                    ok = true;
                }
            }
            if (!ok) return false;
        }
        return true;
    }

    /** 주문을 진열대에서 꺼냄 (등급 높은 것부터) → 쓴 요리들 */
    private List<ItemStack> takeOrder(List<ItemStack> order) {
        List<ItemStack> used = new ArrayList<>();
        for (ItemStack o : order) {
            if (village) {
                used.add(o.copy());
                continue;
            }
            int best = -1;
            for (int i = 0; i < SHELF; i++) {
                ItemStack s = items.get(i);
                if (s.is(o.getItem()) && (best < 0 || Grade.of(s) > Grade.of(items.get(best)))) best = i;
            }
            if (best < 0) continue;
            used.add(items.get(best).copyWithCount(1));
            items.get(best).shrink(1);
        }
        setChanged();
        return used;
    }

    /** [내주기] */
    public void serve(ServerPlayer p, int ticket) {
        int role = role(p);
        if (village && role == ROLE_GUEST) {
            p.sendSystemMessage(Component.literal("먼저 [알바 시작]을 눌러 주세요").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (role == ROLE_GUEST) return;
        CustomerEntity c = ticket(ticket);
        if (c == null || !c.waiting()) return;
        List<ItemStack> order = c.order();
        if (!hasStock(order)) {
            p.sendSystemMessage(Component.literal("진열대에 주문한 요리가 모자라요").withStyle(ChatFormatting.RED));
            return;
        }
        List<ItemStack> used = takeOrder(order);
        long price = 0;
        float grade = 0;
        for (ItemStack s : used) {
            price += menuPrice(s);
            grade += Grade.of(s);
        }
        grade /= Math.max(1, used.size());
        price = price * c.kind().payX100 / 100;
        int tipPct = 5 + Math.round(grade * 6) + Math.round(c.patienceFrac() * 10) + c.kind().tipBonus;
        long tip = price * tipPct / 100;
        MinecraftServer server = p.level().getServer();
        WildData wd = WildData.get(server);
        String who = c.kind().ko;
        long mine;
        if (village) {
            mine = Tax.collect(server, p.getUUID(), price / 5 + tip);
            wd.addBalance(p.getUUID(), mine);
        } else if (role == ROLE_OWNER) {
            mine = Tax.collect(server, p.getUUID(), price + tip);
            wd.addBalance(p.getUUID(), mine);
        } else {
            long wage = role == ROLE_STAFF ? price * staffPct / 100 : Math.min(price, albaFee);
            mine = Tax.collect(server, p.getUUID(), wage + tip);
            wd.addBalance(p.getUUID(), mine);
            if (owner != null) {
                long ownerCut = Tax.collect(server, owner, price - wage);
                wd.addBalance(owner, ownerCut);
                ServerPlayer op = server.getPlayerList().getPlayer(owner);
                if (op != null && op != p) {
                    op.sendSystemMessage(Component.literal("🍽 " + p.getName().getString() + " 님이 " + who + "에게 서빙 · 매출 +" + ownerCut + "동")
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }
        wd.setDirty();
        salesToday += price;
        tipsToday += tip;
        servedToday++;
        xp += c.kind() == CustomerEntity.Kind.NOBLE ? 4 : 2;
        tickets.remove(c.getUUID());
        c.served(used, tip);
        Jobs.served(p);
        p.sendSystemMessage(Component.literal("🍽 " + who + "에게 내줌 · +" + mine + "동 (팁 " + tip + "동 포함, 세후)").withStyle(ChatFormatting.GREEN));
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
        setChanged();
    }

    /** 손님이 화나서 떠남 */
    public void timeout(CustomerEntity c) {
        tickets.remove(c.getUUID());
        xp = Math.max(0, xp - 3);
        setChanged();
    }

    /** 손님이 앉아서 다 먹음 (보너스 평판) */
    public void finished(CustomerEntity c) {
        xp += 1;
        setChanged();
    }

    // ───────── 손님(플레이어)으로서 ─────────
    /** 진열대 칸 i 요리 사 먹기 */
    public void buy(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= SHELF) return;
        ItemStack s = items.get(slot);
        if (s.isEmpty() || Cooking.dishOf(s) == null) return;
        int price = menuPrice(s);
        MinecraftServer server = p.level().getServer();
        WildData wd = WildData.get(server);
        // 지닌 동전(주머니 · 배낭 포함)으로 먼저, 모자라면 통장에서
        boolean paid = com.snolred.wildplus.economy.Coins.pay(p, price);
        if (!paid && wd.balance(p.getUUID()) >= price) {
            wd.addBalance(p.getUUID(), -price);
            paid = true;
        }
        if (!paid) {
            p.sendSystemMessage(Component.literal("돈이 모자라요 (" + price + "동 · 지닌 동전 + 통장)").withStyle(ChatFormatting.RED));
            return;
        }
        ItemStack got = s.copyWithCount(1);
        if (!village) {
            s.shrink(1);
            if (owner != null) wd.addBalance(owner, Tax.collect(server, owner, price));
        }
        wd.setDirty();
        if (!p.getInventory().add(got)) net.minecraft.world.Containers.dropItemStack(level, p.getX(), p.getY(), p.getZ(), got);
        salesToday += price;
        p.sendSystemMessage(Component.literal("🍴 " + got.getHoverName().getString() + " 주문 · -" + price + "동").withStyle(ChatFormatting.AQUA));
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6F, 1.8F);
        setChanged();
    }

    /** 평점 남기기 (하루 1번 · 주인 · 직원은 못 남김) */
    public boolean rate(ServerPlayer p, int stars, String text) {
        int role = role(p);
        if (role == ROLE_OWNER || role == ROLE_STAFF) {
            p.sendSystemMessage(Component.literal("자기 식당에는 평점을 남길 수 없어요").withStyle(ChatFormatting.RED));
            return false;
        }
        Long d = ratedDay.get(p.getUUID());
        if (d != null && d == day()) {
            p.sendSystemMessage(Component.literal("오늘은 이미 평점을 남겼어요 (하루 1번)").withStyle(ChatFormatting.RED));
            return false;
        }
        stars = Math.max(1, Math.min(5, stars));
        text = text.length() > 40 ? text.substring(0, 40) : text;
        ratedDay.put(p.getUUID(), day());
        ratingSum += stars;
        ratingCount++;
        reviews.add(0, stars + "\t" + p.getName().getString() + "\t" + text.replace('\t', ' '));
        while (reviews.size() > 9) reviews.remove(reviews.size() - 1);
        if (stars >= 4) xp += 5;
        else if (stars <= 2) xp = Math.max(0, xp - 5);
        setChanged();
        return true;
    }

    /** 주인 답글 (후기 i 번째, 최신 = 0) */
    public boolean reply(ServerPlayer p, int i, String text) {
        if (role(p) != ROLE_OWNER || i < 0 || i >= reviews.size()) return false;
        text = text.replace('\t', ' ').trim();
        if (text.length() > 40) text = text.substring(0, 40);
        String[] f = reviews.get(i).split("\t", -1);
        String base = (f.length > 0 ? f[0] : "0") + "\t" + (f.length > 1 ? f[1] : "?") + "\t" + (f.length > 2 ? f[2] : "");
        reviews.set(i, text.isEmpty() ? base : base + "\t" + text);
        setChanged();
        return true;
    }

    public void startAlba(ServerPlayer p) {
        if (role(p) != ROLE_GUEST) return;
        if (!village && !hiring) {
            p.sendSystemMessage(Component.literal("지금은 알바를 모집하지 않아요").withStyle(ChatFormatting.RED));
            return;
        }
        albaDay.put(p.getUUID(), day());
        setChanged();
        p.sendSystemMessage(Component.literal("👕 오늘 하루 " + displayName() + " 알바 시작! 손님 티켓의 [내주기]로 서빙하세요"
                + (village ? " (매출의 20% + 팁)" : " (서빙 1번에 " + albaFee + "동 + 팁)")).withStyle(ChatFormatting.GREEN));
    }

    public void apply(ServerPlayer p) {
        if (village || role(p) == ROLE_STAFF || role(p) == ROLE_OWNER || applicants.contains(p.getUUID())) return;
        if (applicants.size() >= 5) applicants.remove(0);
        applicants.add(p.getUUID());
        applicantNames.add(p.getName().getString());
        while (applicantNames.size() > applicants.size()) applicantNames.remove(0);
        setChanged();
        p.sendSystemMessage(Component.literal("📝 직원 지원 완료! 주인이 수락하면 직원이 돼요").withStyle(ChatFormatting.GREEN));
        if (owner != null && level instanceof ServerLevel sl) {
            ServerPlayer op = sl.getServer().getPlayerList().getPlayer(owner);
            if (op != null) op.sendSystemMessage(Component.literal("📝 " + p.getName().getString() + " 님이 " + displayName() + " 직원에 지원했어요 (카운터 → 관리)")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    public void quit(ServerPlayer p) {
        int i = staff.indexOf(p.getUUID());
        if (i >= 0) {
            staff.remove(i);
            staffNames.remove(i);
        }
        albaDay.remove(p.getUUID());
        setChanged();
        p.sendSystemMessage(Component.literal("그만뒀어요").withStyle(ChatFormatting.GRAY));
    }

    public void accept(int i) {
        if (i < 0 || i >= applicants.size() || staff.size() >= 3) return;
        staff.add(applicants.remove(i));
        staffNames.add(applicantNames.remove(i));
        setChanged();
    }

    public void fire(int i) {
        if (i < 0 || i >= staff.size()) return;
        staff.remove(i);
        staffNames.remove(i);
        setChanged();
    }

    // ───────── 화면 숫자 ─────────
    public int data(int i, Player viewer) {
        if (i < TICKETS * 8) {
            CustomerEntity c = ticket(i / 8);
            if (c == null) return 0;
            List<ItemStack> o = c.order();
            return switch (i % 8) {
                case 0 -> c.kind().ordinal() + 1;
                case 1, 2, 3 -> {
                    int k = i % 8 - 1;
                    if (k >= o.size()) yield 0;
                    CookingData.Dish d = Cooking.dishOf(o.get(k));
                    int idx = -1;
                    for (int j = 0; j < CookingData.DISHES.length; j++) if (CookingData.DISHES[j] == d) idx = j;
                    yield idx + 1;
                }
                case 4 -> Math.round(c.patienceFrac() * 100);
                case 5 -> {
                    long sum = 0;
                    for (ItemStack s : o) sum += menuPrice(s);
                    yield (int) Math.min(32000, sum * c.kind().payX100 / 100);
                }
                case 6 -> c.kind().tipBonus;
                default -> hasStock(o) ? 1 : 0;
            };
        }
        return switch (i) {
            case 32 -> stars();
            case 33 -> Math.min(32000, xp);
            case 34 -> stars() >= 5 ? 0 : STAR_XP[stars() + 1];
            case 35 -> ratingCount == 0 ? 0 : (int) (ratingSum * 10 / ratingCount);
            case 36 -> Math.min(32000, ratingCount);
            case 37 -> (int) Math.min(32000, salesToday / 10);
            case 38 -> (int) Math.min(32000, tipsToday / 10);
            case 39 -> servedToday;
            case 40 -> open ? 1 : 0;
            case 41 -> hiring ? 1 : 0;
            case 42 -> albaFee;
            case 43 -> staffPct;
            case 44 -> role(viewer);
            case 45 -> village ? 1 : 0;
            case 46, 47 -> {
                long money = WildData.get(((ServerLevel) level).getServer()).balance(viewer.getUUID())
                        + com.snolred.wildplus.economy.Coins.totalValue(viewer);
                money = Math.min(money, 30000L * 30000L - 1);
                yield (int) (i == 46 ? money % 30000 : money / 30000);
            }
            default -> 0;
        };
    }

    // ───────── 보관함 (진열대) ─────────
    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return !village && Cooking.dishOf(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Cooking.dishOf(stack) != null;
    }

    @Override
    public int getContainerSize() {
        return SHELF;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player, 8.0F);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !village) net.minecraft.world.Containers.dropContents(server, pos, items);
    }

    // ───────── 저장 ─────────
    private static final Codec<List<String>> STRS = Codec.STRING.listOf();
    private static final Codec<List<UUID>> IDS = UUIDUtil.STRING_CODEC.listOf();

    private static List<String> mapOut(Map<UUID, Long> m) {
        List<String> out = new ArrayList<>();
        m.forEach((k, v) -> out.add(k + "=" + v));
        return out;
    }

    private static void mapIn(Map<UUID, Long> m, List<String> l) {
        m.clear();
        for (String s : l) {
            int i = s.indexOf('=');
            if (i <= 0) continue;
            try {
                m.put(UUID.fromString(s.substring(0, i)), Long.parseLong(s.substring(i + 1)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.stream().map(ItemStack::copy).toList());
        out.putString("name", name);
        if (owner != null) out.store("owner", UUIDUtil.STRING_CODEC, owner);
        out.putString("owner_name", ownerName);
        out.putBoolean("village", village);
        out.putBoolean("open", open);
        out.putBoolean("hiring", hiring);
        out.putInt("alba_fee", albaFee);
        out.putInt("staff_pct", staffPct);
        out.putInt("xp", xp);
        out.store("staff", IDS, List.copyOf(staff));
        out.store("staff_names", STRS, List.copyOf(staffNames));
        out.store("applicants", IDS, List.copyOf(applicants));
        out.store("applicant_names", STRS, List.copyOf(applicantNames));
        out.store("alba", STRS, mapOut(albaDay));
        out.putLong("rating_sum", ratingSum);
        out.putInt("rating_count", ratingCount);
        out.store("reviews", STRS, List.copyOf(reviews));
        out.store("rated", STRS, mapOut(ratedDay));
        out.putLong("stat_day", statDay);
        out.putLong("sales", salesToday);
        out.putLong("tips", tipsToday);
        out.putInt("served", servedToday);
        out.store("tickets", IDS, List.copyOf(tickets));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SHELF; i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        name = in.getStringOr("name", "");
        owner = in.read("owner", UUIDUtil.STRING_CODEC).orElse(null);
        ownerName = in.getStringOr("owner_name", "");
        village = in.getBooleanOr("village", false);
        open = in.getBooleanOr("open", true);
        hiring = in.getBooleanOr("hiring", false);
        albaFee = in.getIntOr("alba_fee", 30);
        staffPct = in.getIntOr("staff_pct", 20);
        xp = in.getIntOr("xp", 0);
        staff.clear();
        staff.addAll(in.read("staff", IDS).orElse(List.of()));
        staffNames.clear();
        staffNames.addAll(in.read("staff_names", STRS).orElse(List.of()));
        applicants.clear();
        applicants.addAll(in.read("applicants", IDS).orElse(List.of()));
        applicantNames.clear();
        applicantNames.addAll(in.read("applicant_names", STRS).orElse(List.of()));
        mapIn(albaDay, in.read("alba", STRS).orElse(List.of()));
        ratingSum = in.getLongOr("rating_sum", 0L);
        ratingCount = in.getIntOr("rating_count", 0);
        reviews.clear();
        reviews.addAll(in.read("reviews", STRS).orElse(List.of()));
        mapIn(ratedDay, in.read("rated", STRS).orElse(List.of()));
        statDay = in.getLongOr("stat_day", 0L);
        salesToday = in.getLongOr("sales", 0L);
        tipsToday = in.getLongOr("tips", 0L);
        servedToday = in.getIntOr("served", 0);
        tickets.clear();
        tickets.addAll(in.read("tickets", IDS).orElse(List.of()));
    }
}
