"""3.4 도안: 목장 · 집들이(집 평가) · 주말 대회 (3D 상자).
좌표: x 오른쪽, y 위, z 앞(+z 정면), 16 = 1칸, 바닥 y=0. 축사 외형은 house_design 과 같은 블록 단위."""
from cosmetic_design import Box, mix, shade
from kitchen_design import C, DARKWOOD, glow, marble, metal, planks, stone
from furniture_design import GOLD, WHITE, fabric
import house_design as H
import anim_fx as AF
import math

HAY = (222, 190, 90)
WOOD = (176, 128, 78)
IRON = (150, 154, 160)


def hay(face, u, v, W, Hh):
    if (u * 5 + v * 3) % 7 == 0:
        return shade(HAY, 0.8)
    return shade(HAY, 1.08) if (u + v * 2) % 5 == 0 else HAY


def hoops(c, band=(90, 90, 96), rows=(1, -2)):
    """통: 나무 판 + 쇠 띠"""
    def f(face, u, v, W, Hh):
        if face in ("top", "bottom"):
            return shade(c, 1.05) if (u + v) % 4 else shade(c, 0.85)
        if any(v == (r % Hh) for r in rows):
            return band
        return shade(c, 0.8) if u % 4 == 3 else c
    return f


def cheese_c(face, u, v, W, Hh):
    if face != "top" and (u * 3 + v * 5) % 9 == 0:
        return (230, 175, 50)
    return (250, 205, 80) if face != "top" else (255, 220, 110)


# ═══════════ 목장 ═══════════
def hay_trough():
    """건초 여물통 (2칸): 건초를 넣으면 반경 8칸 동물 애정도 +, 하루 한 번."""
    out = []
    for x in (-15, 13):
        for z in (-6, 4):
            out.append(Box(x, 0, z, 2, 4, 2, planks(DARKWOOD)))
    out += [Box(-16, 4, -7, 32, 1.5, 14, planks(WOOD))]
    out += [Box(-16, 5.5, -7, 32, 7, 1.5, planks(WOOD)), Box(-16, 5.5, 5.5, 32, 7, 1.5, planks(WOOD)),
            Box(-16, 5.5, -7, 1.5, 7, 14, planks(WOOD)), Box(14.5, 5.5, -7, 1.5, 7, 14, planks(WOOD))]
    out += [Box(-14.5, 5.5, -5.5, 29, 6, 11, hay)]
    for (x, z, s) in [(-10, -2, 4), (-2, 1, 5), (6, -3, 4), (10, 2, 3)]:
        out.append(Box(x, 11.5, z, s, 2, s * 0.8, hay))
    out += [Box(-16.2, 8, 6.8, 32.4, 1, 0.4, metal(IRON)), Box(-16.2, 8, -7.2, 32.4, 1, 0.4, metal(IRON))]
    # 하트 표지판
    heart = lambda f, u, v, W, Hh: (230, 80, 100) if f in ("front", "back") and (u, v) in {(1, 1), (3, 1), (1, 2), (2, 2), (3, 2), (2, 3)} else (240, 225, 200)
    out += [Box(-1, 12.5, 6.4, 1, 6, 1, planks(DARKWOOD)), Box(-3, 16, 6.6, 5, 5, 0.8, heart)]
    return out


def cheese_barrel():
    """치즈 숙성통 (1칸): 우유 → 치즈 (게임 속 하루). 위에 숙성 중인 치즈 바퀴."""
    out = [Box(-6, 0, -6, 12, 13, 12, hoops((150, 100, 55), rows=(1, 6, -2)))]
    out += [Box(-6.5, 12.5, -6.5, 13, 1, 13, planks(DARKWOOD))]
    out += [Box(-4.5, 13.5, -4.5, 9, 3.5, 9, cheese_c), Box(-3, 17, -3, 6, 1, 6, C((240, 230, 210)))]
    out += [Box(1.5, 13.5, 4.4, 3, 3.5, 0.3, C((255, 235, 140)))]  # 잘린 면
    out += [Box(-2, 4, 6, 4, 3, 0.8, C((240, 225, 190))), Box(-1.2, 4.6, 6.6, 2.4, 1.6, 0.4, C((60, 50, 40)))]  # 이름표
    out += [Box(5.8, 3, -1, 1.5, 1.5, 2, metal(IRON)), Box(7, 2.3, -0.3, 1, 1, 0.6, metal(IRON))]  # 꼭지
    return out


def butter_churn():
    """버터 교반기 (1칸): 우유 → 버터 · 생크림. 손잡이를 돌리는 통."""
    out = [Box(-7, 0, -7, 14, 1.5, 14, planks(DARKWOOD))]
    out += [Box(-5, 1.5, -5, 10, 15, 10, hoops((215, 170, 110), rows=(1, 7, -2)))]
    out += [Box(-5.5, 16.5, -5.5, 11, 1.2, 11, planks((190, 140, 90)))]
    out += [Box(-0.7, 17.7, -0.7, 1.4, 9, 1.4, planks(DARKWOOD)), Box(-4, 25, -0.6, 8, 1.2, 1.2, planks(DARKWOOD)),
            Box(-4.5, 24.3, -1, 1.8, 2.6, 2, C((200, 60, 50))), Box(2.7, 24.3, -1, 1.8, 2.6, 2, C((200, 60, 50)))]
    # 옆 받침: 버터 덩이 + 크림 병
    out += [Box(5, 1.5, 3, 3, 0.8, 4, C((235, 225, 210))), Box(5.4, 2.3, 3.6, 2.2, 1.6, 2.6, C((255, 230, 120)))]
    out += [Box(-8, 1.5, 3, 2.6, 4, 2.6, C((250, 250, 245))), Box(-7.6, 5.5, 3.4, 1.8, 1, 1.8, C((120, 170, 220)))]
    return out


def spinning_wheel():
    """물레 (1칸): 양털 → 실 → 천 (천은 가구 · 옷 재료)."""
    out = [Box(-7, 3, -2, 14, 1.5, 4, planks(WOOD))]
    for (x, z) in [(-6, -2), (5, -2), (-6, 1), (5, 1)]:
        out.append(Box(x, 0, z, 1.2, 3, 1.2, planks(DARKWOOD)))
    # 바퀴 (z 축 둘레 살 8개)
    rim = planks((150, 100, 55))
    cx, cy = -1, 13
    for k in range(16):
        a = k * math.pi / 8
        out.append(Box(cx + 8 * math.cos(a) - 1, cy + 8 * math.sin(a) - 1, -0.6, 2, 2, 1.2, AF.chase((150, 100, 55), (230, 190, 120), k, 16, n=16, ticks=1)))
    out += [Box(cx - 8, cy - 0.4, -0.3, 16, 0.8, 0.6, planks(DARKWOOD)), Box(cx - 0.4, cy - 8, -0.3, 0.8, 16, 0.6, planks(DARKWOOD)),
            Box(cx - 8, cy - 0.4, -0.3, 16, 0.8, 0.6, planks(DARKWOOD), rot=("z", math.pi / 4, (cx, cy, 0))),
            Box(cx - 8, cy - 0.4, -0.3, 16, 0.8, 0.6, planks(DARKWOOD), rot=("z", -math.pi / 4, (cx, cy, 0)))]
    out += [Box(cx - 1, cy - 1, -1, 2, 2, 2, C(GOLD)), Box(cx - 0.5, 4.5, -0.3, 1, cy - 5, 0.6, planks(DARKWOOD))]
    # 실패 + 양털 뭉치
    out += [Box(4, 4.5, -1, 2, 7, 2, planks(DARKWOOD)), Box(3.5, 8, -1.5, 3, 3, 3, fabric((240, 238, 230)))]
    out += [Box(-8, 4.5, -2, 4, 3, 4, fabric((250, 250, 245)))]
    return out


def nest_box():
    """둥지 상자 (1칸): 근처 닭이 여기에 알을 낳음 — 애정도가 높으면 좋은 알 · 가끔 황금 알."""
    out = [Box(-7, 0, -6, 14, 2, 12, planks(WOOD))]
    out += [Box(-7, 2, -6, 14, 7, 1.5, planks(WOOD)), Box(-7, 2, -6, 1.5, 7, 12, planks(WOOD)), Box(5.5, 2, -6, 1.5, 7, 12, planks(WOOD)),
            Box(-7, 2, 4.5, 14, 2.5, 1.5, planks(WOOD))]
    out += [Box(-8, 9, -7, 16, 1.5, 7, planks((180, 70, 55))), Box(-8, 10.5, -7, 16, 1.5, 4, planks((160, 60, 48)))]
    out += [Box(-7, 9, -6, 1.5, 0.01, 1, planks(WOOD))]
    out += [Box(-5.5, 2, -4.5, 11, 2.5, 9, hay)]
    for (x, z, c) in [(-3, -1, (245, 235, 215)), (0, 1, (240, 215, 180)), (2.5, -2, (255, 215, 60))]:
        out += [Box(x, 4.5, z, 2, 2.5, 2, C(c)), Box(x + 0.3, 7, z + 0.3, 1.4, 0.6, 1.4, C(shade(c, 1.04)))]
    return out


def ranch_market():
    """가축 상점 (1칸): 진열대 위 미니 소 · 닭 + 간판. 가축 · 사료 · 브러시 · 축사 상자 판매."""
    out = [Box(-8, 0, -8, 16, 8, 16, planks((190, 140, 90))), Box(-8.5, 8, -8.5, 17, 1, 17, hay)]
    cow = lambda f, u, v, W, Hh: (40, 35, 35) if (u * 3 + v * 2) % 5 < 2 else (245, 245, 240)
    out += [Box(-6, 11, -2, 7, 3.5, 3.5, cow), Box(1, 12, -1.5, 2.5, 2.5, 2.5, cow), Box(3.2, 12.6, -1, 0.6, 1, 1.6, C((240, 170, 160)))]
    for (x, z) in [(-5.5, -2), (-0.8, -2), (-5.5, 0.8), (-0.8, 0.8)]:
        out.append(Box(x, 9, z, 0.8, 2, 0.8, C((240, 240, 235))))
    out += [Box(3, 9, 2.5, 3, 3, 2.5, C((250, 250, 248))), Box(5, 12, 3, 1.5, 1.5, 1.5, C((250, 250, 248))),
            Box(6.2, 12.4, 3.4, 0.8, 0.5, 0.7, C((240, 170, 50))), Box(5.2, 13.5, 3.4, 0.9, 0.8, 0.7, C((220, 50, 50)))]
    sign = lambda f, u, v, W, Hh: (60, 120, 60) if f in ("front", "back") and 0 < u < W - 1 and 0 < v < Hh - 1 else (240, 230, 200)
    out += [Box(-7, 9, -8, 1, 13, 1, planks(DARKWOOD)), Box(6, 9, -8, 1, 13, 1, planks(DARKWOOD)), Box(-7, 17, -8.2, 14, 5, 1.2, sign)]
    out += [Box(-3.5, 18.5, -7.5, 7, 2, 0.3, C((250, 240, 200)))]
    return out


# ── 축사 외형 (블록 단위, 집 상자처럼 설치하면 지어짐) ──
def barn():
    """큰 축사 11×9: 빨간 판자벽 · 흰 테두리 · 박공 지붕 · 2층 건초 다락 · 앞 울타리 마당."""
    B, sol = H.B, H.solid
    red, white = (170, 50, 45), (245, 242, 235)
    out = H.plate(11, 15)
    out += [B(-5.5, 0, -5, 11, 0.4, 9, sol((120, 118, 112)))]
    wall = lambda f, u, v, W, Hh: shade(red, 0.82) if u % 3 == 2 else red
    out += [B(-5.5, 0.4, -5, 11, 4.6, 9, wall)]
    for (x, z) in [(-5.6, -5.1), (5.1, -5.1), (-5.6, 3.6), (5.1, 3.6)]:
        out.append(B(x, 0.4, z, 0.5, 4.6, 0.5, sol(white, False)))
    out += [B(-5.6, 4.9, -5.1, 11.2, 0.3, 9.2, sol(white, False))]
    # 큰 미닫이문 (X 무늬)
    xdoor = lambda f, u, v, W, Hh: white if u in (0, W - 1) or v in (0, Hh - 1) or abs(u - v * W / Hh) < 1 or abs((W - 1 - u) - v * W / Hh) < 1 else shade(red, 0.9)
    out += [B(-2, 0.4, 4.02, 4, 3.4, 0.1, xdoor)]
    out += [B(-1, 3.4, 4.05, 2, 1.3, 0.1, lambda f, u, v, W, Hh: (230, 200, 120) if 0 < u < W - 1 and 0 < v < Hh - 1 else white)]
    out += H.window(-4.5, 2, 4.02, 1.2, 1.2) + H.window(3.3, 2, 4.02, 1.2, 1.2)
    out += H.window(5.5, 2, -3, 2, 1.2, "x") + H.window(5.5, 2, 0.5, 2, 1.2, "x")
    # 박공 지붕 (높게)
    k = 0
    while 12 - 2 * k > 0:
        w = 12 - 2 * k
        out.append(B(-w / 2, 5.2 + k * 0.6, -5.5, w, 0.6, 10, H.tiles((90, 85, 80))))
        k += 1
    out += [B(-3.5, 5.2, 4.02, 7, 2.5, 0.1, wall), B(-1, 5.6, 4.06, 2, 1.6, 0.1, lambda f, u, v, W, Hh: HAY if 0 < u < W - 1 and v > 0 else white)]
    out += [B(-1.2, 8.8, -0.8, 2.4, 1.2, 1.6, sol(white)), B(-0.15, 10, -0.15, 0.3, 1, 0.3, sol(GOLD, False))]  # 환기탑 + 풍향계
    # 앞마당 울타리 + 건초 더미 + 여물통
    fence = sol((150, 105, 60))
    for x in (-5.5, -3, 3, 5.3):
        out.append(B(x, 0, 9.4, 0.25, 1.1, 0.25, fence))
    for z in (4.2, 6.8, 9.4):
        out += [B(-5.6, 0, z, 0.25, 1.1, 0.25, fence), B(5.4, 0, z, 0.25, 1.1, 0.25, fence)]
    out += [B(-5.5, 0.7, 9.4, 3.5, 0.15, 0.2, fence), B(2, 0.7, 9.4, 3.5, 0.15, 0.2, fence),
            B(-5.6, 0.7, 4.2, 0.2, 0.15, 5.4, fence), B(5.4, 0.7, 4.2, 0.2, 0.15, 5.4, fence)]
    out += [B(-5, 0, 4.6, 1, 1, 1, hay), B(-4, 0, 4.6, 1, 1, 1, hay), B(-4.5, 1, 4.6, 1, 1, 1, hay)]
    out += [B(3, 0, 5, 2, 0.6, 1, sol(WOOD)), B(3.1, 0.6, 5.1, 1.8, 0.1, 0.8, hay)]
    # 소 한 마리
    cow = lambda f, u, v, W, Hh: (40, 35, 35) if (u + v * 2) % 5 < 2 else (245, 245, 240)
    out += [B(-1.5, 0.5, 6, 1.6, 0.8, 0.8, cow), B(0.1, 0.9, 6.15, 0.5, 0.5, 0.5, cow)]
    for (x, z) in [(-1.4, 6), (-0.2, 6), (-1.4, 6.6), (-0.2, 6.6)]:
        out.append(B(x, 0, z, 0.2, 0.5, 0.2, sol((40, 35, 35))))
    return out


def coop():
    """닭장 5×5: 노란 판자 · 경사 지붕 · 닭 사다리 · 철망 마당."""
    B, sol = H.B, H.solid
    yel, white = (230, 200, 120), (245, 242, 235)
    out = H.plate(7, 9)
    wall = lambda f, u, v, W, Hh: shade(yel, 0.85) if u % 3 == 2 else yel
    for (x, z) in [(-2.5, -2.5), (2.2, -2.5), (-2.5, 0.2), (2.2, 0.2)]:
        out.append(B(x, 0, z, 0.3, 1, 0.3, sol(DARKWOOD)))
    out += [B(-2.5, 1, -2.5, 5, 2.4, 3, wall)]
    out += [B(-2.6, 3.4, -2.8, 5.2, 0.3, 3.6, H.tiles((180, 70, 55))), B(-2.6, 3.7, -2.8, 5.2, 0.3, 2, H.tiles((160, 60, 48)))]
    out += [B(-0.5, 1, 0.52, 1, 1.4, 0.1, lambda f, u, v, W, Hh: white if u in (0, W - 1) or v in (0, Hh - 1) else (60, 45, 35))]
    out += H.window(1.2, 2, 0.52, 0.8, 0.8)
    ramp = lambda f, u, v, W, Hh: shade(WOOD, 0.7) if v % 3 == 0 else WOOD
    out.append(B(-0.5, 0, 0.5, 1, 0.12, 1.6, ramp, ) if False else B(-0.5, 0.45, 0.55, 1, 0.12, 1.3, ramp))
    # 철망 마당
    mesh = lambda f, u, v, W, Hh: (200, 205, 210) if (u + v) % 2 == 0 else None
    out += [B(-2.5, 0, 0.5, 0.05, 1.2, 3.5, mesh), B(2.45, 0, 0.5, 0.05, 1.2, 3.5, mesh), B(-2.5, 0, 3.95, 5, 1.2, 0.05, mesh)]
    out += [B(-2.5, 0, 0.5, 5, 0.02, 3.5, hay)]
    for (x, z) in [(-1.6, 2.2), (1.1, 2.8), (0.2, 1.6)]:
        out += [B(x, 0.15, z, 0.5, 0.45, 0.4, sol((250, 250, 248))), B(x + 0.35, 0.45, z + 0.05, 0.25, 0.3, 0.3, sol((250, 250, 248))),
                B(x + 0.6, 0.55, z + 0.12, 0.1, 0.08, 0.15, sol((240, 170, 50), False)), B(x + 0.42, 0.75, z + 0.12, 0.12, 0.1, 0.15, sol((220, 50, 50), False))]
    return out


# ═══════════ 집들이 · 집 평가 ═══════════
def guestbook():
    """방명록 (1칸): 집 주인이 놓음. 손님이 누르면 별점(1~5) + 한 줄 후기, 주인은 답글."""
    out = [Box(-5, 0, -5, 10, 1.5, 10, planks(DARKWOOD)), Box(-1.5, 1.5, -1.5, 3, 10, 3, planks((150, 100, 55)))]
    out += [Box(-6, 11.5, -4, 12, 1.5, 9, planks(DARKWOOD), rot=("x", 0.35, (0, 12, 0)))]
    page = lambda f, u, v, W, Hh: (90, 90, 100) if f == "top" and v % 2 == 1 and 1 <= u < W - 1 and u != W // 2 else (250, 246, 232)
    out += [Box(-5.5, 13, -3.5, 5.3, 0.8, 8, page, rot=("x", 0.35, (0, 12, 0))),
            Box(0.2, 13, -3.5, 5.3, 0.8, 8, page, rot=("x", 0.35, (0, 12, 0)))]
    out += [Box(-0.2, 13.2, -3.5, 0.4, 0.8, 8, C((180, 40, 50)), rot=("x", 0.35, (0, 12, 0)))]
    # 깃펜 + 잉크병
    out += [Box(4.5, 11, -5.5, 2, 2, 2, C((40, 40, 60))), Box(5.2, 13, -5, 0.5, 6, 0.5, C((250, 250, 250)), rot=("z", -0.3, (5.5, 13, -5))),
            Box(4.3, 15.5, -5.3, 1.6, 3.5, 0.3, C((235, 235, 245)), rot=("z", -0.3, (5.5, 13, -5)))]
    # 별 다섯 개 (앞 판)
    for k in range(5):
        out.append(Box(-4.4 + k * 1.9, 5, 1.6, 1.4, 1.4, 0.4, C(GOLD if k < 4 else (120, 110, 90))))
    return out


def house_plaque():
    """우리 집 문패 (벽걸이 1칸 · 뒤 벽에 붙음): 집 이름판 + 별 다섯 + 금 리본 · 앞쪽 끝에 보이지 않는 점 (가운데 맞춤용)."""
    z = -8
    out = [Box(-7, 3, z + 0.5, 14, 10, 1.5, planks((150, 100, 55))), Box(-7.5, 2.5, z, 15, 11, 0.5, planks(DARKWOOD))]
    name = lambda f, u, v, W, Hh: (60, 40, 25) if f == "front" and v in (1, 3) and 1 < u < W - 2 and u % 3 != 0 else (240, 228, 200)
    out += [Box(-5.5, 8, z + 2, 11, 4, 0.4, name)]
    for k in range(5):
        out.append(Box(-5 + k * 2.2, 4.3, z + 2, 1.6, 1.6, 0.5, AF.glint(GOLD, n=24, ticks=2, pause=12)))
    out += [Box(4.5, 9, z + 2.3, 3, 3, 0.4, C((220, 60, 60))), Box(4.8, 6, z + 2.3, 1, 3, 0.3, C((200, 50, 50))), Box(6.2, 6, z + 2.3, 1, 3, 0.3, C((200, 50, 50))),
            Box(5.3, 9.6, z + 2.6, 1.4, 1.4, 0.3, C(GOLD))]
    out += [Box(-0.5, 13, z + 1, 1, 2, 0.5, metal(IRON))]
    out += [Box(-0.05, 0, 7.9, 0.1, 0.1, 0.1, lambda *a: None)]
    return out


# ═══════════ 주말 대회 ═══════════
def contest_board():
    """대회 게시판 (2칸 폭 · 2칸 높이): 이번 주 종목 · 남은 시간 · 실시간 순위 1~4."""
    out = [Box(-15, 0, -2, 2, 30, 2, planks(DARKWOOD)), Box(13, 0, -2, 2, 30, 2, planks(DARKWOOD))]
    out += [Box(-14, 8, -1.5, 28, 20, 1.5, planks((120, 80, 45)))]

    def board(f, u, v, W, Hh):
        if f != "front":
            return (40, 60, 45)
        if v < 3:
            return (240, 200, 70) if 2 < u < W - 3 else (40, 60, 45)
        row = (v - 4) // 3
        if (v - 4) % 3 == 2 or row < 0 or row > 3:
            return (40, 60, 45)
        if u < 4:
            return [(255, 210, 60), (200, 205, 215), (205, 130, 70), (150, 160, 150)][row]
        return (230, 235, 225) if u < W - 4 - row * 3 and u % 5 != 0 else (40, 60, 45)
    out += [Box(-12.5, 10, 0, 25, 16, 0.4, AF.Anim(16, 3, lambda t, f, u, v, W, Hh: (255, 240, 150) if f == "front" and 4 <= v <= 5 and u < 4 and t % 4 < 2 else board(f, u, v, W, Hh)))]
    out += [Box(-16, 28, -2.5, 32, 2, 3, planks(DARKWOOD))]
    for k, c in enumerate([(220, 60, 60), (250, 200, 60), (70, 140, 220), (90, 180, 90), (220, 60, 60), (250, 200, 60), (70, 140, 220)]):
        out.append(Box(-13 + k * 4, 25, 0.6, 3, 3, 0.3, AF.chase(shade(c, 0.6), c, k, 7, n=14, ticks=2)))
    # 시계 (남은 시간)
    out += [Box(-3, 30, -1.5, 6, 6, 1.5, C(WHITE)), Box(-0.3, 32.5, 0.1, 0.6, 3, 0.3, C((40, 40, 40))), Box(-0.3, 32.7, 0.1, 2.3, 0.6, 0.3, C((40, 40, 40)))]
    return out


def podium():
    """시상대 (3칸): 가운데 1등 · (정면에서 볼 때) 왼쪽 2등 · 오른쪽 3등. 올라서면 앉기 대신 포즈 + 폭죽."""
    out = []
    # 모델은 북쪽(-z)을 보고 놓이므로 정면에서 보면 도안 x 가 거울처럼 뒤집힘 → 2등을 +x, 숫자도 좌우 반전해서 그림
    for (x, h, c, n) in [(8, 10, (205, 210, 220), 2), (-8, 16, (250, 205, 70), 1), (-24, 7, (210, 140, 80), 3)]:
        out += [Box(x, 0, -8, 16, h, 16, marble), Box(x - 0.1, h - 0.6, -8.2, 16.2, 1, 16.4, C(c))]
        digit = {1: {(1, 0), (1, 1), (1, 2), (1, 3), (1, 4), (0, 3)}, 2: {(0, 4), (1, 4), (2, 4), (2, 3), (1, 2), (0, 1), (0, 0), (1, 0), (2, 0)},
                 3: {(0, 4), (1, 4), (2, 4), (2, 3), (1, 2), (2, 2), (2, 1), (0, 0), (1, 0), (2, 0)}}[n]
        for (dx, dy) in digit:
            out.append(Box(x + 10 - (dx + 1) * 1.4, h / 2 - 3 + dy * 1.4, 8, 1.4, 1.4, 0.4, C(shade(c, 0.45))))
    out += [Box(-25, 0, 8, 50, 0.6, 3, C((180, 40, 50)))]
    return out


def trophy(cup, base=(60, 50, 45), tag="1"):
    def f():
        g = AF.glint(cup, n=24, ticks=2, pause=10)
        out = [Box(-5, 0, -5, 10, 3, 10, planks(base)), Box(-4, 3, -4, 8, 2, 8, planks(shade(base, 1.2)))]
        out += [Box(-3.5, 1, 5, 7, 1.5, 0.3, C(cup))]
        out += [Box(-1, 5, -1, 2, 4, 2, g), Box(-2.5, 9, -2.5, 5, 1, 5, g)]
        out += [Box(-4, 10, -4, 8, 3, 8, g), Box(-5, 13, -5, 10, 5, 10, g), Box(-5.5, 17.5, -5.5, 11, 1, 11, g)]
        out += [Box(-8, 12, -0.7, 3, 1.4, 1.4, g), Box(-8, 12, -0.7, 1.4, 5, 1.4, g), Box(-8, 16, -0.7, 3, 1.4, 1.4, g),
                Box(5, 12, -0.7, 3, 1.4, 1.4, g), Box(6.6, 12, -0.7, 1.4, 5, 1.4, g), Box(5, 16, -0.7, 3, 1.4, 1.4, g)]
        out += [Box(-1.5, 13, 5, 3, 3, 0.4, C(shade(cup, 1.2))), Box(-0.6, 13.6, 5.3, 1.2, 1.8, 0.3, C(shade(cup, 0.6)))]
        return out
    return f


# 이름 → (한글, 도안, lat0, lat1, dep0, dep1, 높이, 기능)
RANCH = {
    "hay_trough": ("건초 여물통", hay_trough, -1, 0, 0, 0, 1, "건초 → 근처 동물 애정도 +"),
    "nest_box": ("둥지 상자", nest_box, 0, 0, 0, 0, 1, "닭이 여기 알을 낳음 · 좋은 알/황금 알"),
    "cheese_barrel": ("치즈 숙성통", cheese_barrel, 0, 0, 0, 0, 1, "우유 → 치즈 (게임 하루)"),
    "butter_churn": ("버터 교반기", butter_churn, 0, 0, 0, 0, 2, "우유 → 버터 · 생크림"),
    "spinning_wheel": ("물레", spinning_wheel, 0, 0, 0, 0, 2, "양털 → 실 → 천"),
    "ranch_market": ("가축 상점", ranch_market, 0, 0, 0, 0, 2, "가축 · 사료 · 브러시 · 축사 상자"),
}
HOMEVISIT = {
    "guestbook": ("방명록", guestbook, 0, 0, 0, 0, 2, "별점 + 후기 · 주인 답글"),
    "house_plaque": ("우리 집 문패", house_plaque, 0, 0, 0, 0, 1, "벽걸이 · 평균 별점 · 예쁜 집 리본"),
}
CONTEST = {
    "contest_board": ("대회 게시판", contest_board, -1, 0, 0, 0, 2, "종목 · 남은 시간 · 실시간 순위"),
    "podium": ("시상대", podium, -1, 1, 0, 0, 1, "1·2·3등 자리"),
    "trophy_gold": ("금 트로피", trophy((240, 195, 60)), 0, 0, 0, 0, 2, "1등 보상 가구"),
    "trophy_silver": ("은 트로피", trophy((200, 205, 215)), 0, 0, 0, 0, 2, "2등 보상 가구"),
    "trophy_bronze": ("동 트로피", trophy((205, 130, 70)), 0, 0, 0, 0, 2, "3등 보상 가구"),
}
BARNS = {
    "barn": ("큰 축사", barn, (13, 15), 11, 150_000, "소·양 12마리 · 건초 다락 · 마당"),
    "coop": ("닭장", coop, (7, 9), 5, 40_000, "닭 8마리 · 둥지 · 철망 마당"),
}
