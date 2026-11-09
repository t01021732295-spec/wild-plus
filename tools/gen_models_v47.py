"""4.7: 도안(Box 목록)을 실제 게임 모델로 → assets/wildplus/models_gen/<이름>.json + textures/entity/gen/<이름>.png.
GenModelRenderer 가 읽어서 큐브로 굽는다. 그룹마다 따로 움직이고, glow 는 어두워도 빛난다."""
import json
import math
from pathlib import Path

from PIL import Image

import boss_designs_v47 as D

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus"
FACES = ("top", "bottom", "left", "front", "right", "back")
BRIGHT = {D.LV, D.LV2, D.HOT, D.EL, D.BL, (255, 255, 255)}


def solid_color(b):
    try:
        cs = {b.paint("front", u, v, 8, 8) for u in range(8) for v in range(8)}
        return next(iter(cs)) if len(cs) == 1 else None
    except Exception:
        return None


def build(name, boxes, orig_shift, grouper):
    min_y = min(b.y0 for b in boxes)
    shift = -min_y
    cubes = []
    for b in boxes:
        w, h, d = (max(1, math.ceil(v)) for v in (b.w, b.h, b.d))
        c = solid_color(b)
        glow = c in BRIGHT
        o = (b.x0, b.y0 - orig_shift, b.z0, b.w, b.h, b.d, glow)
        cubes.append((b, w, h, d, glow, grouper(*o)))
    W = 1024
    x = y = row = 0
    placed = []
    for b, w, h, d, glow, group in sorted(cubes, key=lambda c: -(c[2] + c[3])):
        nw, nh = 2 * (w + d), h + d
        if x + nw > W:
            x, y, row = 0, y + row, 0
        placed.append((x, y, b, w, h, d, glow, group))
        x += nw
        row = max(row, nh)
    H = y + row
    H = 2 ** math.ceil(math.log2(max(H, 1)))
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = im.load()
    def face(ox, oy, fw, fh, name_, b):
        for u in range(fw):
            for v in range(fh):
                c = b.paint(name_, u, v, fw, fh)
                if c is not None:
                    px[ox + u, oy + v] = tuple(c) + (255,)
    groups = {}
    for u, v, b, w, h, d, glow, group in placed:
        face(u + d, v, w, d, "top", b)
        face(u + d + w, v, w, d, "bottom", b)
        face(u, v + d, d, h, "left", b)
        face(u + d, v + d, w, h, "front", b)
        face(u + d + w, v + d, d, h, "right", b)
        face(u + d + w + d, v + d, w, h, "back", b)
        y0 = b.y0 + shift
        groups.setdefault(group, {"solid": [], "glow": []})["glow" if glow else "solid"].append([u, v, round(b.x0, 2), round(24 - (y0 + b.h), 2), round(-(b.z0 + b.d), 2), round(b.w, 2), round(b.h, 2), round(b.d, 2)])
    (ROOT / "textures/entity/gen").mkdir(parents=True, exist_ok=True)
    (ROOT / "models_gen").mkdir(parents=True, exist_ok=True)
    im.save(ROOT / f"textures/entity/gen/{name}.png")
    (ROOT / f"models_gen/{name}.json").write_text(json.dumps({"tw": W, "th": H, "yshift": shift + orig_shift, "groups": groups}, separators=(",", ":")))
    print(name, len(cubes), W, H, {k: len(v["solid"]) + len(v["glow"]) for k, v in groups.items()})


def main():
    def volcano(x, y, z, w, h, d, glow):
        if glow and -13 <= x and x + w <= 13 and z >= 13 and 45 <= y <= 90: return "heart"
        if x + w <= -24: return "armL"
        if x >= 26: return "armR"
        return "body"
    def storm(x, y, z, w, h, d, glow):
        if x <= -38: return "swords"
        if z <= -14.5 and y < 76: return "cape"
        if y >= 79 and -13 <= x and x + w <= 13: return "helm"
        return "body"
    def hawk(x, y, z, w, h, d, glow):
        if y >= 23 and x >= 5: return "wingR"
        if y >= 23 and x + w <= -5: return "wingL"
        return "body"
    def sentinel(x, y, z, w, h, d, glow):
        return "cloud" if y < 11 else "body"
    def priest(x, y, z, w, h, d, glow):
        return "ring" if y >= 44 and w <= 1.5 and d <= 1.5 else "body"
    build("volcano", D.g, D.Y, volcano)
    build("storm_king", D.K, D.Q, storm)
    build("thunder_hawk", D.T, 0, hawk)
    build("cloud_sentinel", D.P, 0, sentinel)
    build("cloud_priest", D.A, 0, priest)


if __name__ == "__main__":
    main()
