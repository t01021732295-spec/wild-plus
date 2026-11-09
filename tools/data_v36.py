"""4.3 데이터 (data_v36): 공허 수정 평원(바이옴 · 지면 · 수정) · 엔드 몬스터 3종 텍스처 · 전리품 · 언어.
텍스처는 WILDPLUS_VANILLA_REF 폴더의 바닐라 그림에서 색을 바꾼다. 없으면 이미 만든 것을 둔다."""
import json
import os
from pathlib import Path

from PIL import Image

from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, write
from data_v18 import simple

TEX = ASSETS / "textures"
REF = Path(os.environ.get("WILDPLUS_VANILLA_REF", "/tmp/claude-0/-home-claude/fdcb12d3-1cc2-5f99-8e4d-6d1b88a56850/scratchpad/van3"))
KO = {"entity.wildplus.void_stalker": "공허 추적자", "entity.wildplus.rift_mite": "균열 진드기", "entity.wildplus.void_wraith": "공허 망령",
      "block.wildplus.void_turf": "공허 지면", "block.wildplus.void_crystal_cyan": "청록 공허 수정", "block.wildplus.void_crystal_purple": "보라 공허 수정",
      "item.wildplus.rift_dust": "균열 가루", "tooltip.wildplus.rift_dust": "균열 진드기가 남긴 가루\n공허 군주 소환 재료",
      "item.wildplus.wraith_plume": "망령 깃", "tooltip.wildplus.wraith_plume": "공허 망령의 깃털\n공허 군주 소환 재료", "biome.wildplus.void_crystal_plains": "공허 수정 평원"}
EN = {"entity.wildplus.void_stalker": "Void Stalker", "entity.wildplus.rift_mite": "Rift Mite", "entity.wildplus.void_wraith": "Void Wraith",
      "block.wildplus.void_turf": "Void Turf", "block.wildplus.void_crystal_cyan": "Cyan Void Crystal", "block.wildplus.void_crystal_purple": "Purple Void Crystal",
      "item.wildplus.rift_dust": "Rift Dust", "item.wildplus.wraith_plume": "Wraith Plume", "biome.wildplus.void_crystal_plains": "Void Crystal Plains"}
BLOCKS = ("void_turf", "void_crystal_cyan", "void_crystal_purple")


def recolor(src, dst, tint, vein, seed):
    if not (REF / src).exists():
        return
    im = Image.open(REF / src).convert("RGBA")
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r + g + b) / 765
            c = tuple(int(tint[k] * (0.45 + lum * 1.4)) for k in range(3))
            if (x * 7 + y * 13 + seed) % 23 == 0:
                c = vein
            px[x, y] = tuple(min(255, v) for v in c) + (a,)
    (TEX / "entity/end").mkdir(parents=True, exist_ok=True)
    im.save(TEX / f"entity/end/{dst}.png")


def tile(name, f):
    im = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), f(x, y))
    im.save(TEX / f"block/{name}.png")


def textures():
    recolor("enderman.png", "void_stalker", (34, 22, 62), (96, 232, 238), 1)
    recolor("endermite.png", "rift_mite", (58, 30, 110), (96, 232, 238), 4)
    recolor("phantom.png", "void_wraith", (52, 34, 96), (150, 90, 240), 7)
    tile("void_turf", lambda x, y: (96, 232, 238) if (x * 11 + y * 7) % 41 == 0 else (70, 40, 130) if (x * 3 + y * 5) % 3 else (56, 32, 108))
    tile("void_crystal_cyan", lambda x, y: (190, 250, 250) if (x + y) % 9 == 0 else (96, 232, 238) if (x - y) % 5 else (60, 180, 200))
    tile("void_crystal_purple", lambda x, y: (205, 160, 255) if (x + y) % 9 == 0 else (136, 72, 226) if (x - y) % 5 else (96, 48, 170))
    dust = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in [(5, 9), (6, 9), (7, 8), (8, 8), (9, 9), (10, 10), (6, 10), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11), (6, 11), (9, 11), (5, 11), (10, 11), (8, 6), (5, 6), (11, 7)]:
        dust.putpixel((x, y), (96, 232, 238, 255) if (x + y) % 3 == 0 else (136, 72, 226, 255))
    dust.save(TEX / "item/rift_dust.png")
    plume = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for k in range(11):
        x, y = 3 + k, 13 - k
        plume.putpixel((x, y), (200, 190, 230, 255))
        for w in range(1, 3):
            plume.putpixel((x, min(15, y + w)), (60, 36, 110, 255))
            plume.putpixel((max(0, x - w), y), (90, 50, 160, 255))
    plume.save(TEX / "item/wraith_plume.png")


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    textures()
    simple("rift_dust")
    simple("wraith_plume")
    for b in BLOCKS:
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        simple(b, i(f"block/{b}"))
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/pickaxe.json", [i(b) for b in BLOCKS])

    def drop(entity, name, lo, hi):
        write(DATA / f"loot_table/entities/{entity}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": i(name), "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}]}], "random_sequence": i(f"entities/{entity}")})
    drop("void_stalker", "void_shard", 1, 2)
    drop("rift_mite", "rift_dust", 1, 2)
    drop("void_wraith", "wraith_plume", 1, 1)
    write(DATA / "worldgen/feature/void_plains.json", {"type": i("void_plains")})
    write(DATA / "worldgen/placed_feature/void_plains.json", {"feature": i("void_plains"), "placement": []})
    write(DATA / "worldgen/biome/void_crystal_plains.json", {
        "attributes": {"minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {"monster": [
            {"type": i("void_stalker"), "count": 2, "weight": 8}, {"type": i("rift_mite"), "count": 4, "weight": 6}, {"type": i("void_wraith"), "count": 1, "weight": 4}, {"type": "minecraft:enderman", "count": 4, "weight": 3}]}}, "modifier": "overlay"}},
        "carvers": [], "downfall": 0.5, "effects": {"water_color": "#3f76e4"},
        "features": [[], [], [], [], ["minecraft:end_gateway_return"], [], [i("ore_void")], [], [], [i("void_plains")]],
        "has_precipitation": False, "temperature": 0.5})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v36")
