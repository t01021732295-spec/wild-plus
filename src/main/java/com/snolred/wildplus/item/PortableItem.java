package com.snolred.wildplus.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** 휴대용 제작대 / 휴대용 엔더 상자: 들고 우클릭하면 바로 열린다 */
public class PortableItem extends Item {
    public enum Kind { CRAFTING, ENDER_CHEST }

    private final Kind kind;

    public PortableItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            if (kind == Kind.CRAFTING) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                        new CraftingMenu(id, inv, ContainerLevelAccess.create(level, p.blockPosition())) {
                            @Override
                            public boolean stillValid(Player who) {
                                return true;
                            }
                        }, Component.translatable("container.crafting")));
            } else {
                player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                        ChestMenu.threeRows(id, inv, p.getEnderChestInventory()), Component.translatable("container.enderchest")));
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.6F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
