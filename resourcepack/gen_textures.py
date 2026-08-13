#!/usr/bin/env python3
"""Generates every Elementary texture.

- Tier 1 shards: the vanilla amethyst shard texture (vanilla/amethyst_shard.png)
  recoloured per element with a luminance gradient map — flat, vanilla-style
  facet shading, no painted gradients.
- Tier 2 gems: a diagonal cut gem drawn to match the reference art (bright
  angular highlight, dark drop edge), recoloured per element.
- Ability icons: game-icons.net glyphs (icons_svg/, CC BY 3.0 — see README)
  rendered white at 64x64 so the HUD and GUIs can tint them freely.

Deps: pillow, cairosvg.  Run from resourcepack/:  python3 gen_textures.py
Outputs into assets/elementary/textures/{item,hud}/ plus a labelled
contact sheet for review.
"""
import io
import os
import re

import cairosvg
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "assets", "elementary", "textures", "item")
HUD_DIR = os.path.join(HERE, "assets", "elementary", "textures", "hud")


def c(hexstr, a=255):
    hexstr = hexstr.lstrip("#")
    return (int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16), a)


# ------------------------------------------------------------------ tier 1
# Luminance gradient-map ramps, dark -> light. Each flat colour of the
# vanilla texture maps to one flat colour here: facets stay crisp.
T1_RAMPS = {
    "earth": ["#20340f", "#4c9c2e", "#a4dd6e", "#e2f7c0"],
    "water": ["#0b2a6e", "#2a7fd6", "#5fd4ee", "#d8f8fc"],
    "fire":  ["#57100a", "#c93c14", "#f5952f", "#ffe9ad"],
    "air":   ["#54677f", "#a7c4de", "#e2eefa", "#ffffff"],
}


def luminance(rgba):
    r, g, b = rgba[0], rgba[1], rgba[2]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ramp_sample(stops, t):
    """t in [0,1] across evenly spaced hex stops, linear RGB interpolation."""
    stops = [c(s) for s in stops]
    if t <= 0:
        return stops[0]
    if t >= 1:
        return stops[-1]
    seg = t * (len(stops) - 1)
    i = int(seg)
    f = seg - i
    a, b = stops[i], stops[i + 1]
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def gradient_map(img, stops):
    src = img.convert("RGBA")
    opaque = {p for p in src.getdata() if p[3] > 0}
    lums = {p: luminance(p) for p in opaque}
    lo, hi = min(lums.values()), max(lums.values())
    table = {p: ramp_sample(stops, (l - lo) / (hi - lo)) for p, l in lums.items()}
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    out.putdata([table[p] if p[3] > 0 else (0, 0, 0, 0) for p in src.getdata()])
    return out


# ------------------------------------------------------------------ tier 2
# Diagonal cut gem, transcribed from the reference art: an elongated
# hexagon running bottom-left -> top-right with flat-cut ends. The 3D
# read comes from asymmetric edges — the upper-left edge is lit (Ol),
# the lower-right edge is dark (Od) with a darker drop edge (D) outside
# it for thickness — plus a bold pale Z-shaped highlight (H).
# Roles: D drop edge, Od dark edge, Ol lit edge, B body, M dim panel,
# L soft light, H bright highlight.
GEM_PALETTES = {
    "earth": {"D": "#0d1c06", "Od": "#1e3d10", "Ol": "#62c53a", "B": "#4fae2a",
              "M": "#398420", "L": "#94e05c", "H": "#e0fab6"},
    "water": {"D": "#051535", "Od": "#0e3070", "Ol": "#4babf2", "B": "#2f95ea",
              "M": "#2270c2", "L": "#6fd8f2", "H": "#dcf8fe"},
    "fire":  {"D": "#2e0802", "Od": "#671807", "Ol": "#fa8132", "B": "#f0681a",
              "M": "#c24310", "L": "#ffa844", "H": "#ffecb4"},
    "air":   {"D": "#38465c", "Od": "#5d7189", "Ol": "#e8f2fc", "B": "#d8e7f6",
              "M": "#b5cde5", "L": "#eef6fe", "H": "#ffffff"},
}

# Reference colourway (the art the gem was transcribed from), used only
# for eyeballing against the original — not shipped as an element.
GEM_REFERENCE = {"D": "#1c0c33", "Od": "#371a63", "Ol": "#9f52f0", "B": "#8a30e8",
                 "M": "#6f2ad0", "L": "#b678f2", "H": "#e8d3fc"}


def draw_gem(pal):
    """24x24 transcription of the reference gem.

    The silhouette is a sheared bar: flat horizontal top, straight
    vertical right side, a 1:1 staircase down the lower-right, flat
    bottom, vertical left side, and a staircase back up the upper-left.
    Inside: a big pale arch across the top end (with a V notch of body),
    a pale Z sweep down the lit side that kinks across to the dark edge,
    a lit bevel inside the upper-left/left/bottom rim, an echo streak
    fencing off the dim bottom-left panel, and a darkest drop edge
    outside the right/lower-right/bottom for thickness.
    """
    pal = {k: c(v) for k, v in pal.items()}
    N = 24
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = img.load()

    top = [(x, 1) for x in range(11, 20)]
    right = [(19, y) for y in range(2, 10)]
    lr_stairs = [(18, 10), (17, 11), (16, 12), (15, 13),
                 (14, 14), (13, 15), (12, 16), (11, 17)]
    bottom = [(x, 18) for x in range(2, 11)]
    left = [(2, y) for y in range(10, 18)]
    ul_stairs = [(10, 2), (9, 3), (8, 4), (7, 5),
                 (6, 6), (5, 7), (4, 8), (3, 9)]
    outline = set(top + right + lr_stairs + bottom + left + ul_stairs)

    # interior = flood fill from the centre; 1:1 stairs are tight for a
    # 4-connected fill, so no leaks
    interior, queue = set(), [(11, 9)]
    while queue:
        (x, y) = queue.pop()
        if (x, y) in interior or (x, y) in outline or not (0 <= x < N and 0 <= y < N):
            continue
        interior.add((x, y))
        queue += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    shape = outline | interior

    drop = set()
    for (x, y) in right + lr_stairs + bottom + [(19, 1)]:
        for (dx, dy) in ((1, 0), (0, 1), (1, 1)):
            if (x + dx, y + dy) not in shape:
                drop.add((x + dx, y + dy))

    # pale arch across the top end: two full rows, then legs down each
    # side leaving a V notch of body in the middle
    face = ({(x, 2) for x in range(11, 19)}
            | {(x, 3) for x in range(11, 19) if x != 14}
            | {(11, 4), (12, 4), (11, 5), (12, 5), (17, 4), (18, 4), (18, 5)})
    # Z sweep: pale 1px line floating ~2 cells inside the lit edge,
    # kinking at mid-height into one crossing stroke that lands on the
    # dark staircase, with a short tail riding up it
    leg = [(10, 5), (9, 6), (8, 7), (7, 8), (7, 9), (6, 10), (6, 11), (6, 12)]
    crossing = [(7, 13), (8, 14), (9, 15), (10, 16), (11, 16)]
    sweep = set(leg + crossing)
    # lit facet: the filled strip between the upper-left staircase/wall
    # and the leg, plus a 1px rim inside the wall and bottom edge
    strip_rows = {3: (10, 10), 4: (9, 10), 5: (8, 9), 6: (7, 8), 7: (6, 7),
                  8: (5, 6), 9: (4, 6), 10: (3, 5), 11: (3, 5), 12: (3, 5)}
    bevel = ({(x, y) for y, (a, b) in strip_rows.items() for x in range(a, b + 1)}
             | {(3, y) for y in range(13, 18)} | {(x, 17) for x in range(4, 11)})
    # small pale accent where the dim panel meets the kink
    echo = {(4, 13), (5, 14)}
    # dim panel pinned under the crossing stroke
    dim = {(x, y) for y in range(13, 17) for x in range(4, 10) if x < y - 6}

    for (x, y) in drop:
        if 0 <= x < N and 0 <= y < N:
            px[x, y] = pal["D"]
    for (x, y) in outline:
        px[x, y] = pal["Od"]
    for (x, y) in interior:
        if (x, y) in face or (x, y) in sweep:
            px[x, y] = pal["H"]
        elif (x, y) in echo:
            px[x, y] = pal["L"]
        elif (x, y) in bevel:
            px[x, y] = pal["Ol"]
        elif (x, y) in dim:
            px[x, y] = pal["M"]
        else:
            px[x, y] = pal["B"]
    return img


# ------------------------------------------------------------------- icons
# game-icons.net glyphs (CC BY 3.0). Keys are our texture names; values are
# the icon name in icons_svg/ and its author for the credits list.
ICONS = {
    "icon_tremor": ("quake-stomp", "Lorc"),
    "icon_bulwark": ("stone-wall", "Delapouite"),
    "icon_cataclysm": ("spiky-explosion", "Lorc"),
    "icon_tide_pull": ("fishing-hook", "Lorc"),
    "icon_thunderstorm": ("lightning-storm", "Lorc"),
    "icon_maelstrom": ("ink-swirl", "Lorc"),
    "icon_fireball": ("fireball", "Lorc"),
    "icon_pyre": ("fire-ring", "Lorc"),
    "icon_meteor": ("burning-meteor", "Lorc"),
    "icon_updraft": ("eruption", "Lorc"),
    "icon_gale": ("wind-slap", "Lorc"),
    "icon_tempest": ("tornado", "Lorc"),
}

ICON_SIZE = 64


def render_icon(svg_name):
    """White glyph on transparency — tintable by the HUD at runtime."""
    path = os.path.join(HERE, "icons_svg", svg_name + ".svg")
    svg = open(path).read()
    svg = re.sub(r'<path fill="#fff" d="M0 0h512v512H0z"/>', "", svg, count=1)
    svg = svg.replace('fill="#000"', 'fill="#ffffff"')
    png = cairosvg.svg2png(bytestring=svg.encode(),
                           output_width=ICON_SIZE, output_height=ICON_SIZE)
    return Image.open(io.BytesIO(png)).convert("RGBA")


# ------------------------------------------------------------------- main
def main():
    os.makedirs(ITEM_DIR, exist_ok=True)
    os.makedirs(HUD_DIR, exist_ok=True)
    out = {}

    vanilla = Image.open(os.path.join(HERE, "vanilla", "amethyst_shard.png"))
    for element, ramp in T1_RAMPS.items():
        name = f"{element}_shard_tier1"
        img = gradient_map(vanilla, ramp)
        img.save(os.path.join(ITEM_DIR, name + ".png"))
        out[name] = img

    for element, pal in GEM_PALETTES.items():
        name = f"{element}_shard_tier2"
        img = draw_gem(pal)
        img.save(os.path.join(ITEM_DIR, name + ".png"))
        out[name] = img

    for name, (svg_name, _author) in ICONS.items():
        img = render_icon(svg_name)
        img.save(os.path.join(HUD_DIR, name + ".png"))
        out[name] = img

    # Contact sheet for review: shards at 10x, icons at 2x.
    pad, cols, tile_px = 12, 5, 160
    cell = tile_px + pad * 2 + 14
    rows = (len(out) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), c("#20242c"))
    draw = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(out.items()):
        gx, gy = (i % cols) * cell, (i // cols) * cell
        tile = Image.new("RGBA", (tile_px, tile_px), c("#2a2f3a"))
        tdraw = ImageDraw.Draw(tile)
        for ty in range(0, tile_px, 20):
            for tx in range(0, tile_px, 20):
                if (tx + ty) // 20 % 2:
                    tdraw.rectangle([tx, ty, tx + 19, ty + 19], fill=c("#323845"))
        k = max(1, tile_px // img.width)
        big = img.resize((img.width * k, img.height * k), Image.NEAREST)
        off = (tile_px - big.width) // 2
        tile.alpha_composite(big, (off, off))
        sheet.alpha_composite(tile, (gx + pad, gy + pad))
        draw.text((gx + pad, gy + pad + tile_px + 2), name, fill=c("#c8cdd8"))
    sheet_path = os.path.join(HERE, "contact_sheet.png")
    sheet.save(sheet_path)
    print(f"wrote {len(out)} textures + {sheet_path}")


if __name__ == "__main__":
    main()
