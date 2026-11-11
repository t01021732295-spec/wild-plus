"""5.0 생활 도안: 양봉 상자 · 건조대 · 장독대 · 양어장 (3D 상자). 좌표: x 오른쪽, y 위, +z 정면, 16 = 1칸."""
import math

from cosmetic_design import Box, mix, shade, solid as S

WOOD = (176, 128, 78); WOOD_D = (126, 86, 50); HONEY = (240, 176, 48); HONEY_L = (255, 214, 96); WHITE = (244, 240, 230)
CLAY = (112, 66, 46); CLAY_D = (78, 44, 32); CLAY_L = (150, 96, 66); ROPE = (214, 190, 140); WATER = (70, 150, 220); STONE = (136, 138, 146)


def planks(c, step=4):
    return lambda f, u, v, W, H: shade(c, 0.86) if v % step == step - 1 else shade(c, 1.0 + ((u * 7 + v // step * 13) % 5 - 2) * 0.025)


def beehive():
    """양봉 상자: 받침 다리 + 2단 벌통 + 지붕, 앞에 벌 드나드는 구멍과 꿀 흐른 자국"""
    comb = lambda f, u, v, W, H: HONEY_L if (u + (v // 2) * 2) % 4 < 2 and v % 2 == 0 else HONEY
    out = [Box(-7, 0, -7, 2, 3, 2, S(WOOD_D)), Box(5, 0, -7, 2, 3, 2, S(WOOD_D)), Box(-7, 0, 5, 2, 3, 2, S(WOOD_D)), Box(5, 0, 5, 2, 3, 2, S(WOOD_D)),
           Box(-8, 3, -8, 16, 1, 16, S(WOOD_D)),
           Box(-7, 4, -7, 14, 6, 14, planks(WHITE, 3)), Box(-7.5, 10, -7.5, 15, 1, 15, S(WOOD)), Box(-7, 11, -7, 14, 6, 14, planks((250, 214, 120), 3)),
           Box(-8.5, 17, -8.5, 17, 1.5, 17, S(WOOD_D)), Box(-7.5, 18.5, -7.5, 15, 1.5, 15, S(shade(WOOD_D, 1.15))),
           Box(-3, 4.6, 7, 6, 1.4, .4, S((40, 28, 20))), Box(-4, 4, 7, 8, .6, 2, S(WOOD)),             # 출입구 · 발판
           Box(-2, 12.5, 7, 4, 3, .4, comb), Box(1.2, 9.4, 7, 1, 2.2, .3, S(HONEY)), Box(1.4, 8.4, 7, .6, 1, .3, S(HONEY_L)),
           Box(-7.4, 13, -2, .4, 2, 4, S(WOOD_D)), Box(7, 13, -2, .4, 2, 4, S(WOOD_D))]                  # 손잡이
    for x, y, z in ((-5, 21.5, 2), (4, 23, -3), (7.5, 14, 9), (-9.5, 8, 4)):                              # 벌
        out += [Box(x, y, z, 2, 1.4, 1.4, lambda f, u, v, W, H: (40, 30, 20) if u % 2 else (250, 200, 50)), Box(x + .4, y + 1.3, z + .2, 1.2, .3, 1, S((230, 240, 250)))]
    return out


def drying_rack():
    """건조대: 나무 틀 + 줄 2단에 고추 · 생선 · 고기를 널어 말림"""
    out = [Box(-8, 0, -5, 1.5, 20, 1.5, S(WOOD_D)), Box(6.5, 0, -5, 1.5, 20, 1.5, S(WOOD_D)), Box(-8, 0, 3.5, 1.5, 20, 1.5, S(WOOD_D)), Box(6.5, 0, 3.5, 1.5, 20, 1.5, S(WOOD_D)),
           Box(-8, 19, -5, 16, 1.5, 1.5, S(WOOD)), Box(-8, 19, 3.5, 16, 1.5, 1.5, S(WOOD)), Box(-8, 5, -5, 1.5, 1.2, 10, S(WOOD)), Box(6.5, 5, -5, 1.5, 1.2, 10, S(WOOD)),
           Box(-8, 19, -5, 1.5, 1.5, 10, S(WOOD)), Box(6.5, 19, -5, 1.5, 1.5, 10, S(WOOD)),
           Box(-6.5, 17.5, 4, 13, .4, .4, S(ROPE)), Box(-6.5, 12, -4.4, 13, .4, .4, S(ROPE))]
    for k, x in enumerate((-5.5, -3.2, -.9)):                                                              # 고추
        out += [Box(x, 13.2, 3.8, 1.2, 4.2, 1, S(mix((210, 40, 34), (150, 24, 24), k / 2))), Box(x + .3, 17.2, 4, .6, .6, .6, S((70, 120, 50)))]
    for x in (1.6, 4.2):                                                                                   # 생선
        out += [Box(x, 12.2, 3.9, 1.8, 5, .8, lambda f, u, v, W, H: (176, 190, 200) if v % 2 else (140, 156, 170)), Box(x + .2, 11.2, 4, 1.4, 1.2, .6, S((120, 136, 150)))]
    for x in (-5, -1.5, 2):                                                                                # 육포
        out += [Box(x, 7.4, -4.6, 2.6, 4.6, .7, lambda f, u, v, W, H: (120, 52, 40) if (u + v) % 3 else (150, 76, 56))]
    return out


def jar(x, z, r, h, lid=True):
    b = [Box(x - r * .7, 0, z - r * .7, r * 1.4, h * .16, r * 1.4, S(CLAY_D)), Box(x - r * .92, h * .16, z - r * .92, r * 1.84, h * .2, r * 1.84, S(CLAY)),
         Box(x - r, h * .36, z - r, r * 2, h * .34, r * 2, lambda f, u, v, W, H: CLAY_L if (v == 1 and u % 4 < 3) else CLAY), Box(x - r * .88, h * .7, z - r * .88, r * 1.76, h * .16, r * 1.76, S(CLAY)),
         Box(x - r * .66, h * .86, z - r * .66, r * 1.32, h * .08, r * 1.32, S(CLAY_D))]
    if lid:
        b += [Box(x - r * .82, h * .94, z - r * .82, r * 1.64, h * .07, r * 1.64, S(CLAY_L)), Box(x - r * .22, h * 1.01, z - r * .22, r * .44, h * .07, r * .44, S(CLAY_D))]
    return b


def jangdok():
    """장독대: 돌 받침 위에 크고 작은 항아리 셋 (하나는 뚜껑이 열려 김치가 보임)"""
    stone = lambda f, u, v, W, H: shade(STONE, 0.86) if (u % 5 == 4 or v % 4 == 3) else shade(STONE, 1 + ((u // 5 + v // 4) % 3 - 1) * .05)
    out = [Box(-8, 0, -8, 16, 2, 16, stone)]
    out += [Box(q.x0, q.y0 + 2, q.z0, q.w, q.h, q.d, q.paint) for q in jar(-2.6, -2.4, 4.8, 13) + jar(4.2, 3.4, 3.2, 9) + jar(-4.2, 4.6, 2.6, 7, lid=False)]
    out += [Box(-5.8, 7.6, 3, 3.2, .6, 3.2, lambda f, u, v, W, H: (206, 60, 44) if (u + v) % 2 else (236, 220, 190)), Box(-1.4, 2, 5.6, 3, .8, 3, S(CLAY_L))]
    return out


def fish_farm():
    """양어장 (2칸 폭): 나무 틀 수조 + 물 + 물고기 그림자 · 수초 · 먹이통 · 그물"""
    def water(f, u, v, W, H):
        if f != "top":
            return mix(WATER, (40, 90, 170), v / max(1, H - 1))
        fish = ((6, 5), (7, 5), (8, 5), (9, 6), (18, 10), (19, 10), (20, 10), (17, 11), (24, 4), (25, 4), (26, 4))
        if (u, v) in fish:
            return (246, 150, 60) if u < 12 else (220, 226, 236) if u < 22 else (240, 96, 80)
        return shade(WATER, 1.12) if (u + v * 2) % 9 == 0 else WATER
    out = [Box(-16, 0, -8, 32, 2, 16, planks(WOOD_D)), Box(-16, 2, -8, 32, 9, 1.5, planks(WOOD)), Box(-16, 2, 6.5, 32, 9, 1.5, planks(WOOD)),
           Box(-16, 2, -6.5, 1.5, 9, 13, planks(WOOD)), Box(14.5, 2, -6.5, 1.5, 9, 13, planks(WOOD)), Box(-14.5, 2, -6.5, 29, 7.4, 13, water),
           Box(-16.4, 4, -8.4, 32.8, 1, .6, S((90, 96, 104))), Box(-16.4, 4, 7.8, 32.8, 1, .6, S((90, 96, 104))),
           Box(-12, 9.4, -5, 1, 2.6, 1, S((60, 150, 80))), Box(-10.6, 9.4, -4, 1, 1.8, 1, S((80, 176, 96))), Box(10, 9.4, 3, 1, 2.2, 1, S((60, 150, 80))),
           Box(11.5, 11, -8, 4, 3, 3, planks(WOOD_D, 2)), Box(12, 13.6, -7.5, 3, .6, 2, S((226, 190, 120))),      # 먹이통
           Box(-15, 11, 4, .8, 9, .8, S(WOOD_D)), Box(-17, 19, 2.6, 5, .6, 3.6, lambda f, u, v, W, H: (230, 230, 220) if (u + v) % 2 else None)]  # 뜰채
    return out
