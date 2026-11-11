"""5.0 생활 데이터 (data_v43): 계절 작물 4종 · 꿀/말린 것/장 · 시설 4종(양봉 상자 · 건조대 · 장독대 · 양어장) · 새 요리 10종."""
import json

from PIL import Image

import cooking_gen
import kitchen_models
import life_design
from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, shaped, shapeless, write
from data_v8 import seed_crop_loot
from data_v18 import simple
from textures_v20 import green, hue_map, load, save, sprite

# 작물: (한글, 씨앗 한글, 계절, 열매 색상(hue, sat, light), 수확 개수)
CROPS = {"soybean": ("콩", "콩 씨앗", "봄", (62, 0.5, 1.15), (2, 4)), "korean_melon": ("참외", "참외 씨앗", "여름", (52, 0.9, 1.15), (1, 3)),
         "sweet_potato": ("고구마", "고구마 순", "가을", (335, 0.45, 0.85), (2, 4)), "radish": ("무", "무 씨앗", "겨울", (95, 0.15, 1.5), (1, 3))}
GOODS = {"honey_jar": "꿀단지", "royal_jelly": "로열젤리", "chili_powder": "고춧가루", "raisin": "건포도", "dried_fish": "건어물", "jerky": "육포",
         "kkakdugi": "깍두기", "danmuji": "단무지", "doenjang": "된장", "soy_sauce": "간장"}
BLOCKS = {"beehive_box": ("양봉 상자", "주변(5칸)에 꽃이 많을수록 꿀이 빨리 차요 (최대 16)\n빈손으로 눌러 꿀단지 · 벌집 · 가끔 로열젤리\n겨울에는 절반 속도"),
          "drying_rack": ("건조대", "재료를 들고 눌러 널기 (한 번에 16개) → 빈손으로 걷기\n고추 → 고춧가루 · 포도 → 건포도 · 고기 → 육포 · 물고기 → 건어물"),
          "jangdok": ("장독대", "재료를 들고 눌러 담그기 (한 번에 16개) → 빈손으로 꺼내기\n무 → 깍두기 · 양배추 → 단무지 (하루)\n콩 → 된장 (이틀) · 된장 → 간장 (하루)"),
          "fish_farm": ("양어장", "같은 물고기 2마리 이상을 넣으면 하루에 1마리씩 늘어요 (최대 +16)\n빈손: 새끼 건지기 · 웅크리고: 전부 꺼내기\n전설의 물고기는 안 돼요")}

KO = {"tooltip.wildplus.honey_jar": "양봉 상자에서 얻는 꿀 · 요리 재료", "tooltip.wildplus.royal_jelly": "아주 가끔 나오는 귀한 꿀 · 비싸게 팔려요",
      "tooltip.wildplus.doenjang": "장독대에서 콩으로 담근 장 · 한 번 더 익히면 간장"}
EN = {}

JAR = ["................", "................", ".....llllll.....", "....lddddddl....", ".....aaaaaa.....", "....abbbbbba....",
       "...abbccbbbba...", "...abcbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...", "....abbbbbba....",
       ".....aaaaaa.....", "................", "................", "................"]
PILE = ["................", "................", "................", "................", "................", ".......b........",
        "......bcb.......", ".....bbbcb......", "....abbbbbb.....", "...abbcbbbba....", "..aabbbbbbbaa...", "..ddddddddddd...",
        "...ddddddddd....", "................", "................", "................"]
SOY = ["................", "................", "......g.........", ".....aag........", "....abbaa.......", "....abcbba......",
       ".....abbcba.....", "......abbbba....", ".......abcba....", "........abba....", ".........aa.....", "................",
       "................", "................", "................", "................"]
MELON = ["................", "........g.......", ".......gg.......", ".....aaaaaa.....", "....abcbcbba....", "...abbcbcbbba...",
         "...abbcbcbbba...", "...abbcbcbbba...", "...abbcbcbbba...", "...abbcbcbbba...", "....abcbcbba....", ".....aaaaaa.....",
         "................", "................", "................", "................"]
SWEET = ["................", "................", "...........a....", ".........aab....", ".......aabbba...", "......abbcbba...",
         ".....abbcbbba...", "....abbbbbba....", "...abbcbbbba....", "...abbbbbba.....", "..abbbbaa.......", "..abaa..........",
         ".a..............", "................", "................", "................"]
RADISH = ["................", "....g..g.g......", ".....g.gg.......", "......ggg.......", "......lll.......", ".....abbba......",
          ".....abcba......", ".....abbba......", ".....abcba......", ".....abbba......", "......abba......", "......aba.......",
          ".......a........", ".......a........", "................", "................"]
FISH = ["................", "................", "..........aa....", ".a......aabba...", ".ba...aabbbbba..", ".bbaaabbbcbbdba.",
        ".bbaaabbbbbbbba.", ".ba...aabbbbba..", ".a......aabba...", "..........aa....", "................", "......e...e.....",
        ".....e...e......", "................", "................", "................"]
JERKY = ["................", "................", "..aaaaaaaaa.....", ".abbbcbbbbba....", ".abcbbbbcbbba...", ".abbbbcbbbbba...",
         "..abbcbbbbcba...", "..abbbbbcbbba...", "...abcbbbbbbba..", "...abbbbcbbbba..", "....aaaaaaaaa...", "................",
         "................", "................", "................", "................"]


def textures():
    for name, (_, _, _, (hue, sat, light), _) in CROPS.items():
        for st in range(4):
            im = load(f"block/tomato_crop_stage{st}.png")
            if name == "radish":
                im = hue_map(im, green, 100, 0.55, 1.05)
            if name == "sweet_potato":
                im = hue_map(im, green, 125, 0.45, 0.9)
            save(hue_map(im, lambda h, s, l: (h < 25 or h > 330) and s > 0.3, hue, sat, light), f"block/{name}_crop_stage{st}.png")
        save(hue_map(load("item/tomato_seeds.png"), lambda h, s, l: s > 0.1, hue, min(0.6, sat) * 0.8, 0.95), f"item/{name}_seeds.png")
    save(sprite(SOY, {"a": (150, 150, 60), "b": (214, 206, 110), "c": (240, 236, 160), "g": (80, 150, 60)}), "item/soybean.png")
    save(sprite(MELON, {"a": (200, 150, 20), "b": (250, 210, 50), "c": (255, 250, 220), "g": (90, 150, 60)}), "item/korean_melon.png")
    save(sprite(SWEET, {"a": (110, 40, 70), "b": (170, 70, 100), "c": (210, 110, 130)}), "item/sweet_potato.png")
    save(sprite(RADISH, {"a": (200, 200, 190), "b": (245, 245, 238), "c": (225, 228, 220), "l": (170, 210, 120), "g": (80, 160, 60)}), "item/radish.png")
    lid = {"l": (150, 110, 70), "d": (110, 78, 48)}
    save(sprite(JAR, {"a": (190, 120, 20), "b": (245, 180, 50), "c": (255, 232, 150), **lid}), "item/honey_jar.png")
    save(sprite(JAR, {"a": (200, 170, 90), "b": (252, 240, 190), "c": (255, 255, 240), "l": (240, 200, 90), "d": (200, 150, 50)}), "item/royal_jelly.png")
    save(sprite(JAR, {"a": (150, 40, 30), "b": (214, 70, 50), "c": (250, 240, 225), **lid}), "item/kkakdugi.png")
    save(sprite(JAR, {"a": (190, 150, 20), "b": (250, 215, 50), "c": (255, 245, 170), **lid}), "item/danmuji.png")
    save(sprite(JAR, {"a": (96, 60, 30), "b": (156, 106, 56), "c": (196, 150, 90), "l": (112, 66, 46), "d": (78, 44, 32)}), "item/doenjang.png")
    save(sprite(JAR, {"a": (30, 18, 14), "b": (62, 36, 24), "c": (120, 80, 60), "l": (112, 66, 46), "d": (78, 44, 32)}), "item/soy_sauce.png")
    save(sprite(PILE, {"a": (150, 24, 20), "b": (214, 50, 36), "c": (250, 110, 70), "d": (230, 224, 210)}), "item/chili_powder.png")
    save(sprite(PILE, {"a": (60, 26, 80), "b": (104, 52, 124), "c": (150, 96, 170), "d": (230, 224, 210)}), "item/raisin.png")
    save(sprite(FISH, {"a": (130, 110, 80), "b": (196, 170, 120), "c": (230, 210, 160), "d": (40, 30, 20), "e": (214, 190, 140)}), "item/dried_fish.png")
    save(sprite(JERKY, {"a": (80, 30, 24), "b": (130, 56, 40), "c": (170, 90, 60)}), "item/jerky.png")


def farm_shifted():
    return life_design.fish_farm()


def build(lang_ko, lang_en):
    ko, en = cooking_gen.build()
    for d in cooking_gen.DISHES:
        simple(d)
    textures()
    stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    seeds_pool = []
    for c, (name, seed, season, _, (lo, hi)) in CROPS.items():
        simple(c)
        simple(f"{c}_seeds")
        crop = f"{c}_crop"
        write(ASSETS / f"blockstates/{crop}.json", {"variants": {f"age={a}": {"model": i(f"block/{crop}_stage{s}")} for a, s in stage.items()}})
        for s in range(4):
            write(ASSETS / f"models/block/{crop}_stage{s}.json", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout", "textures": {"crop": i(f"block/{crop}_stage{s}")}})
        write(DATA / f"loot_table/blocks/{crop}.json", seed_crop_loot(crop, c, f"{c}_seeds", lo, hi))
        write(DATA / f"recipe/{c}_seeds.json", shapeless([i(c)], i(f"{c}_seeds"), 2))
        KO[f"item.wildplus.{c}"] = name
        KO[f"item.wildplus.{c}_seeds"] = seed
        KO[f"block.wildplus.{crop}"] = name
        KO[f"tooltip.wildplus.{c}_seeds"] = f"{season} 작물 · 제철에는 2배 빨리 자라고, 다른 계절에는 아주 느려요\n잔디에서 가끔 나옴 · /season 으로 계절 확인"
        EN[f"item.wildplus.{c}"] = c.replace("_", " ").title()
        EN[f"item.wildplus.{c}_seeds"] = c.replace("_", " ").title() + " Seeds"
        seeds_pool.append({"type": "minecraft:item", "name": i(f"{c}_seeds"), "weight": 2})
    # 잔디 씨앗 표에 추가 (이미 있으면 건너뜀)
    gp = DATA / "loot_table/inject/grass_seeds.json"
    g = json.loads(gp.read_text())
    have = {e["name"] for e in g["pools"][0]["entries"]}
    g["pools"][0]["entries"] += [e for e in seeds_pool if e["name"] not in have]
    write(gp, g)
    for n, name in GOODS.items():
        simple(n)
        KO[f"item.wildplus.{n}"] = name
        EN[f"item.wildplus.{n}"] = n.replace("_", " ").title()
    kitchen_models.export("beehive_box", life_design.beehive, 0, 0, 0, 0, 2)
    kitchen_models.export("drying_rack", life_design.drying_rack, 0, 0, 0, 0, 2)
    kitchen_models.export("jangdok", life_design.jangdok, 0, 0, 0, 0, 1)
    kitchen_models.export("fish_farm", farm_shifted, 0, 1, 0, 0, 2)
    for b, (name, tip) in BLOCKS.items():
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
        KO[f"block.wildplus.{b}"] = name
        KO[f"tooltip.wildplus.{b}"] = tip
        EN[f"block.wildplus.{b}"] = b.replace("_", " ").title()
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/axe.json", [i("beehive_box"), i("drying_rack"), i("fish_farm")])
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/pickaxe.json", [i("jangdok")])
    merge_tag(DATA.parent / "minecraft/tags/block/crops.json", [i(f"{c}_crop") for c in CROPS])
    rec = {
        "beehive_box": shaped(["PPP", "HHH", "PPP"], {"P": "#minecraft:planks", "H": "minecraft:honeycomb"}, i("beehive_box"), category="misc"),
        "drying_rack": shaped(["SSS", "S S", "L L"], {"S": "minecraft:stick", "L": "#minecraft:logs"}, i("drying_rack"), category="misc"),
        "jangdok": shaped(["T T", "TCT", "SSS"], {"T": "minecraft:terracotta", "C": "minecraft:clay_ball", "S": "minecraft:stone"}, i("jangdok"), category="misc"),
        "fish_farm": shaped(["P P", "PBP", "LLL"], {"P": "#minecraft:planks", "B": "minecraft:water_bucket", "L": "#minecraft:logs"}, i("fish_farm"), category="misc"),
    }
    for n, r in rec.items():
        write(DATA / f"recipe/{n}.json", r)
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en, **EN})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v43")
