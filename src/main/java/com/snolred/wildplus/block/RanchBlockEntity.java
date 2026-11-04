package com.snolred.wildplus.block;

import com.snolred.wildplus.content.Ranch;
import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.registry.ModBlockEntities;
import com.snolred.wildplus.registry.ModItems17;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 목장 기계 저장소.
 * <ul>
 *   <li>건초 여물통: 건초 더미(9) · 밀(1) 을 사료로 저장 → 1분마다 반경 8칸 동물이 하루 한 번 먹고 애정도 +25</li>
 *   <li>둥지 상자: 1분마다 반경 6칸 닭이 하루 한 번 알을 낳음 (애정도 등급 · 가끔 황금 알) · 빈손 우클릭으로 꺼냄</li>
 *   <li>치즈 숙성통 · 버터 교반기 · 물레: 재료(같은 등급끼리) 를 넣으면 하나씩 가공, 등급은 그대로</li>
 * </ul>
 */
public class RanchBlockEntity extends BlockEntity {
    public static final int MAX_IN = 64, MAX_OUT = 64;
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private int feed, progress, made;

    public RanchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RANCH.get(), pos, state);
    }

    private RanchBlock.Kind kind() {
        return getBlockState().getBlock() instanceof RanchBlock r ? r.kind : RanchBlock.Kind.NEST_BOX;
    }

    /** 가공에 걸리는 틱 (1개당) */
    private int ticks() {
        return switch (kind()) {
            case CHEESE_BARREL -> 1200;
            case BUTTER_CHURN -> 400;
            case SPINNING_WHEEL -> 200;
            default -> 0;
        };
    }

    /** 이 재료를 넣을 수 있나 */
    public boolean accepts(ItemStack s) {
        return switch (kind()) {
            case CHEESE_BARREL, BUTTER_CHURN -> s.is(ModItems17.MILK_BOTTLE.get()) || s.is(Items.MILK_BUCKET);
            case SPINNING_WHEEL -> s.is(ModItems17.WOOL_TUFT.get()) || s.is(ItemTags.WOOL);
            case HAY_TROUGH -> s.is(Items.HAY_BLOCK) || s.is(Items.WHEAT);
            default -> false;
        };
    }

    /** 재료 1개 → 결과 */
    private ItemStack result(ItemStack in) {
        int g = Grade.of(in);
        Item out;
        int n = 1;
        switch (kind()) {
            case CHEESE_BARREL -> out = ModItems14Ref.cheese();
            case BUTTER_CHURN -> out = made % 2 == 0 ? ModItems17.BUTTER.get() : ModItems17.CREAM.get();
            default -> {
                out = ModItems17.YARN.get();
                n = in.is(ItemTags.WOOL) ? 3 : 2;
            }
        }
        return Grade.apply(new ItemStack(out, n), g);
    }

    public void use(ServerPlayer p) {
        Level level = p.level();
        ItemStack hand = p.getMainHandItem();
        RanchBlock.Kind k = kind();
        // 넣기
        if (!hand.isEmpty() && accepts(hand)) {
            if (k == RanchBlock.Kind.HAY_TROUGH) {
                int per = hand.is(Items.HAY_BLOCK) ? 9 : 1;
                int room = (MAX_IN * 9 - feed) / per;
                int n = Math.min(room, hand.getCount());
                if (n <= 0) {
                    p.sendOverlayMessage(Component.literal("여물통이 가득 찼어요").withStyle(ChatFormatting.YELLOW));
                    return;
                }
                feed += n * per;
                hand.shrink(n);
                level.playSound(null, worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
                p.sendOverlayMessage(Component.literal("🌾 사료 " + feed + "회분 · 근처 동물이 하루 한 번 먹고 애정도가 올라요").withStyle(ChatFormatting.GREEN));
                setChanged();
                return;
            }
            // 우유 양동이 = 우유병 3개
            ItemStack add = hand.is(Items.MILK_BUCKET) ? Grade.apply(new ItemStack(ModItems17.MILK_BOTTLE.get(), 3), Grade.of(hand)) : hand.copy();
            if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, add)) {
                p.sendOverlayMessage(Component.literal("다른 재료(등급)가 들어 있어요 · 다 가공된 뒤에 넣어 주세요").withStyle(ChatFormatting.YELLOW));
                return;
            }
            int room = MAX_IN - input.getCount();
            if (hand.is(Items.MILK_BUCKET)) {
                if (room < 3) return;
                input = input.isEmpty() ? add : input.copyWithCount(input.getCount() + 3);
                hand.shrink(1);
                if (!p.getInventory().add(new ItemStack(Items.BUCKET))) Containers.dropItemStack(level, p.getX(), p.getY(), p.getZ(), new ItemStack(Items.BUCKET));
            } else {
                int n = Math.min(room, hand.getCount());
                if (n <= 0) return;
                input = input.isEmpty() ? hand.copyWithCount(n) : input.copyWithCount(input.getCount() + n);
                hand.shrink(n);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            status(p);
            setChanged();
            return;
        }
        // 꺼내기
        if (!output.isEmpty()) {
            ItemStack got = output;
            output = ItemStack.EMPTY;
            Ranch.contest(p, got);
            String name = got.getHoverName().getString();
            int n = got.getCount();
            if (!p.getInventory().add(got)) Containers.dropItemStack(level, p.getX(), p.getY() + 0.5, p.getZ(), got);
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
            p.sendOverlayMessage(Component.literal("📦 " + name + " ×" + n).withStyle(ChatFormatting.GOLD));
            setChanged();
            return;
        }
        status(p);
    }

    private void status(ServerPlayer p) {
        RanchBlock.Kind k = kind();
        String msg = switch (k) {
            case HAY_TROUGH -> "🌾 사료 " + feed + "회분 · 건초 더미(9회) · 밀(1회)을 들고 우클릭해 채우세요";
            case NEST_BOX -> "🥚 근처 6칸 닭이 하루 한 번 여기 알을 낳아요 · 애정도가 높으면 좋은 알 · 황금 알";
            default -> input.isEmpty() ? "⚙ " + k.ko + ": " + (k == RanchBlock.Kind.SPINNING_WHEEL ? "양털 · 양털 뭉치" : "우유병 · 우유 양동이") + "를 들고 우클릭"
                    : String.format("⚙ %s: %s ×%d 가공 중 (%d초 남음)", k.ko, input.getHoverName().getString(), input.getCount(), Math.max(0, ticks() - progress) / 20);
        };
        p.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RanchBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        RanchBlock.Kind k = be.kind();
        switch (k) {
            case HAY_TROUGH -> {
                if (level.getGameTime() % 1200 != 37 || be.feed <= 0) return;
                long today = Ranch.today(sl);
                for (Animal a : level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(8, 3, 8))) {
                    if (be.feed <= 0) break;
                    if (!Ranch.isRanchAnimal(a) || a.getPersistentData().getLongOr(Ranch.FED_DAY, -1L) == today) continue;
                    a.getPersistentData().putLong(Ranch.FED_DAY, today);
                    Ranch.addHearts(a, 25);
                    be.feed--;
                }
                be.setChanged();
            }
            case NEST_BOX -> {
                if (level.getGameTime() % 1200 != 53 || be.output.getCount() >= MAX_OUT) return;
                long today = Ranch.today(sl);
                for (Animal a : level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(6, 2, 6))) {
                    if (a.getType() != EntityType.CHICKEN || a.isBaby() || a.getPersistentData().getLongOr(Ranch.LAID_DAY, -1L) == today) continue;
                    ItemStack egg = Ranch.product(a, sl);
                    if (!be.output.isEmpty() && !ItemStack.isSameItemSameComponents(be.output, egg)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, egg); // 다른 등급은 옆에 떨어뜨림
                    } else {
                        be.output = be.output.isEmpty() ? egg : be.output.copyWithCount(Math.min(MAX_OUT, be.output.getCount() + egg.getCount()));
                    }
                    a.getPersistentData().putLong(Ranch.LAID_DAY, today);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0);
                    level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                be.setChanged();
            }
            default -> {
                if (be.input.isEmpty()) {
                    be.progress = 0;
                    return;
                }
                ItemStack res = be.result(be.input);
                if (!be.output.isEmpty() && (!ItemStack.isSameItemSameComponents(be.output, res) || be.output.getCount() + res.getCount() > MAX_OUT)) return;
                if (++be.progress < be.ticks()) {
                    if (be.progress % 40 == 0) {
                        level.playSound(null, pos, k == RanchBlock.Kind.SPINNING_WHEEL ? SoundEvents.WOOL_HIT : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.4F, 1.3F);
                    }
                    return;
                }
                be.progress = 0;
                be.made++;
                be.input = be.input.getCount() <= 1 ? ItemStack.EMPTY : be.input.copyWithCount(be.input.getCount() - 1);
                be.output = be.output.isEmpty() ? res : be.output.copyWithCount(be.output.getCount() + res.getCount());
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0);
                be.setChanged();
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            for (ItemStack s : List.of(input, output)) if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("input", ItemStack.OPTIONAL_CODEC, input);
        out.store("output", ItemStack.OPTIONAL_CODEC, output);
        out.putInt("feed", feed);
        out.putInt("progress", progress);
        out.putInt("made", made);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        feed = in.getIntOr("feed", 0);
        progress = in.getIntOr("progress", 0);
        made = in.getIntOr("made", 0);
    }

    /** 치즈 = 요리 아이템 (ModItems14) */
    private static final class ModItems14Ref {
        static Item cheese() {
            return com.snolred.wildplus.registry.ModItems14.dish("cheese");
        }
    }
}
