package dev.wellopti;

import dev.wellopti.config.WellOptiConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Ticks entities WellOpti is hiding less often on the client.
 *
 * <p>Your client ticks every nearby entity 20 times a second to smooth its movement and run its animations,
 * even ones that aren't being drawn. For entities WellOpti hid last frame (behind walls, too far away, or over
 * a farm's crowd limit) that work is wasted, so they tick once every {@value #INTERVAL} ticks instead.
 *
 * <p>They aren't skipped entirely on purpose: the client moves entities towards their server position during
 * the tick, so a mob that never ticked would never move out from behind its wall on your screen. The server
 * runs the real mobs either way, so gameplay doesn't change.
 */
public final class TickThrottle {
	private static final int INTERVAL = 4;
	/** Entities this close always tick normally. */
	private static final double ALWAYS_TICK_SQR = 8.0 * 8.0;

	private static IntOpenHashSet hiddenThisFrame = new IntOpenHashSet();
	private static IntOpenHashSet hiddenLastFrame = new IntOpenHashSet();

	private TickThrottle() {
	}

	/** Called whenever WellOpti decides not to draw an entity this frame. */
	public static void markHidden(Entity entity) {
		hiddenThisFrame.add(entity.getId());
	}

	/** Called at the start of each frame's entity pass. */
	public static void beginFrame() {
		IntOpenHashSet previous = hiddenLastFrame;
		hiddenLastFrame = hiddenThisFrame;
		hiddenThisFrame = previous;
		hiddenThisFrame.clear();
	}

	public static boolean shouldSkipTick(Entity entity, ClientLevel level) {
		if (!WellOptiConfig.get().entityCulling.throttleHiddenTicks || !WellOptiClient.active || !hiddenLastFrame.contains(entity.getId())) {
			return false;
		}

		// Spread throttled entities across ticks so they don't all tick on the same one.
		if (((level.getGameTime() + entity.getId()) % INTERVAL) == 0) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Entity camera = minecraft.getCameraEntity();
		if (entity instanceof Player || entity == camera || entity.isVehicle() || entity.isPassenger() || entity.isCurrentlyGlowing()) {
			return false;
		}
		if (camera != null && entity.distanceToSqr(camera) < ALWAYS_TICK_SQR) {
			return false;
		}

		WellOptiStats.throttledTicks++;
		return true;
	}
}
