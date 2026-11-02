"""17차 데이터: 정렬 상자 · 자동 사료통 · 우편함, 각인 · 파티 · 집 문구."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, shapeless, write

LANG_KO = {
    "block.wildplus.sorting_chest": "정렬 상자",
    "tooltip.wildplus.sorting_chest": "넣은 아이템을 반경 8칸 안 상자·통으로 자동 분배\n받을 상자 지정: 상자에 아이템 액자를 붙이고 그 아이템을 끼우기\n액자에 정렬 상자를 끼운 상자 = 나머지 전부 · 지정 없으면 같은 아이템 있는 상자로",
    "block.wildplus.feeder": "자동 사료통",
    "tooltip.wildplus.feeder": "먹이를 넣어 두면 반경 8칸 동물을 30초마다 번식 (24마리까지)\n바닥의 알·깃털·양털·가죽을 주워 담고, 빈 양동이로 우유를 받음",
    "block.wildplus.mailbox": "우편함",
    "tooltip.wildplus.mailbox": "친구에게 아이템을 보내고, 받은 우편을 꺼냄\n어느 우편함에서나 같은 우편 (한 사람당 54묶음)",
}
LANG_EN = {
    "block.wildplus.sorting_chest": "Sorting Chest",
    "tooltip.wildplus.sorting_chest": "Sends items to nearby chests. Put an item frame with an item on a chest to assign it; a frame with a sorting chest marks the overflow chest",
    "block.wildplus.feeder": "Auto Feeder",
    "tooltip.wildplus.feeder": "Breeds nearby animals every 30s, collects eggs/feathers/wool, milks cows with empty buckets",
    "block.wildplus.mailbox": "Mailbox",
    "tooltip.wildplus.mailbox": "Send items to friends and collect your mail from any mailbox",
}


def box(frm, to, tex, rot=None, uv=None):
    fs = {}
    for f in ["north", "south", "east", "west", "up", "down"]:
        t = tex if isinstance(tex, str) else tex.get(f, tex.get("side"))
        face = {"texture": t}
        if uv and f in uv:
            face["uv"] = uv[f]
        fs[f] = face
    e = {"from": frm, "to": to, "faces": fs}
    if rot:
        e["rotation"] = rot
    return e


def rotated(model):
    return {"variants": {f"facing={d}": ({"model": model, "y": r} if r else {"model": model})
                         for d, r in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}}


def assets():
    # 정렬 상자
    write(ASSETS / "models/block/sorting_chest.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": i("block/sorting_chest_front"), "side": i("block/sorting_chest_side"), "top": i("block/sorting_chest_top")}})
    write(ASSETS / "blockstates/sorting_chest.json", rotated(i("block/sorting_chest")))
    # 자동 사료통: 나무 여물통 + 건초
    T = {"plank": "minecraft:block/spruce_planks", "log": "minecraft:block/stripped_spruce_log", "hay": "minecraft:block/hay_block_top",
         "hayside": "minecraft:block/hay_block_side", "iron": "minecraft:block/iron_block", "particle": "minecraft:block/spruce_planks"}
    el = [
        box([0, 0, 2], [16, 2, 14], "#plank"),
        box([0, 2, 2], [16, 8, 4], "#plank"),
        box([0, 2, 12], [16, 8, 14], "#plank"),
        box([0, 2, 4], [2, 8, 12], "#plank"),
        box([14, 2, 4], [16, 8, 12], "#plank"),
        box([2, 2, 4], [14, 6, 12], {"side": "#hayside", "up": "#hay", "down": "#hay"}),
        box([-0.5, 5, 1.5], [16.5, 6, 2.5], "#iron"),
        box([-0.5, 5, 13.5], [16.5, 6, 14.5], "#iron"),
        box([1, 0, 1], [3, 3, 15], "#log"),
        box([13, 0, 1], [15, 3, 15], "#log"),
    ]
    write(ASSETS / "models/block/feeder.json", {"parent": "minecraft:block/block", "textures": T, "elements": el})
    write(ASSETS / "blockstates/feeder.json", rotated(i("block/feeder")))
    # 우편함: 나무 기둥 + 빨간 우체통 + 깃발
    T = {"post": "minecraft:block/dark_oak_log", "body": "minecraft:block/red_concrete", "door": "minecraft:block/iron_block",
         "slot": "minecraft:block/black_concrete", "flag": "minecraft:block/yellow_concrete", "particle": "minecraft:block/red_concrete"}
    el = [
        box([6.5, 0, 6.5], [9.5, 8, 9.5], "#post"),
        box([5, 0, 5], [11, 1, 11], "#post"),
        box([3.5, 8, 3], [12.5, 15, 13], "#body"),
        box([4.5, 15, 4], [11.5, 16, 12], "#body"),
        box([4.5, 9, 2.5], [11.5, 14, 3], "#door"),
        box([6, 12.5, 2.3], [10, 13.2, 2.6], "#slot"),
        box([12.5, 11, 9], [13.2, 18, 9.8], "#flag"),
        box([12.5, 16, 9.8], [13.2, 18, 12.5], "#flag"),
    ]
    write(ASSETS / "models/block/mailbox.json", {"parent": "minecraft:block/block", "textures": T, "elements": el})
    write(ASSETS / "blockstates/mailbox.json", rotated(i("block/mailbox")))
    for n in ["sorting_chest", "feeder", "mailbox"]:
        write(ASSETS / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{n}")}})


def data():
    for n in ["sorting_chest", "feeder", "mailbox"]:
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    rec = {
        "sorting_chest": shaped([" C ", "MHM", " R "], {"C": "minecraft:chest", "M": i("mythril_ingot"), "H": "minecraft:hopper",
                                                        "R": "minecraft:comparator"}, i("sorting_chest"), category="redstone"),
        "feeder": shaped(["PWP", "PHP"], {"P": "#minecraft:planks", "W": "minecraft:wheat", "H": "minecraft:hopper"}, i("feeder"), category="misc"),
        "mailbox": shaped([" D ", "ICI", " L "], {"D": "minecraft:red_dye", "I": "minecraft:iron_ingot", "C": "minecraft:chest",
                                                  "L": "#minecraft:logs"}, i("mailbox"), category="misc"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags"
    merge_tag(t / "block/mineable/axe.json", [i("sorting_chest"), i("feeder")])
    merge_tag(t / "block/mineable/pickaxe.json", [i("mailbox")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
