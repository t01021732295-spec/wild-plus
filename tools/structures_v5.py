"""11차 구조물: 큰 마을(80×80, 상점가 + 주민 주거지·농장) · 도적 대형 아지트(56×56).

큰 마을: 가운데에 7차 상점 마을(48×48)을 그대로 넣고, 둘레에 주민 집 16채 · 밭 4곳 · 우물 · 철 골렘.
주민은 직업이 있는 일반 주민(침대·작업 블록이 집마다 있음)과 아이들.
도적 아지트: 돌담 + 망루 4개 + 천막 + 본관(보물 창고, 상자 다수) + 도적 14명 + 도적 두목.
"""
import random

from nbtlib import Byte, Compound, Float, Int, List, String

import structures_v4
from structures_v3 import FENCE, LANTERN, Template, chest, mob

N = 80
H = 12
ROAD = range(37, 43)  # 가운데 상점 마을의 큰길(21~26)이 16칸 밀린 자리

PROFESSIONS = [
    ("farmer", "minecraft:composter", {"level": "0"}),
    ("fisherman", "minecraft:barrel", {"facing": "up", "open": "false"}),
    ("librarian", "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"}),
    ("armorer", "minecraft:blast_furnace", {"facing": "north", "lit": "false"}),
    ("butcher", "minecraft:smoker", {"facing": "north", "lit": "false"}),
    ("cartographer", "minecraft:cartography_table", None),
    ("cleric", "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"}),
    ("fletcher", "minecraft:fletching_table", None),
    ("leatherworker", "minecraft:cauldron", None),
    ("mason", "minecraft:stonecutter", {"facing": "north"}),
    ("shepherd", "minecraft:loom", {"facing": "north"}),
    ("toolsmith", "minecraft:smithing_table", None),
    ("weaponsmith", "minecraft:grindstone", {"face": "floor", "facing": "north"}),
    ("farmer", "minecraft:composter", {"level": "0"}),
    ("librarian", "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"}),
    ("fisherman", "minecraft:barrel", {"facing": "up", "open": "false"}),
]
WALLS = ["minecraft:oak_planks", "minecraft:birch_planks", "minecraft:spruce_planks", "minecraft:white_terracotta"]
ROOFS = ["minecraft:dark_oak_planks", "minecraft:spruce_planks", "minecraft:red_terracotta", "minecraft:brown_terracotta"]
BEDS = ["minecraft:red_bed", "minecraft:blue_bed", "minecraft:green_bed", "minecraft:yellow_bed", "minecraft:white_bed",
        "minecraft:light_blue_bed", "minecraft:orange_bed", "minecraft:purple_bed"]
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def villager(prof, yaw, baby=False):
    extra = {"VillagerData": Compound({"profession": String(f"minecraft:{prof}"), "level": Int(2),
                                       "type": String("minecraft:plains")})}
    if baby:
        extra["Age"] = Int(-24000)
    return mob("minecraft:villager", yaw, extra)


def house(t, x0, z0, door, idx, rnd):
    """7×7 주민 집. door = 문이 난 쪽 (north/south/east/west). 안에 침대 · 작업 블록 · 상자 · 등."""
    x1, z1 = x0 + 6, z0 + 6
    wall, roof = WALLS[idx % len(WALLS)], ROOFS[(idx * 3) % len(ROOFS)]
    t.fill(x0, 0, z0, x1, 0, z1, "minecraft:cobblestone")
    t.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:oak_planks")
    for y in range(1, 4):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) and z in (z0, z1):
                    t.set(x, y, z, "minecraft:oak_log", {"axis": "y"})
                elif x in (x0, x1) or z in (z0, z1):
                    t.set(x, y, z, wall)
    # 창문
    for (x, z) in [(x0, z0 + 3), (x1, z0 + 3), (x0 + 3, z0), (x0 + 3, z1)]:
        t.set(x, 2, z, "minecraft:glass_pane", {"east": "false", "west": "false", "north": "false", "south": "false",
                                                "waterlogged": "false"})
    # 문
    dx, dz = {"north": (x0 + 3, z0), "south": (x0 + 3, z1), "west": (x0, z0 + 3), "east": (x1, z0 + 3)}[door]
    for y, half in ((1, "lower"), (2, "upper")):
        t.set(dx, y, dz, "minecraft:oak_door", {"facing": OPP[door], "half": half, "hinge": "left", "open": "false",
                                                "powered": "false"})
    # 지붕 (두 단)
    t.fill(x0 - 1, 4, z0 - 1, x1 + 1, 4, z1 + 1, roof)
    t.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, roof)
    t.set(x0 + 3, 6, z0 + 3, roof)
    # 침대: 문 반대쪽 벽에 붙여서
    back = OPP[door]
    bed = BEDS[idx % len(BEDS)]
    if back in ("north", "south"):
        bz_head = z0 + 1 if back == "north" else z1 - 1
        bz_foot = bz_head + (1 if back == "north" else -1)
        t.set(x0 + 1, 1, bz_head, bed, {"facing": back, "part": "head", "occupied": "false"})
        t.set(x0 + 1, 1, bz_foot, bed, {"facing": back, "part": "foot", "occupied": "false"})
        jx, jz = x1 - 1, bz_head
        cx, cz = x0 + 3, bz_head
    else:
        bx_head = x0 + 1 if back == "west" else x1 - 1
        bx_foot = bx_head + (1 if back == "west" else -1)
        t.set(bx_head, 1, z0 + 1, bed, {"facing": back, "part": "head", "occupied": "false"})
        t.set(bx_foot, 1, z0 + 1, bed, {"facing": back, "part": "foot", "occupied": "false"})
        jx, jz = bx_head, z1 - 1
        cx, cz = bx_head, z0 + 3
    prof, block, props = PROFESSIONS[idx % len(PROFESSIONS)]
    t.set(jx, 1, jz, block, props)
    t.set(cx, 1, cz, *chest("minecraft:chests/village/village_plains_house", OPP[back]))
    t.set(x0 + 3, 3, z0 + 3, "minecraft:lantern", LANTERN)
    t.set(x0 + 5 if back != "east" else x0 + 1, 1, z0 + 5 if back != "south" else z0 + 1, "minecraft:potted_poppy")
    # 주민 (집 안) + 가끔 아이
    t.entity(x0 + 3.5, 1.0, z0 + 3.5, villager(prof, rnd.choice([0.0, 90.0, 180.0, 270.0])))
    if idx % 3 == 0:
        t.entity(x0 + 2.5, 1.0, z0 + 4.5, villager("none", 45.0, baby=True))
    # 문 앞 길
    ox, oz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[door]
    for k in range(1, 3):
        t.set(dx + ox * k, 0, dz + oz * k, "minecraft:dirt_path")


def farm(t, x0, z0, w, d, crop, rnd):
    """밭: 경작지 + 가운데 물길 + 작물 (다 자란 것 섞임) + 울타리"""
    for x in range(x0 - 1, x0 + w + 1):
        for z in range(z0 - 1, z0 + d + 1):
            if x in (x0 - 1, x0 + w) or z in (z0 - 1, z0 + d):
                t.set(x, 0, z, "minecraft:oak_log", {"axis": "x"})
    mid = z0 + d // 2
    for x in range(x0, x0 + w):
        for z in range(z0, z0 + d):
            if z == mid:
                t.set(x, 0, z, "minecraft:water", {"level": "0"})
            else:
                t.set(x, 0, z, "minecraft:farmland", {"moisture": "7"})
                t.set(x, 1, z, crop, {"age": str(rnd.choice([3, 5, 7, 7]))})


def big_town():
    rnd = random.Random(11)
    t = Template(N, H, N)
    t.fill(0, 0, 0, N - 1, 0, N - 1, "minecraft:grass_block", {"snowy": "false"})
    structures_v4.town(t, 16, 16, fence=False)
    # 큰길을 마을 끝까지 + 주거지 골목
    for i in range(N):
        for k in ROAD:
            if 16 <= i < 64:
                continue
            mat = "minecraft:stone_bricks" if k in (38, 39, 40, 41) else "minecraft:polished_andesite"
            t.set(i, 0, k, mat)
            t.set(k, 0, i, mat)
    for i in range(2, N - 2):
        for (x, z) in ((i, 15), (i, 64), (15, i), (64, i)):
            t.set(x, 0, z, "minecraft:dirt_path")
    # 바깥 울타리 (입구 4곳)
    for i in range(N):
        for (x, z) in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
            if i not in ROAD:
                t.set(x, 1, z, "minecraft:oak_fence", FENCE)
    # 주민 집 16채 (문은 골목 쪽)
    idx = 0
    for x0 in (3, 12, 22, 45, 55, 66):
        house(t, x0, 6, "south", idx, rnd)
        idx += 1
        house(t, x0, 67, "north", idx, rnd)
        idx += 1
    for z0 in (22, 49):
        house(t, 6, z0, "east", idx, rnd)
        idx += 1
        house(t, 67, z0, "west", idx, rnd)
        idx += 1
    # 밭 4곳 (서쪽·동쪽 띠)
    farm(t, 3, 31, 9, 5, "minecraft:wheat", rnd)
    farm(t, 68, 31, 9, 5, "minecraft:carrots", rnd)
    farm(t, 3, 44, 9, 3, "minecraft:potatoes", rnd)
    farm(t, 68, 44, 9, 3, "minecraft:beetroots", rnd)
    for (x, z, prof) in [(7.5, 30.0, "farmer"), (72.5, 30.0, "farmer")]:
        t.entity(x, 1.0, z, villager(prof, 0.0))
    # 우물 (북서·남동 골목)
    for (wx, wz) in ((31, 2), (46, 74)):
        t.fill(wx, 0, wz, wx + 3, 0, wz + 3, "minecraft:cobblestone")
        t.fill(wx, 1, wz, wx + 3, 1, wz + 3, "minecraft:cobblestone_wall", {"east": "none", "west": "none", "north": "none",
                                                                            "south": "none", "up": "true", "waterlogged": "false"})
        t.fill(wx + 1, 1, wz + 1, wx + 2, 1, wz + 2, "minecraft:water", {"level": "0"})
        t.fill(wx + 1, 0, wz + 1, wx + 2, 0, wz + 2, "minecraft:water", {"level": "0"})
        for (px, pz) in ((wx, wz), (wx + 3, wz), (wx, wz + 3), (wx + 3, wz + 3)):
            t.set(px, 2, pz, "minecraft:oak_fence", FENCE)
        t.fill(wx, 3, wz, wx + 3, 3, wz + 3, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    # 가로등 (주거지 골목)
    for (x, z) in [(8, 15), (30, 15), (50, 15), (72, 15), (8, 64), (30, 64), (50, 64), (72, 64), (15, 30), (64, 30), (15, 50), (64, 50)]:
        t.fill(x, 1, z, x, 3, z, "minecraft:spruce_fence", FENCE)
        t.set(x, 4, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # 나무
    for (x, z) in [(2, 2), (77, 2), (2, 77), (77, 77), (33, 9)]:
        t.fill(x, 1, z, x, 4, z, "minecraft:oak_log", {"axis": "y"})
        t.fill(x - 1, 4, z - 1, x + 1, 5, z + 1, "minecraft:oak_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
        t.set(x, 6, z, "minecraft:oak_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
        t.set(x, 4, z, "minecraft:oak_log", {"axis": "y"})
    # 길 위를 걷는 주민 · 철 골렘
    for (x, z, prof) in [(40.5, 10.5, "nitwit"), (10.5, 40.5, "mason"), (70.5, 39.5, "shepherd"), (39.5, 70.5, "cleric")]:
        t.entity(x, 1.0, z, villager(prof, 90.0))
    for (x, z) in [(40.5, 4.5), (40.5, 75.5)]:
        t.entity(x, 1.0, z, mob("minecraft:iron_golem", 0.0, {"PlayerCreated": Byte(0)}))
    return t.save("town")


# ───────────────────────── 도적 아지트 ─────────────────────────
HW = 56
HH = 12


def bandit_boss():
    return mob("wildplus:bandit", 180.0, {"NeoForgeData": Compound({"wildplus_bandit_boss": Byte(1)})})


def tent(t, x0, z0, wool):
    """5×5 A자 천막"""
    for z in range(z0, z0 + 5):
        t.set(x0, 1, z, wool)
        t.set(x0 + 4, 1, z, wool)
        t.set(x0 + 1, 2, z, wool)
        t.set(x0 + 3, 2, z, wool)
        t.set(x0 + 2, 3, z, wool)
    t.set(x0 + 2, 3, z0, "minecraft:oak_fence", FENCE)
    t.set(x0 + 1, 1, z0 + 3, "minecraft:white_carpet")
    t.set(x0 + 2, 1, z0 + 3, "minecraft:white_carpet")


def hideout():
    rnd = random.Random(21)
    t = Template(HW, HH, HW)
    # 땅: 거친 흙 · 자갈 · 흙길이 섞인 맨땅
    for x in range(HW):
        for z in range(HW):
            r = rnd.random()
            t.set(x, 0, z, "minecraft:coarse_dirt" if r < 0.45 else "minecraft:gravel" if r < 0.6 else
                  "minecraft:dirt_path" if r < 0.75 else "minecraft:dirt")
    # 돌담 (높이 5) + 통나무 기둥 + 위 울타리 가시
    for i in range(HW):
        for (x, z) in ((i, 0), (i, HW - 1), (0, i), (HW - 1, i)):
            gate = z == HW - 1 and 25 <= x <= 30
            if gate:
                continue
            post = i % 6 == 0
            for y in range(1, 6):
                t.set(x, y, z, "minecraft:spruce_log" if post else ("minecraft:mossy_cobblestone" if rnd.random() < 0.3 else
                                                                    "minecraft:cobblestone"), {"axis": "y"} if post else None)
            t.set(x, 6, z, "minecraft:oak_fence", FENCE)
    # 정문 (위 가로대 + 횃불)
    t.fill(24, 5, HW - 1, 31, 5, HW - 1, "minecraft:spruce_log", {"axis": "x"})
    for x in (24, 31):
        t.set(x, 3, HW - 2, "minecraft:wall_torch", {"facing": "north"})
    # 망루 4개 (5×5, 높이 9)
    for (tx, tz) in ((1, 1), (HW - 6, 1), (1, HW - 6), (HW - 6, HW - 6)):
        for y in range(1, 10):
            for (px, pz) in ((tx, tz), (tx + 4, tz), (tx, tz + 4), (tx + 4, tz + 4)):
                t.set(px, y, pz, "minecraft:spruce_log", {"axis": "y"})
        t.fill(tx, 7, tz, tx + 4, 7, tz + 4, "minecraft:spruce_planks")
        for i in range(5):
            for (x, z) in ((tx + i, tz), (tx + i, tz + 4), (tx, tz + i), (tx + 4, tz + i)):
                t.set(x, 8, z, "minecraft:spruce_fence", FENCE)
        t.fill(tx, 10, tz, tx + 4, 10, tz + 4, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
        t.fill(tx + 2, 1, tz + 2, tx + 2, 6, tz + 2, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
        t.set(tx + 2, 7, tz + 2, "minecraft:spruce_trapdoor", {"facing": "north", "half": "top", "open": "false",
                                                              "powered": "false", "waterlogged": "false"})
        t.set(tx + 2, 8, tz + 2, "minecraft:lantern", LANTERN)
        t.entity(tx + 1.5, 8.0, tz + 1.5, mob("wildplus:bandit", 180.0))
    # 천막 6개 + 모닥불 + 상자
    wools = ["minecraft:red_wool", "minecraft:brown_wool", "minecraft:black_wool", "minecraft:gray_wool"]
    for k, (x0, z0) in enumerate([(8, 30), (16, 38), (8, 44), (36, 30), (44, 38), (36, 44)]):
        tent(t, x0, z0, wools[k % len(wools)])
        t.set(x0 + 2, 1, z0 + 1, *chest("wildplus:chests/bandit_camp", "south"))
        t.entity(x0 + 2.5, 1.0, z0 + 6.5, mob("wildplus:bandit", rnd.random() * 360))
    for (x, z) in ((14, 35), (28, 42), (42, 35)):
        t.set(x, 1, z, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
        for (ox, oz) in ((-2, 0), (2, 0), (0, -2), (0, 2)):
            t.set(x + ox, 1, z + oz, "minecraft:spruce_log", {"axis": "x" if ox else "z"})
    # 물자 (통·건초·화약)
    for (x, z, b, p) in [(6, 8, "minecraft:barrel", {"facing": "up", "open": "false"}), (7, 8, "minecraft:hay_block", {"axis": "y"}),
                         (6, 9, "minecraft:barrel", {"facing": "up", "open": "false"}), (48, 8, "minecraft:tnt", {"unstable": "false"}),
                         (49, 8, "minecraft:barrel", {"facing": "up", "open": "false"}), (49, 9, "minecraft:hay_block", {"axis": "y"}),
                         (26, 50, "minecraft:hay_block", {"axis": "y"}), (29, 50, "minecraft:barrel", {"facing": "up", "open": "false"})]:
        t.set(x, 1, z, b, p)
    # 본관 (17×13, 짙은 참나무) — 가운데 보물 창고
    bx0, bz0, bx1, bz1 = 19, 8, 36, 21
    t.fill(bx0, 0, bz0, bx1, 0, bz1, "minecraft:dark_oak_planks")
    for y in range(1, 6):
        for x in range(bx0, bx1 + 1):
            for z in range(bz0, bz1 + 1):
                if x in (bx0, bx1) and z in (bz0, bz1):
                    t.set(x, y, z, "minecraft:dark_oak_log", {"axis": "y"})
                elif x in (bx0, bx1) or z in (bz0, bz1):
                    t.set(x, y, z, "minecraft:stone_bricks" if y <= 2 else "minecraft:dark_oak_planks")
    t.fill(bx0 - 1, 6, bz0 - 1, bx1 + 1, 6, bz1 + 1, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    t.fill(bx0 + 1, 7, bz0 + 1, bx1 - 1, 7, bz1 - 1, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    for x in (27, 28):  # 문 (남쪽)
        t.set(x, 1, bz1, "minecraft:air")
        t.set(x, 2, bz1, "minecraft:air")
    for x in range(bx0 + 2, bx1 - 1, 4):
        t.set(x, 3, bz0, "minecraft:iron_bars", {"east": "false", "west": "false", "north": "false", "south": "false",
                                                 "waterlogged": "false"})
    for (x, z) in ((22, 12), (33, 12), (22, 18), (33, 18), (27, 15)):
        t.set(x, 5, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # 보물 창고 (쇠창살로 둘러싼 북쪽 방)
    for x in range(bx0 + 1, bx1):
        t.set(x, 1, 13, "minecraft:iron_bars", {"east": "true", "west": "true", "north": "false", "south": "false",
                                                "waterlogged": "false"})
        t.set(x, 2, 13, "minecraft:iron_bars", {"east": "true", "west": "true", "north": "false", "south": "false",
                                                "waterlogged": "false"})
    t.set(27, 1, 13, "minecraft:iron_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    t.set(27, 2, 13, "minecraft:iron_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    t.set(28, 1, 14, "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    for x in (21, 23, 25, 30, 32, 34):
        t.set(x, 1, 9, *chest("wildplus:chests/bandit_hideout", "south"))
    for (x, z) in ((21, 11), (22, 11), (34, 11), (33, 11)):
        t.set(x, 1, z, "minecraft:gold_block")
    t.set(22, 2, 11, "minecraft:gold_block")
    t.set(33, 2, 11, "minecraft:raw_gold_block")
    t.set(27, 1, 9, "minecraft:emerald_block")
    t.set(28, 1, 9, "minecraft:diamond_block")
    # 본관 안: 긴 탁자 + 두목
    t.fill(23, 1, 17, 32, 1, 17, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"})
    t.entity(27.5, 1.0, 15.5, bandit_boss())
    t.entity(24.5, 1.0, 19.5, mob("wildplus:bandit", 90.0))
    t.entity(31.5, 1.0, 19.5, mob("wildplus:bandit", 270.0))
    # 마당 도적
    for (x, z) in ((27.5, 26.5), (12.5, 24.5), (44.5, 24.5), (27.5, 46.5), (20.5, 52.5)):
        t.entity(x, 1.0, z, mob("wildplus:bandit", rnd.random() * 360))
    return t.save("bandit_hideout")


if __name__ == "__main__":
    print("big town", big_town())
    print("hideout", hideout())
