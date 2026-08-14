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
    # player-bound Aqua (aqudr): icier and lighter than Water
    "aqua":  ["#134a80", "#3fb2e8", "#8ce4f6", "#f2fdff"],
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
              "M": "#398420", "S": "#3d8c20", "L": "#94e05c",
              "H": "#e0fab6", "W": "#f2fdda"},
    "water": {"D": "#051535", "Od": "#0e3070", "Ol": "#4babf2", "B": "#2f95ea",
              "M": "#2270c2", "S": "#2472c4", "L": "#6fd8f2",
              "H": "#dcf8fe", "W": "#f0fcff"},
    "fire":  {"D": "#2e0802", "Od": "#671807", "Ol": "#fa8132", "B": "#f0681a",
              "M": "#c24310", "S": "#c8480f", "L": "#ffa844",
              "H": "#ffecb4", "W": "#fff8da"},
    "air":   {"D": "#38465c", "Od": "#5d7189", "Ol": "#e8f2fc", "B": "#d8e7f6",
              "M": "#b5cde5", "S": "#b9cfe6", "L": "#eef6fe",
              "H": "#ffffff", "W": "#ffffff"},
    # Aqua: the Water gem with a light blue bottom — the dim panel goes
    # pale while the shadow rim stays deep like Water's
    "aqua":  {"D": "#051535", "Od": "#0e3070", "Ol": "#4babf2", "B": "#2f95ea",
              "M": "#a6e6fa", "S": "#2472c4", "L": "#c8f2fd",
              "H": "#dcf8fe", "W": "#f0fcff"},
}

# Reference colourway (the art the gem was transcribed from), used only
# for eyeballing against the original — not shipped as an element.
GEM_REFERENCE = {"D": "#1c0c33", "Od": "#371a63", "Ol": "#9f52f0", "B": "#8a30e8",
                 "M": "#6f2ad0", "S": "#6b28c8", "L": "#b678f2",
                 "H": "#e8d3fc", "W": "#f6ecff"}


def draw_gem(pal):
    """24x24 cut gem, turned and shaded like the vanilla amethyst shard.

    Same diagonal as the vanilla shard (tip up-right): flat top cap,
    short vertical right side, staircase down the lower-right, flat
    bottom cap, vertical left side, staircase back up. Shading follows
    the vanilla texture's language: a full dark outline (no drop
    shadow), a bright top cut face with sparkle pixels, a thin lit line
    inside the upper-left edge with a short highlight streak trailing
    off the face, a flat body, a darker band along the lower-right, and
    a dim region at the bottom end (light blue on Aqua).
    """
    pal = {k: c(v) for k, v in pal.items()}
    N = 24
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = img.load()

    top = [(x, 1) for x in range(14, 19)]
    right = [(18, y) for y in range(2, 6)]
    lr_stairs = [(17, 6), (16, 7), (15, 8), (14, 9), (13, 10),
                 (12, 11), (11, 12), (10, 13), (9, 14)]
    bottom = [(x, 15) for x in range(5, 10)]
    left = [(5, y) for y in range(11, 15)]
    ul_stairs = [(6, 10), (7, 9), (8, 8), (9, 7), (10, 6),
                 (11, 5), (12, 4), (13, 3), (14, 2)]
    outline = set(top + right + lr_stairs + bottom + left + ul_stairs)

    interior, queue = set(), [(14, 4)]
    while queue:
        (x, y) = queue.pop()
        if (x, y) in interior or (x, y) in outline or not (0 <= x < N and 0 <= y < N):
            continue
        interior.add((x, y))
        queue += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]

    # bright top cut face with the vanilla-style sparkle pixels
    face = {(x, y) for (x, y) in interior if y <= 3}
    sparkle = {(16, 2)}
    cream = {(15, 3)}
    # thin lit line inside the upper-left staircase, and a short
    # highlight streak trailing down off the face like the vanilla shard
    lit = {(x + 1, y) for (x, y) in ul_stairs} - face
    streak = {(13, 4), (12, 5), (11, 6)}
    # darker band hugging the lower-right staircase and right wall
    shade = ({(x - 1, y) for (x, y) in lr_stairs}
             | {(17, y) for y in range(2, 6)}) & interior
    # dim bottom end (Aqua turns this light blue)
    dim = {(x, y) for (x, y) in interior if y >= 12 and x <= 8}

    for (x, y) in outline:
        px[x, y] = pal["Od"]
    for (x, y) in interior:
        if (x, y) in sparkle:
            px[x, y] = pal["W"]
        elif (x, y) in cream:
            px[x, y] = (255, 253, 213, 255)  # the vanilla shard's warm glint
        elif (x, y) in face:
            px[x, y] = pal["H"]
        elif (x, y) in streak:
            px[x, y] = pal["L"]
        elif (x, y) in lit:
            px[x, y] = pal["Ol"]
        elif (x, y) in dim:
            px[x, y] = pal["M"]
        elif (x, y) in shade:
            px[x, y] = pal["S"]
        else:
            px[x, y] = pal["B"]
    # content spans x 5..18, y 1..15 - recentre on the canvas
    centered = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    centered.alpha_composite(img, (0, 3))
    return centered


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
    "icon_mirage": ("shadow-follower", "Lorc"),
    "icon_orbital_ice": ("frozen-orb", "Lorc"),
    "icon_subzero": ("frozen-body", "Delapouite"),
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
