package com.snolred.wildplus.event;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.Prices;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModItems34;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 5.1.1 네더 강화
 * · 요새/보루 강화: 블레이즈 · 위더 스켈레톤 · 피글린 야수가 단단해지고(체력 +60%, 공격 +25%), 일부는 다이아 검을 든다
 * · 피글린 암시장: 금 갑옷을 입은 내가 성체 피글린을 웅크리고 우클릭 → 네더 물건을 돈(동)으로 산다 (하루 수량 제한)
 */
public final class NetherPlus {
    private NetherPlus() {}

    private static final Identifier HP = Identifier.fromNamespaceAndPath("wildplus", "nether_elite_hp"), DMG = Identifier.fromNamespaceAndPath("wildplus", "nether_elite_dmg");

    // ───────── 요새 · 보루 강화 ─────────
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel lv) || lv.dimension() != Level.NETHER || !(event.getEntity() instanceof Mob m)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString();
        boolean elite = switch (id) {
            case "minecraft:blaze", "minecraft:wither_skeleton", "minecraft:piglin_brute" -> true;
            default -> false;
        };
        if (!elite) return;
        var hp = m.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP) == null) {
            hp.addPermanentModifier(new AttributeModifier(HP, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            m.setHealth(m.getMaxHealth());
        }
        var dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(DMG) == null) dmg.addPermanentModifier(new AttributeModifier(DMG, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        var r = lv.getRandom();
        if (!id.equals("minecraft:blaze") && r.nextFloat() < 0.15F) {
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(id.equals("minecraft:piglin_brute") ? Items.DIAMOND_AXE : Items.DIAMOND_SWORD));
        }
    }

    // ───────── 피글린 암시장 ─────────
    private record Good(String name, Supplier<ItemStack> item, long price, int stock) {}

    private static List<Good> goods() {
        return List.of(
                new Good("고대 잔해", () -> new ItemStack(Items.ANCIENT_DEBRIS), Prices.x(6_000L), 2),
                new Good("네더라이트 조각", () -> new ItemStack(Items.NETHERITE_SCRAP), Prices.x(9_000L), 1),
                new Good("겁화석 원석 ×2", () -> new ItemStack(ModItems34.RAW_KALPA.get(), 2), Prices.x(4_000L), 3),
                new Good("잿불 파편 ×8", () -> new ItemStack(ModItems34.EMBER_SHARD.get(), 8), Prices.x(2_500L), 3),
                new Good("위더 스켈레톤 해골", () -> new ItemStack(Items.WITHER_SKELETON_SKULL), Prices.x(15_000L), 1),
                new Good("블레이즈 막대 ×8", () -> new ItemStack(Items.BLAZE_ROD, 8), Prices.x(1_500L), 5),
                new Good("가스트 눈물 ×2", () -> new ItemStack(Items.GHAST_TEAR, 2), Prices.x(1_500L), 5),
                new Good("마그마 크림 ×8", () -> new ItemStack(Items.MAGMA_CREAM, 8), Prices.x(1_000L), 5),
                new Good("화염구 ×8", () -> new ItemStack(Items.FIRE_CHARGE, 8), Prices.x(600L), 5),
                new Good("🗺 겁화의 성채 소문", () -> new ItemStack(Items.MAP), Prices.x(3_000L), 1));
    }

    private static boolean goldArmor(ServerPlayer p) {
        for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack a = p.getItemBySlot(s);
            if (a.is(Items.GOLDEN_HELMET) || a.is(Items.GOLDEN_CHESTPLATE) || a.is(Items.GOLDEN_LEGGINGS) || a.is(Items.GOLDEN_BOOTS)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        if (!(event.getTarget() instanceof LivingEntity t) || !BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).toString().equals("minecraft:piglin") || t.isBaby()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!goldArmor(p)) {
            p.sendOverlayMessage(Component.literal("피글린이 금 갑옷을 안 입은 손님은 상대하지 않아요").withStyle(ChatFormatting.GOLD));
            return;
        }
        open(p, t);
    }

    private static CompoundTag sold(ServerPlayer p) {
        ContentData d = ContentData.get(p.level().getServer());
        CompoundTag t = d.player("piglin_market", p.getUUID());
        if (t.getLongOr("day", -1L) != ContentData.gameDay) {
            for (String k : List.copyOf(t.keySet())) t.remove(k);
            t.putLong("day", ContentData.gameDay);
            d.dirty();
        }
        return t;
    }

    public static void open(ServerPlayer p, LivingEntity piglin) {
        List<Good> gs = goods();
        CompoundTag sold = sold(p);
        List<PickMenu.Entry> es = new ArrayList<>();
        for (int i = 0; i < gs.size(); i++) {
            Good g = gs.get(i);
            int left = g.stock() - sold.getIntOr("g" + i, 0);
            es.add(new PickMenu.Entry(g.name(), left <= 0 ? "오늘 품절" : String.format("%,d동 · 오늘 남은 %d", g.price(), left), g.item().get(),
                    left <= 0 ? PickMenu.K_INFO : PickMenu.K_ROW));
        }
        PickMenu.open(p, Component.literal("🐷 피글린 암시장"), "금 갑옷 손님 전용 · 돈(동)으로 거래 · 매일 수량이 채워져요", "", es, PickMenu.FLAG_SHOP, i -> {
            if (i < 0 || i >= gs.size() || !piglin.isAlive()) return;
            Good g = gs.get(i);
            CompoundTag sd = sold(p);
            int done = sd.getIntOr("g" + i, 0);
            if (done >= g.stock()) {
                p.sendOverlayMessage(Component.literal("오늘은 품절이에요").withStyle(ChatFormatting.RED));
                return;
            }
            if (!Coins.pay(p, g.price())) {
                p.sendOverlayMessage(Component.literal(String.format("%,d동이 필요해요", g.price())).withStyle(ChatFormatting.RED));
                return;
            }
            sd.putInt("g" + i, done + 1);
            ContentData.get(p.level().getServer()).dirty();
            if (i == gs.size() - 1) rumor(p);
            else {
                ItemStack st = g.item().get();
                if (!p.getInventory().add(st)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), st);
            }
            p.level().playSound(null, piglin.blockPosition(), SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.NEUTRAL, 1.0F, 1.0F);
            p.sendOverlayMessage(Component.literal("🐷 " + g.name() + " 구매").withStyle(ChatFormatting.GOLD));
            open(p, piglin);
        }, null, q -> piglin.isAlive() && q.distanceToSqr(piglin) < 64);
    }

    private static void rumor(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        var reg = lv.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        BlockPos found = null;
        try {
            var key = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("wildplus", "kalpa_citadel"));
            var at = lv.getChunkSource().getGenerator().findNearestMapStructure(lv, net.minecraft.core.HolderSet.direct(reg.getOrThrow(key)), p.blockPosition(), 100, false);
            if (at != null) found = at.getFirst();
        } catch (Exception ignored) {
        }
        p.sendSystemMessage(Component.literal(found == null ? "🗺 피글린: \"이 근처엔 성채가 없어… 네더에서 다시 물어봐\""
                : "🗺 피글린: \"겁화의 성채는 " + found.getX() + ", " + found.getZ() + " 쯤에 있어\"").withStyle(ChatFormatting.YELLOW));
        if (found == null) Coins.give(p, Prices.x(3_000L));
    }
}
