"""3.3 집 구조물 (.nbt): 설치하면 지어지는 6가지 집. 앞(현관) = 남쪽(+z), y=0 = 바닥(땅 높이를 대신함).
가구는 3.3 가구 블록을 그대로 놓는다 (큰 가구의 나머지 칸은 놓일 때 자동으로 채워짐)."""
from nbtlib import Compound, Int, List, String

from structures_v3 import Template

LANTERN_H = {"hanging": "true", "waterlogged": "false"}
LANTERN = {"hanging": "false", "waterlogged": "false"}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def walls(t, x0, z0, x1, z1, y0, y1, block, corner=None, axis_logs=False):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x not in (x0, x1) and z not in (z0, z1):
                    continue
                if corner and x in (x0, x1) and z in (z0, z1):
                    t.set(x, y, z, corner, {"axis": "y"})
                elif axis_logs:
                    t.set(x, y, z, block, {"axis": "x" if z in (z0, z1) else "z"})
                else:
                    t.set(x, y, z, block)


def pane(t, x, y, z, along="x", block="minecraft:glass_pane"):
    if along == "x":
        p = {"east": "true", "west": "true", "north": "false", "south": "false", "waterlogged": "false"}
    else:
        p = {"east": "false", "west": "false", "north": "true", "south": "true", "waterlogged": "false"}
    t.set(x, y, z, block, p)


def door(t, x, y, z, facing="south", block="minecraft:spruce_door", hinge="left"):
    for dy, half in ((0, "lower"), (1, "upper")):
        t.set(x, y + dy, z, block, {"facing": facing, "half": half, "hinge": hinge, "open": "false", "powered": "false"})


def stair(block, facing, half="bottom"):
    return block, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}


def gable(t, x0, x1, z0, z1, y0, stairs, ridge, gable_wall, over=1):
    """용마루가 z 방향인 맞배 지붕 (x0~x1 이 벽 바깥면). 박공벽도 채움."""
    l, r, y = x0 - over, x1 + over, y0
    while l < r:
        for z in range(z0 - over, z1 + over + 1):
            b, p = stair(stairs, "east")
            t.set(l, y, z, b, p)
            b, p = stair(stairs, "west")
            t.set(r, y, z, b, p)
        # 박공벽
        for x in range(max(l + 1, x0), min(r - 1, x1) + 1):
            for z in (z0, z1):
                if y > y0:
                    t.set(x, y, z, gable_wall)
        l += 1
        r -= 1
        y += 1
    if l == r:
        for z in range(z0 - over, z1 + over + 1):
            t.set(l, y, z, ridge, {"type": "bottom", "waterlogged": "false"} if ridge.endswith("_slab") else None)
    return y


STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def furn(t, x, y, z, name, facing="south", nbt=None):
    if name in ("canopy_bed", "bunk_bed"):
        # 침대: 발치 (x,z) + 머리는 바라보는 쪽 한 칸
        dx, dz = STEP[facing]
        t.set(x, y, z, "wildplus:" + name, {"facing": facing, "part": "foot", "occupied": "false"})
        t.set(x + dx, y, z + dz, "wildplus:" + name, {"facing": facing, "part": "head", "occupied": "false"})
        return
    t.set(x, y, z, "wildplus:" + name, {"facing": facing, "lit": "true"}, nbt)


def wall(t, x0, z0, x1, z1, y0, y1, block, props=None):
    """칸막이 벽 (한 줄)"""
    t.fill(x0, y0, z0, x1, y1, z1, block, props)


def opening(t, x, y, z, h=2):
    """문 없는 통로 (아치)"""
    for dy in range(h):
        t.set(x, y + dy, z, "minecraft:air")


def window_z(t, x, y, z, block="minecraft:glass_pane"):
    pane(t, x, y, z, "z", block)


BOOK = "minecraft:bookshelf"
CHEST = lambda f: ("minecraft:chest", {"facing": f, "type": "single", "waterlogged": "false"})
CARPET = lambda c: "minecraft:" + c + "_carpet"


# ═══════════ 1. 통나무 오두막 (9×11): 거실·부엌 + 다락 침실 ═══════════
def cabin():
    t = Template(9, 10, 11)
    t.fill(0, 0, 0, 8, 0, 10, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 7, 0, 7, "minecraft:cobblestone")
    t.fill(2, 0, 2, 6, 0, 6, "minecraft:spruce_planks")
    walls(t, 1, 1, 7, 7, 1, 4, "minecraft:spruce_log", "minecraft:stripped_spruce_log", axis_logs=True)
    door(t, 4, 1, 7)
    for x in (2, 6):
        pane(t, x, 2, 7)
    for z in (3, 5):
        pane(t, 1, 2, z, "z")
        pane(t, 7, 2, z, "z")
    t.fill(2, 5, 2, 6, 5, 6, "minecraft:spruce_planks")  # 다락 바닥
    t.fill(1, 5, 1, 1, 5, 7, "minecraft:stripped_spruce_log", {"axis": "z"})  # 벽 위 · 지붕 사이 틈 막기
    t.fill(7, 5, 1, 7, 5, 7, "minecraft:stripped_spruce_log", {"axis": "z"})
    gable(t, 1, 7, 1, 7, 5, "minecraft:mangrove_stairs", "minecraft:mangrove_slab", "minecraft:spruce_planks")
    pane(t, 4, 6, 7)  # 다락 박공창
    pane(t, 4, 6, 1)
    t.fill(2, 1, 0, 2, 9, 0, "minecraft:cobblestone")  # 굴뚝 (뒤 벽 바깥)
    t.set(2, 9, 0, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # 현관 데크 + 난간 + 등
    t.fill(1, 0, 8, 7, 0, 9, "minecraft:spruce_planks")
    for x in (1, 2, 6, 7):
        t.set(x, 1, 9, "minecraft:spruce_fence")
    t.set(5, 3, 8, "minecraft:lantern", LANTERN_H)
    t.set(5, 4, 8, "minecraft:spruce_fence")
    t.set(1, 1, 8, "minecraft:potted_poppy")
    # 1층 부엌 (뒤 벽): 작업대 · 화로 · 상자 / 거실: 안락의자 · 스탠드 · 러그
    t.set(4, 1, 2, "minecraft:furnace", {"facing": "south", "lit": "false"})
    t.set(5, 1, 2, "minecraft:crafting_table")
    t.set(6, 1, 2, *CHEST("south"))
    t.set(3, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    furn(t, 6, 1, 5, "armchair", "west")
    furn(t, 6, 1, 6, "floor_lamp", "west")
    furn(t, 4, 1, 4, "coffee_table", "south")
    t.set(4, 4, 4, "minecraft:lantern", LANTERN_H)
    # 다락 침실 (사다리로): 이층 침대 · 협탁 · 상자
    t.fill(3, 1, 6, 3, 5, 6, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    furn(t, 4, 6, 3, "bunk_bed", "north")
    furn(t, 5, 6, 2, "nightstand", "south")
    t.set(3, 6, 2, *CHEST("south"))
    t.set(4, 6, 5, CARPET("brown"))
    return t.save("house/cabin")


# ═══════════ 2. 숲속 통나무집 (11×11): 거실 + 침실 + 부엌 ═══════════
def log_house():
    t = Template(11, 11, 11)
    t.fill(0, 0, 0, 10, 0, 10, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 9, 0, 7, "minecraft:stone_bricks")
    t.fill(2, 0, 2, 8, 0, 6, "minecraft:oak_planks")
    walls(t, 1, 1, 9, 7, 1, 4, "minecraft:oak_log", "minecraft:dark_oak_log", axis_logs=True)
    door(t, 5, 1, 7, block="minecraft:dark_oak_door")
    for x in (2, 3, 7, 8):
        pane(t, x, 2, 7)
    for z in (3, 5):
        pane(t, 9, 2, z, "z")
        pane(t, 1, 2, z, "z")
    for x in (2, 3, 7, 8):  # 창 아래 꽃상자
        t.set(x, 1, 8, "minecraft:flowering_azalea_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
    t.fill(2, 5, 2, 8, 5, 6, "minecraft:oak_planks")
    t.fill(1, 5, 1, 1, 5, 7, "minecraft:stripped_oak_log", {"axis": "z"})
    t.fill(9, 5, 1, 9, 5, 7, "minecraft:stripped_oak_log", {"axis": "z"})
    gable(t, 1, 9, 1, 7, 5, "minecraft:dark_oak_stairs", "minecraft:dark_oak_slab", "minecraft:oak_planks")
    t.fill(3, 1, 0, 3, 10, 0, "minecraft:bricks")  # 굴뚝 (벽난로 뒤)
    # 베란다 (지붕 · 기둥: 현관 앞은 비움)
    t.fill(1, 0, 8, 9, 0, 10, "minecraft:oak_planks")
    for x in (1, 3, 7, 9):
        t.fill(x, 1, 10, x, 3, 10, "minecraft:dark_oak_fence")
    for x in range(0, 11):
        for z in (8, 9, 10):
            t.set(x, 4, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(3, 3, 9, "minecraft:lantern", LANTERN_H)
    t.set(7, 3, 9, "minecraft:lantern", LANTERN_H)
    furn(t, 8, 1, 9, "armchair", "south")
    t.fill(0, 1, 2, 0, 2, 4, "minecraft:oak_log", {"axis": "x"})  # 장작더미
    # 칸막이: 동쪽 침실 (x 7~8) · 문
    wall(t, 6, 2, 6, 6, 1, 4, "minecraft:stripped_oak_wood", {"axis": "y"})
    door(t, 6, 1, 5, block="minecraft:oak_door", facing="east")
    # 거실 (벽난로 · 소파 · 러그) + 부엌 구석 (냉장고 · 싱크대)
    furn(t, 3, 1, 2, "fireplace", "south")
    furn(t, 3, 1, 5, "sofa_brown", "north")
    furn(t, 5, 1, 2, "fridge", "south")
    furn(t, 5, 1, 3, "sink", "west")
    t.set(4, 4, 4, "minecraft:lantern", LANTERN_H)
    # 침실: 이층 침대 · 협탁 · 옷 상자
    furn(t, 8, 1, 3, "bunk_bed", "north")
    furn(t, 7, 1, 2, "nightstand", "south")
    t.set(8, 1, 6, *CHEST("west"))
    t.set(7, 1, 6, CARPET("green"))
    t.set(8, 4, 4, "minecraft:lantern", LANTERN_H)
    return t.save("house/log_house")


# ═══════════ 3. 벽돌 2층집 (11×12): 1층 거실·주방 / 2층 침실 + 서재 + 욕실 ═══════════
def brick_house():
    t = Template(11, 15, 12)
    t.fill(0, 0, 0, 10, 0, 11, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 9, 0, 9, "minecraft:polished_andesite")
    t.fill(2, 0, 2, 8, 0, 8, "minecraft:birch_planks")
    walls(t, 1, 1, 9, 9, 1, 8, "minecraft:bricks", "minecraft:stone_bricks")
    for x in range(1, 10):
        for z in (1, 9):
            t.set(x, 4, z, "minecraft:smooth_quartz")
    for z in range(1, 10):
        for x in (1, 9):
            t.set(x, 4, z, "minecraft:smooth_quartz")
    t.fill(2, 4, 2, 8, 4, 8, "minecraft:birch_planks")  # 2층 바닥
    door(t, 5, 1, 9, block="minecraft:birch_door")
    t.set(5, 3, 10, "minecraft:spruce_trapdoor", {"facing": "north", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
    for x in (2, 3, 7, 8):
        pane(t, x, 2, 9)
        pane(t, x, 2, 1)
    for x in (2, 3, 7, 8):
        for y in (6, 7):
            pane(t, x, y, 9)
    for z in (3, 6):
        for y in (2, 6):
            pane(t, 1, y, z, "z")
            pane(t, 9, y, z, "z")
    # 발코니 (침실 문 앞 · 2칸 깊이)
    door(t, 4, 5, 9, block="minecraft:birch_door")
    t.fill(3, 4, 10, 6, 4, 11, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
    for x in range(3, 7):
        t.set(x, 5, 11, "minecraft:birch_fence")
    for x in (3, 6):
        t.set(x, 5, 10, "minecraft:birch_fence")
    t.fill(2, 9, 2, 8, 9, 8, "minecraft:birch_planks")
    gable(t, 1, 9, 1, 9, 9, "minecraft:red_nether_brick_stairs", "minecraft:red_nether_brick_slab", "minecraft:bricks")
    t.fill(8, 9, 0, 8, 14, 0, "minecraft:bricks")
    # 계단 (동쪽 벽 · 1층 → 2층, 4단)
    for k in range(4):
        b, p = stair("minecraft:birch_stairs", "north")
        t.set(8, 1 + k, 7 - k, b, p)
    t.set(8, 4, 5, "minecraft:air")
    t.set(8, 4, 6, "minecraft:air")
    # 1층 칸막이: 뒤쪽 주방 (z 2~3) · 아치 통로
    wall(t, 2, 4, 7, 4, 1, 3, "minecraft:stripped_birch_wood", {"axis": "y"})
    opening(t, 4, 1, 4)
    opening(t, 5, 1, 4)
    furn(t, 2, 1, 2, "fridge", "south")
    furn(t, 3, 1, 2, "sink", "south")
    t.set(5, 1, 2, "wildplus:kitchen_stove", {"facing": "south", "lit": "false"})
    t.set(6, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    t.set(7, 1, 2, "minecraft:smoker", {"facing": "south", "lit": "false"})
    # 1층 거실: TV · 소파 · 커피 테이블 · 화분
    furn(t, 3, 1, 5, "tv_stand", "south")
    furn(t, 3, 1, 8, "sofa_navy", "north")
    furn(t, 3, 1, 7, "coffee_table", "south")
    furn(t, 6, 1, 8, "monstera", "south")
    t.set(5, 3, 6, "minecraft:lantern", LANTERN_H)
    # 2층 칸막이: 서쪽 침실 (x 2~4) | 동쪽 앞 욕실 (z 7~8) | 동쪽 뒤 서재 + 계단참
    wall(t, 5, 2, 5, 8, 5, 8, "minecraft:white_concrete")
    door(t, 5, 5, 3, block="minecraft:birch_door", facing="east")
    wall(t, 6, 6, 8, 6, 5, 8, "minecraft:white_concrete")
    door(t, 6, 5, 6, block="minecraft:birch_door", facing="north")
    # 침실: 캐노피 침대 · 협탁 2 · 옷장
    furn(t, 3, 5, 3, "canopy_bed", "north")
    furn(t, 2, 5, 2, "nightstand", "south")
    furn(t, 4, 5, 2, "nightstand", "south")
    furn(t, 2, 5, 6, "wardrobe", "east")
    t.fill(3, 5, 5, 4, 5, 6, CARPET("pink"))
    # 서재: 책상 · 스탠드
    furn(t, 7, 5, 2, "desk", "south")
    furn(t, 8, 5, 2, "floor_lamp", "south")
    # 욕실: 욕조 · 싱크대
    furn(t, 6, 5, 8, "bathtub", "north")
    furn(t, 8, 5, 8, "sink", "north")
    for (x, y, z) in ((3, 8, 4), (7, 8, 4), (7, 8, 7)):
        t.set(x, y, z, "minecraft:lantern", LANTERN_H)
    # 앞마당: 우체통 · 화단
    t.set(9, 1, 10, "minecraft:cobblestone_wall", {"east": "none", "west": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false"})
    t.set(9, 2, 10, "minecraft:red_terracotta")
    for x in (1, 2):
        t.set(x, 1, 10, "minecraft:rose_bush", {"half": "lower"})
        t.set(x, 2, 10, "minecraft:rose_bush", {"half": "upper"})
    return t.save("house/brick_house")


# ═══════════ 4. 한옥 (13×12): 대청 + 안방 + 사랑방 ═══════════
def hanok():
    t = Template(13, 10, 12)
    t.fill(0, 0, 0, 12, 0, 11, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 11, 1, 9, "minecraft:stone_bricks")  # 기단 (한 칸 높음)
    t.fill(2, 1, 2, 10, 1, 8, "minecraft:spruce_planks")
    walls(t, 2, 2, 10, 8, 2, 4, "minecraft:white_concrete", "minecraft:stripped_dark_oak_log")
    for x in (4, 6, 8):
        for z in (2, 8):
            t.fill(x, 2, z, x, 4, z, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    for x in (2, 10):
        t.fill(x, 2, 5, x, 4, 5, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    for x in (3, 5, 7, 9):  # 창호
        for y in (2, 3):
            t.set(x, y, 8, "minecraft:birch_trapdoor", {"facing": "south", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    door(t, 6, 2, 8, block="minecraft:birch_door")
    for z in (3, 7):  # 옆 창호
        t.set(2, 3, z, "minecraft:birch_trapdoor", {"facing": "west", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
        t.set(10, 3, z, "minecraft:birch_trapdoor", {"facing": "east", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    # 대청마루 (앞 · 한 줄) + 기둥
    t.fill(1, 1, 9, 11, 1, 9, "minecraft:spruce_planks")
    for x in (1, 4, 8, 11):
        t.fill(x, 2, 9, x, 4, 9, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    # 기와지붕: 넓게 (처마 2칸) + 끝 들림
    tile = "minecraft:deepslate_tile_stairs"
    y = 5
    l, r = 0, 12
    while l < r:
        for z in range(0, 12):
            b, p = stair(tile, "east")
            t.set(l, y, z, b, p)
            b, p = stair(tile, "west")
            t.set(r, y, z, b, p)
        l += 1
        r -= 1
        y += 1
        if y > 9:
            break
    for x in range(l, r + 1):
        for z in range(0, 12):
            t.set(x, min(y, 9), z, "minecraft:deepslate_tiles")
    for z in (0, 11):  # 앞뒤 처마 끝 (들림)
        for x in range(1, 12):
            b, p = stair(tile, "north" if z == 0 else "south", "top")
            t.set(x, 5, z, b, p)
    for (x, z) in ((0, 0), (12, 0), (0, 11), (12, 11)):
        t.set(x, 6, z, "minecraft:deepslate_tile_wall", {"east": "none", "west": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false"})
    t.fill(2, 5, 2, 10, 5, 8, "minecraft:spruce_planks")
    # 칸막이: 뒤쪽에 안방(서) · 사랑방(동), 앞쪽은 대청
    wall(t, 3, 5, 9, 5, 2, 4, "minecraft:white_concrete")
    wall(t, 6, 3, 6, 4, 2, 4, "minecraft:white_concrete")
    for x in (3, 9):
        t.fill(x, 2, 5, x, 4, 5, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    t.fill(6, 2, 5, 6, 4, 5, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    door(t, 4, 2, 5, block="minecraft:birch_door", facing="south")
    door(t, 8, 2, 5, block="minecraft:birch_door", facing="south")
    # 안방: 침대 · 협탁 · 화장대 대신 장 (상자)
    furn(t, 3, 2, 4, "bunk_bed", "north")
    furn(t, 5, 2, 3, "nightstand", "south")
    t.set(4, 2, 3, *CHEST("south"))
    t.set(4, 2, 4, CARPET("red"))
    # 사랑방 (서재): 책장 · 괘종시계 · 방석
    furn(t, 8, 2, 3, "bookshelf", "south")
    furn(t, 9, 2, 3, "grandfather_clock", "south")
    t.set(8, 2, 4, CARPET("yellow"))
    # 대청: 다과상 · 방석 · 등
    furn(t, 6, 2, 6, "coffee_table", "south")
    for (x, z) in ((5, 6), (7, 6), (6, 7)):
        t.set(x, 2, z, CARPET("red"))
    for (x, z) in ((4, 4), (8, 4), (6, 6)):
        t.set(x, 4, z, "minecraft:lantern", LANTERN_H)
    # 돌계단 · 장독대
    t.set(6, 0, 10, "minecraft:stone_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for x in (9, 10, 11):
        t.set(x, 1, 11, "minecraft:decorated_pot", {"facing": "north", "cracked": "false", "waterlogged": "false"})
    return t.save("house/hanok")


# ═══════════ 5. 모던 주택 (13×13): 1층 거실·주방 / 2층 침실 + 드레스룸 / 옥상 정원 ═══════════
def modern():
    t = Template(13, 10, 13)
    t.fill(0, 0, 0, 12, 0, 12, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 11, 0, 9, "minecraft:smooth_stone")
    t.fill(2, 0, 2, 10, 0, 8, "minecraft:polished_diorite")
    walls(t, 1, 1, 11, 9, 1, 4, "minecraft:white_concrete")
    t.fill(1, 5, 1, 11, 5, 9, "minecraft:gray_concrete")  # 1층 지붕 = 테라스
    for x in range(2, 7):  # 통유리 (앞 왼쪽)
        for y in (1, 2, 3):
            pane(t, x, y, 9, "x", "minecraft:light_gray_stained_glass_pane")
    for x in range(7, 11):  # 나무 루버
        for y in range(1, 5):
            t.set(x, y, 9, "minecraft:stripped_spruce_wood", {"axis": "y"})
    door(t, 8, 1, 9, block="minecraft:iron_door")
    t.set(9, 2, 10, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    t.set(7, 2, 8, "minecraft:stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    for z in (3, 6):
        for y in (2, 3):
            pane(t, 11, y, z, "z", "minecraft:light_gray_stained_glass_pane")
    # 2층 상자 (뒤쪽 오른쪽)
    walls(t, 5, 1, 11, 6, 6, 8, "minecraft:gray_concrete")
    t.fill(5, 9, 1, 11, 9, 6, "minecraft:white_concrete")
    for x in (6, 9, 10):
        for y in (6, 7):
            pane(t, x, y, 6, "x", "minecraft:light_gray_stained_glass_pane")
    t.fill(6, 5, 2, 10, 5, 5, "minecraft:polished_diorite")
    door(t, 7, 6, 6, block="minecraft:birch_door")
    # 옥상 정원 + 유리 난간
    t.fill(1, 6, 7, 4, 6, 9, "minecraft:moss_block")
    for x in range(5, 12):
        pane(t, x, 6, 9, "x")
    for z in range(7, 10):
        pane(t, 11, 6, z, "z")
    for x in (2, 3):
        t.set(x, 7, 8, "minecraft:azalea")
    # 수영장 + 선베드
    t.fill(1, 0, 10, 7, 0, 12, "minecraft:quartz_block")
    t.fill(2, 0, 11, 6, 0, 11, "minecraft:water", {"level": "0"})
    t.set(9, 1, 11, "minecraft:white_carpet")
    t.set(10, 1, 11, "minecraft:white_carpet")
    # 계단 (뒤 벽 따라 5단 → 2층 침실로 바로)
    for k in range(5):
        b, p = stair("minecraft:smooth_quartz_stairs", "east")
        t.set(2 + k, 1 + k, 2, b, p)
    for (x, y) in ((4, 5), (5, 5), (5, 6), (5, 7)):
        t.set(x, y, 2, "minecraft:air")
    # 1층: 거실 (소파 · TV · 러그 · 화분) + 주방 (동쪽 벽)
    furn(t, 4, 1, 4, "sofa_gray", "south")
    furn(t, 4, 1, 7, "tv_stand", "north")
    furn(t, 2, 1, 8, "monstera", "south")
    furn(t, 10, 1, 2, "fridge", "west")
    furn(t, 10, 1, 3, "sink", "west")
    t.set(10, 1, 4, "wildplus:kitchen_stove", {"facing": "west", "lit": "false"})
    furn(t, 8, 1, 4, "coffee_table", "south")
    # 2층 칸막이: 침실 (x 6~7) | 드레스룸 (x 9~10)
    wall(t, 8, 2, 8, 5, 6, 8, "minecraft:white_concrete")
    door(t, 8, 6, 5, block="minecraft:birch_door", facing="east")
    furn(t, 7, 6, 4, "canopy_bed", "north")
    furn(t, 7, 6, 2, "floor_lamp", "south")
    t.set(6, 6, 3, CARPET("light_gray"))
    # 드레스룸: 옷장 · 화장대 · 전신 거울 대신 옷 상자
    furn(t, 10, 6, 2, "wardrobe", "south")
    furn(t, 10, 6, 4, "vanity", "west")
    t.set(10, 6, 5, *CHEST("west"))
    t.set(9, 6, 3, CARPET("pink"))
    for (x, y, z) in ((6, 4, 5), (9, 4, 6), (7, 8, 3), (9, 8, 3)):
        t.set(x, y, z, "minecraft:sea_lantern")
    return t.save("house/modern")


# ═══════════ 6. 대저택 (19×18): 1층 응접실·식당·주방 / 2층 안방·손님방·옷방·욕실 / 3층 서재·음악실 ═══════════
def mansion():
    t = Template(19, 19, 18)
    t.fill(0, 0, 0, 18, 0, 17, "minecraft:grass_block", {"snowy": "false"})
    t.fill(2, 0, 1, 16, 0, 12, "minecraft:polished_andesite")
    t.fill(3, 0, 2, 15, 0, 11, "minecraft:dark_oak_planks")
    walls(t, 2, 1, 16, 12, 1, 9, "minecraft:sandstone", "minecraft:chiseled_sandstone")
    for y in (4, 7):
        t.fill(3, y, 2, 15, y, 11, "minecraft:dark_oak_planks")
        for x in range(2, 17):
            for z in (1, 12):
                t.set(x, y, z, "minecraft:smooth_quartz")
    for y in (2, 5, 8):
        for x in (3, 4, 6, 12, 14, 15):
            pane(t, x, y, 12)
            pane(t, x, y, 1)
        for z in (3, 5):
            pane(t, 2, y, z, "z")
            pane(t, 16, y, z, "z")
    # 탑 2개 (앞 모서리)
    for (x0, x1) in ((0, 2), (16, 18)):
        walls(t, x0, 8, x1, 12, 1, 12, "minecraft:sandstone", "minecraft:cut_sandstone")
        t.fill(x0 + 1, 13, 9, x1 - 1, 13, 11, "minecraft:blue_terracotta")
        t.fill(x0 + 1, 14, 10, x1 - 1, 15, 10, "minecraft:blue_terracotta")
        t.set((x0 + x1) // 2, 16, 10, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        for y in (5, 9):
            pane(t, (x0 + x1) // 2, y, 12)
    # 기둥 현관 + 박공
    t.fill(6, 0, 13, 12, 0, 14, "minecraft:smooth_quartz")
    for x in (6, 12):
        t.fill(x, 1, 14, x, 5, 14, "minecraft:quartz_pillar", {"axis": "y"})
    for x in (7, 11):
        t.fill(x, 1, 14, x, 5, 14, "minecraft:quartz_pillar", {"axis": "y"})
    t.fill(5, 6, 13, 13, 6, 14, "minecraft:smooth_quartz")
    for k in range(3):
        t.fill(6 + k, 7 + k, 13, 12 - k, 7 + k, 14, "minecraft:smooth_quartz_slab" if k == 2 else "minecraft:smooth_quartz",
               {"type": "bottom", "waterlogged": "false"} if k == 2 else None)
    door(t, 9, 1, 12, block="minecraft:dark_oak_door", hinge="left")
    door(t, 8, 1, 12, block="minecraft:dark_oak_door", hinge="right")
    t.set(9, 3, 12, "minecraft:yellow_stained_glass")
    t.set(8, 3, 12, "minecraft:yellow_stained_glass")
    # 지붕
    t.fill(3, 10, 2, 15, 10, 11, "minecraft:dark_oak_planks")
    gable(t, 2, 16, 1, 12, 10, "minecraft:deepslate_brick_stairs", "minecraft:deepslate_brick_slab", "minecraft:sandstone")
    # 분수 + 꽃밭
    t.fill(7, 0, 15, 11, 1, 17, "minecraft:smooth_stone")
    t.fill(8, 1, 16, 10, 1, 16, "minecraft:water", {"level": "0"})
    t.set(9, 2, 16, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(9, 1, 16, "minecraft:smooth_stone")
    for x in (1, 2, 3, 15, 16, 17):
        for z in (14, 15, 16):
            t.set(x, 1, z, ["minecraft:poppy", "minecraft:dandelion", "minecraft:allium"][(x + z) % 3])
    IW = "minecraft:stripped_dark_oak_wood"
    # 계단: 1층 → 2층 (동쪽 x15, 남쪽으로 오름) · 2층 → 3층 (x14, 북쪽으로 오름)
    for k in range(4):
        b, p = stair("minecraft:dark_oak_stairs", "south")
        t.set(15, 1 + k, 5 + k, b, p)
    for z in (5, 6, 7):
        t.set(15, 4, z, "minecraft:air")
    for k in range(3):
        b, p = stair("minecraft:dark_oak_stairs", "north")
        t.set(14, 5 + k, 8 - k, b, p)
    for z in (7, 8):
        t.set(14, 7, z, "minecraft:air")
    # ── 1층: 응접실 (앞) | 식당 (뒤 서) · 주방 (뒤 동) ──
    wall(t, 3, 7, 14, 7, 1, 3, IW, {"axis": "y"})
    opening(t, 5, 1, 7)
    opening(t, 6, 1, 7)
    opening(t, 12, 1, 7)
    wall(t, 9, 2, 9, 6, 1, 3, IW, {"axis": "y"})
    opening(t, 9, 1, 4)
    furn(t, 9, 1, 8, "fireplace", "south")
    furn(t, 6, 1, 10, "sofa_red", "north")
    furn(t, 12, 1, 10, "sofa_red", "north")
    furn(t, 9, 1, 10, "coffee_table", "south")
    furn(t, 14, 1, 10, "aquarium", "west")
    furn(t, 4, 1, 8, "monstera", "south")
    furn(t, 9, 3, 9, "chandelier", "south")
    # 식당: 식탁 · 의자 4
    t.set(6, 1, 4, "wildplus:dining_table", {"facing": "south"})
    for (x, z, f) in ((5, 3, "south"), (6, 3, "south"), (5, 5, "north"), (6, 5, "north")):
        t.set(x, 1, z, "wildplus:dining_chair", {"facing": f})
    furn(t, 3, 1, 2, "grandfather_clock", "south")
    # 주방: 냉장고 · 싱크대 · 화구 · 오븐 상자
    furn(t, 13, 1, 2, "fridge", "south")
    furn(t, 12, 1, 2, "sink", "south")
    t.set(11, 1, 2, "wildplus:kitchen_stove", {"facing": "south", "lit": "false"})
    t.set(10, 1, 2, "minecraft:smoker", {"facing": "south", "lit": "false"})
    t.set(10, 1, 6, "minecraft:barrel", {"facing": "up", "open": "false"})
    # ── 2층: 가운데 복도 (z 7) | 북: 안방 · 손님방 | 남: 옷방 · 욕실 ──
    wall(t, 3, 6, 13, 6, 5, 6, "minecraft:smooth_sandstone")
    wall(t, 3, 8, 13, 8, 5, 6, "minecraft:smooth_sandstone")
    wall(t, 9, 2, 9, 5, 5, 6, "minecraft:smooth_sandstone")
    wall(t, 7, 9, 7, 11, 5, 6, "minecraft:smooth_sandstone")
    door(t, 5, 5, 6, block="minecraft:dark_oak_door", facing="north")
    door(t, 11, 5, 6, block="minecraft:dark_oak_door", facing="north")
    door(t, 5, 5, 8, block="minecraft:dark_oak_door", facing="south")
    door(t, 10, 5, 8, block="minecraft:dark_oak_door", facing="south")
    # 안방
    furn(t, 5, 5, 4, "canopy_bed", "north")
    furn(t, 4, 5, 3, "nightstand", "south")
    furn(t, 6, 5, 3, "nightstand", "south")
    furn(t, 8, 5, 2, "floor_lamp", "south")
    t.fill(4, 5, 5, 7, 5, 5, CARPET("purple"))
    # 손님방
    furn(t, 11, 5, 4, "bunk_bed", "north")
    furn(t, 13, 5, 2, "floor_lamp", "south")
    t.set(10, 5, 2, *CHEST("south"))
    # 옷방
    furn(t, 3, 5, 9, "wardrobe", "east")
    furn(t, 4, 5, 11, "wardrobe", "north")
    furn(t, 6, 5, 10, "vanity", "west")
    # 욕실
    furn(t, 9, 5, 11, "bathtub", "north")
    furn(t, 12, 5, 11, "sink", "north")
    furn(t, 13, 5, 9, "monstera", "south")
    # ── 3층: 서재 (서) | 음악실 (동) ──
    wall(t, 9, 2, 9, 11, 8, 9, "minecraft:smooth_sandstone")
    door(t, 9, 8, 9, block="minecraft:dark_oak_door", facing="east")
    furn(t, 4, 8, 2, "bookshelf", "south")
    furn(t, 7, 8, 2, "bookshelf", "south")
    furn(t, 6, 8, 6, "desk", "south")
    furn(t, 4, 8, 9, "armchair", "east")
    furn(t, 3, 8, 11, "grandfather_clock", "east")
    furn(t, 11, 8, 2, "piano", "south")
    furn(t, 12, 8, 6, "armchair", "north")
    furn(t, 13, 8, 11, "aquarium", "west")
    furn(t, 11, 8, 11, "floor_lamp", "north")
    for (x, y, z) in ((5, 6, 3), (11, 6, 3), (5, 9, 6), (12, 9, 6), (8, 6, 7), (4, 3, 4), (12, 3, 4)):
        t.set(x, y, z, "minecraft:lantern", LANTERN_H)
    return t.save("house/mansion")



# ═══════════ 3.4 목장: 큰 축사 (13×15) · 닭장 (7×9) ═══════════
FENCE_X = {"east": "true", "west": "true", "north": "false", "south": "false", "waterlogged": "false"}
FENCE_Z = {"east": "false", "west": "false", "north": "true", "south": "true", "waterlogged": "false"}


def gate(t, x, y, z, facing="south"):
    t.set(x, y, z, "minecraft:spruce_fence_gate", {"facing": facing, "in_wall": "false", "open": "false", "powered": "false"})


def barn():
    t = Template(13, 11, 15)
    t.fill(0, 0, 0, 12, 0, 14, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 11, 0, 9, "minecraft:coarse_dirt")
    t.fill(5, 0, 2, 7, 0, 9, "minecraft:spruce_planks")  # 가운데 통로
    walls(t, 1, 1, 11, 9, 1, 4, "minecraft:red_terracotta", "minecraft:stripped_birch_log")
    for x in (1, 11):  # 흰 테두리 띠
        for z in range(1, 10):
            t.set(x, 4, z, "minecraft:white_terracotta")
    for x in range(1, 12):
        for z in (1, 9):
            t.set(x, 4, z, "minecraft:white_terracotta")
    for z in (1, 9):
        for x in (1, 11):
            t.fill(x, 1, z, x, 4, z, "minecraft:stripped_birch_log", {"axis": "y"})
    for x in (5, 6, 7):  # 큰 출입구 (3칸)
        for y in (1, 2, 3):
            t.set(x, y, 9, "minecraft:air")
    for z in (3, 7):
        pane(t, 1, 2, z, "z")
        pane(t, 11, 2, z, "z")
    # 우리 두 칸 (울타리 · 문) + 여물통
    for z in range(2, 9):
        t.set(4, 1, z, "minecraft:spruce_fence", FENCE_Z)
        t.set(8, 1, z, "minecraft:spruce_fence", FENCE_Z)
    gate(t, 4, 1, 6, "east")
    gate(t, 8, 1, 6, "west")
    t.set(2, 1, 3, "wildplus:hay_trough", {"facing": "east"})
    t.set(10, 1, 3, "wildplus:hay_trough", {"facing": "west"})
    for (x, z) in ((2, 8), (10, 8)):
        t.set(x, 1, z, "minecraft:hay_block", {"axis": "y"})
    t.set(6, 1, 2, "minecraft:water_cauldron", {"level": "3"})
    # 건초 다락 (사다리) + 건초 더미
    t.fill(2, 5, 2, 10, 5, 8, "minecraft:spruce_planks")
    t.fill(1, 5, 1, 11, 5, 1, "minecraft:red_terracotta")
    t.fill(1, 5, 9, 11, 5, 9, "minecraft:red_terracotta")
    t.fill(1, 5, 2, 1, 5, 8, "minecraft:red_terracotta")
    t.fill(11, 5, 2, 11, 5, 8, "minecraft:red_terracotta")
    t.fill(6, 1, 2, 6, 5, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    t.set(6, 1, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for (x, z) in ((2, 3), (3, 3), (2, 4), (9, 7), (10, 7), (10, 6), (2, 7), (3, 8)):
        t.set(x, 6, z, "minecraft:hay_block", {"axis": "y"})
    t.set(3, 7, 3, "minecraft:hay_block", {"axis": "y"})
    pane(t, 6, 6, 9)
    gable(t, 1, 11, 1, 9, 5, "minecraft:dark_oak_stairs", "minecraft:dark_oak_slab", "minecraft:red_terracotta", over=0)
    for (x, y, z) in ((3, 4, 5), (9, 4, 5), (6, 4, 7)):
        t.set(x, y, z, "minecraft:lantern", LANTERN_H)
    # 앞마당 울타리 + 문
    for x in range(1, 12):
        if x != 6:
            t.set(x, 1, 14, "minecraft:spruce_fence", FENCE_X)
    gate(t, 6, 1, 14, "south")
    for z in range(10, 14):
        t.set(1, 1, z, "minecraft:spruce_fence", FENCE_Z)
        t.set(11, 1, z, "minecraft:spruce_fence", FENCE_Z)
    t.set(1, 1, 14, "minecraft:spruce_fence", {"east": "true", "west": "false", "north": "true", "south": "false", "waterlogged": "false"})
    t.set(11, 1, 14, "minecraft:spruce_fence", {"east": "false", "west": "true", "north": "true", "south": "false", "waterlogged": "false"})
    t.set(2, 1, 12, "minecraft:hay_block", {"axis": "y"})
    t.set(10, 1, 11, "minecraft:composter", {"level": "0"})
    return t.save("house/barn")


def coop():
    t = Template(7, 6, 9)
    t.fill(0, 0, 0, 6, 0, 8, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 5, 0, 4, "minecraft:hay_block", {"axis": "y"})
    walls(t, 1, 1, 5, 4, 1, 3, "minecraft:birch_planks", "minecraft:stripped_birch_log")
    for (x, z) in ((1, 1), (5, 1), (1, 4), (5, 4)):
        t.fill(x, 1, z, x, 3, z, "minecraft:stripped_birch_log", {"axis": "y"})
    t.set(3, 1, 4, "minecraft:air")  # 닭 출입구
    t.set(3, 2, 4, "minecraft:air")
    pane(t, 1, 2, 2, "z")
    pane(t, 5, 2, 2, "z")
    t.set(2, 1, 2, "wildplus:nest_box", {"facing": "south"})
    t.set(4, 1, 2, "wildplus:nest_box", {"facing": "south"})
    t.set(3, 3, 3, "minecraft:lantern", LANTERN_H)
    for x in range(0, 7):  # 경사 지붕
        t.set(x, 4, 0, "minecraft:mangrove_slab", {"type": "bottom", "waterlogged": "false"})
        for z in (1, 2):
            t.set(x, 4, z, "minecraft:mangrove_planks")
        for z in (3, 4, 5):
            t.set(x, 4, z, "minecraft:mangrove_slab", {"type": "bottom", "waterlogged": "false"})
    for x in range(1, 6):
        t.set(x, 5, 1, "minecraft:mangrove_slab", {"type": "bottom", "waterlogged": "false"})
        t.set(x, 5, 2, "minecraft:mangrove_slab", {"type": "bottom", "waterlogged": "false"})
    # 철망 마당 (울타리) + 문 + 물그릇
    for z in range(5, 9):
        t.set(0, 1, z, "minecraft:birch_fence", FENCE_Z)
        t.set(6, 1, z, "minecraft:birch_fence", FENCE_Z)
    for x in range(0, 7):
        if x != 3:
            t.set(x, 1, 8, "minecraft:birch_fence", FENCE_X)
    gate(t, 3, 1, 8, "south")
    t.set(5, 1, 6, "minecraft:water_cauldron", {"level": "3"})
    t.set(1, 1, 6, "minecraft:hay_block", {"axis": "y"})
    return t.save("house/coop")


BARNS = {"barn": barn, "coop": coop}


HOUSES = {"cabin": cabin, "log_house": log_house, "brick_house": brick_house, "hanok": hanok, "modern": modern, "mansion": mansion}


def build():
    return {k: fn() for k, fn in {**HOUSES, **BARNS}.items()}


if __name__ == "__main__":
    print(build())
