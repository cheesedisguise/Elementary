# Elementary — Design Document

> Paper 1.21.11 plugin. Every player is permanently bound to one of four elemental shards, granting a passive and two activatable abilities. Tier 2 is earned through an element-specific challenge or taken from a fallen player, and unlocks an ultimate.

This document is the source of truth. If code and this document disagree, the document wins — update it first, then the code.

---

## 1. Core rules

1. On first join, a player is assigned a **random element** and given their shard.
2. The shard is a **recoloured amethyst shard** with custom model data.
3. The shard is **bound to its owner's UUID**. Nobody else can use it.
4. Passives apply **only while the shard is held in the offhand**.
5. Abilities are on **individual cooldowns**. No shared energy resource.
6. **Shards cannot be dropped, stored, traded, or lost.** They are returned automatically on respawn.
7. A player has exactly one shard at a time.
8. **No ability breaks or permanently places blocks.** Everything is entity-based or reverts.

### Shard binding enforcement

The shard must be impossible to lose. Cancel all of the following when the item is a shard:

- `PlayerDropItemEvent` — cannot be dropped
- `PlayerDeathEvent` — removed from drops, restored on `PlayerRespawnEvent`
- `InventoryClickEvent` / `InventoryDragEvent` — cannot be moved into any container, including chests, ender chests, shulkers, and item frames
- `EntityPickupItemEvent` — no other player can pick one up if one ever escapes
- Death in the void, lava, or on despawn timers — irrelevant, since it never leaves the inventory

On respawn, verify the player still holds their shard. If not, silently re-issue it with their stored element, tier, and progress intact.

---

## 2. Control scheme

| Input | Result |
|---|---|
| Shard in offhand | Passive active |
| Right-click | **Ability 1** |
| Sneak + left-click | **Ability 2** |
| Sneak + right-click | **Ultimate** (Tier 2 only) |
| `/info` | Your shard: element, tier, abilities, controls, challenge progress |
| `/recipes` | Craftable items and their recipes |

---

## 3. HUD

Abilities display as **icons, not names**, with the cooldown shown above each icon.

### The constraint

The action bar is a single line of text. You cannot stack a number above an icon in it. There are two real approaches:

### Option A — BossBars (build this first)

One BossBar per ability slot, stacked vertically at the top of the screen.

- **Title:** the ability's icon glyph, plus remaining seconds when on cooldown
- **Progress:** fills from 0 → 1 as the cooldown recharges, so the bar *is* the timer
- **Colour:** green when ready, red while recharging
- Show only while the shard is in the offhand; hide otherwise

This is maybe 80 lines and works with no resource pack at all (fall back to `◆ ▲ ✦` style symbols until the pack exists).

### Option B — Custom font HUD (the real thing)

The only way to freely position a number above an icon is a **custom font with negative-space glyphs** in the resource pack.

1. Define a font `elementary:hud` in the pack.
2. Map each ability icon texture to a codepoint in the Unicode Private Use Area (`U+E000` onward).
3. Add negative-space glyphs (widths of `-1`, `-2`, `-4`, `-8` px) to shift the cursor backward.
4. Render the icon, shift back with negative space, then draw the cooldown digits offset upward via the glyph's `ascent` value.
5. Send the whole thing as one action-bar component.

You already need a pack for the shard textures, so this is incremental. It is fiddly to align — expect to iterate on `ascent` and `height` values.

**Build Option A now, Option B once the pack exists.** Keep the HUD behind an interface so swapping is a one-file change.

### Icon assignments

| Element | Ability 1 | Ability 2 | Ultimate |
|---|---|---|---|
| Earth | Tremor — cracked ground | Bulwark — shield wall | Cataclysm — spike burst |
| Water | Tide Pull — hook | Thunderstorm — storm cloud | Maelstrom — spiral |
| Fire | Fireball — flame orb | Pyre — flame ring | Meteor — falling rock |
| Air | Updraft — upward arrows | Gale — wind swirl | Tempest — cyclone |

---

## 4. Ability messages

Ability messages are **client-sided** — only the caster sees them. Nothing is broadcast to other players.

**Format:**

```
⟨Element colour⟩ You used Tremor!
```

**Ultimates** keep a bolder personal format and a sound cue, still visible to the caster only:

```
⚡ You unleashed MAELSTROM!
```

**Rules:**

- Sent only to the caster, coloured to match the element (Earth green, Water aqua, Fire red, Air white)
- Nobody else ever receives an ability message — chat stays clean and abilities keep the element of surprise
- No rate limiting needed: a successful cast cannot repeat inside its own cooldown, and failed casts never produce a chat line
- Failed activations (on cooldown, wrong tier) send a private action-bar message only
- Toggleable per player with `/info` → settings, and globally in `config.yml`
- The Tier 2 ascension announcement (section 6) is a separate system and stays server-wide — that one is meant to be public

---

## 5. The four elements

### 🟫 Earth Shard

*Brown-to-green gradient. Zone denial and a shield you can weaponise.*

**Passive — Stoneskin**
- +4 max health (2 extra hearts)
- Haste I while standing on stone, deepslate, or dirt-family blocks
- Fall damage reduced by 50%

**Ability 1 (RMB) — Tremor** · 25s
Ground slam. Enemies within 6 blocks are launched upward, take 4 damage, and get Slowness II for 4s. Does not affect the caster.

**Ability 2 (⇧LMB) — Bulwark** · 35s
Summons a 5-wide × 3-tall stone wall 3 blocks in front of the player. The wall **continuously repositions to face wherever the caster looks**, orbiting them at a fixed 3-block distance. It blocks projectiles and bodies.

**Punch the wall** and it launches forward at ~8 blocks/second for up to 15 blocks, shoving any player or mob it contacts along with it and dealing 3 damage. It dissipates on hitting terrain or reaching max range. Lifetime 12s either way. Recast dismisses early.

---

### 🟦 Water Shard

*Blue-to-cyan gradient. Sustained melee damage. Wants to be in your face.*

**Passive — Tidebound**
- Permanent water breathing and Dolphin's Grace
- Regeneration I while in contact with water or rain
- No mining slowdown underwater, clear vision underwater

**Ability 1 (RMB) — Tide Pull** · 20s
Fires a 15-block hook. Hits a player or mob → yanks them to you. Hits a block → yanks you to it. No damage.

**Ability 2 (⇧LMB) — Thunderstorm** · 40s
A storm cloud forms above the caster and **follows them for 10s**. While active, every melee hit the caster lands calls a lightning strike on the target dealing **2 true damage** (bypasses armour, absorption, and i-frames) on top of normal weapon damage.

- **No internal cooldown per target** — every hit procs
- Lightning is visual only (`strikeLightningEffect`) — no fires, no collateral damage
- Only affects entities the caster hits. No AoE, no friendly fire

> This is the highest sustained damage in the plugin by design. Watch it in playtesting; if it dominates, raise the cooldown to 60s before touching the damage.

---

### 🟥 Fire Shard

*Red-to-orange gradient. Area control. Stand your ground and burn.*

**Passive — Emberheart**
- Full fire and lava immunity
- Melee attacks ignite the target for 3s
- +2 damage dealt while in the Nether

**Ability 1 (RMB) — Fireball** · 20s
Explosive projectile. 6 damage, 1.5-block blast, ignites on hit. **Does not break blocks.**

**Ability 2 (⇧LMB) — Pyre** · 30s
Ignites a 5-block-radius ring of flame at the caster's feet that **stays there for 10s**. It does not follow the caster.

- **Caster inside:** Strength I and Speed I
- **Enemies inside:** 1 damage per second and set alight
- Visual flame only — no real fire blocks, no spread

**Terrain corruption.** For the duration, the floor inside the radius is converted to Nether blocks, weighted:

| Block | Share |
|---|---|
| Netherrack | 60% |
| Basalt | 25% |
| Magma Block | 15% |

Only the **topmost solid block of each column** is converted — raycast down from the caster's Y level, one block per column, no digging into terrain. Every original `BlockData` is stored and **restored exactly when the effect expires.**

**Never convert:** bedrock, obsidian, any block with an inventory or tile entity (chests, furnaces, hoppers, shulkers, spawners, signs, beds), any block with something standing on it that would suffocate, liquids, or air.

Fixed in place, so it's a commitment. Leave it and you lose the buff — but the scorched ground marks the territory for the full ten seconds.

---

### ⬜ Air Shard

*White-to-pale-blue gradient. Vertical control and disengagement.*

**Passive — Windborne**
- Permanent Speed I
- Complete fall damage immunity
- Sneaking in midair grants Slow Falling

**Ability 1 (RMB) — Updraft** · 25s
Launches **all other players and mobs within 6 blocks** 5 blocks straight up. The caster is unaffected and stays grounded.

> Nerfed from 8 blocks / 20s. Five blocks is enough to interrupt, break a combo, and reposition someone, but survivable without armour. Because Air takes zero fall damage and the target does, any height increase scales asymmetrically into a kill button.

**Ability 2 (⇧LMB) — Gale** · 30s
A cone of wind 8 blocks long and 60° wide in the aimed direction:

- Enemies in the cone are knocked back hard, take 2 damage, and get Nausea for 3s
- **The caster is launched in the opposite direction** — a hard recoil away from where they aimed

Air's escape. Aim at a pursuer to push them off and rocket yourself clear in one motion. Aim at the ground to launch upward.

---

## 6. Tier 2

Two routes. Either finish your element's challenge, or take an Upgrader from someone you killed.

### Route 1 — Challenges

Progress is tracked persistently and shown in `/info`, with chat notifications at 25 / 50 / 75 / 100%.

| Element | Challenge |
|---|---|
| **🟫 Earth** | **Unmoved** — absorb 200 total damage without dying. Any death resets to zero. |
| **🟦 Water** | **Deep Current** — swim 1,000 blocks fully submerged without surfacing. Breaking the surface resets the attempt. |
| **🟥 Fire** | **Kindling** — kill 50 entities while they are burning. |
| **⬜ Air** | **Untouched Sky** — remain continuously airborne for 20 seconds using sneak-Slow Falling. Touching any block, including water, resets the timer. |

### Route 2 — The Upgrader

**Tier 2 is lost on death.** When a Tier 2 player dies — by any cause — they drop back to Tier 1, and the lost tier drops where they fell as an **Upgrader**. Anyone can pick it up and right-click it while holding their shard to promote it to Tier 2. It works on any element.

**Tier 1 deaths generate nothing.** There is no tier to lose, so there is nothing to drop. Upgraders are never minted by kills — new ones enter the world only when someone completes a challenge, and from then on they circulate. Every kill of a Tier 2 player is a transfer, not a creation.

**No drop cooldown — none is needed.** An earlier draft had a 24-hour per-victim limiter to stop friends minting Upgraders from nothing by trading deaths. Since a death now costs the victim their own Tier 2, the system is self-limiting: killing the same player twice yields nothing the second time, because the first death already made them Tier 1. Farming a person is impossible by construction. There is no per-victim tracking of any kind.

**Carried Upgraders are also lost on death.** An unused Upgrader in your inventory follows normal item rules — die carrying one and it drops where you fell, alongside the one your own tier just became if you were Tier 2. A single kill can therefore yield **two** Upgraders: the victim's tier, plus a hoarded spare. That's intended — it rewards catching someone who's been hoarding. Carrying an unused Upgrader is a liability, so the correct play is almost always to use it immediately.

**Losing Tier 2 resets challenge progress to zero.** The challenge can be completed again from scratch, so both routes stay open forever.

**On demotion, the shard itself reverts** — the cut gem texture drops back to the raw shard. Since the item name never changes (see below), the texture is the honest signal of what you currently are.

**Dropped Upgrader behaviour:**
- Glows, so it's findable in grass and caves
- Never despawns
- Can still be destroyed by lava, fire, or the void — dying somewhere stupid means the tier leaves the economy entirely

> Knob to consider: if losing Upgraders to lava feels too punishing, make the item fire- and void-immune instead. And if full loss on *any* death feels brutal in playtesting, restrict demotion to player kills only — PvE deaths would then keep your tier and drop nothing. Any-death is the default because it keeps the rule simple and makes recovering your own dropped tier after a stupid death a tense corpse run rather than a free pass.

### Tier 2 upgrades

| Element | Passive | Ability 1 | Ability 2 |
|---|---|---|---|
| **Earth** | +6 max health; Resistance I on stone | Tremor radius 6 → 9, adds 3s root | Bulwark travels 25 blocks, 6 damage |
| **Water** | Regeneration II near water | Tide Pull hits up to 3 targets | Thunderstorm lasts 15s, 3 true damage |
| **Fire** | Nether bonus applies everywhere at +1 | Fireball fires 3 in a spread | Pyre radius 5 → 8, adds Regeneration I to caster |
| **Air** | Speed II | Updraft radius 6 → 9 | Gale cone 8 → 12 blocks, stronger recoil |

### Item appearance across tiers

**The item name never changes.** A shard is called `Earth Shard` at Tier 1 and `Earth Shard` at Tier 2 — no numeral, no "II", no rename. Ascending is signalled entirely by the texture.

| | Tier 1 | Tier 2 |
|---|---|---|
| **Name** | Earth Shard | Earth Shard *(unchanged)* |
| **Shape** | Amethyst shard silhouette | Cut, faceted gem |
| **Colour** | Element gradient | Same gradient, richer and more saturated |

**Tier 1** keeps the raw amethyst shard outline, recoloured per element — brown-to-green for Earth, blue-to-cyan for Water, red-to-orange for Fire, white-to-pale-blue for Air.

**Tier 2** switches to a **cut gem silhouette**: a faceted diamond/teardrop form with a bright core and darker facet edges, in the same colourway. Raw shard becomes polished gem — the progression reads instantly even at 16×16 in a hotbar, which matters a lot given the name gives nothing away.

> Draw these yourself rather than extracting textures from an existing gem plugin. The faceted style is easy to reproduce and you'd be recolouring to your four elements regardless; shipping someone else's PNGs is how projects get pulled.

**Consequences of the fixed name:**
- Tier is only readable from the texture in-world, so make the two silhouettes genuinely distinct — not just a brightness change
- `/info` remains the authoritative place to check your own tier
- The action bar HUD may still show tier (`◆ Earth II`) since that's plugin UI, not the item
- Lore text is optional and configurable, but must not contain the tier if you want ascension to stay visually subtle

**Custom model data allocation:**

| Element | Tier 1 | Tier 2 |
|---|---|---|
| Earth | 1001 | 1002 |
| Water | 1011 | 1012 |
| Fire | 1021 | 1022 |
| Air | 1031 | 1032 |

Leaving gaps between elements means a fifth element slots in without renumbering.

### The Tier 2 advancement

Reaching Tier 2 by **either** route grants a hidden purple advancement, a server-wide sound, and a chat announcement.

**Four advancements, one per element:**

| Element | Advancement | Description |
|---|---|---|
| 🟫 Earth | **Unbreakable** | Ascend to a tier 2 element Shard! |
| 🟦 Water | **Eye of the Storm** | Ascend to a tier 2 element Shard! |
| 🟥 Fire | **Inferno** | Ascend to a tier 2 element Shard! |
| ⬜ Air | **Skybound** | Ascend to a tier 2 element Shard! |

All four share the same description. Only the title and icon differ.

**Advancement JSON** (`data/elementary/advancement/earth_ascension.json`):

```json
{
  "parent": "elementary:root",
  "display": {
    "icon": {
      "id": "minecraft:amethyst_shard",
      "components": { "minecraft:custom_model_data": { "floats": [1002] } }
    },
    "title": { "text": "Unbreakable", "color": "dark_green" },
    "description": { "text": "Ascend to a tier 2 element Shard!" },
    "frame": "challenge",
    "show_toast": true,
    "announce_to_chat": false,
    "hidden": true
  },
  "criteria": {
    "granted": { "trigger": "minecraft:impossible" }
  }
}
```

**Why each field matters:**
- `"frame": "challenge"` — this is the purple one. Ornate magenta border, light-purple chat line, and the `ui.toast.challenge_complete` sound. `goal` and `task` frames are yellow and grey respectively.
- `"hidden": true` — invisible in the advancement tree until earned. Hidden only works on **non-root** advancements, which is why these need a parent.
- `"trigger": "minecraft:impossible"` — nothing in vanilla can ever satisfy it, so the plugin is the only thing that can grant it.

**The root advancement** (`root.json`) creates the tab. It cannot be hidden, so give it `"show_toast": false` and `"announce_to_chat": false`, and grant it silently when a player first receives their shard. The tab will exist but contain nothing visible until someone ascends.

> **Version note:** the datapack folder is `advancement/` (singular) as of 1.21 — the old plural `advancements/` is silently ignored. Also verify the `custom_model_data` component shape against your exact 1.21.11 build; the format changed to an object with `floats` / `strings` / `flags` / `colors` arrays in 1.21.4, and getting it wrong means the icon silently falls back to a plain amethyst shard.

**Registering it.** Runtime advancement registration via `Bukkit.getUnsafe().loadAdvancement()` is deprecated and unreliable. Instead:

1. Bundle the datapack inside the plugin jar under `resources/datapack/`.
2. On first enable, copy it to `<world>/datapacks/elementary/` if not already present.
3. Call `Bukkit.reloadData()` to load it without a restart.
4. Version the pack folder so updates overwrite cleanly, and log clearly if the copy fails.

**Granting it:**

```java
Advancement adv = Bukkit.getAdvancement(
    new NamespacedKey("elementary", "earth_ascension"));
player.getAdvancementProgress(adv).awardCriteria("granted");
```

**On grant, simultaneously:**

- **Global sound** — play `ui.toast.challenge_complete` to every online player at volume 1.0, pitch 1.0. Everyone hears it regardless of distance, so the whole server knows someone ascended.
- **One chat line**, server-wide, exactly one line, nothing else:

```
Steve has made the advancement [Eye of the Storm]
```

**Why this is sent manually rather than by vanilla.** Minecraft picks the announcement wording from the frame type, and they don't line up:

| Frame | Vanilla wording | Frame appearance |
|---|---|---|
| `task` | has made the advancement | plain, grey |
| `goal` | has reached the goal | rounded, grey |
| `challenge` | has completed the challenge | **ornate, purple** |

The purple frame comes bundled with "has completed the challenge". So keep `"frame": "challenge"` for the toast and border, set `"announce_to_chat": false`, and send the line yourself.

**Replicating vanilla's format exactly:**
- Plain white text: `<player> has made the advancement `
- Advancement name wrapped in square brackets, coloured `dark_purple` to match the challenge frame — switch to `light_purple` if it reads too dim against your chat background
- A hover event on the bracketed name, matching vanilla's tooltip layout — the advancement title on the first line, description underneath:

```
Eye of the Storm
Ascend to a tier 2 element Shard!
```

- Sent to every online player, including the earner
- No prefix, no icon, no second line — vanilla advancement messages have none, and adding one breaks the illusion

**The one part that can't be changed per-advancement:** the toast header. A `challenge` frame renders "Challenge Complete!" above the title; `task` renders "Advancement Made!". This string is `advancements.toast.challenge` in the language file, so overriding it in the resource pack changes it for *every* challenge advancement in the game, vanilla ones included. Leave it alone unless that trade is worth it — the purple toast is what players register, not the header text.

```java
Component name = Component.text("[" + title + "]", NamedTextColor.DARK_PURPLE)
    .hoverEvent(HoverEvent.showText(
        Component.text(title, NamedTextColor.DARK_PURPLE)
            .append(Component.newline())
            .append(Component.text(description, NamedTextColor.WHITE))));
Bukkit.broadcast(Component.text(player.getName() + " has made the advancement ")
    .append(name));
```

**Edge cases:**
- Rerolling with a Shard Trader drops you to Tier 1 but **does not revoke the advancement**. Neither does losing Tier 2 on death. It's a record of what you achieved, not current state.
- Re-earning Tier 2 in the same element — after a reroll or a death — won't re-fire; advancements grant once. That's correct behaviour; don't work around it.
- Earning Tier 2 in a *different* element after rerolling fires that element's advancement. Collecting all four is a natural long-term goal.

---

## 7. Ultimates (Tier 2 only, ⇧RMB, 60s cooldown)

### 🟫 Earth — Cataclysm
Stone spikes erupt in a 10-block radius. 8 damage, enemies rooted 3s, caster gains Resistance II for 8s. Display entities, gone after 3s.

### 🟦 Water — Maelstrom
A whirlpool at the caster's position for 6s, 12-block radius. Enemies inside are pulled continuously toward the centre, given Slowness III, and take drowning damage regardless of water.

### 🟥 Fire — Meteor
Call a meteor at the crosshair, up to 30 blocks. 12 damage in a 6-block radius, ignites everything hit, burning ground for 8s. **No terrain damage.**

### ⬜ Air — Tempest
True flight for 8s. Every enemy within 8 blocks is continuously lifted and takes 1 damage per second. Flight ends abruptly — safe, since Air ignores falling.

---

## 8. Items and commands

### Shard Trader
- **Recipe:** 4 Amethyst Shard + 1 Ender Eye + 4 Gold Ingot
- Right-click to reroll into a *different* random element at Tier 1. Consumed on use.
- **Rerolling wipes Tier 2 and all challenge progress.**

### Upgrader
- **Not craftable.** Dropped by Tier 2 players on death (section 6) — a dying player's tier made physical.
- Right-click while holding your shard to promote it to Tier 2.
- Droppable, tradeable, and **lost on death** — it drops where you fell and anyone can claim it.
- Glows and never despawns, but lava and the void will still destroy it.

> The Restoration Totem from earlier drafts is removed. Shards can no longer be lost, so it has no function.

### `/info`

A GUI showing:
- Element name, colour, and tier
- Passive description
- All abilities with icon, name, input, cooldown, and full effect text
- Ultimate — greyed out with an unlock hint if Tier 1
- Challenge progress bar with exact numbers (`147 / 200 damage absorbed`)
- Controls reference: RMB, ⇧LMB, ⇧RMB
- Ability message toggle

### `/recipes`

A GUI listing every craftable item with a visual 3×3 grid. Currently just the Shard Trader — but built as a list so it scales. Include a note that Upgraders are obtained through combat, not crafting, so people don't hunt for a recipe that doesn't exist.

---

## 9. Data model

**Item PDC** (`elementary`):
- `element` — STRING
- `tier` — INTEGER
- `owner` — STRING (UUID)
- `shard_id` — STRING (unique instance)
- `item_type` — STRING: `SHARD` / `TRADER` / `UPGRADER`

**Player PDC:**
- `bound_element`, `bound_tier`
- `challenge_progress` — DOUBLE
- `ability_messages_enabled` — BYTE
- `has_received_shard` — BYTE

Cooldowns and in-flight challenge state live in memory and reset on restart. Committed progress persists.

*(The old `last_upgrader_drop` timestamp is gone — the Upgrader system no longer has a per-victim cooldown to track.)*

---

## 10. Technical notes

**Moving Bulwark.** `BlockDisplay` entities repositioned each tick with interpolation, plus manual collision: get entities in the wall's bounding box each tick and set their velocity to the travel vector. Never real blocks — they can't move smoothly, cause chunk updates, and leave grief if the plugin crashes mid-cast.

**True damage.** `DamageSource` built on `DamageType.GENERIC_KILL` bypasses armour, enchantments, and i-frames. Never subtract health directly — that skips death handling and breaks death events, which matters here because death drives the Upgrader system.

**Lightning.** `World.strikeLightningEffect()` for visuals, damage applied separately. `strikeLightning()` starts fires.

**Death-tier handoff.** Demotion and the Upgrader drop both happen in `PlayerDeathEvent`: read the victim's tier, drop the Upgrader at the death location if they were Tier 2, write the demoted tier *before* the respawn re-issue runs so the restored shard is already Tier 1. Keep it in one listener so the ordering can't drift.

**Pyre's terrain transform.** This is the only ability that touches real blocks, so it needs to be bulletproof:
- Store every changed position with its original `BlockData` in a `TerrainTransform` object owned by the zone.
- Apply with `setBlockData(data, false)` to skip physics updates — otherwise you trigger cascading updates on gravel, sand, and redstone.
- **Persist active transforms to disk** (`active_transforms.json`, written on creation, deleted on restore). On plugin enable, restore anything left in that file before doing anything else. Without this, a crash mid-Pyre leaves permanent netherrack.
- On plugin disable, restore every active transform synchronously.
- If a player modifies a converted block during the effect, drop it from the restore list rather than overwriting their change on expiry.
- Cap converted blocks per cast (~250) as a safety valve against unexpected geometry.
- Magma placed adjacent to water creates bubble columns. That's vanilla behaviour and genuinely fun near shorelines — but if it causes drowning deaths you didn't intend, exclude columns with water neighbours from magma specifically.

**Cataclysm visuals.** Display entities and particles only — spikes never become real blocks.

**Shard lock-down.** Route every shard-protection listener through one `ShardProtectionListener` at `EventPriority.HIGHEST` so nothing slips through a plugin conflict. Write a test checklist: drop, die, ender chest, shulker, item frame, villager trade, hopper, dispenser.

**Challenge tracking.** Air's airborne check needs `isOnGround()` plus a downward raycast — slabs and shallow water register oddly. Water's swim counter should sample position once per second, not per tick.

---

## 11. Architecture

```
ElementaryPlugin
├── shard/            Element, ShardItem, ShardManager
├── power/            Power (abstract), PowerRegistry, impl/{Earth,Water,Fire,Air}Power
├── challenge/        Challenge (abstract), impl/{Unmoved,DeepCurrent,Kindling,UntouchedSky}
├── entity/           MovingWall, ZoneEffect
├── item/             Upgrader, ShardTrader, UpgraderDropHandler
├── advancement/      DatapackInstaller, AdvancementGranter
├── listener/         InteractListener, JoinListener, DeathListener,
│                     RespawnListener, ShardProtectionListener, DamageListener
├── hud/              AbilityHud (interface), BossBarHud, FontHud
├── command/          InfoCommand, RecipesCommand, AdminCommand
├── ui/               InfoGui, RecipesGui
└── util/             CooldownManager, Announcer, Keys
```

Adding a fifth element = one power file, one challenge file, one enum constant, four icon glyphs.

---

## 12. Build order

1. **Scaffold** — Gradle, Java 21, `paper-plugin.yml`, plugin loads.
2. **The item** — PDC, custom model data, `/shard give` admin command.
3. **Binding + lock-down** — first join, respawn restore, all protection listeners. Test the full checklist before moving on; this is load-bearing for everything else.
4. **Power abstraction** — Earth only, passive only.
5. **Tremor** — proves cooldowns, targeting, and the ability message system.
6. **BossBar HUD** — behind the `AbilityHud` interface.
7. **Bulwark** — hardest ability in the plugin. Do it while the codebase is small.
8. **Remaining three elements** — one per session.
9. **Challenges** — all four, with persistence and progress notifications.
10. **Upgrader drops** — death demotion, the drop, the challenge-progress reset.
11. **Advancements** — datapack installer first, verify it loads with `/advancement grant`, then wire up granting, the global sound, and the announcement.
12. **`/info` and `/recipes` GUIs.**
13. **Ultimates.**
14. **Resource pack** — 8 shard textures + 12 ability icons + 4 advancement icons.
15. **Font HUD** — swap `BossBarHud` for `FontHud`.
16. **Balance pass** with real players.

---

## 13. Balance notes

- **Roles:** Earth denies space, Fire controls it, Water out-damages you in melee, Air decides whether the fight happens at all.
- **No element heals allies.** Teamfights are pure damage races. If that plays badly, Thunderstorm is where an ally component naturally goes.
- **Thunderstorm is intentionally the strongest damage tool.** Watch it. If Water wins every duel, raise the cooldown to 60s — don't cut the damage.
- **Tier 2 churns by design.** It's a crown to defend, not a rank to keep — the total number of Tier 2s only grows through challenges, and every death puts one back in play. If churn feels too punishing, restrict demotion to player kills before adding any drop cooldown.
- **Updraft is now disruption, not execution.** Resist raising the height; Air's fall immunity makes every extra block worth more to Air than to anyone else.
- **Fire has zero mobility** and will be kited by Air. If Fire never gets to use Pyre, add a brief slow on Fireball hit rather than giving Fire a dash.
- **Pyre's magma stacks with its own tick damage.** Enemies inside take 1/sec from the ability plus magma contact damage, which is more than the listed number suggests. Reduce the magma share below 15% before touching the tick rate.
- **Sneaking negates magma damage** in vanilla, so crouch-walking through a Pyre roughly halves the punishment. Keep this — it's free counterplay and rewards players who know the game.
- **Bulwark punching is skill expression.** Don't make it auto-launch.
- Test every ability at spawn, in the Nether, in the End, and underwater. The End is where AoE ground abilities usually break.
