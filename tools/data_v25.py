"""3.5 데이터: 경마장 (블록 · 경주마 피규어 · 경마장 상자 · 구조물) · 마을 발전 (게시판 · 기부함 · 시계판 · 부지 상자 5 · 건물 15)."""
import json

import derby_design as D
import kitchen_models
import structures_v35
import village_design as V
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
from data_v23 import mini_house

ROOT = kitchen_models.ROOT

BLOCKS = {
    "stable": ("마구간", "Stable", "가로 2칸 · 말을 타고 우클릭하면 내 경주마로 등록\n훈련 (달리기 · 장애물) · 먹이 (당근 · 황금 당근 · 건초) · 휴식 · 경주 출전 신청"),
    "betting_kiosk": ("마권 발매기", "Betting Kiosk", "오늘 경주표 · 배당률 · 단승 · 연승 · 복연승 마권 사기\n경주: 게임 속 매일 오후 3시 · 맞히면 배당만큼"),
    "starting_gate": ("출발 게이트", "Starting Gate", "가로 4칸 · 경마장의 출발점 (경주 때 말들이 여기서 달려 나감)"),
    "scoreboard": ("전광판", "Scoreboard", "가로 3칸 · 누르면 오늘 경주 순위 · 배당 · 지난 결과"),
    "finish_post": ("결승 기둥", "Finish Post", "결승선 장식"),
    "project_board": ("마을 발전 게시판", "Village Project Board", "가로 3칸 · 함께 짓는 마을 건물 5종 (도서관 · 놀이공원 · 항구 · 시계탑 · 온천)\n재료와 돈을 기부하면 한 단계씩 지어지고, 완공되면 모두에게 효과"),
    "donation_box": ("기부함", "Donation Box", "마을 발전 기부 (게시판과 같은 화면)"),
    "clock_face": ("시계판", "Clock Face", "벽에 거는 큰 시계 · 바늘이 돎"),
}

RECIPES = {
    "stable": shaped(["SSS", "P P", "PHP"], {"S": "#minecraft:wooden_slabs", "P": "#minecraft:planks", "H": "minecraft:hay_block"}, i("stable"), category="misc"),
    "betting_kiosk": shaped(["GGG", "IPI", "IRI"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot", "P": "minecraft:paper", "R": "minecraft:redstone"},
                            i("betting_kiosk"), category="misc"),
    "project_board": shaped(["PPP", "PMP", "S S"], {"P": "#minecraft:planks", "M": "minecraft:map", "S": "minecraft:stick"}, i("project_board"), category="misc"),
    "donation_box": shaped(["PGP", "PCP"], {"P": "#minecraft:planks", "G": "minecraft:gold_ingot", "C": "minecraft:chest"}, i("donation_box"), category="misc"),
    "clock_face": shaped([" G ", "GCG", " G "], {"G": "minecraft:gold_nugget", "C": "minecraft:clock"}, i("clock_face"), category="misc"),
}


def kit_model(name, fn):
    raid = json.loads((ROOT / "models/block/raid_altar.json").read_text())
    kitchen_models.export(name, lambda fn=fn: mini_house(fn), 0, 0, 0, 0, 1)
    (ROOT / f"blockstates/{name}.json").unlink(missing_ok=True)
    model = json.loads((ROOT / f"models/block/{name}.json").read_text())
    model["display"] = raid["display"]
    (ROOT / f"models/item/{name}.json").write_text(json.dumps(model, ensure_ascii=False))
    (ROOT / f"models/block/{name}.json").unlink()
    (ROOT / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"wildplus:item/{name}"}}))


def build(lang_ko, lang_en):
    ko, en = {}, {}
    for name, (kname, fn, l0, l1, d0, d1, h, feat) in D.DERBY.items():
        kitchen_models.export(name, fn, l0, l1, d0, d1, h)
    kitchen_models.export("project_board", V.project_board, -1, 1, 0, 0, 2)
    kitchen_models.export("donation_box", V.donation_box, 0, 0, 0, 0, 1)
    kitchen_models.export("clock_face", V.clock_face, 0, 0, 0, 0, 1)
    for n, (k, e, tip) in BLOCKS.items():
        ko[f"block.wildplus.{n}"], en[f"block.wildplus.{n}"] = k, e
        ko[f"tooltip.wildplus.{n}"] = tip
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    merge_tag(MC / "tags/block/mineable/axe.json", [i(n) for n in ("stable", "project_board", "donation_box", "scoreboard", "finish_post")])
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i(n) for n in ("betting_kiosk", "starting_gate", "clock_face")])
    for n, obj in RECIPES.items():
        write(DATA / "recipe" / f"{n}.json", obj)
    # 경주마 피규어 (화면 · 아이콘용 3D 아이템)
    for k in range(6):
        kit_model(f"race_horse_{k}", lambda k=k: D.horse(D.COATS[k], D.SILKS[k]))
        ko[f"item.wildplus.race_horse_{k}"] = "경주마 피규어"
        en[f"item.wildplus.race_horse_{k}"] = "Race Horse Figure"
    # 경마장 상자 · 마을 부지 상자
    kit_model("house_racetrack", D.racetrack_mini)
    ko["item.wildplus.house_racetrack"] = "경마장 상자"
    en["item.wildplus.house_racetrack"] = "Racetrack Kit"
    ko["tooltip.wildplus.house_racetrack"] = ("37×41칸 · 흙 타원 주로 · 관중석 · 출발 게이트 · 전광판 · 마구간 4 · 마권 발매기 2\n"
                                              "웅크리고(Shift) 우클릭하면 지어짐 · 넓고 평평한 곳 · 경주는 게임 속 매일 오후 3시")
    for pid, (pname, fn, eff, mats, cost) in V.PROJECTS.items():
        kit_model(f"village_{pid}", fn)
        ko[f"item.wildplus.village_{pid}"] = f"{pname} 공사 부지 상자"
        en[f"item.wildplus.village_{pid}"] = pid.replace("_", " ").title() + " Site Kit"
        ko[f"tooltip.wildplus.village_{pid}"] = f"15×15칸 · 웅크리고(Shift) 우클릭하면 공사 부지(1단계)가 생김\n기부가 모이면 이 자리에서 2단계 · 완공으로 지어짐 · 완공 효과: {eff}"
    structures_v35.build()
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})
