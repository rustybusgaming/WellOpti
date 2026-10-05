package dev.wellopti.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class WellOptiConfigTest {
	private static final Gson GSON = new Gson();

	@Test
	void balancedPresetMatchesDefaults() {
		WellOptiConfig preset = new WellOptiConfig();
		Preset.BALANCED.applyTo(preset);
		assertEquals(GSON.toJson(new WellOptiConfig()), GSON.toJson(preset), "the Balanced preset should be exactly the defaults");
	}

	@Test
	void presetsGetMoreAggressiveInOrder() {
		Preset[] presets = Preset.values();
		for (int i = 1; i < presets.length; i++) {
			WellOptiConfig lighter = new WellOptiConfig();
			WellOptiConfig heavier = new WellOptiConfig();
			presets[i - 1].applyTo(lighter);
			presets[i].applyTo(heavier);
			String pair = presets[i - 1] + " -> " + presets[i];
			assertLessOrEqual(heavier.entityCulling.droppedItems, lighter.entityCulling.droppedItems, pair);
			assertLessOrEqual(heavier.blockEntityCulling.storage, lighter.blockEntityCulling.storage, pair);
			assertLessOrEqual(heavier.particles.maxParticles, lighter.particles.maxParticles, pair);
			assertLessOrEqual(heavier.dynamicFps.unfocusedFps, lighter.dynamicFps.unfocusedFps, pair);
		}
	}

	@Test
	void missingSectionsAreFilledIn() {
		WellOptiConfig cfg = GSON.fromJson("{\"particles\": {\"enabled\": false, \"maxParticles\": 123}}", WellOptiConfig.class);
		cfg.sanitize();
		assertNotNull(cfg.dynamicFps);
		assertNotNull(cfg.occlusionCulling);
		assertNotNull(cfg.hud.corner);
		assertEquals(123, cfg.particles.maxParticles);
		assertEquals(false, cfg.particles.enabled);
		assertEquals(new WellOptiConfig().entityCulling.droppedItems, cfg.entityCulling.droppedItems);
	}

	@Test
	void sillyValuesAreClamped() {
		WellOptiConfig cfg = GSON.fromJson(
			"{\"dynamicFps\": {\"unfocusedFps\": -5, \"minimizedFps\": 9999},"
				+ " \"entityCulling\": {\"droppedItems\": -1},"
				+ " \"backgroundAudio\": {\"unfocusedVolume\": 250}}",
			WellOptiConfig.class);
		cfg.sanitize();
		assertEquals(1, cfg.dynamicFps.unfocusedFps);
		assertEquals(260, cfg.dynamicFps.minimizedFps);
		assertEquals(0, cfg.entityCulling.droppedItems);
		assertEquals(100, cfg.backgroundAudio.unfocusedVolume);
	}

	@Test
	void unknownHudCornerFallsBackToDefault() {
		WellOptiConfig cfg = GSON.fromJson("{\"hud\": {\"corner\": \"MIDDLE_OF_NOWHERE\"}}", WellOptiConfig.class);
		cfg.sanitize();
		assertEquals(WellOptiConfig.HudCorner.TOP_LEFT, cfg.hud.corner);
	}

	private static void assertLessOrEqual(int actual, int limit, String message) {
		if (actual > limit) {
			throw new AssertionError(message + ": expected " + actual + " <= " + limit);
		}
	}
}
