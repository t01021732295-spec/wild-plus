"""3.9 땅 경계 기둥 도안: 땅을 사면 청크 네 모서리에 저절로 세워지는 표지 (주인 색 깃발 · 등불 · 이름 팻말)."""
from cosmetic_design import Box, shade
from kitchen_design import C, DARKWOOD, glow, metal, planks, stone


def land_post(color=(70, 170, 90)):
    out = [Box(-4, 0, -4, 8, 2, 8, stone((128, 126, 120))), Box(-3, 2, -3, 6, 1, 6, stone((150, 148, 140)))]
    out += [Box(-1.5, 3, -1.5, 3, 22, 3, planks(DARKWOOD))]
    # 등불
    out += [Box(-2, 25, -2, 4, 1, 4, metal((60, 60, 64))), Box(-1.6, 26, -1.6, 3.2, 3.5, 3.2, glow((255, 236, 150), (240, 170, 60))),
            Box(-2, 29.5, -2, 4, 1, 4, metal((60, 60, 64))), Box(-0.5, 30.5, -0.5, 1, 1.2, 1, metal((60, 60, 64)))]
    # 주인 색 깃발 (양쪽으로 펄럭)
    flag = lambda f, u, v, W, H: shade(color, 1.15 if (u + v) % 5 == 0 else (0.85 if v > H - 2 else 1.0))
    out += [Box(1.5, 16, -0.3, 9, 7, 0.6, flag), Box(10.5, 17, -0.3, 1.5, 5, 0.6, lambda f, u, v, W, H: shade(color, 0.8))]
    # 이름 팻말 (앞)
    sign = lambda f, u, v, W, H: (60, 40, 24) if f == "front" and 1 <= v <= H - 2 and 1 <= u <= W - 2 and (u + v) % 4 == 0 else (196, 156, 96)
    out += [Box(-4, 8, 1.6, 8, 5, 0.8, sign), Box(-0.5, 7, 1.6, 1, 1, 0.8, planks(DARKWOOD))]
    return out


DESIGNS = {"land_post": ("땅 경계 기둥 · 내 땅 네 모서리에 자동 · 주인 색 깃발", land_post)}
