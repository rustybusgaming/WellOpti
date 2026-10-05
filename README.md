# WellOpti

A client-side Fabric mod that makes Minecraft run very goodly.

WellOpti skips work your PC doesn't need to do: it doesn't draw things too far away to matter,
it stops particle floods, and it stops rendering at full speed when you aren't looking at the game.
Everything it does is visual only. The world, farms and redstone behave exactly like vanilla.

**Minecraft 26.3 · Fabric Loader 0.19.5+ · Fabric API · Java 25**

## What it does

| Feature | What it does | Default |
| --- | --- | --- |
| **Dynamic FPS** | Caps FPS when the game window isn't focused, and drops it lower when the window is minimised. Your GPU, fans and battery get a break while you're alt-tabbed. | 30 FPS unfocused, 3 FPS minimised |
| **Entity culling** | Stops drawing dropped items, XP orbs, item frames and armor stands past a set distance. Useful near mob farms, storage halls and item sorters. Glowing entities are always drawn. | items 48, XP 32, frames 48, stands 64 blocks |
| **Block entity culling** | Stops drawing sign text, banners, heads, chests and shulker boxes, and items on shelves, campfires and pots, past a set distance. Vanilla draws all of these out to 64 blocks. | signs 24, banners 48, heads 32, storage 48, displays 32 blocks |
| **Particle cap** | Puts a hard limit on live particles, so TNT, explosions and particle-spamming servers can't tank your FPS. Item pickup animations always get through. | 4000 particles |

Culling turns itself off while you're zoomed in with a spyglass, so far-away things are still visible when you look at them on purpose.

## Commands

- `/wellopti`: show what's turned on and the current distances
- `/wellopti reload`: reload `config/wellopti.json` without restarting
- `/wellopti stats`: show how many draws and particles WellOpti skipped since you last checked

## Config

The first time you launch, WellOpti writes `config/wellopti.json`. Edit it and run `/wellopti reload`.
Every feature has an `enabled` switch, and setting any distance to `0` turns that one cull off.

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
Drop the jar and Fabric API into your `mods` folder.

## License

MIT
