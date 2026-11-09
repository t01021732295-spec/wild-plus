"""4.7 데이터 (data_v40): 화산 거신 전리품 · 몹 이름 · 탑 전용 몹 드롭. 모델은 gen_models_v47.py."""
import json

from PIL import Image

from data_v2 import ASSETS, DATA, i, write
from data_v18 import simple

KO = {"entity.wildplus.volcano_boss": "화산 거신", "entity.wildplus.storm_king": "폭풍의 기사왕", "entity.wildplus.cloud_sentinel": "구름 파수병", "entity.wildplus.thunder_hawk": "천둥매", "entity.wildplus.cloud_priest": "구름 사제",
      "item.wildplus.volcano_heart": "거신의 녹은 심장", "tooltip.wildplus.volcano_heart": "화산 거신 레이드 전리품"}
EN = {"entity.wildplus.volcano_boss": "Volcano Colossus", "entity.wildplus.storm_king": "Storm King", "entity.wildplus.cloud_sentinel": "Cloud Sentinel", "entity.wildplus.thunder_hawk": "Thunder Hawk", "entity.wildplus.cloud_priest": "Cloud Priest", "item.wildplus.volcano_heart": "Molten Heart"}


def build(lang_ko, lang_en):
    write(ASSETS / "lang/ko_kr.json", {**lang_ko, **KO})
    write(ASSETS / "lang/en_us.json", {**lang_en, **EN})
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 8) ** 2) ** .5
            if d < 6.5:
                im.putpixel((x, y), (255, 245, 190, 255) if d < 2 else (255, 150, 40, 255) if d < 4 else (120, 30, 10, 255))
    im.save(ASSETS / "textures/item/volcano_heart.png")
    simple("volcano_heart")
    for e, it in (("cloud_sentinel", "minecraft:iron_ingot"), ("thunder_hawk", "minecraft:feather"), ("cloud_priest", "minecraft:lapis_lazuli")):
        write(DATA / f"loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": it}]}], "random_sequence": i(f"entities/{e}")})


if __name__ == "__main__":
    build(json.loads((ASSETS / "lang/ko_kr.json").read_text()), json.loads((ASSETS / "lang/en_us.json").read_text()))
    print("ok v40")
