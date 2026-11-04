"""3.3 집 꾸미기 도안: 가구 (3D 상자). 좌표: x 오른쪽, y 위, z 앞(+z 정면), 16 = 1칸, 바닥 y=0."""
from cosmetic_design import Box, mix, shade
from kitchen_design import C, DARKWOOD, FIRE, bricks, glow, marble, metal, planks, stone
import anim_fx as AF


LIT = True  # 켜짐/꺼짐 모델 (data 생성 때 바꿔 가며 내보냄)
TV_CH = 0  # TV 채널 (0 자연 · 1 뉴스 · 2 주식 · 3 스포츠 · 4 만화)
TV_CHANNELS = [AF.tv_screen, AF.tv_news, AF.tv_stocks, AF.tv_sports, AF.tv_cartoon]
WHITE = (245, 245, 242)
GOLD = (232, 190, 70)


def fabric(c, seam=None):
    seam = seam or shade(c, 0.82)

    def f(face, u, v, W, H):
        if (u + v * 3) % 7 == 0:
            return shade(c, 0.94)
        if face == "top" and (u == 0 or v == 0 or u == W - 1 or v == H - 1):
            return seam
        return c
    return f


def velvet(c):
    def f(face, u, v, W, H):
        return mix(shade(c, 1.08), shade(c, 0.85), v / max(1, H - 1)) if face != "top" else shade(c, 1.1 if (u + v) % 4 else 1.0)
    return f


def books(face, u, v, W, H):
    if face != "front":
        return (120, 80, 45)
    cols = [(180, 50, 50), (60, 90, 160), (70, 140, 80), (220, 180, 70), (130, 70, 140), (230, 230, 220), (200, 110, 50), (50, 60, 70)]
    c = cols[((u // 2) * 5 + (u // 7)) % len(cols)]
    if v == 0 and u % 4 == 1:
        return None if False else shade(c, 0.6)
    if u % 2 == 1 and (u // 2) % 3 == 0:
        return shade(c, 0.7)
    return c


def lamp(on, off=(200, 190, 170)):
    """켜지면 빛나는 갓 · 불꽃, 꺼지면 무광"""
    return glow(*on) if LIT else (lambda f, u, v, W, H: off if (u + v) % 3 else shade(off, 0.9))


def glass(c=(170, 215, 235)):
    def f(face, u, v, W, H):
        return shade(c, 1.12) if (u - v) % 9 in (0, 1) else c
    return f


# ═══════════ 거실 ═══════════
SOFA_COLORS = {"teal": ("청록", (60, 130, 140)), "red": ("빨강", (170, 45, 50)), "gray": ("회색", (120, 124, 130)),
               "cream": ("크림", (225, 210, 180)), "navy": ("남색", (45, 60, 110)), "pink": ("분홍", (225, 140, 165)),
               "green": ("초록", (80, 130, 70)), "brown": ("갈색", (130, 85, 55))}
SOFA_COLOR = "teal"


def sofa():
    """3인 소파 (3칸): 앉을 수 있음. 벨벳 쿠션 3개 · 등 쿠션 · 팔걸이 · 장식 쿠션 2개."""
    teal = SOFA_COLORS[SOFA_COLOR][1]
    out = []
    for x in (-23, 21):
        for z in (-6, 5):
            out.append(Box(x, 0, z, 2, 3, 2, planks(DARKWOOD)))
    out += [Box(-24, 3, -7, 48, 5, 14, velvet(shade(teal, 0.8)))]
    for k in range(3):
        out += [Box(-19 + k * 13, 8, -4, 12.6, 3.5, 11, velvet(teal))]
        out += [Box(-19 + k * 13, 8, -7, 12.6, 13, 4, velvet(shade(teal, 0.92)))]
    for x in (-24, 20):
        out += [Box(x, 3, -7, 4, 13, 14, velvet(shade(teal, 0.85))), Box(x - 0.5, 15, -7, 5, 2, 14, velvet(teal))]
    out += [Box(-17, 11, -3.5, 7, 7, 2.5, fabric((240, 200, 90)), rot=("z", 0.3, (-13, 11, -2))),
            Box(10, 11, -3.5, 7, 7, 2.5, fabric((230, 120, 100)), rot=("z", -0.3, (13, 11, -2)))]
    return out


def armchair():
    """안락의자 (1칸): 앉을 수 있음. 통통한 가죽 + 단추 + 둥근 팔걸이."""
    leather = (140, 70, 40)

    def tufted(face, u, v, W, H):
        if face == "front" and u % 3 == 1 and v % 3 == 1:
            return shade(leather, 0.65)
        return mix(shade(leather, 1.1), shade(leather, 0.8), v / max(1, H - 1))
    out = [Box(-6.5, 0, -5.5, 1.5, 3, 1.5, C((40, 30, 20))), Box(5, 0, -5.5, 1.5, 3, 1.5, C((40, 30, 20))),
           Box(-6.5, 0, 4.5, 1.5, 3, 1.5, C((40, 30, 20))), Box(5, 0, 4.5, 1.5, 3, 1.5, C((40, 30, 20)))]
    out += [Box(-7, 3, -7, 14, 5, 14, tufted), Box(-5, 8, -4, 10, 3, 10, tufted), Box(-6, 8, -7, 12, 15, 4, tufted)]
    out += [Box(-8, 3, -6, 3, 10, 13, tufted), Box(5, 3, -6, 3, 10, 13, tufted),
            Box(-8.5, 12, -6, 4, 2.5, 13, tufted), Box(4.5, 12, -6, 4, 2.5, 13, tufted)]
    out += [Box(-6.5, 22, -7, 13, 2.5, 4, tufted)]
    return out


def coffee_table():
    """커피 테이블 (1칸): 낮은 원목 + 유리 상판 · 찻잔 · 잡지."""
    out = []
    for x in (-6.5, 5):
        for z in (-6.5, 5):
            out.append(Box(x, 0, z, 1.5, 6, 1.5, planks(DARKWOOD)))
    out += [Box(-7, 3, -7, 14, 1, 14, planks((150, 100, 60))), Box(-7.5, 6, -7.5, 15, 1.2, 15, planks(DARKWOOD)),
            Box(-6.5, 7.2, -6.5, 13, 0.4, 13, glass())]
    out += [Box(-5, 7.6, -1, 6, 0.5, 8, C((220, 80, 70))), Box(-4.5, 8.1, -0.5, 5, 0.2, 7, C((250, 245, 230)))]
    out += [Box(2, 7.6, -4, 4, 0.4, 4, C(WHITE)), Box(2.8, 8, -3.2, 2.4, 2, 2.4, C(WHITE)), Box(3.2, 9.8, -2.8, 1.6, 0.3, 1.6, C((120, 70, 40))),
            Box(5.2, 8.6, -2.6, 0.8, 1, 0.6, C(WHITE)), Box(2.5, 10.2, -2.5, 3, 7, 0.01, AF.steam())]
    out += [Box(-6, 3.6, -5, 4, 1.5, 3, C((90, 140, 200))), Box(-1, 3.6, -5, 3, 2, 3, C((210, 180, 80)))]
    return out


def fireplace():
    """벽난로 (2칸 · 높이 2): 돌 + 대리석 선반 · 장작불 (빛) · 촛대 · 작은 시계 · 굴뚝."""
    st = stone((160, 155, 148))
    out = [Box(-16, 0, -8, 32, 2, 12, st), Box(-15, 2, -6, 30, 18, 10, bricks((175, 165, 150), (150, 140, 128), (110, 104, 98)))]
    out += [Box(-8, 2, 3.9, 16, 11, 0.3, C((40, 30, 26))), Box(-9, 13, 3.8, 18, 2, 1, st)]
    out += [Box(-6, 2.5, 4.2, 12, 2, 1.2, planks((110, 70, 40))), Box(-5, 4.5, 4.3, 10, 1.6, 1, planks((130, 85, 50)))]
    if LIT:
        out += [Box(-6, 5.5, 4.4, 12, 7, 0.4, AF.flames())]
    else:
        out += [Box(-5, 6, 4.4, 10, 0.8, 0.4, C((70, 66, 64)))]
    out += [Box(-17, 20, -7, 34, 2, 12, marble)]
    candle = AF.flicker((255, 210, 90), n=12) if LIT else C((240, 235, 220))
    out += [Box(-12, 22, -2, 2, 5, 2, C(GOLD)), Box(-11.5, 27, -1.5, 1, 1.5, 1, candle),
            Box(10, 22, -2, 2, 5, 2, C(GOLD)), Box(10.5, 27, -1.5, 1, 1.5, 1, candle)]
    out += [Box(-3, 22, -3, 6, 7, 3, planks(DARKWOOD)), Box(-2, 24, 0.1, 4, 4, 0.3, C((245, 240, 225))),
            Box(-0.2, 25.5, 0.4, 0.4, 2, 0.2, C((30, 30, 30)))]
    out += [Box(-8, 22, -7, 16, 10, 6, bricks((150, 140, 128), (130, 122, 112), (100, 96, 90)))]
    return out


def tv_stand():
    """TV 장식장 (2칸): 낮은 장 + 큰 TV (화면 빛) · 스피커 · 화분."""
    out = [Box(-15, 0, -6, 30, 8, 12, planks((230, 225, 215), (200, 195, 185)))]
    for x in (-14, 0.5):
        out += [Box(x, 1, 5.9, 13.5, 6, 0.4, planks((200, 170, 130))), Box(x + 6, 3.5, 6.3, 2, 1, 0.6, C((60, 60, 64)))]

    def screen(face, u, v, W, H):
        if face != "front" or not LIT:
            return (30, 30, 34) if (u - v) % 9 else (55, 55, 62)
        if u in (0, W - 1) or v in (0, H - 1):
            return (20, 20, 24)
        sky = mix((90, 170, 240), (180, 220, 255), v / H)
        if v > H * 0.65:
            return (80, 170, 90) if (u + v) % 5 else (60, 140, 70)
        if (u - W * 0.7) ** 2 + (v - H * 0.3) ** 2 < 6:
            return (255, 230, 120)
        return sky
    out += [Box(-2, 8, -2, 4, 2, 3, C((30, 30, 34))), Box(-0.8, 10, -1, 1.6, 2, 1, C((30, 30, 34))),
            Box(-13, 12, -1.5, 26, 15, 1.5, TV_CHANNELS[TV_CH]() if LIT else screen)]
    out += [Box(-15, 8, -3, 3, 8, 3, C((40, 40, 44))), Box(-14.2, 13, 0.1, 1.4, 1.4, 0.3, C((90, 90, 96))),
            Box(12, 8, -3, 3, 8, 3, C((40, 40, 44))), Box(12.8, 13, 0.1, 1.4, 1.4, 0.3, C((90, 90, 96)))]
    return out


def floor_lamp():
    """스탠드 조명 (1칸 · 빛): 대리석 받침 · 금 기둥 · 천 갓."""
    out = [Box(-4, 0, -4, 8, 1.5, 8, marble), Box(-0.6, 1.5, -0.6, 1.2, 20, 1.2, metal(GOLD))]
    for (y, w) in [(20, 9), (22, 8), (24, 7), (26, 6)]:
        out.append(Box(-w / 2, y, -w / 2, w, 2, w, lamp(((255, 240, 200), (240, 200, 140)), (225, 215, 195))))
    out += [Box(-1, 19.5, -1, 2, 1, 2, lamp(((255, 255, 220), (255, 220, 120)), (200, 200, 190)))]
    return out


def monstera():
    """큰 화분 (1칸): 테라코타 화분 + 몬스테라 잎."""
    pot = bricks((200, 110, 70), (185, 100, 62), (160, 85, 55), 16, 3)
    out = [Box(-5, 0, -5, 10, 9, 10, pot), Box(-5.5, 8, -5.5, 11, 1.5, 11, C((180, 95, 60))), Box(-4.5, 9, -4.5, 9, 0.5, 9, C((80, 55, 35)))]
    leaf = lambda f, u, v, W, H: (60, 150, 70) if (u * 3 + v) % 5 else (40, 110, 50)
    for (ang, axis, x, z, h) in [(0.5, "z", -2, 0, 14), (-0.5, "z", 2, 0, 15), (0.5, "x", 0, -2, 13), (-0.45, "x", 0, 2, 16), (0.0, "z", 0, 0, 18)]:
        out.append(Box(x - 0.4, 9, z - 0.4, 0.8, h - 4, 0.8, C((70, 130, 60)), rot=(axis, ang, (x, 9, z))))
        out.append(Box(x - 4, 9 + h - 5, z - 0.3, 8, 6, 0.6, leaf, rot=(axis, ang, (x, 9, z))))
    return out


def rug():
    """러그 (3×2칸 · 바닥): 메달리온 무늬 페르시아 카펫."""
    def pat(face, u, v, W, H):
        if face != "top":
            return (120, 30, 40)
        if u < 2 or v < 2 or u >= W - 2 or v >= H - 2:
            return (230, 200, 120) if (u + v) % 2 else (200, 160, 80)
        if u < 4 or v < 4 or u >= W - 4 or v >= H - 4:
            return (50, 60, 110)
        cx, cy = W / 2, H / 2
        d = abs(u - cx) / (W / 2) + abs(v - cy) / (H / 2)
        if d < 0.35:
            return (230, 200, 120) if d < 0.18 else (50, 60, 110)
        if d < 0.45:
            return (240, 230, 210)
        return (150, 35, 45) if (u + v) % 6 else (170, 50, 55)
    return [Box(-24, 0, -8, 48, 0.6, 32, pat)]


# ═══════════ 침실 ═══════════
def canopy_bed():
    """캐노피 침대 (1×2칸 · 진짜 침대): 4 기둥 + 지붕 천 + 커튼 · 체크 이불 · 베개 (+z = 머리 쪽)."""
    wood = planks((120, 70, 40))
    out = [Box(-8, 0, -16, 16, 5, 32, wood), Box(-7.5, 5, -15.5, 15, 4, 31, C((250, 248, 240)))]

    def quilt(face, u, v, W, H):
        if face == "top" and ((u // 3) + (v // 3)) % 2:
            return (110, 140, 200)
        return (90, 120, 180)
    out += [Box(-8, 8, -16, 16, 2.5, 22, quilt), Box(-8, 10.4, 2, 16, 0.5, 4, C((250, 250, 245)))]
    out += [Box(-6.5, 9, 8, 13, 3.5, 6, fabric(WHITE))]
    out += [Box(-8, 5, 14, 16, 16, 2, wood), Box(-6, 18, 14.5, 12, 4, 2, C(GOLD))]  # 머리판
    for x in (-8, 6):
        for z in (-16, 14):
            out.append(Box(x, 0, z, 2, 29, 2, wood))
            out.append(Box(x - 0.5, 29, z - 0.5, 3, 1.5, 3, C(GOLD)))
    curtain = lambda f, u, v, W, H: (180, 40, 60) if u % 3 else (150, 30, 50)
    out += [Box(-8, 27, -16, 16, 2, 32, velvet((170, 35, 55)))]
    for x in (-8.5, 7.5):
        out += [Box(x, 12, -15, 1, 15, 5, curtain), Box(x, 12, 9, 1, 15, 5, curtain)]
    out += [Box(-7, 24, -16.5, 14, 3, 1, curtain)]
    return out


def bunk_bed():
    """이층 침대 (1×2칸 · 높이 2 · 진짜 침대): 아래 파랑 · 위 빨강 이불 · 사다리 · 위층 난간."""
    wood = planks((170, 120, 70))
    out = []
    for x in (-8, 6):
        for z in (-16, 14):
            out.append(Box(x, 0, z, 2, 31, 2, wood))
    for (y, c) in [(3, (90, 120, 180)), (18, (190, 60, 60))]:
        out += [Box(-8, y, -16, 16, 2, 32, wood), Box(-7, y + 2, -15, 14, 2.5, 30, C((250, 248, 240)))]
        quilt = lambda f, u, v, W, H, c=c: shade(c, 1.1) if f == "top" and ((u // 2) + (v // 2)) % 2 else c
        out += [Box(-7.5, y + 4, -15, 15, 1.8, 21, quilt), Box(-6, y + 4, 8, 12, 2.5, 6, fabric(WHITE))]
    for z in (-16, 14):
        out += [Box(-8, 9, z, 16, 2, 2, wood), Box(-8, 25, z, 16, 2, 2, wood)]
    out += [Box(-8, 24, -16, 1.5, 3, 30, wood), Box(6.5, 24, -2, 1.5, 3, 16, wood)]  # 위층 난간
    for y in (6, 11, 16, 21):
        out.append(Box(7.5, y, 4, 1.5, 1, 8, wood))  # 사다리 발판
    out += [Box(7.5, 0, 3, 1.5, 28, 1.2, wood), Box(7.5, 0, 11.8, 1.5, 28, 1.2, wood)]
    out += [Box(-5, 22.5, -12, 4, 4, 3, C((210, 160, 100)))]  # 곰 인형
    return out


def wardrobe():
    """옷장 (2칸 · 높이 2 · 보관함 54칸): 양문 + 금 손잡이 · 서랍 2개 · 왕관 장식."""
    w = planks((150, 95, 55), (110, 65, 35))
    out = [Box(-15, 0, -7, 30, 2, 14, planks(DARKWOOD)), Box(-14.5, 2, -6.5, 29, 28, 13, w)]
    for x in (-14, 0.5):
        out += [Box(x, 9, 6.4, 13.5, 20, 0.5, planks((170, 110, 65))), Box(x + 1.5, 11, 6.8, 10.5, 16, 0.3, planks((160, 102, 60))),
                Box(x + (11 if x < 0 else 1.5), 18, 7, 1, 4, 0.8, C(GOLD))]
    for y in (3, 6):
        out += [Box(-14, y, 6.4, 28, 2.6, 0.5, planks((170, 110, 65))), Box(-2, y + 1, 6.9, 4, 0.8, 0.6, C(GOLD))]
    out += [Box(-16, 30, -7.5, 32, 2, 15, planks(DARKWOOD)), Box(-5, 32, -1, 10, 3, 2, C(GOLD)), Box(-1.5, 35, -1, 3, 2, 2, C(GOLD))]
    return out


def vanity():
    """화장대 (2칸): 서랍장 + 둥근 거울 (금테) · 향수병 · 보석함 · 의자."""
    w = planks((240, 235, 228), (210, 200, 190))
    out = [Box(-14, 0, -6, 28, 13, 10, w)]
    for x in (-13, 4):
        for y in (1.5, 7):
            out += [Box(x, y, 3.9, 9, 4.5, 0.4, w), Box(x + 3.5, y + 2, 4.3, 2, 0.8, 0.5, C(GOLD))]
    out += [Box(-15, 13, -7, 30, 1.2, 12, marble)]
    for (y, w2) in [(15, 10), (17, 14), (20, 16), (24, 16), (28, 14), (31, 10)]:
        out.append(Box(-w2 / 2, y, -6, w2, 3 if y < 31 else 2, 1.2, C(GOLD)))
        out.append(Box(-w2 / 2 + 1, y + 0.2, -4.9, w2 - 2, 2.6 if y < 31 else 1.6, 0.3, AF.mirror_shine(offset=-(y - 15) - (16 - w2) / 2)))
    out += [Box(-12, 14.2, -2, 2, 3, 2, C((230, 120, 170))), Box(-11.5, 17.2, -1.5, 1, 1, 1, C(GOLD)),
            Box(-9, 14.2, -1, 2, 2.5, 2, C((150, 200, 230))), Box(8, 14.2, -3, 5, 3, 4, velvet((150, 40, 80)))]
    out += [Box(-4, 0, 7, 8, 7, 7, velvet((230, 170, 190))), Box(-3.5, 0, 7.5, 1, 6, 1, C(GOLD))]
    return out


def nightstand():
    """협탁 (1칸): 서랍 2개 + 작은 스탠드 + 책."""
    w = planks((150, 95, 55))
    out = [Box(-6, 0, -6, 12, 12, 12, w)]
    for y in (1.5, 6.5):
        out += [Box(-5, y, 5.9, 10, 4, 0.4, planks((170, 110, 65))), Box(-1, y + 1.5, 6.3, 2, 0.8, 0.6, C(GOLD))]
    out += [Box(-3, 12, -3, 4, 1, 4, metal(GOLD)), Box(-1.5, 13, -1.5, 1, 5, 1, metal(GOLD))]
    out += [Box(-4, 18, -4, 6, 4, 6, lamp(((255, 240, 200), (240, 190, 130)), (225, 215, 195)))]
    out += [Box(2, 12, 0, 3.5, 1, 4.5, C((60, 90, 160))), Box(2, 13, 0.3, 3.5, 0.8, 4, C((180, 50, 50)))]
    return out


# ═══════════ 주방 · 서재 ═══════════
def fridge():
    """냉장고 (1칸 · 높이 2 · 보관함 27칸): 위 냉동 · 아래 냉장 문 · 은 손잡이 · 자석 메모."""
    body = metal((235, 238, 240))
    out = [Box(-7.5, 0, -7, 15, 31, 14, body)]
    out += [Box(-7.3, 20, 6.9, 14.6, 10.5, 0.5, body), Box(-7.3, 1, 6.9, 14.6, 18.5, 0.5, body),
            Box(-7.5, 19.5, 7, 15, 0.5, 0.6, C((180, 185, 190)))]
    out += [Box(4.5, 22, 7.4, 1, 6, 1.2, metal((190, 195, 200))), Box(4.5, 10, 7.4, 1, 8, 1.2, metal((190, 195, 200)))]
    for (x, y, c) in [(-5, 15, (230, 70, 70)), (-2, 13, (70, 160, 230)), (-4.5, 11, (250, 210, 60))]:
        out.append(Box(x, y, 7.45, 1.2, 1.2, 0.3, C(c)))
    out += [Box(-3, 7, 7.45, 4, 5, 0.1, C((255, 250, 200)))]
    return out


def sink():
    """싱크대 (1칸): 수납장 + 대리석 상판 + 스테인리스 개수대 + 수도꼭지."""
    w = planks((110, 140, 120), (90, 115, 100))
    out = [Box(-8, 0, -8, 16, 14, 15, w), Box(-7, 1, 6.9, 6.5, 12, 0.4, w), Box(0.5, 1, 6.9, 6.5, 12, 0.4, w),
           Box(-1.5, 9, 7.3, 0.8, 3, 0.5, C(GOLD)), Box(0.8, 9, 7.3, 0.8, 3, 0.5, C(GOLD))]
    out += [Box(-8, 14, -8, 16, 1.5, 16, marble), Box(-5, 14.5, -4, 10, 1.1, 8, metal((185, 190, 196))),
            Box(-4, 14.6, -3, 8, 0.6, 6, AF.ripple((140, 190, 220), n=16, ticks=2))]
    out += [Box(-0.4, 15.2, -3.2, 0.8, 4.8, 0.8, AF.drip())]
    out += [Box(-0.8, 15.5, -6.5, 1.6, 6, 1.6, metal((200, 205, 210))), Box(-0.6, 20, -6.5, 1.2, 1.2, 4, metal((200, 205, 210))),
            Box(-3, 15.5, -6.5, 1.5, 1.5, 1.5, C((200, 60, 60))), Box(1.5, 15.5, -6.5, 1.5, 1.5, 1.5, C((60, 100, 200)))]
    return out


def bookshelf():
    """책장 (2칸 · 높이 2): 4단 알록달록 책 · 위에 화분 · 지구본."""
    w = planks((120, 75, 40))
    out = [Box(-16, 0, -7, 32, 31, 1, w), Box(-16, 0, -7, 2, 31, 12, w), Box(14, 0, -7, 2, 31, 12, w)]
    for y in (0, 7.5, 15, 22.5, 30):
        out.append(Box(-16, y, -7, 32, 1.5, 12, w))
    for k, y in enumerate((1.5, 9, 16.5, 24)):
        n = 26 - k * 3
        out.append(Box(-14, y, -5, n, 5.5 + (k % 2), 9, books))
        out.append(Box(-14 + n, y, -5, 1.5, 4.5, 9, C((200, 60, 60)), rot=("z", -0.4, (-14 + n, y, 0))))
    out += [Box(10, 31.5, -2, 4, 4, 4, C((190, 105, 70))), Box(9, 35, -3, 6, 4, 6, C((80, 160, 80)))]
    out += [Box(-12, 31.5, -2, 4, 1, 4, C(GOLD)), Box(-11, 32.5, -1, 1, 2, 1, C(GOLD)),
            Box(-13, 34, -3, 6, 6, 6, lambda f, u, v, W, H: (70, 150, 210) if (u * 2 + v) % 5 > 1 else (90, 170, 90))]
    return out


def desk():
    """책상 + 컴퓨터 (2칸): 모니터 화면 빛 · 키보드 · 마우스 · 머그컵 · 의자."""
    w = planks((200, 160, 110))
    out = [Box(-15, 13, -7, 30, 1.5, 13, w), Box(-15, 0, -7, 2, 13, 13, w), Box(5, 0, -7, 10, 13, 13, w)]
    for y in (1, 5, 9):
        out += [Box(5.5, y, 5.9, 9, 3.5, 0.4, planks((215, 175, 125))), Box(9, y + 1.2, 6.3, 2, 0.8, 0.5, C((60, 60, 64)))]

    def code(face, u, v, W, H):
        if face != "front":
            return (35, 35, 40)
        if u in (0, W - 1) or v in (0, H - 1):
            return (25, 25, 28)
        if v % 2 == 0 and 1 < u < (5 + (v * 7) % (W - 6)):
            return [(120, 220, 140), (240, 200, 90), (130, 180, 250)][(v // 2) % 3]
        return (30, 34, 48)
    out += [Box(-6, 14.5, -5, 6, 0.8, 4, C((40, 40, 44))), Box(-3.8, 15.3, -4, 1.6, 4, 1, C((40, 40, 44))),
            Box(-11, 18, -5, 18, 11, 1, AF.code_screen())]
    out += [Box(-10, 14.5, 0, 12, 0.8, 4, C((50, 50, 56))), Box(4, 14.5, 1, 2, 1, 3, C((50, 50, 56))),
            Box(9, 14.5, -1, 3, 3.5, 3, C((230, 90, 80))), Box(10, 18, 0, 1, 0.3, 1, C((90, 55, 30)))]
    out += [Box(-8, 0, 10, 1.5, 7, 1.5, C((40, 40, 44))), Box(-12, 7, 7, 9, 2, 9, velvet((50, 50, 60))),
            Box(-12, 9, 14, 9, 12, 2, velvet((50, 50, 60)))]
    return out


# ═══════════ 장식 ═══════════
def grandfather_clock():
    """괘종시계 (1칸 · 높이 2): 시계판 · 금 진자 · 유리문 · 장식 머리."""
    w = planks((110, 60, 30), (80, 40, 20))
    out = [Box(-6, 0, -5, 12, 3, 10, w), Box(-5, 3, -4, 10, 20, 8, w), Box(-4, 5, 3.9, 8, 16, 0.3, AF.pendulum())]
    out += [Box(-6, 23, -5, 12, 9, 10, w)]

    def face(fc, u, v, W, H):
        cx, cy = (W - 1) / 2, (H - 1) / 2
        r = ((u - cx) ** 2 + (v - cy) ** 2) ** 0.5
        if r > W / 2:
            return (110, 60, 30)
        if r > W / 2 - 1:
            return GOLD
        if (u == round(cx) and v <= cy) or (v == round(cy) and u >= cx):
            return (30, 30, 30)
        return (248, 244, 228)
    out += [Box(-4.5, 23.5, 5, 9, 8, 0.3, face)]
    out += [Box(-7, 32, -5.5, 14, 2, 11, w), Box(-2, 34, -1, 4, 3, 2, C(GOLD))]
    return out


def piano():
    """업라이트 피아노 (2칸): 반짝이는 검정 · 흰 건반 · 검은 건반 · 의자 · 악보."""
    black = lambda f, u, v, W, H: (25, 25, 30) if (u + v) % 6 else (60, 60, 70)
    out = [Box(-16, 0, -7, 32, 26, 9, black), Box(-16, 9, 2, 32, 3, 5, black)]

    def keys(face, u, v, W, H):
        if face != "top":
            return (240, 240, 235)
        if v < H * 0.55 and u % 7 in (1, 2, 4, 5, 6) and u % 2 == 0:
            return (20, 20, 22)
        return (250, 250, 245) if u % 2 else (225, 225, 220)
    out += [Box(-15, 12, 2, 30, 1, 5, AF.piano_keys())]
    out += [Box(-15, 0, 3, 1.5, 9, 1.5, black), Box(13.5, 0, 3, 1.5, 9, 1.5, black)]
    out += [Box(-5, 14, 0, 10, 7, 0.4, C((250, 248, 235))), Box(-4, 19, 0.3, 8, 0.4, 0.2, C((60, 60, 60)))]
    out += [Box(-8, 26, -5, 3, 4, 3, C(GOLD)), Box(-7.5, 30, -4.5, 2, 2, 2, glow((255, 240, 160), (255, 170, 60)))]
    out += [Box(-9, 0, 9, 18, 7, 6, velvet((40, 40, 46)))]
    return out


def aquarium():
    """어항 (2칸): 장 위 유리 수조 · 모래 · 수초 · 물고기 · 거품."""
    out = [Box(-15, 0, -6, 30, 10, 12, planks((60, 60, 70)))]
    # 물 · 물고기 · 수초 · 거품은 모두 움직이는 텍스처 (물이 흐르고 물고기가 왕복)
    out += [Box(-14.8, 10, -5.8, 29.6, 14, 11.6, AF.aquarium_water())]
    out += [Box(-15, 24, -6, 30, 1.5, 12, C((60, 60, 70)))]
    return out


def chandelier():
    """샹들리에 (천장 걸이 · 빛): 사슬 · 금 고리 2단 · 초 8개 · 크리스털."""
    out = [Box(-0.5, 14, -0.5, 1, 18, 1, C((120, 110, 80)))]
    for (y, r) in [(6, 7), (11, 4)]:
        out += [Box(-r, y, -r, 2 * r, 1, 1, metal(GOLD)), Box(-r, y, r - 1, 2 * r, 1, 1, metal(GOLD)),
                Box(-r, y, -r, 1, 1, 2 * r, metal(GOLD)), Box(r - 1, y, -r, 1, 1, 2 * r, metal(GOLD))]
    flame = AF.flicker((255, 225, 120)) if LIT else C((240, 235, 225))
    for (x, z) in [(-7, -7), (6, -7), (-7, 6), (6, 6), (-0.5, -7), (-0.5, 6), (-7, -0.5), (6, -0.5)]:
        out += [Box(x, 7, z, 1, 3, 1, C(WHITE)), Box(x + 0.2, 10, z + 0.2, 0.6, 1.2, 0.6, flame)]
    crystal = AF.sparkle((220, 240, 255), density=7) if LIT else glass((220, 240, 255))
    out += [Box(-1.5, 2, -1.5, 3, 4, 3, crystal), Box(-0.8, 0, -0.8, 1.6, 2, 1.6, crystal)]
    for (x, z) in [(-4, -4), (3, -4), (-4, 3), (3, 3)]:
        out.append(Box(x, 3, z, 1, 3, 1, crystal))
    return out


def bathtub():
    """다리 달린 욕조 (2칸): 흰 법랑 · 금 다리 · 거품 물 · 금 수도꼭지 · 고무 오리."""
    enamel = lambda f, u, v, W, H: (250, 250, 248) if f != "top" else (240, 240, 238)
    out = []
    for x in (-13, 11):
        for z in (-5, 3):
            out.append(Box(x, 0, z, 2, 3, 2, C(GOLD)))
    out += [Box(-15, 3, -7, 30, 2, 14, enamel), Box(-16, 5, -8, 32, 9, 16, enamel), Box(-17, 13, -8.5, 34, 1.5, 17, enamel)]
    out += [Box(-15, 12, -7, 30, 1.2, 14, AF.ripple((150, 205, 235)))]
    for (x, z, s) in [(-10, -3, 3), (-6, 1, 2.5), (5, -2, 3.5), (9, 2, 2), (0, -4, 2)]:
        out.append(Box(x, 13, z, s, s * 0.6, s, C((255, 255, 255))))
    out += [Box(13, 14.5, -1, 1.5, 5, 1.5, C(GOLD)), Box(10.5, 18, -1, 4, 1.2, 1.2, C(GOLD))]
    out += [Box(-3, 13.2, 2, 3, 2, 3, C((255, 220, 40))), Box(-2.5, 15, 2.5, 2, 2, 2, C((255, 220, 40))), Box(-3.5, 15.6, 3, 1, 0.6, 1, C((240, 130, 40)))]
    return out


def home_store():
    """가구점 (1칸): 작은 진열대 위 미니 소파 · 스탠드 · 액자 + 간판."""
    out = [Box(-8, 0, -8, 16, 8, 16, planks((190, 140, 90))), Box(-8.5, 8, -8.5, 17, 1, 17, marble)]
    out += [Box(-6, 9, -2, 8, 2, 4, velvet((60, 130, 140))), Box(-6, 11, -2, 8, 3, 1.2, velvet((60, 130, 140))),
            Box(-6.5, 9, -2, 1, 3.5, 4, velvet((50, 110, 120))), Box(1.5, 9, -2, 1, 3.5, 4, velvet((50, 110, 120)))]
    out += [Box(4, 9, -3, 2, 0.5, 2, C(GOLD)), Box(4.6, 9.5, -2.4, 0.8, 5, 0.8, C(GOLD)), Box(3.5, 14, -3.5, 3, 2.5, 3, glow((255, 240, 200), (240, 200, 140)))]
    sign = lambda f, u, v, W, H: (40, 30, 24) if f in ("front", "back") and 0 < u < W - 1 and 0 < v < H - 1 else (230, 190, 70)
    out += [Box(-7, 9, -8, 1, 13, 1, planks(DARKWOOD)), Box(6, 9, -8, 1, 13, 1, planks(DARKWOOD)), Box(-7, 17, -8.2, 14, 5, 1.2, sign)]
    out += [Box(-4, 18.5, -7.5, 2, 2, 0.3, C((240, 200, 80))), Box(-1, 18.5, -7.5, 2, 2, 0.3, C((200, 90, 90))), Box(2, 18.5, -7.5, 2, 2, 0.3, C((90, 160, 200)))]
    return out


# 이름 → (한글, 도안, 좌우 lat0..lat1, 앞뒤 dep0..dep1, 높이, 특징)
FURNITURE = {
    "sofa": ("3인 소파", sofa, -1, 1, 0, 0, 1, "앉기"),
    "armchair": ("안락의자", armchair, 0, 0, 0, 0, 1, "앉기"),
    "coffee_table": ("커피 테이블", coffee_table, 0, 0, 0, 0, 1, ""),
    "fireplace": ("벽난로", fireplace, 0, 1, 0, 0, 2, "빛 · 불"),
    "tv_stand": ("TV 장식장", tv_stand, 0, 1, 0, 0, 2, "켜고 끄기"),
    "floor_lamp": ("스탠드 조명", floor_lamp, 0, 0, 0, 0, 2, "빛 · 켜고 끄기"),
    "monstera": ("큰 화분", monstera, 0, 0, 0, 0, 2, ""),
    "rug": ("러그", rug, -1, 1, 0, 1, 1, "바닥"),
    "canopy_bed": ("캐노피 침대", canopy_bed, 0, 0, 0, 1, 2, "잠자기"),
    "bunk_bed": ("이층 침대", bunk_bed, 0, 0, 0, 1, 2, "잠자기"),
    "wardrobe": ("옷장", wardrobe, 0, 1, 0, 0, 2, "보관함 54칸"),
    "vanity": ("화장대", vanity, 0, 1, 0, 0, 2, ""),
    "nightstand": ("협탁", nightstand, 0, 0, 0, 0, 2, "빛"),
    "fridge": ("냉장고", fridge, 0, 0, 0, 0, 2, "보관함 27칸 · 음식 안 상함"),
    "sink": ("싱크대", sink, 0, 0, 0, 0, 2, "물 받기"),
    "bookshelf": ("책장", bookshelf, 0, 1, 0, 0, 2, "인챈트 책장 효과"),
    "desk": ("책상 · 컴퓨터", desk, 0, 1, 0, 0, 2, ""),
    "grandfather_clock": ("괘종시계", grandfather_clock, 0, 0, 0, 0, 2, "시간 알림"),
    "piano": ("피아노", piano, 0, 1, 0, 0, 2, "연주"),
    "aquarium": ("어항", aquarium, 0, 1, 0, 0, 2, "빛"),
    "chandelier": ("샹들리에", chandelier, 0, 0, 0, 0, 1, "천장 · 빛"),
    "bathtub": ("욕조", bathtub, 0, 1, 0, 0, 1, "앉기"),
}
