"""5.0 네더 도안: 겁화 장비 · 겁화의 군주 · 잿불 사냥개 · 용암 거북 · 재의 주술사 · 흑요 골렘 · 겁화의 성채 · 잿불 감시탑."""
import math
import random
import sys

from cosmetic_design import Box, mannequin, mix, render, shade, sheet, solid as S

BK = (26, 18, 24); BK2 = (54, 38, 46); CR = (150, 20, 36); CR2 = (214, 46, 54); GD = (240, 196, 96); GD2 = (160, 116, 48)
WH = (255, 246, 220); EM = (255, 150, 60); BONE = (226, 214, 196); ASH = (96, 88, 90); ASH2 = (132, 122, 122); OBS = (34, 22, 48); OBS2 = (70, 48, 96)
R = math.radians


def forged(seed=0, base=BK, alt=BK2, rate=27, trim=None):
    """검은 강철 + 진홍 균열 + 흰 불씨 (trim 이면 가장자리 금테)"""
    def f(face, u, v, w, h):
        if trim and (u in (0, w - 1) or v in (0, h - 1)):
            return trim
        if (u * 7 + v * 13 + seed) % (rate * 4) == 0:
            return WH
        if (u * 5 + v * 3 + seed) % rate == 0 or (u - 2 * v + seed) % (rate + 7) == 0:
            return CR2
        return base if (u + v * 2 + seed) % 4 else alt
    return f


def flame(seed=0):
    """아래 흰색 → 금 → 진홍 불꽃"""
    return lambda f, u, v, w, h: mix(mix(WH, GD, min(1, (1 - v / max(1, h - 1)) * 2)), CR2, max(0, (1 - v / max(1, h - 1)) * 2 - 1)) if (u + v + seed) % 5 else EM


def blade(f, u, v, w, h):
    t = v / max(1, h - 1)
    if u == w // 2 or f in ("left", "right"):
        return mix(WH, GD, t)
    return mix(CR2, CR, t) if (u + v) % 6 else EM


class M:
    def __init__(self, dy=0):
        self.b, self.dy = [], dy

    def a(self, x, y, z, w, h, d, p, rot=None):
        if rot:
            rot = (rot[0], R(rot[1]), (rot[2][0], rot[2][1] + self.dy, rot[2][2]))
        self.b.append(Box(x, y + self.dy, z, w, h, d, p if callable(p) else S(p), rot=rot))


# ───────── 겁화 장비 (마네킹 착용) ─────────
def gear():
    m = M()
    m.b += mannequin()
    a = m.a
    # 투구: 검은 강철 + 금테 + 뒤로 휜 뿔 한 쌍 + 이마 불씨
    a(-4.6, 23.6, -4.6, 9.2, 9, 9.2, lambda f, u, v, w, h: None if (f == "front" and 3 <= v <= 5 and 2 <= u <= 6) else forged(1, trim=GD if v == 0 else None)(f, u, v, w, h))
    a(-1, 31, 4.4, 2, 2.4, .6, WH); a(-.5, 28.4, 4.5, 1, 2.6, .5, GD)
    for s in (-1, 1):
        for i, (dx, dy, w) in enumerate([(4.6, 30, 2.2), (6, 32, 1.8), (6.8, 34.4, 1.4), (6.6, 36.8, 1)]):
            a(s * dx - (w if s < 0 else 0), dy, -2 - i * .6, w, 2.8, w, BONE if i > 1 else ASH2)
    # 흉갑: 가슴 가운데 흰 불 핵 + 금 갈비
    a(-4.5, 11.6, -2.5, 9, 12.6, 5, forged(3, trim=None))
    a(-1.5, 17, 2.5, 3, 3, .5, GD); a(-1, 17.5, 2.7, 2, 2, .5, WH)
    for i in range(3):
        a(-3.6, 15 + i * 2.2, 2.5, 2, .8, .4, GD); a(1.6, 15 + i * 2.2, 2.5, 2, .8, .4, GD)
    a(-4.5, 11.2, -2.5, 9, 1.4, 5, CR)
    # 견갑 + 팔
    for s in (-1, 1):
        x = 4 if s > 0 else -8.6
        a(x, 21, -2.8, 4.6, 4, 5.6, forged(5 + s, trim=GD))
        a(x + (3.6 if s > 0 else -.6), 23.6, -1, 1.6, 3, 2, BONE)
        a(x + .1, 12, -2.3, 4.4, 5, 4.6, forged(7 + s))
    # 각반 + 장화 + 허리 천
    for x in (-4.3, .1):
        a(x, 0, -2.4, 4.2, 9, 4.8, forged(9, trim=None)); a(x, 0, -2.4, 4.2, 2, 5.6, BK2); a(x + 1.1, 6, 2.4, 2, 2.4, .5, GD)
    a(-2, 6, 2.5, 4, 6, .5, lambda f, u, v, w, h: None if (v == h - 1 and u % 2) else (GD if u in (0, w - 1) else CR))
    # 대검: 진홍 → 흰 날, 금 코등이
    a(9.2, 9, 1, 1.2, 5, 1.2, BK2); a(7.4, 14, .6, 4.8, 1.2, 2, GD); a(9.3, 8, 1.1, 1, 1, 1, CR2)
    a(8.6, 15.2, 1.2, 2.4, 20, .8, blade); a(9.2, 35.2, 1.2, 1.2, 2, .8, WH)
    return m.b


# ───────── 보스: 겁화의 군주 ─────────
def lord():
    m = M(-34)
    a = m.a
    # 다리 (발굽) + 허리 갑주 + 찢긴 진홍 천
    for s in (-1, 1):
        x = 3 if s > 0 else -13
        a(x - 1, 0, -6, 12, 5, 14, BONE); a(x, 5, -5, 10, 16, 10, forged(s + 2)); a(x - 1, 19, -6, 12, 6, 12, forged(s + 5, trim=GD)); a(x + 1, 25, -5, 9, 12, 10, forged(s + 8))
        a(x + 2.5, 21, 6, 5, 5, 2, BONE)
    a(-14, 36, -7, 28, 9, 14, forged(11, trim=GD))
    for i, (x, ln) in enumerate([(-12, 26), (-6, 32), (0, 30), (6, 24)]):
        a(x, 37 - ln, 7.2, 6, ln, .6, lambda f, u, v, w, h, i=i: None if (v > h - 4 and (u + i) % 2) else (GD if u == 0 and v < h - 4 else mix(CR2, CR, v / h)))
    # 몸통: 벌어진 흉곽 안에 흰 태양 핵
    a(-17, 45, -9, 34, 30, 18, forged(13, rate=17))
    a(-8, 50, 9, 16, 20, .6, CR); a(-6, 52, 9.6, 12, 16, .8, EM); a(-4, 54, 10.4, 8, 12, 1, GD); a(-2, 57, 11.4, 4, 6, 1, WH)
    for i in range(5):
        y = 50 + i * 4.4
        for s in (-1, 1):
            L = 9 - abs(i - 2)
            a(1 if s > 0 else -1 - L, y, 11.6, L, 1.8, 1.6, GD if i % 2 == 0 else GD2)
    # 견갑: 3단 + 뿔 가시
    for s in (-1, 1):
        x = 15 if s > 0 else -33
        a(x, 66, -11, 18, 10, 22, forged(15 + s, trim=GD)); a(x + (2 if s > 0 else 0), 76, -9, 16, 5, 18, forged(18 + s)); a(x + (5 if s > 0 else 1), 81, -6, 12, 4, 12, BK2)
        for i in range(3):
            a(x + (10 + i * 3 if s > 0 else 5 - i * 3), 82 + i * 5, -2 + i, 4 - i, 7, 4 - i, BONE, ("z", -s * (14 + i * 8), (x + 9, 82, 0)))
    # 머리: 왕관 투구 + 뿔 4개 + 이글거리는 눈
    a(-7, 76, -6, 14, 14, 14, forged(21)); a(-8, 86, -7, 16, 3, 16, GD)
    a(-5.5, 81, 8, 4, 2, .5, WH); a(1.5, 81, 8, 4, 2, .5, WH); a(-3, 76, 8, 6, 3, .6, BK2)
    for i in range(-2, 3):
        a(i * 2.6 - 1, 89, 6.6 - abs(i), 2, 4 + (2 - abs(i)) * 2.5, 1.6, GD)
    for s in (-1, 1):
        for i, (dx, dy, w) in enumerate([(7, 84, 5), (11, 88, 4.4), (14, 94, 3.6), (15, 101, 2.8), (13.5, 108, 2)]):
            a(s * dx - (w if s < 0 else 0), dy, -2 - i * .8, w, 8, w, ASH2 if i < 2 else BONE)
        for i, (dx, dy, w) in enumerate([(4, 89, 3), (5.4, 94, 2.2), (5.8, 99, 1.4)]):
            a(s * dx - (w if s < 0 else 0), dy, -5, w, 6, w, BONE)
    # 머리 뒤 부서진 불의 고리 (왕관)
    for k in range(14):
        if k in (3, 10):
            continue
        ang = k / 14 * math.tau
        a(math.cos(ang) * 19 - 2, 96 + math.sin(ang) * 19 - 2, -11, 4, 4, 2, GD if k % 2 else WH)
        if k % 3 == 0:
            a(math.cos(ang) * 24 - 1, 96 + math.sin(ang) * 24 - 1, -11, 2, 2, 1.5, EM)
    # 등: 검날 날개 6장
    for s in (-1, 1):
        for i, (ang, ln) in enumerate([(28, 52), (52, 44), (78, 34)]):
            px = s * 10
            a(px - 2.5, 68, -14 - i, 5, ln, 1.6, blade, ("z", -s * ang, (px, 68, -13)))
            a(px - 1, 68 + ln, -14 - i, 2, 6, 1.6, WH, ("z", -s * ang, (px, 68, -13)))
    # 오른팔: 대검을 땅에 짚음
    for i in range(4):
        a(20 + i * 1.5, 56 - i * 9, -5 + i * 2, 11, 11, 11, forged(30 + i, trim=GD if i == 3 else None))
    a(22, 18, 4, 13, 9, 12, BK2)
    a(26, 26, 9, 4, 14, 4, BK2); a(18, 22, 8, 20, 4, 6, GD); a(27, 40, 10, 2.4, 4, 2.4, CR2)
    a(23.5, -22, 9.6, 9, 44, 2.6, blade); a(26, -27, 9.6, 4, 5, 2.6, WH)
    # 왼팔: 사슬에 매단 불타는 향로
    for i in range(4):
        a(-31 - i * 2, 56 - i * 8, -5 + i * 2.5, 11, 10, 11, forged(40 + i))
    a(-40, 24, 6, 12, 9, 12, BK2)
    for i in range(6):
        a(-35.5 + (i % 2), 22 - i * 3.6, 11, 3, 3.6, 1.4 if i % 2 else 3, ASH2)
    a(-40, -8, 6, 12, 9, 12, forged(50, trim=GD)); a(-38, 1, 8, 8, 2, 8, GD)
    for i in range(5):
        a(-36 + math.sin(i * 1.4) * 2, 3 + i * 4, 10 + math.cos(i) * 2, 4 - i * .5, 5, 4 - i * .5, WH if i < 2 else GD if i < 4 else CR2)
    return m.b


# ───────── 몬스터 ─────────
def hound():
    m = M(4)
    a = m.a
    for x, z in ((-5, 6), (2, 6), (-5, -10), (2, -10)):
        a(x, 0, z, 3, 8, 3, forged(x + z)); a(x - .4, 0, z, 3.8, 1.6, 4, BONE)
    a(-5, 8, -11, 10, 8, 22, forged(3, rate=9)); a(-4, 9, -2, 8, 1, 9, EM)
    for i in range(4):
        a(-5.3, 9 + i * 1.6, -1 + i * 2.2, .5, 1, 6, GD); a(4.8, 9 + i * 1.6, -1 + i * 2.2, .5, 1, 6, GD)
    a(-4, 9, 11, 8, 8, 8, forged(6)); a(-3, 9, 19, 6, 3.4, 6, BK2); a(-3, 12.4, 19, 6, 2.4, 6, forged(8))
    a(-3.6, 14, 18.8, 2, 1.6, .5, WH); a(1.6, 14, 18.8, 2, 1.6, .5, WH)
    for x in (-2.6, -1, .6, 2.2):
        a(x, 11.2, 25, .9, 1.6, .5, BONE)
    for s in (-1, 1):
        a(s * 3 - (1.4 if s < 0 else 0), 17, 12, 1.4, 4, 2, ASH2, ("z", -s * 20, (s * 3, 17, 13)))
    for i in range(7):
        a(-1.2, 16, 14 - i * 3.6, 2.4, 3 + (i % 2) * 2.4, 2.4, flame(i))
    for i in range(5):
        a(-1, 12 + i * 1.6, -12 - i * 2.4, 2, 2, 2.6, flame(i) if i > 1 else BK2)
    return m.b


def turtle():
    m = M(6)
    a = m.a
    for x, z in ((-13, 7), (9, 7), (-13, -11), (9, -11)):
        a(x, 0, z, 5, 5, 6, ASH); a(x - .5, 0, z + (4 if z > 0 else -1), 6, 2, 3, BONE)
    a(-11, 4, -12, 22, 5, 24, ASH2); a(-13, 8, -14, 26, 6, 28, forged(2, OBS, OBS2, 11)); a(-11, 14, -12, 22, 5, 24, forged(4, OBS, OBS2, 9)); a(-8, 19, -9, 16, 3, 18, forged(6, OBS2, OBS, 7))
    for x, z, h in ((-5, -4, 7), (3, 2, 9), (-2, -9, 5)):
        a(x, 22, z, 5, h, 5, forged(x + 9, ASH, BK2, 5)); a(x + 1, 22 + h, z + 1, 3, 1, 3, WH)
        for i in range(3):
            a(x + 1.6, 23 + h + i * 3, z + 1.6, 1.8 - i * .4, 3, 1.8 - i * .4, GD if i == 0 else EM)
    for x, z in ((-12, -3), (9, 5), (6, -11)):
        a(x, 14, z, 3, 6, 3, CR2); a(x + .6, 20, z + .6, 1.8, 2, 1.8, WH)
    a(-4, 5, 14, 8, 6, 8, ASH); a(-3.4, 8.4, 21.8, 2, 1.6, .5, WH); a(1.4, 8.4, 21.8, 2, 1.6, .5, WH); a(-3, 5, 20, 6, 1.4, 3, BONE)
    a(-1.5, 5, -17, 3, 3, 4, ASH)
    return m.b


def shaman():
    m = M(0)
    a = m.a
    robe = lambda f, u, v, w, h: None if (v == h - 1 and u % 2) else (GD2 if v == h - 3 else mix(ASH2, ASH, v / h) if (u + v) % 5 else BK2)
    a(-4.6, 0, -3, 9.2, 15, 6, robe); a(-4, 14, -2.4, 8, 10, 4.8, forged(3, ASH, BK2, 9)); a(-1.6, 10, 2.5, 3.2, 12, .5, lambda f, u, v, w, h: CR2 if v % 4 else GD)
    a(-4.4, 24, -4.4, 8.8, 9.4, 8.8, lambda f, u, v, w, h: None if (f == "front" and 2 <= v <= 8 and 1 <= u <= 6) else (ASH if (u + v) % 3 else BK2))
    a(-3, 24.4, -1, 6, 7.6, 4.6, BONE); a(-2.2, 28.4, 3.6, 1.4, 1.6, .4, CR2); a(.8, 28.4, 3.6, 1.4, 1.6, .4, CR2); a(-.4, 25.4, 3.6, .8, 2, .4, BK)
    for s in (-1, 1):
        for i, (dx, dy, w) in enumerate([(3.4, 31, 1.8), (4.6, 33.4, 1.4), (5, 36, 1)]):
            a(s * dx - (w if s < 0 else 0), dy, 0, w, 3, w, BONE)
        a(4.4 if s > 0 else -7.4, 13, -2, 3, 11, 4, robe)
    a(-9, 4, 2, 1.2, 34, 1.2, (84, 60, 48)); a(-10.6, 36, .4, 4.4, 1, 4.4, GD)
    for k in range(4):
        ang = k / 4 * math.tau
        a(-8.4 + math.cos(ang) * 2.6 - .5, 37, 2.6 + math.sin(ang) * 2.6 - .5, 1, 4, 1, BONE)
    a(-10, 40, 1, 3.2, 3.2, 3.2, flame(1)); a(-9.4, 43, 1.6, 2, 2, 2, WH)
    for k, (x, y, z) in enumerate(((8, 26, 5), (10, 18, -1), (6, 33, -4))):
        a(x, y, z, 2.6, 4, .4, lambda f, u, v, w, h: CR2 if (u == 1 and 1 <= v <= 2) else BONE)
    return m.b


def golem():
    m = M(-13)
    a = m.a
    ob = lambda s: forged(s, OBS, OBS2, 12)
    for s in (-1, 1):
        x = 2 if s > 0 else -11
        a(x, 0, -5, 9, 6, 11, ob(s)); a(x + 1, 6, -4, 7, 10, 8, ob(s + 3))
    a(-12, 16, -7, 24, 8, 14, ob(5)); a(-15, 24, -9, 30, 18, 18, ob(7))
    a(-5, 27, 9, 10, 10, .6, CR); a(-3.4, 28.6, 9.5, 6.8, 6.8, .8, EM); a(-1.6, 30.4, 10.2, 3.2, 3.2, .8, WH)
    a(-6, 42, -5, 12, 9, 11, ob(9)); a(-4.4, 45, 6, 3, 2, .5, WH); a(1.4, 45, 6, 3, 2, .5, WH)
    for s in (-1, 1):
        x = 15 if s > 0 else -26
        a(x, 34, -8, 11, 10, 16, ob(11 + s))
        for i, (dx, dz, h, ang) in enumerate([(2, -4, 12, 12), (6, 2, 9, 26), (4, -1, 15, -6)]):
            a(x + dx, 43, dz, 3.4, h, 3.4, lambda f, u, v, w, hh: mix(WH, CR2, v / max(1, hh - 1)), ("z", -s * ang, (x + dx + 1.7, 43, dz)))
        ax = 16 if s > 0 else -25
        a(ax, 16, -5, 9, 18, 10, ob(14 + s)); a(ax - 2, 2, -7, 13, 14, 14, ob(17 + s)); a(ax, 0, 7, 9, 3, 2, CR2)
    return m.b


# ───────── 구조물 (1 = 블록 1칸) ─────────
def brick(c1, c2, glow=None):
    def f(face, u, v, w, h):
        if glow and (u * 11 + v * 7) % 53 == 0:
            return glow
        return c2 if (v % 2 == 0 and u % 4 == 0) or (v % 2 == 1 and u % 4 == 2) or (u + v) % 7 == 0 else c1
    return f


BS = (44, 40, 52); BS2 = (30, 26, 36); BAS = (78, 76, 84); BAS2 = (58, 56, 64); LAVA = (255, 120, 20)
lava = lambda f, u, v, w, h: (255, 200, 80) if (u * 3 + v * 5) % 11 == 0 else LAVA if (u + v) % 3 else (230, 80, 10)


def citadel():
    m = M(10)
    a = m.a
    a(-34, -4, -30, 68, 3, 60, lava)                                              # 용암 호수
    a(-24, -2, -22, 48, 5, 44, brick(BAS, BAS2))                                   # 기단
    for x, z in ((-24, -22), (17, -22), (-24, 15), (17, 15)):                      # 모서리 탑
        a(x, 3, z, 7, 22, 7, brick(BS, BS2, CR2)); a(x - 1, 25, z - 1, 9, 2, 9, BAS2)
        for dx, dz in ((0, 0), (6, 0), (0, 6), (6, 6), (3, 0), (3, 6), (0, 3), (6, 3)):
            a(x + dx, 27, z + dz, 1, 2, 1, BS2)
        a(x + 2.5, 27, z + 2.5, 2, 3, 2, flame(x))
    a(-17, 3, -22, 34, 12, 3, brick(BS, BS2)); a(-17, 3, 19, 13, 12, 3, brick(BS, BS2)); a(4, 3, 19, 13, 12, 3, brick(BS, BS2))   # 성벽
    a(-24, 3, -15, 3, 12, 30, brick(BS, BS2)); a(21, 3, -15, 3, 12, 30, brick(BS, BS2))
    a(-4, 3, 19, 8, 3, 3, BS2); a(-5, 11, 18.5, 10, 5, 4, brick(BS, BS2, GD)); a(-2, 13, 22.5, 4, 3, .4, GD)                           # 문
    a(-3, -1, 22, 6, 1, 14, brick(BAS, BAS2))                                       # 다리
    for z in (25, 30, 35):
        a(-4, 0, z, 1, 3, 1, BS2); a(3, 0, z, 1, 3, 1, BS2)
    a(-9, 3, -12, 18, 20, 18, brick(BS, BS2, CR2)); a(-11, 23, -14, 22, 3, 22, BAS2)  # 중앙 용광로 탑
    a(-7, 26, -10, 14, 12, 14, brick(BS2, BS, GD)); a(-5, 38, -8, 10, 3, 10, BAS2); a(-3, 41, -6, 6, 9, 6, brick(BS, BS2))
    a(-2, 50, -5, 4, 1, 4, lava)
    for i in range(6):
        a(-1.5 + math.sin(i) * 1.2, 51 + i * 3, -4.5 + math.cos(i * 1.7) * 1.2, 3 - i * .35, 3, 3 - i * .35, WH if i < 2 else GD if i < 4 else CR2)
    for x in (-10, 9.4):                                                           # 용암 줄기
        a(x, 3, -5, .6, 20, 4, lava)
    a(-2, 3, 6, 4, 9, .6, lava)
    a(-4, 3, 8, 8, 1, 8, brick(BAS, BAS2, GD)); a(-1.5, 4, 10.5, 3, 2, 3, BS2); a(-1, 6, 11, 2, 2, 2, flame(3))   # 보스 제단
    for x, z in ((-18, -8), (14, -8), (-18, 8), (14, 8)):                          # 사슬 기둥 + 화로
        a(x, 3, z, 2, 6, 2, BAS2); a(x - .5, 9, z - .5, 3, 1, 3, GD2); a(x + .2, 10, z + .2, 1.6, 2, 1.6, flame(x + z))
    return m.b


def watchtower():
    m = M(0)
    a = m.a
    a(-8, -2, -8, 16, 2, 16, brick((110, 40, 40), (90, 30, 34)))
    a(-4, 0, -4, 8, 4, 8, brick(BAS, BAS2)); a(-3, 4, -3, 6, 16, 6, brick(BS, BS2, CR2)); a(-1, 0, 4, 2, 3, .4, BS2)
    for y in (8, 14):
        a(-.6, y, 3, 1.2, 2, .3, CR2)
    a(-5, 20, -5, 10, 1, 10, BAS2)
    for dx, dz in ((-5, -5), (4, -5), (-5, 4), (4, 4), (-.5, -5), (-.5, 4), (-5, -.5), (4, -.5)):
        a(dx, 21, dz, 1, 2, 1, BS2)
    a(-1.5, 21, -1.5, 3, 1, 3, GD2)
    for i in range(4):
        a(-1 + i * .2, 22 + i * 1.6, -1 + i * .2, 2 - i * .4, 1.8, 2 - i * .4, WH if i == 0 else GD if i < 3 else CR2)
    a(4, 21, -.5, 5, .6, 1, BS2)
    for i in range(5):
        a(8, 20.4 - i * 1.2, -.3, .6, 1.2, .6, ASH2)
    a(7.2, 12.4, -1.1, 2.2, 2.6, 2.2, forged(3, trim=GD))
    return m.b


if __name__ == "__main__":
    out = sys.argv[1]
    BG = (34, 18, 22)
    V = lambda b, **k: render(b, size=(360, 440), bg=BG, **k)
    sheet("5.0 네더 · 겁화 장비 / 보스 「겁화의 군주」", [("겁화 장비 (앞)", V(gear(), yaw=-28, pitch=10, scale=10)), ("겁화 장비 (뒤)", V(gear(), yaw=150, pitch=10, scale=10)),
                                              ("겁화의 군주 (앞)", V(lord(), yaw=-24, pitch=8, scale=2.6)), ("겁화의 군주 (옆·뒤)", V(lord(), yaw=140, pitch=10, scale=2.6))], out + "/nether_a.png")
    sheet("5.0 네더 · 새 몬스터 4종", [("잿불 사냥개", V(hound(), yaw=-38, pitch=14, scale=8)), ("용암 거북", V(turtle(), yaw=-42, pitch=20, scale=8)),
                                 ("재의 주술사", V(shaman(), yaw=-26, pitch=10, scale=8.4)), ("흑요 골렘", V(golem(), yaw=-28, pitch=10, scale=5.4))], out + "/nether_b.png")
    sheet("5.0 네더 · 새 구조물", [("겁화의 성채 (앞)", V(citadel(), yaw=-32, pitch=28, scale=5.0)), ("겁화의 성채 (뒤)", V(citadel(), yaw=148, pitch=28, scale=5.0)),
                              ("잿불 감시탑", V(watchtower(), yaw=-34, pitch=20, scale=11))], out + "/nether_c.png")
    print("ok")
