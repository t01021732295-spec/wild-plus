"""5.0 DPS 허수아비 도안: 나무 기둥 받침 + 짚 몸통(밧줄로 묶음) + 가슴 과녁 + 자루 머리 + 위에 떠 있는 DPS 판."""
import sys

from cosmetic_design import Box, mix, render, shade, sheet, solid as S

WOOD = (150, 104, 62); WOOD_D = (104, 70, 40); STRAW = (226, 190, 96); STRAW_D = (186, 146, 62); ROPE = (122, 86, 52)
SACK = (214, 196, 160); SACK_D = (170, 150, 116); RED = (206, 52, 48); WHITE = (244, 240, 230); STONE = (128, 128, 134)


def straw(f, u, v, w, h):
    if f in ("top", "bottom"):
        return STRAW_D
    return STRAW if (u * 3 + v) % 5 else STRAW_D if (u + v) % 2 else shade(STRAW, 1.08)


def sack(f, u, v, w, h):
    if f == "front":
        if v in (h - 4, h - 5) and u in (2, 3, w - 3, w - 4):
            return (40, 30, 26)                    # 꿰맨 눈
        if v == 3 and 2 <= u <= w - 3:
            return (90, 60, 40) if u % 2 else SACK_D  # 꿰맨 입
    return SACK if (u + v * 2) % 7 else SACK_D


def target(f, u, v, w, h):
    if f != "front":
        return WOOD_D
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = max(abs(u - cx), abs(v - cy))
    return RED if d < 1 else WHITE if d < 2 else RED if d < 3 else WHITE


def dummy():
    b = [Box(-7, 0, -7, 14, 2, 14, lambda f, u, v, w, h: STONE if (u + v) % 4 else shade(STONE, 0.9)),
         Box(-5, 2, -5, 10, 2, 10, S(WOOD_D)),
         Box(-1.5, 4, -1.5, 3, 18, 3, lambda f, u, v, w, h: WOOD if v % 6 else WOOD_D),          # 기둥
         Box(-10, 22, -1.5, 20, 2.5, 3, lambda f, u, v, w, h: WOOD if u % 6 else WOOD_D),        # 가로대 (팔)
         Box(-6, 14, -4, 12, 14, 8, straw),                                                      # 짚 몸통
         Box(-6.4, 17, -4.4, 12.8, 1.2, 8.8, S(ROPE)), Box(-6.4, 24, -4.4, 12.8, 1.2, 8.8, S(ROPE)),
         Box(-4, 18.5, 4, 8, 8, 0.6, target),                                                    # 가슴 과녁
         Box(-12, 20.5, -2.5, 3, 5, 5, straw), Box(9, 20.5, -2.5, 3, 5, 5, straw),               # 팔 끝 짚뭉치
         Box(-12.3, 22, -2.8, 3.6, 1, 5.6, S(ROPE)), Box(8.7, 22, -2.8, 3.6, 1, 5.6, S(ROPE)),
         Box(-4.5, 28, -4.5, 9, 9, 9, sack),                                                      # 자루 머리
         Box(-5, 28, -5, 10, 1.2, 10, S(ROPE)), Box(-1, 37, -1, 2, 2, 2, S(SACK_D)),
         Box(-2, 15, 4.1, 1.2, 1.6, 0.4, S((60, 60, 60))), Box(2.6, 26, 4.1, 1, 1, 0.4, S((60, 60, 60)))]  # 화살 자국
    # 위에 떠 있는 DPS 판 (게임에서는 글자로 표시)
    def panel(f, u, v, w, h):
        if f != "front":
            return (30, 30, 40)
        if v in (0, h - 1) or u in (0, w - 1):
            return (240, 196, 80)
        return (24, 26, 36)
    b += [Box(-9, 42, 0, 18, 7, 0.6, panel)]
    return b


if __name__ == "__main__":
    out = sys.argv[1]
    BG = (150, 196, 150)
    V = lambda **k: render(dummy(), size=(360, 440), bg=BG, **k)
    sheet("DPS 허수아비 도안", [("앞 대각", V(yaw=-28, pitch=10, scale=7.5)), ("옆", V(yaw=-80, pitch=6, scale=7.5)), ("뒤", V(yaw=150, pitch=10, scale=7.5))], out)
