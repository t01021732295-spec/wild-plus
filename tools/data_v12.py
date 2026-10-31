"""13차 데이터: 루비 광석·도구·갑옷, 이벤트 8종 추가, 이벤트 투척병, 환율 고정."""
from data_v2 import ASSETS, DATA, MC, cook, i, merge_tag, ore_feature, ore_loot, placed, self_loot, shaped, shapeless, write

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
KO = {"sword": "검", "pickaxe": "곡괭이", "axe": "도끼", "shovel": "삽", "hoe": "괭이",
      "helmet": "투구", "chestplate": "흉갑", "leggings": "레깅스", "boots": "부츠"}
EN = {"sword": "Sword", "pickaxe": "Pickaxe", "axe": "Axe", "shovel": "Shovel", "hoe": "Hoe",
      "helmet": "Helmet", "chestplate": "Chestplate", "leggings": "Leggings", "boots": "Boots"}

EVENTS = {
    "treasure_goblin": ("보물 토끼", "빛나는 보물 토끼를 잡으면 금화·다이아몬드", "Treasure Rabbits", "Catch the glowing rabbits for gold and diamonds"),
    "ore_rain": ("광물 비", "하늘에서 광물이 쏟아짐 (아주 드물게 루비)", "Ore Rain", "Ores fall from the sky (rarely a ruby)"),
    "bloom": ("꽃피는 봄", "주변에 꽃이 피고 동물들이 사랑에 빠짐", "Spring Bloom", "Flowers bloom and animals fall in love"),
    "frost_wind": ("한파", "느려지고 물이 얼어붙음 (화염 저항이면 괜찮음)", "Cold Snap", "You slow down and water freezes (fire resistance helps)"),
    "hero_time": ("영웅의 시간", "힘 II · 신속 I", "Hero Time", "Strength II and Speed I"),
    "double_drops": ("풍요", "몹을 잡으면 전리품 두 배", "Bounty", "Mobs drop double loot"),
    "full_moon": ("사냥꾼의 달", "몬스터 경험치 3배 · 25% 확률로 은화", "Hunter's Moon", "Monsters give triple XP and sometimes silver"),
    "vampire_night": ("흡혈의 밤", "때린 피해의 25%만큼 체력 회복", "Vampire Night", "Heal 25% of the melee damage you deal"),
}

LANG_KO = {
    "block.wildplus.ruby_ore": "루비 광석",
    "block.wildplus.deepslate_ruby_ore": "심층암 루비 광석",
    "block.wildplus.ruby_block": "루비 블록",
    "item.wildplus.ruby": "루비",
    "tooltip.wildplus.ruby": "다이아몬드보다 깊은 곳(Y 0 아래)에서 드물게 나는 보석\n루비 장비는 불·용암에 타지 않음",
    "tooltip.wildplus.ruby_sword": "화염검: 맞은 적을 4초 동안 불태움\n25% 확률로 폭염 (주변 3칸 적에게 피해 4 + 불)",
    "tooltip.wildplus.ruby_pickaxe": "광맥 탐지: 웅크리고 들면 주변 8칸 광석에 불꽃 표시",
    "tooltip.wildplus.ruby_axe": "처형: 불타는 적에게 피해 1.5배 (루비 검과 같이 쓰기 좋음)",
    "tooltip.wildplus.ruby_shovel": "용광 삽: 모래 → 유리, 자갈 → 부싯돌, 점토 → 벽돌로 바로 구워짐",
    "tooltip.wildplus.ruby_hoe": "자동 파종: 다 자란 작물을 캐면 씨앗으로 다시 심어 줌",
    "item.wildplus.event_bottle": "이벤트 투척병",
    "item.wildplus.event_bottle.random": "이벤트 투척병 (무작위)",
    "item.wildplus.event_bottle.of": "이벤트 투척병: %s",
    "tooltip.wildplus.event_bottle": "던져서 깨뜨리면 이벤트가 바로 시작됨\n진행 중인 이벤트는 끝나고 바뀜 · 무작위 병은 위험한 이벤트도 나옴",
    "message.wildplus.event_bottle.used": "✦ %s 님이 이벤트 투척병을 깨뜨렸어요!",
    "entity.wildplus.treasure_rabbit": "보물 토끼",
    "equipment.wildplus.ruby": "루비",
}
LANG_EN = {
    "block.wildplus.ruby_ore": "Ruby Ore",
    "block.wildplus.deepslate_ruby_ore": "Deepslate Ruby Ore",
    "block.wildplus.ruby_block": "Block of Ruby",
    "item.wildplus.ruby": "Ruby",
    "tooltip.wildplus.ruby": "A rare gem found deeper than diamonds (below Y 0)\nRuby gear never burns",
    "tooltip.wildplus.ruby_sword": "Flame Blade: sets targets on fire for 4s\n25% chance to burst (4 damage + fire within 3 blocks)",
    "tooltip.wildplus.ruby_pickaxe": "Vein Sense: sneak to reveal ores within 8 blocks",
    "tooltip.wildplus.ruby_axe": "Execute: 1.5x damage to burning targets",
    "tooltip.wildplus.ruby_shovel": "Kiln Shovel: sand → glass, gravel → flint, clay → bricks",
    "tooltip.wildplus.ruby_hoe": "Auto Replant: harvested crops are replanted from their seeds",
    "item.wildplus.event_bottle": "Event Bottle",
    "item.wildplus.event_bottle.random": "Event Bottle (Random)",
    "item.wildplus.event_bottle.of": "Event Bottle: %s",
    "tooltip.wildplus.event_bottle": "Throw it to start an event right away\nReplaces the current event · random bottles can be dangerous",
    "message.wildplus.event_bottle.used": "✦ %s shattered an event bottle!",
    "entity.wildplus.treasure_rabbit": "Treasure Rabbit",
    "equipment.wildplus.ruby": "Ruby",
}
for t in TOOLS + ARMOR:
    LANG_KO[f"item.wildplus.ruby_{t}"] = f"루비 {KO[t]}"
    LANG_EN[f"item.wildplus.ruby_{t}"] = f"Ruby {EN[t]}"
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.ruby_{a}"] = "2부위: 화염 저항\n4부위: 나를 때린 적이 불타고 받는 피해 10% 감소"
    LANG_EN[f"tooltip.wildplus.ruby_{a}"] = "2 pieces: Fire Resistance\n4 pieces: attackers catch fire, take 10% less damage"
for k, (ko, kod, en, end) in EVENTS.items():
    LANG_KO[f"event.wildplus.{k}"] = ko
    LANG_KO[f"event.wildplus.{k}.desc"] = kod
    LANG_EN[f"event.wildplus.{k}"] = en
    LANG_EN[f"event.wildplus.{k}.desc"] = end


def model(name, parent, tex):
    write(ASSETS / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": i(f"item/{tex}")}})
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})


def assets():
    for b in ["ruby_ore", "deepslate_ruby_ore", "ruby_block"]:
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{b}")}})
    model("ruby", "minecraft:item/generated", "ruby")
    model("event_bottle", "minecraft:item/generated", "event_bottle")
    for t in TOOLS:
        model(f"ruby_{t}", "minecraft:item/handheld", f"ruby_{t}")
    for a in ARMOR:
        model(f"ruby_{a}", "minecraft:item/generated", f"ruby_{a}")
    write(ASSETS / "equipment/ruby.json", {"layers": {
        "humanoid": [{"texture": i("ruby")}], "humanoid_leggings": [{"texture": i("ruby")}]}})


def data():
    for b in ["ruby_ore", "deepslate_ruby_ore"]:
        write(DATA / f"loot_table/blocks/{b}.json", ore_loot(b, i("ruby")))
    write(DATA / "loot_table/blocks/ruby_block.json", self_loot("ruby_block"))
    R, S = i("ruby"), "minecraft:stick"
    rec = {
        "ruby_block": shaped(["###", "###", "###"], {"#": R}, i("ruby_block"), category="building"),
        "ruby_from_block": shapeless([i("ruby_block")], R, 9),
        "ruby_from_smelting": cook("smelting", [i("ruby_ore"), i("deepslate_ruby_ore")], R, 1.0, 200, "misc"),
        "ruby_from_blasting": cook("blasting", [i("ruby_ore"), i("deepslate_ruby_ore")], R, 1.0, 100, "misc"),
        "ruby_sword": shaped(["X", "X", "#"], {"X": R, "#": S}, i("ruby_sword")),
        "ruby_pickaxe": shaped(["XXX", " # ", " # "], {"X": R, "#": S}, i("ruby_pickaxe")),
        "ruby_axe": shaped(["XX", "X#", " #"], {"X": R, "#": S}, i("ruby_axe")),
        "ruby_shovel": shaped(["X", "#", "#"], {"X": R, "#": S}, i("ruby_shovel")),
        "ruby_hoe": shaped(["XX", " #", " #"], {"X": R, "#": S}, i("ruby_hoe")),
        "ruby_helmet": shaped(["XXX", "X X"], {"X": R}, i("ruby_helmet")),
        "ruby_chestplate": shaped(["X X", "XXX", "XXX"], {"X": R}, i("ruby_chestplate")),
        "ruby_leggings": shaped(["XXX", "X X", "X X"], {"X": R}, i("ruby_leggings")),
        "ruby_boots": shaped(["X X", "X X"], {"X": R}, i("ruby_boots")),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    write(DATA / "tags/item/ruby_repair.json", {"values": [R]})
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i(b) for b in ["ruby_ore", "deepslate_ruby_ore", "ruby_block"]])
    merge_tag(t / "block/needs_iron_tool.json", [i(b) for b in ["ruby_ore", "deepslate_ruby_ore", "ruby_block"]])
    merge_tag(t / "block/beacon_base_blocks.json", [i("ruby_block")])
    merge_tag(t / "item/beacon_payment_items.json", [R])
    for t_ in TOOLS:
        merge_tag(t / f"item/{t_}s.json", [i(f"ruby_{t_}")])
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
        merge_tag(t / f"item/{slot}_armor.json", [i(f"ruby_{piece}")])
    merge_tag(MC.parent / "c/tags/block/ores.json", [i(f"{p}{m}_ore") for m in ("ruby", "mythril", "jade") for p in ("", "deepslate_")])
    merge_tag(MC.parent / "c/tags/item/gems.json", [R])
    # 월드 생성: 다이아몬드보다 깊고 드묾 (Y -64 ~ 0, 한 청크에 평균 2번 · 덩어리 3~4개)
    write(DATA / "worldgen/feature/ore_ruby.json", ore_feature("ruby", 4, 0.5))
    write(DATA / "worldgen/placed_feature/ore_ruby.json", placed(i("ore_ruby"), 2, {
        "type": "minecraft:trapezoid", "min_inclusive": {"absolute": -80}, "max_inclusive": {"absolute": 0}}))
    write(DATA / "neoforge/biome_modifier/add_ruby_ore.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": i("ore_ruby"), "step": "underground_ores"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
