"""3.1 데이터: 요리 2차 — 여러 칸 조리 기구 6종 + 요리 41종."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
from data_v18 import simple
import cooking_gen
import kitchen_models

STATIONS = ["cooking_pot", "brick_oven", "bbq_grill", "prep_table", "kitchen_stove", "aging_rack"]
PICKAXE = ["cooking_pot", "brick_oven", "bbq_grill", "kitchen_stove"]
AXE = ["prep_table", "aging_rack"]

RECIPES = {
    "cooking_pot": shaped(["I I", "ICI", "BFB"], {"I": "minecraft:iron_ingot", "C": "minecraft:cauldron", "B": "minecraft:bricks",
                                                   "F": "minecraft:campfire"}, i("cooking_pot"), category="misc"),
    "brick_oven": shaped(["BBB", "BFB", "BSB"], {"B": "minecraft:bricks", "F": "minecraft:furnace", "S": "minecraft:smooth_stone"},
                         i("brick_oven"), category="misc"),
    "bbq_grill": shaped(["BBB", "ICI", "I I"], {"B": "minecraft:iron_bars", "I": "minecraft:iron_ingot", "C": "minecraft:campfire"},
                        i("bbq_grill"), category="misc"),
    "prep_table": shaped(["SKS", "PPP", "L L"], {"S": "#minecraft:wooden_slabs",
                                                  "K": "minecraft:iron_sword", "P": "#minecraft:planks", "L": "#minecraft:logs"},
                         i("prep_table"), category="misc"),
    "kitchen_stove": shaped(["III", "IFI", "QQQ"], {"I": "minecraft:iron_ingot", "F": "minecraft:blast_furnace", "Q": "minecraft:quartz_block"},
                            i("kitchen_stove"), category="misc"),
    "aging_rack": shaped(["PBP", "PBP", "L L"], {"P": "#minecraft:planks", "B": "minecraft:barrel", "L": "#minecraft:logs"},
                         i("aging_rack"), category="misc"),
}


def build(lang_ko, lang_en):
    ko, en = cooking_gen.build()
    for d in cooking_gen.DISHES:
        simple(d)
    kitchen_models.build()
    for s in STATIONS:
        write(DATA / f"loot_table/blocks/{s}.json", self_loot(s))
    for name, obj in RECIPES.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i(s) for s in PICKAXE])
    merge_tag(t / "block/mineable/axe.json", [i(s) for s in AXE])
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})
