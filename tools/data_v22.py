"""3.2 데이터: 식당 (카운터 · 입구 · 식탁 · 의자 · 간판) + 손님 피부 + 마을 식당 알바."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
import customer_skins
import kitchen_models
import restaurant_design as R
from cosmetic_design import Box

BLOCKS = ["restaurant_counter", "restaurant_entrance", "dining_table", "dining_chair", "restaurant_sign"]


def sign_with_marker():
    # 간판은 벽 쪽(뒤)에 붙도록: 앞쪽 끝에 보이지 않는 점을 둬서 가운데 맞춤이 칸 전체를 기준으로 되게
    return R.sign() + [Box(-1, 0, 8, 0.1, 0.1, 0.1, lambda *a: None)]


MODELS = {
    "restaurant_counter": (R.counter, -1, 1, 0, 0, 2),
    "restaurant_entrance": (R.entrance, 0, 0, 0, 0, 1),
    "dining_table": (R.table, 0, 1, 0, 0, 1),
    "dining_chair": (R.chair, 0, 0, 0, 0, 1),
    "restaurant_sign": (sign_with_marker, 0, 1, 0, 0, 2),
}

RECIPES = {
    "restaurant_counter": shaped(["QQQ", "PBP", "PCP"], {"Q": "minecraft:quartz_slab", "P": "#minecraft:planks", "B": "minecraft:bell",
                                                         "C": "minecraft:chest"}, i("restaurant_counter"), category="misc"),
    "restaurant_entrance": shaped(["L  ", "S  ", "RRR"], {"L": "minecraft:lantern", "S": "minecraft:stick", "R": "minecraft:red_carpet"},
                                  i("restaurant_entrance"), category="misc"),
    "dining_table": shaped(["RWR", "PPP", "S S"], {"R": "minecraft:red_carpet", "W": "minecraft:white_carpet", "P": "#minecraft:planks",
                                                   "S": "minecraft:stick"}, i("dining_table"), category="misc"),
    "dining_chair": shaped(["P  ", "PWP", "S S"], {"P": "#minecraft:planks", "W": "minecraft:red_wool", "S": "minecraft:stick"},
                           i("dining_chair"), 2, category="misc"),
    "restaurant_sign": shaped(["GPG", "PSP", "GPG"], {"G": "minecraft:gold_nugget", "P": "#minecraft:planks", "S": "#minecraft:signs"},
                              i("restaurant_sign"), category="misc"),
}

LANG_KO = {
    "block.wildplus.restaurant_counter": "식당 카운터",
    "tooltip.wildplus.restaurant_counter": "가로 3칸 · 진열대에 요리를 채우면 손님이 입구로 들어와 주문 → [내주기]로 서빙\n"
                                          "근처 16칸에 식당 입구 · 의자가 있어야 함 · 모루로 이름 붙이면 식당 이름 · 알바 · 직원 · 평점",
    "block.wildplus.restaurant_entrance": "식당 입구",
    "tooltip.wildplus.restaurant_entrance": "손님이 여기서 나타나 들어오고 여기로 나감 (카운터 16칸 안) · 밟고 지나갈 수 있음",
    "block.wildplus.dining_table": "식탁",
    "tooltip.wildplus.dining_table": "가로 2칸 · 체크 식탁보와 꽃병 · 옆에 의자를 놓아 주세요",
    "block.wildplus.dining_chair": "식당 의자",
    "tooltip.wildplus.dining_chair": "요리를 받은 손님이 앉아서 먹음 (♥ · 평판 +1) · 의자가 없으면 포장해 감",
    "block.wildplus.restaurant_sign": "식당 간판",
    "tooltip.wildplus.restaurant_sign": "벽에 거는 가로 2칸 간판 (장식)",
    "entity.wildplus.customer": "식당 손님",
}
LANG_EN = {
    "block.wildplus.restaurant_counter": "Restaurant Counter", "block.wildplus.restaurant_entrance": "Restaurant Entrance",
    "block.wildplus.dining_table": "Dining Table", "block.wildplus.dining_chair": "Dining Chair",
    "block.wildplus.restaurant_sign": "Restaurant Sign", "entity.wildplus.customer": "Restaurant Guest",
}


def build(lang_ko, lang_en):
    customer_skins.build()
    for name, cfg in MODELS.items():
        kitchen_models.export(name, *cfg)
    for b in BLOCKS:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    for name, obj in RECIPES.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    merge_tag(MC / "tags/block/mineable/axe.json", [i(b) for b in BLOCKS])
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
