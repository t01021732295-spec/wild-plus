package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.FurnitureBedBlock;
import com.snolred.wildplus.block.FurnitureBlock;
import com.snolred.wildplus.block.FurnitureBlock.Kind;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 3.3 집 꾸미기: 가구 (소파 8색 · 침대 2 · 그 밖 19종) + 가구점 */
public final class ModBlocks6 {
    public static final String[] SOFA_COLORS = {"teal", "red", "gray", "cream", "navy", "pink", "green", "brown"};
    /** 블록 이름 → 가구 (소파는 sofa_색) */
    public static final Map<String, DeferredBlock<FurnitureBlock>> FURNITURE = new LinkedHashMap<>();

    private static BlockBehaviour.Properties props(Kind k) {
        BlockBehaviour.Properties p = switch (k) {
            case FRIDGE, SINK -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.METAL);
            case FIREPLACE -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS);
            case SOFA, ARMCHAIR, RUG -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOL);
            case AQUARIUM, CHANDELIER -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS);
            default -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS);
        };
        return p.noOcclusion().strength(1.5F, 3.0F).lightLevel(s -> s.hasProperty(FurnitureBlock.LIT) && s.getValue(FurnitureBlock.LIT) ? k.light : 0);
    }

    static {
        for (Kind k : Kind.values()) {
            if (k == Kind.SOFA) {
                for (String c : SOFA_COLORS) FURNITURE.put("sofa_" + c, BLOCKS.registerBlock("sofa_" + c, p -> new FurnitureBlock(p, Kind.SOFA), () -> props(Kind.SOFA)));
            } else {
                String n = k.name().toLowerCase(Locale.ROOT);
                if (k == Kind.TV_STAND) FURNITURE.put(n, BLOCKS.registerBlock(n, p -> (FurnitureBlock) new com.snolred.wildplus.block.TvBlock(p), () -> props(k)));
                else FURNITURE.put(n, BLOCKS.registerBlock(n, p -> new FurnitureBlock(p, k), () -> props(k)));
            }
        }
    }

    public static final DeferredBlock<FurnitureBedBlock> CANOPY_BED = BLOCKS.registerBlock("canopy_bed", p -> new FurnitureBedBlock(p, false),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.0F));
    public static final DeferredBlock<FurnitureBedBlock> BUNK_BED = BLOCKS.registerBlock("bunk_bed", p -> new FurnitureBedBlock(p, true),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.0F));
    public static final DeferredBlock<com.snolred.wildplus.block.ServiceBlock> HOME_STORE = BLOCKS.registerBlock("home_store",
            p -> new com.snolred.wildplus.block.ServiceBlock(p, com.snolred.wildplus.home.HomeShop::open),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F, 6.0F).lightLevel(s -> 8).noOcclusion());

    public static void init() {}

    private ModBlocks6() {}
}
