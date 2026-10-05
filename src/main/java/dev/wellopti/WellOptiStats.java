package dev.wellopti;

/** Running counters behind {@code /wellopti stats} and the performance HUD. Only touched from the render thread. */
public final class WellOptiStats {
	public static long culledEntities;
	public static long culledBlockEntities;
	public static long occluded;
	public static long droppedParticles;

	/** Totals at the last {@code /wellopti stats} call. */
	private static final long[] commandBaseline = new long[4];
	private static long commandSince = System.nanoTime();

	/** Totals at the start of the current one-second HUD window, and the per-second rates from the last full window. */
	private static final long[] hudBaseline = new long[4];
	private static long hudWindowStart = System.nanoTime();
	private static final long[] perSecond = new long[4];

	private WellOptiStats() {
	}

	private static long[] totals() {
		return new long[] {culledEntities, culledBlockEntities, occluded, droppedParticles};
	}

	/** Rolls the HUD's one-second window. Call every client tick. */
	public static void tick() {
		long now = System.nanoTime();
		if (now - hudWindowStart < 1_000_000_000L) {
			return;
		}

		double seconds = (now - hudWindowStart) / 1e9;
		long[] totals = totals();
		for (int i = 0; i < totals.length; i++) {
			perSecond[i] = Math.round((totals[i] - hudBaseline[i]) / seconds);
			hudBaseline[i] = totals[i];
		}
		hudWindowStart = now;
	}

	public static long entitiesPerSecond() {
		return perSecond[0];
	}

	public static long blockEntitiesPerSecond() {
		return perSecond[1];
	}

	public static long occludedPerSecond() {
		return perSecond[2];
	}

	public static long particlesPerSecond() {
		return perSecond[3];
	}

	/** Returns {entities, blockEntities, occluded, particles, seconds} since the last call, then starts a new window. */
	public static double[] sinceLastCommand() {
		long now = System.nanoTime();
		long[] totals = totals();
		double[] result = new double[5];
		for (int i = 0; i < totals.length; i++) {
			result[i] = totals[i] - commandBaseline[i];
			commandBaseline[i] = totals[i];
		}
		result[4] = Math.max(1e-3, (now - commandSince) / 1e9);
		commandSince = now;
		return result;
	}
}
