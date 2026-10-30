"""4차 업데이트 텍스처: 새 자판기(진열창 구조), 뽑기 기계, 자석 부적, 휴대용 제작대·엔더 상자."""
import random

from PIL import Image

from textures_v2 import ROOT, render, save
from textures_v3 import Skin, hexa

RED, RED_DARK, RED_LIGHT, EDGE = "#b8202a", "#7a121a", "#e0484e", "#3a080c"


def vending():
    """실물 크기(2칸 높이) 자판기. 16x32 로 그려서 위·아래 칸으로 자른다.
    모델 좌표 y(0~32) ↔ 텍스처 행 r = 31 - y, 모델 x(0~16) ↔ 열 u = 15 - x.
    진열창: x 4~15(u 1~11), y 6~29(행 3~25). 조작판: x 0~4(u 12~15)."""
    f = Skin(16, 32, 51)
    f.rect(0, 0, 16, 32, RED, 0.04)
    f.rect(0, 0, 16, 1, RED_LIGHT)
    f.rect(1, 1, 14, 1, "#fff2c8")               # 맨 위 불 켜진 간판 띠
    for u in (3, 6, 9, 12):
        f.px(u, 1, "#f2c230")
    f.rect(1, 3, 11, 23, "#101010")              # (진열창 뒤 - 가려짐)
    f.rect(12, 3, 4, 23, "#2a2a2e", 0.04)        # 조작판
    f.rect(12, 4, 3, 2, "#3fd46a")               # 잔액 표시창
    f.px(12, 4, "#9affb0")
    for r in range(5):                            # 상품 선택 버튼
        for c in range(2):
            f.px(12 + c * 2, 8 + r * 2, "#e8e8e8")
            f.px(13 + c * 2, 8 + r * 2, "#c83030")
    f.rect(13, 18, 1, 3, "#050505")              # 동전 투입구(세로)
    f.px(14, 18, "#f2c230")
    f.rect(12, 22, 3, 1, "#050505")              # 지폐 투입구
    f.px(13, 24, "#c8c8c8")                      # 반환 레버
    f.px(14, 24, "#8a8a8a")
    f.rect(0, 26, 16, 6, RED_DARK, 0.04)         # 아래 틀
    for u in (2, 6, 9):                           # 맨 아래 선반 가격표
        f.rect(u, 26, 2, 1, "#f4f4f4")
    f.rect(2, 28, 9, 3, "#151515")               # 꺼내는 곳
    f.rect(2, 28, 9, 1, "#3a3a3a")               # 덮개
    f.rect(5, 29, 3, 1, "#5a5a5a")               # 덮개 손잡이
    f.rect(0, 31, 16, 1, EDGE)
    f.rect(0, 0, 1, 32, EDGE)
    f.rect(15, 0, 1, 26, RED_DARK)

    inner = Skin(16, 32, 52)                      # 진열장 안쪽 뒷벽
    inner.rect(0, 0, 16, 32, "#1e2f3a", 0.06)
    inner.rect(1, 3, 11, 1, "#fff6d0")           # 천장 조명
    inner.rect(1, 4, 11, 1, "#8a9aa0")
    for u in (4, 8):                              # 칸막이 그림자
        inner.rect(u, 5, 1, 21, "#17252e")

    shelf = Skin(16, 16, 56)                      # 선반: 0행 = 앞쪽 모서리(가격표), 1~3행 = 윗면
    shelf.rect(0, 0, 16, 16, "#9aa4ae", 0.03)
    shelf.rect(0, 0, 16, 1, "#5a646e")
    for u in (2, 6, 9):
        shelf.rect(u, 0, 2, 1, "#f4f4f4")

    glass = Image.new("RGBA", (16, 32), (0, 0, 0, 0))   # 거의 투명 + 빛 반사 줄
    for i in range(7):
        glass.putpixel((2 + i, 11 - i), hexa("#d8f0ff"))
        glass.putpixel((3 + i, 11 - i), hexa("#a8d8f0"))
    for i in range(4):
        glass.putpixel((6 + i, 23 - i), hexa("#d8f0ff"))

    side = Skin(16, 32, 53)                       # 옆면: 큰 광고판
    side.rect(0, 0, 16, 32, RED, 0.05)
    side.rect(0, 0, 16, 1, RED_LIGHT)
    side.rect(0, 31, 16, 1, EDGE)
    side.rect(0, 0, 1, 32, EDGE)
    side.rect(15, 0, 1, 32, EDGE)
    side.rect(2, 4, 12, 20, "#f2f2f2")
    side.rect(3, 5, 10, 18, "#3a8adf")
    side.rect(4, 6, 8, 1, "#ffffff")
    side.rect(4, 8, 5, 1, "#f2c230")
    side.rect(7, 11, 3, 1, "#d8f0ff")            # 음료병
    side.rect(6, 12, 5, 9, "#d8f0ff")
    side.rect(6, 15, 5, 3, "#d83030")
    side.rect(0, 26, 16, 1, RED_DARK)

    top = Skin(16, 16, 54)
    top.rect(0, 0, 16, 16, RED_DARK, 0.05)
    top.rect(1, 1, 14, 14, RED, 0.05)
    inside = Skin(16, 16, 55)
    inside.rect(0, 0, 16, 16, "#4a1016", 0.05)

    for name, im in (("front", f.im), ("interior", inner.im), ("side", side.im)):
        save(im.crop((0, 0, 16, 16)), f"block/vending_machine_{name}_upper.png")
        save(im.crop((0, 16, 16, 32)), f"block/vending_machine_{name}_lower.png")
    glass.crop((0, 0, 16, 16)).save(ROOT / "block/vending_machine_glass_upper.png")
    glass.crop((0, 16, 16, 32)).save(ROOT / "block/vending_machine_glass_lower.png")
    save(shelf.im, "block/vending_machine_shelf.png")
    save(top.im, "block/vending_machine_top.png")
    save(inside.im, "block/vending_machine_inner.png")
    # 아이템 아이콘: 자판기 전체를 앞에서 본 모습
    icon = f.im.resize((8, 16), Image.NEAREST)
    ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ic.paste(icon, (4, 0))
    shown = Skin(16, 16, 57)
    shown.im = ic
    for (u, v, c) in ((5, 3, "#ffd84a"), (7, 3, "#7cfc6a"), (9, 3, "#ff8ad8"),
                      (5, 7, "#5ac8ff"), (7, 7, "#ffffff"), (9, 7, "#ff5a5a"),
                      (5, 11, "#ffa64a"), (7, 11, "#7cfc6a"), (9, 11, "#5ac8ff")):
        shown.px(u, v, c)
    for v in (2, 6, 10):
        for u in range(5, 10):
            if ic.getpixel((u, v))[3]:
                shown.px(u, v, "#cfe8f6")
    save(shown.im, "item/vending_machine.png")
    for old in ("front", "interior", "glass", "side"):
        (ROOT / f"block/vending_machine_{old}.png").unlink(missing_ok=True)


def gacha():
    f = Skin(16, 16, 61)
    f.rect(0, 0, 16, 16, "#2f6fd0", 0.04)
    # 둥근 유리 돔
    rnd = random.Random(7)
    colors = ["#ff5a5a", "#ffd84a", "#7cfc6a", "#5ac8ff", "#ff8ad8", "#ffffff"]
    for y in range(1, 9):
        for x in range(1, 15):
            dx, dy = (x - 7.5) / 6.8, (y - 8.5) / 7.8
            if dx * dx + dy * dy <= 1.0:
                f.px(x, y, "#cfe8f6" if (x + y) % 5 else "#ffffff")
    for _ in range(16):                           # 캡슐
        x, y = rnd.randint(3, 12), rnd.randint(4, 8)
        dx, dy = (x - 7.5) / 6.0, (y - 8.5) / 7.0
        if dx * dx + dy * dy <= 0.9:
            c = rnd.choice(colors)
            f.px(x, y, c)
            if x + 1 < 14:
                f.px(x + 1, y, "#ffffff")
    f.rect(0, 9, 16, 7, "#d23a3a", 0.05)         # 받침
    f.rect(0, 9, 16, 1, "#ff6a6a")
    f.rect(3, 11, 3, 3, "#c8c8c8")               # 손잡이
    f.px(4, 12, "#6a6a6a")
    f.rect(9, 11, 4, 1, "#111111")               # 동전 투입구
    f.px(10, 11, "#f2c230")
    f.rect(9, 13, 4, 2, "#151515")               # 캡슐 나오는 곳
    side = Skin(16, 16, 62)
    side.rect(0, 0, 16, 9, "#2f6fd0", 0.05)
    side.rect(1, 1, 14, 7, "#cfe8f6")
    side.rect(0, 9, 16, 7, "#d23a3a", 0.05)
    side.rect(0, 9, 16, 1, "#ff6a6a")
    top = Skin(16, 16, 63)
    top.rect(0, 0, 16, 16, "#2f6fd0", 0.05)
    top.rect(3, 3, 10, 10, "#cfe8f6")
    top.rect(6, 6, 4, 4, "#ffd84a")
    save(f.im, "block/gacha_machine_front.png")
    save(side.im, "block/gacha_machine_side.png")
    save(top.im, "block/gacha_machine_top.png")


MAGNET = [
"................", "................", "...kkkk..kkkk...", "...kWWk..kWWk...",
"...kWWk..kWWk...", "...kRRk..kRRk...", "...kRRk..kRRk...", "...kRRk..kRRk...",
"...kRRRkkRRRk...", "....kRRRRRRk....", ".....kRRRRk.....", "......kkkk......",
"................", ".......c........", "......ccc.......", ".......c........"]
MAGNET_PAL = {"k": "#2a0a0a", "W": "#e8e8f0", "R": "#d83030", "c": "#7fe3ff"}
CRAFT = [
"................", "................", "...kkkkkkkkkk...", "..kTtTtTtTtTtk..",
"..ktGgGgGgGgTk..", "..kTgkkgkkgtTk..", "..ktGgGgGgGgTk..", "..kSSSSSSSSSSk..",
"..kWWWWWWWWWWk..", "..kWBWWWWWBWWk..", "..kWWWWWWWWWWk..", "..kWWBWWWWWBWk..",
"..kWWWWWWWWWWk..", "...kkkkkkkkkk...", "................", "................"]
CRAFT_PAL = {"k": "#2a1a0a", "T": "#a8743e", "t": "#8a5a2a", "G": "#6a4a2a", "g": "#4a3018",
             "S": "#3a2414", "W": "#b8885a", "B": "#5a3a1a"}
ENDER = [
"................", "................", "...kkkkkkkkkk...", "..kOoOoOoOoOok..",
"..koDDDDDDDDok..", "..kODDDDDDDDOk..", "..kkkkkGGkkkkk..", "..kODDDGgDDDOk..",
"..koDDDDDDDDok..", "..kODDDDDDDDOk..", "..koDDDDDDDDok..", "..kODDDDDDDDOk..",
"..kOoOoOoOoOok..", "...kkkkkkkkkk...", "................", "................"]
ENDER_PAL = {"k": "#05080a", "O": "#1f3a3a", "o": "#152a2a", "D": "#0d1a1a", "G": "#3fd4a8", "g": "#a8ffe8"}


def build():
    vending()
    gacha()
    save(render(MAGNET, MAGNET_PAL), "item/magnet_charm.png")
    save(render(CRAFT, CRAFT_PAL), "item/portable_crafting_table.png")
    save(render(ENDER, ENDER_PAL), "item/portable_ender_chest.png")


if __name__ == "__main__":
    build()
    print("ok")
