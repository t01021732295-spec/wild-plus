"""14차 텍스처: 레이드 전리품 3종, 하늘의 탑 차원문(애니메이션), 레이드 제단 룬 바닥, 주간 목표 게시판 그림."""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/wildplus/textures"


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def orb(base_dark, base_light, rim, eye=None):
    """빛나는 구슬 (16×16): 테두리 + 구 그라데이션 + 반사광"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    cx, cy, r = 7.5, 8.0, 6.3
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                # 왼쪽 위가 밝게
                t = max(0.0, min(1.0, 1.0 - (math.hypot(x - 5.5, y - 5.5) / 9.0)))
                px[x, y] = lerp(base_dark, base_light, t) + (255,)
            elif d <= r + 1.0:
                px[x, y] = rim + (255,)
    for (x, y) in [(5, 4), (4, 5), (5, 5)]:
        px[x, y] = (255, 255, 255, 255)
    if eye:
        for (x, y, c) in eye:
            px[x, y] = c + (255,)
    return im


def titan_core():
    """거신의 핵: 갈라진 돌 껍질 사이로 주황빛 용암 심장"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(3)
    cx, cy = 7.5, 8.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 6.6:
                stone = lerp((60, 52, 46), (120, 108, 96), max(0, 1 - math.hypot(x - 5, y - 5) / 10))
                px[x, y] = stone + (255,)
            elif d <= 7.4:
                px[x, y] = (34, 28, 24, 255)
    # 갈라진 틈 (주황 → 노랑)
    cracks = [(7, 3), (7, 4), (8, 5), (8, 6), (7, 7), (6, 8), (5, 9), (8, 8), (9, 9), (10, 10), (11, 10), (7, 9), (7, 10), (6, 11), (4, 7), (3, 7), (10, 6), (11, 5)]
    for (x, y) in cracks:
        px[x, y] = (255, 140, 30, 255)
    for (x, y) in [(7, 8), (8, 7), (7, 9)]:
        px[x, y] = (255, 230, 120, 255)
    for _ in range(6):
        x, y = rnd.randint(4, 11), rnd.randint(4, 12)
        if px[x, y][3] and px[x, y][0] < 130:
            px[x, y] = (90, 80, 72, 255)
    return im


def lich_orb():
    eye = [(7, 7, (40, 0, 60)), (9, 7, (40, 0, 60)), (8, 9, (40, 0, 60)), (7, 10, (230, 255, 120)), (9, 10, (230, 255, 120))]
    im = orb((40, 10, 70), (190, 110, 255), (25, 5, 40), eye)
    return im


def frost_crown():
    """서리 왕관 조각: 얼음 결정 왕관"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    edge, mid, hi = (30, 70, 120, 255), (120, 200, 245, 255), (230, 250, 255, 255)
    # 띠
    d.rectangle([2, 10, 13, 13], fill=mid, outline=edge)
    # 뾰족한 결정 3개
    for (bx, h) in [(3, 6), (7, 9), (11, 6)]:
        for i in range(h):
            w = max(0, (h - i) // 3)
            d.line([bx - w + 0, 9 - i, bx + w + 1, 9 - i], fill=mid)
        d.line([bx, 9 - h + 1, bx, 9], fill=hi)
    d.point([(7, 11), (4, 12), (10, 12)], fill=(90, 160, 255, 255))
    d.line([3, 11, 12, 11], fill=hi)
    return im


def sky_portal(frames=16):
    """하늘빛 소용돌이 차원문 (세로로 프레임을 이어 붙인 애니메이션)"""
    im = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    px = im.load()
    for f in range(frames):
        ph = f / frames * math.pi * 2
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                a = math.atan2(dy, dx)
                d = math.hypot(dx, dy)
                v = (math.sin(a * 3 + d * 0.9 - ph) + 1) / 2
                v2 = (math.sin(x * 0.7 + y * 0.4 + ph * 2) + 1) / 2
                c = lerp((40, 110, 220), (200, 240, 255), v * 0.7 + v2 * 0.3)
                alpha = 200 + int(40 * v)
                px[x, y + f * 16] = c + (alpha,)
    return im


def altar_runes():
    """레이드 제단 윗면: 검은 돌 바탕에 붉게 빛나는 룬 고리"""
    im = Image.new("RGBA", (16, 16), (24, 20, 30, 255))
    px = im.load()
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-6, 6)
            px[x, y] = (26 + n, 22 + n, 32 + n, 255)
    for i in range(48):
        a = i / 48 * math.pi * 2
        x, y = int(round(7.5 + math.cos(a) * 6)), int(round(7.5 + math.sin(a) * 6))
        px[x, y] = (230, 60, 70, 255)
    for (x, y) in [(7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8), (7, 7), (8, 8), (7, 8), (8, 7)]:
        px[x, y] = (255, 120, 120, 255)
    for (x, y) in [(5, 5), (10, 5), (5, 10), (10, 10)]:
        px[x, y] = (200, 40, 60, 255)
    return im


def goal_board():
    """목표 게시판 (64×64 중 위 64×35 사용): 코르크 판 + 메모지 4장 + 압정 + 그림 아이콘"""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(5)
    for y in range(35):
        for x in range(64):
            n = rnd.randint(-14, 14)
            base = (178 + n, 132 + n // 2, 86 + n // 3)
            if rnd.random() < 0.08:
                base = (140, 98, 60)
            px[x, y] = base + (255,)
    d = ImageDraw.Draw(im)
    # 머리 띠 (짙은 나무 + 금색 글씨 대신 별 무늬)
    d.rectangle([0, 0, 63, 5], fill=(92, 58, 30, 255))
    for x in range(4, 62, 6):
        d.point([(x, 2), (x + 1, 3), (x - 1, 3), (x, 4)], fill=(240, 200, 90, 255))
    notes = [((3, 8), (255, 246, 200)), ((33, 8), (220, 240, 255)), ((3, 21), (255, 222, 232)), ((33, 21), (224, 255, 224))]
    pins = [(229, 72, 77), (59, 130, 246), (34, 163, 90), (242, 193, 78)]
    icons = ["sword", "pick", "fish", "tower"]
    for k, ((nx, ny), col) in enumerate(notes):
        d.rectangle([nx + 1, ny + 1, nx + 28, ny + 12], fill=(90, 60, 30, 120))
        d.rectangle([nx, ny, nx + 27, ny + 11], fill=col + (255,))
        # 글줄
        for ly in (ny + 4, ny + 7):
            d.line([nx + 10, ly, nx + 24 - (ly % 3) * 2, ly], fill=(120, 110, 100, 255))
        # 진행 막대
        d.line([nx + 10, ny + 9, nx + 24, ny + 9], fill=(200, 190, 170, 255))
        d.line([nx + 10, ny + 9, nx + 10 + 4 + k * 3, ny + 9], fill=(60, 170, 90, 255))
        # 압정
        d.point([(nx + 13, ny - 1), (nx + 14, ny - 1), (nx + 13, ny), (nx + 14, ny)], fill=pins[k] + (255,))
        # 아이콘
        ix, iy = nx + 2, ny + 2
        if icons[k] == "sword":
            for i in range(6):
                d.point((ix + i, iy + 6 - i), fill=(150, 160, 170, 255))
            d.point([(ix + 1, iy + 4), (ix + 2, iy + 5), (ix, iy + 6)], fill=(110, 70, 30, 255))
        elif icons[k] == "pick":
            d.line([ix, iy + 1, ix + 6, iy + 1], fill=(120, 200, 230, 255))
            d.line([ix + 3, iy + 1, ix + 3, iy + 7], fill=(110, 70, 30, 255))
        elif icons[k] == "fish":
            d.ellipse([ix, iy + 2, ix + 5, iy + 5], fill=(80, 140, 220, 255))
            d.polygon([(ix + 5, iy + 3), (ix + 7, iy + 1), (ix + 7, iy + 6)], fill=(80, 140, 220, 255))
        else:
            d.rectangle([ix + 2, iy + 1, ix + 5, iy + 7], fill=(200, 205, 215, 255))
            d.point([(ix + 3, iy + 3), (ix + 4, iy + 5)], fill=(255, 210, 80, 255))
            d.polygon([(ix + 1, iy + 1), (ix + 3, iy - 1), (ix + 6, iy + 1)], fill=(43, 95, 168, 255))
    # 시즌 리본 (아래)
    d.rectangle([0, 33, 63, 34], fill=(80, 50, 120, 255))
    return im


def build():
    item, block = OUT / "item", OUT / "block"
    item.mkdir(parents=True, exist_ok=True)
    block.mkdir(parents=True, exist_ok=True)
    titan_core().save(item / "titan_core.png")
    lich_orb().save(item / "lich_orb.png")
    frost_crown().save(item / "frost_crown.png")
    sky_portal().save(block / "sky_portal.png")
    (block / "sky_portal.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 2, "interpolate": True}}))
    altar_runes().save(block / "raid_altar_top.png")
    goal_board().save(block / "goal_board.png")
    return 6


if __name__ == "__main__":
    print(build())
