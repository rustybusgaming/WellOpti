# WellOpti

A client-side Fabric mod that makes Minecraft run very goodly.

WellOpti skips work your PC doesn't need to do: it doesn't draw things too far away to matter,
it stops particle floods, and it stops rendering at full speed when you aren't looking at the game.
Everything it does is visual only. The world, farms and redstone behave exactly like vanilla.

**Minecraft 26.3 · Fabric Loader 0.19.5+ · Fabric API · Java 25**

## What it does

| Feature | What it does | Default |
| --- | --- | --- |
| **Dynamic FPS** | Caps FPS when the game window isn't focused, drops it lower when the window is minimised, and caps it while a singleplayer game is paused. Your GPU, fans and battery get a break while you're alt-tabbed. | 30 FPS unfocused, 3 FPS minimised, 60 FPS paused |
| **Entity culling** | Stops drawing dropped items, XP orbs, item frames, armor stands, arrows stuck in blocks, and ambient mobs (bats, fish, squid) past a set distance. Useful near mob farms, storage halls and item sorters. Glowing entities and arrows in flight are always drawn. | items 48, XP 32, frames 48, stands 64, stuck arrows 24, ambient mobs 48 blocks |
| **Block entity culling** | Stops drawing sign text, banners, heads, chests and shulker boxes, and items on shelves, campfires and pots, past a set distance. Vanilla draws all of these out to 64 blocks. | signs 24, banners 48, heads 32, storage 48, displays 32 blocks |
| **Particle cap** | Puts a hard limit on live particles, so TNT, explosions and particle-spamming servers can't tank your FPS. Item pickup animations always get through. | 4000 particles |

Culling turns itself off while you're zoomed in with a spyglass, so far-away things are still visible when you look at them on purpose.

## Settings

Open the settings screen with `/wellopti config`, the **Open WellOpti Settings** key (unbound by default, set it in Controls),
or the config button in [Mod Menu](https://modrinth.com/mod/modmenu) if you have it installed.
It uses the same sliders and buttons as vanilla's Video Settings, and every option has a tooltip.

Settings are saved to `config/wellopti.json`. You can also edit that file by hand and run `/wellopti reload`.
Every feature has an on/off switch, and setting any distance to **Off** (`0` in the file) turns just that one cull off.

## Is it actually helping?

Press **F7** to switch WellOpti off and on while you play. The action bar shows which mode you're in.
Stand somewhere busy (a mob farm, a storage room, a big build), open the F3 screen, and compare FPS in each mode.
The toggle isn't saved: WellOpti is always on when you start the game.

## Commands

- `/wellopti`: show what's turned on and the current distances
- `/wellopti config`: open the settings screen
- `/wellopti toggle`: same as pressing F7
- `/wellopti reload`: reload `config/wellopti.json` without restarting
- `/wellopti stats`: show how many draws and particles WellOpti skipped since you last checked

## Pair it with

WellOpti is built to sit alongside the big performance mods, not to replace them. For the best results, also install:

- **Sodium**: a much faster chunk renderer
- **Lithium**: faster game logic and server ticks
- **FerriteCore**: lower memory use

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/wellopti-<version>.jar`. Building needs a Java 25 JDK.
GitHub Actions also builds the jar on every push. Download it from the run's **Artifacts** section.
Drop the jar and Fabric API into your `mods` folder.

## License

MIT
