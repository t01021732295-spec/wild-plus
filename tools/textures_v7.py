"""8차 텍스처: 새 작물·음식·물고기·드릴·기계·카지노·흡혈 효과 아이콘.
작물과 음식은 기존 모드 그림을 재색칠, 물고기는 바닐라 물고기를 재색칠, 기계 옆면은 바닐라 철 블록 위에 그린다."""
import colorsys
from pathlib import Path

from PIL import Image

from textures_v2 import render, save
from textures_v5 import OUT, gradient, lum

T = OUT  # assets/wildplus/textures


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def remap(im, select, pal):
    """select(h,s,l) 인 픽셀을 밝기 순서대로 pal(어두움→밝음)로 바꾼다"""
    im = im.convert("RGBA").copy()
    px = im.load()
    picked = []
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if select(h, s, l):
                picked.append((x, y))
    if not picked:
        return im
    ls = [lum(px[p]) for p in picked]
    lo, hi = min(ls), max(ls)
    for p in picked:
        c = gradient(pal, (lum(px[p]) - lo) / max(1.0, hi - lo))
        px[p] = c + (px[p][3],)
    return im


def red(h, s, l):
    return s > 0.3 and (h < 0.06 or h > 0.9)


def any_(h, s, l):
    return True


def warm(h, s, l):  # 고기·생선 살색 (빨강~주황)
    return s > 0.2 and (h < 0.12 or h > 0.9)


YELLOW = ["#5a3a00", "#a87010", "#e0b020", "#f8d84a", "#fff2a0"]
LEAFY = ["#1e4a14", "#3a7a22", "#6ab03a", "#a8e070", "#e0ffc0"]
BLUE = ["#101848", "#202e8a", "#3a50c8", "#6a8af0", "#b8c8ff"]


def crops():
    for s in range(4):
        tom = Image.open(T / f"block/tomato_crop_stage{s}.png")
        straw = Image.open(T / f"block/strawberry_crop_stage{s}.png")
        remap(tom, red, YELLOW).save(T / f"block/corn_crop_stage{s}.png")
        remap(tom, red, LEAFY).save(T / f"block/cabbage_crop_stage{s}.png")
        remap(straw, red, BLUE).save(T / f"block/blueberry_crop_stage{s}.png")
    item = T / "item"
    remap(Image.open(item / "tomato_seeds.png"), any_, ["#6a4a10", "#c8a030", "#f8e080"]).save(item / "corn_seeds.png")
    remap(Image.open(item / "tomato_seeds.png"), any_, ["#284a18", "#5a9a30", "#b8e890"]).save(item / "cabbage_seeds.png")
    remap(Image.open(item / "strawberry.png"), red, BLUE).save(item / "blueberry.png")
    remap(Image.open(item / "strawberry_tart.png"), red, BLUE).save(item / "blueberry_muffin.png")
    remap(Image.open(item / "tomato_soup.png"), red, LEAFY).save(item / "salad.png")


CORN = [
    "................",
    "..........gG....",
    ".........gGG....",
    "........kYYgG...",
    ".......kYyYYgG..",
    "......kYyYyYg...",
    ".....kYyYyYYg...",
    "....kYyYyYYg....",
    "...kYyYyYYg.....",
    "...kYYyYYg......",
    "..gkYYYYg.......",
    ".gGgkkkg........",
    "gGg.............",
    "Gg..............",
    "................",
    "................",
]
CORN_PAL = {"k": "#5a3a00", "Y": "#f8d84a", "y": "#d8a820", "g": "#3a7a22", "G": "#6ab03a"}
GRILLED_PAL = {"k": "#3a2000", "Y": "#d8a030", "y": "#8a5a10", "g": "#5a4a1a", "G": "#7a6a2a"}

CABBAGE = [
    "................",
    "................",
    ".....kkkkkk.....",
    "...kkGgGGgGkk...",
    "..kGgGLLLLgGgk..",
    "..kgGLLlLLLGgk..",
    ".kGgLLlLLlLLgGk.",
    ".kgGLlLLLLlLGgk.",
    ".kGgLLLlLLLLgGk.",
    ".kgGLLLLlLLLGgk.",
    "..kgGLLlLLLGgk..",
    "..kGgGLLLLgGgk..",
    "...kkgGGGgGkk...",
    ".....kkkkkk.....",
    "................",
    "................",
]
CABBAGE_PAL = {"k": "#1e4a14", "G": "#3a7a22", "g": "#2c6018", "L": "#a8e070", "l": "#d8f8b0"}

DRILL = [
    "................",
    "................",
    "..............k.",
    ".............kSk",
    "............kSsk",
    "...........kSsk.",
    "..........kSsk..",
    ".........kMMk...",
    "....kkkkkMMMk...",
    "...kRRRRkMMk....",
    "..kRrRRRRkk.....",
    "..kRRrRRk.......",
    "..kkRRRk........",
    "..kBBkk.........",
    ".kBBk...........",
    ".kkk............",
]
DRILL_PAL = {"k": "#141414", "S": "#e8f8ff", "s": "#8ad8f4", "M": "#2f7db3", "R": "#c8a020", "r": "#f8e070", "B": "#3a3a40"}

LIFESTEAL = [
    "..................",
    "........kk........",
    ".......kRRk.......",
    ".......kRRk.......",
    "......kRRRRk......",
    "......kRrRRk......",
    ".....kRrRRRRk.....",
    ".....kRrRRRRk.....",
    "....kRrRRRRRRk....",
    "....kRrRRRRRRk....",
    "....kRRRRRRRRk....",
    "....kRRRRRRRDk....",
    ".....kRRRRRDk.....",
    "......kDDDDk......",
    ".......kkkk.......",
    "..................",
    "..................",
    "..................",
]
LIFESTEAL_PAL = {"k": "#2a0006", "R": "#d01a2a", "r": "#ff8a8a", "D": "#80101a"}


def items():
    save(render(CORN, CORN_PAL), "item/corn.png")
    save(render(CORN, GRILLED_PAL), "item/grilled_corn.png")
    save(render(CABBAGE, CABBAGE_PAL), "item/cabbage.png")
    save(render(DRILL, DRILL_PAL), "item/drill.png")
    (T / "mob_effect").mkdir(parents=True, exist_ok=True)
    render(LIFESTEAL, LIFESTEAL_PAL).save(T / "mob_effect/lifesteal.png")


def fish(vanilla: Path):
    v = vanilla / "minecraft/textures/item"
    if not v.exists():
        v = vanilla / "assets/minecraft/textures/item"
    cod, salmon, trop = Image.open(v / "cod.png"), Image.open(v / "salmon.png"), Image.open(v / "tropical_fish.png")
    ccod, csal = Image.open(v / "cooked_cod.png"), Image.open(v / "cooked_salmon.png")
    item = T / "item"
    remap(cod, any_, ["#1a1610", "#3a3226", "#5e5444", "#8a7e66", "#b8ae94"]).save(item / "catfish.png")
    remap(ccod, any_, ["#2a1606", "#5a3412", "#8a5a26", "#c08a4a", "#e8c08a"]).save(item / "cooked_catfish.png")
    remap(salmon, any_, ["#0a1426", "#1a2e5a", "#3a5a8a", "#8aa8c8", "#e0ecf4"]).save(item / "tuna.png")
    remap(csal, warm, ["#3a0a0a", "#7a1a1a", "#b03a2a", "#e07050", "#ffb0a0"]).save(item / "tuna_steak.png")
    remap(cod, any_, ["#14200e", "#34482a", "#6a8050", "#b0c098", "#eef4e0"]).save(item / "sweetfish.png")
    remap(ccod, any_, ["#2a1a06", "#5a4012", "#90702a", "#c8a050", "#f0d8a0"]).save(item / "grilled_sweetfish.png")
    remap(trop, any_, ["#3a0800", "#a01a00", "#e05a10", "#f8b020", "#fff6a0"]).save(item / "dragon_king_fish.png")


def machines(vanilla: Path):
    b = vanilla / "minecraft/textures/block"
    if not b.exists():
        b = vanilla / "assets/minecraft/textures/block"
    iron = Image.open(b / "iron_block.png").convert("RGBA")

    def rect(im, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                im.putpixel((x, y), hexrgb(c) + (255,))

    # 자동 수확기: 철 블록 + 초록 창 안에 밀 이삭 + 아래 배출구
    im = iron.copy()
    rect(im, 2, 2, 13, 9, "#2a2a2e")
    rect(im, 3, 3, 12, 8, "#1e4a14")
    for x in (4, 7, 10):
        rect(im, x, 4, x, 8, "#6ab03a")
        rect(im, x, 3, x + 1, 5, "#e0b020")
    rect(im, 4, 11, 11, 13, "#3a3a40")
    rect(im, 5, 12, 10, 12, "#111111")
    im.save(T / "block/auto_harvester_side.png")
    # 광석 채굴기: 철 블록 + 곡괭이 + 광석 조각
    im = iron.copy()
    rect(im, 2, 2, 13, 13, "#2a2a2e")
    rect(im, 3, 3, 12, 12, "#5a5a60")
    for (x, y, c) in ((4, 9, "#8ad8f4"), (5, 10, "#8ad8f4"), (9, 4, "#e0b020"), (10, 5, "#e0b020"), (6, 5, "#d83a3a"), (9, 10, "#3fd46a")):
        rect(im, x, y, x, y, c)
    for i in range(6):
        rect(im, 4 + i, 4 + i, 4 + i, 4 + i, "#8a5a2a")
    rect(im, 8, 3, 12, 3, "#c8c8d0")
    rect(im, 12, 3, 12, 7, "#c8c8d0")
    im.save(T / "block/ore_miner_side.png")


FISHER = [
    "dddddddddddddddd",
    "dbbbbbbbbbbbbbbd",
    "dbaaaaaaaaaaaabd",
    "dbaWWWWWWWWWWabd",
    "dbaWwwwwwwrwWabd",
    "dbaWwwwwwwrwWabd",
    "dbaWwwFFwwrwWabd",
    "dbaWwFFFFwrwWabd",
    "dbaWwwFFwwrwWabd",
    "dbaWwwwwwwcwWabd",
    "dbaWWWWWWWWWWabd",
    "dbaaaaaaaaaaaabd",
    "dccccccccccccccd",
    "dbaaaaaaaaaaaabd",
    "dbbbbbbbbbbbbbbd",
    "dddddddddddddddd",
]
FISHER_PAL = {"d": "#3e2c18", "b": "#6b4e2d", "a": "#86653d", "c": "#5c4326", "W": "#2a2a2e", "w": "#2a5a8a",
              "F": "#e08a4a", "r": "#c8c8c8", "c2": "#fff"}

CASINO = [
    "dddddddddddddddd",
    "dGGGGGGGGGGGGGGd",
    "dGgGGGGGGGGGGgGd",
    "dGGGyyyyyyyyGGGd",
    "dGGyGGGGGGGGyGGd",
    "dGGyGRGGGGBGyGGd",
    "dGGyGRRGGBBGyGGd",
    "dGGyGGGGGGGGyGGd",
    "dGGyGGWWWWGGyGGd",
    "dGGyGGWkkWGGyGGd",
    "dGGyGGWWWWGGyGGd",
    "dGGGyyyyyyyyGGGd",
    "dGgGGGGGGGGGGgGd",
    "dGGGGGGGGGGGGGGd",
    "dGGGGGGGGGGGGGGd",
    "dddddddddddddddd",
]
CASINO_PAL = {"d": "#3a2416", "G": "#1e6a36", "g": "#2a8a46", "y": "#e8c840", "R": "#d02a2a", "B": "#2a2a2a",
              "W": "#f4f4f4", "k": "#111111"}


def build(vanilla: Path):
    crops()
    items()
    fish(vanilla)
    machines(vanilla)
    save(render(FISHER, {**FISHER_PAL}), "block/auto_fisher_side.png")
    save(render(CASINO, CASINO_PAL), "block/casino_table_top.png")


if __name__ == "__main__":
    import sys
    build(Path(sys.argv[1]))
    print("ok")
