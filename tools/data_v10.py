"""11차 데이터: 큰 마을 · 도적 아지트 월드 생성, 보물 상자, 랜덤 이벤트 24종, 새 효과 7종 · 물약."""
from data_v2 import ASSETS, DATA, entry, i, write
from data_v3 import BANDIT_BIOMES

EVENTS = {
    "gold_rush": ("골드 러시", "몬스터를 잡으면 동전이 우수수", "Gold Rush", "Monsters drop extra coins"),
    "meteor_shower": ("유성우", "주변에 유성이 떨어져 광물을 남김", "Meteor Shower", "Meteors crash nearby and leave ores"),
    "harvest_festival": ("풍년 축제", "주변 작물이 쑥쑥 자람", "Harvest Festival", "Crops nearby grow fast"),
    "fish_frenzy": ("물고기 떼", "행운 III · 돌고래의 은총 - 낚시 대박", "Fish Frenzy", "Luck III and Dolphin's Grace"),
    "monster_raid": ("몬스터 습격", "30초마다 몬스터 무리가 몰려옴", "Monster Raid", "Monster waves every 30 seconds"),
    "lucky_day": ("행운의 날", "모두에게 행운 II", "Lucky Day", "Luck II for everyone"),
    "merchant_caravan": ("상인 행렬", "떠돌이 상인이 찾아옴 (이벤트 끝나면 떠남)", "Merchant Caravan", "Merchants visit until the event ends"),
    "supply_drop": ("보급품 낙하", "근처에 보급 상자가 떨어짐 (좌표 공개)", "Supply Drop", "A supply crate lands nearby"),
    "slime_rain": ("황금 슬라임 비", "황금 슬라임이 쏟아짐", "Golden Slime Rain", "Golden slimes everywhere"),
    "xp_festival": ("경험치 축제", "몬스터 경험치 2배", "XP Festival", "Double mob experience"),
    "mining_fever": ("채굴 열풍", "모두에게 성급함 II", "Mining Fever", "Haste II for everyone"),
    "storm": ("대폭풍", "천둥 번개가 몰아침", "Great Storm", "Thunder and lightning"),
    "plague": ("역병", "가끔 배고픔·나약함에 걸림", "Plague", "Hunger and weakness strike at random"),
    "tax_holiday": ("세금 면제의 날", "이 동안 번 돈엔 세금이 없음", "Tax Holiday", "No income tax for a while"),
    "bank_bonus": ("은행 감사 이벤트", "통장 잔액의 2% 보너스 (최대 300동)", "Bank Bonus", "2% bonus on bank balance (max 300)"),
    "treasure_hunt": ("보물 찾기", "땅속에 묻힌 보물 상자 (대략적인 좌표 공개)", "Treasure Hunt", "A chest is buried nearby (rough coords)"),
    "elite_invasion": ("정예 대습격", "1분마다 정예 몬스터 출현", "Elite Invasion", "Elite monsters every minute"),
    "blessing": ("하늘의 축복", "재생 · 저항", "Blessing", "Regeneration and Resistance"),
    "low_gravity": ("저중력", "높이 뛰고 천천히 떨어짐", "Low Gravity", "Jump high, fall slowly"),
    "swift_wind": ("신속의 바람", "모두에게 신속 II", "Swift Wind", "Speed II for everyone"),
    "coin_rain": ("동전 비", "하늘에서 동전이 떨어짐", "Coin Rain", "Coins fall from the sky"),
    "animal_migration": ("동물 대이동", "동물 무리가 나타남", "Animal Migration", "Herds of animals appear"),
    "ore_blessing": ("광맥의 축복", "광석을 캐면 50% 확률로 두 배", "Ore Blessing", "50% chance of double ore drops"),
    "wanted": ("현상수배", "현상수배범을 잡으면 500동", "Wanted", "Catch the outlaw for 500 coins"),
}

EFFECTS = {
    "miners_focus": ("광부의 집중", "Miner's Focus"),
    "ironskin": ("강철 피부", "Ironskin"),
    "greed": ("탐욕", "Greed"),
    "bleeding": ("출혈", "Bleeding"),
    "frailty": ("취약", "Frailty"),
    "berserk": ("광전사", "Berserk"),
    "glass_cannon": ("유리 대포", "Glass Cannon"),
}
POTIONS = {"miners_focus": "광부의 집중", "strong_miners_focus": "강력한 광부의 집중", "ironskin": "강철 피부", "greed": "탐욕",
           "bleeding": "출혈", "frailty": "취약", "berserk": "광전사", "glass_cannon": "유리 대포", "lifesteal": "흡혈"}
POTIONS_EN = {"miners_focus": "Miner's Focus", "strong_miners_focus": "Strong Miner's Focus", "ironskin": "Ironskin",
              "greed": "Greed", "bleeding": "Bleeding", "frailty": "Frailty", "berserk": "Berserk", "glass_cannon": "Glass Cannon",
              "lifesteal": "Lifesteal"}

LANG_KO = {
    "gui.wildplus.vending.fixed": "진열된 묶음째로 팝니다 (개수 그대로)",
    "message.wildplus.lottery.win": "당첨! %s동을 동전으로 받았어요 (세금 없음)",
    "gui.wildplus.vending.fixed_owned": "1개씩 팝니다 · 가격은 개당",
    "entity.wildplus.wanted": "현상수배범",
    "entity.wildplus.bandit_boss": "도적 두목",
    "structure.wildplus.bandit_hideout": "도적 아지트",
    "message.wildplus.event.end": "이벤트 [%s] 이(가) 끝났어요",
    "message.wildplus.event.bank_bonus": "은행 감사 이벤트: 통장에 %s동이 들어왔어요",
    "message.wildplus.event.treasure": "보물 상자가 %s 근처 땅속(3칸 아래)에 묻혔어요!",
    "message.wildplus.event.supply": "보급 상자가 %s 에 떨어졌어요! (연기를 찾아가세요)",
    "message.wildplus.event.wanted": "현상수배범이 X %s, Z %s 근처에 나타났어요! (빛나는 도적, 현상금 500동)",
    "message.wildplus.event.wanted_done": "★ %s 님이 현상수배범을 잡았어요! (현상금 500동)",
    "message.wildplus.event.plague_hit": "역병에 걸렸어요… (배고픔 · 나약함)",
    "wildplus.configuration.eventsEnabled": "랜덤 이벤트",
    "wildplus.configuration.eventInterval": "랜덤 이벤트 평균 간격 (분)",
    "wildplus.configuration.randomEvents": "랜덤 이벤트",
}
LANG_EN = {
    "gui.wildplus.vending.fixed": "Sold as the shown bundle",
    "message.wildplus.lottery.win": "Winner! You got %s coins (tax-free)",
    "gui.wildplus.vending.fixed_owned": "Sold one at a time · price per item",
    "entity.wildplus.wanted": "Wanted Outlaw",
    "entity.wildplus.bandit_boss": "Bandit Chief",
    "structure.wildplus.bandit_hideout": "Bandit Hideout",
    "message.wildplus.event.end": "Event [%s] is over",
    "message.wildplus.event.bank_bonus": "Bank bonus: %s coins added to your account",
    "message.wildplus.event.treasure": "A treasure chest is buried near %s (3 blocks deep)!",
    "message.wildplus.event.supply": "A supply crate landed at %s! (follow the smoke)",
    "message.wildplus.event.wanted": "A wanted outlaw appeared near X %s, Z %s! (glowing bandit, 500 coin bounty)",
    "message.wildplus.event.wanted_done": "★ %s caught the wanted outlaw! (500 coin bounty)",
    "message.wildplus.event.plague_hit": "You caught the plague... (hunger, weakness)",
    "wildplus.configuration.eventsEnabled": "Random events",
    "wildplus.configuration.eventInterval": "Average minutes between events",
    "wildplus.configuration.randomEvents": "Random events",
}
for k, (ko, kod, en, end) in EVENTS.items():
    LANG_KO[f"event.wildplus.{k}"] = ko
    LANG_KO[f"event.wildplus.{k}.desc"] = kod
    LANG_EN[f"event.wildplus.{k}"] = en
    LANG_EN[f"event.wildplus.{k}.desc"] = end
for k, (ko, en) in EFFECTS.items():
    LANG_KO[f"effect.wildplus.{k}"] = ko
    LANG_EN[f"effect.wildplus.{k}"] = en
for k, ko in POTIONS.items():
    en = POTIONS_EN[k]
    LANG_KO[f"item.minecraft.potion.effect.wildplus_{k}"] = f"{ko}의 물약"
    LANG_KO[f"item.minecraft.splash_potion.effect.wildplus_{k}"] = f"투척용 {ko}의 물약"
    LANG_KO[f"item.minecraft.lingering_potion.effect.wildplus_{k}"] = f"잔류형 {ko}의 물약"
    LANG_KO[f"item.minecraft.tipped_arrow.effect.wildplus_{k}"] = f"{ko}의 화살"
    LANG_EN[f"item.minecraft.potion.effect.wildplus_{k}"] = f"Potion of {en}"
    LANG_EN[f"item.minecraft.splash_potion.effect.wildplus_{k}"] = f"Splash Potion of {en}"
    LANG_EN[f"item.minecraft.lingering_potion.effect.wildplus_{k}"] = f"Lingering Potion of {en}"
    LANG_EN[f"item.minecraft.tipped_arrow.effect.wildplus_{k}"] = f"Arrow of {en}"


def worldgen():
    # 큰 마을: 80×80
    write(DATA / "worldgen/structure/town.json", {
        "type": "minecraft:jigsaw", "biomes": "#wildplus:has_structure/town", "max_distance_from_center": 100,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 1, "spawn_overrides": {},
        "start_height": {"absolute": 0}, "start_pool": i("town"), "step": "surface_structures",
        "terrain_adaptation": "beard_box", "use_expansion_hack": False})
    write(DATA / "worldgen/structure_set/towns.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 190341277, "separation": 24, "spacing": 56},
        "structures": [{"structure": i("town"), "weight": 1}]})
    # 도적 아지트
    write(DATA / "tags/worldgen/biome/has_structure/bandit_hideout.json", {"values": BANDIT_BIOMES})
    write(DATA / "worldgen/structure/bandit_hideout.json", {
        "type": "minecraft:jigsaw", "biomes": "#wildplus:has_structure/bandit_hideout", "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 1, "spawn_overrides": {},
        "start_height": {"absolute": 0}, "start_pool": i("bandit_hideout"), "step": "surface_structures",
        "terrain_adaptation": "beard_box", "use_expansion_hack": False})
    write(DATA / "worldgen/template_pool/bandit_hideout.json", {
        "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i("bandit_hideout"),
                                  "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}],
        "fallback": "minecraft:empty"})
    write(DATA / "worldgen/structure_set/bandit_hideouts.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 448812301, "separation": 30, "spacing": 72},
        "structures": [{"structure": i("bandit_hideout"), "weight": 1}]})


def book():
    return entry("minecraft:book", 10, extra={"type": "minecraft:enchant_randomly", "options": "#minecraft:on_random_loot"})


def loot():
    write(DATA / "loot_table/chests/bandit_hideout.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 6, "max": 10}, "entries": [
            entry(i("gold_coin"), 18, 2, 6), entry(i("silver_coin"), 22, 5, 15), entry("minecraft:diamond", 10, 1, 3),
            entry("minecraft:emerald", 12, 2, 6), entry(i("mythril_ingot"), 8, 1, 3), entry(i("jade"), 8, 2, 4),
            entry("minecraft:gold_ingot", 12, 3, 8), entry(i("blood_shard"), 8, 2, 6), entry("minecraft:golden_apple", 6, 1, 2),
            entry(i("recall_scroll"), 5), entry("minecraft:saddle", 3), entry("minecraft:name_tag", 3),
            entry(i("lucky_charm"), 3), entry("minecraft:netherite_scrap", 2), entry("minecraft:enchanted_golden_apple", 1),
            entry(i("lottery_ticket"), 6, 1, 3), book()]}],
        "random_sequence": i("chests/bandit_hideout")})
    write(DATA / "loot_table/gameplay/supply_drop.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry("minecraft:cooked_beef", 15, 4, 10), entry("minecraft:golden_carrot", 10, 3, 8), entry("minecraft:arrow", 10, 16, 32),
            entry("minecraft:iron_ingot", 12, 4, 10), entry(i("silver_coin"), 14, 3, 10), entry(i("gold_coin"), 5, 1, 3),
            entry("minecraft:golden_apple", 4), entry("minecraft:experience_bottle", 8, 4, 10), entry(i("traveler_ration"), 8, 2, 4),
            entry("minecraft:diamond", 3, 1, 2), entry("minecraft:ender_pearl", 5, 2, 4)]}],
        "random_sequence": i("gameplay/supply_drop")})
    write(DATA / "loot_table/gameplay/treasure_hunt.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 5, "max": 8}, "entries": [
            entry(i("gold_coin"), 16, 3, 8), entry("minecraft:diamond", 12, 2, 4), entry("minecraft:emerald", 12, 3, 8),
            entry(i("mythril_ingot"), 8, 2, 4), entry(i("jade"), 8, 2, 5), entry("minecraft:gold_block", 4),
            entry(i("lucky_charm"), 4), entry("minecraft:heart_of_the_sea", 2), entry("minecraft:totem_of_undying", 1), book()]}],
        "random_sequence": i("gameplay/treasure_hunt")})


MIXES = [  # (재료 물약, 재료, 결과 물약)
    ("minecraft:awkward", i("raw_mythril"), i("miners_focus")),
    (i("miners_focus"), "minecraft:glowstone_dust", i("strong_miners_focus")),
    ("minecraft:awkward", "minecraft:iron_ingot", i("ironskin")),
    ("minecraft:awkward", i("gold_coin"), i("greed")),
    ("minecraft:awkward", i("blood_shard"), i("lifesteal")),
    (i("lifesteal"), "minecraft:fermented_spider_eye", i("bleeding")),
    (i("ironskin"), "minecraft:fermented_spider_eye", i("frailty")),
    ("minecraft:awkward", i("chili"), i("berserk")),
    (i("berserk"), i("jade"), i("glass_cannon")),
]
OURS = ["miners_focus", "strong_miners_focus", "ironskin", "greed", "lifesteal", "bleeding", "frailty", "berserk", "glass_cannon"]


def brew(container_in, potion_in, reagent, container_out, potion_out):
    return {"type": "minecraft:brewing",
            "input": {"item": container_in, "potion_contents": {"potions": potion_in}},
            "output": {"components": {"minecraft:potion_contents": {"potion": potion_out}}, "id": container_out},
            "reagent": {"item": reagent}}


def brewing():
    for (pin, reagent, pout) in MIXES:
        for c in ("potion", "splash_potion", "lingering_potion"):
            name = f"brewing/{c}_{pin.split(':')[1]}_{reagent.split(':')[1]}"
            write(DATA / "recipe" / f"{name}.json", brew(f"minecraft:{c}", pin, reagent, f"minecraft:{c}", pout))
    for p in OURS:
        write(DATA / "recipe" / f"brewing/potion_{p}_gunpowder.json",
              brew("minecraft:potion", i(p), "minecraft:gunpowder", "minecraft:splash_potion", i(p)))
        write(DATA / "recipe" / f"brewing/splash_potion_{p}_dragon_breath.json",
              brew("minecraft:splash_potion", i(p), "minecraft:dragon_breath", "minecraft:lingering_potion", i(p)))


def build(lang_ko, lang_en):
    brewing()
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    worldgen()
    loot()
