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


# ═══════════ 1. 통나무 오두막 (9×11) ═══════════
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
    t.fill(2, 5, 2, 6, 5, 6, "minecraft:spruce_planks")  # 천장
    gable(t, 1, 7, 1, 7, 5, "minecraft:mangrove_stairs", "minecraft:mangrove_slab", "minecraft:spruce_planks")
    t.fill(2, 1, 1, 2, 9, 1, "minecraft:cobblestone")  # 굴뚝
    t.set(2, 9, 1, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # 현관 데크 + 난간 + 등
    t.fill(1, 0, 8, 7, 0, 9, "minecraft:spruce_planks")
    for x in (1, 2, 6, 7):
        t.set(x, 1, 9, "minecraft:spruce_fence")
    t.set(5, 3, 8, "minecraft:lantern", LANTERN_H)
    t.set(5, 4, 8, "minecraft:spruce_fence")
    t.set(1, 1, 8, "minecraft:potted_poppy")
    # 안: 이층 침대 · 상자 · 작업대 · 화로 · 안락의자 · 스탠드
    furn(t, 2, 1, 3, "bunk_bed", "south")
    t.set(6, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"})
    t.set(5, 1, 2, "minecraft:crafting_table")
    t.set(4, 1, 2, "minecraft:furnace", {"facing": "south", "lit": "false"})
    furn(t, 6, 1, 5, "armchair", "west")
    furn(t, 6, 1, 6, "floor_lamp", "west")
    t.set(4, 4, 4, "minecraft:lantern", LANTERN_H)
    return t.save("house/cabin")


# ═══════════ 2. 숲속 통나무집 (11×11) ═══════════
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
    gable(t, 1, 9, 1, 7, 5, "minecraft:dark_oak_stairs", "minecraft:dark_oak_slab", "minecraft:oak_planks")
    t.fill(7, 1, 1, 7, 10, 1, "minecraft:bricks")
    # 베란다 (지붕 · 기둥)
    t.fill(1, 0, 8, 9, 0, 10, "minecraft:oak_planks")
    for x in (1, 5, 9):
        t.fill(x, 1, 10, x, 3, 10, "minecraft:dark_oak_fence")
    for x in range(0, 11):
        for z in (8, 9, 10):
            b, p = stair("minecraft:dark_oak_stairs", "north")
            t.set(x, 4, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(3, 3, 9, "minecraft:lantern", LANTERN_H)
    t.set(7, 3, 9, "minecraft:lantern", LANTERN_H)
    furn(t, 7, 1, 9, "armchair", "south")
    # 장작더미
    t.fill(0, 1, 2, 0, 2, 4, "minecraft:oak_log", {"axis": "x"})
    # 안: 벽난로 · 소파 · 침대 2 · 테이블
    furn(t, 4, 1, 2, "fireplace", "south")
    furn(t, 5, 1, 5, "sofa_brown", "north")
    furn(t, 5, 1, 4, "rug", "south")
    furn(t, 2, 1, 3, "bunk_bed", "east")
    furn(t, 8, 1, 2, "fridge", "south")
    t.set(8, 1, 6, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"})
    t.set(5, 4, 4, "minecraft:lantern", LANTERN_H)
    return t.save("house/log_house")


# ═══════════ 3. 벽돌 2층집 (11×11) ═══════════
def brick_house():
    t = Template(11, 15, 11)
    t.fill(0, 0, 0, 10, 0, 10, "minecraft:grass_block", {"snowy": "false"})
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
    for x in (2, 3, 5, 7, 8):
        for y in (6, 7):
            pane(t, x, y, 9)
    for z in (3, 4, 6, 7):
        for y in (2, 6):
            pane(t, 1, y, z, "z")
            pane(t, 9, y, z, "z")
    # 발코니 (2층 문 앞)
    door(t, 4, 5, 9, block="minecraft:birch_door")
    t.fill(3, 4, 10, 6, 4, 10, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
    for x in range(3, 7):
        t.set(x, 5, 10, "minecraft:birch_fence")
    t.fill(2, 9, 2, 8, 9, 8, "minecraft:birch_planks")
    gable(t, 1, 9, 1, 9, 9, "minecraft:red_nether_brick_stairs", "minecraft:red_nether_brick_slab", "minecraft:bricks")
    t.fill(8, 9, 2, 8, 14, 2, "minecraft:bricks")
    # 계단 (안) : 1층 → 2층
    for k in range(3):
        b, p = stair("minecraft:birch_stairs", "north")
        t.set(8, 1 + k, 7 - k, b, p)
    t.set(8, 4, 5, "minecraft:air")
    t.set(8, 4, 6, "minecraft:air")
    t.set(8, 4, 4, "minecraft:air")
    # 1층: 거실 · 주방
    furn(t, 3, 1, 3, "sofa_navy", "south")
    furn(t, 3, 1, 6, "tv_stand", "north")
    furn(t, 3, 1, 5, "coffee_table", "south")
    furn(t, 6, 1, 2, "fridge", "south")
    furn(t, 7, 1, 2, "sink", "south")
    furn(t, 2, 1, 8, "monstera", "south")
    # 2층: 침실 · 서재
    furn(t, 2, 5, 3, "canopy_bed", "south")
    furn(t, 6, 5, 2, "wardrobe", "south")
    furn(t, 2, 5, 7, "desk", "east")
    furn(t, 5, 5, 6, "nightstand", "south")
    for (x, y, z) in ((5, 3, 5), (5, 8, 5)):
        t.set(x, y, z, "minecraft:lantern", LANTERN_H)
    # 앞마당: 우체통 · 화단
    t.set(9, 1, 10, "minecraft:cobblestone_wall", {"east": "none", "west": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false"})
    t.set(9, 2, 10, "minecraft:red_terracotta")
    for x in (1, 2, 3):
        t.set(x, 1, 10, "minecraft:rose_bush", {"half": "lower"})
        t.set(x, 2, 10, "minecraft:rose_bush", {"half": "upper"})
    return t.save("house/brick_house")


# ═══════════ 4. 한옥 (13×12) ═══════════
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
    for x in range(2, 11):  # 창호 (격자 = 자작나무 다락문 대신 비계 느낌의 창)
        if x in (3, 5, 7, 9):
            for y in (2, 3):
                t.set(x, y, 8, "minecraft:birch_trapdoor", {"facing": "south", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    door(t, 6, 2, 8, block="minecraft:birch_door")
    t.set(6, 2, 8, "minecraft:birch_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
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
    # 돌계단 · 장독대
    t.set(6, 0, 10, "minecraft:stone_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for x in (9, 10, 11):
        t.set(x, 1, 11, "minecraft:decorated_pot", {"facing": "north", "cracked": "false", "waterlogged": "false"})
    # 안: 이불 대신 침대 · 책장 · 괘종시계
    furn(t, 3, 2, 4, "bunk_bed", "south")
    furn(t, 8, 2, 3, "bookshelf", "south")
    furn(t, 6, 2, 3, "grandfather_clock", "south")
    furn(t, 6, 2, 6, "coffee_table", "south")
    t.set(6, 4, 5, "minecraft:lantern", LANTERN_H)
    return t.save("house/hanok")


# ═══════════ 5. 모던 주택 (13×13) ═══════════
def modern():
    t = Template(13, 10, 13)
    t.fill(0, 0, 0, 12, 0, 12, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 11, 0, 9, "minecraft:smooth_stone")
    t.fill(2, 0, 2, 10, 0, 8, "minecraft:polished_diorite")
    walls(t, 1, 1, 11, 9, 1, 4, "minecraft:white_concrete")
    t.fill(1, 5, 1, 11, 5, 9, "minecraft:gray_concrete")  # 1층 지붕 = 테라스
    # 통유리 (앞 왼쪽)
    for x in range(2, 7):
        for y in (1, 2, 3):
            pane(t, x, y, 9, "x", "minecraft:light_gray_stained_glass_pane")
    for x in range(7, 11):  # 나무 루버
        for y in range(1, 5):
            t.set(x, y, 9, "minecraft:stripped_spruce_wood", {"axis": "y"})
    door(t, 8, 1, 9, block="minecraft:iron_door")
    t.set(9, 2, 10, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    # 2층 상자 (뒤쪽 오른쪽)
    walls(t, 5, 1, 11, 6, 6, 8, "minecraft:gray_concrete")
    t.fill(5, 9, 1, 11, 9, 6, "minecraft:white_concrete")
    for x in range(6, 11):
        for y in (6, 7):
            pane(t, x, y, 6, "x", "minecraft:light_gray_stained_glass_pane")
    t.fill(6, 5, 2, 10, 5, 5, "minecraft:polished_diorite")
    door(t, 8, 6, 6, block="minecraft:iron_door")
    # 옥상 정원 + 유리 난간
    t.fill(1, 6, 7, 4, 6, 9, "minecraft:moss_block")
    for x in range(1, 12):
        pane(t, x, 6, 9, "x") if x > 4 else None
    for x in (2, 3):
        t.set(x, 7, 8, "minecraft:azalea")
    # 수영장 + 선베드
    t.fill(1, 0, 10, 7, 0, 12, "minecraft:quartz_block")
    t.fill(2, 0, 11, 6, 0, 11, "minecraft:water", {"level": "0"})
    t.fill(2, -0, 11, 6, 0, 11, "minecraft:water", {"level": "0"})
    t.set(9, 1, 11, "minecraft:white_carpet")
    t.set(10, 1, 11, "minecraft:white_carpet")
    # 안: 소파 · TV · 주방 · 침실 (2층)
    furn(t, 4, 1, 3, "sofa_gray", "south")
    furn(t, 3, 1, 7, "tv_stand", "north")
    furn(t, 4, 1, 5, "rug", "south")
    furn(t, 10, 1, 2, "fridge", "west")
    furn(t, 10, 1, 3, "sink", "west")
    furn(t, 2, 1, 8, "monstera", "south")
    furn(t, 6, 6, 3, "canopy_bed", "south")
    furn(t, 9, 6, 2, "vanity", "south")
    furn(t, 10, 6, 5, "floor_lamp", "west")
    # 계단 (안쪽 왼쪽 → 2층은 테라스 쪽 문)
    for k in range(4):
        b, p = stair("minecraft:smooth_quartz_stairs", "east")
        t.set(2 + k, 1 + k, 2, b, p)
    t.set(5, 5, 2, "minecraft:air")
    t.set(4, 5, 2, "minecraft:air")
    for (x, y, z) in ((6, 4, 5), (8, 8, 3)):
        t.set(x, y, z, "minecraft:sea_lantern")
    return t.save("house/modern")


# ═══════════ 6. 대저택 (19×18) ═══════════
def mansion():
    t = Template(19, 18, 18)
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
        for z in (3, 5, 8, 10):
            pane(t, 2, y, z, "z")
            pane(t, 16, y, z, "z")
    # 탑 2개 (앞 모서리)
    for tx in (0, 17):
        for x in (tx, tx + 1) if tx == 0 else (tx - 0, tx + 1):
            pass
    for (x0, x1) in ((0, 2), (16, 18)):
        walls(t, x0, 8, x1, 12, 1, 12, "minecraft:sandstone", "minecraft:cut_sandstone")
        t.fill(x0 + 1, 13, 9, x1 - 1, 13, 11, "minecraft:blue_terracotta")
        t.fill(x0 + 1, 14, 10, x1 - 1, 15, 10, "minecraft:blue_terracotta")
        t.set((x0 + x1) // 2, 16, 10, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        for y in (5, 9):
            pane(t, (x0 + x1) // 2, y, 12)
    # 기둥 현관 + 박공
    t.fill(6, 0, 13, 12, 0, 14, "minecraft:smooth_quartz")
    for x in (6, 8, 10, 12):
        t.fill(x, 1, 14, x, 5, 14, "minecraft:quartz_pillar", {"axis": "y"})
    t.fill(5, 6, 13, 13, 6, 14, "minecraft:smooth_quartz")
    for k in range(3):
        t.fill(6 + k, 7 + k, 13, 12 - k, 7 + k, 14, "minecraft:smooth_quartz_slab" if k == 2 else "minecraft:smooth_quartz",
               {"type": "bottom", "waterlogged": "false"} if k == 2 else None)
    door(t, 9, 1, 12, block="minecraft:dark_oak_door", hinge="left")
    door(t, 8, 1, 12, block="minecraft:dark_oak_door", hinge="right")
    t.set(9, 3, 12, "minecraft:yellow_stained_glass")
    t.set(8, 3, 12, "minecraft:yellow_stained_glass")
    # 지붕 (네모 낮은 맞배)
    t.fill(3, 10, 2, 15, 10, 11, "minecraft:dark_oak_planks")
    gable(t, 2, 16, 1, 12, 10, "minecraft:blue_terracotta" if False else "minecraft:deepslate_brick_stairs", "minecraft:deepslate_brick_slab",
          "minecraft:sandstone")
    # 분수 + 꽃밭
    t.fill(7, 0, 15, 11, 1, 17, "minecraft:smooth_stone")
    t.fill(8, 1, 16, 10, 1, 16, "minecraft:water", {"level": "0"})
    t.set(9, 2, 16, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(9, 1, 16, "minecraft:smooth_stone")
    for x in (1, 2, 3, 15, 16, 17):
        for z in (14, 15, 16):
            t.set(x, 1, z, ["minecraft:poppy", "minecraft:dandelion", "minecraft:allium"][(x + z) % 3])
    # 안: 1층 응접실 · 2층 침실 2 · 3층 서재
    furn(t, 5, 1, 3, "sofa_red", "south")
    furn(t, 13, 1, 3, "sofa_red", "south")
    furn(t, 8, 1, 2, "fireplace", "south")
    furn(t, 8, 1, 6, "rug", "south")
    furn(t, 4, 1, 9, "piano", "east")
    furn(t, 14, 1, 9, "aquarium", "west")
    furn(t, 9, 3, 7, "chandelier", "south")
    furn(t, 3, 5, 3, "canopy_bed", "south")
    furn(t, 13, 5, 3, "canopy_bed", "south")
    furn(t, 6, 5, 2, "wardrobe", "south")
    furn(t, 11, 5, 2, "vanity", "south")
    furn(t, 3, 8, 2, "bookshelf", "south")
    furn(t, 13, 8, 2, "bookshelf", "south")
    furn(t, 8, 8, 6, "desk", "south")
    furn(t, 15, 8, 10, "grandfather_clock", "west")
    furn(t, 3, 5, 10, "bathtub", "east")
    # 계단 (뒤 가운데 · 1→2→3층)
    for k in range(3):
        b, p = stair("minecraft:dark_oak_stairs", "south")
        t.set(16 - 1, 1 + k, 5 + k, b, p)
        t.set(16 - 1, 4 + 1 + k, 5 + k, b, p)
    for z in (5, 6, 7, 8):
        t.set(15, 4, z, "minecraft:air")
        t.set(15, 7, z, "minecraft:air")
    for (x, y, z) in ((9, 6, 9), (9, 9, 9), (4, 3, 6), (14, 3, 6)):
        t.set(x, y, z, "minecraft:lantern", LANTERN_H)
    return t.save("house/mansion")


HOUSES = {"cabin": cabin, "log_house": log_house, "brick_house": brick_house, "hanok": hanok, "modern": modern, "mansion": mansion}


def build():
    return {k: fn() for k, fn in HOUSES.items()}


if __name__ == "__main__":
    print(build())
