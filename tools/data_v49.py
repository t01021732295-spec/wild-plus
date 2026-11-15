"""5.1.1 네더 마무리 (data_v49): 겁화 각인석 4종 + 제련 각인석 · 바이옴 2종(잿불 황야 · 재의 숲) · 요새/보루 상자 보강."""
from PIL import Image

from data_v2 import ASSETS, DATA, i, write
from data_v18 import simple
from data_v35 import stone_icon

LANG_KO = {
    "item.wildplus.engrave_inferno": "업화 각인석", "item.wildplus.engrave_pyre": "화장 각인석", "item.wildplus.engrave_cinder": "잿불 갑주 각인석",
    "item.wildplus.engrave_phoenix": "불사조 각인석", "item.wildplus.engrave_smelt": "제련 각인석",
    "tooltip.wildplus.engrave_inferno": "겁화 검 · 겁화 활 전용 (강화대 각인)\n불타는 적에게 피해 +25%",
    "tooltip.wildplus.engrave_pyre": "겁화 활 전용 (강화대 각인)\n화살이 맞은 자리 3칸에 불기둥 (피해 6 · 불붙임)",
    "tooltip.wildplus.engrave_cinder": "겁화 갑옷 전용 (강화대 각인)\n나를 때린 적에게 피해 2 + 불 (부위마다, 최대 8)",
    "tooltip.wildplus.engrave_phoenix": "겁화 갑옷 전용 (강화대 각인)\n불·용암 피해 무시 · 불타는 동안 초당 체력 1 회복",
    "tooltip.wildplus.engrave_smelt": "도구 공용 (강화대 각인)\n캔 철·금·구리 원석이 바로 주괴로",
    "biome.wildplus.ember_wastes": "잿불 황야", "biome.wildplus.ash_grove": "재의 숲",
}
LANG_EN = {"item.wildplus.engrave_inferno": "Inferno Engraving Stone", "item.wildplus.engrave_pyre": "Pyre Engraving Stone",
           "item.wildplus.engrave_cinder": "Cinder Engraving Stone", "item.wildplus.engrave_phoenix": "Phoenix Engraving Stone",
           "item.wildplus.engrave_smelt": "Smelting Engraving Stone", "biome.wildplus.ember_wastes": "Ember Wastes", "biome.wildplus.ash_grove": "Ash Grove"}

STONES = {"inferno": ((236, 92, 40), (60, 20, 16)), "pyre": ((255, 176, 60), (70, 30, 10)), "cinder": ((150, 40, 46), (30, 14, 16)),
          "phoenix": ((255, 214, 120), (120, 40, 20)), "smelt": ((200, 120, 70), (70, 60, 60))}

WASTES_7 = ["minecraft:spring_open", "minecraft:patch_fire", "minecraft:patch_soul_fire", "minecraft:glowstone_extra", "minecraft:glowstone",
            "minecraft:brown_mushroom_nether", "minecraft:red_mushroom_nether", "minecraft:ore_magma", "minecraft:spring_closed", "minecraft:ore_gravel_nether",
            "minecraft:ore_blackstone", "minecraft:ore_gold_nether", "minecraft:ore_quartz_nether", "minecraft:ore_ancient_debris_large", "minecraft:ore_debris_small"]
WASTES_9 = ["minecraft:spring_lava", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal"]


def spawn(t, n, w):
    return {"type": t, "count": n, "weight": w}


def biome(name, fog, particles, monsters, music):
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 12000, "sound": music}},
            "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {
                "creature": [{"type": "minecraft:strider", "count": {"type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1}, "weight": 60}],
                "monster": monsters}}, "modifier": "overlay"},
            "minecraft:visual/ambient_particles": {"argument": particles, "modifier": "append"},
            "minecraft:visual/fog_color": fog,
        },
        "carvers": "minecraft:nether_cave", "downfall": 0.0, "effects": {"water_color": "#3f76e4"},
        "features": [[], [], [], [], [], [], [], WASTES_7, [], WASTES_9 + [i(name)]],
        "has_precipitation": False, "temperature": 2.0,
    }


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    for k, (c, frame) in STONES.items():
        stone_icon(c, frame).save(ASSETS / f"textures/item/engrave_{k}.png")
        simple(f"engrave_{k}")
    # 바이옴
    write(DATA / "worldgen/biome/ember_wastes.json", biome("ember_wastes", "#4a1206",
          [{"particle": {"type": "minecraft:ash"}, "probability": 0.06}, {"particle": {"type": "minecraft:small_flame"}, "probability": 0.004}],
          [spawn(i("ember_hound"), 2, 40), spawn(i("lava_turtle"), 1, 18), spawn("minecraft:magma_cube", 3, 40), spawn("minecraft:ghast", 2, 20),
           spawn("minecraft:zombified_piglin", 3, 40), spawn("minecraft:piglin", 3, 10)], "minecraft:music.nether.nether_wastes"))
    write(DATA / "worldgen/biome/ash_grove.json", biome("ash_grove", "#2c2a30",
          [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.12}],
          [spawn(i("ash_shaman"), 1, 30), spawn(i("obsidian_golem"), 1, 10), spawn("minecraft:skeleton", 3, 40), spawn("minecraft:ghast", 2, 30),
           spawn("minecraft:enderman", 2, 10)], "minecraft:music.nether.soul_sand_valley"))
    for n in ("ember_wastes", "ash_grove"):
        write(DATA / f"worldgen/feature/{n}.json", {"type": i(n)})
        write(DATA / f"worldgen/placed_feature/{n}.json", {"feature": i(n), "placement": []})
    write(DATA.parent / "minecraft/tags/worldgen/biome/is_nether.json", {"replace": False, "values": [i("ember_wastes"), i("ash_grove")]})
    # 요새 · 보루 상자 보강
    def e(name, w, lo, hi):
        return {"type": "minecraft:item", "name": name, "weight": w, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    write(DATA / "loot_table/inject/nether_v51.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [e(i("ember_shard"), 10, 2, 6), e(i("raw_kalpa"), 3, 1, 2), {"type": "minecraft:empty", "weight": 6}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i("engrave_smelt"), "weight": 1}, {"type": "minecraft:empty", "weight": 12}]},
    ], "random_sequence": i("inject/nether_v51")})
    tables = ["nether_bridge", "bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable"]
    write(DATA / "loot_modifiers/nether_v51.json", {"type": "neoforge:add_table", "condition": {"type": "minecraft:any_of", "terms": [
        {"type": "neoforge:loot_table_id", "loot_table_id": f"minecraft:chests/{t}"} for t in tables]}, "table": i("inject/nether_v51")})
