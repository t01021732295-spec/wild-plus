"""4.2 데이터 (data_v35): 각인석 · 공허 깃 · 군주 조각 · 금고 자물쇠 · 엔드 시티 상자 추가 전리품."""
import json

from PIL import Image

from data_v2 import ASSETS, DATA, i, write
from data_v18 import simple

TEX = ASSETS / "textures"
VOID = {"abyss": ((90, 30, 170), "심연"), "devour": ((190, 40, 90), "포식"), "chain": ((70, 200, 230), "연쇄 균열"), "warp": ((150, 90, 240), "차원 도약"), "heart": ((230, 70, 150), "공허 심장")}
COMMON = {"harvest": ((110, 190, 80), "수확"), "fury": ((225, 90, 50), "맹공"), "guard": ((150, 160, 175), "견고"), "vigor": ((235, 110, 120), "활력"), "rush": ((240, 205, 80), "질주")}
EFFECT = {"abyss": "공허 검: 공허 베기 범위 +2칸 · 피해 +30%", "devour": "공허 검: 표식 걸린 적 처치 시 체력 4 회복", "chain": "공허 활: 폭발이 주변 적 1명에게 번짐", "warp": "공허 갑옷: 공허 도약 쿨타임 -40%", "heart": "공허 갑옷: 치명타를 입으면 3초 무적 (10분에 1번)",
          "harvest": "공허 장비 외 도구: 드롭 +10%", "fury": "공허 장비 외 무기: 체력이 가득 찬 적에게 피해 +25%", "guard": "공허 장비 외 갑옷: 받는 피해 -5% (부위마다, 최대 -20%)", "vigor": "공허 장비 외 갑옷: 최대 체력 +2칸 (1번만)", "rush": "공허 장비 외 신발: 이동속도 +8%"}
LANG_KO = {"item.wildplus.void_feather": "공허 깃", "tooltip.wildplus.void_feather": "강화대: 겉날개에 공허 활공 각인\n겉날개 내구도가 닳지 않음",
           "item.wildplus.lord_shard": "군주의 왕관 조각", "tooltip.wildplus.lord_shard": "공허 군주를 부르는 재료의 조각",
           "block.wildplus.vault_lock": "금고 자물쇠"}
LANG_EN = {"item.wildplus.void_feather": "Void Feather", "item.wildplus.lord_shard": "Lord's Crown Shard", "block.wildplus.vault_lock": "Vault Lock"}


def stone_icon(c, frame):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dark = tuple(int(v * .45) for v in c)
    light = tuple(min(255, int(v * 1.4) + 30) for v in c)
    rows = ["......####......", ".....#oooo#.....", "....#oLLooo#....", "...#oLooooox#...", "..#oLoo##ooox#..", "..#ooo#oo#oox#..", "..#ooo#oo#oox#..", "..#oooo##ooox#..", "...#ooooooox#...", "....#ooooox#....", ".....#oox#......", "......##........"]
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            col = {"#": frame, "o": c, "L": light, "x": dark}.get(ch)
            if col:
                im.putpixel((x, y + 2), col + (255,))
    return im


def feather():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for k in range(11):
        x, y = 3 + k, 13 - k
        im.putpixel((x, y), (220, 220, 240, 255))
        for w in range(1, 4 - abs(k - 5) // 2):
            if y + w < 16:
                im.putpixel((x, y + w), (130, 70, 220, 255))
            if x - w >= 0:
                im.putpixel((x - w, y), (70, 200, 230, 255))
    return im


def shard():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in [(6, 3), (7, 3), (8, 4), (5, 4), (6, 4), (7, 4), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (7, 10)]:
        im.putpixel((x, y), (236, 196, 96, 255) if (x + y) % 4 else (255, 235, 160, 255))
    return im


def lock_tex():
    im = Image.new("RGB", (16, 16), (30, 22, 50))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                im.putpixel((x, y), (236, 196, 96))
            elif abs(x - 7.5) + abs(y - 7.5) < 5:
                im.putpixel((x, y), (96, 232, 238) if abs(x - 7.5) + abs(y - 7.5) < 2.5 else (136, 72, 226))
    return im


def build(lang_ko, lang_en):
    ko, en = {**lang_ko, **LANG_KO}, {**lang_en, **LANG_EN}
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    for frame, d, label in (((236, 196, 96), VOID, "공허 전용"), ((175, 180, 195), COMMON, "공용")):
        for k, (c, name) in d.items():
            n = f"engrave_{k}"
            stone_icon(c, frame).save(TEX / f"item/{n}.png")
            simple(n)
            ko[f"item.wildplus.{n}"] = f"{name} 각인석"
            en[f"item.wildplus.{n}"] = f"{k.title()} Engrave Stone"
            ko[f"tooltip.wildplus.{n}"] = f"[{label}] 강화대 재료 칸에 넣어 새김\n{EFFECT[k]}"
    feather().save(TEX / "item/void_feather.png")
    shard().save(TEX / "item/lord_shard.png")
    lock_tex().save(TEX / "block/vault_lock.png")
    simple("void_feather")
    simple("lord_shard")
    write(ASSETS / "models/block/vault_lock.json", {"parent": "minecraft:block/cube_all", "textures": {"all": i("block/vault_lock")}})
    write(ASSETS / "blockstates/vault_lock.json", {"variants": {"": {"model": i("block/vault_lock")}}})
    write(ASSETS / "lang/ko_kr.json", ko)
    write(ASSETS / "lang/en_us.json", en)

    def e(name, w, lo=1, hi=1):
        return {"type": "minecraft:item", "name": name, "weight": w, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    stones = [e(i(f"engrave_{k}"), 3) for k in COMMON] + [e(i(f"engrave_{k}"), 1) for k in VOID]
    write(DATA / "loot_table/inject/end_city_v42.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [e(i("void_shard"), 10, 3, 8), e(i("raw_void_stone"), 3, 1, 3), {"type": "minecraft:empty", "weight": 4}]},
        {"rolls": 1, "entries": stones + [{"type": "minecraft:empty", "weight": 60}]},
        {"rolls": 1, "entries": [e(i("lord_shard"), 1), {"type": "minecraft:empty", "weight": 6}]},
    ], "random_sequence": i("inject/end_city_v42")})
    write(DATA / "loot_modifiers/end_city_v42.json", {"type": "neoforge:add_table", "condition": {"type": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/end_city_treasure"}, "table": i("inject/end_city_v42")})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v35")
