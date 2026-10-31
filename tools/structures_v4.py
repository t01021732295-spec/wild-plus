"""7차 구조물: 상점 마을 (48×48). 가운데 광장(분수·워프 포인트) + 십자 큰길 + 상점 8채.

상점: 잡화점 · 대장간 · 은행 · 증권거래소 · 약국 · 공구점 · 뽑기방 · 시청
모든 자판기는 무한 재고(야생 자판기), 상인 둘이 상주한다.
"""
from nbtlib import Byte, Compound, String

from structures_v3 import FENCE, LANTERN, Template, mob, vending

W = 48
H = 10
NORTH_Z, SOUTH_Z = 11, 28        # 북쪽 건물 z 11~19 (문 남쪽), 남쪽 건물 z 28~36 (문 북쪽)
WEST_X = [1, 11]
EAST_X = [28, 38]

STAIRS = {"half": "bottom", "shape": "straight", "waterlogged": "false"}


def face(facing):
    return {"facing": facing}


def building(t, x0, z0, door, wall, roof, accent):
    """9×9 건물. door = 'south'(문이 z0+8 벽) 또는 'north'(문이 z0 벽). 바닥 y0, 벽 y1~4, 지붕 y5."""
    x1, z1 = x0 + 8, z0 + 8
    t.fill(x0, 0, z0, x1, 0, z1, "minecraft:spruce_planks")
    for y in range(1, 5):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                if corner:
                    t.set(x, y, z, "minecraft:stripped_spruce_log", {"axis": "y"})
                elif edge:
                    t.set(x, y, z, wall)
    # 창문
    for z in (z0 + 2, z0 + 3, z0 + 5, z0 + 6):
        for x in (x0, x1):
            t.set(x, 2, z, "minecraft:glass_pane", {"east": "false", "west": "false", "north": "true", "south": "true",
                                                    "waterlogged": "false"})
    # 문 (2칸 높이 입구 + 양옆 랜턴)
    dz = z1 if door == "south" else z0
    for x in (x0 + 3, x0 + 4, x0 + 5):
        t.set(x, 1, dz, "minecraft:air")
        t.set(x, 2, dz, "minecraft:air")
    t.set(x0 + 3, 3, dz, accent)
    t.set(x0 + 4, 3, dz, accent)
    t.set(x0 + 5, 3, dz, accent)
    # 지붕 (한 칸 튀어나온 처마)
    t.fill(x0 - 1, 5, z0 - 1, x1 + 1, 5, z1 + 1, roof)
    t.fill(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, roof)
    # 천장 조명
    t.set(x0 + 4, 4, z0 + 4, "minecraft:lantern", LANTERN)
    t.set(x0 + 2, 4, z0 + 2, "minecraft:lantern", LANTERN)
    t.set(x0 + 6, 4, z0 + 6, "minecraft:lantern", LANTERN)
    back = z0 + 1 if door == "south" else z1 - 1
    inner_face = "south" if door == "south" else "north"
    return back, inner_face


def place(t, x, z, block, props=None, nbt=None):
    t.set(x, 1, z, block, props, nbt)


def place_vending(t, x, z, products, facing):
    t.set(x, 1, z, *vending(products, facing))
    t.set(x, 2, z, "wildplus:vending_machine", {"facing": facing, "half": "upper"})


FOOD = [("minecraft:bread", 8, 2), ("wildplus:pizza", 4, 4), ("wildplus:honey_toast", 4, 5),
        ("wildplus:tomato_soup", 1, 6), ("wildplus:traveler_ration", 4, 8), ("minecraft:cooked_beef", 8, 4),
        ("wildplus:strawberry_tart", 4, 5), ("minecraft:golden_carrot", 8, 6), ("minecraft:cake", 1, 15)]
SEEDS = [("minecraft:wheat_seeds", 16, 2), ("wildplus:tomato_seeds", 8, 2), ("wildplus:chili_seeds", 8, 2),
         ("wildplus:strawberry", 4, 3), ("minecraft:carrot", 8, 3), ("minecraft:potato", 8, 3),
         ("minecraft:bone_meal", 16, 4), ("wildplus:fertilizer", 8, 3), ("minecraft:oak_sapling", 4, 2)]
TOOLS = [("minecraft:iron_pickaxe", 1, 40), ("minecraft:iron_axe", 1, 40), ("minecraft:iron_shovel", 1, 25),
         ("minecraft:shears", 1, 15), ("minecraft:bucket", 1, 20), ("wildplus:copper_backpack", 1, 60),
         ("wildplus:teleporter", 1, 40), ("wildplus:chunk_loader", 1, 100), ("wildplus:lottery_ticket", 1, 30)]
BUILD = [("minecraft:torch", 32, 1), ("minecraft:oak_log", 16, 4), ("minecraft:stone_bricks", 32, 6),
         ("minecraft:glass", 16, 5), ("minecraft:oak_planks", 32, 3), ("minecraft:lantern", 4, 6),
         ("minecraft:white_bed", 1, 15), ("minecraft:chest", 2, 6), ("minecraft:crafting_table", 1, 3)]
MEDICINE = [("minecraft:golden_apple", 1, 80), ("minecraft:honey_bottle", 2, 15), ("minecraft:milk_bucket", 1, 20),
            ("minecraft:golden_carrot", 4, 20), ("wildplus:recall_scroll", 1, 40), ("minecraft:glistering_melon_slice", 2, 30),
            ("minecraft:ender_pearl", 2, 40), ("minecraft:experience_bottle", 4, 40), ("minecraft:totem_of_undying", 1, 3000)]
LOTTO = [("wildplus:lottery_ticket", 1, 30), ("wildplus:lottery_ticket", 5, 150), ("wildplus:coin_pouch", 1, 15),
         ("minecraft:firework_rocket", 8, 8), ("minecraft:cookie", 8, 2), ("minecraft:cake", 1, 15),
         ("minecraft:name_tag", 1, 40), ("minecraft:jukebox", 1, 30), ("minecraft:music_disc_cat", 1, 80)]
GEAR = [("wildplus:mythril_ingot", 1, 40), ("wildplus:jade", 1, 20), ("wildplus:blood_shard", 2, 30),
        ("minecraft:iron_ingot", 4, 10), ("minecraft:diamond", 1, 100), ("minecraft:lapis_lazuli", 8, 6),
        ("minecraft:anvil", 1, 40), ("minecraft:experience_bottle", 4, 30), ("wildplus:enhance_table", 1, 250)]


def town():
    t = Template(W, H, W)
    # 땅: 잔디 + 가장자리 울타리(입구 4곳)
    t.fill(0, 0, 0, W - 1, 0, W - 1, "minecraft:grass_block", {"snowy": "false"})
    for i in range(W):
        for (x, z) in ((i, 0), (i, W - 1), (0, i), (W - 1, i)):
            if not (21 <= i <= 26):
                t.set(x, 1, z, "minecraft:oak_fence", FENCE)
    # 십자 큰길 (가운데 돌길 + 양쪽 보도)
    for i in range(W):
        for k in range(21, 27):
            mat = "minecraft:stone_bricks" if k in (22, 23, 24, 25) else "minecraft:polished_andesite"
            t.set(i, 0, k, mat)
            t.set(k, 0, i, mat)
    # 광장 + 분수 + 워프 포인트
    t.fill(18, 0, 18, 29, 0, 29, "minecraft:smooth_stone")
    t.fill(21, 0, 21, 26, 0, 26, "minecraft:chiseled_stone_bricks")
    t.fill(22, 1, 22, 25, 1, 25, "minecraft:stone_brick_wall", {"east": "none", "west": "none", "north": "none",
                                                                  "south": "none", "up": "true", "waterlogged": "false"})
    t.fill(23, 1, 23, 24, 1, 24, "minecraft:water", {"level": "0"})
    t.fill(23, 0, 23, 24, 0, 24, "minecraft:stone_bricks")
    t.set(21, 1, 21, "wildplus:warp_point")
    t.set(26, 1, 26, "wildplus:shipping_bin", None, Compound({"id": String("wildplus:shipping_bin"), "public": Byte(1)}))
    # 가로등
    for (x, z) in [(20, 8), (27, 8), (20, 39), (27, 39), (8, 20), (8, 27), (39, 20), (39, 27), (20, 20), (27, 27), (20, 27), (27, 20)]:
        t.fill(x, 1, z, x, 3, z, "minecraft:spruce_fence", FENCE)
        t.set(x, 4, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # 벤치 (광장 둘레)
    for (x, z, f) in [(18, 23, "east"), (18, 24, "east"), (29, 23, "west"), (29, 24, "west"),
                      (23, 18, "south"), (24, 18, "south"), (23, 29, "north"), (24, 29, "north")]:
        t.set(x, 1, z, "minecraft:spruce_stairs", {**STAIRS, "facing": f})
    # 정원 나무 (북쪽·남쪽 끝)
    for (x, z) in [(5, 4), (15, 5), (32, 4), (42, 5), (5, 42), (15, 43), (32, 43), (42, 42)]:
        t.fill(x, 1, z, x, 4, z, "minecraft:oak_log", {"axis": "y"})
        t.fill(x - 2, 4, z - 2, x + 2, 5, z + 2, "minecraft:oak_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
        t.fill(x - 1, 6, z - 1, x + 1, 6, z + 1, "minecraft:oak_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
        t.set(x, 4, z, "minecraft:oak_log", {"axis": "y"})
    for (x, z) in [(8, 3), (24, 5), (38, 3), (9, 44), (24, 42), (38, 44)]:
        t.set(x, 1, z, "minecraft:poppy" if (x + z) % 2 else "minecraft:dandelion")

    shops = [
        # (x0, z0, 문 방향, 벽, 지붕, 장식, 내부를 채우는 함수)
        (WEST_X[0], NORTH_Z, "south", "minecraft:oak_planks", "minecraft:red_terracotta", "minecraft:red_wool", "grocery"),
        (WEST_X[1], NORTH_Z, "south", "minecraft:stone_bricks", "minecraft:gray_terracotta", "minecraft:black_wool", "smithy"),
        (EAST_X[0], NORTH_Z, "south", "minecraft:smooth_quartz", "minecraft:green_terracotta", "minecraft:gold_block", "bank"),
        (EAST_X[1], NORTH_Z, "south", "minecraft:smooth_quartz", "minecraft:blue_terracotta", "minecraft:light_blue_wool", "stocks"),
        (WEST_X[0], SOUTH_Z, "north", "minecraft:white_concrete", "minecraft:lime_terracotta", "minecraft:lime_wool", "pharmacy"),
        (WEST_X[1], SOUTH_Z, "north", "minecraft:spruce_planks", "minecraft:brown_terracotta", "minecraft:orange_wool", "tools"),
        (EAST_X[0], SOUTH_Z, "north", "minecraft:pink_concrete", "minecraft:magenta_terracotta", "minecraft:yellow_wool", "arcade"),
        (EAST_X[1], SOUTH_Z, "north", "minecraft:bricks", "minecraft:cyan_terracotta", "minecraft:white_wool", "hall"),
    ]
    for (x0, z0, door, wall, roof, accent, kind) in shops:
        back, f = building(t, x0, z0, door, wall, roof, accent)
        mid = z0 + 4
        if kind == "grocery":
            place_vending(t, x0 + 2, back, FOOD, f)
            place_vending(t, x0 + 6, back, SEEDS, f)
            t.set(x0 + 4, 1, back, "minecraft:barrel", {"facing": "up", "open": "false"})
            t.entity(x0 + 4.5, 1.0, mid + 0.5, mob("wildplus:merchant", 0.0 if door == "south" else 180.0))
        elif kind == "smithy":
            place(t, x0 + 2, back, "minecraft:anvil", {"facing": "east"})
            place(t, x0 + 4, back, "wildplus:enhance_table", face(f))
            place(t, x0 + 6, back, "minecraft:blast_furnace", {"facing": f, "lit": "false"})
            place(t, x0 + 1, mid, "minecraft:smithing_table")
            place_vending(t, x0 + 7, mid, GEAR, "west")
            t.entity(x0 + 4.5, 1.0, mid + 0.5, mob("wildplus:blacksmith_merchant", 0.0 if door == "south" else 180.0))
        elif kind == "bank":
            place(t, x0 + 2, back, "wildplus:atm", face(f))
            place(t, x0 + 4, back, "wildplus:exchange", face(f))
            place(t, x0 + 6, back, "wildplus:atm", face(f))
            t.fill(x0 + 2, 1, mid, x0 + 6, 1, mid, "minecraft:polished_andesite_slab", {"type": "top", "waterlogged": "false"})
        elif kind == "stocks":
            place(t, x0 + 2, back, "wildplus:stock_terminal", face(f))
            place(t, x0 + 4, back, "wildplus:stock_terminal", face(f))
            place(t, x0 + 6, back, "wildplus:stock_terminal", face(f))
            place(t, x0 + 4, mid, "wildplus:bounty_board", face(f))
        elif kind == "pharmacy":
            place(t, x0 + 3, back, "wildplus:pharmacy", face(f))
            place(t, x0 + 5, back, "wildplus:pharmacy", face(f))
            place_vending(t, x0 + 7, back, MEDICINE, f)
            place(t, x0 + 1, back, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})
        elif kind == "tools":
            place_vending(t, x0 + 2, back, TOOLS, f)
            place_vending(t, x0 + 6, back, BUILD, f)
            place(t, x0 + 4, back, "minecraft:crafting_table")
        elif kind == "arcade":
            for x in (x0 + 2, x0 + 4, x0 + 6):
                place(t, x, back, "wildplus:gacha_machine", face(f))
            place_vending(t, x0 + 1, mid, LOTTO, "east")
            place(t, x0 + 4, mid + (1 if door == "north" else -1), "wildplus:casino_table", face(f))
            place(t, x0 + 6, mid + (1 if door == "north" else -1), "wildplus:casino_table", face(f))
        elif kind == "hall":
            place(t, x0 + 2, back, "wildplus:weather_station", face(f))
            place(t, x0 + 4, back, "wildplus:bounty_board", face(f))
            place(t, x0 + 6, back, "wildplus:warp_point")
    return t.save("town")


if __name__ == "__main__":
    print(town())
