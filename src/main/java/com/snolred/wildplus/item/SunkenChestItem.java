package com.snolred.wildplus.item;

import com.snolred.wildplus.WildPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * 침몰한 보물상자: 낚시로 얻고, 우클릭하면 보물이 쏟아진다.
 * 등급 5단계 (동 · 은 · 금 · 다이아 · 루비): 등급만큼 보물표를 더 굴리고 등급별 보너스가 붙는다. 좋은 낚싯대일수록 높은 등급이 잘 나옴.
 */
public class SunkenChestItem extends Item {
    public static final String TIER_KEY = "wp_chest";
    public static final String[] TIER = {"동", "은", "금", "다이아", "루비"};
    private static final net.minecraft.ChatFormatting[] COLOR = {net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.GRAY,
            net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.RED};
    /** 은 · 금 · 다이아 · 루비가 나올 기본 확률 (낚싯대 등급 배율을 곱함) */
    private static final float[] CHANCE = {0F, 0.30F, 0.12F, 0.04F, 0.008F};

    public static int tier(ItemStack s) {
        var d = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null ? 0 : Math.max(0, Math.min(4, d.copyTag().getIntOr(TIER_KEY, 0)));
    }

    /** 등급을 붙인 상자 */
    public static ItemStack make(int tier) {
        ItemStack s = new ItemStack(com.snolred.wildplus.registry.ModItems2.SUNKEN_CHEST.get());
        tier = Math.max(0, Math.min(4, tier));
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt(TIER_KEY, tier);
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(t));
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) tier), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(TIER[tier] + " 보물상자")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(COLOR[tier]).withItalic(false)));
        s.set(net.minecraft.core.component.DataComponents.RARITY, tier >= 4 ? net.minecraft.world.item.Rarity.EPIC
                : tier >= 2 ? net.minecraft.world.item.Rarity.RARE : net.minecraft.world.item.Rarity.UNCOMMON);
        return s;
    }

    /** 낚시로 나온 상자 등급 굴리기: m = 낚싯대 등급 배율 */
    public static int roll(net.minecraft.util.RandomSource r, float m) {
        for (int t = 4; t >= 1; t--) if (r.nextFloat() < Math.min(0.6F, CHANCE[t] * m)) return t;
        return 0;
    }

    /** 등급 보너스 */
    private static java.util.List<ItemStack> bonus(int tier, net.minecraft.util.RandomSource r) {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        switch (tier) {
            case 1 -> out.add(new ItemStack(com.snolred.wildplus.registry.ModItems3.GOLD_COIN.get(), 3 + r.nextInt(3)));
            case 2 -> {
                out.add(new ItemStack(com.snolred.wildplus.registry.ModItems3.GOLD_COIN.get(), 10 + r.nextInt(8)));
                out.add(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 2 + r.nextInt(2)));
            }
            case 3 -> {
                out.add(new ItemStack(com.snolred.wildplus.registry.ModItems3.DIAMOND_COIN.get(), 3 + r.nextInt(3)));
                out.add(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 4 + r.nextInt(4)));
                out.add(new ItemStack(net.minecraft.world.item.Items.HEART_OF_THE_SEA));
            }
            case 4 -> {
                out.add(new ItemStack(com.snolred.wildplus.registry.ModItems13.EMERALD_COIN.get(), 1 + r.nextInt(2)));
                out.add(new ItemStack(com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get(), 2));
                out.add(new ItemStack(r.nextFloat() < 0.3F ? net.minecraft.world.item.Items.NETHER_STAR : net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            }
            default -> {}
        }
        return out;
    }
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, WildPlus.id("gameplay/sunken_chest"));

    public SunkenChestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            LootTable table = server.getServer().reloadableRegistries().getLootTable(LOOT);
            LootParams params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
            int tier = tier(stack);
            net.minecraft.network.chat.MutableComponent msg = net.minecraft.network.chat.Component.literal("✦ ")
                    .append(net.minecraft.network.chat.Component.literal(TIER[tier] + " 보물상자").withStyle(COLOR[tier]))
                    .append(net.minecraft.network.chat.Component.literal("에서 나온 것: ").withStyle(net.minecraft.ChatFormatting.GOLD));
            boolean first = true;
            java.util.List<ItemStack> all = new java.util.ArrayList<>();
            for (int k = 0; k <= tier; k++) all.addAll(table.getRandomItems(params));
            all.addAll(bonus(tier, player.getRandom()));
            for (ItemStack loot : all) {
                if (loot.isEmpty()) continue;
                if (!first) msg.append(net.minecraft.network.chat.Component.literal(", ").withStyle(net.minecraft.ChatFormatting.GRAY));
                first = false;
                msg.append(loot.getHoverName().copy().withStyle(loot.getRarity().color()));
                if (loot.getCount() > 1) {
                    msg.append(net.minecraft.network.chat.Component.literal(" ×" + loot.getCount()).withStyle(net.minecraft.ChatFormatting.WHITE));
                }
                if (!player.getInventory().add(loot)) {
                    ItemEntity drop = new ItemEntity(server, player.getX(), player.getY() + 0.5, player.getZ(), loot);
                    drop.setNoPickUpDelay();
                    server.addFreshEntity(drop);
                }
            }
            if (first) msg.append(net.minecraft.network.chat.Component.literal("(텅 비어 있었어요)").withStyle(net.minecraft.ChatFormatting.GRAY));
            player.sendSystemMessage(msg);
            if (tier >= 3) {
                server.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("🎣 " + player.getName().getString() + " 님이 ")
                        .withStyle(net.minecraft.ChatFormatting.GOLD)
                        .append(net.minecraft.network.chat.Component.literal(TIER[tier] + " 보물상자").withStyle(COLOR[tier]))
                        .append(net.minecraft.network.chat.Component.literal("를 열었습니다!").withStyle(net.minecraft.ChatFormatting.GOLD)), false);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 50, 0.5, 0.6, 0.5, 0.4);
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1.0F, 0.9F);
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
