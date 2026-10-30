"""7차 데이터: 장비 효과 설명, 방어구 관통 인챈트, 워프 이름 변경·출하 상자 공개 설정 문구, HUD 소지금."""
import json

from data_v2 import ASSETS, DATA, MC, i, merge_tag, write

SET_BLOOD = "2부위: 밤에 힘 I\n4부위: 밤에 힘 I + 저항 I, 밤 몬스터 피해 25% 감소"
SET_MYTH = "2부위: 성급함 I\n4부위: 성급함 II + 신속 I"
SET_JADE = "2부위: 행운 I\n4부위: 행운 II + 얻는 경험치 1.5배\n인챈트가 가장 잘 붙음"

LANG_KO = {
    "tooltip.wildplus.bloodsteel_armor": SET_BLOOD,
    "tooltip.wildplus.bloodsteel_sword": "흡혈: 입힌 피해의 20%만큼 회복",
    "tooltip.wildplus.bloodsteel_pickaxe": "밤·블러드문에 채굴 속도 1.5배",
    "tooltip.wildplus.bloodsteel_axe": "밤·블러드문에 채굴 속도 1.5배",
    "tooltip.wildplus.bloodsteel_shovel": "밤·블러드문에 채굴 속도 1.5배",
    "tooltip.wildplus.mythril_sword": "냉기: 20% 확률로 적에게 둔화 II (2초)",
    "tooltip.wildplus.mythril_pickaxe": "자동 제련: 철·금·구리·미스릴 원석이 주괴로 나옴",
    "tooltip.wildplus.mythril_axe": "웅크리고 벌목 시 나무를 256개까지 한 번에",
    "tooltip.wildplus.mythril_shovel": "3×3 파기 (웅크리면 한 칸)",
    "tooltip.wildplus.mythril_hoe": "3×3 경작 (웅크리면 한 칸)",
    "tooltip.wildplus.jade_sword": "행운의 칼날: 몬스터 처치 시 50% 확률로 동화 1~3개",
    "tooltip.wildplus.jade_pickaxe": "광석을 캘 때 25% 확률로 두 배",
    "tooltip.wildplus.jade_axe": "원목 20% 추가 · 가끔 사과",
    "tooltip.wildplus.jade_shovel": "흙·모래·자갈에서 가끔 보물 (부싯돌·점토·동화·비취…)",
    "tooltip.wildplus.jade_hoe": "다 자란 작물 수확량 +1 (우클릭 수확 포함)",
    "enchantment.wildplus.armor_piercing": "방어구 관통",
    "gui.wildplus.warp.rename": "이름 바꾸기",
    "gui.wildplus.warp.rename_hint": "새 이름 입력",
    "gui.wildplus.public": "공개",
    "gui.wildplus.private": "비공개",
    "container.wildplus.shipping_bin": "출하 상자 · %s · 내일 아침 예상 %s",
    "message.wildplus.shipping.public": "출하 상자를 공개로 바꿨어요 (누구나 넣고 꺼낼 수 있음)",
    "message.wildplus.shipping.private": "출하 상자를 비공개로 바꿨어요 (나만 열 수 있음)",
    "message.wildplus.shipping.locked": "다른 사람의 비공개 출하 상자예요",
    "tooltip.wildplus.shipping_bin": "넣어 둔 물건이 매일 아침 팔리고 대금은 은행 통장으로\n웅크리고 우클릭: 공개 / 비공개 바꾸기 (기본 비공개)",
    "commands.wildplus.guide": "제작 안내서를 받았어요",
    "commands.wildplus.cookbook": "요리책을 받았어요",
    "commands.wildplus.warp_renamed": "워프 이름을 [%s] (으)로 바꿨어요",
    "commands.wildplus.warp_rename_fail": "내 워프 포인트 창을 연 뒤에 바꿀 수 있어요",
    "hud.wildplus.money": "소지금 %s동",
}
for a in ("helmet", "chestplate", "leggings", "boots"):
    LANG_KO[f"tooltip.wildplus.mythril_{a}"] = SET_MYTH
    LANG_KO[f"tooltip.wildplus.jade_{a}"] = SET_JADE

LANG_EN = {
    "tooltip.wildplus.bloodsteel_armor": "2 pieces: Strength I at night\n4 pieces: + Resistance I at night, 25% less monster damage at night",
    "tooltip.wildplus.bloodsteel_sword": "Lifesteal: heal 20% of damage dealt",
    "tooltip.wildplus.bloodsteel_pickaxe": "Mines 1.5x faster at night",
    "tooltip.wildplus.bloodsteel_axe": "Mines 1.5x faster at night",
    "tooltip.wildplus.bloodsteel_shovel": "Mines 1.5x faster at night",
    "tooltip.wildplus.mythril_sword": "Frost: 20% chance to apply Slowness II",
    "tooltip.wildplus.mythril_pickaxe": "Auto-smelt: raw ores drop as ingots",
    "tooltip.wildplus.mythril_axe": "Tree felling up to 256 logs",
    "tooltip.wildplus.mythril_shovel": "Digs 3x3 (sneak for one block)",
    "tooltip.wildplus.mythril_hoe": "Tills 3x3 (sneak for one block)",
    "tooltip.wildplus.jade_sword": "Lucky blade: 50% chance for 1-3 copper coins on kill",
    "tooltip.wildplus.jade_pickaxe": "25% chance for double ore drops",
    "tooltip.wildplus.jade_axe": "20% extra logs, sometimes apples",
    "tooltip.wildplus.jade_shovel": "Sometimes digs up treasure",
    "tooltip.wildplus.jade_hoe": "+1 crop yield when harvesting",
    "enchantment.wildplus.armor_piercing": "Armor Piercing",
    "gui.wildplus.warp.rename": "Rename",
    "gui.wildplus.warp.rename_hint": "New name",
    "gui.wildplus.public": "Public",
    "gui.wildplus.private": "Private",
    "container.wildplus.shipping_bin": "Shipping Bin · %s · tomorrow %s",
    "message.wildplus.shipping.public": "Shipping bin is now public",
    "message.wildplus.shipping.private": "Shipping bin is now private",
    "message.wildplus.shipping.locked": "This is someone else's private shipping bin",
    "tooltip.wildplus.shipping_bin": "Contents sell every morning; money goes to your bank\nSneak + right-click: toggle public/private",
    "commands.wildplus.guide": "You received the guidebook",
    "commands.wildplus.cookbook": "You received the cookbook",
    "commands.wildplus.warp_renamed": "Warp renamed to [%s]",
    "commands.wildplus.warp_rename_fail": "Open your warp point first",
    "hud.wildplus.money": "Cash %s",
}
for a in ("helmet", "chestplate", "leggings", "boots"):
    LANG_EN[f"tooltip.wildplus.mythril_{a}"] = "2 pieces: Haste I\n4 pieces: Haste II + Speed I"
    LANG_EN[f"tooltip.wildplus.jade_{a}"] = "2 pieces: Luck I\n4 pieces: Luck II + 1.5x experience"


def enchant():
    # 방어구 관통: 검·도끼용. 상대 방어구 효과를 레벨당 10% 무시 (바닐라 최대 IV, 강화대로 VI 까지)
    write(DATA / "enchantment/armor_piercing.json", {
        "anvil_cost": 4,
        "description": {"translate": "enchantment.wildplus.armor_piercing"},
        "effects": {"minecraft:armor_effectiveness": [{"effect": {"type": "minecraft:add", "value": {
            "type": "minecraft:linear", "base": -0.1, "per_level_above_first": -0.1}}}]},
        "max_cost": {"base": 50, "per_level_above_first": 10},
        "max_level": 4,
        "min_cost": {"base": 10, "per_level_above_first": 10},
        "primary_items": "#minecraft:enchantable/sword",
        "slots": ["mainhand"],
        "supported_items": "#minecraft:enchantable/sharp_weapon",
        "weight": 3,
    })
    for tag in ("in_enchanting_table", "non_treasure", "tradeable", "on_random_loot", "on_traded_equipment"):
        merge_tag(MC / f"tags/enchantment/{tag}.json", [i("armor_piercing")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    enchant()
