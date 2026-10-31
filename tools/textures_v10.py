"""11차 텍스처: 새 효과 아이콘 7종 (18×18)"""
from PIL import Image, ImageDraw

from textures_v5 import OUT


def icon(draw_fn):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_fn(d)
    return im


def outline(im, color=(20, 20, 24, 255)):
    """윤곽선 1픽셀"""
    px = im.load()
    out = im.copy()
    po = out.load()
    for x in range(18):
        for y in range(18):
            if px[x, y][3] == 0 and any(0 <= x + dx < 18 and 0 <= y + dy < 18 and px[x + dx, y + dy][3] > 0
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                po[x, y] = color
    return out


def build():
    icons = {
        # 곡괭이
        "miners_focus": lambda d: (d.line([(4, 13), (12, 5)], fill=(139, 90, 43), width=2),
                                   d.arc([3, 2, 15, 12], 200, 340, fill=(242, 193, 78), width=2)),
        # 방패
        "ironskin": lambda d: (d.polygon([(4, 3), (14, 3), (14, 9), (9, 15), (4, 9)], fill=(170, 176, 186)),
                               d.polygon([(6, 5), (12, 5), (12, 9), (9, 12), (6, 9)], fill=(210, 215, 222)),
                               d.line([(9, 4), (9, 13)], fill=(120, 126, 136))),
        # 금화
        "greed": lambda d: (d.ellipse([3, 3, 14, 14], fill=(201, 150, 40)), d.ellipse([4, 4, 13, 13], fill=(242, 193, 78)),
                            d.line([(7, 6), (8, 11), (9, 8), (10, 11), (11, 6)], fill=(140, 96, 16))),
        # 핏방울
        "bleeding": lambda d: (d.polygon([(9, 2), (13, 9), (5, 9)], fill=(176, 16, 30)), d.ellipse([5, 7, 13, 15], fill=(176, 16, 30)),
                               d.point([(7, 10), (7, 11)], fill=(255, 120, 130))),
        # 금 간 심장
        "frailty": lambda d: (d.ellipse([3, 4, 9, 10], fill=(138, 122, 168)), d.ellipse([8, 4, 14, 10], fill=(138, 122, 168)),
                              d.polygon([(3, 8), (14, 8), (9, 14)], fill=(138, 122, 168)),
                              d.line([(9, 5), (8, 8), (10, 10), (9, 13)], fill=(40, 30, 60))),
        # 엇갈린 칼
        "berserk": lambda d: (d.line([(4, 4), (13, 13)], fill=(208, 32, 42), width=2), d.line([(13, 4), (4, 13)], fill=(208, 32, 42), width=2),
                              d.line([(3, 12), (6, 15)], fill=(90, 60, 30), width=2), d.line([(14, 12), (11, 15)], fill=(90, 60, 30), width=2)),
        # 유리 조각 + 금
        "glass_cannon": lambda d: (d.polygon([(9, 2), (15, 9), (9, 16), (3, 9)], fill=(154, 232, 255)),
                                   d.polygon([(9, 4), (13, 9), (9, 14), (5, 9)], fill=(200, 245, 255)),
                                   d.line([(7, 6), (10, 9), (8, 12)], fill=(60, 120, 150))),
    }
    # 망자의 두루마리: 귀환의 두루마리를 청록·보라 빛으로
    from PIL import Image as _I
    import colorsys as _c
    src = _I.open(OUT / "item/recall_scroll.png").convert("RGBA")
    px = src.load()
    for x in range(src.width):
        for y in range(src.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s_ = _c.rgb_to_hls(r / 255, g / 255, b / 255)
            if s_ > 0.25:
                rr, gg, bb = _c.hls_to_rgb(0.5, l, min(1.0, s_ + 0.1))
                px[x, y] = (int(rr * 255), int(gg * 255), int(bb * 255), a)
            else:
                px[x, y] = (int(r * 0.8), int(g * 0.85), int(b * 0.95), a)
    src.save(OUT / "item/death_scroll.png")
    (OUT / "mob_effect").mkdir(parents=True, exist_ok=True)
    for name, fn in icons.items():
        outline(icon(fn)).save(OUT / f"mob_effect/{name}.png")


if __name__ == "__main__":
    build()
