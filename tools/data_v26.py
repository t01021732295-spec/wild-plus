"""3.6 데이터 (data_v26): 로열 에메랄드 난이도 · 능력 상향 / 생명의 정수 / 정예 몹 개편 언어."""
from PIL import Image

from data_v2 import ASSETS, DATA, i, shaped, write
from data_v18 import ARMOR, simple

LANG_KO = {
    "item.wildplus.life_essence": "생명의 정수",
    "tooltip.wildplus.life_essence": "정예 몹 3% (블러드문 정예 6%) · 핏빛 군주에게서만 얻는 생명의 힘\n생명의 결정 재료",
    "item.wildplus.life_crystal": "생명의 결정",
    "tooltip.wildplus.life_crystal": ("생명의 정수 + 불사의 토템 2 + 바다의 심장 + 에메랄드 블록 4 + 에메랄드화 → 1개\n"
                                      "강화대에서 에메랄드 장비를 로열 에메랄드로 승급 (명품관 150만동 · 투기장 메달 250)"),
    "tooltip.wildplus.royal_emerald_sword": "공격력 9.5 · 속도 1.9 · 내구도 무한\n약탈 강화: 80% 전리품 두 배 · 경험치 2배\n"
                                            "생명 수확: 처치마다 체력 4 회복 · 35% 확률 에메랄드\n흡혈: 근접 피해의 15% 회복",
    "tooltip.wildplus.royal_emerald_pickaxe": "내구도 무한 · 행운의 손길 65%\n광맥의 축복: 광석 캐면 성급함 II 10초",
    "tooltip.wildplus.royal_emerald_axe": "내구도 무한 · 선제공격 3배\n처형: 체력 30% 이하인 적에게 1.8배",
    "tooltip.wildplus.royal_emerald_shovel": "내구도 무한 · 보물 찾기 10% (다이아 확률 2배)",
    "tooltip.wildplus.royal_emerald_hoe": "내구도 무한 · 풍작 100% (다 자란 작물 항상 두 배)\n대지의 축복: 손에 들면 주변 13×13 작물이 빨리 자람",
    "tooltip.wildplus.royal_emerald_bow": "맞히면 체력 4 회복 + 재생 II + 흡수 · 당김 1.8배 · 피해 1.6배 · 내구도 무한",
}
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.royal_emerald_{a}"] = (
        "내구도 무한 · 불에 타지 않음\n2부위: 행운 II · 마을의 영웅 II · 재생 I · 포만 (10초마다 배고픔 회복)\n"
        "4부위: 체력 증가 IV (+8하트) · 저항 I\n생명의 가호: 체력 30% 아래면 흡수 III + 재생 III (30초마다)\n"
        "생명의 오라: 4초마다 주변 8칸 동료 체력 2 회복\n불멸의 맹세: 죽을 피해를 받으면 체력 절반으로 버팀 (10분마다)")

AFFIX = {
    "burning": "화염", "vampiric": "흡혈", "thorns": "가시", "swift": "질풍", "giant": "거인", "venom": "맹독",
    "summoner": "소환사", "ironclad": "철벽", "volatile": "폭발", "frost": "빙결", "berserk": "광폭", "regenerating": "재생",
    "blink": "순간이동", "horde": "무리 대장", "infect": "감염", "sniper": "명사수", "volley": "연사", "mega_blast": "대폭발",
    "stealth": "그림자", "webber": "거미줄", "leaper": "도약", "warp": "공간 왜곡", "hexer": "저주", "cleaver": "회전 베기",
    "sunder": "갑옷 파괴",
}
for k, v in AFFIX.items():
    LANG_KO[f"affix.wildplus.{k}"] = v

LANG_EN = {"item.wildplus.life_essence": "Life Essence"}
for k in AFFIX:
    LANG_EN[f"affix.wildplus.{k}"] = k.replace("_", " ").title()


def icon():
    """생명의 정수: 초록 빛 구슬 + 잎 + 반짝임"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if d < 5.6:
                t = d / 5.6
                g = int(230 - 120 * t)
                px[x, y] = (int(90 - 50 * t), g, int(120 - 60 * t), 255)
            elif d < 6.4:
                px[x, y] = (20, 90, 50, 255)
    for (x, y) in [(5, 6), (6, 5), (6, 6), (5, 7)]:
        px[x, y] = (230, 255, 230, 255)
    for (x, y, c) in [(8, 2, (60, 170, 70)), (9, 1, (80, 200, 90)), (10, 1, (80, 200, 90)), (10, 2, (60, 170, 70)), (8, 3, (110, 80, 40))]:
        px[x, y] = c + (255,)
    for (x, y) in [(13, 4), (2, 12), (13, 13)]:
        px[x, y] = (255, 255, 200, 255)
    im.save(ASSETS / "textures/item/life_essence.png")


def build(lang_ko, lang_en):
    icon()
    simple("life_essence")
    write(DATA / "recipe/life_crystal.json", shaped(["BHB", "TXT", "BCB"], {
        "B": "minecraft:emerald_block", "H": "minecraft:heart_of_the_sea", "T": "minecraft:totem_of_undying",
        "X": i("life_essence"), "C": i("emerald_coin")}, i("life_crystal"), category="misc"))
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
