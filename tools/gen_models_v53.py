"""5.3 고대 도시 몬스터 모델: ancient_designs_v53 → models_gen/*.json (GenModelRenderer::stalker/bats/priest/sculkGolem)."""
import gen_models_v47 as G
import ancient_designs_v53 as A

BRIGHT = {A.CY, A.CY2}


def main():
    def stalker(x, y, z, w, h, d, glow):
        cx = x + w / 2
        if cx >= 7 or cx <= -7.5:
            if y < 36: return "armR" if cx > 0 else "armL"
        if y >= 31: return "head"
        if y < 18: return "legR" if cx > 0 else "legL"
        return "body"

    def bats(x, y, z, w, h, d, glow):
        if w <= 0.81 and d <= 0.81 and abs(x) < 6 and abs(z) < 6 and 13 <= y <= 19: return "core"
        if abs(x + w / 2) < 3 and abs(z + d / 2) < 3 and 13 <= y <= 19: return "core"
        if z >= 5: return "bat3"
        return "bat1" if x + w / 2 < 0 else "bat2"

    def priest(x, y, z, w, h, d, glow):
        if x >= 7: return "staff"
        if x + w <= -7: return "candles"
        return "body"

    def golem(x, y, z, w, h, d, glow):
        if y < 34 and x >= 13: return "armR"
        if y < 34 and x + w <= -13: return "armL"
        if y >= 38 and (h >= 10 or (w <= 3.1 and y >= 46)): return "tendrils"
        if y < 14 and abs(x + w / 2) < 12: return "legR" if x > 0 else "legL"
        return "body"

    G.build("sculk_stalker", A.stalker(), 0, stalker, BRIGHT)
    G.build("echo_bats", A.bats(), 0, bats, BRIGHT)
    G.build("silent_priest", A.priest(), 0, priest, BRIGHT)
    G.build("sculk_golem", A.golem(), 0, golem, BRIGHT)


if __name__ == "__main__":
    main()
