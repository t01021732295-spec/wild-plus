"""5.1 겁화의 군주 모델: nether_designs_v50.lord → models_gen/kalpa_lord.json (GenModelRenderer::lord)"""
import gen_models_v47 as G
import nether_designs_v50 as N


def main():
    def group(x, y, z, w, h, d, glow):
        if z <= -13.5: return "wings"
        if z <= -10.5 and y >= 70 and w <= 4.1: return "ring"
        if x < -33 and y < 23: return "censer"
        return "body"
    G.build("kalpa_lord", N.lord(), -34, group, {N.WH, N.EM})


if __name__ == "__main__":
    main()
