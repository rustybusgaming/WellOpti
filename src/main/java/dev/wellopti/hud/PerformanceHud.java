package dev.wellopti.hud;

import dev.wellopti.compat.Mc;
import dev.wellopti.AdaptiveDistance;
import dev.wellopti.WellOptiClient;
import dev.wellopti.WellOptiStats;
import dev.wellopti.config.WellOptiConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/** A small corner overlay: FPS, frame time, memory, and what WellOpti skipped in the last second. */
public final class PerformanceHud {
	private static final int PADDING = 3;
	private static final int BACKGROUND = 0x90000000;
	private static final int TITLE = 0xFFFFAA00;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int GOOD = 0xFF55FF55;
	private static final int OK = 0xFFFFFF55;
	private static final int BAD = 0xFFFF5555;
	private static final long MEGABYTE = 1024L * 1024L;

	private record Line(String text, int color) {
	}

	public void draw(HudCanvas graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		WellOptiConfig.Hud cfg = WellOptiConfig.get().hud;
		if (!cfg.enabled || Mc.isHudHidden(minecraft) || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<Line> lines = new ArrayList<>();
		int fps = minecraft.getFps();
		String frameTime = fps > 0 ? String.format(" (%.1f ms)", 1000.0 / fps) : "";
		lines.add(new Line("WellOpti" + (WellOptiClient.active ? "" : " [OFF]"), WellOptiClient.active ? TITLE : BAD));
		lines.add(new Line(fps + " FPS" + frameTime, fps >= 60 ? GOOD : fps >= 30 ? OK : BAD));

		if (cfg.showMemory) {
			Runtime runtime = Runtime.getRuntime();
			long used = (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
			long max = runtime.maxMemory() / MEGABYTE;
			int percent = max > 0 ? (int) (used * 100 / max) : 0;
			lines.add(new Line(String.format("Mem %d%% %d/%d MB", percent, used, max), percent < 70 ? TEXT : percent < 90 ? OK : BAD));
		}

		if (cfg.showCulling) {
			// Counters tick once per drawn frame, so dividing by FPS gives "skipped per frame".
			int frames = Math.max(1, fps);
			lines.add(new Line("Skipping per frame:", TEXT));
			lines.add(new Line(String.format(" %d far entities", WellOptiStats.entitiesPerSecond() / frames), TEXT));
			lines.add(new Line(String.format(" %d far block entities", WellOptiStats.blockEntitiesPerSecond() / frames), TEXT));
			lines.add(new Line(String.format(" %d hidden behind walls", WellOptiStats.occludedPerSecond() / frames), TEXT));
			lines.add(new Line(String.format(" %d crowded mobs", WellOptiStats.crowdedPerSecond() / frames), TEXT));
			lines.add(new Line(String.format(" %d particles/s blocked", WellOptiStats.particlesPerSecond()), TEXT));
		}

		if (WellOptiConfig.get().adaptive.enabled) {
			int percent = Math.round(AdaptiveDistance.scale() * 100);
			lines.add(new Line("Adaptive: " + percent + "% distance", percent >= 100 ? GOOD : percent >= 75 ? OK : BAD));
		}

		Font font = minecraft.font;
		int width = 0;
		for (Line line : lines) {
			width = Math.max(width, font.width(line.text()));
		}
		int height = lines.size() * font.lineHeight;

		int boxWidth = width + PADDING * 2;
		int boxHeight = height + PADDING * 2 - 1;
		boolean right = cfg.corner == WellOptiConfig.HudCorner.TOP_RIGHT || cfg.corner == WellOptiConfig.HudCorner.BOTTOM_RIGHT;
		boolean bottom = cfg.corner == WellOptiConfig.HudCorner.BOTTOM_LEFT || cfg.corner == WellOptiConfig.HudCorner.BOTTOM_RIGHT;
		int x = right ? graphics.width() - boxWidth - 2 : 2;
		int y = bottom ? graphics.height() - boxHeight - 2 : 2;

		graphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND);
		int textY = y + PADDING;
		for (Line line : lines) {
			graphics.text(font, line.text(), x + PADDING, textY, line.color());
			textY += font.lineHeight;
		}
	}
}
