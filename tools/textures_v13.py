"""16차 텍스처: 스타 루비 장비(네더라이트 모양 + 진홍·장밋빛 + 금빛 반짝임), 다이아 배낭, 필터 호퍼, 던전 봉인 벽돌.

바닐라 원본은 ci-probe 브랜치에서 받은 폴더(WILDPLUS_VANILLA2)를 쓴다.
"""
import colorsys
import math
import random
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, gradient, lum, recolor, sel_all, sel_cyan as sel_cyan_any, sel_grey

STAR = ["#14000c", "#3e0420", "#7a0c36", "#c0164c", "#f23c6e", "#ff8db2", "#fff0f6"]
MYTHRIL = ["#0a1a2c", "#16385a", "#22578a", "#3a82b8", "#6cb6e0", "#b4e4f8", "#eefaff"]
GOLD_HI = (255, 226, 140)


def sparkle(im, cx, cy, big=True):
    """4갈래 별 반짝임 (가운데 흰색, 팔은 연한 금색)"""
    px = im.load()
    w, h = im.size

    def put(x, y, c):
        if 0 <= x < w and 0 <= y < h:
            px[x, y] = c + (255,)

    put(cx, cy, (255, 255, 255))
    arm = 2 if big else 1
    for d in range(1, arm + 1):
        c = (255, 244, 200) if d == 1 else GOLD_HI
        for (dx, dy) in [(d, 0), (-d, 0), (0, d), (0, -d)]:
            put(cx + dx, cy + dy, c)


GOLD = ["#3a2400", "#6b4a0a", "#a8780e", "#d9a22e", "#ffd25a", "#ffeaa0", "#fffbe6"]


def sel_wood(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s > 0.15 and 0.04 <= h <= 0.14


def star_item(src: Path, spark, metal_sel, wood=True):
    """다이아(도구)·철(갑옷) 그림의 금속 부분을 스타 루비 색으로, 나무 손잡이는 금으로, 별 반짝임 추가"""
    im = recolor(src, STAR, metal_sel)
    if wood:
        tmp = im.copy()
        tmp_path = Path("/tmp/_wp_star_tmp.png")
        tmp.save(tmp_path)
        im = recolor(tmp_path, GOLD, sel_wood)
    if not wood:
        # 갑옷: 테두리를 금테로
        px = im.load()
        edge = []
        for y in range(im.height):
            for x in range(im.width):
                if px[x, y][3] == 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < im.width and 0 <= ny < im.height) or px[nx, ny][3] == 0:
                        edge.append((x, y))
                        break
        for (x, y) in edge:
            px[x, y] = (150, 104, 18, 255) if y > im.height // 2 else (196, 146, 34, 255)
    for (x, y, big) in spark:
        sparkle(im, x, y, big)
    return im


def star_layer(src: Path, seed):
    """갑옷 몸 그림: 진홍색으로 바꾸고 별가루(밝은 점)를 흩뿌림"""
    im = recolor(src, STAR, sel_all)
    px = im.load()
    rnd = random.Random(seed)
    for _ in range(int(im.width * im.height * 0.006)):
        x, y = rnd.randrange(im.width), rnd.randrange(im.height)
        if px[x, y][3] > 0 and lum(px[x, y]) > 60:
            px[x, y] = ((255, 240, 250) if rnd.random() < 0.5 else GOLD_HI) + (255,)
    return im


def hue_shift(src: Path, target_h, sat_mul=1.0, light_add=0.0):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if s > 0.2:
                h = target_h
                s = min(1.0, s * sat_mul)
                l = min(1.0, l + light_add)
            rr, gg, bb = colorsys.hls_to_rgb(h, l, s)
            px[x, y] = (int(rr * 255), int(gg * 255), int(bb * 255), a)
    return im


def seal_bricks(gate=False):
    """던전 봉인 벽돌 (보라빛 검은 벽돌), 문은 영혼불 룬이 빛남"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    rnd = random.Random(31 if gate else 17)
    for y in range(16):
        for x in range(16):
            mortar = (y % 4 == 3) or ((x + (4 if (y // 4) % 2 else 0)) % 8 == 7)
            n = rnd.randint(-6, 6)
            px[x, y] = ((14 + n, 10 + n, 22 + n, 255) if mortar else (40 + n, 30 + n, 56 + n, 255))
    if gate:
        for y in range(1, 15):
            for x in (3, 7, 8, 12):
                px[x, y] = (30, 26, 34, 255)
        for i in range(24):
            a = i / 24 * math.pi * 2
            x, y = int(round(7.5 + math.cos(a) * 4)), int(round(7.5 + math.sin(a) * 4))
            px[x, y] = (90, 220, 255, 255)
        for (x, y) in [(7, 7), (8, 8), (7, 8), (8, 7), (7, 5), (8, 10), (5, 7), (10, 8)]:
            px[x, y] = (190, 250, 255, 255)
    else:
        for (x, y) in [(5, 5), (10, 9), (3, 12)]:
            px[x, y] = (120, 80, 170, 255)
    return im


def build(van: Path):
    T = van / "minecraft/textures"
    if not T.exists():
        T = van / "assets/minecraft/textures"
    item, eq, block = T / "item", T / "entity/equipment", T / "block"
    out_item = OUT / "item"
    sparks = {
        "sword": [(11, 2, True), (5, 9, False)], "pickaxe": [(3, 3, True), (12, 5, False)],
        "axe": [(5, 2, True), (10, 7, False)], "shovel": [(12, 2, True)], "hoe": [(4, 2, True), (10, 3, False)],
        "helmet": [(3, 4, True), (12, 6, False)], "chestplate": [(4, 4, True), (11, 10, False)],
        "leggings": [(4, 3, True), (11, 11, False)], "boots": [(3, 8, True), (12, 11, False)],
    }
    n = 0
    van1 = Path(__file__).resolve().parent.parent  # 이전 바닐라 묶음(다이아·철 그림)은 WILDPLUS_VANILLA
    import os
    v1 = Path(os.environ.get("WILDPLUS_VANILLA", "")) / "minecraft/textures/item"
    for t, sp in sparks.items():
        if t in ("sword", "pickaxe", "axe", "shovel", "hoe"):
            star_item(v1 / f"diamond_{t}.png", sp[:1], sel_cyan_any).save(out_item / f"star_ruby_{t}.png")
        else:
            star_item(v1 / f"iron_{t}.png", sp[:1], sel_grey, wood=False).save(out_item / f"star_ruby_{t}.png")
        n += 1
    (OUT / "entity/equipment/humanoid").mkdir(parents=True, exist_ok=True)
    (OUT / "entity/equipment/humanoid_leggings").mkdir(parents=True, exist_ok=True)
    star_layer(eq / "humanoid/netherite.png", 1).save(OUT / "entity/equipment/humanoid/star_ruby.png")
    star_layer(eq / "humanoid_leggings/netherite.png", 2).save(OUT / "entity/equipment/humanoid_leggings/star_ruby.png")
    # 다이아 배낭: 금 배낭을 청록빛으로
    hue_shift(out_item / "gold_backpack.png", 0.5, 1.1, 0.05).save(out_item / "diamond_backpack.png")
    # 필터 호퍼: 바닐라 호퍼를 미스릴 색으로
    for name in ("hopper_outside", "hopper_top", "hopper_inside"):
        recolor(block / f"{name}.png", MYTHRIL, sel_all).save(OUT / "block" / f"filter_{name}.png")
    seal_bricks(False).save(OUT / "block/dungeon_seal.png")
    seal_bricks(True).save(OUT / "block/dungeon_seal_gate.png")
    sorting_textures()
    return n + 11


if __name__ == "__main__":
    import sys
    print(build(Path(sys.argv[1])))


def sorting_textures():
    """정렬 상자: 짙은 나무 상자 + 금속 테 + 앞면에 색깔 화살표 세 개(분류)"""
    rnd = random.Random(7)

    def wood(seed):
        im = Image.new("RGBA", (16, 16))
        px = im.load()
        r = random.Random(seed)
        for y in range(16):
            for x in range(16):
                n = r.randint(-8, 8) + (6 if (y // 4) % 2 else 0)
                px[x, y] = (110 + n, 74 + n // 2, 42 + n // 3, 255)
        for i in range(16):
            for (x, y) in [(i, 0), (i, 15), (0, i), (15, i)]:
                px[x, y] = (70, 72, 80, 255)
        for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
            px[x, y] = (190, 195, 205, 255)
        return im

    side = wood(3)
    front = wood(4)
    px = front.load()
    cols = [(229, 72, 77), (59, 130, 246), (34, 163, 90)]
    for k, c in enumerate(cols):
        y = 4 + k * 4
        for x in range(3, 11):
            px[x, y] = c + (255,)
        for (x, yy) in [(10, y - 1), (11, y - 1), (11, y), (12, y), (10, y + 1), (11, y + 1)]:
            px[x, yy] = c + (255,)
    top = wood(5)
    px = top.load()
    for x in range(5, 11):
        for y in range(5, 11):
            px[x, y] = (40, 34, 30, 255)
    for (x, y) in [(6, 6), (9, 6), (6, 9), (9, 9)]:
        px[x, y] = (200, 170, 90, 255)
    side.save(OUT / "block/sorting_chest_side.png")
    front.save(OUT / "block/sorting_chest_front.png")
    top.save(OUT / "block/sorting_chest_top.png")
