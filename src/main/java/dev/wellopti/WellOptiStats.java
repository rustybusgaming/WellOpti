package dev.wellopti;

/** Counters for {@code /wellopti stats}. Only touched from the render thread. */
public final class WellOptiStats {
	public static long culledEntities;
	public static long culledBlockEntities;
	public static long droppedParticles;
	private static long since = System.nanoTime();

	private WellOptiStats() {
	}

	public static double secondsSinceReset() {
		return Math.max(1e-3, (System.nanoTime() - since) / 1e9);
	}

	public static void reset() {
		culledEntities = 0;
		culledBlockEntities = 0;
		droppedParticles = 0;
		since = System.nanoTime();
	}
}
