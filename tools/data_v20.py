"""2.18 데이터: 낚싯대 라인 · 미끼 · 비료 3단계 · 새 작물 · 비료 살포기 · 알바 게시판."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, shapeless, write
from data_v2 import entry
from data_v8 import seed_crop_loot
from data_v18 import simple

RODS = {"mythril": "미스릴", "ruby": "루비", "sapphire": "사파이어", "dragon": "용왕의"}
ROD_TIP = {
    "mythril": "입질 빠름 · 희귀 어종 2배 · 보물상자 4% · 15% 두 마리 · 등급 확률 ×1.3\n강화대에서 루비 블록 + 1,000동으로 루비 낚싯대 승급 (인챈트 유지)",
    "ruby": "입질 더 빠름 · 희귀 어종 2.4배 · 용왕어 3% · 20% 두 마리 · 등급 확률 ×1.6 · 불에 안 탐\n강화대에서 사파이어 블록 + 1,000동으로 사파이어 낚싯대 승급",
    "sapphire": "입질 아주 빠름 · 보물상자 8% · 35% 두 마리 · 등급 확률 ×2 · 행운 +1\n강화대에서 용왕어 + 1,000동으로 용왕의 낚싯대 승급",
    "dragon": "최고 낚싯대: 입질 최고 · 희귀 3배 · 용왕어 6% · 보물상자 10% · 50% 두 마리 · 등급 확률 ×3",
}
CROPS = {"grape": ("포도", "Grape"), "rice": ("쌀", "Rice"), "onion": ("양파", "Onion"), "ginseng": ("인삼", "Ginseng")}
SEED_KO = {"grape": "포도 씨앗", "rice": "볍씨", "onion": "양파 씨앗", "ginseng": "인삼 씨앗"}

LANG_KO = {
    "item.wildplus.worm": "지렁이",
    "tooltip.wildplus.worm": "미끼 (왼손에 들고 낚시): 입질이 빨라지고 등급 확률 ×1.3 · 한 마리마다 1개\n흙·잔디·경작지를 파면 가끔 나옴",
    "item.wildplus.shrimp_bait": "새우 미끼",
    "tooltip.wildplus.shrimp_bait": "미끼 (왼손): 희귀 어종 +15% · 등급 확률 ×1.5\n물고기 1 + 켈프 1 → 4개",
    "item.wildplus.golden_bait": "황금 미끼",
    "tooltip.wildplus.golden_bait": "미끼 (왼손): 입질 빨라짐 · 등급 확률 ×3\n금 조각 8 + 지렁이 → 2개",
    "item.wildplus.rich_fertilizer": "고급 비료",
    "tooltip.wildplus.rich_fertilizer": "주변 3x3 작물 3번 성장 + 경작지에 2단계 영양 (수확 3번)\n금 품질 15% · 무지개 3% · 더 빨리 자람",
    "item.wildplus.golden_fertilizer": "황금 비료",
    "tooltip.wildplus.golden_fertilizer": "주변 3x3 작물 4번 성장 + 경작지에 3단계 영양 (수확 3번)\n금 품질 28% · 무지개 8% · 가장 빨리 자람",
    "tooltip.wildplus.fertilizer": "주변 3x3 작물·묘목 2번 성장 + 경작지에 1단계 영양 (수확 3번)\n은 품질 25% · 금 6% · 조금 빨리 자람",
    "block.wildplus.fertilizer_spreader": "비료 살포기",
    "tooltip.wildplus.fertilizer_spreader": "넣어 둔 비료를 주변 9×9 경작지에 3초마다 한 칸씩 자동으로 뿌림\n좋은 비료부터 · 영양이 없거나 낮은 칸부터 · 호퍼로 채우기",
    "block.wildplus.job_board": "알바 게시판",
    "tooltip.wildplus.job_board": "매일 바뀌는 알바 3개 · 골라서 출근하고 일을 끝내면 보수가 통장으로\n하루 하나라도 끝내면 연속 근무 보너스 (+5%씩, 최대 +50%)",
    "tooltip.wildplus.ginseng_seeds": "아주 천천히 자람 (게임 속 약 7일) · 비싸게 팔림\n잔디에서 아주 가끔",
    "tooltip.wildplus.grape_seeds": "잔디에서 가끔 나옴 · 포도로도 만들 수 있음",
    "tooltip.wildplus.rice_seeds": "잔디에서 가끔 나옴 · 쌀로도 만들 수 있음",
    "tooltip.wildplus.onion_seeds": "잔디에서 가끔 나옴 · 양파로도 만들 수 있음",
    "tooltip.wildplus.ginseng": "먹으면 재생 · 흡수 · 비싸게 팔리는 약초",
}
LANG_EN = {
    "item.wildplus.worm": "Worm", "item.wildplus.shrimp_bait": "Shrimp Bait", "item.wildplus.golden_bait": "Golden Bait",
    "item.wildplus.rich_fertilizer": "Rich Fertilizer", "item.wildplus.golden_fertilizer": "Golden Fertilizer",
    "block.wildplus.fertilizer_spreader": "Fertilizer Spreader", "block.wildplus.job_board": "Job Board",
}
for r, ko in RODS.items():
    LANG_KO[f"item.wildplus.{r}_fishing_rod"] = f"{ko} 낚싯대"
    LANG_KO[f"tooltip.wildplus.{r}_fishing_rod"] = ROD_TIP[r]
    LANG_EN[f"item.wildplus.{r}_fishing_rod"] = ("Dragon King" if r == "dragon" else r.capitalize()) + " Fishing Rod"
LANG_KO["tooltip.wildplus.jade_fishing_rod"] = "희귀 어종·보물상자 확률 2배, 가끔 두 마리\n강화대에서 미스릴 블록 + 1,000동으로 미스릴 낚싯대 승급 (인챈트 유지)"
for c, (ko, en) in CROPS.items():
    LANG_KO[f"block.wildplus.{c}_crop"] = ko
    LANG_EN[f"block.wildplus.{c}_crop"] = en
    LANG_KO[f"item.wildplus.{c}"] = ko
    LANG_EN[f"item.wildplus.{c}"] = en
    LANG_KO[f"item.wildplus.{c}_seeds"] = SEED_KO[c]
    LANG_EN[f"item.wildplus.{c}_seeds"] = f"{en} Seeds"


def assets():
    for r in RODS:
        for name in (f"{r}_fishing_rod", f"{r}_fishing_rod_cast"):
            write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{r}_fishing_rod.json", {"model": {
            "type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
            "on_false": {"type": "minecraft:model", "model": i(f"item/{r}_fishing_rod")},
            "on_true": {"type": "minecraft:model", "model": i(f"item/{r}_fishing_rod_cast")}}})
    for n in ["worm", "shrimp_bait", "golden_bait", "rich_fertilizer", "golden_fertilizer"]:
        simple(n)
    stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    for c in CROPS:
        simple(c)
        simple(f"{c}_seeds")
        crop = f"{c}_crop"
        write(ASSETS / f"blockstates/{crop}.json", {"variants": {f"age={a}": {"model": i(f"block/{crop}_stage{s}")} for a, s in stage.items()}})
        for s in range(4):
            write(ASSETS / f"models/block/{crop}_stage{s}.json", {"parent": "minecraft:block/crop", "textures": {"crop": i(f"block/{crop}_stage{s}")}})
    write(ASSETS / "models/block/fertilizer_spreader.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": i("block/fertilizer_spreader_side"), "top": i("block/fertilizer_spreader_top"), "bottom": "minecraft:block/iron_block"}})
    write(ASSETS / "blockstates/fertilizer_spreader.json", {"variants": {f"facing={d}": {"model": i("block/fertilizer_spreader")}
                                                                          for d in ("north", "south", "east", "west")}})
    write(ASSETS / "items/fertilizer_spreader.json", {"model": {"type": "minecraft:model", "model": i("block/fertilizer_spreader")}})
    write(ASSETS / "models/block/job_board.json", {"parent": "minecraft:block/cube_column", "textures": {
        "side": i("block/job_board_front"), "end": "minecraft:block/oak_planks"}})
    write(ASSETS / "blockstates/job_board.json", {"variants": {"": {"model": i("block/job_board")}}})
    write(ASSETS / "items/job_board.json", {"model": {"type": "minecraft:model", "model": i("block/job_board")}})


def data():
    for c, (lo, hi) in {"grape": (2, 4), "rice": (2, 4), "onion": (1, 3), "ginseng": (1, 2)}.items():
        write(DATA / f"loot_table/blocks/{c}_crop.json", seed_crop_loot(f"{c}_crop", c, f"{c}_seeds", lo, hi))
    for b in ["fertilizer_spreader", "job_board"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    # 잔디 씨앗 표 (기존 + 새 작물)
    write(DATA / "loot_table/inject/grass_seeds.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1,
                   "condition": {"type": "minecraft:all_of", "terms": [
                       {"type": "minecraft:random_chance", "chance": 0.18},
                       {"type": "minecraft:inverted", "term": "minecraft:tool/can_shear"}]},
                   "entries": [entry(i("chili_seeds"), 4), entry(i("tomato_seeds"), 4), entry(i("strawberry"), 2),
                               entry(i("corn_seeds"), 4), entry(i("cabbage_seeds"), 3), entry(i("blueberry"), 2),
                               entry(i("grape_seeds"), 2), entry(i("rice_seeds"), 3), entry(i("onion_seeds"), 3), entry(i("ginseng_seeds"), 1)]}],
        "random_sequence": i("inject/grass_seeds"),
    })
    rec = {
        "grape_seeds": shapeless([i("grape")], i("grape_seeds"), 2),
        "rice_seeds": shapeless([i("rice")], i("rice_seeds"), 1),
        "onion_seeds": shapeless([i("onion")], i("onion_seeds"), 2),
        "shrimp_bait": shapeless(["#minecraft:fishes", "minecraft:kelp"], i("shrimp_bait"), 4),
        "golden_bait": shaped(["NNN", "NWN", "NNN"], {"N": "minecraft:gold_nugget", "W": i("worm")}, i("golden_bait"), 2, category="misc"),
        "rich_fertilizer": shapeless([i("fertilizer"), i("fertilizer"), "minecraft:bone_block"], i("rich_fertilizer"), 2),
        "golden_fertilizer": shapeless([i("rich_fertilizer"), i("rich_fertilizer"), "minecraft:gold_ingot"], i("golden_fertilizer"), 2),
        "fertilizer_spreader": shaped(["IDI", "IFI", "IHI"], {"I": "minecraft:iron_ingot", "D": "minecraft:dispenser",
                                                               "F": i("fertilizer"), "H": "minecraft:hopper"}, i("fertilizer_spreader"), category="misc"),
        "job_board": shaped(["PPP", "WWW", "S S"], {"P": "minecraft:paper", "W": "#minecraft:planks", "S": "minecraft:stick"},
                            i("job_board"), category="misc"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags"
    rods = [i(f"{r}_fishing_rod") for r in RODS]
    merge_tag(t / "item/enchantable/fishing.json", rods)
    merge_tag(t / "item/enchantable/durability.json", rods)
    merge_tag(t / "item/enchantable/vanishing.json", rods)
    merge_tag(t / "item/villager_plantable_seeds.json", [i(f"{c}_seeds") for c in CROPS])
    merge_tag(t / "block/crops.json", [i(f"{c}_crop") for c in CROPS])
    merge_tag(t / "block/maintains_farmland.json", [i(f"{c}_crop") for c in CROPS])
    merge_tag(t / "block/mineable/pickaxe.json", [i("fertilizer_spreader")])
    merge_tag(t / "block/mineable/axe.json", [i("job_board")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
