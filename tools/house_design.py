"""3.3 집 도안 (블록 단위 상자 · 칸마다 4픽셀 무늬). x 오른쪽, y 위, z 앞(+z = 현관), 가운데 기준."""
from cosmetic_design import Box, mix, shade

R = 0.25  # 블록당 4칸 무늬


def B(x, y, z, w, h, d, paint):
    b = Box(x * 16, y * 16, z * 16, w * 16, h * 16, d * 16, paint)
    b.res = R
    return b


def solid(c, noise=True):
    return lambda f, u, v, W, H: shade(c, 0.94) if noise and (u * 7 + v * 13) % 11 == 0 else c


def logs(c):
    def f(face, u, v, W, H):
        if face in ("top", "bottom"):
            return shade(c, 1.1)
        if v % 4 == 3:
            return shade(c, 0.7)
        return shade(c, 1.05) if v % 4 == 0 else c
    return f


def planks(c):
    def f(face, u, v, W, H):
        if face in ("top", "bottom"):
            return shade(c, 1.05)
        return shade(c, 0.78) if v % 2 == 1 and (u + v * 3) % 8 == 0 else (shade(c, 0.9) if v % 2 else c)
    return f


def bricks(c1, c2=None, mortar=(200, 190, 175)):
    c2 = c2 or shade(c1, 0.88)

    def f(face, u, v, W, H):
        if face in ("top", "bottom"):
            return shade(c1, 0.95)
        if v % 2 == 1:
            return mortar if (u + (v // 2) * 2) % 4 == 0 else (c1 if (u // 4 + v) % 3 else c2)
        return c1 if (u + v) % 5 else c2
    return f


def tiles(c):
    def f(face, u, v, W, H):
        if face == "bottom":
            return shade(c, 0.6)
        return shade(c, 0.78) if v % 2 == 0 else (c if (u + v) % 3 else shade(c, 1.08))
    return f


def grass(face, u, v, W, H):
    if face == "top":
        return (95, 160, 70) if (u * 3 + v * 7) % 5 else (80, 140, 60)
    return (120, 85, 55) if v > 0 else (95, 160, 70)


def window(x, y, z, w=1, h=1, axis="z", frame=(250, 250, 245), glass_c=(150, 200, 230)):
    """벽에 붙는 창 (axis: 'z' 이면 앞/뒤 벽, 'x' 면 옆 벽)"""
    g = lambda f, u, v, W, H: frame if u in (0, W - 1) or v in (0, H - 1) or u == W // 2 else (shade(glass_c, 1.15) if (u - v) % 5 == 0 else glass_c)
    if axis == "z":
        return [B(x, y, z, w, h, 0.12, g)]
    return [B(x, y, z, 0.12, h, w, g)]


def door(x, y, z, c=(110, 65, 35), axis="z"):
    def f(face, u, v, W, H):
        if u == W - 1 and v == H // 2:
            return (230, 190, 70)
        return shade(c, 0.8) if u in (0, W - 1) or v in (0, H - 1) or v == H // 2 else c
    return [B(x, y, z, 1, 2, 0.14, f)] if axis == "z" else [B(x, y, z, 0.14, 2, 1, f)]


def gable(cx, y0, z0, w, d, c, steps=None, overhang=0.5):
    """맞배 지붕: 층마다 좌우로 1칸씩 좁아짐 (앞뒤로 overhang 만큼 나옴)"""
    out = []
    k = 0
    while w - 2 * k > 0:
        ww = w - 2 * k + 2 * overhang if k == 0 else w - 2 * k
        out.append(B(cx - ww / 2, y0 + k * 0.5, z0 - overhang, ww, 0.5, d + 2 * overhang, tiles(c)))
        k += 1
        if steps and k >= steps:
            break
    return out


def plate(w, d, c=None):
    return [B(-w / 2 - 1, -0.5, -d / 2 - 1, w + 2, 0.5, d + 2, grass)]


# ═══════════ 1. 통나무 오두막 7×7 ═══════════
def cabin():
    W, D = 7, 7
    out = plate(W, D + 2)
    out += [B(-3.5, 0, -3.5, 7, 0.5, 7, solid((120, 120, 118)))]
    out += [B(-3.5, 0.5, -3.5, 7, 3.5, 7, logs((140, 95, 55)))]
    for (x, z) in [(-3.6, -3.6), (3.1, -3.6), (-3.6, 3.1), (3.1, 3.1)]:
        out.append(B(x, 0.5, z, 0.5, 3.5, 0.5, logs((100, 65, 35))))
    out += door(-0.5, 0.5, 3.5)
    out += window(-2.7, 1.6, 3.5, 1.2, 1.2) + window(1.5, 1.6, 3.5, 1.2, 1.2)
    out += window(3.5, 1.6, -1, 1.5, 1.2, "x") + window(-3.62, 1.6, -1, 1.5, 1.2, "x")
    out += gable(0, 4, -3.5, 8, 7, (150, 60, 45))
    out += [B(-2.8, 3, -2.5, 1.2, 4.5, 1.2, bricks((140, 130, 125), (120, 112, 108)))]
    # 현관 데크 + 난간 + 등
    out += [B(-3.5, 0, 3.5, 7, 0.4, 2, planks((170, 120, 70)))]
    for x in (-3.4, -2.2, 1.9, 3.1):
        out.append(B(x, 0.4, 5.2, 0.25, 1, 0.25, solid((120, 80, 45))))
    out += [B(-3.5, 1.3, 5.2, 2.2, 0.15, 0.25, solid((120, 80, 45))), B(1.3, 1.3, 5.2, 2.2, 0.15, 0.25, solid((120, 80, 45)))]
    out += [B(0.8, 2.2, 3.55, 0.35, 0.45, 0.35, solid((255, 220, 120), False))]
    out += [B(-3.3, 0.4, 4, 0.8, 0.5, 0.8, solid((150, 100, 60))), B(-3.15, 0.9, 4.15, 0.5, 0.5, 0.5, solid((80, 160, 70)))]
    return out


# ═══════════ 2. 숲속 통나무집 9×8 (지붕 있는 베란다) ═══════════
def log_house():
    out = plate(9, 11)
    out += [B(-4.5, 0, -4, 9, 0.5, 8, solid((130, 128, 124)))]
    out += [B(-4.5, 0.5, -4, 9, 4, 8, logs((160, 110, 65)))]
    for (x, z) in [(-4.6, -4.1), (4.1, -4.1), (-4.6, 3.6), (4.1, 3.6)]:
        out.append(B(x, 0.5, z, 0.5, 4, 0.5, logs((110, 70, 40))))
    out += door(-0.5, 0.5, 4)
    out += window(-3.5, 1.8, 4, 1.6, 1.4) + window(2, 1.8, 4, 1.6, 1.4)
    out += window(4.5, 1.8, -2.5, 1.5, 1.4, "x") + window(4.5, 1.8, 0.5, 1.5, 1.4, "x")
    for x in (-3.3, 2.3):  # 꽃 화분
        out += [B(x - 0.1, 1.5, 4.05, 1.8, 0.35, 0.4, solid((120, 80, 45))), B(x, 1.85, 4.1, 1.6, 0.3, 0.3,
                lambda f, u, v, W, H: [(230, 70, 90), (250, 210, 70), (180, 90, 200)][u % 3])]
    # 베란다 (지붕 + 기둥 + 흔들의자 느낌)
    out += [B(-4.5, 0, 4, 9, 0.4, 2.5, planks((180, 130, 80)))]
    for x in (-4.4, -0.8, 4.1):
        out.append(B(x, 0.4, 6.1, 0.35, 3, 0.35, logs((110, 70, 40))))
    out += [B(-5, 3.4, 4, 10, 0.3, 2.9, tiles((90, 70, 55)))]
    out += [B(2.2, 0.4, 4.7, 1, 0.6, 1, planks((150, 100, 60))), B(2.2, 1, 4.7, 1, 1, 0.2, planks((150, 100, 60)))]
    out += gable(0, 4.5, -4, 10, 8, (90, 70, 55))
    out += [B(2.5, 4, -3, 1.2, 5, 1.2, bricks((150, 140, 130)))]
    # 장작더미
    out += [B(-5.6, 0, -2, 1, 1.2, 3, lambda f, u, v, W, H: (200, 160, 110) if f in ("left", "right") and (u + v) % 2 else (130, 85, 45))]
    return out


# ═══════════ 3. 벽돌 2층집 9×9 (발코니) ═══════════
def brick_house():
    out = plate(9, 11)
    out += [B(-4.5, 0, -4.5, 9, 0.5, 9, solid((150, 145, 140)))]
    out += [B(-4.5, 0.5, -4.5, 9, 7.5, 9, bricks((170, 80, 60), (150, 68, 52)))]
    out += [B(-4.6, 4, -4.6, 9.2, 0.3, 9.2, solid((235, 230, 220)))]
    out += door(-0.5, 0.5, 4.5, (40, 70, 110))
    out += [B(-1, 2.5, 4.5, 2, 0.2, 0.8, solid((40, 70, 110)))]  # 문 위 차양
    for x in (-3.5, 2.2):
        out += window(x, 1.5, 4.5, 1.3, 1.6)
    for x in (-3.5, -0.6, 2.2):
        out += window(x, 5, 4.5, 1.3, 1.8)
    out += window(4.5, 1.5, -2, 1.5, 1.6, "x") + window(4.5, 5, -2, 1.5, 1.6, "x") + window(4.5, 5, 1, 1.5, 1.6, "x")
    # 2층 발코니 + 흰 난간
    out += [B(-2, 4.3, 4.5, 4, 0.3, 1.3, solid((235, 230, 220)))]
    for k in range(9):
        out.append(B(-2 + k * 0.48, 4.6, 5.65, 0.12, 0.8, 0.12, solid((245, 245, 240), False)))
    out += [B(-2, 5.4, 5.6, 4, 0.12, 0.2, solid((245, 245, 240), False))]
    out += gable(0, 8, -4.5, 10, 9, (170, 50, 40))
    out += [B(2.6, 8, -2.5, 1.2, 4.5, 1.2, bricks((150, 70, 55)))]
    # 계단 + 화단 + 우체통
    out += [B(-1, 0, 4.5, 2, 0.25, 1, solid((180, 175, 170))), B(-1, 0, 5.5, 2, 0.12, 0.6, solid((180, 175, 170)))]
    out += [B(-4.5, 0, 4.6, 3, 0.4, 0.8, lambda f, u, v, W, H: (230, 80, 100) if f == "top" and (u + v) % 2 else (90, 160, 70))]
    out += [B(3.5, 0, 5.8, 0.15, 1.1, 0.15, solid((60, 60, 60))), B(3.3, 1.1, 5.6, 0.55, 0.4, 0.6, solid((200, 50, 50)))]
    return out


# ═══════════ 4. 한옥 11×9 (기와지붕 · 대청마루) ═══════════
def hanok():
    out = plate(11, 11)
    out += [B(-5.5, 0, -4.5, 11, 1, 9, lambda f, u, v, W, H: (175, 170, 160) if (u // 2 + v // 2) % 2 else (155, 150, 142))]

    def wall(face, u, v, W, H):
        if face in ("top", "bottom"):
            return (240, 235, 222)
        if u % 8 == 0 or v in (0, H - 1) or v == H // 2:
            return (95, 60, 35)
        return (242, 238, 228)
    out += [B(-5, 1, -4, 10, 3.5, 8, wall)]

    def changho(face, u, v, W, H):  # 창호 (격자 창)
        if u in (0, W - 1) or v in (0, H - 1):
            return (95, 60, 35)
        return (120, 80, 45) if u % 2 == 0 or v % 2 == 0 else (250, 245, 225)
    for x in (-4, -1.5, 1, 3.5):
        out.append(B(x - 0.5, 1.5, 4, 1.8, 2.3, 0.12, changho))
    for x in (-5.2, -2.6, 0, 2.6, 5):
        out.append(B(x - 0.15, 1, 4.5, 0.4, 3.6, 0.4, solid((95, 60, 35))))
    out += [B(-5.5, 1, 4, 11, 0.35, 1.5, planks((185, 140, 90)))]  # 대청마루
    # 기와지붕: 넓게 · 처마 끝이 살짝 들림
    tile = tiles((80, 85, 92))
    for k, (w, d) in enumerate([(13, 11), (11.5, 9.5), (10, 8), (8.5, 6.5), (7, 5)]):
        out.append(B(-w / 2, 4.5 + k * 0.45, -d / 2, w, 0.45, d, tile))
    out += [B(-4, 6.75, -0.4, 8, 0.6, 0.8, solid((60, 64, 70)))]  # 용마루
    for (x, z) in [(-6.6, -5.6), (5.9, -5.6), (-6.6, 4.9), (5.9, 4.9)]:
        out.append(B(x, 4.85, z, 0.7, 0.4, 0.7, solid((70, 74, 80))))  # 처마 끝 들림
    out += [B(-4.2, 6.8, -0.3, 0.5, 0.6, 0.6, solid((70, 74, 80))), B(3.7, 6.8, -0.3, 0.5, 0.6, 0.6, solid((70, 74, 80)))]
    # 돌계단 · 장독대
    out += [B(-1, 0, 5.5, 2, 0.5, 0.8, solid((165, 160, 150)))]
    for (x, s) in [(4.2, 0.8), (5, 0.6), (4.6, 0.5)]:
        out += [B(x, 0, 5.8, s, s * 1.2, s, solid((110, 70, 45))), B(x + 0.05, s * 1.2, 5.85, s - 0.1, 0.12, s - 0.1, solid((80, 50, 30)))]
    return out


# ═══════════ 5. 모던 주택 11×9 (통유리 · 평지붕 · 수영장) ═══════════
def modern():
    out = plate(11, 13)
    white = solid((242, 242, 238))
    gray = solid((70, 75, 80))
    out += [B(-5.5, 0, -4.5, 11, 0.3, 9, solid((200, 200, 196)))]
    out += [B(-5.5, 0.3, -4.5, 11, 3.7, 9, white)]
    out += [B(-1.5, 4, -4.5, 7, 3.5, 6.5, gray)]
    big = lambda f, u, v, W, H: (40, 40, 44) if u in (0, W - 1) or v in (0, H - 1) or u % 6 == 0 else ((190, 225, 240) if (u - v) % 7 else (220, 240, 250))
    out += [B(-5, 0.6, 4.5, 6, 3, 0.12, big), B(-1, 4.3, 2, 6, 2.8, 0.12, big)]
    slats = lambda f, u, v, W, H: (170, 120, 75) if u % 2 == 0 else (130, 90, 55)
    out += [B(1.5, 0.3, 4.52, 4, 3.7, 0.12, slats)]
    out += door(2.5, 0.3, 4.6, (40, 40, 44))
    out += [B(-5.7, 4, -4.7, 11.4, 0.25, 9.4, gray), B(-1.7, 7.5, -4.7, 7.4, 0.25, 6.9, white)]
    # 옥상 정원 + 테라스 난간 유리
    out += [B(-5.3, 4.25, 2.2, 3.5, 0.3, 2, lambda f, u, v, W, H: (90, 160, 70) if (u + v) % 3 else (70, 140, 55))]
    out += [B(-5.5, 4.25, 4.4, 11, 0.9, 0.1, solid((200, 230, 240), False))]
    # 수영장 + 선베드
    out += [B(-5, -0.4, 5.5, 6, 0.45, 3, lambda f, u, v, W, H: (80, 180, 230) if (u + v) % 4 else (120, 210, 245))]
    out += [B(-5.2, 0, 5.3, 6.4, 0.08, 0.2, white), B(2, 0, 6, 1.6, 0.4, 0.7, white), B(2, 0.4, 6, 0.6, 0.5, 0.7, white)]
    return out


# ═══════════ 6. 저택 15×12 (기둥 현관 · 탑 2개 · 분수) ═══════════
def mansion():
    out = plate(19, 16)
    st = bricks((215, 205, 185), (200, 190, 170), (170, 162, 150))
    out += [B(-7.5, 0, -6, 15, 0.6, 12, solid((180, 175, 168)))]
    out += [B(-7.5, 0.6, -6, 15, 9, 12, st)]
    for y in (3.6, 6.6):
        out.append(B(-7.6, y, -6.1, 15.2, 0.25, 12.2, solid((240, 238, 230))))
    for row, y in enumerate((1.3, 4.3, 7.2)):
        for x in (-6.5, -4.6, 3.2, 5.1):
            out += window(x, y, 6, 1, 1.6)
    # 탑 2개 (원뿔 지붕 계단식)
    for tx in (-9.5, 7.5):
        out += [B(tx, 0, -2, 2, 12, 4, st)]
        for k, w in enumerate([2.8, 2.2, 1.6, 1.0, 0.5]):
            out.append(B(tx + 1 - w / 2, 12 + k * 0.7, -w, w, 0.7, w * 2, tiles((60, 70, 110))))
        out += window(tx + 0.5, 5, 2, 1, 1.4) + window(tx + 0.5, 8.5, 2, 1, 1.4)
    # 기둥 현관 + 삼각 박공
    out += [B(-3, 0.6, 6, 6, 0.4, 2.5, solid((230, 228, 220)))]
    for x in (-2.8, -1.1, 0.6, 2.3):
        out.append(B(x, 1, 7.8, 0.5, 5.5, 0.5, lambda f, u, v, W, H: (250, 248, 240) if u % 2 else (225, 222, 212)))
    out += [B(-3.3, 6.5, 6, 6.6, 0.5, 2.6, solid((240, 238, 230)))]
    for k in range(4):
        out.append(B(-3 + k * 0.8, 7 + k * 0.5, 6.1, 6 - k * 1.6, 0.5, 2.4, solid((240, 238, 230))))
    out += door(-1, 0.6, 6, (90, 40, 30)) + door(0, 0.6, 6, (90, 40, 30))
    out += [B(-1, 2.8, 6.02, 2, 0.8, 0.1, lambda f, u, v, W, H: (230, 190, 70) if (u + v) % 2 else (200, 230, 240))]
    out += gable(0, 9.6, -6, 16, 12, (60, 70, 110), steps=4)
    out += [B(-5, 11.5, -1, 1, 2.5, 1, st), B(4, 11.5, -1, 1, 2.5, 1, st)]
    # 분수 + 길
    out += [B(-0.5, -0.45, 8.5, 1, 0.5, 3, solid((200, 195, 185)))]
    out += [B(-2, 0, 11.5, 4, 0.6, 3, solid((200, 200, 195))), B(-1.6, 0.4, 11.9, 3.2, 0.22, 2.2, solid((100, 190, 235))),
            B(-0.3, 0.6, 12.7, 0.6, 1.6, 0.6, solid((210, 205, 195))), B(-0.8, 2.2, 12.2, 1.6, 0.25, 1.6, solid((210, 205, 195))),
            B(-0.25, 2.45, 12.75, 0.5, 0.6, 0.5, solid((140, 210, 245)))]
    for x in (-8.5, 6.5):
        out += [B(x, 0, 7.5, 2, 0.2, 5, lambda f, u, v, W, H: (230, 90, 120) if (u * 3 + v) % 4 == 0 else (70, 150, 60))]
    return out


# 이름 → (한글, 도안, 크기 (가로, 세로), 높이, 가격(동), 특징)
HOUSES = {
    "cabin": ("통나무 오두막", cabin, (9, 11), 8, 50_000, "1인용 · 침대 · 상자 · 작업대"),
    "log_house": ("숲속 통나무집", log_house, (11, 11), 9, 120_000, "베란다 · 벽난로 · 침대 2"),
    "brick_house": ("벽돌 2층집", brick_house, (11, 11), 13, 250_000, "2층 · 발코니 · 방 3개"),
    "hanok": ("한옥", hanok, (13, 12), 8, 400_000, "기와지붕 · 대청마루 · 장독대"),
    "modern": ("모던 주택", modern, (13, 13), 8, 600_000, "통유리 · 옥상 정원 · 수영장"),
    "mansion": ("대저택", mansion, (19, 18), 16, 1_200_000, "탑 2개 · 기둥 현관 · 분수 · 방 8개"),
}
