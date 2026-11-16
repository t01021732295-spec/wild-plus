"""5.3 고대 도시(딥다크) 구조물 3종. 도안: ancient_designs_v53.spire / archive / sanctum.
· 메아리 첨탑 (17×34×17): 천장 암반에 매달린 5층 탑, 층마다 몬스터 · 맨 아래 메아리 수정과 상자
· 봉인된 서고 (21×9×17): 공명 종 4개 + 악보 석판 · 스컬크 봉인 문 · 안쪽 금고 상자 (event.AncientArchive)
· 잠든 성소 (41×20×41): 원형 대성당 · 기둥 8개 · 양털 바닥 · 가운데 성소 제단(메아리 조각 4개 → 고대의 워든)"""
import math
import random

from structures_v3 import Template, chest, mob

DS, DSB, DST, POL, CHIS = "minecraft:deepslate", "minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:polished_deepslate", "minecraft:chiseled_deepslate"
SCULK, VEIN, SENSOR, CAT, SOUL = "minecraft:sculk", "minecraft:sculk_vein", "minecraft:sculk_sensor", "minecraft:sculk_catalyst", "minecraft:soul_lantern"
WOOL, CANDLE, AMETH, BOOK = "minecraft:gray_wool", "minecraft:light_blue_candle", "minecraft:amethyst_block", "minecraft:bookshelf"
BELL, SEAL, TABLET, ALTAR = "wildplus:ancient_bell", "wildplus:ancient_seal", "wildplus:score_tablet", "wildplus:sanctum_altar"


def rough(r):
    q = r.random()
    return SCULK if q < .18 else DST if q < .45 else DSB if q < .8 else POL


def spire():
    X, Y, Z = 17, 34, 17
    t = Template(X, Y, Z)
    r = random.Random(53)
    c = X // 2
    for x in range(X):                                                   # 천장 암반 (맨 위 3줄)
        for z in range(Z):
            for y in range(Y - 3, Y):
                t.set(x, y, z, SCULK if r.random() < .25 else DS)
    tiers = [(13, 6), (11, 6), (9, 6), (7, 5), (5, 4)]
    top = Y - 3
    for i, (w, h) in enumerate(tiers):
        y0 = top - h
        h0 = w // 2
        for x in range(c - h0, c + h0 + 1):
            for z in range(c - h0, c + h0 + 1):
                edge = x in (c - h0, c + h0) or z in (c - h0, c + h0)
                for y in range(y0, top):
                    if edge or y == y0:
                        t.set(x, y, z, rough(r))
                    else:
                        t.set(x, y, z, "minecraft:air")
                if edge and (x + z) % 4 == 0:
                    t.set(x, y0 + h // 2, z, "minecraft:tinted_glass")
        for x in range(c - h0 - 1, c + h0 + 2):                          # 층마다 스컬크 띠
            for z in (c - h0 - 1, c + h0 + 1):
                t.set(x, y0, z, SCULK)
                t.set(z, y0, x, SCULK)
        t.set(c, top - 1, c, SOUL, {"hanging": "true", "waterlogged": "false"})
        if i < len(tiers) - 1:                                           # 아래층으로 내려가는 구멍 + 사다리 대신 덩굴
            t.set(c, y0, c, "minecraft:air")
            for y in range(y0 - 1, y0 + 1):
                t.set(c + 1, y, c, "minecraft:air")
        mobid = ["sculk_stalker", "silent_priest", "sculk_stalker", "sculk_golem", "sculk_stalker"][i]
        t.entity(c + 1.5, y0 + 1.0, c - 1.5, mob(f"wildplus:{mobid}", 0.0))
        top = y0
    # 맨 아래 끝: 메아리 수정 + 금고 상자
    t.set(c, top + 1, c + 1, *chest("wildplus:chests/echo_spire", "north"))
    for y in range(top - 4, top):
        t.set(c, y, c, AMETH if y > top - 3 else "minecraft:amethyst_cluster", {"facing": "down", "waterlogged": "false"} if y == top - 4 else None)
    for x, z in ((2, 3), (14, 5), (4, 13), (12, 12)):                   # 늘어진 촉수
        for y in range(Y - 14, Y - 3):
            t.set(x, y, z, VEIN if y % 3 else SCULK, {"down": "true", "up": "false", "north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"} if y % 3 else None)
    return t


def archive():
    X, Y, Z = 21, 9, 17
    t = Template(X, Y, Z)
    r = random.Random(530)
    for x in range(X):
        for z in range(Z):
            t.set(x, 0, z, DST if (x + z) % 3 else POL)
            t.set(x, Y - 1, z, DSB)
            if x in (0, X - 1) or z in (0, Z - 1):
                for y in range(1, Y - 1):
                    t.set(x, y, z, rough(r) if y % 3 else CHIS)
    for x in range(2, X - 2):                                            # 책장 (뒷벽)
        if x % 4 != 0:
            for y in range(1, 5):
                t.set(x, y, Z - 2, BOOK)
    bells = [(4, 6), (8, 9), (12, 9), (16, 6)]
    for i, (x, z) in enumerate(bells):
        t.set(x, 1, z, POL)
        t.set(x, 2, z, BELL, {"note": str(i)})
        t.set(x, 3, z, CANDLE, {"candles": "1", "lit": "true", "waterlogged": "false"})
    t.set(10, 1, 4, CHIS)
    t.set(10, 2, 4, TABLET)
    for z in (7, 8, 9):                                                  # 옆문 (서쪽 · 동쪽)
        for y in (1, 2):
            t.set(0, y, z, "minecraft:air")
            t.set(X - 1, y, z, "minecraft:air")
    # 금고 방 (앞벽 안쪽 z=1~2) — 봉인 벽(z=3)이 퍼즐을 풀면 녹음
    for x in range(8, 13):
        for y in range(1, 5):
            t.set(x, y, 1, "minecraft:air")
    t.set(10, 1, 2, *chest("wildplus:chests/sealed_archive", "north"))
    t.set(8, 1, 2, SOUL, {"hanging": "false", "waterlogged": "false"})
    t.set(12, 1, 2, SOUL, {"hanging": "false", "waterlogged": "false"})
    # 금고 칸과 서고 사이를 봉인 벽으로 막음 (z=3)
    for x in range(8, 13):
        for y in range(1, 5):
            t.set(x, y, 3, SEAL)
    for x in (7, 13):
        for y in range(1, 5):
            for z in (1, 2, 3):
                t.set(x, y, z, CHIS)
    t.set(10, 1, 13, SENSOR, {"sculk_sensor_phase": "inactive", "power": "0", "waterlogged": "false"})
    t.entity(5.5, 1.0, 12.5, mob("wildplus:silent_priest", 180.0))
    t.entity(15.5, 1.0, 12.5, mob("wildplus:sculk_stalker", 180.0))
    return t


def sanctum():
    X, Y, Z = 41, 20, 41
    t = Template(X, Y, Z)
    r = random.Random(5300)
    c = X // 2
    for x in range(X):
        for z in range(Z):
            d = math.hypot(x - c, z - c)
            if d > 20.5:
                continue
            t.set(x, 0, z, WOOL if (int(d) % 5 == 2 and r.random() < .85) else DST if (x + z) % 2 else POL)
            if d > 18.5:                                                 # 벽
                for y in range(1, Y - 2):
                    t.set(x, y, z, rough(r) if y % 4 else CHIS)
            t.set(x, Y - 2, z, DS if r.random() < .7 else SCULK)         # 천장
    for k in range(8):                                                   # 기둥 (숨는 곳)
        a = k / 8 * math.tau
        px, pz = c + round(math.cos(a) * 12), c + round(math.sin(a) * 12)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in range(1, Y - 2):
                    t.set(px + dx, y, pz + dz, DSB if y % 5 else CHIS)
        t.set(px, 1, pz + 2, CANDLE, {"candles": "3", "lit": "true", "waterlogged": "false"})
    for dx in range(-3, 4):                                              # 제단
        for dz in range(-3, 4):
            t.set(c + dx, 1, c + dz, POL if max(abs(dx), abs(dz)) == 3 else SCULK)
    t.set(c, 2, c, ALTAR)
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        t.set(c + dx, 2, c + dz, CAT)
    t.set(c, 1, c + 17, *chest("wildplus:chests/sleeping_sanctum", "north"))
    for x, z in ((c - 8, c + 8), (c + 8, c - 8)):
        t.entity(x + .5, 1.0, z + .5, mob("wildplus:sculk_stalker", 0.0))
    t.entity(c + .5, 1.0, c - 9.5, mob("wildplus:silent_priest", 0.0))
    return t


def build():
    return [spire().save("echo_spire"), archive().save("sealed_archive"), sanctum().save("sleeping_sanctum")]


if __name__ == "__main__":
    print(build())
