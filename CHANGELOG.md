# Changelog

All notable changes to WellOpti. Each release's section is used as its release notes on GitHub and Modrinth.

## [1.6.0]

### Added
- Free memory after leaving a world: WellOpti drops its caches and asks Java to free the world you left while you're on the menu. Lowers memory use between worlds (it doesn't raise FPS). On by default; toggle under Memory in the settings.
- Memory diagnostics in the performance HUD: allocation rate (how fast new memory is being used up) and garbage collections in the last 10 seconds.
- The benchmark also reports memory churn and garbage collection time, with and without WellOpti.

### Changed
- Much less garbage from WellOpti itself, which means fewer garbage collections and the stutters they cause:
  - The behind-walls checks no longer create objects for every entity on every frame. Their per-frame bookkeeping now lives in primitive tables on the render thread.
  - The far-mob detail and name tag hooks no longer create a wrapper object for every mob layer on every frame.
  - The particle cap counts particles once per tick instead of walking every particle group on each spawn.
  - Block entity distance checks and the HUD text no longer allocate every frame.
- WellOpti's lookup tables shrink back down after a very busy scene and when you leave a world.

## [1.5.0]

### Added
- Minecraft 1.21.9 and 1.21.10 support (one jar for both).
- Hidden entities update less often: entities WellOpti isn't drawing (behind walls, too far away, or over a farm's crowd limit) tick 5 times a second on your client instead of 20, and catch up as soon as they're visible. The server still runs every mob normally. Players, anything being ridden, and nearby or glowing entities always tick normally.
- Presets are remembered per server: pick one while connected and it's switched back on next time you join that server. Singleplayer counts as one.
- Distance culling for paintings, and for minecarts and boats nobody is riding.
- French, Russian, Japanese and Korean translations.
- This changelog, used for release notes.

### Fixed
- Shaders (Iris): during the shadow pass, WellOpti no longer hides mobs that are behind walls from your camera, so they keep their shadows, and the shadow pass no longer counts towards the farm crowd limit.

## [1.4.0]

### Added
- Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2 support alongside 26.3, one jar per version.
- Built-in benchmark: measures FPS with WellOpti on and off in the same spot and reports average FPS and 1% lows (`/wellopti benchmark` or the settings button).
- Ambient particle range and density for decorative particles (campfire smoke, rain splashes, drips, spores, falling leaves).
- Memory advisor: a short notice if Minecraft has very little memory, or memory stays nearly full.
- German, Spanish, Brazilian Portuguese and Simplified Chinese translations.

### Changed
- Occlusion checks run on a background thread instead of the render thread.

### Fixed
- Three tooltips with a bare `%` showed their raw translation key instead of the text.

## [1.3.0]

### Added
- Mob culling: separate distances for animals, villagers and hostile mobs. Bosses, named mobs, glowing mobs and players are always drawn.
- Crowd limit: draw at most a few mobs per block, for mob farms.
- Far-mob detail: distant mobs skip armor, held items, heads, elytra and saddles.
- Shorter name tag distance for mobs.
- Spawner, trial spawner and vault culling.
- Adaptive mode: culling distances shrink while FPS is below a target and grow back when it recovers.

## [1.2.0]

### Added
- Occlusion culling: entities and block entities completely hidden behind solid blocks aren't drawn.
- Presets: Quality, Balanced, Performance and Potato, in the settings screen.
- Performance HUD with FPS, frame time, memory use and culling counts.
- Background audio: optional lower volume while unfocused or minimised.

## [1.1.0]

### Added
- In-game settings screen, also available from Mod Menu.
- F7 toggles WellOpti off and on, for comparing FPS.
- FPS cap while a singleplayer game is paused.
- Culling for arrows stuck in blocks and for ambient mobs (bats, fish, squid).

## [1.0.0]

### Added
- First release: dynamic FPS, distance culling for decorative entities and block entities, and a particle cap.
