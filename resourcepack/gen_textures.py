#!/usr/bin/env python3
"""Generates every Elementary texture as a 16x16 PNG.

Textures are authored as text-art grids: each character is one pixel, mapped
through a palette. Shards share two silhouette templates (tier 1 raw shard,
tier 2 cut gem) recoloured per element; ability icons are individual grids.

Run from resourcepack/:  python3 gen_textures.py
Outputs into assets/elementary/textures/{item,hud}/ plus a labelled
contact sheet for review.
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "assets", "elementary", "textures", "item")
HUD_DIR = os.path.join(HERE, "assets", "elementary", "textures", "hud")


def c(hexstr, a=255):
    hexstr = hexstr.lstrip("#")
    return (int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16), a)


T = (0, 0, 0, 0)  # transparent

# ---------------------------------------------------------------- palettes
# Shard roles: O outline · 1 dark base · 2 mid · 3 light · 4 tip · H highlight · S sparkle
# Tier 2 uses the same roles but richer, more saturated values.
SHARD_PALETTES = {
    "earth": {
        1: {"O": "#2b1c10", "1": "#6b4226", "2": "#776830", "3": "#5da03e",
            "4": "#8ed455", "H": "#c8f096", "S": "#eaffd0"},
        2: {"O": "#1d2408", "1": "#7a4a1e", "2": "#7e8f1f", "3": "#4bb838",
            "4": "#8ef04e", "H": "#d6ff9e", "S": "#f4ffdd"},
    },
    "water": {
        1: {"O": "#0e2a5c", "1": "#2456c4", "2": "#2e86e0", "3": "#45c8e8",
            "4": "#7ffbf0", "H": "#c8fff8", "S": "#eafffc"},
        2: {"O": "#071f52", "1": "#1440c8", "2": "#1e7ef0", "3": "#2fd4f4",
            "4": "#7cfff4", "H": "#d0fffa", "S": "#f2fffd"},
    },
    "fire": {
        1: {"O": "#4a0e0e", "1": "#a81e1e", "2": "#e05a24", "3": "#f58a2b",
            "4": "#ffc13d", "H": "#ffe58a", "S": "#fff7d0"},
        2: {"O": "#3d0505", "1": "#c01414", "2": "#f04c14", "3": "#ff8c1a",
            "4": "#ffd23d", "H": "#fff0a0", "S": "#fffbe0"},
    },
    "air": {
        1: {"O": "#5a6b85", "1": "#9fb6d9", "2": "#bccfe8", "3": "#d9e6f5",
            "4": "#f4f9ff", "H": "#ffffff", "S": "#ffffff"},
        2: {"O": "#46587a", "1": "#8fb0e0", "2": "#b4d2f2", "3": "#dcedfc",
            "4": "#f8fcff", "H": "#ffffff", "S": "#ffffff"},
    },
}

# ------------------------------------------------------- shard silhouettes
# Tier 1: raw amethyst-style cluster — one tall crystal, two shardlets.
SHARD_T1 = [
    "................",
    ".......O........",
    "......O4O.......",
    "......O44O......",
    ".....O4H4O......",
    ".....O4H44O.....",
    ".....O3H34O.....",
    "..O.O33H334O....",
    ".O4O.O3H33O.O...",
    ".O33O23H32OO4O..",
    "O333O22H22O33O..",
    "O233O22H22O33O..",
    "O122O21H12O22O..",
    ".O11O111O11O1O..",
    "..OOO.OOO.OOO...",
    "................",
]

# Tier 2: brilliant-cut gem — flat table, wide girdle, pavilion to a point.
SHARD_T2 = [
    "................",
    "...OOOOOOOOOO...",
    "..O4S43333334O..",
    ".O44S334444334O.",
    "O334433443344H3O",
    "O133344444333H1O",
    "O213344H44331H2O",
    ".O2133HH4H312O..",
    "..O213H4H4312O..",
    "...O21344312O...",
    "....O213312O....",
    ".....O21312O....",
    "......O212O.....",
    ".......O2O......",
    "........O.......",
    "................",
]

# ------------------------------------------------------------ ability icons
# Each icon gets its own small palette. W = white.
ICONS = {
    # EARTH — Tremor: rock debris thrown over cracked ground
    "icon_tremor": (
        {"O": "#2b1c10", "g": "#6b4226", "G": "#8a5c32", "k": "#3a2814",
         "e": "#8ed455", "S": "#b8bdbd", "s": "#8f9494", "W": "#ffffff"},
        [
            "................",
            ".......OO.......",
            "......OSsO......",
            "..OO..OssO..e...",
            ".OSsO..OO...OO..",
            ".OssO..e...OSsO.",
            "..OO...e....OO..",
            "................",
            "OOOOOOOOOOOOOOOO",
            "OGGGOGGGGGOGGGGO",
            "OGGOkGGGGGGkGGGO",
            "OGGGOGGkOGGGOGGO",
            "OgGOkGGOkGGGkGgO",
            "OggGOggkGggOkggO",
            "OgggkggOkggkGggO",
            "OOOOOOOOOOOOOOOO",
        ],
    ),
    # EARTH — Bulwark: crenellated stone wall
    "icon_bulwark": (
        {"O": "#2b1c10", "s": "#9aa19e", "S": "#c2c8c6", "d": "#6e7573"},
        [
            "................",
            ".OOOO.OOOO.OOOO.",
            ".OSsO.OSsO.OSsO.",
            ".OSsO.OSsO.OSsO.",
            ".OSsOOOSsOOOSsO.",
            ".OSssssSssssssO.",
            ".OsssssssssssdO.",
            ".OOOOOOOOOOOOOO.",
            ".OSssOSsssssOsO.",
            ".OsssOssssssOsO.",
            ".OdddOddddddOdO.",
            ".OSssssOSsssssO.",
            ".OsssssOssssssO.",
            ".OdddddOddddddO.",
            ".OOOOOOOOOOOOOO.",
            "................",
        ],
    ),
    # EARTH — Cataclysm: erupting stone spikes
    "icon_cataclysm": (
        {"O": "#2b1c10", "s": "#8f9494", "S": "#b8bdbd", "d": "#6d7373",
         "e": "#8ed455", "E": "#c8f096"},
        [
            "................",
            ".......O........",
            "......OSO.......",
            "......OSsO......",
            "..O..OSssO..O...",
            ".OSO.OSssO.OsO..",
            ".OSsOOSsdOOSsO..",
            ".OSsO.OSsO.OsO..",
            "OSssOOSssdOOsdO.",
            "OSsdOSsssdOSsdO.",
            "OssdOsssddOssdO.",
            "OsddOsdsddOsddO.",
            ".EeOOOeeOOOeE...",
            "..EeeEeeEeeE....",
            "................",
            "................",
        ],
    ),
    # WATER — Tide Pull: grappling hook on a taut line
    "icon_tide_pull": (
        {"O": "#0e2a5c", "h": "#45c8e8", "H": "#c8fff8", "r": "#7ffbf0"},
        [
            "..............OO",
            ".............OrO",
            "..........OOOrO.",
            ".........OhhO...",
            ".........OhhO...",
            ".........OhhO...",
            ".........OhhO...",
            ".........OhhO...",
            "...O.....OhhO...",
            "..OO....OhhhO...",
            ".OhHO..OhhhO....",
            ".OhhO..OhhO.....",
            "..OhhOOhhhO.....",
            "...OhhhhhO......",
            "....OOOOO.......",
            "................",
        ],
    ),
    # WATER — Thunderstorm: storm cloud + bolt
    "icon_thunderstorm": (
        {"O": "#0e2a5c", "b": "#2e86e0", "B": "#45c8e8", "d": "#2456c4",
         "W": "#ffffff", "y": "#7ffbf0"},
        [
            "................",
            "....OOO..OO.....",
            "...OBBBOOBBO....",
            "..OBWBBBBBBBO...",
            ".OBBBBBBBBBBBO..",
            ".OBbBBBbbBBbBO..",
            ".ObbbdbbbdbbdO..",
            "..OddddddddddO..",
            "...OOOOWWOOOO...",
            "......OWWO......",
            ".....OWWO.......",
            "....OWWWWO......",
            "......OWWO......",
            ".....OWWO.......",
            ".....OWO........",
            "......W.........",
        ],
    ),
    # WATER — Maelstrom is drawn programmatically (see draw_maelstrom below):
    # a hand-plotted spiral never comes out round at 16x16.
    # FIRE — Fireball: blazing orb
    "icon_fireball": (
        {"O": "#4a0e0e", "r": "#a81e1e", "o": "#e05a24", "a": "#f58a2b",
         "y": "#ffc13d", "Y": "#ffe58a", "W": "#fff7d0"},
        [
            "................",
            "......O..O......",
            "..O..OaO.OyO....",
            "..OyOOaaOOyO....",
            "...OyOaaayO.....",
            "..OOyaaayyOO....",
            ".OraayyyyyaarO..",
            ".OrayYYYYYyarO..",
            "OoayYWWWWYyaoO..",
            "OoayYWWWWYyaoO..",
            ".OrayYYYYYyarO..",
            ".OraayyyyyaarO..",
            "..OOaaayyaOO....",
            "....OOraOO......",
            "......OO........",
            "................",
        ],
    ),
    # FIRE — Pyre: ring of flame
    "icon_pyre": (
        {"O": "#4a0e0e", "r": "#a81e1e", "o": "#e05a24", "a": "#f58a2b",
         "y": "#ffc13d", "Y": "#ffe58a"},
        [
            "................",
            "...O....O...O...",
            "..OyO..OyO.OyO..",
            "..OyO.OyaO.OyO..",
            "..OyaO.OyaOOyaO.",
            ".OyaaO.OaaO.OaaO",
            "..OOOOOOOOOOOO..",
            ".OaaaaaaaaaaaaO.",
            "OaoOOOOOOOOOOoaO",
            "OroO........OorO",
            "OroOOOOOOOOOOorO",
            ".OroooooooooorO.",
            "..OOrrrrrrrrOO..",
            "....OOOOOOOO....",
            "................",
            "................",
        ],
    ),
    # FIRE — Meteor: falling rock with a flame trail
    "icon_meteor": (
        {"O": "#4a0e0e", "k": "#4d3a30", "K": "#6d564a", "s": "#8a7264",
         "o": "#e05a24", "a": "#f58a2b", "y": "#ffc13d", "Y": "#ffe58a"},
        [
            "..........O..O..",
            "........OOyOOyO.",
            ".......OyyaOaO..",
            "......OyYyaaO...",
            ".....OyYyaaoO...",
            "....OyYyyaoO....",
            "...OyYyyaaoO....",
            "..OyyyaaaoO.....",
            ".OOaaaaooO......",
            "OKskaooOO.......",
            "OsKKkaO.........",
            "OKsKkkO.........",
            "OkKkskO.........",
            ".OkkkO..........",
            "..OOO...........",
            "................",
        ],
    ),
    # AIR — Updraft: stacked rising chevrons
    "icon_updraft": (
        {"O": "#46587a", "w": "#b4d2f2", "l": "#dcedfc", "W": "#ffffff"},
        [
            "................",
            ".......OO.......",
            "......OWWO......",
            ".....OWWWWO.....",
            "....OWWOOWWO....",
            "...OWWO..OWWO...",
            "...OOO....OOO...",
            ".......OO.......",
            "......OllO......",
            ".....OllllO.....",
            "....OllOOllO....",
            "....OOO..OOO....",
            ".......OO.......",
            "......OwwO......",
            ".....OwOOwO.....",
            "......O..O......",
        ],
    ),
    # AIR — Gale: streaming wind lines with a curl
    "icon_gale": (
        {"O": "#46587a", "w": "#b4d2f2", "l": "#dcedfc", "W": "#ffffff"},
        [
            "................",
            "................",
            "....OOOOOOO.....",
            "...OWWWWWWWO....",
            "...OOOOOOOWWO...",
            "..........OWO...",
            ".OOOOOOOOOOWO...",
            "OllllllllllWO...",
            "OOOOOOOOOOOO....",
            "................",
            "...OOOOOOOOOO...",
            "..OwwwwwwwwwwO..",
            "..OOOOOOOOOOwO..",
            "..........OwwO..",
            "...........OO...",
            "................",
        ],
    ),
    # AIR — Tempest: cyclone funnel
    "icon_tempest": (
        {"O": "#46587a", "w": "#b4d2f2", "l": "#dcedfc", "W": "#ffffff",
         "d": "#8fb0e0"},
        [
            "................",
            ".OOOOOOOOOOOOO..",
            "OWWWWWWWWWWWWWO.",
            ".OOOOOOOOOOOOWO.",
            "..OlllllllllOO..",
            "..OOOOOOOOOlO...",
            "...OwwwwwwwO....",
            "....OOOOOOwO....",
            "....OdddddO.....",
            ".....OOOOdO.....",
            ".....OwwwO......",
            "......OOwO......",
            "......OdO.......",
            ".......OO.......",
            ".......O........",
            "................",
        ],
    ),
}


def draw_maelstrom():
    """Whirlpool seen from above: dark disc, Archimedean spiral arm, white eye."""
    import math
    img = Image.new("RGBA", (16, 16), T)
    px = img.load()
    cx = cy = 7.5
    outline, dark, arm, foam, eye = (c("#0e2a5c"), c("#2456c4"),
                                     c("#45c8e8"), c("#7ffbf0"), c("#eafffc"))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r <= 6.4:
                px[x, y] = dark
            elif r <= 7.4:
                px[x, y] = outline
    # Two spiral arms, half a turn apart, winding in over ~1.5 turns.
    for offset, colour in ((0.0, arm), (math.pi, foam)):
        steps = 60
        for i in range(steps):
            t = i / (steps - 1)
            ang = offset + t * math.pi * 3.0
            r = 6.0 - t * 4.8
            x = int(round(cx + r * math.cos(ang)))
            y = int(round(cy + r * math.sin(ang)))
            if 0 <= x < 16 and 0 <= y < 16 and px[x, y] == dark:
                px[x, y] = colour
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = eye
    return img


def render(grid, palette):
    assert len(grid) == 16, f"grid has {len(grid)} rows"
    img = Image.new("RGBA", (16, 16), T)
    px = img.load()
    for y, row in enumerate(grid):
        assert len(row) == 16, f"row {y} has {len(row)} chars: {row!r}"
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            px[x, y] = c(palette[ch])
    return img


def main():
    os.makedirs(ITEM_DIR, exist_ok=True)
    os.makedirs(HUD_DIR, exist_ok=True)
    out = {}

    for element, tiers in SHARD_PALETTES.items():
        for tier, palette in tiers.items():
            grid = SHARD_T1 if tier == 1 else SHARD_T2
            name = f"{element}_shard_tier{tier}"
            img = render(grid, palette)
            img.save(os.path.join(ITEM_DIR, name + ".png"))
            out[name] = img

    for name, (palette, grid) in ICONS.items():
        img = render(grid, palette)
        img.save(os.path.join(HUD_DIR, name + ".png"))
        out[name] = img

    maelstrom = draw_maelstrom()
    maelstrom.save(os.path.join(HUD_DIR, "icon_maelstrom.png"))
    out["icon_maelstrom"] = maelstrom

    # Contact sheet for review: 10x upscale, dark checker background.
    scale, pad, cols = 10, 12, 5
    cell = 16 * scale + pad * 2 + 14
    rows = (len(out) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), c("#20242c"))
    for i, (name, img) in enumerate(out.items()):
        gx, gy = (i % cols) * cell, (i // cols) * cell
        tile = Image.new("RGBA", (16 * scale, 16 * scale), c("#2a2f3a"))
        for ty in range(8):
            for tx in range(8):
                if (tx + ty) % 2 == 0:
                    continue
                for yy in range(2 * scale):
                    for xx in range(2 * scale):
                        tile.putpixel((tx * 2 * scale + xx, ty * 2 * scale + yy),
                                      c("#323845"))
        big = img.resize((16 * scale, 16 * scale), Image.NEAREST)
        tile.alpha_composite(big)
        sheet.alpha_composite(tile, (gx + pad, gy + pad))
        from PIL import ImageDraw
        ImageDraw.Draw(sheet).text((gx + pad, gy + pad + 16 * scale + 2),
                                   name, fill=c("#c8cdd8"))
    sheet_path = os.path.join(HERE, "contact_sheet.png")
    sheet.save(sheet_path)
    print(f"wrote {len(out)} textures + {sheet_path}")


if __name__ == "__main__":
    main()
