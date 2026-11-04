"""3.4 데이터: 움직이는 텍스처 (가구 · 조리 기구 작동 중 · 기존 블록) · 목장 · 집들이 · 주말 대회 · 부동산 중개소."""
import json

import anim_textures
import kitchen_design as K
import kitchen_models
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, shapeless, write

ROOT = kitchen_models.ROOT
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
# 조리 기구: 조리 중(lit=true)이면 불이 일렁이고 국물이 끓는 모델
ON_STATIONS = {"cooking_pot", "brick_oven", "bbq_grill", "kitchen_stove"}


def export_stations():
    for name, cfg in kitchen_models.STATIONS.items():
        K.ON = False
        kitchen_models.export(name, *cfg)
        if name in ON_STATIONS:
            K.ON = True
            kitchen_models.export(name + "_on", *cfg)
            (ROOT / f"blockstates/{name}_on.json").unlink(missing_ok=True)
            (ROOT / f"items/{name}_on.json").unlink(missing_ok=True)
            K.ON = False
        variants = {}
        for f, r in ROT.items():
            for lit in ("false", "true"):
                m = f"wildplus:block/{name}" + ("_on" if lit == "true" and name in ON_STATIONS else "")
                variants[f"facing={f},lit={lit}"] = {"model": m, "y": r} if r else {"model": m}
        (ROOT / f"blockstates/{name}.json").write_text(json.dumps({"variants": variants}, indent=1))


def build(lang_ko, lang_en):
    ko, en = {}, {}
    export_stations()
    anim_textures.build()
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})
