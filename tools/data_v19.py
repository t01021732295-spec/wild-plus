"""2.12 데이터: 에메랄드 장비 · 생명의 결정 · 로열 에메랄드."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, shaped, shapeless, write
from data_v18 import ARMOR, EN, KO, TOOLS, bow, simple

UP = "\n강화대에서 생명의 결정 + 1,000동으로 로열 에메랄드 승급"
TIP = {
    "sword": ("약탈: 처치하면 30% 확률로 전리품 두 배 · 경험치 1.3배", "공격력 8.5 · 속도 1.8 · 내구도 무한\n약탈 강화: 70% 전리품 두 배 · 경험치 2배\n생명 수확: 처치마다 체력 2 회복 · 20% 확률 에메랄드"),
    "pickaxe": ("행운의 손길: 광석 20% 확률로 하나 더", "내구도 무한 · 행운의 손길 50%\n광맥의 축복: 광석 캐면 성급함 II"),
    "axe": ("선제공격: 체력이 가득한 적에게 피해 1.8배", "내구도 무한 · 선제공격 2.6배\n처형: 체력 30% 이하인 적에게 1.5배"),
    "shovel": ("보물 찾기: 흙·모래·자갈에서 2% 확률로 에메랄드·금 조각·가끔 다이아", "내구도 무한 · 보물 찾기 7% (다이아 확률 2배)"),
    "hoe": ("풍작: 다 자란 작물 30% 확률로 수확량 두 배", "내구도 무한 · 풍작 75%\n대지의 축복: 손에 들면 주변 9×9 작물이 빨리 자람"),
}
LANG_KO = {
    "item.wildplus.display_greatsword": "황금 왕의 대검 (관상용)",
    "tooltip.wildplus.display_greatsword": "명품관 한정 · 들고 다니며 뽐내는 장식 무기",
    "item.wildplus.display_staff": "별빛 지팡이 (관상용)",
    "tooltip.wildplus.display_staff": "명품관 한정 · 들고 다니며 뽐내는 장식 무기",
    "item.wildplus.display_scythe": "공허의 낫 (관상용)",
    "tooltip.wildplus.display_scythe": "명품관 한정 · 들고 다니며 뽐내는 장식 무기",
    "block.wildplus.arena_desk": "투기장 접수처",
    "tooltip.wildplus.arena_desk": "투기장 경기 만들기 · 참가 · 관전 · 랭킹 · 메달 상점\n/arena 로도 열 수 있음",
    "item.wildplus.life_crystal": "생명의 결정",
    "tooltip.wildplus.life_crystal": "불사의 토템 + 바다의 심장 + 에메랄드 블록 2 → 2개\n강화대에서 에메랄드 장비를 로열 에메랄드로 승급",
    "equipment.wildplus.emerald": "에메랄드",
    "equipment.wildplus.royal_emerald": "로열 에메랄드",
    "item.wildplus.emerald_bow": "에메랄드 활",
    "tooltip.wildplus.emerald_bow": "생명의 화살: 맞히면 내 체력 1 회복 · 당김 1.3배 · 피해 1.25배" + UP,
    "item.wildplus.royal_emerald_bow": "로열 에메랄드 활",
    "tooltip.wildplus.royal_emerald_bow": "맞히면 체력 3 회복 + 재생 + 흡수 · 당김 1.6배 · 피해 1.5배 · 내구도 무한",
    "item.wildplus.royal_emerald_apple": "로열 에메랄드 사과",
    "tooltip.wildplus.royal_emerald_apple": "재생 II (1분) · 흡수 IV (3분)\n행운 II · 마을의 영웅 III · 체력 증가 II (5분)",
}
LANG_EN = {"item.wildplus.display_greatsword": "Golden King's Greatsword", "item.wildplus.display_staff": "Starlight Staff",
           "item.wildplus.display_scythe": "Void Scythe", "block.wildplus.arena_desk": "Arena Desk", "item.wildplus.life_crystal": "Life Crystal", "item.wildplus.emerald_bow": "Emerald Bow",
           "item.wildplus.royal_emerald_bow": "Royal Emerald Bow", "item.wildplus.royal_emerald_apple": "Royal Emerald Apple",
           "equipment.wildplus.emerald": "Emerald", "equipment.wildplus.royal_emerald": "Royal Emerald"}
for t in TOOLS + ARMOR:
    LANG_KO[f"item.wildplus.emerald_{t}"] = f"에메랄드 {KO[t]}"
    LANG_EN[f"item.wildplus.emerald_{t}"] = f"Emerald {EN[t]}"
    LANG_KO[f"item.wildplus.royal_emerald_{t}"] = f"로열 에메랄드 {KO[t]}"
    LANG_EN[f"item.wildplus.royal_emerald_{t}"] = f"Royal Emerald {EN[t]}"
for t in TOOLS:
    LANG_KO[f"tooltip.wildplus.emerald_{t}"] = TIP[t][0] + UP
    LANG_KO[f"tooltip.wildplus.royal_emerald_{t}"] = TIP[t][1]
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.emerald_{a}"] = "2부위: 행운 I · 마을의 영웅 I (거래 할인)\n4부위: 체력 증가 I (+2하트) · 받는 피해 10% 감소" + UP
    LANG_KO[f"tooltip.wildplus.royal_emerald_{a}"] = ("내구도 무한 · 불에 타지 않음\n2부위: 행운 II · 마을의 영웅 II · 재생 I\n"
                                                      "4부위: 체력 증가 III (+6하트) · 저항 I\n생명의 가호: 체력 30% 아래면 흡수 III + 재생 III (40초마다)")


def assets():
    write(ASSETS / "models/block/arena_desk.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": i("block/arena_desk_side"), "top": "minecraft:block/polished_blackstone_bricks", "bottom": "minecraft:block/polished_blackstone_bricks"}})
    write(ASSETS / "blockstates/arena_desk.json", {"variants": {"": {"model": i("block/arena_desk")}}})
    write(ASSETS / "items/arena_desk.json", {"model": {"type": "minecraft:model", "model": i("block/arena_desk")}})
    simple("life_crystal")
    for w in ["greatsword", "staff", "scythe"]:
        simple(f"display_{w}", "minecraft:item/handheld")
    simple("royal_emerald_apple")
    for p in ["emerald", "royal_emerald"]:
        for t in TOOLS:
            simple(f"{p}_{t}", "minecraft:item/handheld")
        for a in ARMOR:
            simple(f"{p}_{a}")
        bow(f"{p}_bow")
        write(ASSETS / f"equipment/{p}.json", {"layers": {
            "humanoid": [{"texture": i(p)}], "humanoid_leggings": [{"texture": i(p)}]}})


def data():
    E, ST = "minecraft:emerald", "minecraft:stick"
    rec = {
        "emerald_sword": shaped(["X", "X", "#"], {"X": E, "#": ST}, i("emerald_sword")),
        "emerald_pickaxe": shaped(["XXX", " # ", " # "], {"X": E, "#": ST}, i("emerald_pickaxe")),
        "emerald_axe": shaped(["XX", "X#", " #"], {"X": E, "#": ST}, i("emerald_axe")),
        "emerald_shovel": shaped(["X", "#", "#"], {"X": E, "#": ST}, i("emerald_shovel")),
        "emerald_hoe": shaped(["XX", " #", " #"], {"X": E, "#": ST}, i("emerald_hoe")),
        "emerald_helmet": shaped(["XXX", "X X"], {"X": E}, i("emerald_helmet")),
        "emerald_chestplate": shaped(["X X", "XXX", "XXX"], {"X": E}, i("emerald_chestplate")),
        "emerald_leggings": shaped(["XXX", "X X", "X X"], {"X": E}, i("emerald_leggings")),
        "emerald_boots": shaped(["X X", "X X"], {"X": E}, i("emerald_boots")),
        "emerald_bow": shaped([" MS", "M S", " MS"], {"M": E, "S": "minecraft:string"}, i("emerald_bow")),
        "arena_desk": shaped(["SIS", "BGB", "BBB"], {"S": "minecraft:iron_sword", "I": "minecraft:iron_ingot", "G": "minecraft:gold_block",
                                                     "B": "minecraft:polished_blackstone_bricks"}, i("arena_desk"), category="misc"),
        "life_crystal": shapeless(["minecraft:totem_of_undying", "minecraft:heart_of_the_sea", "minecraft:emerald_block",
                                   "minecraft:emerald_block"], i("life_crystal"), 2),
        "royal_emerald_apple": shaped(["BBB", "BAB", "BCB"], {"B": "minecraft:emerald_block", "A": i("emerald_apple"), "C": i("life_crystal")},
                                      i("royal_emerald_apple"), category="misc"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    write(DATA / "tags/item/emerald_repair.json", {"values": [E]})
    from data_v2 import self_loot
    write(DATA / "loot_table/blocks/arena_desk.json", self_loot("arena_desk"))
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("arena_desk")])
    t = MC / "tags"
    for p in ["emerald", "royal_emerald"]:
        for t_ in TOOLS:
            merge_tag(t / f"item/{t_}s.json", [i(f"{p}_{t_}")])
        for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
            merge_tag(t / f"item/{slot}_armor.json", [i(f"{p}_{piece}")])
    bows = [i("emerald_bow"), i("royal_emerald_bow")]
    merge_tag(t / "item/enchantable/bow.json", bows)
    merge_tag(t / "item/enchantable/durability.json", bows)
    merge_tag(t / "item/enchantable/vanishing.json", bows)


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
