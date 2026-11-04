"""3.4 도안: 광석 채굴기 (1칸 바닥 · 높이 2칸 모양) — 철골 데릭 탑 · 드릴 · 엔진 · 컨베이어 · 경광등 · 제어판.
ON = 작동 중 (드릴 회전 · 컨베이어 움직임 · 경광등 · 계기판 깜빡)."""
import math

import anim_fx as AF
from cosmetic_design import Anim, Box, mix, shade
from kitchen_design import C, metal

ON = False
STEEL = (120, 126, 134)
DARK = (55, 58, 64)
YELLOW = (235, 185, 40)


def hazard(face, u, v, W, H):
    return YELLOW if ((u + v) // 2) % 2 == 0 else (30, 30, 32)


def plate(face, u, v, W, H):
    if face == "top":
        return shade(STEEL, 0.9) if (u % 4 == 0 or v % 4 == 0) else STEEL
    return mix(shade(STEEL, 1.05), shade(STEEL, 0.75), v / max(1, H - 1))


def drill_paint():
    """드릴: 나선 줄무늬가 아래로 흘러감 (회전)"""
    if not ON:
        return lambda f, u, v, W, H: (150, 150, 158) if (u + v) % 3 else (90, 90, 98)

    def f(t, face, u, v, W, H):
        k = (u + v - t) % 4
        return (190, 190, 200) if k == 0 else ((140, 140, 150) if k == 1 else (90, 90, 98))
    return Anim(8, 1, f)


def belt_paint():
    """컨베이어: 고무 벨트 + 광석 조각이 앞으로 이동"""
    ores = [(70, 70, 76), (230, 190, 60), (80, 200, 220), (200, 60, 70), (60, 180, 90)]

    def f(t, face, u, v, W, H):
        if face != "top":
            return (40, 40, 44)
        tt = t if ON else 0
        if (v + tt) % 4 == 0:
            return (25, 25, 28)
        if u in (1, 2) and (v + tt) % 6 in (1, 2):
            return ores[((v + tt) // 6) % len(ores)]
        return (55, 55, 60)
    return Anim(12, 2, f) if ON else (lambda face, u, v, W, H: f(0, face, u, v, W, H))


def beacon_paint():
    """경광등: 작동 중이면 주황 불빛이 빙 돎"""
    if not ON:
        return lambda f, u, v, W, H: (120, 70, 30)

    def f(t, face, u, v, W, H):
        side = {"front": 0, "right": 1, "back": 2, "left": 3}.get(face, -1)
        if side == t % 4 or face == "top":
            return (255, 190, 60)
        return (170, 90, 30)
    return Anim(4, 3, f)


def panel_paint():
    lights = [(80, 220, 90), (240, 200, 60), (220, 60, 60), (90, 160, 250)]

    def f(t, face, u, v, W, H):
        if face != "front":
            return DARK
        if 0 < u < W - 1 and 0 < v < H - 1:
            if v == 1 and u % 2 == 1:
                on = ON and (t + u) % 3 != 0
                return lights[(u // 2) % 4] if on else shade(lights[(u // 2) % 4], 0.35)
            if v == H - 2:
                return (40, 120, 60) if u < 1 + (t % (W - 2) if ON else 0) else (20, 30, 24)
            return (30, 32, 36)
        return DARK
    return Anim(12, 3, f) if ON else (lambda face, u, v, W, H: f(0, face, u, v, W, H))


def ore_miner():
    out = []
    # 바닥 판 (경고 줄무늬 테두리) + 가운데 시추 구멍
    out += [Box(-8, 0, -8, 16, 2, 16, plate), Box(-8.2, 2, -8.2, 16.4, 0.6, 1.2, hazard), Box(-8.2, 2, 7, 16.4, 0.6, 1.2, hazard),
            Box(-8.2, 2, -8.2, 1.2, 0.6, 16.4, hazard), Box(7, 2, -8.2, 1.2, 0.6, 16.4, hazard)]
    out += [Box(-3, 2, -3, 6, 0.3, 6, C((25, 22, 20)))]
    # 데릭 탑: 다리 4개 (위로 갈수록 모임) + 가로대 + 꼭대기 도르래
    for (x, z) in [(-6, -6), (5, -6), (-6, 5), (5, 5)]:
        for k in range(5):
            y = 2 + k * 5
            sx = x + (0 - x) * k * 0.09
            sz = z + (0 - z) * k * 0.09
            out.append(Box(sx, y, sz, 1.2, 5, 1.2, metal((200, 70, 50))))
    for y in (8, 15, 22):
        r = 5.8 - (y - 2) * 0.09 * 5
        out += [Box(-r, y, -r, 2 * r, 0.8, 0.8, metal((200, 70, 50))), Box(-r, y, r - 0.8, 2 * r, 0.8, 0.8, metal((200, 70, 50))),
                Box(-r, y, -r, 0.8, 0.8, 2 * r, metal((200, 70, 50))), Box(r - 0.8, y, -r, 0.8, 0.8, 2 * r, metal((200, 70, 50)))]
    out += [Box(-3, 27, -3, 6, 1.5, 6, metal(DARK)), Box(-1.5, 28.5, -2.5, 3, 3, 5, metal(STEEL)), Box(-0.5, 29.5, -3, 1, 1, 6, C((40, 40, 44)))]
    # 드릴 파이프 (회전) + 드릴 머리 (구멍 속)
    out += [Box(-1, 3, -1, 2, 24, 2, drill_paint()), Box(-2, 2.2, -2, 4, 2.5, 4, drill_paint())]
    # 엔진 (뒤 오른쪽): 몸체 · 방열판 · 배기관
    vent = lambda f, u, v, W, H: (40, 40, 44) if f in ("front", "right") and v % 2 == 1 and 0 < u < W - 1 else (220, 170, 40)
    out += [Box(2.5, 2.5, -7.5, 5, 6, 5, vent), Box(5.8, 8.5, -6.8, 1.2, 6, 1.2, metal((80, 80, 86))), Box(5.5, 14.5, -7.1, 1.8, 0.8, 1.8, C((30, 30, 32)))]
    # 컨베이어 (앞 왼쪽 → 광석 상자)
    out += [Box(-7.5, 4, 1, 3, 1, 6.5, belt_paint()), Box(-7.5, 2.5, 2, 0.6, 1.5, 0.6, metal(DARK)), Box(-5.1, 2.5, 6, 0.6, 1.5, 0.6, metal(DARK))]
    out += [Box(-7.8, 2.5, 5.5, 3.6, 3, 2.3, metal((150, 110, 60))), Box(-7.4, 5.5, 5.9, 1.2, 0.8, 1.2, C((230, 190, 60))),
            Box(-6, 5.5, 6.2, 1, 0.7, 1, C((80, 200, 220)))]
    # 제어판 (앞 오른쪽)
    out += [Box(3.5, 2.5, 3.5, 4, 5, 2, panel_paint()), Box(4, 7.5, 3.8, 3, 0.6, 1.4, C(DARK))]
    # 경광등 (탑 꼭대기 옆)
    out += [Box(-6, 2.5, -7.5, 1, 9, 1, metal(STEEL)), Box(-6.7, 11.5, -8.2, 2.4, 2.4, 2.4, beacon_paint())]
    return out


# ═══════════ 부동산 중개소 (1칸 · 높이 1.8칸 모양) ═══════════
def realty():
    """공인중개사 사무소 데스크: 뒤 코르크 게시판에 매물 사진 카드 6장(차례로 반짝) · 빨간 간판(집 + 열쇠) ·
    대리석 상판 책상 · 매물 모니터 · 계약서 · 도장 · SOLD 팻말."""
    from kitchen_design import marble, planks, DARKWOOD
    out = []
    # 책상 (앞)
    out += [Box(-8, 0, -1, 16, 9, 8, planks((150, 100, 60))), Box(-8.3, 9, -1.3, 16.6, 1, 8.6, marble)]
    out += [Box(-7, 1, 6.9, 6.5, 7, 0.4, planks((170, 115, 70))), Box(0.5, 1, 6.9, 6.5, 7, 0.4, planks((170, 115, 70))),
            Box(-4.5, 4, 7.3, 1.5, 0.8, 0.4, C((230, 190, 70))), Box(3, 4, 7.3, 1.5, 0.8, 0.4, C((230, 190, 70)))]
    # 뒤 게시판 (코르크 + 나무 테)
    cork = lambda f, u, v, W, H: (190, 140, 90) if (u * 3 + v * 5) % 7 else (170, 120, 75)
    out += [Box(-8, 0, -8, 16, 26, 2, planks(DARKWOOD)), Box(-7, 10, -6.1, 14, 14, 0.3, cork)]

    def card(k):
        roof = [(200, 60, 50), (60, 90, 160), (70, 140, 80), (150, 90, 160), (220, 150, 50), (90, 90, 100)][k]

        def f(t, face, u, v, W, H):
            on = ON_REALTY and (t // 4) % 6 == k
            if face != "front":
                return (245, 240, 230)
            if v == 0 or u in (0, W - 1) or v == H - 1:
                return (255, 220, 90) if on else (245, 240, 230)
            if v <= 1 + abs(u - W // 2) * 0 and v < 2:
                return roof
            if v == 2 and 0 < u < W - 1:
                return roof
            if v >= 3 and 1 <= u <= W - 2:
                return (60, 50, 40) if (u == W // 2 and v >= H - 3) else (250, 245, 235)
            return (150, 200, 240)
        return Anim(24, 3, f) if ON_REALTY else (lambda face, u, v, W, H: f(0, face, u, v, W, H))
    for k in range(6):
        x = -6 + (k % 3) * 4.3
        y = 17.5 - (k // 3) * 6.5
        out += [Box(x, y, -5.9, 3.6, 5, 0.3, card(k)), Box(x + 1.5, y + 4.6, -5.6, 0.6, 0.6, 0.3, C((220, 50, 50)))]
    # 간판 (빨강 · 흰 집 + 금 열쇠)
    def sign(face, u, v, W, H):
        if face not in ("front", "back"):
            return (170, 30, 40)
        if u in (0, W - 1) or v in (0, H - 1):
            return (240, 200, 80)
        hx = 3
        if v >= 1 and abs(u - hx) <= v - 1 and v <= 3:
            return (255, 255, 255)
        if 4 <= v <= 5 and hx - 2 <= u <= hx + 2:
            return (255, 255, 255)
        if v == 3 and 7 <= u <= W - 3:
            return (250, 210, 70)
        if v in (2, 4) and u == 8:
            return (250, 210, 70)
        return (200, 40, 50)
    out += [Box(-8, 26, -8.4, 16, 7, 2.6, sign)]
    # 모니터 (매물 화면) + 계약서 + 도장 + SOLD 팻말
    def monitor(t, face, u, v, W, H):
        if face != "front":
            return (40, 40, 46)
        if u in (0, W - 1) or v in (0, H - 1):
            return (25, 25, 28)
        page = (t // 6) % 3
        if v <= 1:
            return (40, 120, 220)
        if 2 <= v <= H - 3 and 1 <= u <= 3:
            return [(200, 60, 50), (60, 140, 80), (60, 90, 160)][page] if v < 4 else (240, 235, 220)
        if 5 <= u <= W - 2 and v in (2, 4, 6):
            return (230, 70, 70) if v == 6 else (90, 100, 120)
        return (245, 248, 252)
    out += [Box(1, 10, 0, 6, 0.6, 4, C((40, 40, 46))), Box(3.4, 10.6, 1, 1.2, 2, 1, C((40, 40, 46))),
            Box(0.5, 12.5, 0.6, 7, 5, 0.8, Anim(18, 4, monitor) if ON_REALTY else (lambda face, u, v, W, H: monitor(0, face, u, v, W, H)))]
    paper = lambda f, u, v, W, H: (60, 60, 70) if f == "top" and v % 2 == 1 and 0 < u < W - 1 else (250, 248, 240)
    out += [Box(-6.5, 10, 2, 5, 0.3, 4, paper), Box(-2.5, 10.3, 4.5, 1.4, 0.2, 1.4, C((220, 50, 50))),
            Box(-1, 10, 1.5, 1.2, 1.8, 1.2, C((90, 55, 30))), Box(-1.2, 11.8, 1.3, 1.6, 0.6, 1.6, C((200, 40, 40)))]
    out += [Box(-7.5, 10, -0.5, 0.5, 6, 0.5, C((120, 80, 40))), Box(-7.5, 13.5, -0.4, 4, 2.5, 0.3,
            lambda f, u, v, W, H: (255, 255, 255) if f in ("front", "back") and v == 1 and u % 2 == 0 else (200, 40, 50))]
    out += [Box(-0.05, 0, 7.9, 0.1, 0.1, 0.1, lambda *a: None)]
    return out


ON_REALTY = True
