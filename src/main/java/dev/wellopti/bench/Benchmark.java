package dev.wellopti.bench;

import dev.wellopti.WellOptiClient;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Measures FPS with WellOpti on, then off, in the same spot, and prints the difference.
 *
 * <p>Each half gets a short warm-up (so the previous mode's leftovers don't count) followed by a
 * measuring window where every frame's duration is recorded. We report the average FPS and the
 * "1% low": the frame rate of the slowest 1% of frames, which is what you feel as stutter.
 */
public final class Benchmark {
	private static final long COUNTDOWN_NANOS = 3_000_000_000L;
	private static final long WARMUP_NANOS = 3_000_000_000L;
	private static final long MEASURE_NANOS = 12_000_000_000L;

	private enum Phase {
		IDLE,
		COUNTDOWN,
		WARMUP_ON,
		MEASURE_ON,
		WARMUP_OFF,
		MEASURE_OFF
	}

	private record Result(double averageFps, double onePercentLowFps, int frames) {
	}

	private static Phase phase = Phase.IDLE;
	private static long phaseStart;
	private static long lastFrame;
	private static boolean wasActive;
	private static final LongArrayList FRAME_TIMES = new LongArrayList();
	private static Result withWellOpti;

	private Benchmark() {
	}

	public static boolean isRunning() {
		return phase != Phase.IDLE;
	}

	public static void start(Minecraft minecraft) {
		if (isRunning()) {
			message(minecraft, Component.translatable("wellopti.benchmark.alreadyRunning").withStyle(ChatFormatting.RED));
			return;
		}
		if (minecraft.level == null) {
			message(minecraft, Component.translatable("wellopti.benchmark.needWorld").withStyle(ChatFormatting.RED));
			return;
		}

		wasActive = WellOptiClient.active;
		withWellOpti = null;
		enter(Phase.COUNTDOWN);
		message(minecraft, Component.translatable("wellopti.benchmark.started").withStyle(ChatFormatting.GOLD));
	}

	public static void stop(Minecraft minecraft, Component reason) {
		if (!isRunning()) {
			return;
		}
		finish();
		message(minecraft, reason.copy().withStyle(ChatFormatting.RED));
	}

	/** Called at the start of every frame. */
	public static void onFrame() {
		long now = System.nanoTime();
		if (phase == Phase.MEASURE_ON || phase == Phase.MEASURE_OFF) {
			if (lastFrame != 0) {
				FRAME_TIMES.add(now - lastFrame);
			}
		}
		lastFrame = now;
	}

	/** Called every client tick to move between phases and show progress. */
	public static void tick(Minecraft minecraft) {
		if (!isRunning()) {
			return;
		}

		if (minecraft.level == null) {
			stop(minecraft, Component.translatable("wellopti.benchmark.cancelled.leftWorld"));
			return;
		}

		// Menus and unfocused windows change the frame rate on purpose; numbers taken then would be meaningless.
		if (phase != Phase.COUNTDOWN && (minecraft.gui.screen() != null || !minecraft.getWindow().isFocused())) {
			stop(minecraft, Component.translatable("wellopti.benchmark.cancelled.menu"));
			return;
		}

		long elapsed = System.nanoTime() - phaseStart;
		switch (phase) {
			case COUNTDOWN -> {
				progress(minecraft, Component.translatable("wellopti.benchmark.countdown", secondsLeft(COUNTDOWN_NANOS, elapsed)));
				if (elapsed >= COUNTDOWN_NANOS) {
					WellOptiClient.active = true;
					enter(Phase.WARMUP_ON);
				}
			}
			case WARMUP_ON, MEASURE_ON -> {
				progress(minecraft, Component.translatable("wellopti.benchmark.measuringOn",
					secondsLeft(phase == Phase.WARMUP_ON ? WARMUP_NANOS + MEASURE_NANOS : MEASURE_NANOS, elapsed)));
				if (phase == Phase.WARMUP_ON && elapsed >= WARMUP_NANOS) {
					enter(Phase.MEASURE_ON);
				} else if (phase == Phase.MEASURE_ON && elapsed >= MEASURE_NANOS) {
					withWellOpti = summarize();
					WellOptiClient.active = false;
					enter(Phase.WARMUP_OFF);
				}
			}
			case WARMUP_OFF, MEASURE_OFF -> {
				progress(minecraft, Component.translatable("wellopti.benchmark.measuringOff",
					secondsLeft(phase == Phase.WARMUP_OFF ? WARMUP_NANOS + MEASURE_NANOS : MEASURE_NANOS, elapsed)));
				if (phase == Phase.WARMUP_OFF && elapsed >= WARMUP_NANOS) {
					enter(Phase.MEASURE_OFF);
				} else if (phase == Phase.MEASURE_OFF && elapsed >= MEASURE_NANOS) {
					Result without = summarize();
					Result with = withWellOpti;
					finish();
					report(minecraft, with, without);
				}
			}
			default -> {
			}
		}
	}

	private static void enter(Phase next) {
		phase = next;
		phaseStart = System.nanoTime();
		lastFrame = 0;
		FRAME_TIMES.clear();
	}

	private static void finish() {
		WellOptiClient.active = wasActive;
		phase = Phase.IDLE;
		FRAME_TIMES.clear();
	}

	private static Result summarize() {
		int frames = FRAME_TIMES.size();
		if (frames == 0) {
			return new Result(0, 0, 0);
		}

		long[] times = FRAME_TIMES.toLongArray();
		long total = 0;
		for (long t : times) {
			total += t;
		}

		java.util.Arrays.sort(times);
		int worstCount = Math.max(1, frames / 100);
		long worstTotal = 0;
		for (int i = frames - worstCount; i < frames; i++) {
			worstTotal += times[i];
		}

		double averageFps = frames * 1e9 / total;
		double onePercentLowFps = worstCount * 1e9 / worstTotal;
		return new Result(averageFps, onePercentLowFps, frames);
	}

	private static void report(Minecraft minecraft, Result with, Result without) {
		message(minecraft, Component.translatable("wellopti.benchmark.done").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		message(minecraft, Component.translatable("wellopti.benchmark.resultOn", fmt(with.averageFps), fmt(with.onePercentLowFps)).withStyle(ChatFormatting.GREEN));
		message(minecraft, Component.translatable("wellopti.benchmark.resultOff", fmt(without.averageFps), fmt(without.onePercentLowFps)).withStyle(ChatFormatting.GRAY));

		if (without.averageFps > 0 && without.onePercentLowFps > 0) {
			message(minecraft, Component.translatable("wellopti.benchmark.difference",
				percent(with.averageFps, without.averageFps), percent(with.onePercentLowFps, without.onePercentLowFps)).withStyle(ChatFormatting.YELLOW));
		}

		WellOptiClient.LOGGER.info("Benchmark: with WellOpti {} avg / {} 1% low ({} frames); without {} avg / {} 1% low ({} frames)",
			fmt(with.averageFps), fmt(with.onePercentLowFps), with.frames, fmt(without.averageFps), fmt(without.onePercentLowFps), without.frames);
	}

	private static String fmt(double fps) {
		return String.format("%.0f", fps);
	}

	private static String percent(double with, double without) {
		double change = (with / without - 1.0) * 100.0;
		return String.format("%+.0f%%", change);
	}

	private static int secondsLeft(long duration, long elapsed) {
		return (int) Math.max(0, Math.ceil((duration - elapsed) / 1e9));
	}

	private static void progress(Minecraft minecraft, Component text) {
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.GOLD));
		}
	}

	private static void message(Minecraft minecraft, Component text) {
		minecraft.gui.hud.getChat().addClientSystemMessage(text);
	}
}
