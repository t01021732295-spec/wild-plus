"""2차 업데이트 텍스처: 광석·도구·낚시·농사.

gen_textures.py 의 main() 이 마지막에 build() 를 호출한다.
템플릿 + 팔레트 교체 방식: 같은 모양을 색만 바꿔 여러 아이템에 쓴다.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures"


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def render(rows, pal):
    h, w = len(rows), len(rows[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == w, (row, len(row))
        for x, ch in enumerate(row):
            if ch != "." and pal.get(ch):
                im.putpixel((x, y), hexc(pal[ch]))
    return im


def save(im, path):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)


# ───────────────────── 도구 모양 (K 외곽, R 어두움, r 중간, p 밝음, w 하이라이트, B/L 손잡이) ─────────────────────
SWORD = [
"................", "............KKK.", "...........KwpK.", "..........KwpRK.",
".........KwpRK..", "........KwpRK...", ".......KwpRK....", "..KK..KwpRK.....",
"..KpKKwpRK......", "...KpKpRK.......", "....KbRK........", "...KbBKpK.......",
"..KbBK.KpK......", ".KLBK...KK......", ".KKK............", "................"]
PICKAXE = [
"................", "....KKKKK.......", "...KpwwppKK.....", "....KKKrrpRK....",
".......KKBrRK...", "........KBKRK...", ".......KBK.KRK..", "......KBK..KpK..",
".....KBK...KRK..", "....KBK.....K...", "...KBK..........", "..KBK...........",
".KLK............", ".KK.............", "................", "................"]
AXE = [
"................", "......KKK.......", ".....KpwpKK.....", "....KpwrrpK.....",
"....KprrRKBK....", "....KrrRKBLK....", ".....KRKBKRK....", "......KBK.K.....",
".....KBK........", "....KBK.........", "...KBK..........", "..KBK...........",
".KLK............", ".KK.............", "................", "................"]
SHOVEL = [
"................", "..........KKK...", ".........KwppK..", "........KwprpK..",
"........KprrRK..", ".........KRRK...", "........KBKK....", ".......KBK......",
"......KBK.......", ".....KBK........", "....KBK.........", "...KBK..........",
"..KLK...........", "..KK............", "................", "................"]
HOE = [
"................", ".....KKKKKK.....", "....KwwppprK....", ".....KKKKBRK....",
"........KBKK....", ".......KBK......", "......KBK.......", ".....KBK........",
"....KBK.........", "...KBK..........", "..KBK...........", ".KLK............",
".KK.............", "................", "................", "................"]
INGOT = [
"................", "................", "................", "................",
"......KKKKKKK...", ".....KwpppppRK..", "....KwprrrrrRK..", "...KwprrrrrRRK..",
"..KpppppppRRK...", "..KrrrrrrrRK....", "..KRRRRRRRK.....", "...KKKKKKK......",
"................", "................", "................", "................"]
RAW = [
"................", "................", "................", ".......KKK......",
".....KKpwpKK....", "....KpwrrrpKK...", "...KprrRrrrrpK..", "...KrrRRrrprrK..",
"..KprrrrrRrrRK..", "..KrrpRrrrrRK...", "...KRrrrrRRRK...", "....KKRRRRKK....",
"......KKKK......", "................", "................", "................"]
GEM = [
"................", "................", "................", ".....KKKKKK.....",
"....KwwppprK....", "...KwppprrrRK...", "..KwpppprrrrRK..", "..KppppprrrRRK..",
"...KpprrrrRRK...", "....KprrrRRK....", ".....KrrRRK.....", "......KRRK......",
".......KK.......", "................", "................", "................"]

TOOL_STICK = {"b": "#3d2510", "B": "#6b4423", "L": "#8f6235"}
MYTHRIL = {"K": "#10283c", "R": "#2c6a95", "r": "#4fa6d4", "p": "#93dbf7", "w": "#e6fbff", **TOOL_STICK}
JADE = {"K": "#08301d", "R": "#17714a", "r": "#2fb06e", "p": "#7fe3ad", "w": "#d8ffe9", **TOOL_STICK}


# ───────────────────── 물고기 (o 외곽, b 몸통, c 배, s 지느러미/무늬, W 눈흰자, k 눈동자) ─────────────────────
FISH = [
"................", "................", "................", "........ooo.....",
".......ossso....", ".oo..oobbbbboo..", "osbo.obbbbbbbbo.", "osssobbbbbbbWko.",
"osbbbbbbbbbbbbbo", "osssbccccccccbo.", "osboocccccccoo..", ".oo..oossooo....",
".......oo.......", "................", "................", "................"]
EEL = [
"................", "................", "................", "................",
"................", "..........oooo..", ".........obbWko.", "...ooo..obbbbbo.",
"..obbbo.obcccoo.", ".obcccbobbcco...", "obco.obbbcco....", "oco...occco.....",
".o.....ooo......", "................", "................", "................"]
ANGLER = [
"....oo..........", "...oSSo.........", "....oo.o........", ".......o........",
"......oooooo....", "....oobbbbbboo..", "...obbbbbbbbbbo.", "..obbWkbbbbbbsso",
".obbbbbbbbbbbsso", ".oWbWbWbbbbbbsso", ".occcccccccbbo..", "..occcccccco....",
"...oooooooo.....", "................", "................", "................"]

FISH_PALETTES = {
    "rainbow_trout": {"o": "#2a3b2a", "b": "#7f9a5a", "c": "#f2c6c6", "s": "#d9536b", "W": "#ffffff", "k": "#111111"},
    "golden_carp": {"o": "#5a3a00", "b": "#f0b81e", "c": "#ffe9a0", "s": "#ff8c1a", "W": "#ffffff", "k": "#111111"},
    "moonlight_fish": {"o": "#14183d", "b": "#5a6ad6", "c": "#d6e4ff", "s": "#a5f0ff", "W": "#ffffff", "k": "#111111"},
    "ice_smelt": {"o": "#2b4a5a", "b": "#bfe6f2", "c": "#ffffff", "s": "#7fbfdd", "W": "#ffffff", "k": "#111111"},
    "cooked_rainbow_trout": {"o": "#3a2210", "b": "#a8682e", "c": "#e2b07a", "s": "#7a3e18", "W": "#e2b07a", "k": "#3a2210"},
}
EEL_PALETTES = {
    "storm_eel": {"o": "#1a1f2b", "b": "#3f4f6e", "c": "#f2e55a", "W": "#ffffff", "k": "#111111"},
    "grilled_eel": {"o": "#2e1a0c", "b": "#8a4a1e", "c": "#d9963c", "W": "#d9963c", "k": "#2e1a0c"},
}
ANGLER_PAL = {"o": "#120d14", "b": "#4a3a52", "c": "#7a6a6a", "s": "#2b2133", "S": "#fff27a", "W": "#f2f2f2", "k": "#000000"}


# ───────────────────── 기타 아이템 ─────────────────────
FISHING_ROD = [
"................", "...........KK...", "..........KBK...", ".........KBKs...",
"........KBK.s...", ".......KBK..s...", "......KBK...s...", ".....KBK....s...",
"....KJK.....s...", "...KJK......s...", "..KLK.......s...", ".KLK........s...",
".KK.........sgg.", "............g...", "............gg..", "................"]
FISHING_ROD_CAST = [
"................", "...........KK...", "..........KBK...", ".........KBKs...",
"........KBK.....", ".......KBK......", "......KBK.......", ".....KBK........",
"....KJK.........", "...KJK..........", "..KLK...........", ".KLK............",
".KK.............", "................", "................", "................"]
ROD_PAL = {"K": "#14301f", "B": "#5a3a1e", "J": "#2fb06e", "L": "#7fe3ad", "s": "#e8e8e8", "g": "#9a9a9a"}

SUNKEN_CHEST = [
"................", "................", "..kkkkkkkkkkkk..", ".kGnnnnnnnnnnGk.",
".knNNNNGNNNNNnk.", ".knNNNNNNNGNNnk.", ".kkkkkkYYkkkkkk.", ".knNNNNYyNNNNnk.",
".knNNGNNNNNNNnk.", ".knNNNNNNNGNNnk.", ".knNNNNNNNNNNnk.", ".kGnnnnnnnnnnGk.",
"..kkkkkkkkkkkk..", "................", "................", "................"]
CHEST_PAL = {"k": "#1f140a", "n": "#4a3018", "N": "#7a5530", "G": "#3f7a4a", "Y": "#e0b53a", "y": "#8a6a1e"}

TOMATO = [
"................", "................", "........e.......", ".......eEe......",
".....eEEFEEe....", "....cCeCCeCc....", "...cChCCCCCCc...", "..cChhCCCCCCCc..",
"..cChCCCCCCCCc..", "..cCCCCCCCCCCc..", "..cCCCCCCCCCcc..", "...cCCCCCCCcc...",
"....ccCCCCcc....", "......cccc......", "................", "................"]
STRAWBERRY = [
"................", "................", ".......e........", ".....eEFEe......",
"....eEEeEEe.....", "....cCCCCCCc....", "...cCyCCCyCCc...", "...cChCCCCCyc...",
"...cCCCyCCCCc...", "....cCCCCyCc....", "....cCyCCCCc....", ".....cCCCyc.....",
"......cCCc......", ".......cc.......", "................", "................"]
FRUIT_PAL = {"e": "#1f4d16", "E": "#3e8a2a", "F": "#6cc14a", "c": "#7a0e0e", "C": "#e0362c", "h": "#ff9a8a", "y": "#ffe27a"}

BOWL = [
"................", "................", "................", "................",
"................", "..nnnnnnnnnnnn..", ".nsSSStSSaSSSsn.", ".nsSaSSSStSSSsn.", ".nNsSSSSSSSSsNn.",
"..nNNNNNNNNNNn..", "..nNNNNNNNNNNn..", "...nNNNNNNNNn...", "....nnnnnnnn....",
"................", "................", "................"]
BOWL_BASE = {"n": "#4a2c14", "N": "#7a4c26"}
SOUP_PALETTES = {
    "tomato_soup": {**BOWL_BASE, "s": "#9a1a12", "S": "#d93a26", "t": "#ff7a5a", "a": "#3e8a2a"},
    "fish_stew": {**BOWL_BASE, "s": "#a8361c", "S": "#e2632f", "t": "#f4efe0", "a": "#c41e1e"},
    "sashimi_platter": {**BOWL_BASE, "s": "#e8e8e8", "S": "#ff9a7a", "t": "#ffd0c0", "a": "#3e8a2a"},
}

PIZZA = [
"................", "................", ".uuuuuuuuuuuuu..", ".uUUUUUUUUUUUu..",
"..uvCvvCvvvCu...", "..uvvvCvvvvvu...", "...uvvzvvCvu....", "...uvCvvvvvu....",
"....uvvvCvu.....", "....uvzvvvu.....", ".....uvCvu......", ".....uvvvu......",
"......uvu.......", "......uu........", "................", "................"]
PIZZA_PAL = {"u": "#7a4a1e", "U": "#c98a3e", "v": "#f2d27a", "C": "#d93a26", "z": "#5a2a1a"}

TART = [
"................", "................", "................", "................",
"................", "......cCh.......", "...cCh..cCh.....", "..wwwwwwwwwwww..",
".uwwwwwwwwwwwwu.", ".uUUUUUUUUUUUUu.", ".uUuUuUuUuUuUUu.", "..uuuuuuuuuuuu..",
"................", "................", "................", "................"]
TART_PAL = {"u": "#7a4a1e", "U": "#d9a05a", "w": "#fff4e6", "c": "#7a0e0e", "C": "#e0362c", "h": "#ff9a8a"}

FERTILIZER = [
"................", "................", ".......kk.......", "......kSSk......",
".....kSSSSk.....", "....kNNNNNNk....", "...kNnNNgNNNk...", "...kNNNgNNnNk...",
"..kNNgNNNNNNNk..", "..kNNNNnNNgNNk..", "..kNnNNNgNNNNk..", "..kNNNNNNNNnNk..",
"...kNNNNNNNNk...", "....kkkkkkkk....", "................", "................"]
FERT_PAL = {"k": "#2a1a0a", "N": "#8a6a3e", "n": "#5a3e1e", "g": "#4caa3a", "S": "#c8b07a"}

SEEDS = [
"................", "................", "................", "................",
"................", ".......jt.......", "....jt..tj......", "......jt..jt....",
"...tj..jt.......", ".....tj..tj.....", "....jt.jt...t...", "......tj..jt....",
"........jt......", "................", "................", "................"]
TOMATO_SEEDS_PAL = {"j": "#a8945a", "t": "#e8d8a0"}


# ───────────────────── 블록 ─────────────────────
def noise_base(seed, colors, streak=False):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        row_shift = rnd.randint(0, 2) if streak else 0
        for x in range(16):
            v = rnd.random()
            if streak and (y % 4 == row_shift):
                v *= 0.6
            idx = 0 if v < 0.35 else 1 if v < 0.75 else 2
            im.putpixel((x, y), hexc(colors[idx]))
    return im


STONE = ["#6f6f6f", "#7f7f7f", "#919191"]
DEEPSLATE = ["#3a3a40", "#4a4a50", "#5a5a60"]

ORE_SPOTS = [  # (x, y) 중심 좌표 - 광맥 모양
    (3, 3), (4, 3), (3, 4), (11, 2), (12, 2), (12, 3), (7, 7), (8, 7), (7, 8), (8, 8),
    (2, 11), (3, 11), (3, 12), (12, 11), (13, 12), (12, 12), (9, 13),
]


def ore(seed, base, light, mid, dark):
    im = noise_base(seed, base, streak=base is DEEPSLATE)
    for i, (x, y) in enumerate(ORE_SPOTS):
        im.putpixel((x, y), hexc(mid))
    for i, (x, y) in enumerate(ORE_SPOTS):
        if i % 3 == 0:
            im.putpixel((x, y), hexc(light))
        if (x + 1, y + 1) not in ORE_SPOTS and x + 1 < 16 and y + 1 < 16 and i % 2 == 0:
            im.putpixel((x + 1, y + 1), hexc(dark))
    return im


def storage(light, mid, dark, edge):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = mid
            if x in (0, 15) or y in (0, 15):
                c = edge
            elif x == 1 or y == 1:
                c = light
            elif x == 14 or y == 14:
                c = dark
            elif (x + y) % 6 == 0:
                c = light
            im.putpixel((x, y), hexc(c))
    return im


TOMATO_CROP = [
[ "................", "................", "................", "................",
  "................", "................", "................", "................",
  "................", "................", "...F........F...", "..FEF......FEF..",
  "...E...F....E...", "...e..FEF...e...", "...e...e....e...", "...e...e....e..."],
[ "................", "................", "................", "................",
  "......u.....u...", "......u.....u...", "....F.uF...Fu...", "...FEFu...FEFu..",
  "....EFu.F..EFuF.", "....e.uFEF.e.uE.", "...FeFu.e.FeFu..", "...EeEu.e.EeEu..",
  "....e.u.e..e.u..", "....e.u.e..e.u..", "....e.u.e..e.u..", "....e.u.e..e.u.."],
[ "................", "......u.....u...", "....F.uF...Fu...", "...FEFu..F.FEFu.",
  "..FEFEu.FEFFEFu.", "...EeEuF.E.EeEu.", "..FeyFuEF.FeFuF.", "...EeEuEe..EeEuE",
  "..FeFEu.eFFeFyu.", "...EeEu.eE.EeEu.", "..yeFEuF.e.FeFu.", "...EeEuE.e.EeEu.",
  "....e.u..e..e.u.", "....e.u..e..e.u.", "....e.u..e..e.u.", "....e.u..e..e.u."],
[ "................", "......u.....u...", "....F.uF...Fu...", "...FEFu..F.FEFu.",
  "..FECCu.FEFFEFu.", "...ChCuF.E.ECCu.", "..FCCFuEF.FCChu.", "...EeEuEe..ECCuE",
  "..FeFEu.eFFeFyu.", "..CCEEu.eE.EeEu.", "..ChCEuF.e.FCCu.", "..CCEEuE.e.EChu.",
  "....e.u..e..ECCu", "....e.u..e..e.u.", "....e.u..e..e.u.", "....e.u..e..e.u."],
]
TOMATO_CROP_PAL = {"e": "#1f4d16", "E": "#3e8a2a", "F": "#6cc14a", "u": "#8f6235", "y": "#f2e05a",
                   "C": "#e0362c", "h": "#ff9a8a"}

STRAWBERRY_CROP = [
[ "................", "................", "................", "................",
  "................", "................", "................", "................",
  "................", "................", "................", "................",
  "..F.....F...F...", ".FEF...FEF.FEF..", "..E..F..E...E.F.", "..e.FEF.e...e.E."],
[ "................", "................", "................", "................",
  "................", "................", "................", "................",
  "................", "................", "..F...F.....F...", ".FEF.FEF...FEF..",
  "FEFEFEFEF.FEFEF.", ".EeE.EeE.F.EeEF.", "..e...e.FEF.e.E.", "..e...e..e..e.e."],
[ "................", "................", "................", "................",
  "................", "................", "................", "................",
  "................", "..W...F...W.....", ".WyW.FEF.WyW.F..", "..WF.FEFF.WF.FEF",
  "FEFEFEFEFEFEFEFE", ".EeE.EeE.E.EeEE.", "..e...e.FEF.e.E.", "..e...e..e..e.e."],
[ "................", "................", "................", "................",
  "................", "................", "................", "................",
  "................", "......F.........", ".F...FEF...F..F.", "FEF.FEFFF.FEFFEF",
  "FEFEFCCEFEFECCFE", ".CCE.ChE.ECCEChE", ".ChC.CCe.FChe.CC", "..C...e..e.Ce.e."],
]
STRAWBERRY_CROP_PAL = {"e": "#1f4d16", "E": "#3e8a2a", "F": "#6cc14a", "W": "#ffffff", "y": "#f2e05a",
                       "C": "#e0362c", "h": "#ff9a8a"}


def sprinkler_textures():
    side = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    top = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    metal = ["#4f5a63", "#7d8a94", "#b3c0c8"]
    for y in range(16):
        for x in range(16):
            c = metal[1]
            if x in (0, 15) or y in (0, 15):
                c = metal[0]
            elif (x + y) % 5 == 0:
                c = metal[2]
            side.putpixel((x, y), hexc(c))
            top.putpixel((x, y), hexc(c))
    # 노즐 구멍과 비취 장식
    for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11), (7, 7), (8, 8), (7, 8), (8, 7)]:
        top.putpixel((x, y), hexc("#1d2a33"))
    for x in range(5, 11):
        side.putpixel((x, 7), hexc("#2fb06e"))
        side.putpixel((x, 8), hexc("#17714a"))
    return side, top


def build():
    # 광석·재료
    save(ore(11, STONE, "#e6fbff", "#4fa6d4", "#2c6a95"), "block/mythril_ore.png")
    save(ore(12, DEEPSLATE, "#e6fbff", "#4fa6d4", "#2c6a95"), "block/deepslate_mythril_ore.png")
    save(ore(13, STONE, "#d8ffe9", "#2fb06e", "#17714a"), "block/jade_ore.png")
    save(ore(14, DEEPSLATE, "#d8ffe9", "#2fb06e", "#17714a"), "block/deepslate_jade_ore.png")
    save(storage("#c8f0ff", "#5fb4d9", "#2c6a95", "#10283c"), "block/mythril_block.png")
    save(storage("#b8f5d2", "#3fbf7a", "#17714a", "#08301d"), "block/jade_block.png")
    save(render(RAW, MYTHRIL), "item/raw_mythril.png")
    save(render(INGOT, MYTHRIL), "item/mythril_ingot.png")
    save(render(GEM, JADE), "item/jade.png")
    for name, shape in [("sword", SWORD), ("pickaxe", PICKAXE), ("axe", AXE), ("shovel", SHOVEL), ("hoe", HOE)]:
        save(render(shape, MYTHRIL), f"item/mythril_{name}.png")

    # 낚시
    save(render(FISHING_ROD, ROD_PAL), "item/jade_fishing_rod.png")
    save(render(FISHING_ROD_CAST, ROD_PAL), "item/jade_fishing_rod_cast.png")
    for name, pal in FISH_PALETTES.items():
        save(render(FISH, pal), f"item/{name}.png")
    for name, pal in EEL_PALETTES.items():
        save(render(EEL, pal), f"item/{name}.png")
    save(render(ANGLER, ANGLER_PAL), "item/anglerfish.png")
    save(render(SUNKEN_CHEST, CHEST_PAL), "item/sunken_chest.png")

    # 농사·요리
    save(render(TOMATO, FRUIT_PAL), "item/tomato.png")
    save(render(STRAWBERRY, FRUIT_PAL), "item/strawberry.png")
    save(render(SEEDS, TOMATO_SEEDS_PAL), "item/tomato_seeds.png")
    for name, pal in SOUP_PALETTES.items():
        save(render(BOWL, pal), f"item/{name}.png")
    save(render(PIZZA, PIZZA_PAL), "item/pizza.png")
    save(render(TART, TART_PAL), "item/strawberry_tart.png")
    save(render(FERTILIZER, FERT_PAL), "item/fertilizer.png")
    for i, rows in enumerate(TOMATO_CROP):
        save(render(rows, TOMATO_CROP_PAL), f"block/tomato_crop_stage{i}.png")
    for i, rows in enumerate(STRAWBERRY_CROP):
        save(render(rows, STRAWBERRY_CROP_PAL), f"block/strawberry_crop_stage{i}.png")
    side, top = sprinkler_textures()
    save(side, "block/sprinkler_side.png")
    save(top, "block/sprinkler_top.png")


if __name__ == "__main__":
    build()
    print("ok")
