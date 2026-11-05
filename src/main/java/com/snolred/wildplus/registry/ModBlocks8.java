package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModBlocks.BLOCKS;

import com.snolred.wildplus.block.BigBlock;
import com.snolred.wildplus.block.PlaqueBlock;
import com.snolred.wildplus.block.StartingGateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** 3.5 블록: 경마장 (마구간 · 마권 발매기 · 출발 게이트 · 전광판 · 결승 기둥) · 마을 발전 (게시판 · 기부함 · 시계판) */
public final class ModBlocks8 {
    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().strength(1.5F, 3.0F);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.METAL).noOcclusion().strength(2.0F, 6.0F);
    }

    public static final DeferredBlock<BigBlock> STABLE = BLOCKS.registerBlock("stable",
            p -> new BigBlock(p, com.snolred.wildplus.content.Derby::openStable, -1, 0, 0, 0, 2, null), ModBlocks8::wood);
    public static final DeferredBlock<BigBlock> BETTING_KIOSK = BLOCKS.registerBlock("betting_kiosk",
            p -> new BigBlock(p, com.snolred.wildplus.content.Derby::openBetting, 0, 0, 0, 0, 2, null), () -> metal().lightLevel(s -> 7));
    public static final DeferredBlock<StartingGateBlock> STARTING_GATE = BLOCKS.registerBlock("starting_gate", StartingGateBlock::new, ModBlocks8::metal);
    public static final DeferredBlock<BigBlock> SCOREBOARD = BLOCKS.registerBlock("scoreboard",
            p -> new BigBlock(p, com.snolred.wildplus.content.Derby::openBoard, -1, 1, 0, 0, 2, null), () -> metal().lightLevel(s -> 8));
    public static final DeferredBlock<BigBlock> FINISH_POST = BLOCKS.registerBlock("finish_post",
            p -> new BigBlock(p, null, 0, 0, 0, 0, 2, null), ModBlocks8::wood);
    public static final DeferredBlock<BigBlock> PROJECT_BOARD = BLOCKS.registerBlock("project_board",
            p -> new BigBlock(p, com.snolred.wildplus.content.Village::open, -1, 1, 0, 0, 2, null), ModBlocks8::wood);
    public static final DeferredBlock<BigBlock> DONATION_BOX = BLOCKS.registerBlock("donation_box",
            p -> new BigBlock(p, com.snolred.wildplus.content.Village::open, 0, 0, 0, 0, 1, null), ModBlocks8::wood);
    public static final DeferredBlock<PlaqueBlock> CLOCK_FACE = BLOCKS.registerBlock("clock_face", ClockFace::new, ModBlocks8::wood);

    /** 시계판: 문패처럼 뒤 벽에 붙는 얇은 판 (누르면 지금 시각) */
    public static final class ClockFace extends PlaqueBlock {
        public ClockFace(Properties p) {
            super(p);
        }

        @Override
        public void open(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                long t = sp.level().getServer().overworld().getOverworldClockTime();
                int mins = (int) (((t % 24000L) + 6000) % 24000L * 60 / 1000);
                sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("🕰 %d일차 (%s) %02d:%02d", t / 24000L + 1,
                        com.snolred.wildplus.content.Contest.weekdayName(t / 24000L), mins / 60, mins % 60)).withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
    }

    public static void init() {}

    private ModBlocks8() {}
}
