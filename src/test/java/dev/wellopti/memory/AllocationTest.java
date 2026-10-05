package dev.wellopti.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wellopti.occlusion.Raycast;
import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AllocationTest {
	private static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

	@Test
	void allocationCounterMovesWhenWeAllocate() {
		long before = MemoryStats.totalAllocated();
		Assumptions.assumeTrue(before >= 0, "this JVM doesn't report allocation");
		byte[][] junk = new byte[64][];
		for (int i = 0; i < junk.length; i++) {
			junk[i] = new byte[64 * 1024];
		}
		assertTrue(MemoryStats.totalAllocated() - before >= 4L * 1024 * 1024, "allocating 4 MB should show up; got " + junk.length);
	}

	/** The occlusion worker casts thousands of rays a second; the ray march itself must not create garbage. */
	@Test
	void rayMarchingAllocatesNothing() {
		Assumptions.assumeTrue(THREADS.isThreadAllocatedMemorySupported());
		Raycast.Opacity wallAtX5 = (x, y, z) -> x == 5;

		// Warm up so the JIT compiles the loop (and any one-off class loading is done) before measuring.
		boolean sink = false;
		for (int i = 0; i < 20_000; i++) {
			sink ^= Raycast.isClear(0.5, 0.5, 0.5, 10.5, 3.5 + (i & 7) * 0.01, 2.5, 256, wallAtX5);
		}

		long before = THREADS.getCurrentThreadAllocatedBytes();
		for (int i = 0; i < 100_000; i++) {
			sink ^= Raycast.isClear(0.5, 0.5, 0.5, 10.5, 3.5 + (i & 7) * 0.01, 2.5, 256, wallAtX5);
		}
		long allocated = THREADS.getCurrentThreadAllocatedBytes() - before;

		// A few hundred bytes of noise is fine; one object per ray would be megabytes.
		assertTrue(allocated < 16 * 1024, "100k rays allocated " + allocated + " bytes (" + sink + ")");
	}
}
