package com.snolred.wildplus.entity;

import com.snolred.wildplus.block.ChairBlock;
import com.snolred.wildplus.block.RestaurantCounterBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 식당 손님: 입구에서 나타나 카운터 앞에 줄을 서고 (머리 위에 주문 요리 · 인내심),
 * 요리를 받으면 빈 의자에 앉아 먹고 (♥) 다시 입구로 나가 사라진다. 기다리다 지치면 화내고 떠남.
 * 요리사(CHEF)는 마을 식당 카운터 뒤에 서 있는 장식 NPC.
 */
public class CustomerEntity extends PathfinderMob {
    public enum Kind {
        VILLAGER("마을 주민", 150, 1, 2, 0, 100, ChatFormatting.GREEN),
        TRAVELER("여행자", 120, 1, 2, 15, 100, ChatFormatting.GOLD),
        ADVENTURER("모험가", 180, 2, 3, 0, 110, ChatFormatting.AQUA),
        NOBLE("귀족 손님 ★", 120, 3, 3, 10, 200, ChatFormatting.LIGHT_PURPLE),
        CHEF("요리사", 0, 0, 0, 0, 0, ChatFormatting.WHITE);

        public final String ko;
        public final int patience, minOrder, maxOrder, tipBonus, payX100;
        public final ChatFormatting color;

        Kind(String ko, int patience, int minOrder, int maxOrder, int tipBonus, int payX100, ChatFormatting color) {
            this.ko = ko;
            this.patience = patience;
            this.minOrder = minOrder;
            this.maxOrder = maxOrder;
            this.tipBonus = tipBonus;
            this.payX100 = payX100;
            this.color = color;
        }
    }

    public enum Phase { ARRIVE, WAIT, SEAT, EAT, LEAVE, IDLE }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SEATED = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOW_ORDER = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<ItemStack>[] ORDER = new EntityDataAccessor[]{
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.ITEM_STACK)};

    private Phase phase = Phase.IDLE;
    private BlockPos counter = BlockPos.ZERO, door = BlockPos.ZERO, seat;
    private int patience, maxPatience, timer;
    private String shop = "";

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, 0);
        b.define(SEATED, false);
        b.define(SHOW_ORDER, false);
        b.define(MOOD, 0);
        for (EntityDataAccessor<ItemStack> a : ORDER) b.define(a, ItemStack.EMPTY);
    }

    public Kind kind() {
        Kind[] all = Kind.values();
        int k = this.entityData.get(KIND);
        return k >= 0 && k < all.length ? all[k] : Kind.VILLAGER;
    }

    public boolean seated() {
        return this.entityData.get(SEATED);
    }

    public boolean showOrder() {
        return this.entityData.get(SHOW_ORDER);
    }

    /** 0 보통 · 1 기쁨(먹는 중) · 2 화남 */
    public int mood() {
        return this.entityData.get(MOOD);
    }

    public List<ItemStack> order() {
        List<ItemStack> out = new ArrayList<>();
        for (EntityDataAccessor<ItemStack> a : ORDER) {
            ItemStack s = this.entityData.get(a);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    public boolean waiting() {
        return phase == Phase.ARRIVE || phase == Phase.WAIT;
    }

    public float patienceFrac() {
        return maxPatience <= 0 ? 1F : Math.max(0F, (float) patience / maxPatience);
    }

    public void setup(Kind kind, BlockPos counter, BlockPos door, List<ItemStack> order, String shop) {
        this.entityData.set(KIND, kind.ordinal());
        for (int i = 0; i < ORDER.length; i++) this.entityData.set(ORDER[i], i < order.size() ? order.get(i).copyWithCount(1) : ItemStack.EMPTY);
        this.entityData.set(SHOW_ORDER, true);
        this.counter = counter.immutable();
        this.door = door.immutable();
        this.shop = shop;
        this.maxPatience = this.patience = kind.patience * 20;
        this.phase = Phase.ARRIVE;
        updateName();
    }

    /** 마을 식당 요리사 (가만히 서 있음) */
    public void setupChef() {
        this.entityData.set(KIND, Kind.CHEF.ordinal());
        this.phase = Phase.IDLE;
        this.setCustomName(Component.literal("요리사").withStyle(ChatFormatting.WHITE));
        this.setCustomNameVisible(false);
    }

    private void updateName() {
        Kind k = kind();
        MutableComponent n = Component.literal(k.ko + " ").withStyle(k.color);
        if (waiting()) {
            int bars = Math.round(patienceFrac() * 8);
            ChatFormatting c = patienceFrac() > 0.5F ? ChatFormatting.GREEN : patienceFrac() > 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
            n.append(Component.literal("▮".repeat(bars)).withStyle(c)).append(Component.literal("▮".repeat(8 - bars)).withStyle(ChatFormatting.DARK_GRAY));
        } else if (phase == Phase.EAT) {
            n.append(Component.literal("♥ 냠냠").withStyle(ChatFormatting.RED));
        } else if (mood() == 2) {
            n.append(Component.literal("💢 너무 늦어요!").withStyle(ChatFormatting.RED));
        } else {
            n.append(Component.literal("잘 먹었습니다!").withStyle(ChatFormatting.GOLD));
        }
        this.setCustomName(n);
        this.setCustomNameVisible(true);
    }

    /** 카운터가 요리를 내줌 */
    public void served(List<ItemStack> dishes, long tip) {
        this.entityData.set(SHOW_ORDER, false);
        if (!dishes.isEmpty()) this.setItemInHand(InteractionHand.MAIN_HAND, dishes.get(0).copyWithCount(1));
        this.entityData.set(MOOD, 1);
        this.seat = findSeat();
        this.phase = seat != null ? Phase.SEAT : Phase.LEAVE;
        this.timer = 0;
        if (seat == null) this.entityData.set(MOOD, 0); // 포장해서 감
        updateName();
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2.0, getZ(), 8, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /** 카운터 근처 10칸의 빈 의자 */
    private BlockPos findSeat() {
        if (!(this.level() instanceof ServerLevel sl)) return null;
        List<BlockPos> taken = new ArrayList<>();
        for (CustomerEntity c : sl.getEntitiesOfClass(CustomerEntity.class, this.getBoundingBox().inflate(24))) {
            if (c != this && c.seat != null) taken.add(c.seat);
        }
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(counter.offset(-10, -2, -10), counter.offset(10, 2, 10))) {
            if (!(sl.getBlockState(p).getBlock() instanceof ChairBlock) || taken.contains(p)) continue;
            double d = p.distSqr(blockPosition());
            if (d < bd) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();
        if (phase == Phase.IDLE) return;
        timer++;
        switch (phase) {
            case ARRIVE, WAIT -> {
                RestaurantCounterBlockEntity be = sl.getBlockEntity(counter) instanceof RestaurantCounterBlockEntity r ? r : null;
                if (be == null || !be.tickets.contains(getUUID())) {
                    leave(false);
                    return;
                }
                BlockPos spot = be.queueSpot(getUUID());
                Vec3 target = Vec3.atBottomCenterOf(spot);
                if (position().distanceToSqr(target) > 0.6) {
                    if (timer % 20 == 1 || getNavigation().isDone()) getNavigation().moveTo(target.x, target.y, target.z, 0.7);
                } else {
                    phase = Phase.WAIT;
                    getNavigation().stop();
                    getLookControl().setLookAt(Vec3.atCenterOf(counter).add(0, 0.5, 0));
                }
                if (--patience <= 0) {
                    be.timeout(this);
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.1, getZ(), 6, 0.3, 0.2, 0.3, 0.0);
                    sl.playSound(null, blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1F, 1F);
                    leave(true);
                    return;
                }
                if (timer % 20 == 0) updateName();
            }
            case SEAT -> {
                if (seat == null || !(sl.getBlockState(seat).getBlock() instanceof ChairBlock)) {
                    leave(false);
                    return;
                }
                Vec3 target = Vec3.atBottomCenterOf(seat);
                if (position().distanceToSqr(target) > 1.2 && timer < 400) {
                    if (timer % 20 == 1 || getNavigation().isDone()) getNavigation().moveTo(target.x, target.y, target.z, 0.7);
                } else {
                    getNavigation().stop();
                    float yaw = sl.getBlockState(seat).getValue(ChairBlock.FACING).toYRot();
                    this.snapTo(target.x, seat.getY() - 0.15, target.z, yaw, 0);
                    this.setYHeadRot(yaw);
                    this.yBodyRot = yaw;
                    this.setNoGravity(true);
                    this.noPhysics = true;
                    this.entityData.set(SEATED, true);
                    phase = Phase.EAT;
                    timer = 0;
                    updateName();
                }
            }
            case EAT -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (timer % 10 == 0) {
                    ItemStack hand = getMainHandItem();
                    if (!hand.isEmpty()) sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, hand.getItem()),
                            getX(), getEyeY() - 0.2, getZ(), 3, 0.1, 0.05, 0.1, 0.05);
                    sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.5F, 0.9F + random.nextFloat() * 0.2F);
                }
                if (timer % 40 == 0) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.0, getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                if (timer >= 400) {
                    if (sl.getBlockEntity(counter) instanceof RestaurantCounterBlockEntity be) be.finished(this);
                    sl.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.2F);
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    this.entityData.set(SEATED, false);
                    this.setNoGravity(false);
                    this.noPhysics = false;
                    this.snapTo(getX(), seat.getY() + 0.6, getZ(), getYRot(), 0);
                    this.entityData.set(MOOD, 0);
                    seat = null;
                    leave(false);
                }
            }
            case LEAVE -> {
                Vec3 target = Vec3.atBottomCenterOf(door);
                if (position().distanceToSqr(target) < 1.5 || timer > 600) {
                    sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 6, 0.2, 0.4, 0.2, 0.01);
                    this.discard();
                    return;
                }
                if (timer % 20 == 1 || getNavigation().isDone()) getNavigation().moveTo(target.x, target.y, target.z, 0.7);
            }
            default -> {}
        }
    }

    private void leave(boolean angry) {
        this.entityData.set(SHOW_ORDER, false);
        if (angry) this.entityData.set(MOOD, 2);
        phase = Phase.LEAVE;
        timer = 0;
        updateName();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS; // 손님을 눌러도 아무 창도 열지 않음 (서빙은 카운터에서)
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 손님 · 요리사는 다치지 않음 (/kill 같은 명령은 됨)
        if (source.getEntity() instanceof Player || amount < 1000) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return !seated();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 손님은 저장하지 않음 (멀어지면 사라짐) · 요리사만 남음 */
    @Override
    public boolean shouldBeSaved() {
        return kind() == Kind.CHEF && super.shouldBeSaved();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("wp_kind", kind().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        int k = in.getIntOr("wp_kind", Kind.CHEF.ordinal());
        this.entityData.set(KIND, k);
        if (k == Kind.CHEF.ordinal()) setupChef();
    }
}
