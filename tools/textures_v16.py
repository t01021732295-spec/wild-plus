"""2.9 텍스처: 사파이어(에메랄드 모양 보석 · 은빛 손잡이) · 스카이 사파이어(백은 테 + 눈꽃 반짝임) · 용의 깃털."""
import colorsys
import math
import os
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, recolor, sel_all, sel_cyan, sel_green, sel_grey
from textures_v13 import sel_wood, star_layer
from textures_v14 import sel_arrow_tip, sel_wood_bow

SAPPHIRE = ["#04082a", "#0a1460", "#1430a8", "#2452e0", "#4c82ff", "#a0c4ff", "#eef4ff"]
SKY = ["#020a24", "#06205a", "#0c3c9c", "#1a66e0", "#4aa6ff", "#b4e2ff", "#ffffff"]
SILVER = ["#14182a", "#22283e", "#343c58", "#4a5474", "#64708e", "#8a96b0"]
WHITE_SILVER = ["#5a6a80", "#8a9ab0", "#c0ccdc", "#e6eef8", "#ffffff"]


def _tmp(im, name):
    p = Path(f"/tmp/_wp_{name}.png")
    im.save(p)
    return p


def snowflake(im, cx, cy):
    """작은 눈꽃 반짝임: 가운데 흰색, 대각 팔은 하늘색"""
    px = im.load()
    w, h = im.size

    def put(x, y, c):
        if 0 <= x < w and 0 <= y < h:
            px[x, y] = c + (255,)

    put(cx, cy, (255, 255, 255))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(cx + dx, cy + dy, (200, 236, 255))
    for dx, dy in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
        put(cx + dx * 2, cy + dy * 2, (150, 210, 255))


def edge_trim(im, top, bottom):
    px = im.load()
    edge = []
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < im.width and 0 <= ny < im.height) or px[nx, ny][3] == 0:
                    edge.append((x, y))
                    break
    for (x, y) in edge:
        px[x, y] = (bottom if y > im.height // 2 else top) + (255,)


def plume():
    """용의 깃털: 보라→자홍 깃, 하늘빛 끝, 흰 깃대"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for t in range(15):
        x, y = 2 + t * 0.8, 14 - t * 0.9
        cx, cy = int(round(x)), int(round(y))
        width = max(0.0, math.sin(t / 14 * math.pi)) * 3.4 + 0.6
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                # 깃대에 수직 방향으로 퍼짐
                d = abs(dx * 0.75 + dy * 0.66)
                along = abs(dx * 0.66 - dy * 0.75)
                if along > 0.6 or d > width:
                    continue
                X, Y = cx + dx, cy + dy
                if not (0 <= X < 16 and 0 <= Y < 16):
                    continue
                k = t / 14
                if k > 0.8:
                    col = (130, 220, 255)
                else:
                    a, b = (90, 20, 150), (230, 60, 200)
                    col = tuple(int(a[i] + (b[i] - a[i]) * (k / 0.8)) for i in range(3))
                shade = 0.75 + 0.25 * (1 - d / max(width, 0.1))
                px[X, Y] = tuple(min(255, int(c * shade)) for c in col) + (255,)
    for t in range(16):
        x, y = int(round(1.5 + t * 0.8)), int(round(14.5 - t * 0.9))
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = (250, 240, 255, 255) if t < 12 else (200, 240, 255, 255)
    for (x, y) in [(8, 4), (11, 6)]:
        px[x, y] = (255, 255, 255, 255)
    return im


def build(van: Path):
    T = van / "minecraft/textures"
    if not T.exists():
        T = van / "assets/minecraft/textures"
    item, block = T / "item", T / "block"
    v2 = Path(os.environ.get("WILDPLUS_VANILLA2", "")) / "minecraft/textures/entity/equipment"
    out_item = OUT / "item"
    n = 0
    # 보석 · 광석 · 블록 (에메랄드 모양을 파랗게)
    recolor(item / "emerald.png", SAPPHIRE, sel_all).save(out_item / "sapphire.png")
    recolor(block / "emerald_ore.png", SAPPHIRE, sel_green).save(OUT / "block/sapphire_ore.png")
    recolor(block / "deepslate_emerald_ore.png", SAPPHIRE, sel_green).save(OUT / "block/deepslate_sapphire_ore.png")
    recolor(block / "emerald_block.png", SAPPHIRE, sel_all).save(OUT / "block/sapphire_block.png")
    n += 4
    # 사파이어 도구: 다이아 도구 + 은 손잡이 / 갑옷: 철 갑옷을 파랗게
    for t in ["sword", "pickaxe", "axe", "shovel", "hoe"]:
        im = recolor(item / f"diamond_{t}.png", SAPPHIRE, sel_cyan)
        im = recolor(_tmp(im, "sa"), SILVER, sel_wood)
        im.save(out_item / f"sapphire_{t}.png")
        # 스카이: 더 밝은 하늘빛 + 백은 손잡이 + 눈꽃
        sk = recolor(item / f"diamond_{t}.png", SKY, sel_cyan)
        sk = recolor(_tmp(sk, "sk"), WHITE_SILVER, sel_wood)
        snowflake(sk, {"sword": 11, "pickaxe": 4, "axe": 5, "shovel": 12, "hoe": 4}[t], {"sword": 3, "pickaxe": 3, "axe": 3, "shovel": 3, "hoe": 3}[t])
        sk.save(out_item / f"sky_sapphire_{t}.png")
        n += 2
    for a in ["helmet", "chestplate", "leggings", "boots"]:
        recolor(item / f"iron_{a}.png", SAPPHIRE, sel_grey).save(out_item / f"sapphire_{a}.png")
        sk = recolor(item / f"iron_{a}.png", SKY, sel_grey)
        edge_trim(sk, (236, 244, 255), (170, 186, 206))
        snowflake(sk, {"helmet": 4, "chestplate": 5, "leggings": 5, "boots": 4}[a], {"helmet": 5, "chestplate": 5, "leggings": 4, "boots": 9}[a])
        sk.save(out_item / f"sky_sapphire_{a}.png")
        n += 2
    # 몸에 입은 그림
    eq = OUT / "entity/equipment"
    for layer in ["humanoid", "humanoid_leggings"]:
        (eq / layer).mkdir(parents=True, exist_ok=True)
        src = v2 / layer / "iron.png"
        if not src.exists():
            src = T / "entity/equipment" / layer / "iron.png"
        recolor(src, SAPPHIRE, sel_all).save(eq / layer / "sapphire.png")
        src2 = v2 / layer / "netherite.png"
        if not src2.exists():
            src2 = src
        im = recolor(src2, SKY, sel_all)
        px = im.load()
        import random
        rnd = random.Random(9 if layer == "humanoid" else 10)
        for _ in range(int(im.width * im.height * 0.006)):
            x, y = rnd.randrange(im.width), rnd.randrange(im.height)
            if px[x, y][3] > 0:
                px[x, y] = (255, 255, 255, 255) if rnd.random() < 0.5 else (170, 220, 255, 255)
        im.save(eq / layer / "sky_sapphire.png")
        n += 2
    # 활
    vb = Path(os.environ.get("WILDPLUS_VANILLA3", "")) / "minecraft/textures/item"
    for suf in ["", "_pulling_0", "_pulling_1", "_pulling_2"]:
        recolor(vb / f"bow{suf}.png", SAPPHIRE, sel_wood_bow).save(out_item / f"sapphire_bow{suf}.png")
        im = recolor(vb / f"bow{suf}.png", SKY, sel_wood_bow)
        im = recolor(_tmp(im, "sb"), WHITE_SILVER, sel_arrow_tip)
        snowflake(im, 3, 3)
        im.save(out_item / f"sky_sapphire_bow{suf}.png")
        n += 2
    plume().save(out_item / "dragon_plume.png")
    return n + 1


if __name__ == "__main__":
    import sys
    print(build(Path(sys.argv[1])))
