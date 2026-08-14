#!/usr/bin/env python3
"""Generates every Elementary texture.

- Tier 1 shards: the vanilla amethyst shard texture (vanilla/amethyst_shard.png)
  recoloured per element with a luminance gradient map — flat, vanilla-style
  facet shading, no painted gradients.
- Tier 2: the tier 1 shard wrapped in a glow outline of a lighter tone
  of its own colour (the "glowing" effect baked into the texture).
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
    # Ice (aqudr always spawns with it): icier and lighter than Water
    "ice":    ["#134a80", "#3fb2e8", "#8ce4f6", "#f2fdff"],
    # Light: pale creamy yellow, softer than Lightning
    "light":  ["#8a6d1a", "#eed060", "#fff3a8", "#fffdf0"],
    # Shadow: super dark red, embers in the dark
    "shadow": ["#1c0409", "#4d0f18", "#8f2030", "#d05868"],
    # Lightning: vivid electric yellow
    "lightning": ["#6b5200", "#ecc61a", "#ffee5a", "#ffffd8"],
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
# Tier 2 is the tier 1 shard itself, surrounded by a glow outline in a
# lighter tone of its own colour - the vanilla "glowing" effect look,
# baked into the texture. Drawn on a 2x canvas so the outline is exactly
# one art pixel thick and the shard renders the same size in the slot.
GLOW_COLORS = {
    "earth":  "#c8f096",
    "water":  "#a8ecf8",
    "fire":   "#ffcf70",
    "air":    "#ffffff",
    "ice":    "#d6f6ff",
    "light":  "#fffce0",
    "shadow": "#c84a58",
    "lightning": "#fff26a",
}


def glow_wrap(t1, glow_hex):
    """Upscale the shard 2x and trace a 2px glow outline around it."""
    big = t1.convert("RGBA").resize((t1.width * 2, t1.height * 2), Image.NEAREST)
    out = big.copy()
    px_in = big.load()
    px_out = out.load()
    glow = c(glow_hex)
    w, h = big.size
    reach = 2  # one art pixel at the doubled scale

    def opaque_near(x, y):
        for dy in range(-reach, reach + 1):
            for dx in range(-reach, reach + 1):
                if dx == 0 and dy == 0:
                    continue
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px_in[nx, ny][3] > 0:
                    return True
        return False

    for y in range(h):
        for x in range(w):
            if px_in[x, y][3] == 0 and opaque_near(x, y):
                px_out[x, y] = glow
    return out


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
    "icon_flash": ("beams-aura", "Lorc"),
    "icon_sunspear": ("sunbeams", "Lorc"),
    "icon_solar_flare": ("sun", "Lorc"),
    "icon_shadowstep": ("teleport", "Lorc"),
    "icon_grasp": ("shadow-grasp", "Lorc"),
    "icon_eclipse": ("eclipse-flare", "Lorc"),
    "icon_arc": ("lightning-arc", "Lorc"),
    "icon_chain_lightning": ("chain-lightning", "Willdabeast"),
    "icon_supercell": ("heavy-lightning", "Lorc"),
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
    tier1 = {}
    for element, ramp in T1_RAMPS.items():
        name = f"{element}_shard_tier1"
        img = gradient_map(vanilla, ramp)
        img.save(os.path.join(ITEM_DIR, name + ".png"))
        out[name] = img
        tier1[element] = img

    for element, glow in GLOW_COLORS.items():
        name = f"{element}_shard_tier2"
        img = glow_wrap(tier1[element], glow)
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
