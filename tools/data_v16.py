"""18차 데이터: 광물 사과 11종, 활 3종 · 특수 화살 4종."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, shaped, shapeless, write

APPLES = {
    "copper": ("구리 사과", "Copper Apple", "minecraft:copper_ingot", "신속 I · 성급함 I (1분)"),
    "iron": ("철 사과", "Iron Apple", "minecraft:iron_ingot", "저항 I · 흡수 I (1분)"),
    "redstone": ("레드스톤 사과", "Redstone Apple", "minecraft:redstone", "신속 II · 점프 강화 II (45초)"),
    "lapis": ("청금석 사과", "Lapis Apple", "minecraft:lapis_lazuli", "행운 · 수중 호흡 (3분) · 돌고래의 우아함"),
    "emerald": ("에메랄드 사과", "Emerald Apple", "minecraft:emerald", "마을의 영웅 II (거래 할인) · 행운 II (5분)"),
    "jade": ("비취 사과", "Jade Apple", i("jade"), "재생 I (1분) · 포만감 · 행운 (3분)"),
    "mythril": ("미스릴 사과", "Mythril Apple", i("mythril_ingot"), "성급함 II · 야간 투시 (5분)"),
    "diamond": ("다이아몬드 사과", "Diamond Apple", "minecraft:diamond", "저항 II (1분) · 흡수 II (2분) · 재생 II"),
    "ruby": ("루비 사과", "Ruby Apple", i("ruby"), "힘 II (1분) · 화염 저항 (5분) · 흡수"),
}
BOWS = {"mythril_bow": ("미스릴 활", "Mythril Bow", "당기는 속도 1.5배 · 화살 피해 1.2배"),
        "ruby_bow": ("루비 활", "Ruby Bow", "불화살 · 화살 피해 1.35배 · 당김 1.25배 · 불에 안 탐\n강화대에서 네더의 별 + 1,000동으로 스타 루비 활 승급"),
        "star_ruby_bow": ("스타 루비 활", "Star Ruby Bow", "화살 3발 동시 · 당김 2배 · 피해 1.6배 · 불화살\n맞으면 방어 무시 고정 피해 3 · 내구도 무한")}
ARROWS = {"explosive_arrow": ("폭발 화살", "Explosive Arrow", "맞은 곳이 폭발 (블록은 부서지지 않음)", "minecraft:tnt"),
          "homing_arrow": ("유도 화살", "Homing Arrow", "16칸 안의 가장 가까운 몬스터를 따라감", "minecraft:ender_eye"),
          "lightning_arrow": ("번개 화살", "Lightning Arrow", "맞은 곳에 번개가 떨어짐", "minecraft:lightning_rod"),
          "frost_arrow": ("서리 화살", "Frost Arrow", "맞은 적을 구속 III · 얼림 (5초)", "minecraft:blue_ice")}

LANG_KO, LANG_EN = {}, {}
for k, (ko, en, mat, tip) in APPLES.items():
    LANG_KO[f"item.wildplus.{k}_apple"] = ko
    LANG_EN[f"item.wildplus.{k}_apple"] = en
    LANG_KO[f"tooltip.wildplus.{k}_apple"] = tip
LANG_KO.update({"item.wildplus.netherite_apple": "네더라이트 사과", "item.wildplus.star_ruby_apple": "스타 루비 사과",
                "tooltip.wildplus.netherite_apple": "저항 III · 흡수 IV · 재생 II · 화염 저항",
                "tooltip.wildplus.star_ruby_apple": "힘 II · 저항 II · 재생 II · 신속 II (1분)\n흡수 IV (3분) · 화염 저항 · 야간 투시 (5분)"})
LANG_EN.update({"item.wildplus.netherite_apple": "Netherite Apple", "item.wildplus.star_ruby_apple": "Star Ruby Apple"})
for k, (ko, en, tip) in BOWS.items():
    LANG_KO[f"item.wildplus.{k}"], LANG_EN[f"item.wildplus.{k}"], LANG_KO[f"tooltip.wildplus.{k}"] = ko, en, tip
for k, (ko, en, tip, _) in ARROWS.items():
    LANG_KO[f"item.wildplus.{k}"], LANG_EN[f"item.wildplus.{k}"], LANG_KO[f"tooltip.wildplus.{k}"] = ko, en, tip


def model(name, parent="minecraft:item/generated", tex=None):
    write(ASSETS / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": i(f"item/{tex or name}")}})


def assets():
    for k in list(APPLES) + ["netherite", "star_ruby"]:
        model(f"{k}_apple")
        write(ASSETS / f"items/{k}_apple.json", {"model": {"type": "minecraft:model", "model": i(f"item/{k}_apple")}})
    for b in BOWS:
        model(b, "minecraft:item/bow")
        for n in range(3):
            model(f"{b}_pulling_{n}", "minecraft:item/bow")
        write(ASSETS / f"items/{b}.json", {"model": {
            "type": "minecraft:condition", "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": i(f"item/{b}")},
            "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                        "entries": [{"model": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_1")}, "threshold": 0.65},
                                    {"model": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_2")}, "threshold": 0.9}],
                        "fallback": {"type": "minecraft:model", "model": i(f"item/{b}_pulling_0")}}}})
    for a in ARROWS:
        model(a)
        write(ASSETS / f"items/{a}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{a}")}})


def data():
    rec = {}
    for k, (_, _, mat, _) in APPLES.items():
        rec[f"{k}_apple"] = shaped(["MMM", "MAM", "MMM"], {"M": mat, "A": "minecraft:apple"}, i(f"{k}_apple"), category="misc")
    rec["netherite_apple"] = shaped(["GNG", "NAN", "GNG"], {"G": "minecraft:gold_ingot", "N": "minecraft:netherite_ingot",
                                                             "A": "minecraft:apple"}, i("netherite_apple"), category="misc")
    rec["star_ruby_apple"] = shaped(["RRR", "RAR", "RSR"], {"R": i("ruby_block"), "A": i("ruby_apple"), "S": "minecraft:nether_star"},
                                    i("star_ruby_apple"), category="misc")
    rec["mythril_bow"] = shaped([" MS", "M S", " MS"], {"M": i("mythril_ingot"), "S": "minecraft:string"}, i("mythril_bow"))
    rec["ruby_bow"] = shaped([" MS", "M S", " MS"], {"M": i("ruby"), "S": "minecraft:string"}, i("ruby_bow"))
    for a, (_, _, _, mat) in ARROWS.items():
        rec[a] = shapeless(["minecraft:arrow"] * 4 + [mat], i(a), 4, category="equipment")
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags/item"
    merge_tag(t / "arrows.json", [i(a) for a in ARROWS])
    bows = [i(b) for b in BOWS]
    merge_tag(t / "enchantable/bow.json", bows)
    merge_tag(t / "enchantable/durability.json", bows)
    merge_tag(t / "enchantable/vanishing.json", bows)


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
