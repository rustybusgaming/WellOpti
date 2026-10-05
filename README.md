# WellOpti

A client-side Fabric mod that makes Minecraft run very goodly.

WellOpti skips work your PC doesn't need to do: it doesn't draw things too far away to matter,
it stops particle floods, and it stops rendering at full speed when you aren't looking at the game.
Everything it does is visual only. The world, farms and redstone behave exactly like vanilla.

**Minecraft 26.3 · Fabric Loader 0.19.5+ · Fabric API · Java 25**

## What it does

| Feature | What it does | Default |
| --- | --- | --- |
| **Occlusion culling** | Skips drawing mobs, items, chests, signs and other detailed objects that are completely hidden behind solid blocks. Vanilla only skips things outside your view, so a mob farm behind a wall or a storage room under your feet still gets drawn. This is usually the biggest win in caves, bases and around farms. | on |
| **Dynamic FPS** | Caps FPS when the game window isn't focused, drops it lower when the window is minimised, and caps it while a singleplayer game is paused. Your GPU, fans and battery get a break while you're alt-tabbed. | 30 FPS unfocused, 3 FPS minimised, 60 FPS paused |
| **Mob culling** | Stops drawing animals, villagers and hostile mobs past a set distance. Bosses, named mobs, glowing mobs and players are always drawn. | animals 48, villagers 48, hostile 64 blocks |
| **Crowd limit** | Draws at most a few mobs per block. Mob farms cram dozens of chickens, cows or villagers into one block, where you can't tell 8 from 80. The mobs are all still there; only the drawing is skipped. | 8 per block |
| **Far-mob detail** | Past a set distance, mobs skip their armor, held items, elytra, heads and saddles. Body details like sheep wool and villager outfits always draw, and so does players' gear. | 32 blocks |
| **Mob name tags** | Shortens how far away mob name tags are drawn (vanilla: 64 blocks). Players' name tags are left alone. | 32 blocks |
| **Entity culling** | Stops drawing dropped items, XP orbs, item frames, armor stands, arrows stuck in blocks, and ambient mobs (bats, fish, squid) past a set distance. Useful near mob farms, storage halls and item sorters. Glowing entities and arrows in flight are always drawn. | items 48, XP 32, frames 48, stands 64, stuck arrows 24, ambient mobs 48 blocks |
| **Block entity culling** | Stops drawing sign text, banners, heads, chests and shulker boxes, items on shelves, campfires and pots, and spawners (with their little spinning mob), past a set distance. Vanilla draws all of these out to 64 blocks. | signs 24, banners 48, heads 32, storage 48, displays 32, spawners 32 blocks |
| **Particle cap** | Puts a hard limit on live particles, so TNT, explosions and particle-spamming servers can't tank your FPS. Item pickup animations always get through. | 4000 particles |

| **Adaptive mode** | When FPS drops below your target, all culling distances shrink a little at a time; they grow back once FPS recovers. Good for places that only lag sometimes. On in the Performance and Potato presets. | off, target 60 FPS, shrinks to 50% at most |
| **Background audio** | Turns the game's volume down while you're in another window or the game is minimised. | off (100%) until you change it |
| **Performance HUD** | A small corner overlay with FPS, frame time, memory use, and how much WellOpti is skipping each frame. | hidden |

Distance culling turns itself off while you're zoomed in with a spyglass, so far-away things are still visible when you look at them on purpose.

### How occlusion culling stays safe

It casts a few rays from your camera to each object's corners. Only if *every* ray hits a full, solid, opaque block is the object skipped,
so glass, leaves, slabs, fences and doors never hide anything. Results are cached and re-checked several times a second, and sooner when you move,
so things reappear almost instantly when you open a door or break a wall. If it ever runs out of time for checks in a busy frame,
it draws things rather than guessing. Players are never hidden, because their name tags show through walls.

## Presets

Pick one with the buttons at the top of the settings screen (Mod Menu's config button, `/wellopti config`, or the settings key):

| Preset | For |
| --- | --- |
| **Quality** | A light touch. It only culls at long range, so you'll barely notice anything missing. |
| **Balanced** | The defaults: a good boost with distances most people won't notice. |
| **Performance** | Shorter distances, fewer particles, lower background FPS. For weaker PCs and busy bases. |
| **Potato** | Anything for frames. Things pop in close to you, but your toaster will thank you. |

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
- `/wellopti hud`: show or hide the performance HUD (there's also a rebindable key, unbound by default)
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
`./gradlew test` runs the unit tests for the occlusion raycaster and the config.

GitHub Actions builds the jar and runs the tests on every push. Download it from the run's **Artifacts** section.
Pushing a tag like `v1.2.0` publishes a GitHub release with the jar attached.
Drop the jar and Fabric API into your `mods` folder.

## License

MIT
