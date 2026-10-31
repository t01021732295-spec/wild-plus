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
                    "검: 흡혈 20%\n도구: 밤에 채굴 1.5배\n갑옷 2: 밤 힘 I\n갑옷 4: +저항, 밤 피해\n25% 감소"),
            new Entry("귀환의 두루마리", null, "종이 + 엔더 진주\n+ 핏빛 파편 2", "3초간 읽으면 침대\n(없으면 스폰)로 이동"),
            new Entry("미스릴 장비", null, "철 장비와 같은 모양,\n미스릴 주괴로\n(검·곡괭이·도끼·삽·\n괭이·갑옷 4부위)",
                    "검 냉기 · 곡괭이 자동\n제련 · 삽 3x3 · 괭이\n3x3 · 도끼 256개 벌목\n갑옷 2: 성급함 I\n갑옷 4: 성급함 II+신속"),
            new Entry("비취 장비", null, "철 장비와 같은 모양,\n비취로\n(검·곡괭이·도끼·삽·\n괭이·갑옷 4부위)",
                    "검 동전 · 곡괭이 2배 ·\n도끼 원목+ · 삽 보물 ·\n괭이 수확+1\n갑옷 2: 행운 I\n갑옷 4: 행운 II·경험치"),
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
            new Entry("청크로더", null, "금화 1 + 엔더 진주\n+ 흑요석\n(상인에게 100동)", "놓인 청크를 항상\n불러와 둠"),
            new Entry("강화대", g("[청][금][청]", "[철][대][철]", "[  ][  ][  ]"), "청 = 청금석\n금 = 금화\n철 = 철 주괴\n대 = 대장장이 작업대",
                    "바닐라 최대 이후\n인챈트를 돈으로 강화\n(단계마다 3배)\n동전으로 수리도 가능"),
            new Entry("약국", g("[판][와][판]", "[석][양][석]", "[석][은][석]"), "판 = 유리판\n와 = 네더 사마귀\n석 = 석영 블록\n양 = 양조기\n은 = 은화",
                    "동전으로 효과 구매"),
            new Entry("기상 관측소", g("[구][햇][구]", "[구][금][구]", "[구][구][구]"), "구 = 구리 주괴\n햇 = 햇빛 감지기\n금 = 금화",
                    "동전으로 날씨·시간\n바꾸기"),
            new Entry("드릴", g("[미][다][미]", "[미][레][미]", "[  ][철][  ]"), "미 = 미스릴 주괴\n다 = 다이아 곡괭이\n레 = 레드스톤 블록\n철 = 철 주괴",
                    "곡괭이+삽, 3x3 채굴\n(웅크리면 한 칸)"),
            new Entry("자동 수확기", g("[철][괭][철]", "[철][상][철]", "[철][레][철]"), "철 = 철 주괴\n괭 = 철 괭이\n상 = 상자\n레 = 레드스톤",
                    "스프링클러 바로 밑에\n설치. 범위 안 다 자란\n작물 수확 + 씨앗 칸의\n씨앗 다시 심기. 호퍼 OK"),
            new Entry("광석 채굴기", g("[철][다][철]", "[미][금][미]", "[철][레][철]"), "철 = 철 주괴\n다 = 다이아 곡괭이\n미 = 미스릴 블록\n금 = 금화\n레 = 레드스톤 블록",
                    "창에서 광물 선택,\n현실 시간 분당 1~2개.\n호퍼로 빼기"),
            new Entry("자동 낚시 장치", g("[판][낚][판]", "[판][상][판]", "[판][레][판]"), "판 = 아무 판자\n낚 = 낚싯대\n상 = 상자\n레 = 레드스톤",
                    "물 위에 설치, 낚싯대\n칸에 낚싯대를 넣으면\n그 인챈트대로 낚시"),
            new Entry("카지노 테이블", g("[에][에][에]", "[참][금][참]", "[참][  ][참]"), "에 = 에메랄드\n참 = 짙은 참나무 판자\n금 = 금화",
                    "동전 던지기·주사위·\n슬롯머신. 상점 마을\n오락실에도 있음"),
            new Entry("블러드문 보상", null, "블러드문이 시작된 뒤\n아침까지 한 번도 안\n죽고 정예 몬스터\n5마리 이상 처치",
                    "30분간 흡혈(준 피해\n20% 회복)·재생 I·\n힘 II"),
            new Entry("인챈트 IX", null, "강화대에서 레벨이 있는\n모든 인챈트를\nIX(9)까지 강화", "바닐라 최대 이후\n단계마다 비용 3배"),
            new Entry("명예의 전당", g("[금][블][금]", "[석][다][석]", "[석][석][석]"), "금 = 금화\n블 = 금 블록\n석 = 석영 블록\n다 = 다이아 블록",
                    "돈으로 하는 엔드 컨텐츠\n명예 등급·비행 허가증\n보스 소환권·서버 축제\n부자 순위"),
            new Entry("명예 등급", null, "브론즈 2천 → 실버 8천\n→ 골드 2만5천 → 다이아\n8만 → 레전드 25만동\n등급마다 하트+2 ·\n세금-10% · 이자+0.2%p",
                    "추가 혜택(누적)\n브론즈: 배고픔 회복\n실버: 낙하 피해 절반\n골드: 처치 시 동전\n다이아: 상시 성급함\n레전드: 신속·피해-10%"),
            new Entry("특별 구매", null, "비행 30분 500 ·\n완전 회복 300 ·\n경험치 5레벨 800 ·\n상인 호출 1천 ·\n보물 지도 1천5백",
                    "수호의 축복 2천\n(30분 저항·재생·\n화염 저항) · 보스\n소환권 3천 · 서버\n축제 5천"),
            new Entry("핏빛 군주", null, "블러드문 중 서버 전체\n정예 10마리 처치 시\n나타나는 미니보스\n(보스 소환권으로도)",
                    "체력 400 · 부하 소환\n· 광폭화 · 흡혈\n보상: 군주의 심장\n(2,000동)·금화·다이아"),
            new Entry("정예 특수 능력", null, "정예 몹은 능력 1개\n(블러드문엔 2개)\n화염·흡혈·가시·질풍\n거인·맹독·소환사·\n철벽·폭발",
                    "체력 3배 · 공격 2배\n이름 앞 [능력]으로\n확인"),
            new Entry("상인 5종", null, "잡화 상인 · 대장장이\n농부 · 어부 · 경제\n상인 (상점 마을)",
                    "상인마다 파는 물건과\n사 주는 물건이 다름"),
            new Entry("새 물약 (양조기)", null, "어색한 물약 +\n미스릴 원석: 광부의 집중\n철 주괴: 강철 피부\n금화: 탐욕\n핏빛 파편: 흡혈\n고추: 광전사",
                    "광전사+비취: 유리 대포\n흡혈+발효 거미 눈:\n출혈 · 강철 피부+\n발효 거미 눈: 취약"),
            new Entry("새 효과", null, "광부의 집중: 채굴 +40%\n강철 피부: 방어 +4\n탐욕: 몬스터 은화\n출혈: 지속 피해",
                    "취약: 받는 피해 +25%\n광전사: 공격 +40%,\n받는 피해 +25%\n유리 대포: 공격 +80%,\n최대 체력 -40%"),
            new Entry("랜덤 이벤트", null, "평균 15분마다 24종\n중 하나: 골드 러시·\n유성우·보급품·보물\n찾기·현상수배·\n몬스터 습격 등",
                    "보스바에 남은 시간.\n설정에서 간격 조절"),
            new Entry("도적 아지트", null, "돌담과 망루 4개의\n큰 도적 요새.\n본관 보물 창고에\n상자 6개 + 천막 상자",
                    "도적 무리와\n도적 두목(체력 160,\n가시·흡혈)이 지킴"),
            new Entry("고대 지하 묘지", null, "땅 위의 무너진 신전\n아래로 내려가는 대형\n던전 (2층). 스포너 방\n·도서관·감옥·납골당\n·화살 함정",
                    "맨 아래 투기장의\n고대 수호자(체력 300)\n를 쓰러뜨리면 뒤쪽\n보물 창고"),
            new Entry("망자의 두루마리", g("[  ][뼈][  ]", "[종][엔][종]", "[  ][은][  ]"), "뼈 = 뼈\n종 = 종이\n엔 = 엔더 진주\n은 = 은화",
                    "마지막으로 죽은 곳으로\n순간이동. 자판기·잡화\n상인에게서도 팜"),
            new Entry("환율", null, "환전소 수수료는 매일\n아침 바뀜\n살 때 10~12개\n팔 때 8~10개", "작업대(10:1)보다\n유리한 날은 없음"),
            new Entry("방어구 관통", null, "검·도끼 인챈트\n(마법 부여대·책)\n최대 IV, 강화대로 IX", "레벨당 상대 방어구\n효과 10% 무시"),
            new Entry("세금", null, "출하·자판기 수익·\n주식 차익·의뢰·\n이자에 소득세 (복권은 면세).\n하루 소득 누진:\n~100동 5% … 30%", "은행 ATM에서\n오늘 소득·세금 확인"),
            new Entry("상점 마을", null, "평원·초원·사바나·\n사막 등에 드물게 생김", "상점 8채: 잡화점·\n대장간·은행·증권·\n약국·공구점·뽑기방·\n시청. 광장에 워프"),
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
