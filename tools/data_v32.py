"""4.0 데이터 (data_v32): 공허석 재료 · 광석 · 공허 장비 (바닐라 형태 + 장식)."""
import colorsys
import json
import random

from PIL import Image

from data_v2 import ASSETS, DATA, MC, i, merge_tag, ore_loot, placed, self_loot, shaped, shapeless, write
from data_v18 import bow, simple

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
KO = {"sword": "검", "pickaxe": "곡괭이", "axe": "도끼", "shovel": "삽", "hoe": "괭이", "bow": "활",
      "helmet": "투구", "chestplate": "흉갑", "leggings": "레깅스", "boots": "부츠"}
EN = {"sword": "Sword", "pickaxe": "Pickaxe", "axe": "Axe", "shovel": "Shovel", "hoe": "Hoe", "bow": "Bow",
      "helmet": "Helmet", "chestplate": "Chestplate", "leggings": "Leggings", "boots": "Boots"}
K, D, M, V, G, W, T = (13, 9, 22), (34, 23, 58), (66, 42, 112), (118, 68, 200), (190, 125, 255), (238, 218, 255), (95, 235, 225)
TEX = ASSETS / "textures"
ALL = TOOLS + ["bow"] + ARMOR

TIP = {
    "sword": "공격력 10.5 · 공격 속도 1.9 · 내구도 무한\n공허 베기: 3타마다 주변 3칸 방어 무시 피해 12\n균열 표식: 맞은 적이 5초간 받는 피해 +15% (보스 +8%)",
    "bow": "당김 2배 · 피해 1.8배 · 내구도 무한\n공허 화살: 2번 맞힐 때 1번, 주변 3칸 방어 무시 피해 8 · 균열 표식 · 벽 1겹 관통 · 띄우기\n귀환: 맞히면 화살이 그대로 돌아옴 (특수 화살 포함)",
    "pickaxe": "내구도 무한 · 웅크리고 우클릭: 캔 것을 엔더 상자로 바로 보내기 켜고 끄기",
    "axe": "내구도 무한 · 체력 30% 이하 적에게 피해 2배\n처치하면 공허 참격: 주변 4칸에 그 적 최대 체력의 20% (최대 30)",
    "shovel": "내구도 무한 · 5×5 파기 (웅크리면 한 칸)",
    "hoe": "내구도 무한 · 5×5 수확 · 자동 파종 (웅크리면 한 칸)",
    "helmet": "2부위: 엔더맨이 먼저 공격 안 함 · 저항 I · 힘 I · 최대 체력 +2칸\n4부위: 저항 II · 힘 I · 신속 I · 최대 체력 +4칸 · 받는 피해 10% 감소 · 공허 도약 · 공허 생환",
}
TIP["chestplate"] = TIP["helmet"] + "\n겉날개 겸용"
TIP["leggings"] = TIP["boots"] = TIP["helmet"] + "\n공허 도약: 공중에서 웅크리면 바라보는 쪽 8칸 (8초)"
LANG_KO = {
    "block.wildplus.void_ore": "공허석 광석", "block.wildplus.void_block": "공허석 블록", "block.wildplus.void_brick": "공허 벽돌",
    "item.wildplus.void_shard": "공허 파편", "item.wildplus.raw_void_stone": "공허석 원석", "item.wildplus.void_stone": "공허석", "item.wildplus.void_heart": "공허 심장",
    "tooltip.wildplus.void_shard": "공허 유적과 각성 드래곤에게서 얻는 파편",
    "tooltip.wildplus.raw_void_stone": "엔드 외곽 섬에서 네더라이트 곡괭이로 캠\n원석 4 + 파편 4 → 공허석",
    "tooltip.wildplus.void_stone": "공허 장비 재료",
    "tooltip.wildplus.void_heart": "강화대에서 미완성 공허 장비를 완성하는 핵심 재료",
    "equipment.wildplus.void": "공허",
}
LANG_EN = {
    "block.wildplus.void_ore": "Void Ore", "block.wildplus.void_block": "Void Block", "block.wildplus.void_brick": "Void Brick",
    "item.wildplus.void_shard": "Void Shard", "item.wildplus.raw_void_stone": "Raw Voidstone", "item.wildplus.void_stone": "Voidstone", "item.wildplus.void_heart": "Void Heart",
    "equipment.wildplus.void": "Void",
}
for a in ALL:
    LANG_KO[f"item.wildplus.void_{a}"] = f"공허 {KO[a]}"
    LANG_KO[f"tooltip.wildplus.void_{a}"] = TIP[a]
    LANG_KO[f"item.wildplus.void_{a}_blank"] = f"미완성 공허 {KO[a]}"
    LANG_KO[f"tooltip.wildplus.void_{a}_blank"] = "강화대에서 공허 심장과 돈으로 완성"
    LANG_EN[f"item.wildplus.void_{a}"] = f"Void {EN[a]}"
    LANG_EN[f"item.wildplus.void_{a}_blank"] = f"Unfinished Void {EN[a]}"


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[n] + (b[n] - a[n]) * t) for n in range(3))


def lum(c):
    return (c[0] * .3 + c[1] * .59 + c[2] * .11) / 255


def ramp(l):
    st = [(0, K), (.3, D), (.55, M), (.8, V), (1, G)]
    for (a, ca), (b, cb) in zip(st, st[1:]):
        if l <= b:
            return mix(ca, cb, (l - a) / (b - a))
    return G


def recolor(im):
    im = im.convert("RGBA")
    o = im.copy()
    ps = [lum(im.getpixel((x, y))) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0]
    lo, hi = min(ps), max(ps)
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                o.putpixel((x, y), ramp((lum(p) - lo) / (hi - lo + 1e-6)) + (255,))
    return o


def over(im, pts, c):
    for x, y in pts:
        if im.getpixel((x, y))[3] > 0:
            im.putpixel((x, y), c + (255,))


def tool_tex(path):
    im = Image.open(path).convert("RGBA")
    o = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(*[c / 255 for c in p[:3]])
            wood = 0.03 < h < 0.17 and s < .75 and v < .72
            o.putpixel((x, y), (mix(D, M, lum(p) * 1.6) if wood else ramp(min(1, lum(p) * 1.55 + .12))) + (255,))
    return o


def sparkle(s, frame=0):
    im = s.copy()
    pts = sorted(((x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3] and lum(im.getpixel((x, y))) > .42), key=lambda q: q[0] - q[1])
    for k, p in enumerate(pts):
        c = W if (k + frame * 3) % 7 == 0 else T if (k + frame * 3) % 11 == 0 else None
        if c:
            im.putpixel(p, c + (255,))
    return im


def dim(s):
    o = s.copy()
    for y in range(16):
        for x in range(16):
            p = o.getpixel((x, y))
            if p[3]:
                o.putpixel((x, y), mix(p[:3], (70, 70, 78), .55) + (255,))
    return o


def animated(name, s):
    strip = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for f in range(4):
        strip.paste(sparkle(s, f), (0, 16 * f))
    strip.save(TEX / f"item/{name}.png")
    (TEX / f"item/{name}.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 14, "interpolate": True}}))


def diag(a, b):
    n = max(abs(a[0] - b[0]), abs(a[1] - b[1]), 1)
    return [(round(a[0] + (b[0] - a[0]) * t / n), round(a[1] + (b[1] - a[1]) * t / n)) for t in range(n + 1)]


def icon(layers):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for c, pts in layers:
        for p in pts:
            if 0 <= p[0] < 16 and 0 <= p[1] < 16:
                im.putpixel(p, c + (255,))
    return im


def block_tex(fn):
    im = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            im.putpixel((x, y), fn(x, y))
    return im


def ore(u, v):
    r = random.Random(f"{u}{v}")
    base = (214, 212, 160) if r.random() < .5 else (196, 192, 142)
    for cx, cy, big in ((4, 4, 1), (11, 6, 1), (6, 11, 0), (13, 12, 0)):
        dd = abs(u - cx) + abs(v - cy)
        if dd == 0:
            return W if big else G
        if dd == 1:
            return G if big else V
        if big and dd == 2 and r.random() < .7:
            return V
        if dd <= 2 and r.random() < .4:
            return D
    return base


def vblock(u, v):
    if u in (0, 15) or v in (0, 15):
        return M
    if u == v or u + v == 15:
        return G if 3 <= u <= 12 else V
    if u in (7, 8) and v in (7, 8):
        return W
    return D if random.Random(f"{u}{v}").random() < .6 else mix(D, K, .7)


def brick(u, v):
    r = random.Random(f"{u // 4}{v // 4}").random()
    return K if v % 4 == 0 or (u + (v // 4) % 2 * 2) % 8 == 0 else mix(D, M, r * .6)


def textures():
    src = TEX / "item"
    base = {}
    for a in ALL:
        base[a] = tool_tex(src / f"ruby_{a}.png")
        if a in TOOLS or a == "bow":
            animated(f"void_{a}", base[a])
        else:
            sparkle(base[a]).save(src / f"void_{a}.png")
        dim(base[a]).save(src / f"void_{a}_blank.png")
    for n in range(3):
        sparkle(tool_tex(src / f"ruby_bow_pulling_{n}.png")).save(src / f"void_bow_pulling_{n}.png")
    eq = TEX / "entity/equipment"
    h = recolor(Image.open(eq / "humanoid/ruby.png"))
    l = recolor(Image.open(eq / "humanoid_leggings/ruby.png"))
    over(h, [(11, 9), (12, 9)], G); over(h, [(11, 10), (12, 10)], T); over(h, [(11, 11), (12, 11), (9, 8), (10, 9), (14, 8), (13, 9)], V)
    over(h, [(23, 23), (24, 23), (23, 24), (24, 24)], W)
    over(h, [(22, 23), (25, 23), (22, 24), (25, 24), (23, 22), (24, 22), (23, 25), (24, 25)], T)
    over(h, [(21, 22), (26, 22), (21, 26), (26, 26), (22, 27), (25, 27), (20, 21), (27, 21), (23, 28), (24, 29)], G)
    over(h, [(35, 22), (36, 23), (35, 24), (36, 25), (35, 26), (36, 27), (35, 28)], G); over(h, [(35, 24), (36, 25)], T)
    over(h, [(x, 21) for x in range(40, 52)], V); over(h, [(45, 21), (46, 21)], G)
    over(h, [(4 + n, 28) for n in range(4)], V); over(h, [(5, 29), (6, 29)], G)
    over(l, [(5, 24), (6, 24)], T); over(l, [(4, 25), (7, 25), (5, 23), (6, 23)], G)
    h.save(eq / "humanoid/void.png"); l.save(eq / "humanoid_leggings/void.png")
    recolor(Image.open(eq / "wings/sky_sapphire.png")).save(eq / "wings/void.png")
    circle = lambda cx, cy, r2, a, b: [(x, y) for y in range(a, b) for x in range(a, b) if (x - cx) ** 2 + (y - cy) ** 2 <= r2]
    icon([(K, [(9, 1), (10, 2), (11, 4), (12, 7), (11, 10), (9, 12), (7, 14), (5, 12), (4, 9), (5, 6), (7, 3), (8, 2)]),
          (V, [(x, y) for y in range(3, 12) for x in range(6, 11) if abs(x - 8) <= 1 + abs(6 - abs(y - 7)) / 3]), (G, diag((8, 3), (8, 11))), (W, [(8, 5), (8, 6), (8, 7)])]).save(src / "void_shard.png")
    icon([(K, circle(8, 8, 37, 1, 16)), (D, circle(8, 8, 22, 2, 15)), (V, [(5, 5), (6, 5), (6, 6), (9, 4), (10, 4), (10, 5), (11, 8), (10, 8), (6, 10), (7, 10)]), (G, [(6, 5), (10, 4), (11, 8), (7, 10)]), (T, [(8, 7)])]).save(src / "raw_void_stone.png")
    icon([(K, circle(8, 8, 37, 1, 16)), (M, circle(8, 8, 25, 2, 15)), (V, circle(7, 7, 14, 2, 14)), (G, [(5, 4), (6, 4), (7, 4), (5, 5), (6, 5), (6, 6), (7, 6), (7, 7)]), (W, [(5, 4), (6, 5)]), (T, [(10, 8), (9, 10), (8, 11)])]).save(src / "void_stone.png")
    icon([(K, [(x, y) for y in range(2, 15) for x in range(1, 15) if (y < 8 and min((x - 4.5) ** 2 + (y - 5) ** 2, (x - 10.5) ** 2 + (y - 5) ** 2) <= 14) or (y >= 8 and abs(x - 7.5) <= 7 - (y - 8) * 1.1)]),
          (M, [(x, y) for y in range(3, 13) for x in range(2, 14) if (y < 8 and min((x - 4.5) ** 2 + (y - 5) ** 2, (x - 10.5) ** 2 + (y - 5) ** 2) <= 7) or (y >= 8 and abs(x - 7.5) <= 5.2 - (y - 8) * 1.1)]),
          (G, diag((7, 5), (7, 10)) + diag((8, 6), (8, 9))), (W, [(7, 7), (8, 7), (5, 4)]), (T, [(7, 8)])]).save(src / "void_heart.png")
    bt = TEX / "block"
    block_tex(ore).save(bt / "void_ore.png"); block_tex(vblock).save(bt / "void_block.png"); block_tex(brick).save(bt / "void_brick.png")


def assets():
    for n in ("void_shard", "raw_void_stone", "void_stone", "void_heart"):
        simple(n)
    for t in TOOLS:
        simple(f"void_{t}", "minecraft:item/handheld")
    for a in ARMOR:
        simple(f"void_{a}")
    bow("void_bow")
    for a in ALL:
        simple(f"void_{a}_blank", "minecraft:item/handheld" if a in TOOLS else "minecraft:item/generated")
    for b in ("void_ore", "void_block", "void_brick"):
        write(ASSETS / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i(f"block/{b}")}})
        write(ASSETS / f"blockstates/{b}.json", {"variants": {"": {"model": i(f"block/{b}")}}})
        write(ASSETS / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{b}")}})
    write(ASSETS / "equipment/void.json", {"layers": {"humanoid": [{"texture": i("void")}], "humanoid_leggings": [{"texture": i("void")}], "wings": [{"texture": i("void")}]}})


def data():
    write(DATA / "loot_table/blocks/void_ore.json", ore_loot("void_ore", i("raw_void_stone")))
    for b in ("void_block", "void_brick"):
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    S, ST, B = i("void_stone"), "minecraft:stick", lambda a: i(f"void_{a}_blank")
    rec = {
        "void_stone": shapeless([i("raw_void_stone")] * 4 + [i("void_shard")] * 4, S),
        "void_block": shaped(["###", "###", "###"], {"#": S}, i("void_block"), category="building"),
        "void_stone_from_block": shapeless([i("void_block")], S, 9),
        "void_brick": shaped(["BBB", "BSB", "BBB"], {"B": "minecraft:end_stone_bricks", "S": i("void_shard")}, i("void_brick"), 8, category="building"),
        "void_sword_blank": shaped(["X", "X", "#"], {"X": S, "#": ST}, B("sword")),
        "void_pickaxe_blank": shaped(["XXX", " # ", " # "], {"X": S, "#": ST}, B("pickaxe")),
        "void_axe_blank": shaped(["XX", "X#", " #"], {"X": S, "#": ST}, B("axe")),
        "void_shovel_blank": shaped(["X", "#", "#"], {"X": S, "#": ST}, B("shovel")),
        "void_hoe_blank": shaped(["XX", " #", " #"], {"X": S, "#": ST}, B("hoe")),
        "void_helmet_blank": shaped(["XXX", "X X"], {"X": S}, B("helmet")),
        "void_chestplate_blank": shaped(["X X", "XXX", "XXX"], {"X": S}, B("chestplate")),
        "void_leggings_blank": shaped(["XXX", "X X", "X X"], {"X": S}, B("leggings")),
        "void_boots_blank": shaped(["X X", "X X"], {"X": S}, B("boots")),
        "void_bow_blank": shaped([" MS", "M S", " MS"], {"M": S, "S": "minecraft:string"}, B("bow")),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    write(DATA / "tags/item/void_repair.json", {"values": [S]})
    t = MC / "tags"
    blocks = [i(b) for b in ("void_ore", "void_block", "void_brick")]
    merge_tag(t / "block/mineable/pickaxe.json", blocks)
    merge_tag(t / "block/needs_diamond_tool.json", [i("void_ore"), i("void_block")])
    merge_tag(t / "block/incorrect_for_diamond_tool.json", [i("void_ore")])
    merge_tag(t / "block/beacon_base_blocks.json", [i("void_block")])
    merge_tag(t / "item/beacon_payment_items.json", [S])
    for a in TOOLS:
        merge_tag(t / f"item/{a}s.json", [i(f"void_{a}")])
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
        merge_tag(t / f"item/{slot}_armor.json", [i(f"void_{piece}")])
    b = [i("void_bow")]
    merge_tag(t / "item/enchantable/bow.json", b)
    merge_tag(t / "item/enchantable/durability.json", b)
    merge_tag(t / "item/enchantable/vanishing.json", b)
    merge_tag(MC.parent / "c/tags/block/ores.json", [i("void_ore")])
    write(DATA / "worldgen/feature/ore_void.json", {"type": "minecraft:ore", "size": 4, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:end_stone"}, "state": i("void_ore")}]})
    write(DATA / "worldgen/placed_feature/ore_void.json", placed(i("ore_void"), 5, {"type": "minecraft:uniform", "min_inclusive": {"absolute": 10}, "max_inclusive": {"absolute": 75}}))
    write(DATA / "neoforge/biome_modifier/add_void_ore.json", {"type": "neoforge:add_features",
        "biomes": ["minecraft:end_highlands", "minecraft:end_midlands"], "features": i("ore_void"), "step": "underground_ores"})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    textures()
    assets()
    data()


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v32")
