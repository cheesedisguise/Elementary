# Elementary plugin

Paper 1.21.11 plugin implementing DESIGN.md. Build:

```
cd plugin && gradle build
# -> build/libs/elementary-0.1.0.jar
```

Install the jar in `plugins/`, serve the `resourcepack/` folder (zipped)
as the server resource pack, and set aqudr's real UUID under
`bound-players` in `plugins/Elementary/config.yml`.

**Optional:** install the [PacketEvents](https://modrinth.com/plugin/packetevents)
plugin. With it, Mirage clones are packet-level fake players wearing
aqudr's actual skin; without it they fall back to armour-stand
mannequins wearing his head and gear.

Commands: `/info` (own shard, progress, cooldowns; `/info toggle` for
ability messages), `/broker <player> <shard>` (admin: set anyone's
element), `/elementary set <player> <element> <1|2>` and
`/elementary reload` (admin).

Passives are always on and the HUD is always visible — it draws ability
icons + cooldowns above the hotbar no matter what either hand holds
(needs the pack; `hud-style: auto` gates the glyphs on pack acceptance,
`bossbar` is the no-pack fallback). Casting works with the shard in
either hand: RMB = ability 1, sneak+LMB = ability 2,
sneak+RMB = ultimate (Tier 2). Orbital Ice pellets fire on plain left
click while the ring is up.
