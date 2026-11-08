"""3.9.3 최적화: kitchen_models 가 만든 512×512 모델 텍스처를 실제로 쓰는 만큼만 남긴다.
(블록 아틀라스가 2,900만 픽셀 → 360만 픽셀 정도로 줄어 메모리 · 로딩 · 밉맵 부담이 크게 줄어듦)
모델 UV 는 0..16 비율이라, 텍스처를 줄인 만큼 UV 를 키워 같은 자리를 가리키게 한다."""
import glob
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/wildplus")


def _p2(n):
    k = 16
    while k < n:
        k *= 2
    return k


def _models_using(tex_id):
    out = []
    for m in glob.glob(os.path.join(ROOT, "models/**/*.json"), recursive=True):
        j = json.load(open(m))
        keys = [k for k, v in j.get("textures", {}).items() if v == tex_id]
        if keys:
            out.append((m, j, keys))
    return out


def shrink(name, models=None):
    path = os.path.join(ROOT, "textures/block", name + ".png")
    if not os.path.exists(path) or os.path.exists(path + ".mcmeta"):
        return None
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    if w * h < 256 * 256:
        return None
    tex_id = "wildplus:block/" + name
    users = models if models is not None else _models_using(tex_id)
    if any(k not in ("atlas", "particle") for _, _, keys in users for k in keys):
        return None  # 다른 방식으로 쓰는 텍스처는 그대로
    bb = im.getbbox() or (0, 0, 16, 16)
    need_w, need_h = bb[2], bb[3]
    for _, j, keys in users:
        if "atlas" not in keys:
            continue
        for el in j.get("elements", []):
            for f in el.get("faces", {}).values():
                if f.get("texture") == "#atlas" and "uv" in f:
                    u = f["uv"]
                    need_w = max(need_w, max(u[0], u[2]) / 16 * w)
                    need_h = max(need_h, max(u[1], u[3]) / 16 * h)
    nw, nh = _p2(int(round(need_w + 0.49))), _p2(int(round(need_h + 0.49)))
    nw, nh = min(nw, w), min(nh, h)
    if (nw, nh) == (w, h):
        return (w, h)
    sx, sy = w / nw, h / nh
    for m, j, keys in users:
        if "atlas" not in keys:
            continue
        for el in j.get("elements", []):
            for f in el.get("faces", {}).values():
                if f.get("texture") == "#atlas" and "uv" in f:
                    u = f["uv"]
                    f["uv"] = [round(u[0] * sx, 4), round(u[1] * sy, 4), round(u[2] * sx, 4), round(u[3] * sy, 4)]
        with open(m, "w") as fp:
            fp.write(json.dumps(j, ensure_ascii=False))
    im.crop((0, 0, nw, nh)).save(path)
    return (nw, nh)


def shrink_all():
    before = after = 0
    for p in sorted(glob.glob(os.path.join(ROOT, "textures/block/*.png"))):
        name = os.path.basename(p)[:-4]
        w, h = Image.open(p).size
        r = shrink(name)
        before += w * h
        after += (r[0] * r[1]) if r else w * h
    return before, after


if __name__ == "__main__":
    b, a = shrink_all()
    print(f"block textures {b:,} px -> {a:,} px")
