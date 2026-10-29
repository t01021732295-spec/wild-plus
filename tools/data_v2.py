"""2차 업데이트 데이터: 광석·도구·낚시·농사.

gen_data.py 의 main() 이 마지막에 build(LANG_KO, LANG_EN) 을 호출한다.
"""
import json
from pathlib import Path

NS = "wildplus"
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS
MC = RES / "data/minecraft"


def write(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def i(name):
    return f"{NS}:{name}"


def merge_tag(path: Path, values):
    """기존 태그 파일(1차 데이터)이 있으면 값만 덧붙인다."""
    old = json.loads(path.read_text()) if path.exists() else {"replace": False, "values": []}
    for v in values:
        if v not in old["values"]:
            old["values"].append(v)
    write(path, old)


# ───────────────────── 이름·설명 ─────────────────────
LANG_KO = {
    "block.wildplus.mythril_ore": "미스릴 광석",
    "block.wildplus.deepslate_mythril_ore": "심층암 미스릴 광석",
    "block.wildplus.mythril_block": "미스릴 블록",
    "block.wildplus.jade_ore": "비취 광석",
    "block.wildplus.deepslate_jade_ore": "심층암 비취 광석",
    "block.wildplus.jade_block": "비취 블록",
    "block.wildplus.tomato_crop": "토마토",
    "block.wildplus.strawberry_crop": "딸기",
    "block.wildplus.sprinkler": "스프링클러",
    "item.wildplus.raw_mythril": "미스릴 원석",
    "item.wildplus.mythril_ingot": "미스릴 주괴",
    "item.wildplus.jade": "비취",
    "item.wildplus.mythril_sword": "미스릴 검",
    "item.wildplus.mythril_pickaxe": "미스릴 곡괭이",
    "item.wildplus.mythril_axe": "미스릴 도끼",
    "item.wildplus.mythril_shovel": "미스릴 삽",
    "item.wildplus.mythril_hoe": "미스릴 괭이",
    "item.wildplus.jade_fishing_rod": "비취 낚싯대",
    "item.wildplus.rainbow_trout": "무지개송어",
    "item.wildplus.cooked_rainbow_trout": "구운 무지개송어",
    "item.wildplus.golden_carp": "황금 잉어",
    "item.wildplus.moonlight_fish": "달빛 물고기",
    "item.wildplus.storm_eel": "폭풍 장어",
    "item.wildplus.grilled_eel": "장어구이",
    "item.wildplus.anglerfish": "심해 아귀",
    "item.wildplus.ice_smelt": "얼음 빙어",
    "item.wildplus.sunken_chest": "침몰한 보물상자",
    "item.wildplus.sashimi_platter": "모둠회",
    "item.wildplus.fish_stew": "매운탕",
    "item.wildplus.tomato_seeds": "토마토 씨앗",
    "item.wildplus.tomato": "토마토",
    "item.wildplus.strawberry": "딸기",
    "item.wildplus.fertilizer": "비료",
    "item.wildplus.tomato_soup": "토마토 수프",
    "item.wildplus.pizza": "피자 조각",
    "item.wildplus.strawberry_tart": "딸기 타르트",
    "tooltip.wildplus.mythril_pickaxe": "다이아몬드보다 빠른 채굴 속도",
    "tooltip.wildplus.mythril_hoe": "3x3 범위를 한 번에 경작 (웅크리면 한 칸)",
    "tooltip.wildplus.jade_fishing_rod": "희귀 어종·보물상자 확률 2배, 가끔 두 마리",
    "tooltip.wildplus.rainbow_trout": "강에서, 특히 낮에 잘 잡힘",
    "tooltip.wildplus.golden_carp": "어디서나 아주 드물게 잡히는 행운의 물고기",
    "tooltip.wildplus.moonlight_fish": "밤에만 잡힘",
    "tooltip.wildplus.storm_eel": "비 오는 날에 잡힘 (뇌우 때 더 잘)",
    "tooltip.wildplus.anglerfish": "바다에서 잡힘 (밤에 더 잘)",
    "tooltip.wildplus.ice_smelt": "추운 지역에서 잡힘",
    "tooltip.wildplus.sunken_chest": "우클릭으로 열기",
    "tooltip.wildplus.sprinkler": "주변 9x9 경작지를 적시고 작물 성장을 3~4배 빠르게",
    "tooltip.wildplus.fertilizer": "주변 3x3 작물·묘목에 뼛가루 2배 효과",
    "tooltip.wildplus.tomato_seeds": "잔디에서 가끔 나옴",
    "tooltip.wildplus.strawberry": "밭에 바로 심을 수 있음",
}
LANG_EN = {
    "block.wildplus.mythril_ore": "Mythril Ore",
    "block.wildplus.deepslate_mythril_ore": "Deepslate Mythril Ore",
    "block.wildplus.mythril_block": "Block of Mythril",
    "block.wildplus.jade_ore": "Jade Ore",
    "block.wildplus.deepslate_jade_ore": "Deepslate Jade Ore",
    "block.wildplus.jade_block": "Block of Jade",
    "block.wildplus.tomato_crop": "Tomatoes",
    "block.wildplus.strawberry_crop": "Strawberries",
    "block.wildplus.sprinkler": "Sprinkler",
    "item.wildplus.raw_mythril": "Raw Mythril",
    "item.wildplus.mythril_ingot": "Mythril Ingot",
    "item.wildplus.jade": "Jade",
    "item.wildplus.mythril_sword": "Mythril Sword",
    "item.wildplus.mythril_pickaxe": "Mythril Pickaxe",
    "item.wildplus.mythril_axe": "Mythril Axe",
    "item.wildplus.mythril_shovel": "Mythril Shovel",
    "item.wildplus.mythril_hoe": "Mythril Hoe",
    "item.wildplus.jade_fishing_rod": "Jade Fishing Rod",
    "item.wildplus.rainbow_trout": "Rainbow Trout",
    "item.wildplus.cooked_rainbow_trout": "Cooked Rainbow Trout",
    "item.wildplus.golden_carp": "Golden Carp",
    "item.wildplus.moonlight_fish": "Moonlight Fish",
    "item.wildplus.storm_eel": "Storm Eel",
    "item.wildplus.grilled_eel": "Grilled Eel",
    "item.wildplus.anglerfish": "Anglerfish",
    "item.wildplus.ice_smelt": "Ice Smelt",
    "item.wildplus.sunken_chest": "Sunken Chest",
    "item.wildplus.sashimi_platter": "Sashimi Platter",
    "item.wildplus.fish_stew": "Spicy Fish Stew",
    "item.wildplus.tomato_seeds": "Tomato Seeds",
    "item.wildplus.tomato": "Tomato",
    "item.wildplus.strawberry": "Strawberry",
    "item.wildplus.fertilizer": "Fertilizer",
    "item.wildplus.tomato_soup": "Tomato Soup",
    "item.wildplus.pizza": "Pizza Slice",
    "item.wildplus.strawberry_tart": "Strawberry Tart",
    "tooltip.wildplus.mythril_pickaxe": "Mines faster than diamond",
    "tooltip.wildplus.mythril_hoe": "Tills a 3x3 area (sneak for one block)",
    "tooltip.wildplus.jade_fishing_rod": "Double rare-fish and treasure chance, sometimes two fish",
    "tooltip.wildplus.rainbow_trout": "Found in rivers, especially by day",
    "tooltip.wildplus.golden_carp": "A lucky fish, very rarely caught anywhere",
    "tooltip.wildplus.moonlight_fish": "Only bites at night",
    "tooltip.wildplus.storm_eel": "Bites in the rain (more during thunderstorms)",
    "tooltip.wildplus.anglerfish": "Found in oceans (more at night)",
    "tooltip.wildplus.ice_smelt": "Found in cold biomes",
    "tooltip.wildplus.sunken_chest": "Right-click to open",
    "tooltip.wildplus.sprinkler": "Waters farmland in a 9x9 area and makes crops grow 3-4x faster",
    "tooltip.wildplus.fertilizer": "Double bone meal on crops and saplings in a 3x3 area",
    "tooltip.wildplus.tomato_seeds": "Sometimes found in grass",
    "tooltip.wildplus.strawberry": "Can be planted directly",
}

ORES = ["mythril_ore", "deepslate_mythril_ore", "jade_ore", "deepslate_jade_ore"]
STORAGE = ["mythril_block", "jade_block"]
FLAT = ["raw_mythril", "mythril_ingot", "jade", "rainbow_trout", "cooked_rainbow_trout", "golden_carp",
        "moonlight_fish", "storm_eel", "grilled_eel", "anglerfish", "ice_smelt", "sunken_chest", "sashimi_platter",
        "fish_stew", "tomato_seeds", "tomato", "strawberry", "fertilizer", "tomato_soup", "pizza", "strawberry_tart"]
TOOLS = ["mythril_sword", "mythril_pickaxe", "mythril_axe", "mythril_shovel", "mythril_hoe"]
RAW_FISH = [i("rainbow_trout"), i("ice_smelt"), i("anglerfish"), i("golden_carp"), i("moonlight_fish"),
            "minecraft:cod", "minecraft:salmon"]


def assets():
    for name in ORES + STORAGE:
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": i(f"block/{name}")}}})
        write(ASSETS / f"models/block/{name}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{name}")}})
    for name in FLAT:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    for name in TOOLS:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/handheld", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    # 낚싯대: 던졌을 때 모양이 바뀜
    for name in ["jade_fishing_rod", "jade_fishing_rod_cast"]:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": i(f"item/{name}")}})
    write(ASSETS / "items/jade_fishing_rod.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
        "on_false": {"type": "minecraft:model", "model": i("item/jade_fishing_rod")},
        "on_true": {"type": "minecraft:model", "model": i("item/jade_fishing_rod_cast")}}})
    # 작물
    stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    for crop in ["tomato_crop", "strawberry_crop"]:
        write(ASSETS / f"blockstates/{crop}.json",
              {"variants": {f"age={a}": {"model": i(f"block/{crop}_stage{s}")} for a, s in stage.items()}})
        for s in range(4):
            write(ASSETS / f"models/block/{crop}_stage{s}.json",
                  {"parent": "minecraft:block/crop", "textures": {"crop": i(f"block/{crop}_stage{s}")}})
    # 스프링클러: 기둥 + 헤드
    write(ASSETS / "blockstates/sprinkler.json", {"variants": {"": {"model": i("block/sprinkler")}}})
    side, top = i("block/sprinkler_side"), i("block/sprinkler_top")

    def box(frm, to):
        faces = {f: {"texture": "#side"} for f in ["north", "south", "east", "west"]}
        faces["up"] = {"texture": "#top"}
        faces["down"] = {"texture": "#side"}
        return {"from": frm, "to": to, "faces": faces}

    write(ASSETS / "models/block/sprinkler.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": side, "side": side, "top": top},
        "elements": [box([5, 0, 5], [11, 2, 11]), box([7, 2, 7], [9, 10, 9]), box([4, 10, 4], [12, 13, 12])],
    })
    write(ASSETS / "items/sprinkler.json", {"model": {"type": "minecraft:model", "model": i("block/sprinkler")}})


# ───────────────────── 루트 테이블 ─────────────────────
def ore_loot(block, drop):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": i(block)},
            {"type": "minecraft:item", "name": drop, "modifier": [
                {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"type": "minecraft:explosion_decay"}]},
        ]}]}], "random_sequence": i(f"blocks/{block}")}


def self_loot(block):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": i(block)}]}], "random_sequence": i(f"blocks/{block}")}


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


def entry(name, weight, lo=1, hi=1, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    mods = []
    if hi > 1:
        mods.append(count(lo, hi))
    if extra:
        mods.append(extra)
    if mods:
        e["modifier"] = mods[0] if len(mods) == 1 else mods
    return e


def loot():
    for b in ["mythril_ore", "deepslate_mythril_ore"]:
        write(DATA / f"loot_table/blocks/{b}.json", ore_loot(b, i("raw_mythril")))
    for b in ["jade_ore", "deepslate_jade_ore"]:
        write(DATA / f"loot_table/blocks/{b}.json", ore_loot(b, i("jade")))
    for b in STORAGE + ["sprinkler"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))

    def ripe(crop):
        return {"type": "minecraft:match_block", "blocks": i(crop), "state": {"age": "7"}}

    write(DATA / "loot_table/blocks/tomato_crop.json", {
        "type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"},
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": i("tomato"), "condition": ripe("tomato_crop"), "modifier": count(2, 4)},
                {"type": "minecraft:item", "name": i("tomato_seeds")}]}]},
            {"rolls": 1, "condition": ripe("tomato_crop"), "entries": [{
                "type": "minecraft:item", "name": i("tomato_seeds"),
                "modifier": {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                             "formula": "minecraft:binomial_with_bonus_count",
                             "parameters": {"extra": 1, "probability": 0.5714286}}}]},
        ], "random_sequence": i("blocks/tomato_crop")})
    write(DATA / "loot_table/blocks/strawberry_crop.json", {
        "type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"},
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i("strawberry")}]},
            {"rolls": 1, "condition": ripe("strawberry_crop"), "entries": [{
                "type": "minecraft:item", "name": i("strawberry"),
                "modifier": [count(1, 3), {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                                           "formula": "minecraft:binomial_with_bonus_count",
                                           "parameters": {"extra": 1, "probability": 0.5714286}}]}]},
        ], "random_sequence": i("blocks/strawberry_crop")})

    write(DATA / "loot_table/gameplay/sunken_chest.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            entry("minecraft:gold_ingot", 20, 2, 6),
            entry(i("jade"), 18, 2, 5),
            entry(i("raw_mythril"), 15, 2, 5),
            entry("minecraft:emerald", 12, 1, 4),
            entry(i("mythril_ingot"), 8, 1, 2),
            entry("minecraft:diamond", 6, 1, 2),
            entry(i("recall_scroll"), 8, 1, 2),
            entry("minecraft:nautilus_shell", 6),
            entry("minecraft:book", 10, extra={"type": "minecraft:enchant_randomly", "options": "#minecraft:on_random_loot"}),
            entry(i("lucky_charm"), 2),
            entry("minecraft:heart_of_the_sea", 1),
        ]}],
        "random_sequence": i("gameplay/sunken_chest"),
    })

    # 잔디 씨앗: 1차의 고추 씨앗 표를 확장 (고추·토마토·딸기)
    write(DATA / "loot_table/inject/grass_seeds.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1,
                   "condition": {"type": "minecraft:all_of", "terms": [
                       {"type": "minecraft:random_chance", "chance": 0.14},
                       {"type": "minecraft:inverted", "term": "minecraft:tool/can_shear"}]},
                   "entries": [entry(i("chili_seeds"), 4), entry(i("tomato_seeds"), 4), entry(i("strawberry"), 2)]}],
        "random_sequence": i("inject/grass_seeds"),
    })
    # 상자 보물에 새 재료 추가: 별도 테이블을 같은 상자들에 한 번 더 굴린다
    write(DATA / "loot_table/inject/treasure_v2.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 40},
            entry(i("raw_mythril"), 15, 1, 4),
            entry(i("jade"), 12, 1, 3),
            entry(i("tomato_seeds"), 10, 2, 4),
            entry(i("strawberry"), 8, 1, 3),
            entry(i("fertilizer"), 8, 2, 5),
            entry(i("mythril_ingot"), 4),
        ]}],
        "random_sequence": i("inject/treasure_v2"),
    })


def modifiers():
    tables = ["minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
              "minecraft:chests/shipwreck_supply", "minecraft:chests/ruined_portal",
              "minecraft:chests/village/village_plains_house", "minecraft:chests/village/village_weaponsmith",
              "minecraft:chests/igloo_chest", "minecraft:chests/pillager_outpost",
              "minecraft:chests/underwater_ruin_big", "minecraft:chests/desert_pyramid",
              "minecraft:chests/jungle_temple", "minecraft:chests/stronghold_corridor"]
    write(DATA / "loot_modifiers/treasure_v2.json", {
        "type": "neoforge:add_table",
        "condition": {"type": "minecraft:any_of",
                      "terms": [{"type": "neoforge:loot_table_id", "loot_table_id": t} for t in tables]},
        "table": i("inject/treasure_v2")})
    write(DATA / "loot_modifiers/fishing.json", {
        "type": i("fishing"),
        "condition": {"type": "neoforge:loot_table_id", "loot_table_id": "minecraft:gameplay/fishing"}})


# ───────────────────── 레시피 ─────────────────────
def shaped(pattern, key, result, n=1, category="equipment"):
    return {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
            "result": {"id": result, "count": n}}


def shapeless(ingredients, result, n=1, category="misc"):
    return {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
            "result": {"id": result, "count": n}}


def cook(kind, ingredient, result, xp, time, category="food"):
    return {"type": f"minecraft:{kind}", "category": category, "ingredient": ingredient,
            "result": {"id": result}, "experience": xp, "cookingtime": time}


def recipes():
    M, S = i("mythril_ingot"), "minecraft:stick"
    r = {
        "mythril_ingot_from_smelting": cook("smelting", i("raw_mythril"), M, 0.8, 200, "misc"),
        "mythril_ingot_from_blasting": cook("blasting", i("raw_mythril"), M, 0.8, 100, "misc"),
        "mythril_ingot_from_ore_smelting": cook("smelting", [i("mythril_ore"), i("deepslate_mythril_ore")], M, 0.8, 200, "misc"),
        "jade_from_ore_smelting": cook("smelting", [i("jade_ore"), i("deepslate_jade_ore")], i("jade"), 1.0, 200, "misc"),
        "mythril_block": shaped(["###", "###", "###"], {"#": M}, i("mythril_block"), category="building"),
        "mythril_ingot_from_block": shapeless([i("mythril_block")], M, 9),
        "jade_block": shaped(["###", "###", "###"], {"#": i("jade")}, i("jade_block"), category="building"),
        "jade_from_block": shapeless([i("jade_block")], i("jade"), 9),
        "mythril_sword": shaped(["X", "X", "#"], {"X": M, "#": S}, i("mythril_sword")),
        "mythril_pickaxe": shaped(["XXX", " # ", " # "], {"X": M, "#": S}, i("mythril_pickaxe")),
        "mythril_axe": shaped(["XX", "X#", " #"], {"X": M, "#": S}, i("mythril_axe")),
        "mythril_shovel": shaped(["X", "#", "#"], {"X": M, "#": S}, i("mythril_shovel")),
        "mythril_hoe": shaped(["XX", " #", " #"], {"X": M, "#": S}, i("mythril_hoe")),
        "jade_fishing_rod": shaped(["  J", " RJ", "  M"], {"J": i("jade"), "R": "minecraft:fishing_rod", "M": M},
                                   i("jade_fishing_rod")),
        "sprinkler": shaped([" J ", "IWI", " I "], {"J": i("jade"), "I": M, "W": "minecraft:water_bucket"},
                            i("sprinkler"), category="misc"),
        "fertilizer": shapeless(["minecraft:bone_meal", "minecraft:bone_meal",
                                 ["minecraft:rotten_flesh", "minecraft:wheat_seeds", "minecraft:kelp", "minecraft:dirt"]],
                                i("fertilizer"), 2),
        "tomato_seeds": shapeless([i("tomato")], i("tomato_seeds"), 2),
        "tomato_soup": shapeless(["minecraft:bowl", i("tomato"), i("tomato"), i("tomato")], i("tomato_soup")),
        "pizza": shapeless(["minecraft:wheat", "minecraft:wheat", i("tomato"),
                            ["minecraft:cooked_porkchop", "minecraft:cooked_beef", "minecraft:cooked_chicken",
                             "minecraft:brown_mushroom", "minecraft:red_mushroom"]], i("pizza"), 3),
        "strawberry_tart": shapeless(["minecraft:wheat", i("strawberry"), i("strawberry"), "minecraft:sugar",
                                      "minecraft:egg"], i("strawberry_tart"), 2),
        "sashimi_platter": shapeless(["minecraft:bowl", RAW_FISH, RAW_FISH], i("sashimi_platter")),
        "fish_stew": shapeless(["minecraft:bowl", RAW_FISH + [i("storm_eel")], i("chili"), i("tomato")], i("fish_stew")),
    }
    for kind, time, suffix in [("smelting", 200, ""), ("smoking", 100, "_from_smoking"), ("campfire_cooking", 600, "_from_campfire")]:
        r["cooked_rainbow_trout" + suffix] = cook(kind, i("rainbow_trout"), i("cooked_rainbow_trout"), 0.35, time)
        r["grilled_eel" + suffix] = cook(kind, i("storm_eel"), i("grilled_eel"), 0.35, time)
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


# ───────────────────── 태그·월드 생성 ─────────────────────
def tags():
    write(DATA / "tags/item/mythril_repair.json", {"values": [i("mythril_ingot")]})
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i(b) for b in ORES + STORAGE + ["sprinkler"]])
    merge_tag(t / "block/needs_iron_tool.json", [i(b) for b in ORES + STORAGE])
    merge_tag(t / "block/crops.json", [i("tomato_crop"), i("strawberry_crop")])
    merge_tag(t / "block/maintains_farmland.json", [i("tomato_crop"), i("strawberry_crop")])
    merge_tag(t / "block/bee_growables.json", [i("tomato_crop"), i("strawberry_crop")])
    merge_tag(t / "item/swords.json", [i("mythril_sword")])
    merge_tag(t / "item/pickaxes.json", [i("mythril_pickaxe")])
    merge_tag(t / "item/axes.json", [i("mythril_axe")])
    merge_tag(t / "item/shovels.json", [i("mythril_shovel")])
    merge_tag(t / "item/hoes.json", [i("mythril_hoe")])
    merge_tag(t / "item/enchantable/fishing.json", [i("jade_fishing_rod")])
    merge_tag(t / "item/enchantable/durability.json", [i("jade_fishing_rod")])
    merge_tag(t / "item/fishes.json", [i(f) for f in ["rainbow_trout", "cooked_rainbow_trout", "golden_carp",
                                                      "moonlight_fish", "storm_eel", "anglerfish", "ice_smelt"]])
    merge_tag(t / "item/villager_plantable_seeds.json", [i("tomato_seeds"), i("strawberry")])


def ore_feature(name, size, discard):
    def target(state, tag, lo, hi):
        return {"state": state, "target": {"predicate_type": "minecraft:any_of", "rules": [
            {"predicate_type": "minecraft:all_of", "rules": [
                {"predicate_type": "minecraft:tag_match", "tag": "minecraft:height_specific_ore_replaceables"},
                {"predicate_type": "minecraft:height_match", "min_inclusive": lo, "max_inclusive": hi}]},
            {"predicate_type": "minecraft:all_of", "rules": [
                {"predicate_type": "minecraft:not", "rule": {"predicate_type": "minecraft:tag_match",
                                                             "tag": "minecraft:height_specific_ore_replaceables"}},
                {"predicate_type": "minecraft:tag_match", "tag": tag}]}]}}

    return {"type": "minecraft:ore", "discard_chance_on_air_exposure": discard, "size": size, "targets": [
        target(i(f"{name}_ore"), "minecraft:stone_ore_replaceables", 0, 2031),
        target(i(f"deepslate_{name}_ore"), "minecraft:deepslate_ore_replaceables", -2032, 8)]}


def placed(feature, count, height):
    return {"feature": feature, "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": height},
        {"type": "minecraft:biome"}]}


def worldgen():
    write(DATA / "worldgen/feature/ore_mythril.json", ore_feature("mythril", 7, 0.2))
    write(DATA / "worldgen/feature/ore_jade.json", ore_feature("jade", 5, 0.0))
    write(DATA / "worldgen/placed_feature/ore_mythril.json", placed(i("ore_mythril"), 6, {
        "type": "minecraft:trapezoid", "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 32}}))
    write(DATA / "worldgen/placed_feature/ore_jade.json", placed(i("ore_jade"), 5, {
        "type": "minecraft:uniform", "min_inclusive": {"absolute": -16}, "max_inclusive": {"absolute": 96}}))
    write(DATA / "neoforge/biome_modifier/add_ores.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": [i("ore_mythril"), i("ore_jade")], "step": "underground_ores"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    loot()
    modifiers()
    recipes()
    tags()
    worldgen()
