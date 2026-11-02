"""동물 탈 도안(cosmetic_v3.HATS)을 게임 모델 데이터(HatModels.java)로 옮긴다.
도안 좌표(y 위 · z 앞, 정수리 y=32) → 머리 모델 좌표(y 아래 · -z 앞, 목 y=0)."""
from pathlib import Path

from cosmetic_v3 import HATS

OUT = Path(__file__).resolve().parent.parent / "src/main/java/com/snolred/wildplus/client/HatModels.java"


def f(v):
    return f"{v:.3f}F"


def export():
    hats = []
    for _, fn in HATS:
        rows = []
        for b in fn():
            if b.y0 + b.h <= 24:  # 머리 아래 부품은 탈이 아님
                continue
            c = b.paint("front", 1, 0, 4, 4)
            col = (c[0] << 16) | (c[1] << 8) | c[2]
            x0, y0, z0 = b.x0, -(b.y0 + b.h - 24), -(b.z0 + b.d)
            px = py = pz = 0.0
            rz = 0.0
            if b.rot:
                axis, ang, piv = b.rot
                assert axis == "z"
                px, py, pz = piv[0], -(piv[1] - 24), -piv[2]
                rz = -ang
            rows.append(f"            {{{col}, {f(x0 - px)}, {f(y0 - py)}, {f(z0 - pz)}, {f(b.w)}, {f(b.h)}, {f(b.d)}, {f(b.grow)}, "
                        f"{f(px)}, {f(py)}, {f(pz)}, {f(rz)}}}")
        hats.append("        {\n" + ",\n".join(rows) + "\n        }")
    names = ", ".join(f'"{n}"' for n, _ in HATS)
    OUT.write_text(f"""package com.snolred.wildplus.client;

/**
 * 동물 탈 모델 (tools/export_hats.py 가 cosmetic_v3 도안에서 만든 파일 — 직접 고치지 말 것).
 * 부품 하나 = {{색(RGB), x, y, z, 너비, 높이, 깊이, 부풀림, 기준점 x, y, z, z축 회전}} — 머리 모델 좌표.
 */
public final class HatModels {{
    private HatModels() {{}}

    public static final String[] NAMES = {{{names}}};

    public static final float[][][] PARTS = {{
{",\n".join(hats)}
    }};
}}
""", encoding="utf-8")
    return len(hats)


if __name__ == "__main__":
    print(export())
