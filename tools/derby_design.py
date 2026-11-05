"""3.5 도안: 경마장 (3D 상자). 좌표: x 오른쪽, y 위, z 앞(+z 정면), 16 = 1칸. 경마장 전체는 house_design 처럼 블록 단위."""
import math

import anim_fx as AF
import house_design as H
from cosmetic_design import Anim, Box, mix, shade
from kitchen_design import C, DARKWOOD, metal, planks

SILKS = [((220, 50, 50), (250, 250, 250)), ((50, 90, 200), (250, 220, 60)), ((40, 150, 80), (250, 250, 250)),
         ((240, 170, 40), (60, 40, 30)), ((150, 70, 180), (250, 250, 250)), ((30, 30, 36), (230, 60, 60))]
COATS = [(140, 85, 45), (60, 40, 30), (200, 150, 90), (230, 225, 215), (110, 70, 40), (40, 36, 34)]


def horse(coat=(140, 85, 45), silk=None, x=0, z=0, y=0, scale=1.0, gallop=0.0):
    """말 (옆에서 보면 오른쪽(+x)이 머리) + 기수 (silk = (몸통색, 무늬색))"""
    k = scale
    mane = shade(coat, 0.55)

    def body(f, u, v, W, Hh):
        return shade(coat, 1.08) if f == "top" else (shade(coat, 0.92) if v > Hh * 0.7 else coat)
    out = [Box(x - 7 * k, y + 9 * k, z - 3 * k, 14 * k, 7 * k, 6 * k, body)]
    # 다리 4개 (달리면 앞뒤로 벌어짐)
    for (lx, ph) in ((-6, 0), (-3.5, math.pi), (3, math.pi), (5.5, 0)):
        sw = math.sin(gallop + ph) * 2.2 * k
        out += [Box(x + lx * k + sw * 0.4, y, z - 2.6 * k + (0 if lx < 0 else 3.4 * k), 1.6 * k, 9.5 * k, 1.6 * k, C(shade(coat, 0.85))),
                Box(x + lx * k + sw * 0.4 - 0.1 * k, y, z - 2.7 * k + (0 if lx < 0 else 3.4 * k), 1.8 * k, 1.4 * k, 1.8 * k, C((40, 36, 34)))]
    # 목 · 머리 · 귀 · 갈기 · 꼬리
    out += [Box(x + 5 * k, y + 13 * k, z - 1.8 * k, 4 * k, 7 * k, 3.6 * k, body, rot=("z", -0.45, (x + 7 * k, y + 14 * k, z))),
            Box(x + 8 * k, y + 18 * k, z - 1.8 * k, 6 * k, 3.4 * k, 3.6 * k, body),
            Box(x + 12.8 * k, y + 18.2 * k, z - 1.4 * k, 1.4 * k, 2 * k, 2.8 * k, C(shade(coat, 0.75))),
            Box(x + 8.4 * k, y + 21.2 * k, z - 1.4 * k, 1 * k, 1.6 * k, 0.9 * k, C(shade(coat, 0.8))),
            Box(x + 8.4 * k, y + 21.2 * k, z + 0.5 * k, 1 * k, 1.6 * k, 0.9 * k, C(shade(coat, 0.8))),
            Box(x + 4.6 * k, y + 15 * k, z - 0.6 * k, 4.4 * k, 6.5 * k, 1.2 * k, C(mane), rot=("z", -0.45, (x + 7 * k, y + 14 * k, z))),
            Box(x - 9.5 * k, y + 8 * k, z - 0.8 * k, 2.8 * k, 8 * k, 1.6 * k, C(mane), rot=("z", 0.4, (x - 7 * k, y + 15 * k, z)))]
    out += [Box(x + 12 * k, y + 19.6 * k, z + 1.75 * k, 0.8 * k, 0.8 * k, 0.1 * k, C((20, 20, 20))),
            Box(x + 12 * k, y + 19.6 * k, z - 1.85 * k, 0.8 * k, 0.8 * k, 0.1 * k, C((20, 20, 20)))]
    if silk:
        a, b = silk

        def jersey(f, u, v, W, Hh):
            return b if (u // 2) % 2 == 0 and f in ("front", "back", "left", "right") else a
        # 안장 천 + 기수 (구부린 몸 · 헬멧 · 팔 · 다리)
        out += [Box(x - 3 * k, y + 15.8 * k, z - 3.3 * k, 6 * k, 0.6 * k, 6.6 * k, C(a)),
                Box(x - 2.4 * k, y + 16.5 * k, z - 1.6 * k, 4.4 * k, 5 * k, 3.2 * k, jersey, rot=("z", -0.55, (x, y + 17 * k, z))),
                Box(x + 1.6 * k, y + 21 * k, z - 1.4 * k, 2.8 * k, 2.8 * k, 2.8 * k, C((245, 205, 170))),
                Box(x + 1.4 * k, y + 22.8 * k, z - 1.6 * k, 3.2 * k, 1.6 * k, 3.2 * k, C(a)),
                Box(x + 2 * k, y + 18.6 * k, z - 2.4 * k, 4 * k, 1 * k, 0.8 * k, C(a)), Box(x + 2 * k, y + 18.6 * k, z + 1.6 * k, 4 * k, 1 * k, 0.8 * k, C(a)),
                Box(x - 1 * k, y + 13 * k, z - 3.5 * k, 2.4 * k, 4 * k, 0.8 * k, C((245, 245, 245))),
                Box(x - 1 * k, y + 13 * k, z + 2.7 * k, 2.4 * k, 4 * k, 0.8 * k, C((245, 245, 245)))]
    return out


# ═══════════ 블록 ═══════════
def stable():
    """마구간 (2칸 · 높이 2): 내 경주마를 등록 · 훈련 · 먹이. 반문 · 이름표 · 말굽 간판 · 건초 · 물통 · 안장 걸이."""
    w = planks((150, 95, 55))
    out = [Box(-16, 0, -8, 32, 1, 16, planks((120, 80, 45)))]
    out += [Box(-16, 1, -8, 32, 22, 2, w), Box(-16, 1, -8, 2, 22, 16, w), Box(14, 1, -8, 2, 22, 16, w)]
    out += [Box(-17, 23, -9, 34, 2, 18, planks(DARKWOOD)), Box(-17, 25, -9, 34, 3, 9, planks((170, 60, 50)))]
    # 반문 (X 무늬) + 열린 위쪽
    xd = lambda f, u, v, W, Hh: (240, 235, 220) if u in (0, W - 1) or v in (0, Hh - 1) or abs(u - v * W / Hh) < 1.2 or abs((W - 1 - u) - v * W / Hh) < 1.2 else (170, 60, 50)
    out += [Box(-14, 1, 6, 12, 10, 1.5, xd), Box(2, 1, 6, 12, 10, 1.5, xd), Box(-14, 11, 6, 28, 1, 1.8, planks(DARKWOOD))]
    # 안에서 내다보는 말 머리
    out += horse(COATS[0], None, x=-14, z=2, y=-6, scale=0.85)[-9:-5]
    # 이름표 · 말굽 간판
    out += [Box(-5, 18, 8.2, 10, 3.5, 0.4, lambda f, u, v, W, Hh: (60, 40, 25) if f == "front" and v == 1 and 1 < u < W - 2 and u % 2 else (240, 225, 190))]
    shoe = lambda f, u, v, W, Hh: (200, 200, 210) if f == "front" and (u in (0, W - 1) or v == Hh - 1) else None
    out += [Box(-2, 13, 8.4, 4, 4, 0.3, shoe)]
    # 건초 · 물통 · 안장 걸이
    out += [Box(-13, 1, -5, 6, 6, 6, lambda f, u, v, W, Hh: (222, 190, 90) if (u + v) % 4 else (200, 165, 70))]
    out += [Box(8, 1, -6, 5, 4, 5, metal((130, 135, 140))), Box(8.5, 4.6, -5.5, 4, 0.3, 4, C((110, 170, 220)))]
    out += [Box(-14, 14, -6, 1, 1, 10, planks(DARKWOOD)), Box(-14.5, 10, -3, 2, 4, 5, C((120, 60, 30))), Box(-14.6, 9, -2, 1, 1.5, 1, C((200, 180, 60)))]
    out += [Box(-12, 19, 6.6, 2, 2, 2, AF.flicker((255, 220, 120))), Box(10, 19, 6.6, 2, 2, 2, AF.flicker((255, 220, 120)))]
    return out


def betting_kiosk():
    """마권 발매기 (1칸 · 높이 2): 초록 부스 · 배당률 화면(흐름) · 마권 출력구 · 동전 투입구 · 'WIN' 등."""
    green = lambda f, u, v, W, Hh: (30, 110, 70) if (u + v) % 9 else (25, 95, 60)
    out = [Box(-7, 0, -6, 14, 26, 12, green), Box(-7.5, 26, -6.5, 15, 2, 13, C((230, 190, 70)))]

    def odds(t, f, u, v, W, Hh):
        if f != "front":
            return (20, 24, 30)
        if u in (0, W - 1) or v in (0, Hh - 1):
            return (10, 12, 16)
        row = (v - 1) // 2
        if (v - 1) % 2 == 1:
            return (14, 18, 24)
        c = [s[0] for s in SILKS][(row + t // 6) % 6]
        if u <= 2:
            return c
        if u >= W - 5:
            return (255, 220, 80) if (u + row + t) % 3 else (255, 180, 40)
        return (200, 230, 210) if (u * 3 + row + t // 3) % 5 else (40, 60, 50)
    out += [Box(-5.5, 13, 5.8, 11, 10, 0.4, Anim(24, 3, odds))]
    out += [Box(-5, 6, 5.8, 10, 2.5, 0.6, C((20, 20, 24))), Box(-3, 6.6, 6.3, 6, 1, 0.3, C((250, 250, 245)))]  # 마권 출력구
    out += [Box(2.5, 10, 5.8, 2, 2, 0.5, C((200, 200, 205))), Box(3.2, 10.7, 6.2, 0.6, 0.8, 0.2, C((20, 20, 20)))]  # 동전 구멍
    out += [Box(-4, 9.5, 5.8, 3.5, 2, 0.6, AF.chase((90, 40, 20), (255, 200, 60), 0, 1, n=8, ticks=4))]
    win = lambda f, u, v, W, Hh: (255, 255, 255) if f in ("front", "back") and v in (1, 2) and u % 3 != 2 and 0 < u < W - 1 else (200, 40, 50)
    out += [Box(-6, 28, -1, 12, 4, 2, win)]
    return out


def starting_gate():
    """출발 게이트 (가로 4칸 · 높이 2): 철제 틀 · 번호 칸 6개 · 빨강/흰 문 · 위 가로대 번호판."""
    frame = metal((235, 235, 238))
    out = [Box(-32, 0, -8, 64, 1, 16, C((120, 110, 100)))]
    for k in range(7):
        x = -32 + k * 64 / 6
        out += [Box(x - 0.8, 0, -8, 1.6, 26, 1.6, frame), Box(x - 0.8, 0, 6.4, 1.6, 26, 1.6, frame), Box(x - 0.8, 24, -8, 1.6, 2, 16, frame)]
    out += [Box(-33, 26, -9, 66, 2, 18, metal((60, 120, 190)))]
    for k in range(6):
        x = -32 + k * 64 / 6 + 1
        a, b = SILKS[k]
        gate = lambda f, u, v, W, Hh, a=a: (250, 250, 250) if (u + v) % 4 < 2 else (210, 40, 50)
        out += [Box(x, 2, 7, 64 / 6 - 2.2, 12, 0.6, gate)]
        num = lambda f, u, v, W, Hh, a=a, n=k + 1: (255, 255, 255) if f in ("front", "back") and (u, v) in DIGITS[n] else a
        out += [Box(x + 2, 20, 7.4, 6, 5, 0.4, num)]
    return out


DIGITS = {1: {(2, 1), (2, 2), (2, 3), (1, 1)}, 2: {(1, 1), (2, 1), (3, 2), (2, 3), (1, 3), (2, 3), (3, 3)}, 3: {(1, 1), (2, 1), (3, 2), (2, 2), (1, 3), (2, 3)},
          4: {(1, 1), (1, 2), (2, 2), (3, 2), (3, 1), (3, 3)}, 5: {(3, 1), (2, 1), (1, 2), (2, 2), (2, 3), (1, 3)}, 6: {(2, 1), (1, 2), (2, 2), (1, 3), (2, 3), (3, 3)}}


def scoreboard():
    """전광판 (가로 3칸 · 높이 2): 다리 2개 · 검은 판 · 순위 1~6 (기수색 · 이름 막대 · 반짝) · 위 시계."""
    out = [Box(-20, 0, -1, 2, 16, 2, metal((90, 90, 96))), Box(18, 0, -1, 2, 16, 2, metal((90, 90, 96)))]
    out += [Box(-24, 12, -1.5, 48, 20, 2, C((30, 30, 34)))]

    def board(t, f, u, v, W, Hh):
        if f != "front":
            return (30, 30, 34)
        if u in (0, W - 1) or v in (0, Hh - 1):
            return (230, 190, 70)
        if v <= 3:
            return (255, 220, 80) if (u + t) % 6 < 3 and 4 < u < W - 5 else (10, 12, 16)
        row = (v - 5) // 2
        if (v - 5) % 2 == 1 or row > 5 or row < 0:
            return (10, 12, 16)
        order = [(row + t // 8) % 6]
        silk = SILKS[order[0]][0]
        if u <= 3:
            return (255, 255, 255) if u == 2 else (10, 12, 16)
        if u <= 6:
            return silk
        if u < W - 8 - ((order[0] * 5) % 9):
            return (230, 240, 230) if row else (255, 230, 120)
        return (10, 12, 16)
    out += [Box(-23, 13, 0.6, 46, 18, 0.4, Anim(48, 2, board))]
    return out


def finish_post():
    """결승 기둥 (1칸 · 높이 2): 빨강/흰 줄 기둥 · 둥근 판 · 깃발."""
    stripe = lambda f, u, v, W, Hh: (230, 50, 50) if (v // 3) % 2 == 0 else (250, 250, 250)
    out = [Box(-3, 0, -3, 6, 1, 6, C((200, 200, 205))), Box(-1, 1, -1, 2, 26, 2, stripe)]
    disc = lambda f, u, v, W, Hh: (230, 50, 50) if ((u - W / 2 + 0.5) ** 2 + (v - Hh / 2 + 0.5) ** 2) < (W / 2) ** 2 * 0.45 else (250, 250, 250)
    out += [Box(-5, 22, -0.6, 10, 10, 1.2, disc)]
    flag = lambda f, u, v, W, Hh: (20, 20, 20) if (u + v) % 2 else (250, 250, 250)
    out += [Box(1, 27, -0.2, 7, 5, 0.4, flag)]
    return out


# ═══════════ 경마장 상자 (설치하면 지어짐 · 블록 단위) ═══════════
def racetrack():
    """경마장 25×41: 흙 타원 주로 · 흰 난간 · 관중석(지붕 · 의자) · 출발 게이트 · 결승 기둥 · 전광판 · 마구간 줄."""
    B, sol = H.B, H.solid
    out = H.plate(25, 41)
    dirt = sol((150, 110, 70))
    grass_in = sol((110, 170, 80))
    # 타원 주로: 바깥 반지름 rx 11 · rz 19, 폭 3
    for gx in range(-12, 13):
        for gz in range(-20, 21):
            r_out = (gx / 11.5) ** 2 + (gz / 19.5) ** 2
            r_in = (gx / 8.0) ** 2 + (gz / 16.0) ** 2
            if r_out <= 1 and r_in > 1:
                out.append(B(gx - 0.5, -0.4, gz - 0.5, 1, 0.42, 1, dirt))
            elif r_in <= 1:
                out.append(B(gx - 0.5, -0.4, gz - 0.5, 1, 0.41, 1, grass_in))
    # 난간 (바깥 · 안쪽)
    for k in range(0, 360, 6):
        a = math.radians(k)
        for (rx, rz) in ((11.6, 19.6), (7.8, 15.8)):
            out.append(B(math.cos(a) * rx - 0.1, 0, math.sin(a) * rz - 0.1, 0.2, 0.8, 0.2, sol((250, 250, 250), False)))
            a2 = math.radians(k + 6)
            out.append(B(min(math.cos(a), math.cos(a2)) * rx, 0.7, min(math.sin(a), math.sin(a2)) * rz, abs(math.cos(a2) - math.cos(a)) * rx + 0.15, 0.12,
                         abs(math.sin(a2) - math.sin(a)) * rz + 0.15, sol((250, 250, 250), False)))
    # 관중석 (오른쪽 바깥, 계단식 의자 + 지붕)
    for k in range(4):
        out.append(B(12 + k * 0.0, k * 0.6, -8, 0.01, 0.01, 0.01, sol((0, 0, 0))))
        out.append(B(12.2 + k * 0.9, 0, -8, 0.9, 0.6 * (k + 1), 16, sol((180, 180, 185))))
        for z in range(-8, 8):
            out.append(B(12.3 + k * 0.9, 0.6 * (k + 1), z + 0.1, 0.6, 0.25, 0.8, sol([(220, 60, 60), (60, 110, 200), (250, 200, 60)][(z + k) % 3], False)))
    for z in (-8, 7.6):
        out.append(B(15.6, 0, z, 0.4, 4.6, 0.4, sol((90, 90, 96))))
    out += [B(11.8, 4.5, -8.5, 4.4, 0.3, 17, H.tiles((200, 60, 50)))]
    # 출발 게이트 (아래쪽 직선) · 결승 기둥 · 전광판 (안쪽 잔디)
    for k in range(6):
        a, b = SILKS[k]
        out += [B(8.3 + k * 0.5, 0, -2 + 0, 0.08, 1.6, 0.08, sol((235, 235, 238), False))]
    out += [B(8.2, 0, -3, 3.2, 1.6, 0.15, sol((235, 235, 238), False)), B(8.2, 1.5, -3, 3.2, 0.2, 2, sol((60, 120, 190), False))]
    for k in range(6):
        out.append(B(8.25 + k * 0.52, 0.1, -1.2, 0.45, 0.8, 0.06, sol(SILKS[k][0], False)))
    out += [B(9.5, 0, 6, 0.2, 1.8, 0.2, H.solid((230, 50, 50), False)), B(9.1, 1.6, 6.05, 1, 1, 0.1, sol((250, 250, 250), False))]
    out += [B(-3, 0, -1, 0.2, 1.4, 0.2, sol((90, 90, 96))), B(3, 0, -1, 0.2, 1.4, 0.2, sol((90, 90, 96))),
            B(-3.5, 1.2, -1.1, 7, 1.6, 0.2, lambda f, u, v, W, Hh: [(30, 30, 34), (255, 220, 80), (230, 240, 230)][(u * 7 + v * 3) % 3 if 0 < v < Hh - 1 else 0])]
    # 마구간 줄 (왼쪽 바깥)
    for k in range(4):
        z = -6 + k * 3
        out += [B(-15.5, 0, z, 2.4, 1.8, 2.6, H.planks((150, 95, 55))), B(-15.7, 1.8, z - 0.1, 2.8, 0.3, 2.8, H.tiles((170, 60, 50))),
                B(-13.12, 0, z + 0.5, 0.05, 1, 1.6, sol((170, 60, 50), False))]
    # 경주 중인 말들 (도안 장식)
    for k in range(4):
        a = math.radians(200 + k * 7)
        for b_ in horse(COATS[k], SILKS[k], x=math.cos(a) * 9.8 * 16, z=math.sin(a) * 17.8 * 16, y=0, scale=0.9, gallop=k * 1.3):
            out.append(b_)
    return out


DERBY = {
    "stable": ("마구간", stable, -1, 0, 0, 0, 2, "내 말 등록 · 훈련 · 먹이 · 컨디션"),
    "betting_kiosk": ("마권 발매기", betting_kiosk, 0, 0, 0, 0, 2, "경주표 · 배당률 · 마권 사기 · 당첨금"),
    "starting_gate": ("출발 게이트", starting_gate, -2, 1, 0, 0, 2, "6마리 출발 칸 · 경주 시작"),
    "scoreboard": ("전광판", scoreboard, -1, 1, 0, 0, 2, "실시간 순위 · 배당 · 다음 경주 시간"),
    "finish_post": ("결승 기둥", finish_post, 0, 0, 0, 0, 2, "결승선 · 사진 판정"),
}
