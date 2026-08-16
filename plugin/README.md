# Elementary plugin

Paper 1.21.11 plugin implementing DESIGN.md. Build:

```
cd plugin && gradle build
# -> build/libs/elementary-2.0.0.jar
```

Install the jar in `plugins/`, serve the `resourcepack/` folder (zipped)
as the server resource pack, and set aqudr's real UUID under
`bound-players` in `plugins/Elementary/config.yml`. No other plugins
are needed.

Commands: `/info` (own shard, progress, cooldowns; `/info toggle` for
ability messages), `/trust [player]` and `/untrust <player>` (your ally
list: harmful abilities spare allies, support includes them),
`/broker <player> <shard>` (admin: set anyone's element),
`/elementary set <player> <element> <1|2>` and
`/elementary reload` (admin).

Passives are always on and the HUD is always visible — it draws ability
icons + cooldowns above the hotbar wherever the shard sits
(needs the pack; `hud-style: auto` gates the glyphs on pack acceptance,
`bossbar` is the no-pack fallback). Casting needs the shard in the
main hand: RMB = ability 1, sneak+LMB = ability 2,
sneak+RMB = ultimate (Tier 2). Orbital Ice pellets fire on plain left
click while the ring is up.
