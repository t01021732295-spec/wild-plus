"""5.0 네더 몬스터 모델: nether_designs_v50 의 도안 → models_gen/*.json + textures/entity/gen/*.png (GenModelRenderer)."""
import gen_models_v47 as G
import nether_designs_v50 as N

BRIGHT = {N.WH, N.EM}


def main():
    def hound(x, y, z, w, h, d, glow):
        if z + d <= -10.5: return "tail"
        if z >= 10.5: return "head"
        if y < 8 and h >= 8: return "legA" if (x < 0) == (z > 0) else "legB"
        return "body"
    def turtle(x, y, z, w, h, d, glow):
        return "head" if z >= 13.5 and y < 12 else "body"
    def shaman(x, y, z, w, h, d, glow):
        return "talisman" if x >= 12 and w < 1 else "body"
    def golem(x, y, z, w, h, d, glow):
        if y < 34 and x >= 13: return "armR"
        if y < 34 and x + w <= -13: return "armL"
        return "body"
    G.build("ember_hound", N.hound(), 4, hound, BRIGHT)
    G.build("lava_turtle", N.turtle(), 6, turtle, BRIGHT)
    G.build("ash_shaman", N.shaman(), -4, shaman, BRIGHT)
    G.build("obsidian_golem", N.golem(), -13, golem, BRIGHT)


if __name__ == "__main__":
    main()
