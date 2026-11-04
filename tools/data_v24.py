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


def export_ore_miner():
    """광석 채굴기 3D (멈춤 · 작동 중)"""
    import machine_design as M
    M.ON = False
    kitchen_models.export("ore_miner", M.ore_miner, 0, 0, 0, 0, 2)
    M.ON = True
    kitchen_models.export("ore_miner_on", M.ore_miner, 0, 0, 0, 0, 2)
    M.ON = False
    (ROOT / "blockstates/ore_miner_on.json").unlink(missing_ok=True)
    (ROOT / "items/ore_miner_on.json").unlink(missing_ok=True)
    (ROOT / "blockstates/ore_miner.json").write_text(json.dumps({"variants": {
        "lit=false": {"model": "wildplus:block/ore_miner"}, "lit=true": {"model": "wildplus:block/ore_miner_on"}}}, indent=1))


def export_tv():
    """TV 채널 5개 (켜짐) + 꺼짐: facing × lit × channel"""
    import furniture_design as F
    l0, l1, d0, d1, h = 0, 1, 0, 0, 2
    for ch in range(1, 5):
        F.TV_CH, F.LIT = ch, True
        kitchen_models.export(f"tv_stand_ch{ch}", F.tv_stand, l0, l1, d0, d1, h)
        (ROOT / f"blockstates/tv_stand_ch{ch}.json").unlink(missing_ok=True)
        (ROOT / f"items/tv_stand_ch{ch}.json").unlink(missing_ok=True)
    F.TV_CH = 0
    variants = {}
    for f, r in ROT.items():
        for lit in ("true", "false"):
            for ch in range(5):
                m = "wildplus:block/tv_stand" + ("_off" if lit == "false" else ("" if ch == 0 else f"_ch{ch}"))
                variants[f"channel={ch},facing={f},lit={lit}"] = {"model": m, "y": r} if r else {"model": m}
    (ROOT / "blockstates/tv_stand.json").write_text(json.dumps({"variants": variants}, indent=1))


def build(lang_ko, lang_en):
    ko, en = {}, {}
    export_tv()
    ko["tooltip.wildplus.piano"] = "가로 2칸 · 우클릭: 건반 화면 — 마우스나 키보드로 직접 연주\n악기 7가지 (피아노 · 벨 · 플루트 · 기타 · 베이스 · 실로폰 · 8비트) · '작은 별' 자동 연주 · 근처 친구들도 들어요"
    ko["tooltip.wildplus.tv_stand"] = "가로 2칸 · 우클릭: 켜기 → 누를 때마다 채널 (자연 · 뉴스 · 주식 · 스포츠 · 만화)\n뉴스 · 주식 · 대회 중계는 서버의 진짜 정보 · 웅크리고 누르면 끄기"
    export_stations()
    export_ore_miner()
    anim_textures.build()
    build_34(ko, en)
    import machine_design as M
    kitchen_models.export("realty", M.realty, 0, 0, 0, 0, 2)
    ko["block.wildplus.realty"] = "부동산 중개소"
    ko["tooltip.wildplus.realty"] = "진짜 부동산처럼: 매물 카드(땅 9필지 · 신축 집 6종 · 축사 · 닭장) → 상세 정보 → 계약하기 → 서명\n내 땅 관리 · 매물로 내놓기도 여기서"
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})


# ═══════════ 3.4 목장 · 집들이 · 주말 대회 ═══════════
from PIL import Image  # noqa: E402

import ranch_design as R  # noqa: E402
from data_v18 import simple  # noqa: E402
from data_v23 import mini_house  # noqa: E402

TEX_ITEM = ASSETS / "textures/item"


def _icon(name, draw):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = tuple(c) + (255,)
    draw(put)
    im.save(TEX_ITEM / f"{name}.png")


def _ellipse(put, cx, cy, rx, ry, col, edge=None, hi=None):
    for x in range(16):
        for y in range(16):
            d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
            if d <= 1:
                c = col
                if edge and d > 0.72:
                    c = edge
                if hi and ((x + 0.5 - cx + rx * 0.35) ** 2 / (rx * 0.3) ** 2 + (y + 0.5 - cy + ry * 0.4) ** 2 / (ry * 0.25) ** 2) <= 1:
                    c = hi
                put(x, y, c)


def icons():
    def milk(put):
        for y in range(4, 15):
            w = 2 if y < 7 else 4
            for x in range(8 - w, 8 + w):
                edge = x in (8 - w, 8 + w - 1) or y == 14
                put(x, y, (170, 200, 215) if edge else ((250, 250, 248) if y > 7 else (220, 235, 240)))
        for x in (7, 8):
            put(x, 3, (150, 110, 70))
            put(x, 2, (150, 110, 70))
        put(6, 9, (255, 255, 255))
    _icon("milk_bottle", milk)

    def butter(put):
        for x in range(2, 14):
            put(x, 12, (200, 200, 205))
            put(x, 13, (160, 160, 168))
        for x in range(4, 12):
            for y in range(7, 12):
                put(x, y, (255, 225, 110) if y > 7 else (255, 240, 160))
        for y in range(8, 12):
            put(11, y, (220, 180, 70))
    _icon("butter", butter)

    def cream(put):
        for x in range(4, 12):
            for y in range(7, 14):
                put(x, y, (110, 160, 220) if y in (7, 13) or x in (4, 11) else (250, 250, 250))
        _ellipse(put, 8, 6, 4, 2.2, (255, 255, 252), (230, 230, 225))
        put(8, 3, (255, 255, 255))
    _icon("cream", cream)

    def tuft(put):
        for (cx, cy, r) in ((6, 8, 3.5), (10, 7, 3.2), (8, 10.5, 3.5), (11, 10, 2.6)):
            _ellipse(put, cx, cy, r, r * 0.9, (245, 245, 240), (215, 215, 210))
    _icon("wool_tuft", tuft)

    def yarn(put):
        _ellipse(put, 8, 9, 5.5, 5.5, (230, 90, 120), (190, 60, 90))
        for k in range(-5, 6):
            put(8 + k, 9 + k // 2, (250, 150, 170))
            put(8 + k // 2, 9 - k, (250, 150, 170))
        for x in range(12, 16):
            put(x, 14, (230, 90, 120))
    _icon("yarn", yarn)

    def cloth(put):
        for x in range(2, 14):
            for y in range(5, 13):
                c = (240, 235, 220) if (x + y) % 4 else (225, 215, 195)
                if y in (5, 12):
                    c = (200, 190, 170)
                put(x, y, c)
        for x in range(2, 14):
            put(x, 8, (200, 70, 80))
    _icon("cloth", cloth)

    def golden_egg(put):
        _ellipse(put, 8, 9, 4.6, 6, (250, 205, 60), (200, 150, 30), (255, 245, 180))
    _icon("golden_egg", golden_egg)

    def brush(put):
        for x in range(2, 12):
            for y in range(6, 9):
                put(x, y, (170, 110, 60) if y < 8 else (130, 80, 40))
        for x in range(3, 11):
            for y in range(9, 12):
                put(x, y, (240, 220, 180) if (x + y) % 2 else (210, 190, 150))
        for x in range(11, 15):
            put(x, 6, (150, 95, 50))
            put(x, 7, (120, 75, 40))
    _icon("animal_brush", brush)


def export_34():
    """목장 기계 · 가축 상점 · 방명록 · 문패 · 대회 게시판 · 시상대 · 트로피 3D 모델"""
    for d in (R.RANCH, R.HOMEVISIT, R.CONTEST):
        for name, (ko, fn, l0, l1, d0, d1, h, feat) in d.items():
            kitchen_models.export(name, fn, l0, l1, d0, d1, h)
    # 축사 상자 (3D 미니어처 아이템)
    raid = json.loads((ROOT / "models/block/raid_altar.json").read_text())
    for kid, (ko, fn, size, h, price, feat) in R.BARNS.items():
        name = f"house_{kid}"
        kitchen_models.export(name, lambda fn=fn: mini_house(fn), 0, 0, 0, 0, 1)
        (ROOT / f"blockstates/{name}.json").unlink(missing_ok=True)
        model = json.loads((ROOT / f"models/block/{name}.json").read_text())
        model["display"] = raid["display"]
        (ROOT / f"models/item/{name}.json").write_text(json.dumps(model, ensure_ascii=False))
        (ROOT / f"models/block/{name}.json").unlink()
        (ROOT / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"wildplus:item/{name}"}}))


SIMPLE = {"milk_bottle": ("우유병", "Milk Bottle", "동물 브러시로 소 · 염소를 빗질하면 얻음 · 치즈 숙성통 · 버터 교반기 재료 · 요리 재료"),
          "butter": ("버터", "Butter", "버터 교반기에서 우유로 만듦 · 쿠키 · 토스트 재료"),
          "cream": ("생크림", "Cream", "버터 교반기에서 우유로 만듦 · 치즈케이크 · 크림 스튜 · 까르보나라 재료"),
          "wool_tuft": ("양털 뭉치", "Wool Tuft", "양을 빗질하면 얻음 · 물레에서 실로"),
          "yarn": ("실", "Yarn", "물레에서 양털로 만듦 · 실 4개 → 천"),
          "cloth": ("천", "Cloth", "실 4개로 만든 고운 천 · 비싸게 팔림"),
          "golden_egg": ("황금 알", "Golden Egg", "애정도 ♥5 닭이 가끔 낳는 알 · 아주 비싸게 팔림 (목장 대회 +10점)"),
          "animal_brush": ("동물 브러시", "Animal Brush", "소 · 닭 · 양 · 염소 · 돼지를 우클릭 (하루 한 번)\n애정도 +40 · 오늘의 생산물 (하트 2 은 · 4 금 · 5 무지개 등급)")}

BLOCKS_34 = {
    "hay_trough": ("건초 여물통", "Hay Trough", "가로 2칸 · 건초 더미(9회분) · 밀(1회분)을 넣으면 반경 8칸 동물이 하루 한 번 먹고 애정도 +25"),
    "nest_box": ("둥지 상자", "Nest Box", "근처 6칸 닭이 하루 한 번 여기에 알을 낳음 · 애정도가 높으면 은 · 금 · 무지개 알 · 황금 알\n빈손 우클릭으로 꺼내기"),
    "cheese_barrel": ("치즈 숙성통", "Cheese Barrel", "우유병 · 우유 양동이를 넣으면 1분에 치즈 1개 (등급 그대로)"),
    "butter_churn": ("버터 교반기", "Butter Churn", "높이 2 · 우유를 넣으면 20초마다 버터 · 생크림이 번갈아"),
    "spinning_wheel": ("물레", "Spinning Wheel", "높이 2 · 양털 뭉치(실 2) · 양털 블록(실 3) → 10초 · 실 4개로 천"),
    "ranch_market": ("가축 상점", "Ranch Market", "가축 알 · 건초 · 브러시 · 목장 기계 · 축사 상자를 파는 가게"),
    "guestbook": ("방명록", "Guestbook", "집에 놓으면 우리 집 등록 · 친구가 별점 · 한 줄 후기, 주인은 답글\n집 점수(가구 · 침대 · 조명 · 트로피 · 꽃) · 매주 '예쁜 집' 상금 5만 동"),
    "house_plaque": ("우리 집 문패", "House Plaque", "벽에 거는 문패 · 누르면 집 이름 · 평균 별점 · 집 점수 · 최근 후기"),
    "contest_board": ("대회 게시판", "Contest Board", "가로 2 · 높이 2 · 주말 대회 종목 · 남은 시간 · 실시간 순위 · 지난 결과"),
    "podium": ("시상대", "Podium", "가로 3칸 · 올라서서 누르면 폭죽 + 지난 대회 결과"),
    "trophy_gold": ("금 트로피", "Gold Trophy", "주말 대회 1등 · 집 점수 +6 · 반짝반짝"),
    "trophy_silver": ("은 트로피", "Silver Trophy", "주말 대회 2등 · 집 점수 +6"),
    "trophy_bronze": ("동 트로피", "Bronze Trophy", "주말 대회 3등 · 집 점수 +6"),
}

RECIPES_34 = {
    "animal_brush": shaped(["  W", " S ", "P  "], {"W": "#minecraft:wool", "S": "minecraft:stick", "P": "#minecraft:planks"}, i("animal_brush"), category="equipment"),
    "hay_trough": shaped(["P P", "PHP"], {"P": "#minecraft:planks", "H": "minecraft:hay_block"}, i("hay_trough"), category="misc"),
    "nest_box": shaped(["SSS", "PHP"], {"S": "#minecraft:wooden_slabs", "P": "#minecraft:planks", "H": "minecraft:hay_block"}, i("nest_box"), category="misc"),
    "cheese_barrel": shaped(["I I", "PBP", "PIP"], {"I": "minecraft:iron_nugget", "P": "#minecraft:planks", "B": "minecraft:barrel"}, i("cheese_barrel"), category="misc"),
    "butter_churn": shaped([" S ", "PBP", " P "], {"S": "minecraft:stick", "P": "#minecraft:planks", "B": "minecraft:bucket"}, i("butter_churn"), category="misc"),
    "spinning_wheel": shaped([" S ", "SIS", "PPP"], {"S": "minecraft:stick", "I": "minecraft:iron_ingot", "P": "#minecraft:planks"}, i("spinning_wheel"), category="misc"),
    "ranch_market": shaped(["PEP", "PHP", "PPP"], {"P": "#minecraft:planks", "E": "minecraft:emerald", "H": "minecraft:hay_block"}, i("ranch_market"), category="misc"),
    "guestbook": shaped([" BF", " S ", "PPP"], {"B": "minecraft:writable_book", "F": "minecraft:feather", "S": "minecraft:stick", "P": "#minecraft:planks"},
                        i("guestbook"), category="misc"),
    "house_plaque": shaped(["GGG", "PSP"], {"G": "minecraft:gold_nugget", "P": "#minecraft:planks", "S": "#minecraft:signs"}, i("house_plaque"), category="misc"),
    "contest_board": shaped(["SBS", "SBS", "P P"], {"S": "minecraft:stick", "B": "minecraft:black_concrete", "P": "#minecraft:planks"}, i("contest_board"), category="misc"),
    "podium": shaped(["   ", " Q ", "QQQ"], {"Q": "minecraft:quartz_block"}, i("podium"), category="misc"),
    "cloth": shapeless([i("yarn"), i("yarn"), i("yarn"), i("yarn")], i("cloth")),
    "milk_bucket_from_bottles": shapeless([i("milk_bottle"), i("milk_bottle"), i("milk_bottle"), "minecraft:bucket"], "minecraft:milk_bucket"),
}


def build_34(ko, en):
    TEX_ITEM.mkdir(parents=True, exist_ok=True)
    icons()
    export_34()
    for n, (k, e, tip) in SIMPLE.items():
        simple(n)
        ko[f"item.wildplus.{n}"], en[f"item.wildplus.{n}"] = k, e
        ko[f"tooltip.wildplus.{n}"] = tip
    for n, (k, e, tip) in BLOCKS_34.items():
        ko[f"block.wildplus.{n}"], en[f"block.wildplus.{n}"] = k, e
        ko[f"tooltip.wildplus.{n}"] = tip
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    for kid, (k, fn, (w, d), h, price, feat) in R.BARNS.items():
        ko[f"item.wildplus.house_{kid}"] = f"{k} 상자"
        en[f"item.wildplus.house_{kid}"] = kid.title() + " Kit"
        ko[f"tooltip.wildplus.house_{kid}"] = (f"{w}×{d}칸 · {feat}\n들고 땅을 보면 빛나는 테두리로 자리 미리 보기 (초록 = 입구)\n"
                                               "웅크리고(Shift) 우클릭하면 지어짐 · 자리에 나무 · 건물이 없어야 함 · 가축 상점에서 판매")
    for n, obj in RECIPES_34.items():
        write(DATA / "recipe" / f"{n}.json", obj)
    t = MC / "tags"
    merge_tag(t / "block/mineable/axe.json", [i(n) for n in BLOCKS_34 if not n.startswith("trophy") and n != "podium"])
    merge_tag(t / "block/mineable/pickaxe.json", [i(n) for n in BLOCKS_34 if n.startswith("trophy") or n == "podium"])
    # 게임 속 요일 HUD
    ko["hud.wildplus.time_wd"] = "%s일차 (%s) · %s %s:%s"
    en["hud.wildplus.time_wd"] = "Day %s (%s) · %s %s:%s"
    for k, (a, b) in enumerate(zip("월화수목금토일", ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"])):
        ko[f"hud.wildplus.wd.{k}"], en[f"hud.wildplus.wd.{k}"] = a, b
