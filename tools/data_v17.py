"""2.8 데이터: 쓰레기통 · 자동 제작대."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
from data_v15 import box

LANG_KO = {
    "block.wildplus.trash_can": "쓰레기통",
    "tooltip.wildplus.trash_can": "넣은 아이템이 바로 사라짐 (되돌릴 수 없음)\n호퍼·필터 호퍼를 연결하면 자동으로 버림",
    "block.wildplus.auto_crafter": "자동 제작대",
    "tooltip.wildplus.auto_crafter": "설계도 3×3에 조합법을 놓으면 재료가 들어오는 대로 자동 제작\n재료 칸 1~9는 설계도 같은 자리 재료 전용 (다른 재료가 자리를 막지 않음)\n위·옆 호퍼로 재료 입력, 아래 호퍼로 결과 출력 · 레드스톤 신호로 정지",
}
LANG_EN = {
    "block.wildplus.trash_can": "Trash Can",
    "tooltip.wildplus.trash_can": "Deletes anything put inside. Works with hoppers",
    "block.wildplus.auto_crafter": "Auto Crafter",
    "tooltip.wildplus.auto_crafter": "Set a recipe in the 3x3 pattern; crafts automatically as ingredients arrive. Hopper in from top/sides, out from bottom",
}


def assets():
    T = {"side": i("block/trash_can_side"), "lid": i("block/trash_can_lid"), "inner": i("block/trash_can_inner"),
         "particle": i("block/trash_can_side")}
    el = [
        box([3, 0, 3], [13, 13, 13], {"side": "#side", "up": "#inner", "down": "#lid"}),
        box([2, 0, 2], [14, 1, 14], {"side": "#lid", "up": "#lid", "down": "#lid"}),
        box([2, 13, 2], [14, 15, 14], {"side": "#lid", "up": "#lid", "down": "#lid"}),
        box([6, 15, 7], [10, 16, 9], {"side": "#lid", "up": "#lid", "down": "#lid"}),
    ]
    write(ASSETS / "models/block/trash_can.json", {"parent": "minecraft:block/block", "textures": T, "elements": el})
    write(ASSETS / "blockstates/trash_can.json", {"variants": {"": {"model": i("block/trash_can")}}})
    write(ASSETS / "models/block/auto_crafter.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": i("block/auto_crafter_top"), "side": i("block/auto_crafter_side"), "bottom": i("block/auto_crafter_bottom")}})
    write(ASSETS / "blockstates/auto_crafter.json", {"variants": {"": {"model": i("block/auto_crafter")}}})
    for n in ["trash_can", "auto_crafter"]:
        write(ASSETS / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{n}")}})


def data():
    for n in ["trash_can", "auto_crafter"]:
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    rec = {
        "trash_can": shaped(["I I", "ICI", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:cactus"}, i("trash_can"), category="misc"),
        "auto_crafter": shaped(["ITI", "RMR", "IHI"], {"I": "minecraft:iron_ingot", "T": "minecraft:crafting_table",
                                                      "R": "minecraft:redstone", "M": i("mythril_ingot"), "H": "minecraft:hopper"},
                               i("auto_crafter"), category="redstone"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("trash_can"), i("auto_crafter")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    data()
