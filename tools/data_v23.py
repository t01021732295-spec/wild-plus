"""3.3 데이터: 집 꾸미기 — 가구 모델(켜짐/꺼짐) · 침대 · 집 상자(3D 미니어처) · 가구점 · 집 구조물."""
import json

from cosmetic_design import Box
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
import furniture_design as F
import house_design as Hs
import house_structures
import kitchen_models

ROOT = kitchen_models.ROOT
TOGGLE = {"fireplace", "tv_stand", "floor_lamp", "nightstand", "chandelier"}
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
KO = {k: v[0] for k, v in F.FURNITURE.items()}
SOFA_KO = {k: v[0] for k, v in F.SOFA_COLORS.items()}


def shifted(fn, dy):
    def g():
        out = fn()
        for b in out:
            b.y0 += dy
            if b.rot:
                a, ang, p = b.rot
                b.rot = (a, ang, (p[0], p[1] + dy, p[2]))
        return out
    return g


def export_furniture(name, fn, lat0, lat1, dep0, dep1, h, toggle):
    """모델 (켜짐 · 꺼짐) + 상태 (facing × lit) + 아이템"""
    F.LIT = True
    kitchen_models.export(name, fn, lat0, lat1, dep0, dep1, h)
    if toggle:
        F.LIT = False
        kitchen_models.export(name + "_off", fn, lat0, lat1, dep0, dep1, h)
        (ROOT / f"blockstates/{name}_off.json").unlink(missing_ok=True)
        (ROOT / f"items/{name}_off.json").unlink(missing_ok=True)
        F.LIT = True
    variants = {}
    for f, r in ROT.items():
        for lit in ("true", "false"):
            m = f"wildplus:block/{name}" + ("_off" if toggle and lit == "false" else "")
            variants[f"facing={f},lit={lit}"] = {"model": m, "y": r} if r else {"model": m}
    (ROOT / f"blockstates/{name}.json").write_text(json.dumps({"variants": variants}, indent=1))


def export_bed(name, fn):
    """침대: 발치 칸(본체) 기준, 머리는 바라보는 쪽 한 칸 (모델 -z) → 앞뒤 칸 -1..0"""
    kitchen_models.export(name, fn, 0, 0, -1, 0, 2)
    variants = {}
    for f, r in ROT.items():
        for part in ("foot", "head"):
            for occ in ("true", "false"):
                m = f"wildplus:block/{name}" if part == "foot" else "wildplus:block/big_part_hidden"
                variants[f"facing={f},occupied={occ},part={part}"] = {"model": m, "y": r} if r and part == "foot" else {"model": m}
    (ROOT / f"blockstates/{name}.json").write_text(json.dumps({"variants": variants}, indent=1))


def bed_loot(name):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": i(name)}],
            "conditions": [{"condition": "minecraft:survives_explosion"},
                           {"condition": "minecraft:block_state_property", "block": i(name), "properties": {"part": "head"}}]}]}


def mini_house(fn, size=24.0):
    """집 도안을 작게 줄인 3D 아이템 모델 (칠하기는 원래 4칸 무늬를 줄여서)"""
    boxes = fn()
    xs = [b.x0 for b in boxes] + [b.x0 + b.w for b in boxes]
    zs = [b.z0 for b in boxes] + [b.z0 + b.d for b in boxes]
    ys = [b.y0 for b in boxes] + [b.y0 + b.h for b in boxes]
    span = max(max(xs) - min(xs), max(zs) - min(zs), max(ys) - min(ys))
    k = size / span
    out = []
    for b in boxes:
        res = getattr(b, "res", 1)
        dims = {"front": (b.w, b.h), "back": (b.w, b.h), "left": (b.d, b.h), "right": (b.d, b.h), "top": (b.w, b.d), "bottom": (b.w, b.d)}

        def paint(face, u, v, W, H, b=b, res=res, dims=dims):
            ow, oh = (max(1, round(t * res)) for t in dims[face])
            return b.paint(face, min(ow - 1, int(u * ow / max(1, W))), min(oh - 1, int(v * oh / max(1, H))), ow, oh)
        nb = Box(b.x0 * k, (b.y0 - min(ys)) * k, b.z0 * k, max(0.3, b.w * k), max(0.3, b.h * k), max(0.3, b.d * k), paint)
        out.append(nb)
    return out


def build(lang_ko, lang_en):
    ko, en = {}, {}
    # 가구
    for name, (kname, fn, l0, l1, d0, d1, h, feat) in F.FURNITURE.items():
        if name in ("canopy_bed", "bunk_bed"):
            continue
        if name == "sofa":
            for c, (cko, rgb) in F.SOFA_COLORS.items():
                F.SOFA_COLOR = c
                export_furniture(f"sofa_{c}", fn, l0, l1, d0, d1, h, False)
                ko[f"block.wildplus.sofa_{c}"] = f"3인 소파 ({cko})"
                en[f"block.wildplus.sofa_{c}"] = f"{c.title()} Sofa"
                ko[f"tooltip.wildplus.sofa_{c}"] = "가로 3칸 · 우클릭하면 앉기 (세 자리) · 웅크리면 일어나기"
            F.SOFA_COLOR = "teal"
            continue
        if name == "chandelier":
            fn = shifted(fn, -16)
        export_furniture(name, fn, l0, l1, d0, d1, h, name in TOGGLE)
        ko[f"block.wildplus.{name}"] = kname
        en[f"block.wildplus.{name}"] = name.replace("_", " ").title()
    tips = {
        "armchair": "우클릭하면 앉기", "coffee_table": "우클릭: 커피 한 잔 (신속 · 성급함 3분 · 5분마다)",
        "fireplace": "가로 2칸 · 우클릭으로 불 켜기/끄기 · 밝은 빛 · 장작 타는 소리", "tv_stand": "가로 2칸 · 우클릭으로 TV 켜기/끄기",
        "floor_lamp": "우클릭으로 켜기/끄기 · 밝은 빛", "monstera": "장식 화분", "rug": "가로 3 × 세로 2칸 바닥 카펫 · 밟고 지나감",
        "wardrobe": "가로 2칸 · 높이 2 · 보관함 54칸", "vanity": "가로 2칸 · 우클릭: 치장 (날개 · 링 · 탈) 갈아입기",
        "nightstand": "우클릭으로 스탠드 켜기/끄기", "fridge": "높이 2 · 보관함 27칸", "sink": "빈 양동이 · 유리병을 들고 우클릭하면 물을 받음",
        "bookshelf": "가로 2칸 · 높이 2 · 마법 부여대 근처에 두면 책장 2개 효과", "desk": "가로 2칸 · 우클릭: 컴퓨터로 주식 보기",
        "grandfather_clock": "높이 2 · 우클릭: 지금 시각 알려 줌", "piano": "가로 2칸 · 우클릭할 때마다 음이 울림",
        "aquarium": "가로 2칸 · 빛 · 우클릭: 먹이 주기 (행운 5분 · 5분마다)", "chandelier": "천장 아래 칸에 설치 · 우클릭으로 켜기/끄기 · 밝은 빛",
        "bathtub": "가로 2칸 · 우클릭하면 몸 담그기 (재생)",
    }
    for k, t in tips.items():
        ko[f"tooltip.wildplus.{k}"] = t
    # 침대
    F.LIT = True
    export_bed("canopy_bed", F.canopy_bed)
    export_bed("bunk_bed", F.bunk_bed)
    for b, (kk, ee) in {"canopy_bed": ("캐노피 침대", "Canopy Bed"), "bunk_bed": ("이층 침대", "Bunk Bed")}.items():
        ko[f"block.wildplus.{b}"] = kk
        en[f"block.wildplus.{b}"] = ee
        ko[f"tooltip.wildplus.{b}"] = "1 × 2칸 · 진짜 침대 (밤에 잠자기 · 리스폰 지점)"
        write(DATA / f"loot_table/blocks/{b}.json", bed_loot(b))
    # 가구점
    kitchen_models.export("home_store", F.home_store, 0, 0, 0, 0, 1)
    ko["block.wildplus.home_store"] = "가구점"
    en["block.wildplus.home_store"] = "Furniture Store"
    ko["tooltip.wildplus.home_store"] = "집 상자 6종 · 가구 30종을 파는 가게 (진열장 화면)"
    # 집 상자 (3D 미니어처 아이템)
    raid = json.loads((ROOT / "models/block/raid_altar.json").read_text())
    for hid, (hko, fn, (w, d), h, price, feat) in Hs.HOUSES.items():
        name = f"house_{hid}"
        kitchen_models.export(name, lambda fn=fn: mini_house(fn), 0, 0, 0, 0, 1)
        (ROOT / f"blockstates/{name}.json").unlink(missing_ok=True)
        model = json.loads((ROOT / f"models/block/{name}.json").read_text())
        model["display"] = raid["display"]
        (ROOT / f"models/item/{name}.json").write_text(json.dumps(model, ensure_ascii=False))
        (ROOT / f"models/block/{name}.json").unlink()
        (ROOT / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"wildplus:item/{name}"}}))
        ko[f"item.wildplus.{name}"] = f"{hko} 상자"
        en[f"item.wildplus.{name}"] = hid.replace("_", " ").title() + " Kit"
        ko[f"tooltip.wildplus.{name}"] = (f"{w}×{d}칸 · {feat}\n들고 땅을 보면 빛나는 테두리로 자리 미리 보기 (초록 = 현관)\n"
                                          "웅크리고(Shift) 우클릭하면 집이 지어짐 · 자리에 나무 · 건물이 없어야 함 · 내 땅 · 빈 땅만")
    # 부속 칸: 낮은 칸 · 납작한 칸
    bp = json.loads((ROOT / "blockstates/big_part.json").read_text())
    for f, r in ROT.items():
        for part in ("low", "flat"):
            bp["variants"][f"facing={f},part={part}"] = {"model": "wildplus:block/big_part_hidden"}
    (ROOT / "blockstates/big_part.json").write_text(json.dumps(bp, indent=2))
    # 떨어뜨리는 것 · 태그 · 만들기
    names = [f"sofa_{c}" for c in F.SOFA_COLORS] + [n for n in F.FURNITURE if n not in ("sofa", "canopy_bed", "bunk_bed")] + ["home_store"]
    for n in names:
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    t = MC / "tags"
    merge_tag(t / "block/mineable/axe.json", [i(n) for n in names + ["canopy_bed", "bunk_bed"]])
    merge_tag(t / "block/enchantment_power_provider.json", [i("bookshelf"), i("big_part")])
    write(DATA / "recipe/home_store.json", shaped(["PEP", "PCP", "PPP"], {"P": "#minecraft:planks", "E": "minecraft:emerald", "C": "minecraft:chest"},
                                                 i("home_store"), category="misc"))
    # 집 구조물
    (house_structures.Template.__module__)
    from structures_v3 import OUT
    (OUT / "house").mkdir(parents=True, exist_ok=True)
    house_structures.build()
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})
