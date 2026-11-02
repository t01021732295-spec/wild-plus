"""2.12 텍스처: 명품관 치장 — 날개 3 · 링 2 · 동물 탈 바탕 · 관상용 무기 3 (직접 그림)."""
import math
import random

from PIL import Image

from textures_v5 import OUT

COS = OUT / "entity/cosmetic"


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ───────── 날개 (64×32, 판 16×20) ─────────
def wing_shape(ul, v):
    """ul: 몸쪽 0 → 끝 15, v: 위 0 → 아래 19. 날개 안이면 (깃털 칸 번호, 위쪽 뼈인가) 아니면 None"""
    top = 4 - ul * 0.27 + (1.5 if ul < 2 else 0)
    bottom = 19 - ul * 0.8
    if ul % 3 == 2:
        bottom -= 1.5  # 깃털 끝 들쭉날쭉
    if v < top or v > bottom:
        return None
    return ul // 3, v - top < 1.6


def paint_wing(name, color_fn):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    for ul in range(16):
        for v in range(20):
            sh = wing_shape(ul, v)
            if sh is None:
                continue
            c = color_fn(ul, v, sh[0], sh[1])
            if c is None:
                continue
            px[1 + ul, 1 + v] = c + (255,)
            px[18 + (15 - ul), 1 + v] = c + (255,)
    # 위·아래·옆 면 (판 두께)
    edge = color_fn(8, 2, 0, True)
    for u in range(1, 33):
        px[u, 0] = edge + (255,)
    for v in range(1, 21):
        px[0, v] = edge + (255,)
        px[17, v] = edge + (255,)
    im.save(COS / f"wings_{name}.png")


def angel(ul, v, feather, bone):
    if bone:
        return (250, 214, 110)
    base = mix((255, 255, 255), (206, 214, 228), v / 19)
    if ul % 3 == 0:
        base = mix(base, (170, 180, 200), 0.35)
    return base


def demon(ul, v, feather, bone):
    if bone or (ul % 4 == 0 and v < 17):
        return (24, 10, 14)
    t = v / 19
    return mix((120, 14, 26), (54, 6, 12), t)


def dragon(ul, v, feather, bone):
    if bone:
        return (90, 255, 240)
    edge = wing_shape(ul, v + 1) is None
    if edge:
        return (140, 255, 250)
    if ul % 4 == 1 and v % 2 == 0:
        return (70, 200, 220)
    return mix((60, 20, 110), (24, 8, 50), v / 19)


# ───────── 링 (32×16) ─────────
def halo(name, a, b, sparkle):
    im = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = im.load()
    r = random.Random(5)
    for y in range(16):
        for x in range(32):
            c = mix(a, b, (x + y) % 6 / 6)
            if sparkle and r.random() < 0.08:
                c = (255, 255, 255)
            px[x, y] = c + (255,)
    im.save(COS / f"halo_{name}.png")


# ───────── 옷 (64×32, 갑옷과 같은 배치) ─────────
def faces(u0, v0, w, h, d):
    return {"up": (u0 + d, v0, w, d), "down": (u0 + d + w, v0, w, d), "west": (u0, v0 + d, d, h),
            "north": (u0 + d, v0 + d, w, h), "east": (u0 + d + w, v0 + d, d, h), "south": (u0 + 2 * d + w, v0 + d, w, h)}


BODY, ARM, LEG = (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)


def outfit(name, part_fn):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(hash(name) & 0xFFFF)
    for part, box in (("body", BODY), ("arm", ARM), ("leg", LEG)):
        for face, (x0, y0, w, h) in faces(*box).items():
            for y in range(h):
                for x in range(w):
                    c = part_fn(part, face, x, y, w, h)
                    if c is None:
                        continue
                    n = rnd.randint(-5, 5)
                    px[x0 + x, y0 + y] = tuple(max(0, min(255, k + n)) for k in c) + (255,)
    im.save(COS / f"outfit_{name}.png")


GOLDC = (238, 190, 70)


def knight(part, face, x, y, w, h):
    navy, dark = (28, 42, 80), (40, 40, 46)
    if part == "body":
        if face == "north":
            if y == 0:
                return GOLDC
            if y == 9:
                return (230, 200, 90) if x in (3, 4) else (24, 20, 20)
            if x + y == 8 or x + y == 9:
                return (170, 30, 40)  # 붉은 띠
            if x in (3, 4) and y in (2, 5):
                return GOLDC
        if face == "up":
            return GOLDC
        return navy
    if part == "arm":
        if face == "up" or (y == 0 and face != "down"):
            return GOLDC
        if y >= 10:
            return GOLDC if y == 10 else (230, 230, 236)
        return navy
    if y >= 9:
        return (20, 18, 18)
    if face in ("west", "east") and x == 1:
        return GOLDC
    return dark


def royal(part, face, x, y, w, h):
    crimson, fur = (138, 16, 32), (244, 242, 236)
    furc = lambda: (20, 20, 20) if (x * 7 + y * 3) % 5 == 0 else fur
    if part == "body":
        if y <= 1 and face != "down":
            return furc()
        if face == "north" and x in (3, 4):
            return fur if y > 1 else furc()
        if face in ("north", "south") and x in (1, 6):
            return GOLDC
        return crimson
    if part == "arm":
        if y >= 10:
            return furc()
        if face in ("north", "south") and x == 1:
            return GOLDC
        return crimson
    if y >= 10:
        return GOLDC if y == 10 else (60, 20, 24)
    return mix(crimson, (100, 10, 22), y / 12)


def ninja(part, face, x, y, w, h):
    black = (22, 22, 28)
    if part == "body":
        if y in (8, 9):
            return (140, 20, 30)
        if face == "north" and abs(x - (3 + y // 3)) == 0 and y < 8:
            return (50, 50, 60)
        return black
    if part == "arm":
        if y >= 6 and (x + y) % 3 == 0:
            return (110, 110, 120)
        return black
    if y >= 6 and (x + y) % 3 == 0:
        return (110, 110, 120)
    return black


def angel_dress(part, face, x, y, w, h):
    white = mix((252, 252, 255), (220, 226, 240), y / 12)
    if part == "body":
        if y == 7:
            return GOLDC
        if face == "north" and x + y in (4, 5):
            return (140, 200, 255)
        if y == 0:
            return GOLDC
        return white
    if part == "arm":
        if y >= 10:
            return GOLDC
        return white
    if y == 11:
        return GOLDC
    return white


# ───────── 관상용 무기 (32×32) ─────────
def weapon(name, draw):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw(im.load())
    im.save(OUT / "item" / f"display_{name}.png")


def put(px, x, y, c):
    if 0 <= x < 32 and 0 <= y < 32:
        px[x, y] = c + (255,)


def greatsword(px):
    # 손잡이 (아래 왼쪽) → 날 (위 오른쪽)
    for t in range(0, 8):
        put(px, 3 + t, 28 - t, (90, 50, 20))
        put(px, 4 + t, 28 - t, (120, 70, 30))
    put(px, 2, 29, GOLDC)
    put(px, 3, 29, GOLDC)
    put(px, 2, 28, GOLDC)
    # 가드
    for k in range(-5, 6):
        put(px, 11 + k, 21 + k, GOLDC)
        put(px, 12 + k, 21 + k, (190, 140, 40))
    put(px, 11, 20, (220, 30, 50))
    put(px, 12, 21, (220, 30, 50))
    # 날
    for t in range(0, 17):
        x, y = 13 + t, 19 - t
        for o in range(-2, 3):
            c = mix((255, 248, 220), (210, 170, 70), abs(o) / 2.5)
            if o == 0:
                c = (255, 255, 255)
            put(px, x + (o if o > 0 else 0), y + (-o if o < 0 else 0), c)
    put(px, 30, 1, (255, 255, 255))
    put(px, 29, 1, (255, 240, 180))


def staff(px):
    for t in range(0, 22):
        put(px, 4 + t, 28 - t, (70, 40, 90) if t % 4 else (240, 200, 90))
        put(px, 5 + t, 28 - t, (100, 60, 130))
    # 초승달 + 별 결정
    cx, cy = 25, 6
    for a in range(0, 360, 6):
        r = math.radians(a)
        x, y = cx + round(math.cos(r) * 5), cy + round(math.sin(r) * 5)
        if not (-60 < a - 315 < 60):
            put(px, x, y, (250, 214, 110))
    for (x, y) in [(25, 3), (25, 4), (25, 5), (25, 6), (25, 7), (25, 8), (25, 9), (22, 6), (23, 6), (24, 6), (26, 6), (27, 6), (28, 6)]:
        put(px, x, y, (160, 240, 255))
    put(px, 25, 6, (255, 255, 255))
    for (x, y) in [(24, 5), (26, 5), (24, 7), (26, 7)]:
        put(px, x, y, (120, 200, 255))


def scythe(px):
    for t in range(0, 24):
        put(px, 5 + t // 2, 29 - t, (40, 20, 50))
        put(px, 6 + t // 2, 29 - t, (70, 30, 90))
    # 휘어진 날
    for a in range(0, 100):
        r = math.radians(200 + a * 1.4)
        for rr in range(9, 13):
            x, y = 9 + round(math.cos(r) * rr) + 12, 14 + round(math.sin(r) * rr) - 6
            c = (255, 110, 230) if rr == 12 else mix((30, 8, 40), (120, 40, 160), (rr - 9) / 3)
            put(px, x, y, c)


def hat_base():
    """동물 탈 바탕: 거의 흰 잡음 — 게임에서 부품마다 색을 곱해서 칠한다"""
    im = Image.new("RGBA", (64, 64))
    px = im.load()
    r = random.Random(7)
    for y in range(64):
        for x in range(64):
            v = r.randint(228, 255)
            px[x, y] = (v, v, v, 255)
    im.save(COS / "hat_base.png")


def build():
    COS.mkdir(parents=True, exist_ok=True)
    paint_wing("angel", angel)
    paint_wing("demon", demon)
    paint_wing("dragon", dragon)
    halo("gold", (255, 236, 150), (220, 160, 40), False)
    halo("star", (230, 250, 255), (110, 190, 255), True)
    hat_base()
    weapon("greatsword", greatsword)
    weapon("staff", staff)
    weapon("scythe", scythe)
    return 12


if __name__ == "__main__":
    print(build())
