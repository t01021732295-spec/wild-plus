"""2.10 텍스처: 레이드·하늘의 탑 보스 9종 (바닐라 몹 그림을 다시 칠하고 무늬를 그려 넣음).

원본은 ci-probe 에서 받은 바닐라 몹 그림 (WILDPLUS_BOSSVAN = .../assets/minecraft/textures/entity).
"""
import colorsys
import os
import random
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, recolor


def hls(p):
    return colorsys.rgb_to_hls(*(c / 255 for c in p[:3]))


def sel(fn):
    return lambda rgb: fn(*hls(rgb))


def _tmp(im, n):
    p = Path(f"/tmp/_wp_boss_{n}.png")
    im.save(p)
    return p


def chain(src, *steps):
    """여러 번 재색칠: steps = (팔레트, 선택 함수)"""
    im = Image.open(src).convert("RGBA")
    for k, (pal, f) in enumerate(steps):
        im = recolor(_tmp(im, k), pal, f)
    return im


def speckle(im, colors, rate, seed, where=lambda p: True):
    px = im.load()
    r = random.Random(seed)
    for _ in range(int(im.width * im.height * rate)):
        x, y = r.randrange(im.width), r.randrange(im.height)
        if px[x, y][3] > 0 and where(px[x, y]):
            px[x, y] = r.choice(colors) + (255,)


def cracks(im, color, n, seed):
    """용암 균열: 어두운 칸 위로 짧게 꺾이는 선"""
    px = im.load()
    r = random.Random(seed)
    for _ in range(n):
        x, y = r.randrange(im.width), r.randrange(im.height)
        for _ in range(r.randint(3, 7)):
            if 0 <= x < im.width and 0 <= y < im.height and px[x, y][3] > 0:
                px[x, y] = color + (255,)
                if 0 <= x + 1 < im.width and px[x + 1, y][3] > 0 and r.random() < 0.3:
                    px[x + 1, y] = (255, 210, 90, 255)
            x += r.choice((-1, 0, 1))
            y += r.choice((0, 1))


def crown(im, x0, x1, y, gold=(250, 200, 60), gem=(80, 220, 255)):
    px = im.load()
    for x in range(x0, x1):
        px[x, y] = gold + (255,)
        if (x - x0) % 2 == 0:
            px[x, y + 1] = gold + (255,)
    px[(x0 + x1) // 2, y + 1] = gem + (255,)


BASALT = ["#120f10", "#221c1d", "#332a2a", "#463a38", "#5c4c48", "#7a665e"]
BONE = ["#5a5468", "#8c849a", "#bdb6c8", "#e4e0ec", "#ffffff"]
PURPLE = ["#07020c", "#160624", "#2a0c44", "#40166a", "#5c2896"]
CYAN = ["#0a6a7a", "#22c4dc", "#9ef6ff"]
ICE = ["#24456a", "#4a7aa8", "#7fb4dc", "#b8e2f8", "#eefaff"]
BARK = ["#2a1a0c", "#4a3018", "#6e4a26", "#8e6638"]
MOSS = ["#0e1e08", "#1c3610", "#2e5418", "#467a24", "#62a034"]
VOID = ["#050208", "#120a1c", "#231236", "#3a1c58", "#58307e"]
CRIMSON = ["#2a0408", "#540a12", "#801420", "#a8222e"]
GOLD = ["#5a3a04", "#8e6410", "#c89a22", "#f2cc4a", "#fff0a0"]
SAND = ["#5a4420", "#8a6c38", "#b8965a", "#dcc084", "#f6e2b0"]
FIRE = ["#0e0604", "#2a0c04", "#5a1a06", "#a03a08", "#e8761a", "#ffd04a"]
ABYSS = ["#020814", "#06162e", "#0c2a4e", "#16446e", "#2a6a94"]

dark = sel(lambda h, l, s: l < 0.42)
light_grey = sel(lambda h, l, s: l >= 0.42 and s < 0.25)
not_yellow = sel(lambda h, l, s: not (s > 0.35 and 0.1 <= h <= 0.18))
yellow = sel(lambda h, l, s: s > 0.35 and 0.1 <= h <= 0.18)
allp = sel(lambda h, l, s: True)
not_cyan_glow = sel(lambda h, l, s: not (l > 0.55 and 0.42 <= h <= 0.55 and s > 0.35))


def face_eyes(im, color, y0=9, y1=16, x0=8, x1=16):
    """얼굴 앞면에서 가장 어두운 두 점 묶음(눈)을 빛나는 색으로"""
    px = im.load()
    cands = [(sum(px[x, y][:3]), x, y) for y in range(y0, y1) for x in range(x0, x1) if px[x, y][3] > 0]
    cands.sort()
    for _, x, y in cands[:4]:
        px[x, y] = color + (255,)


def build(base: Path):
    out = OUT / "entity/boss"
    out.mkdir(parents=True, exist_ok=True)
    E = base
    # 대지의 거신: 현무암 + 용암 균열
    t = chain(E / "illager/ravager.png", (BASALT, allp))
    cracks(t, (255, 120, 20), 70, 1)
    t.save(out / "titan.png")
    # 망령 군주: 뼈빛 얼굴 + 검보라 로브 + 청록 장식 + 빛나는 눈
    t = chain(E / "illager/evoker.png", (PURPLE, sel(lambda h, l, s: l < 0.42 and not (s > 0.35 and 0.1 <= h <= 0.18))),
              (BONE, light_grey), (CYAN, yellow))
    face_eyes(t, (120, 255, 255))
    t.save(out / "lich.png")
    # 서리 여왕: 얼음 몸 + 얼음 왕관
    t = chain(E / "skeleton/stray.png", (ICE, allp))
    crown(t, 8, 16, 8, gold=(200, 240, 255), gem=(60, 140, 255))
    crown(t, 0, 8, 8, gold=(200, 240, 255), gem=(60, 140, 255))
    crown(t, 16, 32, 8, gold=(200, 240, 255), gem=(60, 140, 255))
    speckle(t, [(255, 255, 255)], 0.01, 3)
    t.save(out / "frost.png")
    # 덩굴 집행자: 나무껍질 피부 + 이끼 옷 + 덩굴
    t = chain(E / "illager/vindicator.png", (MOSS, dark), (BARK, light_grey))
    speckle(t, [(120, 200, 60), (90, 170, 40), (200, 90, 150)], 0.05, 4)
    face_eyes(t, (180, 255, 90))
    t.save(out / "vine.png")
    # 공허의 기사: 검보라 + 자홍 별가루 + 빛나는 눈
    t = chain(E / "illager/vindicator.png", (VOID, allp))
    speckle(t, [(230, 120, 255), (180, 80, 255), (255, 255, 255)], 0.03, 5)
    face_eyes(t, (240, 120, 255))
    t.save(out / "void.png")
    # 황금 왕: 진홍 옷 + 금 장식 + 왕관
    t = chain(E / "illager/vindicator.png", (CRIMSON, dark), (GOLD, sel(lambda h, l, s: 0.42 <= l < 0.6 and s >= 0.25)))
    crown(t, 8, 16, 8)
    crown(t, 0, 8, 8)
    crown(t, 16, 32, 8)
    speckle(t, [(255, 220, 80)], 0.02, 6, where=lambda p: hls(p)[1] < 0.4)
    t.save(out / "gold.png")
    # 모래 파라오: 사암 몸 + 파랑·금 줄무늬 두건
    t = chain(E / "zombie/husk.png", (SAND, allp))
    px = t.load()
    for y in range(8, 16):
        for x in list(range(0, 8)) + list(range(16, 32)):
            if px[x, y][3] > 0:
                px[x, y] = ((30, 70, 170) if y % 2 == 0 else (240, 196, 60)) + (255,)
    for x in range(8, 16):
        for y in range(0, 8):
            px[x, y] = ((240, 196, 60) if (x + y) % 3 else (30, 70, 170)) + (255,)
    crown(t, 8, 16, 8, gold=(240, 196, 60), gem=(220, 40, 40))
    face_eyes(t, (255, 230, 120))
    t.save(out / "pharaoh.png")
    # 화염 군주: 숯 같은 뼈 + 불씨
    t = chain(E / "skeleton/wither_skeleton.png", (FIRE, allp))
    cracks(t, (255, 140, 30), 25, 7)
    t.save(out / "flame.png")
    # 심해 수호자: 짙은 바다빛 + 빛나는 점은 그대로
    t = chain(E / "zombie/drowned.png", (ABYSS, not_cyan_glow))
    speckle(t, [(90, 240, 255), (160, 255, 240)], 0.02, 8)
    t.save(out / "abyss.png")
    return 9


if __name__ == "__main__":
    import sys
    print(build(Path(sys.argv[1])))
