"""치장 도안: 상자(큐브) 모델 + 칸마다 색칠 → 입체 미리보기 그림 (게임 모델과 같은 치수, 1칸 = 마인크래프트 1픽셀).

좌표: x 오른쪽(플레이어 왼쪽이 +x), y 위, z 앞. 발바닥 y=0, 정수리 y=32.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFont

SKIN = (247, 200, 168)
SKIN_D = (226, 172, 140)
HAIR = (70, 46, 34)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Box:
    """(x0,y0,z0) 아래·왼·뒤 모서리, 크기 (w,h,d). paint(face, u, v, W, H) -> (r,g,b) 또는 None(투명)"""

    def __init__(self, x0, y0, z0, w, h, d, paint, grow=0.0, rot=None):
        self.x0, self.y0, self.z0, self.w, self.h, self.d = x0, y0, z0, w, h, d
        self.paint, self.grow = paint, grow
        self.rot = rot  # (축 'x', 각도, 기준점) 날개용

    def quads(self):
        g = self.grow
        x0, y0, z0 = self.x0 - g, self.y0 - g, self.z0 - g
        W, H, D = self.w + 2 * g, self.h + 2 * g, self.d + 2 * g
        nw, nh, nd = max(1, round(self.w)), max(1, round(self.h)), max(1, round(self.d))
        out = []

        def add(face, origin, du, dv, nu, nv, normal):
            for u in range(nu):
                for v in range(nv):
                    c = self.paint(face, u, v, nu, nv)
                    if c is None:
                        continue
                    p = [tuple(origin[k] + du[k] * (u + a) / nu + dv[k] * (v + b) / nv for k in range(3)) for a, b in ((0, 0), (1, 0), (1, 1), (0, 1))]
                    out.append((p, c, normal))
        # 앞(+z): u →x, v ↓y
        add("front", (x0, y0 + H, z0 + D), (W, 0, 0), (0, -H, 0), nw, nh, (0, 0, 1))
        add("back", (x0 + W, y0 + H, z0), (-W, 0, 0), (0, -H, 0), nw, nh, (0, 0, -1))
        add("right", (x0 + W, y0 + H, z0 + D), (0, 0, -D), (0, -H, 0), nd, nh, (1, 0, 0))
        add("left", (x0, y0 + H, z0), (0, 0, D), (0, -H, 0), nd, nh, (-1, 0, 0))
        add("top", (x0, y0 + H, z0), (W, 0, 0), (0, 0, D), nw, nd, (0, 1, 0))
        add("bottom", (x0, y0, z0 + D), (W, 0, 0), (0, 0, -D), nw, nd, (0, -1, 0))
        if self.rot:
            axis, ang, piv = self.rot
            out = [([rotate(p, axis, ang, piv) for p in ps], c, rotate(n, axis, ang, (0, 0, 0))) for ps, c, n in out]
        return out


def rotate(p, axis, ang, piv):
    x, y, z = p[0] - piv[0], p[1] - piv[1], p[2] - piv[2]
    c, s = math.cos(ang), math.sin(ang)
    if axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    elif axis == "z":
        x, y = x * c - y * s, x * s + y * c
    else:
        y, z = y * c - z * s, y * s + z * c
    return (x + piv[0], y + piv[1], z + piv[2])


def render(boxes, yaw=-35, pitch=18, scale=9, size=(360, 420), bg=(40, 43, 56), sparkles=None):
    img = Image.new("RGBA", size, bg + (255,))
    d = ImageDraw.Draw(img)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    light = (0.45, 0.8, 0.55)

    def proj(p):
        x, y, z = p[0], p[1] - 16, p[2]
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return size[0] / 2 + x * scale, size[1] / 2 - y * scale + 10, z

    polys = []
    for b in boxes:
        for ps, c, n in b.quads():
            n2 = rotate(n, "y", math.radians(-yaw), (0, 0, 0))
            # 카메라 쪽을 보는 면만
            nx, ny, nz = n
            vx, vz = nx * cy + nz * sy, -nx * sy + nz * cy
            vy, vz = ny * cp - vz * sp, ny * sp + vz * cp
            if vz <= 0.01:
                continue
            pts = [proj(p) for p in ps]
            depth = sum(q[2] for q in pts) / 4
            lit = 0.62 + 0.38 * max(0, n[0] * light[0] + n[1] * light[1] + n[2] * light[2])
            polys.append((depth, [(q[0], q[1]) for q in pts], shade(c, lit)))
    polys.sort(key=lambda t: t[0])
    for _, pts, c in polys:
        d.polygon(pts, fill=c + (255,), outline=c + (255,))
    if sparkles:
        r = random.Random(3)
        for (x, y, z, col) in sparkles:
            px, py, _ = proj((x, y, z))
            s = r.choice([2, 3])
            d.line((px - s * 2, py, px + s * 2, py), fill=col + (220,), width=1)
            d.line((px, py - s * 2, px, py + s * 2), fill=col + (220,), width=1)
            d.ellipse((px - 2, py - 2, px + 2, py + 2), fill=(255, 255, 255, 230))
    return img


# ───────── 마네킹 (살구색 몸) ─────────
def solid(c, top=None, dark=0.0):
    def f(face, u, v, W, H):
        base = top if (face == "top" and top) else c
        if dark and face in ("front", "back", "left", "right"):
            base = mix(base, shade(base, 0.8), v / max(1, H - 1) * dark)
        return base
    return f


def head_paint(face, u, v, W, H):
    if face == "top" or v <= 1:
        return HAIR
    if face == "back":
        return HAIR if v < 6 else SKIN
    if face in ("left", "right"):
        return HAIR if v < 3 or (face == "left" and u < 3) or (face == "right" and u > 4) else SKIN
    if face == "front":
        if v == 4 and u in (2, 5):
            return (60, 90, 160)
        if v == 4 and u in (1, 6):
            return (250, 250, 250)
        if v == 6 and u in (3, 4):
            return (200, 120, 110)
        return SKIN
    return SKIN


def mannequin():
    return [
        Box(-4, 24, -4, 8, 8, 8, head_paint),
        Box(-4, 12, -2, 8, 12, 4, solid(SKIN)),
        Box(-8, 12, -2, 4, 12, 4, solid(SKIN)),
        Box(4, 12, -2, 4, 12, 4, solid(SKIN)),
        Box(-4, 0, -2, 4, 12, 4, solid(SKIN)),
        Box(0, 0, -2, 4, 12, 4, solid(SKIN)),
    ]


def sheet(title, views, path, font_path="/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", note=None):
    """views: [(이름, 그림)]"""
    f1 = ImageFont.truetype(font_path, 26)
    f2 = ImageFont.truetype(font_path, 16)
    cols = min(4, len(views))
    rows = (len(views) + cols - 1) // cols
    W = cols * 380 + 20
    H = 70 + rows * 470 + (40 if note else 0)
    out = Image.new("RGBA", (W, H), (28, 30, 40, 255))
    d = ImageDraw.Draw(out)
    d.text((20, 18), title, font=f1, fill=(255, 214, 110))
    for i, (name, im) in enumerate(views):
        x, y = 20 + (i % cols) * 380, 70 + (i // cols) * 470
        out.alpha_composite(im, (x, y))
        d.text((x + 180, y + 438), name, font=f2, fill=(240, 240, 250), anchor="mm")
    if note:
        d.text((20, H - 32), note, font=f2, fill=(180, 186, 200))
    out.save(path)
