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

/** 침몰한 보물상자: 낚시로 얻고, 우클릭하면 보물이 쏟아진다 */
public class SunkenChestItem extends Item {
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
            for (ItemStack loot : table.getRandomItems(params)) {
                if (!player.getInventory().add(loot)) {
                    ItemEntity drop = new ItemEntity(server, player.getX(), player.getY() + 0.5, player.getZ(), loot);
                    drop.setNoPickUpDelay();
                    server.addFreshEntity(drop);
                }
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
