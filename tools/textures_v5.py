"""5차 텍스처: 바닐라 마인크래프트 그림을 재색칠해서 모드 장비·광물을 원작 그림체에 맞춘다.

사용: python3 tools/textures_v5.py <바닐라 assets 폴더>
  (바닐라 원본은 저장소에 넣지 않는다. CI 의 API Probe 워크플로가 ci-probe 브랜치에 뽑아 둔 것을 쓴다.)
원리: 바닐라 스프라이트에서 "재질 부분"(다이아몬드의 청록색, 철의 회색 등)만 골라
밝기 순서를 그대로 유지한 채 새 재질 팔레트로 옮긴다. 손잡이·외곽선·돌 바탕은 그대로 둔다.
"""
import colorsys
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures"

# 어두움 → 밝음
PALETTES = {
    "mythril": ["#081a2c", "#123758", "#1f5785", "#2f7db3", "#5aaedb", "#9ad8f4", "#e2f8ff"],
    "jade": ["#04241a", "#0b4630", "#126344", "#1d8a58", "#36b673", "#86e3ae", "#dbffe9"],
    "bloodsteel": ["#1e050a", "#4a0a14", "#7a1220", "#a81d2c", "#d03a48", "#ec7a82", "#ffc8cc"],
}


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(rgb):
    r, g, b = rgb[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def gradient(pal, t):
    cols = [hexrgb(c) for c in pal]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    a, b = cols[i], cols[i + 1]
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3))


def sel_cyan(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s > 0.18 and 0.38 <= h <= 0.58


def sel_green(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s > 0.18 and 0.22 <= h <= 0.45


def sel_grey(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s < 0.12


def sel_all(rgb):
    return True


def recolor(src: Path, pal, select, lo=None, hi=None):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    picked = [(x, y) for x in range(im.width) for y in range(im.height)
              if px[x, y][3] > 0 and select(px[x, y])]
    if not picked:
        return im
    ls = [lum(px[p]) for p in picked]
    lo = min(ls) if lo is None else lo
    hi = max(ls) if hi is None else hi
    span = max(1.0, hi - lo)
    for p in picked:
        r = gradient(pal, (lum(px[p]) - lo) / span)
        px[p] = r + (px[p][3],)
    return im


def fronts(iron_path: Path):
    """증권 단말기 · ATM 앞면: 바닐라 철 블록 위에 화면·버튼을 그린다"""
    def px(im, x, y, c):
        im.putpixel((x, y), hexrgb(c) + (255,))

    def rect(im, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                px(im, x, y, c)

    # 증권 단말기: 큰 차트 화면 + 아래 키보드
    im = Image.open(iron_path).convert("RGBA")
    rect(im, 1, 1, 14, 10, "#2a2a2e")
    rect(im, 2, 2, 13, 9, "#0b141c")
    for x in range(2, 14):
        px(im, x, 5, "#16242e")
    candles = [(3, 6, 8, "#d83a3a"), (5, 5, 7, "#3a7ad8"), (7, 4, 6, "#d83a3a"), (9, 3, 6, "#d83a3a"), (11, 3, 5, "#3a7ad8")]
    for (x, y0, y1, c) in candles:
        rect(im, x, y0, x, y1, c)
        px(im, x + 1, y0 + 1, c)
    for (x, y) in ((3, 8), (5, 7), (7, 6), (9, 5), (11, 4), (12, 3)):
        px(im, x, y - 1 if y > 3 else y, "#ffd84a")
    rect(im, 2, 12, 13, 13, "#4a4a52")
    for x in range(3, 13, 2):
        px(im, x, 12, "#c8c8d0")
        px(im, x + 1, 13, "#c8c8d0")
    px(im, 13, 10, "#7cfc6a")
    im.save(OUT / "block/stock_terminal_front.png")

    # ATM: 위 화면, 왼쪽 숫자판, 오른쪽 카드 투입구, 아래 지폐 출구
    im = Image.open(iron_path).convert("RGBA")
    rect(im, 1, 1, 14, 7, "#2a2a2e")
    rect(im, 2, 2, 13, 6, "#0f3a1e")
    for (y, w) in ((3, 8), (5, 5)):
        rect(im, 3, y, 3 + w, y, "#7cfc6a")
    rect(im, 2, 9, 7, 13, "#3a3a40")
    for gx in range(3):
        for gy in range(3):
            px(im, 3 + gx * 2, 10 + gy * 1 + (gy > 0) * 0, "#e0e0e6")
    rect(im, 3, 10, 3, 10, "#e0e0e6"); rect(im, 5, 10, 5, 10, "#e0e0e6"); rect(im, 7, 10, 7, 10, "#e0e0e6")
    rect(im, 3, 12, 3, 12, "#e0e0e6"); rect(im, 5, 12, 5, 12, "#e0e0e6"); rect(im, 7, 12, 7, 12, "#d8b030")
    rect(im, 9, 9, 13, 10, "#2a2a2e")
    rect(im, 10, 10, 12, 10, "#050505")
    px(im, 13, 9, "#7cfc6a")
    rect(im, 3, 14, 12, 14, "#050505")
    rect(im, 9, 12, 13, 12, "#d8b030")
    im.save(OUT / "block/atm_front.png")

    # 환전소: 위 초록 간판 + 창구 안 동화·은화·금화 더미 + 화살표
    im = Image.open(iron_path).convert("RGBA")
    rect(im, 1, 1, 14, 4, "#1e5a2e")
    rect(im, 2, 2, 13, 3, "#2f8a46")
    for x in (3, 4, 6, 7, 9, 10, 12):
        px(im, x, 2 + (x % 2), "#e8f8e0")
    rect(im, 1, 6, 14, 13, "#2a2a2e")
    rect(im, 2, 7, 13, 12, "#4a3a2a")
    for (x0, c1, c2, h) in ((3, "#e8955a", "#a0522d", 3), (7, "#f0f0f6", "#9a9aa8", 4), (11, "#fff070", "#c89a10", 5)):
        for k in range(h):
            y = 12 - k
            px(im, x0, y, c2)
            px(im, x0 + 1, y, c1 if k % 2 == 0 else c2)
    px(im, 5, 9, "#ffffff"); px(im, 9, 9, "#ffffff")
    rect(im, 2, 14, 13, 14, "#6a6a72")
    im.save(OUT / "block/exchange_front.png")


def build(vanilla: Path):
    T = vanilla / "minecraft/textures"
    if not T.exists():
        T = vanilla / "assets/minecraft/textures"
    item, block, eq = T / "item", T / "block", T / "entity/equipment"
    jobs = []
    tools = ["sword", "pickaxe", "axe", "shovel", "hoe"]
    armor = ["helmet", "chestplate", "leggings", "boots"]
    for mat in ("mythril", "jade", "bloodsteel"):
        for t in tools:
            if mat == "bloodsteel" and t == "hoe":
                continue
            jobs.append((item / f"diamond_{t}.png", mat, sel_cyan, f"item/{mat}_{t}.png"))
        for a in armor:
            jobs.append((item / f"iron_{a}.png", mat, sel_grey, f"item/{mat}_{a}.png"))
        jobs.append((eq / "humanoid/iron.png", mat, sel_all, f"entity/equipment/humanoid/{mat}.png"))
        jobs.append((eq / "humanoid_leggings/iron.png", mat, sel_all, f"entity/equipment/humanoid_leggings/{mat}.png"))
    jobs += [
        (item / "iron_ingot.png", "mythril", sel_all, "item/mythril_ingot.png"),
        (item / "iron_ingot.png", "bloodsteel", sel_all, "item/bloodsteel_ingot.png"),
        (item / "raw_iron.png", "mythril", sel_all, "item/raw_mythril.png"),
        (item / "emerald.png", "jade", sel_all, "item/jade.png"),
        (block / "diamond_ore.png", "mythril", sel_cyan, "block/mythril_ore.png"),
        (block / "deepslate_diamond_ore.png", "mythril", sel_cyan, "block/deepslate_mythril_ore.png"),
        (block / "emerald_ore.png", "jade", sel_green, "block/jade_ore.png"),
        (block / "deepslate_emerald_ore.png", "jade", sel_green, "block/deepslate_jade_ore.png"),
        (block / "diamond_block.png", "mythril", sel_all, "block/mythril_block.png"),
        (block / "emerald_block.png", "jade", sel_all, "block/jade_block.png"),
    ]
    fronts(block / "iron_block.png")
    for src, mat, sel, dst in jobs:
        im = recolor(src, PALETTES[mat], sel)
        out = OUT / dst
        out.parent.mkdir(parents=True, exist_ok=True)
        im.save(out)
    return len(jobs)


if __name__ == "__main__":
    print(build(Path(sys.argv[1])), "textures")
