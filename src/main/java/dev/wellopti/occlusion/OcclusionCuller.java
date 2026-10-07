package dev.wellopti.occlusion;

import dev.wellopti.WellOptiClient;
import dev.wellopti.WellOptiStats;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
 * <p>The raycasts run on a background thread so they never cost the render thread a frame. The render
 * thread only reads cached answers and queues up anything stale; finished answers come back through a
 * queue it drains once per frame, so the per-object lookups stay on one thread and allocation-free.
 * Anything without a fresh answer is drawn: this class should never be the reason something you could
 * see goes missing.
 */
public final class OcclusionCuller {
	private static final long VISIBLE_RECHECK_NANOS = 300_000_000L;
	private static final long HIDDEN_RECHECK_NANOS = 100_000_000L;
	/** A hidden answer that is this old and still hasn't been refreshed is no longer trusted. */
	private static final long HIDDEN_GRACE_NANOS = 1_000_000_000L;
	private static final double CAMERA_MOVE_RECHECK_SQR = 1.0;
	/** Things this close are always drawn; checking them isn't worth it. */
	private static final double ALWAYS_VISIBLE_SQR = 4.0 * 4.0;
	/** Skip huge things (the ender dragon, giant hitboxes from mods); too many rays, too little gain. */
	private static final double MAX_CHECKED_SIZE = 8.0;
	private static final int MAX_RAY_STEPS = 256;
	/** Pull corner sample points slightly inwards so rays don't graze along a neighbouring block face. */
	private static final double INSET = 0.05;
	private static final int MAX_QUEUED = 8192;
	private static final long EVICT_AFTER_NANOS = 5_000_000_000L;

	// Render-thread state. Primitive maps, so the per-entity, per-frame lookups below allocate nothing:
	// with boxed Long keys, a busy scene was creating hundreds of thousands of throwaway objects a second.
	private static final Long2ObjectOpenHashMap<Result> RESULTS = new Long2ObjectOpenHashMap<>();
	private static final Long2LongOpenHashMap LAST_USED = new Long2LongOpenHashMap();
	private static final LongOpenHashSet PENDING = new LongOpenHashSet();

	// Hand-off between threads. Requests are only made when an answer is stale (a few times a second per object).
	private static final LinkedBlockingQueue<Request> QUEUE = new LinkedBlockingQueue<>(MAX_QUEUED);
	private static final ConcurrentLinkedQueue<Result> COMPLETED = new ConcurrentLinkedQueue<>();
	private static volatile ClientLevel currentLevel;
	private static Thread worker;

	/** A finished check. {@code answered} is false when the check was dropped or failed; the object then counts as visible. */
	private record Result(long key, long checkedAt, double camX, double camY, double camZ, boolean visible, boolean answered, ClientLevel level) {
	}

	private record Request(long key, ClientLevel level, double camX, double camY, double camZ,
		double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
	}

	private OcclusionCuller() {
	}

	public static boolean isEntityHidden(Entity entity, ClientLevel level, double camX, double camY, double camZ) {
		AABB box = entity.getBoundingBox();
		if (box.getXsize() > MAX_CHECKED_SIZE || box.getYsize() > MAX_CHECKED_SIZE || box.getZsize() > MAX_CHECKED_SIZE) {
			return false;
		}

		double dx = Math.max(Math.max(box.minX - camX, camX - box.maxX), 0.0);
		double dy = Math.max(Math.max(box.minY - camY, camY - box.maxY), 0.0);
		double dz = Math.max(Math.max(box.minZ - camZ, camZ - box.maxZ), 0.0);
		if (dx * dx + dy * dy + dz * dz < ALWAYS_VISIBLE_SQR) {
			return false;
		}

		return isHidden(entity.getId(), level, camX, camY, camZ, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public static boolean isBlockEntityHidden(BlockPos pos, ClientLevel level, Vec3 cam) {
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();
		if (cam.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) < ALWAYS_VISIBLE_SQR) {
			return false;
		}

		return isHidden(blockKey(pos), level, cam.x, cam.y, cam.z, x, y, z, x + 1, y + 1, z + 1);
	}

	/** Called at the start of each frame: takes in whatever the worker finished since last frame. */
	public static void beginFrame() {
		Result result;
		while ((result = COMPLETED.poll()) != null) {
			PENDING.remove(result.key);
			if (!result.answered || result.level != currentLevel) {
				RESULTS.remove(result.key);
			} else {
				RESULTS.put(result.key, result);
			}
		}
	}

	/** Called once per client tick: tracks level changes and drops entries nobody has asked about lately. */
	public static void tick(ClientLevel level) {
		if (level != currentLevel) {
			currentLevel = level;
			clear();
			return;
		}

		long cutoff = System.nanoTime() - EVICT_AFTER_NANOS;
		for (var iterator = LAST_USED.long2LongEntrySet().fastIterator(); iterator.hasNext(); ) {
			var entry = iterator.next();
			if (entry.getLongValue() < cutoff) {
				RESULTS.remove(entry.getLongKey());
				iterator.remove();
			}
		}
	}

	/** Forgets everything; also shrinks the tables back down after a big scene. */
	public static void clear() {
		QUEUE.clear();
		COMPLETED.clear();
		PENDING.clear();
		RESULTS.clear();
		LAST_USED.clear();
		PENDING.trim();
		RESULTS.trim();
		LAST_USED.trim();
	}

	private static boolean isHidden(long key, ClientLevel level, double camX, double camY, double camZ,
		double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		long now = System.nanoTime();
		LAST_USED.put(key, now);

		Result result = RESULTS.get(key);
		if (result != null && result.level != level) {
			result = null;
		}

		boolean stale = result == null
			|| now - result.checkedAt > (result.visible ? VISIBLE_RECHECK_NANOS : HIDDEN_RECHECK_NANOS)
			|| distanceSqr(camX, camY, camZ, result.camX, result.camY, result.camZ) > CAMERA_MOVE_RECHECK_SQR;
		if (stale && PENDING.add(key)) {
			ensureWorker();
			if (!QUEUE.offer(new Request(key, level, camX, camY, camZ, minX, minY, minZ, maxX, maxY, maxZ))) {
				PENDING.remove(key);
			}
		}

		if (result == null || result.visible) {
			return false;
		}

		// A hidden answer is only trusted while it's reasonably fresh; if the worker is swamped, draw it.
		if (now - result.checkedAt > HIDDEN_GRACE_NANOS) {
			return false;
		}

		WellOptiStats.occluded++;
		return true;
	}

	private static double distanceSqr(double x0, double y0, double z0, double x1, double y1, double z1) {
		double dx = x0 - x1;
		double dy = y0 - y1;
		double dz = z0 - z1;
		return dx * dx + dy * dy + dz * dz;
	}

	private static long blockKey(BlockPos pos) {
		// Entity ids are ints, so anything above the int range can't collide with them.
		return pos.asLong() ^ 0x7000_0000_0000_0000L;
	}

	private static synchronized void ensureWorker() {
		if (worker != null && worker.isAlive()) {
			return;
		}

		worker = new Thread(OcclusionCuller::workLoop, "WellOpti Occlusion");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY - 1);
		worker.start();
	}

	private static void workLoop() {
		while (true) {
			Request request;
			try {
				request = QUEUE.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return;
			}

			if (request == null) {
				continue;
			}

			// Every request gets a reply, even a dropped or failed one, so the render thread can clear it from PENDING.
			boolean answered = false;
			boolean visible = true;
			try {
				if (request.level == currentLevel) {
					visible = check(request);
					answered = true;
				}
			} catch (Throwable t) {
				// We read the world from off the render thread, which can occasionally catch a chunk mid-update.
				// Treat any failure as "visible" and carry on; a wrong guess here only costs a frame of drawing.
				WellOptiClient.LOGGER.debug("Occlusion check failed", t);
			}
			COMPLETED.add(new Result(request.key, System.nanoTime(), request.camX, request.camY, request.camZ, visible, answered, request.level));
		}
	}

	/** Casts rays from the camera to the box's centre and corners. */
	private static boolean check(Request r) {
		Raycast.Opacity opacity = new LevelOpacity(r.level);
		double cx = r.camX;
		double cy = r.camY;
		double cz = r.camZ;

		// A camera inside a solid block (spectator noclip, suffocating) sees nothing useful; don't hide anything.
		if (opacity.isOpaque(floor(cx), floor(cy), floor(cz))) {
			return true;
		}

		double x0 = r.minX + INSET;
		double y0 = r.minY + INSET;
		double z0 = r.minZ + INSET;
		double x1 = r.maxX - INSET;
		double y1 = r.maxY - INSET;
		double z1 = r.maxZ - INSET;

		return Raycast.isClear(cx, cy, cz, (r.minX + r.maxX) * 0.5, (r.minY + r.maxY) * 0.5, (r.minZ + r.maxZ) * 0.5, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x0, y0, z0, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x1, y0, z0, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x0, y1, z0, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x1, y1, z0, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x0, y0, z1, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x1, y0, z1, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x0, y1, z1, MAX_RAY_STEPS, opacity)
			|| Raycast.isClear(cx, cy, cz, x1, y1, z1, MAX_RAY_STEPS, opacity);
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
