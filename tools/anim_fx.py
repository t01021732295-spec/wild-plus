"""3.4 움직이는 텍스처 효과 모음 (cosmetic_design.Anim). 게임에서는 .mcmeta 프레임 애니메이션으로 재생된다.
모든 효과는 프레임 t (0..n-1) 에서 처음으로 자연스럽게 이어지도록 주기를 n 에 맞춘다."""
import math

from cosmetic_design import Anim, mix, shade

TAU = 2 * math.pi


def _h(*a):
    """작은 정수 해시 (프레임마다 같은 값)"""
    x = 0
    for k in a:
        x = (x * 1103515245 + int(k) * 2654435761 + 12345) & 0x7FFFFFFF
    return x


def flames(n=12, ticks=2, hot=(255, 245, 180), mid=(255, 160, 40), tip=(215, 60, 20), base_h=0.55, back=None):
    """일렁이는 불꽃: 칸마다 높이가 출렁이고 위는 투명(back 이면 그 색)"""
    def f(t, face, u, v, W, H):
        if face in ("top", "bottom"):
            return mid
        ph = TAU * t / n
        h = base_h + 0.28 * math.sin(ph + u * 1.9) + 0.14 * math.sin(2 * ph + u * 0.7 + 1.3)
        h = max(0.2, min(1.0, h))
        fr = (H - 1 - v + 0.5) / H
        if fr > h:
            return back
        k = fr / h
        c = hot if k < 0.35 else (mid if k < 0.72 else tip)
        return shade(c, 1.08) if _h(u, v, t) % 7 == 0 else c
    return Anim(n, ticks, f)


def flicker(c=(255, 220, 120), n=8, ticks=2):
    """촛불 · 작은 불: 밝기만 깜빡"""
    ks = [1.0, 0.9, 1.05, 0.85, 1.0, 0.95, 1.1, 0.9]

    def f(t, face, u, v, W, H):
        return shade(c, ks[t % len(ks)])
    return Anim(n, ticks, f)


def embers(n=16, ticks=3, c1=(255, 150, 40), c2=(150, 30, 10)):
    """숯불: 점들이 천천히 밝아졌다 어두워짐"""
    def f(t, face, u, v, W, H):
        k = 0.5 + 0.5 * math.sin(TAU * t / n + _h(u, v) % 17)
        return mix(c2, c1, k)
    return Anim(n, ticks, f)


def bubbling(liquid=(214, 120, 60), n=16, ticks=2, rate=5):
    """끓는 국물 (윗면): 거품이 부풀었다 터짐 · 옆면은 출렁"""
    light = shade(liquid, 1.25)

    def f(t, face, u, v, W, H):
        if face != "top":
            return shade(liquid, 0.9 if (u + t) % 4 else 1.0)
        for k in range(rate):
            bx, by = _h(k, 1) % max(1, W), _h(k, 2) % max(1, H)
            age = (t + _h(k, 3)) % n
            r = age / n * 2.2
            if age < n - 2 and abs(math.hypot(u - bx, v - by) - r) < 0.6:
                return (255, 235, 200) if age < n * 0.6 else light
            if age >= n - 2 and math.hypot(u - bx, v - by) < 1:
                return shade(liquid, 0.8)
        return liquid if (u * 3 + v + t // 2) % 9 else light
    return Anim(n, ticks, f)


def ripple(water=(110, 190, 235), n=16, ticks=3):
    """잔잔한 물결 (윗면 · 옆면): 반짝이는 줄이 흘러감"""
    def f(t, face, u, v, W, H):
        if face == "top":
            return shade(water, 1.18) if (u + 2 * v - t) % 7 == 0 else water
        return shade(water, 1.12) if (u - v + t) % 9 == 0 else shade(water, 0.95 + 0.05 * (v == 0))
    return Anim(n, ticks, f)


def glint(c, n=24, ticks=2, width=2.0, pause=10):
    """금속 반짝임: 빛줄기가 대각선으로 지나가고 잠시 쉼"""
    def f(t, face, u, v, W, H):
        base = c if face != "top" else shade(c, 1.12)
        base = mix(shade(base, 1.05), shade(base, 0.8), v / max(1, H - 1)) if face != "top" else base
        span = W + H + 4
        moving = n - pause
        if t < moving:
            s = (t / moving) * span - 2
            if abs((u + (H - 1 - v)) - s) < width:
                return mix(base, (255, 255, 250), 0.75)
        return base
    return Anim(n, ticks, f)


def steam(n=16, ticks=3, c=(240, 240, 245)):
    """피어오르는 김 (투명 바탕): 작은 뭉치가 위로 올라가며 흔들림"""
    def f(t, face, u, v, W, H):
        if face in ("top", "bottom"):
            return None
        for k in range(2):
            y = H - 1 - ((t * H // n + k * H // 2) % H)
            x = (W - 1) / 2 + math.sin(TAU * (t / n) * 2 + k) * (W / 3)
            if v == y and abs(u - x) < 0.9:
                return shade(c, 1.0 - 0.25 * (y / max(1, H)))
        return None
    return Anim(n, ticks, f)


def drip(n=16, ticks=2, c=(150, 200, 235)):
    """수도꼭지 물방울: 한 방울이 떨어짐 (투명 바탕)"""
    def f(t, face, u, v, W, H):
        if face in ("top", "bottom"):
            return None
        y = int(t / n * (H + 3)) - 2
        return c if v in (y, y - 1) and 0 <= v < H else None
    return Anim(n, ticks, f)


def chase(base, lit, k, count, n=16, ticks=2):
    """돌아가는 바퀴 · 줄지은 전구: k 번째 조각에 밝은 점이 차례로 지나감"""
    def f(t, face, u, v, W, H):
        d = (k - t * count / n) % count
        return lit if d < 1.5 else (mix(lit, base, 0.6) if d < 3 else base)
    return Anim(n, ticks, f)


def sparkle(c, n=24, ticks=2, density=23):
    """반짝이 (보석 · 유리): 드문드문 별빛"""
    def f(t, face, u, v, W, H):
        if (_h(u, v, face == "top") + t) % density == 0:
            return (255, 255, 255)
        return c
    return Anim(n, ticks, f)


# ═══════════ 어항: 물 흐름 + 헤엄치는 물고기 + 거품 + 흔들리는 수초 ═══════════
FISH = [  # (높이 비율, 색, 위상, 속도 배수)
    (0.30, (255, 140, 40), 0.0, 1), (0.48, (240, 220, 60), 0.35, 2), (0.62, (230, 70, 90), 0.7, 1), (0.40, (90, 160, 250), 0.15, 2),
]
PLANTS = [(0.12, 0.65, (60, 150, 70), 0.0), (0.2, 0.45, (90, 170, 80), 0.4), (0.78, 0.7, (50, 140, 60), 0.2), (0.88, 0.5, (80, 180, 90), 0.7)]


def aquarium_water(n=32, ticks=3):
    def f(t, face, u, v, W, H):
        deep, shallow = (60, 140, 200), (130, 205, 240)
        if face == "top":
            return (175, 225, 250) if (u + 2 * v - t) % 6 == 0 else (120, 195, 235)
        if face == "bottom":
            return (60, 60, 70)
        mirror = face in ("back", "left")
        x = W - 1 - u if mirror else u
        sand = H - 2
        # 모래 · 자갈
        if v >= sand:
            return (230, 210, 160) if _h(x, v) % 5 else (200, 180, 140)
        # 수면 반짝임
        if v == 0:
            return (200, 235, 255) if (x + t) % 5 < 2 else shallow
        # 수초 (흔들림)
        for (px, ph, c, phase) in PLANTS:
            stem = px * W
            hgt = ph * (H - 2)
            above = sand - v  # 바닥에서 높이
            if 0 < above <= hgt:
                sway = math.sin(TAU * (t / n + phase)) * 1.4 * (above / hgt)
                if abs(x - (stem + sway)) < 0.7:
                    return c if above % 3 else shade(c, 1.2)
        # 물고기 (앞 · 뒤 · 옆 모두, 왕복하며 방향 전환)
        if face in ("front", "back") or (face in ("left", "right") and W >= 6):
            for (fy, c, phase, spd) in FISH:
                p = (t / n * spd + phase) % 1.0
                going = p < 0.5
                q = p * 2 if going else 2 - p * 2  # 0→1→0
                fx = 1 + q * (W - 6)
                yy = round(fy * (H - 3)) + 1
                # 몸 3칸 + 꼬리 1칸 (꼬리는 가는 방향 반대쪽), 눈
                body = [fx + d for d in range(3)]
                tail = fx - 1 if going else fx + 3
                if v == yy and any(abs(x - b) < 0.5 for b in body):
                    eye = fx + 2 if going else fx
                    return (20, 20, 30) if abs(x - eye) < 0.5 else c
                if v in (yy - 1, yy + 1) and abs(x - tail) < 0.5 and (t // 2) % 2 == 0:
                    return shade(c, 0.75)
                if v == yy and abs(x - tail) < 0.5:
                    return shade(c, 0.75)
        # 거품 (두 줄기)
        for (bx, off) in ((0.33, 0), (0.7, 9)):
            col = round(bx * W)
            y = sand - 1 - ((t + off) % (sand - 1))
            if v == y and (x == col or x == col + ((t + off) // 3) % 2):
                return (225, 245, 255)
        # 물 (흐르는 결)
        base = mix(shallow, deep, v / max(1, sand))
        if (x - v * 2 + t) % 13 == 0:
            return shade(base, 1.12)
        return base
    return Anim(n, ticks, f)


# ═══════════ 화면 ═══════════
def tv_screen(n=24, ticks=4):
    """TV: 구름이 흘러가는 들판 + 날갯짓하는 새"""
    def f(t, face, u, v, W, H):
        if face != "front":
            return (30, 30, 34) if (u - v) % 9 else (55, 55, 62)
        if u in (0, W - 1) or v in (0, H - 1):
            return (20, 20, 24)
        if v > H * 0.65:
            return (80, 170, 90) if (u + v) % 5 else (60, 140, 70)
        if (u - W * 0.75) ** 2 + (v - H * 0.28) ** 2 < 5:
            return (255, 230, 120)
        for (cy, w, off) in ((0.22, 5, 0), (0.42, 4, W // 2)):
            cx = (t * W / n + off) % (W + w) - w / 2
            if abs(v - cy * H) < 1 and abs(u - cx) < w / 2:
                return (250, 250, 252)
        bx = W - 3 - (t * (W - 4) / n)
        by = round(H * 0.35 + math.sin(TAU * t / n * 2))
        if v == by and abs(u - bx) < 1.5 and not (abs(u - bx) < 0.5 and t % 2):
            return (40, 40, 50)
        return mix((90, 170, 240), (180, 220, 255), v / H)
    return Anim(n, ticks, f)


def code_screen(n=16, ticks=6):
    """컴퓨터: 위로 올라가는 코드 줄 + 깜빡이는 커서"""
    cols = [(120, 220, 140), (240, 200, 90), (130, 180, 250)]

    def f(t, face, u, v, W, H):
        if face != "front":
            return (35, 35, 40)
        if u in (0, W - 1) or v in (0, H - 1):
            return (25, 25, 28)
        line = (v // 2) + t
        if v % 2 == 0 and v < H - 2 and 1 < u < (5 + (line * 7) % (W - 6)):
            return cols[line % 3]
        last = H - 3 if (H - 3) % 2 == 0 else H - 2
        if v == last and u == 2 + (t % 4) and t % 2 == 0:
            return (240, 240, 240)
        return (30, 34, 48)
    return Anim(n, ticks, f)


def pendulum(glass_c=(180, 160, 120), gold=(232, 190, 70), n=20, ticks=2):
    """괘종시계 유리문 안 금 진자 (좌우로 흔들림)"""
    def f(t, face, u, v, W, H):
        g = shade(glass_c, 1.12) if (u - v) % 9 in (0, 1) else glass_c
        if face != "front":
            return g
        ang = 0.38 * math.sin(TAU * t / n)
        cx, top = (W - 1) / 2, 0.5
        L = H * 0.68
        # 막대
        for s in range(int(L * 2)):
            d = s / 2
            px, py = cx + math.sin(ang) * d, top + math.cos(ang) * d
            if abs(u - px) < 0.5 and abs(v - py) < 0.5:
                return gold
        bx, by = cx + math.sin(ang) * L, top + math.cos(ang) * L
        if math.hypot(u - bx, v - by) < 1.6:
            return shade(gold, 1.1 if u < bx else 0.9)
        return shade(g, 0.55)  # 시계 안쪽 그늘
    return Anim(n, ticks, f)


def piano_keys(n=16, ticks=3):
    """건반이 차례로 눌림 (멜로디)"""
    melody = [3, 5, 7, 5, 9, 7, 5, 3, 11, 9, 7, 9, 5, 3, 1, 3]

    def f(t, face, u, v, W, H):
        if face != "top":
            return (240, 240, 235)
        if v < H * 0.55 and u % 7 in (1, 2, 4, 5, 6) and u % 2 == 0:
            return (20, 20, 22)
        key = melody[t % len(melody)] * 2 + 1
        if abs(u - key) < 1 and v >= H * 0.55:
            return (190, 190, 185)
        return (250, 250, 245) if u % 2 else (225, 225, 220)
    return Anim(n, ticks, f)


def mirror_shine(c=(200, 225, 240), n=32, ticks=2, offset=0):
    """거울: 빛 줄기가 비스듬히 지나감"""
    def f(t, face, u, v, W, H):
        g = shade(c, 1.12) if (u - v) % 9 in (0, 1) else c
        s = t * 1.4 - 8
        if abs((u - (v + offset)) - s) < 1.2:
            return mix(g, (255, 255, 255), 0.7)
        return g
    return Anim(n, ticks, f)
