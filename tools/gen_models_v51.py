"""5.0.10 DPS 허수아비 모델: dummy_design 의 도안(위 DPS 판은 이름표로 대신) → models_gen/dps_dummy.json"""
import gen_models_v47 as G
import dummy_design as D


def main():
    boxes = D.dummy()[:-1]

    def group(x, y, z, w, h, d, glow):
        if y < 4: return "base"
        if y >= 28: return "head"
        return "body"
    G.build("dps_dummy", boxes, 0, group, set())


if __name__ == "__main__":
    main()
