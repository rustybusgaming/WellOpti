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
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("wellopti.json");

	private static WellOptiConfig instance = new WellOptiConfig();

	public DynamicFps dynamicFps = new DynamicFps();
	public EntityCulling entityCulling = new EntityCulling();
	public BlockEntityCulling blockEntityCulling = new BlockEntityCulling();
	public Particles particles = new Particles();

	public static final class DynamicFps {
		/** Lower the frame rate when the game window isn't focused or is minimised. */
		public boolean enabled = true;
		/** FPS cap while the window is visible but another window has focus. */
		public int unfocusedFps = 30;
		/** FPS cap while the window is minimised (vanilla uses 10). */
		public int minimizedFps = 3;
	}

	public static final class EntityCulling {
		public boolean enabled = true;
		/** Never cull entities while zoomed in with a spyglass. */
		public boolean disableWhileScoping = true;
		public int droppedItems = 48;
		public int experienceOrbs = 32;
		public int itemFrames = 48;
		public int armorStands = 64;
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
	}

	public static final class Particles {
		public boolean enabled = true;
		/** Hard cap on live particles; vanilla allows up to 16384 per render group. */
		public int maxParticles = 4000;
	}

	public static WellOptiConfig get() {
		return instance;
	}

	public static void load() {
		WellOptiConfig loaded = null;
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				loaded = GSON.fromJson(reader, WellOptiConfig.class);
			} catch (IOException | JsonParseException e) {
				WellOptiClient.LOGGER.error("Could not read {}, using defaults", PATH, e);
			}
		}

		instance = loaded == null ? new WellOptiConfig() : loaded;
		instance.sanitize();
		instance.save();
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			WellOptiClient.LOGGER.error("Could not write {}", PATH, e);
		}
	}

	/** Fills in sections missing from older or hand-edited files and clamps silly values. */
	private void sanitize() {
		if (dynamicFps == null) dynamicFps = new DynamicFps();
		if (entityCulling == null) entityCulling = new EntityCulling();
		if (blockEntityCulling == null) blockEntityCulling = new BlockEntityCulling();
		if (particles == null) particles = new Particles();

		dynamicFps.unfocusedFps = clamp(dynamicFps.unfocusedFps, 1, 260);
		dynamicFps.minimizedFps = clamp(dynamicFps.minimizedFps, 1, 260);

		entityCulling.droppedItems = clamp(entityCulling.droppedItems, 0, 1024);
		entityCulling.experienceOrbs = clamp(entityCulling.experienceOrbs, 0, 1024);
		entityCulling.itemFrames = clamp(entityCulling.itemFrames, 0, 1024);
		entityCulling.armorStands = clamp(entityCulling.armorStands, 0, 1024);

		blockEntityCulling.signText = clamp(blockEntityCulling.signText, 0, 1024);
		blockEntityCulling.banners = clamp(blockEntityCulling.banners, 0, 1024);
		blockEntityCulling.skulls = clamp(blockEntityCulling.skulls, 0, 1024);
		blockEntityCulling.storage = clamp(blockEntityCulling.storage, 0, 1024);
		blockEntityCulling.itemDisplays = clamp(blockEntityCulling.itemDisplays, 0, 1024);

		particles.maxParticles = clamp(particles.maxParticles, 0, 65536);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
