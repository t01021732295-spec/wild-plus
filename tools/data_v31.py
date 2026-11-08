"""3.9.8 데이터 (data_v31): 스카이 사파이어 검 · 활 빙결 폭발 확정 (툴팁)."""
import json

from data_v2 import ASSETS, write

LANG_KO = {
    "tooltip.wildplus.sky_sapphire_sword": "공격력 8 · 공격 속도 2.0 · 내구도 무한\n빙결 폭발 확정 (주변 3칸 피해 6 · 정지 1.5초 · 구속 III · 동상 4초)\n실명 3초 + 급강하 일격 (공중에서 때리면 1.5배)",
    "tooltip.wildplus.sky_sapphire_bow": "화살이 1.35배 빠르게 날아감 · 당김 1.75배 · 피해 1.5배 · 내구도 무한\n빙결 폭발 확정 (주변 3칸 피해 6 · 정지 0.75초 · 구속 III · 동상 2.5초)",
}
LANG_EN = {"tooltip.wildplus.sky_sapphire_bow": "Faster arrows, guaranteed frost burst, unbreakable"}


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v31")
