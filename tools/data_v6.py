"""6차 데이터: 세금, 체력바, 청크로더, 강화대, 약국, 기상 관측소.
인챈트 최대 레벨 덮어쓰기(5차)는 없애고, 바닐라 최대 이후 레벨은 강화대에서 돈으로만 올린다."""
import shutil

from data_v2 import ASSETS, DATA, MC, i, merge_tag, write
from data_v2 import shaped, shapeless

BLOCKS = ["chunk_loader", "enhance_table", "pharmacy", "weather_station"]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

LANG_KO = {
    "block.wildplus.chunk_loader": "청크로더",
    "block.wildplus.enhance_table": "강화대",
    "block.wildplus.pharmacy": "약국",
    "block.wildplus.weather_station": "기상 관측소",
    "tooltip.wildplus.chunk_loader": "놓인 청크(16×16)를 항상 불러와 둠 — 농장·화로가 멀리서도 돌아감",
    "tooltip.wildplus.enhance_table": "바닐라 최대 레벨 너머의 인챈트를 동전으로 강화 (단계마다 3배) · 동전으로 수리",
    "tooltip.wildplus.pharmacy": "동전으로 효과(신속·성급함·힘·저항·행운 등)를 사는 곳",
    "tooltip.wildplus.weather_station": "동전으로 날씨(맑음·비·뇌우)와 시간(아침·밤)을 바꾸는 곳",
    "container.wildplus.enhance": "강화대",
    "container.wildplus.pharmacy": "약국",
    "container.wildplus.weather_station": "기상 관측소",
    "message.wildplus.tax.income": "자판기 수익 %s동 (세금 %s동) → %s동 받음",
    "message.wildplus.shipping.sold": "출하 상자: %s개 팔림, %s동 (세금 %s동) → 통장에 %s동",
    "message.wildplus.lottery.win": "당첨! %s동 (세금 %s동)",
    "message.wildplus.chunk_loader.on": "청크로더 작동: 청크 [%s, %s] 를 항상 불러와요",
    "message.wildplus.enhance.no_money": "돈이 부족해요 (필요: %s동)",
    "message.wildplus.enhance.done": "강화 성공! %s (%s동)",
    "message.wildplus.service.no_money": "돈이 부족해요 (필요: %s동)",
    "message.wildplus.service.bought": "%s 구매 (%s동)",
    "message.wildplus.service.weather": "%s 님이 기상 관측소에서 [%s] (%s동)",
    "wildplus.configuration.taxEnabled": "소득세",
    "wildplus.configuration.healthBars": "몬스터 체력바",
}
LANG_EN = {
    "block.wildplus.chunk_loader": "Chunk Loader",
    "block.wildplus.enhance_table": "Enhancement Table",
    "block.wildplus.pharmacy": "Pharmacy",
    "block.wildplus.weather_station": "Weather Station",
    "tooltip.wildplus.chunk_loader": "Keeps its chunk (16x16) loaded at all times",
    "tooltip.wildplus.enhance_table": "Pay coins to push enchantments past their vanilla max (x3 per level) · repair for coins",
    "tooltip.wildplus.pharmacy": "Buy status effects with coins",
    "tooltip.wildplus.weather_station": "Buy weather (clear/rain/thunder) and time (day/night) with coins",
    "container.wildplus.enhance": "Enhancement Table",
    "container.wildplus.pharmacy": "Pharmacy",
    "container.wildplus.weather_station": "Weather Station",
    "message.wildplus.tax.income": "Vending income %s (tax %s) → received %s",
    "message.wildplus.shipping.sold": "Shipping bin: %s items sold for %s (tax %s) → %s to your bank",
    "message.wildplus.lottery.win": "You won %s! (tax %s)",
    "message.wildplus.chunk_loader.on": "Chunk loader active: chunk [%s, %s] stays loaded",
    "message.wildplus.enhance.no_money": "Not enough money (need %s)",
    "message.wildplus.enhance.done": "Enhanced! %s (%s)",
    "message.wildplus.service.no_money": "Not enough money (need %s)",
    "message.wildplus.service.bought": "Bought %s (%s)",
    "message.wildplus.service.weather": "%s used the weather station: [%s] (%s)",
    "wildplus.configuration.taxEnabled": "Income tax",
    "wildplus.configuration.healthBars": "Mob health bars",
}


def facing(name):
    write(ASSETS / f"blockstates/{name}.json", {"variants": {
        f"facing={d}": ({"model": i(f"block/{name}"), "y": r} if r else {"model": i(f"block/{name}")}) for d, r in ROT.items()}})


def assets():
    write(ASSETS / "models/block/chunk_loader.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "minecraft:block/lodestone_top", "bottom": "minecraft:block/chiseled_stone_bricks", "side": "minecraft:block/lodestone_side"}})
    write(ASSETS / "blockstates/chunk_loader.json", {"variants": {"": {"model": i("block/chunk_loader")}}})
    write(ASSETS / "models/block/enhance_table.json", {"parent": "minecraft:block/cube", "textures": {
        "particle": "minecraft:block/smithing_table_front", "up": "minecraft:block/enchanting_table_top",
        "down": "minecraft:block/smithing_table_bottom", "north": "minecraft:block/smithing_table_front",
        "south": "minecraft:block/smithing_table_front", "east": "minecraft:block/smithing_table_side",
        "west": "minecraft:block/smithing_table_side"}})
    facing("enhance_table")
    write(ASSETS / "models/block/pharmacy.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": i("block/pharmacy_front"), "side": "minecraft:block/quartz_block_side", "top": "minecraft:block/quartz_block_top"}})
    facing("pharmacy")
    write(ASSETS / "models/block/weather_station.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": i("block/weather_station_front"), "side": "minecraft:block/cut_copper", "top": "minecraft:block/daylight_detector_top"}})
    facing("weather_station")
    for name in BLOCKS:
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{name}")}})


def recipes():
    r = {
        "chunk_loader": shapeless([i("gold_coin"), "minecraft:ender_pearl", "minecraft:obsidian"], i("chunk_loader")),
        "enhance_table": shaped(["LGL", "ISI"], {"L": "minecraft:lapis_lazuli", "G": i("gold_coin"), "I": "minecraft:iron_ingot",
                                               "S": "minecraft:smithing_table"}, i("enhance_table"), category="misc"),
        "pharmacy": shaped(["PNP", "QBQ", "QSQ"], {"P": "minecraft:glass_pane", "N": "minecraft:nether_wart", "Q": "minecraft:quartz_block",
                                                  "B": "minecraft:brewing_stand", "S": i("silver_coin")}, i("pharmacy"), category="misc"),
        "weather_station": shaped(["CDC", "CSC", "CCC"], {"C": "minecraft:copper_ingot", "D": "minecraft:daylight_detector",
                                                         "S": i("gold_coin")}, i("weather_station"), category="misc"),
    }
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def loot():
    for name in BLOCKS:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
             "entries": [{"type": "minecraft:item", "name": i(name)}]}], "random_sequence": i(f"blocks/{name}")})


def tags():
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i("chunk_loader"), i("pharmacy"), i("weather_station")])
    merge_tag(t / "block/mineable/axe.json", [i("enhance_table")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    recipes()
    loot()
    tags()
    # 5차의 인챈트 최대 레벨 덮어쓰기 제거 (바닐라 그대로, 그 이상은 강화대)
    shutil.rmtree(MC / "enchantment", ignore_errors=True)
