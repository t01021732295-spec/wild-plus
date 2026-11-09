"""4.5 데이터 (data_v38): 복권 발매기 · 운명의 제단 모델, 설명, 제작법."""
import json
import math

import kitchen_models
from cosmetic_design import Box, solid as S
from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, shaped, write

GD=(232,186,70);GD2=(170,120,40);RD=(200,40,50);WH=(238,234,226);DK=(30,28,36);DK2=(58,54,66);CY=(90,225,235);PU=(140,70,230)


def screen(lines,bg=(18,30,60),fg=CY):
    return lambda f,u,v,w,h:(fg if f=='front' and v%3==1 and 1<u<w-2 and (u*3+v)%lines else bg)


def lottery():
    # 복권 발매기 1x1x2
    lot=[Box(-8,0,-8,16,4,16,S(DK2)),Box(-8,4,-8,16,20,16,lambda f,u,v,w,h:RD if v%6<5 else GD),Box(-7,24,-7,14,8,14,S(WH)),
    Box(-6,12,8,12,10,.5,screen(4)),Box(-7,6,8,14,3,1.5,S(DK)),Box(-5,6.5,9.5,10,1,.3,S(GD)),
    Box(-8,32,-8,16,3,16,S(GD)),Box(-6,35,-6,12,10,12,lambda f,u,v,w,h:GD if (u+v)%4==0 else (250,225,120)),Box(-8,45,-8,16,2,16,S(RD))]
    for i,c in enumerate([(240,80,80),(240,200,70),(90,200,110),(90,150,240),(190,100,230)]):
        lot.append(Box(-6+i*2.6,26,7.2,2,2,1,S(c)));lot.append(Box(-6+i*2.6,29,7.2,2,1,.7,S(WH)))
    return lot


def altar():
    # 랜덤 인챈트
    ench=[Box(-11,0,-11,22,4,22,S((40,30,70))),Box(-9,4,-9,18,8,18,lambda f,u,v,w,h:PU if (u+v)%5==0 else (30,24,56)),Box(-11,12,-11,22,2,22,S(GD)),Box(-7,14,-7,14,2,14,S((70,20,20))),Box(-5,16,-4,10,2,8,S(WH)),Box(-.4,16,-4.5,.8,2.4,9,S(RD))]
    for i in range(8):
        a=i*math.pi/4; ench.append(Box(10*math.cos(a)-1,26+3*math.sin(i)+0,10*math.sin(a)-1,2,2,2,S(CY if i%2 else PU)))
    ench+=[Box(-3,23,-3,6,6,6,lambda f,u,v,w,h:GD if u in(0,w-1) or v in(0,h-1) else (DK if (u,v) in((2,4),(3,4),(3,3),(3,2)) else WH))]
    for s,z in((-1,-9),(1,-9),(-1,9),(1,9)): ench+=[Box(s*9-1,14,z-1,2,12,2,S(GD2))]
    return ench


KO = {"block.wildplus.lottery_machine": "복권 발매기", "tooltip.wildplus.lottery_machine": "즉석 복권 · 숫자 복권(토요일 오후 8시 45분 추첨)",
      "block.wildplus.fate_altar": "운명의 제단", "tooltip.wildplus.fate_altar": "장비를 들고 우클릭: 3~5만동에 랜덤 인챈트 1개 (1~15단계)",
      "tooltip.wildplus.lottery_ticket": "우클릭으로 긁기. 최고 500,000동!",
      "tooltip.wildplus.void_pickaxe": "3×3 채굴 · 원석 자동 제련 (웅크리면 한 칸)\n웅크리고 우클릭: 엔더 상자로 바로 보내기",
      "tooltip.wildplus.void_axe": "나무 통째로 베기 (웅크리면 한 칸)\n체력 30% 이하 적에게 피해 2배",
      "tooltip.wildplus.void_shovel": "5×5 채굴 · 채굴 속도 3배 (웅크리면 한 칸)",
      "tooltip.wildplus.void_hoe": "7×7 수확 · 자동으로 다시 심기 (웅크리면 한 칸)"}
EN = {"block.wildplus.lottery_machine": "Lottery Machine", "block.wildplus.fate_altar": "Altar of Fate"}
ROMAN = {11: "XI", 12: "XII", 13: "XIII", 14: "XIV", 15: "XV"}


def build(lang_ko, lang_en):
    kitchen_models.export("lottery_machine", lottery, 0, 0, 0, 0, 3)
    kitchen_models.export("fate_altar", altar, 0, 0, 0, 0, 2)
    lv = {f"enchantment.level.{k}": v for k, v in ROMAN.items()}
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO, **lv})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN, **lv})
    for n in ("lottery_machine", "fate_altar"):
        write(DATA / f"loot_table/blocks/{n}.json", self_loot(n))
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/pickaxe.json", [i("lottery_machine"), i("fate_altar")])
    write(DATA / "recipe/lottery_machine.json", shaped(["GRG", "GPG", "III"], {"G": "minecraft:gold_ingot", "R": "minecraft:redstone", "P": "minecraft:paper", "I": "minecraft:iron_block"}, i("lottery_machine"), category="misc"))
    write(DATA / "recipe/fate_altar.json", shaped([" B ", "DOD", "OOO"], {"B": "minecraft:book", "D": "minecraft:diamond_block", "O": "minecraft:crying_obsidian"}, i("fate_altar"), category="misc"))


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v38")
