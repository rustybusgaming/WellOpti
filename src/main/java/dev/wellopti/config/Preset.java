package dev.wellopti.config;

import java.util.Locale;

/**
 * One-click bundles of settings, from "barely noticeable" to "anything for frames".
 * Applying a preset turns every optimisation on and sets its distances; audio and HUD settings are left alone.
 */
public enum Preset {
	//            items xp frames stands arrows ambient | signs banners skulls storage displays | particles | unfocused minimised paused
	QUALITY(      64,   48, 64,    96,    48,    64,      48,   64,     48,    64,     48,        8000,       60,       5,        60),
	BALANCED(     48,   32, 48,    64,    24,    48,      24,   48,     32,    48,     32,        4000,       30,       3,        60),
	PERFORMANCE(  32,   24, 32,    48,    16,    32,      16,   32,     24,    32,     24,        2000,       20,       1,        30),
	POTATO(       24,   16, 24,    32,    12,    24,      12,   24,     16,    24,     16,        1000,       10,       1,        20);

	private final int droppedItems;
	private final int experienceOrbs;
	private final int itemFrames;
	private final int armorStands;
	private final int stuckArrows;
	private final int ambientMobs;
	private final int signText;
	private final int banners;
	private final int skulls;
	private final int storage;
	private final int itemDisplays;
	private final int maxParticles;
	private final int unfocusedFps;
	private final int minimizedFps;
	private final int pausedFps;

	Preset(
		int droppedItems, int experienceOrbs, int itemFrames, int armorStands, int stuckArrows, int ambientMobs,
		int signText, int banners, int skulls, int storage, int itemDisplays,
		int maxParticles,
		int unfocusedFps, int minimizedFps, int pausedFps
	) {
		this.droppedItems = droppedItems;
		this.experienceOrbs = experienceOrbs;
		this.itemFrames = itemFrames;
		this.armorStands = armorStands;
		this.stuckArrows = stuckArrows;
		this.ambientMobs = ambientMobs;
		this.signText = signText;
		this.banners = banners;
		this.skulls = skulls;
		this.storage = storage;
		this.itemDisplays = itemDisplays;
		this.maxParticles = maxParticles;
		this.unfocusedFps = unfocusedFps;
		this.minimizedFps = minimizedFps;
		this.pausedFps = pausedFps;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "wellopti.preset." + id();
	}

	public void applyTo(WellOptiConfig cfg) {
		cfg.dynamicFps.enabled = true;
		cfg.dynamicFps.unfocusedFps = unfocusedFps;
		cfg.dynamicFps.minimizedFps = minimizedFps;
		cfg.dynamicFps.pausedFps = pausedFps;

		cfg.entityCulling.enabled = true;
		cfg.entityCulling.droppedItems = droppedItems;
		cfg.entityCulling.experienceOrbs = experienceOrbs;
		cfg.entityCulling.itemFrames = itemFrames;
		cfg.entityCulling.armorStands = armorStands;
		cfg.entityCulling.stuckArrows = stuckArrows;
		cfg.entityCulling.ambientMobs = ambientMobs;

		cfg.blockEntityCulling.enabled = true;
		cfg.blockEntityCulling.signText = signText;
		cfg.blockEntityCulling.banners = banners;
		cfg.blockEntityCulling.skulls = skulls;
		cfg.blockEntityCulling.storage = storage;
		cfg.blockEntityCulling.itemDisplays = itemDisplays;

		cfg.particles.enabled = true;
		cfg.particles.maxParticles = maxParticles;

		cfg.occlusionCulling.enabled = true;
		cfg.occlusionCulling.entities = true;
		cfg.occlusionCulling.blockEntities = true;
	}
}
