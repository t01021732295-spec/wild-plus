"""구조물 템플릿(.nbt) 생성기: 교역소, 도적 야영지.

26.3 구조물 NBT 형식: palette 항목은 {id, properties}, DataVersion 5023.
템플릿 부피 전체를 공기로 채운 뒤 블록을 놓는다 (나무·풀이 구조물을 뚫지 않도록).
"""
from pathlib import Path

import nbtlib
from nbtlib import Byte, Compound, Double, Float, Int, List, String

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/wildplus/structure"
DATA_VERSION = 5023


class Template:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []
        for x in range(sx):
            for y in range(sy):
                for z in range(sz):
                    self.blocks[(x, y, z)] = ("minecraft:air", None, None)

    def set(self, x, y, z, block, props=None, nbt=None):
        self.blocks[(x, y, z)] = (block, props, nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, block, props=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, block, props)

    def entity(self, x, y, z, nbt):
        self.entities.append((x, y, z, nbt))

    def save(self, name):
        palette, index = [], {}
        blocks = []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items()):
            key = (block, tuple(sorted((props or {}).items())))
            if key not in index:
                entry = Compound({"id": String(block)})
                if props:
                    entry["properties"] = Compound({k: String(v) for k, v in props.items()})
                index[key] = len(palette)
                palette.append(entry)
            b = Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])})
            if nbt is not None:
                b["nbt"] = nbt
            blocks.append(b)
        ents = []
        for (x, y, z, nbt) in self.entities:
            ents.append(Compound({
                "pos": List[Double]([Double(x), Double(y), Double(z)]),
                "blockPos": List[Int]([Int(int(x)), Int(int(y)), Int(int(z))]),
                "nbt": nbt,
            }))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(v) for v in self.size]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](ents),
        })
        OUT.mkdir(parents=True, exist_ok=True)
        nbtlib.File(root, gzipped=True).save(OUT / f"{name}.nbt")
        return len(blocks), len(ents)


def mob(entity_id, yaw=0.0, extra=None):
    c = Compound({
        "id": String(entity_id),
        "PersistenceRequired": Byte(1),
        "Rotation": List[Float]([Float(yaw), Float(0.0)]),
    })
    if extra:
        c.update(extra)
    return c


def chest(loot, facing="north"):
    return ("minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
            Compound({"id": String("minecraft:chest"), "LootTable": String(loot)}))


def vending(products, facing):
    """구조물 자판기: 정해진 상품·가격, 무한 재고. products = [(아이템, 진열 개수, 가격), ...] 최대 9개"""
    nbt = Compound({"id": String("wildplus:vending_machine"), "infinite": Byte(1)})
    prices = [10] * 9
    for idx, (item, count, price) in enumerate(products[:9]):
        nbt[f"item{idx}"] = Compound({"id": String(item), "count": Int(count)})
        prices[idx] = price
    nbt["prices"] = nbtlib.IntArray([Int(p) for p in prices])
    return ("wildplus:vending_machine", {"facing": facing, "half": "lower"}, nbt)


FENCE = {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"}
LANTERN = {"hanging": "true", "waterlogged": "false"}
LOG_X = {"axis": "x"}
LOG_Z = {"axis": "z"}


def trading_post():
    """교역소: 줄무늬 천막 가판대 + 상인 둘 + 무한 재고 자판기 셋"""
    t = Template(13, 7, 11)
    t.fill(0, 0, 0, 12, 0, 10, "minecraft:grass_block", {"snowy": "false"})
    t.fill(1, 0, 1, 11, 0, 9, "minecraft:spruce_planks")
    t.fill(0, 0, 4, 0, 0, 6, "minecraft:dirt_path")
    # 기둥
    for (x, z) in [(1, 2), (11, 2), (1, 9), (11, 9)]:
        t.fill(x, 1, z, x, 3, z, "minecraft:spruce_fence", FENCE)
    # 줄무늬 천막 (x 방향 줄무늬)
    for x in range(0, 13):
        wool = "minecraft:red_wool" if x % 2 == 0 else "minecraft:white_wool"
        t.fill(x, 4, 2, x, 4, 9, wool)
        t.set(x, 5, 5, wool)
        t.set(x, 5, 6, wool)
    # 매달린 등불
    for (x, z) in [(3, 4), (9, 4), (6, 7)]:
        t.set(x, 3, z, "minecraft:lantern", LANTERN)
    # 계산대 (z=3): 통 + 반 블록
    for x in range(2, 11):
        if x in (2, 6, 10):
            t.set(x, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
        else:
            t.set(x, 1, 3, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    t.set(4, 2, 3, "minecraft:potted_poppy")
    t.set(8, 2, 3, "minecraft:potted_dandelion")
    # 뒤쪽 물품
    t.set(2, 1, 8, "minecraft:hay_block", {"axis": "y"})
    t.set(3, 1, 8, "minecraft:composter", {"level": "0"})
    t.set(9, 1, 8, "minecraft:anvil", {"facing": "east"})
    t.set(10, 1, 8, "minecraft:smithing_table")
    t.set(6, 1, 8, *chest("wildplus:chests/trading_post", "north"))
    # 자판기 둘 + 뽑기 기계 (오른쪽 바깥, 동쪽을 바라봄)
    food = [("minecraft:bread", 8, 2), ("wildplus:pizza", 4, 4), ("wildplus:honey_toast", 4, 5),
            ("wildplus:tomato_soup", 1, 6), ("wildplus:traveler_ration", 4, 8), ("minecraft:cooked_beef", 8, 4),
            ("wildplus:strawberry_tart", 4, 5), ("minecraft:golden_carrot", 8, 6), ("minecraft:cake", 1, 15)]
    goods = [("minecraft:torch", 32, 1), ("wildplus:fertilizer", 8, 3), ("wildplus:tomato_seeds", 8, 2),
             ("minecraft:oak_sapling", 4, 2), ("minecraft:arrow", 16, 2), ("wildplus:recall_scroll", 2, 40),
             ("minecraft:shears", 1, 15), ("minecraft:lead", 2, 10), ("wildplus:coin_pouch", 1, 15)]
    # 자판기는 실물 크기(2칸 높이): 위 칸도 같이 놓는다
    t.set(12, 1, 4, *vending(food, "east"))
    t.set(12, 2, 4, "wildplus:vending_machine", {"facing": "east", "half": "upper"})
    t.set(12, 1, 5, "wildplus:gacha_machine", {"facing": "east"})
    t.set(12, 1, 6, *vending(goods, "east"))
    t.set(12, 2, 6, "wildplus:vending_machine", {"facing": "east", "half": "upper"})
    # 상인
    t.entity(4.5, 1.0, 5.5, mob("wildplus:merchant", 180.0))
    t.entity(8.5, 1.0, 5.5, mob("wildplus:blacksmith_merchant", 180.0))
    return t.save("trading_post")


def tent(t, x, z, wool):
    """3x3 A자 천막 (앞이 열려 있음, z 방향으로 길게)"""
    for dz in range(3):
        t.set(x, 1, z + dz, wool)
        t.set(x + 2, 1, z + dz, wool)
        t.set(x, 2, z + dz, wool)
        t.set(x + 2, 2, z + dz, wool)
        t.set(x + 1, 3, z + dz, wool)
    t.set(x + 1, 1, z + 2, wool)
    t.set(x + 1, 2, z + 2, wool)


def bandit_camp():
    """도적 야영지: 모닥불, 천막 셋, 약탈품 상자, 도적 셋"""
    t = Template(15, 6, 15)
    for x in range(15):
        for z in range(15):
            block = "minecraft:coarse_dirt" if (x * 7 + z * 3) % 5 == 0 else "minecraft:grass_block"
            t.set(x, 0, z, block, {"snowy": "false"} if block == "minecraft:grass_block" else None)
    t.fill(5, 0, 5, 9, 0, 9, "minecraft:coarse_dirt")
    t.set(7, 1, 7, "minecraft:campfire", {"lit": "true", "signal_fire": "false", "facing": "north", "waterlogged": "false"})
    # 통나무 의자
    t.fill(5, 1, 6, 5, 1, 8, "minecraft:stripped_spruce_log", LOG_Z)
    t.fill(9, 1, 6, 9, 1, 8, "minecraft:stripped_spruce_log", LOG_Z)
    t.fill(6, 1, 10, 8, 1, 10, "minecraft:stripped_spruce_log", LOG_X)
    # 천막
    tent(t, 1, 1, "minecraft:black_wool")
    tent(t, 11, 1, "minecraft:gray_wool")
    tent(t, 1, 11, "minecraft:brown_wool")
    t.set(2, 1, 2, *chest("wildplus:chests/bandit_camp", "south"))
    t.set(12, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    # 울타리 등불, 과녁, 짐더미
    t.fill(13, 1, 12, 13, 2, 12, "minecraft:spruce_fence", FENCE)
    t.set(13, 3, 12, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    t.set(12, 1, 13, "minecraft:target", {"power": "0"})
    t.set(11, 1, 13, "minecraft:hay_block", {"axis": "y"})
    t.set(4, 1, 7, "minecraft:barrel", {"facing": "up", "open": "false"})
    # 도적
    t.entity(6.5, 1.0, 4.5, mob("wildplus:bandit", 0.0))
    t.entity(10.5, 1.0, 8.5, mob("wildplus:bandit", 90.0))
    t.entity(4.5, 1.0, 10.5, mob("wildplus:bandit", 200.0))
    return t.save("bandit_camp")


def build():
    a = trading_post()
    b = bandit_camp()
    return a, b


if __name__ == "__main__":
    print(build())
