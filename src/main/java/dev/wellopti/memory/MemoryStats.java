package dev.wellopti.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * How hard the game is working Java's memory: how fast it creates new objects ("allocation rate") and how
 * often and how long the garbage collector runs to clean them up. High allocation means more collections,
 * and collections are what you feel as stutter. Sampled once a second from the JVM's own counters.
 */
public final class MemoryStats {
	private static final long MEGABYTE = 1024L * 1024L;
	private static final int WINDOW_SECONDS = 10;

	private static final com.sun.management.ThreadMXBean THREADS = threadBean();
	private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();

	private static long lastSampleNanos = System.nanoTime();
	private static long lastAllocated = totalAllocated();
	private static long lastGcCount = gcCount();
	private static long lastGcMillis = gcMillis();

	private static long allocatedBytesPerSecond = -1;
	private static final long[] gcCountWindow = new long[WINDOW_SECONDS];
	private static final long[] gcMillisWindow = new long[WINDOW_SECONDS];
	private static int windowIndex;

	private MemoryStats() {
	}

	/** Call every client tick; it only samples once a second. */
	public static void tick() {
		long now = System.nanoTime();
		long elapsed = now - lastSampleNanos;
		if (elapsed < 1_000_000_000L) {
			return;
		}

		long allocated = totalAllocated();
		if (allocated >= 0 && lastAllocated >= 0) {
			allocatedBytesPerSecond = Math.round((allocated - lastAllocated) * 1e9 / elapsed);
		}
		long count = gcCount();
		long millis = gcMillis();
		gcCountWindow[windowIndex] = Math.max(0, count - lastGcCount);
		gcMillisWindow[windowIndex] = Math.max(0, millis - lastGcMillis);
		windowIndex = (windowIndex + 1) % WINDOW_SECONDS;

		lastSampleNanos = now;
		lastAllocated = allocated;
		lastGcCount = count;
		lastGcMillis = millis;
	}

	/** Megabytes of new objects per second across all threads, or -1 if this JVM doesn't report it. */
	public static long allocationMegabytesPerSecond() {
		return allocatedBytesPerSecond < 0 ? -1 : allocatedBytesPerSecond / MEGABYTE;
	}

	/** Garbage collections in the last ten seconds. */
	public static long recentCollections() {
		long total = 0;
		for (long c : gcCountWindow) {
			total += c;
		}
		return total;
	}

	/** Milliseconds spent collecting garbage in the last ten seconds. */
	public static long recentCollectionMillis() {
		long total = 0;
		for (long m : gcMillisWindow) {
			total += m;
		}
		return total;
	}

	public static int windowSeconds() {
		return WINDOW_SECONDS;
	}

	/** Total bytes ever allocated by every thread, or -1 if unavailable. */
	public static long totalAllocated() {
		if (THREADS == null) {
			return -1;
		}
		try {
			return THREADS.getTotalThreadAllocatedBytes();
		} catch (UnsupportedOperationException | LinkageError e) {
			return -1;
		}
	}

	public static long gcMillis() {
		long total = 0;
		for (GarbageCollectorMXBean gc : COLLECTORS) {
			total += Math.max(0, gc.getCollectionTime());
		}
		return total;
	}

	private static long gcCount() {
		long total = 0;
		for (GarbageCollectorMXBean gc : COLLECTORS) {
			total += Math.max(0, gc.getCollectionCount());
		}
		return total;
	}

	private static com.sun.management.ThreadMXBean threadBean() {
		try {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			if (bean instanceof com.sun.management.ThreadMXBean sun && sun.isThreadAllocatedMemorySupported()) {
				if (!sun.isThreadAllocatedMemoryEnabled()) {
					sun.setThreadAllocatedMemoryEnabled(true);
				}
				return sun;
			}
		} catch (UnsupportedOperationException | SecurityException | LinkageError e) {
			// Not a HotSpot-style JVM; the HUD just won't show an allocation rate.
		}
		return null;
	}
}
