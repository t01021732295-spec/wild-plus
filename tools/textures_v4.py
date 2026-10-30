"""4차 업데이트 텍스처: 새 자판기(진열창 구조), 뽑기 기계, 자석 부적, 휴대용 제작대·엔더 상자."""
import random

from PIL import Image

from textures_v2 import ROOT, render, save
from textures_v3 import Skin, hexa

RED, RED_DARK, RED_LIGHT, EDGE = "#b8202a", "#7a121a", "#e0484e", "#3a080c"


def vending():
    # 정면 틀: 진열창(u1~11, v1~13)은 비어 있고, 오른쪽 u11~16 은 조작판, 아래 v13~16 은 꺼내는 곳
    f = Skin(16, 16, 51)
    f.rect(0, 0, 16, 16, RED, 0.04)
    f.rect(0, 0, 16, 1, RED_LIGHT)
    f.rect(1, 1, 10, 12, "#101010")              # (가려지는 부분)
    f.rect(11, 1, 5, 12, "#2a2a2e")              # 조작판
    f.rect(12, 2, 3, 2, "#3fd46a")               # 잔액 표시창
    f.px(12, 2, "#9affb0")
    for r in range(3):                            # 상품 버튼
        for c in range(2):
            f.px(12 + c * 2, 5 + r * 2, "#e0e0e0")
    f.rect(12, 11, 3, 1, "#111111")              # 동전 투입구
    f.px(13, 11, "#f2c230")
    f.rect(0, 13, 16, 3, RED_DARK)
    f.rect(2, 14, 8, 2, "#151515")               # 꺼내는 곳
    f.rect(2, 14, 8, 1, "#050505")
    f.rect(0, 15, 16, 1, EDGE)
    # 진열장 안쪽 벽 + 선반
    inner = Skin(16, 16, 52)
    inner.rect(0, 0, 16, 16, "#1e2f3a", 0.06)
    for v in (5, 9, 13):
        inner.rect(0, v, 16, 1, "#9aa4ae")
        inner.rect(0, v + 1, 16, 1, "#5a646e")
    # 유리: 거의 투명, 빛 반사 줄 몇 개
    glass = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(5):
        glass.putpixel((2 + i, 6 - i), hexa("#d8f0ff"))
        glass.putpixel((3 + i, 6 - i), hexa("#a8d8f0"))
    for i in range(3):
        glass.putpixel((7 + i, 12 - i), hexa("#d8f0ff"))
    side = Skin(16, 16, 53)
    side.rect(0, 0, 16, 16, RED, 0.05)
    side.rect(0, 0, 16, 1, RED_LIGHT)
    side.rect(0, 15, 16, 1, EDGE)
    side.rect(0, 0, 1, 16, EDGE)
    side.rect(15, 0, 1, 16, EDGE)
    side.rect(3, 4, 10, 7, "#f2f2f2")            # 옆면 광고판
    side.rect(4, 5, 8, 5, "#3a8adf")
    side.rect(5, 6, 6, 1, "#ffffff")
    side.rect(5, 8, 4, 1, "#f2c230")
    top = Skin(16, 16, 54)
    top.rect(0, 0, 16, 16, RED_DARK, 0.05)
    top.rect(1, 1, 14, 14, RED, 0.05)
    inside = Skin(16, 16, 55)
    inside.rect(0, 0, 16, 16, "#4a1016", 0.05)
    save(f.im, "block/vending_machine_front.png")
    save(inner.im, "block/vending_machine_interior.png")
    glass.save(ROOT / "block/vending_machine_glass.png")
    save(side.im, "block/vending_machine_side.png")
    save(top.im, "block/vending_machine_top.png")
    save(inside.im, "block/vending_machine_inner.png")


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
