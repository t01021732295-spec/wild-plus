"""Wild Plus 리소스(JSON) 생성기.

    python3 tools/gen_data.py

모델·블록상태·번역·레시피·루트테이블·루트 수정자·태그를 src/main/resources 아래에 만든다.
"""
import json
import shutil
from pathlib import Path

NS = "wildplus"
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS


def write(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def i(name):
    return f"{NS}:{name}"


# ───────────────────── 아이템 목록 ─────────────────────
FLAT_ITEMS = [
    "blood_shard", "bloodsteel_ingot",
    "bloodsteel_helmet", "bloodsteel_chestplate", "bloodsteel_leggings", "bloodsteel_boots",
    "recall_scroll", "lucky_charm", "traveler_ration",
    "chili_seeds", "chili", "spicy_stew", "honey_toast", "raw_skewer", "spicy_skewer",
]
TOOL_ITEMS = ["bloodsteel_sword", "bloodsteel_pickaxe", "bloodsteel_axe", "bloodsteel_shovel"]

LANG_KO = {
    "itemGroup.wildplus": "야생 플러스",
    "item.wildplus.blood_shard": "핏빛 파편",
    "item.wildplus.bloodsteel_ingot": "핏빛 강철 주괴",
    "item.wildplus.bloodsteel_sword": "핏빛 강철 검",
    "item.wildplus.bloodsteel_pickaxe": "핏빛 강철 곡괭이",
    "item.wildplus.bloodsteel_axe": "핏빛 강철 도끼",
    "item.wildplus.bloodsteel_shovel": "핏빛 강철 삽",
    "item.wildplus.bloodsteel_helmet": "핏빛 강철 투구",
    "item.wildplus.bloodsteel_chestplate": "핏빛 강철 흉갑",
    "item.wildplus.bloodsteel_leggings": "핏빛 강철 레깅스",
    "item.wildplus.bloodsteel_boots": "핏빛 강철 부츠",
    "item.wildplus.recall_scroll": "귀환의 두루마리",
    "item.wildplus.lucky_charm": "행운의 부적",
    "item.wildplus.traveler_ration": "여행자의 도시락",
    "item.wildplus.chili_seeds": "고추 씨앗",
    "item.wildplus.chili": "고추",
    "item.wildplus.spicy_stew": "얼큰한 스튜",
    "item.wildplus.honey_toast": "꿀 토스트",
    "item.wildplus.raw_skewer": "생고기 꼬치",
    "item.wildplus.spicy_skewer": "매콤 꼬치구이",
    "block.wildplus.chili_crop": "고추",
    "tooltip.wildplus.blood_shard": "블러드문과 정예 몹이 남기는 붉은 결정",
    "tooltip.wildplus.bloodsteel_sword": "흡혈: 입힌 피해의 20%만큼 회복",
    "tooltip.wildplus.bloodsteel_armor": "4부위 세트: 밤에 몬스터 피해 15% 감소",
    "tooltip.wildplus.recall_scroll": "3초간 읽으면 침대(없으면 월드 스폰)로 귀환",
    "tooltip.wildplus.lucky_charm": "가지고 있으면 행운 효과",
    "bossbar.wildplus.blood_moon": "블러드문",
    "message.wildplus.blood_moon.start": "핏빛 달이 떠오릅니다... 몬스터들이 광폭해집니다!",
    "message.wildplus.blood_moon.end": "핏빛 달이 저물었습니다. 살아남았군요!",
    "message.wildplus.blood_moon.no_sleep": "블러드문이 뜬 밤에는 잠들 수 없습니다!",
    "message.wildplus.recall.success": "두루마리의 힘으로 귀환했습니다.",
    "message.wildplus.recall.fail": "귀환할 장소를 찾지 못했습니다.",
    "entity.wildplus.elite_prefix": "정예 %s",
    "wildplus.configuration.title": "야생 플러스 설정",
    "wildplus.configuration.bloodMoon": "블러드문",
    "wildplus.configuration.bloodMoonChance": "블러드문 확률",
    "wildplus.configuration.bloodMoonGraceDays": "블러드문 유예 기간(일)",
    "wildplus.configuration.bloodMoonExtraSpawns": "추가 출몰 몹 수",
    "wildplus.configuration.eliteMobs": "정예 몹",
    "wildplus.configuration.eliteChance": "정예 몹 확률",
    "commands.wildplus.blood_moon.started": "블러드문을 시작했습니다.",
    "commands.wildplus.blood_moon.stopped": "블러드문을 종료했습니다.",
}

LANG_EN = {
    "itemGroup.wildplus": "Wild Plus",
    "item.wildplus.blood_shard": "Blood Shard",
    "item.wildplus.bloodsteel_ingot": "Bloodsteel Ingot",
    "item.wildplus.bloodsteel_sword": "Bloodsteel Sword",
    "item.wildplus.bloodsteel_pickaxe": "Bloodsteel Pickaxe",
    "item.wildplus.bloodsteel_axe": "Bloodsteel Axe",
    "item.wildplus.bloodsteel_shovel": "Bloodsteel Shovel",
    "item.wildplus.bloodsteel_helmet": "Bloodsteel Helmet",
    "item.wildplus.bloodsteel_chestplate": "Bloodsteel Chestplate",
    "item.wildplus.bloodsteel_leggings": "Bloodsteel Leggings",
    "item.wildplus.bloodsteel_boots": "Bloodsteel Boots",
    "item.wildplus.recall_scroll": "Scroll of Recall",
    "item.wildplus.lucky_charm": "Lucky Charm",
    "item.wildplus.traveler_ration": "Traveler's Ration",
    "item.wildplus.chili_seeds": "Chili Seeds",
    "item.wildplus.chili": "Chili",
    "item.wildplus.spicy_stew": "Spicy Stew",
    "item.wildplus.honey_toast": "Honey Toast",
    "item.wildplus.raw_skewer": "Raw Meat Skewer",
    "item.wildplus.spicy_skewer": "Spicy Skewer",
    "block.wildplus.chili_crop": "Chili",
    "tooltip.wildplus.blood_shard": "A red crystal left by Blood Moons and elite mobs",
    "tooltip.wildplus.bloodsteel_sword": "Lifesteal: heals 20% of damage dealt",
    "tooltip.wildplus.bloodsteel_armor": "Full set: 15% less damage from monsters at night",
    "tooltip.wildplus.recall_scroll": "Read for 3 seconds to return to your bed (or world spawn)",
    "tooltip.wildplus.lucky_charm": "Grants Luck while carried",
    "bossbar.wildplus.blood_moon": "Blood Moon",
    "message.wildplus.blood_moon.start": "The Blood Moon rises... monsters grow frenzied!",
    "message.wildplus.blood_moon.end": "The Blood Moon has set. You survived!",
    "message.wildplus.blood_moon.no_sleep": "You can't sleep during a Blood Moon!",
    "message.wildplus.recall.success": "The scroll carries you home.",
    "message.wildplus.recall.fail": "There is nowhere to return to.",
    "entity.wildplus.elite_prefix": "Elite %s",
    "wildplus.configuration.title": "Wild Plus Settings",
    "wildplus.configuration.bloodMoon": "Blood Moon",
    "wildplus.configuration.bloodMoonChance": "Blood Moon chance",
    "wildplus.configuration.bloodMoonGraceDays": "Blood Moon grace days",
    "wildplus.configuration.bloodMoonExtraSpawns": "Extra spawns",
    "wildplus.configuration.eliteMobs": "Elite Mobs",
    "wildplus.configuration.eliteChance": "Elite mob chance",
    "commands.wildplus.blood_moon.started": "Blood Moon started.",
    "commands.wildplus.blood_moon.stopped": "Blood Moon stopped.",
}

RAW_MEATS = ["minecraft:beef", "minecraft:porkchop", "minecraft:mutton", "minecraft:chicken", "minecraft:rabbit"]
COOKED_MEATS = ["minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_mutton",
                "minecraft:cooked_chicken", "minecraft:cooked_rabbit", "minecraft:cooked_cod", "minecraft:cooked_salmon"]


def shaped(pattern, key, result, count=1, category="equipment"):
    return {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
            "result": {"id": result, "count": count}}


def shapeless(ingredients, result, count=1, category="misc"):
    return {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
            "result": {"id": result, "count": count}}


def cooking(kind, ingredient, result, xp, time):
    return {"type": f"minecraft:{kind}", "category": "food", "ingredient": ingredient,
            "result": {"id": result}, "experience": xp, "cookingtime": time}


def recipes():
    I, S = i("bloodsteel_ingot"), "minecraft:stick"
    r = {
        "bloodsteel_ingot": shapeless(["minecraft:iron_ingot"] + [i("blood_shard")] * 4, I),
        "bloodsteel_sword": shaped(["X", "X", "#"], {"X": I, "#": S}, i("bloodsteel_sword")),
        "bloodsteel_pickaxe": shaped(["XXX", " # ", " # "], {"X": I, "#": S}, i("bloodsteel_pickaxe")),
        "bloodsteel_axe": shaped(["XX", "X#", " #"], {"X": I, "#": S}, i("bloodsteel_axe")),
        "bloodsteel_shovel": shaped(["X", "#", "#"], {"X": I, "#": S}, i("bloodsteel_shovel")),
        "bloodsteel_helmet": shaped(["XXX", "X X"], {"X": I}, i("bloodsteel_helmet")),
        "bloodsteel_chestplate": shaped(["X X", "XXX", "XXX"], {"X": I}, i("bloodsteel_chestplate")),
        "bloodsteel_leggings": shaped(["XXX", "X X", "X X"], {"X": I}, i("bloodsteel_leggings")),
        "bloodsteel_boots": shaped(["X X", "X X"], {"X": I}, i("bloodsteel_boots")),
        "recall_scroll": shapeless(["minecraft:paper", "minecraft:ender_pearl", i("blood_shard"), i("blood_shard")],
                                   i("recall_scroll")),
        "chili_seeds": shapeless([i("chili")], i("chili_seeds"), 2),
        "spicy_stew": shapeless(["minecraft:bowl", i("chili"), i("chili"), "minecraft:baked_potato", "minecraft:carrot"],
                                i("spicy_stew"), category="misc"),
        "honey_toast": shapeless(["minecraft:bread", "minecraft:honey_bottle"], i("honey_toast")),
        "raw_skewer": shapeless(["minecraft:stick", "minecraft:stick", RAW_MEATS, i("chili")], i("raw_skewer"), 2),
        "traveler_ration": shapeless(["minecraft:bread", COOKED_MEATS, "minecraft:dried_kelp", "minecraft:apple"],
                                     i("traveler_ration"), 2),
        "spicy_skewer_smelting": cooking("smelting", i("raw_skewer"), i("spicy_skewer"), 0.35, 200),
        "spicy_skewer_smoking": cooking("smoking", i("raw_skewer"), i("spicy_skewer"), 0.35, 100),
        "spicy_skewer_campfire": cooking("campfire_cooking", i("raw_skewer"), i("spicy_skewer"), 0.35, 600),
    }
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def item_entry(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["modifier"] = {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}
    return e


def loot():
    # 26.x 루트 테이블 형식: condition(단일 객체) / modifier / match_block
    ripe = {"type": "minecraft:match_block", "blocks": i("chili_crop"), "state": {"age": "7"}}
    write(DATA / "loot_table/blocks/chili_crop.json", {
        "type": "minecraft:block",
        "modifier": {"type": "minecraft:explosion_decay"},
        "pools": [
            # 다 자랐으면 고추 1~3개, 아니면 씨앗 1개
            {"rolls": 1, "entries": [{
                "type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": i("chili"), "condition": ripe,
                     "modifier": {"type": "minecraft:set_count",
                                  "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}},
                    {"type": "minecraft:item", "name": i("chili_seeds")},
                ]}]},
            # 다 자랐으면 씨앗 + 행운 보너스
            {"rolls": 1, "condition": ripe, "entries": [{
                "type": "minecraft:item", "name": i("chili_seeds"),
                "modifier": {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                             "formula": "minecraft:binomial_with_bonus_count",
                             "parameters": {"extra": 1, "probability": 0.5714286}}}]},
        ],
        "random_sequence": i("blocks/chili_crop"),
    })
    # 상자에 추가로 굴리는 보물 테이블
    write(DATA / "loot_table/inject/treasure_common.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
            {"type": "minecraft:empty", "weight": 30},
            item_entry(i("blood_shard"), 20, 1, 4),
            item_entry(i("chili_seeds"), 15, 2, 5),
            item_entry(i("traveler_ration"), 15, 1, 3),
            item_entry(i("recall_scroll"), 8),
            item_entry(i("bloodsteel_ingot"), 4),
            item_entry(i("lucky_charm"), 1),
        ]}],
        "random_sequence": i("inject/treasure_common"),
    })
    write(DATA / "loot_table/inject/treasure_rare.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
            {"type": "minecraft:empty", "weight": 10},
            item_entry(i("blood_shard"), 20, 2, 6),
            item_entry(i("recall_scroll"), 15, 1, 2),
            item_entry(i("bloodsteel_ingot"), 10, 1, 2),
            item_entry(i("traveler_ration"), 10, 2, 4),
            item_entry(i("lucky_charm"), 4),
        ]}],
        "random_sequence": i("inject/treasure_rare"),
    })
    write(DATA / "loot_table/inject/grass_seeds.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1,
                   "condition": {"type": "minecraft:all_of", "terms": [
                       {"type": "minecraft:random_chance", "chance": 0.08},
                       {"type": "minecraft:inverted", "term": "minecraft:tool/can_shear"},
                   ]},
                   "entries": [{"type": "minecraft:item", "name": i("chili_seeds")}]}],
        "random_sequence": i("inject/grass_seeds"),
    })


def any_table(*tables):
    terms = [{"type": "neoforge:loot_table_id", "loot_table_id": t} for t in tables]
    return terms[0] if len(terms) == 1 else {"type": "minecraft:any_of", "terms": terms}


def modifiers():
    common = ["minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
              "minecraft:chests/shipwreck_supply", "minecraft:chests/ruined_portal",
              "minecraft:chests/village/village_weaponsmith", "minecraft:chests/igloo_chest",
              "minecraft:chests/pillager_outpost", "minecraft:chests/underwater_ruin_small",
              "minecraft:chests/underwater_ruin_big"]
    rare = ["minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple",
            "minecraft:chests/buried_treasure", "minecraft:chests/shipwreck_treasure",
            "minecraft:chests/woodland_mansion", "minecraft:chests/ancient_city",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/bastion_treasure",
            "minecraft:chests/end_city_treasure"]
    write(DATA / "loot_modifiers/treasure_common.json",
          {"type": "neoforge:add_table", "condition": any_table(*common), "table": i("inject/treasure_common")})
    write(DATA / "loot_modifiers/treasure_rare.json",
          {"type": "neoforge:add_table", "condition": any_table(*rare), "table": i("inject/treasure_rare")})
    write(DATA / "loot_modifiers/grass_seeds.json",
          {"type": "neoforge:add_table",
           "condition": any_table("minecraft:blocks/short_grass", "minecraft:blocks/tall_grass", "minecraft:blocks/fern"),
           "table": i("inject/grass_seeds")})


def tags():
    write(DATA / "tags/item/bloodsteel_repair.json", {"values": [i("bloodsteel_ingot")]})
    mc = RES / "data/minecraft/tags"
    write(mc / "item/swords.json", {"replace": False, "values": [i("bloodsteel_sword")]})
    write(mc / "item/pickaxes.json", {"replace": False, "values": [i("bloodsteel_pickaxe")]})
    write(mc / "item/axes.json", {"replace": False, "values": [i("bloodsteel_axe")]})
    write(mc / "item/shovels.json", {"replace": False, "values": [i("bloodsteel_shovel")]})
    write(mc / "item/head_armor.json", {"replace": False, "values": [i("bloodsteel_helmet")]})
    write(mc / "item/chest_armor.json", {"replace": False, "values": [i("bloodsteel_chestplate")]})
    write(mc / "item/leg_armor.json", {"replace": False, "values": [i("bloodsteel_leggings")]})
    write(mc / "item/foot_armor.json", {"replace": False, "values": [i("bloodsteel_boots")]})
    write(mc / "item/villager_plantable_seeds.json", {"replace": False, "values": [i("chili_seeds")]})
    write(mc / "block/crops.json", {"replace": False, "values": [i("chili_crop")]})
    write(mc / "block/maintains_farmland.json", {"replace": False, "values": [i("chili_crop")]})
    write(mc / "block/bee_growables.json", {"replace": False, "values": [i("chili_crop")]})


def assets():
    for name in FLAT_ITEMS:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    for name in TOOL_ITEMS:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/handheld", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    write(ASSETS / "blockstates/chili_crop.json",
          {"variants": {f"age={a}": {"model": i(f"block/chili_crop_stage{s}")} for a, s in stage.items()}})
    for s in range(4):
        write(ASSETS / f"models/block/chili_crop_stage{s}.json",
              {"parent": "minecraft:block/crop",
               "textures": {"crop": i(f"block/chili_crop_stage{s}")}})
    write(ASSETS / "equipment/bloodsteel.json", {"layers": {
        "humanoid": [{"texture": i("bloodsteel")}],
        "humanoid_leggings": [{"texture": i("bloodsteel")}],
    }})
    write(ASSETS / "lang/ko_kr.json", LANG_KO)
    write(ASSETS / "lang/en_us.json", LANG_EN)


def main():
    for d in [DATA, RES / "data/minecraft", ASSETS / "models", ASSETS / "items", ASSETS / "blockstates",
              ASSETS / "lang", ASSETS / "equipment"]:
        if d.exists():
            shutil.rmtree(d)
    assets()
    recipes()
    loot()
    modifiers()
    tags()
    import data_v2
    data_v2.build(LANG_KO, LANG_EN)
    import data_v3
    data_v3.build({**LANG_KO, **data_v2.LANG_KO}, {**LANG_EN, **data_v2.LANG_EN})
    import data_v4
    data_v4.build({**LANG_KO, **data_v2.LANG_KO, **data_v3.LANG_KO}, {**LANG_EN, **data_v2.LANG_EN, **data_v3.LANG_EN})
    import data_v5
    data_v5.build({**LANG_KO, **data_v2.LANG_KO, **data_v3.LANG_KO, **data_v4.LANG_KO},
                  {**LANG_EN, **data_v2.LANG_EN, **data_v3.LANG_EN, **data_v4.LANG_EN})
    import data_v6
    data_v6.build({**LANG_KO, **data_v2.LANG_KO, **data_v3.LANG_KO, **data_v4.LANG_KO, **data_v5.LANG_KO},
                  {**LANG_EN, **data_v2.LANG_EN, **data_v3.LANG_EN, **data_v4.LANG_EN, **data_v5.LANG_EN})
    import data_v7
    data_v7.build({**LANG_KO, **data_v2.LANG_KO, **data_v3.LANG_KO, **data_v4.LANG_KO, **data_v5.LANG_KO, **data_v6.LANG_KO},
                  {**LANG_EN, **data_v2.LANG_EN, **data_v3.LANG_EN, **data_v4.LANG_EN, **data_v5.LANG_EN, **data_v6.LANG_EN})
    import data_v8
    from data_v2 import ASSETS as _A
    data_v8.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v9
    data_v9.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import structures_v5
    print("big town", structures_v5.big_town(), "hideout", structures_v5.hideout())
    import data_v10
    data_v10.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import structures_v6
    print("dungeon", structures_v6.dungeon())
    import data_v11
    data_v11.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v12
    data_v12.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v13
    data_v13.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v14
    data_v14.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v15
    data_v15.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v16
    data_v16.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v17
    data_v17.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v18
    data_v18.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v19
    data_v19.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v20
    data_v20.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v21
    data_v21.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v22
    data_v22.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v23
    data_v23.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v24
    data_v24.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v25
    data_v25.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v26
    data_v26.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v27
    data_v27.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v28
    data_v28.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v29
    data_v29.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v30
    data_v30.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v31
    data_v31.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v32
    data_v32.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v33
    data_v33.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v34
    data_v34.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v35
    data_v35.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v36
    data_v36.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import data_v37
    data_v37.build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    import structures_v40
    structures_v40.build()
    n = sum(1 for _ in RES.rglob("*.json"))
    print("ok", n, "json files")


if __name__ == "__main__":
    main()
