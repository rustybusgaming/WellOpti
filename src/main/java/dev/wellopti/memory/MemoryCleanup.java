package dev.wellopti.memory;

import dev.wellopti.Culling;
import dev.wellopti.WellOptiClient;
import dev.wellopti.config.WellOptiConfig;
import dev.wellopti.occlusion.OcclusionCuller;
import net.minecraft.client.Minecraft;

/**
 * Frees memory after you leave a world: WellOpti drops its own caches, then asks Java to collect the world you
 * just left. That runs while you're on the menu, where a short pause doesn't matter, instead of whenever Java
 * gets round to it during your next session. It lowers memory use between worlds; it doesn't raise FPS.
 */
public final class MemoryCleanup {
	private static final long MEGABYTE = 1024L * 1024L;
	/** Wait this many ticks after the world is gone, so the game has finished tearing it down. */
	private static final int DELAY_TICKS = 40;

	private static boolean pending;
	private static int ticksWithoutWorld;

	private MemoryCleanup() {
	}

	/** Called from the disconnect event. */
	public static void leftWorld() {
		pending = true;
		ticksWithoutWorld = 0;
	}

	public static void tick(Minecraft minecraft) {
		if (!pending) {
			return;
		}
		if (minecraft.level != null) {
			// Joined another world before the cleanup ran; skip it rather than pause during play.
			pending = false;
			return;
		}
		if (++ticksWithoutWorld < DELAY_TICKS) {
			return;
		}
		pending = false;

		OcclusionCuller.clear();
		Culling.clearCaches();
		if (!WellOptiConfig.get().memory.cleanOnLeave) {
			return;
		}

		Runtime runtime = Runtime.getRuntime();
		long before = runtime.totalMemory() - runtime.freeMemory();
		long start = System.nanoTime();
		System.gc();
		long after = runtime.totalMemory() - runtime.freeMemory();
		WellOptiClient.LOGGER.info("Freed {} MB after leaving the world ({} ms)",
			Math.max(0, (before - after) / MEGABYTE), (System.nanoTime() - start) / 1_000_000);
	}
}
