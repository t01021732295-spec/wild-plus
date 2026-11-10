"""4.8 데이터 (data_v41): 거래소 모델 · 제작법 · 언어 (미니맵 키 · 설정)."""
import json

import kitchen_models
from cosmetic_design import Box, solid as S
from data_v2 import ASSETS, DATA, i, merge_tag, self_loot, shaped, write

WD=(126,84,50);WD2=(92,60,34);GD=(232,186,70);RD=(196,52,52);WH=(238,232,220);DK=(30,30,40);GN=(70,170,90);NV=(36,46,84)


def stall():
    def scr(f,u,v,w,h):
        if f!='front':return DK
        if v<2 or v>h-3 or u<1 or u>w-2:return (20,24,36)
        if v%4==1: return GD if u<w//3 else (GN if u>w*2//3 else WH)
        return (24,40,70)
    def board(f,u,v,w,h):
        if f!='front':return WD2
        if u in(0,w-1) or v in(0,h-1):return GD
        cx=(u-1)//5; cy=(v-1)//5
        if (u-1)%5==0 or (v-1)%5==0: return WD2
        return [(220,90,90),(90,170,230),(240,200,80),(120,200,120),(200,120,220)][(cx*3+cy)%5] if (u+v)%3 else WH
    b=[Box(-24,0,-8,48,4,16,S(WD2)),Box(-24,4,-8,48,14,16,lambda f,u,v,w,h:WD if (u//4)%2 else WD2),Box(-25,18,-9,50,2,18,S(GD))]
    b+=[Box(-22,20,-6,4,30,4,S(WD2)),Box(18,20,-6,4,30,4,S(WD2)),Box(-20,22,-6,40,28,1.5,board)]
    b+=[Box(-26,50,-11,52,3,22,lambda f,u,v,w,h:RD if (u//4)%2 else WH),Box(-24,53,-9,48,3,18,lambda f,u,v,w,h:RD if (u//4)%2 else WH),Box(-20,56,-6,40,3,12,lambda f,u,v,w,h:RD if (u//4)%2 else WH)]
    for i in range(7):b.append(Box(-24+i*8,46,10,6,4,1,S(RD if i%2 else WH)))
    b+=[Box(-9,58,-1,18,7,2,S(NV)),Box(-8,59,1,16,5,.5,lambda f,u,v,w,h:GD if (u+v)%5==0 or v==2 else NV)]
    b+=[Box(10,18,-4,9,12,7,S(DK)),Box(10.5,20,3,8,9,.5,scr),Box(12,18,4,5,1,1,S(GD))]
    for x,c in((-19,(220,90,90)),(-14,(240,200,80)),(-9,(90,170,230)),(-4,(120,200,120))):b+=[Box(x,18,-2,4,4,4,S(c)),Box(x+.5,22,-1.5,3,1,3,S(WH))]
    return b


KO = {"block.wildplus.market_stall": "거래소", "tooltip.wildplus.market_stall": "플레이어끼리 물건을 사고파는 곳\n대금은 통장으로 · 수수료 5%\n일괄 출하도 여기서",
      "key.wildplus.map": "큰 지도 열기", "key.wildplus.map_in": "미니맵 확대", "key.wildplus.map_out": "미니맵 축소", "key.category.wildplus.main": "야생 플러스",
      "wildplus.configuration.minimap": "미니맵 표시"}
EN = {"block.wildplus.market_stall": "Market Stall", "key.wildplus.map": "Open Map", "key.wildplus.map_in": "Minimap Zoom In", "key.wildplus.map_out": "Minimap Zoom Out", "key.category.wildplus.main": "Wild Plus",
      "wildplus.configuration.minimap": "Show minimap"}


def build(lang_ko, lang_en):
    kitchen_models.export("market_stall", stall, -1, 1, 0, 0, 4)
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    write(DATA / "loot_table/blocks/market_stall.json", self_loot("market_stall"))
    merge_tag(DATA.parent / "minecraft/tags/block/mineable/axe.json", [i("market_stall")])
    write(DATA / "recipe/market_stall.json", shaped(["WWW", "PCP", "PGP"], {"W": "#minecraft:wool", "P": "#minecraft:planks", "C": "minecraft:chest", "G": "minecraft:gold_block"}, i("market_stall"), category="misc"))


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v41")
