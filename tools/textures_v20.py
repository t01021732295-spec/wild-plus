"""2.18 텍스처: 낚싯대 4종 · 미끼 3종 · 비료 2종 · 새 작물 4종(단계 · 씨앗 · 열매) · 비료 살포기 · 알바 게시판."""
import colorsys
import random

from PIL import Image

from textures_v5 import OUT

T = OUT


def load(rel):
    return Image.open(T / rel).convert("RGBA")


def hue_map(im, pick, target_hue, sat=None, light=1.0):
    """pick(h,s,l) 인 픽셀의 색상을 target_hue 로 바꾼다 (명암 유지)"""
    im = im.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if not pick(h * 360, s, l):
                continue
            s2 = s if sat is None else sat
            r2, g2, b2 = colorsys.hls_to_rgb(target_hue / 360, max(0, min(1, l * light)), s2)
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return im


def green(h, s, l):
    return 70 <= h <= 190 and s > 0.15


def save(im, rel):
    p = T / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)


def sprite(rows, pal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in pal:
                px[x, y] = pal[ch] + (255,)
    return im


# ───────── 낚싯대: 비취 낚싯대의 초록 부분을 재질 색으로 ─────────
RODS = {"mythril": (200, 0.55, 1.1), "ruby": (350, 0.75, 1.0), "sapphire": (220, 0.8, 1.0), "dragon": (45, 0.9, 1.15)}


def rods():
    for name, (hue, sat, light) in RODS.items():
        for cast in ("", "_cast"):
            im = hue_map(load(f"item/jade_fishing_rod{cast}.png"), green, hue, sat, light)
            if name == "dragon":  # 용왕: 손잡이 끝에 붉은 보석
                im.putpixel((1, 14) if not cast else (1, 14), (230, 40, 60, 255))
            save(im, f"item/{name}_fishing_rod{cast}.png")


# ───────── 미끼 ─────────
WORM = ["................", "................", "................", "................",
        "..........aa....", ".........abba...", "........ab..ba..", "...aa..ab....b..",
        "..abba.ab.......", ".ab..bab........", ".b....bb........", "................",
        "................", "................", "................", "................"]
SHRIMP = ["................", "................", "......aaaa......", ".....abbbba.....",
          "....abccccba....", "...abc....cba...", "...bc......cb...", "...bc.......d...",
          "...bc......dd...", "....cb....dd....", ".....cbbbbd.....", "......cccc......",
          "................", "................", "................", "................"]


def baits():
    save(sprite(WORM, {"a": (200, 110, 120), "b": (150, 70, 85)}), "item/worm.png")
    save(sprite(SHRIMP, {"a": (255, 190, 150), "b": (240, 130, 90), "c": (200, 90, 60), "d": (255, 220, 200)}), "item/shrimp_bait.png")
    g = sprite(WORM, {"a": (255, 230, 120), "b": (220, 160, 40)})
    for p in [(3, 3), (12, 10), (13, 3)]:
        g.putpixel(p, (255, 255, 230, 255))
    save(g, "item/golden_bait.png")


def fertilizers():
    base = load("item/fertilizer.png")
    save(hue_map(base, lambda h, s, l: s > 0.1, 275, 0.55), "item/rich_fertilizer.png")
    gold = hue_map(base, lambda h, s, l: s > 0.1, 45, 0.85, 1.15)
    for p in [(4, 3), (11, 5)]:
        gold.putpixel(p, (255, 255, 220, 255))
    save(gold, "item/golden_fertilizer.png")


# ───────── 새 작물: 토마토 단계 그림의 열매 색만 바꿔서 ─────────
CROPS = {
    "grape": (275, 0.6, 0.85),    # 보라 포도
    "rice": (50, 0.35, 1.25),     # 누런 벼
    "onion": (35, 0.55, 1.1),     # 갈색 양파
    "ginseng": (25, 0.45, 1.0),   # 인삼 (빨간 열매 대신 주황)
}


def red(h, s, l):
    return (h < 25 or h > 330) and s > 0.3


def crops():
    for name, (hue, sat, light) in CROPS.items():
        for st in range(4):
            im = load(f"block/tomato_crop_stage{st}.png")
            if name == "rice":  # 벼: 잎을 연두~누렇게, 열매는 이삭색
                im = hue_map(im, green, 75 if st < 3 else 55, 0.45)
            if name == "ginseng":
                im = hue_map(im, green, 110, 0.6, 0.85)
            save(hue_map(im, red, hue, sat, light), f"block/{name}_crop_stage{st}.png")
        save(hue_map(load("item/tomato_seeds.png"), lambda h, s, l: s > 0.1, hue, sat * 0.8, light * 0.9), f"item/{name}_seeds.png")


GRAPE = ["................", ".......gg.......", "......g.........", ".....aab........",
         "....abbab.......", "....babbab......", ".....abbaab.....", ".....babbab.....",
         "......abbab.....", "......babba.....", ".......abb......", "........b.......",
         "................", "................", "................", "................"]
RICE = ["................", "........a.......", ".......aba......", "......ab.ba.....",
        ".....ab...b.....", ".....b..a..b....", "........ba......", ".......ab.......",
        "......cc........", ".....cccc.......", ".....cccc.......", "......cc........",
        "................", "................", "................", "................"]
ONION = ["................", ".......g........", "......gg........", "......g.g.......",
         ".......a........", ".....aabaa......", "....abbbbba.....", "...abbcbbbba....",
         "...abbbcbbba....", "...abbbbcbba....", "....abbbbba.....", ".....aaaaa......",
         "......d.d.......", "................", "................", "................"]
GINSENG = ["................", "......gg.g......", ".......gg.......", "........a.......",
           ".......aba......", "......abba......", "......abbba.....", ".....abbbba.....",
           "....ab.abba.....", "...a..abb.ba....", "......ab...b....", ".....a.b....a...",
           "....a...b.......", "................", "................", "................"]


def produce():
    save(sprite(GRAPE, {"a": (150, 70, 190), "b": (100, 40, 140), "g": (90, 150, 60)}), "item/grape.png")
    save(sprite(RICE, {"a": (240, 220, 150), "b": (210, 180, 100), "c": (250, 250, 240)}), "item/rice.png")
    save(sprite(ONION, {"a": (190, 130, 70), "b": (225, 175, 110), "c": (245, 215, 170), "d": (240, 235, 220), "g": (90, 160, 60)}), "item/onion.png")
    save(sprite(GINSENG, {"a": (200, 160, 110), "b": (235, 205, 160), "g": (60, 140, 60)}), "item/ginseng.png")


# ───────── 블록 ─────────
def blocks():
    rnd = random.Random(5)
    # 비료 살포기: 철판 상자 + 초록 비료 창 + 아래 분사구
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for y in range(16):
        for x in range(16):
            v = 150 + rnd.randint(-8, 8)
            px[x, y] = (v, v + 4, v + 8, 255)
    for x in range(16):
        px[x, 0] = px[x, 15] = (90, 96, 104, 255)
    for y in range(16):
        px[0, y] = px[15, y] = (90, 96, 104, 255)
    for y in range(4, 10):
        for x in range(4, 12):
            px[x, y] = (70, 140, 60, 255) if (x + y) % 3 else (110, 180, 80, 255)
    for x in range(3, 13):
        px[x, 3] = px[x, 10] = (60, 64, 70, 255)
    for x in (4, 7, 10, 13):
        px[x - 1, 13] = (40, 40, 44, 255)
    save(side, "block/fertilizer_spreader_side.png")
    top = side.copy()
    tp = top.load()
    for y in range(3, 13):
        for x in range(3, 13):
            tp[x, y] = (40, 44, 48, 255) if (x in (3, 12) or y in (3, 12)) else (120, 90, 50, 255)
    save(top, "block/fertilizer_spreader_top.png")
    # 알바 게시판: 원목 판 + 메모지 3장 + 압정
    board = Image.new("RGBA", (16, 16))
    bp = board.load()
    for y in range(16):
        for x in range(16):
            v = rnd.randint(-10, 10)
            bp[x, y] = (150 + v, 105 + v, 60 + v, 255) if y % 5 else (120, 82, 46, 255)
    for (x0, y0, w, h, c) in [(2, 2, 5, 5, (255, 248, 210)), (9, 3, 5, 6, (220, 240, 255)), (4, 9, 6, 5, (255, 225, 225))]:
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                bp[x, y] = c + (255,)
        for x in range(x0 + 1, x0 + w - 1):
            bp[x, y0 + 2] = (120, 110, 100, 255)
        bp[x0 + w // 2, y0] = (210, 40, 50, 255)
    save(board, "block/job_board_front.png")


# ───────── 2.18 추가: 행운 상자 · 에메랄드화 · 에메랄드 배낭 · 스텔라 코어 · 스카이 사파이어 날개 ─────────
BOX = ["................", "................", "...aaaaaaaaaa...", "..abbbbcbbbbba..",
       "..abbbbcbbbbba..", "..aaaaacaaaaaa..", "..adddgggdddda..", "..adddgegddddа..".replace("а", "a"),
       "..adddgggdddda..", "..addddcdddddа..".replace("а", "a"), "..addddcdddddа..".replace("а", "a"), "..addddcdddddа..".replace("а", "a"),
       "..aaaaacaaaaaa..", "................", "................", "................"]


def extras():
    pal = {"a": (60, 20, 70), "b": (150, 60, 190), "c": (255, 210, 80), "d": (120, 40, 160), "g": (240, 190, 60), "e": (255, 255, 200)}
    box = sprite(BOX, pal)
    for p_ in [(2, 1), (13, 1), (14, 12)]:
        box.putpixel(p_, (255, 255, 220, 255))
    save(box, "item/lucky_box.png")
    green = lambda h, s_, l: s_ > 0.08
    save(hue_map(load("item/diamond_coin.png"), green, 140, 0.75, 0.95), "item/emerald_coin.png")
    save(hue_map(load("item/diamond_backpack.png"), lambda h, s_, l: 150 <= h <= 220 and s_ > 0.1, 140, 0.7), "item/emerald_backpack.png")
    # 스텔라 코어: 붉은 보석 핵 + 금빛 테 + 별빛
    core = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cp = core.load()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 6.5:
                t = d / 6.5
                cp[x, y] = (int(255 - 60 * t), int(90 - 70 * t), int(120 - 80 * t), 255) if d < 5 else (240, 190, 70, 255)
    for (x, y) in [(6, 5), (7, 5), (6, 6), (9, 9)]:
        cp[x, y] = (255, 230, 240, 255)
    for (x, y) in [(1, 1), (14, 2), (13, 14), (2, 13)]:
        cp[x, y] = (255, 250, 200, 255)
    save(core, "item/stellar_core.png")


def sky_wings():
    """스카이 사파이어 날개 (겉날개 그림 자리, 4배 해상도 256×128). 날개 판 10×20 ×4 = 40×80 픽셀"""
    S = 4
    im = Image.new("RGBA", (64 * S, 32 * S), (0, 0, 0, 0))
    px = im.load()
    deep, mid, light, rim = (18, 44, 140), (40, 110, 220), (120, 210, 255), (235, 245, 255)

    def wing_face(u0, v0, w, h, outer_left):
        for v in range(h * S):
            for u in range(w * S):
                # 날개 모양: 바깥쪽 아래로 깃털 끝이 길게, 안쪽 아래는 사선으로 잘림
                uu = (w * S - 1 - u) if outer_left else u          # 0 = 몸쪽, 끝 = 바깥
                fu = uu / (w * S)                                  # 0~1
                feather = (uu // (S * 2)) % 2
                bottom = h * S * (0.55 + 0.45 * fu) - feather * S * 1.5
                if v > bottom:
                    continue
                t = v / (h * S)
                c = tuple(int(deep[i] + (mid[i] - deep[i]) * t * 1.2) for i in range(3))
                # 깃털 줄 (가로 띠) + 깃털 가장자리 밝게
                band = (v // (S * 3)) % 2
                if band:
                    c = tuple(min(255, int(x * 1.12)) for x in c)
                if v > bottom - S * 1.2:
                    c = light
                if uu % (S * 2) == 0 and v > h * S * 0.35:
                    c = tuple(int(x * 0.75) for x in c)
                # 윗날 (날개 뼈): 은빛 테
                if v < S * 1.5:
                    c = rim if v < S else (200, 220, 240)
                # 별빛 점
                if (u * 7 + v * 13) % 97 == 0 and v > S * 2:
                    c = (255, 255, 255)
                px[u0 * S + u, v0 * S + v] = c + (255,)

    # 겉날개 UV (64×32 기준): 앞 (24,2) 뒤 (36,2) 크기 10×20, 옆 (22,2)/(34,2) 2×20, 위 (24,0) 아래 (34,0) 10×2
    wing_face(24, 2, 10, 20, False)
    wing_face(36, 2, 10, 20, True)
    for (u0, v0, w, h) in [(22, 2, 2, 20), (34, 2, 2, 20), (24, 0, 10, 2), (34, 0, 10, 2)]:
        for v in range(h * S):
            for u in range(w * S):
                px[u0 * S + u, v0 * S + v] = (200, 220, 240, 255) if v < S * 2 else mid + (255,)
    save(im, "entity/equipment/wings/sky_sapphire.png")


def build():
    extras()
    sky_wings()
    rods()
    baits()
    fertilizers()
    crops()
    produce()
    blocks()
    return 4 * 2 + 3 + 2 + 4 * 5 + 4 + 3


if __name__ == "__main__":
    print(build())
