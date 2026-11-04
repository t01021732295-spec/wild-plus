"""3.2 식당 손님 · 요리사 피부 (64×64 플레이어 피부 배치)."""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures/entity/customer"

SKIN, SKIN_D = (247, 200, 168), (226, 172, 140)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


class Skin:
    def __init__(self):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()

    def rect(self, x, y, w, h, c, k_noise=True):
        for i in range(w):
            for j in range(h):
                cc = c
                if k_noise and len(c) >= 3 and ((x + i) * 7 + (y + j) * 13) % 11 == 0:
                    cc = shade(c, 0.93)
                self.px[x + i, y + j] = tuple(cc[:3]) + ((cc[3],) if len(cc) > 3 else (255,))

    def box(self, u, v, w, h, d, fn):
        """상자 펼친 그림: fn(면, x, y, 가로, 세로) -> 색. 면: top bottom right front left back"""
        faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                 "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}
        for f, (x0, y0, fw, fh) in faces.items():
            for i in range(fw):
                for j in range(fh):
                    c = fn(f, i, j, fw, fh)
                    if c is not None:
                        self.px[x0 + i, y0 + j] = tuple(c[:3]) + (255,) if len(c) == 3 else c


def make(hair, shirt, pants, shoes, eye=(60, 90, 160), apron=None, coat=None, armor=None, crown=None, hat=None, hatband=None,
         cape=None, beard=None, brim=None, collar=None):
    s = Skin()

    def head(f, x, y, w, h):
        if f == "top":
            return hair
        if f == "bottom":
            return SKIN_D
        if f == "back":
            return hair if y < 6 else SKIN_D
        if f in ("left", "right"):
            return hair if y < 3 or (y < 5 and ((f == "right" and x < 2) or (f == "left" and x > 5))) else SKIN
        # 앞: 앞머리 · 눈 · 입
        if y < 2:
            return hair
        if y == 2 and x in (0, 7):
            return hair
        if y == 4 and x in (1, 6):
            return (250, 250, 250)
        if y == 4 and x in (2, 5):
            return eye
        if y == 3 and x in (1, 2, 5, 6):
            return shade(hair, 0.8)
        if beard and y >= 6:
            return beard
        if y == 6 and x in (3, 4):
            return (190, 110, 100)
        if y == 5 and x in (3, 4):
            return SKIN_D
        return SKIN

    s.box(0, 0, 8, 8, 8, head)

    def body(f, x, y, w, h):
        base = coat or shirt
        if cape and f == "back":
            return shade(cape, 0.9 if (x + y) % 5 else 1.05)
        if armor and f in ("front", "back") and 1 <= y <= 8:
            return shade(armor, 1.1 if y < 2 else 0.95 if (x + y) % 4 else 1.0)
        if f == "front":
            if collar and y < 2:
                return collar
            if apron and y >= 2 and 1 <= x <= 6:
                return apron if not (y == 2 and x in (1, 6)) else shade(apron, 0.85)
            if coat and x in (3, 4):
                return shade(coat, 0.75) if y % 3 else (230, 200, 90)
            if y == 11:
                return (60, 40, 25)
        if y == 11:
            return (60, 40, 25)
        return shade(base, 1.0 if f != "back" else 0.92)

    s.box(16, 16, 8, 12, 4, body)

    def arm(f, x, y, w, h):
        if y >= 10:
            return SKIN
        if armor and y < 4:
            return shade(armor, 1.05)
        base = coat or shirt
        return shade(base, 0.95 if f in ("back", "left") else 1.0)

    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    def leg(f, x, y, w, h):
        if f == "bottom":
            return shade(shoes, 0.8)
        if y >= 10:
            return shoes
        return shade(pants, 0.95 if f in ("back",) else 1.0)

    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)

    # 모자 겹 (머리 바깥 0.5 픽셀)
    def hatlayer(f, x, y, w, h):
        if crown:
            if f == "top":
                return None
            if f in ("front", "back", "left", "right") and y <= 1:
                return crown if (y == 1 or x % 3 == 1) else None
            if f == "front" and y == 1 and x in (3, 4):
                return (220, 40, 60)
            return None
        if hat:
            if f == "top":
                return hat
            if f in ("front", "back", "left", "right"):
                if y <= 1:
                    return hat
                if y == 2:
                    return hatband or shade(hat, 0.8)
            return None
        return None

    if crown or hat:
        s.box(32, 0, 8, 8, 8, hatlayer)
    return s.im


KINDS = {
    "villager": dict(hair=(110, 70, 40), shirt=(90, 150, 90), pants=(70, 80, 110), shoes=(60, 45, 35), apron=(240, 230, 200)),
    "traveler": dict(hair=(60, 40, 30), shirt=(150, 110, 60), pants=(80, 70, 60), shoes=(70, 50, 35), coat=(140, 95, 50),
                     hat=(120, 80, 40), hatband=(200, 60, 50)),
    "adventurer": dict(hair=(40, 40, 48), shirt=(70, 80, 110), pants=(60, 60, 70), shoes=(50, 50, 55), armor=(175, 180, 190),
                       hat=(160, 165, 175), hatband=(120, 125, 135)),
    "noble": dict(hair=(230, 210, 140), shirt=(120, 40, 140), pants=(40, 30, 60), shoes=(30, 25, 30), crown=(240, 200, 70),
                  cape=(150, 30, 60), collar=(245, 240, 230), eye=(70, 150, 90)),
    "chef": dict(hair=(40, 30, 25), shirt=(250, 250, 250), pants=(40, 40, 50), shoes=(30, 30, 30), apron=(60, 60, 70),
                 hat=(250, 250, 250), hatband=(230, 230, 230), beard=None),
}


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, kw in KINDS.items():
        make(**kw).save(OUT / f"{name}.png")
    return len(KINDS)


if __name__ == "__main__":
    print(build())
