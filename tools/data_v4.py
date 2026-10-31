"""4차 업데이트 데이터: 새 자판기 모델, 뽑기 기계, 무덤, 자석 부적, 휴대용 제작대·엔더 상자, 편의 기능 문구."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, write
from data_v2 import entry, shaped, shapeless

LANG_KO = {
    "block.wildplus.gacha_machine": "뽑기 기계",
    "block.wildplus.grave": "무덤",
    "item.wildplus.magnet_charm": "자석 부적",
    "item.wildplus.portable_crafting_table": "휴대용 제작대",
    "item.wildplus.portable_ender_chest": "휴대용 엔더 상자",
    "tooltip.wildplus.vending_machine": "실물 크기(2칸 높이) · 설치한 사람만 상품·가격을 정할 수 있어요. 동전을 넣고 상품 버튼을 누르면 나와요",
    "tooltip.wildplus.gacha_machine": "우클릭 한 번에 동전 10동 · 캡슐 하나! 대박이 나오면 주변에 알려져요",
    "tooltip.wildplus.magnet_charm": "주변 8칸의 아이템·경험치를 끌어옴 · 우클릭: 켜기/끄기 · 웅크리면 잠시 멈춤",
    "tooltip.wildplus.portable_crafting_table": "우클릭하면 어디서나 제작대",
    "tooltip.wildplus.portable_ender_chest": "우클릭하면 어디서나 엔더 상자",
    "container.wildplus.vending_machine.owned": "%s의 자판기",
    "container.wildplus.vending_machine.wild": "떠돌이 자판기",
    "gui.wildplus.vending.insert": "동전 넣기",
    "gui.wildplus.vending.refund": "반환",
    "gui.wildplus.vending.manage": "관리",
    "gui.wildplus.vending.done": "완료",
    "gui.wildplus.vending.collect": "수익 %s동",
    "gui.wildplus.vending.coin_slot": "투입구",
    "gui.wildplus.vending.credit": "잔액",
    "gui.wildplus.vending.tray": "꺼내는 곳",
    "gui.wildplus.vending.hint_owner": "진열 칸에 상품을 넣으세요",
    "gui.wildplus.vending.fixed": "정해진 상품·가격으로 팝니다",
    "gui.wildplus.vending.fixed_owned": "주인이 정한 상품·가격이에요",
    "message.wildplus.gacha.no_money": "한 번에 %s · 가진 돈 %s — 돈이 부족해요",
    "message.wildplus.gacha.jackpot": "🎉 대박! %s 님이 뽑기에서 %s 을(를) 뽑았습니다!",
    "message.wildplus.magnet.on": "자석 부적 켬",
    "message.wildplus.magnet.off": "자석 부적 끔",
    "message.wildplus.grave.created": "무덤이 X %s, Y %s, Z %s 에 생겼어요 (아이템 %s묶음). 우클릭하면 돌려받아요",
    "message.wildplus.grave.restored": "무덤에서 아이템 %s묶음을 돌려받았어요",
    "message.wildplus.grave.not_yours": "%s 님의 무덤입니다",
    "message.wildplus.death_pos": "사망 위치: X %s, Y %s, Z %s (%s)",
    "hud.wildplus.time": "%s일차 · %s %s:%s",
    "hud.wildplus.coords": "X %s  Y %s  Z %s · %s",
    "hud.wildplus.dir.north": "북", "hud.wildplus.dir.south": "남",
    "hud.wildplus.dir.east": "동", "hud.wildplus.dir.west": "서",
    "wildplus.configuration.convenience": "편의 기능",
    "wildplus.configuration.treeFelling": "나무 한 번에 베기 (웅크리고 도끼)",
    "wildplus.configuration.veinMining": "광맥 한 번에 캐기 (웅크리고 곡괭이)",
    "wildplus.configuration.rightClickHarvest": "우클릭 수확·자동 재심기",
    "wildplus.configuration.graves": "무덤",
    "wildplus.configuration.economy": "경제",
    "wildplus.configuration.gachaPrice": "뽑기 1회 가격",
    "wildplus.configuration.showHud": "정보 HUD 표시",
    "wildplus.configuration.hudCoords": "HUD에 좌표 표시",
}
LANG_EN = {
    "block.wildplus.gacha_machine": "Capsule Machine",
    "block.wildplus.grave": "Grave",
    "item.wildplus.magnet_charm": "Magnet Charm",
    "item.wildplus.portable_crafting_table": "Portable Crafting Table",
    "item.wildplus.portable_ender_chest": "Portable Ender Chest",
    "tooltip.wildplus.vending_machine": "Life-size (2 blocks tall) · Only the placer sets products and prices. Insert coins and press a product",
    "tooltip.wildplus.gacha_machine": "10 coins per pull, one capsule! Jackpots are announced",
    "tooltip.wildplus.magnet_charm": "Pulls items and XP within 8 blocks · Right-click: toggle · Sneak to pause",
    "tooltip.wildplus.portable_crafting_table": "Right-click for a crafting table anywhere",
    "tooltip.wildplus.portable_ender_chest": "Right-click for your ender chest anywhere",
    "container.wildplus.vending_machine.owned": "%s's Vending Machine",
    "container.wildplus.vending_machine.wild": "Wandering Vending Machine",
    "gui.wildplus.vending.insert": "Insert",
    "gui.wildplus.vending.refund": "Refund",
    "gui.wildplus.vending.manage": "Manage",
    "gui.wildplus.vending.done": "Done",
    "gui.wildplus.vending.collect": "Collect %s",
    "gui.wildplus.vending.coin_slot": "Coins",
    "gui.wildplus.vending.credit": "Credit",
    "gui.wildplus.vending.tray": "Tray",
    "gui.wildplus.vending.hint_owner": "Put products on the shelves",
    "gui.wildplus.vending.fixed": "Fixed products and prices",
    "gui.wildplus.vending.fixed_owned": "Products set by the owner",
    "message.wildplus.gacha.no_money": "%s per pull · you have %s — not enough money",
    "message.wildplus.gacha.jackpot": "🎉 Jackpot! %s pulled %s from a capsule machine!",
    "message.wildplus.magnet.on": "Magnet on",
    "message.wildplus.magnet.off": "Magnet off",
    "message.wildplus.grave.created": "A grave was made at X %s, Y %s, Z %s (%s stacks). Right-click it to get them back",
    "message.wildplus.grave.restored": "Recovered %s stacks from your grave",
    "message.wildplus.grave.not_yours": "This is %s's grave",
    "message.wildplus.death_pos": "You died at X %s, Y %s, Z %s (%s)",
    "hud.wildplus.time": "Day %s · %s %s:%s",
    "hud.wildplus.coords": "X %s  Y %s  Z %s · %s",
    "hud.wildplus.dir.north": "N", "hud.wildplus.dir.south": "S",
    "hud.wildplus.dir.east": "E", "hud.wildplus.dir.west": "W",
    "wildplus.configuration.convenience": "Convenience",
    "wildplus.configuration.treeFelling": "Tree felling (sneak + axe)",
    "wildplus.configuration.veinMining": "Vein mining (sneak + pickaxe)",
    "wildplus.configuration.rightClickHarvest": "Right-click harvest & replant",
    "wildplus.configuration.graves": "Graves",
    "wildplus.configuration.economy": "Economy",
    "wildplus.configuration.gachaPrice": "Capsule machine price",
    "wildplus.configuration.showHud": "Show info HUD",
    "wildplus.configuration.hudCoords": "Show coordinates on HUD",
}

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def facing_states(model):
    return {"variants": {f"facing={d}": ({"model": model, "y": r} if r else {"model": model}) for d, r in ROT.items()}}


def faces(**kw):
    return {k: {"texture": v} for k, v in kw.items()}


def assets():
    # 자판기: 실물 크기(2칸 높이). 진열창은 아래 칸 y6 ~ 위 칸 y13 까지 이어지고 선반이 3단
    def tex(half):
        return {"particle": i(f"block/vending_machine_side_{half}"), "front": i(f"block/vending_machine_front_{half}"),
                "interior": i(f"block/vending_machine_interior_{half}"), "glass": i(f"block/vending_machine_glass_{half}"),
                "side": i(f"block/vending_machine_side_{half}"), "top": i("block/vending_machine_top"),
                "inner": i("block/vending_machine_inner"), "shelf": i("block/vending_machine_shelf")}

    def shelf(y):
        return {"from": [4, y - 0.5, 0.6], "to": [15, y, 3],
                "faces": {"north": {"texture": "#shelf", "uv": [1, 0, 12, 1]},
                          "up": {"texture": "#shelf", "uv": [1, 1, 12, 4]},
                          "down": {"texture": "#shelf", "uv": [1, 1, 12, 4]}}}

    write(ASSETS / "models/block/vending_machine_lower.json", {
        "parent": "minecraft:block/block", "textures": tex("lower"),
        "elements": [
            {"from": [0, 0, 3], "to": [16, 16, 16], "faces": faces(north="#interior", south="#side", east="#side",
                                                                  west="#side", down="#top")},
            {"from": [0, 0, 0], "to": [16, 6, 3], "faces": {**faces(north="#front", east="#side", west="#side", down="#top"),
                                                           "up": {"texture": "#shelf", "uv": [0, 1, 16, 4]}}},
            {"from": [0, 6, 0], "to": [4, 16, 3], "faces": faces(north="#front", west="#side", east="#inner")},
            {"from": [15, 6, 0], "to": [16, 16, 3], "faces": faces(north="#front", east="#side", west="#inner")},
            shelf(13.5),
            {"from": [4, 6, 0.5], "to": [15, 16, 0.5], "faces": faces(north="#glass", south="#glass")},
        ]})
    write(ASSETS / "models/block/vending_machine_upper.json", {
        "parent": "minecraft:block/block", "textures": tex("upper"),
        "elements": [
            {"from": [0, 0, 3], "to": [16, 16, 16], "faces": faces(north="#interior", south="#side", east="#side",
                                                                  west="#side", up="#top")},
            {"from": [0, 13, 0], "to": [16, 16, 3], "faces": faces(north="#front", east="#side", west="#side",
                                                                  up="#top", down="#inner")},
            {"from": [0, 0, 0], "to": [4, 13, 3], "faces": faces(north="#front", west="#side", east="#inner")},
            {"from": [15, 0, 0], "to": [16, 13, 3], "faces": faces(north="#front", east="#side", west="#inner")},
            shelf(5),
            {"from": [4, 0, 0.5], "to": [15, 13, 0.5], "faces": faces(north="#glass", south="#glass")},
        ]})
    write(ASSETS / "blockstates/vending_machine.json", {"variants": {
        f"facing={d},half={h}": ({"model": i(f"block/vending_machine_{h}"), "y": r} if r
                                 else {"model": i(f"block/vending_machine_{h}")})
        for d, r in ROT.items() for h in ("lower", "upper")}})
    write(ASSETS / "models/item/vending_machine.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": i("item/vending_machine")}})
    write(ASSETS / "items/vending_machine.json", {"model": {"type": "minecraft:model", "model": i("item/vending_machine")}})
    (ASSETS / "models/block/vending_machine.json").unlink(missing_ok=True)

    write(ASSETS / "models/block/gacha_machine.json", {
        "parent": "minecraft:block/orientable",
        "textures": {"front": i("block/gacha_machine_front"), "side": i("block/gacha_machine_side"),
                     "top": i("block/gacha_machine_top")}})
    write(ASSETS / "blockstates/gacha_machine.json", facing_states(i("block/gacha_machine")))
    write(ASSETS / "items/gacha_machine.json", {"model": {"type": "minecraft:model", "model": i("block/gacha_machine")}})

    write(ASSETS / "models/block/grave.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/mossy_stone_bricks", "stone": "minecraft:block/mossy_stone_bricks",
                     "base": "minecraft:block/cobblestone", "soil": "minecraft:block/coarse_dirt"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 2, 15], "faces": faces(north="#base", south="#base", east="#base",
                                                                 west="#base", up="#soil", down="#base")},
            {"from": [3, 2, 6], "to": [13, 12, 10], "faces": faces(north="#stone", south="#stone", east="#stone",
                                                                  west="#stone", up="#stone", down="#stone")},
            {"from": [5, 12, 6], "to": [11, 14, 10], "faces": faces(north="#stone", south="#stone", east="#stone",
                                                                   west="#stone", up="#stone")},
        ]})
    write(ASSETS / "blockstates/grave.json", facing_states(i("block/grave")))

    for name in ["magnet_charm", "portable_crafting_table", "portable_ender_chest"]:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})


def recipes():
    r = {
        "gacha_machine": shaped(["GGG", "IRI", "ICI"], {"G": "minecraft:glass", "I": "minecraft:iron_ingot",
                                                        "R": "minecraft:redstone", "C": i("copper_coin")},
                                i("gacha_machine"), category="misc"),
        "magnet_charm": shaped(["I I", "R R", "MIM"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone",
                                                       "M": i("mythril_ingot")}, i("magnet_charm")),
        "portable_crafting_table": shapeless(["minecraft:crafting_table", "minecraft:leather", "minecraft:string"],
                                             i("portable_crafting_table")),
        "portable_ender_chest": shapeless(["minecraft:ender_chest", "minecraft:leather", "minecraft:string",
                                           i("jade")], i("portable_ender_chest")),
    }
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def loot():
    write(DATA / "loot_table/blocks/vending_machine.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:match_block", "blocks": i("vending_machine"),
                                   "state": {"half": "lower"}},
         "entries": [{"type": "minecraft:item", "name": i("vending_machine")}]}],
        "random_sequence": i("blocks/vending_machine")})
    write(DATA / "loot_table/blocks/gacha_machine.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
         "entries": [{"type": "minecraft:item", "name": i("gacha_machine")}]}],
        "random_sequence": i("blocks/gacha_machine")})
    write(DATA / "loot_table/gameplay/gacha.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [
            entry(i("copper_coin"), 30, 4, 12), entry(i("silver_coin"), 10, 1, 2), entry("minecraft:redstone", 8, 8, 16),
            entry("minecraft:redstone_block", 2, 1, 2),
            entry(i("tomato_seeds"), 6, 2, 4), entry(i("strawberry"), 6, 2, 4), entry(i("chili_seeds"), 6, 2, 4),
            entry(i("pizza"), 6, 2, 2), entry(i("strawberry_tart"), 5, 2, 2), entry(i("honey_toast"), 5),
            entry(i("fertilizer"), 6, 2, 4), entry("minecraft:emerald", 6, 1, 2), entry(i("jade"), 4, 1, 2),
            entry(i("recall_scroll"), 4), entry(i("mythril_ingot"), 3), entry("minecraft:firework_rocket", 4, 3, 3),
            entry("minecraft:name_tag", 2), entry("minecraft:diamond", 2), entry(i("magnet_charm"), 1),
            entry(i("gold_coin"), 2), entry(i("golden_carp"), 1), entry(i("lucky_charm"), 1),
            entry("minecraft:enchanted_golden_apple", 1),
        ]}], "random_sequence": i("gameplay/gacha")})


def tags():
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("gacha_machine"), i("grave")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    recipes()
    loot()
    tags()
