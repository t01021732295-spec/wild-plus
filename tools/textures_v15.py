"""2.8 텍스처: 쓰레기통(초록 철통), 자동 제작대(나무+철 기계, 윗면 금빛 3×3 격자, 옆면 톱니)."""
import math
import random

from PIL import Image

from textures_v5 import OUT


def _img():
    return Image.new("RGBA", (16, 16))


def trash_side():
    im = _img()
    px = im.load()
    r = random.Random(5)
    for y in range(16):
        for x in range(16):
            n = r.randint(-5, 5)
            base = (52, 112, 72) if x % 4 not in (0,) else (38, 86, 54)  # 세로 홈
            if x % 4 == 1:
                base = (74, 140, 96)  # 홈 옆 밝은 면
            px[x, y] = (base[0] + n, base[1] + n, base[2] + n, 255)
    for x in range(16):
        px[x, 0] = (150, 160, 165, 255)
        px[x, 1] = (110, 118, 124, 255)
        px[x, 15] = (70, 76, 82, 255)
    # 재활용 표시 (흰 삼각 화살표)
    for (x, y) in [(7, 6), (8, 6), (6, 7), (9, 7), (5, 8), (10, 8), (5, 9), (6, 9), (9, 9), (10, 9), (7, 9), (8, 9)]:
        px[x, y] = (230, 240, 232, 255)
    return im


def trash_lid():
    im = _img()
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = 150 - int(d * 4)
            px[x, y] = (c - 20, c, c - 30, 255) if (x + y) % 7 else (c - 30, c - 10, c - 40, 255)
    for x in range(16):
        for y in (0, 15):
            px[x, y] = (60, 66, 70, 255)
            px[y, x] = (60, 66, 70, 255)
    for x in range(5, 11):
        px[x, 7] = (190, 196, 200, 255)
        px[x, 8] = (120, 126, 130, 255)
    return im


def trash_inner():
    im = _img()
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (20, 34, 24, 255)
    return im


def plank(seed, dark=False):
    im = _img()
    px = im.load()
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            n = r.randint(-7, 7) + (5 if (y // 4) % 2 else -2)
            b = (96, 62, 36) if dark else (140, 96, 56)
            px[x, y] = (b[0] + n, b[1] + n // 2, b[2] + n // 3, 255)
        if y % 4 == 3:
            for x in range(16):
                c = px[x, y]
                px[x, y] = (c[0] - 30, c[1] - 22, c[2] - 14, 255)
    return im


def frame(im):
    px = im.load()
    for i in range(16):
        for (x, y) in [(i, 0), (i, 15), (0, i), (15, i)]:
            px[x, y] = (88, 92, 100, 255)
        for (x, y) in [(i, 1), (1, i)]:
            if 0 < i < 15:
                px[x, y] = (150, 156, 166, 255)
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        px[x, y] = (210, 214, 222, 255)
    return im


def crafter_top():
    im = frame(plank(11))
    px = im.load()
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = (44, 30, 20, 255)
    for k in (3, 6, 9, 12):
        for t in range(3, 13):
            px[k, t] = (232, 182, 64, 255)
            px[t, k] = (232, 182, 64, 255)
    for (x, y) in [(4, 4), (7, 7), (10, 10), (10, 4), (4, 10)]:
        px[x, y] = (110, 200, 255, 255)
    return im


def crafter_side(active=False):
    im = frame(plank(12, dark=True))
    px = im.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            a = math.atan2(y - cy, x - cx)
            d = math.hypot(x - cx, y - cy)
            tooth = 4.6 + (1.2 if math.cos(a * 8) > 0.2 else 0)
            if d <= tooth and d >= 1.5:
                shade = 175 - int(d * 9)
                px[x, y] = (shade, shade + 4, shade + 12, 255)
            elif d < 1.5:
                px[x, y] = (232, 182, 64, 255)
    # 아래쪽 출력 표시 (초록 화살표)
    g = (74, 222, 128, 255)
    for (x, y) in [(7, 12), (8, 12), (7, 13), (8, 13), (6, 13), (9, 13)]:
        px[x, y] = g
    return im


def crafter_bottom():
    im = frame(plank(13, dark=True))
    px = im.load()
    for y in range(5, 11):
        for x in range(5, 11):
            px[x, y] = (30, 30, 34, 255)
    return im


def velvet(seed):
    im = _img()
    px = im.load()
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            n = r.randint(-6, 6)
            px[x, y] = (70 + n, 18 + n // 2, 90 + n, 255)
    for i in range(16):
        for (x, y) in [(i, 0), (i, 15), (0, i), (15, i)]:
            px[x, y] = (232, 182, 64, 255)
        for (x, y) in [(i, 1), (1, i), (i, 14), (14, i)]:
            if 0 < i < 15:
                px[x, y] = (150, 104, 30, 255)
    return im


def luxury_side():
    im = velvet(21)
    px = im.load()
    # 다이아몬드 문양
    for y in range(4, 12):
        w = 4 - abs(y - 7.5) + 0.5
        for x in range(16):
            if abs(x - 7.5) <= w:
                px[x, y] = (120, 240, 240, 255) if (x + y) % 3 else (230, 255, 255, 255)
    for (x, y) in [(7, 4), (8, 4), (7, 11), (8, 11)]:
        px[x, y] = (40, 160, 170, 255)
    return im


def luxury_top():
    im = velvet(22)
    px = im.load()
    for x in range(4, 12):
        px[x, 7] = (232, 182, 64, 255)
        px[x, 8] = (232, 182, 64, 255)
    for (x, y) in [(4, 5), (7, 4), (8, 4), (11, 5), (4, 6), (11, 6), (7, 5), (8, 5)]:
        px[x, y] = (232, 182, 64, 255)
    px[7, 6] = px[8, 6] = (220, 40, 60, 255)
    return im


def realty_side():
    im = frame(plank(31))
    px = im.load()
    # 집 그림 (빨간 지붕 + 흰 벽 + 문)
    for y in range(4, 8):
        for x in range(8 - (y - 3), 8 + (y - 3)):
            px[x, y] = (196, 52, 46, 255)
    for y in range(8, 13):
        for x in range(4, 12):
            px[x, y] = (240, 232, 214, 255)
    for y in range(10, 13):
        px[7, y] = px[8, y] = (110, 70, 40, 255)
    px[5, 9] = px[10, 9] = (110, 180, 230, 255)
    return im


def build():
    b = OUT / "block"
    trash_side().save(b / "trash_can_side.png")
    trash_lid().save(b / "trash_can_lid.png")
    trash_inner().save(b / "trash_can_inner.png")
    crafter_top().save(b / "auto_crafter_top.png")
    crafter_side().save(b / "auto_crafter_side.png")
    crafter_bottom().save(b / "auto_crafter_bottom.png")
    luxury_side().save(b / "luxury_shop_side.png")
    luxury_top().save(b / "luxury_shop_top.png")
    realty_side().save(b / "realty_side.png")
    frame(plank(32)).save(b / "realty_top.png")
    return 6


if __name__ == "__main__":
    print(build())
