package dev.wellopti.occlusion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RaycastTest {
	private static final int STEPS = 400;

	private static final class Grid implements Raycast.Opacity {
		private final Set<Long> solid = new HashSet<>();

		Grid fill(int x, int y, int z) {
			solid.add(key(x, y, z));
			return this;
		}

		/** A solid wall across the whole YZ plane at this X, within a generous range. */
		Grid wallX(int x) {
			for (int y = -20; y <= 20; y++) {
				for (int z = -20; z <= 20; z++) {
					fill(x, y, z);
				}
			}
			return this;
		}

		@Override
		public boolean isOpaque(int x, int y, int z) {
			return solid.contains(key(x, y, z));
		}

		private static long key(int x, int y, int z) {
			return ((long) x & 0x1FFFFF) << 42 | ((long) y & 0x1FFFFF) << 21 | ((long) z & 0x1FFFFF);
		}
	}

	@Test
	void openAirIsClear() {
		assertTrue(Raycast.isClear(0.5, 0.5, 0.5, 10.5, 3.2, -7.9, STEPS, new Grid()));
	}

	@Test
	void wallBlocksStraightRay() {
		Grid grid = new Grid().wallX(5);
		assertFalse(Raycast.isClear(0.5, 0.5, 0.5, 10.5, 0.5, 0.5, STEPS, grid));
		assertFalse(Raycast.isClear(10.5, 0.5, 0.5, 0.5, 0.5, 0.5, STEPS, grid), "blocked in the other direction too");
	}

	@Test
	void wallBlocksDiagonalRay() {
		assertFalse(Raycast.isClear(0.2, 1.7, -3.1, 9.9, -2.3, 6.4, STEPS, new Grid().wallX(5)));
	}

	@Test
	void holeInWallLetsRayThrough() {
		Grid grid = new Grid().wallX(5);
		grid.solid.remove(Grid.key(5, 0, 0));
		assertTrue(Raycast.isClear(0.5, 0.5, 0.5, 10.5, 0.5, 0.5, STEPS, grid));
	}

	@Test
	void startAndEndCellsAreIgnored() {
		Grid grid = new Grid().fill(0, 0, 0).fill(4, 0, 0);
		assertTrue(Raycast.isClear(0.5, 0.5, 0.5, 4.5, 0.5, 0.5, STEPS, grid), "camera inside a block or target poking into a wall");
	}

	@Test
	void sameCellIsClear() {
		assertTrue(Raycast.isClear(0.1, 0.1, 0.1, 0.9, 0.9, 0.9, STEPS, new Grid().fill(0, 0, 0)));
	}

	@Test
	void singleBlockInTheWayBlocks() {
		assertFalse(Raycast.isClear(0.5, 64.5, 0.5, 0.5, 64.5, 6.5, STEPS, new Grid().fill(0, 64, 3)));
	}

	@Test
	void rayPassesBesideBlock() {
		assertTrue(Raycast.isClear(0.5, 64.5, 0.5, 0.5, 64.5, 6.5, STEPS, new Grid().fill(1, 64, 3)));
	}

	@Test
	void negativeCoordinatesWork() {
		// floor(-1.5) is -2: negative coordinates must round down, not towards zero.
		Grid grid = new Grid().fill(-3, -2, -3);
		assertFalse(Raycast.isClear(-0.5, -1.5, -0.5, -5.5, -1.5, -5.2, STEPS, grid));
		assertTrue(Raycast.isClear(-0.5, -0.5, -0.5, -5.5, -0.5, 0.5, STEPS, grid));
	}

	@Test
	void runningOutOfStepsCountsAsClear() {
		assertTrue(Raycast.isClear(0.5, 0.5, 0.5, 100.5, 0.5, 0.5, 3, new Grid().wallX(50)), "never hide something just because we gave up");
	}

	@Test
	void verticalRayIsBlockedByCeiling() {
		assertFalse(Raycast.isClear(0.5, 60.5, 0.5, 0.5, 70.5, 0.5, STEPS, new Grid().fill(0, 65, 0)));
	}
}
