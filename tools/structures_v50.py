"""5.0 네더 구조물: 겁화의 성채(47×46×55) · 잿불 감시탑(11×34×11). 도안: nether_designs_v50.citadel / watchtower.
성채: 용암 호수(템플릿 y=1 → 세계 y=31) 위 기단 · 모서리 탑 4개(꼭대기 화로) · 성벽 · 남쪽 문과 다리 · 가운데 본성(보스 홀, 문은 kalpa_gate) · 굴뚝.
화로 4개에 불을 붙이면(event.KalpaCitadel) 본성 문이 열리고 제단을 쓸 수 있다."""
import random
import sys

from PIL import Image

from structures_v3 import Template, chest, mob

BRICK, STONE, BAS, PBAS = "minecraft:polished_blackstone_bricks", "minecraft:blackstone", "minecraft:basalt", "minecraft:polished_basalt"
NB, RNB, MAG, GILD, LIGHT, LAVA = "minecraft:nether_bricks", "minecraft:red_nether_bricks", "minecraft:magma_block", "minecraft:gilded_blackstone", "minecraft:shroomlight", "minecraft:lava"
GATE, ALTAR, BRAZIER = "wildplus:kalpa_gate", "wildplus:kalpa_altar", "wildplus:kalpa_brazier"
F = 3  # 바닥 높이


def box(t, x0, x1, y0, y1, z0, z1, block, hollow=False, props=None):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                if hollow and x0 < x < x1 and z0 < z < z1 and y0 < y < y1:
                    t.set(x, y, z, "minecraft:air")
                else:
                    t.set(x, y, z, block, props)


def wall_mix(t, x0, x1, y0, y1, z0, z1, r, hollow=True):
    """벽: 검은 벽돌 + 띠(붉은 네더 벽돌) + 가끔 금박 · 빛"""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                inside = x0 < x < x1 and z0 < z < z1
                if hollow and inside:
                    t.set(x, y, z, "minecraft:air")
                    continue
                q = r.random()
                b = RNB if (y - F) % 5 == 0 else GILD if q < .04 else STONE if q < .16 else BRICK
                t.set(x, y, z, b)


def citadel():
    X, Y, Z = 47, 46, 55
    t = Template(X, Y, Z)
    r = random.Random(50)
    for x in range(X):
        for z in range(Z):
            t.set(x, 0, z, BAS)
            t.set(x, 1, z, LAVA, {"level": "0"})
    box(t, 3, 43, 1, F, 3, 43, BRICK)                                   # 기단
    for x in range(3, 44):
        for z in range(3, 44):
            if (x + z) % 7 == 0:
                t.set(x, F, z, PBAS)
    # 성벽 (두께 2, 높이 8) + 톱니
    for x in range(3, 44):
        for z in range(3, 44):
            if x in (3, 4, 42, 43) or z in (3, 4, 42, 43):
                for y in range(F + 1, F + 9):
                    t.set(x, y, z, RNB if (y - F) % 5 == 0 else BRICK)
                if (x + z) % 2 == 0 and (x in (3, 43) or z in (3, 43)):
                    t.set(x, F + 9, z, BRICK)
    for x in range(21, 26):                                             # 남쪽 성문 (열려 있음)
        for z in (42, 43):
            for y in range(F + 1, F + 6):
                t.set(x, y, z, "minecraft:air")
    for x in (20, 26):
        for y in range(F + 1, F + 10):
            t.set(x, y, 44, GILD if y == F + 9 else BRICK)
        t.set(x, F + 10, 44, LIGHT)
    # 다리
    for z in range(44, Z):
        for x in range(21, 26):
            t.set(x, F, z, BRICK if (x + z) % 5 else PBAS)
            t.set(x, 1, z, BRICK)
            t.set(x, 2, z, BRICK)
        if z % 3 == 0:
            for x in (21, 25):
                t.set(x, F + 1, z, STONE)
                t.set(x, F + 2, z, MAG if z % 6 == 0 else STONE)
    # 모서리 탑 4개: 속이 빈 7×7, 안쪽 사다리, 꼭대기에 화로와 상자
    for k, (x0, z0) in enumerate(((1, 1), (39, 1), (1, 39), (39, 39))):
        x1, z1, top = x0 + 6, z0 + 6, F + 16
        box(t, x0, x1, 1, F, z0, z1, BRICK)
        wall_mix(t, x0, x1, F + 1, top, z0, z1, r)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                t.set(x, top, z, BRICK)
                edge = x in (x0, x1) or z in (z0, z1)
                if edge and (x + z) % 2 == 0:
                    t.set(x, top + 1, z, BRICK)
        west, north = x0 < 20, z0 < 20
        dx = x1 if west else x0                                          # 안뜰 쪽 문
        dz = z0 + 4 if north else z0 + 2
        for y in (F + 1, F + 2):
            t.set(dx, y, dz, "minecraft:air")
        lx = x0 + 1 if west else x1 - 1                                  # 사다리 (바깥 벽에 붙음)
        lz = z0 + 3
        for y in range(F + 1, top + 1):
            t.set(lx, y, lz, "minecraft:ladder", {"facing": "east" if west else "west", "waterlogged": "false"})
        t.set(x0 + 3, top + 1, z0 + 3, BRAZIER, {"lit": "false"})
        t.set(x0 + 3, top + 1, z0 + (5 if north else 1), *chest("wildplus:chests/kalpa_tower", "south" if north else "north"))
        t.set(x0 + 3, F + 8, z0 + 3, LIGHT)
        t.entity(x0 + 2.5, top + 1.0, z0 + 2.5, mob("wildplus:ash_shaman" if k % 2 == 0 else "wildplus:ember_hound", 0.0))
        t.entity(x0 + 3.5, F + 1.0, z0 + 3.5, mob("wildplus:ember_hound", 0.0))
    # 본성 (보스 홀): 바깥 25×25, 안 23×23 × 높이 14
    K0, K1, H = 11, 35, F + 15
    wall_mix(t, K0, K1, F + 1, H, K0, K1, r)
    box(t, K0, K1, H, H, K0, K1, BRICK)
    for x in range(K0 + 1, K1):
        for z in range(K0 + 1, K1):
            d = max(abs(x - 23), abs(z - 23))
            t.set(x, F, z, GILD if d in (3, 8) and (x + z) % 2 == 0 else PBAS if d < 3 else BRICK)
            if (x - K0) % 6 == 3 and (z - K0) % 6 == 3:
                t.set(x, H, z, LIGHT)
    for px, pz in ((16, 16), (29, 16), (16, 29), (29, 29)):              # 기둥 (낙하 때 숨는 곳)
        box(t, px, px + 1, F + 1, H - 1, pz, pz + 1, PBAS)
        t.set(px, F + 4, pz, LIGHT)
    t.set(23, F, 23, ALTAR)
    for x in range(22, 25):                                             # 본성 문 (화로 4개를 켜면 열림)
        for y in range(F + 1, F + 5):
            t.set(x, y, K1, GATE)
    for x in (21, 25):
        for y in range(F + 1, F + 6):
            t.set(x, y, K1 + 1, GILD if y == F + 5 else PBAS)
    for x in (K0 + 4, K0 + 12, K0 + 20):                                # 북쪽 벽 앞 보물 상자
        t.set(x, F + 1, K0 + 1, *chest("wildplus:chests/kalpa_vault", "south"))
    for x in (K0, K1):                                                  # 바깥벽 마그마 줄기
        for z in (17, 23, 29):
            for y in range(F + 1, H):
                t.set(x, y, z, MAG)
    # 지붕: 3단 + 굴뚝 (속이 빈 껍질), 꼭대기에 용암 못
    for (a, b, y0, y1) in ((13, 33, H + 1, H + 4), (16, 30, H + 5, H + 9), (19, 27, H + 10, H + 15)):
        box(t, a, b, y0, y1, a, b, BRICK, hollow=True)
        for x in range(a, b + 1):
            for z in range(a, b + 1):
                if x in (a, b) or z in (a, b):
                    t.set(x, y1, z, RNB)
                    if (x + z) % 4 == 0:
                        t.set(x, y0 + 1, z, MAG)
    box(t, 21, 25, H + 16, H + 22, 21, 25, NB, hollow=True)
    box(t, 22, 24, H + 21, H + 21, 22, 24, BRICK)
    box(t, 22, 24, H + 22, H + 22, 22, 24, LAVA, props={"level": "0"})
    for x, z in ((21, 21), (25, 21), (21, 25), (25, 25)):
        t.set(x, H + 23, z, GILD)
    # 안뜰 수비대
    for x, z, m in ((8, 23, "ash_shaman"), (38, 23, "ash_shaman"), (23, 39, "obsidian_golem"), (18, 39, "ember_hound"), (28, 39, "ember_hound"),
                    (23, 8, "lava_turtle"), (23, 48, "lava_turtle")):
        t.entity(x + .5, F + 1.0, z + .5, mob(f"wildplus:{m}", 180.0))
    return t


def watchtower():
    X, Y, Z = 11, 34, 11
    t = Template(X, Y, Z)
    r = random.Random(51)
    for pos in list(t.blocks):                                           # 탑 둘레 한 칸 밖은 건드리지 않음 (아래쪽)
        if pos[1] < 8:
            del t.blocks[pos]
    box(t, 3, 7, 0, 7, 3, 7, STONE)                                      # 닻 기둥
    box(t, 1, 9, 8, 8, 1, 9, RNB)                                        # 발판
    for x in range(1, 10):
        for z in range(1, 10):
            if x in (1, 9) or z in (1, 9):
                t.set(x, 8, z, BRICK)
    top = 26
    wall_mix(t, 2, 8, 9, top, 2, 8, r)
    for y in (9, 10):                                                    # 문 (남쪽)
        t.set(5, y, 8, "minecraft:air")
    for y in range(9, top + 1):
        t.set(5, y, 3, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for y in (14, 20):
        for x, z in ((2, 5), (8, 5), (5, 8)):
            t.set(x, y, z, MAG)
    for x in range(1, 10):                                               # 꼭대기 (한 칸 넓게) + 톱니
        for z in range(1, 10):
            if not (x == 5 and z == 3):
                t.set(x, top, z, BRICK)
            if (x in (1, 9) or z in (1, 9)) and (x + z) % 2 == 0:
                t.set(x, top + 1, z, BRICK)
    t.set(5, top + 1, 5, GILD)                                           # 화톳불
    t.set(5, top + 2, 5, MAG)
    t.set(5, top + 3, 5, LIGHT)
    t.set(7, top + 1, 6, *chest("wildplus:chests/kalpa_tower", "west"))
    for x in (10,):                                                      # 등불 팔
        t.set(9, top + 2, 5, STONE)
        t.set(x, top + 2, 5, STONE)
        t.set(x, top + 1, 5, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    t.entity(3.5, top + 1.0, 6.5, mob("wildplus:ash_shaman", 0.0))
    t.entity(2.5, 9.0, 9.5, mob("wildplus:ember_hound", 0.0))
    t.entity(8.5, 9.0, 9.5, mob("wildplus:ember_hound", 0.0))
    return t


COLOR = {BRICK: (58, 52, 64), STONE: (44, 40, 50), BAS: (86, 84, 92), PBAS: (110, 108, 116), NB: (60, 30, 36), RNB: (120, 30, 34), MAG: (220, 110, 30), GILD: (230, 180, 70),
         LIGHT: (255, 200, 120), LAVA: (255, 120, 20), GATE: (200, 40, 50), ALTAR: (255, 240, 200), BRAZIER: (255, 230, 120), "minecraft:ladder": (150, 110, 70),
         "minecraft:chest": (190, 130, 50), "minecraft:lantern": (255, 220, 140)}


def preview(t, path, s=5):
    X, Y, Z = t.size
    im = Image.new("RGB", ((X + Z) * s + 40, (X + Z) * s // 2 + Y * s + 40), (30, 16, 20))
    px = im.load()
    for (x, y, z), (b, _, _) in sorted(t.blocks.items(), key=lambda e: (e[0][0] + e[0][2], e[0][1])):
        if b == "minecraft:air":
            continue
        c = COLOR.get(b, (200, 0, 200))
        sx = 20 + (x - z + Z) * s
        sy = im.height - 20 - (x + z) * s // 2 - y * s
        for dx in range(s * 2):
            for dy in range(s + s // 2):
                if 0 <= sx + dx < im.width and 0 <= sy - dy < im.height:
                    k = 1.0 if dy >= s else 0.72 if dx < s else 0.86
                    px[sx + dx, sy - dy] = tuple(int(v * k) for v in c)
    im.save(path)


def build():
    return [citadel().save("kalpa_citadel"), watchtower().save("ember_watchtower")]


if __name__ == "__main__":
    if len(sys.argv) > 1:
        preview(citadel(), sys.argv[1] + "/citadel.png")
        preview(watchtower(), sys.argv[1] + "/tower.png", 9)
    else:
        print(build())
