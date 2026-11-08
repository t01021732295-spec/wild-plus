"""4.1 데이터 (data_v34): 큰 ATM · 약국 매대 모델, 신규 포션 5종 양조 · 아이콘 · 언어."""
import json
import random

from PIL import Image

import kitchen_models
from cosmetic_design import Box, solid
from data_v2 import ASSETS, DATA, MC, i, merge_tag, self_loot, write
from data_v10 import brew

SIL, SD, NV, BL, D, GN, W, WD, WD2 = (205, 210, 218), (150, 156, 166), (34, 62, 120), (70, 140, 235), (40, 44, 54), (40, 160, 90), (245, 245, 240), (150, 108, 66), (118, 82, 48)
COL = [(220, 70, 70), (70, 120, 220), (90, 190, 110), (240, 190, 70), (170, 100, 210), (240, 240, 230)]


def screen(f, u, v, w, h):
    if f != "front":
        return D
    if v < 3:
        return NV
    if 3 <= v <= 4 and 1 <= u < w - 1:
        return W
    if 6 <= v <= h - 2 and u % 3 == 1 and 1 <= u < w - 1:
        return BL
    return (28, 70, 150)


def keys(f, u, v, w, h):
    if f != "top":
        return SD
    if u % 3 != 2 and v % 3 != 2:
        return (250, 200, 70) if u // 3 == 2 and v // 3 == 0 else (240, 70, 70) if u // 3 == 2 and v // 3 == 2 else (235, 238, 242)
    return (60, 66, 78)


def sign(f, u, v, w, h):
    if f != "front":
        return NV
    glyphs = ((".#.", "#.#", "###", "#.#", "#.#"), ("###", ".#.", ".#.", ".#.", ".#."), ("#...#", "##.##", "#.#.#", "#...#", "#...#"))
    on = set()
    for g, ox in zip(glyphs, (1, 6, 10)):
        for yy, row in enumerate(g):
            for xx, ch in enumerate(row):
                if ch == "#":
                    on.add((ox + xx, yy))
    return W if (u, v) in on else NV


def atm():
    return [Box(-8, 0, -7, 16, 4, 14, solid(SD)), Box(-7.5, 4, -6.5, 15, 24, 13, solid(SIL)), Box(-8, 27, -7, 16, 5, 14, sign),
            Box(-6, 17, 6.5, 12, 8, .5, screen), Box(-6, 13, 6, 8, 1, 3.5, keys), Box(3, 14, 6.5, 3, 1, .3, solid(D)), Box(3, 12, 6.5, 3, .6, .3, solid(BL)),
            Box(-5, 7, 6.5, 10, 3, .4, lambda f, u, v, w, h: (30, 32, 38) if v < h - 1 else (80, 86, 96)), Box(-2, 7.3, 6.9, 1.5, 1.3, .2, solid(GN))]


def cross(f, u, v, w, h):
    if f not in ("front", "back"):
        return WD2
    cx, cy = w / 2 - .5, h / 2 - .5
    return (250, 250, 250) if (abs(u - cx) < 1.2 and abs(v - cy) < 3.2) or (abs(v - cy) < 1.2 and abs(u - cx) < 3.2) else GN


def rack():
    r = random.Random(3)
    b = [Box(-24, 0, -8, 48, 4, 16, solid(WD2)), Box(-24, 4, -8, 2, 32, 16, solid(WD)), Box(22, 4, -8, 2, 32, 16, solid(WD)), Box(-24, 4, -8, 48, 32, 1.5, solid(WD2)), Box(-24, 36, -8, 48, 2, 16, solid(WD))]
    for y in (4, 14, 24):
        if y > 4:
            b.append(Box(-22, y, -6.5, 44, 1.4, 14, solid(WD)))
        base = y + 1.4 if y > 4 else y
        x = -20
        while x < 19:
            c, k = r.choice(COL), r.random()
            if k < .35:
                b += [Box(x, base, 0, 3, 5, 3, solid(c)), Box(x + .5, base + 5, .5, 2, 1.5, 2, solid(W))]
            elif k < .7:
                b.append(Box(x, base, -3, 5, 4, 4, lambda f, u, v, w, h, c=c: W if 1 < v < h - 1 else c))
            else:
                b += [Box(x, base, 1, 2.5, 6, 2.5, solid(c)), Box(x + .5, base + 6, 1.5, 1.5, 1, 1.5, solid(D))]
            x += 6
    b.append(Box(-9, 38, -3, 18, 10, 2, cross))
    return b


POTIONS = {
    "void_evade": ("공허 회피", "Void Evasion", (122, 70, 210), "minecraft:awkward", i("void_shard")),
    "heal_aura": ("회복 오라", "Healing Aura", (242, 120, 150), "minecraft:awkward", "minecraft:golden_apple"),
    "iron_wall": ("철벽의 숨결", "Iron Wall", (140, 150, 170), i("ironskin"), "minecraft:netherite_scrap"),
    "ore_sight": ("광맥의 눈", "Ore Sight", (90, 210, 220), "minecraft:awkward", "minecraft:diamond"),
    "time_warp": ("시간 왜곡", "Time Warp", (250, 200, 90), "minecraft:awkward", "minecraft:clock"),
}
DESC = {
    "void_evade": "맞을 때 25% 확률로 피해 무효 + 뒤로 순간이동 (8초에 1번)",
    "heal_aura": "주변 5칸 플레이어에게 재생 I",
    "iron_wall": "넉백 무효 · 방어력 +6",
    "ore_sight": "주변 12칸 광석을 벽 너머로 빛나게 표시",
    "time_warp": "주변 적의 공격 · 이동 30% 감소, 내 공격 속도 · 이동 속도 30% 증가",
}
KINDS = {"potion": "의 물약", "splash_potion": "의 투척 물약", "lingering_potion": "의 잔류 물약", "tipped_arrow": "의 화살"}
KINDS_EN = {"potion": "Potion of ", "splash_potion": "Splash Potion of ", "lingering_potion": "Lingering Potion of ", "tipped_arrow": "Arrow of "}


def effect_icon(c):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            d = (x - 8.5) ** 2 + (y - 8.5) ** 2
            if d < 45:
                im.putpixel((x, y), (tuple(min(255, v + 70) for v in c) if d < 10 else c if d < 30 else tuple(v // 2 for v in c)) + (255,))
    return im


def build(lang_ko, lang_en):
    kitchen_models.export("atm_tall", atm, 0, 0, 0, 0, 2)
    kitchen_models.export("pharmacy_rack", rack, -1, 1, 0, 0, 3)
    ko = {"block.wildplus.atm_tall": "은행 ATM", "tooltip.wildplus.atm_tall": "예금 · 출금 · 송금 · 입출금 내역 · 대출", "block.wildplus.pharmacy_rack": "약국 매대",
          "tooltip.wildplus.pharmacy_rack": "가로 3칸 · 높이 3칸 약 진열대. 약국 이용"}
    en = {"block.wildplus.atm_tall": "Bank ATM", "block.wildplus.pharmacy_rack": "Pharmacy Rack"}
    for n in ("atm_tall", "pharmacy_rack"):
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [i("atm_tall")])
    merge_tag(MC / "tags/block/mineable/axe.json", [i("pharmacy_rack")])
    for old, new in (("atm", "atm_tall"), ("pharmacy", "pharmacy_rack")):
        p = DATA / "recipe" / f"{old}.json"
        d = json.loads(p.read_text())
        d["result"]["id"] = i(new)
        write(p, d)
    icons = ASSETS / "textures/mob_effect"
    for n, (k, e, c, base, reagent) in POTIONS.items():
        effect_icon(c).save(icons / f"{n}.png")
        ko[f"effect.wildplus.{n}"], en[f"effect.wildplus.{n}"] = k, e
        for var in (n, f"long_{n}"):
            for kind, suf in KINDS.items():
                ko[f"item.minecraft.{kind}.effect.wildplus_{var}"] = k + suf
                en[f"item.minecraft.{kind}.effect.wildplus_{var}"] = KINDS_EN[kind] + e
        for c_ in ("potion", "splash_potion", "lingering_potion"):
            write(DATA / "recipe" / f"brewing/{c_}_{base.split(':')[1]}_{reagent.split(':')[1]}.json", brew(f"minecraft:{c_}", base, reagent, f"minecraft:{c_}", i(n)))
            write(DATA / "recipe" / f"brewing/{c_}_{n}_redstone.json", brew(f"minecraft:{c_}", i(n), "minecraft:redstone", f"minecraft:{c_}", i(f"long_{n}")))
        for p in (n, f"long_{n}"):
            write(DATA / "recipe" / f"brewing/potion_{p}_gunpowder.json", brew("minecraft:potion", i(p), "minecraft:gunpowder", "minecraft:splash_potion", i(p)))
            write(DATA / "recipe" / f"brewing/splash_potion_{p}_dragon_breath.json", brew("minecraft:splash_potion", i(p), "minecraft:dragon_breath", "minecraft:lingering_potion", i(p)))
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **ko, "effect.wildplus.void_evade.description": DESC["void_evade"]})
    write(ASSETS / "lang/en_us.json", {**lang_en, **en})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v34")
