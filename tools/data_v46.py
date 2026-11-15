"""5.0.10 DPS 측정 허수아비 (data_v46): 아이템 그림 · 모델 · 이름 · 레시피."""
from PIL import Image

from data_v2 import ASSETS, DATA, i, shaped, write
from data_v18 import simple

LANG_KO = {
    "item.wildplus.dps_dummy": "DPS 측정 허수아비",
    "entity.wildplus.dps_dummy": "DPS 측정 허수아비",
    "tooltip.wildplus.dps_dummy": "땅에 우클릭해서 세움 · 맞아도 죽지 않음\n최근 10초 피해로 DPS 측정 (머리 위 = 모두 합계)\n때린 사람 화면 아래: 내 DPS · 한 방 최고 · 치명타 · 누적\n우클릭 = 기록 초기화 · 웅크리고 우클릭 = 회수",
}
LANG_EN = {"item.wildplus.dps_dummy": "DPS Dummy", "entity.wildplus.dps_dummy": "DPS Dummy"}

WOOD, WOOD_D, STRAW, STRAW_D, ROPE = (150, 104, 62), (104, 70, 40), (226, 190, 96), (186, 146, 62), (122, 86, 52)
SACK, SACK_D, RED, WHITE, EYE = (214, 196, 160), (170, 150, 116), (206, 52, 48), (244, 240, 230), (40, 30, 26)


def icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    def rect(x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                px[x, y] = c + (255,)
    rect(7, 9, 8, 15, WOOD_D); rect(7, 9, 7, 15, WOOD)        # 기둥
    rect(5, 15, 10, 15, (128, 128, 134))                      # 받침
    rect(1, 7, 14, 7, WOOD); rect(1, 8, 14, 8, WOOD_D)        # 가로대
    rect(0, 6, 1, 9, STRAW); rect(14, 6, 15, 9, STRAW)        # 팔 끝 짚
    rect(4, 6, 11, 12, STRAW)                                 # 몸통
    for x in range(4, 12):
        px[x, 8 if x % 2 else 11] = STRAW_D + (255,)
    rect(4, 7, 11, 7, ROPE)
    rect(6, 8, 9, 11, WHITE); rect(7, 9, 8, 10, RED)          # 과녁
    px[6, 8] = px[9, 8] = px[6, 11] = px[9, 11] = RED + (255,)
    rect(5, 1, 10, 5, SACK); rect(5, 5, 10, 5, ROPE)          # 머리
    rect(5, 1, 5, 4, SACK_D)
    px[6, 2] = px[9, 2] = EYE + (255,)
    for x in (6, 8): px[x, 4] = EYE + (255,)
    rect(7, 0, 8, 0, SACK_D)
    im.save(ASSETS / "textures/item/dps_dummy.png")


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    icon()
    simple("dps_dummy")
    write(DATA / "recipe/dps_dummy.json", shaped([" L ", "SHS", " T "], {"L": "minecraft:leather", "S": "minecraft:stick", "H": "minecraft:hay_block", "T": "minecraft:target"},
                                                 i("dps_dummy"), 1, "misc"))
