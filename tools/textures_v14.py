"""18차 텍스처: 광물 사과 11종 (황금 사과의 금빛 부분만 광물 색으로 재색칠)."""
import colorsys
from pathlib import Path

from textures_v5 import OUT, recolor
from textures_v13 import sparkle

PAL = {
    "copper": ["#3a1a08", "#6e3416", "#a85a2a", "#d27c3e", "#ec9a5a", "#f8c49a", "#fff0e2"],
    "iron": ["#2a2a2e", "#545860", "#7e848c", "#a8aeb6", "#cfd4da", "#e8ecf0", "#ffffff"],
    "redstone": ["#2a0000", "#5a0404", "#8e0a0a", "#c41414", "#ea3a2a", "#ff7a6a", "#ffd0c8"],
    "lapis": ["#060c2e", "#102062", "#1a3496", "#2850c4", "#4a78e4", "#8eb2f8", "#e0ecff"],
    "emerald": ["#022a10", "#065a26", "#0c8a3c", "#17b854", "#41dc78", "#94f4b4", "#e6fff0"],
    "jade": ["#04241a", "#0b4630", "#126344", "#1d8a58", "#36b673", "#86e3ae", "#dbffe9"],
    "mythril": ["#081a2c", "#123758", "#1f5785", "#2f7db3", "#5aaedb", "#9ad8f4", "#e2f8ff"],
    "diamond": ["#06302e", "#0c5c58", "#13908a", "#22c4bc", "#4ee8e0", "#a4fff8", "#f0ffff"],
    "ruby": ["#2a0010", "#5a0420", "#900a30", "#c8143e", "#ec3a5c", "#ff8aa0", "#ffe2e8"],
    "netherite": ["#120e12", "#2a2228", "#3e3238", "#56464c", "#6e5c60", "#94827e", "#c8b8b0"],
    "star_ruby": ["#14000c", "#3e0420", "#7a0c36", "#c0164c", "#f23c6e", "#ff8db2", "#fff0f6"],
}


def sel_gold(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s > 0.2 and 0.0 <= h <= 0.2


def body_only(src: Path, pal):
    """사과 몸통(꼭지 아래)만 재색칠: 꼭지(위 4줄)는 원래 색 유지"""
    from PIL import Image
    orig = Image.open(src).convert("RGBA")
    im = recolor(src, pal, sel_gold)
    px, op = im.load(), orig.load()
    for y in range(0, 4):
        for x in range(im.width):
            px[x, y] = op[x, y]
    return im


def build(van: Path):
    T = van / "minecraft/textures/item"
    if not T.exists():
        T = van / "assets/minecraft/textures/item"
    src = T / "golden_apple.png"
    for name, pal in PAL.items():
        im = body_only(src, pal)
        if name == "star_ruby":
            sparkle(im, 11, 5, True)
            sparkle(im, 5, 11, False)
        im.save(OUT / "item" / f"{name}_apple.png")
    return len(PAL) + build_bows(van)


if __name__ == "__main__":
    import sys
    print(build(Path(sys.argv[1])))


BOW_PAL = {
    "mythril_bow": ["#081a2c", "#123758", "#1f5785", "#2f7db3", "#5aaedb", "#9ad8f4", "#e2f8ff"],
    "ruby_bow": ["#2a0010", "#5a0420", "#900a30", "#c8143e", "#ec3a5c", "#ff8aa0", "#ffe2e8"],
    "star_ruby_bow": ["#14000c", "#3e0420", "#7a0c36", "#c0164c", "#f23c6e", "#ff8db2", "#fff0f6"],
}
ARROW_TIP = {
    "explosive_arrow": ["#3a0a00", "#7a1a04", "#c43a0a", "#ff7a1a", "#ffc04a"],
    "homing_arrow": ["#1a0630", "#3e0e6a", "#6a20b0", "#a050e8", "#e0b0ff"],
    "lightning_arrow": ["#3a3000", "#7a6a00", "#c8b000", "#ffe838", "#fffbc0"],
    "frost_arrow": ["#06243a", "#0e4a74", "#2a86c4", "#7ccaf4", "#e4f8ff"],
}


def sel_wood_bow(rgb):
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s > 0.15 and 0.02 <= h <= 0.16


def sel_arrow_tip(rgb):
    """화살촉(회색 금속)과 깃(흰색)"""
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in rgb[:3]))
    return s < 0.15


def build_bows(van: Path):
    T = van / "assets/minecraft/textures/item"
    if not T.exists():
        T = van / "minecraft/textures/item"
    n = 0
    for name, pal in BOW_PAL.items():
        for suf in ["", "_pulling_0", "_pulling_1", "_pulling_2"]:
            im = recolor(T / f"bow{suf}.png", pal, sel_wood_bow)
            if name == "star_ruby_bow":
                tmp = Path("/tmp/_wp_bow.png")
                im.save(tmp)
                im = recolor(tmp, ["#6b4a0a", "#a8780e", "#d9a22e", "#ffd25a", "#ffeaa0"], sel_arrow_tip)
                sparkle(im, 3, 3, False)
            im.save(OUT / "item" / f"{name}{suf}.png")
            n += 1
    for name, pal in ARROW_TIP.items():
        recolor(T / "arrow.png", pal, sel_arrow_tip).save(OUT / "item" / f"{name}.png")
        n += 1
    return n
