"""5.0 네더 데이터 (data_v44): 겁화석 광석 · 재료 · 겁화 장비 (공허와 같은 단계, 바닐라 형태 + 장식 — 입체 장식은 client/KalpaArmorLayer)."""
import json
import random

from PIL import Image

import data_v32 as V
from data_v2 import ASSETS, DATA, MC, i, merge_tag, ore_loot, placed, self_loot, shaped, shapeless, write
from data_v18 import bow, simple

TOOLS, ARMOR, ALL, KO, EN = V.TOOLS, V.ARMOR, V.ALL, V.KO, V.EN
# 검은 강철 → 진홍 → 금빛 불 → 흰 불
PAL = dict(K=(14, 8, 10), D=(40, 22, 26), M=(104, 22, 32), V=(196, 40, 46), G=(244, 156, 62), W=(255, 244, 214), T=(255, 208, 96))
TEX = ASSETS / "textures"

SET = "2부위: 화염 저항 · 힘 I · 최대 체력 +2칸\n4부위: 저항 I · 네더에서 신속 I · 최대 체력 +4칸 · 용암 위에 뜸"
TIP = {
    "sword": "공격력 10.5 · 공격 속도 1.9 · 내구도 무한\n겁화 표식: 때릴 때마다 표식 +1 (8초) · 불붙임\n5개째에 겁화 폭발: 그 적 16 · 주변 3.5칸 10 (방어 무시)",
    "bow": "당김 2배 · 피해 1.8배 · 내구도 무한\n맞히면 표식 +1 · 불붙임\n끝까지 당긴 화살은 폭발: 주변 3칸 방어 무시 피해 8",
    "pickaxe": "내구도 무한 · 5×5 채굴 (웅크리면 한 칸) · 원석 자동 제련\n광석 15% 확률로 2배 · 캔 것이 바로 가방으로",
    "axe": "내구도 무한 · 나무 통째로 베기 · 캔 것이 바로 가방으로",
    "shovel": "내구도 무한 · 5×5 파기 · 속도 3배 · 캔 것이 바로 가방으로",
    "hoe": "내구도 무한 · 7×7 수확 · 자동 파종 · 캔 것이 바로 가방으로",
    "helmet": SET + "\n투구: 3초마다 주변 6칸의 적에게 겁화 표식 +1",
    "chestplate": SET + "\n흉갑: 죽을 피해를 한 번 버티고 체력 40% 회복 + 주변 5칸 폭발 20 (5분마다)",
    "leggings": SET + "\n각반: 잃은 체력만큼 주는 피해 증가 (최대 +30%)",
    "boots": SET + "\n장화: 달린 자리에 3초 동안 불길이 남아 적을 태움",
}
LANG_KO = {
    "block.wildplus.kalpa_ore": "겁화석 광석", "block.wildplus.kalpa_block": "겁화석 블록",
    "item.wildplus.ember_shard": "잿불 파편", "item.wildplus.raw_kalpa": "겁화석 원석", "item.wildplus.kalpa_stone": "겁화석", "item.wildplus.kalpa_horn": "군주의 뿔",
    "tooltip.wildplus.ember_shard": "네더의 겁화 몬스터와 감시탑에서 얻는 파편",
    "tooltip.wildplus.raw_kalpa": "네더 깊은 곳(용암 바다 아래)에서 네더라이트 곡괭이로 캠\n원석 4 + 잿불 파편 4 → 겁화석",
    "tooltip.wildplus.kalpa_stone": "겁화 장비 재료",
    "tooltip.wildplus.kalpa_horn": "겁화의 군주가 떨어뜨리는 뿔\n강화대에서 미완성 겁화 장비를 완성하는 핵심 재료",
    "equipment.wildplus.kalpa": "겁화",
}
LANG_EN = {"block.wildplus.kalpa_ore": "Kalpa Ore", "block.wildplus.kalpa_block": "Kalpa Block", "item.wildplus.ember_shard": "Ember Shard",
           "item.wildplus.raw_kalpa": "Raw Kalpa Stone", "item.wildplus.kalpa_stone": "Kalpa Stone", "item.wildplus.kalpa_horn": "Lord's Horn", "equipment.wildplus.kalpa": "Kalpa"}
for a in ALL:
    LANG_KO[f"item.wildplus.kalpa_{a}"] = f"겁화 {KO[a]}"
    LANG_KO[f"tooltip.wildplus.kalpa_{a}"] = TIP[a]
    LANG_KO[f"item.wildplus.kalpa_{a}_blank"] = f"미완성 겁화 {KO[a]}"
    LANG_KO[f"tooltip.wildplus.kalpa_{a}_blank"] = "강화대에서 군주의 뿔과 돈으로 완성"
    LANG_EN[f"item.wildplus.kalpa_{a}"] = f"Kalpa {EN[a]}"
    LANG_EN[f"item.wildplus.kalpa_{a}_blank"] = f"Unfinished Kalpa {EN[a]}"


def ore(u, v):
    r = random.Random(f"k{u}{v}")
    base = (96, 34, 34) if r.random() < .5 else (78, 26, 28)
    if r.random() < .12:
        base = (58, 18, 22)
    for cx, cy, big in ((4, 4, 1), (11, 6, 1), (6, 11, 0), (13, 12, 0)):
        dd = abs(u - cx) + abs(v - cy)
        if dd == 0:
            return PAL["W"] if big else PAL["G"]
        if dd == 1:
            return PAL["G"] if big else PAL["T"]
        if big and dd == 2 and r.random() < .7:
            return PAL["V"]
        if dd <= 2 and r.random() < .4:
            return PAL["K"]
    return base


def kblock(u, v):
    if u in (0, 15) or v in (0, 15):
        return PAL["M"]
    if abs(u - 7.5) + abs(v - 7.5) < 2:
        return PAL["W"]
    if abs(u - 7.5) + abs(v - 7.5) < 4:
        return PAL["G"]
    if u == v or u + v == 15:
        return PAL["V"]
    return PAL["D"] if random.Random(f"b{u}{v}").random() < .6 else PAL["K"]


def textures():
    old = {k: getattr(V, k) for k in PAL}
    for k, c in PAL.items():
        setattr(V, k, c)
    try:
        src = TEX / "item"
        for a in ALL:
            base = V.tool_tex(src / f"ruby_{a}.png")
            if a in TOOLS or a == "bow":
                V.animated(f"kalpa_{a}", base)
            else:
                V.sparkle(base).save(src / f"kalpa_{a}.png")
            V.dim(base).save(src / f"kalpa_{a}_blank.png")
        for n in range(3):
            V.sparkle(V.tool_tex(src / f"ruby_bow_pulling_{n}.png")).save(src / f"kalpa_bow_pulling_{n}.png")
        eq = TEX / "entity/equipment"
        h = V.recolor(Image.open(eq / "humanoid/ruby.png"))
        l = V.recolor(Image.open(eq / "humanoid_leggings/ruby.png"))
        G, W, T, Vv = PAL["G"], PAL["W"], PAL["T"], PAL["V"]
        V.over(h, [(11, 9), (12, 9)], W); V.over(h, [(11, 10), (12, 10)], G); V.over(h, [(10, 9), (13, 9), (9, 8), (14, 8)], T)          # 이마 · 눈 틈
        V.over(h, [(23, 23), (24, 23), (23, 24), (24, 24)], W)                                                                          # 가슴 핵
        V.over(h, [(22, 23), (25, 23), (22, 24), (25, 24), (23, 22), (24, 22), (23, 25), (24, 25)], T)
        V.over(h, [(21, 22), (26, 22), (21, 26), (26, 26), (22, 27), (25, 27), (20, 21), (27, 21), (23, 28), (24, 29)], G)
        V.over(h, [(x, 21) for x in range(40, 52)], T); V.over(h, [(45, 21), (46, 21)], W)
        V.over(h, [(4 + n, 28) for n in range(4)], T)
        V.over(l, [(5, 24), (6, 24)], W); V.over(l, [(4, 25), (7, 25), (5, 23), (6, 23)], G)
        h.save(eq / "humanoid/kalpa.png"); l.save(eq / "humanoid_leggings/kalpa.png")
        K, D, M = PAL["K"], PAL["D"], PAL["M"]
        circle = lambda cx, cy, r2, a, b: [(x, y) for y in range(a, b) for x in range(a, b) if (x - cx) ** 2 + (y - cy) ** 2 <= r2]
        V.icon([(K, [(8, 2), (9, 3), (10, 5), (11, 8), (10, 11), (8, 13), (6, 12), (5, 9), (6, 6), (7, 4)]),
                (Vv, [(x, y) for y in range(4, 12) for x in range(6, 11) if abs(x - 8) <= 1 + abs(5 - abs(y - 8)) / 3]), (G, V.diag((8, 4), (8, 11))), (W, [(8, 6), (8, 7)])]).save(src / "ember_shard.png")
        V.icon([(K, circle(8, 8, 37, 1, 16)), (D, circle(8, 8, 22, 2, 15)), (Vv, [(5, 5), (6, 5), (6, 6), (9, 4), (10, 4), (10, 5), (11, 8), (10, 8), (6, 10), (7, 10)]),
                (G, [(6, 5), (10, 4), (11, 8), (7, 10)]), (W, [(8, 7)])]).save(src / "raw_kalpa.png")
        V.icon([(K, circle(8, 8, 37, 1, 16)), (M, circle(8, 8, 25, 2, 15)), (Vv, circle(7, 7, 14, 2, 14)), (G, [(5, 4), (6, 4), (7, 4), (5, 5), (6, 5), (6, 6), (7, 6), (7, 7)]),
                (W, [(5, 4), (6, 5)]), (T, [(10, 8), (9, 10), (8, 11)])]).save(src / "kalpa_stone.png")
        horn = [(4, 14), (5, 14), (5, 13), (6, 13), (6, 12), (7, 12), (7, 11), (8, 11), (8, 10), (9, 10), (9, 9), (10, 8), (10, 7), (11, 6), (11, 5), (12, 4), (12, 3), (12, 2)]
        thick = [(x + 1, y) for x, y in horn[:12]] + [(x, y - 1) for x, y in horn[:8]]
        V.icon([((70, 60, 58), thick), ((226, 214, 196), horn), (T, [(5, 13), (6, 12)]), (G, [(7, 11), (8, 10)]), (W, [(12, 2), (12, 3)]), (Vv, [(4, 14), (5, 14), (6, 14)])]).save(src / "kalpa_horn.png")
        bt = TEX / "block"
        V.block_tex(ore).save(bt / "kalpa_ore.png"); V.block_tex(kblock).save(bt / "kalpa_block.png")
    finally:
        for k, c in old.items():
            setattr(V, k, c)


def assets():
    for n in ("ember_shard", "raw_kalpa", "kalpa_stone", "kalpa_horn"):
        simple(n)
    for t in TOOLS:
        simple(f"kalpa_{t}", "minecraft:item/handheld")
    for a in ARMOR:
        simple(f"kalpa_{a}")
    bow("kalpa_bow")
    for a in ALL:
        simple(f"kalpa_{a}_blank", "minecraft:item/handheld" if a in TOOLS else "minecraft:item/generated")
    for b in ("kalpa_ore", "kalpa_block"):
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        write(ASSETS / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{b}")}})
    write(ASSETS / "equipment/kalpa.json", {"layers": {"humanoid": [{"texture": i("kalpa")}], "humanoid_leggings": [{"texture": i("kalpa")}]}})


def data():
    write(DATA / "loot_table/blocks/kalpa_ore.json", ore_loot("kalpa_ore", i("raw_kalpa")))
    write(DATA / "loot_table/blocks/kalpa_block.json", self_loot("kalpa_block"))
    S, ST, B = i("kalpa_stone"), "minecraft:blaze_rod", lambda a: i(f"kalpa_{a}_blank")
    rec = {
        "kalpa_stone": shapeless([i("raw_kalpa")] * 4 + [i("ember_shard")] * 4, S),
        "kalpa_block": shaped(["###", "###", "###"], {"#": S}, i("kalpa_block"), category="building"),
        "kalpa_stone_from_block": shapeless([i("kalpa_block")], S, 9),
        "kalpa_sword_blank": shaped(["X", "X", "#"], {"X": S, "#": ST}, B("sword")),
        "kalpa_pickaxe_blank": shaped(["XXX", " # ", " # "], {"X": S, "#": ST}, B("pickaxe")),
        "kalpa_axe_blank": shaped(["XX", "X#", " #"], {"X": S, "#": ST}, B("axe")),
        "kalpa_shovel_blank": shaped(["X", "#", "#"], {"X": S, "#": ST}, B("shovel")),
        "kalpa_hoe_blank": shaped(["XX", " #", " #"], {"X": S, "#": ST}, B("hoe")),
        "kalpa_helmet_blank": shaped(["XXX", "X X"], {"X": S}, B("helmet")),
        "kalpa_chestplate_blank": shaped(["X X", "XXX", "XXX"], {"X": S}, B("chestplate")),
        "kalpa_leggings_blank": shaped(["XXX", "X X", "X X"], {"X": S}, B("leggings")),
        "kalpa_boots_blank": shaped(["X X", "X X"], {"X": S}, B("boots")),
        "kalpa_bow_blank": shaped([" MS", "M S", " MS"], {"M": S, "S": "minecraft:string"}, B("bow")),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    write(DATA / "tags/item/kalpa_repair.json", {"values": [S]})
    t = MC / "tags"
    blocks = [i(b) for b in ("kalpa_ore", "kalpa_block")]
    merge_tag(t / "block/mineable/pickaxe.json", blocks)
    merge_tag(t / "block/needs_diamond_tool.json", blocks)
    merge_tag(t / "block/incorrect_for_diamond_tool.json", [i("kalpa_ore")])
    merge_tag(t / "block/beacon_base_blocks.json", [i("kalpa_block")])
    merge_tag(t / "item/beacon_payment_items.json", [S])
    for a in TOOLS:
        merge_tag(t / f"item/{a}s.json", [i(f"kalpa_{a}")])
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
        merge_tag(t / f"item/{slot}_armor.json", [i(f"kalpa_{piece}")])
    b = [i("kalpa_bow")]
    merge_tag(t / "item/enchantable/bow.json", b)
    merge_tag(t / "item/enchantable/durability.json", b)
    merge_tag(t / "item/enchantable/vanishing.json", b)
    merge_tag(MC.parent / "c/tags/block/ores.json", [i("kalpa_ore")])
    write(DATA / "worldgen/feature/ore_kalpa.json", {"type": "minecraft:ore", "size": 4, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:netherrack"}, "state": i("kalpa_ore")}]})
    write(DATA / "worldgen/placed_feature/ore_kalpa.json", placed(i("ore_kalpa"), 4, {"type": "minecraft:uniform", "min_inclusive": {"absolute": 6}, "max_inclusive": {"absolute": 30}}))
    write(DATA / "neoforge/biome_modifier/add_kalpa_ore.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_nether", "features": i("ore_kalpa"), "step": "underground_ores"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    textures()
    assets()
    data()


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v44")
