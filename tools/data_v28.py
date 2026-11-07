"""3.8 데이터 (data_v28): 흉조 업데이트 — 흉조 투척병 · 테마 흉조 5종(양조) · 우민 올가미 · 금지된 물약 · 흉조 제단 · 우민 감옥
· 우민 밀수꾼/용병 스킨 · 전초 기지/대저택 구조물 태그 · 이벤트 보상 상향 · 후반 이벤트 언어.

텍스처(아이템 아이콘 · 몹 스킨)는 바닐라 그림(흉조 병 · 변명자)을 바탕으로 색을 바꿔 만든다.
바닐라 그림이 있는 폴더를 WILDPLUS_VANILLA_REF 로 알려 주면 다시 그리고, 없으면 이미 만든 텍스처를 그대로 둔다.
"""
import json
import os
from pathlib import Path

from PIL import Image

from data_v2 import ASSETS, DATA, MC, entry, i, merge_tag, self_loot, shaped, shapeless, write
from data_v18 import simple

REF = Path(os.environ.get("WILDPLUS_VANILLA_REF", "/tmp/claude-0/-home-claude/42e6d42c-ce25-5d34-a33d-971e68bb1142/scratchpad/van2"))

THEMES = {
    "burning_omen": ("burning", "불타는 흉조", "Burning Omen", "minecraft:blaze_powder", "블레이즈 가루",
                     "우민이 불에 강하고 불을 붙임 · 쇠뇌 화살이 불화살 · 블레이즈 합류 · 보상 +30%"),
    "frozen_omen": ("frozen", "얼어붙은 흉조", "Frozen Omen", "minecraft:blue_ice", "푸른 얼음",
                    "우민에게 맞으면 느려짐 · 스트레이 합류 · 보상 +30%"),
    "deep_omen": ("deep", "심해의 흉조", "Abyssal Omen", "minecraft:prismarine_crystals", "프리즈머린 수정",
                  "우민에게 맞으면 나약함 · 삼지창 드라운드 합류 · 보상 +30%"),
    "wraith_omen": ("wraith", "망령의 흉조", "Wraith Omen", "minecraft:phantom_membrane", "팬텀 막",
                    "우민에게 맞으면 시듦 · 벡스 합류 · 우민 흡수 보호막 · 보상 +40%"),
    "golden_omen": ("golden", "황금 흉조", "Golden Omen", "minecraft:gold_block", "금 블록",
                    "우민이 금 갑옷 · 체력 1.5배 · 처치마다 금화 · 돈 보상 2.5배"),
}

LANG_KO = {
    "item.wildplus.omen_flask": "흉조 투척병",
    "tooltip.wildplus.omen_flask": "던져서 깨뜨림 · 몬스터에 맞으면 무조건 정예 (보상 ↑)\n친구에게 맞으면 흉조(나쁜 징조) 장난 · /omen prank off 로 거부\n흉조 병 + 화약 + 실",
    "item.wildplus.illager_lasso": "우민 올가미",
    "tooltip.wildplus.illager_lasso": "체력 40% 이하인 약탈자 · 변명자 · 소환사 · 환술사를 묶음\n묶은 채 우민 감옥을 누르면 포로 · 웅크리고 쓰면 풀어 줌",
    "item.wildplus.forbidden_potion": "금지된 물약 (광기)",
    "tooltip.wildplus.forbidden_potion": "1분 동안 힘 III · 신속 II · 저항 I\n끝나면 1분 나약함 II · 구속 · 허기 (우민 밀수꾼)",
    "block.wildplus.omen_altar": "흉조 제단",
    "tooltip.wildplus.omen_altar": ("마을에 놓고 흉조 병을 바치면 수호 게이지 (병 I~V = 1~5점 · 테마 흉조 6점)\n"
                                    "1단계 경비 골렘 · 2단계 주민 체력 2배 · 3단계 망루 자동 사격\n4단계 마을 상점 10% 할인 · 5단계 습격 때 주민 보호막 (주변 48칸)"),
    "block.wildplus.illager_prison": "우민 감옥",
    "tooltip.wildplus.illager_prison": "우민 올가미로 묶은 포로를 4명까지 가둠\n💰 몸값 (다음 날 아침 통장) · 🛡 전향 (흉조 병 1 → 우민 용병) · 📜 심문",
    "entity.wildplus.illager_mercenary": "우민 용병",
    "entity.wildplus.illager_smuggler": "우민 밀수꾼",
    # 후반 이벤트
    "event.wildplus.omen_storm": "흉조 폭풍",
    "event.wildplus.omen_storm.desc": "모두에게 흉조 · 흉조 습격 보상 ×1.5 · 우민 밀수꾼 3배",
    "event.wildplus.illager_armada": "우민 대군",
    "event.wildplus.illager_armada.desc": "우민 16명이 쳐들어옴 · 처치마다 3,000동 · 전멸시키면 모두 10만동",
    "event.wildplus.starfall": "별똥별 대낙하",
    "event.wildplus.starfall.desc": "루비 · 사파이어 · 미스릴이 박힌 운석 (정예 수호자)",
    "event.wildplus.golden_vault": "황금 금고",
    "event.wildplus.golden_vault.desc": "다이아 동전 30~60 · 에메랄드 동전이 든 금고 (황금 정예가 지킴)",
    "event.wildplus.market_boom": "시장 호황",
    "event.wildplus.market_boom.desc": "출하 상자가 20초마다 바로 팔리고 값은 1.5배",
    # 보상 상향
    "message.wildplus.event.wanted": "현상수배범이 X %s, Z %s 근처에 나타났어요! (빛나는 도적, 현상금 3만동)",
    "message.wildplus.event.wanted_done": "★ %s 님이 현상수배범을 잡았어요! (현상금 3만동)",
    "event.wildplus.bank_bonus.desc": "통장 잔액의 2% 보너스 (최대 3만동)",
    "event.wildplus.coin_rain.desc": "하늘에서 은화 · 금화 · 가끔 다이아 동전이 떨어짐",
    "event.wildplus.wanted.desc": "현상수배범을 잡으면 3만동",
    "event.wildplus.gold_rush.desc": "몬스터를 잡으면 은화 · 금화가 우수수",
    "event.wildplus.full_moon.desc": "몬스터 경험치 3배 · 25% 확률로 금화",
    "event.wildplus.treasure_goblin.desc": "빛나는 보물 토끼를 잡으면 금화 · 다이아 동전 · 다이아몬드",
}
LANG_EN = {
    "item.wildplus.omen_flask": "Omen Flask", "item.wildplus.illager_lasso": "Illager Lasso", "item.wildplus.forbidden_potion": "Forbidden Potion",
    "block.wildplus.omen_altar": "Omen Altar", "block.wildplus.illager_prison": "Illager Prison",
    "entity.wildplus.illager_mercenary": "Illager Mercenary", "entity.wildplus.illager_smuggler": "Illager Smuggler",
    "event.wildplus.omen_storm": "Omen Storm", "event.wildplus.omen_storm.desc": "Everyone gets Bad Omen · raid rewards x1.5",
    "event.wildplus.illager_armada": "Illager Armada", "event.wildplus.illager_armada.desc": "A huge illager warband attacks",
    "event.wildplus.starfall": "Starfall", "event.wildplus.starfall.desc": "A meteor full of rare ores crashes nearby",
    "event.wildplus.golden_vault": "Golden Vault", "event.wildplus.golden_vault.desc": "A guarded vault full of coins appears",
    "event.wildplus.market_boom": "Market Boom", "event.wildplus.market_boom.desc": "Shipping bins sell instantly at 1.5x",
}
for name, (_, ko, en, reagent, rko, desc) in THEMES.items():
    LANG_KO[f"item.wildplus.{name}"] = ko
    LANG_KO[f"tooltip.wildplus.{name}"] = (f"마시면 흉조(나쁜 징조) + 다음 습격 테마\n{desc}\n"
                                           f"양조대: 흉조 병 + {rko} (흉조 세기 그대로)")
    LANG_EN[f"item.wildplus.{name}"] = en


# ───────── 텍스처 ─────────
def _swap(im, pairs):
    p = im.load()
    for x in range(im.width):
        for y in range(im.height):
            c = p[x, y]
            if c[3] and c[:3] in pairs:
                p[x, y] = pairs[c[:3]] + (c[3],)


def textures():
    import illager_skins as S
    tex = ASSETS / "textures"
    bottle = REF / "ominous_bottle.png"
    vind = REF / "vindicator.png"
    if bottle.exists():
        base = Image.open(bottle).convert("RGBA")
        S.omen_variant(base, "splash").save(tex / "item/omen_flask.png")
        for name, (kind, *_rest) in THEMES.items():
            S.omen_variant(base, kind).save(tex / f"item/{name}.png")
        # 금지된 물약: 같은 병 모양 · 검붉은 유리 · 핏빛 물약
        fp = base.copy()
        _swap(fp, {**dict(zip(S.GLASS, [(40, 10, 16), (90, 20, 30), (150, 40, 50), (220, 80, 90)])),
                   **dict(zip(S.LIQUID, [(30, 0, 6), (90, 6, 20), (170, 20, 40), (240, 70, 80)])),
                   **dict(zip(S.EYES, [(20, 20, 20), (60, 60, 60)]))})
        fp.save(tex / "item/forbidden_potion.png")
    if vind.exists():
        v = Image.open(vind).convert("RGBA")
        (tex / "entity").mkdir(parents=True, exist_ok=True)
        S.make_smuggler(v).save(tex / "entity/illager_smuggler.png")
        S.make_mercenary(v).save(tex / "entity/illager_mercenary.png")
    lasso = tex / "item/illager_lasso.png"
    if not lasso.exists():
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        p = im.load()
        import math
        for a in range(0, 360, 6):
            x = 9 + 4.6 * math.cos(math.radians(a))
            y = 5.5 + 3.6 * math.sin(math.radians(a))
            p[int(x), int(y)] = (196, 156, 96, 255) if a % 24 else (150, 112, 66, 255)
        for k in range(8):
            p[max(0, 5 - k // 2), 8 + k] = (176, 136, 80, 255)
            p[max(0, 6 - k // 2), 8 + k] = (130, 96, 58, 255)
        for (x, y) in [(5, 8), (6, 8), (5, 9)]:
            p[x, y] = (90, 64, 40, 255)
        im.save(lasso)


def build(lang_ko, lang_en):
    textures()
    import kitchen_models
    import omen_design as O
    kitchen_models.export("omen_altar", O.omen_altar, 0, 1, 0, 0, 2)
    kitchen_models.export("illager_prison", O.prison, 0, 1, 0, 0, 2)
    for b in ["omen_altar", "illager_prison"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("omen_altar"), i("illager_prison")])
    for n in ["omen_flask", "illager_lasso", "forbidden_potion", *THEMES]:
        simple(n)
    # 레시피
    write(DATA / "recipe/omen_flask.json", shapeless(["minecraft:ominous_bottle", "minecraft:gunpowder", "minecraft:string"], i("omen_flask")))
    write(DATA / "recipe/illager_lasso.json", shaped([" S ", "SLS", " S "], {"S": "minecraft:string", "L": "minecraft:lead"},
                                                     i("illager_lasso"), category="misc"))
    write(DATA / "recipe/omen_altar.json", shaped(["BCB", "GEG", "DDD"], {
        "B": "minecraft:ominous_bottle", "C": "minecraft:crying_obsidian", "G": "minecraft:gold_block", "E": "minecraft:emerald_block",
        "D": "minecraft:polished_deepslate"}, i("omen_altar"), category="misc"))
    write(DATA / "recipe/illager_prison.json", shaped(["III", "ILI", "SSS"], {
        "I": "minecraft:iron_bars", "L": "minecraft:lantern", "S": "minecraft:stone_bricks"}, i("illager_prison"), category="misc"))
    for name, (_, _ko, _en, reagent, *_r) in THEMES.items():
        write(DATA / f"recipe/brewing/{name}.json", {"type": "minecraft:brewing", "input": {"item": "minecraft:ominous_bottle"},
                                                       "reagent": {"item": reagent}, "output": {"id": i(name)}})
    # 구조물 태그 (심문 · 밀수 지도)
    write(DATA / "tags/worldgen/structure/outposts.json", {"values": ["minecraft:pillager_outpost"]})
    write(DATA / "tags/worldgen/structure/mansions.json", {"values": ["minecraft:mansion"]})
    # 이벤트 상자 보상 상향 (보급품 · 보물 찾기에 동전 묶음 추가)
    for name, extra in [("supply_drop", {"rolls": 1, "entries": [entry(i("diamond_coin"), 1, 5, 15)]}),
                        ("treasure_hunt", {"rolls": 2, "entries": [entry(i("diamond_coin"), 6, 8, 16), entry(i("emerald_coin"), 3, 1, 2),
                                                                   entry(i("life_essence"), 1)]})]:
        path = DATA / f"loot_table/gameplay/{name}.json"
        if path.exists():
            t = json.loads(path.read_text())
            if not any(e.get("name") == i("diamond_coin") for q in t["pools"] for e in q["entries"]):
                t["pools"].append({**extra, "bonus_rolls": 0})
                write(path, t)
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})


if __name__ == "__main__":
    _A = ASSETS
    build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
    print("ok v28")
