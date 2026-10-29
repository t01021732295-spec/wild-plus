"""3차 업데이트 데이터: 화폐·상인·자판기·몹·구조물."""
from data_v2 import ASSETS, DATA, MC, RES, i, merge_tag, write
from data_v2 import entry, shaped, shapeless

LANG_KO = {
    "item.wildplus.copper_coin": "동화",
    "item.wildplus.silver_coin": "은화",
    "item.wildplus.gold_coin": "금화",
    "item.wildplus.coin_pouch": "동전 주머니",
    "block.wildplus.vending_machine": "동전 자판기",
    "item.wildplus.merchant_spawn_egg": "잡화 상인 생성 알",
    "item.wildplus.blacksmith_merchant_spawn_egg": "대장장이 상인 생성 알",
    "item.wildplus.bandit_spawn_egg": "도적 생성 알",
    "item.wildplus.golden_slime_spawn_egg": "황금 슬라임 생성 알",
    "entity.wildplus.merchant": "잡화 상인",
    "entity.wildplus.blacksmith_merchant": "대장장이 상인",
    "entity.wildplus.bandit": "도적",
    "entity.wildplus.golden_slime": "황금 슬라임",
    "wildplus.coins.amount": "%s동 (금화 %s · 은화 %s · 동화 %s)",
    "tooltip.wildplus.copper_coin": "1동 · 은화 1 = 동화 10",
    "tooltip.wildplus.silver_coin": "10동 · 제작대에서 동화 10개로 바꿀 수 있음",
    "tooltip.wildplus.gold_coin": "100동 · 제작대에서 은화 10개로 바꿀 수 있음",
    "tooltip.wildplus.coin_pouch": "우클릭: 동전 넣기 · 웅크리고 우클릭: 꺼내기(자동 환전)",
    "tooltip.wildplus.pouch_value": "보관: %s",
    "tooltip.wildplus.vending_machine": "물건을 넣고 동전 가격을 정해 두면 다른 사람이 사 갈 수 있음",
    "message.wildplus.pouch.deposit": "넣음 %s → 보관 %s",
    "message.wildplus.pouch.withdraw": "꺼냄 %s",
    "message.wildplus.bandit.stole": "도적이 동전 %s동어치를 훔쳤습니다! 처치하면 되찾을 수 있어요",
    "message.wildplus.vending.price_set": "가격을 1개당 %s 으로 정했습니다",
    "message.wildplus.vending.stocked": "%s %s개 보충 (재고 %s)",
    "message.wildplus.vending.other_product": "이 자판기에는 이미 %s 이(가) 들어 있어요. 먼저 재고를 비워 주세요",
    "message.wildplus.vending.unstocked": "재고 %s개를 회수했습니다",
    "message.wildplus.vending.nothing": "(비어 있음)",
    "message.wildplus.vending.status_owner": "상품 %s · 재고 %s · 가격 %s · 이번에 회수한 수익 %s",
    "message.wildplus.vending.sold_out": "품절입니다",
    "message.wildplus.vending.no_money": "%s: 1개 %s · 가진 돈 %s — 돈이 부족해요",
    "message.wildplus.vending.bought": "%s %s개 구매 (%s 지불) · 남은 돈 %s",
}
LANG_EN = {
    "item.wildplus.copper_coin": "Copper Coin",
    "item.wildplus.silver_coin": "Silver Coin",
    "item.wildplus.gold_coin": "Gold Coin",
    "item.wildplus.coin_pouch": "Coin Pouch",
    "block.wildplus.vending_machine": "Coin Vending Machine",
    "item.wildplus.merchant_spawn_egg": "General Merchant Spawn Egg",
    "item.wildplus.blacksmith_merchant_spawn_egg": "Blacksmith Merchant Spawn Egg",
    "item.wildplus.bandit_spawn_egg": "Bandit Spawn Egg",
    "item.wildplus.golden_slime_spawn_egg": "Golden Slime Spawn Egg",
    "entity.wildplus.merchant": "General Merchant",
    "entity.wildplus.blacksmith_merchant": "Blacksmith Merchant",
    "entity.wildplus.bandit": "Bandit",
    "entity.wildplus.golden_slime": "Golden Slime",
    "wildplus.coins.amount": "%s c (%s gold · %s silver · %s copper)",
    "tooltip.wildplus.copper_coin": "Worth 1 · 10 copper = 1 silver",
    "tooltip.wildplus.silver_coin": "Worth 10 · can be crafted into 10 copper",
    "tooltip.wildplus.gold_coin": "Worth 100 · can be crafted into 10 silver",
    "tooltip.wildplus.coin_pouch": "Right-click: deposit coins · Sneak: withdraw (auto change)",
    "tooltip.wildplus.pouch_value": "Holds: %s",
    "tooltip.wildplus.vending_machine": "Stock it and set a coin price; others can buy from it",
    "message.wildplus.pouch.deposit": "Deposited %s → holding %s",
    "message.wildplus.pouch.withdraw": "Withdrew %s",
    "message.wildplus.bandit.stole": "A bandit stole %s worth of coins! Defeat it to get them back",
    "message.wildplus.vending.price_set": "Price set to %s each",
    "message.wildplus.vending.stocked": "Stocked %s x%s (stock %s)",
    "message.wildplus.vending.other_product": "This machine already sells %s. Empty it first",
    "message.wildplus.vending.unstocked": "Took back %s items",
    "message.wildplus.vending.nothing": "(empty)",
    "message.wildplus.vending.status_owner": "Product %s · stock %s · price %s · earnings collected %s",
    "message.wildplus.vending.sold_out": "Sold out",
    "message.wildplus.vending.no_money": "%s: %s each · you have %s — not enough money",
    "message.wildplus.vending.bought": "Bought %s x%s (paid %s) · left %s",
}

FLAT = ["copper_coin", "silver_coin", "gold_coin", "coin_pouch", "merchant_spawn_egg",
        "blacksmith_merchant_spawn_egg", "bandit_spawn_egg", "golden_slime_spawn_egg"]


def assets():
    for name in FLAT:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    write(ASSETS / "models/block/vending_machine.json", {
        "parent": "minecraft:block/orientable",
        "textures": {"front": i("block/vending_machine_front"), "side": i("block/vending_machine_side"),
                     "top": i("block/vending_machine_top")}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    write(ASSETS / "blockstates/vending_machine.json", {"variants": {
        f"facing={d}": ({"model": i("block/vending_machine"), "y": r} if r else {"model": i("block/vending_machine")})
        for d, r in rot.items()}})
    write(ASSETS / "items/vending_machine.json", {"model": {"type": "minecraft:model", "model": i("block/vending_machine")}})


def recipes():
    r = {
        "copper_coin_from_silver": shapeless([i("silver_coin")], i("copper_coin"), 10),
        "silver_coin_from_gold": shapeless([i("gold_coin")], i("silver_coin"), 10),
        "coin_pouch": shaped([" S ", "L L", " L "], {"S": "minecraft:string", "L": "minecraft:leather"},
                             i("coin_pouch"), category="misc"),
        "vending_machine": shaped(["IGI", "ICI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane",
                                                           "C": i("copper_coin"), "R": "minecraft:redstone"},
                                  i("vending_machine"), category="misc"),
    }
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


def loot():
    write(DATA / "loot_table/blocks/vending_machine.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
         "entries": [{"type": "minecraft:item", "name": i("vending_machine")}]}],
        "random_sequence": i("blocks/vending_machine")})
    write(DATA / "loot_table/entities/bandit.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:killed_by_player"},
         "entries": [{"type": "minecraft:item", "name": i("copper_coin"), "modifier": count(2, 6)}]},
        {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:killed_by_player"}, {"type": "minecraft:random_chance", "chance": 0.35}]},
         "entries": [{"type": "minecraft:item", "name": i("silver_coin")}]},
        {"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": 0.15},
         "entries": [{"type": "minecraft:item", "name": "minecraft:emerald"}]},
    ], "random_sequence": i("entities/bandit")})
    write(DATA / "loot_table/entities/golden_slime.json", {"type": "minecraft:entity", "pools": [],
                                                          "random_sequence": i("entities/golden_slime")})
    write(DATA / "loot_table/chests/trading_post.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry(i("copper_coin"), 25, 3, 12), entry(i("silver_coin"), 10, 1, 3),
            entry("minecraft:bread", 15, 2, 5), entry(i("tomato_seeds"), 10, 2, 5),
            entry(i("strawberry"), 10, 2, 5), entry(i("chili_seeds"), 10, 2, 5),
            entry(i("fertilizer"), 8, 2, 4), entry("minecraft:emerald", 8, 1, 3),
            entry(i("traveler_ration"), 8, 1, 2), entry(i("coin_pouch"), 3)]}],
        "random_sequence": i("chests/trading_post")})
    write(DATA / "loot_table/chests/bandit_camp.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry(i("copper_coin"), 25, 5, 20), entry(i("silver_coin"), 18, 2, 6), entry(i("gold_coin"), 4, 1, 2),
            entry("minecraft:emerald", 12, 1, 4), entry("minecraft:iron_ingot", 12, 2, 6),
            entry("minecraft:gold_ingot", 8, 1, 4), entry(i("blood_shard"), 8, 1, 4),
            entry(i("recall_scroll"), 6), entry("minecraft:iron_sword", 5), entry(i("lucky_charm"), 1)]}],
        "random_sequence": i("chests/bandit_camp")})
    # 기존 상자 보물에 동전 추가
    write(DATA / "loot_table/inject/treasure_v3.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 30},
            entry(i("copper_coin"), 40, 3, 15), entry(i("silver_coin"), 20, 1, 4), entry(i("gold_coin"), 3)]}],
        "random_sequence": i("inject/treasure_v3")})
    tables = ["minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
              "minecraft:chests/shipwreck_treasure", "minecraft:chests/buried_treasure", "minecraft:chests/ruined_portal",
              "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple", "minecraft:chests/pillager_outpost",
              "minecraft:chests/woodland_mansion", "minecraft:chests/stronghold_corridor",
              "minecraft:chests/village/village_plains_house", "minecraft:chests/village/village_taiga_house"]
    write(DATA / "loot_modifiers/treasure_v3.json", {
        "type": "neoforge:add_table",
        "condition": {"type": "minecraft:any_of",
                      "terms": [{"type": "neoforge:loot_table_id", "loot_table_id": t} for t in tables]},
        "table": i("inject/treasure_v3")})


TRADING_BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna",
                  "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:taiga",
                  "minecraft:snowy_plains", "minecraft:desert", "minecraft:cherry_grove"]
BANDIT_BIOMES = ["minecraft:plains", "minecraft:forest", "minecraft:dark_forest", "minecraft:birch_forest",
                 "minecraft:taiga", "minecraft:old_growth_spruce_taiga", "minecraft:savanna",
                 "minecraft:savanna_plateau", "minecraft:badlands", "minecraft:wooded_badlands"]


def structure(name, biomes_tag):
    write(DATA / f"worldgen/structure/{name}.json", {
        "type": "minecraft:jigsaw", "biomes": biomes_tag, "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 1, "spawn_overrides": {},
        "start_height": {"absolute": 0}, "start_pool": i(name), "step": "surface_structures",
        "terrain_adaptation": "beard_thin", "use_expansion_hack": False})
    write(DATA / f"worldgen/template_pool/{name}.json", {
        "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i(name),
                                  "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}],
        "fallback": "minecraft:empty"})


def worldgen():
    write(DATA / "tags/worldgen/biome/has_structure/trading_post.json", {"values": TRADING_BIOMES})
    write(DATA / "tags/worldgen/biome/has_structure/bandit_camp.json", {"values": BANDIT_BIOMES})
    write(DATA / "tags/worldgen/biome/has_bandits.json", {"values": BANDIT_BIOMES})
    structure("trading_post", "#wildplus:has_structure/trading_post")
    structure("bandit_camp", "#wildplus:has_structure/bandit_camp")
    write(DATA / "worldgen/structure_set/trading_posts.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 581239471, "separation": 8, "spacing": 26},
        "structures": [{"structure": i("trading_post"), "weight": 1}]})
    write(DATA / "worldgen/structure_set/bandit_camps.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 772051993, "separation": 10, "spacing": 30},
        "structures": [{"structure": i("bandit_camp"), "weight": 1}]})
    write(DATA / "neoforge/biome_modifier/add_bandits.json", {
        "type": "neoforge:add_spawns", "biomes": "#wildplus:has_bandits",
        "spawners": {"type": i("bandit"), "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3},
                     "weight": 12}})
    write(DATA / "neoforge/biome_modifier/add_golden_slime.json", {
        "type": "neoforge:add_spawns", "biomes": "#minecraft:is_overworld",
        "spawners": {"type": i("golden_slime"), "count": 1, "weight": 4}})


def tags():
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("vending_machine")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    recipes()
    loot()
    worldgen()
    tags()
    import structures_v3
    structures_v3.build()
