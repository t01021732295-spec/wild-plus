package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = WildPlus.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public class WildPlusClient {
    public WildPlusClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String key = null;
        if (stack.is(ModItems.BLOOD_SHARD.get())) key = "blood_shard";
        else if (stack.is(ModItems.BLOODSTEEL_SWORD.get())) key = "bloodsteel_sword";
        else if (isArmor(stack.getItem())) key = "bloodsteel_armor";
        else if (stack.is(ModItems.RECALL_SCROLL.get())) key = "recall_scroll";
        else if (stack.is(ModItems.LUCKY_CHARM.get())) key = "lucky_charm";
        else {
            // 그 외 아이템은 번역 파일에 tooltip.wildplus.<아이템 이름> 이 있으면 표시
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (WildPlus.MODID.equals(id.getNamespace()) && Language.getInstance().has("tooltip.wildplus." + id.getPath())) {
                key = id.getPath();
            }
        }
        if (key != null) {
            event.getToolTip().add(1, Component.translatable("tooltip.wildplus." + key).withStyle(ChatFormatting.GRAY));
        }
        if (Coins.isPouch(stack)) {
            event.getToolTip().add(1, Component.translatable("tooltip.wildplus.pouch_value", Coins.describe(Coins.pouchValue(stack)))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static boolean isArmor(Item item) {
        return item == ModItems.BLOODSTEEL_HELMET.get() || item == ModItems.BLOODSTEEL_CHESTPLATE.get()
                || item == ModItems.BLOODSTEEL_LEGGINGS.get() || item == ModItems.BLOODSTEEL_BOOTS.get();
    }
}
