"""2.20(요리 2차): kitchen_design 도안 상자들을 마인크래프트 블록 모델(elements)과 텍스처 아틀라스로 바꾼다.
도안 좌표: x 오른쪽, y 위, z 앞(+z 정면), 픽셀(16 = 1칸). 모델(정면 = 북쪽 -z): mx = x + XOFF, my = y, mz = ZOFF - z.
면마다 도안의 칠하기 함수로 그린 픽셀을 아틀라스에 붙이고 UV 로 가리킨다 → 도안과 똑같이 보인다."""
import json
import math
from pathlib import Path

from PIL import Image

import kitchen_design as K

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus"

# 이름: (도안, 좌우 칸 lat0..lat1, 앞뒤 칸 dep0..dep1, 높이 칸)
STATIONS = {
    "cooking_pot": (K.cauldron, 0, 1, 0, 0, 2),
    "brick_oven": (K.oven, 0, 1, 0, 1, 2),
    "bbq_grill": (K.grill, 0, 1, 0, 0, 2),
    "prep_table": (K.prep, 0, 1, 0, 0, 2),
    "kitchen_stove": (K.stove, 0, 0, 0, 0, 2),
    "aging_rack": (K.aging, 0, 1, 0, 0, 2),
}
FACE = {"front": "north", "back": "south", "right": "east", "left": "west", "top": "up", "bottom": "down"}
ANGLES = [-45, -22.5, 0, 22.5, 45]


def squash(y, top):
    """모델은 높이 32 까지만 → 26 위쪽을 눌러 담는다"""
    if top <= 32 or y <= 26:
        return y
    return 26 + (y - 26) * 6 / (top - 26)


def faces_of(b):
    """(도안 면 이름, 가로 픽셀, 세로 픽셀)"""
    w, h, d = max(1, round(b.w)), max(1, round(b.h)), max(1, round(b.d))
    return [("front", w, h), ("back", w, h), ("right", d, h), ("left", d, h), ("top", w, d), ("bottom", w, d)]


def export(name, fn, lat0, lat1, dep0, dep1, height):
    K.STEAM_BOXES = False
    boxes = fn()
    # 칸 중심에 맞춤
    xs = [b.x0 for b in boxes] + [b.x0 + b.w for b in boxes]
    zs = [b.z0 for b in boxes] + [b.z0 + b.d for b in boxes]
    top = max(b.y0 + b.h for b in boxes)
    cx = (lat0 + lat1 + 1) * 8  # 차지하는 칸의 가운데 (모델 x)
    cz = (dep0 + dep1 + 1) * 8
    xoff = cx - (min(xs) + max(xs)) / 2
    zoff = cz + (min(zs) + max(zs)) / 2
    span = max(max(xs) - min(xs), max(zs) - min(zs))
    k = min(1.0, 48 / span) if span > 48 else 1.0  # 너무 크면 줄임

    # 아틀라스 (선반 쌓기)
    A = 512
    atlas = Image.new("RGBA", (A, A), (0, 0, 0, 0))
    px = atlas.load()
    cur_x = cur_y = row_h = 0
    elements = []
    for b in boxes:
        x0 = (b.x0 * k + xoff)
        x1 = ((b.x0 + b.w) * k + xoff)
        y0, y1 = squash(b.y0, top), squash(b.y0 + b.h, top)
        z0 = zoff - (b.z0 + b.d) * k
        z1 = zoff - b.z0 * k
        clamp = lambda v: max(-16.0, min(32.0, v))
        frm = [round(clamp(x0), 3), round(clamp(y0), 3), round(clamp(z0), 3)]
        to = [round(clamp(x1), 3), round(clamp(y1), 3), round(clamp(z1), 3)]
        if any(to[i] - frm[i] < 0.01 for i in range(3)):
            to = [max(to[i], frm[i] + 0.05) for i in range(3)]
        el = {"from": frm, "to": to, "faces": {}}
        for face, fw, fh in faces_of(b):
            if cur_x + fw > A:
                cur_x, cur_y, row_h = 0, cur_y + row_h, 0
            if cur_y + fh > A:
                raise SystemExit(f"atlas full: {name}")
            any_px = False
            for u in range(fw):
                for v in range(fh):
                    c = b.paint(face, u, v, fw, fh)
                    if c is None:
                        continue
                    any_px = True
                    px[cur_x + u, cur_y + v] = tuple(int(t) for t in c[:3]) + (255,)
            if any_px:
                uv = [cur_x * 16 / A, cur_y * 16 / A, (cur_x + fw) * 16 / A, (cur_y + fh) * 16 / A]
                el["faces"][FACE[face]] = {"texture": "#atlas", "uv": [round(t, 4) for t in uv]}
            cur_x += fw
            row_h = max(row_h, fh)
        if b.rot:
            axis, ang, piv = b.rot
            deg = math.degrees(ang) * (-1 if axis == "x" else 1)
            deg = min(ANGLES, key=lambda a: abs(a - deg))
            if deg != 0:
                el["rotation"] = {"origin": [round(clamp(piv[0] * k + xoff), 3), round(clamp(squash(piv[1], top)), 3),
                                             round(clamp(zoff - piv[2] * k), 3)], "axis": axis, "angle": deg}
        if el["faces"]:
            elements.append(el)
    atlas.save(ROOT / f"textures/block/{name}.png")
    raid = json.loads((ROOT / "models/block/raid_altar.json").read_text())
    model = {"textures": {"atlas": f"wildplus:block/{name}", "particle": f"wildplus:block/{name}"},
             "render_type": "minecraft:cutout", "elements": elements, "display": raid["display"]}
    (ROOT / f"models/block/{name}.json").write_text(json.dumps(model, ensure_ascii=False))
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    (ROOT / f"blockstates/{name}.json").write_text(json.dumps({"variants": {
        f"facing={f}": ({"model": f"wildplus:block/{name}", "y": r} if r else {"model": f"wildplus:block/{name}"}) for f, r in rot.items()}}, indent=2))
    (ROOT / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"wildplus:block/{name}"}}, indent=2))
    return len(elements)


def build():
    return {n: export(n, *cfg) for n, cfg in STATIONS.items()}


if __name__ == "__main__":
    print(build())
