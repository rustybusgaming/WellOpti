package dev.wellopti;

import dev.wellopti.compat.Mc;
import dev.wellopti.config.WellOptiConfig;
import net.minecraft.client.Minecraft;

/**
 * Adaptive mode: nudges every culling distance down while FPS is under the target, and back up once it
 * recovers. It moves in small steps once a second, with a gap between "too slow" and "fast enough" so it
 * settles instead of bouncing.
 */
public final class AdaptiveDistance {
	private static final float STEP_DOWN = 0.10F;
	private static final float STEP_UP = 0.05F;
	/** Only shrink once FPS is clearly below target, so a game capped right at the target doesn't count as lagging. */
	private static final float LAG_MARGIN = 0.95F;
	/** Only grow distances back once FPS is comfortably above target. */
	private static final float RECOVER_MARGIN = 1.15F;

	private static float scale = 1.0F;
	private static int ticks;

	private AdaptiveDistance() {
	}

	/** Multiplier for culling distances: 1.0 normally, lower while adaptive mode is cutting back. */
	public static float scale() {
		return scale;
	}

	public static void tick(Minecraft minecraft) {
		WellOptiConfig.Adaptive cfg = WellOptiConfig.get().adaptive;
		if (!cfg.enabled || !WellOptiClient.active) {
			scale = 1.0F;
			return;
		}

		if (++ticks < 20) {
			return;
		}
		ticks = 0;

		// Dynamic FPS deliberately lowers the frame rate in these cases; don't mistake that for lag.
		if (minecraft.level == null || minecraft.isPaused() || !Mc.isWindowFocused(minecraft)) {
			return;
		}

		// A target above the player's own FPS limit could never be reached.
		int target = cfg.targetFps;
		int limit = minecraft.options.framerateLimit().get();
		if (limit < 260) {
			target = Math.min(target, limit);
		}

		int fps = minecraft.getFps();
		float min = cfg.minScale / 100.0F;
		if (fps < target * LAG_MARGIN) {
			scale = Math.max(min, scale - STEP_DOWN);
		} else if (fps > target * RECOVER_MARGIN || scale < min) {
			scale = Math.min(1.0F, scale + STEP_UP);
		}
	}
}
