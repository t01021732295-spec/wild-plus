"""4.6 데이터 (data_v39): 음악 플레이어 모델 · 음반/악보 아이콘 · 제작법 · 언어."""
import json

from PIL import Image

import kitchen_models
from cosmetic_design import Box, solid as S
from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, shaped, shapeless, write
from data_v18 import simple

WD = (120, 76, 44); WD2 = (86, 52, 30); BK = (44, 34, 34); BK2 = (74, 56, 50); DK = (30, 28, 36); DK2 = (58, 54, 66); SI = (190, 198, 215); RD = (200, 40, 50); GD = (232, 186, 70); CY = (90, 225, 235)
KO = {"block.wildplus.music_player": "음악 플레이어", "tooltip.wildplus.music_player": "서버 폴더의 노래를 트는 플레이어\n사용법은 우클릭 → [사용법]",
      "item.wildplus.custom_disc": "커스텀 음반", "tooltip.wildplus.custom_disc": "음악 플레이어에서 곡을 담아요",
      "item.wildplus.sheet_music": "악보", "tooltip.wildplus.sheet_music": "음악 플레이어에서 곡을 담고\n들고 피아노를 우클릭하면 자동 연주"}
EN = {"block.wildplus.music_player": "Music Player", "item.wildplus.custom_disc": "Custom Disc", "item.wildplus.sheet_music": "Sheet Music"}


def player():
    grooves = lambda f, u, v, w, h: (16, 16, 20) if ((u - 7) ** 2 + (v - 7) ** 2) % 9 < 6 else (60, 60, 70)
    b = [Box(-12, 0, -9, 24, 10, 18, S(WD)), Box(-12, 10, -9, 24, 2, 18, S(WD2)), Box(-7, 12, -7, 14, 1, 14, S(DK)), Box(-6, 13, -6, 12, .6, 12, grooves), Box(-1.5, 13.5, -1.5, 3, .7, 3, S(RD)),
         Box(8, 12, -6, 2, 6, 2, S(SI)), Box(2, 17, -6, 8, 1.2, 1.2, S(SI)), Box(1, 15, -6, 2, 3, 2, S(DK2)), Box(-10, 3, 9, 3, 3, .5, S(GD)), Box(-5, 3, 9, 3, 3, .5, S(CY)), Box(7, 3, 9, 3, 3, .5, S(RD))]
    cone = lambda c: (lambda f, u, v, w, h: DK if ((u - c) ** 2 + (v - c) ** 2) < c * c - 1 else BK2)
    for x in (-24, 12):
        b += [Box(x, 0, -7, 12, 30, 14, S(BK)), Box(x + 2, 18, 7, 8, 8, .5, cone(4)), Box(x + 4, 20, 7.5, 4, 4, .4, S(SI)), Box(x + 3, 5, 7, 6, 6, .5, cone(3))]
    return b


def icons():
    d = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
            if r < 7.6:
                d.putpixel((x, y), (235, 70, 80, 255) if r < 2.4 else (24, 22, 30, 255) if int(r) % 2 else (56, 52, 66, 255))
    d.save(ASSETS / "textures/item/custom_disc.png")
    s = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(2, 14):
        for y in range(1, 15):
            s.putpixel((x, y), (238, 230, 205, 255))
    for y in (3, 6, 9, 12):
        for x in range(3, 13):
            s.putpixel((x, y), (70, 60, 50, 255))
    for x, y, h in ((5, 3, 4), (8, 6, 4), (11, 9, 4)):
        for k in range(h):
            s.putpixel((x, y + k), (30, 30, 40, 255))
        s.putpixel((x - 1, y + h - 1), (30, 30, 40, 255))
    s.save(ASSETS / "textures/item/sheet_music.png")


def build(lang_ko, lang_en):
    kitchen_models.export("music_player", player, -1, 1, 0, 0, 2)
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    icons()
    simple("custom_disc")
    simple("sheet_music")
    write(DATA / "loot_table/blocks/music_player.json", self_loot("music_player"))
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/axe.json", [i("music_player")])
    write(DATA / "recipe/music_player.json", shaped(["PNP", "PJP", "III"], {"P": "#minecraft:planks", "N": "minecraft:note_block", "J": "minecraft:jukebox", "I": "minecraft:iron_ingot"}, i("music_player"), category="misc"))
    write(DATA / "recipe/custom_disc.json", shaped([" C ", "CIC", " C "], {"C": "minecraft:coal", "I": "minecraft:iron_ingot"}, i("custom_disc"), n=2, category="misc"))
    write(DATA / "recipe/sheet_music.json", shapeless(["minecraft:paper", "minecraft:ink_sac", "minecraft:feather"], i("sheet_music"), n=2))


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v39")
