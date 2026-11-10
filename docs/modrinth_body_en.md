# Create: Navigation Terminal

> A Create-flavoured redstone navigation terminal: pick a destination, emit 0–15 redstone by distance, and get a cockpit-style HUD while seated in a bound vehicle.

---

## ⚠️ Compliance notice (read first)

- **The Xaero integration is optional and read-only**: it contains and redistributes none of Xaero's code,
  class files, decompiled output, or assets. It only reads the player's local `xaero/world-map/` cache and
  reflectively reads Xaero's **public** API classes to list waypoints.
- **Xaero is not required.** Without it, terminal / cards / redstone / HUD / fleet all work; only the
  waypoint candidate list is empty and the map falls back to a schematic grid.
- Xaero is licensed **ARR (All Rights Reserved)**, so this integration sits in a good-faith grey area under a
  strict reading. **If the Xaero author objects, the integration will be removed immediately** and the mod
  falls back to the no-Xaero implementation.
- If your environment must not include any interop code, use the **`-noxaero` variant** below — that jar
  contains no Xaero-reading classes at all.

---

## Requirements

| Type | Dependency | Notes |
|---|---|---|
| Required | Minecraft **1.21.1** + NeoForge **21.1.244+** | — |
| Required | [Drive-By-Wire with Sable](https://modrinth.com/mod/drive-by-wire-sable) (`drivebywire`, MIT) | Cable signal source; without it the terminal simply cannot be wired up |
| Optional | [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) / [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) | Waypoints and offline terrain cache |
| Optional | Ponderer (MIT) | Hosts the 6 built-in ponder scenes; skipped if absent |

> Dependencies are declared in `META-INF/neoforge.mods.toml`. **Create is not a declared dependency**;
> seat/nameplate interaction goes through Drive-By-Wire and read-only reflection of public methods.

---

## Features

**Navigation Terminal (block)**
- Three tabs — Navigation / Parameters / Route; footer: reset arrival, clear target, bind seat
- Four parameters: mode (distance ramp / arrival switch / fixed power), max power 0–15, coverage range 8–512, arrival radius 1–64
- Three parameter presets; multi-stop routes with reordering and one-way / loop endings
- Cross-dimension targets emit full signal plus a "switch dimension" hint
- Distance is **horizontal only** and computed server-side, so the GUI number and the redstone signal share one source

**Waypoint Card (item)**
- Up to 32 waypoints per card; can be named and locked read-only (a locked card refuses writes, but can still be read and renamed)
- **Dye it in a crafting table** (16 colours, one texture per colour) to sort routes; dyeing preserves the stored route

**Vehicle HUD**
- Only while seated in the bound seat; disappears on dismount. 8-direction arrow, heading tape, target marker, distance scale, route progress
- Bearing uses **camera position + camera attitude**, so first person, all three F5 modes and custom cameras need no calibration

---

## Installation

1. Install Minecraft 1.21.1 + NeoForge 21.1.244+;
2. Add Drive-By-Wire (required);
3. Drop this mod's jar into `mods/`;
4. Optionally add the two Xaero mods for waypoints and terrain;
5. Launch.

**Servers need it too** (block entity plus 26 network payloads); a client-only install cannot join.

---

## Two build variants

| Jar | Content | When to use |
|---|---|---|
| `create_navigation_terminal-<ver>.jar` | Full feature set (includes the read-only Xaero interop) | Normal use |
| `create_navigation_terminal-noxaero-<ver>.jar` | **No Xaero interop classes in the jar**, and `mods.toml` declares no Xaero dependency; everything else identical | Environments that must not ship third-party interop code |

```bat
gradlew.bat build                 :: full
gradlew.bat build -PnoXaero       :: no-Xaero variant
```

---

## Configuration

`config/create_navigation_terminal-client.toml`, all keys under `[hud]` (24 keys).

| Common keys | Default | Description |
|---|---|---|
| `colorMain` / `colorDim` | `3CFF5A` / `28C040` | HUD colours (RRGGBB, no `#`) |
| `markerStyle` | `box` | Marker shape (6 variants) |
| `smoothing` / `deadZone` | `0.6` / `2.0` | Position smoothing / dead zone (px) |
| `requireGoggles` | `true` | Require Engineer's Goggles |
| `showRadar` / `showHorizon` / `showFleet` / `showBeacons` | `true` | Element toggles |
| `hiddenWaypoints` | empty | Waypoint names to hide, comma separated |
| `showDebug` | `false` | Debug line (top-left) |

Re-enter the world to apply; no game restart needed. Full list in `README.md` section 8.

---

## Known issue

**Crash when opening the creative inventory, `create:minecart_contraption` unbound** — a Create-side problem,
unrelated to this mod:

```
Parsing error loading recipe create:crafting/kinetics/minecart_from_contraption_cart:
  Failed to parse either. First: Not a json array: {"item":"create:minecart_contraption"}
NullPointerException: Trying to access unbound value:
  ResourceKey[minecraft:item / create:minecart_contraption]
  at create…AllCreativeModeTabs$RegistrateDisplayItemsGenerator.makeExclusionPredicate
```

Debug direction: disable mods that rewrite ingredient parsing (e.g. Polymorph) and check KubeJS data-pack overrides.

---

## Changelog

Per-version history lives in `CHANGELOG.md`; recent versions:

| Version | Summary |
|---|---|
| 1.1.93-beta | Removed the Xaero write path (integration is strictly read-only); compliance and reflection details documented |
| 1.1.92-beta | Creative-tab icon re-cut to a 32×32 cropped texture |
| 1.1.91-beta | Mod logo and creative-tab icon |
| 1.1.90-beta | GUI/HUD string and object allocation caching (behaviour equivalent) |
| 1.1.88-beta | Craft waypoint cards with dyes |

---

## Licence and credits

- This mod: **MIT License** (author HZLDM)
- All third-party mod names and trademarks belong to their authors; this page describes interoperability only
- This mod contains no decompiled code, crack patches, or licence-verification bypasses

> The Xaero integration is a good-faith interoperability effort. If the rights holder objects, contact us and it
> will be removed — falling back to the no-Xaero implementation immediately.
