"""3.8 흉조 도안 (3D 상자): 흉조 제단 · 포로 감옥 · 우민 밀수꾼 · 우민 용병. 좌표는 kitchen_design 과 같음."""
from cosmetic_design import Box, mix, shade
from kitchen_design import C, DARKWOOD, glow, metal, planks, stone

DEEP = (52, 50, 62)
OMEN_C = (40, 140, 130)  # 흉조 병 청록
BANNER_W = (235, 232, 222)


def banner(face, u, v, W, H):
    """우민 깃발 무늬 (흰 바탕 · 검은 얼굴 · 회색 줄)"""
    if face not in ("front", "back"):
        return (200, 196, 186)
    cx = W / 2
    if v < H * 0.12 or v > H * 0.88:
        return (60, 60, 64)
    if abs(u - cx) < W * 0.32 and H * 0.25 < v < H * 0.55:
        if H * 0.36 < v < H * 0.42 and abs(u - cx) > W * 0.1:
            return (230, 230, 230)  # 눈
        return (40, 40, 44)
    if H * 0.62 < v < H * 0.7:
        return (120, 120, 126)
    return BANNER_W


def omen_altar():
    """흉조 제단 (2칸 · 높이 2): 짙은 돌 제단 + 우민 깃발 2 · 흉조 병 받침 6개 · 가운데 수호 게이지 수정 (단계마다 빛)"""
    st = stone(DEEP)
    out = [Box(-16, 0, -10, 32, 4, 20, st), Box(-14, 4, -8, 28, 8, 16, stone(shade(DEEP, 1.15))),
           Box(-15, 12, -9, 30, 2, 18, stone((80, 76, 92)))]
    # 계단 · 테두리 금
    out += [Box(-16, 0, 10, 32, 2, 3, st), Box(-15.5, 13.6, -9.5, 31, 0.6, 19, C((200, 160, 60)))]
    # 깃발 기둥 2 + 깃발
    for x in (-15, 13):
        out += [Box(x, 14, -7, 2, 18, 2, planks(DARKWOOD)), Box(x - 3, 30, -7.5, 8, 1.2, 1, planks(DARKWOOD)),
                Box(x - 3, 16, -6.2, 8, 14, 0.4, banner)]
    # 흉조 병 받침 6 (빛나는 병)
    bottle = glow((90, 230, 210), OMEN_C)
    for k, x in enumerate((-11, -7, -3, 1, 5, 9)):
        z = 5 if k % 2 == 0 else 3
        out += [Box(x, 14, z, 3, 1, 3, C((110, 104, 120))), Box(x + 0.5, 15, z + 0.5, 2, 3, 2, bottle),
                Box(x + 0.9, 18, z + 0.9, 1.2, 1, 1.2, C((90, 70, 50)))]
    # 가운데 수정 (수호 게이지)
    crystal = glow((180, 255, 240), (60, 170, 160))
    out += [Box(-3, 14, -5, 6, 2, 6, C((60, 56, 70))), Box(-2, 16, -4, 4, 9, 4, crystal),
            Box(-1, 25, -3, 2, 3, 2, crystal), Box(-1.5, 16, -4.5, 3, 7, 0.4, glow((230, 255, 250), (120, 220, 210)))]
    return out


def prison():
    """우민 감옥 (2칸 · 높이 2): 돌 바닥 · 쇠창살 우리 · 자물쇠 문 · 쇠사슬 · 짚 · 횃불"""
    st = stone((120, 118, 114))
    iron = metal((90, 92, 98))
    out = [Box(-16, 0, -8, 32, 2, 16, st)]
    for x in range(-15, 16, 4):
        out += [Box(x, 2, 6.5, 1, 26, 1, iron), Box(x, 2, -7.5, 1, 26, 1, iron)]
    for z in range(-7, 8, 4):
        out += [Box(-15.5, 2, z, 1, 26, 1, iron), Box(14.5, 2, z, 1, 26, 1, iron)]
    for y in (2, 15, 27):
        out += [Box(-16, y, 6, 32, 1.5, 2, iron), Box(-16, y, -8, 32, 1.5, 2, iron),
                Box(-16, y, -8, 2, 1.5, 16, iron), Box(14, y, -8, 2, 1.5, 16, iron)]
    out += [Box(-16, 28.5, -8, 32, 2, 16, planks(DARKWOOD))]
    # 문 + 자물쇠
    out += [Box(-3, 2, 7, 6, 26, 0.6, lambda f, u, v, W, H: (70, 72, 78) if u % 3 else (110, 112, 118)),
            Box(1.5, 12, 7.5, 3, 3, 1.2, C((210, 170, 60))), Box(2.5, 15, 7.6, 1, 1.5, 1, C((190, 150, 50)))]
    # 짚 · 쇠사슬 · 그릇
    out += [Box(-13, 2, -6, 10, 1.2, 8, lambda f, u, v, W, H: (220, 190, 90) if (u + v) % 3 else (190, 160, 70))]
    for k in range(5):
        out.append(Box(9, 20 - k * 2.5, -6.5, 1.2, 2, 1.2, iron))
    out += [Box(8, 7, -7, 3, 1, 3, iron), Box(6, 2, -2, 4, 2, 4, C((140, 100, 60)))]
    # 횃불
    out += [Box(15.5, 18, 7.5, 1, 6, 1, planks(DARKWOOD)), Box(15.3, 24, 7.3, 1.4, 2, 1.4, glow((255, 240, 160), (255, 150, 40)))]
    return out


def illager(ox=0, oz=0, robe=(60, 60, 66), trim=(40, 40, 46), extra=()):
    """우민 체형: 큰 머리 · 긴 코 · 일자 눈썹 · 회색 피부 · 팔짱"""
    skin = (150, 160, 158)

    def head(f, u, v, W, H):
        if f == "top":
            return (40, 40, 44)
        if f == "front":
            if v == 3 and 1 <= u <= 6:
                return (40, 40, 44)  # 일자 눈썹
            if v == 4 and u in (2, 5):
                return (40, 120, 70)
            return skin
        return skin if v > 1 else (40, 40, 44)
    out = [Box(ox - 4, 24, oz - 4, 8, 10, 8, head), Box(ox - 1, 26, oz + 4, 2, 4, 2, C(shade(skin, 0.9)))]
    out += [Box(ox - 4, 12, oz - 3, 8, 12, 6, lambda f, u, v, W, H: robe if (u + v) % 7 else shade(robe, 1.1)),
            Box(ox - 4.2, 0, oz - 3.2, 8.4, 12, 6.4, C(trim))]
    out += [Box(ox - 6, 17, oz + 2.5, 12, 4, 4, C(shade(robe, 0.9)))]  # 팔짱
    out += list(extra)
    return out


def smuggler():
    """우민 밀수꾼: 두건 망토 · 큰 배낭 (흉조 병 매달림) · 등불"""
    hood = (60, 40, 70)
    ex = [Box(-4.6, 30, -4.6, 9.2, 5, 9.2, C(hood)), Box(-4.8, 22, -4.8, 9.6, 10, 2, C(hood)),
          Box(-5, 3, -5, 10, 20, 1.5, lambda f, u, v, W, H: hood if u % 3 else shade(hood, 1.2))]
    ex += [Box(-5, 10, -9, 10, 14, 5, planks((140, 95, 55))), Box(-5.5, 23, -8.5, 11, 2, 4, C((90, 60, 40)))]
    bottle = glow((90, 230, 210), OMEN_C)
    for (x, y) in [(-6.5, 14), (5.5, 16), (-6.5, 19)]:
        ex.append(Box(x, y, -7, 1.5, 3, 1.5, bottle))
    ex += [Box(6, 12, 3, 0.6, 6, 0.6, C((60, 60, 60))), Box(5, 9, 2, 3, 3, 3, glow((255, 230, 140), (240, 160, 60)))]
    return illager(robe=(70, 50, 80), trim=(40, 30, 50), extra=ex)


def mercenary():
    """우민 용병 (전향한 변명자): 마을 파란 옷 · 철 어깨 · 도끼 · 마을 문장 방패"""
    steel = metal((180, 184, 192))
    ex = [Box(-5, 20, -3.5, 3, 3, 7, steel), Box(2, 20, -3.5, 3, 3, 7, steel), Box(-4.4, 33, -4.4, 8.8, 2, 8.8, steel)]
    ex += [Box(6, 8, 5, 1, 14, 1, planks(DARKWOOD)), Box(5, 18, 5.5, 5, 5, 0.8, steel)]
    crest = lambda f, u, v, W, H: (240, 200, 70) if f == "front" and abs(u - W / 2) < 1.5 and 2 < v < H - 2 else (40, 90, 170)
    ex += [Box(-9, 10, -2, 1.2, 12, 8, crest)]
    return illager(robe=(50, 90, 160), trim=(40, 60, 110), extra=ex)


DESIGNS = {
    "omen_altar": ("흉조 제단 · 2칸 · 마을 수호 게이지", omen_altar),
    "illager_prison": ("우민 감옥 · 2칸 · 포로 몸값 · 전향 · 심문", prison),
    "smuggler": ("우민 밀수꾼 · 흉조일 때만 나타남", smuggler),
    "mercenary": ("우민 용병 · 전향한 변명자", mercenary),
}
