"""5.3.7 (data_v53): 루비화(100,000동) · 루비 배낭(162칸) — 그림 · 이름 · 설명 · 쪼개기 레시피."""
import json

from data_v2 import ASSETS, DATA, i, shapeless, write
from data_v18 import simple
from textures_v20 import hue_map, load, save

LANG_KO = {
    "item.wildplus.ruby_coin": "루비화",
    "tooltip.wildplus.ruby_coin": "100,000동 · 웅크리고 우클릭 또는 제작대에서 에메랄드화 10개로 쪼갬\n에메랄드화 10 → 루비화는 환전소에서",
    "item.wildplus.ruby_backpack": "루비 배낭",
    "tooltip.wildplus.ruby_backpack": "162칸 (18×9) · 불에 타지 않음\n에메랄드 배낭 창에서 900,000동으로 업그레이드",
    "gui.wildplus.backpack.tier5": "루비 배낭",
}
LANG_EN = {"item.wildplus.ruby_coin": "Ruby Coin", "item.wildplus.ruby_backpack": "Ruby Backpack", "gui.wildplus.backpack.tier5": "Ruby Backpack"}


def red(h, s, l):
    return 150 <= h <= 220 and s > 0.1


def build(lang_ko, lang_en):
    ko = {**lang_ko, **LANG_KO}
    sm = ko.get("tooltip.wildplus.sheet_music", "")
    ko["tooltip.wildplus.sheet_music"] = sm.split("\n웅크리고")[0] + "\n웅크리고 피아노를 우클릭하면 음 높이 조절 (옥타브 · 반음)"
    write(ASSETS / "lang/ko_kr.json", ko)
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    save(hue_map(load("item/diamond_coin.png"), red, 352, 0.8, 0.95), "item/ruby_coin.png")
    save(hue_map(load("item/diamond_backpack.png"), red, 352, 0.75), "item/ruby_backpack.png")
    simple("ruby_coin")
    simple("ruby_backpack")
    write(DATA / "recipe/emerald_coin_from_ruby.json", shapeless([i("ruby_coin")], i("emerald_coin"), 10))


if __name__ == "__main__":
    from data_v2 import ASSETS as _A
    build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
