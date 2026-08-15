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
    "icon_fissure": ("quake-stomp", "Lorc"),
    "icon_bulwark": ("stone-wall", "Delapouite"),
    "icon_cataclysm": ("spiky-explosion", "Lorc"),
    "icon_tide_pull": ("fishing-hook", "Lorc"),
    "icon_healing_spring": ("waterfall", "Delapouite"),
    "icon_maelstrom": ("ink-swirl", "Lorc"),
    "icon_fireball": ("fireball", "Lorc"),
    "icon_pyre": ("fire-ring", "Lorc"),
    "icon_meteor": ("burning-meteor", "Lorc"),
    "icon_updraft": ("eruption", "Lorc"),
    "icon_gale": ("wind-slap", "Lorc"),
    "icon_tempest": ("tornado", "Lorc"),
    "icon_frozen_over": ("frozen-ring", "Delapouite"),
    "icon_orbital_ice": ("frozen-orb", "Lorc"),
    "icon_subzero": ("frozen-body", "Delapouite"),
    "icon_consecrate": ("beams-aura", "Lorc"),
    "icon_sunspear": ("sunbeams", "Lorc"),
    "icon_solar_flare": ("sun", "Lorc"),
    "icon_shadowstep": ("teleport", "Lorc"),
    "icon_mark_for_death": ("human-target", "Delapouite"),
    "icon_eclipse": ("eclipse-flare", "Lorc"),
    "icon_volt_rush": ("sonic-lightning", "Lorc"),
    "icon_overcharge": ("lightning-slashes", "Lorc"),
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
    img = Image.open(io.BytesIO(png)).convert("RGBA")
    img.load()[ICON_SIZE - 1, ICON_SIZE - 1] = (255, 255, 255, 1)
    return img


# ------------------------------------------------------------------- font
# The elementary:hud font powers the action-bar HUD: every ability icon
# on a private-use codepoint, ASCII digits redrawn tall so cooldown
# numbers sit ABOVE the icons, and space glyphs for pixel positioning.
# Icon codepoints, in ICONS order starting at U+E000 - keep HudFont.java
# in the plugin in sync with this list.
DIGITS = {
    "0": ["01110","10001","10011","10101","11001","10001","01110"],
    "1": ["00100","01100","00100","00100","00100","00100","01110"],
    "2": ["01110","10001","00001","00010","00100","01000","11111"],
    "3": ["11111","00010","00100","00010","00001","10001","01110"],
    "4": ["00010","00110","01010","10010","11111","00010","00010"],
    "5": ["11111","10000","11110","00001","00001","10001","01110"],
    "6": ["00110","01000","10000","11110","10001","10001","01110"],
    "7": ["11111","00001","00010","00100","01000","01000","01000"],
    "8": ["01110","10001","10001","01110","10001","10001","01110"],
    "9": ["01110","10001","10001","01111","00001","00010","01100"],
}


def write_digits():
    """60x18 atlas: ten 6x18 cells, digit pixels in the top 7 rows so a
    17-ascent glyph floats the number above a 7-ascent icon."""
    img = Image.new("RGBA", (60, 18), (0, 0, 0, 0))
    px = img.load()
    for i, ch in enumerate("0123456789"):
        cell = i * 6
        for y, row in enumerate(DIGITS[ch]):
            for x, bit in enumerate(row):
                if bit == "1":
                    px[cell + x, y] = (255, 255, 255, 255)
        # near-invisible pixel pins every advance to a uniform 6px
        px[cell + 4, 17] = (255, 255, 255, 1)
    img.save(os.path.join(HUD_DIR, "digits.png"))


def write_font():
    import json
    advances = {}
    for i, width in enumerate([1, 2, 4, 8, 16, 32]):
        advances[chr(0xE100 + i)] = -width
    for i, width in enumerate([1, 2, 4, 8, 16]):
        advances[chr(0xE108 + i)] = width
    providers = [
        {"type": "space", "advances": advances},
        {"type": "bitmap", "file": "elementary:hud/digits.png",
         "height": 18, "ascent": 17, "chars": ["0123456789"]},
    ]
    for i, name in enumerate(ICONS):
        providers.append({"type": "bitmap",
                          "file": f"elementary:hud/{name}.png",
                          "height": 16, "ascent": 7,
                          "chars": [chr(0xE000 + i)]})
    font_dir = os.path.join(HERE, "assets", "elementary", "font")
    os.makedirs(font_dir, exist_ok=True)
    with open(os.path.join(font_dir, "hud.json"), "w") as f:
        json.dump({"providers": providers}, f, indent=2)


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
    write_digits()
    write_font()

    sheet_path = os.path.join(HERE, "contact_sheet.png")
    sheet.save(sheet_path)
    print(f"wrote {len(out)} textures + digits + font + {sheet_path}")


if __name__ == "__main__":
    main()
