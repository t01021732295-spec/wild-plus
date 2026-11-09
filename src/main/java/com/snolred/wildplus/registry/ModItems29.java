package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** 4.6 음악 플레이어 · 커스텀 음반 · 악보 */
public final class ModItems29 {
    public static final DeferredItem<BlockItem> MUSIC_PLAYER = ITEMS.registerSimpleBlockItem("music_player", ModBlocks18.MUSIC_PLAYER);
    public static final DeferredItem<Item> CUSTOM_DISC = ITEMS.registerSimpleItem("custom_disc", p -> p.stacksTo(16));
    public static final DeferredItem<Item> SHEET_MUSIC = ITEMS.registerSimpleItem("sheet_music", p -> p.stacksTo(16));

    public static void init() {}

    private ModItems29() {}
}
