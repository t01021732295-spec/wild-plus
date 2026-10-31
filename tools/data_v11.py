"""12차 데이터: 대형 던전(고대 지하 묘지) 월드 생성 · 보물, 망자의 두루마리, 하루 환율 문구."""
from data_v2 import ASSETS, DATA, entry, i, shaped, write

DUNGEON_BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:birch_forest",
                  "minecraft:dark_forest", "minecraft:taiga", "minecraft:snowy_plains", "minecraft:savanna",
                  "minecraft:desert", "minecraft:meadow", "minecraft:old_growth_spruce_taiga", "minecraft:windswept_hills",
                  "minecraft:badlands", "minecraft:jungle", "minecraft:cherry_grove", "minecraft:swamp"]

LANG_KO = {
    "message.wildplus.honor.heal": "완전 회복! 체력·배고픔 가득, 나쁜 효과 제거, 흡수 II 2분",
    "message.wildplus.honor.merchant": "%s 이(가) 찾아왔어요 (10분 뒤 떠나요)",
    "message.wildplus.honor.map": "보물 지도: 가장 가까운 던전·도적 아지트는 X %s, Z %s (약 %s칸)",
    "message.wildplus.honor.map_none": "근처에 표시할 던전·도적 아지트가 없어요 (돈은 빠지지 않았어요)",
    "message.wildplus.honor.guard": "수호의 축복! 30분 동안 저항 I · 재생 I · 화염 저항",
    "structure.wildplus.dungeon": "고대 지하 묘지",
    "entity.wildplus.dungeon_guardian": "고대 수호자",
    "message.wildplus.dungeon_guardian.call": "고대 수호자가 해골 호위병을 불러냈다!",
    "message.wildplus.dungeon_guardian.dead": "★ %s 님이 고대 수호자를 쓰러뜨렸다! 묘지의 보물 창고가 열렸다",
    "item.wildplus.death_scroll": "망자의 두루마리",
    "tooltip.wildplus.death_scroll": "3초 동안 읽으면 마지막으로 죽은 곳으로 순간이동 (다른 차원도)\n도착하면 10초간 화염 저항 · 느린 낙하 · 저항",
    "message.wildplus.death_scroll.none": "아직 죽은 기록이 없어요",
    "message.wildplus.death_scroll.done": "마지막으로 죽은 곳 (%s, %s, %s) 으로 왔어요",
    "tooltip.wildplus.exchange": "동화 ↔ 은화 ↔ 금화 바꾸기\n수수료(환율)가 매일 아침 바뀜 · 살 때 10~12개, 팔 때 8~10개",
}
LANG_EN = {
    "message.wildplus.honor.heal": "Full restore! Health and food filled, bad effects cleared, Absorption II for 2 min",
    "message.wildplus.honor.merchant": "%s came to visit (leaves in 10 minutes)",
    "message.wildplus.honor.map": "Treasure map: nearest dungeon / bandit hideout at X %s, Z %s (~%s blocks)",
    "message.wildplus.honor.map_none": "No dungeon or bandit hideout nearby (you were not charged)",
    "message.wildplus.honor.guard": "Guardian's blessing! Resistance I, Regeneration I, Fire Resistance for 30 min",
    "structure.wildplus.dungeon": "Ancient Catacomb",
    "entity.wildplus.dungeon_guardian": "Ancient Guardian",
    "message.wildplus.dungeon_guardian.call": "The Ancient Guardian summoned skeleton guards!",
    "message.wildplus.dungeon_guardian.dead": "★ %s defeated the Ancient Guardian! The catacomb vault is open",
    "item.wildplus.death_scroll": "Scroll of the Departed",
    "tooltip.wildplus.death_scroll": "Read for 3 seconds to return to where you last died (any dimension)\nFire Resistance, Slow Falling and Resistance for 10s on arrival",
    "message.wildplus.death_scroll.none": "You haven't died yet",
    "message.wildplus.death_scroll.done": "Returned to where you last died (%s, %s, %s)",
    "tooltip.wildplus.exchange": "Swap copper ↔ silver ↔ gold\nDaily fee: buy 10-12, sell 8-10",
}


def book():
    return entry("minecraft:book", 10, extra={"type": "minecraft:enchant_randomly", "options": "#minecraft:on_random_loot"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    write(DATA / "tags/worldgen/structure/treasure_sites.json", {"values": [i("dungeon"), i("bandit_hideout")]})
    # 던전 월드 생성: 지표면에서 22칸 아래가 템플릿 바닥
    write(DATA / "tags/worldgen/biome/has_structure/dungeon.json", {"values": DUNGEON_BIOMES})
    write(DATA / "worldgen/structure/dungeon.json", {
        "type": "minecraft:jigsaw", "biomes": "#wildplus:has_structure/dungeon", "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 1, "spawn_overrides": {},
        "start_height": {"absolute": -22}, "start_pool": i("dungeon"), "step": "underground_structures",
        "terrain_adaptation": "none", "use_expansion_hack": False})
    write(DATA / "worldgen/template_pool/dungeon.json", {
        "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": i("dungeon"),
                                  "processors": {"processors": []}, "projection": "rigid"}, "weight": 1}],
        "fallback": "minecraft:empty"})
    write(DATA / "worldgen/structure_set/dungeons.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 993817265, "separation": 24, "spacing": 56},
        "structures": [{"structure": i("dungeon"), "weight": 1}]})
    write(DATA / "loot_table/chests/dungeon.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 8}, "entries": [
            entry(i("silver_coin"), 20, 4, 12), entry(i("gold_coin"), 10, 1, 4), entry("minecraft:iron_ingot", 15, 3, 8),
            entry("minecraft:gold_ingot", 12, 2, 6), entry("minecraft:diamond", 6, 1, 2), entry("minecraft:emerald", 8, 2, 5),
            entry(i("raw_mythril"), 8, 2, 4), entry(i("jade"), 6, 1, 3), entry("minecraft:bone", 15, 4, 10),
            entry("minecraft:golden_apple", 4), entry(i("recall_scroll"), 4), entry(i("death_scroll"), 3),
            entry("minecraft:name_tag", 3), entry("minecraft:saddle", 3), book()]}],
        "random_sequence": i("chests/dungeon")})
    write(DATA / "loot_table/chests/dungeon_vault.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 7, "max": 11}, "entries": [
            entry(i("gold_coin"), 18, 3, 9), entry("minecraft:diamond", 14, 2, 5), entry("minecraft:emerald", 10, 4, 10),
            entry(i("mythril_ingot"), 10, 2, 5), entry(i("jade"), 8, 3, 6), entry("minecraft:netherite_scrap", 5, 1, 2),
            entry("minecraft:golden_apple", 8, 1, 3), entry("minecraft:enchanted_golden_apple", 2), entry(i("lucky_charm"), 4),
            entry("minecraft:totem_of_undying", 2), entry("minecraft:diamond_block", 2), entry(i("lottery_ticket"), 6, 2, 5),
            book()]}],
        "random_sequence": i("chests/dungeon_vault")})
    # 망자의 두루마리 (종이 + 뼈 + 엔더 진주 + 은화)
    write(ASSETS / "models/item/death_scroll.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i("item/death_scroll")}})
    write(ASSETS / "items/death_scroll.json", {"model": {"type": "minecraft:model", "model": i("item/death_scroll")}})
    write(DATA / "recipe/death_scroll.json", shaped([" B ", "PEP", " S "], {"B": "minecraft:bone", "P": "minecraft:paper",
                                                                        "E": "minecraft:ender_pearl", "S": i("silver_coin")},
                                                    i("death_scroll"), category="misc"))
