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
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    @SubscribeEvent
    static void onMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.snolred.wildplus.registry.ModMenus.VENDING.get(), VendingScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.PICK.get(), PickScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.BACKPACK.get(), BackpackScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.STOCKS.get(), StockScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.BANK.get(), BankScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.BOUNTY.get(), BountyScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.EXCHANGE.get(), ExchangeScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.ENHANCE.get(), EnhanceScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.MACHINE.get(), MachineScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.CASINO.get(), CasinoScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.HALL.get(), HallScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.CONTENT.get(), ContentScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.SHIPPING.get(), ShippingScreen::new);
        event.register(com.snolred.wildplus.registry.ModMenus.FILTER_HOPPER.get(), FilterHopperScreen::new);
    }

    /** 몬스터(또는 다친 생물) 머리 위 이름표 자리에 체력바를 그린다 */
    @SubscribeEvent
    static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event) {
        if (!ClientConfig.HEALTH_BARS.getAsBoolean()) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity le)
                || le instanceof net.minecraft.world.entity.player.Player
                || le instanceof net.minecraft.world.entity.decoration.ArmorStand || !le.isAlive()) return;
        float hp = le.getHealth(), max = le.getMaxHealth();
        boolean hostile = le instanceof net.minecraft.world.entity.monster.Enemy;
        if (!hostile && hp >= max) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(le) > 24 * 24) return;
        int filled = Math.max(0, Math.min(10, Math.round(10 * hp / Math.max(1.0F, max))));
        ChatFormatting color = hp > max * 0.5F ? ChatFormatting.GREEN : hp > max * 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        if (le.hasCustomName() && le.getCustomName() != null) c.append(le.getCustomName()).append(" ");
        c.append(Component.literal("■".repeat(filled)).withStyle(color));
        c.append(Component.literal("■".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
        c.append(Component.literal(String.format(" %.0f/%.0f", Math.ceil(hp), max)).withStyle(ChatFormatting.WHITE));
        event.setContent(c);
        event.setCanRender(net.minecraft.util.TriState.TRUE);
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
            // 설명이 길면 줄을 나눠서 읽기 쉽게 ("\n" 또는 " · " 기준)
            String text = Component.translatable("tooltip.wildplus." + key).getString();
            String[] lines = text.contains("\n") ? text.split("\n") : text.split(" · ");
            for (int i = lines.length - 1; i >= 0; i--) {
                event.getToolTip().add(1, Component.literal(lines[i].trim()).withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
            }
        }
        if (stack.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SWORD.get())) {
            int lv = com.snolred.wildplus.event.RubyGear.trueLevel(stack);
            event.getToolTip().add(1, Component.literal(String.format("✦ 고정 피해 %.1f (방어 무시) · 강화 %d/4", 4 + 0.5 * lv, lv))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        int ship = com.snolred.wildplus.economy.ShipPrices.centi(stack);
        if (ship > 0) {
            event.getToolTip().add(Component.translatable("tooltip.wildplus.ship_price", com.snolred.wildplus.economy.ShipPrices.fmt(ship))
                    .withStyle(ChatFormatting.DARK_GREEN));
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
