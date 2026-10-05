package dev.wellopti;

import dev.wellopti.compat.Mc;
import dev.wellopti.config.WellOptiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * Two friendly nudges about Java memory, shown as toasts:
 * once per launch if the game was given very little memory, and if memory stays nearly full for a while.
 */
public final class MemoryAdvisor {
	private static final long MEGABYTE = 1024L * 1024L;
	private static final long LOW_ALLOCATION_MB = 2048;
	private static final double NEARLY_FULL = 0.90;
	private static final int NEARLY_FULL_SECONDS = 30;
	private static final long REPEAT_NANOS = 10L * 60L * 1_000_000_000L;
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(10_000L);

	private static boolean checkedAllocation;
	private static int ticks;
	private static int fullSeconds;
	private static long lastFullWarning = -REPEAT_NANOS;

	private MemoryAdvisor() {
	}

	/** Called when joining a world. */
	public static void onJoin(Minecraft minecraft) {
		if (checkedAllocation || !WellOptiConfig.get().memoryAdvisor) {
			return;
		}
		checkedAllocation = true;

		long maxMb = Runtime.getRuntime().maxMemory() / MEGABYTE;
		if (maxMb < LOW_ALLOCATION_MB) {
			toast(minecraft, Component.translatable("wellopti.memory.low.title"), Component.translatable("wellopti.memory.low.message", maxMb));
		}
	}

	/** Called every client tick; samples memory once a second. */
	public static void tick(Minecraft minecraft) {
		if (!WellOptiConfig.get().memoryAdvisor || minecraft.level == null || ++ticks < 20) {
			return;
		}
		ticks = 0;

		Runtime runtime = Runtime.getRuntime();
		double used = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
		fullSeconds = used >= NEARLY_FULL ? fullSeconds + 1 : 0;

		long now = System.nanoTime();
		if (fullSeconds >= NEARLY_FULL_SECONDS && now - lastFullWarning > REPEAT_NANOS) {
			lastFullWarning = now;
			fullSeconds = 0;
			toast(minecraft, Component.translatable("wellopti.memory.full.title"),
				Component.translatable("wellopti.memory.full.message", runtime.maxMemory() / MEGABYTE));
		}
	}

	private static void toast(Minecraft minecraft, Component title, Component message) {
		SystemToast.addOrUpdate(Mc.toasts(minecraft), TOAST, title, message);
	}
}
