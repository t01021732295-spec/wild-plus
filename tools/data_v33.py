"""4.0 데이터 (data_v33): 공허 열쇠 · 각성의 핵 · 유적 블록 · 유적 생성 · 창고 전리품 · 각성 드래곤 텍스처."""
import json
import os
import random
from pathlib import Path

from PIL import Image

from data_v2 import ASSETS, DATA, i, shaped, shapeless, write
from data_v18 import simple
from data_v32 import D, G, K, M, T, V, W, mix

TEX = ASSETS / "textures"
ROMAN = ["", "I", "II", "III", "IV", "V"]
DRAGON_REF = Path(os.environ.get("WILDPLUS_DRAGON_REF", "/tmp/claude-0/-home-claude/fdcb12d3-1cc2-5f99-8e4d-6d1b88a56850/scratchpad/dragon.png"))
STAGE = [None, (V, .05), (G, .09), (G, .14), (T, .2), ((255, 120, 220), .26)]
CORE = [None, V, G, T, (255, 190, 90), (255, 120, 220)]

LANG_KO = {
    "item.wildplus.void_key": "공허 열쇠", "tooltip.wildplus.void_key": "공허 유적의 봉인을 여는 열쇠\n공허 파편 8 + 엔더의 눈",
    "block.wildplus.void_seal": "공허 봉인", "block.wildplus.void_gate": "공허의 문", "block.wildplus.void_altar": "공허 제단", "block.wildplus.void_lift": "공허 부양판",
}
LANG_EN = {"item.wildplus.void_key": "Void Key", "block.wildplus.void_seal": "Void Seal", "block.wildplus.void_gate": "Void Gate", "block.wildplus.void_altar": "Void Altar", "block.wildplus.void_lift": "Void Lift"}
for t in range(1, 6):
    LANG_KO[f"item.wildplus.awakening_core_{t}"] = f"각성의 핵 {ROMAN[t]}"
    LANG_KO[f"tooltip.wildplus.awakening_core_{t}"] = f"엔드 중앙 섬에서 우클릭: 각성 드래곤 {ROMAN[t]}단계 소환\n엔더 드래곤을 한 번 쓰러뜨린 뒤 사용 가능"
    LANG_EN[f"item.wildplus.awakening_core_{t}"] = f"Awakening Core {ROMAN[t]}"


def core_icon(col):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            c = None
            if d < 8:
                c = W if d < 2.5 else col
            elif d < 22:
                c = mix(col, M, .6)
            elif d < 42:
                c = M if d < 34 else K
            if c:
                im.putpixel((x, y), c + (255,))
    for p in ((7, 1), (8, 14), (2, 7), (13, 8)):
        im.putpixel(p, T + (255,))
    return im


def key_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    def put(pts, c):
        for p in pts:
            im.putpixel(p, c + (255,))
    put([(x, y) for x in range(9, 15) for y in range(1, 8) if (x, y) not in ((9, 1), (14, 1), (9, 7), (14, 7))], K)
    put([(x, y) for x in range(10, 14) for y in range(2, 7)], V)
    put([(11, 3), (12, 3), (11, 4)], G); put([(12, 4)], T)
    put([(9 - n, 7 + n) for n in range(7)], V); put([(9 - n, 8 + n) for n in range(6)], M)
    put([(3, 10), (4, 11), (5, 12), (6, 13)], G)
    return im


def block(fn, name):
    im = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), fn(x, y))
    im.save(TEX / f"block/{name}.png")


def noise(x, y):
    return random.Random(f"{x}{y}").random()


def textures():
    for t in range(1, 6):
        core_icon(CORE[t]).save(TEX / f"item/awakening_core_{t}.png")
    key_icon().save(TEX / "item/void_key.png")
    block(lambda x, y: W if 7 <= x <= 8 and 7 <= y <= 8 else T if abs(x - y) == 0 or x + y == 15 else mix(D, K, noise(x, y)), "void_seal")
    block(lambda x, y: G if (x + y) % 8 == 0 else V if (x - y) % 8 == 0 else mix(D, M, noise(x, y) * .5), "void_gate")
    block(lambda x, y: W if abs(x - 7.5) + abs(y - 7.5) < 3 else G if abs(x - 7.5) + abs(y - 7.5) < 6 else mix(M, D, noise(x, y)), "void_altar")
    block(lambda x, y: T if (x + y) % 4 == 0 else mix(V, D, noise(x, y) * .6), "void_lift")
    if not DRAGON_REF.exists():
        return
    src = Image.open(DRAGON_REF).convert("RGBA")
    for t in range(1, 6):
        glow, amount = STAGE[t]
        o = src.copy()
        for y in range(o.height):
            for x in range(o.width):
                p = o.getpixel((x, y))
                if p[3] == 0:
                    continue
                base = mix(p[:3], D, .35)
                if random.Random(f"{t}{(x + y // 2) // 2}{y // 3}").random() < amount:
                    base = glow
                o.putpixel((x, y), base + (p[3],))
        o.save(TEX / f"entity/void_dragon_{t}.png")


def assets():
    simple("void_key")
    for t in range(1, 6):
        simple(f"awakening_core_{t}")
    for b in ("void_gate", "void_altar", "void_lift"):
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
    write(ASSETS / "models/block/void_seal.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i("block/void_seal")}})
    write(ASSETS / "blockstates/void_seal.json", {"variants": {f"facing={f}": {"model": i("block/void_seal")} for f in ("north", "south", "east", "west")}})


def data():
    S, H = i("void_stone"), i("void_heart")
    C = lambda t: i(f"awakening_core_{t}")
    rec = {
        "void_key": shaped(["SSS", "SES", "SSS"], {"S": i("void_shard"), "E": "minecraft:ender_eye"}, i("void_key"), category="misc"),
        "awakening_core_1": shaped(["CBC", "BNB", "CBC"], {"C": "minecraft:end_crystal", "B": "minecraft:dragon_breath", "N": "minecraft:nether_star"}, C(1), category="misc"),
        "awakening_core_2": shapeless([C(1)] + [S] * 2, C(2)),
        "awakening_core_3": shapeless([C(2)] + [S] * 4, C(3)),
        "awakening_core_4": shapeless([C(3)] + [S] * 8, C(4)),
        "awakening_core_5": shapeless([C(4)] + [S] * 7 + [H], C(5)),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    def e(name, weight, lo=1, hi=1):
        return {"type": "minecraft:item", "name": name, "weight": weight, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    write(DATA / "loot_table/chests/void_vault.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [e(i("void_shard"), 10, 4, 10), e(i("raw_void_stone"), 6, 1, 4), e("minecraft:ender_pearl", 5, 2, 6), e("minecraft:diamond", 3, 3, 8), e("minecraft:shulker_shell", 3, 1, 4)]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:book", "weight": 3, "functions": [{"function": "minecraft:enchant_with_levels", "levels": 39, "options": "#minecraft:on_random_loot"}]}, {"type": "minecraft:empty", "weight": 2}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": H, "weight": 3}, {"type": "minecraft:empty", "weight": 7}]},
    ], "random_sequence": i("chests/void_vault")})
    write(DATA / "tags/worldgen/biome/has_structure/void_ruin.json", {"values": ["minecraft:end_highlands", "minecraft:end_midlands"]})
    write(DATA / "worldgen/template_pool/void_ruin.json", {"elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i("void_ruin"), "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}], "fallback": "minecraft:empty"})
    write(DATA / "worldgen/structure/void_ruin.json", {"type": "minecraft:jigsaw", "biomes": "#wildplus:has_structure/void_ruin", "max_distance_from_center": 116, "size": 1,
        "spawn_overrides": {}, "start_height": {"absolute": 42}, "start_pool": i("void_ruin"), "step": "surface_structures", "terrain_adaptation": "none", "use_expansion_hack": False})
    write(DATA / "worldgen/structure_set/void_ruins.json", {"placement": {"type": "minecraft:random_spread", "salt": 407711, "separation": 10, "spacing": 24}, "structures": [{"structure": i("void_ruin"), "weight": 1}]})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    textures()
    assets()
    data()


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v33")
