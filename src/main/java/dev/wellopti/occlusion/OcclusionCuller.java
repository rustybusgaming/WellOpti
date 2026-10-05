package dev.wellopti.occlusion;

import dev.wellopti.WellOptiStats;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hides entities and block entities that are completely behind solid blocks.
 *
 * <p>Vanilla only skips things outside the camera's view cone, so a mob farm behind a wall or a
 * storage room under your feet still gets fully drawn. Here we cast a few rays from the camera to
 * the corners of each object; if every ray hits a full opaque block, nothing could be seen and we
 * skip it.
 *
 * <p>Raycasts aren't free, so results are cached and refreshed on a timer, or sooner when the camera
 * moves. Hidden results expire faster than visible ones so things pop back in quickly when you
 * open a door. When the per-tick budget runs out, anything unchecked is treated as visible: this
 * class should never be the reason something you could see goes missing.
 */
public final class OcclusionCuller {
	private static final long VISIBLE_RECHECK_NANOS = 300_000_000L;
	private static final long HIDDEN_RECHECK_NANOS = 100_000_000L;
	/** When the budget runs out, a hidden result may be reused for this long before we give up and draw it. */
	private static final long HIDDEN_GRACE_NANOS = 1_000_000_000L;
	private static final double CAMERA_MOVE_RECHECK_SQR = 1.0;
	/** Things this close are always drawn; checking them isn't worth it. */
	private static final double ALWAYS_VISIBLE_SQR = 4.0 * 4.0;
	/** Skip huge things (the ender dragon, giant hitboxes from mods); too many rays, too little gain. */
	private static final double MAX_CHECKED_SIZE = 8.0;
	private static final int MAX_RAY_STEPS = 256;
	/** Pull corner sample points slightly inwards so rays don't graze along a neighbouring block face. */
	private static final double INSET = 0.05;
	private static final int CHECKS_PER_TICK = 1024;
	private static final long EVICT_AFTER_NANOS = 5_000_000_000L;

	private static final Int2ObjectOpenHashMap<Result> ENTITIES = new Int2ObjectOpenHashMap<>();
	private static final Long2ObjectOpenHashMap<Result> BLOCK_ENTITIES = new Long2ObjectOpenHashMap<>();
	private static int budget = CHECKS_PER_TICK;
	private static ClientLevel cachedLevel;

	private static final class Result {
		long checkedAt;
		long lastUsed;
		double camX;
		double camY;
		double camZ;
		boolean visible;
	}

	private OcclusionCuller() {
	}

	public static boolean isEntityHidden(Entity entity, ClientLevel level, Vec3 cam) {
		AABB box = entity.getBoundingBox();
		if (box.getXsize() > MAX_CHECKED_SIZE || box.getYsize() > MAX_CHECKED_SIZE || box.getZsize() > MAX_CHECKED_SIZE) {
			return false;
		}

		if (box.distanceToSqr(cam) < ALWAYS_VISIBLE_SQR) {
			return false;
		}

		Result result = lookup(ENTITIES.get(entity.getId()), level, cam);
		if (result == null) {
			Result fresh = check(level, cam, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
			if (fresh == null) {
				return false;
			}
			ENTITIES.put(entity.getId(), fresh);
			result = fresh;
		}
		return countIfHidden(result);
	}

	public static boolean isBlockEntityHidden(BlockPos pos, ClientLevel level, Vec3 cam) {
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();
		if (cam.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) < ALWAYS_VISIBLE_SQR) {
			return false;
		}

		long key = pos.asLong();
		Result result = lookup(BLOCK_ENTITIES.get(key), level, cam);
		if (result == null) {
			Result fresh = check(level, cam, x, y, z, x + 1, y + 1, z + 1);
			if (fresh == null) {
				return false;
			}
			BLOCK_ENTITIES.put(key, fresh);
			result = fresh;
		}
		return countIfHidden(result);
	}

	/** Called once per client tick: refills the raycast budget and drops entries nobody has asked about lately. */
	public static void tick(ClientLevel level) {
		budget = CHECKS_PER_TICK;
		if (level != cachedLevel) {
			clear();
			cachedLevel = level;
			return;
		}

		long cutoff = System.nanoTime() - EVICT_AFTER_NANOS;
		ENTITIES.values().removeIf(r -> r.lastUsed < cutoff);
		BLOCK_ENTITIES.values().removeIf(r -> r.lastUsed < cutoff);
	}

	public static void clear() {
		ENTITIES.clear();
		BLOCK_ENTITIES.clear();
	}

	/** Returns the cached result if it is still trustworthy, or null if it needs re-checking. */
	private static Result lookup(Result cached, ClientLevel level, Vec3 cam) {
		if (cached == null || level != cachedLevel) {
			return null;
		}

		long now = System.nanoTime();
		cached.lastUsed = now;
		long maxAge = cached.visible ? VISIBLE_RECHECK_NANOS : HIDDEN_RECHECK_NANOS;
		boolean stale = now - cached.checkedAt > maxAge || cam.distanceToSqr(cached.camX, cached.camY, cached.camZ) > CAMERA_MOVE_RECHECK_SQR;
		if (stale && budget > 0) {
			return null;
		}

		// Out of budget: an old "visible" answer is always safe to reuse. An old "hidden" one is reused only
		// briefly (so busy scenes don't flicker between checks), then we draw it rather than risk hiding it.
		if (stale && !cached.visible && now - cached.checkedAt > HIDDEN_GRACE_NANOS) {
			cached.visible = true;
		}
		return cached;
	}

	/** Casts rays to the box's centre and corners. Returns null if out of budget. */
	private static Result check(ClientLevel level, Vec3 cam, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (budget <= 0 || level != cachedLevel) {
			return null;
		}
		budget--;

		Raycast.Opacity opacity = new LevelOpacity(level);
		double cx = cam.x;
		double cy = cam.y;
		double cz = cam.z;

		// A camera inside a solid block (spectator noclip, suffocating) sees nothing useful; don't hide anything.
		boolean visible = opacity.isOpaque(floor(cx), floor(cy), floor(cz));

		if (!visible) {
			double x0 = minX + INSET;
			double y0 = minY + INSET;
			double z0 = minZ + INSET;
			double x1 = maxX - INSET;
			double y1 = maxY - INSET;
			double z1 = maxZ - INSET;

			visible = Raycast.isClear(cx, cy, cz, (minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x0, y0, z0, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x1, y0, z0, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x0, y1, z0, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x1, y1, z0, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x0, y0, z1, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x1, y0, z1, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x0, y1, z1, MAX_RAY_STEPS, opacity)
				|| Raycast.isClear(cx, cy, cz, x1, y1, z1, MAX_RAY_STEPS, opacity);
		}

		Result result = new Result();
		result.checkedAt = System.nanoTime();
		result.lastUsed = result.checkedAt;
		result.camX = cx;
		result.camY = cy;
		result.camZ = cz;
		result.visible = visible;
		return result;
	}

	private static boolean countIfHidden(Result result) {
		if (!result.visible) {
			WellOptiStats.occluded++;
			return true;
		}
		return false;
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	private static final class LevelOpacity implements Raycast.Opacity {
		private final ClientLevel level;
		private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		LevelOpacity(ClientLevel level) {
			this.level = level;
		}

		@Override
		public boolean isOpaque(int x, int y, int z) {
			return this.level.getBlockState(this.pos.set(x, y, z)).isSolidRender();
		}
	}
}
