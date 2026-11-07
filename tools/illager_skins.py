"""3.8 우민 밀수꾼 · 우민 용병 스킨 + 흉조 병 변형 아이콘 — 바닐라 변명자 / 흉조 병 텍스처를 바탕으로 색만 바꾸고 덧그림.

make_smuggler(vind) / make_mercenary(vind) : 64x64 IllagerModel 텍스처 (vind = 바닐라 vindicator.png RGBA)
omen_variant(base, kind)                  : 16x16 흉조 병 변형 (base = 바닐라 ominous_bottle.png RGBA)
illager_boxes(tex, ...)                   : 도안 미리보기용 IllagerModel 상자 (cosmetic_design.Box)
"""
import math

from PIL import Image

from cosmetic_design import Box


def _lum(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def _skin(c):
    r, g, b = c[:3]
    return abs(g - b) <= 3 and g - r >= 4 and g > 95


def _ramp(cols, t):
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    a, b = cols[i], cols[i + 1]
    return tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3))


def recolor(im, rect, ramp, keep_skin=True, lo=None, hi=None):
    """rect 안의 (피부가 아닌) 픽셀을 밝기 비율 그대로 새 색 사다리로 바꿈"""
    x0, y0, x1, y1 = rect
    p = im.load()
    px = [(x, y) for x in range(x0, x1) for y in range(y0, y1) if p[x, y][3] and not (keep_skin and _skin(p[x, y]))]
    if not px:
        return
    ls = [_lum(p[x, y]) for x, y in px]
    lo = min(ls) if lo is None else lo
    hi = max(ls) if hi is None else hi
    for (x, y), l in zip(px, ls):
        c = _ramp(ramp, (l - lo) / max(1.0, hi - lo))
        p[x, y] = c + (p[x, y][3],)


# IllagerModel 텍스처 영역 (u, v, w, h, d) — 상자 펼친 그림의 범위
def _area(u, v, w, h, d):
    return (u, v, u + 2 * (d + w), v + d + h)


HEAD = (0, 0, 8, 10, 8)
HAT = (32, 0, 8, 12, 8)
NOSE = (24, 0, 2, 4, 2)
BODY = (16, 20, 8, 12, 6)
ROBE = (0, 38, 8, 20, 6)
LEG = (0, 22, 4, 12, 4)
ARMS = (44, 22, 4, 8, 4)
ARMS_MID = (40, 38, 8, 4, 4)
ARM = (40, 46, 4, 12, 4)


def _faces(u, v, w, h, d):
    """면 이름 → 텍스처 안 사각형 (x0, y0, x1, y1). 도안 left(-x) = 몹의 오른쪽 = 텍스처 'right'"""
    return {"top": (u + d, v, u + d + w, v + d), "bottom": (u + d + w, v, u + d + 2 * w, v + d),
            "left": (u, v + d, u + d, v + d + h), "front": (u + d, v + d, u + d + w, v + d + h),
            "right": (u + d + w, v + d, u + 2 * d + w, v + d + h), "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def _fill_face(p, uv, face, fn):
    x0, y0, x1, y1 = _faces(*uv)[face]
    for x in range(x0, x1):
        for y in range(y0, y1):
            c = fn(x - x0, y - y0, x1 - x0, y1 - y0)
            if c is not None:
                p[x, y] = c if len(c) == 4 else c + (255,)


# ───────────────────────── 밀수꾼 ─────────────────────────
CLOAK = [(18, 12, 24), (40, 26, 52), (66, 44, 84), (96, 70, 118)]
VEST = [(30, 22, 18), (58, 40, 30), (88, 62, 44), (118, 88, 62)]
OMEN_TEAL = (74, 200, 213)


def make_smuggler(vind):
    im = vind.copy()
    recolor(im, _area(*ROBE), CLOAK)
    recolor(im, _area(*BODY), VEST)
    recolor(im, _area(*ARMS), CLOAK)
    recolor(im, _area(*ARMS_MID), CLOAK)
    recolor(im, _area(*ARM), CLOAK)
    recolor(im, _area(*LEG), [(20, 18, 20), (44, 40, 42), (70, 64, 66)])
    p = im.load()
    # 두건 (모자 층): 얼굴만 뚫린 짙은 보라 두건 + 어깨까지 내려오는 깃
    hood = lambda u, v, W, H: _ramp(CLOAK, 0.55 + 0.25 * ((u * 7 + v * 3) % 5) / 4 - (0.25 if v >= H - 2 else 0))
    for f in ("top", "left", "right", "back"):
        _fill_face(p, HAT, f, hood)
    _fill_face(p, HAT, "front", lambda u, v, W, H: (0, 0, 0, 0) if 1 <= u <= W - 2 and 2 <= v <= 9 else hood(u, v, W, H))
    _fill_face(p, HAT, "bottom", lambda u, v, W, H: (0, 0, 0, 0))
    # 가슴 띠 + 흉조 병 3개 (청록 유리 · 보라 물약)
    def strap(u, v, W, H):
        if abs((u + v) - 9) <= 0:
            return (120, 86, 52)
        return None
    _fill_face(p, BODY, "front", strap)
    rx0, ry0, _, _ = _faces(*ROBE)["front"]
    for k, bx in enumerate((1, 3, 5)):
        p[rx0 + bx, ry0 + 4] = OMEN_TEAL + (255,)
        p[rx0 + bx, ry0 + 5] = (162, 98, 152, 255)
        p[rx0 + bx, ry0 + 3] = (126, 69, 60, 255)
    for x in range(rx0, rx0 + 8):
        p[x, ry0 + 2] = (120, 86, 52, 255)
    return im


# ───────────────────────── 용병 ─────────────────────────
BLUE = [(18, 26, 52), (34, 56, 104), (54, 88, 156), (92, 130, 196)]
STEEL = [(70, 74, 82), (120, 126, 136), (176, 182, 192), (222, 226, 232)]
GOLD = (232, 190, 70)


def make_mercenary(vind):
    im = vind.copy()
    recolor(im, _area(*ROBE), BLUE)
    recolor(im, _area(*BODY), STEEL)
    recolor(im, _area(*ARMS), BLUE)
    recolor(im, _area(*ARMS_MID), BLUE)
    recolor(im, _area(*ARM), BLUE)
    p = im.load()
    # 겉옷 가운데 금색 단 + 벨트
    rx0, ry0, _, _ = _faces(*ROBE)["front"]
    for y in range(ry0, ry0 + 20):
        p[rx0 + 3, y] = GOLD + (255,)
        p[rx0 + 4, y] = GOLD + (255,)
    for f in ("front", "back", "left", "right"):
        _fill_face(p, ROBE, f, lambda u, v, W, H: (60, 40, 26) if v == 2 else (GOLD if v == H - 1 else None))
    _fill_face(p, ROBE, "front", lambda u, v, W, H: (200, 200, 210) if v == 2 and u in (3, 4) else None)
    # 철모 (모자 층): 윗부분 3줄 + 챙
    helm = lambda u, v, W, H: _ramp(STEEL, 0.75 - 0.12 * v + (0.12 if (u + v) % 4 == 0 else 0)) if v <= 2 else ((0, 0, 0, 0) if v > 3 else _ramp(STEEL, 0.35))
    for f in ("front", "left", "right", "back"):
        _fill_face(p, HAT, f, helm)
    _fill_face(p, HAT, "top", lambda u, v, W, H: _ramp(STEEL, 0.8 if (u + v) % 3 else 0.65))
    _fill_face(p, HAT, "bottom", lambda u, v, W, H: (0, 0, 0, 0))
    # 가슴 마을 문장 (파랑 바탕 · 금 종)
    def crest(u, v, W, H):
        if 2 <= u <= 5 and 3 <= v <= 7:
            if (u in (3, 4) and v in (4, 5, 6)) or (v == 6 and u in (2, 5)):
                return GOLD
            return (40, 70, 140)
        return None
    _fill_face(p, BODY, "front", crest)
    return im


# ───────────────────────── 흉조 병 변형 ─────────────────────────
GLASS = [(19, 37, 48), (30, 71, 82), (58, 120, 127), (74, 200, 213)]
LIQUID = [(12, 6, 25), (35, 31, 42), (73, 51, 72), (162, 98, 152)]
EYES = [(168, 0, 47), (255, 0, 72)]
CORK = [(126, 69, 60), (208, 148, 118)]

THEMES = {
    "burning": dict(liquid=[(60, 10, 8), (130, 34, 18), (214, 92, 30), (255, 186, 70)], eyes=[(255, 200, 40), (255, 250, 170)]),
    "frozen": dict(liquid=[(20, 40, 92), (60, 110, 182), (130, 190, 240), (214, 242, 255)], eyes=[(220, 250, 255), (255, 255, 255)],
                   glass=[(40, 70, 100), (90, 150, 190), (170, 220, 245), (235, 250, 255)]),
    "deep": dict(liquid=[(5, 14, 40), (14, 40, 82), (30, 84, 132), (66, 168, 176)], eyes=[(80, 230, 200), (180, 255, 230)]),
    "wraith": dict(liquid=[(14, 12, 20), (40, 36, 50), (72, 64, 84), (124, 116, 136)], eyes=[(60, 200, 230), (140, 240, 255)]),
    "golden": dict(liquid=[(80, 48, 10), (150, 100, 20), (222, 170, 40), (255, 232, 110)], eyes=[(255, 255, 255), (255, 255, 255)],
                   glass=[(70, 46, 10), (140, 96, 24), (210, 160, 40), (255, 222, 90)]),
    "splash": dict(),
}


def _swap(im, src, dst):
    p = im.load()
    m = {s: d for s, d in zip(src, dst)}
    for x in range(im.width):
        for y in range(im.height):
            c = p[x, y]
            if c[3] and c[:3] in m:
                p[x, y] = m[c[:3]] + (c[3],)


def omen_variant(base, kind):
    im = base.copy()
    t = THEMES[kind]
    _swap(im, LIQUID, t.get("liquid", LIQUID))
    _swap(im, EYES, t.get("eyes", EYES))
    _swap(im, GLASS, t.get("glass", GLASS))
    p = im.load()

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            p[x, y] = c + (255,)
    if kind == "burning":  # 마개 위 불꽃
        for (x, y, c) in [(7, 1, (255, 150, 40)), (8, 1, (255, 200, 60)), (7, 0, (255, 220, 120)), (8, 2, (255, 120, 30)), (6, 2, (255, 120, 30))]:
            put(x, y, c)
    elif kind == "frozen":  # 유리에 서리
        for (x, y) in [(3, 9), (4, 12), (12, 10), (11, 13), (5, 5)]:
            put(x, y, (240, 252, 255))
    elif kind == "deep":  # 물방울
        for (x, y) in [(11, 2), (12, 0), (4, 1)]:
            put(x, y, (150, 230, 240))
    elif kind == "wraith":  # 영혼 불씨
        for (x, y, c) in [(11, 3, (90, 220, 240)), (12, 2, (60, 170, 200)), (3, 4, (90, 220, 240)), (4, 2, (60, 170, 200))]:
            put(x, y, c)
    elif kind == "golden":  # 반짝이
        for (x, y) in [(12, 4), (13, 5), (12, 6), (11, 5), (3, 12)]:
            put(x, y, (255, 255, 220))
    elif kind == "splash":  # 투척병: 마개 대신 묶은 천 심지 + 끈 고리
        for y in range(0, 4):
            for x in range(16):
                if p[x, y][:3] in [c for c in CORK]:
                    p[x, y] = (0, 0, 0, 0)
        for (x, y, c) in [(7, 2, (230, 226, 210)), (8, 2, (230, 226, 210)), (7, 1, (200, 196, 180)), (8, 1, (230, 226, 210)),
                          (9, 0, (230, 226, 210)), (6, 0, (200, 196, 180)), (7, 3, (150, 60, 50)), (8, 3, (150, 60, 50)),
                          (10, 4, (150, 60, 50)), (11, 5, (150, 60, 50)), (11, 6, (150, 60, 50))]:
            put(x, y, c)
    return im


# ───────────────────────── 미리보기 상자 ─────────────────────────
def _paint(tex, uv, mirror=False):
    fr = _faces(*uv)
    p = tex.load()

    def f(face, u, v, W, H):
        if mirror and face in ("left", "right"):
            face = "right" if face == "left" else "left"
        x0, y0, x1, y1 = fr[face]
        uu = (x1 - x0 - 1 - u) if mirror else u
        c = p[x0 + min(uu, x1 - x0 - 1), y0 + min(v, y1 - y0 - 1)]
        return c[:3] if c[3] > 40 else None
    return f


def _mc(px, py, pz, x, y, z, w, h, d):
    """마인크래프트 모델 좌표 (y 아래, -z 앞) → 도안 좌표 (y 위, +z 앞, 발 = 0)"""
    return (px + x, 24 - (py + y + h), -(pz + z + d), w, h, d)


def illager_boxes(tex, arms="crossed", hat=False, ox=0.0, oz=0.0):
    out = []

    def add(uv, geo, mirror=False, grow=0.0, rot=None):
        x0, y0, z0, w, h, d = geo
        out.append(Box(x0 + ox, y0, z0 + oz, w, h, d, _paint(tex, uv, mirror), grow=grow, rot=rot))
    add(HEAD, _mc(0, 0, 0, -4, -10, -4, 8, 10, 8))
    add(NOSE, _mc(0, -2, 0, -1, -1, -6, 2, 4, 2))
    if hat:
        add(HAT, _mc(0, 0, 0, -4, -10, -4, 8, 12, 8), grow=0.45)
    add(BODY, _mc(0, 0, 0, -4, 0, -3, 8, 12, 6))
    add(ROBE, _mc(0, 0, 0, -4, 0, -3, 8, 20, 6), grow=0.5)
    add(LEG, _mc(-2, 12, 0, -2, 0, -2, 4, 12, 4))
    add(LEG, _mc(2, 12, 0, -2, 0, -2, 4, 12, 4), mirror=True)
    if arms == "crossed":
        piv = (ox, 24 - 3, oz + 1)
        r = ("x", -0.75, piv)
        add(ARMS, _mc(0, 3, -1, -8, -2, -2, 4, 8, 4), rot=r)
        add(ARMS, _mc(0, 3, -1, 4, -2, -2, 4, 8, 4), mirror=True, rot=r)
        add(ARMS_MID, _mc(0, 3, -1, -4, 2, -2, 8, 4, 4), rot=r)
    else:  # 한 손에 무기 든 자세 (팔 내림)
        add(ARM, _mc(-5, 2, 0, -3, -2, -2, 4, 12, 4))
        add(ARM, _mc(5, 2, 0, -1, -2, -2, 4, 12, 4), mirror=True)
    return out
