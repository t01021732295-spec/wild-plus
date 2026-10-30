"""5차 텍스처(직접 그림): 배낭 3종, 소형 텔레포트기, 즉석 복권, 의뢰 게시판 · 출하 상자.
바닐라 아이템처럼 짙은 외곽선 + 왼쪽 위 밝음 / 오른쪽 아래 어두움 규칙으로 그린다.
증권 단말기 · ATM 앞면은 바닐라 철 블록 위에 그려야 해서 textures_v5 (바닐라 필요) 쪽에서 만든다."""
from textures_v2 import render, save

BACKPACK = [
    "................",
    ".....kkkkkk.....",
    "....kLLLLLLk....",
    "...kL......Lk...",
    "..kkkkkkkkkkkk..",
    ".kHhhhhhhhhhhDk.",
    ".kHhhhhBBhhhhDk.",
    ".kHhhhhBbhhhhDk.",
    ".kkkkkkkkkkkkkk.",
    ".kHhhhhhhhhhhDk.",
    ".kHhkkkkkkkkhDk.",
    ".kHhkhhhhhhkhDk.",
    ".kHhkhhhhhhkhDk.",
    ".kHhkkkkkkkkhDk.",
    ".kDDDDDDDDDDDDk.",
    "..kkkkkkkkkkkk..",
]
LEATHER = {"k": "#2b1708", "H": "#b8743e", "h": "#9a5c2c", "D": "#6e3e1a", "L": "#7a4520"}
TRIM = {"copper": ("#e8955a", "#a0522d"), "silver": ("#f0f0f6", "#9a9aa8"), "gold": ("#fff070", "#c89a10")}

TELEPORTER = [
    "................",
    "..........kk....",
    ".........kYk....",
    "........kYk.....",
    ".....kkkkkkk....",
    "....kSssssssk...",
    "...kSsPPPPPsdk..",
    "...kSPpPPPPPdk..",
    "...kSPPEEPPPdk..",
    "...kSPPEePPPdk..",
    "...kSPPPPPPPdk..",
    "...kSsPPPPPsdk..",
    "....kSdddddddk..",
    ".....kkkkkkkk...",
    "................",
    "................",
]
TELE_PAL = {"k": "#121216", "S": "#8a8a96", "s": "#5a5a66", "d": "#34343e", "P": "#2a6a5a", "p": "#7fe3c8",
            "E": "#1a0a2a", "e": "#c084ff", "Y": "#c084ff"}

TICKET = [
    "................",
    "................",
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kWWWWWWWWWWWWk.",
    ".kWRRWGGGGGGGWk.",
    ".kWRRWGgGGgGGWk.",
    ".kWWWWGGGGGGGWk.",
    ".kWllWGGgGGGgWk.",
    ".kWWWWGGGGGGGWk.",
    ".kWllWWWWWWWWWk.",
    ".kWWWWllllllWWk.",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
    "................",
]
TICKET_PAL = {"k": "#5a4a2a", "W": "#f4ecd2", "R": "#d23a2a", "G": "#c8a028", "g": "#fff0a0", "l": "#a89a78"}

# 가문비나무 판자 색 (바닐라 가문비 판자와 비슷한 톤)
SP = {"a": "#7a5a35", "b": "#6b4e2d", "c": "#5c4326", "d": "#3e2c18", "e": "#86653d"}

BOARD = [
    "dddddddddddddddd",
    "deeeeeeeeeeeeeed",
    "deWWWWebWWWWWbcd",
    "deWlllWbWlllWbcd",
    "deWWWWWbWWWWWbcd",
    "deWlllWbWllWWbcd",
    "debbRbbbWWWWWbcd",
    "debbbbbbbbRbbbcd",
    "deWWWWWWbbbbbbcd",
    "deWlllllWbWWWWcd",
    "deWWWWWWWbWllWcd",
    "deWllllWWbWWWWcd",
    "deWWWWWWWbWllWcd",
    "debbbRbbbbbbbbcd",
    "deccccccccccccdd",
    "dddddddddddddddd",
]
BOARD_PAL = {**SP, "W": "#ece4cc", "l": "#9a9078", "R": "#c8282a"}

CRATE_SIDE = [
    "dddddddddddddddd",
    "dMeeeeeeeeeeeeMd",
    "deaaaaaaaaaaaacd",
    "deabbbbbbbbbbbcd",
    "dccccccccccccccd",
    "deaaaaaaaaaaaacd",
    "deabbbbbbbbbbbcd",
    "dccccccccccccccd",
    "deaaaaaaaaaaaacd",
    "deabbbbbbbbbbbcd",
    "dccccccccccccccd",
    "deaaaaaaaaaaaacd",
    "deabbbbbbbbbbbcd",
    "dccccccccccccccd",
    "dMccccccccccccMd",
    "dddddddddddddddd",
]
CRATE_TOP = [
    "dddddddddddddddd",
    "dMeeeeeeeeeeeeMd",
    "deaaaaaabaaaaacd",
    "deabbbbabbbbbbcd",
    "deaaaaaabaaaaacd",
    "deabbbbabbbbbbcd",
    "deaaakkkkkkaaacd",
    "deabbkGYYYGkbbcd",
    "deaaakkkkkkaaacd",
    "deabbbbabbbbbbcd",
    "deaaaaaabaaaaacd",
    "deabbbbabbbbbbcd",
    "deaaaaaabaaaaacd",
    "deabbbbabbbbbbcd",
    "dMccccccccccccMd",
    "dddddddddddddddd",
]
CRATE_PAL = {**SP, "M": "#9a9aa2", "k": "#111111", "G": "#8a6a10", "Y": "#f2c230"}


PHARMACY = [
    "QQQQQQQQQQQQQQQQ",
    "QqqqqqGGqqqqqqqQ",
    "QqqqqqGGqqqqqqqQ",
    "QqqqGGGGGGqqqqqQ",
    "QqqqGGGGGGqqqqqQ",
    "QqqqqqGGqqqqqqqQ",
    "QqqqqqGGqqqqqqqQ",
    "QkkkkkkkkkkkkkkQ",
    "QkwRwkwBwkwYwkkQ",
    "QkRRRkBBBkYYYkkQ",
    "QkRRRkBBBkYYYkkQ",
    "QsssssssssssssssQ"[:16],
    "QkwPwkwGwkwOwkkQ",
    "QkPPPkGGGkOOOkkQ",
    "QsssssssssssssQQ"[:16],
    "QQQQQQQQQQQQQQQQ",
]
PHARMACY_PAL = {"Q": "#cfc8bc", "q": "#ece6dc", "G": "#2fa84a", "k": "#3a3a3e", "s": "#8a8a90", "w": "#e8f4ff",
                "R": "#d83a3a", "B": "#3a6ad8", "Y": "#e8c82a", "P": "#b04ad8", "O": "#e8862a"}

WEATHER = [
    "CCCCCCCCCCCCCCCC",
    "CcccccccccccccdC",
    "CcYYcccccwwwccdC",
    "CcYYccccwwwwwcdC",
    "Ccccccccccccccdc"[:16],
    "CcccckkkkkkccccC",
    "CccckWWWWWWkcccC",
    "CcckWWWWWWWWkccC",
    "CcckWWWWNWWWkccC",
    "CcckWWWNWWWWkccC",
    "CcckWWNWWWWWkccC",
    "CccckWWWWWWkcccC",
    "CcccckkkkkkccccC",
    "CcccccccccccccdC",
    "CddddddddddddddC",
    "CCCCCCCCCCCCCCCC",
]
WEATHER_PAL = {"C": "#8a4a2e", "c": "#c8704a", "d": "#a3553a", "Y": "#f8d040", "w": "#eef4f8", "k": "#3a2a20",
               "W": "#f4f0e4", "N": "#c82a2a"}


def build():
    save(render(PHARMACY, PHARMACY_PAL), "block/pharmacy_front.png")
    save(render(WEATHER, WEATHER_PAL), "block/weather_station_front.png")
    for tier, (light, dark) in TRIM.items():
        pal = {**LEATHER, "B": light, "b": dark}
        rows = [r.replace("L", "L") for r in BACKPACK]
        im = render(rows, pal)
        # 등급 색 테두리 줄 (가방 덮개 아래 선)
        for x in range(2, 14):
            im.putpixel((x, 8), tuple(int(light.lstrip("#")[k:k + 2], 16) for k in (0, 2, 4)) + (255,))
        save(im, f"item/{tier}_backpack.png")
    save(render(TELEPORTER, TELE_PAL), "item/teleporter.png")
    save(render(TICKET, TICKET_PAL), "item/lottery_ticket.png")
    save(render(BOARD, BOARD_PAL), "block/bounty_board_front.png")
    save(render(CRATE_SIDE, CRATE_PAL), "block/shipping_bin_side.png")
    save(render(CRATE_TOP, CRATE_PAL), "block/shipping_bin_top.png")


if __name__ == "__main__":
    build()
    print("ok")
