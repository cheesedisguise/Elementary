# Elementary resource pack

All textures are generated — never hand-edit a PNG. Edit the palettes,
geometry, or icon mapping in `gen_textures.py` and re-run it:

```
pip install pillow cairosvg   # once
python3 gen_textures.py
```

## Contents

- `assets/elementary/textures/item/` — 16 shard textures: 8 elements × 2 tiers.
  Tier 1 (16×16) is the **vanilla amethyst shard** recoloured per element with
  a luminance gradient map (flat colours, vanilla facet shading). Tier 2
  (32×32) is the tier 1 shard wrapped in a one-art-pixel glow outline of a
  lighter tone of its own colour. See DESIGN.md §6.
- `assets/elementary/textures/hud/` — 24 ability icons + 6 status-effect
  icons (DESIGN.md §3), rendered from `icons_svg/` as white-on-transparent
  64×64 glyphs so the HUD can tint them per element and state. These
  become `elementary:hud` font glyphs (U+E000+, in `ICONS` order).
- `icons_svg/` — the game-icons.net SVG sources for the icons (see
  credits below).
- `vanilla/amethyst_shard.png` — the vanilla 1.21 amethyst shard texture
  (16×16), the recolour base for the tier 1 shards.
- `assets/minecraft/items/amethyst_shard.json` — 1.21.4+ item model
  definition: `range_dispatch` on `custom_model_data` mapping
  1001/1002 (earth), 1011/1012 (water), 1021/1022 (fire), 1031/1032 (air),
  1041/1042 (ice), 1051/1052 (shadow), 1061/1062 (light), 1071/1072
  (lightning) to the elementary models, falling back to the vanilla shard.
- `contact_sheet.png` — labelled preview of every texture, regenerated on
  each run.

## pack.mcmeta

Resource pack format **75** = Java 1.21.11 (verified against the
[pack format table](https://minecraft.wiki/w/Pack_format)). Since 1.21.9 the
metadata uses `min_format`/`max_format`; the legacy `pack_format` field is
kept alongside for older tooling. Bump these when the server updates.

## Credits & licences

**Ability icons** are from [game-icons.net](https://game-icons.net),
licensed [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/):

| Icon | Used for | Author |
|---|---|---|
| `quake-stomp` | Fissure | [Lorc](https://lorcblog.blogspot.com/) |
| `thrown-charcoal` | Boulder | Lorc |
| `spiky-explosion` | Cataclysm | Lorc |
| `fishing-hook` | Tide Pull | Lorc |
| `waterfall` | Healing Spring | Delapouite |
| `ink-swirl` | Maelstrom | Lorc |
| `fireball` | Fireball | Lorc |
| `fire-ring` | Pyre | Lorc |
| `burning-meteor` | Meteor Shower | Lorc |
| `eruption` | Updraft | Lorc |
| `wind-slap` | Gale | Lorc |
| `tornado` | Tempest | Lorc |
| `frozen-ring` | Frozen Over | Delapouite |
| `frozen-orb` | Orbital Ice | Lorc |
| `frozen-body` | Sub-Zero | [Delapouite](https://delapouite.com/) |
| `daggers` | Shade Daggers | Lorc |
| `teleport` | Shadestep | Lorc |
| `evil-moon` | Hunt | Lorc |
| `brainstorm` | Neural Overload | Lorc |
| `sunbeams` | Sunspear | Lorc |
| `sun` | Supernova | Lorc |
| `sonic-lightning` | Volt Dash | Lorc |
| `lightning-frequency` | Emotion Wave | Lorc |
| `power-lightning` | Powerplant | Lorc |
| `beams-aura` | Radiance (status) | Lorc |
| `terror` | Fear (status) | Lorc |
| `star-pupil` | Luminosity (status) | Lorc |
| `yin-yang` | Harmony (status) | Delapouite |
| `brain` | Concussion (status) | Lorc |
| `explosion-rays` | Absolute Radiance (status) | Lorc |

Keep this attribution with the pack when distributing it (the CC BY
licence requires it — a link to this file from the server's pack listing
is enough).

**Tier 1 shard textures** derive from Mojang's amethyst shard texture and
are for use within Minecraft (standard resource-pack practice; not
redistributable outside the game context).

**Tier 2 gem** is original art transcribed from the project's reference
image.

## Still missing (build order step 14+)

- Advancement icons need no files — they reuse the tier 2 shard models via
  custom model data in the advancement JSON.
- The `elementary:hud` font (glyph mapping + negative-space glyphs) comes
  with the Option B HUD, build order step 15.
