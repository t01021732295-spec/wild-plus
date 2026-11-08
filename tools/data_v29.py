"""3.9 데이터 (data_v29): 활 약탈 인챈트 · 행운의 부적 2~10단계 (모델 · 텍스처 · 9개 합성 레시피 · 언어)."""
import json

from PIL import Image

from data_v2 import ASSETS, DATA, MC, i, merge_tag, shaped, write
from data_v18 import simple

TIER_INFO = [
    None,
    "행운 I",
    "행운 II",
    "행운 II · 경험치 +20%",
    "행운 III · 경험치 +20% · 몬스터 동전 +50%",
    "행운 III · 경험치 +20% · 몬스터 동전 +50% · 행운의 일격 (7% 피해 2배)",
    "행운 IV · …5단계 효과 · 광석 두 배 (8%)",
    "행운 IV · …6단계 효과 · 물고기 두 배 (15%)",
    "행운 V · …7단계 효과 · 기적 (죽을 때 한 번 버팀, 10분)",
    "행운 V · …8단계 효과 · 정예 보상 두 배",
    "행운 VI · 모든 효과 · 황금 오라 (32칸 안 친구 행운 II)",
]
ROMAN = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"]
# 단계별 보석 색 (밝음 · 어두움) · 테두리 (밝음 · 중간 · 어두움)
GEM = [None, ((108, 193, 74), (62, 138, 42)), ((110, 230, 220), (40, 150, 150)), ((110, 160, 255), (40, 70, 190)),
       ((200, 120, 255), (120, 50, 190)), ((255, 140, 200), (190, 60, 130)), ((255, 90, 90), (170, 20, 30)),
       ((255, 170, 60), (200, 90, 10)), ((240, 250, 255), (150, 200, 230)), ((150, 60, 220), (40, 10, 70)), ((255, 255, 255), (255, 200, 60))]
GOLD = ((255, 224, 102), (217, 162, 30), (122, 90, 16))
PLATINUM = ((245, 248, 255), (190, 200, 215), (110, 118, 135))
ROSE = ((255, 200, 190), (225, 140, 130), (130, 70, 70))
FRAME = [None, GOLD, GOLD, GOLD, GOLD, GOLD, ROSE, ROSE, PLATINUM, PLATINUM, GOLD]


def ship(t):
    return min(4_000_000, 200 * 9 ** (t - 1))  # 3.9.4: 처음 값의 20%


def charm_textures():
    tex = ASSETS / "textures/item"
    base = Image.open(tex / "lucky_charm.png").convert("RGBA")
    rainbow = [(255, 80, 80), (255, 170, 60), (255, 240, 80), (100, 220, 100), (80, 160, 255), (180, 100, 255)]
    for t in range(2, 11):
        im = base.copy()
        p = im.load()
        g_hi, g_lo = GEM[t]
        f_hi, f_mid, f_lo = FRAME[t]
        swap = {(108, 193, 74): g_hi, (62, 138, 42): g_lo, GOLD[0]: f_hi, GOLD[1]: f_mid, GOLD[2]: f_lo}
        k = 0
        for y in range(16):
            for x in range(16):
                c = p[x, y]
                if c[3] and c[:3] in swap:
                    col = swap[c[:3]]
                    if t == 10 and c[:3] in ((108, 193, 74), (62, 138, 42)):
                        col = rainbow[k % len(rainbow)]
                        k += 1
                    p[x, y] = col + (255,)
        # 단계가 높을수록 반짝이 점
        spark = [(3, 7), (12, 7), (2, 11), (13, 11), (7, 15), (4, 14), (11, 14), (1, 9), (14, 9)]
        for (x, y) in spark[: max(0, t - 4) + (2 if t == 10 else 0)]:
            if p[x, y][3] == 0:
                p[x, y] = ((255, 250, 200) if t != 10 else rainbow[(x + y) % 6]) + (255,)
        im.save(tex / f"lucky_charm_{t}.png")


LANG_KO = {
    "enchantment.wildplus.bow_looting": "활 약탈",
    "item.wildplus.lucky_charm": "행운의 부적 I",
    "tooltip.wildplus.lucky_charm": f"가지고 있으면: {TIER_INFO[1]}\n9개를 제작대에 채우면 II단계 · 출하 {ship(1):,}동 · 가방 속 가장 높은 단계 하나만 적용",
}
LANG_EN = {"enchantment.wildplus.bow_looting": "Bow Looting", "item.wildplus.lucky_charm": "Lucky Charm I"}
for t in range(2, 11):
    LANG_KO[f"item.wildplus.lucky_charm_{t}"] = f"행운의 부적 {ROMAN[t]}"
    nxt = f"9개를 제작대에 채우면 {ROMAN[t + 1]}단계 · " if t < 10 else "최고 단계 · "
    LANG_KO[f"tooltip.wildplus.lucky_charm_{t}"] = f"가지고 있으면: {TIER_INFO[t]}\n{nxt}출하 {ship(t):,}동 · 가방 속 가장 높은 단계 하나만 적용"
    LANG_EN[f"item.wildplus.lucky_charm_{t}"] = f"Lucky Charm {ROMAN[t]}"


def build(lang_ko, lang_en):
    # 활 약탈: 활 전용 약탈 (드롭 수는 자바 Bows.onArrowKillDrops, 장비 떨굼 확률은 바닐라 약탈과 같은 방식)
    write(DATA / "enchantment/bow_looting.json", {
        "anvil_cost": 4,
        "description": {"translate": "enchantment.wildplus.bow_looting"},
        "effects": {"minecraft:equipment_drops": [{
            "effect": {"type": "minecraft:add", "value": {"type": "minecraft:linear", "base": 0.01, "per_level_above_first": 0.01}},
            "enchanted": "attacker",
            "requirements": {"type": "minecraft:entity_properties", "entity": "attacker", "predicate": {"minecraft:entity_type": "minecraft:player"}},
        }]},
        "max_cost": {"base": 65, "per_level_above_first": 9},
        "max_level": 3,
        "min_cost": {"base": 15, "per_level_above_first": 9},
        "slots": ["mainhand", "offhand"],
        "supported_items": "#minecraft:enchantable/bow",
        "weight": 2,
    })
    for tag in ("in_enchanting_table", "non_treasure", "tradeable", "on_random_loot", "on_traded_equipment"):
        merge_tag(MC / f"tags/enchantment/{tag}.json", [i("bow_looting")])
    # 행운의 부적 단계
    charm_textures()
    for t in range(2, 11):
        simple(f"lucky_charm_{t}")
        prev = i("lucky_charm") if t == 2 else i(f"lucky_charm_{t - 1}")
        write(DATA / f"recipe/lucky_charm_{t}.json", shaped(["CCC", "CCC", "CCC"], {"C": prev}, i(f"lucky_charm_{t}"), category="misc"))
    ko = {**lang_ko, **LANG_KO}
    for r in ("stellar_ruby", "sky_sapphire", "royal_emerald", "dragon"):
        k = f"tooltip.wildplus.{r}_fishing_rod"
        if k in ko and "내구도 무한" not in ko[k]:
            ko[k] = ko[k] + "\n내구도 무한"
    write(ASSETS / "lang/ko_kr.json", ko)
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v29")
