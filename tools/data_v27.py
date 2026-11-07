"""3.7 데이터: 물가 3배 — 설명 글의 가격 숫자 맞추기."""
from data_v2 import ASSETS, write

FIX = [("+ 1,000동으로", "+ 3,000동으로"), ("+ 1,000동 →", "+ 3,000동 →"), ("생명의 결정 + 1,000동", "생명의 결정 + 3,000동")]


def build(lang_ko, lang_en):
    out = {}
    for k, v in lang_ko.items():
        if not isinstance(v, str):
            continue
        nv = v
        for a, b in FIX:
            nv = nv.replace(a, b)
        if nv != v:
            out[k] = nv
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **out})
