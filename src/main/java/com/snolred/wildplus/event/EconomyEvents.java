package com.snolred.wildplus.event;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.entity.BanditEntity;
import com.snolred.wildplus.entity.GoldenSlimeEntity;
import com.snolred.wildplus.registry.ModItems3;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 화폐가 흘러드는 곳
 * - 몬스터 처치: 동화 (블러드문엔 더 많이), 정예 몹: 은화·금화
 * - 황금 슬라임: 크기에 비례해 동전
 * - 도적: 플레이어를 때리면 동전을 훔치고, 처치하면 되찾는다
 */
public final class EconomyEvents {
    private EconomyEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        RandomSource random = level.getRandom();
        Collection<ItemEntity> drops = event.getDrops();

        if (entity instanceof GoldenSlimeEntity slime) {
            int size = Math.max(1, slime.getSize());
            add(drops, entity, ModItems3.COPPER_COIN.get(), size * 2 + random.nextInt(size * 2 + 1));
            if (random.nextFloat() < 0.15F * size) add(drops, entity, ModItems3.SILVER_COIN.get(), 1);
            if (size >= 4 && random.nextFloat() < 0.2F) add(drops, entity, ModItems3.GOLD_COIN.get(), 1);
            return;
        }
        if (entity instanceof BanditEntity) {
            long stolen = entity.getPersistentData().getLongOr(BanditEntity.STOLEN_KEY, 0L);
            for (ItemStack s : Coins.toCoins(stolen)) {
                add(drops, entity, s.getItem(), s.getCount());
            }
        }
        if (!(entity instanceof Monster)) {
            return;
        }
        if (EliteMobs.isElite(entity)) {
            add(drops, entity, ModItems3.SILVER_COIN.get(), 2 + random.nextInt(4));
            if (random.nextFloat() < 0.25F) add(drops, entity, ModItems3.GOLD_COIN.get(), 1);
        } else {
            boolean blood = BloodMoon.isActive(level);
            if (random.nextFloat() < (blood ? 0.6F : 0.35F)) {
                add(drops, entity, ModItems3.COPPER_COIN.get(), (blood ? 2 : 1) + random.nextInt(3));
            }
        }
    }

    private static void add(Collection<ItemEntity> drops, LivingEntity entity, net.minecraft.world.item.Item item, int count) {
        if (count <= 0) return;
        ItemEntity e = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(item, count));
        e.setDefaultPickUpDelay();
        drops.add(e);
    }

    /** 도적에게 맞으면 동전을 빼앗긴다 */
    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof BanditEntity bandit)) {
            return;
        }
        if (player.getRandom().nextFloat() > 0.5F || player.isCreative()) {
            return;
        }
        Inventory inv = player.getInventory();
        long taken = 0;
        // 은화가 있으면 1개, 없으면 동화 1~5개
        for (int i = 0; i < inv.getContainerSize() && taken == 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems3.SILVER_COIN.get())) {
                s.shrink(1);
                taken = Coins.SILVER;
            }
        }
        for (int i = 0; i < inv.getContainerSize() && taken == 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems3.COPPER_COIN.get())) {
                int n = Math.min(s.getCount(), 1 + player.getRandom().nextInt(5));
                s.shrink(n);
                taken = n;
            }
        }
        if (taken > 0) {
            long before = bandit.getPersistentData().getLongOr(BanditEntity.STOLEN_KEY, 0L);
            bandit.getPersistentData().putLong(BanditEntity.STOLEN_KEY, before + taken);
            bandit.setPersistenceRequired();
            player.sendOverlayMessage(Component.translatable("message.wildplus.bandit.stole", taken).withStyle(ChatFormatting.RED));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }
}
