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
    "earth": {"D": "#0d1c06", "Od": "#1e3d10", "Ol": "#94e05c", "B": "#4fae2a",
              "M": "#398420", "L": "#94e05c", "H": "#e0fab6"},
    "water": {"D": "#051535", "Od": "#0e3070", "Ol": "#6fd8f2", "B": "#2f95ea",
              "M": "#2270c2", "L": "#6fd8f2", "H": "#dcf8fe"},
    "fire":  {"D": "#2e0802", "Od": "#671807", "Ol": "#ffa844", "B": "#f0681a",
              "M": "#c24310", "L": "#ffa844", "H": "#ffecb4"},
    "air":   {"D": "#38465c", "Od": "#5d7189", "Ol": "#f4faff", "B": "#d8e7f6",
              "M": "#b5cde5", "L": "#eef6fe", "H": "#ffffff"},
}

# Reference colourway (the art the gem was transcribed from), used only
# for eyeballing against the original — not shipped as an element.
GEM_REFERENCE = {"D": "#1c0c33", "Od": "#371a63", "Ol": "#b678f2", "B": "#8a30e8",
                 "M": "#6f2ad0", "L": "#b678f2", "H": "#e8d3fc"}


def draw_gem(pal):
    """32x32 transcription of the reference gem.

    A diagonal band running bottom-left -> top-right (s = x+y across the
    band, d = x-y along it), flat 45-degree cut faces at both ends, a
    dark outline with a lit bevel inside the upper-left edge, a pale
    highlight: the top cut face plus a Z sweep (down the bevel, kink
    across the body, tail along the dark edge), a dim panel and echo
    streak at the bottom-left, and a darkest drop edge outside the
    lower-right for thickness.
    """
    pal = {k: c(v) for k, v in pal.items()}
    N = 32
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = img.load()

    def s(x, y):
        return x + y

    def d(x, y):
        return x - y

    cells = [(x, y) for y in range(N) for x in range(N)
             if 20 <= s(x, y) <= 34 and -21 <= d(x, y) <= 21]
    outline = {(x, y) for (x, y) in cells
               if s(x, y) in (20, 34) or abs(d(x, y)) == 21}
    interior = [c_ for c_ in cells if c_ not in outline]

    shape = set(cells)
    drop = set()
    for (x, y) in outline:
        if s(x, y) == 34 or d(x, y) == 21 or (d(x, y) == -21 and s(x, y) >= 27):
            for (dx, dy) in ((1, 1), (1, 0), (0, 1)):
                if (x + dx, y + dy) not in shape:
                    drop.add((x + dx, y + dy))

    # big pale patch across the whole top end, stopping short of the dark
    # side; long sweep hugging the lit edge past the midpoint, kinking
    # across to the dark edge with a short tail; long echo line fencing
    # off the dim bottom-left end
    face = {c_ for c_ in interior if d(*c_) >= 13 and s(*c_) <= 29}
    bevel = {c_ for c_ in interior if s(*c_) in (21, 22) and d(*c_) < 13}
    leg = [(17, 5), (16, 6), (15, 7), (14, 8), (13, 9),
           (12, 10), (11, 11), (10, 12), (9, 13)]
    kink = [(10, 14), (11, 15), (12, 16), (13, 17)]
    tail = [(13, 18), (12, 19), (11, 20)]
    sweep = set(leg + kink + tail)
    echo = {(2, 20), (3, 21), (4, 22), (5, 23), (6, 24)}

    for (x, y) in drop:
        if 0 <= x < N and 0 <= y < N:
            px[x, y] = pal["D"]
    for (x, y) in outline:
        px[x, y] = pal["Od"]
    for (x, y) in interior:
        if (x, y) in face or (x, y) in sweep:
            px[x, y] = pal["H"]
        elif (x, y) in bevel:
            px[x, y] = pal["Ol"]
        elif (x, y) in echo:
            px[x, y] = pal["L"]
        elif d(x, y) <= -12:
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
        if img.size == (16, 16):
            big = img.resize((tile_px, tile_px), Image.NEAREST)
            tile.alpha_composite(big)
        else:
            big = img.resize((128, 128), Image.NEAREST)
            tile.alpha_composite(big, (16, 16))
        sheet.alpha_composite(tile, (gx + pad, gy + pad))
        draw.text((gx + pad, gy + pad + tile_px + 2), name, fill=c("#c8cdd8"))
    sheet_path = os.path.join(HERE, "contact_sheet.png")
    sheet.save(sheet_path)
    print(f"wrote {len(out)} textures + {sheet_path}")


if __name__ == "__main__":
    main()
