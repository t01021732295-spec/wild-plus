"""2.9 데이터: 사파이어 광석·보석·장비·활, 용의 깃털, 스카이 사파이어."""
from data_v2 import ASSETS, DATA, MC, cook, i, merge_tag, ore_feature, ore_loot, placed, self_loot, shaped, shapeless, write

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
KO = {"sword": "검", "pickaxe": "곡괭이", "axe": "도끼", "shovel": "삽", "hoe": "괭이",
      "helmet": "투구", "chestplate": "흉갑", "leggings": "레깅스", "boots": "부츠"}
EN = {"sword": "Sword", "pickaxe": "Pickaxe", "axe": "Axe", "shovel": "Shovel", "hoe": "Hoe",
      "helmet": "Helmet", "chestplate": "Chestplate", "leggings": "Leggings", "boots": "Boots"}
UP = "\n강화대에서 용의 깃털 + 1,000동으로 스카이 사파이어 승급"

TIP_KO = {
    "sword": "빙결검: 맞은 적 구속 II (3초)\n25% 확률로 서리 폭발 (주변 3칸 적 피해 4 + 얼림)",
    "pickaxe": "냉각: 웅크리고 들면 주변 4칸 용암이 굳음 (원천 → 흑요석)\n물속에서도 빨리 캠 (해양의 힘)",
    "axe": "얼음 처형: 느려졌거나 얼어 있는 적에게 피해 1.5배 (사파이어 검과 같이 쓰기 좋음)",
    "shovel": "빙하 삽: 얼음 · 꽁꽁 언 얼음 · 푸른 얼음을 그대로 주움, 눈 블록은 눈 블록으로",
    "hoe": "물의 축복: 웅크리고 들면 주변 4칸 작물이 빨리 자라고 경작지가 촉촉해짐",
}
TIP_EN = {
    "sword": "Frost Blade: Slowness II for 3s\n25% chance to burst (4 damage + freeze within 3 blocks)",
    "pickaxe": "Cooling: sneak to solidify lava within 4 blocks · fast mining underwater",
    "axe": "Ice Execute: 1.5x damage to slowed or frozen targets",
    "shovel": "Glacier Shovel: keeps ice, packed ice, blue ice and snow blocks",
    "hoe": "Water Blessing: sneak to grow nearby crops and moisten farmland",
}
SKY_KO = {
    "sword": "공격력 8 · 공격 속도 2.0 · 내구도 무한\n빙결검 (서리 폭발 40% · 피해 6) + 실명 3초 + 급강하 일격 (공중에서 때리면 1.5배)",
    "pickaxe": "내구도 무한 · 냉각 (용암 굳히기 · 물속 빨리 캐기)",
    "axe": "내구도 무한 · 얼음 처형 (느린·언 적에게 1.5배)",
    "shovel": "내구도 무한 · 빙하 삽",
    "hoe": "내구도 무한 · 물의 축복 (두 배로 빨리 자람)",
}
LANG_KO = {
    "block.wildplus.sapphire_ore": "사파이어 광석",
    "block.wildplus.deepslate_sapphire_ore": "심층암 사파이어 광석",
    "block.wildplus.sapphire_block": "사파이어 블록",
    "item.wildplus.sapphire": "사파이어",
    "tooltip.wildplus.sapphire": "높은 산 (Y 32 위)에서 나는 푸른 보석\n루비의 반대: 불 대신 얼음 · 공격 대신 방어",
    "item.wildplus.dragon_plume": "용의 깃털",
    "tooltip.wildplus.dragon_plume": "드래곤의 숨결 2 + 겉날개 → 깃털 2개\n강화대에서 사파이어 장비를 스카이 사파이어로 승급",
    "equipment.wildplus.sapphire": "사파이어",
    "equipment.wildplus.sky_sapphire": "스카이 사파이어",
    "entity.wildplus.titan_boss": "대지의 거신",
    "entity.wildplus.lich_boss": "망령 군주",
    "entity.wildplus.frost_boss": "서리 여왕",
    "entity.wildplus.vine_boss": "덩굴 집행자",
    "entity.wildplus.pharaoh_boss": "모래 파라오",
    "entity.wildplus.flame_boss": "화염 군주",
    "entity.wildplus.abyss_boss": "심해 수호자",
    "entity.wildplus.void_boss": "공허의 기사",
    "entity.wildplus.gold_boss": "황금 왕",
    "block.wildplus.luxury_shop": "명품관",
    "tooltip.wildplus.luxury_shop": "큰돈으로 귀한 물건 · 행운 상자 · 오라 · 칭호 구매\n쓴 돈은 국고로 사라져 물가를 잡아요",
    "block.wildplus.realty": "부동산 중개소",
    "tooltip.wildplus.realty": "청크(16×16) 땅 사고팔기 · 출입·블록·사용 권한 설정 · 땅 친구 관리\n/land 명령어로도 열 수 있음",
    "item.wildplus.diamond_coin": "다이아화",
    "tooltip.wildplus.diamond_coin": "1,000동 · 웅크리고 우클릭 또는 제작대에서 금화 10개로 쪼갬\n금화 10 → 다이아화는 환전소에서",
    "wildplus.coins.amount": "%s동 (다이아화 %s · 금화 %s · 은화 %s · 동화 %s)",
    "item.wildplus.repair_kit": "휴대용 수리 키트",
    "tooltip.wildplus.repair_kit": "우클릭: 손에 든 것 · 입은 갑옷 중 닳은 장비를 각 25%씩 수리\n고친 장비 하나마다 1회 소모 (10회)",
    "item.wildplus.sapphire_apple": "사파이어 사과",
    "tooltip.wildplus.sapphire_apple": "수중 호흡 · 돌고래의 가호 (3분)\n저항 I · 흡수 I (1분)",
    "item.wildplus.sky_sapphire_apple": "스카이 사파이어 사과",
    "tooltip.wildplus.sky_sapphire_apple": "저항 II · 신속 II (1분) · 흡수 IV (3분)\n느린 낙하 · 전달체의 힘 · 수중 호흡 (5분)",
    "item.wildplus.sapphire_bow": "사파이어 활",
    "tooltip.wildplus.sapphire_bow": "얼음 화살: 맞은 적 구속 II · 얼림\n당김 1.4배 · 피해 1.25배" + UP,
    "item.wildplus.sky_sapphire_bow": "스카이 사파이어 활",
    "tooltip.wildplus.sky_sapphire_bow": "화살이 1.35배 빠르게 날아감 · 당김 1.75배 · 피해 1.5배\n맞으면 구속 III · 얼림 + 주변 적도 느려짐 · 내구도 무한",
}
LANG_EN = {
    "block.wildplus.sapphire_ore": "Sapphire Ore",
    "block.wildplus.deepslate_sapphire_ore": "Deepslate Sapphire Ore",
    "block.wildplus.sapphire_block": "Block of Sapphire",
    "item.wildplus.sapphire": "Sapphire",
    "tooltip.wildplus.sapphire": "A blue gem found high in mountains (above Y 32)",
    "item.wildplus.dragon_plume": "Dragon Plume",
    "tooltip.wildplus.dragon_plume": "Upgrades sapphire gear to Sky Sapphire at the enhance table",
    "equipment.wildplus.sapphire": "Sapphire",
    "equipment.wildplus.sky_sapphire": "Sky Sapphire",
    "entity.wildplus.titan_boss": "Earth Titan",
    "entity.wildplus.lich_boss": "Wraith Lord",
    "entity.wildplus.frost_boss": "Frost Queen",
    "entity.wildplus.vine_boss": "Vine Executioner",
    "entity.wildplus.pharaoh_boss": "Sand Pharaoh",
    "entity.wildplus.flame_boss": "Flame Lord",
    "entity.wildplus.abyss_boss": "Abyss Warden",
    "entity.wildplus.void_boss": "Void Knight",
    "entity.wildplus.gold_boss": "Golden King",
    "block.wildplus.luxury_shop": "Luxury Shop",
    "block.wildplus.realty": "Realty Office",
    "item.wildplus.diamond_coin": "Diamond Coin",
    "tooltip.wildplus.diamond_coin": "Worth 1,000 · can be crafted into 10 gold",
    "wildplus.coins.amount": "%s c (%s diamond · %s gold · %s silver · %s copper)",
    "item.wildplus.repair_kit": "Repair Kit",
    "tooltip.wildplus.repair_kit": "Right-click: repairs held items and worn armor by 25% each (10 uses)",
    "item.wildplus.sapphire_apple": "Sapphire Apple",
    "item.wildplus.sky_sapphire_apple": "Sky Sapphire Apple",
    "item.wildplus.sapphire_bow": "Sapphire Bow",
    "tooltip.wildplus.sapphire_bow": "Frost arrows: Slowness II + freeze",
    "item.wildplus.sky_sapphire_bow": "Sky Sapphire Bow",
    "tooltip.wildplus.sky_sapphire_bow": "Faster arrows, Slowness III + freeze, unbreakable",
}
for t in TOOLS + ARMOR:
    LANG_KO[f"item.wildplus.sapphire_{t}"] = f"사파이어 {KO[t]}"
    LANG_EN[f"item.wildplus.sapphire_{t}"] = f"Sapphire {EN[t]}"
    LANG_KO[f"item.wildplus.sky_sapphire_{t}"] = f"스카이 사파이어 {KO[t]}"
    LANG_EN[f"item.wildplus.sky_sapphire_{t}"] = f"Sky Sapphire {EN[t]}"
for t in TOOLS:
    LANG_KO[f"tooltip.wildplus.sapphire_{t}"] = TIP_KO[t] + UP
    LANG_EN[f"tooltip.wildplus.sapphire_{t}"] = TIP_EN[t]
    LANG_KO[f"tooltip.wildplus.sky_sapphire_{t}"] = SKY_KO[t]
    LANG_EN[f"tooltip.wildplus.sky_sapphire_{t}"] = "Unbreakable · " + TIP_EN[t]
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.sapphire_{a}"] = "2부위: 수중 호흡 + 돌고래의 가호\n4부위: 전달체의 힘 · 받는 피해 10% 감소 · 때린 적 얼림 · 낙하 피해 절반" + UP
    LANG_EN[f"tooltip.wildplus.sapphire_{a}"] = "2 pieces: Water Breathing + Dolphin's Grace\n4 pieces: -10% damage, freeze attackers, half fall damage"
    LANG_KO[f"tooltip.wildplus.sky_sapphire_{a}"] = ("내구도 무한 · 불에 타지 않음\n2부위: 저항 I + 수중 호흡 · 돌고래의 가호\n"
                                                     "4부위: 저항 II + 신속 I · 전달체의 힘 · 낙하 피해 없음 · 피해 10% 감소 · 때린 적 얼림")
    LANG_EN[f"tooltip.wildplus.sky_sapphire_{a}"] = "Unbreakable · 2 pieces: Resistance I · 4 pieces: + Speed I, no fall damage"
LANG_KO["tooltip.wildplus.sky_sapphire_chestplate"] = ("겉날개 겸용! (폭죽 가속 가능) · 내구도 무한\n2부위: 저항 I + 수중 호흡 · 돌고래의 가호\n"
                                                      "4부위: 저항 II + 신속 I · 전달체의 힘 · 낙하 피해 없음 · 피해 10% 감소 · 때린 적 얼림")
LANG_EN["tooltip.wildplus.sky_sapphire_chestplate"] = "Works as an elytra! · Unbreakable"


def model(name, parent, tex=None):
    write(ASSETS / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": i(f"item/{tex or name}")}})


def simple(name, parent="minecraft:item/generated"):
    model(name, parent)
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})


def bow(b):
    model(b, "minecraft:item/bow")
    for n in range(3):
        model(f"{b}_pulling_{n}", "minecraft:item/bow")
    write(ASSETS / f"items/{b}.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:using_item",
        "on_false": {"type": "minecraft:model", "model": i(f"item/{b}")},
        "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                    "entries": [{"model": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_1")}, "threshold": 0.65},
                                {"model": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_2")}, "threshold": 0.9}],
                    "fallback": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_0")}}}})


def assets():
    for b in ["sapphire_ore", "deepslate_sapphire_ore", "sapphire_block"]:
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{b}")}})
    simple("sapphire")
    simple("dragon_plume")
    simple("sapphire_apple")
    simple("repair_kit")
    simple("diamond_coin")
    for b in ["luxury_shop", "realty"]:
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": i(f"block/{b}_side"), "top": i(f"block/{b}_top"), "bottom": i(f"block/{b}_top")}})
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        write(ASSETS / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{b}")}})
    simple("sky_sapphire_apple")
    for p in ["sapphire", "sky_sapphire"]:
        for t in TOOLS:
            simple(f"{p}_{t}", "minecraft:item/handheld")
        for a in ARMOR:
            simple(f"{p}_{a}")
        bow(f"{p}_bow")
    write(ASSETS / "equipment/sapphire.json", {"layers": {
        "humanoid": [{"texture": i("sapphire")}], "humanoid_leggings": [{"texture": i("sapphire")}]}})
    # 스카이 사파이어: 흉갑에 겉날개 날개도 그림
    write(ASSETS / "equipment/sky_sapphire.json", {"layers": {
        "humanoid": [{"texture": i("sky_sapphire")}], "humanoid_leggings": [{"texture": i("sky_sapphire")}],
        "wings": [{"texture": i("sky_sapphire")}]}})


def data():
    for b in ["sapphire_ore", "deepslate_sapphire_ore"]:
        write(DATA / f"loot_table/blocks/{b}.json", ore_loot(b, i("sapphire")))
    write(DATA / "loot_table/blocks/sapphire_block.json", self_loot("sapphire_block"))
    for b in ["luxury_shop", "realty"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    S, ST = i("sapphire"), "minecraft:stick"
    rec = {
        "sapphire_block": shaped(["###", "###", "###"], {"#": S}, i("sapphire_block"), category="building"),
        "sapphire_from_block": shapeless([i("sapphire_block")], S, 9),
        "sapphire_from_smelting": cook("smelting", [i("sapphire_ore"), i("deepslate_sapphire_ore")], S, 1.0, 200, "misc"),
        "sapphire_from_blasting": cook("blasting", [i("sapphire_ore"), i("deepslate_sapphire_ore")], S, 1.0, 100, "misc"),
        "sapphire_sword": shaped(["X", "X", "#"], {"X": S, "#": ST}, i("sapphire_sword")),
        "sapphire_pickaxe": shaped(["XXX", " # ", " # "], {"X": S, "#": ST}, i("sapphire_pickaxe")),
        "sapphire_axe": shaped(["XX", "X#", " #"], {"X": S, "#": ST}, i("sapphire_axe")),
        "sapphire_shovel": shaped(["X", "#", "#"], {"X": S, "#": ST}, i("sapphire_shovel")),
        "sapphire_hoe": shaped(["XX", " #", " #"], {"X": S, "#": ST}, i("sapphire_hoe")),
        "sapphire_helmet": shaped(["XXX", "X X"], {"X": S}, i("sapphire_helmet")),
        "sapphire_chestplate": shaped(["X X", "XXX", "XXX"], {"X": S}, i("sapphire_chestplate")),
        "sapphire_leggings": shaped(["XXX", "X X", "X X"], {"X": S}, i("sapphire_leggings")),
        "sapphire_boots": shaped(["X X", "X X"], {"X": S}, i("sapphire_boots")),
        "sapphire_bow": shaped([" MS", "M S", " MS"], {"M": S, "S": "minecraft:string"}, i("sapphire_bow")),
        "gold_coin_from_diamond": shapeless([i("diamond_coin")], i("gold_coin"), 10),
        "diamond_coin_from_emerald": shapeless([i("emerald_coin")], i("diamond_coin"), 10),
        "luxury_shop": shaped(["GDG", "DED", "GDG"], {"G": "minecraft:gold_ingot", "D": "minecraft:diamond", "E": "minecraft:emerald_block"},
                              i("luxury_shop"), category="misc"),
        "realty": shaped(["PSP", "PBP", "PPP"], {"P": "#minecraft:planks", "S": "minecraft:oak_sign", "B": "minecraft:writable_book"},
                         i("realty"), category="misc"),
        "repair_kit": shaped([" D ", "ILI", "III"], {"D": "minecraft:diamond", "I": "minecraft:iron_ingot", "L": "minecraft:leather"},
                             i("repair_kit"), category="misc"),
        "sapphire_apple": shaped(["MMM", "MAM", "MMM"], {"M": S, "A": "minecraft:apple"}, i("sapphire_apple"), category="misc"),
        "sky_sapphire_apple": shaped(["BBB", "BAB", "BPB"], {"B": i("sapphire_block"), "A": i("sapphire_apple"), "P": i("dragon_plume")},
                                     i("sky_sapphire_apple"), category="misc"),
        "dragon_plume": shapeless(["minecraft:elytra", "minecraft:dragon_breath", "minecraft:dragon_breath"], i("dragon_plume"), 2),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    write(DATA / "tags/item/sapphire_repair.json", {"values": [S]})
    t = MC / "tags"
    blocks = [i(b) for b in ["sapphire_ore", "deepslate_sapphire_ore", "sapphire_block"]]
    merge_tag(t / "block/mineable/pickaxe.json", blocks + [i("luxury_shop")])
    merge_tag(t / "block/mineable/axe.json", [i("realty")])
    merge_tag(t / "block/needs_iron_tool.json", blocks)
    merge_tag(t / "block/beacon_base_blocks.json", [i("sapphire_block")])
    merge_tag(t / "item/beacon_payment_items.json", [S])
    for p in ["sapphire", "sky_sapphire"]:
        for t_ in TOOLS:
            merge_tag(t / f"item/{t_}s.json", [i(f"{p}_{t_}")])
        for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
            merge_tag(t / f"item/{slot}_armor.json", [i(f"{p}_{piece}")])
    bows = [i("sapphire_bow"), i("sky_sapphire_bow")]
    merge_tag(t / "item/enchantable/bow.json", bows)
    merge_tag(t / "item/enchantable/durability.json", bows)
    merge_tag(t / "item/enchantable/vanishing.json", bows)
    merge_tag(MC.parent / "c/tags/block/ores.json", [i("sapphire_ore"), i("deepslate_sapphire_ore")])
    merge_tag(MC.parent / "c/tags/item/gems.json", [S])
    # 월드 생성: 루비와 반대로 높은 곳 — 산악 생물군계 Y 32~256 (에메랄드처럼), 어디서나 Y 64~128 드물게
    write(DATA / "worldgen/feature/ore_sapphire.json", ore_feature("sapphire", 4, 0.0))
    write(DATA / "worldgen/placed_feature/ore_sapphire_mountain.json", placed(i("ore_sapphire"), 10, {
        "type": "minecraft:trapezoid", "min_inclusive": {"absolute": 32}, "max_inclusive": {"absolute": 256}}))
    write(DATA / "worldgen/placed_feature/ore_sapphire.json", placed(i("ore_sapphire"), 2, {
        "type": "minecraft:uniform", "min_inclusive": {"absolute": 40}, "max_inclusive": {"absolute": 128}}))
    write(DATA / "neoforge/biome_modifier/add_sapphire_ore_mountain.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_mountain",
        "features": i("ore_sapphire_mountain"), "step": "underground_ores"})
    write(DATA / "neoforge/biome_modifier/add_sapphire_ore.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": i("ore_sapphire"), "step": "underground_ores"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
