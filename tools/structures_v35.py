"""3.5 구조물: 경마장 (37×41) · 마을 발전 건물 5종 × 3단계 (15×15). 앞(+z) = 남쪽, y=0 = 바닥."""
import math

from structures_v3 import Template

LANTERN = {"hanging": "false", "waterlogged": "false"}
LANTERN_H = {"hanging": "true", "waterlogged": "false"}


def stair(block, facing, half="bottom"):
    return block, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}


def connect(t):
    """울타리 · 철창 연결 상태를 이웃에 맞춰 채움 (구조물은 놓일 때 이웃 갱신을 안 함)"""
    sx, sy, sz = t.size
    STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    for (x, y, z), (b, props, nbt) in list(t.blocks.items()):
        if not (b.endswith("_fence") or b == "minecraft:iron_bars"):
            continue
        p = {"waterlogged": "false"}
        for d, (dx, dz) in STEP.items():
            n = t.blocks.get((x + dx, y, z + dz), ("minecraft:air",))[0]
            ok = n.endswith("_fence") or n.endswith("_fence_gate") or n == "minecraft:iron_bars" or (
                n != "minecraft:air" and not any(k in n for k in ("lantern", "carpet", "torch", "flower", "poppy", "slab", "stairs", "water", "grass_block")))
            if n == "minecraft:grass_block" or n.startswith("wildplus:"):
                ok = False
            p[d] = "true" if ok else "false"
        t.blocks[(x, y, z)] = (b, p, nbt)
    return t


def furn(t, x, y, z, name, facing="south", extra=None):
    props = {"facing": facing}
    props.update(extra or {})
    t.set(x, y, z, "wildplus:" + name, props)


# ═══════════ 경마장 ═══════════
CX, CZ = 18, 20
RXO, RZO, RXI, RZI = 11.5, 19.5, 8.0, 16.0


def _ring(x, z):
    ro = ((x + 0.5 - CX - 0.5) / RXO) ** 2 + ((z + 0.5 - CZ - 0.5) / RZO) ** 2
    ri = ((x + 0.5 - CX - 0.5) / RXI) ** 2 + ((z + 0.5 - CZ - 0.5) / RZI) ** 2
    return ro, ri


def racetrack():
    t = Template(37, 9, 41)
    t.fill(0, 0, 0, 36, 0, 40, "minecraft:grass_block", {"snowy": "false"})
    for x in range(37):
        for z in range(41):
            ro, ri = _ring(x, z)
            if ro <= 1 and ri > 1:
                t.set(x, 0, z, "minecraft:coarse_dirt")
            elif 1 < ro <= 1.17 or 0.86 < ri <= 1:
                t.set(x, 1, z, "minecraft:birch_fence")
    # 안쪽 잔디: 꽃밭 · 전광판 · 결승 기둥
    for (x, z) in ((14, 12), (22, 12), (14, 28), (22, 28), (18, 8), (18, 32)):
        t.set(x, 1, z, ["minecraft:poppy", "minecraft:dandelion", "minecraft:allium", "minecraft:oxeye_daisy"][(x + z) % 4])
    furn(t, 18, 1, 20, "scoreboard", "east")
    furn(t, 24, 1, 20, "finish_post", "east")
    # 출발 게이트 (오른쪽 직선, 북쪽으로 달림) — 경주 경로의 기준점
    furn(t, 28, 1, 20, "starting_gate", "north")
    # 관중석 (동쪽): 계단식 좌석 4줄 · 뒤 벽 · 옆 벽 · 지붕 · 등
    for i in range(4):
        x = 31 + i
        for z in range(12, 29):
            t.fill(x, 1, z, x, i + 1, z, "minecraft:stone_bricks")
            b, p = stair("minecraft:red_nether_brick_stairs" if i % 2 == 0 else "minecraft:smooth_quartz_stairs", "east")
            t.set(x, i + 2, z, b, p)
    t.fill(35, 1, 11, 35, 7, 29, "minecraft:stone_bricks")
    for z in (11, 29):
        t.fill(31, 1, z, 35, 6, z, "minecraft:stone_bricks")
    for x in range(30, 36):
        for z in range(11, 30):
            t.set(x, 8, z, "minecraft:red_nether_brick_slab", {"type": "bottom", "waterlogged": "false"})
    for z in (11, 29):
        t.fill(30, 1, z, 30, 7, z, "minecraft:quartz_pillar", {"axis": "y"})
    for z in (14, 20, 26):
        t.set(34, 7, z, "minecraft:lantern", LANTERN_H)
    # 관중석 입구 (남쪽 옆 벽을 터서 계단 좌석으로 걸어 들어감)
    t.fill(31, 1, 29, 34, 4, 29, "minecraft:air")
    # 마권 발매기 2대
    furn(t, 34, 1, 32, "betting_kiosk", "west")
    furn(t, 34, 1, 34, "betting_kiosk", "west")
    # 마구간 4칸 (서쪽) + 건초 · 물
    for k, z in enumerate((14, 18, 22, 26)):
        furn(t, 3, 1, z, "stable", "east")
    for z in (11, 29):
        t.set(2, 1, z, "minecraft:hay_block", {"axis": "y"})
        t.set(3, 1, z, "minecraft:water_cauldron", {"level": "3"})
    # 길 (자갈) · 가로등
    for x in range(1, 36):
        t.set(x, 0, 38, "minecraft:gravel")
    for x in (2, 12, 24, 34):
        t.set(x, 1, 39, "minecraft:birch_fence")
        t.set(x, 2, 39, "minecraft:lantern", LANTERN)
    return connect(t).save("house/racetrack")


# ═══════════ 마을 발전 건물 (완공) ═══════════
def _plate(t, block="minecraft:grass_block"):
    sx, sy, sz = t.size
    t.fill(0, 0, 0, sx - 1, 0, sz - 1, block, {"snowy": "false"} if block == "minecraft:grass_block" else None)


def library():
    t = Template(15, 12, 15)
    _plate(t)
    t.fill(1, 0, 1, 13, 0, 13, "minecraft:polished_andesite")
    t.fill(2, 1, 2, 12, 1, 10, "minecraft:polished_andesite")
    t.fill(2, 2, 2, 12, 6, 10, "minecraft:smooth_quartz")
    t.fill(3, 2, 3, 11, 5, 9, "minecraft:air")
    t.fill(3, 1, 3, 11, 1, 9, "minecraft:dark_oak_planks")
    for x in (2, 12):
        for z in (2, 10):
            t.fill(x, 2, z, x, 6, z, "minecraft:quartz_pillar", {"axis": "y"})
    for x in (4, 6, 8, 10):  # 큰 창
        t.fill(x, 3, 10, x, 4, 10, "minecraft:glass")
        t.fill(x, 3, 2, x, 4, 2, "minecraft:glass")
    for z in (4, 6, 8):
        t.fill(2, 3, z, 2, 4, z, "minecraft:glass")
        t.fill(12, 3, z, 12, 4, z, "minecraft:glass")
    t.set(7, 2, 10, "minecraft:dark_oak_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    t.set(7, 3, 10, "minecraft:dark_oak_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    # 기둥 현관 + 박공
    t.fill(2, 1, 11, 12, 1, 12, "minecraft:smooth_quartz")
    for x in (3, 5, 9, 11):
        t.fill(x, 2, 12, x, 5, 12, "minecraft:quartz_pillar", {"axis": "y"})
    t.fill(2, 6, 11, 12, 6, 12, "minecraft:smooth_quartz")
    for k in range(3):
        b, p = stair("minecraft:quartz_stairs", "east")
        t.set(2 + k, 7 + k, 12, b, p)
        b, p = stair("minecraft:quartz_stairs", "west")
        t.set(12 - k, 7 + k, 12, b, p)
        t.fill(3 + k, 7 + k, 12, 11 - k, 7 + k, 12, "minecraft:smooth_quartz") if 3 + k <= 11 - k else None
    for k in range(3):
        b, p = stair("minecraft:polished_andesite_stairs", "north")
        t.fill(4, 0, 13 + k * 0, 10, 0, 13, "minecraft:polished_andesite") if k == 0 else None
    b, p = stair("minecraft:polished_andesite_stairs", "north")
    t.fill(5, 1, 13, 9, 1, 13, b, p)
    # 지붕 + 초록 구리 돔
    t.fill(2, 7, 2, 12, 7, 10, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
    for y, r in ((7, 3.4), (8, 3.0), (9, 2.2), (10, 1.2)):
        for x in range(15):
            for z in range(15):
                if (x - 7) ** 2 + (z - 6) ** 2 <= r * r:
                    t.set(x, y, z, "minecraft:oxidized_cut_copper")
    t.set(7, 11, 6, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # 안: 책장 벽 · 독서대 · 마법 부여대 · 양탄자 · 등
    for x in range(3, 12):
        t.fill(x, 2, 3, x, 4, 3, "minecraft:bookshelf")
    for z in range(4, 9):
        t.fill(3, 2, z, 3, 3, z, "minecraft:bookshelf")
        t.fill(11, 2, z, 11, 3, z, "minecraft:bookshelf")
    t.set(7, 2, 6, "minecraft:enchanting_table")
    for (x, z) in ((5, 7), (9, 7)):
        t.set(x, 2, z, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    t.fill(5, 2, 8, 9, 2, 9, "minecraft:red_carpet")
    for (x, z) in ((5, 5), (9, 5), (7, 8)):
        t.set(x, 5, z, "minecraft:lantern", LANTERN_H)
    for x in (1, 13):
        t.set(x, 1, 13, "minecraft:birch_fence")
        t.set(x, 2, 13, "minecraft:lantern", LANTERN)
    return t


def amusement():
    t = Template(15, 13, 15)
    _plate(t)
    t.fill(0, 0, 0, 14, 0, 14, "minecraft:smooth_sandstone")
    # 관람차 (뒤쪽, xy 평면 z=3): 다리 · 바퀴 테 (바다 랜턴 전구) · 칸 6개 · 가운데 금
    cx, cy, cz, R = 7, 7, 3, 5
    for k in range(48):
        a = 2 * math.pi * k / 48
        x, y = round(cx + R * math.cos(a)), round(cy + R * math.sin(a))
        t.set(x, y, cz, "minecraft:sea_lantern" if k % 4 == 0 else "minecraft:iron_block")
    for k in range(6):
        a = 2 * math.pi * k / 6
        for s in range(1, R):
            t.set(round(cx + s * math.cos(a)), round(cy + s * math.sin(a)), cz, "minecraft:iron_bars")
        x, y = round(cx + R * math.cos(a)), round(cy + R * math.sin(a))
        c = ["red", "blue", "yellow", "lime", "purple", "orange"][k]
        for dz in (2, 4):
            t.set(x, y - 1, dz, f"minecraft:{c}_terracotta")
        t.set(x, y - 1, cz, f"minecraft:{c}_stained_glass")
    t.set(cx, cy, cz, "minecraft:gold_block")
    for (x0, x1) in ((3, 6), (11, 8)):
        for k in range(cy):
            x = round(x0 + (x1 - x0) * k / cy)
            t.set(x, k + 1, cz, "minecraft:iron_bars")
            t.set(x, k + 1, cz + 1, "minecraft:iron_bars")
    # 회전목마 (앞 오른쪽): 둥근 판 · 기둥 · 말(울타리 + 색 블록) · 줄무늬 지붕
    mx, mz = 10, 10
    for x in range(15):
        for z in range(15):
            d = (x - mx) ** 2 + (z - mz) ** 2
            if d <= 9:
                t.set(x, 1, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
            if d <= 12:
                t.set(x, 5, z, "minecraft:red_terracotta" if (x + z) % 2 else "minecraft:white_terracotta")
            if d <= 4:
                t.set(x, 6, z, "minecraft:red_terracotta" if (x + z) % 2 else "minecraft:white_terracotta")
    t.fill(mx, 1, mz, mx, 6, mz, "minecraft:gold_block")
    for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        t.fill(mx + dx, 2, mz + dz, mx + dx, 4, mz + dz, "minecraft:end_rod", {"facing": "up"})
        t.set(mx + dx, 2, mz + dz, "minecraft:white_glazed_terracotta", {"facing": "south"})
    t.set(mx, 7, mz, "minecraft:lantern", LANTERN)
    # 솜사탕 가게 (앞 왼쪽) + 깃발 줄 + 벤치
    t.fill(1, 1, 9, 4, 2, 11, "minecraft:pink_terracotta")
    t.fill(1, 3, 9, 4, 3, 11, "minecraft:air")
    for (x, z) in ((1, 9), (4, 9), (1, 11), (4, 11)):
        t.set(x, 3, z, "minecraft:birch_fence")
    for x in range(0, 6):
        for z in range(8, 13):
            t.set(x, 4, z, "minecraft:pink_terracotta" if (x + z) % 2 else "minecraft:white_terracotta")
    for x in range(0, 15, 2):
        t.set(x, 1, 14, "minecraft:birch_fence")
        t.set(x, 2, 14, ["minecraft:red_terracotta", "minecraft:yellow_terracotta", "minecraft:blue_terracotta", "minecraft:lime_terracotta"][(x // 2) % 4])
    return t


def harbor():
    t = Template(15, 12, 15)
    _plate(t)
    t.fill(0, 0, 6, 14, 0, 14, "minecraft:water", {"level": "0"})
    t.fill(0, -0, 6, 14, 0, 6, "minecraft:stone_bricks")
    # 부두 (T자)
    t.fill(6, 0, 7, 8, 0, 14, "minecraft:spruce_planks")
    t.fill(2, 0, 11, 12, 0, 12, "minecraft:spruce_planks")
    for (x, z) in ((6, 9), (8, 9), (6, 13), (8, 13), (2, 12), (12, 12)):
        t.set(x, 1, z, "minecraft:spruce_fence")
        t.set(x, 2, z, "minecraft:lantern", LANTERN)
    # 등대 (뒤 왼쪽): 빨강 · 흰 줄 + 꼭대기 바다 랜턴 + 유리
    for y in range(1, 9):
        c = "minecraft:white_terracotta" if (y // 2) % 2 == 0 else "minecraft:red_terracotta"
        t.fill(2, y, 1, 3, y, 2, c)
    t.fill(1, 9, 0, 4, 9, 3, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    t.fill(2, 9, 1, 3, 9, 2, "minecraft:sea_lantern")
    for (x, z) in ((2, 1), (3, 1), (2, 2), (3, 2)):
        t.set(x, 10, z, "minecraft:glass")
    t.fill(2, 11, 1, 3, 11, 2, "minecraft:red_nether_brick_slab", {"type": "bottom", "waterlogged": "false"})
    # 어시장 천막 + 통 · 상자
    t.fill(9, 1, 1, 13, 1, 3, "minecraft:spruce_planks")
    for (x, z) in ((9, 1), (13, 1), (9, 3), (13, 3)):
        t.fill(x, 2, z, x, 3, z, "minecraft:spruce_fence")
    for x in range(9, 14):
        for z in range(1, 4):
            t.set(x, 4, z, "minecraft:blue_terracotta" if x % 2 else "minecraft:white_terracotta")
    for (x, z) in ((10, 2), (12, 2)):
        t.set(x, 2, z, "minecraft:barrel", {"facing": "up", "open": "true"})
    for (x, z) in ((5, 3), (6, 3), (5, 4)):
        t.set(x, 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    t.set(5, 2, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    return t


def clock_tower():
    t = Template(15, 16, 15)
    _plate(t)
    for x in range(1, 14):
        for z in range(1, 14):
            t.set(x, 0, z, "minecraft:stone_bricks" if (x + z) % 2 else "minecraft:polished_andesite")
    t.fill(6, 1, 6, 8, 9, 8, "minecraft:bricks")
    t.fill(6, 1, 6, 8, 1, 8, "minecraft:stone_bricks")
    t.fill(5, 10, 5, 9, 10, 9, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    t.fill(6, 10, 6, 8, 12, 8, "minecraft:bricks")
    for (x, z, f) in ((7, 9, "south"), (7, 5, "north"), (9, 7, "east"), (5, 7, "west")):
        t.set(x, 11, z, "wildplus:clock_face", {"facing": f})
    for k, r in enumerate((2, 1, 0)):
        for x in range(7 - r - 1, 7 + r + 2):
            for z in range(7 - r - 1, 7 + r + 2):
                t.set(x, 13 + k, z, "minecraft:blue_terracotta")
    t.set(7, 15, 7, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # 가로등 · 벤치 · 화단
    for (x, z) in ((2, 2), (12, 2), (2, 12), (12, 12)):
        t.fill(x, 1, z, x, 2, z, "minecraft:dark_oak_fence")
        t.set(x, 3, z, "minecraft:lantern", LANTERN)
    for (x, z, f) in ((3, 10, "north"), (4, 10, "north"), (10, 10, "north"), (11, 10, "north")):
        b, p = stair("minecraft:spruce_stairs", f)
        t.set(x, 1, z, b, p)
    for (x, z) in ((3, 4), (4, 4), (10, 4), (11, 4)):
        t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
        t.set(x, 1, z, ["minecraft:poppy", "minecraft:dandelion", "minecraft:allium", "minecraft:cornflower"][(x + z) % 4])
    return t


def hot_spring():
    t = Template(15, 9, 15)
    _plate(t)
    t.fill(1, 0, 4, 9, 0, 11, "minecraft:stone")
    t.fill(2, 0, 5, 8, 0, 10, "minecraft:water", {"level": "0"})
    for (x, z) in ((1, 4), (9, 4), (1, 11), (9, 11), (5, 4), (5, 11)):
        t.set(x, 1, z, "minecraft:mossy_cobblestone")
    # 정자 (뒤 오른쪽)
    for (x, z) in ((10, 1), (13, 1), (10, 4), (13, 4)):
        t.fill(x, 1, z, x, 3, z, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    t.fill(10, 0, 1, 13, 0, 4, "minecraft:dark_oak_planks")
    for x in range(9, 15):
        b, p = stair("minecraft:deepslate_tile_stairs", "south")
        t.set(x, 4, 5, b, p)
        b, p = stair("minecraft:deepslate_tile_stairs", "north")
        t.set(x, 4, 0, b, p)
        t.fill(x, 4, 1, x, 4, 4, "minecraft:deepslate_tiles")
    t.fill(10, 5, 1, 13, 5, 4, "minecraft:deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})
    # 대나무 담 (뒤 · 왼쪽) · 석등 · 수건 바구니
    for x in range(0, 15):
        t.set(x, 1, 0, "minecraft:bamboo_fence") if x < 9 else None
    for z in range(0, 15):
        t.set(0, 1, z, "minecraft:bamboo_fence")
    for (x, z) in ((3, 2), (7, 2)):
        t.set(x, 1, z, "minecraft:stone_brick_wall", {"east": "none", "west": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false"})
        t.set(x, 2, z, "minecraft:lantern", LANTERN)
    t.set(11, 1, 7, "minecraft:barrel", {"facing": "up", "open": "true"})
    b, p = stair("minecraft:dark_oak_stairs", "west")
    t.set(11, 1, 9, b, p)
    t.set(11, 1, 10, b, p)
    return t


BUILDINGS = {"library": library, "amusement": amusement, "harbor": harbor, "clock_tower": clock_tower, "hot_spring": hot_spring}


def stage(fn, n):
    """1 = 터 닦기 (흙 · 말뚝 · 줄 대신 울타리 · 공사 표지), 2 = 골조 (아래 절반 + 비계), 3 = 완공"""
    full = fn()
    if n == 3:
        return full
    sx, sy, sz = full.size
    t = Template(sx, sy, sz)
    _plate(t)
    if n == 1:
        t.fill(1, 0, 1, sx - 2, 0, sz - 2, "minecraft:coarse_dirt")
        for x in range(1, sx - 1, 3):
            for z in (1, sz - 2):
                t.set(x, 1, z, "minecraft:oak_fence")
        for z in range(1, sz - 1, 3):
            for x in (1, sx - 2):
                t.set(x, 1, z, "minecraft:oak_fence")
        t.set(sx // 2, 1, sz - 1, "minecraft:oak_fence")
        t.set(sx // 2, 2, sz - 1, "minecraft:yellow_terracotta")
        t.set(sx // 2 - 2, 1, sz // 2, "minecraft:gravel")
        t.set(sx // 2 + 2, 1, sz // 2, "minecraft:cobblestone")
        t.set(sx // 2, 1, sz // 2, "minecraft:crafting_table")
        return connect(t)
    top = max(y for (x, y, z), (b, p, nb) in full.blocks.items() if b != "minecraft:air")
    cut = max(2, int(top * 0.45))
    for (x, y, z), v in full.blocks.items():
        if y <= cut:
            t.blocks[(x, y, z)] = v
    for (x, z) in ((0, 0), (sx - 1, 0), (0, sz - 1), (sx - 1, sz - 1), (sx // 2, 0), (sx // 2, sz - 1)):
        for y in range(1, min(sy, cut + 4)):
            t.set(x, y, z, "minecraft:scaffolding", {"bottom": "false", "distance": "0", "waterlogged": "false"})
    return t


def build():
    from structures_v3 import OUT
    (OUT / "village").mkdir(parents=True, exist_ok=True)
    (OUT / "house").mkdir(parents=True, exist_ok=True)
    racetrack()
    for k, fn in BUILDINGS.items():
        for n in (1, 2, 3):
            t = stage(fn, n)
            if n == 3:
                connect(t)
            t.save(f"village/{k}_{n}")
    return True
