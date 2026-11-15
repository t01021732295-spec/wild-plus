"""5.1 네더 2차 (data_v48): 겁화의 성채 · 잿불 감시탑 (구조물 생성) · 성채 블록(문 · 제단 · 화로) · 겁화의 군주 이름 · 상자 보물."""
import random

from PIL import Image

from data_v2 import ASSETS, DATA, i, write

TEX = ASSETS / "textures"
K, D, M, V, G, W, T = (14, 8, 10), (40, 22, 26), (104, 22, 32), (196, 40, 46), (244, 156, 62), (255, 244, 214), (255, 208, 96)
ASH, ASH2 = (70, 64, 66), (110, 102, 104)

LANG_KO = {
    "block.wildplus.kalpa_gate": "겁화의 문", "block.wildplus.kalpa_altar": "군주의 제단", "block.wildplus.kalpa_brazier": "겁화의 화로",
    "entity.wildplus.kalpa_lord": "겁화의 군주",
}
LANG_EN = {"block.wildplus.kalpa_gate": "Kalpa Gate", "block.wildplus.kalpa_altar": "Lord's Altar", "block.wildplus.kalpa_brazier": "Kalpa Brazier",
           "entity.wildplus.kalpa_lord": "Kalpa Lord"}


def mix(a, b, t):
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))


def noise(x, y, s=""):
    return random.Random(f"{s}{x},{y}").random()


def tex(name, fn):
    im = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), fn(x, y))
    im.save(TEX / f"block/{name}.png")


def textures():
    # 문: 검은 쇠창살 사이로 비치는 진홍 불
    tex("kalpa_gate", lambda x, y: G if x % 4 == 1 and y % 5 == 2 else K if x % 4 in (0, 1) or y in (0, 15) else mix(M, V, noise(x, y, "g") * .7 + (y / 30)))
    # 제단 윗면: 가운데 흰 태양 + 금 테
    def altar(x, y):
        d = abs(x - 7.5) + abs(y - 7.5)
        return W if d < 2.2 else T if d < 3.4 else G if d < 4.4 else K if d < 5.4 else mix(D, M, noise(x, y, "a") * .5)
    tex("kalpa_altar", altar)
    tex("kalpa_altar_side", lambda x, y: G if y in (1, 14) else K if y in (0, 15) else mix(D, M, noise(x, y, "s") * .4) if (x + y // 4) % 4 else K)
    tex("kalpa_brazier_side", lambda x, y: G if y == 0 else mix(K, D, noise(x, y, "b") * .7) if x % 5 else ASH)
    tex("kalpa_brazier_ash", lambda x, y: mix(ASH, ASH2, noise(x, y, "h")) if 1 <= x <= 14 and 1 <= y <= 14 else K)
    tex("kalpa_brazier_fire", lambda x, y: (W if abs(x - 7.5) + abs(y - 7.5) < 3 else T if abs(x - 7.5) + abs(y - 7.5) < 5 else mix(G, V, noise(x, y, "f")))
        if 1 <= x <= 14 and 1 <= y <= 14 else K)


def brazier_model(top):
    side, t = i("block/kalpa_brazier_side"), i(f"block/kalpa_brazier_{top}")
    def el(a, b, top_tex=side):
        return {"from": a, "to": b, "faces": {f: {"texture": "#side" if f != "up" or top_tex == side else "#top"} for f in ("north", "south", "east", "west", "up", "down")}}
    return {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "top": t},
            "elements": [el([4, 0, 4], [12, 3, 12]), el([6, 3, 6], [10, 7, 10]), el([1, 7, 1], [15, 12, 15], "top")]}


def assets():
    write(ASSETS / "models/block/kalpa_gate.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i("block/kalpa_gate")}})
    write(ASSETS / "blockstates/kalpa_gate.json", {"variants": {"": {"model": i("block/kalpa_gate")}}})
    write(ASSETS / "models/block/kalpa_altar.json", {"parent": "minecraft:block/cube_bottom_top",
          "textures": {"top": i("block/kalpa_altar"), "bottom": i("block/kalpa_altar_side"), "side": i("block/kalpa_altar_side")}})
    write(ASSETS / "blockstates/kalpa_altar.json", {"variants": {"": {"model": i("block/kalpa_altar")}}})
    write(ASSETS / "models/block/kalpa_brazier.json", brazier_model("ash"))
    write(ASSETS / "models/block/kalpa_brazier_lit.json", brazier_model("fire"))
    write(ASSETS / "blockstates/kalpa_brazier.json", {"variants": {"lit=false": {"model": i("block/kalpa_brazier")}, "lit=true": {"model": i("block/kalpa_brazier_lit")}}})


def item(name, w, lo, hi, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": w, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    return e


def data():
    write(DATA / "loot_table/chests/kalpa_tower.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            item(i("ember_shard"), 10, 2, 6), item(i("raw_kalpa"), 4, 1, 2), item("minecraft:gold_ingot", 6, 3, 9), item("minecraft:blaze_rod", 5, 1, 4),
            item("minecraft:magma_cream", 4, 2, 5), item("minecraft:fire_charge", 5, 1, 3), item("minecraft:netherite_scrap", 1, 1, 1)]},
    ], "random_sequence": i("chests/kalpa_tower")})
    write(DATA / "loot_table/chests/kalpa_vault.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 6}, "entries": [
            item(i("ember_shard"), 10, 4, 10), item(i("raw_kalpa"), 7, 2, 4), item(i("kalpa_stone"), 3, 1, 2), item("minecraft:diamond", 4, 2, 6),
            item("minecraft:netherite_scrap", 3, 1, 2), item("minecraft:ancient_debris", 2, 1, 2), item("minecraft:gold_block", 3, 1, 3)]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:book", "weight": 3, "functions": [{"function": "minecraft:enchant_with_levels", "levels": 39, "options": "#minecraft:on_random_loot"}]},
                                 {"type": "minecraft:item", "name": "minecraft:enchanted_golden_apple", "weight": 1}, {"type": "minecraft:empty", "weight": 3}]},
    ], "random_sequence": i("chests/kalpa_vault")})
    # 구조물: 성채(용암 바다 위, 기단 바닥 y=30) · 감시탑(닻 기둥이 y=32 부터, 발판 y=40)
    for name, salt, spacing, sep, y in (("kalpa_citadel", 506613, 48, 20, 30), ("ember_watchtower", 506617, 22, 9, 32)):
        write(DATA / f"worldgen/template_pool/{name}.json", {"elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i(name),
              "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}], "fallback": "minecraft:empty"})
        write(DATA / f"worldgen/structure/{name}.json", {"type": "minecraft:jigsaw", "biomes": "#minecraft:is_nether", "max_distance_from_center": 80, "size": 1,
              "spawn_overrides": {}, "start_height": {"absolute": y}, "start_pool": i(name), "step": "surface_structures", "terrain_adaptation": "none", "use_expansion_hack": False})
        write(DATA / f"worldgen/structure_set/{name}s.json", {"placement": {"type": "minecraft:random_spread", "salt": salt, "separation": sep, "spacing": spacing},
              "structures": [{"structure": i(name), "weight": 1}]})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    textures()
    assets()
    data()
    import structures_v50
    print(structures_v50.build())
