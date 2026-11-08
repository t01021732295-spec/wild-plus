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
    # 흉조 병 받침 6 (바닐라 흉조 병 모양)
    for k, x in enumerate((-11, -7, -3, 1, 5, 9)):
        z = 5 if k % 2 == 0 else 3
        out += [Box(x - 0.2, 14, z - 0.2, 3.4, 1, 3.4, C((110, 104, 120)))] + omen_bottle(x, 15, z, 1.0)
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


def prison_big():
    """3.9 우민 감옥 (가로 3 · 깊이 2 · 높이 2): 안이 훤히 보이는 쇠창살 감방 4칸 자리 · 뒷벽 · 지붕 · 자물쇠 문"""
    st = stone((116, 114, 110))
    brick = lambda f, u, v, W, H: shade((128, 124, 118), 0.85 if (v % 4 == 0 or (u + (v // 4) * 4) % 8 == 0) else 1.0)
    iron = metal((84, 86, 92))
    out = [Box(-24, 0, -16, 48, 1.5, 32, st)]
    # 뒷벽 (돌벽돌) + 작은 창 + 쇠사슬 + 횃불
    out += [Box(-24, 1.5, -16, 48, 30.5, 2.5, brick)]
    out += [Box(-3, 20, -13.6, 6, 5, 0.3, C((30, 34, 44)))] + [Box(-2.5 + k * 2, 20, -13.4, 0.8, 5, 0.4, iron) for k in range(3)]
    for x in (-18, 14):
        for k in range(6):
            out.append(Box(x, 26 - k * 2.4, -13.4, 1.2, 2, 1.2, iron))
        out.append(Box(x - 1, 11, -13.6, 3.2, 1.4, 1.6, iron))
    out += [Box(21, 18, -13.5, 1, 6, 1, planks(DARKWOOD)), Box(20.8, 24, -13.7, 1.4, 2, 1.4, glow((255, 240, 160), (255, 150, 40)))]
    # 쇠창살: 앞 · 양옆 (가늘고 성기게 → 안의 포로가 잘 보임)
    for x in range(-23, 24, 6):
        out.append(Box(x, 1.5, 15.2, 0.7, 29, 0.7, iron))
    for z in range(-10, 16, 6):
        out += [Box(-23.6, 1.5, z, 0.7, 29, 0.7, iron), Box(22.9, 1.5, z, 0.7, 29, 0.7, iron)]
    for y in (1.5, 28.8):
        out += [Box(-24, y, 14.8, 48, 1.2, 1.5, iron), Box(-24, y, -13.5, 1.5, 1.2, 29.8, iron), Box(22.5, y, -13.5, 1.5, 1.2, 29.8, iron)]
    # 안쪽 천장 등불
    out += [Box(-0.5, 27, -1, 1, 3, 1, iron), Box(-1.5, 24, -2, 3, 3, 3, glow((255, 236, 150), (240, 170, 60)))]
    # 지붕: 짙은 나무 들보 + 판자
    out += [Box(-24.5, 30.5, -16.5, 49, 1.5, 33, planks(DARKWOOD))]
    for x in (-24, -8, 8, 22):
        out.append(Box(x, 29.5, -16, 2, 1, 32, planks(shade(DARKWOOD, 0.8))))
    # 자물쇠 문 (가운데)
    for x in (-4, -1.5, 1, 3.5):
        out.append(Box(x, 1.5, 15.6, 0.7, 27, 0.7, metal((110, 112, 118))))
    out += [Box(-4, 14, 15.6, 8.2, 1, 0.7, metal((110, 112, 118))), Box(-4, 27.5, 15.6, 8.2, 1, 0.7, metal((110, 112, 118))),
            Box(2, 12, 16.1, 3, 3, 1.2, C((210, 170, 60))), Box(3, 15, 16.2, 1, 1.5, 1, C((190, 150, 50)))]
    # 바닥 짚 4자리 · 양동이 · 그릇
    hay = lambda f, u, v, W, H: (220, 190, 90) if (u + v) % 3 else (190, 160, 70)
    for (x, z) in ((-20, -10), (-8, -10), (4, -10), (14, -10)):
        out.append(Box(x, 1.5, z, 8, 1, 6, hay))
    out += [Box(18, 1.5, 8, 4, 4, 4, metal((120, 110, 90))), Box(-21, 1.5, 9, 4, 1.5, 4, C((140, 100, 60)))]
    return out


def omen_bottle(x, y, z, s=1.0):
    """바닐라 흉조 병 모양 작은 상자: 청록 유리 테두리 · 보라 물약 · 붉은 눈 · 마개"""
    def body(f, u, v, W, H):
        if u in (0, W - 1) or v in (0, H - 1):
            return (74, 200, 213) if (u + v) % 2 else (58, 120, 127)
        if f == "front" and v == H // 2 and u in (W // 2 - 1, W // 2):
            return (255, 0, 72)
        return (73, 51, 72) if v < H // 2 else (35, 31, 42)
    b = Box(x, y, z, 3 * s, 3 * s, 3 * s, body)
    b.res = 2
    return [b, Box(x + 0.9 * s, y + 3 * s, z + 0.9 * s, 1.2 * s, 0.8 * s, 1.2 * s, C((74, 200, 213))),
            Box(x + 0.8 * s, y + 3.8 * s, z + 0.8 * s, 1.4 * s, 0.8 * s, 1.4 * s, C((126, 69, 60)))]


SKINS = {}  # draw 스크립트가 채움: {"smuggler": PIL 64x64, "mercenary": ...}


def smuggler():
    """우민 밀수꾼: 바닐라 변명자 체형 · 보라 두건 망토 · 가죽 조끼 · 큰 배낭 (흉조 병 매달림) · 허리 등불"""
    from illager_skins import illager_boxes
    out = illager_boxes(SKINS["smuggler"], arms="crossed", hat=True)
    leather = planks((120, 84, 50))
    out += [Box(-4.5, 9, -9, 9, 14, 5.5, leather), Box(-5, 22, -9.5, 10, 2.5, 6.5, C((80, 54, 34))),
            Box(-3, 12, -9.6, 6, 5, 0.8, C((96, 66, 40))), Box(-1, 14, -10, 2, 1.5, 0.6, C((200, 170, 80)))]
    out += omen_bottle(-7.6, 13, -8, 0.8) + omen_bottle(5, 15, -8, 0.8) + omen_bottle(-7.6, 18, -6.5, 0.7)
    out += [Box(5.2, 12, 0.5, 0.5, 4, 0.5, C((50, 50, 54))), Box(4.5, 8, -0.2, 2, 4, 2, glow((255, 230, 140), (230, 150, 50))),
            Box(4.3, 12, -0.4, 2.4, 0.6, 2.4, C((50, 50, 54))), Box(4.3, 7.6, -0.4, 2.4, 0.6, 2.4, C((50, 50, 54)))]
    return out


def mercenary():
    """우민 용병 (전향한 변명자): 바닐라 변명자 체형 · 마을 파란 겉옷 · 철모 · 철 어깨 · 도끼 · 마을 문장 방패"""
    from illager_skins import illager_boxes
    out = illager_boxes(SKINS["mercenary"], arms="down", hat=True)
    steel = metal((180, 184, 192))
    out += [Box(-8.6, 21, -2.6, 5.2, 3.2, 5.2, steel), Box(3.4, 21, -2.6, 5.2, 3.2, 5.2, steel)]
    # 오른손 도끼 (앞으로 뻗음)
    out += [Box(-6.6, 11, -1, 1.2, 1.2, 14, planks(DARKWOOD)), Box(-7, 10, 10, 2, 5, 3.5, steel), Box(-6.8, 9, 12.5, 1.6, 7, 1, C((230, 232, 238)))]
    # 왼팔 방패 (마을 문장)
    crest = lambda f, u, v, W, H: (240, 200, 70) if f == "right" and (abs(u - W / 2) < 1.2 and 2 < v < H - 3 or (v == H - 4 and abs(u - W / 2) < 3)) else (40, 70, 150) if f == "right" else (110, 80, 50)
    out += [Box(8, 8, -4, 1.2, 13, 9, crest)]
    return out


DESIGNS = {
    "omen_altar": ("흉조 제단 · 2칸 · 마을 수호 게이지", omen_altar),
    "illager_prison": ("우민 감옥 · 2칸 · 포로 몸값 · 전향 · 심문", prison),
    "illager_prison_big": ("우민 감옥 (3.9) · 가로 3 · 깊이 2 · 높이 2 · 포로가 안에 보임", prison_big),
    "smuggler": ("우민 밀수꾼 · 흉조일 때만 나타남", smuggler),
    "mercenary": ("우민 용병 · 전향한 변명자", mercenary),
}
