# Changelog

All notable changes to WellOpti. Each release's section is used as its release notes on GitHub and Modrinth.

## [1.5.0]

### Added
- Minecraft 1.21.9 and 1.21.10 support (one jar for both).
- Distance culling for paintings, and for minecarts and boats nobody is riding.
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
