package dev.wellopti.config;

import java.util.Locale;

/**
 * One-click bundles of settings, from "barely noticeable" to "anything for frames".
 * Applying a preset turns every optimisation on and sets its distances; audio and HUD settings are left alone.
 */
public enum Preset {
	QUALITY {
		@Override
		void configure(WellOptiConfig cfg) {
			entities(cfg, 64, 48, 64, 96, 48, 64, 64, 64, 96);
			blockEntities(cfg, 48, 64, 48, 64, 48, 48);
			mobs(cfg, 48, 48, 16);
			cfg.particles.maxParticles = 8000;
			fps(cfg, 60, 5, 60);
			adaptive(cfg, false, 60, 75);
		}
	},
	BALANCED {
		@Override
		void configure(WellOptiConfig cfg) {
			entities(cfg, 48, 32, 48, 64, 24, 48, 48, 48, 64);
			blockEntities(cfg, 24, 48, 32, 48, 32, 32);
			mobs(cfg, 32, 32, 8);
			cfg.particles.maxParticles = 4000;
			fps(cfg, 30, 3, 60);
			adaptive(cfg, false, 60, 50);
		}
	},
	PERFORMANCE {
		@Override
		void configure(WellOptiConfig cfg) {
			entities(cfg, 32, 24, 32, 48, 16, 32, 32, 32, 48);
			blockEntities(cfg, 16, 32, 24, 32, 24, 24);
			mobs(cfg, 24, 24, 4);
			cfg.particles.maxParticles = 2000;
			fps(cfg, 20, 1, 30);
			adaptive(cfg, true, 60, 50);
		}
	},
	POTATO {
		@Override
		void configure(WellOptiConfig cfg) {
			entities(cfg, 24, 16, 24, 32, 12, 24, 24, 24, 32);
			blockEntities(cfg, 12, 24, 16, 24, 16, 16);
			mobs(cfg, 16, 16, 2);
			cfg.particles.maxParticles = 1000;
			fps(cfg, 10, 1, 20);
			adaptive(cfg, true, 30, 40);
		}
	};

	abstract void configure(WellOptiConfig cfg);

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "wellopti.preset." + id();
	}

	public void applyTo(WellOptiConfig cfg) {
		cfg.dynamicFps.enabled = true;
		cfg.entityCulling.enabled = true;
		cfg.blockEntityCulling.enabled = true;
		cfg.particles.enabled = true;
		cfg.occlusionCulling.enabled = true;
		cfg.occlusionCulling.entities = true;
		cfg.occlusionCulling.blockEntities = true;
		configure(cfg);
	}

	private static void entities(
		WellOptiConfig cfg, int items, int xp, int frames, int stands, int arrows, int ambient, int passive, int villagers, int hostile
	) {
		WellOptiConfig.EntityCulling e = cfg.entityCulling;
		e.droppedItems = items;
		e.experienceOrbs = xp;
		e.itemFrames = frames;
		e.armorStands = stands;
		e.stuckArrows = arrows;
		e.ambientMobs = ambient;
		e.passiveMobs = passive;
		e.villagers = villagers;
		e.hostileMobs = hostile;
	}

	private static void blockEntities(WellOptiConfig cfg, int signs, int banners, int skulls, int storage, int displays, int spawners) {
		WellOptiConfig.BlockEntityCulling b = cfg.blockEntityCulling;
		b.signText = signs;
		b.banners = banners;
		b.skulls = skulls;
		b.storage = storage;
		b.itemDisplays = displays;
		b.spawners = spawners;
	}

	private static void mobs(WellOptiConfig cfg, int equipment, int nameTags, int crowd) {
		cfg.mobs.equipmentDistance = equipment;
		cfg.mobs.nameTagDistance = nameTags;
		cfg.mobs.crowdLimit = crowd;
	}

	private static void fps(WellOptiConfig cfg, int unfocused, int minimized, int paused) {
		cfg.dynamicFps.unfocusedFps = unfocused;
		cfg.dynamicFps.minimizedFps = minimized;
		cfg.dynamicFps.pausedFps = paused;
	}

	private static void adaptive(WellOptiConfig cfg, boolean enabled, int targetFps, int minScale) {
		cfg.adaptive.enabled = enabled;
		cfg.adaptive.targetFps = targetFps;
		cfg.adaptive.minScale = minScale;
	}
}
