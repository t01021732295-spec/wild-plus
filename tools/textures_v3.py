"""3차 업데이트 텍스처: 화폐, 상인·도적·황금 슬라임 스킨, 스폰 알.

엔티티 스킨은 바닐라 모델의 UV 배치(상자 전개도)에 맞춰 면 단위로 칠한다.
- 상인: VillagerModel (64x64)   - 도적: IllagerModel (64x64)   - 황금 슬라임: SlimeModel (64x32)
"""
import random
from pathlib import Path

from PIL import Image

from textures_v2 import ROOT, hexc, render, save


# ───────────────────── 동전 ─────────────────────
COIN = [
"................", "................", "................", ".....oooooo.....",
"....ohhhhhho....", "...ohhbbbbhho...", "..ohhbmmmmbbbo..", "..ohbmbbbbmbbo..",
"..ohbmbbbbmbdo..", "..ohbmbbbbmbdo..", "..obbbmmmmbddo..", "...obbbbbbddo...",
"....obddddddo...", ".....oooooo.....", "................", "................"]
COIN_PALETTES = {
    "copper_coin": {"o": "#5a2a10", "h": "#ffb38a", "b": "#d9733a", "d": "#a8501e", "m": "#8a3e14"},
    "silver_coin": {"o": "#3a3f45", "h": "#ffffff", "b": "#c8d0d8", "d": "#8a949e", "m": "#6a737c"},
    "gold_coin": {"o": "#6a4400", "h": "#fff6b0", "b": "#f2c230", "d": "#c8901a", "m": "#a8700a"},
}
POUCH = [
"................", "................", "......kkkk......", ".....kYyyYk.....",
"......kyyk......", ".....kNNNNk.....", "....kNnNNNNk....", "...kNNNNNnNNk...",
"..kNNnNNNNNNNk..", "..kNNNNNYYNNNk..", "..kNNNNYccYNnk..", "..kNnNNNYYNNNk..",
"...kNNNNNNNNk...", "....kkkkkkkk....", "................", "................"]
POUCH_PAL = {"k": "#2a1608", "N": "#8a5a2e", "n": "#6a4020", "Y": "#f2c230", "y": "#c8901a", "c": "#fff6b0"}

EGG = [
"................", "................", "......oooo......", ".....obbbbo.....",
"....obbsbbbo....", "....obbbbbso....", "...obsbbbbbbo...", "...obbbbsbbbo...",
"...obbbbbbbso...", "...osbbbbbbbo...", "...obbbsbbbbo...", "....obbbbbbo....",
".....obbsbo.....", "......oooo......", "................", "................"]


def egg(base, spot):
    return render(EGG, {"o": "#1a1a1a", "b": base, "s": spot})


# ───────────────────── 엔티티 스킨 ─────────────────────
def hexa(h):
    """#rrggbb 또는 #rrggbbaa"""
    rgb = tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
    return rgb + ((int(h[7:9], 16),) if len(h) == 9 else (255,))


class Skin:
    def __init__(self, w, h, seed):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def rect(self, x, y, w, h, color, noise=0):
        base = hexa(color)
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                c = base
                if noise:
                    k = self.rnd.uniform(-noise, noise)
                    c = tuple(max(0, min(255, int(v * (1 + k)))) for v in base[:3]) + (base[3],)
                self.im.putpixel((xx, yy), c)

    def box(self, u, v, w, h, d, color, noise=0.06, top=None, bottom=None):
        """바닐라 상자 전개도: 윗면/아랫면, 오른쪽·앞·왼쪽·뒤 면"""
        self.rect(u + d, v, w, d, top or color, noise)
        self.rect(u + d + w, v, w, d, bottom or color, noise)
        self.rect(u, v + d, d, h, color, noise)
        self.rect(u + d, v + d, w, h, color, noise)
        self.rect(u + d + w, v + d, d, h, color, noise)
        self.rect(u + d + w + d, v + d, w, h, color, noise)

    def front(self, u, v, d):
        return u + d, v + d

    def px(self, x, y, color):
        self.im.putpixel((x, y), hexa(color))


def face(s, skin, brow, eye, pupil, mouth=None):
    fx, fy = s.front(0, 0, 8)  # 머리 앞면 (8,8) 8x10
    for x in range(fx + 1, fx + 7):
        s.px(x, fy + 3, brow)
    for x in (fx + 1, fx + 2, fx + 5, fx + 6):
        s.px(x, fy + 4, eye)
    s.px(fx + 2, fy + 4, pupil)
    s.px(fx + 5, fy + 4, pupil)
    if mouth:
        for x in range(fx + 2, fx + 6):
            s.px(x, fy + 8, mouth)


def merchant_skin(seed, robe, trim, under, hood, skin="#c98f6b"):
    s = Skin(64, 64, seed)
    s.box(0, 0, 8, 10, 8, skin)                       # 머리
    face(s, skin, "#3a2414", "#ffffff", "#2a5a2a", "#8a5a46")
    s.box(24, 0, 2, 4, 2, "#b87a58")                  # 코
    # 두건(머리 덧씌움 층): 앞면은 비워 얼굴이 보이게
    s.box(32, 0, 8, 10, 8, hood, noise=0.08)
    fx, fy = 32 + 8, 0 + 8
    s.rect(fx + 1, fy + 2, 6, 8, "#00000000")
    s.box(16, 20, 8, 12, 6, under)                    # 몸통
    s.box(0, 38, 8, 20, 6, robe, noise=0.08)          # 로브
    for x in range(0, 28):                             # 로브 밑단 장식
        s.px(x, 57, trim)
        s.px(x, 56, trim)
    for y in range(44, 58):                            # 앞섶
        s.px(6 + 4, y, trim)
    s.box(44, 22, 4, 8, 4, robe)                      # 팔짱 낀 팔
    s.box(40, 38, 8, 4, 4, robe)
    s.rect(44 + 4, 22 + 4 + 6, 4, 2, skin)           # 손
    s.box(0, 22, 4, 12, 4, "#3a2a1e")                 # 다리
    return s.im


def bandit_skin(seed):
    s = Skin(64, 64, seed)
    skin = "#b88a6a"
    s.box(0, 0, 8, 10, 8, skin)
    face(s, skin, "#1a1a1a", "#ffffff", "#7a1a1a")
    fx, fy = s.front(0, 0, 8)
    # 붉은 복면 (코 아래)
    s.rect(fx, fy + 6, 8, 4, "#8a1a1a", 0.1)
    s.rect(0, 8 + 6, 8, 4, "#8a1a1a", 0.1)
    s.rect(16, 8 + 6, 8, 4, "#8a1a1a", 0.1)
    s.rect(24, 8 + 6, 8, 4, "#8a1a1a", 0.1)
    # 두건
    s.box(32, 0, 8, 12, 8, "#2a2a30", noise=0.1)
    s.rect(32 + 8 + 1, 8 + 3, 6, 9, "#00000000")
    s.box(24, 0, 2, 4, 2, "#a87a5a")
    s.box(16, 20, 8, 12, 6, "#2e2e36")
    s.box(0, 38, 8, 20, 6, "#3a3036", noise=0.1)
    for x in range(0, 28):                             # 허리띠
        s.px(x, 38 + 6 + 6, "#5a3a1a")
    s.px(6 + 3, 38 + 6 + 6, "#e0b53a")
    s.box(44, 22, 4, 8, 4, "#2e2e36")
    s.box(40, 38, 8, 4, 4, "#2e2e36")
    s.box(0, 22, 4, 12, 4, "#1e1e24")
    s.box(40, 46, 4, 12, 4, "#2e2e36")                # 공격 자세 팔
    s.rect(40 + 4, 46 + 4 + 9, 4, 3, skin)
    return s.im


def golden_slime_skin(seed):
    s = Skin(64, 32, seed)
    # 바깥 젤리 (반투명)
    s.box(0, 0, 8, 8, 8, "#ffd84a9c", noise=0.05)
    # 안쪽 몸통
    s.box(0, 16, 6, 6, 6, "#e0a81e", noise=0.1)
    for (x, y) in [(8, 24), (10, 25), (7, 27), (11, 23)]:
        s.px(x, y, "#fff6b0")
    s.box(32, 0, 2, 2, 2, "#3a2a00", noise=0)        # 오른쪽 눈
    s.box(32, 4, 2, 2, 2, "#3a2a00", noise=0)        # 왼쪽 눈
    s.box(32, 8, 1, 1, 1, "#5a3a00", noise=0)        # 입
    return s.im


def build():
    for name, pal in COIN_PALETTES.items():
        save(render(COIN, pal), f"item/{name}.png")
    save(render(POUCH, POUCH_PAL), "item/coin_pouch.png")
    save(egg("#3e7a3a", "#f2c230"), "item/merchant_spawn_egg.png")
    save(egg("#5a3a22", "#9aa4ae"), "item/blacksmith_merchant_spawn_egg.png")
    save(egg("#2e2e36", "#8a1a1a"), "item/bandit_spawn_egg.png")
    save(egg("#f2c230", "#fff6b0"), "item/golden_slime_spawn_egg.png")
    ent = ROOT / "entity"
    ent.mkdir(parents=True, exist_ok=True)
    merchant_skin(31, "#2f6a34", "#f2c230", "#6a4a2a", "#24502a").save(ent / "merchant.png")
    merchant_skin(32, "#4a3426", "#9aa4ae", "#3a3a40", "#2a2a2e", skin="#b8805e").save(ent / "blacksmith_merchant.png")
    bandit_skin(33).save(ent / "bandit.png")
    golden_slime_skin(34).save(ent / "golden_slime.png")


if __name__ == "__main__":
    build()
    print("ok")
