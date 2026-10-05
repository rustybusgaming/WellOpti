package dev.wellopti.compat;

import dev.wellopti.hud.HudCanvas;
import dev.wellopti.hud.PerformanceHud;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Minecraft 26.1.x: screens, chat and toasts live directly on {@code Minecraft}. */
public final class Mc {
	private Mc() {
	}

	public static @Nullable Screen screen(Minecraft minecraft) {
		return minecraft.screen;
	}

	public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
		minecraft.setScreen(screen);
	}

	public static boolean isHudHidden(Minecraft minecraft) {
		return minecraft.options.hideGui;
	}

	public static ToastManager toasts(Minecraft minecraft) {
		return minecraft.getToastManager();
	}

	public static void systemMessage(Minecraft minecraft, Component message) {
		minecraft.gui.getChat().addClientSystemMessage(message);
	}

	public static Vec3 cameraPosition(Minecraft minecraft) {
		return minecraft.gameRenderer.getMainCamera().position();
	}

	public static boolean isWindowFocused(Minecraft minecraft) {
		return minecraft.getWindow().isFocused();
	}

	/** Shows a short message above the hotbar. */
	public static void actionBar(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(message);
		}
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyMappingHelper.registerKeyMapping(key);
	}

	public static void registerHud(Identifier id, PerformanceHud hud) {
		HudElementRegistry.addLast(id, (graphics, deltaTracker) -> hud.draw(new HudCanvas() {
			@Override
			public int width() {
				return graphics.guiWidth();
			}

			@Override
			public int height() {
				return graphics.guiHeight();
			}

			@Override
			public void fill(int x0, int y0, int x1, int y1, int color) {
				graphics.fill(x0, y0, x1, y1, color);
			}

			@Override
			public void text(Font font, String text, int x, int y, int color) {
				graphics.text(font, text, x, y, color, false);
			}
		}));
	}
}
