"""요리 2차: 요리 41종 · 조리 기구 6종의 레시피 표 하나로 Java(CookingData) · 언어 · 아이템 모델 · 아이콘 · 레시피를 만든다.
재료 표기: "minecraft:beef" · "wildplus:rice" · "@fish"(물고기 아무거나)."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/snolred/wildplus/cooking/CookingData.java"
ASSETS = ROOT / "src/main/resources/assets/wildplus"

STATIONS = {  # 이름, 기본 조리 초, 연료 필요
    "POT": ("가마솥", 12, True), "OVEN": ("화덕 오븐", 15, True), "GRILL": ("바비큐 그릴", 8, True),
    "STOVE": ("화구", 6, True), "PREP": ("도마 작업대", 3, False), "AGING": ("숙성통 선반", 120, False),
}
BLOCK = {"POT": "cooking_pot", "OVEN": "brick_oven", "GRILL": "bbq_grill", "STOVE": "kitchen_stove", "PREP": "prep_table", "AGING": "aging_rack"}

M, W = "minecraft:", "wildplus:"
BOWL, BOTTLE = M + "bowl", M + "glass_bottle"

# id: (한글 이름, 기구, 재료, 개수, 그릇(bowl/bottle/None), 효과[(효과, 초, 세기)], 아이콘(종류, 색들))
DISHES = {
    # ───── 가마솥 ─────
    "kimchi_stew": ("김치찌개", "POT", [W + "kimchi", M + "porkchop", W + "onion", BOWL], 1, "bowl",
                    [("strength", 180, 0)], ("bowl", (210, 60, 40), [(250, 250, 240), (90, 160, 60)])),
    "spicy_fish_stew": ("매운탕", "POT", ["@fish", "@fish", W + "chili", W + "onion", BOWL], 1, "bowl",
                        [("strength", 180, 0), ("resistance", 120, 0)], ("bowl", (230, 90, 30), [(250, 230, 220), (90, 160, 60)])),
    "seafood_stew": ("해물탕", "POT", [M + "cod", M + "salmon", M + "kelp", BOWL], 1, "bowl",
                     [("water_breathing", 300, 0), ("dolphins_grace", 120, 0)], ("bowl", (240, 120, 70), [(255, 170, 160), (60, 120, 60)])),
    "curry_rice": ("카레라이스", "POT", [W + "rice", M + "potato", M + "carrot", W + "onion", BOWL], 1, "bowl",
                   [("haste", 240, 0)], ("rice", (200, 140, 40), [(240, 130, 40)])),
    "ginseng_chicken": ("삼계탕", "POT", [M + "chicken", W + "ginseng", W + "rice", BOWL], 1, "bowl",
                        [("regeneration", 30, 1), ("health_boost", 300, 1)], ("bowl", (235, 225, 195), [(250, 240, 220), (200, 160, 110)])),
    "seaweed_soup": ("미역국", "POT", [M + "kelp", M + "kelp", M + "beef", BOWL], 1, "bowl",
                     [("regeneration", 45, 0)], ("bowl", (60, 80, 50), [(40, 100, 50), (150, 90, 70)])),
    "mushroom_cream_soup": ("버섯 크림 수프", "POT", [M + "brown_mushroom", M + "red_mushroom", M + "wheat", BOWL], 1, "bowl",
                            [("absorption", 120, 0)], ("bowl", (240, 230, 205), [(150, 110, 80), (200, 60, 50)])),
    "onion_soup": ("양파 수프", "POT", [W + "onion", W + "onion", M + "bread", BOWL], 1, "bowl",
                   [("fire_resistance", 300, 0)], ("bowl", (150, 90, 40), [(230, 190, 120)])),
    # ───── 화덕 오븐 ─────
    "cheese_pizza": ("치즈 피자", "OVEN", [M + "wheat", M + "wheat", W + "tomato", W + "cheese"], 1, None,
                     [("absorption", 120, 1)], ("pizza", (250, 210, 80), [(220, 50, 40)])),
    "apple_pie": ("애플파이", "OVEN", [M + "apple", M + "apple", M + "wheat", M + "sugar", M + "egg"], 1, None,
                  [("speed", 180, 0)], ("pie", (220, 160, 80), [(200, 40, 40)])),
    "strawberry_cake": ("딸기 케이크", "OVEN", [M + "wheat", M + "wheat", W + "strawberry", W + "strawberry", M + "sugar", M + "egg"], 1, None,
                        [("absorption", 180, 1), ("regeneration", 20, 0)], ("cake", (255, 240, 245), [(230, 40, 70)])),
    "grape_tart": ("포도 타르트", "OVEN", [W + "grape", W + "grape", M + "wheat", M + "sugar"], 1, None,
                   [("jump_boost", 180, 1)], ("tart", (210, 160, 90), [(120, 50, 160)])),
    "gratin": ("그라탕", "OVEN", [M + "potato", M + "potato", W + "cheese", W + "onion"], 1, None,
               [("resistance", 180, 0)], ("dish", (250, 200, 90), [(200, 120, 40)])),
    "baguette": ("바게트", "OVEN", [M + "wheat", M + "wheat", M + "wheat"], 2, None,
                 [], ("bread", (210, 150, 70), [(240, 200, 130)])),
    "fish_bread": ("붕어빵", "OVEN", [M + "wheat", M + "sugar", M + "sweet_berries", M + "egg"], 2, None,
                   [("speed", 120, 0)], ("fishbread", (220, 150, 60), [(120, 60, 50)])),
    "fish_pie": ("생선 파이", "OVEN", ["@fish", M + "wheat", M + "wheat", M + "egg"], 1, None,
                 [("water_breathing", 300, 0), ("luck", 180, 0)], ("pie", (220, 170, 90), [(240, 150, 140)])),
    # ───── 바비큐 그릴 ─────
    "steak": ("스테이크", "GRILL", [M + "beef", M + "beef", W + "onion"], 1, None,
              [("health_boost", 300, 0), ("strength", 120, 0)], ("plate", (130, 60, 35), [(90, 160, 60)])),
    "bulgogi": ("불고기", "GRILL", [M + "beef", W + "onion", M + "sugar", M + "carrot"], 1, None,
                [("strength", 240, 0)], ("plate", (120, 50, 30), [(240, 130, 40), (90, 160, 60)])),
    "pork_belly": ("삼겹살", "GRILL", [M + "porkchop", M + "porkchop", W + "cabbage", W + "onion"], 1, None,
                   [("strength", 120, 0), ("regeneration", 20, 0)], ("plate", (220, 150, 120), [(120, 190, 80)])),
    "grilled_fish_set": ("생선구이 정식", "GRILL", ["@fish", W + "rice", W + "cabbage"], 1, None,
                         [("luck", 300, 0)], ("set", (200, 140, 80), [(250, 250, 240), (120, 190, 80)])),
    "chicken_skewer": ("닭꼬치", "GRILL", [M + "chicken", M + "stick", W + "chili"], 2, None,
                       [("speed", 180, 0)], ("skewer", (200, 110, 50), [(220, 50, 40)])),
    "lamb_chop": ("양갈비", "GRILL", [M + "mutton", M + "mutton", W + "onion"], 1, None,
                  [("resistance", 180, 0)], ("plate", (150, 70, 50), [(250, 240, 220)])),
    "dragon_king_grill": ("용왕 구이", "GRILL", [W + "dragon_king_fish", W + "golden_carp", W + "ginseng"], 1, None,
                          [("regeneration", 120, 1), ("strength", 300, 1), ("resistance", 300, 1)], ("plate", (240, 180, 40), [(230, 60, 60), (255, 240, 150)])),
    # ───── 화구 ─────
    "fried_rice": ("볶음밥", "STOVE", [W + "rice", M + "egg", M + "carrot", W + "onion"], 1, None,
                   [("haste", 180, 0)], ("plate", (230, 190, 100), [(240, 130, 40), (90, 160, 60)])),
    "tteokbokki": ("떡볶이", "STOVE", [W + "rice", W + "rice", W + "chili", W + "chili", W + "cabbage"], 1, None,
                   [("strength", 120, 0), ("speed", 120, 0)], ("plate", (220, 50, 40), [(250, 240, 230)])),
    "seafood_pancake": ("해물파전", "STOVE", [M + "wheat", M + "egg", "@fish", W + "onion"], 1, None,
                        [("water_breathing", 300, 0), ("night_vision", 300, 0)], ("pancake", (220, 180, 90), [(90, 160, 60), (240, 150, 140)])),
    "omurice": ("오므라이스", "STOVE", [W + "rice", M + "egg", M + "egg", W + "tomato"], 1, None,
                [("health_boost", 240, 0)], ("plate", (250, 220, 80), [(220, 40, 40)])),
    "cream_pasta": ("크림 파스타", "STOVE", [M + "wheat", M + "wheat", W + "cheese", M + "brown_mushroom"], 1, None,
                    [("regeneration", 30, 0), ("absorption", 120, 0)], ("plate", (245, 235, 200), [(150, 110, 80)])),
    "eel_rice_bowl": ("장어덮밥", "STOVE", [W + "storm_eel", W + "rice", BOWL], 1, "bowl",
                      [("speed", 120, 1), ("jump_boost", 120, 0)], ("rice", (110, 60, 30), [(60, 40, 20)])),
    "rolled_omelette": ("계란말이", "STOVE", [M + "egg", M + "egg", M + "egg", W + "onion"], 1, None,
                        [("haste", 120, 0)], ("roll", (250, 220, 90), [(90, 160, 60)])),
    # ───── 도마 작업대 ─────
    "gimbap": ("김밥", "PREP", [W + "rice", W + "rice", M + "dried_kelp", M + "carrot", M + "egg"], 2, None,
               [("speed", 240, 0)], ("gimbap", (30, 40, 30), [(250, 250, 240), (240, 130, 40), (250, 220, 80)])),
    "sushi_set": ("초밥 세트", "PREP", ["@fish", "@fish", W + "rice", W + "rice"], 1, None,
                  [("luck", 300, 0), ("water_breathing", 180, 0)], ("sushi", (250, 250, 240), [(250, 130, 100), (240, 170, 160)])),
    "hoe_rice_bowl": ("회덮밥", "PREP", ["@fish", W + "rice", W + "cabbage", W + "chili", BOWL], 1, "bowl",
                      [("haste", 180, 0), ("luck", 180, 0)], ("rice", (250, 250, 240), [(250, 140, 120), (120, 190, 80), (220, 50, 40)])),
    "bibimbap": ("비빔밥", "PREP", [W + "rice", M + "carrot", W + "cabbage", M + "egg", W + "chili", BOWL], 1, "bowl",
                 [("haste", 240, 0), ("regeneration", 30, 0)], ("rice", (250, 250, 240), [(240, 130, 40), (120, 190, 80), (250, 220, 80), (220, 50, 40)])),
    "fruit_salad": ("과일 샐러드", "PREP", [M + "apple", W + "strawberry", W + "grape", W + "blueberry", BOWL], 1, "bowl",
                    [("regeneration", 40, 0)], ("bowl", (250, 245, 230), [(220, 40, 60), (120, 50, 160), (60, 80, 180), (200, 40, 40)])),
    "kimchi": ("김치", "PREP", [W + "cabbage", W + "cabbage", W + "chili", W + "onion"], 2, None,
               [], ("jar", (220, 60, 40), [(240, 220, 200)])),
    # ───── 숙성통 선반 ─────
    "cheese": ("치즈", "AGING", [M + "milk_bucket"], 3, None,
               [], ("cheese", (250, 210, 80), [(230, 180, 50)])),
    "wine": ("와인", "AGING", [W + "grape", W + "grape", W + "grape", W + "grape", BOTTLE], 1, "bottle",
             [("resistance", 300, 0), ("strength", 300, 0)], ("bottle", (120, 20, 50), [])),
    "beer": ("맥주", "AGING", [M + "wheat", M + "wheat", M + "wheat", BOTTLE], 1, "bottle",
             [("haste", 180, 1)], ("bottle", (230, 170, 40), [(250, 250, 240)])),
    "makgeolli": ("막걸리", "AGING", [W + "rice", W + "rice", W + "rice", BOTTLE], 1, "bottle",
                  [("regeneration", 60, 0), ("absorption", 300, 0)], ("bottle", (240, 236, 220), [])),
    "ginseng_wine": ("인삼주", "AGING", [W + "ginseng", M + "sugar", BOTTLE], 1, "bottle",
                     [("health_boost", 600, 1), ("regeneration", 60, 0)], ("bottle", (220, 180, 90), [(200, 150, 90)])),
}
INGREDIENT_ONLY = {"kimchi", "cheese"}  # 먹을 수는 있지만 주로 재료


def java_str(s):
    return '"' + s.replace('"', '\\"') + '"'


def write_java():
    lines = []
    for did, (ko, st, ings, cnt, cont, eff, _) in DISHES.items():
        ings_j = ", ".join(java_str(x) for x in ings)
        eff_j = ", ".join(f"new Eff({java_str(e)}, {s}, {a})" for e, s, a in eff)
        lines.append(f"        new Dish({java_str(did)}, {java_str(ko)}, Station.{st}, new String[]{{{ings_j}}}, {cnt}, "
                     f"{java_str(cont or '')}, new Eff[]{{{eff_j}}}, {len(ings) + 4}, 0.{6 + min(3, len(eff))}F)")
    src = f'''package com.snolred.wildplus.cooking;

/**
 * 요리 2차 레시피 표 (tools/cooking_gen.py 가 만든 파일 — 직접 고치지 말 것).
 * 재료: "minecraft:…" · "wildplus:…" · "@fish"(물고기 아무거나). 그릇: bowl / bottle / 없음.
 */
public final class CookingData {{
    private CookingData() {{}}

    public enum Station {{
        POT("가마솥", 12, true), OVEN("화덕 오븐", 15, true), GRILL("바비큐 그릴", 8, true),
        STOVE("화구", 6, true), PREP("도마 작업대", 3, false), AGING("숙성통 선반", 120, false);

        public final String ko;
        public final int seconds;
        public final boolean fuel;

        Station(String ko, int seconds, boolean fuel) {{
            this.ko = ko;
            this.seconds = seconds;
            this.fuel = fuel;
        }}
    }}

    public record Eff(String id, int seconds, int amp) {{}}

    public record Dish(String id, String ko, Station station, String[] ingredients, int count, String container, Eff[] effects,
                       int nutrition, float saturation) {{}}

    public static final Dish[] DISHES = {{
{(","+chr(10)).join(lines)}
    }};
}}
'''
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(src, encoding="utf-8")


# ───────── 아이콘 ─────────
def icon(kind, base, tops, seed):
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = tuple(int(v) for v in c[:3]) + (255,)

    def shade(c, k):
        return tuple(max(0, min(255, int(v * k))) for v in c)

    def bowl_shape(top_y=7, color=(150, 100, 60)):
        for y in range(top_y, 14):
            w = 7 - max(0, (y - top_y) - 3)
            for x in range(8 - w, 8 + w):
                put(x, y, shade(color, 1.1 if x < 8 else 0.85))
        for x in range(1, 15):
            put(x, top_y - 1, shade(color, 1.25))

    def dots(y0, y1, x0, x1, n):
        for k in range(n):
            put(r.randint(x0, x1), r.randint(y0, y1), tops[k % len(tops)] if tops else base)

    if kind in ("bowl", "rice"):
        bowl_shape(8, (235, 235, 230) if kind == "rice" else (150, 100, 60))
        for x in range(2, 14):
            for y in range(5, 8):
                if abs(x - 7.5) <= 6 - (7 - y):
                    put(x, y, shade((250, 250, 240) if kind == "rice" else base, 1.0 + 0.08 * ((x + y) % 2)))
        if kind == "rice":
            for x in range(4, 12):
                put(x, 5, shade(base, 1.0))
                put(x, 6, shade(base, 0.9))
        dots(4, 7, 3, 12, 7)
    elif kind in ("plate", "set", "pancake", "dish"):
        for x in range(1, 15):
            for y in range(9, 13):
                if (x - 7.5) ** 2 / 49 + (y - 11) ** 2 / 4 <= 1:
                    put(x, y, (240, 240, 236) if kind != "dish" else (200, 90, 40))
        for x in range(3, 13):
            for y in range(6, 11):
                if (x - 7.5) ** 2 / 25 + (y - 8.5) ** 2 / 5 <= 1:
                    put(x, y, shade(base, 1.1 - 0.05 * (y - 6)))
        dots(6, 9, 4, 11, 6)
        if kind == "set":
            for x in range(10, 14):
                for y in range(5, 8):
                    put(x, y, (250, 250, 240))
    elif kind in ("pie", "tart", "pizza"):
        for x in range(1, 15):
            for y in range(7, 13):
                if (x - 7.5) ** 2 / 49 + (y - 10) ** 2 / 9 <= 1:
                    edge = (x - 7.5) ** 2 / 49 + (y - 10) ** 2 / 9 > 0.6
                    put(x, y, shade(base, 0.85) if edge else (tops[0] if kind != "pie" else shade(base, 1.1)))
        if kind == "pie":
            for x in range(3, 13, 3):
                put(x, 9, shade(base, 0.7))
        dots(8, 11, 4, 11, 8 if kind == "pizza" else 5)
    elif kind == "cake":
        for y in range(6, 14):
            for x in range(3, 3 + (y - 4)):
                put(x, y, base if y < 8 or y in (10, 11) else (240, 200, 130))
        for x in range(3, 8):
            put(x, 5, tops[0])
        put(5, 4, tops[0])
    elif kind in ("bread", "fishbread", "roll", "cheese"):
        for x in range(2, 14):
            for y in range(6, 12):
                if (x - 7.5) ** 2 / 36 + (y - 9) ** 2 / 9 <= 1:
                    put(x, y, shade(base, 1.1 - 0.06 * (y - 6)))
        if kind == "bread":
            for x in (5, 8, 11):
                put(x, 7, tops[0])
        if kind == "fishbread":
            put(13, 7, base); put(13, 11, base); put(4, 8, (40, 30, 20))
        if kind == "roll":
            for x in (5, 8, 11):
                for y in range(7, 11):
                    put(x, y, shade(base, 0.85))
        if kind == "cheese":
            for (x, y) in [(5, 8), (9, 9), (7, 10)]:
                put(x, y, tops[0])
    elif kind in ("gimbap", "sushi"):
        for k, cx in enumerate((4, 9, 13) if kind == "gimbap" else (4, 10)):
            for x in range(cx - 2, cx + 3):
                for y in range(7, 12):
                    if kind == "gimbap":
                        d = (x - cx) ** 2 + (y - 9) ** 2
                        put(x, y, base if d >= 4 else (tops[1] if d == 0 else tops[0]))
                    else:
                        put(x, y, (250, 250, 240) if y > 9 else tops[k % len(tops)])
    elif kind == "skewer":
        for x in range(1, 15):
            put(x, 15 - x, (200, 180, 140))
        for k in range(4):
            cx, cy = 4 + k * 3, 11 - k * 3
            for x in range(cx - 1, cx + 2):
                for y in range(cy - 1, cy + 2):
                    put(x, y, base if k % 2 == 0 else tops[0])
    elif kind == "jar":
        for x in range(4, 12):
            for y in range(4, 14):
                put(x, y, (220, 235, 245) if x in (4, 11) or y == 13 else base)
        for x in range(4, 12):
            put(x, 3, (120, 90, 60))
        dots(6, 12, 5, 10, 5)
    elif kind == "bottle":
        for x in range(5, 11):
            for y in range(6, 15):
                put(x, y, shade(base, 1.0 + 0.1 * (x == 6)))
        for y in range(2, 6):
            put(7, y, (200, 230, 240)); put(8, y, (180, 210, 220))
        put(7, 1, (140, 100, 60)); put(8, 1, (140, 100, 60))
        for y in range(9, 12):
            for x in range(6, 10):
                put(x, y, (240, 230, 200))
    return im


def write_icons():
    for k, (did, (_, _, _, _, _, _, (kind, base, tops))) in enumerate(DISHES.items()):
        icon(kind, base, tops, k).save(ASSETS / f"textures/item/{did}.png")


def lang():
    ko, en = {}, {}
    for did, (name, st, ings, cnt, cont, eff, _) in DISHES.items():
        ko[f"item.wildplus.{did}"] = name
        en[f"item.wildplus.{did}"] = did.replace("_", " ").title()
        fx = {"strength": "힘", "resistance": "저항", "water_breathing": "수중 호흡", "dolphins_grace": "돌고래의 가호", "haste": "성급함",
              "regeneration": "재생", "health_boost": "체력 증가", "absorption": "흡수", "fire_resistance": "화염 저항", "speed": "신속",
              "jump_boost": "점프 강화", "luck": "행운", "night_vision": "야간 투시"}
        roman = ["I", "II", "III"]
        parts = [f"{fx[e]} {roman[a]} {s // 60}분" if s >= 60 else f"{fx[e]} {roman[a]} {s}초" for e, s, a in eff]
        how = f"{STATIONS[st][0]}에서 조리"
        ko[f"tooltip.wildplus.{did}"] = ((" · ".join(parts) + "\n") if parts else "") + how + " · 재료 등급이 높으면 요리 등급↑ (효과 시간 · 가격↑)"
    for key, (name, _, _) in STATIONS.items():
        b = BLOCK[key]
        ko[f"block.wildplus.{b}"] = name
        en[f"block.wildplus.{b}"] = b.replace("_", " ").title()
        dishes = [v[0] for v in DISHES.values() if v[1] == key]
        ko[f"tooltip.wildplus.{b}"] = (f"여러 칸 조리 기구 · 재료를 넣으면 자동으로 조리" + (" (연료 필요: 석탄·숯·원목·판자)" if STATIONS[key][2] else "")
                                       + "\n" + " · ".join(dishes))
    return ko, en


def build():
    write_java()
    write_icons()
    return lang()


if __name__ == "__main__":
    build()
    print(len(DISHES), "dishes")
