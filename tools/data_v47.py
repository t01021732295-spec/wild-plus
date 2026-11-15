"""5.0.11 (data_v47): 정화의 주문서(저주 제거) · 운명의 제단 화면 안내."""
from PIL import Image

from data_v2 import ASSETS, DATA, i, shapeless, write
from data_v18 import simple

LANG_KO = {
    "item.wildplus.purify_scroll": "정화의 주문서",
    "tooltip.wildplus.purify_scroll": "귀속 저주 · 소실 저주 등 모든 저주를 지움\n저주 걸린 장비를 왼손(보조 손)에 들고 우클릭\n왼손이 비면 입고 있는 장비 중 저주 걸린 것 하나를 정화",
    "tooltip.wildplus.fate_altar": "우클릭: 운명의 제단 화면\n든 장비에 랜덤 인챈트 1개 (1~15단계) · 2만 4천~4만동",
}
LANG_EN = {"item.wildplus.purify_scroll": "Scroll of Purification"}

PAPER, PAPER_D, EDGE, GOLD, GLOW, INK = (238, 226, 196), (206, 190, 150), (150, 120, 80), (240, 196, 70), (150, 230, 255), (70, 120, 200)


def icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    def put(x, y, c):
        px[x, y] = c + (255,)
    for y in range(3, 13):                    # 펼친 종이
        for x in range(3, 13):
            put(x, y, PAPER if (x + y) % 5 else PAPER_D)
    for x in range(2, 14):                    # 위아래 말린 부분
        put(x, 2, EDGE); put(x, 13, EDGE)
        put(x, 3, PAPER_D); put(x, 12, PAPER_D)
    for y in (2, 13):
        put(1, y, GOLD); put(14, y, GOLD)
    for (x, y) in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (8, 7), (10, 7), (8, 8), (7, 9), (9, 9), (8, 10)):  # 빛나는 문양
        put(x, y, INK)
    put(8, 7, GLOW); put(8, 6, GLOW)
    for (x, y) in ((4, 5), (11, 9), (5, 10)):
        put(x, y, GLOW)
    im.save(ASSETS / "textures/item/purify_scroll.png")


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    icon()
    simple("purify_scroll")
    write(DATA / "recipe/purify_scroll.json", shapeless(["minecraft:paper", "minecraft:amethyst_shard", "minecraft:amethyst_shard",
                                                         "minecraft:lapis_lazuli", "minecraft:lapis_lazuli", "minecraft:gold_ingot"], i("purify_scroll")))
