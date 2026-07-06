# TECHLOG

mode: carry-forward-context
purpose: paste_into_next_chat_without_context_loss
project_root: C:\Users\nikit\Documents\New project
target_mods_dir: C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods
mod_jar: build\libs\Imperfect_salvation-0.1.0.jar
installed_jar: C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar
mc_version: 1.20.4
loader: Fabric
build: Gradle/Loom
last_verified_build: .\gradlew.bat build => SUCCESS
last_installed_jar_size: 289899
last_installed_jar_time: 2026-06-29 17:57:29
last_installed_native_dll_size: 115712
last_installed_native_dll_time: 2026-06-29 17:57:14

current_active_state:
- target_mods_dir: C:\Users\nikit\Desktop\Project Imperfect Salvation\mods
- installed_jar: C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar
- mc_version: 1.20.1
- loader: Fabric
- last_verified_build: .\gradlew.bat build => SUCCESS
- last_installed_jar_size: 791436
- last_installed_jar_time: 2026-07-04 15:40:16
- distant_horizons_not_active: active jar is absent; only C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\_codex_backup_distant_horizons_20260702 remains
- installed_voxy: C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.2.jar
- disabled_old_voxy: C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.1.jar.disabled
- installed_create_stack: Create Fabric 6.0.8.1, Steam 'n' Rails 1.7.2, Create Crafts & Additions 1.3.4, Create Slice & Dice 3.5.2, Create New Age 1.2.0
- installed_launch_deps: Configurable 2.2.3, pneumonocore 1.3.1, cicada-lib 0.14.3
- active_story_systems: server start gate, narrative audio queue, global tasks screen/progression
- active_worldgen_passes: vanilla ore features plus runtime modded wall ore veins; enriched oasis trees; upgraded rift bridges and wall-entry corridors

## 2026-07-04 - Codex void-facing tunnel windows and connector cleanup

Request scope:
- Tunnels that run into open voids must have windows on the side walls.
- Tunnel generation still looks strange in places.

Implemented:
- Removed the broad connector deletion rule from `stateAt(...)`.
  Previously, any solid connector block overlapping an existing void/chamber was replaced with air. That made tunnels
  disappear or look broken exactly where they should have remained as enclosed passages with windows.
- Connector windows are no longer random wall holes:
  - `connectorLayerState(...)` now opens two-block-high windows only when the outer tunnel shell faces an actual open
    void/rift/chamber/railway air volume;
  - solid wall-embedded connector runs stay closed.
- Added open-void side-window checks to the primary railway tunnel walls.
- Tightened connector height-transition carving:
  - transition air is now limited to the internal passage width instead of deleting the whole outer shell;
  - this keeps the tunnel body coherent while still preventing height-transition dead ends.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `791436`, `2026-07-04 15:40:16`.

## 2026-07-04 - Codex tunnel height transition anti-dead-end fix

Request scope:
- Fix tunnel sections that should rise into the next tunnel but instead remain flat/blocked, forming a dead end.

Implemented:
- Primary railway:
  - station sections now use stable height from `primaryRailYAt(stationCenter)` instead of recalculating a different
    base height for every `x` inside the same station;
  - `isPrimaryRailwayAir(...)` now uses the same stable station base for station rooms/exits;
  - added a swept transition air volume around primary railway height changes, so generated blocks between adjacent
    raised/lowered rail sections are cut open instead of becoming a blocking wall.
- District connector tunnels:
  - `connectorLayerState(...)` now checks neighbouring connector heights when the exact current layer does not match;
  - if nearby pieces of the same connector are at different heights, the generator carves the transition volume between
    them as air.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `790634`, `2026-07-04 14:44:48`.

## 2026-07-04 - Codex black fish-eye terminal, corridor openings and railway upgrades

Request scope:
- Startup terminal must be a black monitor with only a slight fish-eye/corner bend; no scanlines, no green texture bands.
- Fix cases where an upper corridor floor/wall blocks a lower corridor.
- Add real windows to corridors exiting walls.
- Remove remaining corridor dead-end behavior by opening intersections/turns.
- Give bridge lamps supports.
- Let railway paths climb/descend and turn, while keeping the mandatory primary railway oasis chamber about 5k-15k blocks from spawn.

Implemented:
- Reworked `StartupTerminalVulkanRenderer`:
  - still calls the Vulkan bridge through `BlackHoleNativeBridge.renderVisualField(...)`;
  - no longer displays the generated visual field as a green CRT texture;
  - converts it into a very dark glass/fish-eye mask with subtle corner shadow only.
- Removed terminal scanline rendering from `ServerStartScreen`.
- Connector corridors:
  - connector shell blocks now yield to existing district-air corridors instead of covering them;
  - connector profiles were widened with an outer window layer;
  - two-block-high window cuts are generated in that outer layer.
- Bridge-to-wall corridors:
  - access/link width now includes the outer window layer;
  - windows can cut through the external shell instead of being hidden by adjacent wall mass.
- Dead-end corridor districts:
  - each former one-axis corridor now gets a center tie branch and widened edge mouths, creating usable corner/opening connections.
- Rift suspension bridges:
  - bridge lamps now have vertical pipe supports and short rusty-metal brackets.
- Primary railway:
  - added `primaryRailYAt(x)`, keeping spawn flat while allowing deterministic climbs/descents further out;
  - rails now use ascending east/west rail shapes when the path changes height;
  - non-station primary railway segments now use an enclosed industrial tunnel profile with ribs, ceiling, side walls, lamps, grated center and rails;
  - the guaranteed primary railway oasis chamber now uses the rail height at its center, preserving the 5k-15k placement while matching the raised/lowered track.
- Secondary railway:
  - added deterministic `RailwayTurn` junction chambers where X/Z railway lanes intersect at compatible heights;
  - junctions include a deck, shell, light and curved rail pieces.
- `RailwayTurn` is a top-level package record (`src/main/java/ru/nikit/megastructure/world/RailwayTurn.java`) to avoid introducing another nested generator class-load risk.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Active jar installed to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed active jar contains `ru/nikit/megastructure/world/RailwayTurn.class`.
- Confirmed active jar does not contain `MegastructureChunkGenerator$RailwayTurn` or `MegastructureChunkGenerator$OasisAnchor`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `789943`, `2026-07-04 11:19:30`.

## 2026-07-04 - Codex locate_oasis OasisAnchor class-load fix

Request scope:
- Fix `/megastructure locate_oasis` failing with:
  `RuntimeException - Failed to load class file for 'ru.nikit.megastructure.world.MegastructureChunkGenerator$OasisAnchor'`.

Implemented:
- Moved `OasisAnchor` out of the private nested generator record into a package-level record:
  `src/main/java/ru/nikit/megastructure/world/OasisAnchor.java`.
- Removed the nested `MegastructureChunkGenerator$OasisAnchor` declaration, matching the earlier `OasisLocation`
  class-load fix.
- `MegastructureChunkGenerator` still constructs and consumes `OasisAnchor` the same way; only the class location changed.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Confirmed rebuilt jar contains `ru/nikit/megastructure/world/OasisAnchor.class`.
- Confirmed rebuilt jar no longer contains `ru/nikit/megastructure/world/MegastructureChunkGenerator$OasisAnchor.class`.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `787351`, `2026-07-04 00:59:07`.

## 2026-07-04 - Codex corridor duplicate cavity fix and tunnel windows

Request scope:
- Fix duplicated tunnel/cavity bands appearing above and below the real tunnel.
- Add two-block-high windows when tunnels run out toward open space.

Implemented:
- Removed the broad `isCorridorContinuityCut(...)` air pass from `districtAir(...)`.
  It was too generic and carved matching empty bands every 36 blocks vertically, causing the visible stacked
  duplicate tunnel voids.
- Deleted the `isCorridorContinuityCut(...)` method entirely.
- Corridor anti-dead-end behavior now relies on the actual corridor generators that are already through-running:
  `isCellCorridor(...)`, `isApartmentCorridor(...)`, `isDeadEndCorridor(...)`, and `isSparseWallCorridor(...)`.
- Added two-block-tall side windows to `bridgeConnectorCorridorState(...)`:
  - windows appear at `relY == 3 || relY == 4`;
  - only on the outer wall;
  - not on portal frames or ribs;
  - intended for connector tunnels that run along or out into open space.

Verification:
- Static search confirms `isCorridorContinuityCut` has no remaining Java references.
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `787454`, `2026-07-04 00:55:49`.

## 2026-07-04 - Codex startup Vulkan CRT hotfix

Request scope:
- The previous startup Vulkan texture showed the wrong visual-field pattern, like a green map/lightning field.
- The desired effect is only a convex old monitor screen rendered through the Vulkan bridge.

Implemented:
- `StartupTerminalVulkanRenderer` still calls `BlackHoleNativeBridge.renderVisualField(...)`, so the startup
  monitor path continues to exercise the existing Vulkan bridge.
- The strong generated visual-field image is no longer displayed directly.
- The Vulkan output is reduced to subtle phosphor/noise input and converted into a CRT glass texture:
  - rounded/vignetted edges;
  - central dome brightness;
  - soft upper and lower reflections;
  - low-amplitude phosphor grain;
  - scanline response.
- `renderVisualField(...)` intensity was lowered and switched away from the reservoir-looking field to prevent
  visible map-like contours from appearing behind terminal text.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `787684`, `2026-07-04 00:52:53`.

## 2026-07-04 - Codex vanilla spore blossom oasis particles and Vulkan startup terminal

Request scope:
- Replace custom oasis particles with ordinary vanilla spore blossom particles.
- Stop treating the startup monitor as only a GUI imitation; use the existing Vulkan bridge for the visual.

Implemented:
- `OasisSporeParticleSpawner` now emits `ParticleTypes.SPORE_BLOSSOM_AIR`.
- Removed the active custom oasis particle stack:
  - deleted `MegastructureParticles`;
  - removed `MegastructureParticles.register()` from common init;
  - deleted `OasisSporeParticle`;
  - deleted `OasisSporeRenderer`;
  - deleted `assets/megastructure/particles/oasis_spore.json`;
  - removed the custom particle factory and renderer registrations from `MegastructureClient`.
- Added `StartupTerminalVulkanRenderer`.
  - It calls `BlackHoleNativeBridge.renderVisualField(...)`, which routes through the existing native Vulkan
    visual-field bridge.
  - The generated Vulkan pixel buffer is converted into a dynamic GUI texture.
  - `ServerStartScreen.renderLaunchGate(...)` draws this Vulkan texture under the terminal text/scanlines.
  - The old CRT overlay is now only a frame/tint/scanline layer around the Vulkan-generated monitor field.
- Verified native DLL availability in the active pack:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\natives\megastructure_blackhole_bridge.dll`.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `786650`, `2026-07-04 00:20:28`.

## 2026-07-03 - Codex CRT terminal timing, rapid errors and corridor continuity guard

Request scope:
- Move terminal text shutdown from 22 seconds to 21 seconds.
- Make the terminal scroll faster, especially during the error phase.
- Render error lines dark red with `[!]` prefix.
- Add an old convex CRT monitor feel to the launch terminal.
- Add stronger protection against corridor dead ends like the screenshot.

Implemented:
- `ServerStartScreen.TERMINAL_SHUTDOWN_TICKS` is now `420` ticks / 21 seconds.
- Normal terminal scroll speed increased from one line per 5 ticks to one line per 3 ticks.
- Error scroll speed increased to two error lines per tick after the 13-second error threshold.
- Error lines now use a dark red palette and begin with `[!]`.
- `ServerStartScreen.renderCrtGlass(...)` adds a CRT-style full-screen treatment:
  - dark green phosphor field;
  - stronger scanlines;
  - edge and corner darkening;
  - subtle horizontal glow bands to imply convex old monitor glass.
- `isApartmentCorridor(...)` no longer limits side corridors to local `12..60` stubs; side lanes now pass through
  their module.
- `isCellCorridor(...)` now keeps corridor air much closer to district borders instead of cutting off 32 blocks
  early.
- `isDeadEndCorridor(...)` now fully crosses its district lane instead of stopping at local `28..DISTRICT_SIZE-28`.
- Added `isCorridorContinuityCut(...)` as a deterministic anti-dead-end safety pass for network, dead-end and
  default/dense-wall corridor districts. It opens continuous district-center spines plus edge pass-throughs on
  periodic corridor axes.
- `isSparseWallCorridor(...)` now generates both a vertical and horizontal through-lane across the whole district
  instead of one lane stopping before the district edge.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Confirmed old active-pack `megastructure-world-0.1.0.jar` is absent.
- Installed jar size/time: `796693`, `2026-07-03 23:56:17`.

## 2026-07-03 - Codex terminal text rewrite, chat persistence and Imperfect_salvation rename

Request scope:
- Remove the visible `/megastructure start` command hint from the terminal.
- Make the terminal feel like an old pure-green console.
- Replace direct lore wording with denser engineering telemetry: pressure, node state, fluid feeds,
  stabilization, phase servos, hydraulic groups and sensor values.
- Keep the terminal visible while chat is open.
- Shut down only terminal text at 22 seconds, while the game reveal still waits for the recording end.
- Rename the mod from `Megastructure World` / `megastructure-world` to `Imperfect_salvation`.

Implemented:
- `ServerStartScreen` no longer prints the command that must be entered; waiting mode now says only that an
  authorized ignition phrase is pending.
- `ServerStartScreen` terminal copy was rewritten into dense Project Eden telemetry:
  - manifold pressure, cryofluid feed, coolant delta-T, node impedance, phase servo drift,
    cage strain, substrate shear, vacuum skirt pressure, hydraulic stroke, carrier phase and quorum values;
  - critical section uses failures in those same systems instead of direct explanations.
- Terminal colors are now green-only, including critical/error lines.
- `TERMINAL_SHUTDOWN_TICKS` is now `440` ticks / 22 seconds.
- `ServerStartChatScreen` now calls the same launch-gate renderer as `ServerStartScreen`, so opening chat no
  longer hides the terminal behind a plain black background.
- `fabric.mod.json` display name is now `Imperfect_salvation`.
- `assets/megastructure/lang/en_us.json` keybind category is now `Imperfect_salvation`.
- `gradle.properties` archive base name is now `Imperfect_salvation`.
- Active installed jar is now:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`
- Removed the old active-pack jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  to avoid two jars with the same Fabric mod id.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size/time: `795868`, `2026-07-03 23:41:58`.

## 2026-07-03 - Codex PROJECT EDEN terminal launch screen and oasis root cleanup

Request scope:
- Replace the launch status panel with a green terminal-like console.
- After `/megastructure start`, show English PROJECT EDEN boot diagnostics.
- Start critical/error spam 13 seconds after launch confirmation.
- Turn off only the terminal text at 20 seconds; reveal the game only when the server_start recording ends.
- Fix the ugly blocky oasis tree roots shown in the screenshot.

Implemented:
- `ServerStartManager.INTRO_DURATION_TICKS` remains `502` ticks / about 25.1 seconds, matching the supplied recording.
- `ServerStartScreen` now renders a full-screen PROJECT EDEN terminal:
  - waiting phase shows a secure terminal prompt and `/megastructure start` hint;
  - intro phase scrolls technical boot lines downward like a console;
  - after `ERROR_START_TICKS = 260` ticks / 13 seconds, it starts emitting critical failures,
    reality-anchor faults, impossible-geometry errors and terminal shutdown messages;
  - after `TERMINAL_SHUTDOWN_TICKS = 400` ticks / 20 seconds, terminal text/scanlines stop rendering,
    but the launch gate stays closed until `INTRO_DURATION_TICKS` finishes;
  - there is no centered gold status panel anymore.
- Regular oasis tree roots in `oasisTreeState(...)` now render only on one low root layer instead of multiple Y levels.
- Regular oasis roots are shorter, thinner, slightly bent, and use fewer root arms, so they should read as ground roots
  rather than square wooden piles.
- Primary rail oasis roots received the same thinning/bending treatment.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`.
- Installed jar size/time: `794481`, `2026-07-03 23:28:14`.

## 2026-07-03 - Codex hotfix for pause blackout, wall ore false positives, corridor stubs and oasis spores

Request scope:
- Fix launch blackout flicker when opening/closing pause screens.
- Stop strange non-ore blocks such as campfires/spore blossoms from appearing in wall ore pockets.
- Remove ugly corridor dead-end stubs.
- Make oasis custom particles visible enough to read in-game.

Implemented:
- `MegastructureClient` now adds a HUD-level black fill while the launch blackout is active and no launch/chat screen owns the frame. This covers the one-frame world flash that can happen after leaving pause/options screens.
- `ServerStartClientState.shouldRenderHudBlackout()` keeps the HUD blackout off the actual launch and launch-chat screens, but active behind pause/options/null-screen frames.
- `DynamicWorldgenPalette.looksLikeOre(...)` now accepts only block ids ending in `_ore` (including deepslate variants). This prevents `spore_blossom`, `*_core_*`, campfire-like/decorative mod blocks and other false positives from entering wall ore veins.
- `isCellCorridor(...)` no longer caps secondary side halls to short local segments, so they run through the cell instead of ending as stubs.
- `isDeadEndCorridor(...)` no longer creates randomized one-sided branch lengths; its generated corridors now run across the district interior.
- `OasisSporeParticleSpawner`, `OasisSporeParticle`, and `OasisSporeRenderer` were strengthened:
  - lower activation thresholds;
  - larger visible particle scale;
  - more vanilla particle emissions;
  - wider hint-based oasis detection;
  - denser custom glowing spore field around the basin.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`.
- Installed jar size/time: `791631`, `2026-07-03 23:18:14`.

## 2026-07-03 - Codex polish pass for startup, tasks, ores, oases, bridges and corridors

Request scope:
- Implement the improvement plan after reviewing the ChatGPT changes.
- Use the first supplied concept as the target for suspended bridge improvements.
- Use the second supplied concept as the target for enclosed industrial corridor/wall-entry improvements.
- Keep the work practical and install the rebuilt jar into the active 1.20.1 Fabric pack.

Implemented - startup and global tasks:
- `ServerStartClientState` now stores the synchronized remaining intro ticks instead of discarding them.
- `ServerStartScreen` now renders a black launch-status panel:
  - `AWAITING LAUNCH CONFIRMATION` while waiting for `/megastructure start`;
  - `SERVER START SEQUENCE` with a progress bar and reveal countdown during the intro recording;
  - chat remains visible on top of the black gate.
- `/megastructure start` still defaults to `ReaperFromHelk`, but the launcher name can now be overridden with:
  `-Dmegastructure.launcher_name=<playerName>`.
- `GlobalTasksScreen` was rebuilt to match the simplified design that the earlier techlog entry claimed:
  - no visible header, active/completed labels, oasis explanation or close hint;
  - only the active task title is centered;
  - task text scales down to fit the panel;
  - bottom-center progress is shown as a minimal `visited / required` counter with a thin progress bar.

Implemented - ores and oases:
- `DynamicWorldgenPalette` now also builds a runtime ore palette from loaded block registry ids that look like ores.
  It excludes ancient debris/netherite and Nether quartz/gold style ores to avoid unsafe/endgame wall spam.
- `MegastructureChunkGenerator` now has a deterministic `wallOreState(...)` pass:
  - only runs in solid wall mass, after air/functional structures/chests are resolved;
  - creates ellipsoid vein pockets instead of single isolated cubes;
  - selects ore blocks from the live runtime palette, so modded ores can appear alongside vanilla ore features.
- Oasis trees are more scenic:
  - regular oasis trees now add deterministic exposed roots;
  - some trees generate fallen trunk segments;
  - crowns get small deterministic gaps instead of perfect blobs;
  - occasional hanging vine strands appear around crown edges;
  - primary rail oasis trees received the same root/fallen trunk/crown-noise treatment.

Implemented - bridges and corridors:
- `bridgeConnectorCorridorState(...)` now has stronger industrial wall-entry language:
  - periodic bulkhead frames;
  - dark side recesses;
  - service pipes/benches along the wall;
  - framed ceiling ribs and lamps.
- `connectorLayerState(...)` now has heavier bulkhead rhythm and darker side recesses, so broad connector corridors better match the enclosed industrial corridor concept.
- `riftSuspensionBridgeState(...)` now reads closer to the suspended-bridge concept:
  - wider deck;
  - edge lamps;
  - underside supports;
  - periodic vertical piers;
  - double overhead cable lines;
  - vertical and diagonal pipe hangers.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `791277`, timestamp `2026-07-03 22:52:44`.

Notes:
- Older lower techlog entries that say Gradle was unavailable are now stale for this workspace; the current verified state is successful build/install.
- Worldgen changes affect newly generated chunks. Existing generated bridge/corridor/oasis/ore chunks must be regenerated or explored in fresh terrain.

## 2026-07-03 - Dynamic oasis trees, modded ores, corridor loot and launch repair

Request scope:
- Oases should contain about 10 trees, and the tree set should include loaded modded trees where possible.
- The user installed many mods and the active pack no longer launched.
- Hostile mobs were not appearing in megastructure worlds.
- Vanilla and modded ores should be embedded into megastructure walls.
- Corridor loot chests should occasionally provide otherwise unavailable survival items, without netherite/endgame valuables.

Implemented:
- Added `DynamicWorldgenPalette`:
  - builds tree material pairs at runtime from loaded `BlockTags.LOGS` and `BlockTags.LEAVES`;
  - pairs vanilla and modded logs/leaves by namespace/species-like id keys, with oak as fallback;
  - builds a runtime ore palette from all loaded ore blocks, excluding Nether quartz/gold style ores;
  - builds a restrained corridor loot item pool from vanilla staples and modded seed/sapling/spore/crystal/shard/gem/dust/fiber/berry-like items.
- Reworked oasis tree placement:
  - regular oases now target 9-11 trees;
  - giant host oases target 10-14 trees;
  - root cathedral and primary rail oasis tree code now uses dynamic tree materials instead of fixed vanilla oak/azalea-only materials;
  - primary rail oases now place 10 deterministic trees around the basin instead of the previous sparse/fixed feel.
- Reworked wall ore generation:
  - `oreOrStone(...)` now selects from the live runtime ore palette, so TechReborn/Create/other loaded mod ores can appear inside megastructure walls without hardcoding every mod id.
- Added corridor loot chests:
  - deterministic sparse chest placement in corridor air cells;
  - avoids protected spawn/route areas and Primary Rift;
  - requires a solid floor and open headroom;
  - fills 3-7 restrained stacks with seeds, amethyst-like crystals/shards, berries, dusts, fibers, basic components, etc.;
  - explicitly avoids netherite, diamonds, creative/spawn eggs, armor/tools and similar endgame/unsafe loot.
- Adjusted `MegastructureMobSystem`:
  - keeps the 15% low spawn-rate design;
  - no longer skips creative players during testing;
  - no longer relies on vanilla natural spawn predicates, because those reject many artificial megastructure floors/light/height contexts;
  - still respects Peaceful difficulty, spectator mode, local mob cap, solid floor and empty collision volume.
- Repaired active pack launch dependencies:
  - installed `Configurable-2.2.3-fabric-yarn+1.20.1.jar` for BetterTrims;
  - installed `pneumonocore-1.3.1+1.20+A.jar` for Gravestones;
  - installed `cicada-lib-0.14.3+1.20.1.jar` for Show Me Your Skin;
  - active Voxy remains `voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.2.jar`;
  - old incompatible Necronomicon/Voxy jars remain disabled.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `313583`, timestamp `2026-07-03 16:41:48`.
- Offline Minecraft launch reached normal client startup/main menu:
  - no Fabric mod-resolution failure after adding Configurable, pneumonocore and cicada;
  - latest startup log contains `Game took 65.37 seconds to start`;
  - no new crash report was produced after the corrected launch dependency pass.
- A later quickplay world-entry attempt stayed at the main menu instead of entering the selected save, so world-entry still needs manual in-game confirmation or a UI-driven test; it did not create a new crash report.

## 2026-07-02 - Volumetric 3D Vulkan effect geometry

Request scope:
- Vulkan effects should not read as flat cards or screen-space overlays.
- Use the Vulkan bridge output as the material for actual world-space 3D volumes/objects.

Implemented:
- Replaced the main `VulkanVisualEffectsRenderer` effect geometry with real 3D primitives:
  - ellipsoid shells for rift/globe/void/reservoir/foundry volumes;
  - torus meshes for rings, halos, orbital bands and volumetric contour layers;
  - cylindrical tube meshes between world-space points for vascular/transit structures.
- Added reusable mesh helpers:
  - `ellipsoidShell(...)`;
  - `torusY(...)`;
  - `torusAroundAxis(...)`;
  - `tubeBetween(...)`;
  - basis/local-point conversion and UV-mapped world-space quad emission.
- The native Vulkan visual-field buffer remains the generated texture/material, but it is now wrapped over these
  3D meshes instead of being drawn as flat vertical/horizontal billboard planes.
- Updated effect profiles:
  - rift: tall layered ellipsoid fracture volume with ring contours;
  - void: nested spherical/flattened shells plus horizontal and vertical torus halos;
  - atom/globe: central shell with multiple inclined orbital torus bands;
  - vascular: actual glowing tubes across the structure volume;
  - reservoir: stacked fluid-like ellipsoid shells and rim torus;
  - foundry: rising volumetric plume shells;
  - transit: tubular corridor-like streaks with ring markers.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `306282`, timestamp `2026-07-02 21:44:10`.

## 2026-07-02 - Vulkan effect pruning, Iris world pipeline, structure mob survival pass

Request scope:
- Remove the unstable Primal/Primary Rift Vulkan effect because it flickered, jumped with view angle, and looked wrong.
- Make shader compatibility keep the Vulkan effect in world-space, not as a HUD/F1 overlay.
- Enable hostile mob presence in megastructure worlds at a low rate.
- Add spider rotten-flesh drops, creeper edible-moss drops, and a hidden per-player eaten-moss counter.

Implemented:
- Removed the direct Primary Rift `VulkanEffectHint` source from `findNearestVulkanEffectHint(...)`.
  Other explicit district effects remain controlled by `vulkanEffectKindForDistrict(...)`.
- Replaced the Iris path with a world-pipeline draw:
  - `MegastructureClient` now registers `VulkanVisualEffectsRenderer.renderIrisWorldPipeline(...)`
    on `WorldRenderEvents.AFTER_ENTITIES`;
  - the Iris path uploads the native Vulkan RGBA buffer into a `NativeImageBackedTexture`;
  - the same world-space quads/volumes are submitted through Minecraft `VertexConsumer` and
    `RenderLayer.getEntityTranslucentEmissive(...)`, so the effect is part of world rendering rather than GUI overlay.
- Added `MegastructureMobSystem`:
  - runs only in worlds using `MegastructureChunkGenerator`;
  - checks every 80 ticks and applies a 15% spawn chance per non-creative, non-spectator player;
  - keeps a small local cap around the player to avoid flooding the structure;
  - spawns a restrained hostile set: zombie, skeleton, spider, creeper.
- Added survival drops:
  - spiders and cave spiders drop rotten flesh;
  - creepers drop `megastructure:edible_moss`.
- Added `EdibleMossItem` with baked-potato food values.
- Added `MossConsumptionState`, a world persistent-state store keyed by player UUID.
  It increments when a player eats edible moss and is intentionally not exposed through commands, UI, or item tooltip.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `301797`, timestamp `2026-07-02 20:13:40`.

## 2026-07-02 - Iris-visible Vulkan effects, Primary Rift locate, oasis infection pass

Request scope:
- Make Vulkan visual effects remain visible with Iris / Complementary Unbound shaders.
- Remove Vulkan effects from the structures where they felt out of place:
  `silent_foundry`, `industrial_wall`, `transit_nexus`, `reservoir_hall`, `tank_cluster`,
  `conduit_basilica`, `machine_root_vault`, `machine_nave`.
- Fix `/locate biome megastructure:primary_rift` not finding Primary Rift.
- Rework oasis overgrowth so moss reads as an aggressive infection, and make the rooted dirt marker
  appear as one 50/50 block under one real oasis tree.

Implemented:
- `VulkanVisualEffectsRenderer` now adds an Iris-aware second emissive world-space pass when Iris is loaded:
  - keeps depth testing and depth writes disabled, so blocks in front still occlude the effect;
  - uses a boosted tint/alpha pass to survive shaderpack tonemapping and bloom better under Complementary Unbound.
- Trimmed `vulkanEffectKindForDistrict(...)` to only the intended special-effect districts:
  - rift: `iris_chasm`;
  - void/lens: `void_altar`, `orbital_web_core`, `abyss`, `descent`;
  - atomic/globe: `atom_storm_array`, `reactor_cathedral`, `globe_monument`.
- Added a guaranteed Primary Rift stripe near spawn and a slightly wider biome-only footprint:
  - actual rift void generation still uses the real rift width;
  - biome lookup gets padding so `/locate biome megastructure:primary_rift` is less likely to miss the stripe sampling.
- Oasis moss infection was strengthened:
  - larger wet/infection radius, especially for giant megacluster hosts;
  - more connected moss veins and branches;
  - wall overgrowth now applies to all oasis host structures, not only giant ones;
  - higher wall strand count and wider moss inserts.
- Static oasis-host audit found that `crown_spire`, `globe_monument`, and `atom_storm_array`
  were allowed as giant oasis hosts but lacked explicit server-side floor/radius/profile/origin mappings.
  Added those mappings to stop those oasis variants from falling back to generic connector settings.
- Rooted dirt marker changed from 20% to 50% per oasis.
- The rooted dirt marker now attaches to the first actually valid generated tree site, so a skipped first tree no longer
  consumes the one-block marker.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `292491`, timestamp `2026-07-02 19:00:23`.

Notes:
- Worldgen changes affect newly generated chunks. Existing generated chunks need regeneration/new terrain to show the new oasis/rift layout.

## 2026-07-02 - Complementary Unbound Vulkan visibility fallback

Request scope:
- Vulkan effects were still completely invisible with Complementary Unbound enabled through Iris.

Root cause:
- The world-space pass was already registered on `WorldRenderEvents.LAST`, but Iris shaderpacks can still run final
  composite/tonemapping after that world event and erase or hide direct GL translucent draws.

Implemented:
- Added `HudRenderCallback` registration for `VulkanVisualEffectsRenderer.renderShaderCompatibilityOverlay(...)`.
- The compatibility overlay only activates when Iris is loaded.
- The overlay reuses the native Vulkan-generated visual texture, but draws it after the shaderpack composite.
- The overlay is projected from the same world-space effect center and radius cached during the world render pass.
- A CPU line-of-sight raycast hides the overlay when the effect center is blocked, preventing obvious through-wall glow
  despite the final pass being HUD-stage.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `297353`, timestamp `2026-07-02 19:27:57`.

Superseded by the next entry:
- This HUD-stage fallback made the effect behave like an overlay and disappear with F1/HUD visibility.
- It was removed in favor of an Iris world-render event split.

## 2026-07-02 - Remove Vulkan HUD fallback, keep Iris rendering world-space

Request scope:
- The Complementary workaround must not be a HUD overlay.
- Vulkan effects must remain the same world-space visuals as without shaders, only visible when shaders are active.

Implemented:
- Removed the `HudRenderCallback` Vulkan compatibility overlay.
- Removed all HUD projection/cache/raycast overlay code from `VulkanVisualEffectsRenderer`.
- Added event split:
  - no Iris: render with the existing `WorldRenderEvents.LAST` path;
  - Iris present: render the same world-space geometry and native Vulkan texture from `WorldRenderEvents.AFTER_TRANSLUCENT`.
- The Iris path is intended to run before shaderpack translucent/composite handling, so the effect stays part of world rendering
  instead of being a GUI/HUD overlay.

Verification:
- `.\gradlew.bat build` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  size `292686`, timestamp `2026-07-02 19:50:46`.

## 2026-07-02 - Working Voxy 1.20.1 active install

Request scope:
- The previous pass only disabled the broken Voxy jar; the pack needs an active working Voxy install.

Root cause:
- The `0.1.5-alpha+mc1.20.1-iris1.7.6-port.1` jar still contained two stale mixins:
  - `sodium.MixinSodiumWorldRender`, whose `drawChunkLayer` descriptor no longer matched Sodium 0.5.13;
  - `minecraft.MixinClientChunkManager`, whose local-variable capture no longer matched Minecraft 1.20.1 `ClientChunkManager.unload`.
- Both were nonessential in the local port:
  - `MixinSodiumWorldRender.cancelRender(...)` did not cancel anything;
  - `MixinClientChunkManager.injectUnload(...)` had the ingest call commented out.

Implemented:
- Removed the two stale mixins from `voxy.mixins.json`.
- Kept the functional Voxy paths:
  - `minecraft.MixinWorldRenderer`;
  - `sodium.MixinDefaultChunkRenderer`;
  - `sodium.MixinRenderSectionManager`;
  - config/debug/Nvidium-guarded mixins.
- Rebuilt Voxy with Java 17:
  `voxy-1.20.4-port/source/gradlew.bat clean build --no-daemon`.
- Installed active jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.2.jar`.
- Left previous broken jar disabled:
  `voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.1.jar.disabled`.

Verification:
- Client startup reached normal menu with Voxy active:
  `Game took 52.889 seconds to start`.
- Quickplay world entry succeeded with Voxy active.
- World log confirmed actual Voxy initialization:
  - `Initializing voxy core`;
  - `Using NvMeshFarWorldRenderer`;
  - `Renderer initialized`;
  - `Voxy core initialized`.
- World loading completed:
  `Async chunk loading for player ReaperFromHelk completed`.
- The old Voxy `MixinSodiumWorldRender`, `MixinClientChunkManager`, `class_631`, and `CcaEntityClient` failure loop did not return.

## 2026-07-02 - 100% world-load hang fix and Create stack install

Request scope:
- Minecraft again froze at 100% while entering the world.
- Install the previously missing 1.20.1 Fabric mods, except Create Aeronautics:
  Create, Steam 'n' Rails, Create Crafts & Additions, Create Slice & Dice, Create New Age, Farmer's Delight, Simply Swords, Vista, Fantasy Armor, Zipline, Just Hammers, Sophisticated Storage.

Root cause:
- `latest.log` showed the first real failure at world join:
  `voxy.mixins.json:minecraft.MixinClientChunkManager` failed applying to `net.minecraft.class_631::method_2859`.
- After that, Minecraft stayed on the 100% world loading screen and emitted secondary client errors from WTHIT/CCA/player/world-null packet handlers.
- Older crash report `crash-2026-07-02_14.06.19-client.txt` also confirmed the Voxy port is incompatible with the installed Sodium 0.5.13 method signatures.

Implemented:
- Disabled the active Voxy port:
  `voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.1.jar.disabled`.
- Installed 1.20.1 Fabric-compatible Modrinth versions:
  - `create-fabric-6.0.8.1+build.1744-mc1.20.1.jar`;
  - `Steam_Rails-1.7.2+fabric-mc1.20.1.jar`;
  - `createaddition-fabric+1.20.1-1.3.4.jar`;
  - `sliceanddice-fabric-3.5.2.jar`;
  - `create-new-age-1.2.0+fabric-mc1.20.1.jar`;
  - `FarmersDelight-1.20.1-2.4.1+refabricated.jar`;
  - `simplyswords-fabric-1.56.0-1.20.1.jar`;
  - `fantasy_armor-fabric-1.2.4-1.20.1.jar`;
  - `zipline-1.1.3+1.20.1.jar`;
  - `justhammers-fabric-20.1.5+mc1.20.1.jar`;
  - required dependency `architectury-9.2.14-fabric.jar`.
- Left existing active mods in place:
  - `sophisticatedstorage-1.20.1-1.3.5.11.142.jar`;
  - `vista-1.20-3.1.3-fabric.jar`.
- The previous disabled Fantasy Armor copy remains as `.jar.disabled`; the active `.jar` is the one Fabric loads.

Verification:
- Manual Java argfile launch reached normal client startup after the install:
  `Game took 63.039 seconds to start`.
- Create and Create Crafts & Additions initialized successfully in `latest.log`.
- No Voxy mixin failure appeared after disabling the Voxy jar.
- Automated quickplay world entry was blocked by Replay Mod's partial recording recovery prompt, not by a Fabric dependency error. If a future run still hangs at 100%, inspect new log lines before any CCA null spam.

## 2026-07-02 - Ventilation Canyon Vulkan removal and oasis locate fix

Request scope:
- Remove the Vulkan visual-field effect from `ventilation_canyon`; it does not fit that district.
- Fix `/megastructure locate_oasis`, which was showing Minecraft's generic red `An unexpected error occurred trying to execute that command`.

Implemented:
- Removed `DISTRICT_VENTILATION_CANYON` from `vulkanEffectKindForDistrict(...)`.
  - `DISTRICT_IRIS_CHASM` still uses the rift/canyon Vulkan effect.
  - Primary rifts still use their direct primary-rift effect path.
- Reworked guaranteed-oasis selection to use the active world variant seed instead of a static class-load-time key.
  - Added `findGuaranteedOasisKey(long variantSeed)`.
  - Server-side oasis descriptors now compare against `findGuaranteedOasisKey(worldVariantSeed)`.
  - Client/render oasis hints now compare against `findGuaranteedOasisKey(activeWorldVariantSeed)`.
  - Nearby-priority suppression now checks the guaranteed oasis key for the same seed path.
- Wrapped `/megastructure locate_oasis` in a guarded command path:
  - real exceptions are logged through SLF4J;
  - the player receives `Failed to locate oasis: <ExceptionType> - <message>` instead of a generic command failure.

Build/install/verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed `megastructure-world-0.1.0.jar`:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  - size: `291526`;
  - installed time: `2026-07-02 14:15:48`.
- Offline Minecraft launch verification reached normal client startup with the new jar.
- Automated verification did not execute `/megastructure locate_oasis` inside an interactive world; if the command still fails, the next log/chat output should now contain the concrete exception instead of only the generic red message.

## 2026-07-02 - Globe Monument volumetric Vulkan effect, Distant Horizons install, Voxy 1.20.1 port

Request scope:
- Keep Globe Monument's existing Vulkan visual motif, but make it read as volumetric instead of a flat plane.
- Change Globe Monument block palette to a gold-hued but non-valuable material set.
- Install Distant Horizons for the 1.20.1 Fabric pack.
- Port the existing Voxy 1.20.4 port to 1.20.1 Fabric and verify that the pack starts.
- Provide an up-to-date list of megastructure districts using the Vulkan visual-field renderer.

Implemented in `megastructure-world`:
- Extended `MegastructureChunkGenerator.VulkanEffectHint` with a `volumetric` flag.
- `findNearestVulkanEffectHint(...)` now marks only `DISTRICT_GLOBE_MONUMENT` as volumetric while preserving the existing `kind=2` atom/plasma visual family.
- `VulkanVisualEffectsRenderer.drawAtom(...)` now routes volumetric hints to `drawVolumetricGlobe(...)`.
- Added `drawVolumetricGlobe(...)`:
  - layered vertical X/Z planes;
  - diagonal cross-planes;
  - multiple horizontal interior bands;
  - warmer tint modulation for the Globe Monument field.
- Added `verticalDiagonal(...)` helper for non-axis-aligned translucent world-space quads.
- Added Globe-specific non-valuable ochre palette entries:
  - `GLOBE_SHELL`: `neepmeat:yellow_rough_concrete`, fallback `minecraft:yellow_terracotta`;
  - `GLOBE_PANEL`: `neepmeat:yellow_tiles`, fallback `minecraft:yellow_concrete`;
  - `GLOBE_RIB`: `neepmeat:smooth_tile_orange`, fallback `minecraft:orange_terracotta`;
  - `GLOBE_SUPPORT`: `neepmeat:rusty_metal_sheet`, fallback `minecraft:brown_terracotta`.
- Reworked `globeMonumentStructureState(...)` to use the Globe palette for the base plate, supports, braces, shell panels, ribs, equator ring and central pedestal.

Installed mods:
- Installed Distant Horizons `3.1.2-b-1.20.1` from Modrinth:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\DistantHorizons-3.1.2-b-1.20.1-fabric-forge.jar`
  - size: `29494329`;
  - installed time: `2026-07-02 13:50:29`.
- Ported and installed Voxy:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\voxy-0.1.5-alpha+mc1.20.1-iris1.7.6-port.1.jar`
  - size: `68730440`;
  - installed time: `2026-07-02 14:07:18`.

Voxy 1.20.1 port details:
- `gradle.properties` moved to Minecraft `1.20.1`, Yarn `1.20.1+build.10`, Fabric Loader `0.19.3`, Fabric API `0.92.9+1.20.1`.
- Voxy mod version is now `0.1.5-alpha+mc1.20.1-iris1.7.6-port.1`.
- `build.gradle` updates:
  - Fabric Loom `1.10.5`;
  - Gradle wrapper `8.12`;
  - Sodium `mc1.20.1-0.5.13-fabric`;
  - Iris `1.7.6+1.20.1`;
  - Cloth Config `11.1.136+fabric`;
  - Mod Menu `7.2.2`;
  - Starlight `1.1.2+1.20`;
  - Vivecraft compile-only `1.20.1-1.3.13-fabric`;
  - Chunky `1.3.146`.
- Fabric rendering data attachment now comes from the selected Fabric API module instead of a pinned 1.20.4 artifact.
- `fabric.mod.json` now depends on Minecraft `1.20.1`, Fabric API `>=0.92.0`, Cloth Config `>=11`.
- Code compatibility fixes:
  - replaced 1.20.4 `NbtTagSizeTracker.ofUnlimitedBytes()` usage with 1.20.1 `NbtIo.readCompressed(InputStream)`;
  - replaced `NbtIo.readCompound(DataInputStream)` with `NbtIo.read(DataInput)`;
  - removed unavailable `ServerInfo.isRealm()` branch;
  - replaced `org.apache.commons.lang3.stream.Streams.of(...)` usage with `Arrays.stream(...)`;
  - updated `MixinSodiumWorldRender` injection signature from `MatrixStack` to Sodium `ChunkRenderMatrices` for Sodium `0.5.13`.

Vulkan visual-field district coverage:
- Primary rifts: rift membrane effect.
- `ventilation_canyon`, `iris_chasm`: rift/canyon effect.
- `void_altar`, `orbital_web_core`, `abyss`, `descent`: void field.
- `atom_storm_array`, `reactor_cathedral`, `globe_monument`: atom/plasma field; `globe_monument` now uses the volumetric variant.
- `conduit_basilica`, `machine_root_vault`, `machine_nave`: vascular pulse field.
- `reservoir_hall`, `tank_cluster`: fluid haze.
- `silent_foundry`, `industrial_wall`: heat/ember field.
- `transit_nexus`: induction streak field.

Build/install/verification:
- `.\gradlew.bat build` in `C:\Users\nikit\Documents\New project` => SUCCESS.
- Installed `megastructure-world-0.1.0.jar`:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`
  - size: `291104`;
  - installed time: `2026-07-02 13:49:15`.
- Voxy build command with Java 17:
  `.\gradlew.bat build` in `voxy-1.20.4-port\source` => SUCCESS.
- Minecraft offline launch verification:
  - `Loading 249 mods`;
  - `distanthorizons 3.1.2-b`;
  - `megastructure 0.1.0`;
  - `voxy 0.1.5-alpha+mc1.20.1-iris1.7.6-port.1`;
  - `Game took 42.058 seconds to start`.
- Test process was stopped after successful client startup.
- Native DLL was not rebuilt in this step; installed/native bridge remains the 2026-06-29 build.

Notes:
- The Minecraft launch test used a generated offline command and a classpath built strictly from `1.20.1.json` plus `fabric-loader-0.19.3-1.20.1.json`.
- An earlier test attempt failed because using every jar under `libraries` pulled in a wrong `authlib`; that was a test-command issue, not a modpack issue.
- Voxy's old disabled jar remains available as a fallback: `voxy-0.1.5-alpha.jar.disabled`.

## 2026-06-29 - Vulkan visual-field renderer and black-hole reactor pause

Goal:
- Put the Vulkan renderer to practical use beyond the paused black-hole idea.
- Keep the final image visible under Iris/Complementary by composing it as depth-tested world-space geometry.
- Stop new random `black_hole_reactor` districts from spawning while the concept is parked.

Implemented:
- Added `native/blackhole_bridge/shaders/visual_field.comp`, a second Vulkan compute shader that generates animated RGBA visual-field atlases:
  - rift membranes;
  - void/altar fields;
  - atom-storm plasma arcs;
  - vascular/conduit pulses;
  - reservoir fluid haze;
  - foundry heat/embers;
  - transit induction streaks.
- Generated `native/blackhole_bridge/src/visual_field_comp_spv.h` from the SPIR-V output.
- Extended `native/blackhole_bridge/src/blackhole_bridge.cpp` with:
  - a second compute pipeline for the visual-field shader;
  - JNI entrypoint `BlackHoleNativeBridge.renderVisualField0(...)`;
  - shared Vulkan storage-buffer output path reused from the existing native bridge.
- Added `VulkanVisualEffectsRenderer`:
  - asks Vulkan for a 512x512 animated atlas only when the player is near a supported district/rift;
  - uploads the atlas into a GL texture with safe pixel-unpack state;
  - renders multiple world-space translucent/emissive planes with depth test enabled and depth writes disabled;
  - restores blend/depth/cull/shader/texture state after drawing for Iris/Sodium/Voxy compatibility.
- Added `MegastructureChunkGenerator.VulkanEffectHint` and `findNearestVulkanEffectHint(...)`:
  - primary rifts get anchored rift-membrane effects;
  - `void_altar`, `orbital_web_core`, abyss/descent districts get void fields;
  - `atom_storm_array`, `reactor_cathedral`, `globe_monument` get plasma/orbit fields;
  - `conduit_basilica`, `machine_root_vault`, `machine_nave` get vascular pulse fields;
  - `reservoir_hall`, `tank_cluster` get fluid haze;
  - `silent_foundry`, `industrial_wall` get heat/ember layers;
  - `transit_nexus` gets induction streaks.
- `MegastructureClient` now registers `VulkanVisualEffectsRenderer`.
- `BlackHoleReactorRenderer` is no longer registered by default; it only runs with `-Dmegastructure.blackhole.enabled=true`.
- New random district selection no longer returns `DISTRICT_BLACK_HOLE_REACTOR`; the old roll range is redistributed between `void_altar` and `atom_storm_array`.

Build/install:
- `tools\glslang\bin\glslangValidator.exe -V native\blackhole_bridge\shaders\visual_field.comp` => SUCCESS.
- `.\gradlew.bat compileJava` => SUCCESS.
- Native CMake/MSVC build through VS 18 Insiders `VsDevCmd.bat` => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\megastructure-world-0.1.0.jar`.
- Installed native DLL to `C:\Users\nikit\Desktop\Project Imperfect Salvation\natives\megastructure_blackhole_bridge.dll`.
- Installed jar/DLL SHA-256 hashes match their build outputs.

Notes:
- This is intentionally not a fullscreen post-process. Vulkan generates the animated atlas; Minecraft draws it as ordinary world-space translucent geometry so Complementary/Iris should see it.
- Existing already-generated `black_hole_reactor` chunks can still exist in old worlds, but new random district generation should not select that district anymore.

## 2026-06-29 - black-hole line-of-sight occlusion guard

Request scope:
- Keep the black hole as a real world-space reactor object.
- Make sure it does not visibly bleed through blocks.

Implemented:
- Added a client-side collision raycast visibility guard before the native/Vulkan black-hole renderer runs.
- The renderer now samples line of sight to:
  - the core center;
  - visible shadow-radius edge points in camera space;
  - major accretion-disk edge points in world space.
- If all samples are blocked by collision geometry, the black-hole pass is skipped entirely.
- The existing world-space GL depth test remains active; this guard is an additional fallback for cases where a late render pass or shaderpack depth state would otherwise allow the effect to show through walls.
- Close-range rendering is still allowed when the camera is inside the visible shadow/capture volume, so falling into the core does not make the effect vanish.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Installed jar SHA-256 matches `build\libs\megastructure-world-0.1.0.jar`.
- Native DLL unchanged in this step.

## 2026-06-29 - black-hole depth occlusion and no billboard layer

Request scope:
- User rejected any remaining screen/card-like black-hole rendering.
- The black hole must be fully rebuilt as a world-space object and must not show through blocks.

Implemented:
- Removed the remaining camera-facing billboard layer (`Mode 7`) from `BlackHoleNativeBridge`.
- Removed billboard VAO/VBO allocation and the quad draw path.
- The black-hole render now consists only of world-space geometry anchored to the reactor core:
  - main accretion disk mesh;
  - upper/lower lensed disk meshes;
  - outer lens/infall shells;
  - visible black-hole shadow sphere.
- Re-enabled depth testing for the entire black-hole composite pass and forces `GL_LEQUAL` while drawing it.
- Restores the previous depth function and GL state afterward.
- Result: solid blocks with valid depth should occlude the black-hole render; it is no longer forced over walls.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to the Void RP modpack.
- Installed jar SHA-256 matches `build\libs\megastructure-world-0.1.0.jar`.
- Native DLL unchanged in this step; Vulkan compute backend remains the installed 15:42 build.

## 2026-06-29 - remove screen-space black-hole pass, restore fixed world-space object

Request scope:
- The previous screen-space lens pass was not acceptable: the black hole must not be an effect on the screen.
- The black hole must stay at its exact world-space reactor core position.
- Rework from the attached `black_hole_vulkan_world_space_ru.docx` principle: `centerWS` is primary; screen coordinates are only a projection of a real world object.

Implemented:
- Removed the fullscreen/screen-space lens pass from `BlackHoleNativeBridge`:
  - removed framebuffer copy sampling;
  - removed screen UV warp shader;
  - removed screen lens program/resources;
  - removed the per-frame `renderScreenSpaceLens` call.
- Kept only geometry anchored to `CoreRelative`, i.e. the black-hole reactor center in world coordinates.
- Restored world-space accretion-disk geometry passes:
  - main disk;
  - upper lensed image;
  - lower lensed image.
- Kept the Vulkan compute output as a world-space black-hole image layer placed at the core, not as a fullscreen post-effect.
- Kept the visible shadow radius at `2.60 * eventHorizonRadius` to match the black-hole shadow scale from the document/reference material.
- Kept Complementary visibility by rendering the final anchored object in the late world-render pass with restored GL state afterward.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to the Void RP modpack.
- Native DLL was not rebuilt in this step because the Vulkan compute shader did not change; installed DLL SHA-256 still matches `native\blackhole_bridge\build\megastructure_blackhole_bridge.dll`.

## 2026-06-29 - shaderpack-visible Vulkan black-hole rewrite and scaffold cleanup

Request scope:
- Make the black hole visible with Complementary Unbound.
- Stop spawning the screenshot block `Rusted Metal Scaffolding` inside walls.
- Rework the black hole from the visual model upward using black-hole optics references: shadow larger than the event horizon, photon ring, lensed upper/lower accretion disk images, Doppler-bright side, and background bending.

References checked:
- NASA SVS 13326, Black Hole Accretion Disk Visualization.
- DNGR / Interstellar black-hole lensing papers and video-reference notes from the user-supplied YouTube link/context.

Implemented:
- `BlockPalette.GRATE` now uses `neepmeat:rusty_metal_sheet` instead of `neepmeat:rusted_metal_scaffold`, so future wall/trim generation no longer produces scaffolding blocks in walls.
- Added a screen-space gravitational lens pass in `BlackHoleNativeBridge`:
  - copies the already-rendered Minecraft/Complementary frame into a GL texture;
  - bends UV sampling around the projected black-hole center;
  - darkens the central shadow and adds restrained photon/caustic color at the lens boundary;
  - disables depth test only for this final black-hole pass, then restores previous GL state.
- Reworked the primary visible black-hole layer:
  - old procedural disk mesh draws are removed from the main path;
  - Vulkan compute output is now placed as a camera-facing world-space billboard;
  - the final shadow sphere uses the visible black-hole shadow scale (`~2.6 * horizon`) instead of a small event-horizon-sized ball.
- Reworked `blackhole_dngr.comp`:
  - normalized the virtual camera distance so the rendered object remains visible at gameplay distances;
  - stronger light bending and frame dragging;
  - wider/hotter accretion disk;
  - stronger relativistic Doppler asymmetry;
  - larger shadow, brighter photon ring and secondary lensed arc.

Verification:
- `tools\glslang\bin\glslangValidator.exe -V native\blackhole_bridge\shaders\blackhole_dngr.comp` => SUCCESS.
- Regenerated `native\blackhole_bridge\src\blackhole_dngr_comp_spv.h`.
- Native CMake build with `--clean-first` => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to the Void RP modpack.
- Installed native bridge to the Void RP modpack; SHA-256 matches the rebuilt native DLL.
- `megastructure_blackhole_bridge.dll.old_locked` may remain until all Java/Minecraft processes release the old mapped DLL, but the active filename was replaced.

## 2026-06-29 - black-hole halo restore, center stair removal and conduit generation cleanup

Request scope:
- Add load-time update support for plain `neepmeat:vascular_conduit` as well as encased conduit.
- Stop generating `neepmeat:encased_vascular_conduit` / `neepmeat:vascular_conduit` as hidden wall/decorative blocks.
- Remove the central spiral/stair shaft from the black-hole reactor generation entirely.
- Restore the black-hole halo without bringing back the orange filled event-horizon artifact.
- Make the black-hole space distortion read more like a surrounding gravitational lens/warped spacetime shell.

Implemented:
- `BlockPalette.PIPE` now uses `neepmeat:rusty_column`; `BlockPalette.RUST_PIPE` now uses `neepmeat:rusty_vent`. These are simple NeepMeat decorative blocks, not vascular network blocks.
- Kept `LoadedChunkBlockUpdater` support for both `vascular_conduit` and `encased_vascular_conduit`, so already-generated old chunks still get a one-time neighbor/block-entity refresh on load.
- Expanded the black-hole core exclusion before all worldgen overlays:
  - central vertical column is air through the reactor district;
  - core sphere is larger;
  - equatorial disk void is slightly wider.
- Strengthened the Vulkan compute image:
  - brighter photon ring;
  - stronger Einstein/caustic ring;
  - additional lens halo;
  - close-range outer warp band driven by `fall`.
- Strengthened the 3D world-space composite shell:
  - restored a visible warm halo at the event-horizon edge;
  - added a large outer lens shell before the inner shell;
  - kept the horizon body black instead of orange-filled.

Verification:
- `tools\glslang\bin\glslangValidator.exe -V native\blackhole_bridge\shaders\blackhole_dngr.comp` => SUCCESS.
- Regenerated `native\blackhole_bridge\src\blackhole_dngr_comp_spv.h`.
- Native CMake build with `--clean-first` => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Installed jar to `C:\Users\nikit\Desktop\Vazhny hlam\Sborka Void RP\mods\megastructure-world-0.1.0.jar` (actual Windows path contains Cyrillic).
- Installed native bridge to `C:\Users\nikit\Desktop\Vazhny hlam\Sborka Void RP\natives\megastructure_blackhole_bridge.dll` (actual Windows path contains Cyrillic).
- SHA-256 of installed native DLL matches the rebuilt native DLL.
- A stale `megastructure_blackhole_bridge.dll.old_locked` copy may remain until Minecraft/Java fully releases the old mapped DLL; it is not the filename loaded by the mod.

## 2026-06-29 - black-hole orange horizon, disk seam and conduit load refresh

Request scope:
- The previous layering fix removed the overlap but made the event horizon look like an orange shaded ball.
- The accretion disk had a visible seam/wrong twist sector.
- The black hole still did not visibly bend space around the sphere or start pulling/twisting space early enough when approaching.
- Generated `neepmeat:encased_vascular_conduit` still looked wrong after spawn/load until updated.

Implemented:
- Fixed the horizon-facing vector in `BlackHoleNativeBridge`; the front of the sphere is no longer treated as the photon rim.
- Reduced the event-horizon photon tint so the sphere stays near-black and only the limb gets a subtle ring.
- Removed disk dependence on seam-prone mesh UV sampling from the Vulkan frame; disk color/alpha now use local position, radial distance and continuous sin/cos angular terms.
- Added a dark/blue lens-bubble shell (`Mode 6`) around the horizon to make space around the sphere visibly compressed and displaced.
- Reworked the fall parameter so world-space shell twisting starts within roughly ten event-horizon radii, not only at near-contact range.
- Passed this fall value into the native Vulkan compute path through `palette.w`; the compute shader now uses it for infall twist and tunnel-band intensity.
- `LoadedChunkBlockUpdater` now refreshes vascular block entities on chunk load:
  - marks the block entity dirty;
  - invokes NeepMeat encased-conduit `onNeighbourUpdate` reflectively when present;
  - recreates/re-adds the conduit if its NeepMeat network is null;
  - forces a client redraw with `REDRAW_ON_MAIN_THREAD`.

Verification:
- `tools\glslang\bin\glslangValidator.exe -V native\blackhole_bridge\shaders\blackhole_dngr.comp` => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Native CMake build with `--clean-first` => SUCCESS.
- Installed updated jar to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Installed updated native bridge to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\natives\megastructure_blackhole_bridge.dll`.

## 2026-06-29 - black-hole fluctuations and close infall distortion

Request scope:
- After the artifact/crash fix, add subtle black-hole fluctuations.
- When approaching/falling into the black hole, make space appear increasingly twisted and dragged inward.

Implemented:
- `BlackHoleReactorRenderer` now generates smooth interpolated fluctuation values instead of abrupt per-frame random flicker.
- `BlackHoleNativeBridge` passes fluctuation/fall uniforms into the world-space 3D compositor.
- The event horizon keeps an opaque shadow but its photon rim now has a subtle time-varying pulse.
- The accretion disk and lensed upper/lower disk images now shimmer slightly through Doppler/filament modulation.
- The outer caustic shell now bends and breathes with fluctuation and becomes more twisted as the camera approaches the capture zone.
- Added a close-range 3D infall shell (`Mode 5`) around the horizon, rendered only when `inside > 0.02`, with additive spiral/shear bands to imply space being dragged inward.
- `blackhole_dngr.comp` now applies Vulkan-side infall twist to the generated ray image when close to the horizon and adds a moving tunnel band into the computed frame.
- Regenerated embedded SPIR-V header `native/blackhole_bridge/src/blackhole_dngr_comp_spv.h` from the GLSL shader.

Verification:
- `tools\glslang\bin\glslangValidator.exe -V native\blackhole_bridge\shaders\blackhole_dngr.comp` => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Native CMake build with `--clean-first` => SUCCESS, rebuilt and relinked `megastructure_blackhole_bridge.dll`.
- Installed updated jar to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Installed updated native bridge to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\natives\megastructure_blackhole_bridge.dll`.

## 2026-06-29 - black-hole layering artifact and native upload crash fix

Request scope:
- Fix the visible horizontal/layered artifact across the black-hole sphere at some camera angles.
- Inspect the latest crash log after the black-hole renderer run.

Findings:
- `latest.log` loaded `megastructure_blackhole_bridge.dll`, then reported the native Vulkan renderer as unavailable for a frame.
- The JVM fatal error log `hs_err_pid44936.log` crashed in the NVIDIA OpenGL driver:
  - native frame: `nvoglv64.dll+0xb9e4e0`;
  - Java stack: `GL11C.nglTexSubImage2D` -> `GL11C.glTexSubImage2D` -> `BlackHoleNativeBridge.composeNativeFrame`.
- This points at the final Minecraft/OpenGL texture upload step, not a normal Java exception and not a NeepMeat worldgen crash.
- Likely cause: inherited GL pixel-unpack state from Minecraft/Sodium/Iris (`GL_UNPACK_ROW_LENGTH`, skip rows/pixels or alignment) made the driver read the Vulkan frame buffer with the wrong row layout.
- The visible stripes were caused by sampling the Vulkan disk image on the event-horizon sphere itself; at grazing angles the sphere surface showed horizontal texture rows instead of remaining a true opaque shadow.

Implemented:
- `BlackHoleNativeBridge.composeNativeFrame` now saves and restores GL pixel unpack state, then forces safe upload state before `glTexSubImage2D`:
  - `GL_UNPACK_ALIGNMENT = 1`;
  - `GL_UNPACK_ROW_LENGTH = 0`;
  - `GL_UNPACK_SKIP_PIXELS = 0`;
  - `GL_UNPACK_SKIP_ROWS = 0`.
- The event-horizon sphere no longer samples the Vulkan color texture as a surface image.
- The horizon pass is rendered as an opaque near-black 3D sphere with only a narrow photon-ring tint, with blending disabled, depth writes enabled and back-face culling enabled for that mesh.
- Native Vulkan fence wait was raised from 16 ms to 100 ms so slower compute frames are less likely to be marked unavailable during a render spike.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Native CMake build with Visual Studio 18 Insiders tools => SUCCESS, rebuilt `megastructure_blackhole_bridge.dll`.
- Installed updated jar to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Installed updated native bridge to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\natives\megastructure_blackhole_bridge.dll`.

Known follow-up:
- `Vascular Conduit ... has a null network` warnings remain in the log. They are not the native renderer crash, but the later NeepMeat generation pass should avoid or repair generated active vascular-network blocks.
- The requested light fluctuations and close-range fall/distortion pass is next after this crash/artifact fix.

## 2026-06-29 - Vulkan black-hole 3D world-volume compositor and rusty lamp restore

Request scope:
- Replace generated `ceiling_light` lamps with enabled `neepmeat:rusty_metal_light`.
- Rework the black-hole display so the normal path is Vulkan-driven and appears as a 3D object in the world, not a flat screen-limited image.

Implemented:
- `BlockPalette.LAMP` is now `neepmeat:rusty_metal_light` with `lit=true`.
- Chunk-load repair now keeps existing `rusty_metal_light` blocks lit and migrates generated `ceiling_light` blocks to the lit rusty-metal lamp.
- The default black-hole path no longer calls the old OpenGL lensing/volume fallback.
- `BlackHoleReactorRenderer` was reduced to the Vulkan route only; the old Java/OpenGL lensing and volume implementation was removed from the active renderer class.
- `BlackHoleNativeBridge` now composes the Vulkan compute result onto world-space 3D meshes:
  - opaque event-horizon sphere;
  - large caustic/lensing shell;
  - primary accretion disk;
  - upper and lower lensed disk images bent around the shadow.
- The Vulkan compute shader remains the source of the black-hole frame; Minecraft/OpenGL is only used as the unavoidable final composition layer into the existing Minecraft renderer.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar to `C:/Users/nikit/Desktop/Р’Р°Р¶РЅС‹Р№ С…Р»Р°Рј/РЎР±РѕСЂРєР° Void RP/mods/megastructure-world-0.1.0.jar`.

Follow-up NeepMeat generation analysis:
- NeepMeat exposes 441 blockstates; useful families include rough concrete, dirty/smooth tiles, rusty/polished/meat-steel metals, metal scaffolds, ladders/rungs/vents/trapdoors, tanks, fluid pipes, vascular conduits, machine blocks, motor/fan/pylon/power blocks, reactor wastes and blood/meat blocks.
- The generator currently routes most materials through a small global `BlockPalette`; next implementation should add themed sub-palettes and apply them per district rather than replacing one global material everywhere.
- Best district mapping:
  - corridor/dense-wall network: dirty tiles, rough concrete, rusty light, rusty vents/rungs;
  - scaffold/industrial/foundry/lift: rusted/yellow/blue metal scaffolds, rusty sheets, ladders, fans, motors;
  - conduit basilica/machine-root/orbital web/void altar/black-hole reactor: encased/vascular conduits, vascular sensors/condensers, meat-steel and power/pylon blocks;
  - reservoir/tank/oasis: tanks, reinforced/clear tank walls, fluid gauges/meters, fluid pipes, contaminated dirt/blood-bubble vegetation;
  - suspended/folded/rim city: asbestos/dirty-white/yellow tile variants, doors/trapdoors, reinforced glass;
  - reactor/atom storm/black-hole reactor: active waste, pylon/power emitter, machine blocks, lit rusty-metal lights.

## 2026-06-29 - fix world-load stall at 100 percent

Request scope:
- World loading reaches 100 percent and does not enter the world.
- Inspect latest launch log and the suspicious chunk-generation map colors.

Findings:
- Latest log reaches `Starting integrated minecraft server`, `Enforcing safe world random access`, and `Changing watch distance to 10`, then has no world-entry or crash output until manual shutdown.
- The loading-screen colors are vanilla chunk-status colors:
  - `#999999` = structure starts;
  - `#80B252` = biomes;
  - `#303572` = carvers;
  - `#FFE0A0` = light;
  - `#FFFFFF` = full.
- The screen was showing chunk-generation progress, not a block texture/color value.
- The likely stall was the synchronous `LoadedChunkBlockUpdater` scan doing full chunk scans and block/neighbor updates inside `ServerChunkEvents.CHUNK_LOAD`.

Implemented:
- `ServerChunkEvents.CHUNK_LOAD` now only queues megastructure chunks.
- Actual water/lamp/conduit repair is processed later from `ServerTickEvents.END_WORLD_TICK` with a budget of one chunk per tick.
- Queued chunks are skipped if they have unloaded before processing, using non-generating `getWorldChunk(..., false)`.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar to `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`.

## 2026-06-29 - generator lamp artifact mitigation

Request scope:
- Investigate visible square artifact after enabling generated NeepMeat lights.

Implemented:
- The generator palette now uses lit `neepmeat:ceiling_light` for procedural `LAMP` placements instead of full-cube `neepmeat:rusty_metal_light`.
- This keeps generated lighting on without turning every light marker into a large cube with the `rusty_metal_light_on` texture on all six sides.
- The chunk-load repair replaces legacy generated `rusty_metal_light` blocks with the new lit generator lamp in megastructure worlds, and keeps `ceiling_light` lit after loading.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar to `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`.

## 2026-06-29 - chunk-load repair for NeepMeat lamps, water and conduits

Request scope:
- Make generated `rusty_metal_light` blocks lit instead of spawning off.
- On chunk load, update clipped water so it starts flowing.
- On chunk load, update `Encased Vascular Conduit` once so generated conduits stop spawning with stale/crooked connection state.

Implemented:
- Added a transformed NeepMeat block lookup path so `BlockPalette.LAMP` uses `rusty_metal_light[lit=true]` for new generation.
- Added `LoadedChunkBlockUpdater`, registered through `ServerChunkEvents.CHUNK_LOAD`.
- The chunk-load pass is restricted to worlds using `MegastructureChunkGenerator`.
- On each loaded megastructure chunk:
  - water fluid ticks are scheduled and neighbors are notified;
  - existing `rusty_metal_light` states are forced to `lit=true`;
  - `vascular_conduit` and `encased_vascular_conduit` states are recomputed through NeepMeat's own neighbor-shape logic and neighbor updates are emitted.
- The pass uses immutable block positions for scheduled updates to avoid mutable-position corruption during chunk scans.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar to `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`.

## 2026-06-29 - Vulkan compute DNGR-style black-hole renderer from scratch

Request scope:
- Stop relying on the OpenGL 3D/lensing renderer as the black-hole implementation.
- Rebuild the visual path around Vulkan itself, using Interstellar/DNGR-style visual targets.

Implemented:
- Added `native/blackhole_bridge/shaders/blackhole_dngr.comp`, a Vulkan compute shader that renders the black hole per pixel.
- Added generated SPIR-V include `native/blackhole_bridge/src/blackhole_dngr_comp_spv.h`.
- Replaced the native transfer-command ellipse renderer with a real Vulkan compute pipeline:
  - shader module from embedded SPIR-V;
  - descriptor set layout with storage buffer output;
  - compute pipeline layout with push constants;
  - `vkCmdBindPipeline`, `vkCmdPushConstants`, `vkCmdDispatch`;
  - shader-write to host-read barrier before JNI copies pixels.
- The compute shader approximates the visual anatomy targeted by DNGR/Interstellar:
  - curved light-ray integration around the shadow;
  - Kerr-like frame dragging/spin term;
  - event-horizon capture/shadow;
  - photon-ring and Einstein/caustic enhancement;
  - thin accretion disk crossings, including secondary lensed hits;
  - Doppler beaming and gravitational redshift in the disk color.
- `BlackHoleNativeBridge` now requests a 512x512 Vulkan-generated frame.
- `BlackHoleReactorRenderer` now uses the Vulkan native renderer first and returns after it succeeds.
- The OpenGL 3D/lensing renderer is no longer the default implementation; it only runs if Vulkan fails and `-Dmegastructure.blackhole.openglFallback=true` is explicitly set.
- Downloaded Khronos `glslangValidator` into `tools/glslang/bin` only as a build-time shader compiler; the compiled SPIR-V is embedded in the DLL source path.

Verification:
- `glslangValidator -V native/blackhole_bridge/shaders/blackhole_dngr.comp` => SUCCESS.
- Native DLL build through Visual Studio CMake/NMake environment => SUCCESS.
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar and DLL to the Void RP modpack:
  - `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`;
  - `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/natives/megastructure_blackhole_bridge.dll`.

## 2026-06-28 - physically guided 3D black-hole visual rewrite

Request scope:
- Rework the black hole into a 3D, volumetric, more physically recognizable visual.
- Make nearby space appear distorted and increasingly twisted as the player approaches.

Implemented:
- `BlackHoleReactorRenderer` now uses the 3D world-space renderer as the default path.
- The previous native offscreen billboard path is no longer allowed to consume the render by default; it only runs when `-Dmegastructure.blackhole.native2d=true` is set for debugging.
- Added a depth-tested local lensing pass:
  - copies the already rendered framebuffer into a GL texture;
  - draws a world-positioned camera-facing lens volume around the reactor core;
  - samples the scene through radial bending, chromatic separation, Einstein-ring brightening and frame-dragging swirl;
  - increases twist strength near the core through the existing `inside` proximity factor.
- Reworked the 3D volume pass:
  - opaque event-horizon shadow sphere;
  - thin photon-ring rim on the shadow silhouette;
  - asymmetric Doppler-bright accretion disk;
  - separate upper/lower lensed disk images bent around the shadow;
  - larger low-alpha caustic shell around the hole.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Installed updated jar to `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`.

## 2026-06-28 - Vulkan black-hole offscreen visualization

Request scope:
- Replace the native Vulkan bridge's command-buffer pulse with an actual black-hole image path.
- Keep composition in the existing black-hole world-render location instead of returning to the old fullscreen overlay.

Implemented:
- `BlackHoleNativeBridge` now passes a direct RGBA pixel buffer to JNI and uploads successful native frames into a GL texture.
- Added a depth-tested world-space billboard compositor for the native frame at the reactor core.
- `native/blackhole_bridge/src/blackhole_bridge.cpp` now renders a 256x256 offscreen black-hole frame through Vulkan transfer commands:
  - transparent background;
  - gravitational lensing shell;
  - asymmetric accretion disk with hot/cold Doppler sides;
  - photon rings;
  - opaque event-horizon shadow.
- The native path returns `true` only after Vulkan fills the frame buffer and Java has a real image to compose.
- Rebuilt and copied `megastructure_blackhole_bridge.dll` to:
  - `native/blackhole_bridge/build/megastructure_blackhole_bridge.dll`;
  - `natives/megastructure_blackhole_bridge.dll`;
  - `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/natives/megastructure_blackhole_bridge.dll`.
- Installed updated mod jar with the new JNI signature to `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/mods/megastructure-world-0.1.0.jar`.

Verification:
- `.\gradlew.bat build` => SUCCESS.
- Native DLL build through Visual Studio CMake/NMake environment => SUCCESS.

## 2026-06-28 - native Vulkan black-hole backend bootstrap

Request scope:
- Pause NeepMeat work completely.
- Continue only the Vulkan side of the black-hole renderer.
- Build a native Vulkan path that the Minecraft client can call through JNI.

Implemented:
- Added a native C++ Vulkan backend in `native/blackhole_bridge/src/blackhole_bridge.cpp`.
- Integrated Khronos Vulkan headers and `volk` loader from local project sources:
  - `tools/vulkan-src/Vulkan-Headers-vulkan-sdk-1.4.350.0`;
  - `tools/vulkan-src/volk-1.4.304`.
- `megastructure_blackhole_bridge.dll` now:
  - initializes Vulkan through `volkInitialize`;
  - creates `VkInstance`;
  - selects a physical GPU with graphics+compute queue;
  - creates `VkDevice`, queue, command pool, primary command buffer and fence;
  - submits a lightweight GPU command-buffer pulse from the black-hole render call.
- The native backend currently returns `false` after the Vulkan pulse, so Java's existing depth-tested world-space black-hole compositor remains visible until Vulkan/GL framebuffer interop is implemented.
- Built the DLL successfully with MSVC/CMake/NMake:
  - source build output: `native/blackhole_bridge/build/megastructure_blackhole_bridge.dll`;
  - dev runtime copy: `natives/megastructure_blackhole_bridge.dll`;
  - modpack runtime copy: `C:/Users/nikit/Desktop/Важный хлам/Сборка Void RP/natives/megastructure_blackhole_bridge.dll`.

Current renderer boundary:
- This is a real native Vulkan device/queue/command path, but not yet the final Vulkan world-space black-hole image.
- The next required step is explicit Vulkan-to-Minecraft composition:
  - either Vulkan/GL external-memory/semaphore interop against the Minecraft GL framebuffer;
  - or a Vulkan offscreen target with controlled upload/composition into Minecraft's render pipeline.

Verification:
- Native bridge build succeeded.
- DLL copied to both project and modpack `natives` directories.

## 2026-06-28 - black-hole visibility/stair removal, mandatory NeepMeat port attempt

Request scope:
- Recreate/fix the black-hole visual path so the old white/flat screen overlay is reduced and the core does not disappear behind the player.
- Remove the central stair/walkway from the black-hole chamber.
- Make NeepMeat mandatory and port/import the available 1.20.4 branch into the project.
- Keep shader compatibility as a target, especially ComplementaryUnbound.

Implemented in main mod:
- `BlackHoleReactorRenderer` now searches nearby generated black-hole cores in a larger radius.
- Black-hole rendering no longer returns early purely because the projected core is behind/outside the camera frustum; the world-space pass can stay active when the player turns around.
- Render distance for the black-hole core is now based on `max(1800, influenceRadius * 6)` instead of the previous short fixed cutoff.
- Fallback GPU shader colors/alpha were toned down:
  - reduced white/yellow overdraw from low angles;
  - lower disk alpha;
  - darker/less flat rim contribution.
- `MegastructureChunkGenerator.blackHoleReactorStructureState` no longer generates the central cross approach through the singularity room:
  - central walkway now exists only outside radius 244;
  - core area is preserved as void/reactor space instead of being crossed by stairs/road.
- `fabric.mod.json` now makes `neepmeat` a hard dependency:
  - version is `*` because the only local 1.20.4 NeepMeat branch reports `0.26.4-beta+1.20.4`, not `0.29.2-beta`.

NeepMeat port state:
- Local `NeepMeat/` repo switched to `origin/1.20.4/dev` as `codex/1.20.4-port`.
- Local 1.20.4 snapshot: `b6a5fc451 Reorganise`.
- Patched NeepMeat metadata:
  - `gradle.properties`: `mod_version = 0.26.4-beta+1.20.4`;
  - `fabric.mod.json`: `minecraft = 1.20.4`.
- Default Java 26 cannot build this Gradle/Groovy project (`Unsupported class file major version 70`); Java 17 is required.
- Offline NeepMeat build fails because `net.fabricmc:intermediary:1.20.4` is not cached.
- Online NeepMeat build with Java 17 hangs for more than 15 minutes at Loom cache rebuild:
  - log stops after `Fabric Loom: 1.9.2`;
  - `Previous process has disowned the lock... rebuilding loom cache`;
  - no `NeepMeat/build/libs` jar produced yet.

Verification:
- Main mod `.\gradlew.bat build` succeeded after dependency/render/generation changes.
- Installed main jar:
  - `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`;
  - size `273614`;
  - time `2026-06-28 04:38:52`.

Critical next step:
- Because `neepmeat` is now a hard dependency, the game requires a compatible NeepMeat 1.20.4 jar in the mods folder.
- Current blocker is NeepMeat Gradle/Loom dependency/cache rebuild, not main mod compilation.

## 2026-06-28 - oasis spore hard fix, seed-rift rarity, lava reservoirs, reactor renderer pass

Request scope:
- Finish the unfinished black-hole renderer/reactor/seed/NeepMeat integration request.
- Fix oasis spores: they are absent or effectively invisible; oasis is not a biome.
- Make two-wall rifts rarer because they cut giant structures too often.
- Implement lava reservoirs.
- Keep the black-hole renderer shader-compatible and connected to the renderer bridge path.

Implemented:
- Oasis spores:
  - `OasisSporeRenderer` no longer lets a bad nearest oasis hint mask suppress physical oasis biomass scans.
  - Local moss/water/tree/clay/NeepMeat biomass can activate the custom green haze independently from biome and hint data.
  - `OasisSporeParticleSpawner` now also uses deterministic oasis hints plus block scans, emits more visible particles, and recognizes NeepMeat biomass/conduit blocks.
- Rifts:
  - Primary rift stripes now use `activeWorldVariantSeed` instead of the fixed shape seed.
  - Accepted rift stripe rate changed from `1/4` to `1/9`, making giant wall cuts much rarer.
  - Rift bridges/access links also use the active world seed, so rift bridge placement changes with seed.
- Lava:
  - Existing lava reservoir system was extended to the black-hole reactor district.
  - Black-hole reactor lava reservoirs are rare, seeded, rimmed, and placed as technical heat basins away from the singularity core.
- Black-hole reactor:
  - Structure gained containment cage rings, outer reactor containment ribs, and collector conduits aimed at the core.
  - Client renderer gained an additional world-space GPU shell mode for a visible gravitational caustic/lensing halo around the singularity.
  - Native renderer bridge remains the extension point. In this environment the native DLL is still not compiled because no native compiler toolchain was available earlier.
- NeepMeat:
  - Latest upstream import remains at `NeepMeat/`, branch `1.20/dev`, version `0.29.2-beta+1.20.1`.
  - Runtime palette bridge continues to use NeepMeat blocks when a compatible NeepMeat jar is present, with vanilla fallback while the upstream 1.20.4 port is unfinished.

Validation required after jar replacement:
- New world required for rift rarity, lava reservoir, black-hole structure, and seed-dependent layout changes.
- Teleport to `/locate biome megastructure:black_hole_reactor` and verify no route/stair crosses the visual core.
- Run `/megastructure locate_oasis`, teleport to result, and verify green spores render from both custom overlay and registered particle spawner.

## 2026-06-27 - native black-hole renderer bridge scaffold

Request scope:
- User rejected a purely Java/OpenGL explanation and explicitly requested a bridge for a separate GPU renderer.
- Remove the old flat screen-space black-hole artifact path and keep the black hole as a world-space effect with correct depth composition.
- Keep the mod buildable while the native backend is not yet compiled.

Implemented:
- Added `BlackHoleNativeBridge`:
  - loads `megastructure_blackhole_bridge` from `-Dmegastructure.blackhole.native`, `natives/`, or `java.library.path`;
  - receives Minecraft framebuffer id, framebuffer size, model-view/projection matrices, camera position, core position, radii, seed, time and instability values;
  - returns `false` on missing native backend, allowing the existing world-space OpenGL renderer to remain active.
- Removed the earlier fullscreen/framebuffer-copy black-hole pass from `BlackHoleReactorRenderer`.
- Added native bridge scaffold:
  - `native/blackhole_bridge/CMakeLists.txt`;
  - `native/blackhole_bridge/README.md`;
  - `native/blackhole_bridge/src/blackhole_bridge.cpp`;
  - JNI symbol: `Java_ru_nikit_megastructure_client_BlackHoleNativeBridge_render0`.
- Kept black-hole rendering inside Minecraft's active render frame unless a native backend is actually loaded, preventing the old unoccluded screen overlay from returning.

Current limitation:
- No Windows native compiler toolchain is available in PATH in this environment:
  - `cmake` not found;
  - `cl` not found;
  - `clang` not found.
- The DLL bridge target is therefore source-scaffolded but not compiled here yet.

Verification:
- `./gradlew.bat build` succeeded.

## 2026-06-27 - NeepMeat upstream import and megastructure palette bridge

Request scope:
- User reminded that NeepMeat must become an obligatory visual direction for the project:
  `https://codeberg.org/MeatWheeze/NeepMeat.git`.
- Import the latest actual upstream version and start integrating its biopunk blocks into the megastructure.

Upstream state:
- Repository imported at `NeepMeat/`.
- Remote: `https://codeberg.org/MeatWheeze/NeepMeat.git`.
- Current latest fetched head: `1d697d581 Increase version to 0.29.2-beta`.
- Branch: `1.20/dev`.
- Upstream currently targets Minecraft `1.20.1`, not `1.20.4`.

Implemented in this mod:
- Added `NeepMeatCompat`, a runtime registry resolver for optional NeepMeat block ids.
- Added `neepmeat` to `fabric.mod.json` `suggests`.
- `BlockPalette` now switches to NeepMeat blocks when that mod is present:
  - `grey_rough_concrete` for main mass/platform material;
  - `white_rough_concrete` for light megastructure concrete;
  - `dirty_white_tiles` for wall panels;
  - `polished_metal`, `rusty_metal`, `rusted_metal_scaffold`, `rusty_metal_light`;
  - `vascular_conduit`, `encased_vascular_conduit` for pipe/infrastructure language;
  - `contaminated_dirt`, `blood_bubble_log`, `blood_bubble_wood`, `blood_bubble_planks`, `blood_bubble_leaves` for oasis/biopunk growth.
- All lookups have vanilla fallbacks, so this mod remains buildable and launchable without a finished 1.20.4 NeepMeat jar.

Current limitation:
- Full source port of NeepMeat from 1.20.1 to 1.20.4 is not complete in this step.
- Reason: upstream is a multi-module mod with Geckolib/Flywheel/CCA/Meatlib dependencies that must all be migrated together; the current completed integration is an ABI-safe visual bridge from this mod into NeepMeat ids once a compatible jar is present.

## User Goal

Create a Fabric 1.20.4 Minecraft worldgen mod for a vast Blame!/Returnal-like megastructure:
endless enclosed artificial world, no normal sky, huge wall gaps, internal megablock corridor systems,
rooms that mimic habitation but feel fake/nonfunctional, rare unstable bridges, shafts, balconies,
service infrastructure, pipes, grates, stairs that are actually walkable, mod ore accessibility.

Primary visual intent:
- immense vertical megastructure, brutalist/cast-stone/concrete-like, not natural caves;
- screenshots imply giant parallel walls, long rifts, tiny human scale, catwalks, ladders/stairs,
  cylindrical shafts, voids, balconies, fake habitation shells, technical ribs/pipes;
- avoid noisy random geometry soup; structures must have hierarchy and readable logic.

## Hard User Preferences

- Do not use literal gray concrete as main material.
- Do not use black concrete.
- Use material closer to concrete than raw stone: currently `smooth_stone` base plus `andesite`,
  `polished_andesite`, `stone_bricks`, `polished_deepslate`.
- Chains must not appear as wrong vertical sticks; only use oriented chain states with axis X/Z where appropriate.
- Sea lanterns must not form continuous lines everywhere; only rare isolated light nodes.
- Rift/gap between two huge walls must be empty except very rare unstable suspension bridges.
- Tunnels must be structured, not randomly mixed.
- Stairs must be walkable and not run into walls/each other.
- World height requested x3; current dimension: `min_y=-384`, `height=1152`, `ceiling_y=767`.
- After compiling, replace jar in target mods dir.

## Current Architecture

Entrypoint:
- `src/main/java/ru/nikit/megastructure/MegastructureMod.java`
- registers chunk generator codec under `megastructure:megastructure`.

Generator:
- `src/main/java/ru/nikit/megastructure/world/MegastructureChunkGenerator.java`
- deterministic coordinate function, no placed structure files yet.
- macro district layer added: `DISTRICT_SIZE=384`, `districtType(x,z)` selects one dominant local landscape/system.
- `stateAt(x,y,z)` order is critical:
  1. bounds check;
  2. primary rift exclusion zone;
  3. if inside rift, only `riftSuspensionBridgeState` may place blocks, else AIR;
  4. compute `districtType(x,z)` then `districtAir(district,x,y,z)`;
  5. structural overlay;
  6. if air => AIR;
  7. wall details;
  8. foundation top/bottom;
  9. ore-or-base material.

Settings:
- `src/main/java/ru/nikit/megastructure/world/MegastructureSettings.java`
- defaults:
  `seaLevel=40, floorY=-384, ceilingY=767, spawnPlatformY=96,
   primaryRiftWidth=80, riftMinWidth=20, riftMaxWidth=100,
   cellSize=96, motifCellSize=768, oreRate=42`

Palette:
- `src/main/java/ru/nikit/megastructure/world/BlockPalette.java`
- `MASS=SMOOTH_STONE`
- `FOUNDATION=POLISHED_ANDESITE`
- `WALKWAY=STONE_BRICKS`
- `WALL_PANEL=ANDESITE`
- `PIPE=POLISHED_DEEPSLATE`
- Historical: `GRATE` used to be `DEEPSLATE_TILE_WALL` after `IRON_BARS` removal.
- Current: `GRATE=POLISHED_DEEPSLATE`; fence/wall-post silhouettes in voids are intentionally removed.
- `DARK_STONE=POLISHED_DEEPSLATE`
- `LIGHT_STONE=SMOOTH_STONE`
- `LAMP=SEA_LANTERN` but used only as rare isolated lights
- `CRACKED_PANEL=CRACKED_STONE_BRICKS`
- `STAIN=TUFF`
- `chain(axis)` returns `Blocks.CHAIN.with(Properties.AXIS, axis)`
- `stairs(facing)` returns bottom `STONE_STAIRS`.

Datapack resources:
- world preset: `src/main/resources/data/megastructure/worldgen/world_preset/megastructure.json`
- dimension type: `src/main/resources/data/megastructure/dimension_type/megastructure.json`
- biome: `src/main/resources/data/megastructure/worldgen/biome/concrete_void.json`
- world preset UI tags:
  - `src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json`
  - `src/main/resources/data/minecraft/tags/worldgen/world_preset/extended.json`
- biome overworld tag for ore-mod compatibility:
  - `src/main/resources/data/minecraft/tags/worldgen/biome/is_overworld.json`

## Implemented Generation Motifs

Macro districts:
- `DISTRICT_NETWORK`: finite bounded interior network, rooms, service shafts, local atrium.
- `DISTRICT_DEAD_END`: long finite dead-end corridor district.
- `DISTRICT_MONOLITH_HALL`: huge void hall with central giga-column/monolith, bridge, broken top, stair ribbon.
- `DISTRICT_COLUMN_FOREST`: chamber full of thin columns and diagonal beams.
- `DISTRICT_CYLINDER`: cylindrical atrium with ring wall and balconies.
- `DISTRICT_ABYSS`: abyss void with bridge and spherical dwelling shell/growths.
- `DISTRICT_DESCENT`: narrow vertical descent well with stair/landing structure.
- `DISTRICT_DENSE_WALL`: mostly solid mass with rare sparse corridor.

Primary rifts:
- rare vertical wall gaps along X stripes.
- frequency controlled in `isPrimaryRift`: only hash stripes with `hash % 4 == 0`.
- width deterministic per stripe, from `riftMinWidth` to `riftMaxWidth`.
- protected void: no normal structures/decor; only rare bridge pass can place blocks.

Rift suspension bridges:
- method `riftSuspensionBridgeState`.
- very rare: per `(stripe, z/384)` hash with `hash % 11 == 0`.
- thin `WALKWAY` deck, deliberate missing deck gaps, solid `GRATE` side trim,
  horizontal X-axis chains above sides.
- intended to look unstable and sparse.

Corridor systems:
- `isCellCorridor`: 96-cell higher-level trunk/side corridors, 36-block vertical module.
- `isApartmentCorridor`: 72-cell internal megablock corridors, 18-block vertical module.
- `isApartmentRoom`: corridor-attached room modules only; rooms are fixed slots along trunk corridors,
  with air doorway gaps and deterministic missing rooms.
- `isServiceShaft`: 96-cell vertical cores, 16x16-ish larger shaft zones.

Stairs:
- old rift-wall stairs removed because they collided with walls/rifts.
- `serviceShaftStairState`: walkable perimeter stair core inside service shaft, 32-step cycle,
  landings every 16 vertical blocks.
- `cylindricalShaftStairState`: ring walkway in cylindrical shafts, not a perfect stair yet.

Large voids:
- `isRoomVoid`: rectangular halls/void rooms.
- `isCylindricalShaft`: rare large cylindrical voids with ring balconies.
- `isAbyssVoid`: rare circular abyss nodes.
- `abyssState`: bridge/porch/entrance/hollow sphere dwelling shell.

Decor:
- `facadeState`: panel seams, ribs, pipe runs, false grates on non-air wall faces.
- `wallDetail`: pipes/copper only near cell edges; no lamp line.
- `corridorDetailState`: rare isolated lamps at grid centers only; no vanilla door blocks.
- `webColumnState` and `cableState` are now limited to large voids only, not normal corridors.

Ores:
- `oreOrStone` embeds vanilla ore blocks in mass.
- custom biome tagged as overworld for broad ore-mod hooks, not guaranteed for all modded ores.

## Last User Request Implemented

Request summary:
- User says generation became too perfectly duplicating/tiled; wants generative systems with rules but not human-obvious,
  diverse world-like landscapes and rare huge setpieces; concept art fidelity; iron bars are out of place.

Changes made:
- added district-based macro motif system with `DISTRICT_SIZE=384`;
- added `districtType` and `districtAir` so only one dominant system runs per district;
- bounded regular corridor networks inside district margins to prevent endless tiled corridors;
- added monolith hall, column forest, cylindrical atrium, abyss dwelling, descent well, dead-end corridor, dense wall districts;
- added district-specific structure states for monolith, cylinder rings, abyss dwelling and descent stairs;
- historical previous step replaced `GRATE=IRON_BARS` with `GRATE=DEEPSLATE_TILE_WALL`; current step replaced it again with solid `POLISHED_DEEPSLATE`;
- disabled old independent shaft/abyss overlays that could cross-contaminate districts;
- built successfully and copied jar to target mods dir.

Web/PCG references consulted this turn:
- Red Blob Games BFS dungeon generation: use primary path, side paths, graph cleanup, pruning.
- Bob Nystrom "Rooms and Mazes": connectedness, rooms near passages, avoid overlap, prune dead ends.
- RogueBasin dungeon-building algorithm: room/corridor connection and staged generation principles.

Applied principles:
- corridor graph/anchors before rooms;
- rooms only attach to existing corridor bands;
- no independent high-frequency room masks;
- large halls are rare exceptions, not base layer;
- visual dirt is facade-level decoration, not geometry-breaking noise.
- macro district/biome layer above local room/corridor layer to create landscape variety and break tile repetition.

## Current User Request Implemented

Request summary:
- User says structures/pipes/chains/fences/stairs must not appear as disconnected void noise or sealed inside solid walls.
- Stairs must only spawn where the owning structure intentionally needs them, and must be more usable for vertical movement.
- Well/circular void views should not get sudden random stairs, fences, or chains.
- More separated motifs and more possible generation families are needed, using new concepts: circular wells/ring shafts,
  tank-like round masses, brutalist block towers, scaffold chambers, long planned bridges, megablock/mass-housing scale.

Code changes:
- `BlockPalette.GRATE` changed from `DEEPSLATE_TILE_WALL` to solid `POLISHED_DEEPSLATE`; this prevents fence/wall-post silhouettes in voids.
- Added three macro districts:
  - `DISTRICT_BLOCK_TOWERS`: large void chamber with several brutalist cuboid masses, ground plane, entrances, and planned bridges.
  - `DISTRICT_TANK_CLUSTER`: round/tank/circular-shaft cluster with cylindrical walls, rims, and deliberate service bridges.
  - `DISTRICT_SCAFFOLD`: large scaffold chamber with grid columns, beams, platforms, and one planned stair tower.
- District weights changed to include 10 district types:
  `NETWORK`, `DEAD_END`, `MONOLITH_HALL`, `COLUMN_FOREST`, `CYLINDER`, `ABYSS`, `DESCENT`,
  `BLOCK_TOWERS`, `TANK_CLUSTER`, `SCAFFOLD`, fallback `DENSE_WALL`.
- `DISTRICT_DEAD_END` no longer carves `isServiceShaft`; prevents unrelated stair shafts appearing in dead-end corridor districts.
- `stairState` now takes `district`; service stairs only run in `DISTRICT_NETWORK`, cylindrical ring stair only in `DISTRICT_CYLINDER`.
- `corridorDetailState` now takes `district`; isolated sea-lantern points only run in corridor-like districts:
  `NETWORK`, `DEAD_END`, `DENSE_WALL`.
- Removed threshold grate/fence placement from corridor details entirely.
- `structuralOverlay` no longer calls global `bridgeState` or global `cableState`.
- Web/beam column overlay now only runs in `DISTRICT_COLUMN_FOREST`, not every large void.
- Deleted old unused global methods: `isRoomVoid`, `isCylindricalShaft`, `isAbyssVoid`, `bridgeState`, `shaftState`, `abyssState`, `cableState`.
- Remaining chain placement is only in `riftSuspensionBridgeState`, where it belongs to rare planned unstable suspension bridges.

Design rule after this request:
- no global void decoration pass except column-forest web beams;
- no random chains/cables/pipes in air;
- no rail/fence-like block in open voids;
- all new large geometry must be district-owned and motif-owned;
- pipe/facade details remain only on non-air wall faces via `facadeState`/`wallDetail`.

Build/install:
- `.\gradlew.bat build --stacktrace` succeeded after edits.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `28472` bytes, `22.06.2026 04:26:20`.

## Current User Request Implemented - Titanic Scale Pass

Request summary:
- User clarified that "giant room with tower" means room radius at least ~500 or 1000 blocks, almost full world height,
  and central block/tower mass around radius 50; current large rooms were too human-scale.
- User says stairs still cannot be climbed.
- User wants the pseudo-concrete material to have slight variation with stone-like blocks.
- User also noted rare bridges between the two rift walls are missing but should exist.

Code changes:
- `DISTRICT_SIZE` increased from `384` to `1024`; this is required for radius ~500 macro rooms.
- `DISTRICT_BLOCK_TOWERS` repurposed into a titanic tower hall:
  - `isBlockTowerVoid`: circular void radius `500`, from `floorY+12` to `ceilingY-28`.
  - `blockTowerStructureState`: near-full-height central tower with radius `50`, extra cuboid outgrowths,
    huge empty floor, sparse ring traces, and long far bridges.
- Rift bridges adjusted:
  - bridge band size changed from `384` to `256`;
  - spawn chance changed from `1/11` to `1/4`;
  - deck width increased and gaps made rarer;
  - still only generated by `riftSuspensionBridgeState` inside primary rift air, so rift gap remains mostly empty.
- Stair logic replaced with generic `squareStairState`:
  - per-Y step target moves to adjacent horizontal cell around a square path;
  - corners get small landings;
  - service shaft, cylinder stair, descent well, and scaffold stairs now use this shared path.
- Material variation:
  - added `MASS_STONE_VARIANT=STONE` and `MASS_ANDESITE_VARIANT=ANDESITE`;
  - added `massState(x,y,z)`;
  - `oreOrStone` now returns rare stone/andesite variants when not placing ore.

Build/install:
- `.\gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `28833` bytes, `22.06.2026 04:49:34`.

## Current User Request Implemented - Railway / Locate Pass

Request summary:
- User requested several infinite tunnels, possibly as railway tunnels without trains.
- Railway tunnels should become an additional bridge type when they exit into the gap between two rift walls.
- User requested every generated structure/system to be trackable as a biome for `/locate`.
- User clarified titan scale should apply to structures, not endless featureless monolithic wall walking; dense wall should be less dominant.

Code changes:
- Added railway infrastructure layer:
  - `railwayState`, `railwayXState`, `railwayZState`;
  - `isRailwayAir`, `isRailwayXAir`, `isRailwayZAir`;
  - X railways: infinite X-axis tunnels selected by Z lanes; Z railways: rarer infinite Z-axis tunnels selected by X lanes;
  - tunnel geometry: carved air around route, `WALKWAY` floor, vanilla rails on top;
  - railway is evaluated before primary rift, so when a route crosses a rift it renders as a bridge instead of disappearing.
- Added rail palette:
  - `RAIL_X = Blocks.RAIL` with `EAST_WEST`;
  - `RAIL_Z = Blocks.RAIL` with `NORTH_SOUTH`.
- Added `isRailwayLineAt(x,z)` static helper for biome lookup.
- Added custom biome source:
  - `DistrictBiomeSource`;
  - registered as `megastructure:district` in `MegastructureMod`;
  - world preset now uses `type: "megastructure:district"` instead of `minecraft:fixed`.
- Added biome JSON files for all macro systems and railway:
  - `primary_rift`
  - `railway_tunnel`
  - `interior_network`
  - `dead_end_corridors`
  - `monolith_hall`
  - `column_forest`
  - `cylindrical_atrium`
  - `abyss_dwelling`
  - `descent_well`
  - `titan_tower_hall`
  - `tank_cluster`
  - `scaffold_chamber`
  - `dense_wall`
- Added all new biome IDs to `data/minecraft/tags/worldgen/biome/is_overworld.json`.
- Changed district weights to reduce pure dense wall:
  `NETWORK 24%`, `DEAD_END 11%`, `MONOLITH 10%`, `COLUMN_FOREST 11%`,
  `CYLINDER 10%`, `ABYSS 9%`, `DESCENT 8%`, `TITAN 7%`,
  `TANK 6%`, `SCAFFOLD 3%`, `DENSE_WALL 1%`.

Locate commands:
- `/locate biome megastructure:primary_rift`
- `/locate biome megastructure:railway_tunnel`
- `/locate biome megastructure:interior_network`
- `/locate biome megastructure:dead_end_corridors`
- `/locate biome megastructure:monolith_hall`
- `/locate biome megastructure:column_forest`
- `/locate biome megastructure:cylindrical_atrium`
- `/locate biome megastructure:abyss_dwelling`
- `/locate biome megastructure:descent_well`
- `/locate biome megastructure:titan_tower_hall`
- `/locate biome megastructure:tank_cluster`
- `/locate biome megastructure:scaffold_chamber`
- `/locate biome megastructure:dense_wall`

Important:
- Existing already-created worlds may have the old fixed biome source saved in `level.dat`; create a new world/preset test if `/locate biome` still only sees `concrete_void`.

Build/install:
- `.\gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `39604` bytes, `22.06.2026 05:29:20`.

## Current User Request Implemented - Concept Documents Pass

Inputs analyzed:
- `C:\Users\nikit\Desktop\Описание.docx`
- `C:\Users\nikit\Downloads\Megastructure_Generation_Concept.docx`

Extracted design priorities:
- world is one continuous artificial organism, not separate dungeons;
- macro districts must behave like architectural biomes and blend through routes;
- tight corridors should regularly open into vast vertical/empty spaces;
- railway lines must be true infrastructure arteries: wide, long, sometimes double-track, with service niches;
- industrial wall/cutaway districts are missing: exposed layers of a technical city with rails, shafts, platforms, pipes, modules;
- decor must remain subordinate to architectural mass;
- dense featureless monolith should not dominate exploration.

Code changes:
- Added new macro district:
  - `DISTRICT_INDUSTRIAL_WALL = 10`;
  - `DISTRICT_DENSE_WALL = 11`;
  - `isIndustrialWallVoid`;
  - `industrialWallStructureState`.
- `DISTRICT_INDUSTRIAL_WALL` creates an open industrial cutaway:
  - huge vertical rectangular slice through a wall;
  - rear wall panels;
  - vertical shaft columns;
  - rail decks with rails;
  - service platforms;
  - attached pipe bands;
  - hollow hanging modules.
- District weights adjusted:
  `NETWORK 22%`, `DEAD_END 10%`, `MONOLITH 10%`, `COLUMN_FOREST 10%`,
  `CYLINDER 10%`, `ABYSS 8%`, `DESCENT 8%`, `TITAN 8%`,
  `TANK 6%`, `SCAFFOLD 4%`, `INDUSTRIAL_WALL 3%`, `DENSE_WALL 1%`.
- Railway tunnels upgraded:
  - wider carved tunnel profile;
  - double-track rails instead of single center rail;
  - periodic side service niches;
  - small pipe runs inside niches;
  - rift-crossing behavior preserved.
- Added biome:
  - `megastructure:industrial_wall`
  - added to `DistrictBiomeSource`;
  - added to world preset;
  - added to `minecraft:is_overworld` biome tag.
- HUD motif name added: `industrial wall`.

Locate commands updated:
- `/locate biome megastructure:primary_rift`
- `/locate biome megastructure:railway_tunnel`
- `/locate biome megastructure:interior_network`
- `/locate biome megastructure:dead_end_corridors`
- `/locate biome megastructure:monolith_hall`
- `/locate biome megastructure:column_forest`
- `/locate biome megastructure:cylindrical_atrium`
- `/locate biome megastructure:abyss_dwelling`
- `/locate biome megastructure:descent_well`
- `/locate biome megastructure:titan_tower_hall`
- `/locate biome megastructure:tank_cluster`
- `/locate biome megastructure:scaffold_chamber`
- `/locate biome megastructure:industrial_wall`
- `/locate biome megastructure:dense_wall`

Build/install:
- `.\gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `41600` bytes, `22.06.2026 05:57:03`.

## Current User Request Implemented - Crash Fix Rail BlockState

Input:
- User reported game crash.
- Crash log source: `C:\Users\nikit\Desktop\Кралог.docx`.

Crash root cause:
- `ExceptionInInitializerError` during chunk generation.
- Real cause: `BlockPalette.<clinit>` attempted to set rail shape through
  `Properties.STRAIGHT_RAIL_SHAPE` on `Blocks.RAIL`.
- Minecraft/Fabric 1.20.4 runtime rejected the property assignment:
  `Cannot set property ... shape ... as it does not exist in Block{minecraft:rail}`.
- Failure path:
  `MegastructureChunkGenerator.facadeState -> structuralOverlay -> stateAt -> populateNoise`.

Fix:
- `BlockPalette.RAIL_X` and `BlockPalette.RAIL_Z` now use
  `Blocks.RAIL.getDefaultState()` with no manual shape mutation.
- Removed direct `RailShape` usage from the palette.
- This prioritizes launch stability. Rail visual direction may need a later, safer implementation
  through a verified property lookup or by placing rail orientation in a post-generation pass.

Build/install:
- `.\gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `41537` bytes, `22.06.2026 7:06:36`.

## Current User Request Implemented - Spawn Rail Hub, Route Integration, 10 Districts

Request:
- Spawn directly on railway tracks near exactly two large visible structures.
- Rebuild stations so they respect surrounding voids, look intentional, and occur less often.
- Distribute railways through the full world height instead of the lower portion.
- Make wall-to-wall bridges much more frequent, multi-level, and connected to tunnels.
- Add at least ten unique structures.

Spawn precinct:
- Reserved deterministic chamber around world origin.
- New worlds using this generator set spawn to `(0, spawnRailY + 1, -3)` directly above a rail.
- Server hook only applies while world time is `<= 1`; existing worlds keep their configured spawn.
- Spawn station contains an east-west double rail deck, two platforms, canopy ribs, sparse lamps, and two large landmarks:
  - stepped western transit bastion;
  - eastern ring tower with central core.
- Chamber floor and both landmarks are structurally grounded; connector bridges terminate at them.

Railways/stations:
- Railway Y now ranges from near the lower structure layers to near the ceiling using world height, rather than the previous fixed lower range.
- Removed repeating service niches every 96 blocks.
- Full stations now test one candidate per 1536-block route segment and accept only 1/4 candidates.
- Stations are allowed only in districts with surrounding structural mass:
  `NETWORK`, `DEAD_END`, `DENSE_WALL`, `INDUSTRIAL_WALL`, `MACHINE_NAVE`, `CONDUIT_BASILICA`.
- A station is rejected when its complete 180-block corridor intersects a primary rift.
- Stations include wide platforms, side walls, roof ribs, sparse lamps, and lateral access corridors.

Rift bridges:
- Independent suspension bridge bands reduced from 256 to 192 blocks.
- Candidate frequency increased from 1/4 to 1/2.
- Bridge height uses almost the complete dimension height.
- Removed bridge chains; decks use solid walkways and restrained side grates.
- Every accepted bridge carves 72-block access tunnels into both walls.
- Tunnel ends widen into cross-junctions, so bridges terminate inside explorable passages instead of solid mass.
- Railway lines continue to create additional rail bridges across rifts at their own route heights.

New district structures and locate commands:
- `transit_nexus`: crossed multi-level railway decks around a load-bearing interchange core.
  `/locate biome megastructure:transit_nexus`
- `reactor_cathedral`: long cathedral hall, central reactor shell, buttresses, processional bridges.
  `/locate biome megastructure:reactor_cathedral`
- `hanging_archive`: ceiling-anchored archive stacks and alternating galleries.
  `/locate biome megastructure:hanging_archive`
- `ventilation_canyon`: dimension-height technical canyon with grounded duct trunks and catwalk tiers.
  `/locate biome megastructure:ventilation_canyon`
- `inverted_pyramid`: enormous ceiling-attached inverted stepped mass crossed by upper bridges.
  `/locate biome megastructure:inverted_pyramid`
- `ring_vault`: cylindrical vault with three structural rings, bands, and traversable spokes.
  `/locate biome megastructure:ring_vault`
- `machine_nave`: long hall of grounded machine banks, columns, central aisle, and gantries.
  `/locate biome megastructure:machine_nave`
- `fractured_habitat`: six deterministic habitation modules connected to a central structural spine.
  `/locate biome megastructure:fractured_habitat`
- `conduit_basilica`: repeated load-bearing arches, attached conduit trunks, and side galleries.
  `/locate biome megastructure:conduit_basilica`
- `reservoir_hall`: circular empty hall with three dry basin walls, ring walks, gauge tower, access bridge.
  `/locate biome megastructure:reservoir_hall`

Codec/resources:
- New biomes grouped under `biome_source.additional_districts` to stay below `RecordCodecBuilder` product arity limits.
- Added all ten biome JSON files and added them to `minecraft:is_overworld`.
- Updated world preset with all ten registry entries.

Verification/install:
- Parsed all `33` JSON resources with `ConvertFrom-Json` successfully.
- `./gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `59056` bytes, `22.06.2026 16:28:41`.

Important testing constraint:
- Use a newly created world to test the deterministic spawn hook and updated biome-source codec.
- Existing generated chunks cannot change and existing worlds retain their old spawn position.

## Current User Request Implemented - Continuous Railway and Guaranteed Connectivity

Request:
- Spawn should have one distant giant landmark, not two close towers.
- Spawn railway must be an actual infinite travel route with correctly oriented rails and occasional turns.
- Every generated district/structure must connect into a chain of tunnels, with more than one connection allowed.
- Rework the ten additional structures to remove unsupported floating geometry.
- Increase wall-to-wall bridge density again.

Rail BlockState fix:
- Replaced default-state-only `RAIL_X` and `RAIL_Z` with explicit `Properties.RAIL_SHAPE` states.
- Added safe `state.contains(Properties.RAIL_SHAPE)` guard to prevent recurrence of the previous static initialization crash.
- Added shapes:
  - `EAST_WEST`;
  - `NORTH_SOUTH`;
  - `NORTH_EAST`;
  - `NORTH_WEST`;
  - `SOUTH_EAST`;
  - `SOUTH_WEST`.
- Existing X and Z railways now use their correct visual direction.

Primary infinite railway:
- Spawn route is now a dedicated infinite deterministic mainline at `spawnPlatformY`.
- Route period: `2048` blocks.
- First turn: local X `896`; line moves from Z `0` to Z `96`.
- Second turn: local X `1920`; line returns from Z `96` to Z `0`.
- Corners use actual curved rail shapes and vertical portions use north-south rails.
- Route calculation uses floor-mod and is continuous through negative coordinates.
- Spawn precinct uses the same route function as the infinite line; no local/global seam remains.
- Topology script validated `8961` rail blocks across four periods with `0` missing neighbor links.

Spawn precinct revision:
- Removed the western stepped tower entirely.
- Retained one landmark at approximately `(238, 160)`, about `287` blocks from spawn.
- Landmark has a grounded plinth, cylindrical shell, central core, attached structural rings.
- Landmark is connected to the station by an L-shaped walkway at platform height.
- New world spawn moved to `(0, spawnRailY + 1, 0)`, directly above the correctly oriented main rail.
- Station connects upward to the global tunnel level using a walkable switchback stair.
- A dedicated technical gallery connects that stair to the district-center network node.

Guaranteed tunnel graph:
- Global connector level: `floorY + 540` (`Y=156` with current settings).
- Every 1024x1024 district has a west-to-east route.
- Route shape is broken into five deterministic legs:
  west portal -> first bend -> district center -> second bend -> east portal.
- Edge portal coordinates are calculated from shared edge hashes; adjacent districts always produce the same doorway position.
- Every fourth district column also receives north-to-south links with the same shared-edge rule.
- Result is one connected world graph rather than isolated probabilistic corridors.
- Routes crossing primary rifts become solid walkway bridges with side grates instead of disappearing.
- Each district center contains a full-height access shaft.
- Shaft contains alternating 12-block east/west stair flights and connecting landings.
- The shaft intersects the central connector node and opens into every central district structure across its vertical extent.

Ten-structure attachment pass:
- `transit_nexus`: added spokes between all concourse rings and load-bearing core.
- `reactor_cathedral`: reactor shell/core remain grounded; processional decks intersect structural core/buttresses.
- `hanging_archive`: archive slabs extended into ceiling-anchor columns.
- `ventilation_canyon`: catwalks and cross-bridges intersect grounded duct trunks.
- `inverted_pyramid`: pyramid remains attached directly to chamber ceiling; upper bridges intersect its stepped body.
- `ring_vault`: added cardinal foundation columns from floor into all three ring systems.
- `machine_nave`: machine banks remain floor-mounted; gantries intersect repeated columns.
- `fractured_habitat`: each module now has two-leg gallery routing to the central structural spine.
- `conduit_basilica`: added repeated brackets joining conduit trunks to structural arches.
- `reservoir_hall`: raised basin walls to meet their corresponding ring walkways.
- Global center shaft supplies at least one traversable access point to every one of these structures.

Bridge density:
- Independent bridge band spacing reduced from `192` to `160` blocks.
- Acceptance changed from `1/2` to `3/4` candidates.
- Existing multi-height selection and paired wall access tunnels preserved.
- Guaranteed connector-network crossings create an additional systematic bridge layer.

Verification/install:
- `./gradlew.bat compileJava --stacktrace` succeeded after rail property changes.
- Primary railway graph validation succeeded: `8961` blocks, `0` broken links.
- `./gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `61989` bytes, `22.06.2026 19:00:54`.

Testing constraint:
- Create a new world. Existing chunks retain old rails, structures, bridges, and tunnel masks.
- In-game inspection is still required for minecart behavior at curve chunks and visual clearance on switchback stairs.

## Current User Request Implemented - Rail Bridge Profile, Ring Function, Oasis Design

Inputs:
- Screenshot 1 showed the custom primary railway turn expanding into a giant slab with one visible rail line.
- Screenshot 2 showed `ring_vault` as concentric geometry without an understandable engineering role.

Spawn railway correction:
- Removed the custom 2048-block turning route from the spawn mainline.
- Spawn railway is now a forced ordinary infinite X-axis line using the same profile as procedural X railways.
- Deck width: 15 blocks (`Z=-7..7`).
- Four east-west rail strips at `Z=-3`, `-2`, `2`, `3`.
- Tunnel clearance now matches ordinary railway height (`baseY+1..baseY+7`).
- Rift crossing uses the same narrow deck and side treatment instead of the previous 96-block turn slab.
- New-world spawn moved above rail strip `Z=2`.
- Correct `Properties.RAIL_SHAPE` orientation and runtime property guard remain active.

Ring vault redesign:
- Removed three continuous decorative cylindrical walls.
- Added central load-bearing spindle around the guaranteed access shaft.
- Added three maintenance rings at levels `72`, `148`, and `224` above chamber floor.
- Every ring now has:
  - walkable annular deck;
  - attached underside conduit bus;
  - four grounded cardinal pylons;
  - four radial service bridges into the spindle;
  - four identical drive housings attached to pylons/spokes.
- Outer ring has four wall access bridges.
- Geometry is deterministic and functional; no chains or random debris were added.

Buried-structure correction:
- Root cause: each structure biome previously occupied its full 1024x1024 district while the actual chamber occupied only its central footprint.
- `/locate biome` could therefore return a solid district edge far outside the chamber.
- Added `isDistrictBiomeFootprintAt` with a footprint for all 22 district types.
- Outside a structure footprint, biome source now reports `interior_network` instead of the structure biome.
- Expanded monolith hall void from approximately `128x112` to `160x142` half-extents and increased vertical clearance.
- Expanded transit nexus void beyond all of its structural extents.
- Existing guaranteed center shaft and connector node remain active for every district.

Oasis system:
- Deliberately not implemented in code.
- Added detailed design document:
  `C:\Users\nikit\Documents\New project\OASIS_DESIGN.md`
- Core design:
  - oasis is a rare overlay on an existing accessible structure, never a separate district;
  - base chance around 1/40 eligible hosts with 3-district minimum separation;
  - optional one-host guarantee 3-6 districts from spawn;
  - water must emerge from a wall-attached 5-11 block pipe into one connected catchment;
  - moss follows a surface-aware wetness gradient up to 56 blocks;
  - 6-10 manually generated supported trees, no soil requirement;
  - maximum one rooted-dirt block with 20% chance per full oasis;
  - no grass blocks, podzol, farmland, or continuous dirt banks;
  - vines require overhead attachment and cannot obstruct routes;
  - approach tunnels receive progressive moisture clues rather than a separate oasis biome.

Verification/install:
- Parsed all `33` JSON resources successfully.
- `./gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `62600` bytes, `22.06.2026 20:45:54`.

Testing constraint:
- Use a new world for updated railway, ring-vault geometry, biome footprints, and chamber masks.
- In-game screenshot verification is still required; build/resource validation cannot evaluate visual scale or darkness.

## Current User Request Implemented - Green Oasis Overlay v1

Request:
- Implement the previously designed green oasis system.
- Oasis must modify an existing structure rather than generate as a separate district.
- Water, moss, vines, several trees, and extremely scarce dirt are required.

Selection/distribution:
- Added per-generator thread-safe descriptor cache keyed by district coordinates.
- Eligible hosts:
  `tank_cluster`, `reservoir_hall`, `reactor_cathedral`, `ring_vault`,
  `industrial_wall`, `scaffold_chamber`, `column_forest`, `monolith_hall`,
  `fractured_habitat`, `conduit_basilica`, `hanging_archive`.
- Raw candidate probability: `1/40` eligible districts.
- Nearby-candidate priority filtering enforces minimum Chebyshev spacing of 3 districts.
- Model validation over 41x41 districts found 25 accepted oases with no spacing violations.
- Guaranteed starter oasis searches distance rings 3, 4, 5, then 6 and chooses the best eligible host in the nearest non-empty ring.

Guaranteed oasis in current deterministic world:
- District: `(-1, 3)`.
- Host: `monolith_hall`.
- District center: approximately `(-512, 3584)`.
- Basin: approximately `(-512, Y=201, 3618)`.
- Water source Y: `239`.
- Trees: `6`.
- Rooted dirt: absent for this oasis.
- Safe inspection point: `/tp @s -484 203 3618`.

Hydrology/pipe:
- Four profiles implemented:
  `pipefall`, `cistern garden`, `hanging seep`, `monolith spring`.
- Host-specific floor and wall extents match each structure's existing generation formulas.
- Rectangular hosts use side-specific X/Z wall distance.
- Industrial wall forces the source pipe onto its short structural wall.
- Circular hosts calculate vine attachment position against the curved chamber boundary.
- Main pipe is a hollow 11-block outer-diameter run.
- Pipe continues 10 blocks into the host wall and uses repeated structural collars.
- Pipe material depends on profile: deepslate-like pipe or copper/rust pipe.
- A 3x3 source waterfall descends into one contained basin.
- Basin uses a three-block moss rim to prevent uncontrolled horizontal flooding.

Vegetation:
- Wetness footprint extends up to 56 blocks from basin edge.
- Surface-aware visual zones are approximated through deterministic distance-based coverage:
  saturated, dense, patchy, trace.
- Floor replacement uses moss blocks; outer damp areas use moss carpet.
- Each oasis creates `6-10` manually generated trees.
- Tree placement uses deterministic radial peripheral positions around the basin.
- Tree heights: `5-11`; first tree may reach `15`.
- Variants: oak-like, birch-like, and azalea-canopy.
- Leaves are persistent; trees do not depend on vanilla soil placement rules.
- Every tree has a moss root cup on a supported floor.
- Maximum soil: one `rooted_dirt` under the first tree with `20%` oasis probability.
- No grass blocks, normal dirt fields, podzol, farmland, or soil banks.
- Four deterministic vine cascades attach to the actual interior wall boundary.

Safety/connectivity:
- Oasis overlay executes only after a valid existing district host is selected.
- Primary/secondary rail footprints are protected from all oasis blocks.
- Global connector tunnel and center access shaft are protected from obstruction.
- Basin/tree placement is kept inside host-specific chamber bounds.
- Moss carpet clues appear only on outer shoulders of the connector tunnel, never its walkable center.
- F3 debug text reports `Oasis overlay: <profile>` inside an oasis host district.
- Oasis remains part of its original structure biome; no separate biome was added.

Documentation:
- Updated `C:\Users\nikit\Documents\New project\OASIS_DESIGN.md` from design-only to implemented v1.
- Document records implemented and deferred features.
- Deferred:
  arbitrary runoff simulation, multi-level streams, sapling loot control,
  minor seep overlays.

Verification/install:
- Oasis spacing/guarantee model validated independently.
- `./gradlew.bat compileJava --stacktrace` succeeded repeatedly during implementation.
- Parsed all `33` JSON resources successfully.
- `./gradlew.bat build --stacktrace` succeeded.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed jar size/time: `71750` bytes, `22.06.2026 21:35:44`.

Testing constraint:
- Create a new world. Existing chunks cannot gain oasis overlays.
- First visual inspection target: `/tp @s -484 203 3618`.
- Verify water containment, vine survival, tree support, route clearance, and overall scale in-game.

## Current User Request Implemented - Metro Supports and Host-Adaptive Oasis Locator

Request:
- Add downward side supports to the overhead beams in the spawn metro so the ribs do not float.
- Make oases locatable as generated points of interest.
- Make each oasis adapt to and visibly grow from its host structure instead of reading as an unrelated overlay.

Spawn metro:
- Every two-block-wide canopy rib at the spawn station now has paired two-block-wide side supports.
- Supports align with the outer platform edges at `|z|=21..23` and descend continuously from the canopy at `baseY+12` to the platform at `baseY+1`.
- Four-block panel banding breaks up the long columns while retaining the existing foundation material.
- Rail beds, central travel lanes, platform interiors, lamps, and access stairs remain clear.

Oasis discovery:
- Registered operator command `/megastructure locate_oasis` with default search radius `64` districts.
- Registered optional `/megastructure locate_oasis <radius>` form with range `1..256` districts.
- Search uses the same deterministic descriptor cache and selection rules as world generation.
- Result reports host structure, oasis profile, block distance, coordinates, and a ready `/tp` command.
- Returned coordinates are a dry viewing point `basinRadius+8` blocks perpendicular to the waterfall, not the water-column center.
- Command rejects dimensions that do not use `MegastructureChunkGenerator`.
- Oases remain overlays of their host biome; they are intentionally not exposed as a fake standalone biome.

Host-adaptive oasis anchors:
- `tank_cluster`: selects one of the five generated tanks and reuses its actual center and generated radius.
- `reservoir_hall`: uses the center of the existing nested reservoir system and its outer wall.
- `reactor_cathedral`: attaches to the reactor shell and places the catchment outside it.
- `ring_vault`: selects one real ring radius and its matching vertical level.
- `scaffold_chamber`: attaches to a real intersection in the shifted structural grid.
- `column_forest`: resolves a real column from that district's generated grid shift.
- `monolith_hall`: emerges from the central monolith base seam.
- `fractured_habitat`: selects one generated habitat module and uses its actual center and half-size.
- `conduit_basilica`: selects one of the two existing main conduits and spills outward.
- `hanging_archive`: attaches to the central ceiling anchor and falls between archive slabs.
- `industrial_wall`: preserves the wall-orientation rule and uses the short structural face.
- Per-location seed still varies side, height, basin radius, tree count and placement, wetness patches, vines, pipe collars, and rare rooted dirt.
- Ring-vault and conduit-basilica vines remain disabled where no continuous generic wall can support them.

Guaranteed oasis after the host-anchor refinement:
- District: `(-1, 3)`; host: `monolith hall`; profile: `monolith spring`.
- Basin center: `(-512, 202, 3618)` including water surface.
- Locator viewing point: `(-484, 203, 3618)`.
- Water source Y: `239`; basin radius: `20`; trees: `6`; rooted dirt: absent.

Documentation:
- Updated `OASIS_DESIGN.md` with the implemented locator command and all 11 host adapters.
- Updated `README.md` with locator command usage.

Verification/install:
- `compileJava` succeeded using Gradle 8.8 / Fabric Loom 1.6.12.
- Parsed all `33` JSON resources successfully.
- Full offline `build` succeeded: 6 actionable tasks, 5 executed, 1 up-to-date.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed size/time: `78853` bytes, `2026-06-23 08:31:32`.
- Installed SHA-256: `07BA41A13D3926400F243E9EF0F824123A91B9DC19FEDE6AAA9433A625ABB97E`.
- Source and installed hashes match.

Testing constraint:
- Metro support and oasis geometry changes require new chunks; create a new world for an unambiguous test.
- No automated in-game visual inspection was performed.
- First target: run `/megastructure locate_oasis`, then inspect support contact, water containment, host attachment, tree support, and route clearance.

## Current User Requests Implemented - Procedural Oasis Grammar, Five Districts, Enclosed Spawn and Recurrent Stations

Requests:
- Stop treating oasis host types as if each had one prepared oasis composition.
- Make every oasis globally unique beyond moving the same pipe and trees.
- Recess water inside the pipe instead of attaching a water block to its end.
- Prevent pipes from starting in empty space.
- Remove unsupported moss floating beside railway/connector roads.
- Erode structure blocks around water intersections instead of silently replacing a clean column.
- Keep the five newly proposed oasis families, but also add five new general megastructure districts.
- Remove the guaranteed giant open structure from spawn; start inside concrete at an enclosed metro station.
- Repeat station-class stops farther along railways with variable combinations of rooms and service spaces.

Oasis composition grammar v2:
- Oasis rarity remains `1/40` eligible hosts before three-district spacing arbitration; hosts without an accepted descriptor generate no oasis preparation.
- A host selects one dominant hydrological profile from a compatibility set, then one deterministic modifier and sometimes a second independent modifier.
- Nine rule families exist, but they are not prefab structures:
  `pipefall`, `cistern garden`, `hanging seep`, `monolith spring`,
  `terraced deluge`, `root cathedral`, `broken aqueduct`, `drowned gallery`, `hanging delta`.
- Features `4..8` can combine; the locator/F3 output reports the resulting composition instead of only one label.
- Catchments are unions of `1-4` seeded elliptical lobes. Each lobe varies center offset, X/Z radius, stretch and overlap.
- Water-drop count is independently generated; hanging-delta compositions produce `2-4` drops with independent positions and source heights.
- Terraced compositions produce `2-5` supported basins with variable spacing, radius, height step and cracked foundation pattern.
- Root-cathedral composition varies dominant-tree direction, distance, height `17-31`, crown radius and eight grounded root directions.
- Broken aqueduct uses an open water trough, side walls and repeated paired columns that continue to the host floor.
- Drowned gallery varies elongated water footprint, broken crossing and three supported islands.
- Hanging delta varies `2-5` host-attached shelves, vertical spacing, projection and diagonal braces.
- Trees independently vary radial distance, direction, height, species, lean direction/amount, crown depth and crown radius.
- Tree generation is skipped if the exact root coordinate does not have a solid base surface.
- Wet moss replacement and moss carpet now require a verified solid base surface.
- Connector-road approach moss was removed completely; it was the cause of screenshot-3 floating moss.

Pipe/source corrections:
- Industrial-wall source side is no longer random: it always resolves to the actual rear wall at depth `-108`, with an independently shifted position along the wall.
- Reservoir source selects one real basin radius (`78`, `152`, `232`) and uses the matching wall height.
- Reactor source wall corrected from radius `60` to the real shell radius `58`.
- Conduit-basilica branch corrected to begin on the real conduit coordinate rather than two blocks away.
- Tank pipes use the selected tank's exact generated radius.
- Scaffold, column-forest and hanging-archive anchors now select among real grid nodes instead of a single fixed node.
- Generic pipes extend `1-8` blocks into their support and receive a wall-connected diagonal buttress.
- Physical pipe lip extends `3-8` blocks beyond the fall axis. Water occupies the lower inner channel from the embedded source to the recessed fall point, leaving a visible hollow lip in front.
- Pipe inner radius varies `2-4`; wall thickness, outer radius, collar interval `9-16` and material vary independently.

Water erosion:
- Every falling column tests the base megastructure state at its current Y.
- When the center intersects solid structure, a deterministic erosion radius of `4-6` is applied.
- Inner intersection blocks become air around the water core; the irregular boundary becomes cracked stone or occasional supported moss.
- Route protection still runs first, so rails, connector corridors and central district access are not eroded.

Guaranteed oasis after district-table expansion (supersedes all earlier guaranteed-oasis coordinates):
- District `(-3, -3)`, host `column forest`.
- Dominant profile `hanging delta`; composition also enables `root cathedral`.
- Three-lobe catchment, three water drops and three attached shelves.
- Anchor `(-2687, -2469)`, basin `(-2724, -2469)`, floor Y `-182`, source Y `-94`.
- Locator viewing point `(-2724, -180, -2439)`.
- Basin nominal radius `22`; pipe inner radius `4`, outer radius `6`, recessed lip depth `8`.

Five new general structure districts:
- `suspended_city`: seeded suspended modules, variable dimensions/levels, ceiling tethers, selected floor pylons and multi-level skyways.
  Locate: `/locate biome megastructure:suspended_city`.
- `iris_chasm`: orientation-varying long chasm with paired bulkheads, seeded circular apertures, rusted iris rims and occasional cross bridges.
  Locate: `/locate biome megastructure:iris_chasm`.
- `machine_root_vault`: cylindrical vault with central trunk and ten seeded tapering machine roots connecting ceiling core to the floor perimeter.
  Locate: `/locate biome megastructure:machine_root_vault`.
- `tilted_stacks`: seven independently seeded grounded habitat stacks whose centers shift with height, plus three transfer levels.
  Locate: `/locate biome megastructure:tilted_stacks`.
- `silent_foundry`: large assembly floor, crane columns/rails, incomplete colossal hull, gantries and inspection spine.
  Locate: `/locate biome megastructure:silent_foundry`.
- District roll expanded from `220` to `270`; a 61x61-district deterministic model found `113-142` instances of each new type.
- All five have explicit biome footprints and are included in biome source codec, world preset and overworld biome tag.
- Total district types are now `27` (`0..26`).

Spawn isolation:
- District `(0,0)` is explicitly `dense_wall`, preventing a random giant chamber from occupying spawn.
- Removed the 1152-scale spawn ellipse, giant landmark, giant open floor and direct landmark walkway.
- Spawn station is a local concrete chamber: approximately 256 blocks long, 58 blocks wide and 13 blocks high.
- Primary railway tunnels continue through concrete beyond both station ends.
- Spawn includes two distinct side rooms: northern waiting/access room and southern service room.
- Existing vertical stair now has a dedicated shaft opening through the northern room roof and remains connected to the upper connector network.
- Canopy ribs retain paired edge supports down to the platforms.

Recurrent stations:
- Primary line checks 1536-block segments outside segment `0`; raw chance is `1/2`, then rift conflicts are rejected.
- Current deterministic first examples: approximately `X=-819` and `X=4198` on the primary railway.
- Secondary-line station selection remains district-aware.
- All distant stations now have platforms, enclosed roof, side columns, end portals and a real transverse vestibule with floor, roof and walls.
- Eight side-room slots are evaluated independently at each station.
- Four density modes produce sparse singles, selected pairs, mixed groups or dense clusters.
- Four room interiors: control console, maintenance frame, waiting benches, divided service room.
- Room doors are cut only for accepted rooms; absent slots remain solid station walls.

Documentation:
- Updated `OASIS_DESIGN.md` to composition grammar v2 and documented erosion/deep-pipe behavior.
- Updated `README.md` with enclosed spawn, recurrent stations and five new locate commands.

Verification/install:
- `compileJava` succeeded after oasis, station and district passes.
- Parsed all `38` JSON resources successfully.
- Full Gradle build succeeded: 6 actionable tasks, 5 executed, 1 up-to-date on final pass.
- Installed jar:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`
- Installed size/time: `93654` bytes, `2026-06-23 15:31:44`.
- Installed SHA-256: `1083D297AF50E6004AF3757ADDD96712314D17D88805E0324D454F5E3A7F4F0E`.
- Source and installed hashes match.

Testing constraints:
- A completely new world is required. District IDs, biome-source codec, spawn district, station masks and oasis descriptors all changed.
- No in-game visual pass was available; generated screenshots are still required to tune scale, darkness, water physics and room density.
- Highest-priority checks: spawn enclosure, stair clearance, first primary station at `X=-819`, guaranteed oasis near `(-2724,-180,-2439)`, and each new biome via `/locate biome`.

## Known Issues / Next High-Value Fixes

### Discord Avatar Iteration

- Generated a square project avatar based on the megastructure visual language.
- Current revision request: improve long-distance and small-size readability.
- Edit target: preserve the circular shaft, central monolith, bridge, and tiny human scale cue.
- Required changes: larger central silhouette, stronger tonal separation, brighter bridge, less facade noise, and clear recognition at Discord server-list size.

- Need in-game visual inspection after latest build; no screenshot verification yet.
- Need inspect new `industrial_wall` district in-game; it is intentionally a first implementation of the document's "cutaway technical city" concept.
- Need check double rails render acceptably after crash fix; rails now use default state to avoid invalid static property crash.
- Need test `/locate biome` in a newly created world using the megastructure preset.
- Need inspect railway tunnels: route density, tunnel height, rail orientation, and rift bridge behavior.
- Railway is a thin biome overlay; `/locate biome megastructure:railway_tunnel` should work, but exact hit position may be on/near a narrow strip.
- Need inspect titan tower hall in-game; it is now radius 500 and may be very expensive visually but should finally read as titanic.
- Need test actual player traversal on `squareStairState`; if Minecraft stair facing is inverted, flip directions in helper once.
- Need verify 1/4 rift bridge chance feels "rare but findable"; tune to 1/5 or 1/6 if too common.
- Need inspect the new `BLOCK_TOWERS`, `TANK_CLUSTER`, and `SCAFFOLD` districts in-game; they are newly added and may need scale/spacing tuning.
- Verify rift suspension chains are acceptable because they are now only on rare planned bridges; remove even those if screenshots still read badly.
- Verify `GRATE=POLISHED_DEEPSLATE` reads as solid mechanical trim and not as railing; rename later if useful.
- Need verify top-down view no longer reads as repeated textile/tile pattern.
- New district setpieces are coarse coordinate masks; expect further visual/route refinement after screenshots.
- Service stair may still be abstract/blocky; test player traversal and adjust step cycle if needed.
- Verify in-game that corridor-attached room slots visibly read as rooms and not just alcoves.
- If still too dense, reduce room slots or increase missing-room probability.
- Door blocks issue should be fixed: actual `IRON_DOOR` removed.
- `cylindricalShaftStairState` is currently ring walkway, not true continuous spiral stairs.
- Generation is still single-function coordinate logic; if complexity grows, split into mask classes/passes:
  `RiftMask`, `InteriorNetwork`, `VerticalCirculation`, `LargeVoidMotifs`, `DecorPass`, `Palette`.
- Need optional debug command or config to locate motifs for testing.
- Need consider performance: height 1152 means per chunk work is large; optimize with section skipping/mask intervals later.

## Build / Install Commands

Build:
`.\gradlew.bat build`

Install:
`Copy-Item -LiteralPath 'C:\Users\nikit\Documents\New project\build\libs\megastructure-world-0.1.0.jar' -Destination 'C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar' -Force`

Verify installed:
`Get-Item -LiteralPath 'C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar' | Select FullName,Length,LastWriteTime`

## Assistant Protocol

On every future user request in this project:
1. Update `TECHLOG.md` with the new request, actions, build/install status, and next known issues.
2. Include a separate `TECHLOG UPDATE` section in the final response.
3. Keep `TECHLOG UPDATE` optimized for assistant context transfer, not human prose.
4. If code is compiled, copy latest jar to target mods dir unless user explicitly says not to.

## 2026-06-23 - Titan branching center, oasis topology repair, districts 27-28

User request:
- Make `titan_tower_hall` a branching center. Every road leaving it must continue through the
  world network to other structures. Add unique internal distribution rooms and make the tower
  read as a working skyscraper.
- Repair oasis topology shown in eight screenshots: irregular smaller roots with all-face wood,
  no trees on water, no unsupported pipe/water segments, erosion only at real impact surfaces,
  no basin/tank-wall intersections, and tank walls touching their floor.
- Add four non-pipe oasis origins, two new general generation types, clickable oasis locate output,
  and a researched non-shader visual-gigantism plan.

Implemented source changes:
- `DISTRICT_BLOCK_TOWERS`/`titan_tower_hall` central radius-50 tower is now a hollow workplace
  shell with 32-block service floors, central atrium, two distribution levels, platform bands,
  paired distribution rooms, portals and connector-network approach decks.
- The existing global district connector graph remains higher-priority than district geometry.
  Titan station roads therefore cross the tower and use shared district-edge coordinates to
  continue deterministically into neighboring structures. Added grounded support pylons under
  exposed connector spans inside the 500-radius hall.
- Removed the former detached radius-184 rings and finite random far bridges from the titan hall.
- Added oasis origin dimension independent from scene profile:
  `ruptured conduit`, `condensation canopy`, `structural seam seep`, `pressure spring`,
  `abandoned filtration bed`.
- Thin scaffold/column/archive hosts cannot select giant pipe origin. Only ruptured-conduit origin
  generates a pipe. Condensation falls begin below attached collector plates; seam/spring/filter
  origins use no unsupported vertical water column.
- Waterfall landing, basin water and terraces now require an open supported floor site. Circular
  tank/reservoir/ring basins are clamped inside host radius. Tank profiles exclude unsafe terrace,
  aqueduct and drowned-gallery families.
- Erosion still bores the water channel through solid material, but cracked/moss impact rings are
  emitted only where the blocked center transitions to free space above or below.
- Tree placement rejects basin and terrace footprints. Root-cathedral placement searches for a
  valid dry site; roots are 4-6 seeded arms with independent direction and 5-13 reach, using
  `OAK_WOOD` for all-face bark instead of a perfect 8-way `OAK_LOG` star.
- Tank cylinder walls now begin at `baseY+1` instead of `baseY+4`.
- Added district 27 `colossus_lift`: grounded guide towers, seeded enclosed carriages, four
  transfer levels and wall stations.
- Added district 28 `folded_city`: four offset nested workplace/habitat shells, repeated internal
  floors, portals and linked transfer axes.
- District roll expanded from 270 to 290. Both new districts have biome source codec entries,
  world-preset entries, biome JSON and overworld tags.
- `/megastructure locate_oasis` coordinates are green, underlined and clickable. Click action is
  `SUGGEST_COMMAND` with the exact `/tp @s x y z`; hover text explains the action.

Visual research/plan:
- Added `VISUAL_SCALE_PLAN.md` with official Epic, Godot, Fabric and Yarn sources plus perception
  research. Proposed client-only `BackgroundRenderer.applyFog` integration, chamber probes,
  smoothed profile transitions, distance bands, compatibility constraints and screenshot matrix.
- This request produced a plan only; visual fog hooks are intentionally not implemented yet.

Verification status at this checkpoint:
- JSON parse validation succeeded for 40 resources.
- `compileJava` initially hit the DFU 16-field `RecordCodecBuilder.group` limit after adding two
  biomes. Resolved with nested `NewestDistricts` codec.
- `compileJava` then succeeded.
- Full build, jar copy and installed hash are pending the final verification pass below.

Final verification/install:
- `clean build --offline --no-daemon` succeeded: 7 actionable tasks, 7 executed.
- Parsed all 40 JSON resources successfully.
- Removed the final unused `BlockPalette.chain` helper after the forbidden-material scan; the
  generator now has no chain spawn path.
- Final rebuild/install hashes are recorded in the completion response and should be treated as
  authoritative over the earlier checkpoint above.
- Final post-cleanup build succeeded: 6 actionable tasks, 5 executed, 1 up-to-date.
- Installed jar size/time: 102397 bytes, 2026-06-23 18:02:37.
- Source and installed SHA-256:
  `2920B4F9594DCE28B0BB9279603AC66A5596A08625C427E61B55F232B26D2510`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Final forbidden-material scan: zero references to black/gray concrete, iron bars or chain blocks.
- New world required because district roll, biome-source codec, titan geometry and oasis descriptor
  schema changed. No in-game screenshot pass was available in this run.

## 2026-06-23 - Volumetric giant oases, hydraulic sources, graded connector graph, client atmosphere

User request:
- Remove continuous downward bridge extrusions near `titan_tower_hall`; retain sparse supports.
- Let all inter-structure routes climb and descend with adjacent walkable one-block height changes.
- Connect wall-rift bridge rooms to the nearest global route.
- Repair disconnected pipe/water examples and guarantee a visible source for every oasis.
- In giant rooms, make oases much larger and three-dimensional: sparse floor spread, moss climbing
  structures and columns, attached vines, and a rare giant tree. The result must resemble an ancient
  natural infection of an industrial hall rather than a pasted green plane.
- Begin a high-quality custom first-person visual scale system rather than relying on vanilla fog.

Generation implementation:
- District connectors now interpolate between deterministic centre and shared edge elevations over
  512 blocks. The bounded slope changes by at most one block per horizontal block and preserves the
  cross-district graph while allowing large vertical variation.
- Titan bridge supports use path tangent and along-path 64-block spacing. The former test on both
  world axes, which could turn a support into a continuous wall, was removed.
- Each generated rift bridge receives an enclosed L-shaped service link from its side junction to the
  district access shaft and therefore into the global connector graph.
- Oasis origins were hydraulically unified. Conduit mouths end at the stream; condensation has one
  collector/drop; seam seep has a wall fall and floor rill; pressure spring has a supported standpipe;
  filtration has a visible channel. Water and source geometry now share one descriptor.
- Giant-oasis hosts include titan tower hall, machine nave, suspended city, silent foundry, colossus
  lift and folded city. Their infection mask reaches up to 360 blocks vertically and basin influence
  extends laterally, but floor replacement probability falls sharply with distance.
- Added structural overgrowth: exposed host surfaces receive sparse moss and air cells receive vines
  only beside actual support. Vines no longer replace solid blocks. Giant hosts can very rarely create
  one 3x3, 38-76-block tree with a broad irregular crown.

Client visual implementation:
- Added client entrypoint, `MegastructureAtmosphereRenderer`, `AtmosphereProfile` and
  `BackgroundRendererMixin`.
- First-person-only renderer uses six depth-tested translucent camera-centred shells. Near geometry
  occludes the shells; distant geometry accumulates atmospheric layers. Seven biome-derived profiles
  cover service, network, titan, abyss, rift, oasis and rail spaces with smooth transitions.
- Vanilla terrain fog is moved behind the custom atmospheric reach only in the megastructure world.
  No FOV, movement, gameplay or world-generation behaviour is changed by the client pass.

Verification checkpoint:
- Generator and client sources compile successfully.
- Full clean build succeeded before this documentation pass.
- Development client reached the main menu without mixin apply failures, injection errors,
  `NoClassDefFoundError`, OpenGL errors or fatal crashes. The expected offline-session HTTP 401 is not
  a mod failure.
- No in-world screenshot calibration was completed. Atmospheric profile values remain provisional
  until fixed camera captures at 8/16/32 chunks are reviewed.
- Final clean build, resource count, install size and SHA-256 are recorded below after completion.

Final verification/install:
- `clean build --offline --no-daemon --console=plain` succeeded in 20 seconds: 7 actionable
  tasks, 7 executed.
- Parsed all 41 JSON resources successfully.
- Forbidden-material scan returned zero references to black/gray concrete, iron bars or chain
  blocks.
- Installed jar size/time: 115877 bytes, 2026-06-23 20:32:35.
- Source and installed SHA-256 match:
  `CED06A2F9C310EAD00AB161F564CF793079D9870CBEA71DCD8DD18A76B116663`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- A new world is required because connector elevations, titan supports, rift access links, oasis
  descriptors and giant-host overlay geometry changed.

## 2026-06-24 - Voxy Fabric 1.20.4 port and Iris 1.7.2 compatibility

User request:
- Port the supplied current Voxy source and `MCRcortex/voxy` to Fabric 1.20.4 with minimal bugs.
- Verify compatibility with Iris.

Source strategy:
- Preserved the supplied current `dev` archive as
  `voxy-1.20.4-port/modern-reference`.
- Cloned upstream Git history into `voxy-upstream` and created worktree/branch
  `codex/1.20.4-port` at `16e37d9b776db4f84b7df7852c080328dfaa9998`, the final upstream commit before
  the Minecraft 1.21 migration. This avoids unsafe binary backporting of the modern Sodium 0.8 ABI.
- Port source is `voxy-1.20.4-port/source` and targets Java 17, Minecraft 1.20.4, Fabric API 0.97.1,
  Sodium 0.5.8 and Iris 1.7.2.

Port changes:
- Updated Iris shadow-state integration from removed package
  `net.coderbot.iris.pipeline.ShadowRenderer` to
  `net.irisshaders.iris.shadows.ShadowRenderer` used by Iris 1.7.2.
- Fixed first-run crash in `ContextSelectionSystem`: a new `VoxyConfig` previously left
  `defaultSaveConfig` null. New and corrupt profiles now validate non-empty JSON and a non-null
  storage backend, then fall back to built-in RocksDB + ZSTD and persist the repaired world profile.
- Added conditional `VoxyMixinPlugin`; the Nvidium mixin is skipped when Nvidium is absent, removing
  the missing-target warning without deleting optional compatibility.
- Updated build tools to Gradle 8.8 / Fabric Loom 1.6.12 and marked the port version
  `0.1.5-alpha+mc1.20.4-iris1.7.2-port.1`.
- Development runtime accepts actual Cloth Config/basic-math jars via optional Gradle properties.
  Production Voxy does not embed or hard-code local paths.
- Added `PORT_NOTES.md` with provenance, runtime matrix and design boundary.

Compatibility verification:
- Clean build succeeds on Java 17 with Fabric API 0.97.1.
- Isolated no-shader Iris 1.7.2 client reached the title screen and initialized the Iris pipeline.
- Active-shader test used `ComplementaryUnbound_r5.5.1.zip`, entered a copied single-player world,
  initialized RocksDB/ZSTD storage and Voxy `NvMeshFarWorldRenderer`, and survived renderer reloads.
- Final 82-second exact-stack run used Fabric API 0.97.1, Sodium 0.5.8, Iris 1.7.2, the assembly's
  Cloth Config 13.0.114/basic-math 0.6.1 and active Complementary. Counts: zero fatal, zero mixin
  apply/injection, zero missing classes, zero OpenGL errors and zero Voxy exceptions.
- Complementary r5.5.1 emits expected 1.20.4 warnings for newer `BIOME_PALE_GARDEN` and trial-spawner
  properties; Iris ignores them and Voxy remains operational.
- Initial dev-runtime failures for Iris nested `jcpp`, `glsl-transformer` and Cloth `basic-math`
  were test-classpath issues caused by Loom stripping nested jars, not production Voxy defects.

Final production build, installed size and SHA-256 are appended below after completion.

Final production build/install:
- Removed the test-only quick-play run argument before packaging.
- Java 17 `clean build --no-daemon --console=plain` succeeded with Gradle 8.8 / Loom 1.6.12:
  8 actionable tasks, 7 executed, 1 up-to-date.
- Production artifact metadata: mod id `voxy`, version
  `0.1.5-alpha+mc1.20.4-iris1.7.2-port.1`, Minecraft `1.20.4`.
- Verified no absolute user path, `compat-world` or quick-play argument remains in production source.
- Installed artifact size: 68730580 bytes.
- Source and installed SHA-256 match:
  `B0C57A65D173BA8FEAB3F05B7A5034B14FEA118BF2E5C40993662D9B7A3D32B3`.
- Installed over the previous Voxy jar at:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\voxy-0.1.5-alpha.jar`.

## 2026-06-24 - Railway station grammar and persistent frame-smear repair

User request:
- Make generated railway stations substantially less repetitive. Rooms and decoration must form
  deterministic single, paired, tandem and dense combinations instead of one fixed station shell.
- Repair the intermittent persistent world-view smear where old frames remain projected through the
  world while GUI layers still render normally.

Frame-smear diagnosis and repair:
- The assembly log captured the exact failure sequence. BBS threw an NPE while opening its texture
  dashboard and immediately logged `[BBS film] setCustomSize customSize=true w=0 h=0`.
- A 0x0 custom world framebuffer explains all reported symptoms: the world color target is no longer
  cleared or redrawn, old frames accumulate, and menus remain intact because GUI rendering uses a
  later layer. This is client render state, not world-save corruption.
- Added an optional reflection bridge to BBS. On client tick and at world-render start it detects an
  active custom size with a non-positive width or height, disables custom sizing, resizes Minecraft's
  main framebuffer to the current window framebuffer and restores the viewport. Positive custom BBS
  export resolutions remain untouched and BBS is not a required dependency.
- Hardened the megastructure atmosphere pass. It now refuses to draw into an invalid framebuffer and
  saves/restores the actual blend, depth-test, depth-write, depth-function, cull, blend-function and
  shader state in `finally`, instead of assuming vanilla defaults after Voxy/Iris/BBS rendering.

Railway station generation:
- Added eight deterministic occupancy programs: isolated room, same-side pair, mirrored pair,
  mirrored tandem, asymmetric chain, central cluster, organic medium-density layout and dense
  interchange layout. Every station can therefore compose rooms singly, in pairs or in larger groups.
- Added twelve functional room families: dispatch room, workshop, waiting room, archive, pump room,
  power room, checkpoint, locker/service room, signal relay, partitioned office, cable chamber and
  broken abandoned room.
- Added independent seeded decor layers: ceiling lamps, roof ribs, attached copper service lines,
  floor staining, structural columns, wall bands and floor grates. Decor combines with room function
  instead of selecting one monolithic room template.
- Adjacent rooms on one side now receive enclosed walkable tandem connectors and carved internal
  portals. Station-scale architecture has six variants including roof ribs, signal beams, platform
  canopies, a supported transfer bridge, monumental portals and compact service markers.

Verification checkpoint:
- Java sources compile successfully on Minecraft 1.20.4 / Fabric Loom 1.6.12.
- Final clean build, resource validation and installed artifact hash are appended below.

Final verification/install:
- `clean build --offline --no-daemon --console=plain` succeeded in 20 seconds: 7 actionable
  tasks, 7 executed.
- Parsed all 41 JSON resources successfully. The forbidden-material scan found zero references to
  black/gray concrete, iron bars or chain blocks.
- Development client loaded the mod, mixins, resources and renderer without mixin apply failures,
  missing classes, OpenGL errors or fatal exceptions. The expected offline profile/Realms 401 errors
  are unrelated to this mod. The timed smoke-test processes were explicitly stopped afterward.
- Installed artifact size: 122431 bytes.
- Source and installed SHA-256 match:
  `603B5FDD7F2D4306CFFBA5A7ECC50184B752D0E016F2D46C2DF49504165E5D04`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Existing worlds receive the framebuffer repair immediately. Newly generated station chunks are
  required to see the expanded room, decor, tandem and architecture grammar.

## 2026-06-24 - Moss carpet support invariant

User report:
- Oasis moss carpet was replacing railway/industrial floor blocks instead of occupying the air cell
  above them.

Root cause and repair:
- The oasis overlay checked that the cell below was structurally solid but did not check that the
  carpet's own base-generation cell was air. Because oasis overlays run before normal district
  geometry is returned, the carpet state replaced any structural block occupying that coordinate.
- `MOSS_CARPET` now requires both conditions: a solid base-generation support directly below and an
  empty base-generation cell at the carpet coordinate. Full moss blocks remain the explicit material
  used where biological growth is intended to replace a surface block.
- Final build, validation and installed artifact hash are appended below.

Verification/install:
- Clean Fabric build succeeded: 7 actionable tasks, 7 executed.
- Parsed all 41 JSON resources. Static scan confirms exactly one `MOSS_CARPET` placement branch and
  that it checks both empty current cell and solid support below.
- Installed artifact size: 122438 bytes.
- Source and installed SHA-256 match:
  `9DCBD3ED94FC2997DA4F490A57787DEAD411551D448832DC1149ACCD2F3673D6`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- The correction applies to newly generated chunks. Already generated misplaced carpet remains stored
  in existing chunk data and is not rewritten automatically.

## 2026-06-24 - Transit nexus through-rail interchange

User correction:
- The railway crossing the center of `transit_nexus` still did not continue as a real railway and the
  tower lacked its requested unique, potentially multi-storey station.

Root cause:
- The solid 56x56 central load-bearing core was evaluated before both railway decks, so it replaced
  the track and passage through the exact center of the tower.
- Internal X and Z rails were limited to decorative deck extents and had no generated continuation
  through the surrounding megastructure mass.

Implementation:
- Added dedicated transit-nexus railway routing before generic railways. Every nexus now emits a
  3584-block X approach at the lower level and a 3584-block Z approach at the upper level, with
  continuous double rails, walkable deck, enclosed tunnel walls, roof ribs and sparse supported lamps.
- The lower X line is continuous through station edge `252`; the upper Z line is continuous through
  station edge `188`. Axis-specific approach boundaries prevent hidden gaps between station and tunnel.
- Reordered the central geometry contract: railway floors and rails are resolved before the tower
  core, and two protected passage bands carve through the core instead of being filled by it.
- Added a unique two-storey interchange: lower and upper platforms, seeded weathering, canopies,
  grounded columns, eight possible distribution/service rooms per floor pair and four room programs
  (dispatch console, archive bank, utility riser and partition office).
- Added a supported two-flight transfer stair between levels. Every successive stair rises by one
  block, both flights have an underside support layer, and intermediate/access landings connect the
  lower X platform to the upper Z platform.
- Final clean build, resource checks and installed artifact hash are appended below.

Verification/install:
- Clean Fabric build succeeded: 7 actionable tasks, 7 executed.
- Parsed all 41 JSON resources; forbidden-material scan returned zero references.
- Static continuity checks passed for the lower station/tunnel seam at `252/253`, upper seam at
  `188/189`, axis-specific approach limits and both one-block-rise stair flights.
- Installed artifact size: 124967 bytes.
- Source and installed SHA-256 match:
  `AE5F4CAEBBD66D9BA24D587399B60318586C8542DF8904353D45B5DE435475CD`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- The transit-nexus redesign appears only in newly generated chunks; existing nexus chunk data is not
  rewritten automatically.

## 2026-06-24 - Seed-varied spawn ensemble and connected moss colonies

User request:
- The spawn station must not always be the same. A random giant structure must be guaranteed nearby,
  but its type must also vary between worlds.
- Moss must never appear as disconnected single blocks. Its shape must remain deterministic and every
  visible colony must connect back to its original growth focus.

World-seed integration:
- The generator previously used one constant `SHAPE_SEED` for all geometry and forced district `(0,0)`
  to dense wall. Minecraft's selected world seed therefore had no effect on the spawn composition.
- Added a separate `worldVariantSeed` derived from `NoiseConfig` with the namespaced
  `megastructure:spawn_variant` random splitter. It is initialized before biome population and before
  any block-column query, then propagated to the generator's own `DistrictBiomeSource` instance.
- This seed affects only the spawn ensemble. Existing global district/noise arrangements continue to
  use `SHAPE_SEED`, preserving the established world identity and locate layout outside spawn.

Spawn station grammar:
- Added six seed-selected station families: bilateral service station, vaulted rib concourse,
  maintenance/crane depot, split-level mezzanine station, checkpoint/distribution station and broken
  terminal.
- Every family owns different hall dimensions, platform dimensions, support rhythm, roof/canopy
  system and a room plan of two to four modules.
- Room modules use four functional programs: waiting benches, supported maintenance rig, dispatch
  consoles and partitioned service office. Every layout reserves the same safe main railway and keeps
  the intentional stair shaft from platform level to the district connector network.

Nearby giant landmark:
- Exactly one of the three districts touching the negative side of spawn is seed-selected as the
  nearby landmark district. This leaves a substantial concrete approach between station and landmark
  instead of placing the structure against the player.
- The landmark type is selected from twelve giant-capable districts: titan tower hall, transit nexus,
  reactor cathedral, ring vault, machine nave, reservoir hall, suspended city, iris chasm, machine
  root vault, silent foundry, colossus lift and folded city.
- Generator and biome source share the same selection, so `/locate biome` reports the actual forced
  landmark type rather than the district that would otherwise occupy that coordinate.

Connected moss model:
- Removed independent per-block chance from floor moss and giant-host surface moss.
- Each oasis now emits seven deterministic growth veins; giant hosts emit twelve. Every vein begins
  inside the expanded basin focus and follows a linearly interpolated seeded meander. Minimum widths
  overlap between twelve-block control segments, preserving blockwise connectivity to the focus.
- Full moss occupies the connected vein field. Moss carpet may remain visually sparse, but it is only
  placed in air directly above a solid block belonging to that connected field, so each carpet block
  physically contacts the colony below.
- Giant-host vertical infection reuses the same connected horizontal field and climbs exposed
  structural surfaces to a deterministic height. It no longer seeds unrelated green pixels across the
  industrial floor or walls.
- All results are pure functions of world/oasis seed and coordinates; player spawn position, chunk
  load order and restarts cannot alter an established layout.

Verification checkpoint:
- Java sources compile successfully on Minecraft 1.20.4 / Fabric Loom 1.6.12.
- Final clean build, resource validation and installed hash are appended below.

Final verification/install:
- Clean Fabric build succeeded in 20 seconds: 7 actionable tasks, 7 executed.
- Parsed all 41 JSON resources; forbidden-material scan returned zero references.
- Static invariants confirm six spawn-station variants, twelve giant-landmark candidates, shared
  biome/generator world-variant seed, one guarded moss-carpet placement branch and the connected moss
  field implementation.
- Installed artifact size: 128281 bytes.
- Source and installed SHA-256 match:
  `36AC019A0553D1171D9B9CC315C316435357E6E2F1AF2C79EC59D6AC35888954`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- A newly created world is required to select a new station/landmark seed and to generate connected
  moss. Existing chunk NBT is never rewritten by the chunk generator.

## 2026-06-24 - Non-vanilla atmospheric dust field

User request:
- Add exceptionally slow, beautiful dust that falls along irregular curved paths and strengthens the
  first-person perception of the megastructure.
- The result must not resemble a vanilla particle effect or anything achievable through vanilla
  particle mechanics.

Rendering architecture:
- Added `MegastructureDustRenderer` as a dedicated first-person world-render pass after the custom
  atmosphere shells. It does not register or spawn vanilla particles, use `ParticleManager`, reuse a
  vanilla particle sprite or depend on entity particle simulation.
- Dust is a deterministic world-space field divided into six-block cells around the camera. Cell hash
  controls occupancy, sub-cell origin, fall velocity, drift phase, size, color and rare filament form.
  Moving the camera therefore produces real parallax; particles do not follow the player.
- The active volume covers thirteen by thirteen horizontal cells and nine vertical cells. Biome-aware
  occupancy produces roughly 300-500 visible candidates near the player while all geometry is batched
  into one draw call.

Motion and appearance:
- Fall velocity varies from 0.004 to 0.012 blocks per tick. Two low-frequency lateral oscillations on
  each horizontal axis create a slowly changing, non-linear descent rather than a straight fall.
- Every mote is a camera-facing diamond built from two procedural layers: a broad vertically stretched
  translucent halo and a compact brighter core. No bitmap sprite is involved.
- Rare motes emit three progressively fading, quadratically offset micro-motes. These curved suspended
  filaments reveal slow air-current direction and deliberately break the visual language of vanilla
  square particles.
- Near-camera fade prevents screen-space clutter; distance fade dissolves the local field into the
  atmospheric depth. A separate cycle-edge fade hides the deterministic bottom-to-top wrap, avoiding
  visible popping during long observation.
- Palette varies subtly between cool gray and aged pale dust. Density is higher in service networks,
  rail halls and foundries, lower in abyssal volumes, and lowest in humid tank/reservoir districts.

Compatibility and state safety:
- Dust is disabled outside megastructure biomes, outside first person, while submerged and whenever the
  main framebuffer has an invalid size.
- Depth testing remains enabled while depth writes are disabled, so real walls, beams and columns
  occlude dust naturally. The renderer captures and restores blend state/function, depth state/function,
  depth mask, culling and shader in `finally` for Iris, Voxy and BBS compatibility.
- Final build, runtime smoke-test and installed artifact hash are appended below.

Final verification/install:
- Clean Fabric build succeeded in 19 seconds: 7 actionable tasks, 7 executed.
- Development client loaded the client entrypoint, renderer registration and resources to the title
  screen without mixin failures, missing classes, OpenGL errors or fatal exceptions. Offline profile
  and Realms 401 errors are expected and unrelated.
- Parsed all 41 JSON resources. Static scan confirms zero references to `ParticleManager`,
  `ParticleTypes`, vanilla particle textures or `addParticle`; one batched draw call; curved X/Z flow;
  rare three-part filaments; and guarded render-state restoration in `finally`.
- Installed artifact size: 134940 bytes.
- Source and installed SHA-256 match:
  `80B45F1F55B36360C25957D2B77A4617BB613F78FDEBA8879D73873D73ABA682`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- No in-world visual calibration was possible in the empty development runtime. Density, alpha and
  scale are deliberately conservative and should be reviewed from the next assembly screenshot.

## 2026-06-24 - Traversable grade apertures, infected oases, ruins and player traversal

User request:
- Keep rising district roads visible and usable from their upper approach instead of allowing the
  receiving floor to seal the descending ramp.
- Increase the non-vanilla atmospheric dust population.
- Turn oasis moss into dense, interwoven infection veins; remove cubic wall patches; restore a
  properly supported side pipe and its water discharge.
- Add approximately twenty partially procedural forms of structural destruction.
- Make ordinary minecarts practical long-distance vehicles and add a throwable, retractable,
  extendable thirty-metre grappling rope.

Connector aperture:
- Connector paths still change altitude by at most one block at a time through rounded integer
  interpolation. Their protected volume now follows every local path elevation from one block below
  the tread through eleven blocks above it.
- The generator explicitly writes air through ten blocks of headroom above every connector tread.
  This cuts a continuous slot through receiving floors and ceilings, so the upper end cannot hide the
  descending route. Rift edges retain only their intentional one-block grate boundary.

Dust field:
- The deterministic renderer now evaluates seven-by-seven horizontal cells and five vertical layers.
  Occupancy thresholds increased to 58 in service networks, 54 in rail/foundry halls, 38 in abyssal
  spaces, 32 in humid halls and 46 elsewhere.
- Rendering remains a single custom world-space mesh with curved drift, layered procedural diamonds,
  depth occlusion and full render-state restoration. No vanilla particle type, sprite or particle
  manager is used.

Oasis infection and side pipe:
- Regular oases now grow fifteen deterministic connected veins; giant-host oases grow twenty-six.
  Per-vein width classes produce narrow two-block filaments, medium strands and occasional thick
  arteries, all following interpolated seeded meanders from the basin colony.
- Giant-host wall infection no longer replaces wall blocks with moss cubes. Growth occupies supported
  air as moss carpet and exposed wall-adjacent air as vertically contiguous vines. Every candidate is
  gated by the connected vein field and by a seeded vertical interval.
- The horizontal side-discharge pipe is restored for every primary water-source profile and two thirds
  of other oases. It begins embedded in host structure, runs to a basin-aligned mouth, receives periodic
  floor-reaching supports, contains water through its interior and discharges at the mouth into a
  connected floor rill. Pipe radius, collar spacing, height, side, material and support spacing remain
  seed-dependent.

Procedural ruins:
- Each eligible non-spawn structure district receives up to three seeded ruin sites, while railway and
  connector protected volumes remain untouched. Orientation, dimensions, omission pattern, materials,
  rubble and elevation are deterministic functions of seed and district.
- Twenty motifs are implemented: collapsed ceiling and fallen slab; fallen column; leaning column;
  fallen beam frame; collapsed bridge span; breached wall; pancaked floor plates; broken stair flight;
  ruptured pipe manifold; caved arch; hanging ceiling teeth; split pylon with fallen half; overturned
  machine shell; collapsed balcony; fractured ring; rubble berm; fallen facade panels; sheared support
  field; impact crater; and broken catwalk with a dropped middle section.
- These motifs are not fixed templates: their width, height, radius, axis, missing sections, fracture
  rhythm, debris palette and rubble footprint vary independently. This provides partially dynamic
  destruction without scattering unsupported single blocks across protected transport paths.

Minecart travel:
- `MinecartPhysicsMixin` only activates while a cart is on rails inside a `megastructure` biome and has
  a passenger. It raises maximum rail speed to 0.90 blocks/tick and smoothly accelerates toward a 0.72
  blocks/tick cruise instead of depending on continuous powered-rail strips.
- A stationary occupied cart selects a valid start direction from straight, ascending or curved rail
  shape and passenger view. Existing motion direction is preserved through travel; crouching applies
  progressive braking. Off-rail carts and all carts outside this world retain vanilla physics.

Grappling rope:
- Registered `megastructure:grappling_rope`, a rendered thrown-hook entity, a compact anchor block and
  a climbable rope block. A module is crafted from a lead, tripwire hook and iron nugget.
- Use in air throws the hook with a shallow custom gravity arc. A successful block impact places an
  anchor and deploys up to thirty rope blocks downward. A blocked placement, timeout or fall below the
  world returns the consumed module to its owner instead of deleting it.
- Use directly on a block performs deliberate placement. Using another module on the anchor or any
  connected rope extends the line by another thirty blocks, up to 240. Using an empty hand retracts the
  complete line and returns one module per started thirty-block segment. Breaking the anchor performs
  the same segment accounting; creative mode does not duplicate modules.
- Rope collision is empty but its block ID is appended to `minecraft:climbable`, giving continuous
  player-controlled ascent and descent without pretending that a vertical vanilla chain is a ladder.

Verification checkpoint:
- Java compilation succeeds against Minecraft 1.20.4, Yarn 1.20.4+build.3 and Fabric API 0.97.2.
- Full clean build, JSON validation, runtime client smoke-test and installed artifact hash follow below.

Final verification/install:
- Clean Fabric build succeeded in 20 seconds: 7 actionable tasks, 7 executed; no Java test sources are
  present in the project.
- Parsed all 48 JSON resources with a standards-compliant UTF-8 parser. Static invariants confirm
  nineteen numbered ruin cases plus one default motif, the 15/26 connected moss-vein split, the rope
  climbable tag and zero references to black concrete, gray concrete, iron bars or chain blocks.
- Development client reached the title screen twice. The final run loaded the common and client
  entrypoints, `MinecartPhysicsMixin`, grappling-hook renderer, block models and resources without
  mixin errors, missing model errors, OpenGL failures or fatal exceptions. The transient hook is marked
  non-saveable, removing its initial data-fixer warning. Offline-profile and Realms 401 messages are
  expected development-environment noise.
- Breaking any connected rope segment now resolves the anchor first, removes the complete line and
  returns the correct number of modules, preventing unsupported rope remnants.
- Installed artifact size: 157065 bytes.
- Source and installed SHA-256 match:
  `D1F6B4067CF70BB8B1F23AE2270D244D455EB19E83AE2E7EF3B48A20DF720117`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Connector apertures, oasis geometry, side pipes and ruins require newly generated chunks. Dust,
  minecart physics and the grappling-rope item are available in existing megastructure worlds after
  replacing the JAR.

## 2026-06-25 - Grounded ruins, rail sanctuaries, rope coils and oasis spores

User request:
- Prevent all detached ruin masses; make ruins more frequent, give fallen pieces impact debris and add
  rare railway-side damage.
- Add a six-tick grappling-rope cooldown, a four-string rope coil, the specified grappling recipe,
  sparse station cobwebs and recoverable minecart coupling.
- Guarantee a distant oasis chamber on the primary railway in each direction, between 5000 and 15000
  blocks from spawn.
- Increase organic moss branching, replace square wall inserts, add clay, and correct all shown pipe,
  support and waterfall-to-basin failures.
- Increase andesite, add granite, and render many unique green biological motes in oases.

Ruin support contract:
- Ruin count per eligible district increased from three candidates to five, with only later candidates
  receiving a one-in-five omission chance.
- Every candidate now resolves a cached ground elevation before emitting geometry. A valid ground plane
  must be solid and exposed at the centre and at four samples sixteen blocks away. Thin catwalks,
  bridges, isolated beams and wall ledges therefore cannot host a large fallen ruin.
- The search spans from 56 blocks above to 224 blocks below the proposed architecture-specific level.
  It evaluates the district structure without recursively evaluating ruins.
- Every accepted site writes deterministic radial cracked-panel scars into existing floor blocks and
  distributes small supported fragments around the impact radius. Floating ceiling slabs were removed;
  ceiling teeth became fallen floor debris; overturned shells and fractured rings now begin at the
  contact layer.
- Primary railway beds receive rare seed-selected incidents at 1024-block intervals. Damage is limited
  to cracked bed blocks and supported loose fragments between or beside tracks, leaving rail blocks
  traversable.

Oasis hydrology and infection:
- Side pipes are now selected whenever host geometry provides at least eighteen blocks between a real
  wall anchor and the mouth. Invalid narrow hosts use their native seep/spring origin instead of
  producing detached pipe sections.
- The pipe mouth moved from outside the basin to two thirds of the basin radius, guaranteeing that the
  falling stream lands in generated water. The separate floor rill was removed.
- All open-trough variants were removed. Every side source is now a closed round conduit with seeded
  radius, collars and material. A valid side pipe becomes the oasis's sole water source, preventing a
  second unexplained vertical column.
- Floor-reaching supports are forbidden within `outerRadius + 8` blocks of the mouth. Remaining pipe
  supports occur behind the discharge and cannot visually merge with the falling water.
- Oasis floors now contain deterministic clay pockets below and around wet connected growth.
- Main moss arteries retain 15/26 regular/giant counts. Each artery now emits two or three thin side
  branches from computed points on its meandering centreline, preserving physical connection while
  producing dense capillary networks.
- Giant-host wall blocks may be replaced by moss only when exposed, horizontally connected to the
  floor colony and intersected by one of 10/18 diagonal vertical tendrils. Tendril start, height, slope,
  segmented meander and width are seed-dependent; square hash-cell patches are no longer used.

Primary railway sanctuaries:
- `primaryRailOasisCenter` deterministically selects one positive and one negative X coordinate from
  world seed, independently rounded to sixteen-block alignment and constrained to 5000-15000 blocks.
- Each location cuts a 264 x 176 x 64 chamber through the monolith while preserving both continuous
  primary railway tracks and entry portals.
- Every chamber contains a side-positioned clay/moss catchment, connected vein field, a guaranteed
  28-40-block tree with irregular roots, a wall-embedded round conduit and a waterfall landing in the
  pool. Four architecture programs vary ribs, balconies, overhead frames and lower machinery.
- Pipe braces descend from the ceiling and are omitted around the waterfall, so the water remains a
  visually independent stream.

Materials and station ecology:
- Mass-stone macro cells now select andesite at approximately one in eighteen and granite at one in
  thirty-seven after the rarer raw-stone test. Variation remains blob-scaled rather than per-block
  noise.
- Sparse cobwebs were added only to upper room corners in spawn and procedural railway stations. They
  are not emitted along open tracks or generic walls.

Rope items and recipes:
- Added `megastructure:rope_coil`, crafted as a 2 x 2 square of four strings.
- Grappling rope recipe is now shaped exactly as requested: iron ingots at top-left and top-centre,
  with one rope coil in the crafting-grid centre.
- Throwing or deliberately placing a grappling rope starts a six-tick (0.3 second) item cooldown.
  Failed throws still return their module.

Minecart coupling:
- `AbstractMinecartEntity` now tracks up to two optional partner UUIDs through synchronized
  `DataTracker` fields and persists them in entity NBT. Two slots permit arbitrary-length wagon chains.
- Use a rope coil on the first cart and then a second cart within twelve blocks. Successful coupling
  consumes one coil; selecting the same cart cancels; full, missing, distant and duplicate pairs give
  action-bar feedback without consuming material.
- Crouch-use a linked cart with an empty hand to remove one coupling and return one rope coil. Repeating
  the action removes the second coupling when present.
- Server physics applies a capped spring-damper correction once per pair, targeting 1.45 blocks while
  both carts remain on rails. Existing passenger cruise and crouch braking remain active.
- Client rendering draws a depth-tested ten-segment sagging two-tone procedural rope between loaded
  partners. No entity, chain block or vanilla leash renderer represents the coupling.

Biological spore field:
- Added `OasisSporeRenderer`, independent from the dust renderer and vanilla particle system. Every ten
  ticks it samples actual nearby moss, vines, leaves and clay, then smoothly raises or lowers density.
- Spores rise in slow orbital helices. Each is a rotating three-petal procedural seed built from three
  translucent triangles with per-seed green variation. The field uses one batched draw call, depth
  occlusion, near/far fades and complete render-state restoration.

World-generation deadlock repair:
- Dedicated-server smoke testing exposed an existing non-reentrant lock error in `populateNoise`:
  chunk sections were manually locked and `ProtoChunk.setBlockState` attempted to acquire the same
  `PalettedContainer` lock again.
- Removed the redundant outer lock/unlock loop. Chunk-status generation already owns the target chunk,
  while `setBlockState` performs its own synchronization.
- After the fix, a clean dedicated server generated all 441 spawn chunks of the 3x-height world and
  reached `Done` in 138.419 seconds, producing four valid region files without generator, mixin or
  registry exceptions.

Verification checkpoint:
- Java compilation succeeds after all generator, item, mixin and renderer changes.
- Dedicated server created and loaded `codex-worldgen-smoke`; client reached the title screen with no
  mod, mixin, model or renderer errors. Final clean build, resource validation and installed hash follow.

Final verification/install:
- Clean Fabric build succeeded in 20 seconds: 7 actionable tasks, 7 executed; no Java test sources are
  present.
- Parsed all 50 UTF-8 JSON resources. Exact assertions passed for `II / C` grappling-rope placement and
  the 2 x 2 four-string coil recipe.
- Static scans found zero black-concrete, gray-concrete, iron-bar or chain-block references and zero
  vanilla particle-manager/type calls in `OasisSporeRenderer`.
- Dedicated server applied the common mixin, loaded seven recipes, generated 441 spawn chunks and
  wrote four region files. Client applied client mixins and loaded all models/render registrations;
  only expected offline-profile and Realms 401 messages occurred.
- Installed artifact size: 181282 bytes.
- Source and installed SHA-256 match:
  `C49D9B29F85455F0CA5B74C7E9764B9C7378B8905CC4FF8AF8CA2D61916C3D1E`.
- Installed target:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- New ruin grounding, wall materials, cobwebs, clay, hydrology and distant railway sanctuaries require
  newly generated chunks. Recipes, cooldowns, minecart coupling and client particle rendering work in
  existing worlds after replacing the JAR.

## 2026-06-25 - Rail-chain braking, coil drops, visible spores and extended ruin set

Request scope:
- Force linked minecarts to brake when a player dismounts, so coupled carts do not keep pushing each
  other far down the rail after travel.
- Breaking a linked minecart must drop the rope coil(s) used by its active couplings.
- Long linked minecart chains must not become heavily slowed just because the carts are coupled.
- Minecart collisions should not be able to change the rail course/heading of carts inside the
  megastructure.
- Oasis spore particles were not noticeable enough and need stronger visible presence.
- Add ten more ruin/debris motifs, with several giant grounded fragments such as a fallen column.

Minecart physics contract:
- `MinecartPhysicsMixin` now tracks recent passenger state with local `hadPassenger` and
  `exitBrakeTicks` fields.
- When a passenger leaves a megastructure minecart on rails, the cart receives 36 ticks of strong
  horizontal braking (`0.74` multiplier). Empty linked carts then keep a softer passive brake
  (`0.94`) so chains settle instead of endlessly shoving each other.
- Link-force correction was softened from the older spring behavior. Separation correction is capped
  at `[-0.045, 0.060]`, vertical transfer is reduced, and passenger-driven carts gently pull trailing
  linked carts toward 78 percent of the driven cart speed. This keeps chains usable without making
  every extra cart feel like a heavy vanilla drag penalty.
- Each tick in a megastructure rail biome, cart velocity is projected onto the current rail axis. This
  removes sideways collision impulses and prevents minecart-to-minecart pushing from changing the
  intended rail course.
- `pushAwayFrom` is cancelled for minecart-vs-minecart pushes while on rails inside the megastructure.
  The carts still remain physical vehicles, but vanilla side knockback no longer rewrites their
  movement direction.

Minecart link drops:
- Added `VehicleEntityMixin`, registered in `megastructure.mixins.json`, injecting
  `VehicleEntity.killAndDropSelf`.
- The mixin filters to `AbstractMinecartEntity` and delegates to
  `MinecartLinking.dropLinkedCoils`.
- `dropLinkedCoils` drops one `megastructure:rope_coil` per active link, removes the destroyed cart
  UUID from all loaded partners, then clears the destroyed cart links. This prevents duplicate drops
  from the surviving cart later.

Oasis spore visibility:
- `OasisSporeRenderer` is still a custom batched renderer and still avoids vanilla particle APIs.
- Horizontal render cells increased from 7 to 10, vertical cells from 4 to 6, and visible distance from
  31 to 44 blocks.
- Activation threshold lowered from `0.025` to `0.010`; local scan radius expanded; moss/leaves/vines
  now contribute more strongly; water also adds minor oasis context.
- Density now reaches 98 percent in high-biomass areas, particle size is larger, and green color/alpha
  were raised so spores are visible as an actual living haze rather than only faint dust.

Ruin/debris generation:
- Ruin type selection expanded from 20 motifs to 30 motifs.
- New large bounded reach helpers allow the bigger motifs to render without clipping at the previous
  72-block horizontal / 112-block vertical limit.
- New motifs 20-29 are grounded variants only: giant fallen column; torn ceiling slab; leaning facade
  chunk; ruptured pipe crescent; broken stair spine; collapsed arch ribs; fallen machine hull; pancaked
  heavy plate; snapped vertical conduit; rubble cascade.
- All new motifs start at ground-relative `dy >= 1` and rely on the existing `findRuinGroundY` support
  scan, so they are generated as fallen debris on broad supported floors rather than floating slabs in
  void.

Verification checkpoint:
- Clean offline Fabric build succeeded after the changes.
- First attempt exposed an inherited-method mixin warning for `remove`; implementation was moved to
  `VehicleEntity.killAndDropSelf`, after which build output was clean except normal deprecated API
  notes.
- Fresh remap JAR installed to
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Source and installed artifact size: 186114 bytes.
- Source and installed SHA-256:
  `FB1C1D40BE364443EBF5936D25C3A3D8CE3F7C13DA538D8B3CBCDBB6CF0C5C39`.
- Dedicated server smoke loaded the common mixins and reached:
  `[11:02:39] [Server thread/INFO] (Minecraft) Done (39.564s)!`.
- Smoke-log scan found no `Mixin apply failed`, `InvalidInjectionException`, `InjectionError`,
  `Exception in server tick loop` or `Caused by:` markers.
- Static scan found no black/gray concrete, chain block, iron-bar, `ParticleTypes`,
  `particleManager` or `addParticle` references in source/resources.
- New world-generation ruin motifs require newly generated chunks. Minecart physics, link drops and
  client spore visibility work in existing worlds after replacing the JAR.

## 2026-06-25 - Spawn singularity transfer debris

Request scope:
- Add small random human-scale debris near world spawn.
- Lore: players are scientists pulled into this reality layer by a singularity; fragments of their
  original environment and equipment were pulled in with them.
- Debris should include micro wooden fragments, small wall pieces and quartz shards, approximately
  "thirty planks" worth of transferred material, not a large ruin field.

Spawn transfer debris design:
- Added palette entries for `OAK_PLANKS`, bottom `OAK_SLAB`, `QUARTZ_BLOCK` and bottom
  `QUARTZ_SLAB`.
- Added `spawnTransferDebrisState`, evaluated inside `spawnPrecinctState` after rail placement and
  before generic platform/architecture placement. Rails therefore keep priority over debris.
- Debris is restricted to the spawn precinct: platform side bands, station hall side areas and
  interiors of generated spawn station rooms.
- Debris is forbidden on the primary railway footprint and on the low platform edge line, so it does
  not block rails or replace the station edge/border.
- The field uses up to 52 deterministic seeded sites, with later sites sparsely skipped. This creates
  roughly thirty small readable fragments depending on station variant and overlap.
- Debris families:
  - wooden planks/slabs in short 1-5 block strips;
  - quartz wall chips as small slab/block fragments;
  - cracked stone-brick wall shards;
  - mixed circular scatter clusters;

Refinement:
- Reworked spawn debris from a single generic scatter field into four explicit layers:
  - `spawnTransferEpicenterState` creates a few compact singularity-transfer epicenters with denser
    material and a readable center/periphery composition;
  - `spawnTransferWoodState` handles the light human-scale wooden transfer debris, visually reading
    as roughly "thirty planks" spread across several micro-sites rather than one ruin pile;
  - `spawnTransferWallShardState` handles quartz and wall fragments from the original environment;
  - `spawnTransferEquipmentState` adds small scientific-equipment-like fragments using grates,
    lamp/slab elements, rusted pipe fragments and quartz pillar remnants.
- The result is now explicitly lore-shaped instead of just decorative noise: spawn reads as a small
  transfer accident where people, station-adjacent material and lab/environment fragments were torn
  through together.
  - rare 2-block-tall tiny piles from oak planks or quartz blocks.
- Centers are selected from station platform side bands and occasionally from actual spawn room
  interiors, so the result reads as matter that arrived with the group rather than megastructure
  damage.

Verification checkpoint:
- Clean offline Fabric build succeeded after adding the debris layer.
- Static source scan confirmed the new spawn transfer debris methods and palette entries are present.
- This change affects newly generated spawn chunks only; existing already-generated spawn chunks must
  be regenerated or recreated to show the debris.

## 2026-06-25 - Primitive loose-stone knapping

Request scope:
- Add a special ground-spawned stone fragment near spawn and transfer-debris areas.
- The fragment must be collectible with right-click, placeable back on the ground, and usable as a
  primitive knapping target.
- Two fragments are required: one lies on the ground, the second is spent as the striking stone.
- Hitting opens a minigame UI with a moving marker and highlighted hit band. Successful strikes
  advance the target from stage 0 to stage 5; the striking stone always breaks on completion.
- Three misses break the striking stone while the worked target keeps its current stage in the world,
  so the player can continue later with another fragment.
- Final reward: a crude stone pickaxe with wooden-pickaxe mining level and half-ish durability.

Implementation:
- Added `PrimitiveSurvivalContent` registry set with:
  - `megastructure:loose_stone` block + item;
  - `megastructure:crude_stone_pickaxe`;
  - `LooseStoneBlockEntity`;
  - `megastructure:loose_stone_knapping` extended screen handler.
- `LooseStoneBlock` is a small ground fragment with five stored phases (`phase=0..4`), floor-support
  checks, empty-hand pickup at phase 0 and server-side knapping entrypoint.
- `LooseStoneItem` is a custom `BlockItem`: using it on an existing loose stone starts knapping
  instead of trying to place another block into the same position.
- `LooseStoneKnappingScreenHandler` owns the minigame contract:
  - opening consumes one loose stone from the player's hand unless the player is in creative;
  - closing the screen early returns that striking stone;
  - successful hits advance persistent block-entity phase and re-roll the target band;
  - three misses close the UI and destroy only the striking stone;
  - the fifth successful phase removes the world fragment and awards the crude pickaxe.
- Added `LooseStoneKnappingScreen`, a custom handled screen with:
  - animated moving cursor;
  - highlighted hit band;
  - visible phase bar;
  - miss counter;
  - simple hammer-motion UI animation and space/click strike input.
- Added `CrudeStoneToolMaterial` and crude pickaxe registration:
  - mining level `0`;
  - durability `30`;
  - mining speed `2.0`;
  - intentionally weak enchantability and no repair path.
- Spawn-world integration:
  - `spawnTransferLooseStoneState` now injects 20 deterministic loose-stone debris sites into the
    existing spawn transfer-debris layer;
  - fragments appear on the floor only and respect the existing no-rail footprint rules.
- Client registration:
  - cutout render layer for the loose stone block;
  - handled-screen registration in `MegastructureClient`.

Verification checkpoint:
- Code paths are wired through registries, worldgen, client screen registration and localized text.
- Full compile/install verification is still pending after this addition.

## 2026-06-26 - Cleanup pass: digging stone, webs, traversal anchor, oasis/lava polish

Request scope:
- Correct the primitive tool identity: reward must be a knapped digging stone, not a stone pickaxe.
- Stop cobwebs from generating as long vertical strands from stations/rail areas into walls.
- Make loose stones global but rare, not dense and spawn-only.
- Make rope anchors visually read as a cross-shaped wall/ceiling/floor fixing.
- Reduce corridor dead-end artifacts in the cell network.
- Add varied per-floor interior content to `Titan_tower_hall` central tower.
- Make oasis spores more visible, add better water-contact erosion, and guarantee Folded City basin water.
- Add rare lava reservoir placement in industrial/hot districts.
- Strengthen the render safety guard for the far-teleport/chat frozen-screen artifact.

Implementation:
- Replaced `megastructure:crude_stone_pickaxe` registration with
  `megastructure:knapped_digging_stone`.
  - The reward still uses weak wood-tier mining behavior through `MiningToolItem`, but the registry
    id, lang keys, item model and UI name no longer present it as a pickaxe.
  - Added `assets/megastructure/models/item/knapped_digging_stone.json` using a flint-like fragment
    visual instead of the vanilla wooden-pickaxe model.
- Removed generated `COBWEB` returns from spawn-station and railway-station rooms.
  - `BlockPalette.COBWEB` remains as an unused palette constant, but no current worldgen path emits it.
  - Existing already-generated chunks can still contain the old bad webs until regenerated.
- Reduced spawn transfer loose-stone sites from `20` to `7`, with less adjacent spread.
- Added `looseStoneScatterState`:
  - rare 64x64x18 deterministic cells;
  - only in air directly above supported structure;
  - excluded from railway footprints and connector volumes.
- Added directed `GrapplingAnchorBlock.FACING` with cross-shaped voxel outlines for all six faces.
  - `GrapplingRopeItem.deploy` now receives the attachment direction.
  - thrown grappling hooks and direct use-on-block both place anchors facing the support block.
  - Blockstate/model JSON now rotates a north-wall cross model for all directions.
- `isCellCorridor` now keeps trunk corridors always passable; the random dead-end gate applies only
  to side halls.
- Added `titanTowerFloorProgramState`:
  - 16-block floor cadence through the Titan tower;
  - ring floors, equipment banks, partitions and risers;
  - station levels and central atrium are preserved.
- Oasis changes:
  - `OasisSporeRenderer` now scans a larger region, ramps strength faster, renders more cells, and
    uses larger/brighter custom spores.
  - `oasisHydraulicErosionState` adds cracked/moss/clay contact traces around side-pipe, standpipe,
    wallfall and central fall water paths without replacing the pipe itself.
  - Folded City basin water can appear even when the floor-open heuristic rejects part of the folded
    floor.
- Added `BlockPalette.LAVA` and `lavaReservoirState`:
  - rare process reservoirs in `DISTRICT_SILENT_FOUNDRY`, `DISTRICT_REACTOR_CATHEDRAL`,
    `DISTRICT_MACHINE_NAVE`, and `DISTRICT_TANK_CLUSTER`;
  - placed over industrial floors, with cracked/rusted rim treatment.
- `RenderSafetyGuard` now restores the viewport to the current framebuffer size every tick/render
  pass and still disables invalid BBS custom 0x0 framebuffers when detected.

Verification checkpoint:
- `./gradlew.bat build` succeeded after the pass.
- Pending manual in-game checks:
  - confirm new chunks no longer produce vertical cobweb strands;
  - verify anchor orientation on wall, floor and ceiling;
  - verify spores are visible enough at normal brightness/fog settings;
  - verify far teleport + chat no longer triggers persistent framebuffer smear.

## 2026-06-26 - Oasis spore render activation fix

Request scope:
- Do not merely increase oasis spore density.
- Fix the actual failure mode where oasis spores are not created/rendered at all inside oasis areas.

Implementation:
- `OasisSporeRenderer` no longer depends on `MegastructureAtmosphereRenderer.isActive()` as the
  precondition for even scanning the area.
  - The old precondition could suppress spores before oasis detection ran, especially when the
    atmospheric renderer considered the current camera context inactive.
  - The renderer now only requires the camera to be in a `megastructure` biome namespace and not
    submerged.
- Reworked oasis detection into three real block scans:
  - dense near-camera scan for local moss/water/leaf/tree/clay presence;
  - wider room scan for large halls where the player is not standing directly on the oasis floor;
  - deep vertical scan for titan-scale rooms where the oasis floor can be far below the camera.
- Added weighted detection for:
  - moss blocks and moss carpet;
  - vines, azalea/oak/birch leaves, azalea blocks;
  - oak/birch logs, oak wood, rooted dirt and clay;
  - water.
- The renderer now ramps to visible strength from any meaningful local oasis biomass instead of
  requiring many sampled blocks to happen to align with the old sparse scan grid.

Verification checkpoint:
- `./gradlew.bat build` succeeded after the fix.
- Manual check still required in-game:
  - teleport into a known oasis;
  - stand on/near moss or water;
  - verify green custom spore particles are visible without needing to increase density further.

## 2026-06-26 - Anchor verticalization, spore visibility hard-fix, async chunkgen start

Request scope:
- Grappling anchor must keep the rope visually vertical/downward while still having a wall-attached
  fixture shape.
- Oasis spores are still not visible in-game; treat this as a render/activation failure, not a
  density issue.
- Start moving chunk generation toward maximum resource usage and define a realistic GPU path.

Implementation:
- Anchor model:
  - `grappling_anchor` now has a dedicated vertical rope element from block bottom to top.
  - Added a short wall-penetrating peg so the fixture still reads as attached to the wall.
  - Removed vertical-axis model flips for `facing=up/down`; the visible rope no longer becomes a
    horizontal rod.
- Oasis spores:
  - Removed the hard `biomePathAtCamera == null` early return.
  - Added biome-profile fallback for oasis-capable megastructure districts.
  - Replaced tiny triangle seed geometry with larger glowing billboard quads.
  - Uses additive blending and disables depth test for this overlay so spores remain visible in dark
    foggy oasis spaces instead of being visually swallowed by the floor/fog.
- Chunk generation:
  - `populateNoise` now schedules the actual block fill through the provided Minecraft generation
    executor via `CompletableFuture.supplyAsync`.
  - This begins using the existing CPU worker pool more aggressively instead of completing the whole
    chunk synchronously in the caller.

GPU/compute design note:
- Direct mandatory GPU worldgen is not safe as a baseline in Fabric because chunk generation is
  server-side logic and must work on dedicated servers/headless hosts.
- Viable path:
  - keep CPU worldgen authoritative;
  - split expensive structure decisions into deterministic chunk-local mask batches;
  - add an optional client/integrated-server compute backend later for those masks;
  - cache the computed masks by seed + district + chunk;
  - fall back to CPU masks when compute support is absent or Iris/driver state is incompatible.

Verification checkpoint:
- `./gradlew.bat build` succeeded.
- Manual check required:
  - anchor should show a vertical rope and wall fixture;
  - oasis should show obvious green custom spore motes;
  - compare chunk load feel around new terrain, especially after teleporting.

## 2026-06-26 - Oasis spores decoupled from biome logic

Request scope:
- Oasis is not a biome; spores must not depend on biome checks.
- Spores must be created/rendered with near certainty when the player is physically inside an oasis
  overlay area.

Implementation:
- Added real registered particle type `megastructure:oasis_spore`.
- Added client particle factory `OasisSporeParticle`.
  - Custom green glowing translucent particle.
  - Slow upward drift and side sway.
  - Long lifetime, bright rendering, non-vanilla motion profile.
- Added `OasisSporeParticleSpawner`.
  - Runs from `ClientTickEvents.END_CLIENT_TICK`.
  - Does not read biome ids.
  - Scans only nearby blocks around the player.
  - Activates from oasis physical blocks:
    - moss block;
    - moss carpet;
    - water;
    - vines;
    - azalea/oak/birch leaves;
    - azalea blocks;
    - oak/birch logs, oak wood;
    - rooted dirt;
    - clay.
  - Spawns actual client particles every tick while the player remains near those blocks.
- Added `assets/megastructure/particles/oasis_spore.json`.
- Removed biome fallback from `OasisSporeRenderer`; it now only uses block scans too.

Verification checkpoint:
- `./gradlew.bat build` succeeded.
- Static search confirms `OasisSporeRenderer`/`OasisSporeParticleSpawner` no longer use
  `biomePathAtCamera`.
- Manual in-game check required in a regenerated/newly loaded oasis:
  - stand directly on moss/moss carpet or near water/leaves;
  - green particles should appear around the player within a second.

## 2026-06-26 - Oasis spore render hardening

Request scope:
- User clarified again that oasis is not a biome and visible spores must be guaranteed by physical
  oasis presence, not by biome lookup.
- Previous implementation could still fail visually if vanilla particle settings/caps suppressed the
  particle path or if the particle collided with local geometry.

Implementation:
- Changed `megastructure:oasis_spore` registration to `FabricParticleTypes.simple(true)`.
  - This makes the particle an always-spawn particle and prevents client particle settings from
    silently discarding it.
- Updated `OasisSporeParticle`.
  - `collidesWithWorld = false`, so particles do not die inside dense megastructure geometry.
  - Increased base size and alpha for reliable visibility in dark/foggy oasis halls.
- Updated `OasisSporeParticleSpawner`.
  - Increased per-tick spawn count.
  - Spawn radius now favors the player-visible area.
  - Lowered activation threshold from a heavy oasis score to any nearby physical oasis cluster.
- Updated `OasisSporeRenderer` fallback.
  - Lowered block-scan activation threshold.
  - Increased fallback spore density, size, and alpha.
  - This renderer remains block-driven and does not read biome ids.

Verification checkpoint:
- Verified vanilla `minecraft:generic_0/1/2` particle textures exist in the 1.20.4 client jar.
- `./gradlew.bat build` succeeded.
- Copied fresh jar to `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`.
- Static check:
  - `MegastructureParticles` uses `simple(true)`;
  - `OasisSporeParticle` has `collidesWithWorld = false`;
  - spore renderer/spawner activation is block-based, not biome-based.

## 2026-06-26 - Deterministic oasis spore overlay and districts 29-30

Request scope:
- User reported that oasis spores still do not appear at all, even at a `/locate` oasis.
- User clarified again: oasis is not a biome; spore visibility must be rebuilt from scratch and
  should work like dust, but spatially near the located oasis.
- Finish the interrupted request to add two more concept locations:
  - a giant upper-rim city/shaft plate with dense lower city crust, central mast, radial roads and
    antenna fields;
  - an orbital web core with central node, spheres, rings and web-like struts.

Implementation:
- Added public deterministic client hint API in `MegastructureChunkGenerator`:
  - `OasisRenderHint`;
  - `findNearestOasisRenderHint(double x, double y, double z, int radiusDistricts)`.
- The hint API reproduces accepted oasis descriptor placement from district coordinates using
  `SHAPE_SEED`, host type, spacing arbitration, basin position and host floor/radius heuristics.
- Reworked `OasisSporeRenderer` activation:
  - no biome lookup;
  - no dependency on already-visible nearby moss/water blocks;
  - finds the nearest deterministic oasis hint every few ticks;
  - forces a visible additive green spore haze while the camera is inside the hint radius;
  - keeps physical block scanning only as an extra strength boost.
- Added district ids:
  - `DISTRICT_UPPER_RIM_CITY = 29`;
  - `DISTRICT_ORBITAL_WEB_CORE = 30`.
- Added the two districts to:
  - district roll table;
  - spawn-giant candidate table;
  - biome footprint checks;
  - giant oasis host list;
  - oasis floor/radius/profile/origin compatibility;
  - district air masks;
  - ruin base Y;
  - structure-state switch;
  - debug/district names.
- Added `upperRimCityStructureState`:
  - huge circular void;
  - outer cylinder wall;
  - high pale deck/plate;
  - central pipe mast;
  - lower dense city crust;
  - radial roads, ring grates and antenna towers.
- Added `orbitalWebCoreStructureState`:
  - central hollow node;
  - surrounding spheres;
  - rings/disks;
  - deterministic pipe/grate web lines between nodes.
- Added biome source codec fields and biome JSONs:
  - `megastructure:upper_rim_city`;
  - `megastructure:orbital_web_core`.
- Updated the world preset newest-districts codec payload.
- Added the two new biomes to the visual atmosphere profile mapping.

Verification:
- `./gradlew.bat clean build` succeeded.
- Fresh jar copied to:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`

Manual test targets:
- Use `/megastructure locate_oasis` and teleport to the result; green spore overlay should now be
  visible from the deterministic oasis radius even if local block scan would miss moss/water.
- Use `/locate biome megastructure:upper_rim_city` and
  `/locate biome megastructure:orbital_web_core` in a new world or newly generated chunks.
## 2026-06-26 - item texture icons

- Added four hand-authored Minecraft-style 16x16 transparent item textures under `src/main/resources/assets/megastructure/textures/item/`:
  - `chipped_digging_stone.png` - primitive chipped digging stone, deliberately not a pickaxe silhouette.
  - `rope_coil.png` - rope bundle / coil.
  - `grappling_rope.png` - rope with compact iron hook/anchor.
  - `pebble.png` - loose pickup pebble for inventory display.
- Added enlarged nearest-neighbor previews under `generated_item_icon_previews/` for inspection only.
- Wired item models to the new textures:
  - `knapped_digging_stone` now uses `megastructure:item/chipped_digging_stone`.
  - `loose_stone` now renders as `megastructure:item/pebble` in inventories instead of using the placed block model.
  - `rope_coil` now uses `megastructure:item/rope_coil`.
  - `grappling_rope` now uses `megastructure:item/grappling_rope`.

## 2026-06-27 - black-hole reactor and late district expansion

Request scope:
- Fix minecart braking so linked/empty carts do not passively become monolithic.
- Fix `/megastructure locate_oasis` runtime class-load failure caused by the nested `MegastructureChunkGenerator$OasisLocation` record.
- Add four screenshot-inspired megastructure districts.
- Add a rare controlled black-hole reactor as both worldgen and active modded behavior.

Implemented:
- `MinecartPhysicsMixin`:
  - removed passive empty-cart link braking;
  - kept forced braking only for the short `exitBrakeTicks` window after a player dismounts.
- `OasisLocation`:
  - moved from nested generator record to top-level `ru.nikit.megastructure.world.OasisLocation`;
  - `findNearestOasis` now returns the top-level record, preventing stale nested-class loading errors after remap.
- Added district ids:
  - `DISTRICT_CROWN_SPIRE = 31`;
  - `DISTRICT_GLOBE_MONUMENT = 32`;
  - `DISTRICT_VOID_ALTAR = 33`;
  - `DISTRICT_ATOM_STORM_ARRAY = 34`;
  - `DISTRICT_BLACK_HOLE_REACTOR = 35`.
- Added district roll/footprint/void masks/structure switch/debug names for all five districts.
- Added structure generators:
  - `crownSpireStructureState`: tapered vertical city/tower with crown-dense upper habitation and antenna masts.
  - `globeMonumentStructureState`: colossal sphere shell on support legs, equator ring and pedestal.
  - `voidAltarStructureState`: stepped pyramid under dark aperture, tendril-like conduits down to the apex.
  - `atomStormArrayStructureState`: central reactor-cloud core with orbital rings and surrounding pylons.
  - `blackHoleReactorStructureState`: containment hall, nested retention rings, pylons and radial feeds around an empty event core.
- Added black-hole runtime:
  - `BlackHoleCoreHint` generator helper;
  - `MegastructureChunkGenerator.findNearestBlackHoleCore(...)`;
  - `BlackHoleReactorSystem` server tick system:
    - pulls entities toward nearby reactor cores;
    - adds tangential swirl;
    - destroys item/orb entities inside the event horizon;
    - damages/repels living entities and players at the horizon instead of only deleting them.
- Added black-hole visuals:
  - `BlackHoleReactorRenderer`;
  - fast fluctuating black disk layers;
  - accretion ring fragments;
  - blue lens/containment halo;
  - renderer registered in `MegastructureClient`.
- Added biome source codec entries and world preset refs:
  - `megastructure:crown_spire`;
  - `megastructure:globe_monument`;
  - `megastructure:void_altar`;
  - `megastructure:atom_storm_array`;
  - `megastructure:black_hole_reactor`.
- Added matching biome JSON files.
- Added the new biomes to atmosphere profile routing.

Verification:
- `./gradlew.bat compileJava` succeeded.
- `./gradlew.bat build` succeeded.
- Fresh jar copied to:
  `C:\Users\nikit\Desktop\Важный хлам\Сборка Void RP\mods\megastructure-world-0.1.0.jar`

Manual test targets:
- `/megastructure locate_oasis` should no longer throw `MegastructureChunkGenerator$OasisLocation` class-load errors.
- Dismount a linked minecart: it should brake briefly after dismount, but empty linked carts should not be globally forced to slow forever.
- `/locate biome megastructure:crown_spire`
- `/locate biome megastructure:globe_monument`
- `/locate biome megastructure:void_altar`
- `/locate biome megastructure:atom_storm_array`
- `/locate biome megastructure:black_hole_reactor`

## 2026-06-27 - crown/globe/void fixes, black-hole visual rework, unloaded-edge fog

Request scope:
- Fix screenshot regressions in the new late districts:
  - `crown_spire` upper city needs a visible bottom/foundation deck.
  - `globe_monument` sphere must actually stand on connected support legs.
  - `void_altar` must not receive broken oasis growth.
- Rework black-hole reactor:
  - visual core must be depth-tested and not visible through blocks;
  - no stairs/walkways/blocks may pass through the event core;
  - accretion/shadow/photon-ring impression must be a client visual mechanic, not block geometry;
  - add sharper unstable fluctuations and stronger capture/swirl for nearby entities.
- Add non-spherical unloaded-boundary fog:
  - fog is built from loaded/unloaded chunk borders;
  - it overlaps slightly into loaded chunks to hide chunk edges;
  - it should remain compatible with Voxy-style far context by depending on actual client chunk loaded state rather than simple distance sphere.

Implemented:
- `MegastructureChunkGenerator`:
  - `crown_spire` now has a broad lower crown deck plus annular/radial underside ribs and hanging supports under the upper city mass.
  - `globe_monument` now uses segment-based angled support legs that physically reach the lower sphere and a central pedestal cap under the globe.
  - `void_altar` was removed from `isGiantOasisHostDistrict`, preventing the broken giant oasis overlay on that altar district.
  - `isBlackHoleCoreExclusion(...)` now clears a spherical event-core volume plus a flat visual-disk volume before railway/connectors/stairs can generate there.
- `BlackHoleReactorRenderer`:
  - black hole visual now uses depth testing and normal alpha blending, so it is occluded by blocks and the black shadow can actually darken the scene.
  - added abrupt high-frequency seed-based jitter/pulse for unstable controlled-core fluctuations.
  - expanded visual-only accretion fragments, lensing halo fragments and a cyan hazard containment ring.
- `BlackHoleReactorSystem`:
  - stronger close-range capture/swirl zone for players and entities near the event core.
- `UnloadedEdgeFogRenderer`:
  - new client-side fog quads are emitted along actual loaded-to-unloaded chunk borders.
  - fog has multiple inward layers, slightly overlapping loaded chunks to mask chunk-edge emptiness.
  - registered after other world visual passes in `MegastructureClient`.

Scientific visual references used:
- NASA black-hole anatomy: event horizon shadow, accretion disk, gravitational lensing, photon sphere and Doppler beaming.
- Event Horizon Telescope first black-hole image context: shadow/ring silhouette as the key recognizable visual target.

Verification:
- `./gradlew.bat build` succeeded after these changes.

## 2026-06-27 - universal minecart mechanics coverage

Request scope:
- Ensure the custom minecart mechanics apply to all minecart types, including cargo and special carts.

Implemented:
- Audited vanilla minecart bytecode in the mapped 1.20.4 jar:
  - `MinecartEntity`, `StorageMinecartEntity`, `ChestMinecartEntity`, `HopperMinecartEntity`, `TntMinecartEntity`, `CommandBlockMinecartEntity` inherit the relevant base `AbstractMinecartEntity` speed/link/course hooks used by `MinecartPhysicsMixin`.
  - `FurnaceMinecartEntity` has its own `getMaxSpeed`, so it needed a dedicated speed hook.
- Added `FurnaceMinecartSpeedMixin`:
  - raises furnace minecart max rail speed to the same megastructure value inside megastructure biomes.
- Registered `FurnaceMinecartSpeedMixin` in `megastructure.mixins.json`.

## 2026-06-27 - black hole reactor visual rewrite

Request scope:
- Make the black-hole reactor core look non-vanilla and physically recognizable:
  - spherical black-hole shadow instead of particle-like clutter;
  - accretion disk and photon/lensing rings;
  - visible near-core spatial warp/refractive screen effect;
  - unstable rapid core fluctuations;
  - no stairs or generated walkways through the singularity;
  - custom death message: `Стал жертвой безумной фантазии создателя`.

Implemented:
- `BlackHoleReactorRenderer` was rebuilt around a separate client-side render pass:
  - draws an actual dark spherical event-horizon shadow mesh with depth testing;
  - adds asymmetric hot accretion disk bands;
  - adds camera-facing photon rings and upper/lower gravitational-lensing arcs;
  - adds cyan containment meridians/rings to read as reactor technology;
  - adds high-frequency seed-based jitter and needle-like visual fluctuations;
  - adds a close-range screen-space warp layer when the player approaches/enters the capture zone.
- `BlackHoleReactorSystem`:
  - added custom `megastructure:creator_fantasy` damage type for black-hole consumption.
- Resources:
  - added `data/megastructure/damage_type/creator_fantasy.json`;
  - added `death.attack.creator_fantasy` translations.
- `MegastructureChunkGenerator`:
  - enlarged `isBlackHoleCoreExclusion(...)`;
  - black-hole reactor structure now returns air in the central sphere/disk void so roads, ladders and core-local structure pieces do not run through the visual singularity.

Notes:
- The visual warp is implemented with camera-facing render geometry, not a full framebuffer distortion shader. It is intentionally stronger near the core and avoids shader-pack fragility.
- Scientific visual targets used: event-horizon shadow, photon ring, accretion disk, gravitational lensing and Doppler/asymmetric brightness.

Verification:
- `./gradlew.bat build` succeeded.

## 2026-06-27 - world-space GPU black-hole volume

Request scope:
- The previous black-hole renderer still felt like a flat screen overlay.
- The core must be an object placed in the world at the reactor coordinates.
- Its visualization must be occluded by real blocks instead of bleeding over geometry.
- Keep GPU-generated visual complexity, but make the core volumetric in player perception.

Implemented:
- Added a second black-hole render path inside `BlackHoleReactorRenderer`:
  - `VOLUME_VERTEX_SHADER`;
  - `VOLUME_FRAGMENT_SHADER`;
  - separate `volumeProgram`;
  - separate GPU VAO/VBO meshes for horizon sphere and accretion disk.
- The reactor core now renders a depth-tested world-space object before the close-range fullscreen distortion:
  - event-horizon sphere is a real 3D mesh centered on the black-hole core;
  - mesh vertices are deformed on the GPU with high-frequency instability pulses;
  - accretion disk is a separate 3D annulus mesh around the sphere;
  - disk turbulence, broken bands, Doppler-like hot/cold coloration and razor photon highlights are shader-generated.
- Depth behavior:
  - the volume pass uses Minecraft's current world `ModelView` and `Projection` matrices;
  - the core is positioned by `CoreRelative = coreWorldPosition - cameraWorldPosition`;
  - `RenderSystem.enableDepthTest()` is used for the volume pass;
  - blocks in front of the core should hide the sphere/disk through the existing depth buffer.
- The fullscreen framebuffer distortion is now reduced to a close-range supplement:
  - it only runs inside `eventHorizonRadius * 8`;
  - it still requires the strict multi-ray line-of-sight check;
  - far/normal viewing should read from the world-space volume, not a flat overlay.
- Fixed internal GL resource ownership:
  - fullscreen triangle VBO remains separate;
  - sphere/disk VBOs are stored separately and no longer overwrite the fullscreen VBO field.

Notes:
- This is a dedicated GPU micro-renderer embedded in the Minecraft client render pipeline.
- It uses the same active GL context as Minecraft for reliable composition with depth, Iris/Sodium-style render state, and the game framebuffer.
- A separate external Vulkan/DirectX process would not automatically share Minecraft's live depth buffer, so it would reintroduce the exact overlay/occlusion problem unless a native interop bridge is built.

Verification:
- `./gradlew.bat build` succeeded.

## 2026-06-27 - black hole projection and occlusion correction

Request scope:
- The GPU black-hole image was still perceived as being drawn behind the actual reactor core.
- The lensing pass could still show through intervening megastructure blocks.
- Keep the renderer GPU-driven, but make it align with Minecraft's actual frame matrices.

Implemented:
- `BlackHoleReactorRenderer` no longer projects the core by approximating camera rotation and vanilla FOV.
- Projection now uses the active world render `model-view` matrix plus `RenderSystem.getProjectionMatrix()`.
- Horizon screen size is now derived by projecting camera-right and camera-up edge samples around the core.
- Line-of-sight is stricter:
  - close capture range still renders so entering the core keeps the intended spatial collapse;
  - outside capture range, visibility is sampled through the center and eight horizon-edge points;
  - a blocked ray must be within 1.25 blocks of the sampled target to count as visible.

Expected effect:
- The black-hole shader center should sit on the actual reactor core instead of drifting behind it.
- The screen-space lensing pass should not render through solid walls/ceilings when the core is hidden.
- Partial visibility through real gaps should still work because edge samples can pass through openings.

Verification:
- `./gradlew.bat build` succeeded.

## 2026-06-27 - GPU black-hole lensing renderer

Request scope:
- Replace the dotted/particle-like black-hole visual with a GPU renderer that feels impossible for vanilla Minecraft:
  - use the client GPU directly;
  - distort the already rendered world image around the black hole;
  - represent a spherical event-horizon shadow;
  - add Interstellar/DNGR-inspired accretion disk and gravitational lensing cues;
  - create stronger near-entry spatial refraction/spaghettification feel;
  - remove central stairs/paths from black-hole generation.

Implemented:
- Replaced `BlackHoleReactorRenderer` with a custom OpenGL pipeline:
  - creates and owns a GL shader program, VAO/VBO fullscreen triangle and framebuffer-copy texture;
  - copies the current Minecraft framebuffer color buffer into a GPU texture each frame near a reactor core;
  - runs a GLSL fragment shader over the screen to sample and bend the current scene around the projected black-hole center;
  - draws a smooth spherical black event-horizon shadow in screen space;
  - adds continuous photon ring, secondary photon ring, hot asymmetric accretion band and upper/lower lensed arcs;
  - adds chromatic splitting, vortex twist, near-core crushing/darkening and rapid instability fluctuation;
  - removed the old dashed containment/world-quad look from the black-hole visual path.
- Added CPU-side projection from world core position to screen UV:
  - uses camera rotation inverse, FOV and framebuffer aspect;
  - skips rendering when the core is behind the camera or outside a reasonable screen area.
- Added line-of-sight raycast:
  - prevents the GPU effect from visibly bleeding through intervening solid blocks except when the player is already inside the close capture zone.
- `MegastructureChunkGenerator` central exclusion from the previous pass remains active:
  - central black-hole sphere/disk void prevents stairs/walkways from occupying the singularity volume.

Scientific/visual references used:
- Interstellar/DNGR paper and DNEG/Caltech notes: gravitational lensing by spinning black holes, ray-bundle rendering, lensed accretion disk wrapping above/below the shadow.
- NASA black-hole anatomy: event-horizon shadow, photon ring/lensing and accretion disk as core visual targets.

Notes:
- This is a real GPU shader pass inside the Minecraft client OpenGL context, not server-side GPU compute.
- It approximates DNGR-style lensing for realtime gameplay rather than solving Kerr geodesics per pixel.

Verification:
- `./gradlew.bat build` succeeded.

## 2026-07-03 - Vanilla ore veins and global story-task system

Request scope:
- Replace the generator's isolated hash-selected ore cubes in megastructure walls with normal Minecraft
  ore veins and normal vanilla ore rates.
- Add a configurable client keybinding (default `J`) that opens a global-story-task menu.
- Initial global task: `НАЙТИ МЕСТО ДЛЯ ЖИЗНИ`.
- When at least one quarter of every unique player who has ever joined the server has physically reached
  an oasis, replace it with `ОБЕСПЕЧИТЬ УСЛОВИЯ`.
- On the transition, show a sudden black full-screen task card with large yellow text, play the supplied
  `Appearance task.mp3` after conversion to OGG, and remove the card at the end of that sound.

Implemented - vanilla ore generation:
- Removed the generator-local `oreOrStone(...)` hash branch and the dynamic `DynamicWorldgenPalette`
  ore list. The generator no longer inserts single ore blocks while filling wall mass.
- Added `VanillaOreFeatures`.
  - Registers the exact standard Overworld placed-feature set from `OrePlacedFeatures` at
    `GenerationStep.Feature.UNDERGROUND_ORES`:
    coal upper/lower; iron upper/middle/small; gold upper/lower; redstone upper/lower;
    diamond normal/large/buried; lapis normal/buried; and normal copper.
  - These are Minecraft's own configured/placed features, retaining vanilla vein geometry,
    count/chance, vein size and Y placement rather than recreating an approximation.
- Added `megastructure:has_vanilla_ores`, covering all 39 registered megastructure biomes.
- Added `minecraft:stone_ore_replaceables` and `minecraft:base_stone_overworld` extensions containing
  only `minecraft:smooth_stone`, the mod's primary mass block.
  - Rails, walkways, stairs, pipes, polished foundation blocks, panel materials and decorative blocks
    do not match this target and are not replaced by vanilla ore placement.
  - Existing vanilla stone/andesite/granite behaviour remains provided by Minecraft's own tag contents.
- Repaired the existing `minecraft:is_overworld` biome tag so the seven late district biomes omitted in
  the uploaded baseline are included: upper rim city, orbital web core, crown spire, globe monument,
  void altar, atom storm array and black-hole reactor.

Implemented - global task progression:
- Added a persistent server state `GlobalTaskState` stored as `megastructure_global_tasks`.
  - Stores every unique joined player UUID, unique oasis visitors and the current global stage.
  - On its first use it imports existing `playerdata/*.dat` UUIDs from the save, so the denominator
    includes players who joined before this update.
  - The requirement is `ceil(all unique players / 4)`, with a minimum of one player.
  - The stage only moves forward: initial `FIND_A_PLACE_TO_LIVE` -> `PROVIDE_CONDITIONS`.
- Added server-side oasis detection through the existing deterministic oasis descriptors and primary
  railway oasis data. It does not rely on oasis being a biome or on client-only visual blocks.
- The server checks online players once per second. A unique player is credited once after entering an
  oasis area; on reaching the quota the stage is broadcast to every connected client.

Implemented - client menu and announcement:
- Added `GlobalTaskKeyBindings`, registered through Fabric's keybinding API with default `J` and
  normal Controls-menu configurability.
- Added `GlobalTasksScreen`, a non-pausing custom black/yellow `ГЛОБАЛЬНЫЕ ЗАДАЧИ` menu.
  - Before progression it shows the active `НАЙТИ МЕСТО ДЛЯ ЖИЗНИ` task and server progress.
  - Afterwards it shows the first task as completed and `ОБЕСПЕЧИТЬ УСЛОВИЯ` as active.
- Added a server-to-client task-sync packet and `GlobalTaskAnnouncementOverlay`.
  - On the only story-stage transition it immediately covers the screen in black and draws the new
    task in 2x large yellow lettering.
  - Added `GlobalTaskSounds` and `sounds.json`.
  - Converted user-supplied `Appearance task.mp3` to
    `assets/megastructure/sounds/task_appearance.ogg`: Vorbis, stereo, 48 kHz, actual duration
    `4.188979 s`.
  - The overlay lifetime is `4190 ms`, matching the OGG duration; it clears itself at sound end.
- Added Russian and English translations for the keybinding, task screen, task labels and notification.

Static verification:
- Parsed all `69` JSON resources successfully.
- Confirmed all 39 biome JSONs are covered by `megastructure:has_vanilla_ores`.
- Confirmed the removed cube-generation identifiers are absent from the generator and dynamic palette.
- Confirmed all 15 normal default Overworld ore placed features are registered.
- Confirmed task asset linkage, translations, stage rounding examples and OGG metadata:
  Vorbis / 48 kHz / stereo / `4.188979 s`.
- Checked the selected Fabric/Minecraft 1.20.1 APIs against the cached mapped classes, including
  `OrePlacedFeatures`, `PositionedSoundInstance.master(...)`, `WorldSavePath.PLAYERDATA` and
  `Screen.shouldPause()`.

Build/install status:
- Full Gradle build was not possible in this environment. The copied wrapper is configured to download
  Gradle `8.8`; its distribution cache is incomplete and external network access is disabled, so the
  wrapper cannot start.
- No JAR was produced or installed in this pass. The deliverable is source/resource patch only and must
  be built in the user's local project environment.

Testing constraints:
- Vanilla ore changes affect only newly generated chunks. Existing cube ores remain saved in existing
  chunk NBT and will not be retroactively transformed.
- Global task state/menu/audio work after the rebuilt JAR is installed. The first persistent-state load
  imports old player UUID files once; use a real server save to verify the historical-player denominator.
- Manual checks after local build:
  - new terrain contains connected vanilla-like ore veins instead of single cubes;
  - `J` opens the global tasks menu and the key can be rebound in Controls;
  - after the required number of unique players enters an oasis, every online client receives the
    black/yellow `ОБЕСПЕЧИТЬ УСЛОВИЯ` card with the 4.19-second sound.


## 2026-07-03 - Global task keybinding startup crash fix

User report:
- Client crashed during the initial resource reload immediately after installing the global task system.
- Crash report:
  `java.lang.IllegalStateException: GameOptions has already been initialised`
  at `KeyBindingHelper.registerKeyBinding(...)`, called from
  `GlobalTaskKeyBindings.<clinit>` during the first `ClientTickEvents.END_CLIENT_TICK`.

Root cause:
- `GlobalTaskKeyBindings` registered the default `J` binding in a static field initializer.
- Referencing `GlobalTaskKeyBindings::tick` does not force that initializer to run at entrypoint time;
  the class was first initialized only when Fabric invoked the tick handler.
- By that time Minecraft had already created `GameOptions`, and Fabric correctly rejects late
  keybinding registration.

Implemented:
- Removed keybinding registration from the static initializer.
- Added idempotent `GlobalTaskKeyBindings.register()`.
- `MegastructureClient.onInitializeClient()` now invokes `GlobalTaskKeyBindings.register()` before
  attaching the client-tick callback.
- `tick(...)` now safely does nothing until registration exists; it only handles already-registered
  key presses and cannot mutate Fabric's keybinding registry.

Verification:
- Static source checks confirm:
  - `KeyBindingHelper.registerKeyBinding(...)` exists only inside `register()`;
  - `MegastructureClient` calls `GlobalTaskKeyBindings.register()` before
    `ClientTickEvents.END_CLIENT_TICK.register(GlobalTaskKeyBindings::tick)`;
  - `tick(...)` contains no registration call.
- Full Gradle compilation remains unavailable in this environment because Gradle 8.8 is not present
  in the wrapper cache and external network access is disabled.
- This is a startup-only client fix; it does not change world data, task progression or ore generation.


## 2026-07-03 - Global task menu simplification + connector corridor / bridge visual pass

User request:
- Simplify the global-task screen so text never spills out of the panel.
- Show only the active task name centered.
- Remove the oasis explanatory text and all extra hints; leave only a bottom-center progress counter.
- Allow closing the task screen only with `Esc`.
- Rework connector corridors to look closer to the supplied industrial corridor reference.
- Rework void/rift bridges to look closer to the supplied long suspended bridge reference.

Implemented:
- Rebuilt `GlobalTasksScreen` layout:
  - removed the visible `GLOBAL TASKS`, `ACTIVE TASK`, `COMPLETED`, oasis-description and close-hint labels;
  - the panel now centers only the current task title;
  - the task title is auto-scaled down to fit the available width, preventing overflow;
  - a minimal numeric progress counter (`visited / required`) is rendered at the bottom center.
- Changed `GlobalTaskKeyBindings.tick(...)` so pressing the task key opens the menu only when no other screen is open.
  Pressing the key again no longer closes the menu; closing is now effectively `Esc`-only.
- Reworked high-level connector passages (`connectorLayerState`) from plain floating strips into enclosed industrial passages:
  - floor now has a narrow central grate/service trench with solid side walkways;
  - side shell/walls and periodic ribs use heavier industrial blocks;
  - ceiling now has a closed top with occasional lamps and brighter inner panels.
- Reworked the bridge approach and room-link passages (`riftBridgeAccessState`, `riftBridgeRoomLinkState`) to use the same enclosed industrial language, with wider junction corners.
- Reworked rift suspension bridges (`riftSuspensionBridgeState`) into a more recognizable suspended-bridge silhouette:
  - solid deck edges with central walkway/grate variation;
  - edge lamp posts / uprights;
  - under-deck braces;
  - elevated side cables and periodic vertical hangers.

Notes / limitations:
- This pass changes only source generation/layout logic; already generated chunks keep their old bridge/corridor shapes until new chunks are generated.
- The bridge/corridor pass is intentionally reference-inspired, not a literal replica of the screenshots.
- Full Gradle compilation is still unavailable in this environment because the project wrapper expects Gradle 8.8 and the required cached distribution is incomplete.


## 2026-07-03 - Pre-launch full-screen gate, Reaper confirmation, server-start intro and task-audio reliability

User request:
- Before the world is launched, put every connected player into a full-screen menu that shows only chat.
- The menu must hide the world, HUD and especially Xaero's minimap/world-map view, while voice-chat controls/configuration remain usable.
- Wait for confirmation from `ReaperFromHelk` before allowing gameplay.
- After confirmation, play the supplied `Server_start.mp3` as `server_start.ogg` over a black screen.
- Ten seconds after confirmation, show the first global task while the server-start recording remains on its black screen.
- Fix the task-appearance sound, which was not reliably audible.

Implemented:
- Added persistent server launch lifecycle:
  - `WAITING` -> `INTRODUCTION` -> `STARTED`;
  - state is stored as `megastructure_server_start` in the Overworld persistent-state manager;
  - server restart preserves whether the launch gate has already been opened.
- Added the start confirmation command:
  - `/megastructure start`;
  - it accepts only a player whose profile name equals `ReaperFromHelk` (case-insensitive);
  - this subcommand does not require operator level, so the named launcher is not accidentally blocked by the old admin-only `locate_oasis` requirement;
  - `locate_oasis` remains operator-only.
- Before `STARTED`, clients receive a full-screen `ServerStartScreen`:
  - draws an opaque black layer after normal HUD rendering, hiding the world and minimap;
  - manually redraws only vanilla chat above that black layer;
  - `T`/the configured chat key and `/` open a black-background chat input screen;
  - `Esc` cannot dismiss the launch gate;
  - vanilla movement/interact keys are continuously released while the gate is active;
  - known Xaero map screens are replaced by the launch gate, while unrelated mod screens (including voice-chat configuration) are not forcibly closed.
- `ServerStartManager` broadcasts the intro start to every online client.
  The black intro lasts 502 server ticks / about 25.1 seconds, matching the complete supplied recording.
  Initial implementation: at 200 ticks / 10 seconds it broadcast the first-task announcement; this timing was corrected in the subsequent `Correct first-task timing after server_start` entry.
- Added `server_start.ogg`:
  - source: supplied `Server_start.mp3`;
  - Vorbis, 44.1 kHz, stereo;
  - exact measured duration: `25.077551 s`;
  - configured as streamed audio to avoid loading the full recording into a short-effect channel.
- Registered the new `megastructure:server_start` sound event and added it to `sounds.json`.
- Reworked task-appearance audio delivery:
  - resource event entries now use direct local sound paths (`task_appearance`, `server_start`) instead of a self-namespaced path;
  - task audio is scheduled for the next client tick after the visual announcement, avoiding packet/resource-reload timing loss;
  - task appearance playback volume is explicitly set to `1.35`.
- The first global task now appears exactly 10 seconds after successful `ReaperFromHelk` confirmation, on top of the still-black server-start screen. The world is revealed when the 25.1-second recording-length intro completes.
- Global-task oasis-progress scanning stays disabled until `STARTED`; player-history accounting still continues before launch so the historical quota is not lost.

Verification:
- Parsed `assets/megastructure/sounds.json` successfully.
- Confirmed all new startup source files and `server_start.ogg` are present at their intended paths.
- Confirmed the server command routes to `ServerStartManager.confirmLaunch(...)`.
- Confirmed the launch screen contains an opaque full-screen fill and redraws `ChatHud` only.
- Initial static check confirmed the client gate blocks Xaero map screens; the original 200-tick task timing was later corrected to run after the recording plus the requested delay.
- Confirmed OGG metadata with ffprobe: Vorbis / 44.1 kHz / stereo / `25.077551 s`.
- Full Gradle compilation remains unavailable in this environment because the project wrapper still attempts to download Gradle 8.8 and outbound network access is disabled.


## 2026-07-03 - Correct first-task timing after server_start

User correction:
- The first task must appear **10 seconds after the end** of `server_start`, not 10 seconds after the launch confirmation.

Implemented:
- Corrected `ServerStartState.announceFirstTaskIfReady(...)`.
- The trigger is now calculated as:
  `INTRO_DURATION_TICKS + FIRST_TASK_DELAY_TICKS` = `502 + 200` server ticks.
- `server_start` finishes at 25.08 seconds; the first-task announcement now occurs at approximately 35.08 seconds after `/megastructure start`.
- The launch screen still releases when the recording ends. The task notification now appears in the playable world ten seconds later.

Verification:
- Static timing-path check confirms the announcement is no longer restricted to the `INTRODUCTION` phase, because that phase ends as the recording finishes.
- No client/resource/audio file changed in this correction.


## 2026-07-03 - Narrative audio delivery, Esc-accessible blackout, and wall-entry corridor correction

User report / clarification:
- Neither `server_start` nor the task-appearance cue was audible.
- The requested corridor reference applies specifically to the enclosed passages where a suspended bridge enters the solid megastructure wall, not primarily to the broad district connector network.
- Before launch, `Esc` must remain usable for the normal pause menu and settings, but the world/HUD/minimap must remain black behind those screens.
- Rejoining must restore the authoritative pre-launch state rather than leaving a player with a broken or bypassed gate.

Audio correction:
- Re-encoded both supplied source recordings into canonical Vorbis OGG assets and included both again in this patch:
  - `assets/megastructure/sounds/task_appearance.ogg` — Vorbis, 48 kHz, stereo, `4.188979 s`;
  - `assets/megastructure/sounds/server_start.ogg` — Vorbis, 44.1 kHz, stereo, `25.077551 s`.
- Rewrote `sounds.json` with explicit `type: file`, fully qualified `megastructure:` resource IDs, streamed playback and explicit per-cue volume.
- Replaced direct one-shot `SoundEvent` playback with `NarrativeSoundPlayer`:
  - playback is deferred for two client ticks;
  - the player retries until the SoundManager has a loaded definition for the requested sound ID;
  - sound is played through an explicit non-positional `MASTER` SoundInstance;
  - the client logs a precise warning if a cue is absent from the active resource pack for ten seconds instead of failing silently;
  - queued cues are cleared on disconnect to avoid stale audio after reconnecting.
- The task card and server-start sequence now use this same path, so both depend on the same checked resource pipeline.

Pre-launch Esc / blackout:
- `Esc` on the black chat gate now opens Minecraft's normal pause menu instead of being swallowed.
- A `ScreenEvents.beforeRender` blackout is installed for every screen while the server state is WAITING or INTRODUCTION. Therefore pause/settings/mod configuration/voice-chat settings can render on top of black, while the world, minimap and HUD behind them remain hidden.
- Xaero map screens are still replaced by the launch gate before launch.
- Closing the pause/settings screens returns the player to the black chat gate until the authoritative server phase becomes STARTED.
- The existing server-persistent `ServerStartState` remains the source of truth on each JOIN; reconnecting during WAITING or INTRODUCTION re-applies the blackout/gate and reconnecting after STARTED remains unrestricted.

Wall-entry corridor correction:
- Reworked `bridgeConnectorCorridorState`, which is called by `riftBridgeAccessState` and `riftBridgeRoomLinkState` for the actual bridge-to-wall passages.
- The passage now has an intentionally enclosed industrial tunnel profile:
  - central recessed grated service channel;
  - two side walk lanes separated by raised edging;
  - low wall-side conduits/benches and intermittent pipes;
  - tiled shell walls plus heavy vertical support ribs;
  - lower closed ceiling, periodic cross-ribs and small central lamps;
  - wider, matching junction sections where bridge links bend into a room route.
- Existing chunks retain their old layouts. Generate new chunks to inspect the corrected passages.

Verification performed:
- `sounds.json` parses successfully as JSON.
- Both packaged sound files were decoded with ffprobe as valid OGG/Vorbis streams.
- Static source checks confirm both cues pass through `NarrativeSoundPlayer`, blackout rendering is gated by the synchronized server phase, and the wall-entry methods call the revised tunnel generator.
- Full Gradle compilation is still unavailable in this environment because Gradle 8.8 is not present in the wrapper cache and external download access is disabled.


## 2026-07-04 - Leaf sapling drops reduced to ten percent

User request:
- Any sapling drop chance must be `10%` of the vanilla chance.

Implemented:
- Added `AbstractBlockSaplingDropMixin`.
- The mixin runs after `AbstractBlock#getDroppedStacks(...)` and only applies when the source block is tagged as leaves (`BlockTags.LEAVES`).
- Any returned item stack tagged as a sapling (`ItemTags.SAPLINGS`) is thinned with a `0.1` keep chance per sapling item.
- This preserves all non-sapling leaf drops, including sticks, apples, leaf blocks from shears / silk touch, and any other modded leaf loot.
- Because the filter is tag-based, modded leaves and modded saplings are also covered when their mods use the standard leaves / saplings tags.
- Breaking an already placed sapling is not affected, because the source block is not leaves.
- Registered the new mixin in `megastructure.mixins.json`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `793461` bytes, timestamp `2026-07-04 18:40:57`.


## 2026-07-04 - Seed-randomized rail routing and spawn-run ascent guard

User request:
- The spawn railway / tunnel path must not be the same in every world.
- Railway direction / climb / descent behavior must be generated from the world seed.
- The tunnel rising away from spawn must not be cut by the prepared spawn structure.

Implemented:
- Moved railway-line presence checks used by both chunk generation and `DistrictBiomeSource` from the fixed `SHAPE_SEED` to `activeWorldVariantSeed`.
- Moved railway turn placement, railway lane placement, station placement, station seeds, and primary-rail ruin placement from the fixed shape seed to `worldVariantSeed`.
- Replaced the old hard-coded sinusoidal primary-rail height curve with a seed-derived smooth profile.
- Added a protected flat spawn run of `1024` blocks before the primary rail is allowed to climb or descend. This keeps the start station / spawn tunnel from being intersected by the first ramp.
- Positive and negative primary-rail directions now get separate seed-derived vertical profiles, so the two outbound directions are no longer mirrored copies.

Notes:
- The primary railway still starts aligned with the spawn station so the initial room remains usable.
- Existing chunks keep their old geometry; inspect newly generated chunks / a new world for the corrected route.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `793712` bytes, timestamp `2026-07-04 18:50:20`.


## 2026-07-04 - Silent startup updater and restart handoff

User request:
- At Minecraft startup, check whether a newer mod version exists.
- If a newer version exists, install it automatically, replace the current jar, and restart Minecraft.
- Do this without an in-game player notification.

Implemented:
- Added `StartupModUpdater`, registered at the very beginning of `MegastructureClient.onInitializeClient()`.
- On client startup the updater:
  - creates / reads `config/imperfect_salvation_updater.properties`;
  - reads `manifest_url` from that config, or from JVM property `imperfect_salvation.update_manifest_url`;
  - downloads a JSON manifest;
  - compares manifest `version` with the installed Fabric metadata version;
  - downloads the jar from manifest `jar_url`;
  - verifies manifest `sha256` against the downloaded jar;
  - writes a temporary PowerShell helper into `.imperfect_salvation_updates`;
  - launches the helper, requests Minecraft shutdown, waits for the JVM process to exit, replaces the installed jar and starts Minecraft again with the same Java command line.
- The updater is silent for the player. It only writes success / failure details to the log.
- A missing `manifest_url` disables update work without an error, so local/offline development is not interrupted.

Manifest format:
```json
{
  "version": "0.1.1",
  "jar_url": "https://example.invalid/Imperfect_salvation-0.1.1.jar",
  "sha256": "full lowercase sha256 hex of the jar"
}
```

Generated config defaults:
```properties
enabled=true
manifest_url=
timeout_seconds=8
allow_downgrade=false
```

Safety / limitations:
- A jar without a matching SHA-256 is never installed.
- The actual class update still requires restart; the helper performs that restart automatically.
- The restart is implemented for the current Windows environment through PowerShell and relaunches the current Java command line.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `805589` bytes, timestamp `2026-07-04 21:09:54`.


## 2026-07-05 - GitHub updater distribution bootstrap

User request:
- Set up the startup updater through GitHub using `https://github.com/hh4ck1/Imperfect_Salvation.git`.

Implemented:
- Bumped local project version to `0.1.1` for the first updater-distributed build.
- Built `Imperfect_salvation-0.1.1.jar`.
- Added `manifest.json` with:
  - `version`: `0.1.1`;
  - `jar_url`: `https://raw.githubusercontent.com/hh4ck1/Imperfect_Salvation/main/releases/Imperfect_salvation-0.1.1.jar`;
  - `sha256`: `612a8648e081642469081f4f8d67555a6114c21db82107a60a5d656365247217`.
- Copied the built jar into `releases/Imperfect_salvation-0.1.1.jar`.
- Added Git remote `origin = https://github.com/hh4ck1/Imperfect_Salvation.git`.
- Created and pushed commit `821d32b` on branch `main` containing only:
  - `manifest.json`;
  - `releases/Imperfect_salvation-0.1.1.jar`.
- Configured the active modpack updater config:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\config\imperfect_salvation_updater.properties`
  now points to:
  `https://raw.githubusercontent.com/hh4ck1/Imperfect_Salvation/main/manifest.json`.

Intentional bootstrap detail:
- The active installed mod jar remains `Imperfect_salvation-0.1.0.jar`, because that jar already contains the updater.
- On next Minecraft startup, it should see GitHub manifest version `0.1.1`, download the jar, verify SHA-256, replace the installed jar and restart Minecraft.

Verification:
- GitHub push to `origin/main` completed successfully.
- `https://raw.githubusercontent.com/hh4ck1/Imperfect_Salvation/main/manifest.json` returned HTTP `200`.
- Downloaded the GitHub raw jar and verified SHA-256:
  `612a8648e081642469081f4f8d67555a6114c21db82107a60a5d656365247217`.
- Active updater config was written with `enabled=true` and the GitHub manifest URL.


## 2026-07-05 - Startup updater relaunch fallback fix

User report:
- The GitHub update downloaded but did not replace the installed mod.

Root cause:
- The installed `0.1.0` updater reached the download step and left
  `.imperfect_salvation_updates\Imperfect_salvation-0.1.1.jar.tmp`.
- It failed before writing the restart helper:
  `java.io.IOException: Current Java arguments are not available`.
- On this TLauncher / Java runtime, `ProcessHandle.info().arguments()` is empty even though the full process command line is available.

Implemented:
- Bumped updater-distributed version to `0.1.2`.
- Updated `StartupModUpdater.writeRestartScript(...)`:
  - keeps the old `command + arguments` relaunch path when arguments are available;
  - adds a fallback through `ProcessHandle.info().commandLine()`;
  - the fallback restarts via `cmd.exe /d /c start "" <full command line>`;
  - starts the PowerShell helper with `-WindowStyle Hidden`.
- Rebuilt `Imperfect_salvation-0.1.2.jar`.
- Updated GitHub `manifest.json`:
  - `version`: `0.1.2`;
  - `jar_url`: `https://raw.githubusercontent.com/hh4ck1/Imperfect_Salvation/main/releases/Imperfect_salvation-0.1.2.jar`;
  - `sha256`: `04cf7687c9171603b161dccbb165c5c15b4d3da4f370f99eff38b7cbaddd18c7`.
- Pushed commit `a134d4e` to `origin/main`.
- Because the already installed `0.1.0` updater cannot self-heal past this error, manually replaced the active modpack jar once with the fixed `0.1.2` build.

Verification:
- `.\gradlew.bat build` completed successfully.
- GitHub manifest returned HTTP `200` and now points to `0.1.2`.
- Active installed jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`
  now has SHA-256 `04cf7687c9171603b161dccbb165c5c15b4d3da4f370f99eff38b7cbaddd18c7`.
- Reading `fabric.mod.json` from the active installed jar confirms internal version `0.1.2`.


## 2026-07-05 - Bridge-wall tunnel routing and spawn-style corridor profile

User report:
- Tunnels that leave spawn look acceptable, but the wall-entry tunnels generated from bridge mouths look wrong.
- Bridge-wall tunnels are missing windows, look crooked, and can create loops through a single room.

Implemented:
- Added a deterministic `RiftBridgeRoomLink` selection per district.
- `riftBridgeRoomLinkState(...)` now uses one selected bridge-to-room link per district instead of drawing every possible bridge-band / rift-stripe link through the same district.
- This removes the common multi-link L-route overlap that produced room loops.
- Reworked `bridgeConnectorCorridorState(...)` to receive absolute coordinates and corridor axis, not only local along/cross coordinates.
- Bridge-wall corridors now use the same overall industrial tunnel profile as the spawn / primary railway corridor:
  - wider 10-block shell;
  - central grated service channel;
  - broad side walk lanes;
  - heavy rib / bulkhead cadence;
  - ceiling light bays;
  - side service pipes;
  - wall windows only when the sampled side actually faces open void.
- Corner / junction sections are slightly widened but use the same material language, so bridge transitions should no longer read as separate broken mini-tunnels.
- Bumped project version to `0.1.3`.
- Updated GitHub updater manifest to point at `releases/Imperfect_salvation-0.1.3.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `807316` bytes, timestamp `2026-07-05 00:48:23`.
- `releases/Imperfect_salvation-0.1.3.jar` SHA-256:
  `e7e77f9a40db140219244a056c9a55680b6814afdc13d16576e55d04d3c7b0f2`.


## 2026-07-05 - Distress phrases in startup terminal error stream

User request:
- When the startup terminal begins printing errors, mix in phrases such as:
  `HELP`, `OH LORD HELP ME`, `SAVE ME PLEASE`, `BEG YOU`, `FIND ME`.
- Reserve a place for a link that will be provided later and should appear between error lines.

Implemented:
- Updated `ServerStartScreen.errorLine(...)`.
- Expanded the error-line cycle from `18` to `28` entries.
- Interleaved the requested distress phrases between the existing technical failure messages.
- Added `DISTRESS_LINK = "[LINK_PENDING]"` as the single placeholder for the future supplied link.
- The placeholder appears as:
  - `[!] DISTRESS LINK: [LINK_PENDING]`;
  - `[!] FIND ME / [LINK_PENDING]`.
- Bumped project version to `0.1.4`.
- Updated GitHub updater manifest to point at `releases/Imperfect_salvation-0.1.4.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `807614` bytes, timestamp `2026-07-05 01:33:56`.
- `releases/Imperfect_salvation-0.1.4.jar` SHA-256:
  `04ff56680130c1019d074592d216620d865d39aede6f777e06a868f745569669`.


## 2026-07-05 - Loaded chunk physics tick scheduling

User report:
- Interactive vanilla blocks generated by the megastructure do not behave naturally.
- Water does not flow.
- Sand and other unsupported falling blocks remain suspended in air.

Root cause:
- The generator writes block states directly during chunk population.
- Many vanilla dynamic behaviors require scheduled ticks or neighbor updates after the chunk is loaded.
- `LoadedChunkBlockUpdater` only scheduled water ticks and did not schedule block ticks for `FallingBlock` instances.

Implemented:
- Updated `LoadedChunkBlockUpdater`.
- On megastructure chunk load, every non-empty `FluidState` now receives a scheduled fluid tick using its own fluid tick rate.
- This covers water, lava, and modded fluids that expose a non-empty fluid state.
- Any block whose block instance is a `FallingBlock` now receives a scheduled block tick two ticks after chunk processing.
- This covers sand, red sand, gravel, concrete powder, anvils, and compatible modded falling blocks.
- The updater still sends neighbor updates around the affected positions.
- Existing NeepMeat light / conduit refresh behavior remains unchanged.
- Bumped project version to `0.1.5`.
- Updated GitHub updater manifest to point at `releases/Imperfect_salvation-0.1.5.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `807819` bytes, timestamp `2026-07-05 01:40:50`.
- `releases/Imperfect_salvation-0.1.5.jar` SHA-256:
  `4e4bdb76d64e3f9b78e63f83c679ebcc0ff3e7d6dc1d38904783877d6b25743d`.


## 2026-07-05 - Replace TechReborn with Modern Industrialization

User request:
- Replace TechReborn with Modern Industrialization from Modrinth:
  `https://modrinth.com/mod/modern-industrialization`.

Implemented in active modpack:
- Installed Modrinth `Modern Industrialization v1.8.6` for Minecraft `1.20.1` / Fabric:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Modern-Industrialization-1.8.6.jar`.
- Verified the installed jar metadata:
  - mod id: `modern_industrialization`;
  - version: `1.8.6`;
  - Minecraft dependency: `1.20.1`;
  - required dependencies: Fabric API, Cloth Config, Team Reborn Energy.
- Confirmed Fabric API and Cloth Config were already present in the active `mods` folder.
- Team Reborn Energy is embedded in the Modern Industrialization jar as `META-INF/jars/energy-3.0.0.jar`.
- Moved the TechReborn stack out of the active `mods` folder into:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\disabled_mods\replaced_by_modern_industrialization`.

Moved out of active loading:
- `RebornCore-5.8.15.jar`;
- `TechReborn-5.8.15.jar`;
- `TechRebornJEI-20.1.9.jar`.

Notes:
- Old TechReborn config files were left in `config\techreborn` for rollback safety.
- Existing JEI / sound config entries that mention `techreborn` are stale preferences only; they do not load the mod jar.
- No code rebuild was required for this modpack-only replacement.


## 2026-07-05 - Distress link inserted into startup terminal errors

User request:
- Replace the startup terminal distress-link placeholder with the supplied Google Drive URL:
  `https://drive.google.com/file/d/1J0vgRLVbo4PEXXGE_tkav-As_s7ZY0M1/view?usp=sharing`.

Implemented:
- Replaced `ServerStartScreen.DISTRESS_LINK`.
- The startup error stream now prints the supplied URL in:
  - `[!] DISTRESS LINK: ...`;
  - `[!] FIND ME / ...`.
- Bumped project version to `0.1.6`.
- Updated GitHub updater manifest to point at `releases/Imperfect_salvation-0.1.6.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Installed the rebuilt jar to:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed jar size: `807885` bytes, timestamp `2026-07-05 23:28:54`.
- `releases/Imperfect_salvation-0.1.6.jar` SHA-256:
  `c83e63294ba973d1af8f343ff953485e5da2ea99dbf0e5b8f8ddcb414480800a`.


## 2026-07-06 - Filename-aware updater test release

User request:
- Prepare a tiny automatic update test through GitHub.
- Publish a new build with a different mod jar filename.
- Do not manually update the active game `mods` folder, so the startup updater can be tested honestly.

Implemented:
- Bumped project version to `0.1.7`.
- Updated `StartupModUpdater` to support an optional manifest field:
  `file_name`.
- Newer updater builds now install downloaded jars to the manifest target filename instead of always preserving the old jar filename.
- Added a second-stage self-rename path:
  if the current mod version already matches the manifest but the local jar filename is stale, the updater writes a helper script, exits Minecraft, renames the current jar to the manifest filename, and restarts Minecraft.
- This preserves compatibility with the currently installed `0.1.6` updater:
  first launch updates jar contents, second updater pass renames `Imperfect_salvation-0.1.0.jar` to `Imperfect_salvation-0.1.7.jar`.
- Updated GitHub updater manifest to point at:
  `releases/Imperfect_salvation-0.1.7.jar`.
- Added manifest `file_name`:
  `Imperfect_salvation-0.1.7.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Built jar metadata reports mod version `0.1.7`.
- Active game folder was intentionally not updated manually.
- Active game folder still contains:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- Installed active jar before the test remains size `807885` bytes, timestamp `2026-07-05 23:28:54`.
- `releases/Imperfect_salvation-0.1.7.jar` SHA-256:
  `581eac6e210355a870e56e023aa68ba41f4ce479467624ae44dd79ab416bae22`.


## 2026-07-06 - Updater relaunch fallback repair

User report:
- The automatic updater downloaded the new jar but did not replace the old jar in the active game folder.

Observed in active modpack:
- Active mod folder still contained:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\mods\Imperfect_salvation-0.1.0.jar`.
- That jar still reported internal mod version `0.1.6`.
- The updater cache contained a fully downloaded jar:
  `C:\Users\nikit\Desktop\Project Imperfect Salvation\.imperfect_salvation_updates\Imperfect_salvation-0.1.7.jar.tmp`.
- The downloaded tmp jar SHA-256 matched the GitHub manifest for `0.1.7`.
- `latest.log` contained:
  `java.io.IOException: Current Java arguments and command line are not available`.

Root cause:
- The old updater relied only on `ProcessHandle.Info.arguments()` or `ProcessHandle.Info.commandLine()`
  to reconstruct the Minecraft relaunch command.
- On this launcher/runtime combination both values were absent.
- Because relaunch data was treated as mandatory, the updater failed after download and before writing the apply-update helper script.

Implemented:
- Bumped project version to `0.1.8`.
- Added `UpdaterRelaunchSupport`.
- The updater now keeps the old ProcessHandle paths when available.
- If ProcessHandle does not expose launch data, the updater reconstructs the relaunch command from:
  - `ManagementFactory.getRuntimeMXBean().getInputArguments()`;
  - `java.class.path`;
  - `sun.java.command`;
  - `java.home\bin\javaw.exe` on Windows.
- The PowerShell helper still receives arguments through an explicit `$argsList` array, so paths and player/profile names with spaces remain quoted correctly.
- The filename-aware update path from `0.1.7` remains active.
- Updated GitHub updater manifest to point at:
  `releases/Imperfect_salvation-0.1.8.jar`.
- Manifest `file_name` is now:
  `Imperfect_salvation-0.1.8.jar`.

Verification:
- `.\gradlew.bat build` completed successfully.
- Direct updater self-test completed successfully:
  `java -cp "build\classes\java\main;build\classes\java\test" ru.nikit.megastructure.client.updater.StartupModUpdaterSelfTest`.
- Self-test covers:
  - quoted launch argument parsing;
  - runtime fallback relaunch command availability;
  - PowerShell relaunch script argument-array generation.
- Active game folder was intentionally not manually updated during this release preparation.
- `releases/Imperfect_salvation-0.1.8.jar` SHA-256:
  `6807c9d621414c785d123f842c95115644c93a4941d39038ad08ddd1ed2d39bb`.
