"""4.9 데이터 (data_v42): 모험 수첩(도감 · 업적 · 칭호) 아이콘 · 제작법 · 언어."""
import json

from PIL import Image

from data_v2 import ASSETS, DATA, i, shapeless, write
from data_v18 import simple

KO = {"item.wildplus.adventure_journal": "모험 수첩", "tooltip.wildplus.adventure_journal": "들고 우클릭: 도감 · 업적 · 칭호\n(/dex 로도 열려요)"}
EN = {"item.wildplus.adventure_journal": "Adventure Journal"}


def icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cover, dark, page, gold, band = (46, 110, 150, 255), (28, 70, 104, 255), (240, 232, 208, 255), (240, 196, 80, 255), (200, 70, 70, 255)
    for x in range(3, 14):
        for y in range(1, 15):
            im.putpixel((x, y), dark if x == 3 or y == 14 else cover)
    for y in range(2, 14):
        im.putpixel((13, y), page)
    for x in range(4, 13):
        im.putpixel((x, 14), page)
    for x, y in ((8, 4), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (7, 7), (8, 7), (9, 7), (7, 8), (9, 8)):  # 별
        im.putpixel((x, y), gold)
    for x in range(5, 12):
        im.putpixel((x, 11), gold)
    for y in range(1, 15):
        im.putpixel((11, y), band) if y > 11 else None
    im.save(ASSETS / "textures/item/adventure_journal.png")


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    icon()
    simple("adventure_journal")
    write(DATA / "recipe/adventure_journal.json", shapeless(["minecraft:book", "minecraft:feather"], i("adventure_journal")))


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v42")
