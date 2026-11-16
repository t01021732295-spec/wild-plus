"""5.3 고대 도시 (data_v51): 몬스터 이름 · 구조물 블록(공명 종 · 봉인 · 악보 석판 · 성소 제단) · 침묵 향 · 상자 보물 · 구조물 생성."""
import random

from PIL import Image

from data_v2 import ASSETS, DATA, i, shapeless, write
from data_v18 import simple

TEX = ASSETS / "textures"
SC, SC2, SC3, CY, CY2 = (8, 46, 56), (14, 74, 86), (4, 28, 34), (60, 230, 236), (150, 255, 250)
DS, DS2, DS3, GD, GD2, BONE = (52, 54, 62), (72, 74, 84), (30, 32, 38), (200, 176, 104), (140, 116, 60), (214, 216, 202)

LANG_KO = {
    "entity.wildplus.sculk_stalker": "스컬크 추적자", "entity.wildplus.echo_bats": "메아리 박쥐 떼", "entity.wildplus.silent_priest": "침묵의 사제",
    "entity.wildplus.sculk_golem": "스컬크 골렘",
    "block.wildplus.ancient_bell": "공명 종", "block.wildplus.ancient_seal": "스컬크 봉인", "block.wildplus.score_tablet": "악보 석판",
    "block.wildplus.sanctum_altar": "성소 제단",
    "item.wildplus.silence_incense": "침묵 향",
    "tooltip.wildplus.silence_incense": "우클릭: 나와 8칸 안 친구들의 소음을 0으로\n30초 동안 소음이 20% 만 오름 (딥다크)",
}
LANG_EN = {"entity.wildplus.sculk_stalker": "Sculk Stalker", "entity.wildplus.echo_bats": "Echo Bat Swarm", "entity.wildplus.silent_priest": "Silent Priest",
           "entity.wildplus.sculk_golem": "Sculk Golem", "block.wildplus.ancient_bell": "Resonance Bell", "block.wildplus.ancient_seal": "Sculk Seal",
           "block.wildplus.score_tablet": "Score Tablet", "block.wildplus.sanctum_altar": "Sanctum Altar", "item.wildplus.silence_incense": "Silence Incense"}


def mix(a, b, t):
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))


def nz(x, y, s):
    return random.Random(f"{s}{x},{y}").random()


def tex(name, fn, folder="block"):
    im = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), fn(x, y))
    im.save(TEX / f"{folder}/{name}.png")


def textures():
    tex("ancient_seal", lambda x, y: CY if nz(x, y, "s") < .06 else SC3 if (x + y) % 5 == 0 else mix(SC, SC2, nz(x, y, "t")))
    tex("ancient_bell", lambda x, y: GD if (y in (2, 3) or x in (0, 15)) else mix(GD2, GD, nz(x, y, "b") * .6) if y > 3 else DS2)
    tex("ancient_bell_top", lambda x, y: CY if 6 <= x <= 9 and 6 <= y <= 9 else GD2)
    def tablet(x, y):
        if y in (3, 7, 11):
            return mix(DS3, CY, .4)
        notes = {(4, 5), (5, 4), (8, 9), (9, 8), (11, 5), (12, 4), (6, 13), (7, 12)}
        return CY2 if (x, y) in notes else DS if (x + y) % 3 else DS2
    tex("score_tablet", tablet)
    tex("score_tablet_side", lambda x, y: DS3 if y in (0, 15) else DS if (x + y) % 4 else DS2)
    tex("sanctum_altar", lambda x, y: CY2 if abs(x - 7.5) + abs(y - 7.5) < 2 else CY if abs(x - 7.5) + abs(y - 7.5) < 3.5 else SC if abs(x - 7.5) + abs(y - 7.5) < 6 else DS)
    tex("sanctum_altar_side", lambda x, y: BONE if y in (1, 14) else SC2 if (x + y // 3) % 5 == 0 else DS)
    # 침묵 향 (아이템)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(9, 15):
        for x in range(5, 11):
            px[x, y] = (DS2 if (x + y) % 3 else DS) + (255,)
    for x in range(4, 12):
        px[x, 9] = GD + (255,)
        px[x, 14] = GD2 + (255,)
    for y in range(3, 9):
        px[7 + (y % 2), y] = BONE + (255,)
    for (x, y) in ((6, 2), (8, 1), (9, 3), (6, 5), (10, 4)):
        px[x, y] = CY2 + (255,) if (x + y) % 2 else CY + (255,)
    im.save(TEX / "item/silence_incense.png")


def assets():
    write(ASSETS / "models/block/ancient_seal.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i("block/ancient_seal")}})
    write(ASSETS / "blockstates/ancient_seal.json", {"variants": {"": {"model": i("block/ancient_seal")}}})
    write(ASSETS / "models/block/score_tablet.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": i("block/score_tablet"),
          "side": i("block/score_tablet_side"), "bottom": i("block/score_tablet_side")}})
    write(ASSETS / "blockstates/score_tablet.json", {"variants": {"": {"model": i("block/score_tablet")}}})
    write(ASSETS / "models/block/sanctum_altar.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": i("block/sanctum_altar"),
          "side": i("block/sanctum_altar_side"), "bottom": i("block/sanctum_altar_side")}})
    write(ASSETS / "blockstates/sanctum_altar.json", {"variants": {"": {"model": i("block/sanctum_altar")}}})
    side, top = i("block/ancient_bell"), i("block/ancient_bell_top")
    def el(a, b):
        return {"from": a, "to": b, "faces": {f: {"texture": "#top" if f in ("up", "down") else "#side"} for f in ("north", "south", "east", "west", "up", "down")}}
    write(ASSETS / "models/block/ancient_bell.json", {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "top": top},
          "elements": [el([3, 0, 3], [13, 2, 13]), el([4, 2, 4], [12, 9, 12]), el([5, 9, 5], [11, 11, 11]), el([7, 11, 7], [9, 12, 9])]})
    write(ASSETS / "blockstates/ancient_bell.json", {"variants": {f"note={n}": {"model": i("block/ancient_bell")} for n in range(4)}})


def item(name, w, lo, hi):
    return {"type": "minecraft:item", "name": name, "weight": w, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


def stone(name, w):
    return {"type": "minecraft:item", "name": i(name), "weight": w}


def data():
    write(DATA / "recipe/silence_incense.json", shapeless(["minecraft:echo_shard", "minecraft:white_wool", "minecraft:amethyst_shard", "minecraft:candle"], i("silence_incense"), 2))
    common = [item("minecraft:echo_shard", 8, 1, 3), item("minecraft:sculk", 6, 4, 12), item("minecraft:experience_bottle", 6, 3, 8), item(i("silence_incense"), 6, 1, 2),
              item("minecraft:amethyst_shard", 5, 3, 8), item("minecraft:diamond", 3, 1, 3)]
    rare = [stone("engrave_execute", 1), stone("engrave_sonic", 1), stone("engrave_bulwark", 1), stone("engrave_echo", 1), {"type": "minecraft:empty", "weight": 10}]
    for name, rolls, extra in (("echo_spire", (3, 5), [item("minecraft:disc_fragment_5", 2, 1, 2)]), ("sealed_archive", (4, 6), [item(i("engrave_expander"), 1, 1, 1),
                               {"type": "minecraft:item", "name": "minecraft:book", "weight": 4, "functions": [{"function": "minecraft:enchant_with_levels", "levels": 39, "options": "#minecraft:on_random_loot"}]}]),
                               ("sleeping_sanctum", (3, 5), [item("minecraft:netherite_scrap", 2, 1, 2)])):
        write(DATA / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": common + extra},
            {"rolls": 1, "entries": rare}], "random_sequence": i(f"chests/{name}")})
    for name, salt, spacing, sep, y in (("echo_spire", 530011, 18, 6, -50), ("sealed_archive", 530017, 14, 5, -42), ("sleeping_sanctum", 530023, 24, 8, -46)):
        write(DATA / f"worldgen/template_pool/{name}.json", {"elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i(name),
              "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}], "fallback": "minecraft:empty"})
        write(DATA / f"worldgen/structure/{name}.json", {"type": "minecraft:jigsaw", "biomes": "minecraft:deep_dark", "max_distance_from_center": 80, "size": 1,
              "spawn_overrides": {}, "start_height": {"absolute": y}, "start_pool": i(name), "step": "underground_structures", "terrain_adaptation": "none", "use_expansion_hack": False})
        write(DATA / f"worldgen/structure_set/{name}s.json", {"placement": {"type": "minecraft:random_spread", "salt": salt, "separation": sep, "spacing": spacing},
              "structures": [{"structure": i(name), "weight": 1}]})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    textures()
    assets()
    simple("silence_incense")
    data()
    import structures_v53
    print(structures_v53.build())
