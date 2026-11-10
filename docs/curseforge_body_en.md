# Create: Navigation Terminal

> A Create-flavoured redstone navigation terminal: pick a destination, emit 0–15 redstone by distance, and get a cockpit-style HUD while seated in a bound vehicle.

**⚠️ Compliance notice**: the Xaero integration is **optional and read-only** — it contains and redistributes none of Xaero's code, class files, decompiled output, or assets; it only reads the player's local `xaero/world-map/` cache and reflectively reads Xaero's **public** API classes. **Xaero is not required.** If the Xaero author objects, the integration will be removed immediately and the mod falls back to the no-Xaero implementation. A **`-noxaero` file** is also provided: that jar contains no Xaero interop classes at all.

---

## Which file to download

| File | Content | When to use |
|---|---|---|
| `create_navigation_terminal-1.0.0.jar` | Full feature set (includes read-only Xaero interop) | Normal use |
| `create_navigation_terminal-noxaero-1.0.0.jar` | **No Xaero interop classes**, and `mods.toml` declares no Xaero dependency; everything else identical | Environments that must not ship third-party interop code |

---

## Requirements

| Type | Dependency | Notes |
|---|---|---|
| Required | Minecraft **1.21.1** + NeoForge **21.1.244+** | — |
| Required | **Drive-By-Wire with Sable** (`drivebywire`, MIT) | Cable signal source; without it the terminal simply cannot be wired up |
| Optional | **Xaero's Minimap** / **Xaero's World Map** | Waypoints and offline terrain cache |
| Optional | **Ponderer** (MIT) | Hosts the 6 built-in ponder scenes; skipped if absent |

> **Create is not a declared dependency**; seat/nameplate interaction goes through Drive-By-Wire and read-only reflection of public methods.
> Servers need it too (block entity plus 26 network payloads).

---

## Features

**Navigation Terminal (block)**
- Three tabs: Navigation / Parameters / Route; footer: reset arrival, clear target, bind seat
- Parameters: mode (distance ramp / arrival switch / fixed power), max power 0–15, coverage range 8–512, arrival radius 1–64
- Three presets; multi-stop routes with reordering, one-way / loop endings; cross-dimension targets emit full signal plus a hint
- Distance is **horizontal only**, computed server-side so the GUI and the redstone signal share one source

**Waypoint Card (item)**
- Up to 32 waypoints per card; nameable, lockable read-only (a locked card refuses writes but can still be read and renamed)
- **Dye it in a crafting table** (16 colours, one texture per colour) to sort routes; dyeing preserves the stored route

**Vehicle HUD**
- Only while seated in the bound seat; gone on dismount. 8-direction arrow, heading tape, target marker, distance scale, route progress
- Bearing uses **camera position + camera attitude**, so first person, all F5 modes and custom cameras need no calibration

---

## Installation

1. Install Minecraft 1.21.1 + NeoForge 21.1.244+;
2. Add Drive-By-Wire (required);
3. Drop this mod's jar into `mods/`;
4. Optionally add the two Xaero mods for waypoints and terrain;
5. Launch.

---

## Configuration

`config/create_navigation_terminal-client.toml`, all keys under `[hud]` (24 keys). Common ones:

| Key | Default | Description |
|---|---|---|
| `colorMain` / `colorDim` | `3CFF5A` / `28C040` | HUD colours (RRGGBB, no `#`) |
| `markerStyle` | `box` | Marker shape (6 variants) |
| `smoothing` / `deadZone` | `0.6` / `2.0` | Position smoothing / dead zone (px) |
| `requireGoggles` | `true` | Require Engineer's Goggles |
| `showRadar` / `showHorizon` / `showFleet` / `showBeacons` | `true` | Element toggles |
| `hiddenWaypoints` | empty | Waypoint names to hide, comma separated |
| `showDebug` | `false` | Debug line (top-left) |

Re-enter the world to apply; no game restart needed.

---

## Known issue

**Crash when opening the creative inventory, `create:minecart_contraption` unbound** — a Create-side problem, unrelated to this mod:

```
Parsing error loading recipe create:crafting/kinetics/minecart_from_contraption_cart:
  Failed to parse either. First: Not a json array: {"item":"create:minecart_contraption"}
NullPointerException: Trying to access unbound value:
  ResourceKey[minecraft:item / create:minecart_contraption]
  at create…AllCreativeModeTabs$RegistrateDisplayItemsGenerator.makeExclusionPredicate
```

Debug direction: disable mods that rewrite ingredient parsing (e.g. Polymorph) and check KubeJS data-pack overrides.

---

## Licence

**MIT License** (author HZLDM). All third-party mod names and trademarks belong to their authors. This mod contains no decompiled code, crack patches, or licence-verification bypasses.

### Development note (AI involvement)

This project's **code and documentation were produced with AI assistance** (Modrinth disclosure:
*Contains AI-generated content* is enabled). Textures (block / item / cards / icon) are drawn by the author.
The mod **contains no generative-AI functionality at runtime** — it makes no network calls and invokes no models.
