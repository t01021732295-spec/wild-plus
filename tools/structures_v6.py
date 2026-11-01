"""12차 구조물: 대형 던전 "고대 지하 묘지" (64×30×64).

땅 위에 무너진 입구(돌기둥·석상)만 보이고, 계단으로 내려가면 2층짜리 미궁이 나온다.
1층(지하): 둘레 복도 + 십자 복도, 스포너 방 · 도서관 · 감옥 · 지하 납골당 · 화살 함정 복도 · 중앙 홀(아래층 계단)
2층(더 깊이): 대기실 → 거미굴·해골 방(스포너) → 보스 투기장(고대 수호자) → 보물 창고
템플릿 y 0~21 은 땅속(벽돌로 채운 뒤 방을 판다), y 22 이 지표면, 그 위는 입구만 놓고 나머지는 structure_void.
"""
import random

from nbtlib import Byte, Compound, Int, List, String

from structures_v3 import Template, chest, mob

SX, SY, SZ = 64, 30, 64
GROUND = 22
L1, L2 = 11, 1  # 각 층 바닥 높이

rnd = random.Random(33)


def brick():
    r = rnd.random()
    return ("minecraft:cracked_deepslate_bricks" if r < 0.18 else "minecraft:deepslate_tiles" if r < 0.3 else
            "minecraft:cobbled_deepslate" if r < 0.36 else "minecraft:deepslate_bricks")


def solid(t, x1, y1, z1, x2, y2, z2):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                t.set(x, y, z, brick())


def carve(t, x1, y1, z1, x2, y2, z2):
    t.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")


def floor(t, x1, y, z1, x2, z2, block="minecraft:polished_deepslate"):
    t.fill(x1, y - 1, z1, x2, y - 1, z2, block)


def room(t, x1, z1, x2, z2, y, h=5, fl="minecraft:polished_deepslate"):
    carve(t, x1, y, z1, x2, y + h - 1, z2)
    floor(t, x1, y, z1, x2, z2, fl)


def spawner(t, x, y, z, entity):
    t.set(x, y, z, "minecraft:spawner", None, Compound({
        "id": String("minecraft:mob_spawner"),
        "SpawnData": Compound({"entity": Compound({"id": String(entity)})}),
        "Delay": Int(20), "MinSpawnDelay": Int(200), "MaxSpawnDelay": Int(600), "SpawnCount": Int(3),
        "MaxNearbyEntities": Int(6), "RequiredPlayerRange": Int(16), "SpawnRange": Int(4)}))


def lantern(t, x, y, z):
    t.set(x, y, z, "minecraft:soul_lantern", {"hanging": "true", "waterlogged": "false"})


def boss():
    return mob("minecraft:wither_skeleton", 180.0, {"NeoForgeData": Compound({"wildplus_dungeon_boss": Byte(1)})})


def dungeon():
    t = Template(SX, SY, SZ)
    # 위쪽(땅 위)은 지형을 건드리지 않게 비워 둔다
    for x in range(SX):
        for y in range(GROUND, SY):
            for z in range(SZ):
                t.set(x, y, z, "minecraft:structure_void")
    solid(t, 0, 0, 0, SX - 1, GROUND - 1, SZ - 1)

    # ───────── 땅 위 입구: 무너진 신전 ─────────
    t.fill(26, GROUND - 1, 8, 38, GROUND - 1, 18, "minecraft:cracked_stone_bricks")
    for (x, z, hgt) in [(26, 8, 5), (38, 8, 3), (26, 18, 4), (38, 18, 6), (32, 8, 2)]:
        for y in range(GROUND, GROUND + hgt):
            t.set(x, y, z, "minecraft:mossy_stone_bricks" if y % 2 else "minecraft:stone_bricks")
        t.set(x, GROUND + hgt, z, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    t.fill(27, GROUND + 5, 8, 30, GROUND + 5, 8, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    for (x, z) in [(29, 10), (35, 10)]:  # 석상
        t.set(x, GROUND, z, "minecraft:chiseled_stone_bricks")
        t.set(x, GROUND + 1, z, "minecraft:skeleton_skull", {"rotation": "8", "powered": "false"})
    t.set(32, GROUND, 9, "minecraft:soul_campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for z in range(10, 19):
        for x in (30, 34):
            t.set(x, GROUND, z, "minecraft:mossy_cobblestone_wall", {"east": "none", "west": "none", "north": "low",
                                                                      "south": "low", "up": "true", "waterlogged": "false"})

    # ───────── 1층 ─────────
    # 둘레 복도 + 십자 복도 (폭 3, 높이 4)
    for (x1, z1, x2, z2) in [(8, 8, 55, 10), (8, 53, 55, 55), (8, 8, 10, 55), (53, 8, 55, 55), (31, 8, 33, 55), (8, 31, 55, 33)]:
        room(t, x1, z1, x2, z2, L1, 4, "minecraft:deepslate_tiles")
    for (x, z) in [(9, 9), (32, 9), (54, 9), (9, 32), (54, 32), (9, 54), (32, 54), (54, 54), (20, 32), (44, 32), (32, 20), (32, 44)]:
        lantern(t, x, L1 + 3, z)
    # ───────── 내려가는 계단 (지표 → 1층) ─────────
    for i in range(GROUND - L1):
        y = GROUND - 1 - i
        z = 11 + i
        carve(t, 31, y, z, 33, y + 4, z)
        for x in range(31, 34):
            t.set(x, y, z, "minecraft:deepslate_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                                                  "waterlogged": "false"})
        if i % 3 == 0:
            lantern(t, 32, y + 4, z)
    stair_end = 11 + (GROUND - L1)  # 1층에 닿는 z

    # 계단 끝 ↔ 복도 (계단 마지막 칸은 남겨 둔다)
    carve(t, 31, L1, stair_end, 33, L1 + 3, stair_end + 1)
    carve(t, 31, L1 + 1, stair_end - 1, 33, L1 + 3, stair_end - 1)

    # 북서: 좀비 스포너 방
    room(t, 13, 13, 25, 25, L1, 6, "minecraft:mossy_cobblestone")
    carve(t, 18, L1, 11, 20, L1 + 2, 12)
    spawner(t, 19, L1, 19, "minecraft:zombie")
    t.set(14, L1, 14, "minecraft:barrel", {"facing": "up", "open": "false"})
    t.set(24, L1, 24, "minecraft:barrel", {"facing": "up", "open": "false"})
    for (x, z) in [(13, 13), (25, 13), (13, 25), (25, 25)]:
        t.fill(x, L1, z, x, L1 + 5, z, "minecraft:chiseled_deepslate")
    lantern(t, 19, L1 + 5, 15)

    # 북동: 도서관
    room(t, 38, 13, 50, 25, L1, 6, "minecraft:dark_oak_planks")
    carve(t, 43, L1, 11, 45, L1 + 2, 12)
    for x in range(38, 51):
        for y in range(L1, L1 + 4):
            if x not in (43, 44, 45) or y > L1 + 2:
                t.set(x, y, 25, "minecraft:bookshelf")
    for z in range(14, 25):
        for y in range(L1, L1 + 4):
            t.set(50, y, z, "minecraft:bookshelf")
            t.set(38, y, z, "minecraft:bookshelf")
    t.set(44, L1, 19, "minecraft:enchanting_table")
    t.set(41, L1, 15, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    t.set(48, L1, 15, "minecraft:bookshelf")
    t.set(40, L1, 22, "minecraft:cobweb")
    t.set(47, L1 + 4, 21, "minecraft:cobweb")
    lantern(t, 44, L1 + 5, 19)
    t.entity(44.5, L1, 22.5, mob("minecraft:skeleton", 0.0))

    # 남서: 감옥 (철창 칸 3개, 안에 좀비)
    room(t, 13, 38, 25, 50, L1, 5, "minecraft:cobblestone")
    carve(t, 18, L1, 51, 20, L1 + 2, 52)
    for cx in (13, 17, 21):
        for y in range(L1, L1 + 3):
            t.set(cx + 3, y, 39, "minecraft:cobbled_deepslate")
            for x in range(cx, cx + 3):
                t.set(x, y, 42, "minecraft:iron_bars", {"east": "true", "west": "true", "north": "false", "south": "false",
                                                       "waterlogged": "false"})
        t.entity(cx + 1.5, L1, 40.5, mob("minecraft:zombie", 180.0))
        t.set(cx, L1, 39, "minecraft:hay_block", {"axis": "y"})
    spawner(t, 19, L1, 46, "minecraft:skeleton")
    t.set(14, L1, 49, "minecraft:barrel", {"facing": "up", "open": "false"})
    lantern(t, 19, L1 + 4, 46)

    # 남동: 지하 납골당 (석관)
    room(t, 38, 38, 50, 50, L1, 6, "minecraft:polished_blackstone_bricks")
    carve(t, 43, L1, 51, 45, L1 + 2, 52)
    for (x, z) in [(40, 40), (40, 46), (47, 40), (47, 46)]:
        t.fill(x, L1, z, x + 1, L1, z + 2, "minecraft:polished_deepslate")
        t.set(x, L1 + 1, z + 1, "minecraft:skeleton_skull", {"rotation": "0", "powered": "false"})
    t.set(44, L1, 44, "minecraft:soul_lantern", {"hanging": "false", "waterlogged": "false"})
    t.set(41, L1, 49, "minecraft:barrel", {"facing": "up", "open": "false"})
    spawner(t, 44, L1, 40, "minecraft:skeleton")
    lantern(t, 44, L1 + 5, 44)
    t.entity(45.5, L1, 47.5, mob("minecraft:skeleton", 90.0))

    # 동쪽 십자 복도: 화살 함정 (발판을 밟으면 벽의 발사기가 쏜다)
    for x in (40, 44, 48):
        t.set(x, L1, 30, "minecraft:dispenser", {"facing": "south", "triggered": "false"}, Compound({
            "id": String("minecraft:dispenser"),
            "Items": List[Compound]([Compound({"Slot": Byte(0), "id": String("minecraft:arrow"), "count": Int(32)})])}))
        t.set(x, L1, 31, "minecraft:stone_pressure_plate", {"powered": "false"})
    # 1층 몹
    for (x, z, m) in [(20.5, 9.5, "minecraft:zombie"), (9.5, 20.5, "minecraft:skeleton"), (54.5, 44.5, "minecraft:zombie"),
                      (44.5, 54.5, "minecraft:skeleton"), (24.5, 32.5, "minecraft:spider")]:
        t.entity(x, L1, z, mob(m, rnd.random() * 360))

    # 중앙 홀 + 2층으로 내려가는 계단
    room(t, 26, 26, 38, 38, L1, 6, "minecraft:polished_deepslate")
    for (x, z) in [(26, 26), (38, 26), (26, 38), (38, 38)]:
        t.fill(x, L1, z, x, L1 + 5, z, "minecraft:deepslate_tile_wall")
    for i in range(L1 - L2):
        y = L1 - 1 - i
        z = 28 + i
        carve(t, 31, y, z, 33, y + 4, z)
        for x in range(31, 34):
            t.set(x, y, z, "minecraft:deepslate_tile_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                                                "waterlogged": "false"})
    lantern(t, 32, L1 + 5, 32)

    # ───────── 2층 ─────────
    # 대기실 (계단 착지)
    room(t, 26, 37, 38, 43, L2, 5)
    for x in range(31, 34):
        t.set(x, L2, 28 + (L1 - L2) - 1, "minecraft:deepslate_tile_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                                                             "waterlogged": "false"})
    # 거미굴(서) · 해골 방(동)
    room(t, 4, 30, 16, 42, L2, 5, "minecraft:coarse_dirt")
    room(t, 48, 30, 60, 42, L2, 5, "minecraft:bone_block")
    carve(t, 17, L2, 35, 25, L2 + 3, 37)
    carve(t, 39, L2, 35, 47, L2 + 3, 37)
    floor(t, 17, L2, 35, 25, 37)
    floor(t, 39, L2, 35, 47, 37)
    spawner(t, 10, L2, 36, "minecraft:cave_spider")
    for (x, y, z) in [(5, L2 + 3, 31), (6, L2 + 4, 41), (14, L2 + 3, 40), (12, L2, 32), (8, L2 + 4, 35)]:
        t.set(x, y, z, "minecraft:cobweb")
    t.set(5, L2, 41, "minecraft:cobweb")
    spawner(t, 54, L2, 36, "minecraft:skeleton")
    t.set(59, L2, 41, "minecraft:bone_block", {"axis": "y"})
    for (x, z) in [(49, 31), (59, 31), (49, 41)]:
        t.set(x, L2, z, "minecraft:skeleton_skull", {"rotation": str(rnd.randrange(16)), "powered": "false"})
    lantern(t, 10, L2 + 4, 36)
    lantern(t, 54, L2 + 4, 36)
    # 보스 투기장 (32×14, 높이 7) — 용암 도랑 + 기둥
    room(t, 16, 45, 47, 57, L2, 7, "minecraft:polished_blackstone_bricks")
    carve(t, 30, L2, 44, 34, L2 + 3, 44)
    for (x, z) in [(20, 48), (43, 48), (20, 54), (43, 54)]:
        t.fill(x, L2, z, x + 1, L2 + 6, z + 1, "minecraft:polished_blackstone")
        lantern(t, x, L2 + 6, z - 1)
    for x in range(17, 47):
        t.set(x, L2 - 1, 46, "minecraft:magma_block")
        t.set(x, L2 - 1, 56, "minecraft:magma_block")
    t.entity(32.0, L2, 51.5, boss())
    for (x, z) in [(24.5, 50.5), (40.5, 50.5)]:
        t.entity(x, L2, z, mob("minecraft:wither_skeleton", 180.0))
    # 보물 창고 (투기장 뒤) — 고대 수호자를 쓰러뜨려야 봉인문이 열린다. 벽·천장·바닥은 부술 수 없는 봉인 벽돌
    room(t, 24, 59, 40, 62, L2, 4, "minecraft:gold_block")
    for x in range(23, 42):
        for y in range(L2 - 1, L2 + 5):
            for z in range(58, 64):
                inside = 24 <= x <= 40 and 59 <= z <= 62 and L2 <= y <= L2 + 3
                if not inside:
                    t.set(x, y, z, "wildplus:dungeon_seal", {"gate": "false"})
    t.fill(31, L2, 58, 33, L2 + 2, 58, "wildplus:dungeon_seal", {"gate": "true"})
    for x in (25, 28, 36, 39):
        t.set(x, L2, 62, *chest("wildplus:chests/dungeon_vault", "north"))
    for (x, z, f) in ((24, 60, "east"), (40, 60, "west"), (24, 61, "east"), (40, 61, "west")):
        t.set(x, L2, z, *chest("wildplus:chests/dungeon", f))
    for x in (26, 27, 37, 38):
        t.set(x, L2, 62, "minecraft:gold_block")
    t.set(32, L2, 62, "minecraft:diamond_block")
    t.set(31, L2, 62, "minecraft:emerald_block")
    t.set(33, L2, 62, "minecraft:emerald_block")
    lantern(t, 32, L2 + 3, 60)
    return t.save("dungeon")


if __name__ == "__main__":
    print("dungeon", dungeon())
