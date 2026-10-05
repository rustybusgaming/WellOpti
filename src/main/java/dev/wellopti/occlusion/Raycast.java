package dev.wellopti.occlusion;

/**
 * Grid ray marching (Amanatides &amp; Woo) over a voxel world, with no Minecraft dependencies so it can be unit tested.
 */
public final class Raycast {
	@FunctionalInterface
	public interface Opacity {
		/** True if the cell at these block coordinates fully blocks sight. */
		boolean isOpaque(int x, int y, int z);
	}

	private Raycast() {
	}

	/**
	 * Walks every cell the segment passes through and reports whether none of them are opaque.
	 * The start and end cells are never tested: the camera may be inside a block (spectators), and
	 * the target may poke into a wall (item frames), and neither should count as being hidden.
	 *
	 * @param maxSteps how many cells to visit before giving up; giving up counts as clear, so a
	 *                 caller that runs out of budget never hides something by mistake
	 */
	public static boolean isClear(double x0, double y0, double z0, double x1, double y1, double z1, int maxSteps, Opacity opacity) {
		int x = floor(x0);
		int y = floor(y0);
		int z = floor(z0);
		int endX = floor(x1);
		int endY = floor(y1);
		int endZ = floor(z1);

		double dx = x1 - x0;
		double dy = y1 - y0;
		double dz = z1 - z0;

		int stepX = Integer.signum(endX - x);
		int stepY = Integer.signum(endY - y);
		int stepZ = Integer.signum(endZ - z);

		// Distance along the ray (as a fraction of its length) between successive grid lines on each axis.
		double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
		double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
		double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);

		// Fraction of the ray needed to reach the first grid line on each axis.
		double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : tDeltaX * (stepX > 0 ? (x + 1 - x0) : (x0 - x));
		double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : tDeltaY * (stepY > 0 ? (y + 1 - y0) : (y0 - y));
		double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : tDeltaZ * (stepZ > 0 ? (z + 1 - z0) : (z0 - z));

		if (x == endX && y == endY && z == endZ) {
			return true;
		}

		for (int steps = 0; steps < maxSteps; steps++) {
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					x += stepX;
					tMaxX += tDeltaX;
				} else {
					z += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else if (tMaxY < tMaxZ) {
				y += stepY;
				tMaxY += tDeltaY;
			} else {
				z += stepZ;
				tMaxZ += tDeltaZ;
			}

			if (x == endX && y == endY && z == endZ) {
				return true;
			}

			if (opacity.isOpaque(x, y, z)) {
				return false;
			}

			// Floating point drift can carry us past the end cell on a diagonal; stop rather than wander off.
			if ((stepX > 0 ? x > endX : x < endX) || (stepY > 0 ? y > endY : y < endY) || (stepZ > 0 ? z > endZ : z < endZ)) {
				return true;
			}
		}

		return true;
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}
}
