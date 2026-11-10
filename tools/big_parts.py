"""3.5: 구조물 안 큰 블록(가구 · 기계)의 부속 칸 자리를 구조물에서 빼 둠 → 구조물을 놓을 때 빈 칸이 부속 칸을 지우지 않음.
크기는 자바 BigBlock 과 같게: 이름 → (좌우 lat0, lat1, 앞뒤 dep0, dep1, 높이). 좌우 + 방향 = facing 의 시계 방향, 앞뒤 + = 뒤쪽."""
DIMS = {
    # 3.3 가구 (FurnitureBlock.Kind)
    "sofa": (-1, 1, 0, 0, 1), "armchair": (0, 0, 0, 0, 1), "coffee_table": (0, 0, 0, 0, 1), "fireplace": (0, 1, 0, 0, 2), "tv_stand": (0, 1, 0, 0, 2),
    "floor_lamp": (0, 0, 0, 0, 1), "monstera": (0, 0, 0, 0, 1), "rug": (-1, 1, 0, 1, 1), "wardrobe": (0, 1, 0, 0, 2), "vanity": (0, 1, 0, 0, 2),
    "nightstand": (0, 0, 0, 0, 1), "fridge": (0, 0, 0, 0, 2), "sink": (0, 0, 0, 0, 1), "bookshelf": (0, 1, 0, 0, 2), "desk": (0, 1, 0, 0, 1),
    "grandfather_clock": (0, 0, 0, 0, 2), "piano": (0, 1, 0, 0, 2), "aquarium": (0, 1, 0, 0, 2), "chandelier": (0, 0, 0, 0, 1), "bathtub": (0, 1, 0, 0, 1),
    # 조리 · 식당
    "cooking_pot": (0, 1, 0, 0, 2), "brick_oven": (0, 1, 0, 1, 2), "bbq_grill": (0, 1, 0, 0, 2), "prep_table": (0, 1, 0, 0, 2), "kitchen_stove": (0, 0, 0, 0, 2),
    "aging_rack": (0, 1, 0, 0, 2), "dining_table": (0, 1, 0, 0, 1),
    # 3.4 목장 · 대회
    "hay_trough": (-1, 0, 0, 0, 1), "butter_churn": (0, 0, 0, 0, 2), "spinning_wheel": (0, 0, 0, 0, 2), "contest_board": (-1, 0, 0, 0, 2), "podium": (-1, 1, 0, 0, 1),
    # 3.5 경마장 · 마을
    "stable": (-1, 0, 0, 0, 2), "betting_kiosk": (0, 0, 0, 0, 2), "starting_gate": (-2, 1, 0, 0, 2), "scoreboard": (-1, 1, 0, 0, 2), "finish_post": (0, 0, 0, 0, 2),
    "project_board": (-1, 1, 0, 0, 2), "goal_board": (-1, 1, 0, 0, 2), "atm_tall": (0, 0, 0, 0, 2), "music_player": (-1, 1, 0, 0, 2), "market_stall": (-1, 1, 0, 0, 4), "lottery_machine": (0, 0, 0, 0, 3), "fate_altar": (0, 0, 0, 0, 2), "pharmacy_rack": (-1, 1, 0, 0, 3),
}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def void_parts(t):
    sx, sy, sz = t.size
    for (x, y, z), (b, props, nbt) in list(t.blocks.items()):
        if not b.startswith("wildplus:"):
            continue
        n = b.split(":")[1]
        n = "sofa" if n.startswith("sofa_") else n
        if n not in DIMS or not props or "facing" not in props:
            continue
        l0, l1, d0, d1, h = DIMS[n]
        r, bk = STEP[CW[props["facing"]]], STEP[OPP[props["facing"]]]
        for yy in range(h):
            for l in range(l0, l1 + 1):
                for d in range(d0, d1 + 1):
                    if yy == 0 and l == 0 and d == 0:
                        continue
                    c = (x + r[0] * l + bk[0] * d, y + yy, z + r[1] * l + bk[1] * d)
                    if 0 <= c[0] < sx and 0 <= c[1] < sy and 0 <= c[2] < sz and t.blocks.get(c, ("minecraft:air",))[0] == "minecraft:air":
                        del t.blocks[c]  # 구조물에서 빼 두면 놓을 때 그 칸을 건드리지 않음
    return t
