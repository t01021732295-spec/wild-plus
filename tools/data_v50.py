"""5.2.1 (data_v50): 각인 확장석 · 고대 각인석 4종 (그림 · 이름 · 설명 · 확장석 레시피)."""
from data_v2 import ASSETS, DATA, i, shaped, write
from data_v18 import simple
from data_v35 import stone_icon

LANG_KO = {
    "item.wildplus.engrave_expander": "각인 확장석",
    "tooltip.wildplus.engrave_expander": "강화대 재료 칸에 넣고 각인 버튼\n그 장비의 각인 칸이 2칸으로 (서로 다른 각인 2개)\n고대의 워든이 30% 확률로 떨어뜨림",
    "item.wildplus.engrave_execute": "고대 처형 각인석", "item.wildplus.engrave_sonic": "고대 음파 각인석",
    "item.wildplus.engrave_bulwark": "고대 보루 각인석", "item.wildplus.engrave_echo": "고대 메아리 각인석",
    "tooltip.wildplus.engrave_execute": "무기 공용 (공허 · 겁화 포함, 강화대 각인)\n체력 절반 이하인 적에게 피해 +30%",
    "tooltip.wildplus.engrave_sonic": "무기 공용 (공허 · 겁화 포함, 강화대 각인)\n4타마다 주변 3칸 방어 무시 피해 21.6",
    "tooltip.wildplus.engrave_bulwark": "갑옷 공용 (공허 · 겁화 포함, 강화대 각인)\n받는 피해 -6% (부위마다, 최대 -24%) · 어둠 면역",
    "tooltip.wildplus.engrave_echo": "갑옷 공용 (공허 · 겁화 포함, 강화대 각인)\n나를 때린 적에게 피해 2.4 + 구속 (부위마다, 최대 9.6)",
}
LANG_EN = {"item.wildplus.engrave_expander": "Engraving Expander", "item.wildplus.engrave_execute": "Ancient Execution Stone",
           "item.wildplus.engrave_sonic": "Ancient Sonic Stone", "item.wildplus.engrave_bulwark": "Ancient Bulwark Stone",
           "item.wildplus.engrave_echo": "Ancient Echo Stone"}

STONES = {"execute": ((40, 200, 210), (10, 30, 40)), "sonic": ((120, 240, 255), (20, 50, 70)), "bulwark": ((30, 120, 140), (8, 24, 32)),
          "echo": ((90, 220, 190), (14, 40, 44)), "expander": ((230, 240, 255), (40, 60, 90))}


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    for k, (c, frame) in STONES.items():
        name = "engrave_expander" if k == "expander" else f"engrave_{k}"
        stone_icon(c, frame).save(ASSETS / f"textures/item/{name}.png")
        simple(name)
    write(DATA / "recipe/engrave_expander.json", shaped(["EAE", "ANA", "EAE"], {"E": "minecraft:echo_shard", "A": "minecraft:amethyst_shard", "N": "minecraft:netherite_ingot"},
                                                       i("engrave_expander"), 1, "misc"))
