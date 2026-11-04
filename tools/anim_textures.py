"""3.4: 기존 16×16 블록 텍스처에 움직임을 입힌다 (프레임을 세로로 쌓고 .mcmeta).
여러 번 돌려도 같다: 이미 움직이는 텍스처면 첫 프레임을 원본으로 쓴다."""
import colorsys
import json
import math
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures/block"
TAU = 2 * math.pi


def _base(name):
    im = Image.open(TEX / f"{name}.png").convert("RGBA")
    w = im.width
    return im.crop((0, 0, w, w)) if im.height > w else im


def _hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def _scale(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def _rnd(x, y):
    return (x * 73856093 ^ y * 19349663) % 997


def scroll(dx=0, dy=0, box=None):
    """box 안을 한 프레임에 dx · dy 칸씩 밀기 (빙 돌아옴)"""
    def f(im, t, n):
        out = im.copy()
        x0, y0, x1, y1 = box or (0, 0, im.width, im.height)
        w, h = x1 - x0, y1 - y0
        src, dst = im.load(), out.load()
        for x in range(x0, x1):
            for y in range(y0, y1):
                dst[x, y] = src[x0 + (x - x0 + dx * t) % w, y0 + (y - y0 + dy * t) % h]
        return out
    return f


def pulse(match, lo=0.7, hi=1.25):
    """match(색) 인 픽셀이 다 같이 숨쉬듯 밝아졌다 어두워짐"""
    def f(im, t, n):
        out = im.copy()
        px = out.load()
        k = lo + (hi - lo) * (0.5 + 0.5 * math.sin(TAU * t / n))
        for x in range(im.width):
            for y in range(im.height):
                c = px[x, y]
                if c[3] and match(c):
                    px[x, y] = _scale(c, k)
        return out
    return f


def twinkle(match, lo=0.6, hi=1.3):
    """match(색) 인 픽셀이 저마다 다른 박자로 반짝임"""
    def f(im, t, n):
        out = im.copy()
        px = out.load()
        for x in range(im.width):
            for y in range(im.height):
                c = px[x, y]
                if c[3] and match(c):
                    ph = _rnd(x, y) / 997
                    k = lo + (hi - lo) * (0.5 + 0.5 * math.sin(TAU * (t / n * 2 + ph)))
                    px[x, y] = _scale(c, k)
        return out
    return f


def glint(width=2.2, pause=0.4):
    """빛줄기가 대각선으로 지나감 (금 · 보석)"""
    def f(im, t, n):
        out = im.copy()
        px = out.load()
        moving = n * (1 - pause)
        if t >= moving:
            return out
        s = t / moving * (im.width * 2 + 6) - 3
        for x in range(im.width):
            for y in range(im.height):
                c = px[x, y]
                if c[3] and abs(x + y - s) < width:
                    px[x, y] = tuple(int(c[i] + (255 - c[i]) * 0.6) for i in range(3)) + (c[3],)
        return out
    return f


def chain(*fx):
    def f(im, t, n):
        for g in fx:
            im = g(im, t, n)
        return im
    return f


sat = lambda c: _hsv(c)[1] > 0.45 and _hsv(c)[2] > 0.45
red = lambda c: c[0] > 150 and c[1] < 110 and c[2] < 110
orange = lambda c: c[0] > 200 and 80 < c[1] < 230 and c[2] < 120
cyan = lambda c: c[2] > 170 and c[1] > 150 and c[0] < 150
purple = lambda c: c[2] > 120 and c[0] > 90 and c[1] < 110
green = lambda c: c[1] > 140 and c[0] < 140 and c[2] < 140
bright = lambda c: _hsv(c)[2] > 0.75 and _hsv(c)[1] > 0.3
blue = lambda c: c[2] > 160 and c[0] < 140

# 이름: (프레임 수, 프레임 틱, 효과)
EFFECTS = {
    "stock_screen": (16, 4, scroll(dx=1, box=(1, 1, 15, 15))),            # 주식 그래프가 왼쪽으로 흘러감
    "stock_terminal_front": (16, 4, twinkle(sat)),                          # 단말기 화면 숫자 깜빡
    "atm_front": (16, 3, pulse(blue, 0.8, 1.2)),                            # ATM 화면 빛
    "gacha_machine_front": (24, 2, twinkle(sat, 0.55, 1.35)),               # 뽑기 공들 반짝
    "enhance_forge_front": (12, 2, twinkle(orange, 0.65, 1.3)),             # 강화 화로 불빛 일렁
    "ore_miner_motor": (8, 1, scroll(dy=1)),                                # 채굴기 모터 회전
    "ore_miner_bit": (8, 1, scroll(dy=2)),                                  # 드릴 날 회전
    "ore_miner_side": (16, 3, twinkle(sat)),                                # 계기판 불빛
    "raid_altar_top": (20, 3, pulse(red, 0.6, 1.35)),                       # 레이드 제단 문양 맥동
    "dungeon_seal": (20, 3, twinkle(purple, 0.5, 1.4)),                     # 봉인 돌 보라 조각
    "dungeon_seal_gate": (20, 3, pulse(cyan, 0.6, 1.3)),                    # 봉인 문 문양 맥동
    "chunk_loader_top": (20, 3, pulse(purple, 0.7, 1.35)),                  # 청크 로더 코어
    "chunk_loader_side": (20, 3, pulse(purple, 0.7, 1.35)),
    "luxury_shop_side": (24, 2, chain(twinkle(cyan, 0.8, 1.2), glint())),   # 명품관 다이아 반짝
    "hall_gold": (32, 2, glint(2.5, 0.55)),                                 # 명예의 전당 금 블록 빛줄기
    "pharmacy_front": (20, 3, pulse(green, 0.75, 1.3)),                     # 약국 초록 십자가
    "vending_machine_front_upper": (16, 3, twinkle(bright, 0.6, 1.3)),      # 자판기 불빛
    "auto_fisher_side": (16, 3, twinkle(sat)),
    "auto_harvester_side": (16, 3, pulse(green, 0.75, 1.25)),
    "casino_rail": (16, 2, twinkle(bright, 0.6, 1.35)),                     # 카지노 테이블 테두리 전구
}


def build():
    for name, (n, ticks, fx) in EFFECTS.items():
        if not (TEX / f"{name}.png").exists():
            continue
        base = _base(name)
        w, h = base.size
        sheet = Image.new("RGBA", (w, h * n))
        for t in range(n):
            sheet.paste(fx(base, t, n), (0, t * h))
        sheet.save(TEX / f"{name}.png")
        (TEX / f"{name}.png.mcmeta").write_text(json.dumps({"animation": {"frametime": ticks}}))
    return len(EFFECTS)
