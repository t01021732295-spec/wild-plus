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

/**
 * 게임 안에서 읽는 제작 안내서 (마인크래프트 기본 "글이 쓰인 책").
 * 한 쪽에 하나씩: 제작대 3×3 칸 그림(글자 칸) + 재료 설명 + 쓰임새.
 */
public final class Guidebook {
    private Guidebook() {}

    /** grid 가 null 이면 모양 상관없이 섞기만 하면 되는 조합 */
    private record Entry(String name, String[] grid, String parts, String use) {}

    private static String[] g(String... rows) {
        return rows;
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("핏빛 강철 주괴", null, "철 주괴 1\n+ 핏빛 파편 4", "핏빛 강철 장비 재료.\n핏빛 파편은 블러드문\n몬스터·정예 몹이 떨굼"),
            new Entry("핏빛 강철 장비", null, "철 장비와 같은 모양,\n철 주괴 자리에\n핏빛 강철 주괴",
                    "검: 피해의 20% 회복\n갑옷 4부위: 밤에\n몬스터 피해 15% 감소"),
            new Entry("귀환의 두루마리", null, "종이 + 엔더 진주\n+ 핏빛 파편 2", "3초간 읽으면 침대\n(없으면 스폰)로 이동"),
            new Entry("미스릴 장비", null, "철 장비와 같은 모양,\n미스릴 주괴로\n(검·곡괭이·도끼·삽·\n괭이·갑옷 4부위)",
                    "다이아보다 빠른 채굴\n갑옷 세트: 성급함 I\n미스릴 괭이: 3x3 경작"),
            new Entry("비취 장비", null, "철 장비와 같은 모양,\n비취로\n(검·곡괭이·도끼·삽·\n괭이·갑옷 4부위)",
                    "인챈트가 아주 잘 붙음\n갑옷 세트: 행운 I"),
            new Entry("비취 낚싯대", g("[  ][  ][비]", "[  ][낚][비]", "[  ][  ][미]"), "비 = 비취\n낚 = 낚싯대\n미 = 미스릴 주괴",
                    "새 어종·보물 확률 2배"),
            new Entry("스프링클러", g("[  ][비][  ]", "[미][물][미]", "[  ][미][  ]"), "비 = 비취\n미 = 미스릴 주괴\n물 = 물 양동이",
                    "주변 9x9 작물이\n3~4배 빨리 자람"),
            new Entry("비료 (2개)", null, "뼛가루 2 +\n썩은 살점·밀 씨앗·\n켈프·흙 중 하나", "주변 3x3에\n뼛가루 2번 효과"),
            new Entry("동전 주머니", g("[  ][실][  ]", "[가][  ][가]", "[  ][가][  ]"), "실 = 실\n가 = 가죽", "동전을 한곳에 모으고\n자동 환전"),
            new Entry("동전 바꾸기", null, "금화 1 → 은화 10\n은화 1 → 동화 10\n(제작대)\n\n반대로 올리거나\n한꺼번에 바꾸려면\n환전소를 쓰세요", ""),
            new Entry("자판기", g("[철][판][철]", "[철][동][철]", "[철][레][철]"), "철 = 철 주괴\n판 = 유리판\n동 = 동화\n레 = 레드스톤",
                    "2칸 높이. 내 상품을\n동전으로 판매.\n옆에 호퍼로 자동 보충"),
            new Entry("뽑기 기계", g("[유][유][유]", "[철][레][철]", "[철][동][철]"), "유 = 유리\n철 = 철 주괴\n레 = 레드스톤\n동 = 동화",
                    "10동에 랜덤 보상"),
            new Entry("자석 부적", g("[철][  ][철]", "[레][  ][레]", "[미][철][미]"), "철 = 철 주괴\n레 = 레드스톤\n미 = 미스릴 주괴",
                    "주변 8칸 아이템·\n경험치를 끌어옴"),
            new Entry("휴대용 제작대·\n엔더 상자", null, "제작대 + 가죽 + 실\n\n엔더 상자 + 가죽\n+ 실 + 비취", "우클릭하면 어디서나\n열림"),
            new Entry("워프 포인트", g("[엔][비][엔]", "[흑][금][흑]", "[흑][흑][흑]"), "엔 = 엔더 진주\n비 = 비취\n흑 = 흑요석\n금 = 금화",
                    "워프 포인트끼리\n무제한 순간이동.\n모루로 이름 붙이기"),
            new Entry("동 배낭", g("[가][실][가]", "[가][상][가]", "[가][동][가]"), "가 = 가죽\n실 = 실\n상 = 상자\n동 = 동화",
                    "30칸. 창에서 은(50칸)\n·금(70칸)으로 업그레이드"),
            new Entry("증권 단말기", g("[판][판][판]", "[철][은][철]", "[철][레][철]"), "판 = 유리판\n철 = 철 주괴\n은 = 은화\n레 = 레드스톤",
                    "실제 주식 시세로\n사고팔기"),
            new Entry("은행 ATM", g("[철][철][철]", "[판][금][판]", "[철][레][철]"), "철 = 철 주괴\n판 = 유리판\n금 = 금화\n레 = 레드스톤",
                    "예금·출금·송금,\n매일 이자 1%"),
            new Entry("환전소", g("[철][철][철]", "[동][은][금]", "[철][철][철]"), "철 = 철 주괴\n동·은·금 = 동전",
                    "동화↔은화↔금화\n양쪽으로 바꾸기"),
            new Entry("의뢰 게시판", g("[막][종][막]", "[종][종][종]", "[막][종][막]"), "막 = 막대기\n종 = 종이",
                    "매일 납품 의뢰 3개"),
            new Entry("출하 상자", null, "상자 + 동화", "넣어 둔 물건이\n매일 아침 팔림"),
            new Entry("상점에서만", null, "소형 텔레포트기\n즉석 복권\n요리책 · 안내서\n\n→ 잡화 상인이나\n교역소 자판기에서",
                    ""));

    public static ItemStack create() {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.literal("야생 플러스\n제작 안내서\n\n").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)
                .append(Component.literal("모드 아이템 만드는 법과\n쓰임새를 정리했어요.\n\n[철] 같은 칸은\n제작대 3×3 칸이고,\n글자 뜻은 아래에 있어요.\n\n요리는 요리책을\n보세요.")
                        .withStyle(ChatFormatting.RESET, ChatFormatting.BLACK))));
        for (Entry e : ENTRIES) {
            MutableComponent page = Component.literal(e.name() + "\n").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD);
            if (e.grid() != null) {
                page.append(Component.literal("\n" + String.join("\n", e.grid()) + "\n\n").withStyle(ChatFormatting.RESET, ChatFormatting.DARK_GRAY));
            } else {
                page.append(Component.literal("(모양 상관없이)\n").withStyle(ChatFormatting.RESET, ChatFormatting.GRAY));
            }
            page.append(Component.literal(e.parts() + "\n").withStyle(ChatFormatting.RESET, ChatFormatting.BLACK));
            if (!e.use().isEmpty()) {
                page.append(Component.literal("\n" + e.use()).withStyle(ChatFormatting.RESET, ChatFormatting.DARK_GREEN));
            }
            pages.add(Filterable.passThrough(page));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("야생 플러스 제작 안내서"), "야생 플러스", 0, pages, true));
        return book;
    }
}
