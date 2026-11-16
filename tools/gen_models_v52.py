"""5.1 겁화의 군주 모델: nether_designs_v50.lord → models_gen/kalpa_lord.json (GenModelRenderer::lord)"""
import gen_models_v47 as G
import nether_designs_v50 as N


def main():
    def group(x, y, z, w, h, d, glow):
        if z <= -13.5: return "wings"
        if z <= -10.5 and y >= 70 and w <= 4.1: return "ring"
        if x < -33 and y < 23: return "censer"
        # 5.2.1 걷기 · 공격 모션용 팔다리
        if x >= 17 and y < 67: return "armR"
        if x <= -29 and y < 67: return "armL"
        if y < 37 and -15 <= x <= 14 and d > 1: return "legR" if x > 0 else "legL"
        return "body"
    G.build("kalpa_lord", N.lord(), -34, group, {N.WH, N.EM})


if __name__ == "__main__":
    main()
