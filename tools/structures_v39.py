"""3.9.9 마을 게시관: 앞이 트인 7×6칸 건물 — 왼쪽 마을 발전 게시판 · 오른쪽 주간 목표 게시판.

hall_cells() 는 앞(+z) 기준 칸 목록. x 0..6, z 0(뒤)..5(앞) + 지붕 처마(x -1..7, z -1).
"""
from nbtlib import Compound, Int, List, String, load

from structures_v3 import OUT, Template

W, D = 7, 6
STAIR = {"half": "bottom", "shape": "straight", "waterlogged": "false"}
LOGY = {"axis": "y"}


def hall_cells(front="south"):
    c = {}
    for x in range(W):
        for z in range(D):
            c[(x, 0, z)] = ("minecraft:stone_bricks", None)
        for y in range(1, 5):
            c[(x, y, 0)] = ("minecraft:spruce_planks", None)
    for x in (0, 6):
        for y in range(3, 5):
            c[(x, y, 1)] = ("minecraft:spruce_planks", None)
        for y in range(1, 5):
            c[(x, y, 2)] = ("minecraft:spruce_planks", None)
    for (x, z) in ((0, 0), (6, 0), (0, 3), (6, 3), (0, 4), (6, 4)):
        for y in range(1, 5):
            c[(x, y, z)] = ("minecraft:spruce_log", LOGY)
    for z in range(-1, D):
        for i in range(0, 4):
            c[(i - 1, 4 + i, z)] = ("minecraft:deepslate_tile_stairs", {"facing": "east", **STAIR})
            c[(W - i, 4 + i, z)] = ("minecraft:deepslate_tile_stairs", {"facing": "west", **STAIR})
        c[(3, 8, z)] = ("minecraft:deepslate_tiles", None)
    for i in range(1, 4):
        for x in range(i, W - i):
            c[(x, 4 + i, 0)] = ("minecraft:spruce_planks", None)
    for y in (1, 2):
        c[(3, y, 1)] = ("minecraft:spruce_log", LOGY)
    c[(3, 3, 1)] = ("minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    for x in (1, 5):
        c[(x, 5, 4)] = ("minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    c[(1, 1, 1)] = ("wildplus:project_board", {"facing": front})
    c[(5, 1, 1)] = ("wildplus:goal_board", {"facing": front})
    return c


PART_CELLS = [(x, y, 1) for cx in (1, 5) for x in (cx - 1, cx, cx + 1) for y in (1, 2) if not (x == cx and y == 1)]


def notice_hall():
    """관리자 명령으로 짓는 구조물: 앞이 south, 틀 안 공기 포함 (나무 등을 치움)"""
    t = Template(9, 10, 9)
    for x in range(9):
        for y in range(1, 10):
            for z in range(9):
                t.set(x, y, z, "minecraft:air")
        for z in range(9):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for x in range(W):
        for z in (6, 7):
            t.set(x + 1, 0, z + 1, "minecraft:dirt_path")
    for (x, y, z), (b, p) in hall_cells().items():
        t.set(x + 1, y, z + 1, b, p)
    return t.save("village/notice_hall")


def add_to_town(put, x0=30, back_z=73):
    """큰 마을 남쪽 골목 빈터(x 30..36): 뒤가 남쪽, 앞이 북쪽 골목. put(x, y, z, block, props)"""
    for (x, y, z), (b, p) in hall_cells("north").items():
        put(x0 + x, y, back_z - z, b, p)
    for (x, y, z) in PART_CELLS:
        put(x0 + x, y, back_z - z, "wildplus:big_part", {"facing": "north", "part": "hidden"})
    for x in range(W):
        for zz in (65, 66, 67):
            if x in (2, 3, 4):
                put(x0 + x, 0, zz, "minecraft:dirt_path", None)


def patch_town():
    """이미 만든 town.nbt 에 게시관만 끼워 넣음 (구조물 재생성 없이)"""
    path = OUT / "town.nbt"
    n = load(path)
    keyf = lambda b, p: (b, tuple(sorted((p or {}).items())))
    index = {keyf(str(e["id"]), {k: str(v) for k, v in e.get("properties", {}).items()}): i for i, e in enumerate(n["palette"])}
    at = {tuple(int(v) for v in b["pos"]): k for k, b in enumerate(n["blocks"])}

    def put(x, y, z, b, p):
        key = keyf(b, p)
        if key not in index:
            e = Compound({"id": String(b)})
            if p:
                e["properties"] = Compound({k: String(v) for k, v in p.items()})
            n["palette"].append(e)
            index[key] = len(n["palette"]) - 1
        if (x, y, z) in at:
            n["blocks"][at[(x, y, z)]]["state"] = Int(index[key])
        else:
            n["blocks"].append(Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}))
            at[(x, y, z)] = len(n["blocks"]) - 1

    add_to_town(put)
    n.save(gzipped=True)
    return True


if __name__ == "__main__":
    print(notice_hall(), patch_town())
