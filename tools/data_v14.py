"""16차 데이터: 스텔라 루비 장비, 다이아 배낭, 필터 호퍼, 던전 봉인 벽돌, 출하 상자 창."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, shapeless, write

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
KO = {"sword": "검", "pickaxe": "곡괭이", "axe": "도끼", "shovel": "삽", "hoe": "괭이",
      "helmet": "투구", "chestplate": "흉갑", "leggings": "레깅스", "boots": "부츠"}
EN = {"sword": "Sword", "pickaxe": "Pickaxe", "axe": "Axe", "shovel": "Shovel", "hoe": "Hoe",
      "helmet": "Helmet", "chestplate": "Chestplate", "leggings": "Leggings", "boots": "Boots"}

LANG_KO = {
    "block.wildplus.filter_hopper": "필터 호퍼",
    "tooltip.wildplus.filter_hopper": "필터에 넣은 아이템만 옆(좌우)으로, 나머지는 바라보는 쪽(일자)으로\n아래를 보게 놓으면 필터 아이템은 네 옆면 중 상자가 있는 쪽으로",
    "block.wildplus.dungeon_seal": "봉인 벽돌",
    "item.wildplus.diamond_backpack": "다이아 배낭",
    "tooltip.wildplus.diamond_backpack": "98칸 (14×7). 우클릭으로 열기 · 불에 타지 않음",
    "gui.wildplus.backpack.tier3": "다이아 배낭",
    "tooltip.wildplus.ruby_sword": "화염검: 맞은 적을 4초 동안 불태움\n25% 확률로 폭염 (주변 3칸 적에게 피해 4 + 불)\n강화대에서 네더의 별 + 1,000동으로 스텔라 루비 승급",
}
LANG_EN = {
    "block.wildplus.filter_hopper": "Filter Hopper",
    "tooltip.wildplus.filter_hopper": "Filtered items go to the sides, everything else goes straight\nWhen facing down, filtered items go to any side container",
    "block.wildplus.dungeon_seal": "Sealed Bricks",
    "item.wildplus.diamond_backpack": "Diamond Backpack",
    "tooltip.wildplus.diamond_backpack": "98 slots (14×7). Right-click to open · fireproof",
    "gui.wildplus.backpack.tier3": "Diamond Backpack",
}
STAR_TIP_KO = {
    "sword": "공격력 9 · 공격 속도 1.6 · 내구도 무한\n화염검 + 방어 무시 고정 피해 (강화대에서 금화로 최대 6)",
    "pickaxe": "내구도 무한 · 광맥 탐지 강화 (웅크리면 16칸 안 광석 표시)\n광석을 캐면 25% 확률로 전리품 두 배",
    "axe": "내구도 무한 · 처형 강화: 때린 적에게 불 + 불타는 적에게 2배",
    "shovel": "내구도 무한 · 용광 삽 (모래→유리 등) + 3×3 파기 (웅크리면 한 칸)",
    "hoe": "내구도 무한 · 자동 파종 + 주변 3×3 작물 한 번에 수확\n50% 확률로 수확량 두 배 (웅크리면 한 칸)",
}
STAR_TIP_EN = {
    "sword": "9 attack · 1.6 speed · unbreakable\nFlame blade + armor-piercing true damage (up to 6 at the enhance table)",
    "pickaxe": "Unbreakable · Vein Sense", "axe": "Unbreakable · Execute", "shovel": "Unbreakable · Kiln Shovel", "hoe": "Unbreakable · Auto Replant",
}
for t in TOOLS + ARMOR:
    LANG_KO[f"item.wildplus.star_ruby_{t}"] = f"스텔라 루비 {KO[t]}"
    LANG_EN[f"item.wildplus.star_ruby_{t}"] = f"Stellar Ruby {EN[t]}"
for t in TOOLS:
    LANG_KO[f"tooltip.wildplus.star_ruby_{t}"] = STAR_TIP_KO[t]
    LANG_EN[f"tooltip.wildplus.star_ruby_{t}"] = STAR_TIP_EN[t]
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.star_ruby_{a}"] = "내구도 무한 · 불에 타지 않음\n2부위: 재생 I / 4부위: 재생 II + 화염 저항 + 야간 투시"
    LANG_EN[f"tooltip.wildplus.star_ruby_{a}"] = "Unbreakable · fireproof\n2 pieces: Regeneration I / 4 pieces: Regeneration II + Fire Resistance + Night Vision"
    LANG_KO[f"tooltip.wildplus.ruby_{a}"] = "2부위: 화염 저항\n4부위: 나를 때린 적이 불타고 받는 피해 10% 감소\n강화대에서 스텔라 루비로 승급 가능"


def model(name, parent, tex):
    write(ASSETS / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": i(f"item/{tex}")}})
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})


def assets():
    for t in TOOLS:
        model(f"star_ruby_{t}", "minecraft:item/handheld", f"star_ruby_{t}")
    for a in ARMOR:
        model(f"star_ruby_{a}", "minecraft:item/generated", f"star_ruby_{a}")
    model("diamond_backpack", "minecraft:item/generated", "diamond_backpack")
    write(ASSETS / "equipment/star_ruby.json", {"layers": {
        "humanoid": [{"texture": i("star_ruby")}], "humanoid_leggings": [{"texture": i("star_ruby")}]}})
    # 필터 호퍼: 바닐라 호퍼 모양에 미스릴 색 텍스처
    tex = {"particle": i("block/filter_hopper_outside"), "top": i("block/filter_hopper_top"),
           "side": i("block/filter_hopper_outside"), "inside": i("block/filter_hopper_inside")}
    write(ASSETS / "models/block/filter_hopper.json", {"parent": "minecraft:block/hopper", "textures": tex})
    write(ASSETS / "models/block/filter_hopper_side.json", {"parent": "minecraft:block/hopper_side", "textures": tex})
    write(ASSETS / "blockstates/filter_hopper.json", {"variants": {
        "facing=down": {"model": i("block/filter_hopper")},
        "facing=north": {"model": i("block/filter_hopper_side")},
        "facing=east": {"model": i("block/filter_hopper_side"), "y": 90},
        "facing=south": {"model": i("block/filter_hopper_side"), "y": 180},
        "facing=west": {"model": i("block/filter_hopper_side"), "y": 270}}})
    write(ASSETS / "items/filter_hopper.json", {"model": {"type": "minecraft:model", "model": i("block/filter_hopper")}})
    # 봉인 벽돌
    for name in ["dungeon_seal", "dungeon_seal_gate"]:
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{name}")}})
    write(ASSETS / "blockstates/dungeon_seal.json", {"variants": {
        "gate=false": {"model": i("block/dungeon_seal")}, "gate=true": {"model": i("block/dungeon_seal_gate")}}})


def data():
    write(DATA / "loot_table/blocks/filter_hopper.json", self_loot("filter_hopper"))
    rec = {
        "filter_hopper_mythril": shaped(["M M", "MHM", " C "], {"M": i("mythril_ingot"), "H": "minecraft:hopper",
                                                                 "C": "minecraft:comparator"}, i("filter_hopper"), category="redstone"),
        "filter_hopper_jade": shaped(["J J", "JHJ", " C "], {"J": i("jade"), "H": "minecraft:hopper",
                                                             "C": "minecraft:comparator"}, i("filter_hopper"), category="redstone"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i("filter_hopper")])
    for t_ in TOOLS:
        merge_tag(t / f"item/{t_}s.json", [i(f"star_ruby_{t_}")])
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
        merge_tag(t / f"item/{slot}_armor.json", [i(f"star_ruby_{piece}")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
