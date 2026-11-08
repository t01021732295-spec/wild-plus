"""3.9.1 데이터 (data_v30): 우민 감옥 확대 (3×2×2) · 땅 경계 기둥 (주인 색 8가지) · 언어."""
import json

import kitchen_models
from data_v2 import ASSETS, i, write

# 주인 색 (LandPostBlock.colorOf 순서)
POST_COLORS = [(70, 170, 90), (70, 120, 220), (220, 70, 70), (240, 190, 50), (160, 80, 210), (40, 190, 190), (240, 130, 40), (235, 110, 170)]

LANG_KO = {"block.wildplus.land_post": "땅 경계 기둥", "entity.wildplus.illager_prisoner": "우민 포로", "affix.wildplus.cataclysm": "대재앙"}
LANG_EN = {"block.wildplus.land_post": "Land Post", "entity.wildplus.illager_prisoner": "Illager Prisoner", "affix.wildplus.cataclysm": "Cataclysm"}


def build(lang_ko, lang_en):
    import omen_design as O
    import land_design as L
    kitchen_models.export("illager_prison", O.prison_big, -1, 1, 0, 1, 2)
    for n, col in enumerate(POST_COLORS):
        kitchen_models.export(f"land_post_{n}", lambda col=col: L.land_post(col), 0, 0, 0, 0, 2)
        # export 는 상자 전체를 가운데에 놓으므로 깃발 때문에 기둥이 왼쪽으로 쏠림 → 기둥을 칸 가운데로 (+4)
        mp = ASSETS / f"models/block/land_post_{n}.json"
        m = json.loads(mp.read_text())
        for el in m["elements"]:
            el["from"][0] = round(el["from"][0] + 4, 3)
            el["to"][0] = round(el["to"][0] + 4, 3)
            if "rotation" in el:
                el["rotation"]["origin"][0] = round(el["rotation"]["origin"][0] + 4, 3)
        mp.write_text(json.dumps(m, ensure_ascii=False))
        # export 가 만든 방향별 블록상태 · 아이템 정의는 쓰지 않음 (기둥은 색 하나짜리 블록상태)
        (ASSETS / f"blockstates/land_post_{n}.json").unlink(missing_ok=True)
        (ASSETS / f"items/land_post_{n}.json").unlink(missing_ok=True)
    write(ASSETS / "blockstates/land_post.json", {"variants": {f"color={n}": {"model": f"wildplus:block/land_post_{n}"} for n in range(8)}})
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v30")
