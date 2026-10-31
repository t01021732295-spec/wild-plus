"""10차 텍스처: 상인 스킨(바닐라 주민 레이어 합성) + 현실 기기처럼 보이는 블록 겉모습.

python3 tools/gen_textures.py (WILDPLUS_VANILLA 필요) 에서 불린다.
"""
import colorsys
from pathlib import Path

from PIL import Image

from textures_v5 import OUT, gradient, lum

T = OUT


def hx(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + ((int(h[6:8], 16),) if len(h) == 8 else (255,))


class P:
    """16×16 그림판"""

    def __init__(self, base=None, size=16):
        if base is None:
            self.im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        else:
            self.im = base.convert("RGBA").copy()
        self.px = self.im.load()
        self.w, self.h = self.im.size

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = hx(c) if isinstance(c, str) else c

    def rect(self, x0, y0, x1, y1, c):
        """x1, y1 포함"""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.put(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.put(x, y0, c)
            self.put(x, y1, c)
        for y in range(y0, y1 + 1):
            self.put(x0, y, c)
            self.put(x1, y, c)

    def bevel(self, x0, y0, x1, y1, light, dark):
        for x in range(x0, x1 + 1):
            self.put(x, y0, light)
            self.put(x, y1, dark)
        for y in range(y0, y1 + 1):
            self.put(x0, y, light)
            self.put(x1, y, dark)

    def vgrad(self, x0, y0, x1, y1, top, bottom):
        for y in range(y0, y1 + 1):
            t = (y - y0) / max(1, y1 - y0)
            c = gradient([top, bottom], t)
            for x in range(x0, x1 + 1):
                self.put(x, y, c + (255,))

    def tint(self, color, alpha):
        c = hx(color)
        for x in range(self.w):
            for y in range(self.h):
                r, g, b, a = self.px[x, y]
                if a:
                    self.px[x, y] = (round(r + (c[0] - r) * alpha), round(g + (c[1] - g) * alpha), round(b + (c[2] - b) * alpha), a)
        return self

    def save(self, name):
        p = T / name
        p.parent.mkdir(parents=True, exist_ok=True)
        self.im.save(p)


def V(vanilla: Path, name):
    for root in (vanilla / "minecraft/textures/block", vanilla / "assets/minecraft/textures/block"):
        if (root / f"{name}.png").exists():
            return Image.open(root / f"{name}.png").convert("RGBA")
    raise FileNotFoundError(name)


# ───────────────────────── 상인 ─────────────────────────
def _recolor(im, pal, sel=lambda h, s, l: True):
    im = im.copy()
    px = im.load()
    pts = []
    for x in range(im.width):
        for y in range(im.height):
            c = px[x, y]
            if c[3] == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in c[:3]))
            if sel(h, s, l):
                pts.append((x, y))
    if not pts:
        return im
    ls = [lum(px[p]) for p in pts]
    lo, hi = min(ls), max(ls)
    for p in pts:
        px[p] = gradient(pal, (lum(px[p]) - lo) / max(1, hi - lo)) + (px[p][3],)
    return im


def merchants(vanilla: Path):
    root = vanilla / "minecraft/textures/entity/villager"
    if not root.exists():
        root = vanilla / "assets/minecraft/textures/entity/villager"
    L = lambda p: Image.open(root / p).convert("RGBA")
    base, typ = L("villager.png"), L("type/plains.png")

    def comp(*layers):
        out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        for layer in layers:
            out.alpha_composite(layer)
        # 상인 배지 (가슴의 금화)
        px = out.load()
        for (x, y) in [(11, 47), (12, 47), (11, 48), (12, 48)]:
            px[x, y] = (242, 193, 78, 255)
        px[11, 47] = (255, 232, 150, 255)
        return out

    skins = {
        "merchant": comp(base, _recolor(typ, ["#10301a", "#1d4f2c", "#2f7a43", "#4fa35f", "#8fd08f"]),
                         _recolor(L("profession/cartographer.png"), ["#3a2410", "#6b4420", "#a6722e", "#e0b050", "#ffe8a0"])),
        "blacksmith_merchant": comp(base, _recolor(typ, ["#1a1a1c", "#2c2c30", "#45454c", "#6a6a74", "#9a9aa4"]),
                                    L("profession/weaponsmith.png")),
        "farmer_merchant": comp(base, _recolor(typ, ["#14223a", "#22385e", "#34558a", "#5a7fb0", "#a8c4e0"]), L("profession/farmer.png")),
        "fisher_merchant": comp(base, _recolor(typ, ["#0e2a3a", "#1a4a60", "#2a6e88", "#4a98b0", "#9ad0e0"]), L("profession/fisherman.png")),
        "banker_merchant": comp(base, _recolor(typ, ["#0b1426", "#16264a", "#24407a", "#3a5fa8", "#7f9fd8"]),
                                _recolor(L("profession/librarian.png"), ["#0c0c0c", "#1a1a1a", "#2a2a2a", "#3c3c3c"],
                                         lambda h, s, l: s > 0.3 and (h < 0.06 or h > 0.9))),
    }
    for name, im in skins.items():
        im.save(T / f"entity/{name}.png")
    # 생성 알: 잡화 상인 알을 재색칠
    egg = Image.open(T / "item/merchant_spawn_egg.png").convert("RGBA")
    for name, pal in (("farmer_merchant", ["#1a2a4a", "#34558a", "#e0c040", "#fff0a0"]),
                      ("fisher_merchant", ["#0e2a3a", "#2a6e88", "#e08a3a", "#ffd0a0"]),
                      ("banker_merchant", ["#0b1426", "#24407a", "#d9a22e", "#ffe8a0"])):
        _recolor(egg, pal).save(T / f"item/{name}_spawn_egg.png")


# ───────────────────────── 블록 ─────────────────────────
STEEL = ["#7b828c", "#9aa1ab", "#b3b9c2", "#c9ced6", "#dde1e7"]


def brushed(vanilla, light=0.0):
    """철 블록을 바탕으로 붓질한 금속판"""
    p = P(V(vanilla, "iron_block"))
    for y in range(16):
        for x in range(16):
            r, g, b, a = p.px[x, y]
            d = ((x * 7 + y * 13) % 5 - 2) * 2
            p.px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d + 2)), a)
    if light:
        p.tint("#ffffff", light)
    return p


def atm(v):
    p = brushed(v)
    p.rect(0, 0, 15, 2, "#123a7a")
    p.rect(0, 2, 15, 2, "#0b2552")
    p.rect(2, 1, 4, 1, "#ffffff")  # 로고
    p.put(13, 1, "#46f08a")
    # 화면
    p.rect(2, 4, 13, 8, "#14181e")
    p.rect(3, 5, 12, 7, "#2f6fd6")
    p.rect(3, 5, 12, 5, "#5a94f0")
    p.rect(4, 6, 8, 6, "#d8e8ff")
    p.rect(10, 7, 11, 7, "#ffd84a")
    # 키패드 + 카드 투입구
    for ky in (10, 12):
        for kx in (3, 5, 7):
            p.rect(kx, ky, kx + 1, ky, "#e8ebef")
            p.put(kx, ky + 1, "#6a717b")
    p.rect(10, 10, 13, 10, "#0e0f12")
    p.put(13, 11, "#46f08a")
    # 현금 배출구
    p.rect(3, 14, 12, 14, "#0e0f12")
    p.rect(3, 15, 12, 15, "#5a616b")
    p.save("block/atm_front.png")
    s = brushed(v)
    s.rect(0, 0, 15, 2, "#123a7a")
    s.rect(0, 2, 15, 2, "#0b2552")
    for y in range(5, 15, 2):
        s.rect(4, y, 11, y, "#6a717b")
    s.save("block/atm_side.png")
    t = brushed(v)
    t.tint("#1a2a4a", 0.55)
    t.frame(0, 0, 15, 15, "#0b1426")
    t.save("block/atm_top.png")


def stock_terminal(v):
    # 모니터 화면 (주식 차트)
    p = P()
    p.rect(0, 0, 15, 15, "#0b0d10")
    p.rect(1, 1, 14, 14, "#0e141c")
    for x in range(1, 15, 3):
        p.rect(x, 1, x, 14, "#16202c")
    ys = [11, 10, 11, 9, 8, 9, 7, 6, 7, 5, 4, 5, 3, 4]
    for i, y in enumerate(ys):
        x = 1 + i
        up = i == 0 or y <= ys[i - 1]
        p.rect(x, y, x, y + 1, "#ff5a5f" if up else "#4f95ff")
    p.rect(1, 13, 14, 14, "#161e29")
    p.rect(2, 13, 4, 13, "#ff5a5f")
    p.rect(6, 13, 8, 13, "#4f95ff")
    p.put(13, 2, "#2bd46a")
    p.save("block/stock_screen.png")
    b = P()
    b.rect(0, 0, 15, 15, "#1a1d22")
    b.rect(6, 6, 9, 9, "#2a2e35")
    b.save("block/stock_monitor_back.png")
    # 책상 (짙은 나무 + 서랍)
    d = P(V(v, "dark_oak_planks"))
    d.rect(0, 0, 15, 0, "#2a1a0c")
    d.frame(2, 3, 13, 7, "#24160a")
    d.rect(7, 5, 8, 5, "#c9a03a")
    d.frame(2, 9, 13, 14, "#24160a")
    d.rect(7, 11, 8, 11, "#c9a03a")
    d.save("block/stock_desk_front.png")
    k = P(V(v, "dark_oak_planks"))
    k.rect(2, 9, 13, 12, "#1a1d22")  # 키보드
    for x in range(3, 13, 2):
        k.put(x, 10, "#5a606a")
        k.put(x, 11, "#5a606a")
    k.save("block/stock_desk_top.png")


def exchange(v):
    """위 절반 = 유리 칸막이(투명), 아래 절반 = 창구 카운터. 모델이 위·아래를 나눠 쓴다"""
    q = P(V(v, "quartz_block_side"))
    # 위: 유리 칸막이 (틀 + 반사선, 안은 투명)
    q.rect(0, 0, 15, 7, (0, 0, 0, 0))
    q.frame(0, 0, 15, 7, "#2a2e35")
    q.rect(0, 0, 15, 1, "#0f4d3a")
    q.rect(5, 0, 10, 0, "#ffe08a")
    for i in range(3):
        q.put(3 + i, 3 + i, "#e8f4ff")
        q.put(10 + i, 2 + i, "#e8f4ff")
    q.rect(5, 6, 10, 7, (0, 0, 0, 0))
    q.frame(4, 5, 11, 7, "#2a2e35")
    q.rect(5, 6, 10, 7, (0, 0, 0, 0))
    # 아래: 카운터 (초록 간판 + ₩)
    q.rect(0, 8, 15, 8, "#c9a03a")
    q.rect(2, 9, 13, 14, "#0f4d3a")
    q.frame(2, 9, 13, 14, "#c9a03a")
    for (x, y) in [(5, 10), (6, 12), (7, 11), (8, 11), (9, 12), (10, 10)]:
        q.put(x, y, "#ffe08a")
    q.rect(4, 11, 11, 11, "#ffe08a")
    q.rect(0, 15, 15, 15, "#5a5a5a")
    q.save("block/exchange_front.png")
    t = P(V(v, "polished_andesite"))
    t.frame(0, 0, 15, 15, "#5a5a5a")
    t.save("block/exchange_top.png")
    g = P()
    g.rect(0, 0, 15, 15, "#2a2e35")
    g.save("block/exchange_frame.png")


def bounty_board(v):
    c = P()
    c.vgrad(0, 0, 15, 15, "#c99a62", "#b5844e")
    seed = 7
    for i in range(40):
        seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF
        c.put(seed % 16, (seed >> 8) % 16, "#9a6a38" if i % 2 else "#dcb27a")
    c.frame(0, 0, 15, 15, "#5a3618")
    for (x0, y0, x1, y1, pin) in [(2, 2, 6, 7, "#e0252b"), (8, 3, 13, 8, "#2a7ae0"), (3, 9, 8, 13, "#1e9a50"), (10, 10, 13, 14, "#f2c14e")]:
        c.rect(x0, y0, x1, y1, "#fbf5e3")
        for y in range(y0 + 2, y1, 2):
            c.rect(x0 + 1, y, x1 - 1, y, "#b8b0a0")
        c.put((x0 + x1) // 2, y0, pin)
    c.save("block/bounty_board_front.png")
    b = P(V(v, "spruce_planks"))
    b.save("block/bounty_board_back.png")


def pharmacy(v):
    p = P(V(v, "white_concrete"))
    p.rect(0, 0, 15, 3, "#1e9a50")
    for (x, y) in [(7, 0), (8, 0), (6, 1), (7, 1), (8, 1), (9, 1), (7, 2), (8, 2)]:
        p.put(x, y, "#ffffff")
    # 유리 진열장 + 약병
    p.rect(1, 5, 14, 14, "#9fc7d6")
    p.frame(1, 5, 14, 14, "#c8ced6")
    p.rect(2, 9, 13, 9, "#e8ecf0")
    for i, col in enumerate(["#e0252b", "#2a7ae0", "#f2c14e", "#1e9a50", "#b03cc0"]):
        x = 2 + i * 2 + (1 if i > 2 else 0)
        p.rect(x, 7, x, 8, col)
        p.put(x, 6, "#ffffff")
    for i, col in enumerate(["#1e9a50", "#e07a1a", "#e0252b", "#2a7ae0"]):
        x = 3 + i * 3
        p.rect(x, 11, x + 1, 13, col)
        p.rect(x, 11, x + 1, 11, "#ffffff")
    p.save("block/pharmacy_front.png")
    s = P(V(v, "white_concrete"))
    s.rect(0, 0, 15, 3, "#1e9a50")
    s.rect(0, 13, 15, 15, "#c8ced6")
    s.save("block/pharmacy_side.png")


def weather_station(v):
    b = P(V(v, "white_concrete"))
    b.frame(0, 0, 15, 15, "#9aa1ab")
    b.rect(3, 3, 12, 8, "#14181e")  # 계기판 LCD
    b.rect(4, 4, 6, 4, "#ffd84a")
    b.rect(8, 4, 11, 4, "#5ac8ff")
    b.rect(4, 6, 11, 6, "#46f08a")
    for x in (4, 7, 10):
        b.rect(x, 10, x + 1, 13, "#c9ced6")  # 통풍 루버
    b.save("block/weather_box.png")
    m = P()
    m.rect(0, 0, 15, 15, "#c9ced6")
    m.rect(0, 0, 15, 0, "#ffffff")
    m.save("block/weather_metal.png")
    cup = P()
    cup.rect(0, 0, 15, 15, "#e0252b")
    cup.save("block/weather_cup.png")


def enhance(v):
    a = P(V(v, "anvil"))
    a.save("block/enhance_anvil.png")
    f = P(V(v, "polished_andesite"))
    f.tint("#2a2a30", 0.5)
    f.frame(0, 0, 15, 15, "#141418")
    f.rect(3, 6, 12, 13, "#1a0a04")
    f.vgrad(4, 8, 11, 13, "#ff9a2a", "#ff3a0a")
    for (x, y) in [(5, 9), (8, 10), (10, 9), (6, 12)]:
        f.put(x, y, "#ffe08a")
    f.rect(3, 6, 12, 6, "#3a3a40")
    f.put(2, 2, "#c9ced6")
    f.put(13, 2, "#c9ced6")
    f.save("block/enhance_forge_front.png")
    s = P(V(v, "polished_andesite"))
    s.tint("#2a2a30", 0.5)
    s.frame(0, 0, 15, 15, "#141418")
    s.save("block/enhance_forge_side.png")


def chunk_loader(v):
    s = brushed(v)
    s.tint("#2a2440", 0.55)
    s.frame(0, 0, 15, 15, "#141022")
    s.frame(1, 1, 14, 14, "#4a3a70")
    s.rect(4, 4, 11, 11, "#1a0a2a")
    s.rect(5, 5, 10, 10, "#7a3ae0")
    s.rect(6, 6, 9, 9, "#c08aff")
    s.rect(7, 7, 8, 8, "#ffffff")
    for c in (2, 13):
        s.put(c, 2, "#c9ced6")
        s.put(c, 13, "#c9ced6")
    s.save("block/chunk_loader_side.png")
    t = P(V(v, "obsidian"))
    t.rect(6, 6, 9, 9, "#c08aff")
    t.frame(5, 5, 10, 10, "#7a3ae0")
    t.save("block/chunk_loader_top.png")


def shipping_bin(v):
    s = P(V(v, "spruce_planks"))
    s.frame(0, 0, 15, 15, "#3a2412")
    for i in range(16):
        s.put(i, i, "#4a3018")
    s.rect(0, 0, 2, 2, "#8a9098")
    s.rect(13, 0, 15, 2, "#8a9098")
    s.rect(0, 13, 2, 15, "#8a9098")
    s.rect(13, 13, 15, 15, "#8a9098")
    s.rect(5, 6, 10, 9, "#f6eed6")  # 출하 표찰
    s.rect(6, 7, 9, 7, "#8a6a3a")
    s.rect(6, 8, 8, 8, "#8a6a3a")
    s.save("block/shipping_bin_side.png")
    t = P(V(v, "spruce_planks"))
    t.frame(0, 0, 15, 15, "#3a2412")
    t.rect(1, 7, 14, 8, "#2a1a0c")  # 넣는 틈
    t.rect(6, 3, 9, 4, "#8a9098")
    t.save("block/shipping_bin_top.png")


def casino(v):
    f = P(V(v, "green_wool"))
    f.tint("#1e7a45", 0.45)
    f.frame(0, 0, 15, 15, "#5a3418")
    f.frame(1, 1, 14, 14, "#c9a54a")
    for (x, y, c) in [(4, 4, "#e0252b"), (5, 4, "#e0252b"), (11, 5, "#1e1e22"), (12, 5, "#1e1e22"), (4, 11, "#f2f2f2"), (5, 11, "#f2f2f2")]:
        f.put(x, y, c)
    f.rect(7, 7, 8, 8, "#e8dca8")
    f.save("block/casino_table_top.png")
    r = P(V(v, "dark_oak_planks"))
    r.rect(0, 0, 15, 0, "#c9a54a")
    r.save("block/casino_rail.png")


def machines(v):
    # 자동 수확기: 강철 + 작물이 보이는 창 + 경고 띠
    h = brushed(v)
    h.rect(0, 0, 15, 1, "#f2c14e")
    for x in range(0, 16, 4):
        h.rect(x, 0, x + 1, 1, "#1a1a1a")
    h.rect(2, 3, 13, 10, "#1a1d22")
    h.rect(3, 4, 12, 9, "#2a4a1a")
    for x in (4, 7, 10):
        h.rect(x, 6, x, 9, "#6ab03a")
        h.rect(x, 4, x + 1, 5, "#e0b020")
    h.rect(3, 4, 12, 4, (255, 255, 255, 60))
    h.rect(4, 12, 11, 14, "#3a3d44")
    h.rect(5, 13, 10, 13, "#0e0f12")
    h.put(13, 13, "#46f08a")
    h.save("block/auto_harvester_side.png")
    ht = brushed(v)
    ht.rect(5, 5, 10, 10, "#3a3d44")
    ht.rect(6, 6, 9, 9, "#0e0f12")
    ht.save("block/auto_harvester_top.png")
    # 광석 채굴기: 아래 함체 + 위 드릴 모터
    m = brushed(v)
    m.tint("#3a3d44", 0.35)
    m.rect(0, 0, 15, 1, "#f2c14e")
    for x in range(0, 16, 4):
        m.rect(x, 0, x + 1, 1, "#1a1a1a")
    m.rect(2, 4, 13, 12, "#1a1d22")
    for (x, y, c) in [(4, 6, "#8ad8f4"), (5, 7, "#8ad8f4"), (10, 5, "#e0b020"), (11, 6, "#e0b020"), (7, 9, "#d83a3a"), (10, 10, "#3fd46a"),
                      (4, 10, "#2f7db3"), (8, 6, "#c9ced6")]:
        m.put(x, y, c)
    m.put(13, 14, "#ff4040")
    m.save("block/ore_miner_side.png")
    mt = P(V(v, "polished_andesite"))
    mt.frame(0, 0, 15, 15, "#3a3d44")
    mt.save("block/ore_miner_top.png")
    mo = P()
    mo.rect(0, 0, 15, 15, "#f2c14e")
    mo.rect(0, 0, 15, 1, "#ffe08a")
    for y in range(3, 16, 3):
        mo.rect(0, y, 15, y, "#c99a2e")
    mo.save("block/ore_miner_motor.png")
    bit = P()
    bit.rect(0, 0, 15, 15, "#8ad8f4")
    for y in range(0, 16, 2):
        bit.rect(0, y, 15, y, "#5aa8d0")
    bit.save("block/ore_miner_bit.png")
    # 자동 낚시 장치: 나무 상자 + 낚싯대
    fsh = P(V(v, "barrel_side"))
    fsh.rect(3, 4, 12, 9, "#1a2a3a")
    fsh.rect(4, 5, 11, 8, "#2a6e88")
    fsh.rect(6, 6, 8, 7, "#e08a4a")
    fsh.rect(4, 5, 11, 5, (255, 255, 255, 60))
    fsh.rect(5, 11, 10, 12, "#f6eed6")
    fsh.put(13, 12, "#46f08a")
    fsh.save("block/auto_fisher_side.png")
    rod = P()
    rod.rect(0, 0, 15, 15, "#6b4420")
    rod.rect(0, 0, 15, 0, "#8a5a2a")
    rod.save("block/auto_fisher_rod.png")


def hall(v):
    mar = P(V(v, "quartz_block_side"))
    mar.frame(0, 0, 15, 15, "#c9a03a")
    for i in range(12):
        mar.put(2 + i, 3 + (i * 5) % 10, "#d8d2c6")
    mar.rect(4, 6, 11, 9, "#c9a03a")  # 명판
    mar.rect(5, 7, 10, 8, "#ffe08a")
    mar.save("block/hall_base.png")
    g = P(V(v, "gold_block"))
    g.save("block/hall_gold.png")


HEART = [
    "................",
    "................",
    "...kkk....kkk...",
    "..kRRRk..kRRRk..",
    ".kRrrRRkkRRRRDk.",
    ".kRrRRRRRRRRRDk.",
    ".kRRRRRRRRRRRDk.",
    ".kRRRRRRRRRRDDk.",
    "..kRRRRRRRRDDk..",
    "...kRRRRRRDDk...",
    "....kRRRRDDk....",
    ".....kRRDDk.....",
    "......kRDk......",
    ".......kk.......",
    "................",
    "................",
]


def lord_heart():
    from textures_v2 import render, save
    im = render(HEART, {"k": "#1a0006", "R": "#a8101e", "r": "#ff7a8a", "D": "#5a0610"})
    px = im.load()
    for (x, y) in [(6, 6), (9, 7), (7, 9)]:
        px[x, y] = (255, 210, 90, 255)  # 금빛 혈관
    save(im, "item/lord_heart.png")


def build(vanilla: Path):
    lord_heart()
    merchants(vanilla)
    atm(vanilla)
    stock_terminal(vanilla)
    exchange(vanilla)
    bounty_board(vanilla)
    pharmacy(vanilla)
    weather_station(vanilla)
    enhance(vanilla)
    chunk_loader(vanilla)
    shipping_bin(vanilla)
    casino(vanilla)
    machines(vanilla)
    hall(vanilla)
