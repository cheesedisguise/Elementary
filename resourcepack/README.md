# Elementary resource pack

All textures are generated — never hand-edit a PNG. Edit the text-art grids or
palettes in `gen_textures.py` and re-run it:

```
python3 gen_textures.py
```

## Contents

- `assets/elementary/textures/item/` — 8 shard textures: 4 elements × 2 tiers.
  Tier 1 is the raw shard cluster, tier 2 the cut gem (see DESIGN.md §6).
- `assets/elementary/textures/hud/` — 12 ability icons for the HUD
  (DESIGN.md §3). These become `elementary:hud` font glyphs when the
  Option B font HUD is built; until then they are plain textures.
- `assets/minecraft/items/amethyst_shard.json` — 1.21.4+ item model
  definition: `range_dispatch` on `custom_model_data` mapping
  1001/1002 (earth), 1011/1012 (water), 1021/1022 (fire), 1031/1032 (air)
  to the elementary models, falling back to the vanilla shard.
- `contact_sheet.png` — labelled preview of every texture, regenerated on
  each run.

## pack.mcmeta

Resource pack format **75** = Java 1.21.11 (verified against the
[pack format table](https://minecraft.wiki/w/Pack_format)). Since 1.21.9 the
metadata uses `min_format`/`max_format`; the legacy `pack_format` field is
kept alongside for older tooling. Bump these when the server updates.

## Still missing (build order step 14+)

- Advancement icons need no files — they reuse the tier 2 shard models via
  custom model data in the advancement JSON.
- The `elementary:hud` font (glyph mapping + negative-space glyphs) comes
  with the Option B HUD, build order step 15.
