"""2.12 텍스처: 에메랄드 장비(나무 손잡이) · 로열 에메랄드(금 손잡이 · 금테 · 반짝임) · 생명의 결정 · 로열 에메랄드 사과."""
import os
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, recolor, sel_all, sel_cyan, sel_grey
from textures_v13 import GOLD, sel_wood, sparkle
from textures_v14 import body_only, sel_arrow_tip, sel_wood_bow
from textures_v16 import edge_trim

EMERALD = ["#022a10", "#065a26", "#0c8a3c", "#17b854", "#41dc78", "#94f4b4", "#e6fff0"]
ROYAL = ["#011a0a", "#04441e", "#0a7a34", "#12b04e", "#3ce07a", "#a8ffc8", "#ffffff"]


def _tmp(im, n):
    p = Path(f"/tmp/_wp_em_{n}.png")
    im.save(p)
    return p


def crystal():
    """생명의 결정: 초록 수정 + 가운데 분홍 하트빛"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(1, 15):
        w = 6 - abs(y - 7.5) * 0.75
        for x in range(16):
            if abs(x - 7.5) <= w:
                t = (y - 1) / 14
                g = (int(40 + 120 * (1 - t)), int(200 + 50 * (1 - t)), int(90 + 60 * (1 - t)))
                if x < 7.5:
                    g = tuple(min(255, c + 30) for c in g)
                px[x, y] = g + (255,)
    for x in range(16):
        for y in range(16):
            if px[x, y][3] and (x in (0, 15) or y in (0, 15) or any(
                    not (0 <= x + dx < 16 and 0 <= y + dy < 16) or px[x + dx, y + dy][3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))):
                px[x, y] = (10, 70, 30, 255)
    for (x, y) in [(6, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (7, 7), (8, 7)]:
        px[x, y] = (255, 120, 170, 255)
    px[7, 7] = (255, 220, 235, 255)
    sparkle(im, 5, 3, False)
    return im


def build(van: Path):
    T = van / "minecraft/textures"
    item = T / "item"
    out = OUT / "item"
    v2 = Path(os.environ.get("WILDPLUS_VANILLA2", "")) / "minecraft/textures/entity/equipment"
    v3 = Path(os.environ.get("WILDPLUS_VANILLA3", "")) / "minecraft/textures/item"
    n = 0
    for t in ["sword", "pickaxe", "axe", "shovel", "hoe"]:
        recolor(item / f"diamond_{t}.png", EMERALD, sel_cyan).save(out / f"emerald_{t}.png")
        im = recolor(item / f"diamond_{t}.png", ROYAL, sel_cyan)
        im = recolor(_tmp(im, "r"), GOLD, sel_wood)
        sparkle(im, {"sword": 11, "pickaxe": 3, "axe": 5, "shovel": 12, "hoe": 4}[t], 2, True)
        im.save(out / f"royal_emerald_{t}.png")
        n += 2
    for a in ["helmet", "chestplate", "leggings", "boots"]:
        recolor(item / f"iron_{a}.png", EMERALD, sel_grey).save(out / f"emerald_{a}.png")
        im = recolor(item / f"iron_{a}.png", ROYAL, sel_grey)
        edge_trim(im, (255, 214, 90), (176, 128, 30))
        sparkle(im, {"helmet": 4, "chestplate": 5, "leggings": 5, "boots": 4}[a], {"helmet": 5, "chestplate": 5, "leggings": 4, "boots": 9}[a], False)
        im.save(out / f"royal_emerald_{a}.png")
        n += 2
    eq = OUT / "entity/equipment"
    for layer in ["humanoid", "humanoid_leggings"]:
        src = v2 / layer / "iron.png"
        if not src.exists():
            src = T / "entity/equipment" / layer / "iron.png"
        recolor(src, EMERALD, sel_all).save(eq / layer / "emerald.png")
        im = recolor(v2 / layer / "netherite.png", ROYAL, sel_all)
        px = im.load()
        import random
        rnd = random.Random(11)
        for _ in range(int(im.width * im.height * 0.008)):
            x, y = rnd.randrange(im.width), rnd.randrange(im.height)
            if px[x, y][3] > 0:
                px[x, y] = (255, 220, 100, 255) if rnd.random() < 0.6 else (255, 255, 255, 255)
        im.save(eq / layer / "royal_emerald.png")
        n += 2
    for suf in ["", "_pulling_0", "_pulling_1", "_pulling_2"]:
        recolor(v3 / f"bow{suf}.png", EMERALD, sel_wood_bow).save(out / f"emerald_bow{suf}.png")
        im = recolor(v3 / f"bow{suf}.png", ROYAL, sel_wood_bow)
        im = recolor(_tmp(im, "b"), GOLD[1:6], sel_arrow_tip)
        sparkle(im, 3, 3, False)
        im.save(out / f"royal_emerald_bow{suf}.png")
        n += 2
    ap = body_only(v3 / "golden_apple.png", ROYAL)
    sparkle(ap, 11, 5, True)
    ap.save(out / "royal_emerald_apple.png")
    crystal().save(out / "life_crystal.png")
    return n + 2


if __name__ == "__main__":
    import sys
    print(build(Path(sys.argv[1])))
