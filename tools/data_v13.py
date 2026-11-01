"""14차 데이터: 주말 콘텐츠 큰 블록 3종(레이드 제단 · 하늘의 탑 입구 · 주간 목표 게시판), 레이드 전리품, 언어."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write

LANG_KO = {
    "block.wildplus.raid_altar": "레이드 제단",
    "block.wildplus.tower_gate": "하늘의 탑 입구",
    "block.wildplus.goal_board": "주간 목표 게시판",
    "block.wildplus.big_part": "큰 블록 부속",
    "container.wildplus.raid_altar": "주말 레이드",
    "container.wildplus.tower_gate": "하늘의 탑",
    "container.wildplus.goal_board": "주간 목표 · 시즌 패스",
    "item.wildplus.titan_core": "거신의 핵",
    "item.wildplus.lich_orb": "망령 군주의 구슬",
    "item.wildplus.frost_crown": "서리 왕관 조각",
    "tooltip.wildplus.raid_altar": "3×3 크기 · 친구들과 함께 이번 주 레이드 보스에 도전\n보스는 게임 속 7일마다 바뀌고, 깰 때마다 강해짐",
    "tooltip.wildplus.tower_gate": "폭 3 · 높이 3 · 근처 친구들과 하늘의 탑에 오름\n층마다 보상, 쓰러져도 아이템 유지",
    "tooltip.wildplus.goal_board": "폭 3 · 높이 2 · 이번 주 공동 목표와 시즌 패스 보상",
    "tooltip.wildplus.titan_core": "대지의 거신 레이드 전리품 · 은행 상인이 1,000동에 사 줌",
    "tooltip.wildplus.lich_orb": "망령 군주 레이드 전리품 · 은행 상인이 1,000동에 사 줌",
    "tooltip.wildplus.frost_crown": "서리 여왕 레이드 전리품 · 은행 상인이 1,000동에 사 줌",
}
LANG_EN = {
    "block.wildplus.raid_altar": "Raid Altar",
    "block.wildplus.tower_gate": "Sky Tower Gate",
    "block.wildplus.goal_board": "Weekly Goal Board",
    "block.wildplus.big_part": "Large Block Part",
    "container.wildplus.raid_altar": "Weekend Raid",
    "container.wildplus.tower_gate": "Sky Tower",
    "container.wildplus.goal_board": "Weekly Goals · Season Pass",
    "item.wildplus.titan_core": "Titan Core",
    "item.wildplus.lich_orb": "Wraith Lord's Orb",
    "item.wildplus.frost_crown": "Frost Crown Shard",
    "tooltip.wildplus.raid_altar": "3×3 · Fight this week's raid boss with friends\nThe boss changes every Friday and grows stronger with each clear",
    "tooltip.wildplus.tower_gate": "3 wide · 3 tall · Climb the Sky Tower with nearby friends\nRewards every floor, keep your items if you fall",
    "tooltip.wildplus.goal_board": "3 wide · 2 tall · Weekly shared goals and season pass rewards",
    "tooltip.wildplus.titan_core": "Titan raid trophy · the banker buys it for 1,000",
    "tooltip.wildplus.lich_orb": "Wraith Lord raid trophy · the banker buys it for 1,000",
    "tooltip.wildplus.frost_crown": "Frost Queen raid trophy · the banker buys it for 1,000",
}


def box(frm, to, tex, faces=None, rot=None, uv=None, tint=None):
    """육면 상자. tex: 한 텍스처 또는 {"side":..,"up":..,"down":..} 같은 면별 텍스처"""
    fs = {}
    names = faces or ["north", "south", "east", "west", "up", "down"]
    for f in names:
        t = tex if isinstance(tex, str) else tex.get(f, tex.get("side"))
        face = {"texture": t}
        if uv and f in uv:
            face["uv"] = uv[f]
        fs[f] = face
    e = {"from": frm, "to": to, "faces": fs}
    if rot:
        e["rotation"] = rot
    return e


def plane_x(frm, to, tex, angle):
    """십자 모양 평면 (불꽃 등)"""
    return {"from": frm, "to": to,
            "rotation": {"origin": [(frm[0] + to[0]) / 2, frm[1], (frm[2] + to[2]) / 2], "axis": "y", "angle": angle, "rescale": True},
            "faces": {"north": {"texture": tex}, "south": {"texture": tex}}}


def display(scale):
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [scale, scale, scale]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [scale * 0.8] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [scale * 1.2] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [scale * 0.7] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
    }


def rotated_states(name, model):
    return {"variants": {f"facing={d}": ({"model": model, "y": r} if r else {"model": model})
                         for d, r in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}}


def raid_altar():
    T = {
        "bricks": "minecraft:block/polished_blackstone_bricks", "stone": "minecraft:block/polished_blackstone",
        "chisel": "minecraft:block/chiseled_polished_blackstone", "gilded": "minecraft:block/gilded_blackstone",
        "gold": "minecraft:block/gold_block", "crying": "minecraft:block/crying_obsidian", "magma": "minecraft:block/magma",
        "runes": i("block/raid_altar_top"), "particle": "minecraft:block/polished_blackstone_bricks",
    }
    el = [
        # 1단 바닥 (3×3)
        box([-16, 0, -16], [32, 4, 32], {"side": "#bricks", "up": "#stone", "down": "#stone"}),
        # 2단
        box([-11, 4, -11], [27, 7, 27], {"side": "#chisel", "up": "#stone", "down": "#stone"}),
        # 앞 계단 (북쪽)
        box([-3, 4, -14], [19, 5, -11], {"side": "#bricks", "up": "#stone", "down": "#stone"}),
        box([-3, 5, -12], [19, 6, -11], {"side": "#bricks", "up": "#stone", "down": "#stone"}),
        # 가운데 제단 + 금판 + 룬
        box([2, 7, 2], [14, 13, 14], {"side": "#chisel", "up": "#stone", "down": "#stone"}),
        box([1, 13, 1], [15, 15, 15], {"side": "#gold", "up": "#runes", "down": "#gold"}),
        # 떠 있는 수정 (45도)
        box([5.5, 17, 5.5], [10.5, 29, 10.5], "#crying", rot={"origin": [8, 23, 8], "axis": "y", "angle": 45}),
    ]
    # 네 모서리 기둥 + 금 화로 + 영혼불
    for (cx, cz) in [(-14, -14), (26, -14), (-14, 26), (26, 26)]:
        el.append(box([cx, 4, cz], [cx + 4, 18, cz + 4], {"side": "#bricks", "up": "#stone", "down": "#stone"}))
        el.append(box([cx - 1, 18, cz - 1], [cx + 5, 20, cz + 5], {"side": "#gilded", "up": "#gold", "down": "#gold"}))
        el.append(box([cx + 0.5, 20, cz + 0.5], [cx + 3.5, 23, cz + 3.5], "#magma"))
    write(ASSETS / "models/block/raid_altar.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                    "textures": T, "elements": el, "display": display(0.2)})
    write(ASSETS / "blockstates/raid_altar.json", rotated_states("raid_altar", i("block/raid_altar")))


def tower_gate():
    T = {"pillar": "minecraft:block/quartz_block_side", "ptop": "minecraft:block/quartz_block_top", "chisel": "minecraft:block/chiseled_quartz_block",
         "smooth": "minecraft:block/quartz_block_bottom", "bricks": "minecraft:block/quartz_bricks", "portal": i("block/sky_portal"),
         "gold": "minecraft:block/gold_block", "ice": "minecraft:block/blue_ice", "diamond": "minecraft:block/diamond_block",
         "particle": "minecraft:block/quartz_bricks"}
    el = [
        # 받침 + 바닥
        box([-16, 0, -1], [-4, 3, 17], {"side": "#chisel", "up": "#smooth", "down": "#smooth"}),
        box([20, 0, -1], [32, 3, 17], {"side": "#chisel", "up": "#smooth", "down": "#smooth"}),
        box([-4, 0, 0], [20, 1, 16], {"side": "#smooth", "up": "#bricks", "down": "#smooth"}),
        # 기둥 둘
        box([-15, 3, 1], [-5, 32, 15], {"side": "#pillar", "up": "#ptop", "down": "#ptop"}),
        box([21, 3, 1], [31, 32, 15], {"side": "#pillar", "up": "#ptop", "down": "#ptop"}),
        # 금테 고리
        box([-16, 14, 0], [-4, 16, 16], "#gold"),
        box([20, 14, 0], [32, 16, 16], "#gold"),
        # 하늘빛 차원문 (반투명, 앞뒤만)
        {"from": [-5, 1, 7.5], "to": [21, 32, 8.5],
         "faces": {"north": {"texture": "#portal"}, "south": {"texture": "#portal"}}},
    ]
    write(ASSETS / "models/block/tower_gate.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                    "textures": T, "elements": el, "display": display(0.22)})
    top = [
        # 상인방 + 금테 + 지붕 계단 + 다이아 꼭지
        box([-5, 0, 7.5], [21, 6, 8.5], {"north": "#portal", "south": "#portal"}, faces=["north", "south"]),
        box([-16, 6, -1], [32, 11, 17], {"side": "#bricks", "up": "#smooth", "down": "#smooth"}),
        box([-16, 11, 0], [32, 12, 16], "#gold"),
        box([-15, 0, 1], [-5, 6, 15], {"side": "#pillar", "up": "#ptop", "down": "#ptop"}),
        box([21, 0, 1], [31, 6, 15], {"side": "#pillar", "up": "#ptop", "down": "#ptop"}),
        box([-10, 12, 2], [26, 14, 14], {"side": "#chisel", "up": "#smooth", "down": "#smooth"}),
        box([-2, 14, 3], [18, 16, 13], {"side": "#chisel", "up": "#smooth", "down": "#smooth"}),
        box([4, 16, 5], [12, 18, 11], "#ice"),
        box([6, 18, 6], [10, 26, 10], "#diamond", rot={"origin": [8, 22, 8], "axis": "y", "angle": 45}),
    ]
    write(ASSETS / "models/block/tower_gate_top.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                        "textures": T, "elements": top})
    write(ASSETS / "blockstates/tower_gate.json", rotated_states("tower_gate", i("block/tower_gate")))
    write(ASSETS / "models/item/tower_gate.json", {"parent": i("block/tower_gate"), "display": display(0.22)})


def goal_board():
    T = {"log": "minecraft:block/stripped_spruce_log", "plank": "minecraft:block/spruce_planks", "dark": "minecraft:block/dark_oak_planks",
         "board": i("block/goal_board"), "lantern": "minecraft:block/lantern", "particle": "minecraft:block/spruce_planks"}
    el = [
        # 기둥 둘 + 받침
        box([-16, 0, 11], [-12, 32, 15], "#log"),
        box([28, 0, 11], [32, 32, 15], "#log"),
        box([-16, 0, 9], [-11, 2, 16], "#dark"),
        box([27, 0, 9], [32, 2, 16], "#dark"),
        # 아래 가로대 · 위 지붕
        box([-12, 4, 11], [28, 7, 15], "#plank"),
        box([-16, 28, 9], [32, 31, 16], {"side": "#dark", "up": "#plank", "down": "#dark"}),
        box([-15, 31, 11], [31, 32, 15], "#plank"),
        # 코르크 판 (앞면에 게시판 그림)
        box([-12, 7, 12], [28, 28, 14], {"side": "#plank", "north": "#board", "up": "#plank", "down": "#plank"},
            uv={"north": [0, 0, 16, 8.4]}),
        # 매달린 등불 둘
        box([-11, 22, 8], [-7, 26, 12], "#lantern", uv={"north": [0, 2, 6, 9], "south": [0, 2, 6, 9], "east": [0, 2, 6, 9],
                                                        "west": [0, 2, 6, 9], "up": [0, 9, 6, 15], "down": [0, 9, 6, 15]}),
        box([23, 22, 8], [27, 26, 12], "#lantern", uv={"north": [0, 2, 6, 9], "south": [0, 2, 6, 9], "east": [0, 2, 6, 9],
                                                       "west": [0, 2, 6, 9], "up": [0, 9, 6, 15], "down": [0, 9, 6, 15]}),
    ]
    write(ASSETS / "models/block/goal_board.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                    "textures": T, "elements": el, "display": display(0.24)})
    write(ASSETS / "blockstates/goal_board.json", rotated_states("goal_board", i("block/goal_board")))


def big_part():
    write(ASSETS / "models/block/big_part_hidden.json", {"textures": {"particle": "minecraft:block/polished_blackstone"}, "elements": []})
    states = {}
    for d, r in {"north": 0, "east": 90, "south": 180, "west": 270}.items():
        states[f"facing={d},part=hidden"] = {"model": i("block/big_part_hidden")}
        states[f"facing={d},part=gate_top"] = {"model": i("block/tower_gate_top"), **({"y": r} if r else {})}
    write(ASSETS / "blockstates/big_part.json", {"variants": states})


def items():
    write(ASSETS / "items/raid_altar.json", {"model": {"type": "minecraft:model", "model": i("block/raid_altar")}})
    write(ASSETS / "items/goal_board.json", {"model": {"type": "minecraft:model", "model": i("block/goal_board")}})
    write(ASSETS / "items/tower_gate.json", {"model": {"type": "minecraft:model", "model": i("item/tower_gate")}})
    for n in ["titan_core", "lich_orb", "frost_crown"]:
        write(ASSETS / f"models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{n}")}})
        write(ASSETS / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{n}")}})


def data():
    for b in ["raid_altar", "tower_gate", "goal_board"]:
        write(DATA / f"loot_table/blocks/{b}.json", self_loot(b))
    rec = {
        "raid_altar": shaped(["GRG", "OEO", "OOO"], {"G": "minecraft:gold_block", "R": i("ruby"), "E": "minecraft:ender_eye",
                                                       "O": "minecraft:obsidian"}, i("raid_altar"), category="misc"),
        "tower_gate": shaped(["QDQ", "QEQ", "Q Q"], {"Q": "minecraft:quartz_block", "D": "minecraft:diamond",
                                                       "E": "minecraft:ender_pearl"}, i("tower_gate"), category="misc"),
        "goal_board": shaped(["PPP", "PBP", "L L"], {"P": "minecraft:spruce_planks", "B": "minecraft:book",
                                                       "L": "minecraft:stripped_spruce_log"}, i("goal_board"), category="misc"),
    }
    for name, obj in rec.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    t = MC / "tags"
    merge_tag(t / "block/mineable/pickaxe.json", [i("raid_altar"), i("tower_gate"), i("big_part")])
    merge_tag(t / "block/mineable/axe.json", [i("goal_board")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    raid_altar()
    tower_gate()
    goal_board()
    big_part()
    items()
    data()
