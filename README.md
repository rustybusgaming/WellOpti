# WellOpti

A client-side Fabric mod that makes Minecraft run very goodly.

WellOpti skips work your PC doesn't need to do: it doesn't draw things too far away to matter,
it stops particle floods, and it stops rendering at full speed when you aren't looking at the game.
Everything it does is visual only. The world, farms and redstone behave exactly like vanilla.

**Minecraft 1.21.9 – 1.21.11 and 26.1 – 26.3 · Fabric Loader 0.19.5+ · Fabric API**

## Supported versions

Download the jar that matches your Minecraft version. Each one is built and checked against that exact version.

| Minecraft | Jar |
| --- | --- |
| 26.3 | `wellopti-<version>+26.3.jar` |
| 26.2 | `wellopti-<version>+26.2.jar` |
| 26.1, 26.1.1, 26.1.2 | `wellopti-<version>+26.1.2.jar` |
| 1.21.11 | `wellopti-<version>+1.21.11.jar` |
| 1.21.9, 1.21.10 | `wellopti-<version>+1.21.10.jar` |

Every feature works the same on all of them. The 26.x jars need Java 25 (what those Minecraft versions ship with);
the 1.21.x jars run on Java 21.

## What it does

| Feature | What it does | Default |
| --- | --- | --- |
| **Occlusion culling** | Skips drawing mobs, items, chests, signs and other detailed objects that are completely hidden behind solid blocks. Vanilla only skips things outside your view, so a mob farm behind a wall or a storage room under your feet still gets drawn. This is usually the biggest win in caves, bases and around farms. | on |
| **Dynamic FPS** | Caps FPS when the game window isn't focused, drops it lower when the window is minimised, and caps it while a singleplayer game is paused. Your GPU, fans and battery get a break while you're alt-tabbed. | 30 FPS unfocused, 3 FPS minimised, 60 FPS paused |
| **Mob culling** | Stops drawing animals, villagers and hostile mobs past a set distance. Bosses, named mobs, glowing mobs and players are always drawn. | animals 48, villagers 48, hostile 64 blocks |
| **Crowd limit** | Draws at most a few mobs per block. Mob farms cram dozens of chickens, cows or villagers into one block, where you can't tell 8 from 80. The mobs are all still there; only the drawing is skipped. | 8 per block |
| **Far-mob detail** | Past a set distance, mobs skip their armor, held items, elytra, heads and saddles. Body details like sheep wool and villager outfits always draw, and so does players' gear. | 32 blocks |
| **Mob name tags** | Shortens how far away mob name tags are drawn (vanilla: 64 blocks). Players' name tags are left alone. | 32 blocks |
| **Entity culling** | Stops drawing dropped items, XP orbs, item frames, armor stands, paintings, arrows stuck in blocks, empty minecarts and boats, and ambient mobs (bats, fish, squid) past a set distance. Useful near mob farms, storage halls, item sorters and minecart systems. Glowing entities, arrows in flight and anything being ridden are always drawn. | items 48, XP 32, frames 48, stands 64, paintings 48, stuck arrows 24, minecarts & boats 64, ambient mobs 48 blocks |
| **Slower updates for hidden entities** | Entities WellOpti isn't drawing (behind walls, too far away, or over a farm's crowd limit) update 5 times a second on your client instead of 20, and catch up as soon as they're visible. The server still runs every mob normally, so farms don't change. Players, anything being ridden, and nearby or glowing entities always update normally. | on |
| **Block entity culling** | Stops drawing sign text, banners, heads, chests and shulker boxes, items on shelves, campfires and pots, and spawners (with their little spinning mob), past a set distance. Vanilla draws all of these out to 64 blocks. | signs 24, banners 48, heads 32, storage 48, displays 32, spawners 32 blocks |
| **Particle cap** | Puts a hard limit on live particles, so TNT, explosions and particle-spamming servers can't tank your FPS. Item pickup animations always get through. | 4000 particles |
| **Ambient particles** | Decorative particles (campfire smoke, rain splashes, drips, spores, falling leaves, ash, bubble columns, fireflies) only spawn near you, and you can thin them out. Particles that tell you something (hits, potions, explosions, block breaking) are never touched. | within 32 blocks, 100% density |

| **Adaptive mode** | When FPS drops below your target, all culling distances shrink a little at a time; they grow back once FPS recovers. Good for places that only lag sometimes. On in the Performance and Potato presets. | off, target 60 FPS, shrinks to 50% at most |
| **Background audio** | Turns the game's volume down while you're in another window or the game is minimised. | off (100%) until you change it |
| **Performance HUD** | A small corner overlay with FPS, frame time, memory use, and how much WellOpti is skipping each frame. | hidden |
| **Memory** | WellOpti keeps its own garbage to a minimum (no per-entity, per-frame allocations in its hot paths), shrinks its tables after busy scenes, and frees memory after you leave a world, while you're on the menu. The HUD shows allocation rate and garbage collections, and the benchmark reports memory churn. For deeper memory savings, install FerriteCore alongside. | on |
| **Memory advisor** | A short notice if Minecraft was given very little memory, or if memory stays nearly full for a while. | on |

Distance culling turns itself off while you're zoomed in with a spyglass, so far-away things are still visible when you look at them on purpose.

### How occlusion culling stays safe

It casts a few rays from your camera to each object's corners. Only if *every* ray hits a full, solid, opaque block is the object skipped,
so glass, leaves, slabs, fences and doors never hide anything. The rays run on a background thread, so they don't cost the game any frame time.
Results are re-checked several times a second, and sooner when you move, so things reappear almost instantly when you open a door or break a wall.
Anything without a fresh answer is drawn rather than guessed at. Players are never hidden, because their name tags show through walls.

## Presets

Pick one with the buttons at the top of the settings screen (Mod Menu's config button, `/wellopti config`, or the settings key).
WellOpti remembers the preset you pick on each server (singleplayer counts as one) and switches back to it when you join there again,
so you can use Potato on a laggy server and Quality in singleplayer. Turn that off with *Remember Preset Per Server*.

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

Run the built-in benchmark: press **Run Benchmark** in the settings screen, or type `/wellopti benchmark`.
Stand still somewhere busy (a mob farm, a storage room, a big build) and leave the game in front.
It measures 15 seconds with WellOpti on and 15 seconds with it off, then prints both results in chat:

```
With WellOpti: 142 FPS average, 88 FPS 1% low
Without WellOpti: 97 FPS average, 61 FPS 1% low
Difference: +46% average FPS, +44% 1% low
```

The **1% low** is the frame rate of your slowest 1% of frames, which is what you feel as stutter.
(The numbers above show the format, not a promise; your results depend on your PC and where you stand.)

You can also press **F7** to flip WellOpti off and on while you play and watch the F3 screen yourself.
The toggle isn't saved: WellOpti is always on when you start the game.

## Commands

- `/wellopti`: show what's turned on and the current distances
- `/wellopti config`: open the settings screen
- `/wellopti benchmark`: measure FPS with and without WellOpti (`/wellopti benchmark stop` cancels)
- `/wellopti toggle`: same as pressing F7
- `/wellopti hud`: show or hide the performance HUD (there's also a rebindable key, unbound by default)
- `/wellopti reload`: reload `config/wellopti.json` without restarting
- `/wellopti stats`: show how many draws and particles WellOpti skipped since you last checked

## Works with Sodium

WellOpti is tested to load alongside Sodium on every supported version (0.7.3 on 1.21.9–1.21.10, 0.8.14 on 1.21.11, 0.9.2 on 26.x),
with all of both mods' changes applied and no conflicts.

## Works with shaders (Iris)

WellOpti is tested to load alongside Iris and Sodium on every supported version. Shaders draw a shadow pass from the sun's point of view;
during it, WellOpti doesn't hide mobs that are only hidden from *your camera*, so they keep their shadows. Distance culling still applies,
since a mob too far away to draw is too far for its shadow to matter. To try it in a dev client: `./gradlew runClient -PwithIris`.
Sodium makes chunks draw faster; WellOpti decides which entities and block entities are worth drawing at all. The two stack.
To try it in a dev client: `./gradlew runClient -PwithSodium`.

**Tip:** for extra FPS in forests, turn off vanilla's *Cutout Leaves* in Video Settings. That stops drawing the leaf faces hidden inside trees.

## Languages

English, Deutsch, Español, Français, Português (Brasil), Русский, 日本語, 한국어 and 简体中文. Corrections and new languages are welcome:
copy `src/main/resources/assets/wellopti/lang/en_us.json`, translate the values, and `./gradlew test` checks that nothing's missing a placeholder.

## Pair it with

WellOpti is built to sit alongside the big performance mods, not to replace them. For the best results, also install:

- **Sodium**: a much faster chunk renderer
- **Lithium**: faster game logic and server ticks
- **FerriteCore**: lower memory use

## Building

```sh
./gradlew build                 # Minecraft 26.3 (the default)
./gradlew build -Pmc=26.2       # any version listed in versions/
./gradlew buildAllVersions      # every supported version
```

Jars are written to `build/libs/wellopti-<version>+<minecraft>.jar`. Building needs a Java 25 JDK.

How the multi-version build works: almost all the code is shared in `src/main`. The few bits that differ between
Minecraft versions (where the game keeps the current screen, the chat and the toasts, window focus, the HUD's drawing calls,
Fabric API names that changed in 26.1, and where the per-frame entity pass starts) live in small classes under
`src/compat/<name>`. Each `versions/<minecraft>.properties` picks its compat folder, its Fabric API, Mod Menu and Sodium versions,
and for obfuscated releases like 1.21.11 sets `obfuscated=true` and `java_version=21`, which switches the build to Loom's
remapping plugin with Mojang's official names. The source reads the same either way. To add a version, add a properties file,
point it at an existing compat folder, and only write a new one if it doesn't compile.
`./gradlew test` runs the unit tests for the occlusion raycaster, the config and the translations.

GitHub Actions builds the jar and runs the tests on every push. Download it from the run's **Artifacts** section.
Pushing a tag like `v1.4.0` publishes a GitHub release with the jar attached.
To also publish to Modrinth, add a `MODRINTH_TOKEN` secret and a `MODRINTH_PROJECT_ID` variable in the repository's
Settings → Secrets and variables → Actions. Without them, the Modrinth step is skipped.
Drop the jar and Fabric API into your `mods` folder.

## License

MIT
