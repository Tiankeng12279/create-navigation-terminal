# Create: Navigation Terminal

> ### ⚠️ Compliance notice (TL;DR)
> **The Xaero integration is optional and read-only**: it contains and redistributes none of Xaero's code,
> class files, decompiled output, or assets. It only reads the player's local `xaero/world-map/` cache and
> reflectively reads Xaero's public API classes to list waypoints.
> **Xaero is not required**; without it every core feature (terminal / cards / redstone / HUD / fleet) still works.
> **If the Xaero author objects, the integration will be removed immediately** and the mod falls back to the
> no-Xaero implementation. Xaero is licensed **ARR (all rights reserved)**, so this integration sits in a
> good-faith grey area under a strict reading — see [Appendix A](#13-appendix-a-xaero-integration-and-format-analysis).

> A Create-flavoured redstone navigation terminal — reads Xaero map waypoints (optional), emits a redstone signal based on distance to the target, and adds a cockpit-style navigation HUD on vehicles.

| Field | Value |
|---|---|
| mod id | `create_navigation_terminal` |
| Version | `1.0.0` (see `gradle.properties`) |
| Minecraft | **1.21.1** |
| NeoForge | **21.1.244** (compile), verified on 21.1.252 at runtime |
| Java | **21** |
| Side | Both (`side = BOTH`) |

---

## Table of Contents

- [1. Overview](#1-overview)
- [2. Features](#2-features)
- [3. Requirements](#3-requirements)
- [4. Installation](#4-installation)
- [5. Building from Source](#5-building-from-source)
- [6. Usage](#6-usage)
- [7. The HUD](#7-the-hud)
- [8. Configuration](#8-configuration)
- [9. Implementation Notes](#9-implementation-notes)
- [10. Repository Layout](#10-repository-layout)
- [11. FAQ](#11-faq)
- [12. Changelog](#12-changelog)
- [13. Appendix A: Xaero Integration and Format Analysis](#13-appendix-a-xaero-integration-and-format-analysis)
- [14. Disclaimer and Terms of Use](#14-disclaimer-and-terms-of-use)

---

## 1. Overview

**Create: Navigation Terminal** adds a **Navigation Terminal** block. Pick a destination on your map and the terminal outputs a 0–15 redstone signal proportional to how far you still are. Sit in a vehicle seat that has been bound to the terminal and a fighter-cockpit-style navigation HUD is drawn on screen.

Three layers:

1. **Terminal block** — pick a target, edit a route, tune parameters, drive redstone;
2. **Waypoint Card** — stores a set of waypoints as an item, portable across players and vehicles;
3. **Vehicle HUD** — only shown while seated in the bound vehicle, gone the moment you stand up.

Xaero's Minimap / World Map are **optional** data sources; the mod works without them (see [Appendix A](#13-appendix-a-xaero-integration-and-format-analysis)).

---

## 2. Features

### Content

| Registry ID | Type | Description |
|---|---|---|
| `create_navigation_terminal:navigation_terminal` | Block + block entity + container GUI | Set a target, tune parameters, output 0–15 redstone by distance |
| `create_navigation_terminal:waypoint_card` | Item | Stores up to 32 waypoints; can be named, locked read-only, and dye-coloured for sorting |
| `create_navigation_terminal:mod_icon` | Item (hidden) | Only used as the creative-tab icon; never inserted into any inventory |

### Navigation

- Target / route / parameters all live in the block entity;
- Distance is **horizontal only** (see [9.1](#91-horizontal-distance-only));
- Cross-dimension targets emit full signal and display a "switch dimension" hint;
- Routes support multiple stops, reordering, and a one-way / loop end behaviour.

### Waypoint Card

- **Write** the current route into the card, **read** a stored route back out;
- Cards can be **named** and flagged **read-only** (a read-only card refuses to be overwritten);
- **Dye the card in a crafting table** (16 dyes → 16 colours) to sort routes by colour;
- Colouring never discards the stored route, name, or read-only flag.

### Vehicle HUD

- Only visible while seated in the bound seat, disappears on dismount;
- 8-direction arrow, heading tape, target marker, distance scale, route progress;
- Bearing is computed from **camera position + camera attitude**, so first person, all three F5 modes and custom cameras need no calibration.

### Ponder

- 6 built-in ponder scenes ship with the mod;
- When **Ponderer** is present they are installed into its script folder automatically; without Ponderer the feature is skipped and nothing else is affected.

---

## 3. Requirements

### Required

| Dependency | Notes |
|---|---|
| Minecraft **1.21.1** | `minecraft_version_range = [1.21.1]` |
| NeoForge **21.1.244+** | Declared as required |
| **DriveByWire** | Declared as required (`versionRange = [0,)`) |
| Java **21** | Compilation and runtime |

> Declarations live in `src/main/templates/META-INF/neoforge.mods.toml`.

### Optional

| Dependency | Purpose |
|---|---|
| **Xaero's Minimap** | Contributes waypoints and the candidate list |
| **Xaero's World Map** | Supplies the local map-cache terrain used as the terminal backdrop |
| **Ponderer** | Hosts the ponder scene scripts (feature is skipped otherwise) |

### About Create

The mod **does not declare Create as a dependency**, and no code references `com.simibubi.*` or `net.createmod.*`. Seat binding and nameplate access go through DriveByWire and reflection (see [13.3](#133-create--create-aeronautics-simulated)). It therefore runs in packs without Create — the vehicle-related gameplay simply has nothing to attach to.

---

## 4. Installation

1. Install **Minecraft 1.21.1 + NeoForge 21.1.244+**;
2. Put DriveByWire into `mods/`;
3. Put `create_navigation_terminal-<version>.jar` into `mods/`;
4. Optionally add Xaero's Minimap / World Map for waypoints and map terrain;
5. Launch the game.

Servers need the mod too (block entity + network payloads). A client-only install cannot join a modded server because the registries differ.

---

## 5. Building from Source

```bat
cd C:\模组构建\create_navigation_terminal
gradlew.bat build
```

Output:

```
build\libs\create_navigation_terminal-<version>.jar
```

Offline build (dependencies already cached):

```bat
gradlew.bat build --offline
```

---

## 6. Usage

The GUI has **three tabs** — `Navigation` / `Parameters` / `Route` — with fixed buttons `Reset Arrival`, `Clear Target`, `Bind Seat` at the bottom. The left column holds the card slot plus `Write`, `Read`, `Settings`, `Rename`, `Read-only`.

### 6.1 Navigation tab: set a target / build a route

The list contains Xaero waypoints (when installed) plus the points stored on the card in your main hand.

- **Left click** a point → set it as the current target (clears the route)
- **Right click** a point → context menu:

```
┌──────────────────────────┐
│ Set as navigation target │
│ Add to route             │
│ Remove from route        │  ← only when the point is in the route
└──────────────────────────┘
```

### 6.2 Parameters tab

| Parameter | Range | Description |
|---|---|---|
| Mode | Distance ramp / Arrival switch / Fixed power | Redstone output logic |
| Max power | 0–15 | Signal ceiling |
| Coverage range | 8–512 blocks | Beyond this the output is 0 |
| Arrival radius | 1–64 blocks | Inside it the target counts as reached |
| On arrival | Keep output / Silent stop / Clear target | Behaviour once arrived |

**Distance ramp**:

```
ratio  = (coverage range - distance) / (coverage range - arrival radius)
output = ratio × max power
```

**Presets**: three slots, each storing the same five parameters.

```
Click [1]  → apply preset 1
Tune       → click [Store] → overwrite the slot you selected
```

### 6.3 Route tab: map preview + stop management

The upper half is a **live miniature map**:

```
┌────────────────────────────────┐
│      ●───────●                 │  ● yellow = stop
│     ╱         ╲                │  ● green  = current stop
│    ●           ●───────●       │  ● white  = player position
│                                │  ─ blue   = route direction
│           ○                    │  ─ purple = loop closing segment
└────────────────────────────────┘
```

- The view is scaled to fit the bounding box of all stops plus the player, so distant stops still fit;
- Adjacent stops are joined in blue; **loop mode** adds a purple segment from the last stop back to the first;
- With Xaero installed the backdrop is sampled terrain; without any Xaero data it falls back to a **schematic grid** (labelled "sample map" in the corner).

The lower half is the stop list plus controls:

```
  1. Iron Camp         413, -143
▶ 2. Nether Gate     -372, -149     ← current stop highlighted
  3. Terminus          33, -8
Stop 2/3 · Loop
[◀] [▶] [×] [Loop]
```

- `◀` / `▶`: step to the previous / next stop
- `×`: delete the current stop (clearing the target when the route becomes empty)
- `Loop` / `One-way`: toggle the end behaviour
  - **Loop**: after the last stop, return to the first
  - **One-way**: stop at the last stop and mark arrival
- The HUD mirrors the progress as `2/3`

### 6.4 Waypoint Card: write / read / rename / lock / colour

```
1. Put the card into the left slot
2. [Write]  current route → card
3. [Read]   card → current route
4. [Rename] opens a text field; Enter saves
5. [Read-only] toggles the lock: a locked card can be read and renamed, but refuses writes
6. Crafting table: card + any dye → a card of that colour (route / name / lock preserved)
```

### 6.5 Binding a seat

```
1. Sit in the vehicle seat
2. Open the terminal GUI on that vehicle
3. Click "Bind Seat" (plain right click — sneaking dismounts you)
```

---

## 7. The HUD

```
         ↑ Iron Camp  2/3
   distance 187   ΔY +12
       Arrived · navigation stopped
```

- Arrow: 8 directions, computed relative to **your current view**;
- Only shown on the bound seat; disappears instantly on dismount;
- Cross-dimension targets display `target in the Nether` (following the target dimension);
- Wearing **Engineer's Goggles** is required by default (`hud.requireGoggles`).

---

## 8. Configuration

File: `config/create_navigation_terminal-client.toml`. All keys live under `[hud]` (24 keys, matching `ClientConfig.java`).

| Key | Default | Description |
|---|---|---|
| `colorMain` | `3CFF5A` | HUD primary colour (RRGGBB, no `#`) |
| `colorDim` | `28C040` | HUD secondary colour |
| `colorOther` | `FFA500` | Colour of other route stops |
| `colorFleet` | `4FA3FF` | Fleet marker colour |
| `colorBeacon` | `00E5FF` | Beacon colour |
| `markerStyle` | `box` | Marker shape (6 variants) |
| `markerYOffset` | `-6` | Marker vertical offset (pixels) |
| `showSpatialMarker` | `true` | Draw the spatial (box) marker |
| `showOtherWaypoints` | `true` | Draw the other route stops |
| `showAllWaypoints` | `true` | Draw every Xaero waypoint |
| `hiddenWaypoints` | empty | Waypoint names to hide, comma separated |
| `showHorizon` | `true` | Draw the horizon indicator |
| `showFleet` | `true` | Draw fleet markers |
| `showBeacons` | `true` | Draw vehicles that have a nameplate but no terminal |
| `requireGoggles` | `true` | Require Engineer's Goggles |
| `showRadar` | `true` | Draw the radar |
| `showDebug` | `false` | Draw the debug line (top-left) |
| `headingTapeY` | `46` | Heading tape Y |
| `distScaleBottom` | `82` | Distance cursor offset from the bottom |
| `crosshairSize` | `10` | Crosshair size (half extent, pixels) |
| `smoothing` | `0.6` | Position smoothing (0 = raw, higher = smoother but laggier) |
| `deadZone` | `2.0` | Dead zone in pixels (suppresses sub-pixel jitter) |
| `relOffsetDeg` | `0.0` | **Legacy**, no longer affects marker position after the 3D-projection rewrite |
| `fovHalfDeg` | `100.0` | **Legacy**, same as above |

> Older builds registered additional keys (`invertMarker`, `invertVertical`, `followVehicleYaw`, `vehicleYawOffset`, `viewWeight`, `thirdPersonYawOffset`, `verticalScale`). They are no longer registered and have no effect; the note exists only to explain old config files.

Changes take effect when you re-enter the world — no game restart required.

---

## 9. Implementation Notes

### 9.1 Horizontal distance only

```
d = sqrt((x1-x2)² + (z1-z2)²)
```

Height difference `ΔY` is displayed for reference and never participates in signal or arrival logic.

### 9.2 Sable SubLevel compatibility

Blocks inside a Sable physics structure (SubLevel) report a virtual `worldPosition`
(measured: `20481034, 128, 20483095`); feeding that into the distance math yields absurd
values such as 2.9e7.

The terminal therefore keeps a set of **world reference coordinates** (`refX/refY/refZ`):

- written when the seat is bound, from the player's real position;
- refreshed continuously while the player sits in the bound vehicle;
- preferred by all distance calculations.

### 9.3 The client never computes distance

The server computes `syncedDistance` every 8 ticks and ships it through `getUpdateTag`.
The client only displays it — so the GUI number and the redstone signal always share one source of truth.

### 9.4 HUD bearing uses the camera, not the player

Third-person views (especially Create Aeronautics' custom camera) orbit the vehicle, and the
camera can be tens of blocks away from the player. Computing `atan2` from the player position
would be off by tens of degrees.

The HUD therefore derives **bearing** from the camera position + camera yaw:

```java
double cdx = targetX - camera.getPosition().x;
double cdz = targetZ - camera.getPosition().z;
double targetYaw = Math.atan2(-cdx, cdz);
float heading = camera.getYRot();
```

**Distance / ΔY** still use the player position (where you actually are).

This makes first person, all three F5 modes and Aeronautics cameras work without any offset calibration.

### 9.5 Target marker via 3D projection

Projection (Minecraft left-handed space: +X east, +Y up, +Z south):

```
forward = (-sin(yaw)·cos(pitch), -sin(pitch),  cos(yaw)·cos(pitch))
right   = (-cos(yaw),            0,          -sin(yaw))
up      = right × forward

side  = (target - cam) · right
upv   = (target - cam) · up
depth = (target - cam) · forward

screenX = centerX + side  / depth × fovScale
screenY = centerY - upv   / depth × fovScale
```

`fovScale = (screenHeight / 2) / tan(35°)`, matching the in-game perspective.

Sub-pixel camera motion still shows up as slight jitter, hence the configurable exponential smoothing (`smoothing`).

### 9.6 How the marker implementation evolved

| Approach | Problem |
|---|---|
| Player position + `getYRot` | Camera orbits the vehicle in third person / Aeronautics; bearing drifts |
| Vehicle yaw | Create seat yaw differs from MC by 90°, worse |
| Camera position + camera yaw | Direction correct, but combining two angles drifts |
| **Camera basis vectors + 3D projection** | **One geometric transform, no accumulated error** |

---

## 10. Repository Layout

```
create_navigation_terminal/
├─ build.gradle                       # NeoForge ModDev build script
├─ gradle.properties                  # Versions and dependency coordinates
├─ README.md                          # Chinese documentation
├─ README_EN.md                       # This document
├─ README.legacy.md                   # Previous README (archive)
├─ libs/                              # compileOnly third-party jars (Xaero / DriveByWire / Sable)
└─ src/main/
   ├─ templates/META-INF/neoforge.mods.toml   # mods.toml template
   ├─ java/com/hzldm/createnavigationterminal/
   │  ├─ CreateNavigationTerminal.java        # @Mod entry point
   │  ├─ block/            (1 file)           # the terminal block
   │  ├─ blockentity/      (1)                # route / parameters / card slot / redstone
   │  ├─ item/             (3)                # waypoint card, card data, waypoint record
   │  ├─ menu/             (1)                # container menu (card slot)
   │  ├─ client/           (17)               # screens, HUD, style, icons, scene installer
   │  ├─ config/           (1)                # client configuration
   │  ├─ fleet/            (1)                # fleet channel
   │  ├─ integration/      (7)                # Xaero / DriveByWire / Sable adapters
   │  ├─ network/          (28)               # 26 payloads + registration + HUD service
   │  ├─ recipe/           (1)                # card dye recipe
   │  ├─ registry/         (7)                # item/block/component/menu/recipe-serializer registries
   │  └─ server/           (2)                # server-side logic
   └─ resources/
      ├─ assets/create_navigation_terminal/
      │  ├─ blockstates/  models/  textures/  # block & item models, 16 card faces, icons
      │  ├─ lang/                              # zh_cn / en_us
      │  └─ ponder_scenes/                     # 6 built-in ponder scenes (shipped)
      └─ data/create_navigation_terminal/
         └─ recipe/card_dye.json               # card dye recipe (custom type)
```

---

## 11. FAQ

**Does it work without Xaero?**
Yes. Terminal, cards, redstone, HUD and fleets are fully independent; only the waypoint candidate list stays empty and the map backdrop falls back to the schematic grid.

**Why is the map just a grid?**
No Xaero terrain data is available (Xaero not installed, or you are inside a ponder mirror scene). The grid is a placeholder backdrop; route points are still drawn on top and the corner is labelled "sample map".

**The HUD does not appear.**
Check in order: ① you are seated in a seat that was **bound** to the terminal; ② `hud.requireGoggles` defaults to `true`, so goggles are required; ③ a target has been set.

**Opening the creative inventory crashes with `create:minecart_contraption` unbound.**
That is a **Create-side** problem unrelated to this mod. Log evidence recorded in this repository:

```
Parsing error loading recipe create:crafting/kinetics/minecart_from_contraption_cart:
  Failed to parse either. First: Not a json array: {"item":"create:minecart_contraption"}
...
NullPointerException: Trying to access unbound value:
  ResourceKey[minecraft:item / create:minecart_contraption]
  at create@6.0.10…AllCreativeModeTabs$RegistrateDisplayItemsGenerator.makeExclusionPredicate
```

Create's own recipe fails to parse, the item ends up unregistered, and building the creative tab throws. Debug directions: disable mods that rewrite ingredient parsing (Polymorph in that environment), and check KubeJS data-pack overrides.

**Does dyeing a card erase its route?**
No. Dyeing uses a custom recipe (`CardDyeRecipe`) that copies the input card's components and only rewrites the colour field.

**Where are the ponder scenes?**
Shipped in `assets/create_navigation_terminal/ponder_scenes/`. When Ponderer is detected they are installed into `config/ponderer/scripts/` at startup, after which pressing `W` on the block or the card shows them.

**Do config changes need a restart?**
No — re-enter the world.

---

## 12. Changelog

### Historical table (preserved verbatim)

| Version | Content |
|---|---|
| 1.0.0 | Navigation terminal block (redstone output + stop on arrival) |
| 1.1.0 | Waypoint card |
| 1.1.1 | Card points merged into the candidate list |
| 1.1.2 | Distance switched to horizontal calculation |
| 1.1.3 | Seat binding + HUD |
| 1.1.4 | Binding moved to plain right click (sneaking dismounts) |
| 1.1.5 | HUD state logging |
| 1.1.6 | Distance and ΔY split into separate lines |
| 1.1.7 | Distance computed and synced by the server |
| 1.1.8 | SubLevel world reference coordinates + GUI bind button |
| 1.1.9 | Diagnostic line removed |
| 1.2.0 | Multi-target looping + cross-dimension |
| 1.2.1 | Route progress in the HUD |
| 1.3.0 | Route editing (step / delete) + parameter presets |
| 1.3.1 | One-way / loop toggle |
| 1.4.0 | GUI tabs (Navigation / Parameters / Route) |
| 1.5.0 | Context menu + route map preview |
| 1.6.0 | Map moved to terrain sampling (attempted Xaero cache read; texture content was placeholder colour, abandoned) |
| 1.9.0 | GUI enlarged to 320×300 |
| 2.1.0 | 1:1 sampling + canopy skipping + biome tinting |
| 2.2.x | Map zoom (scroll wheel); uniform view scaling fixes aspect ratio |
| 2.3.x | Canopy recoloured to yellow-green; MapColor fallback for unknown blocks |
| 2.4.x | Fighter-style HUD: crosshair / heading tape / target marker / distance scale |
| 2.5.x | Vehicle heading experiment (reverted due to yaw offsets) |
| 2.6.x | Client config file; angle normalisation; camera interpolation; third-person branch |
| 2.7.x | HUD bearing moved to camera coordinates — works with F5 and Aeronautics third person |
| 2.8.x | Box marker + 6 styles; true pitch placement; top/bottom edge arrows |
| **2.9.2** | **Switched to 3D projection (camera basis vectors); fixed MC left-handed space; position smoothing** |

> Numbering restarts at public release: the first public version is **`1.0.0`** (see `gradle.properties`). The table above lists internal iterations from before the public release; those numbers are retired and kept only for reference.

### Capabilities present in `1.0.0`
> For the full per-version history see [`CHANGELOG.md`](CHANGELOG.md); below is only a capability summary of the current build.


- Waypoint card: **naming**, **read-only locking**, **16-colour dye sorting** (crafting table) — none of which discard the stored route;
- Cards use 16 dedicated card-face textures (per-colour models, not tinting);
- GUI provides the card slot plus `Write / Read / Settings / Rename / Read-only` buttons;
- 6 built-in **ponder scenes** ship with the mod and auto-install when Ponderer is present;
- Mod icon and creative-tab icon replaced with the project artwork;
- String and object allocation caching in the GUI / HUD render path (`String.format` and class-name checks are memoised).

---

## 13. Appendix A: Xaero Integration and Format Analysis

> This chapter is the **original documentation content, preserved in full**; only heading levels changed for publication.

### 13.1 Xaero integration is optional

**This mod does not require Xaero's World Map or Minimap.**

- Not installed: the map shows live chunks within view distance (grid marks unloaded areas) and the waypoint list is empty
- Installed: Xaero waypoints are merged in, along with explored terrain from its offline cache

Seat binding, fleets, redstone output and the other core features are **completely independent of Xaero**.

### 13.2 Xaero's World Map / Minimap

**Approach**: read the map cache (`.xwmc`) under the player's own `<gameDir>/xaero/world-map/`
and decode it into an image used as the terminal backdrop.

**Boundaries**:

- **Contains none** of Xaero's code, class files, or decompiled output
- **Contains none** of Xaero's resource files (textures, models, language files, `vanilla_states.dat`, etc.)
- **Distributes none** of Xaero's data; it only reads caches produced **on the player's own machine, during the player's own gameplay**
- Knowledge of the cache format comes from **independent observation and structural analysis of the publicly downloadable mod jar and local files**; no implementation code was copied

The decoder (`XaeroCacheReader`) is an independent implementation:

- Zero dependencies, using only `java.util.zip` and `java.io`
- Its layout was reverse-engineered from the byte stream and cross-validated across many files (internal review record: 103 files parsed to EOF, `trailing == 0`)

**Regarding Xaero's own terms**: Xaero's mod licence is governed by its distribution page. This mod does **not modify or redistribute** Xaero itself; it only consumes its output files read-only. If the author considers this usage inappropriate, contact us via an issue and the integration will be removed, falling back to the "view-distance chunks only" implementation.

#### 13.2.1 The exact reflection surface (independently verifiable)

Besides decoding the cache files, the waypoint list is obtained through **read-only reflection** on Xaero's
public classes. All calls live in `integration/xaero/XaeroReflect.java` and follow the same shape:
`Class.forName` + `getMethod(...).invoke(...)`. They **never modify fields, never call any `set*` / `save*`
write method, and never use `setAccessible`**.

| Class (fully qualified) | Public method invoked |
|---|---|
| `xaero.hud.minimap.BuiltInHudModules` | reads the static field `MINIMAP` (`getField(...).get(null)`) |
| `xaero.hud.module.HudModule` | `getCurrentSession()` |
| `xaero.hud.minimap.module.MinimapSession` | `getWorldManager()` |
| `xaero.hud.minimap.world.MinimapWorldManager` | `getCurrentWorld()` |
| `xaero.hud.minimap.world.MinimapWorld` | `getCurrentWaypointSet()` |
| `xaero.hud.minimap.waypoint.set.WaypointSet` | `getWaypoints()` |
| `xaero.hud.minimap.waypoint.WaypointColor` / `WaypointPurpose` | colour / purpose enum values |
| `xaero.common.minimap.waypoints.Waypoint` | `getName()`, plus reading the public fields `x` / `y` / `z` |

Every call is wrapped in `try/catch(Throwable)`: if Xaero is absent or its internals shift, one warning is logged
and the list degrades to empty — nothing is thrown at the player and no other feature is affected.

**On writes**: this mod **writes nothing** into Xaero's data. An earlier build contained a path that wrote the
navigation target back as an Xaero waypoint (`addWaypoint` + `MinimapWorldManagerIO#saveWorld`, which required
`setAccessible` on a private field). It had **no UI entry point** (no caller) and contradicted the read-only
claim, so it was removed entirely; the pre-removal revision is archived at `_backup/XaeroReflect.java.before-readonly`.

### 13.3 Create / Create Aeronautics (Simulated)

**Approach**:

- Seat binding: read the entity UUID or block position (public API)
- Nameplate names: identified by **class-name suffix**, then a public method is invoked
  (`network/NavigationHudService.java`):
  ```java
  if (!be.getClass().getName().endsWith(".NameplateBlockEntity")) continue;  // suffix match, no hard reference
  java.lang.reflect.Method m = be.getClass().getMethod("getName");           // public method, no setAccessible
  Object v = m.invoke(be);                                                  // read-only, never written back
  ```
  i.e. `<providing-mod>.…NameplateBlockEntity#getName()`; the full package name is decided by whichever mod
  provides the block (this repository hard-codes none, so a provider change cannot break compilation).
- No bytecode modification of any kind, and no `setAccessible` on any member

**Boundaries**: no Create-family code or resources are included; all interaction goes through public APIs or read-only reflection.

### 13.4 Other third parties

- **joml** (`org.joml`): bundled with Minecraft; only its public maths API is used
- This project contains **no** decompiled code, crack patches, key generators, or licence-verification bypasses

---

## 14. Disclaimer and Terms of Use

This project is intended for **technical study and single-player / self-hosted play**. It has no
affiliation with, or authorisation from, the authors of the third-party mods mentioned above.
All third-party mod names and trademarks belong to their respective authors; this project only
describes interoperability as "compatible with xxx" and does not use their trademarks as its identity.

### 14.1 Terms of use

This mod is released under the **MIT License**, so the clauses below are **additional notes** on top of MIT and
do not reduce any right MIT grants you:

1. Redistributing the **unmodified** jar inside modpacks is allowed; keep the author and licence information from `neoforge.mods.toml`;
2. **Forking and redistributing modified builds is allowed** (a right granted by MIT); keep the copyright notice and licence text, and describe your changes;
3. Selling the mod itself, or charging money under its name, is not allowed (MIT does not restrict your commercial use of your own modified build, but do not pass it off as the original release);
4. Any save, redstone contraption, or vehicle loss caused by using this mod is the user's own responsibility (MIT's warranty disclaimer applies as well).

### 14.2 Disclaimer

> The statements above exist to **clarify technical boundaries in good faith** and do not exempt
> anyone from legal responsibility. If you intend to distribute this project commercially, obtain
> the necessary authorisation or legal advice yourself.

### 14.3 Copyright

| Item | Owner |
|---|---|
| This mod's code and assets | Author **HZLDM**, licensed under the **MIT License** (`gradle.properties: mod_license`, injected into `neoforge.mods.toml` at build time) |
| Minecraft | Mojang Studios / Microsoft |
| NeoForge | The NeoForged team |
| Xaero's Minimap / World Map | xaero96 and contributors |
| Create / Create Aeronautics | simibubi and Create contributors |
| DriveByWire | Its original authors |
