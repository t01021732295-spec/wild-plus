"""4.0 공허 유적 (98×36×39). 바닥 Y = 16. 좌표는 자바 event.VoidRuin 의 T_SEAL · T_ALTAR 와 같아야 한다.
입구(x 2~13) → 회랑(14~32) → 부유 다리(33~46) → 셜커 복도(47~64) → 보스실(가운데 x 81, 반지름 14) → 창고(z 33~37)."""
import math
import random

from nbtlib import Byte

from structures_v3 import Template, chest, mob

X, Y, Z = 98, 36, 39
B, CZ = 16, 18
BRICK, ROCK, GLOW, GATE = "wildplus:void_brick", "minecraft:end_stone", "wildplus:void_block", "wildplus:void_gate"
LOOT = "wildplus:chests/void_vault"


def build():
    t = Template(X, Y, Z)
    keep = set()
    r = random.Random(40)

    def isle(cx, cz, rx, rz, top, depth):
        for x in range(max(0, cx - rx), min(X, cx + rx + 1)):
            for z in range(max(0, cz - rz), min(Z, cz + rz + 1)):
                q = ((x - cx) / (rx + .5)) ** 2 + ((z - cz) / (rz + .5)) ** 2
                if q > 1:
                    continue
                for y in range(max(0, top - int((1 - q) * depth + r.random())), top + 1):
                    t.set(x, y, z, ROCK)

    def room(x0, x1, z0, z1, h, ceiling=True):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                t.set(x, B, z, BRICK)
                if ceiling:
                    t.set(x, B + h, z, BRICK)
                for y in range(B + 1, B + h):
                    if x in (x0, x1) or z in (z0, z1):
                        t.set(x, y, z, BRICK)
                    else:
                        keep.add((x, y, z))
        for x in range(x0 + 3, x1, 5):
            for z in (z0, z1):
                t.set(x, B + h - 2, z, GLOW)

    def doorway(x, z0, z1, h, block="minecraft:air"):
        for z in range(z0, z1 + 1):
            for y in range(B + 1, B + h):
                t.set(x, y, z, block)
                keep.add((x, y, z))

    # ① 입구 섬과 봉인 문
    isle(7, CZ, 7, 8, B, 9)
    for x in range(3, 14):
        for z in range(CZ - 2, CZ + 3):
            t.set(x, B, z, BRICK)
    # ② 회랑
    isle(23, CZ, 12, 9, B - 1, 9)
    room(13, 32, CZ - 6, CZ + 6, 8)
    doorway(13, CZ - 2, CZ + 2, 6, GATE)
    for y in range(B + 1, B + 7):
        for z in (CZ - 3, CZ + 3):
            t.set(12, y, z, GLOW if y % 2 else BRICK)
    t.set(11, B + 1, CZ, "wildplus:void_seal", {"facing": "west"})
    doorway(32, CZ - 2, CZ + 2, 6)
    for x, z in ((19, CZ - 3), (24, CZ + 3), (28, CZ - 2)):
        t.entity(x + .5, B + 1.0, z + .5, mob("minecraft:enderman", 90.0))
    for x, z in ((21, CZ + 5), (27, CZ - 5)):
        t.entity(x + .5, B + 1.0, z + .5, mob("minecraft:shulker", 0.0, {"AttachFace": Byte(0)}))
    # ③ 부유 다리 + 아래 착지 섬
    for i, x in enumerate(range(33, 46, 3)):
        y = B + int(1.4 * math.sin(i * 1.3)) + 0
        zz = CZ + int(2 * math.sin(i * 2.1))
        for a in range(2):
            for b in range(-1, 2):
                t.set(x + a, y, zz + b, BRICK)
    isle(40, CZ, 7, 7, B - 13, 2)
    for x in range(38, 43):
        for z in range(CZ - 2, CZ + 3):
            t.set(x, B - 13, z, "wildplus:void_lift")
    # ④ 셜커 복도와 레버 3개
    isle(55, CZ, 11, 8, B - 1, 9)
    room(47, 64, CZ - 5, CZ + 5, 8)
    doorway(47, CZ - 2, CZ + 2, 6)
    for x in (50, 56, 62):
        t.set(x, B + 2, CZ - 4, "minecraft:lever", {"face": "wall", "facing": "south", "powered": "false"})
    for x in range(49, 64, 3):
        z = CZ + 4 if x % 2 else CZ - 3
        t.entity(x + .5, B + 1.0, z + .5, mob("minecraft:shulker", 0.0, {"AttachFace": Byte(0)}))
    doorway(64, CZ - 2, CZ + 2, 6, GATE)
    # ⑤ 보스실
    isle(81, CZ, 16, 17, B - 1, 12)
    ax, R = 81, 14
    for x in range(ax - R - 1, ax + R + 2):
        for z in range(CZ - R - 1, CZ + R + 2):
            d = math.hypot(x - ax, z - CZ)
            if d > R + .6:
                continue
            t.set(x, B, z, GLOW if int(d) % 5 == 0 and d > 1 else BRICK)
            for y in range(B + 1, B + 12):
                if d > R - .4:
                    t.set(x, y, z, GLOW if y % 4 == 0 else BRICK)
                else:
                    keep.add((x, y, z))
    for k in range(6):
        a = math.radians(k * 60)
        px, pz = round(ax + 9 * math.cos(a)), round(CZ + 9 * math.sin(a))
        for y in range(B + 1, B + 6):
            t.set(px, y, pz, BRICK)
        t.set(px, B + 6, pz, GLOW)
    t.set(ax, B, CZ, "wildplus:void_altar")
    for x in range(65, 68):
        for z in range(CZ - 2, CZ + 3):
            t.set(x, B, z, BRICK)
            for y in range(B + 1, B + 6):
                t.set(x, y, z, "minecraft:air")
                keep.add((x, y, z))
    for x in range(65, 68):
        for y in range(B + 1, B + 6):
            for z in (CZ - 3, CZ + 3):
                t.set(x, y, z, BRICK)
        for z in range(CZ - 3, CZ + 4):
            t.set(x, B + 6, z, BRICK)
    # ⑥ 창고
    isle(ax, 35, 8, 3, B - 1, 5)
    room(ax - 5, ax + 5, 32, 38, 6)
    for x in range(ax - 1, ax + 2):
        for y in range(B + 1, B + 5):
            t.set(x, y, 32, GATE)
            keep.add((x, y, 32))
            t.set(x, y, 31, "minecraft:air")
            keep.add((x, y, 31))
    for x in (ax - 3, ax, ax + 3):
        t.set(x, B + 1, 36, *chest(LOOT, "north"))
    for pos, v in list(t.blocks.items()):
        if v[0] == "minecraft:air" and pos not in keep:
            del t.blocks[pos]
    return t.save("void_ruin")


if __name__ == "__main__":
    print(build())
