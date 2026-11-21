"""26.1's baby textures, for the baby models shown while Game Drops Backport is installed (babies.client.BabyModels).

Copies each texture from the 26.3 client jar (the newest 26.x release) into
assets/buzzybeesbackport/textures/entity/<same path>, keyed (tRNS) transparency rewritten as alpha for 1.12.2.

Run: python -I tools/babies/textures.py   (idempotent)
"""
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png_rgba  # noqa: E402

JAR = "G:/MCDownloads/minecraft-26.3/client.jar"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                   "src", "main", "resources", "assets", "buzzybeesbackport", "textures", "entity")

TEXTURES = ["bee/bee_baby", "bee/bee_angry_baby", "bee/bee_nectar_baby", "bee/bee_angry_nectar_baby"]


def main():
    jar = zipfile.ZipFile(JAR)
    for t in TEXTURES:
        data = png_rgba.keyed_to_rgba(jar.read("assets/minecraft/textures/entity/%s.png" % t))
        path = os.path.join(OUT, *(t + ".png").split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
    print("%d textures" % len(TEXTURES))


if __name__ == "__main__":
    main()
