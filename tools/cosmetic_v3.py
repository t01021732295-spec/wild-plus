"""3차 도안: '진짜 입은 옷' — 몸에서 떨어진 두께, 깃·소매단·벨트·단추·주머니를 따로 튀어나온 부품으로 쌓고,
치마·망토·코트 자락은 펄럭이는 볼륨으로. 동물 모자 5종.
좌표: x 오른쪽(+x = 플레이어 왼쪽), y 위, z 앞(+z). 몸통 앞면 z=2, 발바닥 y=0, 정수리 y=32."""
import math

from cosmetic_design import SKIN, Box, mix, shade
from cosmetic_outfits import GOLD, WHITE, BLACK, cloth

G = 0.9  # 옷 두께 (몸에서 떨어진 정도)


def C(c):
    return lambda *a: c


def tex(base, fold=3, dark=0.5, hl=True):
    """천 질감: 위쪽 밝고 아래 어두움, 세로 주름, 윗면 밝게"""
    def f(face, u, v, W, H):
        if face == "top":
            return shade(base, 1.12) if hl else base
        if face == "bottom":
            return shade(base, 0.62)
        c = mix(shade(base, 1.06), shade(base, 0.8), v / max(1, H - 1) * dark)
        if fold and face in ("front", "back") and u % fold == fold - 1:
            c = shade(c, 0.86)
        if face in ("left", "right"):
            c = shade(c, 0.94)
        return c
    return f


def torso(paint, g=G, y0=12, h=12):
    return Box(-4, y0, -2, 8, h, 4, paint, g)


def sleeve(paint, g=G, y0=12, h=12, puff=0.0):
    return [Box(-8, y0, -2, 4, h, 4, paint, g + puff), Box(4, y0, -2, 4, h, 4, paint, g + puff)]


def pants(paint, g=G, y0=0, h=12):
    return [Box(-4, y0, -2, 4, h, 4, paint, g), Box(0, y0, -2, 4, h, 4, paint, g)]


def boots(c, h=4, g=1.1, top_c=None):
    out = []
    for x in (-4, 0):
        out.append(Box(x, 0, -2, 4, h, 4, tex(c, fold=0), g))
        out.append(Box(x - g - 0.25, h - 0.6, -2 - g - 0.25, 4 + 2 * g + 0.5, 0.9, 4 + 2 * g + 0.5, C(top_c or shade(c, 1.2))))  # 부츠 입구 테
        out.append(Box(x, 0, 1.6, 4, 1.2, 1.6, tex(c, fold=0), g))  # 앞코
    return out


def cuffs(c, y=12, h=1.6, g=1.35):
    return [Box(-8, y, -2, 4, h, 4, tex(c, fold=0), g), Box(4, y, -2, 4, h, 4, tex(c, fold=0), g)]


def belt(c, y=13.5, h=1.4, buckle=GOLD, g=1.25):
    return [Box(-4, y, -2, 8, h, 4, tex(c, fold=0), g), Box(-1.2, y - 0.3, 2 + g, 2.4, h + 0.6, 0.6, C(buckle))]


def buttons(xs, ys, c=GOLD, z=2 + G):
    return [Box(x - 0.45, y - 0.45, z, 0.9, 0.9, 0.5, C(c)) for x in xs for y in ys]


def tiers(spec, paint_fn):
    """층층 치마: [(y, 높이, 폭, 깊이, 색)]"""
    return [Box(-w / 2, y, -d / 2, w, h, d, paint_fn(c)) for y, h, w, d, c in spec]


def cape(c, y0=2, h=21, w=10.5, lining=None, collar=None):
    out = [Box(-w / 2, y0, -2 - G - 1.4, w, h, 0.8, tex(c, fold=2), rot=("x", 0.10, (0, y0 + h, -2 - G - 1)))]
    if lining:
        out.append(Box(-w / 2 + 0.3, y0 + 0.3, -2 - G - 0.7, w - 0.6, h - 0.6, 0.2, C(lining), rot=("x", 0.10, (0, y0 + h, -2 - G - 1))))
    if collar:
        out.append(Box(-w / 2 - 0.3, y0 + h - 1.2, -2 - G - 1.6, w + 0.6, 2.2, 6.6, tex(collar, fold=0)))
    return out


# ───────── 1. 기사단 정복 ─────────
def knight(main=(28, 44, 86), sub=(170, 28, 40)):
    out = [torso(tex(main)), *sleeve(tex(main))]
    out += [Box(-4, 6.5, -2, 8, 6, 4, tex(main, fold=2), G + 0.5)]  # 코트 아랫자락 (허벅지까지)
    out += [Box(-1.8, 12.5, 2 + G, 3.6, 11.5, 0.5, tex(shade(main, 1.15), fold=0))]  # 앞 여밈판
    out += buttons([-1, 1], [22, 19.5, 17, 14.5])
    out += [Box(-3.8, 21, 2 + G + 0.2, 7.6, 0.8, 0.4, C(sub), rot=("z", 0.75, (0, 18, 0)))]  # 어깨띠
    out += belt((34, 26, 22), y=12.6)
    out += [Box(-4.6, 23.2, -2.6, 9.2, 2.2, 5.2, tex(main, fold=0)), Box(-4.7, 24.6, -2.7, 9.4, 0.5, 5.4, C(GOLD))]  # 세운 깃
    # 견장 (두툼한 판 + 술)
    for sx in (-1, 1):
        x = -9.3 if sx < 0 else 3.7
        out += [Box(x, 24.2, -2.8, 5.6, 1.2, 5.6, tex(GOLD, fold=0))]
        out += [Box(x + i * 1.1, 22.3, -2.8 if j == 0 else 2.0, 0.5, 2, 0.8, C(GOLD)) for i in range(5) for j in (0, 1)]
    out += cuffs(WHITE, y=12, h=1.4) + cuffs(GOLD, y=13.4, h=0.6, g=1.4)
    out += pants(tex((52, 54, 64), fold=0), g=0.6)
    out += [Box(-4.7, 4, -0.4, 0.4, 8, 0.8, C(GOLD)), Box(4.3, 4, -0.4, 0.4, 8, 0.8, C(GOLD))]  # 바지 금줄
    out += boots((24, 20, 20), h=5.5, top_c=(60, 50, 44))
    out += cape(sub, y0=6, h=18, lining=shade(sub, 0.6))
    return out


# ───────── 2. 왕실 로브 ─────────
def royal(main=(140, 18, 34), sub=GOLD):
    fur = lambda f, u, v, W, H: (24, 24, 24) if (u * 5 + v * 3) % 7 == 0 else WHITE
    out = [torso(tex(main, fold=0)), *sleeve(tex(main, fold=0), puff=0.3)]
    out += [Box(-8, 12, -2, 4, 5, 4, tex(main, fold=2), 2.0), Box(4, 12, -2, 4, 5, 4, tex(main, fold=2), 2.0)]  # 넓은 소맷부리
    out += [Box(-8, 12, -2, 4, 1.2, 4, fur, 2.3), Box(4, 12, -2, 4, 1.2, 4, fur, 2.3)]
    # 바닥까지 끌리는 로브 (아래로 넓어짐)
    out += tiers([(8, 4.5, 10.4, 6.6, main), (4, 4.5, 11.6, 7.6, main), (0, 4.5, 12.8, 8.6, main)], lambda c: tex(c, fold=2))
    out += [Box(-6.4, 0, -4.3, 12.8, 1.2, 8.6, fur)]  # 털 밑단
    out += [Box(-0.6, 0.2, 2 + 2.2, 1.2, 12.5, 0.4, C(sub)), Box(-3.4, 0.2, 2 + 2.0, 0.6, 12, 0.4, C(sub)), Box(2.8, 0.2, 2 + 2.0, 0.6, 12, 0.4, C(sub))]
    out += [Box(-5.4, 21, -3.2, 10.8, 3.6, 6.4, fur)]  # 털 망토 어깨
    out += [Box(-1.2, 19.4, 3.2, 2.4, 2.4, 0.8, C(sub)), Box(-0.5, 20.1, 4.0, 1, 1, 0.3, C((220, 30, 60)))]  # 브로치
    out += cape(main, y0=0.5, h=21, w=12, lining=sub)
    return out


# ───────── 3. 그림자 닌자복 ─────────
def ninja(main=(26, 26, 34), sub=(170, 24, 36)):
    out = [torso(tex(main)), *sleeve(tex(main), g=1.0), *pants(tex(main, fold=2), g=1.0)]
    # 겹쳐 여민 앞섶 (대각선 판 2장)
    out += [Box(-3.6, 15, 2 + G, 5, 8.6, 0.5, tex(shade(main, 1.3), fold=0), rot=("z", -0.35, (0, 19, 0)))]
    out += [Box(-4.3, 13, -2, 8.6, 2.2, 4.6, tex(sub, fold=0), 1.0)]  # 허리띠
    out += [Box(1.4, 9.5, 2 + 1.6, 1.6, 4, 0.5, C(sub), rot=("z", 0.25, (2, 13, 0))), Box(2.8, 10, 2 + 1.6, 1.6, 3.5, 0.5, C(shade(sub, 0.8)), rot=("z", -0.2, (3, 13, 0)))]
    # 팔 보호대 · 정강이 감개 (튀어나온 띠)
    for x in (-8, 4):
        out += [Box(x, 12.5 + k * 1.6, -2, 4, 0.9, 4, C((80, 80, 92)), 1.45) for k in range(3)]
    for x in (-4, 0):
        out += [Box(x, 1.5 + k * 1.6, -2, 4, 0.9, 4, C((80, 80, 92)), 1.35) for k in range(4)]
    # 두꺼운 목도리 + 바람에 날리는 꼬리
    out += [Box(-4.8, 22, -2.8, 9.6, 2.8, 5.6, tex(sub, fold=0))]
    out += [Box(0.5, 10, -4.6, 2.4, 12, 0.7, tex(sub, fold=0), rot=("x", 0.55, (1.7, 22.5, -3)))]
    out += [Box(-2.8, 12, -4.3, 2.2, 10, 0.7, tex(shade(sub, 0.82), fold=0), rot=("x", 0.35, (-1.7, 22.5, -3)))]
    out += boots((40, 40, 48), h=2.5)
    return out


# ───────── 4. 천사의 드레스 ─────────
def angel(main=WHITE, sub=(140, 200, 255)):
    out = [torso(tex(main, fold=0), y0=14, h=10)]
    out += [Box(-8, 19.5, -2, 4, 4.5, 4, tex(main, fold=0), 1.7), Box(4, 19.5, -2, 4, 4.5, 4, tex(main, fold=0), 1.7)]  # 퍼프 소매
    out += [Box(-8, 19.4, -2, 4, 0.6, 4, C(GOLD), 1.75), Box(4, 19.4, -2, 4, 0.6, 4, C(GOLD), 1.75)]
    out += [Box(-4.6, 14.2, -2.6, 9.2, 1.0, 5.2, C(GOLD))]  # 금 허리띠
    out += [Box(-3.8, 21.5, 2 + G, 7.6, 0.6, 0.4, C(GOLD)), Box(-0.6, 15.5, 2 + G + 0.1, 1.2, 6, 0.4, C(sub))]
    out += tiers([(9.5, 4.8, 10.4, 6.8, main), (5, 4.8, 12.4, 8.6, main), (0.3, 5, 14.6, 10.4, main)], lambda c: tex(c, fold=2, dark=0.35))
    out += [Box(-7.3, 0.3, -5.2, 14.6, 0.7, 10.4, C(GOLD)), Box(-6.2, 5, -4.3, 12.4, 0.6, 8.6, C(sub))]
    # 큰 등 리본
    out += [Box(-4.2, 15, -5.2, 3.8, 3.2, 1.2, tex(sub, fold=0), rot=("z", 0.25, (-0.3, 16.6, 0))),
            Box(0.4, 15, -5.2, 3.8, 3.2, 1.2, tex(sub, fold=0), rot=("z", -0.25, (0.3, 16.6, 0))),
            Box(-0.8, 15.8, -5.4, 1.6, 1.6, 1.4, C(shade(sub, 0.8))),
            Box(-2.2, 8, -4.8, 1.4, 7.5, 0.5, C(sub), rot=("z", -0.12, (-1.5, 15.5, 0))), Box(0.8, 8, -4.8, 1.4, 7.5, 0.5, C(sub), rot=("z", 0.12, (1.5, 15.5, 0)))]
    return out


# ───────── 5. 수영복 ─────────
def swimsuit(main=(30, 120, 210), sub=WHITE):
    out = [Box(-4, 12, -2, 8, 9.5, 4, tex(main, fold=0, dark=0.25), 0.45)]
    out += [Box(-3.2, 21, -2.45, 1.2, 3, 4.9, C(main)), Box(2.0, 21, -2.45, 1.2, 3, 4.9, C(main))]  # 어깨끈
    out += [Box(-4.5, 14, -1.2, 0.3, 7, 2.4, C(sub)), Box(4.2, 14, -1.2, 0.3, 7, 2.4, C(sub))]  # 옆 줄무늬
    out += [Box(-4.45, 10.6, -2.45, 8.9, 1.8, 4.9, tex(main, fold=0))]  # 엉덩이 부분
    out += [Box(-3.6, 10.2, 2.3, 3.4, 1.4, 0.5, C(main)), Box(0.2, 10.2, 2.3, 3.4, 1.4, 0.5, C(main))]
    # 밀짚모자: 넓은 챙 + 둥근 머리 + 리본 + 꽃
    straw = (230, 200, 126)
    weave = lambda f, u, v, W, H: shade(straw, 0.88) if (u + v) % 3 == 0 else straw
    out += [Box(-7.5, 31.4, -7.5, 15, 0.9, 15, weave), Box(-4.6, 32.2, -4.6, 9.2, 2.6, 9.2, weave), Box(-3.8, 34.6, -3.8, 7.6, 0.8, 7.6, weave),
            Box(-4.7, 32.2, -4.7, 9.4, 0.9, 9.4, C(main)), Box(3.6, 32.0, 3.2, 2, 2, 1.6, C((255, 120, 150)))]
    # 샌들
    for x in (-4, 0):
        out += [Box(x - 0.3, 0, -2.3, 4.6, 0.5, 4.8, C((200, 150, 100))), Box(x + 1.5, 0.5, -0.5, 1, 1, 3, C(main))]
    return out


# ───────── 6. 메이드복 ─────────
def maid(main=(30, 30, 38), sub=WHITE):
    out = [torso(tex(main), y0=13), *sleeve(tex(main, fold=0), y0=12, h=7)]
    out += [Box(-8, 18.4, -2, 4, 5.6, 4, tex(main, fold=0), 1.9), Box(4, 18.4, -2, 4, 5.6, 4, tex(main, fold=0), 1.9)]  # 퍼프 소매
    out += cuffs(sub, y=12, h=1.6, g=1.2)
    out += [Box(-2.4, 14, 2 + G, 4.8, 9, 0.5, tex(sub, fold=0))]  # 흰 앞판
    out += buttons([0], [21, 18.8, 16.6], c=(40, 40, 48), z=2 + G + 0.5)
    crown = lambda f, u, v, W, H: sub if (v > 0 or u % 2 == 0) else None
    out += [Box(-4.8, 23.2, -2.8, 9.6, 2.4, 5.6, crown), Box(-1, 22, 2 + 1.4, 2, 1.6, 0.8, C((190, 30, 50)))]  # 깃 프릴 + 리본
    # 층층 치마 (흰 프릴 4단) + 검은 밑단
    out += tiers([(10.6, 2.6, 10.2, 6.6, sub), (8.2, 2.6, 11.6, 8.0, sub), (5.8, 2.6, 13.0, 9.4, sub), (3.4, 2.6, 14.4, 10.8, sub)],
                 lambda c: tex(c, fold=2, dark=0.3))
    out += [Box(-7.8, 1.2, -6.0, 15.6, 2.4, 12, tex(main, fold=3))]
    out += [Box(-1.6, 13.4, -2 - G - 1.0, 3.2, 2.2, 1.0, C(sub)), Box(-3.8, 13.6, -2 - G - 0.9, 2.4, 1.8, 0.6, C(sub), rot=("z", 0.3, (-1.6, 14.5, 0))),
            Box(1.4, 13.6, -2 - G - 0.9, 2.4, 1.8, 0.6, C(sub), rot=("z", -0.3, (1.6, 14.5, 0)))]  # 뒤 리본
    out += [Box(-4.6, 31.4, -1.4, 9.2, 1.6, 2, crown), Box(-4.7, 31.4, -1.5, 9.4, 0.6, 2.2, C(main))]  # 머리띠
    out += pants(C(sub), g=0.35, h=3) + boots((24, 20, 22), h=1.4)
    return out


# ───────── 7. 세일러 교복 ─────────
def sailor(main=WHITE, sub=(30, 44, 90)):
    out = [torso(tex(main, fold=0)), *sleeve(tex(main, fold=0), g=0.9)]
    out += cuffs(sub, y=12, h=1.4, g=1.2)
    out += [Box(-4.8, 19.6, -2 - G - 0.5, 9.6, 4.6, 0.6, tex(sub, fold=0)), Box(-4.4, 20.0, -2 - G - 0.6, 8.8, 0.4, 0.2, C(WHITE))]  # 뒤 세일러 깃
    out += [Box(-4.6, 22.6, -2.8, 9.2, 1.4, 5.6, tex(sub, fold=0))]
    out += [Box(-3.4, 19, 2 + G, 1.2, 4.2, 0.4, C(sub), rot=("z", -0.4, (-2.8, 23, 0))), Box(2.2, 19, 2 + G, 1.2, 4.2, 0.4, C(sub), rot=("z", 0.4, (2.8, 23, 0)))]
    out += [Box(-1.1, 18.2, 2 + G + 0.2, 2.2, 1.8, 0.8, C((200, 30, 44))), Box(-0.9, 15, 2 + G + 0.3, 0.8, 3.4, 0.4, C((200, 30, 44)), rot=("z", 0.15, (0, 18, 0))),
            Box(0.1, 15.2, 2 + G + 0.3, 0.8, 3.2, 0.4, C((200, 30, 44)), rot=("z", -0.15, (0, 18, 0)))]
    pleat = lambda c: (lambda f, u, v, W, H: shade(c, 0.78) if u % 2 else c)
    out += tiers([(6.5, 6, 11, 7.4, sub)], pleat)
    out += pants(lambda f, u, v, W, H: SKIN if v < 5 else (sub if v < 11 else (30, 22, 20)), g=0.45)
    out += boots((30, 22, 20), h=1.4)
    return out


# ───────── 8. 한복 ─────────
def hanbok(main=(232, 88, 120), sub=(250, 214, 90)):
    saek = [(230, 60, 60), (250, 200, 60), (80, 180, 90), (70, 120, 220), (180, 90, 200)]
    out = [Box(-4, 17.5, -2, 8, 6.5, 4, tex(sub, fold=0), 1.0)]  # 짧은 저고리
    for x in (-8, 4):
        out += [Box(x, 12 + 6 + k * 1.0, -2, 4, 1.0, 4, C(saek[k % 5]), 1.15 + 0.08 * k) for k in range(5)]  # 색동 소매
        out += [Box(x, 12, -2, 4, 6, 4, tex(sub, fold=0), 1.05), Box(x, 12, -2, 4, 1, 4, C(WHITE), 1.15)]
    out += [Box(-2.2, 21.4, 2 + 1.1, 4.4, 2.6, 0.4, C(WHITE)), Box(-2.6, 17.6, 2 + 1.1, 3.6, 5.6, 0.4, tex(shade(sub, 0.88), fold=0), rot=("z", -0.4, (0, 23, 0)))]  # 동정 · 깃
    # 풍성한 치마: 가슴부터 바닥까지 둥글게 퍼짐
    out += tiers([(13.5, 4.5, 10.2, 6.8, main), (9, 4.5, 11.8, 8.2, main), (4.5, 4.5, 13.2, 9.6, main), (0, 4.5, 14.4, 10.8, main)],
                 lambda c: tex(c, fold=2, dark=0.3))
    out += [Box(-5.3, 17.0, -3.6, 10.6, 1.4, 7.2, C(shade(main, 1.18)))]  # 치마 말기
    out += [Box(0.4, 9.5, 3 + 1.2, 1.4, 7.5, 0.5, C((200, 30, 60))), Box(1.9, 10.5, 3 + 1.2, 1.2, 6.5, 0.5, C((200, 30, 60))),
            Box(0.2, 16.6, 3 + 1.1, 2.8, 1.6, 0.9, C((220, 40, 70)))]  # 고름
    return out


# ───────── 9. 마법사 로브 ─────────
def wizard(main=(70, 40, 140), sub=GOLD):
    star = lambda c: (lambda f, u, v, W, H: sub if f in ("front", "back") and (u * 7 + v * 5) % 23 == 0 else tex(c, fold=3)(f, u, v, W, H))
    out = [torso(star(main)), *sleeve(star(main), g=0.9, y0=17, h=7)]
    out += [Box(-8, 12, -2, 4, 5.5, 4, star(main), 2.2), Box(4, 12, -2, 4, 5.5, 4, star(main), 2.2)]  # 넓게 늘어진 소매
    out += [Box(-8, 12, -2, 4, 0.7, 4, C(sub), 2.3), Box(4, 12, -2, 4, 0.7, 4, C(sub), 2.3)]
    out += tiers([(6, 6.5, 10.6, 6.8, main), (0, 6.5, 12.6, 8.4, main)], star)
    out += [Box(-6.3, 0, -4.2, 12.6, 0.8, 8.4, C(sub))]
    out += belt((96, 60, 32), y=13.2) + [Box(2.6, 10.6, 2 + 1.6, 2.2, 2.6, 1.4, C((120, 220, 255))), Box(3.2, 13.2, 2 + 1.9, 1, 0.6, 0.8, C((180, 140, 90)))]
    out += [Box(-4.8, 21.5, -2.8, 9.6, 2.6, 5.6, tex(shade(main, 0.85), fold=0)), Box(-5, 20.5, -3.6, 10, 3.6, 1.2, tex(shade(main, 0.8), fold=0))]  # 두건(목 뒤)
    hat = star(main)
    out += [Box(-6.5, 31.4, -6.5, 13, 0.9, 13, hat), Box(-4.4, 32.2, -4.4, 8.8, 1.3, 8.8, C(sub))]
    w, y = 8.4, 33.5
    for k in range(6):
        w -= 1.25
        out.append(Box(-w / 2 + k * 0.45, y, -w / 2 - k * 0.35, w, 1.7, w, hat))
        y += 1.7
    out.append(Box(1.8, y - 0.4, -2.6, 1.4, 1.4, 1.4, C((255, 240, 150))))
    return out


OUTFITS3 = [("기사단 정복", knight), ("왕실 로브", royal), ("그림자 닌자복", ninja), ("천사의 드레스", angel), ("수영복 + 밀짚모자", swimsuit),
            ("메이드복", maid), ("세일러 교복", sailor), ("한복", hanbok), ("마법사 로브 + 모자", wizard)]


# ───────── 동물 모자 (머리 전체를 덮는 탈) ─────────
def _eyes(c=(24, 24, 30), hi=WHITE, y=27.5, z=4.95, gap=2.4, size=1.4):
    return [Box(-gap - size / 2, y, z, size, size * 1.2, 0.5, C(c)), Box(gap - size / 2, y, z, size, size * 1.2, 0.5, C(c)),
            Box(-gap - size / 2 + 0.2, y + size * 0.7, z + 0.4, 0.5, 0.5, 0.2, C(hi)), Box(gap - size / 2 + 0.2, y + size * 0.7, z + 0.4, 0.5, 0.5, 0.2, C(hi))]


def head_shell(c, g=1.12):  # 투구(1.0)보다 살짝 바깥
    return Box(-4, 24, -4, 8, 8, 8, tex(c, fold=0, dark=0.25), g)


def duck():
    yel, org = (255, 214, 60), (255, 140, 30)
    return [head_shell(yel), Box(-2.6, 25.4, 4.9, 5.2, 1.4, 3.2, tex(org, fold=0)), Box(-2.4, 26.6, 4.9, 4.8, 0.8, 2.8, C(shade(org, 1.1))),
            Box(-0.6, 33, -0.6, 1.2, 1.6, 1.2, C(yel)), Box(-0.2, 34.2, -1.4, 0.8, 1, 1.6, C(yel))] + _eyes(y=28.6)


def bunny():
    wht, pink = (246, 246, 250), (255, 170, 190)
    out = [head_shell(wht)]
    for sx in (-1, 1):
        out += [Box(sx * 2.6 - 1.2, 33, -0.8, 2.4, 8, 1.6, tex(wht, fold=0), rot=("z", -sx * 0.18, (sx * 2.6, 33, 0))),
                Box(sx * 2.6 - 0.7, 33.8, 0.85, 1.4, 6.4, 0.2, C(pink), rot=("z", -sx * 0.18, (sx * 2.6, 33, 0)))]
    out += [Box(-0.6, 26.2, 4.95, 1.2, 0.8, 0.4, C(pink)), Box(-2.8, 25.4, 4.95, 1.2, 0.8, 0.3, C((255, 200, 210))), Box(1.6, 25.4, 4.95, 1.2, 0.8, 0.3, C((255, 200, 210)))]
    return out + _eyes(c=(200, 40, 70), y=27.6)


def cat():
    gr, pink = (120, 120, 132), (255, 170, 190)
    out = [head_shell(gr)]
    for sx in (-1, 1):
        out += [Box(sx * 3 - 1.4, 32.8, -1, 2.8, 2.8, 1.6, tex(gr, fold=0), rot=("z", -sx * 0.5, (sx * 3, 33, 0))),
                Box(sx * 3 - 0.7, 33.2, 0.6, 1.4, 1.6, 0.2, C(pink), rot=("z", -sx * 0.5, (sx * 3, 33, 0)))]
    out += [Box(-0.5, 26.4, 4.95, 1, 0.7, 0.4, C(pink))]
    out += [Box(-4.8 if sx < 0 else 3.0, 26, 4.95, 1.8, 0.25, 0.2, C(WHITE)) for sx in (-1, 1)]
    out += [Box(-3.2, 27.6, 4.9, 6.4, 0.2, 0.1, C(gr))]
    return out + _eyes(c=(80, 200, 120), y=27.8)


def bear():
    br, muz = (140, 92, 56), (220, 180, 140)
    out = [head_shell(br)]
    for sx in (-1, 1):
        out += [Box(sx * 3.4 - 1.4, 32.4, -0.8, 2.8, 2.6, 1.6, tex(br, fold=0)), Box(sx * 3.4 - 0.8, 32.8, 0.8, 1.6, 1.6, 0.2, C(muz))]
    out += [Box(-2, 25, 4.9, 4, 2.8, 1.4, tex(muz, fold=0)), Box(-0.8, 26.8, 6.2, 1.6, 0.9, 0.3, C((40, 26, 20)))]
    return out + _eyes(y=28.4)


def frog():
    gn = (110, 196, 80)
    out = [head_shell(gn)]
    for sx in (-1, 1):
        out += [Box(sx * 2.6 - 1.6, 32.6, 0.4, 3.2, 2.8, 3.2, tex(gn, fold=0)), Box(sx * 2.6 - 1.0, 33.2, 3.55, 2, 2, 0.3, C(WHITE)),
                Box(sx * 2.6 - 0.5, 33.6, 3.85, 1, 1.2, 0.2, C((20, 20, 24)))]
    out += [Box(-3, 25.6, 4.95, 6, 0.6, 0.3, C((60, 120, 40))), Box(-3.6, 26.0, 4.95, 0.8, 0.6, 0.3, C((60, 120, 40))), Box(2.8, 26.0, 4.95, 0.8, 0.6, 0.3, C((60, 120, 40)))]
    out += [Box(-3.6, 25.2, 4.95, 1.2, 0.6, 0.3, C((255, 160, 170))), Box(2.4, 25.2, 4.95, 1.2, 0.6, 0.3, C((255, 160, 170)))]
    return out


HATS = [("오리 탈", duck), ("토끼 탈", bunny), ("고양이 탈", cat), ("곰 탈", bear), ("개구리 탈", frog)]
