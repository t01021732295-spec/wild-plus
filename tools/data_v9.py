"""10차 데이터: 현실적인 블록 모델, 명예의 전당, 핏빛 군주, 정예 특수 능력, 새 상인 3종."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, shaped, write
from data_v4 import facing_states

LANG_KO = {
    "block.wildplus.hall_of_fame": "명예의 전당",
    "container.wildplus.hall_of_fame": "명예의 전당",
    "item.wildplus.lord_heart": "핏빛 군주의 심장",
    "entity.wildplus.blood_lord": "핏빛 군주",
    "entity.wildplus.farmer_merchant": "농부 상인",
    "entity.wildplus.fisher_merchant": "어부 상인",
    "entity.wildplus.banker_merchant": "경제 상인",
    "item.wildplus.farmer_merchant_spawn_egg": "농부 상인 생성 알",
    "item.wildplus.fisher_merchant_spawn_egg": "어부 상인 생성 알",
    "item.wildplus.banker_merchant_spawn_egg": "경제 상인 생성 알",
    "affix.wildplus.burning": "화염",
    "affix.wildplus.vampiric": "흡혈",
    "affix.wildplus.thorns": "가시",
    "affix.wildplus.swift": "질풍",
    "affix.wildplus.giant": "거인",
    "affix.wildplus.venom": "맹독",
    "affix.wildplus.summoner": "소환사",
    "affix.wildplus.ironclad": "철벽",
    "affix.wildplus.volatile": "폭발",
    "message.wildplus.blood_lord.near": "블러드문 정예 처치 %s/%s … 무언가가 깨어나려 한다",
    "message.wildplus.blood_lord.spawn": "☠ 핏빛 군주가 %s 근처에 나타났다!",
    "message.wildplus.blood_lord.rage": "핏빛 군주가 광폭해졌다!",
    "message.wildplus.blood_lord.dead": "★ %s 님이 핏빛 군주를 쓰러뜨렸다!",
    "message.wildplus.honor.no_money": "돈이 부족해요 (필요: %s동, 통장+현금)",
    "message.wildplus.honor.rank_up": "★ %s 님이 명예 등급 %s 에 올랐습니다!",
    "message.wildplus.honor.flight": "비행 허가증 사용! 남은 비행 시간 %s분",
    "message.wildplus.honor.flight_left": "비행 시간이 %s초 남았어요",
    "message.wildplus.honor.flight_end": "비행 시간이 끝났어요 (잠시 천천히 떨어져요)",
    "message.wildplus.honor.boss_overworld": "보스 소환권은 오버월드에서만 쓸 수 있어요",
    "message.wildplus.honor.festival": "🎉 %s 님이 서버 축제를 열었습니다! 1시간 동안 모두에게 신속·성급함·행운",
    "tooltip.wildplus.hall_of_fame": "돈으로 하는 엔드 컨텐츠\n명예 등급(영구 혜택) · 비행 허가증 · 보스 소환권 · 서버 축제 · 부자 순위",
    "tooltip.wildplus.lord_heart": "핏빛 군주의 전리품 · 출하하면 2,000동, 경제 상인도 2,000동에 사 줌",
    "tooltip.wildplus.auto_harvester": "스프링클러 바로 아래에 놓기\n씨앗은 씨앗 칸, 작물은 작물 칸 (호퍼로 빼기)\n수확한 씨앗은 씨앗 칸에 자동 보충 · 자동 심기 켜고 끄기",
    "tooltip.wildplus.auto_fisher": "반경 2칸 안에 물이 있으면 작동\n낚싯대를 넣으면 미끼·바다의 행운이 그대로 적용\n잡은 물고기는 호퍼로 빼기",
}
LANG_EN = {
    "block.wildplus.hall_of_fame": "Hall of Fame",
    "container.wildplus.hall_of_fame": "Hall of Fame",
    "item.wildplus.lord_heart": "Blood Lord's Heart",
    "entity.wildplus.blood_lord": "Blood Lord",
    "entity.wildplus.farmer_merchant": "Farmer Merchant",
    "entity.wildplus.fisher_merchant": "Fisher Merchant",
    "entity.wildplus.banker_merchant": "Banker Merchant",
    "item.wildplus.farmer_merchant_spawn_egg": "Farmer Merchant Spawn Egg",
    "item.wildplus.fisher_merchant_spawn_egg": "Fisher Merchant Spawn Egg",
    "item.wildplus.banker_merchant_spawn_egg": "Banker Merchant Spawn Egg",
    "affix.wildplus.burning": "Burning",
    "affix.wildplus.vampiric": "Vampiric",
    "affix.wildplus.thorns": "Thorns",
    "affix.wildplus.swift": "Swift",
    "affix.wildplus.giant": "Giant",
    "affix.wildplus.venom": "Venom",
    "affix.wildplus.summoner": "Summoner",
    "affix.wildplus.ironclad": "Ironclad",
    "affix.wildplus.volatile": "Volatile",
    "message.wildplus.blood_lord.near": "Blood moon elites %s/%s ... something stirs",
    "message.wildplus.blood_lord.spawn": "☠ The Blood Lord appeared near %s!",
    "message.wildplus.blood_lord.rage": "The Blood Lord is enraged!",
    "message.wildplus.blood_lord.dead": "★ %s defeated the Blood Lord!",
    "message.wildplus.honor.no_money": "Not enough money (need %s, bank + cash)",
    "message.wildplus.honor.rank_up": "★ %s reached honor rank %s!",
    "message.wildplus.honor.flight": "Flight permit used! %s minutes of flight left",
    "message.wildplus.honor.flight_left": "%s seconds of flight left",
    "message.wildplus.honor.flight_end": "Flight time is over (slow falling for a moment)",
    "message.wildplus.honor.boss_overworld": "Boss tickets only work in the Overworld",
    "message.wildplus.honor.festival": "🎉 %s started a server festival! Speed, Haste and Luck for everyone for 1 hour",
    "tooltip.wildplus.hall_of_fame": "Money end-game\nHonor ranks (permanent perks) · flight permits · boss tickets · festivals · rich list",
    "tooltip.wildplus.lord_heart": "Blood Lord trophy · ships for 2,000, the banker buys it for 2,000",
    "tooltip.wildplus.auto_harvester": "Place directly under a sprinkler\nSeeds in the seed slots, crops in the crop slots (hopper out)\nHarvested seeds refill the seed slots · auto-plant on/off",
    "tooltip.wildplus.auto_fisher": "Works with water within 2 blocks\nInsert a rod: Lure and Luck of the Sea apply\nHopper out",
}

S = "minecraft:block/"


def box(frm, to, tex, uv=None, rot=None, faces=("north", "south", "east", "west", "up", "down")):
    """tex: 문자열(모든 면) 또는 {면: 텍스처}. uv: {면: [u1,v1,u2,v2]}"""
    out = {}
    for f in faces:
        t = tex if isinstance(tex, str) else tex.get(f, tex.get("*"))
        if t is None:
            continue
        face = {"texture": t}
        if uv and f in uv:
            face["uv"] = uv[f]
        out[f] = face
    e = {"from": frm, "to": to, "faces": out}
    if rot:
        e["rotation"] = rot
    return e


def model(name, textures, elements, extra=None):
    m = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if extra:
        m.update(extra)
    write(ASSETS / f"models/block/{name}.json", m)


def facing(name):
    write(ASSETS / f"blockstates/{name}.json", facing_states(i(f"block/{name}")))


def assets():
    # ATM: 정육면체
    write(ASSETS / "models/block/atm.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": i("block/atm_front"), "side": i("block/atm_side"), "top": i("block/atm_top")}})
    # 증권 단말기: 책상 + 모니터
    model("stock_terminal", {"particle": S + "dark_oak_planks", "desk": i("block/stock_desk_front"), "wood": S + "dark_oak_planks",
                             "top": i("block/stock_desk_top"), "screen": i("block/stock_screen"), "back": i("block/stock_monitor_back")}, [
        box([0, 0, 0], [16, 8, 16], {"north": "#desk", "up": "#top", "*": "#wood"}, uv={"north": [0, 4, 16, 12]}),
        box([7, 8, 9], [9, 10, 11], "#back"),
        box([5, 8, 8], [11, 8.5, 12], "#back"),
        box([1, 9, 8], [15, 16, 9.5], {"north": "#screen", "*": "#back"}, uv={"north": [1, 1, 15, 15]}),
    ])
    # 환전소: 카운터 + 유리 칸막이
    model("exchange", {"particle": S + "quartz_block_side", "front": i("block/exchange_front"), "top": i("block/exchange_top"),
                       "side": S + "quartz_block_side", "frame": i("block/exchange_frame")}, [
        box([0, 0, 0], [16, 8, 16], {"north": "#front", "up": "#top", "*": "#side"}, uv={"north": [0, 8, 16, 16]}),
        box([0, 8, 7], [16, 16, 8], {"north": "#front", "south": "#front", "*": "#frame"},
            uv={"north": [0, 0, 16, 8], "south": [16, 0, 0, 8]}),
    ], {"render_type": "minecraft:cutout"})
    # 의뢰 게시판: 기둥 2개 + 코르크판
    model("bounty_board", {"particle": S + "spruce_planks", "front": i("block/bounty_board_front"), "back": i("block/bounty_board_back"),
                           "post": S + "spruce_log"}, [
        box([1, 0, 7], [3, 16, 9], "#post"),
        box([13, 0, 7], [15, 16, 9], "#post"),
        box([0, 3, 6.5], [16, 15, 7.5], {"north": "#front", "*": "#back"}, uv={"north": [0, 2, 16, 14]}),
    ])
    # 약국: 정육면체
    write(ASSETS / "models/block/pharmacy.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": i("block/pharmacy_front"), "side": i("block/pharmacy_side"), "top": S + "white_concrete"}})
    # 기상 관측소: 계기 상자 + 기둥 + 풍속계
    model("weather_station", {"particle": S + "white_concrete", "box": i("block/weather_box"), "metal": i("block/weather_metal"),
                              "cup": i("block/weather_cup"), "top": S + "white_concrete"}, [
        box([2, 0, 2], [14, 12, 14], {"up": "#top", "down": "#top", "*": "#box"},
            uv={f: [2, 2, 14, 14] for f in ("north", "south", "east", "west")}),
        box([7.5, 12, 7.5], [8.5, 16, 8.5], "#metal"),
        box([3.5, 15, 7.75], [12.5, 15.5, 8.25], "#metal"),
        box([7.75, 15, 3.5], [8.25, 15.5, 12.5], "#metal"),
        box([2.5, 14.5, 7.25], [4, 16, 8.75], "#cup"),
        box([12, 14.5, 7.25], [13.5, 16, 8.75], "#cup"),
        box([7.25, 14.5, 2.5], [8.75, 16, 4], "#cup"),
        box([7.25, 14.5, 12], [8.75, 16, 13.5], "#cup"),
    ])
    # 강화대: 화로 받침 + 모루
    model("enhance_table", {"particle": i("block/enhance_forge_side"), "front": i("block/enhance_forge_front"),
                            "side": i("block/enhance_forge_side"), "anvil": i("block/enhance_anvil"), "atop": S + "anvil_top"}, [
        box([0, 0, 0], [16, 10, 16], {"north": "#front", "*": "#side"}, uv={"north": [0, 4, 16, 14]}),
        box([4, 10, 4], [12, 11, 12], "#anvil"),
        box([6, 11, 5], [10, 13, 11], "#anvil"),
        box([2, 13, 3], [14, 16, 13], {"up": "#atop", "*": "#anvil"}),
    ])
    # 청크로더 · 출하 상자 · 자동 수확기
    for name, side, top, bottom in (("chunk_loader", i("block/chunk_loader_side"), i("block/chunk_loader_top"), S + "obsidian"),
                                    ("shipping_bin", i("block/shipping_bin_side"), i("block/shipping_bin_top"), S + "spruce_planks"),
                                    ("auto_harvester", i("block/auto_harvester_side"), i("block/auto_harvester_top"), S + "iron_block")):
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": side, "top": top, "bottom": bottom}})
    # 카지노 테이블: 상판 + 다리 4개
    model("casino_table", {"particle": S + "dark_oak_planks", "top": i("block/casino_table_top"), "rail": i("block/casino_rail"),
                           "wood": S + "dark_oak_planks"}, [
        box([0, 12, 0], [16, 16, 16], {"up": "#top", "down": "#wood", "*": "#rail"},
            uv={f: [0, 0, 16, 4] for f in ("north", "south", "east", "west")}),
        box([1, 0, 1], [3, 12, 3], "#wood"),
        box([13, 0, 1], [15, 12, 3], "#wood"),
        box([1, 0, 13], [3, 12, 15], "#wood"),
        box([13, 0, 13], [15, 12, 15], "#wood"),
    ])
    # 광석 채굴기: 함체 + 위 모터 + 배기관
    model("ore_miner", {"particle": i("block/ore_miner_side"), "side": i("block/ore_miner_side"), "top": i("block/ore_miner_top"),
                        "motor": i("block/ore_miner_motor"), "pipe": S + "iron_block"}, [
        box([0, 0, 0], [16, 12, 16], {"up": "#top", "down": "#top", "*": "#side"},
            uv={f: [0, 2, 16, 14] for f in ("north", "south", "east", "west")}),
        box([4, 12, 4], [12, 15, 12], "#motor"),
        box([12.5, 12, 12.5], [14.5, 16, 14.5], "#pipe"),
    ])
    # 자동 낚시 장치: 나무 상자 + 기울어진 낚싯대
    model("auto_fisher", {"particle": S + "barrel_side", "side": i("block/auto_fisher_side"), "top": S + "barrel_top",
                          "rod": i("block/auto_fisher_rod")}, [
        box([0, 0, 0], [16, 12, 16], {"up": "#top", "down": "#top", "*": "#side"},
            uv={f: [0, 2, 16, 14] for f in ("north", "south", "east", "west")}),
        box([7.5, 11, 7.5], [8.5, 24, 8.5], "#rod", rot={"angle": -22.5, "axis": "x", "origin": [8, 12, 8]}),
    ])
    # 명예의 전당: 대리석 받침 + 금 트로피
    model("hall_of_fame", {"particle": i("block/hall_base"), "base": i("block/hall_base"), "qtop": S + "quartz_block_top",
                           "gold": i("block/hall_gold")}, [
        box([1, 0, 1], [15, 6, 15], {"up": "#qtop", "down": "#qtop", "*": "#base"},
            uv={f: [1, 4, 15, 10] for f in ("north", "south", "east", "west")}),
        box([6, 6, 6], [10, 7, 10], "#gold"),
        box([7, 7, 7], [9, 9, 9], "#gold"),
        box([5, 9, 5], [11, 14, 11], "#gold"),
        box([4, 14, 4], [12, 15, 12], "#gold"),
        box([3, 10, 7], [5, 13, 9], "#gold"),
        box([11, 10, 7], [13, 13, 9], "#gold"),
    ])
    for name in ("stock_terminal", "exchange", "bounty_board", "weather_station", "enhance_table", "casino_table", "hall_of_fame",
                 "atm", "pharmacy"):
        facing(name)
    for name in ("chunk_loader", "shipping_bin", "auto_harvester", "ore_miner", "auto_fisher"):
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": i(f"block/{name}")}}})
    for name in ("hall_of_fame",):
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{name}")}})
    # 아이템
    for name in ("lord_heart", "farmer_merchant_spawn_egg", "fisher_merchant_spawn_egg", "banker_merchant_spawn_egg"):
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    write(DATA / "loot_table/blocks/hall_of_fame.json", self_loot("hall_of_fame"))
    write(DATA / "recipe/hall_of_fame.json", shaped(["GTG", "QBQ", "QQQ"], {"G": i("gold_coin"), "T": "minecraft:gold_block",
                                                                         "Q": "minecraft:quartz_block", "B": "minecraft:diamond_block"},
                                                    i("hall_of_fame"), category="misc"))
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("hall_of_fame")])
