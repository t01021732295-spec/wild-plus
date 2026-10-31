package com.snolred.wildplus.economy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/** 게임 안에서 읽는 요리책 (마인크래프트 기본 "글이 쓰인 책"). 한 쪽에 요리 하나씩. */
public final class Cookbook {
    private Cookbook() {}

    private record Dish(String name, String how, String effect) {}

    private static final List<Dish> DISHES = List.of(
            new Dish("얼큰한 스튜", "그릇 + 고추 2\n+ 구운 감자 + 당근\n(제작대, 모양 상관없음)", "배고픔 10\n힘 · 화염 저항 90초"),
            new Dish("꿀 토스트", "빵 + 꿀 병\n(제작대)", "재생 8초"),
            new Dish("매콤 꼬치구이", "① 막대기 2 + 날고기\n+ 고추 → 생고기 꼬치 2\n② 화로·훈연기·모닥불에\n굽기", "배고픔 9\n성급함 1분"),
            new Dish("여행자의 도시락", "빵 + 익힌 고기나 생선\n+ 말린 켈프 + 사과\n→ 2개", "배고픔 8\n신속 1분"),
            new Dish("토마토 수프", "그릇 + 토마토 3", "배를 든든하게 채움"),
            new Dish("피자 조각", "밀 2 + 토마토\n+ 익힌 고기나 버섯\n→ 3조각", "빠르게 먹는 간식"),
            new Dish("딸기 타르트", "밀 + 딸기 2 + 설탕\n+ 달걀 → 2개", "달콤한 디저트"),
            new Dish("모둠회", "그릇 + 생선 2\n(어떤 생선이든)", "수중 호흡 · 행운"),
            new Dish("매운탕", "그릇 + 생선 + 고추\n+ 토마토", "배고픔 12\n힘 · 재생"),
            new Dish("무지개송어 구이", "무지개송어를\n화로·훈연기·모닥불에\n굽기", "배고픔 7"),
            new Dish("장어구이", "폭풍 장어를\n화로·훈연기·모닥불에\n굽기", "신속 2분"),
            new Dish("군옥수수", "옥수수를 화로·훈연기·\n모닥불에 굽기", "배고픔 7"),
            new Dish("샐러드", "그릇 + 양배추 2\n+ 토마토 + 당근", "배고픔 9"),
            new Dish("블루베리 머핀", "밀 + 블루베리 2\n+ 설탕 + 달걀 → 2개", "야간 투시 1분"),
            new Dish("새 물고기 요리", "메기 → 메기 구이\n참치 → 참치 스테이크\n은어 → 은어 소금구이\n(화로·훈연기·모닥불)", "은어 소금구이:\n신속 1분"),
            new Dish("전설의 용왕어", "낚시로 1% 확률\n(비취 낚싯대 2%)\n날로 먹음", "힘 II · 저항 II 2분\n재생 II 30초"),
            new Dish("날로 먹는 재료", "고추: 화염 저항 20초\n달빛 물고기: 야간 투시\n심해 아귀: 수중 호흡\n얼음 빙어: 화염 저항\n황금 잉어: 흡수 II\n+ 재생 II", ""));

    public static ItemStack create() {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.literal("야생 플러스\n요리책\n\n").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal("모드 요리의 재료와\n만드는 법, 효과를\n정리했어요.\n\n고추·토마토·딸기·옥수수·\n양배추·블루베리 씨앗은\n잔디를 부수면 나와요.\n\n레시피 책(제작대 왼쪽\n초록 책)에서도\n볼 수 있어요.")
                        .withStyle(ChatFormatting.RESET, ChatFormatting.BLACK))));
        for (Dish d : DISHES) {
            MutableComponent page = Component.literal(d.name() + "\n\n").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
            page.append(Component.literal("만드는 법\n").withStyle(ChatFormatting.RESET, ChatFormatting.DARK_GRAY));
            page.append(Component.literal(d.how() + "\n\n").withStyle(ChatFormatting.RESET, ChatFormatting.BLACK));
            if (!d.effect().isEmpty()) {
                page.append(Component.literal("효과\n").withStyle(ChatFormatting.RESET, ChatFormatting.DARK_GRAY));
                page.append(Component.literal(d.effect()).withStyle(ChatFormatting.RESET, ChatFormatting.DARK_GREEN));
            }
            pages.add(Filterable.passThrough(page));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("야생 플러스 요리책"), "야생 플러스", 0, pages, true));
        return book;
    }
}
