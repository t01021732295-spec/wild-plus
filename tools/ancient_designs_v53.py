"""5.3 고대 도시(딥다크) 도안: 몬스터 4종 (스컬크 추적자 · 메아리 박쥐 떼 · 침묵의 사제 · 스컬크 골렘) · 구조물 3종 (메아리 첨탑 · 봉인된 서고 · 잠든 성소) · 소음 게이지."""
import math
import sys

from PIL import Image, ImageDraw, ImageFont

from cosmetic_design import Box, mix, render, sheet, solid as S

DS = (40, 42, 50); DS2 = (58, 60, 70); DS3 = (26, 28, 34)          # 심층암
SC = (8, 46, 56); SC2 = (14, 74, 86); SC3 = (4, 28, 34)            # 스컬크
CY = (60, 230, 236); CY2 = (150, 255, 250); WH = (236, 250, 248)   # 영혼빛
BONE = (210, 214, 200); BONE2 = (160, 166, 152); RB = (70, 52, 90); RB2 = (44, 32, 60); GD = (190, 170, 110)
R = math.radians


class M:
    def __init__(self, dy=0):
        self.b, self.dy = [], dy

    def a(self, x, y, z, w, h, d, p, rot=None):
        if rot:
            rot = (rot[0], R(rot[1]), (rot[2][0], rot[2][1] + self.dy, rot[2][2]))
        self.b.append(Box(x, y + self.dy, z, w, h, d, p if callable(p) else S(p), rot=rot))


def sculk(seed=0, glow_rate=23):
    """스컬크: 짙은 청록 + 가끔 맥박치는 영혼빛 점"""
    def f(face, u, v, w, h):
        if (u * 7 + v * 11 + seed) % glow_rate == 0:
            return CY
        if (u * 3 + v * 5 + seed) % 9 == 0:
            return SC3
        return SC if (u + v + seed) % 3 else SC2
    return f


def deep(seed=0):
    """심층암 + 균열 사이로 스컬크"""
    def f(face, u, v, w, h):
        if (u * 5 + v * 7 + seed) % 31 == 0:
            return CY
        if (u - v + seed) % 11 == 0:
            return SC2
        return DS if (u + 2 * v + seed) % 4 else DS2
    return f


# ───────── 몬스터 ─────────
def stalker():
    """스컬크 추적자: 눈 없는 머리 · 귀 대신 스컬크 감지 촉수 · 길고 마른 팔 · 갈비뼈 사이 영혼빛 · 앞으로 숙인 자세"""
    m = M(0)
    a = m.a
    for s in (-1, 1):  # 다리 (역관절)
        x = 2 if s > 0 else -6
        a(x, 0, -2, 4, 2, 6, BONE2); a(x + .5, 2, -1, 3, 9, 3, sculk(s)); a(x + .5, 10, -3, 3, 8, 3, sculk(s + 3), ("x", 20, (x + 2, 10, -1)))
    a(-6, 17, -4, 12, 6, 8, sculk(5))
    a(-7, 22, -5, 14, 12, 9, sculk(7), ("x", 18, (0, 22, 0)))  # 숙인 상체
    for i in range(4):  # 갈비뼈
        a(-6.5, 24 + i * 2.4, 3.6, 13, 1, .6, BONE, ("x", 18, (0, 22, 0)))
    a(-3, 26, 3.4, 6, 6, .5, CY, ("x", 18, (0, 22, 0))); a(-1.5, 27.5, 3.8, 3, 3, .5, CY2, ("x", 18, (0, 22, 0)))
    a(-4.5, 32, 4, 9, 8, 9, sculk(9))  # 머리 (얼굴 없음)
    a(-3.5, 33, 12.6, 7, 2, .5, BONE); a(-3, 34, 12.8, 1, 2, .4, WH); a(-1, 34, 12.8, 1, 2, .4, WH); a(1, 34, 12.8, 1, 2, .4, WH); a(2.6, 34, 12.8, 1, 2, .4, WH)
    for s in (-1, 1):  # 감지 촉수 (귀)
        for i in range(3):
            a(s * (5 + i * 2.4) - (1.6 if s < 0 else 0), 37 + i * 1.6, 7 - i, 1.6, 4 - i * .6, 1.6, CY if i == 2 else SC2, ("z", -s * (30 + i * 14), (s * 5, 37, 7)))
    for s in (-1, 1):  # 긴 팔 + 갈고리 손
        x = 7 if s > 0 else -10
        a(x, 22, 2, 3, 12, 3, sculk(11 + s), ("x", -10, (x + 1.5, 33, 3)))
        a(x, 10, 6, 3, 12, 3, sculk(13 + s), ("x", -30, (x + 1.5, 22, 7)))
        for k in range(3):
            a(x + k, 4, 12 + (k % 2), 1, 5, 1, BONE, ("x", -40, (x + k, 9, 12)))
    return m.b


def bats():
    """메아리 박쥐 떼: 영혼빛 날개의 작은 박쥐 3마리가 하나의 소리 덩어리 주위를 돈다"""
    m = M(0)
    a = m.a
    a(-2, 14, -2, 4, 4, 4, CY); a(-1, 15, -1, 2, 2, 2, CY2)          # 가운데 소리 덩어리
    for k in range(10):
        ang = k / 10 * math.tau
        a(math.cos(ang) * 5 - .4, 16 + math.sin(ang * 2) * 1.2, math.sin(ang) * 5 - .4, .8, .8, .8, CY if k % 2 else SC2)
    for i, (x, y, z, yaw) in enumerate([(-9, 18, -2, 30), (7, 22, 1, -40), (-1, 9, 8, 0)]):
        a(x, y, z, 3, 3, 3, SC3); a(x + .6, y + 1.6, z + 2.9, .7, .7, .3, CY2); a(x + 1.8, y + 1.6, z + 2.9, .7, .7, .3, CY2)
        for s in (-1, 1):
            a(x + (3 if s > 0 else -5), y + 1, z, 5, .6, 3, lambda f, u, v, w, h, i=i: CY if (u + v + i) % 3 == 0 else SC2, ("z", -s * 22, (x + 1.5, y + 1, z)))
            a(x + (1 if s > 0 else 1.4) + (.4 if s > 0 else 0), y + 3, z + .8, .6, 1.2, .6, SC2)
    return m.b


def priest():
    """침묵의 사제: 입을 꿰맨 가면 · 해진 보랏빛 사제복 · 스컬크 비명체를 단 종 지팡이 · 떠다니는 초 3개"""
    m = M(0)
    a = m.a

    def robe(dark=0.0):
        def f(face, u, v, w, h):
            if v <= 1 and (u + v) % 2:
                return None
            c = RB if (u + v // 3) % 5 else RB2
            if face == "front" and abs(u - w / 2) < 1:
                return GD
            return mix(c, (0, 0, 0), dark)
        return f
    a(-6, 0, -5, 12, 14, 10, robe(.15)); a(-5, 14, -4, 10, 10, 8, robe()); a(-6, 22, -5, 12, 3, 10, robe(.1))
    for s in (-1, 1):
        a(s * 6 - (3 if s < 0 else 0), 13, -2, 3, 11, 4, robe(.05))
    a(-4, 24, -4, 8, 8, 8, BONE2)                                        # 머리
    a(-4.4, 24.6, 4, 8.8, 7, .6, BONE)                                   # 가면
    a(-3, 29, 4.6, 2, .8, .4, DS3); a(1, 29, 4.6, 2, .8, .4, DS3)         # 감은 눈
    a(-2.5, 26.4, 4.6, 5, .5, .4, DS3)
    for x in (-2, -.8, .4, 1.6):                                          # 꿰맨 입
        a(x, 25.8, 4.7, .4, 1.6, .4, CY)
    a(-5, 31, -5, 10, 3, 10, robe(.2)); a(-3, 34, -3, 6, 3, 6, robe(.25)); a(-1, 37, -1, 2, 2, 2, GD)  # 두건
    a(9, 0, 4, 1.6, 34, 1.6, DS2)                                         # 종 지팡이
    a(7.4, 32, 2.4, 4.8, 4, 4.8, sculk(3, 7)); a(8, 28, 3, 3.6, 4, 3.6, GD); a(9.2, 27, 4.2, 1.2, 1, 1.2, CY)
    for i, (x, z) in enumerate([(-10, 2), (-8, -7), (-12, -2)]):          # 떠다니는 초
        a(x, 20 + i * 3, z, 1.6, 3, 1.6, BONE); a(x + .5, 23 + i * 3, z + .5, .6, 1, .6, CY2)
    return m.b


def golem():
    """스컬크 골렘(정예): 심층암 덩어리 몸 · 등에서 돋은 감지 촉수 · 가슴 안 고동치는 영혼빛 · 거대한 주먹"""
    m = M(0)
    a = m.a
    for s in (-1, 1):
        x = 2 if s > 0 else -10
        a(x, 0, -4, 8, 4, 9, deep(s)); a(x + 1, 4, -3, 6, 10, 7, deep(s + 2))
    a(-11, 14, -6, 22, 8, 12, deep(4)); a(-14, 22, -8, 28, 16, 16, deep(6))
    a(-5, 25, 8, 10, 9, .6, SC3); a(-3.6, 26.4, 8.4, 7.2, 6.2, .6, CY); a(-1.6, 28, 8.8, 3.2, 3, .6, CY2)
    a(-6, 38, -5, 12, 9, 10, deep(8)); a(-4.4, 41, 5, 3, 2, .5, CY); a(1.4, 41, 5, 3, 2, .5, CY)
    for i, (x, z, ln, ang) in enumerate([(-10, -6, 12, -18), (-4, -8, 16, -6), (3, -8, 14, 8), (8, -6, 10, 22)]):  # 등 촉수
        a(x, 38, z, 2.4, ln, 2.4, lambda f, u, v, w, h: mix(SC2, CY, v / max(1, h - 1)), ("z", -ang, (x + 1.2, 38, z)))
        a(x - .3, 38 + ln - 1, z - .3, 3, 3, 3, CY2, ("z", -ang, (x + 1.2, 38, z)))
    for s in (-1, 1):
        x = 14 if s > 0 else -24
        a(x, 30, -6, 10, 9, 12, deep(11 + s))
        ax = 15 if s > 0 else -23
        a(ax, 14, -4, 8, 16, 8, deep(13 + s)); a(ax - 2, 2, -6, 12, 12, 12, deep(15 + s)); a(ax, 6, 6, 8, 3, .6, CY)
    return m.b


# ───────── 구조물 (1 = 블록 1칸) ─────────
def brick(c1, c2, glow=None):
    def f(face, u, v, w, h):
        if glow and (u * 11 + v * 7) % 47 == 0:
            return glow
        return c2 if (v % 2 == 0 and u % 4 == 0) or (v % 2 == 1 and u % 4 == 2) else c1
    return f


def spire():
    """메아리 첨탑: 딥다크 동굴 천장에 거꾸로 매달린 탑 (층마다 정예, 맨 아래 끝에 열쇠)"""
    m = M(0)
    a = m.a
    a(-14, 40, -14, 28, 3, 28, deep(1))                                 # 천장 암반
    for i, (w, h) in enumerate([(14, 8), (12, 8), (10, 7), (8, 7), (5, 6)]):
        y = 40 - sum(hh for _, hh in [(14, 8), (12, 8), (10, 7), (8, 7), (5, 6)][:i + 1])
        a(-w / 2, y, -w / 2, w, h, w, brick(DS, DS3, CY))
        a(-w / 2 - .5, y, -w / 2 - .5, w + 1, 1, w + 1, SC2)               # 층마다 스컬크 띠
        for dx, dz in ((-w / 2 - .6, -1), (w / 2 - .4, -1), (-1, -w / 2 - .6), (-1, w / 2 - .4)):
            a(dx, y + h / 2 - 1, dz, 1, 2, 2, CY)                          # 창
    a(-1, 0, -1, 2, 4, 2, CY); a(-.5, -2, -.5, 1, 2, 1, CY2)               # 끝: 메아리 수정
    for x, z in ((-9, -4), (8, 6), (-5, 9), (6, -9)):                    # 늘어진 촉수
        a(x, 26, z, 1, 14, 1, lambda f, u, v, w, h: mix(CY, SC2, v / max(1, h - 1)))
    return m.b


def archive():
    """봉인된 서고 (고대 도시 깊은 곳 금고): 공명 종 4개를 정해진 음 순서로 울리면 스컬크 문이 열림"""
    m = M(0)
    a = m.a
    a(-12, -1, -10, 24, 1, 20, brick(DS2, DS3)); a(-12, 0, -10, 24, 9, 1, brick(DS, DS3, CY))
    a(-12, 0, -10, 1, 9, 20, brick(DS, DS3)); a(11, 0, -10, 1, 9, 20, brick(DS, DS3))
    for x in (-10, -6, 4, 8):                                           # 책장
        a(x, 0, -9, 3, 6, 2, lambda f, u, v, w, h: (90, 60, 40) if v % 2 == 0 else ((140, 40, 40) if u % 3 == 0 else (60, 90, 130) if u % 3 == 1 else (180, 160, 90)))
    for i, (x, z) in enumerate([(-7, 2), (-2.5, 5), (2.5, 5), (7, 2)]):  # 공명 종 4개 (음표)
        a(x - .5, 0, z - .5, 1, 4, 1, DS2); a(x - 1, 4, z - 1, 2, 1, 2, GD); a(x - .8, 2.6, z - .8, 1.6, 1.4, 1.6, GD)
        a(x - .3, 5.2 + i * .5, z - .3, .6, .6, .6, CY2)
    a(-3, 0, -9.6, 6, 6, .6, sculk(4, 5)); a(-2, 1, -9.8, 4, 4, .4, CY)   # 봉인된 스컬크 문
    a(-1.5, 0, -6, 3, 1.4, 2, (110, 80, 50)); a(-1, 1.4, -5.6, 2, .8, 1.2, GD)  # 보물 상자
    return m.b


def sanctum():
    """잠든 성소 (워든 아레나): 원형 대성당 · 숨을 수 있는 기둥 8개 · 양털 바닥(소음 없음) · 가운데 워든이 잠든 제단"""
    m = M(0)
    a = m.a
    for k in range(32):                                                 # 바닥 (원형)
        ang = k / 32 * math.tau
        for r in range(4, 22, 3):
            a(math.cos(ang) * r - 1.5, -1, math.sin(ang) * r - 1.5, 3, 1, 3, (90, 80, 120) if (r // 3 + k) % 5 == 0 else DS3 if (r + k) % 2 else DS)
    for k in range(8):                                                  # 기둥
        ang = k / 8 * math.tau
        x, z = math.cos(ang) * 13, math.sin(ang) * 13
        a(x - 1.5, 0, z - 1.5, 3, 16, 3, brick(DS, DS3, CY)); a(x - 2, 16, z - 2, 4, 1, 4, SC2)
    for k in range(20):                                                 # 벽 + 갈비뼈 아치
        ang = k / 20 * math.tau
        a(math.cos(ang) * 22 - 1.5, 0, math.sin(ang) * 22 - 1.5, 3, 18, 3, brick(DS2, DS3))
        if k % 2 == 0:
            a(math.cos(ang) * 22 - .5, 18, math.sin(ang) * 22 - .5, 1, 4, 1, BONE)
    a(-4, 0, -4, 8, 2, 8, brick(DS2, DS3, CY)); a(-2.5, 2, -2.5, 5, 1, 5, sculk(2, 4))  # 제단
    a(-1, 3, -1, 2, 3, 2, CY); a(-.5, 6, -.5, 1, 1, 1, CY2)
    return m.b


def noise_hud(path):
    """소음 게이지 HUD 미리보기"""
    W, H = 720, 260
    im = Image.new("RGB", (W, H), (14, 18, 22))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", 20)
    f2 = ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", 15)
    d.text((24, 16), "소음 게이지 (딥다크 안에서만, 화면 위 가운데)", font=f, fill=(200, 240, 240))
    stages = [(0.25, "고요함", (60, 200, 210)), (0.55, "촉수가 꿈틀거림", (90, 220, 180)), (0.8, "추적자가 몰려옴", (230, 180, 70)), (1.0, "워든 소환!", (230, 70, 70))]
    for row, (fill, label, col) in enumerate(stages):
        y = 64 + row * 46
        d.rounded_rectangle((24, y, 504, y + 24), 6, fill=(30, 36, 42), outline=(70, 90, 96))
        for i in range(4):
            x = 24 + 120 * (i + 1)
            d.line((x, y + 2, x, y + 22), fill=(70, 90, 96), width=2)
        d.rounded_rectangle((26, y + 2, 26 + int(476 * fill), y + 22), 5, fill=col)
        d.text((520, y), f"{int(fill * 100)}% · {label}", font=f2, fill=col)
    d.text((24, H - 30), "달리기 · 점프 · 블록 부수기 · 전투 · 문 = 올라감 / 웅크리기 · 양털 위 = 거의 안 올라감 / 시간이 지나면 내려감", font=f2, fill=(150, 180, 180))
    im.save(path)


if __name__ == "__main__":
    out = sys.argv[1]
    BG = (12, 20, 26)
    V = lambda b, **k: render(b, size=(360, 420), bg=BG, **k)
    sheet("5.3 고대 도시 · 새 몬스터 4종", [("스컬크 추적자", V(stalker(), yaw=-34, pitch=10, scale=7.6)), ("메아리 박쥐 떼", V(bats(), yaw=-30, pitch=16, scale=10)),
                                     ("침묵의 사제", V(priest(), yaw=-28, pitch=10, scale=8.4)), ("스컬크 골렘 (정예)", V(golem(), yaw=-28, pitch=10, scale=5.4))], out + "/ancient_mobs.png")
    sheet("5.3 고대 도시 · 몬스터 (옆 · 뒤)", [("스컬크 추적자 (옆)", V(stalker(), yaw=-82, pitch=6, scale=7.6)), ("침묵의 사제 (뒤)", V(priest(), yaw=150, pitch=10, scale=8.4)),
                                       ("스컬크 골렘 (뒤)", V(golem(), yaw=150, pitch=12, scale=5.4))], out + "/ancient_mobs2.png")
    sheet("5.3 고대 도시 · 새 구조물 3종", [("메아리 첨탑 (천장에 매달림)", V(spire(), yaw=-32, pitch=8, scale=6.4)), ("봉인된 서고 (공명 종 퍼즐)", V(archive(), yaw=-30, pitch=34, scale=10)),
                                     ("잠든 성소 (워든 아레나)", V(sanctum(), yaw=-30, pitch=34, scale=5.6))], out + "/ancient_structures.png")
    noise_hud(out + "/ancient_noise.png")
    print("ok")
