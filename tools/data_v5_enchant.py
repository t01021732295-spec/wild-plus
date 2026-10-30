"""5차: 인챈트 최대 레벨 상향. 바닐라 원본(tools/vanilla/enchantment)을 읽어 max_level 만 바꿔 덮어쓴다."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "vanilla/enchantment"
OUT = ROOT.parent / "src/main/resources/data/minecraft/enchantment"

NEW_MAX = {
    "sharpness": 7, "smite": 7, "bane_of_arthropods": 7, "efficiency": 7, "power": 7, "impaling": 7, "density": 7,
    "unbreaking": 5, "fortune": 5, "looting": 5, "luck_of_the_sea": 5, "respiration": 5, "sweeping_edge": 5,
    "thorns": 5, "swift_sneak": 5, "soul_speed": 5, "loyalty": 5,
    "protection": 6, "fire_protection": 6, "blast_protection": 6, "projectile_protection": 6, "feather_falling": 6,
    "piercing": 6, "breach": 6,
    "fire_aspect": 3, "punch": 3, "knockback": 3,
}


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, level in NEW_MAX.items():
        d = json.loads((SRC / f"{name}.json").read_text())
        d["max_level"] = level
        (OUT / f"{name}.json").write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n")
    return NEW_MAX


if __name__ == "__main__":
    print(len(build()), "enchantments")
