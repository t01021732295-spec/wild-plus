package com.snolred.wildplus.block;

import com.snolred.wildplus.entity.SeatEntity;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 3.3 가구: 여러 칸 큰 블록 + 실제 쓰임새.
 * 앉기(소파 · 안락의자 · 욕조) · 수납(옷장 · 냉장고) · 켜고 끄기(조명 · TV · 벽난로 · 샹들리에) · 잠자기(침대) ·
 * 물 받기(싱크대) · 연주(피아노) · 시간(괘종시계) · 커피(커피 테이블) · 먹이 주기(어항) · 컴퓨터(주식) · 화장대(치장) · 책장(인챈트).
 */
public class FurnitureBlock extends BigBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public enum Kind {
        SOFA(-1, 1, 0, 0, 1, 0, 0, 10, true),
        ARMCHAIR(0, 0, 0, 0, 1, 0, 0, 10, true),
        COFFEE_TABLE(0, 0, 0, 0, 1, 0, 0, 0, true),
        FIREPLACE(0, 1, 0, 0, 2, 15, 0, 0, false),
        TV_STAND(0, 1, 0, 0, 2, 6, 0, 0, false),
        FLOOR_LAMP(0, 0, 0, 0, 1, 15, 0, 0, false),
        MONSTERA(0, 0, 0, 0, 1, 0, 0, 0, false),
        RUG(-1, 1, 0, 1, 1, 0, 0, 0, true),
        WARDROBE(0, 1, 0, 0, 2, 0, 54, 0, false),
        VANITY(0, 1, 0, 0, 2, 0, 0, 0, false),
        NIGHTSTAND(0, 0, 0, 0, 1, 10, 0, 0, false),
        FRIDGE(0, 0, 0, 0, 2, 0, 27, 0, false),
        SINK(0, 0, 0, 0, 1, 0, 0, 0, false),
        BOOKSHELF(0, 1, 0, 0, 2, 0, 0, 0, false),
        DESK(0, 1, 0, 0, 1, 4, 0, 0, false),
        GRANDFATHER_CLOCK(0, 0, 0, 0, 2, 0, 0, 0, false),
        PIANO(0, 1, 0, 0, 2, 0, 0, 0, false),
        AQUARIUM(0, 1, 0, 0, 2, 10, 0, 0, false),
        CHANDELIER(0, 0, 0, 0, 1, 15, 0, 0, false),
        BATHTUB(0, 1, 0, 0, 1, 0, 0, 6, true);

        public final int lat0, lat1, dep0, dep1, height, light, storage, seat;
        public final boolean low;

        Kind(int lat0, int lat1, int dep0, int dep1, int height, int light, int storage, int seat, boolean low) {
            this.lat0 = lat0;
            this.lat1 = lat1;
            this.dep0 = dep0;
            this.dep1 = dep1;
            this.height = height;
            this.light = light;
            this.storage = storage;
            this.seat = seat;
            this.low = low;
        }

        public boolean toggles() {
            return this == FIREPLACE || this == TV_STAND || this == FLOOR_LAMP || this == NIGHTSTAND || this == CHANDELIER;
        }
    }

    public final Kind kind;

    public FurnitureBlock(Properties p, Kind kind) {
        super(p, null, kind.lat0, kind.lat1, kind.dep0, kind.dep1, kind.height, null);
        this.kind = kind;
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    public static int lightOf(BlockState s) {
        return s.getBlock() instanceof FurnitureBlock f && s.getValue(LIT) ? f.kind.light : 0;
    }

    @Override
    protected BigPartBlock.Part partAt(BlockPos master, BlockPos p, BlockPos top) {
        if (kind == Kind.RUG) return BigPartBlock.Part.FLAT;
        if (kind.low && p.getY() == master.getY()) return BigPartBlock.Part.LOW;
        return BigPartBlock.Part.HIDDEN;
    }

    private static final VoxelShape LOW = Block.box(0, 0, 0, 16, 10, 16), FLAT = Block.box(0, 0, 0, 16, 1, 16),
            POLE = Block.box(4, 0, 4, 12, 16, 12), HANG = Block.box(3, 2, 3, 13, 16, 13);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (kind) {
            case RUG -> FLAT;
            case FLOOR_LAMP, MONSTERA, NIGHTSTAND -> POLE;
            case CHANDELIER -> HANG;
            default -> kind.low ? LOW : Shapes.block();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind == Kind.RUG || kind == Kind.CHANDELIER ? Shapes.empty() : getShape(state, level, pos, ctx);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind.storage > 0 ? new FurnitureBlockEntity(pos, state) : null;
    }

    // ───────── 쓰기 ─────────
    @Override
    public void open(Player player, BlockPos m) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return;
        BlockState st = level.getBlockState(m);
        if (!(st.getBlock() instanceof FurnitureBlock)) return;
        Direction f = st.getValue(FACING);
        Vec3 c = Vec3.atBottomCenterOf(m);
        switch (kind) {
            case SOFA, ARMCHAIR, BATHTUB -> {
                // 소파: 세 자리 중 가장 가까운 빈 자리
                Vec3 best = null;
                for (int l = kind.lat0; l <= kind.lat1; l++) {
                    Vec3 at = Vec3.atBottomCenterOf(m.relative(f.getClockWise(), l)).add(0, kind.seat / 16.0, 0);
                    if (kind == Kind.BATHTUB) at = c.add(f.getClockWise().getStepX() * 0.5, kind.seat / 16.0, f.getClockWise().getStepZ() * 0.5);
                    if (SeatEntity.taken(level, at)) continue;
                    if (best == null || at.distanceToSqr(sp.position()) < best.distanceToSqr(sp.position())) best = at;
                    if (kind == Kind.BATHTUB) break;
                }
                if (best != null) SeatEntity.sit(sp, m, best, f.toYRot(), kind == Kind.BATHTUB);
            }
            case WARDROBE, FRIDGE -> {
                if (level.getBlockEntity(m) instanceof FurnitureBlockEntity be) {
                    level.playSound(null, m, kind == Kind.FRIDGE ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.7F, 1.2F);
                    sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> kind.storage >= 54 ? ChestMenu.sixRows(id, inv, be) : ChestMenu.threeRows(id, inv, be),
                            Component.translatable(st.getBlock().getDescriptionId())));
                }
            }
            case FIREPLACE, TV_STAND, FLOOR_LAMP, NIGHTSTAND, CHANDELIER -> {
                boolean on = !st.getValue(LIT);
                level.setBlock(m, st.setValue(LIT, on), 3);
                level.playSound(null, m, kind == Kind.FIREPLACE ? (on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH)
                        : SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.7F, on ? 1.2F : 0.8F);
            }
            case SINK -> {
                ItemStack hand = sp.getMainHandItem();
                ItemStack fill = hand.is(Items.BUCKET) ? new ItemStack(Items.WATER_BUCKET)
                        : hand.is(Items.GLASS_BOTTLE) ? net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                        net.minecraft.world.item.alchemy.Potions.WATER) : ItemStack.EMPTY;
                level.sendParticles(ParticleTypes.SPLASH, c.x, m.getY() + 1.1, c.z, 12, 0.2, 0.05, 0.2, 0.1);
                level.playSound(null, m, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.2F);
                if (!fill.isEmpty()) {
                    hand.shrink(1);
                    if (!sp.getInventory().add(fill)) net.minecraft.world.Containers.dropItemStack(level, sp.getX(), sp.getY(), sp.getZ(), fill);
                }
            }
            case PIANO -> openPiano(sp, level, m);
            case GRANDFATHER_CLOCK -> {
                long t = level.getOverworldClockTime();
                long day = t / 24000L + 1;
                int mins = (int) (((t % 24000L) + 6000) % 24000L * 60 / 1000);
                int h = mins / 60, mm = mins % 60;
                String ampm = h < 12 ? "오전" : "오후";
                int h12 = h % 12 == 0 ? 12 : h % 12;
                sp.sendSystemMessage(Component.literal(String.format("🕰 %d일차 %s %d:%02d", day, ampm, h12, mm)).withStyle(ChatFormatting.GOLD));
                level.playSound(null, m, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.9F, 0.7F);
            }
            case COFFEE_TABLE -> {
                long now = level.getGameTime();
                long last = sp.getPersistentData().getLongOr("wp_coffee", -99999L);
                if (now - last < 6000) {
                    sp.sendOverlayMessage(Component.literal("☕ 커피는 5분에 한 잔만 (" + (6000 - (now - last)) / 20 + "초)").withStyle(ChatFormatting.GRAY));
                    return;
                }
                sp.getPersistentData().putLong("wp_coffee", now);
                sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 180, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 180, 0));
                level.playSound(null, m, SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
                sp.sendOverlayMessage(Component.literal("☕ 커피 한 잔! 신속 · 성급함 3분").withStyle(ChatFormatting.GOLD));
            }
            case AQUARIUM -> {
                long now = level.getGameTime();
                long last = sp.getPersistentData().getLongOr("wp_fishfeed", -99999L);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, c.x, m.getY() + 0.8, c.z, 16, 0.5, 0.3, 0.2, 0.05);
                level.playSound(null, m, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.2F);
                if (now - last >= 6000) {
                    sp.getPersistentData().putLong("wp_fishfeed", now);
                    sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 300, 0));
                    sp.sendOverlayMessage(Component.literal("🐟 물고기들이 좋아해요! 행운 5분").withStyle(ChatFormatting.AQUA));
                }
            }
            case DESK -> com.snolred.wildplus.menu.StockMenu.open(sp, m);
            case VANITY -> com.snolred.wildplus.economy.Luxury.open(sp, null);
            default -> {}
        }
    }

    /** 3.4 피아노 연주 화면: 버튼 번호 = 악기 × 25 + 음 (0~24, 소리 블록 음역) → 피아노 자리에서 울림 (근처 모두 들음) */
    private static void openPiano(ServerPlayer sp, ServerLevel level, BlockPos m) {
        java.util.List<com.snolred.wildplus.menu.PickMenu.Entry> slots = new java.util.ArrayList<>();
        for (int i = 0; i < 7 * 25; i++) slots.add(new com.snolred.wildplus.menu.PickMenu.Entry("", ""));
        var sounds = java.util.List.of(SoundEvents.NOTE_BLOCK_HARP, SoundEvents.NOTE_BLOCK_BELL, SoundEvents.NOTE_BLOCK_FLUTE, SoundEvents.NOTE_BLOCK_GUITAR,
                SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_XYLOPHONE, SoundEvents.NOTE_BLOCK_BIT);
        Vec3 c = Vec3.atBottomCenterOf(m);
        com.snolred.wildplus.menu.PickMenu.open(sp, Component.literal("🎹 피아노"), "", "", slots, com.snolred.wildplus.menu.PickMenu.FLAG_PIANO, id -> {
            if (id < 0 || id >= 7 * 25 || !(level.getBlockState(m).getBlock() instanceof FurnitureBlock)) return;
            int inst = id / 25, note = id % 25;
            float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            level.playSound(null, m, sounds.get(inst).value(), SoundSource.RECORDS, 1.0F, pitch);
            if (inst == 0) level.playSound(null, m, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 0.18F, pitch);
            level.sendParticles(ParticleTypes.NOTE, c.x + (level.getRandom().nextDouble() - 0.5), m.getY() + 2.1, c.z, 0, note / 24.0, 0, 0, 1);
        }, null, pl -> pl.distanceToSqr(c.x, c.y + 0.5, c.z) < 64.0);
    }

    /** 책장: 인챈트 책장 2개만큼 */
    public float enchantPower() {
        return kind == Kind.BOOKSHELF ? 2.0F : 0.0F;
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, BlockGetter level, BlockPos pos) {
        return enchantPower();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (kind) {
            case FIREPLACE -> {
                double fx = x + f.getClockWise().getStepX() * 0.5 + f.getStepX() * 0.3, fz = z + f.getClockWise().getStepZ() * 0.5 + f.getStepZ() * 0.3;
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.FLAME, fx + r.nextGaussian() * 0.15, y + 0.45, fz + r.nextGaussian() * 0.15, 0, 0.01, 0);
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, fx, y + 2.0, fz - f.getStepZ() * 0.3, 0, 0.05, 0);
                if (r.nextInt(12) == 0) level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
            }
            case AQUARIUM -> {
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.BUBBLE_POP, x + f.getClockWise().getStepX() * 0.5 + r.nextGaussian() * 0.4,
                        y + 1.4, z + f.getClockWise().getStepZ() * 0.5 + r.nextGaussian() * 0.2, 0, 0.02, 0);
            }
            default -> {}
        }
    }
}
