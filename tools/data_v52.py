"""5.3.6 (data_v52): 해제 각인석 — 그림 · 이름 · 설명 · 고대 도시 상자 3곳에 추가 (워든 드롭은 WardenBoss)."""
import json

from data_v2 import ASSETS, DATA, i, write
from data_v18 import simple
from data_v35 import stone_icon

LANG_KO = {
    "item.wildplus.engrave_eraser": "해제 각인석",
    "tooltip.wildplus.engrave_eraser": "강화대 재료 칸에 넣고 각인 버튼\n장비에 새긴 각인을 하나 지워요 (나중에 새긴 것부터)\n각인 칸 수는 그대로 · 고대 도시 상자, 고대의 워든",
}
LANG_EN = {"item.wildplus.engrave_eraser": "Engraving Eraser",
           "tooltip.wildplus.engrave_eraser": "Put in the enhance table material slot\nRemoves one engraving (latest first)"}


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    stone_icon((200, 210, 225), (70, 20, 30)).save(ASSETS / "textures/item/engrave_eraser.png")
    simple("engrave_eraser")
    for name in ("echo_spire", "sealed_archive", "sleeping_sanctum"):
        p = DATA / f"loot_table/chests/{name}.json"
        t = json.loads(p.read_text())
        ents = t["pools"][0]["entries"]
        if not any(e.get("name") == i("engrave_eraser") for e in ents):
            ents.append({"type": "minecraft:item", "name": i("engrave_eraser"), "weight": 2})
        write(p, t)


if __name__ == "__main__":
    from data_v2 import ASSETS as _A
    build(json.loads((_A / "lang/ko_kr.json").read_text()), json.loads((_A / "lang/en_us.json").read_text()))
