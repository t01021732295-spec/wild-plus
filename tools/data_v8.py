"""8차 데이터: 옥수수·양배추·블루베리, 새 물고기, 드릴, 자동 수확기·광석 채굴기·자동 낚시 장치, 카지노, 흡혈 효과."""
from data_v2 import ASSETS, DATA, MC, count, cook, entry, i, merge_tag, self_loot, shaped, shapeless, write
from data_v4 import facing_states

FLAT = ["corn_seeds", "corn", "grilled_corn", "cabbage_seeds", "cabbage", "salad", "blueberry", "blueberry_muffin",
        "catfish", "cooked_catfish", "tuna", "tuna_steak", "sweetfish", "grilled_sweetfish", "dragon_king_fish"]
CROPS = ["corn_crop", "cabbage_crop", "blueberry_crop"]
MACHINES = {"auto_harvester": ("minecraft:block/iron_block", "minecraft:block/iron_block"),
            "ore_miner": ("minecraft:block/iron_block", "minecraft:block/iron_block"),
            "auto_fisher": ("minecraft:block/barrel_top", "minecraft:block/spruce_planks")}

LANG_KO = {
    "block.wildplus.corn_crop": "옥수수",
    "block.wildplus.cabbage_crop": "양배추",
    "block.wildplus.blueberry_crop": "블루베리 덤불",
    "item.wildplus.corn_seeds": "옥수수 씨앗",
    "item.wildplus.corn": "옥수수",
    "item.wildplus.grilled_corn": "군옥수수",
    "item.wildplus.cabbage_seeds": "양배추 씨앗",
    "item.wildplus.cabbage": "양배추",
    "item.wildplus.salad": "샐러드",
    "item.wildplus.blueberry": "블루베리",
    "item.wildplus.blueberry_muffin": "블루베리 머핀",
    "item.wildplus.catfish": "메기",
    "item.wildplus.cooked_catfish": "메기 구이",
    "item.wildplus.tuna": "참치",
    "item.wildplus.tuna_steak": "참치 스테이크",
    "item.wildplus.sweetfish": "은어",
    "item.wildplus.grilled_sweetfish": "은어 소금구이",
    "item.wildplus.dragon_king_fish": "전설의 용왕어",
    "item.wildplus.drill": "드릴",
    "block.wildplus.auto_harvester": "자동 수확기",
    "block.wildplus.ore_miner": "광석 채굴기",
    "block.wildplus.auto_fisher": "자동 낚시 장치",
    "block.wildplus.casino_table": "카지노 테이블",
    "container.wildplus.casino": "카지노",
    "effect.wildplus.lifesteal": "흡혈",
    "tooltip.wildplus.corn_seeds": "잔디에서 가끔 나옴 · 옥수수로도 만들 수 있음",
    "tooltip.wildplus.cabbage_seeds": "잔디에서 가끔 나옴 · 양배추로도 만들 수 있음",
    "tooltip.wildplus.blueberry": "그대로 심을 수 있음 · 잔디에서 가끔 나옴",
    "tooltip.wildplus.blueberry_muffin": "야간 투시 1분",
    "tooltip.wildplus.grilled_sweetfish": "신속 1분",
    "tooltip.wildplus.catfish": "늪, 밤의 강에서 잘 잡힘",
    "tooltip.wildplus.tuna": "낮의 바다 (깊은 바다일수록 잘 잡힘)",
    "tooltip.wildplus.sweetfish": "낮의 강에서 잘 잡힘",
    "tooltip.wildplus.dragon_king_fish": "어디서든 1% (비취 낚싯대 2%)\n먹으면 힘 II · 저항 II 2분, 재생 II",
    "tooltip.wildplus.drill": "곡괭이 + 삽, 3×3 채굴 (웅크리면 한 칸)\n다이아몬드 곡괭이급 · 아주 빠름",
    "tooltip.wildplus.auto_harvester": "스프링클러 바로 아래에 놓기\n스프링클러 범위의 다 자란 작물을 수확하고\n씨앗 칸의 씨앗을 다시 심음 · 호퍼로 넣고 빼기",
    "tooltip.wildplus.ore_miner": "창에서 광물 하나를 고르면 현실 시간 기준으로 채굴 (비싼 광물일수록 느림)\n강화: 다이아 블록 4개 → 다이아·에메랄드 / 루비 블록 8개 → 미스릴·비취·루비\n(블록을 들고 우클릭) · 서버·청크가 켜져 있을 때만 채굴 · 호퍼로 빼기",
    "tooltip.wildplus.auto_fisher": "물 위에 놓고 낚싯대를 넣기\n낚싯대의 미끼·바다의 행운이 그대로 적용\n잡은 물고기는 호퍼로 빼기",
    "tooltip.wildplus.casino_table": "동전 던지기 · 주사위 · 슬롯머신\n판돈 최대 10,000동, 딴 돈에는 세금",
    "message.wildplus.blood_moon.clear": "블러드문 클리어! (정예 %s마리, 노데스) 30분간 흡혈·재생 I·힘 II",
    "message.wildplus.blood_moon.fail_died": "블러드문 보상 실패: 블러드문 중에 죽었어요 (정예 %s/%s)",
    "message.wildplus.blood_moon.fail_kills": "블러드문 보상 실패: 정예 몬스터 %s/%s마리",
    "message.wildplus.blood_moon.progress": "블러드문 정예 처치 %s/%s",
    "message.wildplus.casino.no_money": "판돈 %s동이 부족해요",
    "message.wildplus.casino.jackpot": "🎰 %s 님이 슬롯머신 [%s] 잭팟! %s동 획득!",
}
LANG_EN = {
    "block.wildplus.corn_crop": "Corn",
    "block.wildplus.cabbage_crop": "Cabbage",
    "block.wildplus.blueberry_crop": "Blueberry Bush",
    "item.wildplus.corn_seeds": "Corn Seeds",
    "item.wildplus.corn": "Corn",
    "item.wildplus.grilled_corn": "Grilled Corn",
    "item.wildplus.cabbage_seeds": "Cabbage Seeds",
    "item.wildplus.cabbage": "Cabbage",
    "item.wildplus.salad": "Salad",
    "item.wildplus.blueberry": "Blueberry",
    "item.wildplus.blueberry_muffin": "Blueberry Muffin",
    "item.wildplus.catfish": "Catfish",
    "item.wildplus.cooked_catfish": "Cooked Catfish",
    "item.wildplus.tuna": "Tuna",
    "item.wildplus.tuna_steak": "Tuna Steak",
    "item.wildplus.sweetfish": "Sweetfish",
    "item.wildplus.grilled_sweetfish": "Salt-Grilled Sweetfish",
    "item.wildplus.dragon_king_fish": "Legendary Dragon King Fish",
    "item.wildplus.drill": "Drill",
    "block.wildplus.auto_harvester": "Auto Harvester",
    "block.wildplus.ore_miner": "Ore Miner",
    "block.wildplus.auto_fisher": "Auto Fisher",
    "block.wildplus.casino_table": "Casino Table",
    "container.wildplus.casino": "Casino",
    "effect.wildplus.lifesteal": "Lifesteal",
    "tooltip.wildplus.corn_seeds": "Sometimes found in grass · craft from corn",
    "tooltip.wildplus.cabbage_seeds": "Sometimes found in grass · craft from cabbage",
    "tooltip.wildplus.blueberry": "Plant it directly · sometimes found in grass",
    "tooltip.wildplus.blueberry_muffin": "Night Vision 1 min",
    "tooltip.wildplus.grilled_sweetfish": "Speed 1 min",
    "tooltip.wildplus.catfish": "Swamps, and rivers at night",
    "tooltip.wildplus.tuna": "Oceans by day (deep ocean is best)",
    "tooltip.wildplus.sweetfish": "Rivers by day",
    "tooltip.wildplus.dragon_king_fish": "1% anywhere (2% with the jade rod)\nStrength II, Resistance II for 2 min, Regeneration II",
    "tooltip.wildplus.drill": "Pickaxe + shovel, mines 3x3 (sneak for one block)\nDiamond tier, very fast",
    "tooltip.wildplus.auto_harvester": "Place directly under a sprinkler\nHarvests ripe crops in range and replants from the seed slots\nHopper in/out",
    "tooltip.wildplus.ore_miner": "Pick one ore in the window; pricier ores are slower\nUpgrade with 4 diamond blocks / 8 ruby blocks (right-click)\nHopper out",
    "tooltip.wildplus.auto_fisher": "Place on water and insert a fishing rod\nThe rod's Lure and Luck of the Sea apply\nHopper out",
    "tooltip.wildplus.casino_table": "Coin flip · dice · slots\nBets up to 10,000, winnings are taxed",
    "message.wildplus.blood_moon.clear": "Blood moon cleared! (%s elites, no deaths) Lifesteal, Regeneration I, Strength II for 30 min",
    "message.wildplus.blood_moon.fail_died": "No blood moon reward: you died during it (elites %s/%s)",
    "message.wildplus.blood_moon.fail_kills": "No blood moon reward: elites %s/%s",
    "message.wildplus.blood_moon.progress": "Blood moon elites %s/%s",
    "message.wildplus.casino.no_money": "You need %s coins to bet",
    "message.wildplus.casino.jackpot": "🎰 %s hit the [%s] jackpot! Won %s coins!",
}
ENHANCE_KO = "인챈트를 동전으로 IX까지 강화 (바닐라 최대 이후 단계마다 3배) · 동전으로 수리"
ENHANCE_EN = "Pay coins to push enchantments up to IX (x3 per level past vanilla max) · repair for coins"


def assets():
    for name in FLAT:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
    write(ASSETS / "models/item/drill.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": i("item/drill")}})
    write(ASSETS / "items/drill.json", {"model": {"type": "minecraft:model", "model": i("item/drill")}})
    stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    for crop in CROPS:
        write(ASSETS / f"blockstates/{crop}.json",
              {"variants": {f"age={a}": {"model": i(f"block/{crop}_stage{s}")} for a, s in stage.items()}})
        for s in range(4):
            write(ASSETS / f"models/block/{crop}_stage{s}.json",
                  {"parent": "minecraft:block/crop", "textures": {"crop": i(f"block/{crop}_stage{s}")}})
    for name, (top, bottom) in MACHINES.items():
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": i(f"block/{name}_side"), "top": top, "bottom": bottom}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": i(f"block/{name}")}}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{name}")}})
    write(ASSETS / "models/block/casino_table.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": "minecraft:block/dark_oak_planks", "top": i("block/casino_table_top"), "bottom": "minecraft:block/dark_oak_planks"}})
    write(ASSETS / "blockstates/casino_table.json", facing_states(i("block/casino_table")))
    write(ASSETS / "items/casino_table.json", {"model": {"type": "minecraft:model", "model": i("block/casino_table")}})


def ripe(crop):
    return {"type": "minecraft:match_block", "blocks": i(crop), "state": {"age": "7"}}


def seed_crop_loot(crop, produce, seeds, lo, hi):
    return {"type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"},
            "pools": [
                {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": i(produce), "condition": ripe(crop), "modifier": count(lo, hi)},
                    {"type": "minecraft:item", "name": i(seeds)}]}]},
                {"rolls": 1, "condition": ripe(crop), "entries": [{
                    "type": "minecraft:item", "name": i(seeds),
                    "modifier": {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                                 "formula": "minecraft:binomial_with_bonus_count",
                                 "parameters": {"extra": 1, "probability": 0.5714286}}}]},
            ], "random_sequence": i(f"blocks/{crop}")}


def loot():
    write(DATA / "loot_table/blocks/corn_crop.json", seed_crop_loot("corn_crop", "corn", "corn_seeds", 2, 3))
    write(DATA / "loot_table/blocks/cabbage_crop.json", seed_crop_loot("cabbage_crop", "cabbage", "cabbage_seeds", 1, 2))
    write(DATA / "loot_table/blocks/blueberry_crop.json", {
        "type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"},
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i("blueberry")}]},
            {"rolls": 1, "condition": ripe("blueberry_crop"), "entries": [{
                "type": "minecraft:item", "name": i("blueberry"),
                "modifier": [count(2, 4), {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                                           "formula": "minecraft:binomial_with_bonus_count",
                                           "parameters": {"extra": 1, "probability": 0.5714286}}]}]},
        ], "random_sequence": i("blocks/blueberry_crop")})
    for b in list(MACHINES) + ["casino_table"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    # 잔디 씨앗 표에 새 작물 추가
    write(DATA / "loot_table/inject/grass_seeds.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1,
                   "condition": {"type": "minecraft:all_of", "terms": [
                       {"type": "minecraft:random_chance", "chance": 0.16},
                       {"type": "minecraft:inverted", "term": "minecraft:tool/can_shear"}]},
                   "entries": [entry(i("chili_seeds"), 4), entry(i("tomato_seeds"), 4), entry(i("strawberry"), 2),
                               entry(i("corn_seeds"), 4), entry(i("cabbage_seeds"), 3), entry(i("blueberry"), 2)]}],
        "random_sequence": i("inject/grass_seeds"),
    })


def recipes():
    M, I, R = i("mythril_ingot"), "minecraft:iron_ingot", "minecraft:redstone"
    r = {
        "corn_seeds": shapeless([i("corn")], i("corn_seeds"), 2),
        "cabbage_seeds": shapeless([i("cabbage")], i("cabbage_seeds"), 2),
        "salad": shapeless(["minecraft:bowl", i("cabbage"), i("cabbage"), i("tomato"), "minecraft:carrot"], i("salad"), category="misc"),
        "blueberry_muffin": shapeless(["minecraft:wheat", i("blueberry"), i("blueberry"), "minecraft:sugar", "minecraft:egg"],
                                      i("blueberry_muffin"), 2),
        "drill": shaped(["MPM", "MBM", " I "], {"M": M, "P": "minecraft:diamond_pickaxe", "B": "minecraft:redstone_block", "I": I},
                        i("drill")),
        "auto_harvester": shaped(["IHI", "ICI", "IRI"], {"I": I, "H": "minecraft:iron_hoe", "C": "minecraft:chest", "R": R},
                                 i("auto_harvester"), category="misc"),
        "ore_miner": shaped(["IPI", "MGM", "IRI"], {"I": I, "P": "minecraft:diamond_pickaxe", "M": i("mythril_block"),
                                                  "G": i("gold_coin"), "R": "minecraft:redstone_block"}, i("ore_miner"), category="misc"),
        "auto_fisher": shaped(["PFP", "PCP", "PRP"], {"P": "#minecraft:planks", "F": "minecraft:fishing_rod", "C": "minecraft:chest", "R": R},
                              i("auto_fisher"), category="misc"),
        "casino_table": shaped(["EEE", "PGP", "P P"], {"E": "minecraft:emerald", "P": "minecraft:dark_oak_planks", "G": i("gold_coin")},
                               i("casino_table"), category="misc"),
    }
    for raw, done in (("corn", "grilled_corn"), ("catfish", "cooked_catfish"), ("tuna", "tuna_steak"),
                      ("sweetfish", "grilled_sweetfish")):
        r[f"{done}"] = cook("smelting", i(raw), i(done), 0.35, 200)
        r[f"{done}_from_smoking"] = cook("smoking", i(raw), i(done), 0.35, 100)
        r[f"{done}_from_campfire"] = cook("campfire_cooking", i(raw), i(done), 0.35, 600)
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def tags():
    t = MC / "tags"
    crops = [i(c) for c in CROPS]
    merge_tag(t / "block/crops.json", crops)
    merge_tag(t / "block/maintains_farmland.json", crops)
    merge_tag(t / "block/bee_growables.json", crops)
    merge_tag(t / "item/villager_plantable_seeds.json", [i("corn_seeds"), i("cabbage_seeds"), i("blueberry")])
    merge_tag(t / "item/fishes.json", [i(f) for f in ("catfish", "cooked_catfish", "tuna", "tuna_steak", "sweetfish",
                                                       "grilled_sweetfish", "dragon_king_fish")])
    merge_tag(t / "block/mineable/pickaxe.json", [i("auto_harvester"), i("ore_miner")])
    merge_tag(t / "block/mineable/axe.json", [i("auto_fisher"), i("casino_table")])
    merge_tag(t / "item/pickaxes.json", [i("drill")])


def build(lang_ko, lang_en):
    ko = {**lang_ko, **LANG_KO}
    en = {**lang_en, **LANG_EN}
    ko["tooltip.wildplus.enhance_table"] = ENHANCE_KO
    en["tooltip.wildplus.enhance_table"] = ENHANCE_EN
    write(ASSETS / "lang/ko_kr.json", ko)
    write(ASSETS / "lang/en_us.json", en)
    assets()
    loot()
    recipes()
    tags()
