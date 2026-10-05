package dev.wellopti.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.wellopti.WellOptiClient;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Everything WellOpti does can be tuned from {@code config/wellopti.json}.
 * Distances are in blocks. A distance of {@code 0} turns that particular cull off.
 */
public final class WellOptiConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static WellOptiConfig instance = new WellOptiConfig();

	public DynamicFps dynamicFps = new DynamicFps();
	public EntityCulling entityCulling = new EntityCulling();
	public BlockEntityCulling blockEntityCulling = new BlockEntityCulling();
	public Particles particles = new Particles();
	public OcclusionCulling occlusionCulling = new OcclusionCulling();
	public BackgroundAudio backgroundAudio = new BackgroundAudio();
	public Hud hud = new Hud();
	public Mobs mobs = new Mobs();
	public Adaptive adaptive = new Adaptive();

	public static final class DynamicFps {
		/** Lower the frame rate when the game window isn't focused or is minimised. */
		public boolean enabled = true;
		/** FPS cap while the window is visible but another window has focus. */
		public int unfocusedFps = 30;
		/** FPS cap while the window is minimised (vanilla uses 10). */
		public int minimizedFps = 3;
		/** FPS cap while a singleplayer game is paused. 0 turns this off. */
		public int pausedFps = 60;
	}

	public static final class EntityCulling {
		public boolean enabled = true;
		/** Never cull entities while zoomed in with a spyglass. */
		public boolean disableWhileScoping = true;
		public int droppedItems = 48;
		public int experienceOrbs = 32;
		public int itemFrames = 48;
		public int armorStands = 64;
		/** Arrows and tridents stuck in blocks, which pile up around skeleton farms. */
		public int stuckArrows = 24;
		/** Bats, fish, tadpoles and squid. */
		public int ambientMobs = 48;
		/** Cows, sheep, pigs, chickens, horses and other animals. */
		public int passiveMobs = 48;
		/** Villagers and wandering traders. */
		public int villagers = 48;
		/** Zombies, skeletons, creepers and other monsters. Bosses are never culled. */
		public int hostileMobs = 64;
	}

	public static final class BlockEntityCulling {
		public boolean enabled = true;
		public boolean disableWhileScoping = true;
		/** Sign text is unreadable long before vanilla's 64 block cut-off. */
		public int signText = 24;
		public int banners = 48;
		public int skulls = 32;
		/** Chests, ender chests and shulker boxes. */
		public int storage = 48;
		/** Shelves, campfires and decorated pots. */
		public int itemDisplays = 32;
		/** Monster spawners (which spin a little mob inside), trial spawners and vaults. */
		public int spawners = 32;
	}

	public static final class Particles {
		public boolean enabled = true;
		/** Hard cap on live particles; vanilla allows up to 16384 per render group. */
		public int maxParticles = 4000;
	}

	public static final class OcclusionCulling {
		/** Skip drawing things that are completely hidden behind solid blocks. */
		public boolean enabled = true;
		public boolean entities = true;
		public boolean blockEntities = true;
	}

	public static final class BackgroundAudio {
		/** Game volume, in percent, while another window has focus. 100 leaves it alone. */
		public int unfocusedVolume = 100;
		/** Game volume, in percent, while the window is minimised. 100 leaves it alone. */
		public int minimizedVolume = 100;
	}

	public static final class Hud {
		/** Small overlay with FPS, memory and how much WellOpti is skipping. */
		public boolean enabled = false;
		public HudCorner corner = HudCorner.TOP_LEFT;
		public boolean showMemory = true;
		public boolean showCulling = true;
	}

	public static final class Mobs {
		/** Past this distance, mobs skip drawing armor, held items, elytra and heads. 0 always draws them. */
		public int equipmentDistance = 32;
		/** Past this distance, name tags on mobs aren't drawn (vanilla: 64). Players' name tags are left alone. */
		public int nameTagDistance = 32;
		/**
		 * At most this many mobs are drawn per block. Mob farms cram dozens of animals into one block,
		 * where you can't tell 8 from 80 anyway. 0 draws them all.
		 */
		public int crowdLimit = 8;
	}

	public static final class Adaptive {
		/** Shrink culling distances automatically while FPS is below the target, and grow them back once it recovers. */
		public boolean enabled = false;
		public int targetFps = 60;
		/** The furthest distances can shrink, in percent of their normal value. */
		public int minScale = 50;
	}

	public enum HudCorner {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}

	public static WellOptiConfig get() {
		return instance;
	}

	public static void load() {
		WellOptiConfig loaded = null;
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				loaded = GSON.fromJson(reader, WellOptiConfig.class);
			} catch (IOException | JsonParseException e) {
				WellOptiClient.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}

		instance = loaded == null ? new WellOptiConfig() : loaded;
		instance.sanitize();
		instance.save();
	}

	public void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			WellOptiClient.LOGGER.error("Could not write {}", path, e);
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("wellopti.json");
	}

	/** Fills in sections missing from older or hand-edited files and clamps silly values. */
	void sanitize() {
		if (dynamicFps == null) dynamicFps = new DynamicFps();
		if (entityCulling == null) entityCulling = new EntityCulling();
		if (blockEntityCulling == null) blockEntityCulling = new BlockEntityCulling();
		if (particles == null) particles = new Particles();
		if (occlusionCulling == null) occlusionCulling = new OcclusionCulling();
		if (backgroundAudio == null) backgroundAudio = new BackgroundAudio();
		if (hud == null) hud = new Hud();
		if (hud.corner == null) hud.corner = HudCorner.TOP_LEFT;
		if (mobs == null) mobs = new Mobs();
		if (adaptive == null) adaptive = new Adaptive();

		dynamicFps.unfocusedFps = clamp(dynamicFps.unfocusedFps, 1, 260);
		dynamicFps.minimizedFps = clamp(dynamicFps.minimizedFps, 1, 260);
		dynamicFps.pausedFps = clamp(dynamicFps.pausedFps, 0, 260);

		entityCulling.droppedItems = clamp(entityCulling.droppedItems, 0, 1024);
		entityCulling.experienceOrbs = clamp(entityCulling.experienceOrbs, 0, 1024);
		entityCulling.itemFrames = clamp(entityCulling.itemFrames, 0, 1024);
		entityCulling.armorStands = clamp(entityCulling.armorStands, 0, 1024);
		entityCulling.stuckArrows = clamp(entityCulling.stuckArrows, 0, 1024);
		entityCulling.ambientMobs = clamp(entityCulling.ambientMobs, 0, 1024);
		entityCulling.passiveMobs = clamp(entityCulling.passiveMobs, 0, 1024);
		entityCulling.villagers = clamp(entityCulling.villagers, 0, 1024);
		entityCulling.hostileMobs = clamp(entityCulling.hostileMobs, 0, 1024);

		blockEntityCulling.signText = clamp(blockEntityCulling.signText, 0, 1024);
		blockEntityCulling.banners = clamp(blockEntityCulling.banners, 0, 1024);
		blockEntityCulling.skulls = clamp(blockEntityCulling.skulls, 0, 1024);
		blockEntityCulling.storage = clamp(blockEntityCulling.storage, 0, 1024);
		blockEntityCulling.itemDisplays = clamp(blockEntityCulling.itemDisplays, 0, 1024);
		blockEntityCulling.spawners = clamp(blockEntityCulling.spawners, 0, 1024);

		mobs.equipmentDistance = clamp(mobs.equipmentDistance, 0, 1024);
		mobs.nameTagDistance = clamp(mobs.nameTagDistance, 0, 64);
		mobs.crowdLimit = clamp(mobs.crowdLimit, 0, 1024);

		adaptive.targetFps = clamp(adaptive.targetFps, 10, 260);
		adaptive.minScale = clamp(adaptive.minScale, 10, 100);

		particles.maxParticles = clamp(particles.maxParticles, 0, 65536);

		backgroundAudio.unfocusedVolume = clamp(backgroundAudio.unfocusedVolume, 0, 100);
		backgroundAudio.minimizedVolume = clamp(backgroundAudio.minimizedVolume, 0, 100);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
