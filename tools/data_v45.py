"""5.0 네더 데이터 (data_v45): 겁화 몬스터 4종 이름 · 네더 스폰. (모델은 gen_models_v50.py, 드롭은 event/KalpaGear.onMobDrops)"""
import json

from data_v2 import ASSETS, DATA, i, write

MOBS = {"ember_hound": ("잿불 사냥개", "Ember Hound", 20, 2, 4), "lava_turtle": ("용암 거북", "Lava Turtle", 8, 1, 1),
        "ash_shaman": ("재의 주술사", "Ash Shaman", 8, 1, 1), "obsidian_golem": ("흑요석 골렘", "Obsidian Golem", 3, 1, 1)}


def build(lang_ko, lang_en):
    ko, en, spawners = {}, {}, []
    for n, (k, e, weight, lo, hi) in MOBS.items():
        ko[f"entity.wildplus.{n}"] = k
        en[f"entity.wildplus.{n}"] = e
        spawners.append({"type": i(n), "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}, "weight": weight})
    write(DATA / "neoforge/biome_modifier/add_nether_mobs.json", {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_nether", "spawners": spawners})
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v45")
