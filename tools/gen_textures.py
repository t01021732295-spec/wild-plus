"""Wild Plus 텍스처 생성기 (16x16 픽셀아트 + 갑옷 레이어).

    python3 tools/gen_textures.py

글자 하나가 픽셀 하나. '.' 은 투명.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures"

PAL = {
    ".": None,
    # 핏빛 강철
    "K": "#24070b", "R": "#6e0f1c", "r": "#a81d2c", "p": "#d9424f", "w": "#ffb3b8",
    # 손잡이(나무)
    "b": "#3d2510", "B": "#6b4423", "L": "#8f6235",
    # 일반
    "k": "#1a1a1a", "g": "#555555", "G": "#8a8a8a", "W": "#f2f2f2",
    # 종이·두루마리
    "q": "#7a5c34", "P": "#e9d8a6", "Q": "#c8b07a", "m": "#7b3fbf", "M": "#b07cff",
    # 금
    "y": "#7a5a10", "Y": "#d9a21e", "O": "#ffe066",
    # 초록(잎)
    "e": "#1f4d16", "E": "#3e8a2a", "F": "#6cc14a",
    # 고추
    "c": "#5c0a0a", "C": "#c41e1e", "h": "#ff5a4a",
    # 그릇·음식
    "n": "#4a2c14", "N": "#7a4c26", "s": "#b8361c", "S": "#e2632f", "t": "#f0a040",
    "u": "#a86a2c", "U": "#d9a05a", "v": "#f2cf8a", "H": "#ffbf2e", "j": "#e08a10",
    "z": "#8a3a2a", "Z": "#c26a4a", "x": "#e8a080",
}


def img(rows):
    h, w = len(rows), len(rows[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == w, (row, len(row), w)
        for x, ch in enumerate(row):
            col = PAL[ch]
            if col:
                im.putpixel((x, y), tuple(int(col[i:i + 2], 16) for i in (1, 3, 5)) + (255,))
    return im


def save(path, rows):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    img(rows).save(p)


ITEMS = {
"blood_shard": [
"................",
"................",
"..........K.....",
".........KwK....",
"........KpwK....",
"...K...KrpK.....",
"..KpK..KrpK.....",
"..KrpK.KrrK.....",
"...KrpKRrpK.K...",
"...KRrprrpKKpK..",
"....KRrrrpKrpK..",
".....KRRrrrrK...",
"......KKRRRK....",
"........KKK.....",
"................",
"................"],
"bloodsteel_ingot": [
"................",
"................",
"................",
"................",
"......KKKKKKK...",
".....KwpppppRK..",
"....KwprrrrrRK..",
"...KwprrrrrRRK..",
"..KpppppppRRK...",
"..KrrrrrrrRK....",
"..KRRRRRRRK.....",
"...KKKKKKK......",
"................",
"................",
"................",
"................"],
"bloodsteel_sword": [
"................",
"............KKK.",
"...........KwpK.",
"..........KwpRK.",
".........KwpRK..",
"........KwpRK...",
".......KwpRK....",
"..KK..KwpRK.....",
"..KpKKwpRK......",
"...KpKpRK.......",
"....KbRK........",
"...KbBKpK.......",
"..KbBK.KpK......",
".KLBK...KK......",
".KKK............",
"................"],
"bloodsteel_pickaxe": [
"................",
"....KKKKK.......",
"...KpwwppKK.....",
"....KKKrrpRK....",
".......KKBrRK...",
"........KBKRK...",
".......KBK.KRK..",
"......KBK..KpK..",
".....KBK...KRK..",
"....KBK.....K...",
"...KBK..........",
"..KBK...........",
".KLK............",
".KK.............",
"................",
"................"],
"bloodsteel_axe": [
"................",
"......KKK.......",
".....KpwpKK.....",
"....KpwrrpK.....",
"....KprrRKBK....",
"....KrrRKBLK....",
".....KRKBKRK....",
"......KBK.K.....",
".....KBK........",
"....KBK.........",
"...KBK..........",
"..KBK...........",
".KLK............",
".KK.............",
"................",
"................"],
"chili_seeds": [
"................",
"................",
"................",
"................",
"................",
".......jt.......",
"....jt..tj......",
"......jt..jt....",
"...tj..jt.......",
".....tj..tj.....",
"....jt.jt...t...",
"......tj..jt....",
"........jt......",
"................",
"................",
"................"],
"bloodsteel_shovel": [
"................",
"..........KKK...",
".........KwppK..",
"........KwprpK..",
"........KprrRK..",
".........KRRK...",
"........KBKK....",
".......KBK......",
"......KBK.......",
".....KBK........",
"....KBK.........",
"...KBK..........",
"..KLK...........",
"..KK............",
"................",
"................"],
"bloodsteel_helmet": [
"................",
"................",
"................",
"....KKKKKKKK....",
"...KpwwpppprK...",
"..KprrrrrrrrRK..",
"..KrRKKKKKKRrK..",
"..KrK......KrK..",
"..KRK......KRK..",
"...K........K...",
"................",
"................",
"................",
"................",
"................",
"................"],
"bloodsteel_chestplate": [
"................",
"..KKK......KKK..",
".KpwrK....KrpRK.",
".KprrKKKKKKrrRK.",
".KrrrpwpprrrrRK.",
"..KKrprrrrrrKK..",
"....KprrrrrRK...",
"....KrrprrrRK...",
"....KrrrrrrRK...",
"....KprrrrrRK...",
"....KrrrrrrRK...",
"....KRRRRRRRK...",
".....KKKKKKK....",
"................",
"................",
"................"],
"bloodsteel_leggings": [
"................",
"....KKKKKKKK....",
"....KpwppprRK...",
"....KprrrrrRK...",
"....KrrKKrrRK...",
"....KrRK.KrRK...",
"....KrRK.KrRK...",
"....KrRK.KrRK...",
"....KpRK.KpRK...",
"....KrRK.KrRK...",
"....KRRK.KRRK...",
"....KKKK.KKKK...",
"................",
"................",
"................",
"................"],
"bloodsteel_boots": [
"................",
"................",
"................",
"................",
"................",
"...KKK....KKK...",
"...KpK....KpK...",
"...KrK....KrK...",
"...KrK....KrK...",
"..KprK...KprK...",
".KpwrRK.KpwrRK..",
".KrrrRK.KrrrRK..",
".KKKKKK.KKKKKK..",
"................",
"................",
"................"],
"recall_scroll": [
"................",
"................",
"...qqqqqqqqq....",
"..qPPPPPPPPPq...",
"..qQPPPPPPPPq...",
"...qPmmPPPPq....",
"...qPPPMmPPq....",
"...qPPmmPPPq....",
"...qPPPPmPPq....",
"...qPmMPPPPq....",
"...qPPPPPPPq....",
"...qPPPPPPPPq...",
"..qPPPPPPPPQq...",
"...qqqqqqqqq....",
"................",
"................"],
"lucky_charm": [
"................",
".......kk.......",
"......k..k......",
"......k..k......",
".......kk.......",
".......yy.......",
".....yYOOYy.....",
"....yYOEEOYy....",
"....YOEFFEOY....",
"....YOEFFEOY....",
"....yYOEEOYy....",
".....yYOOYy.....",
".......yy.......",
"................",
"................",
"................"],
"traveler_ration": [
"................",
"................",
"................",
"................",
"...nnnnnnnnnn...",
"..nUUUUUUUUUUn..",
"..nUvvUUvvUUUn..",
"..nUUUUUUUUUUn..",
"..nQQQQEQQQQQn..",
"..nUUUUUUUUUUn..",
"..nUvvUUUvvUUn..",
"..nuuuuuuuuuun..",
"...nnnnnnnnnn...",
"................",
"................",
"................"],
"chili": [
"................",
"..........e.....",
".........eE.....",
"........eEF.....",
".......cCCe.....",
"......cChCC.....",
".....cChCCc.....",
"....cChCCc......",
"...cCCCCc.......",
"..cCCCCc........",
"..cCCCc.........",
".cCCcc..........",
".cCc............",
".cc.............",
"................",
"................"],
"spicy_stew": [
"................",
"................",
"................",
"................",
"................",
"..nnnnnnnnnnnn..",
".nsSSStSSCSSSsn.",
".nsSCSSSStSSSsn.",
".nNsSSSSSSSSsNn.",
"..nNNNNNNNNNNn..",
"..nNNNNNNNNNNn..",
"...nNNNNNNNNn...",
"....nnnnnnnn....",
"................",
"................",
"................"],
"honey_toast": [
"................",
"................",
"....uuuuuuuu....",
"...uUUUUUUUUu...",
"..uUjHHHHHHjUu..",
"..uUHHOHHHHHUu..",
"..uUHHHHHOHjUu..",
"..uUjHHHHHHHUu..",
"..uUUjHHHHHjUu..",
"..uUvjjHHjjvUu..",
"..uUvvvvvvvvUu..",
"..uUvvvvvvvvUu..",
"..uuuuuuuuuuuu..",
"................",
"................",
"................"],
"raw_skewer": [
"................",
"..............L.",
".............L..",
"..........xZxL..",
".........xZZxx..",
"........xZxL....",
"......xZxL......",
".....xZZxx......",
"....xZxL........",
"...CchL.........",
"...cCL..........",
"...LL...........",
"..L.............",
".L..............",
"................",
"................"],
"spicy_skewer": [
"................",
"..............L.",
".............L..",
"..........zszL..",
".........zssZz..",
"........zsZL....",
"......zszL......",
".....zssZz......",
"....zsZL........",
"...CchL.........",
"...cCL..........",
"...LL...........",
"..L.............",
".L..............",
"................",
"................"],
}

CROP = {
"chili_crop_stage0": [
"................",
"................",
"................",
"................",
"................",
"................",
"................",
"................",
"................",
"................",
"...F.......F....",
"...E...F...E..F.",
"..eE..FE..eE..E.",
"..e...E...e..eE.",
"..e..eE...e..e..",
"..e..e....e..e.."],
"chili_crop_stage1": [
"................",
"................",
"................",
"................",
"................",
"................",
"..F.......F.....",
"..EF..F...EF..F.",
"..eE.FE...eE.FE.",
"..e..E.F..e..E..",
"..eE.e.E..eE.e..",
"..e..eE...e..eE.",
"..e..e....e..e..",
"..e..e....e..e..",
"..e..e....e..e..",
"..e..e....e..e.."],
"chili_crop_stage2": [
"................",
"................",
"................",
".F......F.......",
".EF..F..EF..F...",
".eE.FE..eE.FE.F.",
".e..E.F.e..E.FE.",
".eE.e.E.eE.e.E..",
".e..eEh.e..eE...",
".e..eCC.e..e..F.",
".eE.e...eE.e.FE.",
".e..eE..e..eE.e.",
".e..e...e..e..e.",
".e..e...e..e..e.",
".e..e...e..e..e.",
".e..e...e..e..e."],
"chili_crop_stage3": [
"................",
".F......F.......",
".EF..F..EF..F...",
".eE.FE..eE.FE.F.",
".eh.E.F.e..E.FE.",
".CC.e.E.eh.e.E..",
".Cc.eE..CC.eEh..",
".c..eCh.Cc.eCC..",
".eE.CC..c..eCc..",
".e..Cc..eE.ec.F.",
".eE.c...e..e.FE.",
".e..eE..e..eE.e.",
".e..e...e..e..e.",
".e..e...e..e..e.",
".e..e...e..e..e.",
".e..e...e..e..e."],
}


def armor_layer(leggings: bool):
    """64x32 휴머노이드 갑옷 레이어 (바닐라 UV 배치)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    dark, mid, light, hi = (0x6e, 0x0f, 0x1c), (0xa8, 0x1d, 0x2c), (0xd9, 0x42, 0x4f), (0xff, 0xb3, 0xb8)
    edge = (0x24, 0x07, 0x0b)

    def box(u, v, w, h, d):
        # 상자 전개도: 위/아래, 그리고 좌/앞/우/뒤 면
        faces = [
            (u + d, v, w, d), (u + d + w, v, w, d),
            (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + d + w + d, v + d, w, h),
        ]
        for (fx, fy, fw, fh) in faces:
            for x in range(fx, fx + fw):
                for y in range(fy, fy + fh):
                    if not (0 <= x < 64 and 0 <= y < 32):
                        continue
                    border = x in (fx, fx + fw - 1) or y in (fy, fy + fh - 1)
                    shade = mid
                    if (x + y) % 7 == 0:
                        shade = light
                    if (x * 3 + y) % 11 == 0:
                        shade = dark
                    im.putpixel((x, y), (edge if border else shade) + (255,))
            # 하이라이트 줄
            for x in range(fx + 1, fx + fw - 1):
                if fh > 2 and 0 <= fy + 1 < 32:
                    im.putpixel((x, fy + 1), hi + (255,))

    if leggings:
        box(16, 16, 8, 12, 4)   # 허리(몸통 아랫부분)
        box(0, 16, 4, 12, 4)    # 다리
    else:
        box(0, 0, 8, 8, 8)      # 머리
        box(16, 16, 8, 12, 4)   # 몸통
        box(40, 16, 4, 12, 4)   # 팔
        box(0, 16, 4, 12, 4)    # 다리(부츠)
        # 투구 얼굴 구멍
        for x in range(9, 15):
            for y in range(11, 13):
                im.putpixel((x, y), (0, 0, 0, 0))
    return im


def main():
    for name, rows in ITEMS.items():
        save(f"item/{name}.png", rows)
    for name, rows in CROP.items():
        save(f"block/{name}.png", rows)
    eq = ROOT / "entity/equipment"
    (eq / "humanoid").mkdir(parents=True, exist_ok=True)
    (eq / "humanoid_leggings").mkdir(parents=True, exist_ok=True)
    armor_layer(False).save(eq / "humanoid/bloodsteel.png")
    armor_layer(True).save(eq / "humanoid_leggings/bloodsteel.png")
    import textures_v2
    textures_v2.build()
    print("ok", len(ITEMS), "items,", len(CROP), "crop stages + v2")


if __name__ == "__main__":
    main()
