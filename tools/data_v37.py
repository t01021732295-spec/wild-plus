"""4.4 데이터 (data_v37): 군주의 인장 · 제단 · 소환 제작법."""
import json

from PIL import Image

from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, shaped, shapeless, write
from data_v18 import simple

TEX = ASSETS / "textures"
KO = {"entity.wildplus.void_lord": "공허 군주", "item.wildplus.lord_sigil": "군주의 인장", "tooltip.wildplus.lord_sigil": "공허 수정 평원의 제단에 쓰면 공허 군주 소환\n게임 속 7일에 1번",
      "block.wildplus.lord_altar": "공허 군주의 제단"}
EN = {"entity.wildplus.void_lord": "Void Lord", "item.wildplus.lord_sigil": "Lord's Sigil", "block.wildplus.lord_altar": "Void Lord's Altar"}


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(3, 13):
        for y in range(3, 13):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 6:
                im.putpixel((x, y), (236, 196, 96, 255) if d > 4.5 else (235, 60, 110, 255) if d < 1.5 else (40, 26, 70, 255))
    im.save(TEX / "item/lord_sigil.png")
    a = Image.new("RGB", (16, 16), (40, 26, 70))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                a.putpixel((x, y), (236, 196, 96))
            elif (x + y) % 6 == 0 or (x - y) % 6 == 0:
                a.putpixel((x, y), (96, 232, 238))
    a.save(TEX / "block/lord_altar.png")
    simple("lord_sigil")
    b = "lord_altar"
    write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
    write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
    simple(b, i(f"block/{b}"))
    write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/pickaxe.json", [i(b)])
    write(DATA / "recipe/lord_sigil.json", shapeless([i("lord_shard")] * 4 + [i("rift_dust")] * 2 + [i("wraith_plume")] * 2 + [i("void_heart")], i("lord_sigil")))
    write(DATA / "recipe/lord_altar.json", shaped(["BCB", "COC", "BCB"], {"B": i("void_brick"), "C": i("void_crystal_cyan"), "O": "minecraft:crying_obsidian"}, i("lord_altar"), category="misc"))


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v37")
