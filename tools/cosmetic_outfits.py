"""치장 옷 9벌 · 날개 3 · 링 2 입체 도안. 색 커스텀: 옷마다 주 색(main) · 보조 색(sub)을 바꿀 수 있음."""
import math

from cosmetic_design import SKIN, Box, mix, shade

GOLD = (240, 196, 72)
WHITE = (246, 246, 250)
BLACK = (28, 28, 34)


def cloth(base, fold=4, hem=None, hem_rows=1, collar=None, stripe=None, top=None):
    """천: 세로 주름(살짝 어두운 줄) + 아래로 갈수록 그늘 + 단(hem) · 깃(collar)"""
    def f(face, u, v, W, H):
        if face == "top":
            return top or shade(base, 1.05)
        if face == "bottom":
            return shade(base, 0.7)
        c = base
        if fold and face in ("front", "back") and u % fold == fold - 1:
            c = shade(c, 0.86)
        c = mix(c, shade(c, 0.78), v / max(1, H - 1) * 0.6)
        if collar and v == 0:
            return collar
        if hem and v >= H - hem_rows:
            return hem
        if stripe and stripe(face, u, v, W, H):
            return stripe(face, u, v, W, H)
        return c
    return f


def body(paint, grow=0.35):
    return [Box(-4, 12, -2, 8, 12, 4, paint, grow)]


def arms(paint, grow=0.35, y0=12, h=12):
    return [Box(-8, y0, -2, 4, h, 4, paint, grow), Box(4, y0, -2, 4, h, 4, paint, grow)]


def legs(paint, grow=0.35, y0=0, h=12):
    return [Box(-4, y0, -2, 4, h, 4, paint, grow), Box(0, y0, -2, 4, h, 4, paint, grow)]


def skirt(paint, y0, h, w, d, grow=0.0):
    return [Box(-w / 2, y0, -d / 2, w, h, d, paint, grow)]


# ───────── 1. 기사단 정복 ─────────
def knight(main=(28, 44, 86), sub=(180, 30, 44)):
    def coat(face, u, v, W, H):
        c = cloth(main)(face, u, v, W, H)
        if face == "front":
            if u in (3, 4) and v in (2, 5, 8):
                return GOLD
            if v == 9:
                return GOLD if u in (3, 4) else (30, 24, 22)
            if (u + v) in (7, 8) and v < 9:
                return sub
            if v == 0:
                return GOLD
        if face == "back" and v == 9:
            return (30, 24, 22)
        return c

    def sleeve(face, u, v, W, H):
        if v >= 10:
            return WHITE if v == 11 else GOLD
        return cloth(main)(face, u, v, W, H)

    def trousers(face, u, v, W, H):
        if v >= 7:
            return mix((24, 22, 24), (60, 56, 60), 0.2 if face == "front" and u == 1 else 0)
        if face in ("left", "right") and u == 1:
            return GOLD
        return cloth((54, 56, 66), fold=0)(face, u, v, W, H)

    epa = cloth(GOLD, fold=0)

    def fringe(face, u, v, W, H):
        return GOLD if u % 2 == 0 else None

    out = body(coat) + arms(sleeve) + legs(trousers)
    # 견장(어깨 위) + 술 장식
    out += [Box(-8.6, 23.6, -2.6, 5.2, 1.2, 5.2, epa), Box(3.4, 23.6, -2.6, 5.2, 1.2, 5.2, epa),
            Box(-8.6, 22.4, -2.6, 0.01, 1.2, 5.2, fringe), Box(8.6, 22.4, -2.6, 0.01, 1.2, 5.2, fringe)]
    # 코트 자락(뒤로 늘어짐)
    out += [Box(-4.4, 6, -2.7, 8.8, 6, 0.6, cloth(main, hem=GOLD))]
    # 망토
    out += [Box(-4.8, 4, -3.4, 9.6, 20, 0.6, cloth(sub, fold=3, collar=GOLD))]
    return out


# ───────── 2. 왕실 로브 ─────────
def royal(main=(140, 18, 34), sub=GOLD):
    fur = lambda face, u, v, W, H: (24, 24, 24) if (u * 5 + v * 3) % 7 == 0 else WHITE
    robe = cloth(main, fold=3, stripe=lambda f, u, v, W, H: sub if f == "front" and u in (1, W - 2) else None)
    out = body(robe) + arms(cloth(main, hem=WHITE, hem_rows=2), grow=0.6)
    out += skirt(cloth(main, fold=3, hem=sub), 0, 12, 10, 6.5)
    out += [Box(-5, 21.5, -2.8, 10, 2.8, 5.6, fur)]  # 털 망토 깃
    out += [Box(-5.2, 1, -3.6, 10.4, 21, 0.7, cloth(main, fold=3, hem=fur(None, 1, 1, 1, 1), collar=WHITE))]  # 뒤 망토
    out += [Box(-0.8, 21.2, 2.6, 1.6, 1.6, 0.6, lambda *a: sub)]  # 브로치
    return out


# ───────── 3. 그림자 닌자복 ─────────
def ninja(main=(24, 24, 32), sub=(170, 24, 36)):
    gi = cloth(main, fold=4, stripe=lambda f, u, v, W, H: shade(main, 1.4) if f == "front" and u == min(7, 2 + v // 2) and v < 7 else None)

    def belt(face, u, v, W, H):
        return sub if v in (8, 9) else gi(face, u, v, W, H)

    wrap = lambda face, u, v, W, H: (96, 96, 108) if v >= 7 and v % 2 == 0 else cloth(main, fold=0)(face, u, v, W, H)
    out = body(belt) + arms(wrap) + legs(wrap)
    # 목도리 + 뒤로 날리는 꼬리
    out += [Box(-4.6, 22.4, -2.6, 9.2, 1.8, 5.2, cloth(sub, fold=0)),
            Box(1, 12, -3.2, 2, 10.5, 0.6, cloth(sub, fold=0), rot=("x", 0.35, (2, 22, -2.6))),
            Box(-2.5, 14, -3.0, 2, 8, 0.6, cloth(shade(sub, 0.85), fold=0), rot=("x", 0.2, (-1.5, 22, -2.4)))]
    # 허리 매듭
    out += [Box(1.5, 13.2, 2.3, 2, 2, 0.8, cloth(sub, fold=0))]
    return out


# ───────── 4. 천사의 드레스 ─────────
def angel(main=WHITE, sub=(140, 200, 255)):
    def top(face, u, v, W, H):
        if v == 7:
            return GOLD
        if face == "front" and u + v in (4, 5) and v < 7:
            return sub
        return cloth(main, fold=0)(face, u, v, W, H)

    out = body(top)
    out += arms(cloth(main, fold=0, hem=GOLD), grow=0.6, y0=19, h=5)  # 퍼프 소매
    out += skirt(cloth(main, fold=2, hem=GOLD), 7, 5, 10, 6.5)
    out += skirt(cloth(main, fold=2, hem=GOLD), 1, 6, 12, 8.5)
    # 등 리본
    out += [Box(-3, 16, -3.2, 2.6, 2.2, 0.8, cloth(sub, fold=0)), Box(0.4, 16, -3.2, 2.6, 2.2, 0.8, cloth(sub, fold=0)),
            Box(-0.5, 16.4, -3.5, 1, 1.4, 1, cloth(shade(sub, 0.85), fold=0)),
            Box(-1.8, 10, -4.1, 1.2, 6, 0.4, cloth(sub, fold=0)), Box(0.6, 10, -4.1, 1.2, 6, 0.4, cloth(sub, fold=0))]
    return out


# ───────── 5. 수영복 (원피스 + 밀짚모자) ─────────
def swimsuit(main=(30, 120, 210), sub=WHITE):
    def suit(face, u, v, W, H):
        if v < 2:
            # 어깨끈 두 줄만, 나머지는 살결
            return cloth(main, fold=0)(face, u, v, W, H) if face in ("front", "back") and u in (1, 6) else SKIN
        if v == 2 and face == "front" and 2 <= u <= 5:
            return SKIN
        if face in ("left", "right") and u in (1, 2):
            return sub  # 옆선
        return cloth(main, fold=0)(face, u, v, W, H)

    def bottom(face, u, v, W, H):
        if v <= 1:
            return cloth(main, fold=0)(face, u, v, W, H) if face != "left" and face != "right" or u != 1 else sub
        return None

    straw = (226, 196, 120)
    hat = lambda face, u, v, W, H: (206, 172, 100) if (u + v) % 4 == 0 else straw
    band = lambda face, u, v, W, H: main
    return body(suit, 0.25) + legs(bottom, 0.25) + [
        Box(-6.5, 31.6, -6.5, 13, 0.8, 13, hat),  # 챙
        Box(-4.4, 32.2, -4.4, 8.8, 2.4, 8.8, hat),  # 머리 부분
        Box(-4.5, 32.2, -4.5, 9, 0.8, 9, band),  # 띠
    ]


# ───────── 6. 메이드복 ─────────
def maid(main=(30, 30, 38), sub=WHITE):
    def dress(face, u, v, W, H):
        if face == "front" and v <= 1 and 2 <= u <= 5:
            return sub  # 둥근 깃
        if face == "front" and v == 2 and u in (3, 4):
            return (190, 30, 50)  # 리본
        return cloth(main, fold=4)(face, u, v, W, H)

    frill = lambda face, u, v, W, H: sub if (v < H - 1 or u % 2 == 0) else None
    apron = lambda face, u, v, W, H: (sub if face == "front" else shade(sub, 0.9)) if not (v == H - 1 and u % 2) else None
    sock = lambda face, u, v, W, H: SKIN if 4 <= v <= 5 else (sub if v < 10 else (24, 20, 22))
    out = body(dress) + arms(cloth(main, fold=0, hem=sub), grow=0.7, y0=19, h=5)
    out += skirt(cloth(main, fold=2), 5, 7, 11, 7.5)
    out += skirt(frill, 4, 1.2, 11.6, 8)  # 속치마 프릴
    out += [Box(-3, 6, 3.8, 6, 10, 0.4, apron),  # 앞치마
            Box(-3.6, 15.6, -2.6, 7.2, 1, 5.2, lambda *a: sub),  # 앞치마 허리끈
            Box(-1.5, 15, -3.2, 3, 2, 0.8, lambda *a: sub),  # 뒤 리본
            Box(-4.4, 31.6, -1.2, 8.8, 1.4, 1.6, frill)]  # 머리띠(프릴)
    out += legs(sock, 0.25)
    return out


# ───────── 7. 세일러 교복 ─────────
def sailor(main=WHITE, sub=(30, 44, 90)):
    def blouse(face, u, v, W, H):
        if face == "front" and v <= 3 and abs(u - 3.5) >= 3 - v:
            return sub  # V자 세일러 깃
        if face == "front" and v == 3 and u in (3, 4):
            return (196, 30, 44)  # 스카프
        return cloth(main, fold=0)(face, u, v, W, H)

    pleat = lambda face, u, v, W, H: shade(sub, 0.8) if u % 2 else sub
    sock = lambda face, u, v, W, H: SKIN if v < 5 else (sub if v < 10 else (30, 22, 20))
    out = body(blouse) + arms(cloth(main, fold=0, hem=sub, hem_rows=2), grow=0.35)
    out += [Box(-4.5, 19.5, -2.7, 9, 4.5, 0.5, cloth(sub, fold=0, stripe=lambda f, u, v, W, H: WHITE if v == 3 else None))]  # 뒤 세일러 깃
    out += [Box(-1, 18.5, 2.3, 2, 2.6, 0.6, lambda *a: (196, 30, 44))]  # 앞 스카프 매듭
    out += skirt(pleat, 6, 6, 10.5, 7)
    out += legs(sock, 0.25)
    return out


# ───────── 8. 한복 ─────────
def hanbok(main=(232, 88, 120), sub=(250, 214, 90)):
    saek = [(230, 60, 60), (250, 200, 60), (80, 180, 90), (70, 120, 220), (180, 90, 200)]

    def jeogori(face, u, v, W, H):
        if face == "front" and v <= 4 and (u == 3 or u == 4 or abs(u - 3.5) < 4.5 - v) and v < 2:
            return WHITE  # 동정(흰 깃)
        if face == "front" and abs(u - (2 + v)) < 1 and v < 5:
            return shade(sub, 0.85)
        return cloth(sub, fold=0)(face, u, v, W, H)

    def sleeve(face, u, v, W, H):
        if v >= 6 and v < 11:
            return saek[(v - 6) % 5]  # 색동
        if v >= 11:
            return WHITE
        return cloth(sub, fold=0)(face, u, v, W, H)

    chima = cloth(main, fold=2)
    out = [Box(-4, 18, -2, 8, 6, 4, jeogori, 0.45)] + arms(sleeve, grow=0.55)
    out += skirt(chima, 0, 18.4, 10, 6.5)  # 가슴부터 발까지 치마
    out += skirt(lambda f, u, v, W, H: shade(main, 1.15), 17.2, 1.2, 10.4, 6.9)  # 치마 말기
    out += [Box(0.6, 12, 3.3, 1.4, 6.2, 0.5, lambda *a: (200, 30, 60)),  # 고름
            Box(1.8, 12.5, 3.3, 1.2, 5.5, 0.5, lambda *a: (200, 30, 60)),
            Box(0.4, 17, 3.3, 2.6, 1.4, 0.8, lambda *a: (220, 40, 70))]
    return out


# ───────── 9. 마법사 로브 ─────────
def wizard(main=(70, 40, 140), sub=GOLD):
    star = lambda f, u, v, W, H: sub if f in ("front", "back") and (u * 7 + v * 5) % 23 == 0 else None
    robe = cloth(main, fold=3, stripe=star, collar=sub)
    out = body(robe) + arms(cloth(main, fold=0, hem=sub), grow=0.8)
    out += skirt(cloth(main, fold=3, stripe=star, hem=sub), 0, 12, 9.6, 6)
    out += [Box(-4.6, 14.4, -2.6, 9.2, 1.2, 5.2, lambda *a: (90, 56, 30)),  # 허리띠
            Box(2.4, 12.4, 2.6, 2, 2.4, 1.2, lambda *a: (120, 220, 255))]  # 물약 주머니
    # 뾰족 모자 (층층이 좁아짐)
    hat = cloth(main, fold=0, stripe=star)
    out += [Box(-6, 31.6, -6, 12, 0.8, 12, hat), Box(-4.4, 32.2, -4.4, 8.8, 1.2, 8.8, lambda *a: sub)]
    w = 8.0
    y = 33.4
    for k in range(5):
        w -= 1.4
        out.append(Box(-w / 2 + k * 0.35, y, -w / 2 - k * 0.2, w, 1.6, w, hat))
        y += 1.6
    return out


OUTFITS = [("기사단 정복", knight), ("왕실 로브", royal), ("그림자 닌자복", ninja), ("천사의 드레스", angel),
           ("수영복 (+밀짚모자)", swimsuit), ("메이드복", maid), ("세일러 교복", sailor), ("한복", hanbok), ("마법사 로브 (+모자)", wizard)]


# ───────── 날개 (깃털 층층이) ─────────
def wings(kind):
    out = []
    pal = {"angel": [(255, 255, 255), (224, 230, 242), (250, 214, 110)],
           "demon": [(130, 16, 30), (70, 8, 16), (24, 10, 14)],
           "dragon": [(80, 30, 140), (36, 12, 70), (110, 255, 240)]}[kind]
    for side in (-1, 1):
        piv = (side * 1.0, 22, -2.4)
        # 뼈대(위쪽 가장자리)
        out.append(Box(min(0, side * 15) + side * 1, 21, -3.6, 15, 1.4, 1.2, lambda *a: pal[2],
                       rot=("z", side * 0.35, piv)))
        # 깃털 줄: 바깥쪽일수록 짧고 위로
        n = 7
        for i in range(n):
            length = 15 - i * 1.3 if kind != "demon" else 13 - i * 0.6
            x = side * (1.5 + i * 2.0) - (2 if side < 0 else 0)
            col = mix(pal[0], pal[1], i / n)

            def feather(face, u, v, W, H, col=col, kind=kind):
                if kind == "dragon" and (u + v) % 4 == 0:
                    return pal[2]
                if kind == "demon" and u == 0:
                    return pal[2]
                return mix(col, shade(col, 0.82), v / max(1, H - 1))

            out.append(Box(x, 21.5 - length, -3.3 - i * 0.12, 2, length, 0.7, feather,
                           rot=("z", side * (0.35 + i * 0.05), piv)))
    return out


def halo(kind):
    out = []
    col = (255, 220, 110) if kind == "gold" else (200, 240, 255)
    hi = (255, 250, 210) if kind == "gold" else (255, 255, 255)
    n = 16
    for i in range(n):
        a = i / n * math.pi * 2
        x, z = math.cos(a) * 5.2, math.sin(a) * 5.2
        out.append(Box(x - 0.7, 34.5, z - 0.7, 1.4, 0.9, 1.4, lambda face, u, v, W, H: hi if face == "top" else col))
    return out
