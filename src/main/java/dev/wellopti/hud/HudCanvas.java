package dev.wellopti.hud;

import net.minecraft.client.gui.Font;

/** The four drawing calls the HUD needs; each Minecraft version's compat class adapts its own GUI renderer to this. */
public interface HudCanvas {
	int width();

	int height();

	void fill(int x0, int y0, int x1, int y1, int color);

	void text(Font font, String text, int x, int y, int color);
}
