"""13차 텍스처: 루비 광석·보석·도구·갑옷 (바닐라 다이아몬드/철 그림 재색칠) + 이벤트 투척병 (직접 그림)."""
import sys
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, recolor, sel_all, sel_cyan, sel_grey

RUBY = ["#2a0010", "#5a0420", "#900a30", "#c8143e", "#ec3a5c", "#ff8aa0", "#ffe2e8"]


def bottle():
    """둥근 유리병 + 보라·금빛 소용돌이 액체 + 반짝이는 별, 코르크 마개"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    glass_edge, glass_hi = (40, 30, 60, 255), (235, 240, 255, 255)
    cx, cy, r = 7.5, 10.0, 5.2
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                # 소용돌이: 각도+거리로 보라 ↔ 분홍 ↔ 금
                import math
                a = math.atan2(y - cy, x - cx) + d * 0.9
                t = (math.sin(a * 2) + 1) / 2
                c1, c2 = (120, 50, 210), (255, 190, 60)
                col = tuple(int(c1[k] + (c2[k] - c1[k]) * t * 0.7) for k in range(3))
                shade = 1.0 - max(0, (x - cx + y - cy)) / 14
                col = tuple(max(0, min(255, int(c * (0.75 + 0.35 * shade)))) for c in col)
                px[x, y] = col + (255,)
            elif d <= r + 1.0:
                px[x, y] = glass_edge
    # 목
    for y in range(2, 5):
        for x in range(6, 10):
            px[x, y] = glass_edge if x in (6, 9) else (170, 160, 210, 255)
    # 코르크
    for x in range(6, 10):
        px[x, 1] = (150, 100, 55, 255)
        px[x, 0] = (110, 70, 35, 255) if x in (6, 9) else (0, 0, 0, 0)
    for x in range(7, 9):
        px[x, 0] = (180, 125, 70, 255)
    # 유리 반사광
    for (x, y) in [(4, 7), (4, 8), (5, 6), (4, 9)]:
        px[x, y] = glass_hi
    # 별
    for (x, y) in [(9, 9), (9, 11), (8, 10), (10, 10), (9, 10)]:
        px[x, y] = (255, 250, 210, 255)
    return im


def build(vanilla: Path):
    T = vanilla / "minecraft/textures"
    if not T.exists():
        T = vanilla / "assets/minecraft/textures"
    item, block, eq = T / "item", T / "block", T / "entity/equipment"
    jobs = [(item / "diamond.png", sel_all, "item/ruby.png"),
            (block / "diamond_ore.png", sel_cyan, "block/ruby_ore.png"),
            (block / "deepslate_diamond_ore.png", sel_cyan, "block/deepslate_ruby_ore.png"),
            (block / "diamond_block.png", sel_all, "block/ruby_block.png"),
            (eq / "humanoid/iron.png", sel_all, "entity/equipment/humanoid/ruby.png"),
            (eq / "humanoid_leggings/iron.png", sel_all, "entity/equipment/humanoid_leggings/ruby.png")]
    for t in ["sword", "pickaxe", "axe", "shovel", "hoe"]:
        jobs.append((item / f"diamond_{t}.png", sel_cyan, f"item/ruby_{t}.png"))
    for a in ["helmet", "chestplate", "leggings", "boots"]:
        jobs.append((item / f"iron_{a}.png", sel_grey, f"item/ruby_{a}.png"))
    for src, sel, dst in jobs:
        out = OUT / dst
        out.parent.mkdir(parents=True, exist_ok=True)
        recolor(src, RUBY, sel).save(out)
    bottle().save(OUT / "item/event_bottle.png")
    return len(jobs) + 1


if __name__ == "__main__":
    print(build(Path(sys.argv[1])))
