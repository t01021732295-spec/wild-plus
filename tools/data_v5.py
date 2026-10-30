"""5차 데이터: 비취·미스릴 장비, 워프·텔레포트기·배낭, 경제(주식·은행·의뢰·출하·복권), 인챈트 상향."""
from data_v2 import ASSETS, DATA, MC, i, merge_tag, write
from data_v2 import shaped, shapeless

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
KO_TOOL = {"sword": "검", "pickaxe": "곡괭이", "axe": "도끼", "shovel": "삽", "hoe": "괭이",
           "helmet": "투구", "chestplate": "흉갑", "leggings": "레깅스", "boots": "부츠"}
EN_TOOL = {k: k.capitalize() for k in KO_TOOL}
SIMPLE_ITEMS = ["teleporter", "lottery_ticket", "copper_backpack", "silver_backpack", "gold_backpack"]
BLOCKS = ["warp_point", "stock_terminal", "atm", "bounty_board", "shipping_bin", "exchange"]

LANG_KO = {
    "block.wildplus.warp_point": "워프 포인트",
    "block.wildplus.stock_terminal": "증권 단말기",
    "block.wildplus.atm": "은행 ATM",
    "block.wildplus.bounty_board": "의뢰 게시판",
    "block.wildplus.shipping_bin": "출하 상자",
    "block.wildplus.exchange": "환전소",
    "tooltip.wildplus.exchange": "동화 ↔ 은화 ↔ 금화를 양쪽으로 바꿔 줌 (수수료 없음)",
    "container.wildplus.exchange": "환전소",
    "message.wildplus.exchange.not_enough": "바꿀 동전이 부족해요",
    "item.wildplus.teleporter": "소형 텔레포트기",
    "item.wildplus.lottery_ticket": "즉석 복권",
    "item.wildplus.copper_backpack": "동 배낭",
    "item.wildplus.silver_backpack": "은 배낭",
    "item.wildplus.gold_backpack": "금 배낭",
    "tooltip.wildplus.warp_point": "설치하면 워프가 생겨요(모루에서 이름을 붙여 설치 가능). 우클릭하면 다른 워프로 무제한 이동",
    "tooltip.wildplus.teleporter": "사용하면 접속 중인 유저를 골라 그 사람에게 순간이동 (1회용, 상점·자판기 전용)",
    "tooltip.wildplus.stock_terminal": "실제 주식 시세로 사고팔기 (1,000원 = 1동, 10분마다 갱신)",
    "tooltip.wildplus.atm": "예금·출금·송금. 매일 아침 이자 1% (하루 최대 500동)",
    "tooltip.wildplus.bounty_board": "매일 바뀌는 납품 의뢰 3개 (보상: 출하 가격의 2배)",
    "tooltip.wildplus.shipping_bin": "넣어 둔 물건이 매일 아침 팔리고, 대금은 은행 통장으로",
    "tooltip.wildplus.lottery_ticket": "우클릭으로 긁기. 최고 5,000동!",
    "tooltip.wildplus.copper_backpack": "30칸. 우클릭으로 열기 · 창에서 은 배낭으로 업그레이드(200동)",
    "tooltip.wildplus.silver_backpack": "50칸. 우클릭으로 열기 · 창에서 금 배낭으로 업그레이드(800동)",
    "tooltip.wildplus.gold_backpack": "70칸. 우클릭으로 열기",
    "tooltip.wildplus.ship_price": "출하 가격: %s",
    "container.wildplus.warp": "워프 포인트 · %s",
    "container.wildplus.stocks": "증권 단말기",
    "container.wildplus.bank": "은행 ATM",
    "container.wildplus.bounty_board": "의뢰 게시판",
    "container.wildplus.bounty_board.day": "의뢰 게시판 · %s일차",
    "container.wildplus.shipping_bin": "출하 상자 (내일 아침 예상 %s)",
    "gui.wildplus.warp.make_private": "비공개로 바꾸기",
    "gui.wildplus.warp.make_public": "공개로 바꾸기",
    "gui.wildplus.backpack.tier0": "동 배낭",
    "gui.wildplus.backpack.tier1": "은 배낭",
    "gui.wildplus.backpack.tier2": "금 배낭",
    "gui.wildplus.backpack.upgrade": "→ %s (%s동)",
    "message.wildplus.warp.created": "워프 포인트 [%s] 이(가) 생겼어요. 다른 워프 포인트와 자유롭게 오갈 수 있어요",
    "message.wildplus.warp.arrived": "%s 에 도착했어요",
    "message.wildplus.warp.gone": "워프 포인트 [%s] 이(가) 사라져서 목록에서 지웠어요",
    "message.wildplus.teleporter.offline": "그 유저는 지금 접속해 있지 않아요",
    "message.wildplus.teleporter.done": "%s 님에게 순간이동했어요",
    "message.wildplus.teleporter.arrived": "%s 님이 소형 텔레포트기로 찾아왔어요",
    "message.wildplus.backpack.no_money": "돈이 부족해요 (필요: %s동)",
    "message.wildplus.backpack.upgraded": "%s(으)로 업그레이드! (%s동)",
    "message.wildplus.stock.no_money": "돈이 부족해요 (필요: %s동)",
    "message.wildplus.stock.bought": "%s %s주 매수 (%s동)",
    "message.wildplus.stock.sold": "%s %s주 매도 (+%s동, 손익 %s동)",
    "message.wildplus.bank.no_cash": "가진 현금이 부족해요",
    "message.wildplus.bank.no_balance": "통장 잔액이 부족해요",
    "message.wildplus.bank.sent": "%s 님에게 %s동을 보냈어요",
    "message.wildplus.bank.received": "%s 님이 %s동을 보냈어요 (통장으로 입금)",
    "message.wildplus.bank.interest": "은행 이자 %s동이 들어왔어요 (잔액 %s동)",
    "message.wildplus.bounty.not_enough": "물건이 부족해요",
    "message.wildplus.bounty.done": "의뢰 완료! %s ×%s 납품 → %s동",
    "message.wildplus.shipping.sold": "출하 상자: 물건 %s개가 팔려 %s동이 통장에 들어왔어요",
    "message.wildplus.lottery.lose": "꽝! 다음 기회에…",
    "message.wildplus.lottery.win": "당첨! %s동",
    "message.wildplus.lottery.jackpot": "%s 님이 즉석 복권 %s동에 당첨됐어요!",
}
LANG_EN = {
    "block.wildplus.warp_point": "Warp Point",
    "block.wildplus.stock_terminal": "Stock Terminal",
    "block.wildplus.atm": "Bank ATM",
    "block.wildplus.bounty_board": "Bounty Board",
    "block.wildplus.shipping_bin": "Shipping Bin",
    "block.wildplus.exchange": "Currency Exchange",
    "tooltip.wildplus.exchange": "Swap copper ↔ silver ↔ gold coins both ways (no fee)",
    "container.wildplus.exchange": "Currency Exchange",
    "message.wildplus.exchange.not_enough": "Not enough coins to exchange",
    "item.wildplus.teleporter": "Pocket Teleporter",
    "item.wildplus.lottery_ticket": "Scratch Ticket",
    "item.wildplus.copper_backpack": "Copper Backpack",
    "item.wildplus.silver_backpack": "Silver Backpack",
    "item.wildplus.gold_backpack": "Gold Backpack",
    "tooltip.wildplus.warp_point": "Creates a warp (rename it in an anvil first). Right-click to travel to any other warp",
    "tooltip.wildplus.teleporter": "Teleport to an online player (single use, shop/vending only)",
    "tooltip.wildplus.stock_terminal": "Trade real-world stocks (1,000 KRW = 1 coin, refreshes every 10 min)",
    "tooltip.wildplus.atm": "Deposit, withdraw, transfer. 1% daily interest (max 500/day)",
    "tooltip.wildplus.bounty_board": "Three new delivery requests every day (2x shipping price)",
    "tooltip.wildplus.shipping_bin": "Contents sell every morning; money goes to your bank",
    "tooltip.wildplus.lottery_ticket": "Right-click to scratch. Up to 5,000!",
    "tooltip.wildplus.copper_backpack": "30 slots. Upgrade to silver in its window (200)",
    "tooltip.wildplus.silver_backpack": "50 slots. Upgrade to gold in its window (800)",
    "tooltip.wildplus.gold_backpack": "70 slots",
    "tooltip.wildplus.ship_price": "Ships for %s",
    "container.wildplus.warp": "Warp Point · %s",
    "container.wildplus.stocks": "Stock Terminal",
    "container.wildplus.bank": "Bank ATM",
    "container.wildplus.bounty_board": "Bounty Board",
    "container.wildplus.bounty_board.day": "Bounty Board · Day %s",
    "container.wildplus.shipping_bin": "Shipping Bin (tomorrow: %s)",
    "gui.wildplus.warp.make_private": "Make private",
    "gui.wildplus.warp.make_public": "Make public",
    "gui.wildplus.backpack.tier0": "Copper Backpack",
    "gui.wildplus.backpack.tier1": "Silver Backpack",
    "gui.wildplus.backpack.tier2": "Gold Backpack",
    "gui.wildplus.backpack.upgrade": "→ %s (%s)",
    "message.wildplus.warp.created": "Warp [%s] created",
    "message.wildplus.warp.arrived": "Arrived at %s",
    "message.wildplus.warp.gone": "Warp [%s] no longer exists and was removed",
    "message.wildplus.teleporter.offline": "That player is offline",
    "message.wildplus.teleporter.done": "Teleported to %s",
    "message.wildplus.teleporter.arrived": "%s teleported to you",
    "message.wildplus.backpack.no_money": "Not enough money (need %s)",
    "message.wildplus.backpack.upgraded": "Upgraded to %s! (%s)",
    "message.wildplus.stock.no_money": "Not enough money (need %s)",
    "message.wildplus.stock.bought": "Bought %s x%s (%s)",
    "message.wildplus.stock.sold": "Sold %s x%s (+%s, P/L %s)",
    "message.wildplus.bank.no_cash": "Not enough cash",
    "message.wildplus.bank.no_balance": "Not enough balance",
    "message.wildplus.bank.sent": "Sent %2$s to %1$s",
    "message.wildplus.bank.received": "%s sent you %s (to your bank)",
    "message.wildplus.bank.interest": "Bank interest: %s (balance %s)",
    "message.wildplus.bounty.not_enough": "You don't have enough",
    "message.wildplus.bounty.done": "Bounty done! %s x%s → %s",
    "message.wildplus.shipping.sold": "Shipping bin sold %s items for %s (to your bank)",
    "message.wildplus.lottery.lose": "No luck this time…",
    "message.wildplus.lottery.win": "You won %s!",
    "message.wildplus.lottery.jackpot": "%s won %s on a scratch ticket!",
}

for mat, ko, en in (("jade", "비취", "Jade"), ("mythril", "미스릴", "Mythril")):
    for t in TOOLS + ARMOR:
        LANG_KO[f"item.wildplus.{mat}_{t}"] = f"{ko} {KO_TOOL[t]}"
        LANG_EN[f"item.wildplus.{mat}_{t}"] = f"{en} {EN_TOOL[t]}"
for a in ARMOR:
    LANG_KO[f"tooltip.wildplus.mythril_{a}"] = "4부위 세트 효과: 성급함 I"
    LANG_EN[f"tooltip.wildplus.mythril_{a}"] = "Full set: Haste I"
    LANG_KO[f"tooltip.wildplus.jade_{a}"] = "4부위 세트 효과: 행운 I · 인챈트가 가장 잘 붙음"
    LANG_EN[f"tooltip.wildplus.jade_{a}"] = "Full set: Luck I · best enchantability"


def assets():
    for mat in ("jade", "mythril"):
        for t in TOOLS:
            name = f"{mat}_{t}"
            write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": i(f"item/{name}")}})
            write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
        for a in ARMOR:
            name = f"{mat}_{a}"
            write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
            write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})
        write(ASSETS / f"equipment/{mat}.json", {"layers": {
            "humanoid": [{"texture": i(mat)}], "humanoid_leggings": [{"texture": i(mat)}]}})
    for name in SIMPLE_ITEMS:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": i(f"item/{name}")}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"item/{name}")}})

    # 블록: 마인크래프트 기본 텍스처를 최대한 활용
    write(ASSETS / "models/block/warp_point.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "minecraft:block/respawn_anchor_top", "bottom": "minecraft:block/obsidian", "side": "minecraft:block/crying_obsidian"}})
    write(ASSETS / "blockstates/warp_point.json", {"variants": {"": {"model": i("block/warp_point")}}})
    for name, side, top in (("stock_terminal", "minecraft:block/iron_block", "minecraft:block/iron_block"),
                            ("atm", "minecraft:block/iron_block", "minecraft:block/iron_block"),
                            ("exchange", "minecraft:block/iron_block", "minecraft:block/iron_block"),
                            ("bounty_board", "minecraft:block/spruce_planks", "minecraft:block/spruce_planks")):
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": i(f"block/{name}_front"), "side": side, "top": top}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {
            f"facing={d}": ({"model": i(f"block/{name}"), "y": r} if r else {"model": i(f"block/{name}")})
            for d, r in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
    write(ASSETS / "models/block/shipping_bin.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": i("block/shipping_bin_top"), "bottom": "minecraft:block/spruce_planks", "side": i("block/shipping_bin_side")}})
    write(ASSETS / "blockstates/shipping_bin.json", {"variants": {"": {"model": i("block/shipping_bin")}}})
    for name in BLOCKS:
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": i(f"block/{name}")}})


def recipes():
    r = {}
    for mat, ing in (("jade", i("jade")), ("mythril", i("mythril_ingot"))):
        X, S = ing, "minecraft:stick"
        if mat == "jade":
            r["jade_sword"] = shaped(["X", "X", "#"], {"X": X, "#": S}, i("jade_sword"))
            r["jade_pickaxe"] = shaped(["XXX", " # ", " # "], {"X": X, "#": S}, i("jade_pickaxe"))
            r["jade_axe"] = shaped(["XX", "X#", " #"], {"X": X, "#": S}, i("jade_axe"))
            r["jade_shovel"] = shaped(["X", "#", "#"], {"X": X, "#": S}, i("jade_shovel"))
            r["jade_hoe"] = shaped(["XX", " #", " #"], {"X": X, "#": S}, i("jade_hoe"))
        r[f"{mat}_helmet"] = shaped(["XXX", "X X"], {"X": X}, i(f"{mat}_helmet"))
        r[f"{mat}_chestplate"] = shaped(["X X", "XXX", "XXX"], {"X": X}, i(f"{mat}_chestplate"))
        r[f"{mat}_leggings"] = shaped(["XXX", "X X", "X X"], {"X": X}, i(f"{mat}_leggings"))
        r[f"{mat}_boots"] = shaped(["X X", "X X"], {"X": X}, i(f"{mat}_boots"))
    r["warp_point"] = shaped(["EJE", "OGO", "OOO"], {"E": "minecraft:ender_pearl", "J": i("jade"),
                                                   "O": "minecraft:obsidian", "G": i("gold_coin")}, i("warp_point"), category="misc")
    r["stock_terminal"] = shaped(["GGG", "ISI", "IRI"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot",
                                                       "S": i("silver_coin"), "R": "minecraft:redstone"}, i("stock_terminal"), category="misc")
    r["atm"] = shaped(["III", "GSG", "IRI"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot",
                                            "S": i("gold_coin"), "R": "minecraft:redstone"}, i("atm"), category="misc")
    r["exchange"] = shaped(["III", "CSG", "III"], {"I": "minecraft:iron_ingot", "C": i("copper_coin"),
                                                 "S": i("silver_coin"), "G": i("gold_coin")}, i("exchange"), category="misc")
    r["bounty_board"] = shaped(["SPS", "PPP", "SPS"], {"S": "minecraft:stick", "P": "minecraft:paper"}, i("bounty_board"), category="misc")
    r["shipping_bin"] = shapeless(["minecraft:chest", i("copper_coin")], i("shipping_bin"))
    r["copper_backpack"] = shaped(["LSL", "LHL", "LCL"], {"L": "minecraft:leather", "S": "minecraft:string",
                                                        "H": "minecraft:chest", "C": i("copper_coin")}, i("copper_backpack"))
    for name, obj in r.items():
        write(DATA / "recipe" / f"{name}.json", obj)


def loot():
    for name in BLOCKS:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
             "entries": [{"type": "minecraft:item", "name": i(name)}]}], "random_sequence": i(f"blocks/{name}")})


def tags():
    t = MC / "tags"
    write(DATA / "tags/item/jade_repair.json", {"values": [i("jade")]})
    merge_tag(t / "block/mineable/pickaxe.json", [i(b) for b in ("warp_point", "stock_terminal", "atm", "exchange")])
    merge_tag(t / "block/mineable/axe.json", [i(b) for b in ("bounty_board", "shipping_bin")])
    merge_tag(t / "item/swords.json", [i("jade_sword")])
    merge_tag(t / "item/pickaxes.json", [i("jade_pickaxe")])
    merge_tag(t / "item/axes.json", [i("jade_axe")])
    merge_tag(t / "item/shovels.json", [i("jade_shovel")])
    merge_tag(t / "item/hoes.json", [i("jade_hoe")])
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
        merge_tag(t / f"item/{slot}_armor.json", [i(f"mythril_{piece}"), i(f"jade_{piece}")])


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **LANG_KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **LANG_EN})
    assets()
    recipes()
    loot()
    tags()
